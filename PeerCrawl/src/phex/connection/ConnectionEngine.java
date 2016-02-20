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
 *  $Id: ConnectionEngine.java,v 1.65 2005/11/20 23:05:25 gregork Exp $
 */
package phex.connection;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.StringTokenizer;

import crawler.Peer;

import phex.common.*;
import phex.common.address.*;
import phex.common.format.NumberFormatUtils;
import phex.connection.handshake.HandshakeHandler;
import phex.connection.handshake.HandshakeStatus;
import phex.host.*;
import phex.http.*;
import phex.msg.*;
import phex.msg.vendor.*;
import phex.net.OnlineObserver;
import phex.net.connection.Connection;
import phex.net.connection.ConnectionFactory;
import phex.net.presentation.PresentationManager;
import phex.query.DynamicQueryConstants;
import phex.query.QueryHistoryMonitor;
import phex.query.QueryManager;
import phex.security.PhexSecurityManager;
import phex.share.ShareFile;
import phex.share.ShareManager;
import phex.share.SharedFilesService;
import phex.statistic.MessageCountStatistic;
import phex.upload.PushWorker;
import phex.utils.*;


/**
 * <p>A worker that handles the communication between this and another gnutella
 * node.</p>
 *
 * <p>The remote node is represented as a Host object. Depending on whether the
 * host is in incoming or outgoing mode, this will perform the relevant
 * handshake negotiations. If this was an
 * outgoing connection, or during negotiations it becomes clear that the Host
 * wishes to partake in a Gnutella network, then enter a message handling loop
 * to forward all messages as necessary. This will usualy result in a message
 * being read, some bookkeeping to keep track of the message, discarding bad
 * messages and finally queuing a request with the Message Manager to pass on any
 * messages that must be generated in response.</p>
 */
public class ConnectionEngine implements ConnectionConstants
{
    private final HostManager hostMgr;
    private final ShareManager shareMgr;
    private final QueryHistoryMonitor queryHistory;
    private final SharedFilesService sharedFilesService;
    private final MsgManager messageMgr;
    private final NetworkManager networkMgr;
    private final PhexSecurityManager securityManager;

    /**
     * pre-allocated buffer for repeated uses.
     */
    private byte[] headerBuffer;

    private final Host connectedHost;
    private Connection connection; 
    private HTTPHeaderGroup headersRead;
    private HTTPHeaderGroup headersSend;

    public ConnectionEngine( Host connectedHost )
    {
        this.connectedHost = connectedHost;
        connection = connectedHost.getConnection();
        shareMgr = ShareManager.getInstance();
        sharedFilesService = ShareManager.getInstance().getSharedFilesService();
        queryHistory = QueryManager.getInstance().getQueryHistoryMonitor();
        messageMgr = MsgManager.getInstance();
        hostMgr = HostManager.getInstance();
        networkMgr = NetworkManager.getInstance();
        securityManager = PhexSecurityManager.getInstance();
    }

    public void processIncomingData()
        throws IOException
    {
        headerBuffer = new byte[ MsgHeader.DATA_LENGTH ];
        try
        {
            while ( true )
            {
                MsgHeader header = readHeader();
                byte[] body = MessageProcessor.readMessageBody( connection,
                    header.getDataLength() );

                connectedHost.incReceivedCount();

                int ttl = header.getTTL();
                int hops = header.getHopsTaken();
                // verify valid ttl and hops data
                if ( ttl < 0 || hops < 0 )
                {
                    dropMessage( header, body, "TTL or hops below 0" );
                    continue;
                }
                // if message traveled too far already... drop it.
                if ( hops > ServiceManager.sCfg.maxNetworkTTL )
                {
                    dropMessage( header, body, "Hops larger then maxNetworkTTL" );
                    continue;
                }
                // limit TTL if too high!
                if ( ttl >= ServiceManager.sCfg.maxNetworkTTL )
                {
                    header.setTTL( (byte)(ServiceManager.sCfg.maxNetworkTTL - hops) );
                }

                Message message;
                try
                {
                    message = MessageProcessor.createMessageFromBody(
                        header, body );
                    if ( message == null )
                    { // unknown message type...
                        dropMessage( header, body, "Unknown message type" );
                        continue;
                    }
                }
                catch ( InvalidMessageException exp )
                {
                    dropMessage( header, body,
                        "Invalid message: " + exp.getMessage() );
                    NLogger.warn(NLoggerNames.IncomingMessages, exp, exp );
                    continue;
                }

                // count the hop and decrement ttl...
                header.countHop();

                //Logger.logMessage( Logger.FINEST, Logger.NETWORK,
                //    "Received Header function: " + header.getPayload() );

                switch ( header.getPayload() )
                {
                    case MsgHeader.PING_PAYLOAD:
                        handlePing( (PingMsg)message );
                        break;

                    case MsgHeader.PONG_PAYLOAD:
                        handlePong( (PongMsg)message );
                        break;

                    case MsgHeader.PUSH_PAYLOAD:
                        handlePushRequest( (PushRequestMsg) message );
                        break;

                    case MsgHeader.QUERY_PAYLOAD:
                        handleQuery( (QueryMsg) message );
                        break;

                    case MsgHeader.QUERY_HIT_PAYLOAD:
                        handleQueryResponse( (QueryResponseMsg) message );
                        break;
                    case MsgHeader.ROUTE_TABLE_UPDATE_PAYLOAD:
                        handleRouteTableUpdate( (RouteTableUpdateMsg) message );
                        break;
                    case MsgHeader.VENDOR_MESSAGE_PAYLOAD:
                    case MsgHeader.STANDARD_VENDOR_MESSAGE_PAYLOAD:
                        handleVendorMessage( (VendorMsg) message );
                        break;
                }

            }
        }
        catch ( IOException exp )
        {
            Logger.logMessage( Logger.FINEST, Logger.NETWORK, exp );
            if ( connectedHost.isConnected() )
            {
                connectedHost.setStatus(HostConstants.STATUS_HOST_ERROR, exp.getMessage());
                hostMgr.disconnectHost( connectedHost );
            }
            throw exp;
        }
        catch ( Exception exp )
        {
            Logger.logMessage( Logger.WARNING, Logger.NETWORK, exp );
            if (connectedHost.isConnected() )
            {
                connectedHost.setStatus(HostConstants.STATUS_HOST_ERROR, exp.getMessage());
                hostMgr.disconnectHost( connectedHost );
            }
            throw new IOException( "Exception occured: " + exp.getMessage() );
        }
    }

