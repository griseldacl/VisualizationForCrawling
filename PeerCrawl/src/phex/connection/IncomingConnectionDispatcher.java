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
 * 
 *  --- CVS Information ---
 *  $Id: IncomingConnectionDispatcher.java,v 1.31 2005/11/13 10:06:32 gregork Exp $
 */
package phex.connection;

import java.io.IOException;
import java.io.OutputStream;

import phex.chat.ChatManager;
import phex.common.ServiceManager;
import phex.common.address.DestAddress;
import phex.common.bandwidth.BandwidthController;
import phex.common.bandwidth.BandwidthManager;
import phex.download.PushHandler;
import phex.host.Host;
import phex.host.HostConstants;
import phex.host.HostManager;
import phex.http.HTTPMessageException;
import phex.http.HTTPProcessor;
import phex.http.HTTPRequest;
import phex.msg.GUID;
import phex.net.connection.Connection;
import phex.net.presentation.SocketFacade;
import phex.share.ShareManager;
import phex.upload.UploadManager;
import phex.utils.*;

/**
 * If during negotiation it is clear that the remote
 * host has connected to obtain data via a GET request or to deliver data in
 * response to a push, then the worker delegates this on.
 */
public class IncomingConnectionDispatcher implements Runnable
{
    public static final String GET_REQUEST_PREFIX = "GET ";
    public static final String HEAD_REQUEST_PREFIX = "HEAD ";
    public static final String GIV_REQUEST_PREFIX = "GIV ";
    public static final String CHAT_REQUEST_PREFIX = "CHAT ";
    public static final String URI_DOWNLOAD_PREFIX = "PHEX_URI ";
    public static final String MAGMA_DOWNLOAD_PREFIX = "PHEX_MAGMA ";
    public static final String RSS_DOWNLOAD_PREFIX = "PHEX_RSS ";

    private final SocketFacade socket;

    public IncomingConnectionDispatcher( SocketFacade socket )
    {
        this.socket = socket;
    }

    public void run()
    {
        GnutellaInputStream gInStream = null;
        try
        {
            NetworkManager networkMgr = NetworkManager.getInstance();
            if (!networkMgr.isNetworkJoined() && !socket.getRemoteAddress().isLocalHost() )
            {
                throw new IOException( "Network not joined." );
            }
            socket.setSoTimeout( ServiceManager.sCfg.socketRWTimeout );
            BandwidthController bwController = BandwidthManager.getInstance()
                .getNetworkBandwidthController();
            Connection connection = new Connection(socket, bwController);
            String requestLine = connection.readLine();
            if ( requestLine == null )
            {
                throw new IOException( "Disconnected from remote host during handshake" );
            }
            NLogger.debug(NLoggerNames.IN_CONNECTION,
                "ConnectionRequest " + requestLine );

            String greeting = networkMgr.getGnutellaNetwork().getNetworkGreeting();
            if ( requestLine.startsWith( greeting + "/" ) )
            {
                DestAddress address = socket.getRemoteAddress();
                Host host = new Host( address, connection );
                host.setType( Host.TYPE_INCOMING );
                host.setStatus( HostConstants.STATUS_HOST_ACCEPTING, "" );
                HostManager.getInstance().acceptIncomingConnection( host,
                    requestLine );
            }
            // used from PushWorker
            else if ( requestLine.startsWith( GET_REQUEST_PREFIX )
                   || requestLine.startsWith( HEAD_REQUEST_PREFIX ) )
            {
                // requestLine = GET /get/1/foo doo.txt HTTP/1.1
                // browse host request = GET / HTTP/1.1
                // URN requestLine = GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0
                HTTPRequest httpRequest = HTTPProcessor.parseHTTPRequest( requestLine );
                HTTPProcessor.parseHTTPHeaders( httpRequest, connection );
                NLogger.debug(NLoggerNames.IN_CONNECTION,
                      httpRequest.getRequestMethod() + " Request: "
                    + httpRequest.buildHTTPRequestString() );
                if ( httpRequest.isGnutellaRequest() )
                {
                    UploadManager.getInstance().handleUploadRequest(
                        connection, httpRequest );
                }
                else
                {
                    // Incoming connection is a HTTP GET/HEAD to upload file.
                    ShareManager.getInstance().httpRequestHandler(
                        connection, httpRequest );
                }
            }
            // used when requesting push transfer
            else if ( requestLine.startsWith( GIV_REQUEST_PREFIX ) )
            {
                handleIncommingGIV(requestLine);
            }
            // used when requesting chat connection
            else if (requestLine.startsWith( CHAT_REQUEST_PREFIX ) )
            {
                DestAddress address = socket.getRemoteAddress();
                NLogger.debug(NLoggerNames.IN_CONNECTION,
                    "Chat request from: " + address );
                gInStream = connection.getInputStream();
                ChatManager.getInstance().acceptChat( socket, gInStream, address );
            }
            else if (requestLine.startsWith( URI_DOWNLOAD_PREFIX ) )
            {
                handleIncommingUriDownload(requestLine);
            }
            else if (requestLine.startsWith( MAGMA_DOWNLOAD_PREFIX ) )
            {
                handleIncommingMagmaDownload(requestLine);
            }
            else if (requestLine.startsWith( RSS_DOWNLOAD_PREFIX ) )
            {
                handleIncommingRSSDownload(requestLine);
            }
            else
            {
                throw new IOException("Unknown connection request: "
                    + requestLine );
            }
        }
        catch ( HTTPMessageException exp )
        {
            NLogger.debug(NLoggerNames.IN_CONNECTION, exp, exp );
            IOUtil.closeQuietly(gInStream);
            IOUtil.closeQuietly(socket);
        }
        catch ( IOException exp )
        {
            NLogger.debug(NLoggerNames.IN_CONNECTION, exp, exp );
            IOUtil.closeQuietly(gInStream);
            IOUtil.closeQuietly(socket);
        }
        catch ( Exception exp )
        {// catch all thats left...
            NLogger.error( NLoggerNames.IN_CONNECTION, exp, exp);
            IOUtil.closeQuietly(gInStream);
            IOUtil.closeQuietly(socket);
        }
    }

