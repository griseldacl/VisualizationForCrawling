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
package phex.connection.handshake;

import phex.common.ServiceManager;
import phex.common.address.DestAddress;
import phex.common.address.IpAddress;
import phex.connection.ConnectionConstants;
import phex.connection.NetworkManager;
import phex.host.*;
import phex.http.*;



public abstract class HandshakeHandler implements ConnectionConstants
{
    protected Host connectedHost;

    public HandshakeHandler( Host connectedHost )
    {
        this.connectedHost = connectedHost;
    }

    /**
     * The default handshake headers are used for incoming and outgoing
     * connections. They are usually extended by the specific incoming and
     * outgoing headers.
     * @return
     */
    protected HTTPHeaderGroup createDefaultHandshakeHeaders()
    {
        // create hash map based on common headers
        HTTPHeaderGroup openHeaders = new HTTPHeaderGroup(
            HTTPHeaderGroup.ACCEPT_HANDSHAKE_GROUP );

        // add Listen-IP even though it might be 127.0.0.1 the port is the
        // most importent part...
        DestAddress myAddress = NetworkManager.getInstance().getLocalAddress();
        openHeaders.addHeader( new HTTPHeader( GnutellaHeaderNames.LISTEN_IP,
            myAddress.getFullHostName() ) );

        // add remote-IP
        DestAddress remoteAddress = connectedHost.getHostAddress();
        IpAddress ipAddress = remoteAddress.getIpAddress();
        openHeaders.addHeader( new HTTPHeader( GnutellaHeaderNames.REMOTE_IP,
            ipAddress.getFormatedString() + ":" + remoteAddress.getPort() ) );
            
        // accepting deflate encoding
        if ( ServiceManager.sCfg.isDeflateConnectionAccepted )
        {
            openHeaders.addHeader( new HTTPHeader(
                HTTPHeaderNames.ACCEPT_ENCODING, "deflate" ) );
        }

        return openHeaders;
    }
    
    public HTTPHeaderGroup createOutgoingHandshakeHeaders()
    {
        HTTPHeaderGroup outHeaders = createDefaultHandshakeHeaders();
        
        return outHeaders;
    }
    
    protected HandshakeStatus createCrawlerHandshakeStatus()
    {
        // create hash map based on common headers
        HTTPHeaderGroup crawlerHeaders = new HTTPHeaderGroup( 
            HTTPHeaderGroup.COMMON_HANDSHAKE_GROUP );
            
        HostManager hostMgr = HostManager.getInstance();
        NetworkHostsContainer networkHostsContainer =
            hostMgr.getNetworkHostsContainer();

        boolean isUltrapeer = hostMgr.isUltrapeer();
        
        crawlerHeaders.addHeader( new HTTPHeader(
            GnutellaHeaderNames.X_ULTRAPEER, String.valueOf( isUltrapeer ) ) );
        
        if ( isUltrapeer )
        {
            // add connected leaves...
            Host[] leafs = networkHostsContainer.getLeafConnections();
            if ( leafs.length > 0 )
            {
                String leafAddressString = buildHostAddressString(leafs, leafs.length );
                crawlerHeaders.addHeader( new HTTPHeader( GnutellaHeaderNames.LEAVES,
                    leafAddressString ) );
            }
        }
        
        // add connected ultrapeers        
        Host[] ultrapeers = networkHostsContainer.getUltrapeerConnections();
        if ( ultrapeers.length > 0 )
        {
            String ultrapeerAddressString = buildHostAddressString(ultrapeers, ultrapeers.length );
            crawlerHeaders.addHeader( new HTTPHeader( GnutellaHeaderNames.PEERS,
                ultrapeerAddressString ) );
        }
        
        return new HandshakeStatus( STATUS_CODE_OK,
            STATUS_MESSAGE_OK, crawlerHeaders );
    }

    protected HTTPHeaderGroup createRejectOutgoingHeaders()
    {
        // create hash map based on common headers
        HTTPHeaderGroup openHeaders = new HTTPHeaderGroup(
            HTTPHeaderGroup.COMMON_HANDSHAKE_GROUP );

        return openHeaders;
    }
    