    private void handlePing( PingMsg pingMsg )
    {
        if ( NLogger.isDebugEnabled( NLoggerNames.IncomingMessages ) )
            NLogger.debug( NLoggerNames.IncomingMessages, "Received Ping: "
            + pingMsg.getDebugString() + " - " + pingMsg.getHeader().getDebugString());

        // count ping statistic
        MessageCountStatistic.pingMsgInCounter.increment( 1 );

        MsgHeader header = pingMsg.getHeader();
        // See if I have seen this Ping before.  Drop msg if duplicate.
        if ( !messageMgr.checkAndAddToPingRoutingTable( header.getMsgID(),
            connectedHost ) )
        {
            dropMessage( pingMsg, "Dropping already seen ping" );
            return;
        }

        if ( pingMsg.getHeader().getTTL() > 0 )
        {
            messageMgr.forwardPing( pingMsg, connectedHost );
        }
        respondToPing( pingMsg );
    }

    private void respondToPing( PingMsg pingMsg )
    {
        MsgHeader header = pingMsg.getHeader();
        // to reduce the incomming connection attemps of other clients
        // only response to ping a when we have free incomming slots or this
        // ping has a original TTL ( current TTL + hops ) of 2.
        byte ttl = header.getTTL();
        byte hops = header.getHopsTaken();
        if ( ( ttl + hops > 2 ) && !hostMgr.areIncommingSlotsAdvertised() )
        {
            return;
        }

        // For crawler pings (hops==1, ttl=1) we have a special treatment...
        // We reply with all our leaf connections... in case we have them as a
        // ultrapeer...
        if ( hops == 1 && ttl == 1)
        {// crawler ping
            // respond with leaf nodes pongs, already "hoped" one step. (ttl=1,hops=1)
            Host[] leafs = hostMgr.getNetworkHostsContainer().getLeafConnections();
            for ( int i = 0; i < leafs.length; i++ )
            {
                DestAddress ha = leafs[i].getHostAddress();
                PongMsg pong = PongMsg.createOtherLeafsOutgoingPong( header.getMsgID(),
                    (byte)1, (byte)1, ha );
                connectedHost.queueMessageToSend( pong );                
            }
        }

        // send back my own pong
        byte newTTL = hops++;
        if ( ( hops + ttl ) <= 2)
        {
            newTTL = 1;
        }
        
        // Get my host:port for InitResponse.
        PongMsg pong = PongMsg.createMyOutgoingPong( header.getMsgID(),
            (byte)newTTL );
        connectedHost.queueMessageToSend( pong );
    }

    /**
     * TODO4 track horizon statistics (hosts, files, file size)
     */
    private void handlePong( PongMsg pongMsg )
    {
        if ( NLogger.isDebugEnabled( NLoggerNames.IncomingMessages ) )
            NLogger.debug( NLoggerNames.IncomingMessages, "Received Pong: "
            + pongMsg.getDebugString() + " - " + pongMsg.getHeader().getDebugString());

        // count pong statistic
        MessageCountStatistic.pongMsgInCounter.increment( 1 );
        HorizonTracker.getInstance().trackPong( pongMsg );

        MsgHeader header = pongMsg.getHeader();

        byte[] pongIP = pongMsg.getIP();
        byte access = securityManager.controlHostIPAccess( pongIP );
        if ( access == PhexSecurityManager.ACCESS_STRONGLY_DENIED )
        {
            // drop message
            dropMessage( pongMsg, "IP access strongly denied." );
            return;
        }

        // add address to host catcher...
        if ( access == PhexSecurityManager.ACCESS_GRANTED )
        {
            hostMgr.getCaughtHostsContainer().addCaughtHost( pongMsg );
        }

        // this port is unsinged and always valid.
        int pongPort = pongMsg.getPort();
        byte hopsTaken = pongMsg.getHeader().getHopsTaken();
        
        // check if this is the response to my Ping message
        if ( hopsTaken == 1 )
        {
            DestAddress connectedAddress = connectedHost.getHostAddress();
            byte[] connectedIP = connectedAddress.getIpAddress().getHostIP();
            if ( Arrays.equals( connectedIP, pongIP ) )
            {
                connectedHost.setFileCount( pongMsg.getFileCount() );
                connectedHost.setTotalFileSize( pongMsg.getFileSizeInKB() );
                // I guess that a hops == 1 with equal ip address is a pong from my
                // direct neighbor, therefore I also update the obviously wrong port.
                int connectedPort = connectedAddress.getPort();
                if ( connectedPort != pongPort )
                {
                    connectedAddress.setPort( pongPort );
                }
            }
        }
        
        
        // Did I forward that Pong GUID before?
        Host returnHost = messageMgr.getPingRouting( header.getMsgID() );
        if ( returnHost == null || returnHost == Host.LOCAL_HOST )
        { // pong was for me or timed out.
            return;
        }

        // Ok, I did forward the Init msg on behalf of returnHost.
        // The InitResponse is for returnHost.  Better route it back.
        if ( pongMsg.getHeader().getTTL() > 0 )
        {
            returnHost.queueMessageToSend( pongMsg );
        }
    }