    /**
     * @param requestLine
     * @throws IOException
     */
    private void handleIncommingUriDownload(String requestLine) throws IOException
    {
        try
        {
            // this must be a request from local host
            if ( !socket.getRemoteAddress().isLocalHost() )
            {
                return;
            }
            OutputStream outStream  = socket.getOutputStream();
            outStream.write( "OK".getBytes() );
            outStream.flush();
        }
        finally
        {
            IOUtil.closeQuietly(socket);
        }
        String uriToken = requestLine.substring( 4 );
        NetworkManager.getInstance().fireIncomingUriDownload( uriToken );
    }
    
    /**
     * @param requestLine
     * @throws IOException
     */
    private void handleIncommingMagmaDownload(String requestLine) throws IOException
    {
        try
        {
            // this must be a request from local host
            if ( !socket.getRemoteAddress().isLocalHost() )
            {
                return;
            }
            OutputStream outStream  = socket.getOutputStream();
            outStream.write( "OK".getBytes() );
            outStream.flush();
        }
        finally
        {
            IOUtil.closeQuietly(socket);
        }
        String fileNameToken = requestLine.substring( 4 );
        NetworkManager.getInstance().fireIncomingMagmaDownload( fileNameToken );
    }
    private void handleIncommingRSSDownload(String requestLine) throws IOException
    {
        try
        {
            // this must be a request from local host
            if ( !socket.getRemoteAddress().isLocalHost() )
            {
                return;
            }
            OutputStream outStream  = socket.getOutputStream();
            outStream.write( "OK".getBytes() );
            outStream.flush();
        }
        finally
        {
            IOUtil.closeQuietly(socket);
        }
        String fileNameToken = requestLine.substring( 4 );
        NetworkManager.getInstance().fireIncomingRSSDownload( fileNameToken );
    }

    private void handleIncommingGIV(String requestLine)
    {
        // A correct request line should line should be:
        // GIV <file-ref-num>:<ClientID GUID in hexdec>/<filename>\n\n
        String remainder = requestLine.substring(4); // skip GIV
        
        try
        {
            // get file number index position
            int fileNumIdx = remainder.indexOf(':');
            // extract file index... and drop it, since we dont use it.
            /*String fileIndex = */remainder.substring(0, fileNumIdx);

            // get GUID end index position.
            int guidIdx = remainder.indexOf('/', fileNumIdx);
            // extract GUID...
            String guidStr = remainder.substring(fileNumIdx + 1, guidIdx);

            // extract file name
            String givenFileName = remainder.substring(guidIdx + 1);
            givenFileName = URLCodecUtils.decodeURL(givenFileName);

            GUID givenGUID = new GUID(guidStr);
            PushHandler.handleIncommingGIV(socket, givenGUID, givenFileName);
        }
        catch ( IndexOutOfBoundsException exp )
        {
            // handle possible out of bounds exception for better logging...
            NLogger.error( NLoggerNames.IN_CONNECTION, 
                "Failed to parse GIV: " + requestLine, exp);
        }        
    }
}