    // TODO2 add x-try-ultrapeer only with high hop (far) ultrapeers
    // to initial outgoing connection headers and to incoming accept headers.
    // currently we have no way to determin these far ultrapeers...
    protected HTTPHeaderGroup createRejectIncomingHeaders()
    {
        // create hash map based on common headers
        HTTPHeaderGroup openHeaders = new HTTPHeaderGroup(
            HTTPHeaderGroup.COMMON_HANDSHAKE_GROUP );

        // add remote-IP
        openHeaders.addHeader( new HTTPHeader( GnutellaHeaderNames.REMOTE_IP,
            connectedHost.getHostAddress().getFullHostName() ) );
            
        // add X-Try-Ultrapeer
        NetworkHostsContainer networkHostsContainer =
            HostManager.getInstance().getNetworkHostsContainer();
        Host[] ultrpeers = networkHostsContainer.getUltrapeerConnections();
        String ultrapeerAddressString = buildHostAddressString(ultrpeers,
            ultrpeers.length );
        openHeaders.addHeader( new HTTPHeader( GnutellaHeaderNames.X_TRY_ULTRAPEERS,
            ultrapeerAddressString ) );

        return openHeaders;
    }

    public HandshakeStatus createHandshakeResponse( HandshakeStatus hostResponse,
       boolean isOutgoing )
    {
        if ( isOutgoing )
        {
            HTTPHeaderGroup myHeaders = new HTTPHeaderGroup(
                HTTPHeaderGroup.COMMON_HANDSHAKE_GROUP );
            
            // support for deflate... if accepted..
            if ( hostResponse.isDeflateAccepted() )
            {
                myHeaders.addHeader( new HTTPHeader(
                    HTTPHeaderNames.CONTENT_ENCODING, "deflate" ) );
            }            

            return new HandshakeStatus( STATUS_CODE_OK, STATUS_MESSAGE_OK,
                myHeaders );
        }
        // must be incoming..
        else if ( HostManager.getInstance().getNetworkHostsContainer().hasPeerSlotsAvailable() )
        {
            HTTPHeaderGroup myHeaders = createDefaultHandshakeHeaders();
            
            // support for deflate... if accepted..
            if ( hostResponse.isDeflateAccepted() )
            {
                myHeaders.addHeader( new HTTPHeader(
                    HTTPHeaderNames.CONTENT_ENCODING, "deflate" ) );
            }            

            return new HandshakeStatus( STATUS_CODE_OK, STATUS_MESSAGE_OK,
                myHeaders );
        }
        else
        {
            return new HandshakeStatus( STATUS_CODE_REJECTED,
                STATUS_MESSAGE_BUSY, createRejectIncomingHeaders() );
        }
    }

    public static HandshakeHandler createHandshakeHandler( Host connectedHost )
    {
        if ( HostManager.getInstance().isAbleToBecomeUltrapeer() )
        {
            return new UltrapeerHandshakeHandler( connectedHost );
        }
        else if ( ServiceManager.sCfg.allowToBecomeLeaf )
        {
            return new LeafHandshakeHandler( connectedHost );
        }
        else
        {
            return new PeerHandshakeHandler( connectedHost );
        }
    }
    
    protected String buildHostAddressString(Host[] hosts, int max )
    {
        StringBuffer buffer = new StringBuffer();
        max = Math.min( max, hosts.length );
        for ( int i = 0; i < hosts.length; i++ )
        {
            DestAddress address = hosts[i].getHostAddress();
            buffer.append( address.getFullHostName() );
            if( i < hosts.length - 1)
            {
                buffer.append(",");
            }
        }
        return buffer.toString();
    }

    protected boolean isCrawlerConnection(HTTPHeaderGroup headers)
    {
        HTTPHeader crawlerHeader = headers.getHeader( GnutellaHeaderNames.CRAWLER );
        if ( crawlerHeader == null )
        {
            return false;
        }
        float crawlerVersion;
        try
        {
            crawlerVersion = crawlerHeader.floatValue();
            if ( crawlerVersion >= 0.1f )
            {
                return true;
            }
            return false;
        }
        catch ( NumberFormatException exp )
        {
            return false;
        }
    }
}