    private void handleQuery( QueryMsg msg )
    {
//    	System.out.println("handle query in connectionengine called");
        if ( NLogger.isDebugEnabled( NLoggerNames.IncomingMessages ) )
            NLogger.debug( NLoggerNames.IncomingMessages, "Received Query: "
            + msg.toString() + " - " + msg.getHeader().getDebugString());
        
        // count query statistic
        MessageCountStatistic.queryMsgInCounter.increment( 1 );

        MsgHeader header = msg.getHeader();

        // See if I have seen this Query before.  Drop msg if duplicate.
        // This drop is done even though this could be an extension of a 
        // probe query. Only Limewire is doing this extension of a probe
        // query currently and as stated by themselfs the efficency of it
        // is doubtfull. 
        if ( !messageMgr.checkAndAddToQueryRoutingTable( header.getMsgID(),
            connectedHost ) )
        {
            dropMessage( msg, "Dropping already seen query" );
            return;
        }
        
        // a leaf is not supposed to forward me queries not comming from itself.
        if ( connectedHost.isUltrapeerLeafConnection() && header.getHopsTaken() > 2 )
        {
            dropMessage( msg, "Dropping Query from Leaf with hops > 2." );
        }

        // logging a msg can be very expensive!
        //mRemoteHost.log( Logger.FINEST, "Received Msg: " + msg + " Hex: " +
        //    HexConverter.toHexString( body ) + " Data: " + new String( body) );

        // Add to the net search history.
        queryHistory.addSearchQuery( msg );

        // TTL > 0 checks for querys depends on routing
        messageMgr.forwardQuery( msg, connectedHost );
        
        //////////////////////
        String searchStr = msg.getSearchString();
        boolean queryHit = Peer.getInstance().processQuery(searchStr);
        
        /////////////////////////
        
        // Search the sharefile database and get groups of sharefiles.
//        ShareFile[] resultFiles = shareMgr.handleQuery( msg );
//        if ( resultFiles.length == 0)
        if(!queryHit)
        {
//        	System.out.println("url domain not in range. message dropped");
            return;
        }
//        replyToQuery( header, resultFiles );
    }

/*    
    private void handleQuery( QueryMsg msg )
    {
        if ( NLogger.isDebugEnabled( NLoggerNames.IncomingMessages ) )
            NLogger.debug( NLoggerNames.IncomingMessages, "Received Query: "
            + msg.toString() + " - " + msg.getHeader().getDebugString());
        
        // count query statistic
        MessageCountStatistic.queryMsgInCounter.increment( 1 );

        MsgHeader header = msg.getHeader();

        // See if I have seen this Query before.  Drop msg if duplicate.
        // This drop is done even though this could be an extension of a 
        // probe query. Only Limewire is doing this extension of a probe
        // query currently and as stated by themselfs the efficency of it
        // is doubtfull. 
        if ( !messageMgr.checkAndAddToQueryRoutingTable( header.getMsgID(),
            connectedHost ) )
        {
            dropMessage( msg, "Dropping already seen query" );
            return;
        }
        
        // a leaf is not supposed to forward me queries not comming from iteself.
        if ( connectedHost.isUltrapeerLeafConnection() && header.getHopsTaken() > 2 )
        {
            dropMessage( msg, "Dropping Query from Leaf with hops > 2." );
        }

        // logging a msg can be very expensive!
        //mRemoteHost.log( Logger.FINEST, "Received Msg: " + msg + " Hex: " +
        //    HexConverter.toHexString( body ) + " Data: " + new String( body) );

        // Add to the net search history.
        queryHistory.addSearchQuery( msg );

        // TTL > 0 checks for querys depends on routing
        messageMgr.forwardQuery( msg, connectedHost );
        
        // Search the sharefile database and get groups of sharefiles.
        ShareFile[] resultFiles = shareMgr.handleQuery( msg );
        if ( resultFiles.length == 0)
        {
            return;
        }
        replyToQuery( header, resultFiles );
    }
*/
    private void replyToQuery( MsgHeader header, ShareFile[] resultFiles )
    {
        // Construct QueryResponse msg.  Copy the original Init's GUID.
        // TTL expansion on query hits doesn't matter very much so it doesn't
        // hurt us to give query hits a TTL boost.
        // Bearshare sets QueryHit TTL to 10
        // gtk-gnutella sets QueryHit TTL to (hops + 5)
        MsgHeader newHeader = new MsgHeader( header.getMsgID(),
            MsgHeader.QUERY_HIT_PAYLOAD,
            // Will take as many hops to get back.
            // hops + 1 decided in gdf 2002-12-04
            (byte)(header.getHopsTaken() + 1),
            //(byte)(Math.min( 10, header.getHopsTaken() + 5 ) ),
            (byte)0, 0 );

        int resultCount = resultFiles.length;
        if ( resultCount > 255 )
        {
            resultCount = 255;
        }

        ShareFile sfile = null;
        QueryResponseRecord[] records = new QueryResponseRecord[ resultCount ];
        QueryResponseRecord record;
        for (int i = 0; i < resultCount; i++)
        {
            sfile = resultFiles[ i ];
            record = new QueryResponseRecord( sfile.getFileIndex(), sfile.getURN(),
                (int)sfile.getFileSize(), sfile.getFileName() );
            records[ i ] = record;
        }

        DestAddress hostAddress = networkMgr.getLocalAddress();
        QueryResponseMsg response = new QueryResponseMsg(
            newHeader, ServiceManager.sCfg.mProgramClientID, hostAddress,
            Math.round( ServiceManager.sCfg.mUploadMaxBandwidth / NumberFormatUtils.ONE_KB * 8),
            records );

        connectedHost.queueMessageToSend( response );
    }


    private void handleQueryResponse( QueryResponseMsg queryResponseMsg )
    {
        // Logging is expensive...
//        if ( Logger.isLevelLogged( Logger.FINEST ) )
//        {
//            Logger.logMessage( Logger.FINEST, Logger.NETWORK,
//                connectedHost, "Received QueryResponse: " + queryResponseMsg + " - " +
//                queryResponseMsg.toDebugString() );
//        }
        
        // count query hit statistic
        MessageCountStatistic.queryHitMsgInCounter.increment( 1 );

        DestAddress queryAddress = queryResponseMsg.getDestAddress();
        byte access = securityManager.controlHostAddressAccess( queryAddress );
        if ( access == PhexSecurityManager.ACCESS_STRONGLY_DENIED )
        {
            // drop message
            dropMessage( queryResponseMsg, "IP access strongly denied." );
            return;
        }

        if ( access == PhexSecurityManager.ACCESS_GRANTED )
        {
            messageMgr.processQueryResponse( connectedHost, queryResponseMsg );
        }
        messageMgr.addToPushRoutingTable( queryResponseMsg.getRemoteClientID(),
            connectedHost );
        
        MsgHeader responseHeader = queryResponseMsg.getHeader();
        
        // check if I forwarded the Query with the same message id as this QueryResponse. 
        QueryGUIDRoutingPair routingPair = messageMgr.getQueryRouting(
            responseHeader.getMsgID(), queryResponseMsg.getRecordCount() );
        if ( routingPair == null )
        {
            return;
        }
        Host returnHost = routingPair.getHost();
        
        // This QueryResponse needs to be routed to returnHost, I forwarded the query
        // on behalf of returnHost. The message is routed back to returnHost
        // if there is enough ttl left and I'm not the return host
        // and I have not yet routed enough results back.
        if ( responseHeader.getTTL() > 0 && returnHost != Host.LOCAL_HOST &&
            routingPair.getRoutedResultCount() < DynamicQueryConstants.DESIRED_ULTRAPEER_RESULTS )
        {
            returnHost.queueMessageToSend( queryResponseMsg );
        }
    }

