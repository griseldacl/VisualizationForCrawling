/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2005 Phex Development Group
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package phex.connection;

import java.io.IOException;

import phex.common.ServiceManager;
import phex.common.address.DestAddress;
import phex.common.bandwidth.BandwidthManager;
import phex.download.PushHandler;
import phex.http.*;
import phex.msg.*;
import phex.net.connection.Connection;
import phex.net.connection.OIOSocketFactory;
import phex.net.presentation.SocketFacade;
import phex.query.BrowseHostResults;
import phex.utils.Logger;

/**
 * This class implements the basic functionality of the new Browse Host protocol.
 *
 * @author Gregor Koukkoullis (Phex Development Team) (C) 2002
 * @version 2002/09/20
 */
public class BrowseHostConnection
{
    private final BrowseHostResults results;
    private final DestAddress address;
    private final GUID hostGUID;

    public BrowseHostConnection( DestAddress aAddress, GUID aHostGUID,
        BrowseHostResults results )
    {
        address = aAddress;
        hostGUID = aHostGUID;
        this.results = results;
    }

    public void sendBrowseHostRequest()
        throws IOException, BrowseHostException
    {
        Logger.logMessage( Logger.INFO, Logger.SEARCH,
            "Connection for Browse Host to " + address );
        SocketFacade socket;
        try
        {
            socket = OIOSocketFactory.connect( address,
                ServiceManager.sCfg.mNetConnectionTimeout );
        }
        catch ( IOException exp )
        {// standard connection failed try push request, if we have a hostGUID
            if ( hostGUID == null )
            {
                throw exp;
            }

            socket = PushHandler.requestSocketViaPush( hostGUID,
                // HEX for Phex
                50484558 );
            if ( socket == null )
            {
                throw new IOException( "Push request failed" );
            }
        }
        
        Connection connection = new Connection( socket, 
            BandwidthManager.getInstance().getNetworkBandwidthController() );


        HTTPRequest request = new HTTPRequest( "GET", "/", true );
        request.addHeader( new HTTPHeader( HTTPHeaderNames.HOST, address.getFullHostName() ) );
        request.addHeader( new HTTPHeader( HTTPHeaderNames.ACCEPT,
        //    "text/html, application/x-gnutella-packets" ) );
            "application/x-gnutella-packets" ) );
        request.addHeader( new HTTPHeader( HTTPHeaderNames.CONTENT_LENGTH, "0" ) );
        request.addHeader( new HTTPHeader( HTTPHeaderNames.CONNECTION, "close" ) );

        String httpRequestStr = request.buildHTTPRequestString();
        Logger.logMessage( Logger.INFO, Logger.SEARCH,
            "Sending Browse Host request: " + httpRequestStr );
        connection.write( httpRequestStr.toString().getBytes() );

        HTTPResponse response;
        try
        {
            response = HTTPProcessor.parseHTTPResponse( connection );
        }
        catch ( HTTPMessageException exp )
        {
            throw new BrowseHostException( "Invalid HTTP Response: " + exp.getMessage() );
        }
        Logger.logMessage( Logger.INFO, Logger.SEARCH,
            "Received Browse Host response: " + response.buildHTTPResponseString() );

        if ( response.getStatusCode() < 200 || response.getStatusCode() > 299 )
        {
            throw new BrowseHostException( "Browse host request not successfull. StatusCode: " +
                response.getStatusCode() + " " + response.getStatusReason() );
        }

        HTTPHeader typeHeader = response.getHeader( HTTPHeaderNames.CONTENT_TYPE );
        if ( typeHeader == null )
        {
            throw new BrowseHostException( "Unknwon content-type." );
        }

        if ( typeHeader.getValue().equals( "application/x-gnutella-packets" ) )
        {
            MsgManager msgMgr = MsgManager.getInstance();
            byte[] headerBuffer = new byte[ MsgHeader.DATA_LENGTH ];
            while( true )
            {
                MsgHeader header = MessageProcessor.parseMessageHeader( connection,
                    headerBuffer );
                if ( header == null )
                {
                    break;
                }
                if ( header.getPayload() != MsgHeader.QUERY_HIT_PAYLOAD )
                {
                    throw new BrowseHostException( "Wrong header payload. Expecting query hit." );
                }
                QueryResponseMsg message = null;
                try
                {
                    message = ( QueryResponseMsg )MessageProcessor.parseMessage(
                        header, connection );
                }
                catch ( InvalidMessageException exp )
                {
                    Logger.logMessage( Logger.FINE, Logger.NETWORK, exp );
                    throw new IOException( "Invalid message returned: "
                        + exp.getMessage() );
                }
                msgMgr.getQueryResultMonitor().processResponse( message );
                results.processResponse( message );
            }
        }
        else
        {
            throw new BrowseHostException( "Not supported content-type. " + typeHeader.getValue() );
        }
     }
}