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
 *  $Id: UltrapeerHandshakeHandler.java,v 1.17 2005/10/03 00:18:22 gregork Exp $
 */
package phex.connection.handshake;

import phex.common.Cfg;
import phex.common.ServiceManager;
import phex.connection.ConnectionConstants;
import phex.host.Host;
import phex.host.HostManager;
import phex.host.NetworkHostsContainer;
import phex.http.*;

public class UltrapeerHandshakeHandler extends HandshakeHandler
    implements ConnectionConstants
{
    private HostManager hostMgr;
    private NetworkHostsContainer hostContainer;

    public UltrapeerHandshakeHandler( Host connectedHost )
    {
        super( connectedHost );
        hostMgr = HostManager.getInstance();
        hostContainer = hostMgr.getNetworkHostsContainer();
    }

    protected HTTPHeaderGroup createDefaultHandshakeHeaders()
    {
        // create hash map based on common headers
        HTTPHeaderGroup openHeaders = super.createDefaultHandshakeHeaders();

        // add ultrapeer headers...
        openHeaders.addHeader( new HTTPHeader(
            GnutellaHeaderNames.X_ULTRAPEER, "true" ) );
        openHeaders.addHeader( new HTTPHeader(
            GnutellaHeaderNames.X_QUERY_ROUTING, "0.1" ) );
        openHeaders.addHeader( new HTTPHeader(
            GnutellaHeaderNames.X_UP_QUERY_ROUTING, "0.1" ) );
        openHeaders.addHeader( new HTTPHeader(
            GnutellaHeaderNames.X_DYNAMIC_QUERY, "0.1") );
        openHeaders.addHeader( new HTTPHeader(
            GnutellaHeaderNames.X_DEGREE,
            String.valueOf( ServiceManager.sCfg.up2upConnections ) ) );
        openHeaders.addHeader( new HTTPHeader(
            GnutellaHeaderNames.X_MAX_TTL,
            String.valueOf( Cfg.DEFAULT_DYNAMIC_QUERY_MAX_TTL ) ) );

        return openHeaders;
    }

    public HandshakeStatus createHandshakeResponse( HandshakeStatus hostResponse,
       boolean isOutgoing )
    {
        if ( isOutgoing )
        {
            return createOutgoingResponse( hostResponse );
        }
        else
        {
            return createIncomingResponse( hostResponse );
        }
    }

    private HandshakeStatus createIncomingResponse(HandshakeStatus hostResponse)
    {
        HTTPHeaderGroup headers = hostResponse.getResponseHeaders();
        
        boolean isCrawler = isCrawlerConnection( headers );
        if ( isCrawler )
        {
            return createCrawlerHandshakeStatus();
        }
        
        // check ultrapeer header
        HTTPHeader upHeader = headers.getHeader( GnutellaHeaderNames.X_ULTRAPEER );
        
        if ( !isConnectionAccepted( upHeader ) )
        {
            return new HandshakeStatus( STATUS_CODE_REJECTED,
                STATUS_MESSAGE_BUSY, createRejectIncomingHeaders() );
        }
        HTTPHeaderGroup myHeaders = createDefaultHandshakeHeaders();
    
        // add ultrapeer needed header for leaf guidance
        if ( upHeader != null && Boolean.valueOf( upHeader.getValue() ).booleanValue() )
        {
            boolean isUltrapeerNeeded = hostContainer.
                hasUltrapeerSlotsAvailable();
            String isUltrapeedNeededStr = isUltrapeerNeeded ? "true" :
                "false";
            myHeaders.addHeader( new HTTPHeader(
                GnutellaHeaderNames.X_ULTRAPEER_NEEDED, isUltrapeedNeededStr ) );
        }
        
        // support for deflate... if accepted..
        if ( hostResponse.isDeflateAccepted() )
        {
            myHeaders.addHeader( new HTTPHeader(
                HTTPHeaderNames.CONTENT_ENCODING, "deflate" ) );
        }
        
        return new HandshakeStatus( STATUS_CODE_OK, STATUS_MESSAGE_OK,
            myHeaders );
    }

    private HandshakeStatus createOutgoingResponse(
        HandshakeStatus hostResponse )
    {
        HTTPHeaderGroup headers = hostResponse.getResponseHeaders();
        
        // check ultrapeer header
        HTTPHeader upHeader = headers.getHeader( GnutellaHeaderNames.X_ULTRAPEER );
        
        // can we accept the connection??
        if ( !isConnectionAccepted( upHeader ) )
        {
            return new HandshakeStatus( STATUS_CODE_REJECTED,
                STATUS_MESSAGE_BUSY, createRejectOutgoingHeaders() );
        }
        
        HTTPHeaderGroup myHeaders = new HTTPHeaderGroup(
            HTTPHeaderGroup.COMMON_HANDSHAKE_GROUP );
        
        HTTPHeader upNeededHeader = headers.getHeader(
            GnutellaHeaderNames.X_ULTRAPEER_NEEDED );
        // if no ultrapeer is needed and we are able to become a leaf node
        // and we are not talking to a bearshare, since bearshare will not
        // accept us as a leaf.
        if ( upNeededHeader != null && !upNeededHeader.booleanValue() &&
            !isBearshare( headers ) && hostMgr.isAbleToBecomeLeafNode() )
        {
            // create new HTTPHeaderGroup since we used empty headers earlier.
            myHeaders = new HTTPHeaderGroup( false );
            myHeaders.addHeader( new HTTPHeader(
               GnutellaHeaderNames.X_ULTRAPEER, "false" ) );
        }
        
        // support for deflate... if accepted..
        if ( hostResponse.isDeflateAccepted() )
        {
            myHeaders.addHeader( new HTTPHeader(
                HTTPHeaderNames.CONTENT_ENCODING, "deflate" ) );
        }
        
        return new HandshakeStatus( STATUS_CODE_OK, STATUS_MESSAGE_OK,
            myHeaders );
    }

    private boolean isConnectionAccepted( HTTPHeader upHeader )
    {
        // this is a peer connection
        if (upHeader == null)
        {
			if ( hostContainer.hasPeerSlotsAvailable() )
			{
				return true;
			}
			else
			{
				return false;
			}
        }

        // this is an Ultrapeer connection
        // we accept it if we have ultrapeer slots or leaf slots for ultrapeers
        // (for leaf guidance) unfortunately we dont know if leaf guidance is accepted
        // though..
        if ( Boolean.valueOf( upHeader.getValue() ).booleanValue() )
        {
            if ( hostContainer.hasUltrapeerSlotsAvailable() ||
                 hostContainer.hasLeafSlotForUltrapeerAvailable() )
            {
                return true;
            }
        }
        // this is a Leaf connection
        else if ( hostContainer.hasLeafSlotsAvailable() ) // upHeader == false
        {
            return true;
        }

        return false;
    }

    private boolean isBearshare( HTTPHeaderGroup headers )
    {
        HTTPHeader header = headers.getHeader( HTTPHeaderNames.USER_AGENT );
        if ( header != null && header.getValue().startsWith( "BearShare" ) )
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}