    private void handleRouteTableUpdate( RouteTableUpdateMsg message )
    {
        // no specific stat so count to total
        MessageCountStatistic.totalInMsgCounter.increment( 1 );
		if ( !(connectedHost.isQueryRoutingSupported() ||
			connectedHost.isUPQueryRoutingSupported()) )
		{
			dropMessage( message, "QRP not supported from host.");
			return;
		}

        QueryRoutingTable qrTable = connectedHost.getLastReceivedRoutingTable();
        if ( qrTable == null )
        {
            // create new table... TODO3 maybe makes not much sense because we might
            // recreate table. maybe there is a way to initialise the qrt lazy
            qrTable = new QueryRoutingTable();
            connectedHost.setLastReceivedRoutingTable( qrTable );
        }
        try
        {
            qrTable.updateRouteTable( message );
            if ( connectedHost.isUltrapeerLeafConnection() )
            {// in case this is a leaf connection, we need to update our
             // local query routing table. This needs to be done since
             // have our leaves QRT aggregated our QRT and are checking
             // during a query against our QRT if leaves might have a hit.
                messageMgr.updateLocalQueryRoutingTable();
            }
        }
        catch ( InvalidMessageException exp )
        {// drop message
            dropMessage( message, "Invalid QRT update message." );
        }
    }


    private void handlePushRequest( PushRequestMsg msg )
    {
        // count push statistic
        MessageCountStatistic.pushMsgInCounter.increment( 1 );

        // logging a msg can be very expensive! the toString() calls are bad
        //mRemoteHost.log( Logger.FINEST, "Received Msg: " + msg);

        byte access = securityManager.controlHostAddressAccess(
            msg.getRequestAddress() );
        if ( access == PhexSecurityManager.ACCESS_STRONGLY_DENIED )
        {
            // drop message
            dropMessage( msg, "IP access strongly denied." );
            return;
        }

        if (ServiceManager.sCfg.mProgramClientID.equals(msg.getClientGUID()))
        {
            if ( access == PhexSecurityManager.ACCESS_GRANTED )
            {
                new PushWorker(msg);
            }
            return;
        }

        Host returnHost = messageMgr.getPushRouting(msg.getClientGUID());
        if (returnHost == null)
        {
//			mRemoteHost.log("Don't route the PushRequest since I didn't forward the QueryResponse msg.  " + msg);
            return;
        }

        // Ok, I did forward the QueryResponse msg on behalf of returnHost.
        // The PushRequest is for the returnHost.  Better route it back.
        if ( msg.getHeader().getTTL() > 0 )
        {
            returnHost.queueMessageToSend( msg );
        }
    }
    
    private void handleVendorMessage( VendorMsg vendorMsg )
    {
        if ( NLogger.isDebugEnabled( NLoggerNames.IncomingMessages ) )
            NLogger.debug( NLoggerNames.IncomingMessages, "Received VendorMsg: "
            + vendorMsg.toString() + " - " + vendorMsg.getHeader().getDebugString());
        
        if ( vendorMsg instanceof MessagesSupportedVMsg )
        {
            handleMessagesSupportedVMsg( (MessagesSupportedVMsg)vendorMsg );
        }
        else if ( vendorMsg instanceof TCPConnectBackVMsg )
        {
            handleTCPConnectBackVMsg( (TCPConnectBackVMsg)vendorMsg );
        }
        else if ( vendorMsg instanceof PushProxyRequestVMsg )
        {
            handlePushProxyRequestVMsg( (PushProxyRequestVMsg)vendorMsg );
        }
        else if ( vendorMsg instanceof PushProxyAcknowledgementVMsg )
        {
            handlePushProxyAcknowledgementVMsg( (PushProxyAcknowledgementVMsg)vendorMsg );
        }
        else if ( vendorMsg instanceof HopsFlowVMsg )
        {
            handleHopsFlowVMsg( (HopsFlowVMsg)vendorMsg );
        }
    }
    
    private void handleMessagesSupportedVMsg(MessagesSupportedVMsg msg)
    {
        connectedHost.setSupportedVMsgs( msg );
        // TODO2 maybe we should do some follow up actions like send connectback messages.
        
        // if push proxy is supported request it..
        boolean isFirewalled = networkMgr.hasConnectedIncoming();
        // if we are a leave or are firewalled and connected to a ultrapeer
        // and the connection supports push proxy.
        if ( ( connectedHost.isLeafUltrapeerConnection() ||
             ( isFirewalled && connectedHost.isUltrapeer() ) ) 
          && connectedHost.isPushProxySupported() )
        {
            PushProxyRequestVMsg pprmsg = new PushProxyRequestVMsg();
            // TODO2 remove this once Limewire support PPR v2
            if ( connectedHost.getVendor() != null &&
                 connectedHost.getVendor().indexOf( "LimeWire" ) != -1 )
            {
                pprmsg.setVersion( 1 );
            }
            connectedHost.queueMessageToSend( pprmsg );
        }        
    }

    /**
     * @param msg
     */
    private void handleTCPConnectBackVMsg(TCPConnectBackVMsg msg)
    {
        final int connectBackPort = msg.getPort();
        final DestAddress address = connectedHost.getHostAddress();
        
        Runnable connectBackRunner = new Runnable()
        {
            public void run()
            {
                Connection connection = null;
                try
                {
                    DestAddress connectBackAddress = new DefaultDestAddress( address.getHostName(),
                        connectBackPort );
                    connection = ConnectionFactory.createConnection(
                        connectBackAddress, 2000 );
                    connection.write( "\n\n".getBytes( ) );
                    connection.flush();
                }
                catch ( IOException exp )
                { // failed.. dont care..
                }
                catch ( Exception exp )
                {
                    NLogger.error( NLoggerNames.OUTGOING_MESSAGES, exp, exp);
                }
                finally
                {
                    if (connection != null)
                    {
                        connection.disconnect();
                    }
                }
            }
        };
        
        ThreadPool.getInstance().addJob( connectBackRunner, "TCPConnectBackJob");
    }
    
    private void handlePushProxyRequestVMsg( PushProxyRequestVMsg pprvmsg )
    {
        if ( !connectedHost.isUltrapeerLeafConnection() ) 
        {
            return;
        }
        DestAddress localAddress = networkMgr.getLocalAddress();
        // PP only works if we have a valid IP to use in the PPAck message.
        if( localAddress.getIpAddress() == null )
        {
            NLogger.warn( NLoggerNames.IncomingMessages, 
                "Local address has no IP to use for PPAck." );
            return;
        }
        GUID requestGUID = pprvmsg.getHeader().getMsgID();        
        PushProxyAcknowledgementVMsg ppavmsg = 
            new PushProxyAcknowledgementVMsg( localAddress,
            requestGUID );
        connectedHost.queueMessageToSend( ppavmsg );
        
        messageMgr.addToPushRoutingTable( requestGUID,
            connectedHost );            
    }
    
    private void handlePushProxyAcknowledgementVMsg( PushProxyAcknowledgementVMsg ppavmsg )
    {
        // the candidate is able to be a push proxy if the ack contains my guid.
        if ( ServiceManager.sCfg.mProgramClientID.equals( ppavmsg.getHeader().getMsgID() ) )
        {
            connectedHost.setPushProxyAddress( ppavmsg.getHostAddress() );
        }
    }
    
    private void handleHopsFlowVMsg( HopsFlowVMsg hopsFlowVMsg )
    {
        byte hopsFlowValue = hopsFlowVMsg.getHopsValue();
        connectedHost.setHopsFlowLimit(hopsFlowValue);
    }

    private void dropMessage( MsgHeader header, byte[] body, String reason )
    {
        NLogger.info( NLoggerNames.IncomingMessages_Dropped, 
            "Dropping message: " + reason + " from: " + connectedHost );
        if ( NLogger.isDebugEnabled( NLoggerNames.IncomingMessages_Dropped ) )
        {
            NLogger.debug( NLoggerNames.IncomingMessages_Dropped,
                "Header: " + header.getDebugString() + " Body: " +
                " (" + HexConverter.toHexString( body, 0,
                header.getDataLength() ) + ")." );
        }
        connectedHost.incDropCount();
        MessageCountStatistic.dropedMsgInCounter.increment( 1 );
    }

    private void dropMessage( Message msg, String reason )
    {
        if ( Logger.isLevelLogged( Logger.FINEST ) )
        {
            Logger.logMessage( Logger.FINEST, Logger.NETWORK, connectedHost, 
                "Dropping message: " + reason + " Header: [" + 
                msg.getHeader().getDebugString() + "] - Message: [" +
                msg.toDebugString() + "].");
        }
        connectedHost.incDropCount();
        MessageCountStatistic.dropedMsgInCounter.increment( 1 );
    }

    private MsgHeader readHeader()
        throws IOException
    {
        MsgHeader header = MessageProcessor.parseMessageHeader( connection,
            headerBuffer );
        if ( header == null )
        {
            throw new ConnectionClosedException("Connection closed by remote host");
        }

        int length = header.getDataLength();
        if ( length < 0 )
        {
            throw new IOException( "Negative body size. Disconnecting the remote host." );
        }
        else if ( length > ServiceManager.sCfg.maxMessageLength )
        {
            // Packet looks suspiciously too big.  Disconnect them.
            if ( Logger.isLevelLogged( Logger.WARNING ) )
            {
                // max 256KB when over 64KB max message length
                byte[] body = MessageProcessor.readMessageBody(
                    connection, 262144 );
                String hexBody = HexConverter.toHexString( body );
                Logger.logMessage( Logger.WARNING, Logger.NETWORK, connectedHost,
                    "Body too big. Header: " + header + "\nBody(256KB): " + hexBody );
            }

            throw new IOException("Packet too big. Disconnecting the remote host.");
        }

        header.setArrivalTime( System.currentTimeMillis() );
        header.setFromHost( connectedHost );

        return header;
    }

    //////////////////// Connection Intialization //////////////////////////////

    public void initializeOutgoingConnection( )
        throws IOException
    {
        connectedHost.setStatus( HostConstants.STATUS_HOST_CONNECTING );
        
        connection = ConnectionFactory.createConnection( 
            connectedHost.getHostAddress() );
        OnlineObserver onlineObserver = NetworkManager.getInstance().getOnlineObserver();
        onlineObserver.markSuccessfulConnection();
        // I am connected to the remote host at this point.
        connectedHost.setConnection( connection );

        // Negotiate 0.6 handshake
        try
        {
            initializeOutgoingWith06();
            configureConnectionType( headersSend, headersRead );
            postHandshakeConfiguration( headersSend, headersRead );
        }
        finally
        {
            if ( headersRead != null )
            {
                // use the connection header whether connection was ok or not
                handleXTryHeaders( headersRead );
                // give free to gc
                headersRead = null;
                headersSend = null;
            }
        }

        // Connection to remote gnutella host is completed at this point.
        connectedHost.setStatus( HostConstants.STATUS_HOST_CONNECTED );
        hostMgr.addConnectedHost( connectedHost );

        // queue first Ping msg to send.
        // add ping routing to local host to track my initial pings...
        messageMgr.pingHost( connectedHost, (byte)ServiceManager.sCfg.ttl );
        
        // after initial handshake ping send message supported VM.
        if ( connectedHost.isVendorMessageSupported( ) )
        {
            MessagesSupportedVMsg vMsg = MessagesSupportedVMsg.getMyMsgSupported();
            if ( NLogger.isDebugEnabled( NLoggerNames.OUTGOING_MESSAGES ) )
                NLogger.debug( NLoggerNames.OUTGOING_MESSAGES, "Queueing MessagesSupportedVMsg: "
                + vMsg.toString() + " - " + vMsg.getHeader().getDebugString() + " - Host: " + connectedHost.toString() );
            connectedHost.queueMessageToSend( vMsg );
        }
    }

    public void initializeIncomingConnection( String requestLine )
        throws IOException
    {
        // "GNUTELLA CONNECT/0.x";
        // "GNUTELLA PCONNECT name/0.x";
        int idx = requestLine.lastIndexOf( '/' ) + 1;
        String version = requestLine.substring( idx, requestLine.length() );

        try
        {
            if ( version.equals( PROTOCOL_04 ) )
            {
                initializeIncomingWith04();
            }
            else if ( is06orHigher( version ) )
            {
                initializeIncomingWith06();
            }
            configureConnectionType( headersSend, headersRead );
            postHandshakeConfiguration( headersSend, headersRead );
        }
        finally
        {
            if ( headersRead != null )
            {
                // use the connection header whether connection was ok or not
                handleXTryHeaders( headersRead );
                // give free to gc
                headersRead = null;
                headersSend = null;
            }
        }
        connectedHost.setStatus( HostConstants.STATUS_HOST_CONNECTED );
        hostMgr.addIncomingHost( connectedHost );
        
        // queue first Ping msg to send.
        // add ping routing to local host to track my initial pings...
        messageMgr.pingHost( connectedHost, (byte)ServiceManager.sCfg.ttl );
        
        // after initial handshake ping send message supported VM.
        if ( connectedHost.isVendorMessageSupported( ) )
        {
            MessagesSupportedVMsg vMsg = MessagesSupportedVMsg.getMyMsgSupported();
            if ( NLogger.isDebugEnabled( NLoggerNames.OUTGOING_MESSAGES ) )
                NLogger.debug( NLoggerNames.OUTGOING_MESSAGES, "Queueing MessagesSupportedVMsg: "
                + vMsg.toString() + " - " + vMsg.getHeader().getDebugString() + " - Host: " + connectedHost.toString() );
            connectedHost.queueMessageToSend( vMsg );
        }
    }

    private void initializeIncomingWith04()
        throws IOException
    {
        HandshakeHandler handshakeHandler = HandshakeHandler.createHandshakeHandler(
            connectedHost );
        HandshakeStatus response = handshakeHandler.createHandshakeResponse(
            new HandshakeStatus( STATUS_CODE_OK, STATUS_MESSAGE_OK ), false );

        if ( response.getStatusCode() != STATUS_CODE_OK )
        {
            throw new IOException( "Connection not accepted: " +
                response.getStatusCode() + " " + response.getStatusMessage() );
        }

        //checkPassword();
        sendStringToHost( GNUTELLA_OK_04 + "\n\n" );
        headersRead = HTTPHeaderGroup.EMPTY_HEADERGROUP;
        headersSend = HTTPHeaderGroup.EMPTY_HEADERGROUP;
    }

    private void initializeIncomingWith06()
        throws IOException
    {
        // read connect headers
        headersRead = HTTPProcessor.parseHTTPHeaders( connection );
        if ( Logger.isLevelLogged( Logger.FINER ) )
        {
            Logger.logMessage( Logger.FINER, Logger.NETWORK, connectedHost,
                "Connect headers: " + headersRead.buildHTTPHeaderString() );
        }
        configureRemoteHost( headersRead );

        // create appropriate handshake handler that takes care about headers
        // and logic...
        HandshakeHandler handshakeHandler = HandshakeHandler.createHandshakeHandler(
            connectedHost );
        HandshakeStatus myResponse = handshakeHandler.createHandshakeResponse(
            new HandshakeStatus( headersRead ), false );
        headersSend = myResponse.getResponseHeaders();

        // send answer to host...
        sendStringToHost( GNUTELLA_06 + " " + myResponse.getStatusCode() + " " +
            myResponse.getStatusMessage() + "\r\n" );
        String httpHeaderString = myResponse.getResponseHeaders().buildHTTPHeaderString();
        sendStringToHost( httpHeaderString );
        sendStringToHost( "\r\n" );

        if ( myResponse.getStatusCode() != STATUS_CODE_OK )
        {
            throw new IOException( "Connection not accepted: " +
                myResponse.getStatusCode() + " " + myResponse.getStatusMessage() );
        }

        HandshakeStatus inResponse = HandshakeStatus.parseHandshakeResponse(
            connection );
        if ( Logger.isLevelLogged( Logger.FINER ) )
        {
            Logger.logMessage( Logger.FINER, Logger.NETWORK, connectedHost,
                "Response Code: '" + inResponse.getStatusCode() + "'." );
            Logger.logMessage( Logger.FINER, Logger.NETWORK, connectedHost,
                "Response Message: '" + inResponse.getStatusMessage() + "'."  );
            Logger.logMessage( Logger.FINER, Logger.NETWORK, connectedHost,
                "Response Headers: " 
                + inResponse.getResponseHeaders().buildHTTPHeaderString() );
        }

        if ( inResponse.getStatusCode() != STATUS_CODE_OK )
        {
            throw new IOException( "Host rejected connection: " +
                inResponse.getStatusCode() + " " +
                inResponse.getStatusMessage() );
        }
        headersRead.replaceHeaders( inResponse.getResponseHeaders() );
    }

    private void initializeOutgoingWith06()
        throws IOException
    {
        connectedHost.setStatus( HostConstants.STATUS_HOST_CONNECTING,
            Localizer.getString( "Negotiate0_6Handshake") );

        // Send the first handshake greeting to the remote host.
        String greeting = networkMgr.getGnutellaNetwork().getNetworkGreeting();

        String requestLine = greeting + '/' + PROTOCOL_06 + "\r\n";
        StringBuffer requestBuffer = new StringBuffer( 100 );
        requestBuffer.append( requestLine );

        // create appropriate handshake handler that takes care about headers
        // and logic...
        HandshakeHandler handshakeHandler = HandshakeHandler.createHandshakeHandler(
            connectedHost );

        HTTPHeaderGroup handshakeHeaders =  
            handshakeHandler.createOutgoingHandshakeHeaders();
        requestBuffer.append( handshakeHeaders.buildHTTPHeaderString() );
        requestBuffer.append( "\r\n" );
        headersSend = handshakeHeaders;

        String requestStr = requestBuffer.toString();
        sendStringToHost( requestStr );

        HandshakeStatus handshakeResponse = HandshakeStatus.parseHandshakeResponse(
            connection );
        headersRead = handshakeResponse.getResponseHeaders();
        if ( Logger.isLevelLogged( Logger.FINER ) )
        {
            Logger.logMessage( Logger.FINER, Logger.NETWORK, connectedHost,
                "Response Code: '" + handshakeResponse.getStatusCode() + "'." );
            Logger.logMessage( Logger.FINER, Logger.NETWORK, connectedHost,
                "Response Message: '" + handshakeResponse.getStatusMessage() + "'."  );
            Logger.logMessage( Logger.FINER, Logger.NETWORK, connectedHost,
                "Response Headers: "
                + headersRead.buildHTTPHeaderString() );
        }

        if ( handshakeResponse.getStatusCode() != STATUS_CODE_OK )
        {
            if ( handshakeResponse.getStatusCode() == STATUS_CODE_REJECTED )
            {
                throw new ConnectionRejectedException(
                    handshakeResponse.getStatusCode() + " "
                    + handshakeResponse.getStatusMessage() );
            }
            throw new ConnectionRejectedException(
                "Gnutella 0.6 connection rejected. Status: " +
                handshakeResponse.getStatusCode() + " - " +
                handshakeResponse.getStatusMessage() );
        }

        configureRemoteHost( headersRead );

        HandshakeStatus myResponse = handshakeHandler.createHandshakeResponse(
            handshakeResponse, true );
        HTTPHeaderGroup myResponseHeaders = myResponse.getResponseHeaders();
        headersSend.replaceHeaders( myResponseHeaders );
        // send answer to host...
        sendStringToHost( GNUTELLA_06 + " " + myResponse.getStatusCode() + " " +
            myResponse.getStatusMessage() + "\r\n" );
        String httpHeaderString = myResponseHeaders.buildHTTPHeaderString();
        sendStringToHost( httpHeaderString );
        sendStringToHost( "\r\n" );

        if ( myResponse.getStatusCode() != STATUS_CODE_OK )
        {
            throw new IOException( "Connection not accepted: " +
                myResponse.getStatusCode() + " " + myResponse.getStatusMessage() );
        }
    }

    private void configureConnectionType( HTTPHeaderGroup myHeadersSend,
       HTTPHeaderGroup theirHeadersRead )
    {
        HTTPHeader myUPHeader = myHeadersSend.getHeader(
            GnutellaHeaderNames.X_ULTRAPEER );
        HTTPHeader theirUPHeader = theirHeadersRead.getHeader(
            GnutellaHeaderNames.X_ULTRAPEER );
        if ( myUPHeader == null || theirUPHeader == null )
        {
            connectedHost.setConnectionType( Host.CONNECTION_NORMAL );
        }
        else if ( myUPHeader.booleanValue() )
        {
            if ( theirUPHeader.booleanValue() )
            {
                connectedHost.setConnectionType( Host.CONNECTION_UP_UP );
            }
            else
            {
                connectedHost.setConnectionType( Host.CONNECTION_UP_LEAF );
            }
        }
        else // !myUPHeader.booleanValue()
        {
            if ( theirUPHeader.booleanValue() )
            {
                connectedHost.setConnectionType( Host.CONNECTION_LEAF_UP );
            }
            else
            {
                connectedHost.setConnectionType( Host.CONNECTION_NORMAL );
            }
        }
    }

    private void handleXTryHeaders( HTTPHeaderGroup headers )
    {
        // X-Try header is not used by most servents anymore... (2003-02-25)
        // we read still read it a while though...
        // http://groups.yahoo.com/group/the_gdf/message/14316
        HTTPHeader[] hostAddresses = headers.getHeaders(
            GnutellaHeaderNames.X_TRY );
        if ( hostAddresses != null )
        {
            handleXTryHosts( hostAddresses, true );
        }
        // for us ultrapeers have low priority other high.. since we cant connect to UP..
        hostAddresses = headers.getHeaders(
            GnutellaHeaderNames.X_TRY_ULTRAPEERS );
        if ( hostAddresses != null )
        {
            handleXTryHosts( hostAddresses, false );
        }
    }

    private void handleXTryHosts( HTTPHeader[] xtryHostAdresses, boolean isUltrapeerList )
    {
        short priority;
        if ( isUltrapeerList )
        {
            priority = CaughtHostsContainer.HIGH_PRIORITY;
        }
        else
        {
            priority = CaughtHostsContainer.NORMAL_PRIORITY;
        }
        CaughtHostsContainer hostContainer = hostMgr.getCaughtHostsContainer();

        for ( int i = 0; i < xtryHostAdresses.length; i++ )
        {
            StringTokenizer tokenizer = new StringTokenizer(
                xtryHostAdresses[i].getValue(), "," );
            while( tokenizer.hasMoreTokens() )
            {
                String hostAddressStr = tokenizer.nextToken().trim();
                try
                {
                    DestAddress address = PresentationManager.getInstance()
                        .createHostAddress( hostAddressStr, IpAddress.DEFAULT_PORT );
                    byte access = securityManager.controlHostAddressAccess( address );
                    switch ( access )
                    {
                        case PhexSecurityManager.ACCESS_DENIED:
                        case PhexSecurityManager.ACCESS_STRONGLY_DENIED:
                            // skip host address...
                            continue;
                    }
                    IpAddress ipAddress = address.getIpAddress();
                    if ( !isUltrapeerList && ipAddress != null && ipAddress.isSiteLocalIP() )
                    { // private IP have low priority except for ultrapeers.
                        priority = CaughtHostsContainer.LOW_PRIORITY;
                    }
                    hostContainer.addCaughtHost( address, priority );
                }
                catch ( MalformedDestAddressException exp )
                {
                }
            }
        }
    }
    
    /**
     * This method uses the header fields to set attributes of the remote host
     * accordingly.
     */
    private void configureRemoteHost( HTTPHeaderGroup headers )
    {
        HTTPHeader header = headers.getHeader( HTTPHeaderNames.USER_AGENT );
        if ( header != null )
        {
            connectedHost.setVendor( header.getValue() );
        }

        if ( connectedHost.isIncomming() )
        {
            header = headers.getHeader( GnutellaHeaderNames.LISTEN_IP );
            if ( header == null )
            {
                header = headers.getHeader( GnutellaHeaderNames.X_MY_ADDRESS );
            }
            if ( header != null )
            {
                DestAddress addi = connectedHost.getHostAddress();
                // parse port
                int port = AddressUtils.parsePort( header.getValue() );
                if ( port > 0 )
                {
                    addi.setPort( port );
                }
            }
        }
        
        header = headers.getHeader( GnutellaHeaderNames.REMOTE_IP );
        if ( header != null )
        {
            byte[] remoteIP = AddressUtils.parseIP( header.getValue() );
            if ( remoteIP != null )
            {
                IpAddress ip = new IpAddress( remoteIP );
                DestAddress address = PresentationManager.getInstance().createHostAddress(ip, -1);
                networkMgr.updateLocalAddress( address );                
            }
        }

        header = headers.getHeader( GnutellaHeaderNames.X_QUERY_ROUTING );
        if ( header != null )
        {
            try
            {
                float version = Float.parseFloat( header.getValue() );
                if ( version >= 0.1f )
                {
                    connectedHost.setQueryRoutingSupported( true );
                }
            }
            catch ( NumberFormatException e )
            { // no qr supported... don't care
            }
        }

        header = headers.getHeader( GnutellaHeaderNames.X_UP_QUERY_ROUTING );
        if ( header != null )
        {
            try
            {
                float version = Float.parseFloat( header.getValue() );
                if ( version >= 0.1f )
                {
                    connectedHost.setUPQueryRoutingSupported( true );
                }
            }
            catch ( NumberFormatException e )
            { // no qr supported... don't care
            }
        }
        
        header = headers.getHeader( GnutellaHeaderNames.X_DYNAMIC_QUERY );
        if ( header != null )
        {
            try
            {
                float version = header.floatValue();
                if ( version >= 0.1f )
                {
                    connectedHost.setDynamicQuerySupported( true );
                }
            }
            catch ( NumberFormatException e)
            {// no dynamiy query supported... don't care
            }
        }
        
        byte maxTTL = headers.getByteHeaderValue( GnutellaHeaderNames.X_MAX_TTL, 
            DynamicQueryConstants.DEFAULT_MAX_TTL );
        connectedHost.setMaxTTL( maxTTL );
        
        int degree = headers.getIntHeaderValue( GnutellaHeaderNames.X_DEGREE, 
            DynamicQueryConstants.NON_DYNAMIC_QUERY_DEGREE );
        connectedHost.setUltrapeerDegree( degree );
    }
    
    private void postHandshakeConfiguration( HTTPHeaderGroup myHeadersSend,
       HTTPHeaderGroup theirHeadersRead )
       throws IOException

    {
        if ( myHeadersSend.isHeaderValueContaining( HTTPHeaderNames.ACCEPT_ENCODING,
            "deflate" ) && theirHeadersRead.isHeaderValueContaining(
            HTTPHeaderNames.CONTENT_ENCODING, "deflate" ) )
        {
            connectedHost.activateInputInflation();
        }
        if ( theirHeadersRead.isHeaderValueContaining( HTTPHeaderNames.ACCEPT_ENCODING,
            "deflate" ) && myHeadersSend.isHeaderValueContaining(
            HTTPHeaderNames.CONTENT_ENCODING, "deflate" ) )
        {
            connectedHost.activateOutputDeflation();
        }
        
        
        HTTPHeader header = theirHeadersRead.getHeader( 
            GnutellaHeaderNames.VENDOR_MESSAGE );
        if ( header != null && !header.getValue().equals("") )
        {
            connectedHost.setVendorMessageSupported( true );
        }
    }

    /**
     * Checks if the connections is acceptable for the current Ultrapeer
     * connection state.
     */
    /*private void doUltrapeerConnectionCheck()
        throws IOException
    {
        if ( connectedHost.isLeafUltrapeerConnection() )
        {
            if ( !ServiceManager.sCfg.allowToBecomeLeaf )
            {
                sendStringToHost( GNUTELLA_06_503 + " I am not accepting Ultrapeers.\r\n\r\n" );
                throw new IOException( "Ultrapeers are not accepted" );
            }
            return;
        }

        // not a Ultrapeer...
        if ( !networkMgr.areNoneUPConnectionsAllowed() )
        {
            sendStringToHost( GNUTELLA_06_503 + " I accept only Ultrapeers.\r\n\r\n" );
            throw new IOException( "Only Ultrapeers accepted" );
        }

        NetworkHostsContainer netContainer =
            HostManager.getInstance().getNetworkHostsContainer();
        if ( netContainer.isShieldedLeafNode() )
        {
            sendStringToHost( GNUTELLA_06_503 + " I am a shielded leaf node.\r\n\r\n" );
            throw new IOException( "Shielded leaf node." );
        }
    }*/

    private boolean is06orHigher( String version )
    {
        int diff = VersionUtils.compare( version, PROTOCOL_06 );
        return ( diff >= 0 );
    }

    private void sendStringToHost( String str )
        throws IOException
    {
        Logger.logMessage( Logger.FINER, Logger.NETWORK, connectedHost,
            "Send: " + str );
        // TODO do we need to take care about encoding? This might always be
        // ISO8859-1
        byte[] bytes = str.getBytes();
        connection.write( bytes, 0, bytes.length );
        connection.flush();
    }
}
