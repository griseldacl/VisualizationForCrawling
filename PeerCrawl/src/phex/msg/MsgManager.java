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
package phex.msg;

import java.util.Iterator;
import java.util.TimerTask;

import phex.common.Environment;
import phex.common.Manager;
import phex.common.QueryRoutingTable;
import phex.common.address.DestAddress;
import phex.connection.NetworkManager;
import phex.host.Host;
import phex.host.HostManager;
import phex.host.NetworkHostsContainer;
import phex.msg.vendor.*;
import phex.query.DynamicQueryConstants;
import phex.query.QueryManager;
import phex.query.QueryResultMonitor;
import phex.upload.UploadManager;
import phex.utils.*;

/**
 * <p>Implementates the management of Gnutella messages through this node.</p>
 *
 * <p>This class will co-ordinate message sending, forwarding and query result
 * collection. In principle, it appears to be possible to instantiate muliple
 * instances. This could potentially be used to manage multiple Gnutella-style
 * networks independently of one another. In practice, I think some of the
 * configuration parameters may be stored in global static fields, so it may be
 * more tricky.</p>
 */
final public class MsgManager implements Manager
{
    private HostManager hostMgr;
    private NetworkHostsContainer hostsContainer;
    private QueryManager queryMgr;
    
    /**
     * Responsible to monitor query results for hits that we might like to use
     * for downloads or passive searching.
     */
    private QueryResultMonitor queryResultMonitor;
    
    /**
     * The last sent query routing table, used for dynamic query
     * for a fast check for matches. 
     */
    private QueryRoutingTable lastSentQueryRoutingTable;

    /**
     * Holds ping GUID routings to Host.
     */
    private GUIDRoutingTable pingRoutingTable;

    /**
     * Holds query GUID routings to Host.
     */
    private QueryGUIDRoutingTable queryRoutingTable;

    /**
     * Holds query reply GUID routings to Host.
     */
    private GUIDRoutingTable pushRoutingTable;

    private static MsgManager instance;

    /**
     * Create a new message manager.
     */
    private MsgManager()
    {
        // holds from 2-4 minutes of ping GUIDs
        pingRoutingTable = new GUIDRoutingTable( 2 * 60 * 1000 );
        // holds from 5-10 minutes of query GUIDs
        queryRoutingTable = new QueryGUIDRoutingTable( 5 * 60 * 1000 );
        // holds from 7-14 minutes of QueryReply GUIDs for push routes.
        pushRoutingTable = new GUIDRoutingTable( 7 * 60 * 1000 );
    }

    public static MsgManager getInstance()
    {
        if ( instance == null )
        {
            instance = new MsgManager();
        }
        return instance;
    }

    /**
     * This method is called in order to initialize the manager. This method
     * includes all tasks that must be done to intialize all the several manager.
     * Like instantiating the singleton instance of the manager. Inside
     * this method you can't rely on the availability of other managers.
     * @return true is initialization was successful, false otherwise.
     */
    public boolean initialize()
    {
        return true;
    }

    /**
     * This method is called in order to perform post initialization of the
     * manager. This method includes all tasks that must be done after initializing
     * all the several managers. Inside this method you can rely on the
     * availability of other managers.
     * @return true is initialization was successful, false otherwise.
     */
    public boolean onPostInitialization()
    {
        hostMgr = HostManager.getInstance();
        hostsContainer = hostMgr.getNetworkHostsContainer();
        queryMgr = QueryManager.getInstance();
		Environment.getInstance().scheduleTimerTask( 
			new QRPUpdateTimer(), QRPUpdateTimer.TIMER_PERIOD,
			QRPUpdateTimer.TIMER_PERIOD );
        Environment.getInstance().scheduleTimerTask( 
            new HopsFlowTimer(), HopsFlowTimer.TIMER_DELAY,
            HopsFlowTimer.TIMER_PERIOD );
        queryResultMonitor = new QueryResultMonitor();
        return true;
    }
    
    /**
     * This method is called after the complete application including GUI completed
     * its startup process. This notification must be used to activate runtime
     * processes that needs to be performed once the application has successfully
     * completed startup.
     */
    public void startupCompletedNotify()
    {
    }

    /**
     * This method is called in order to cleanly shutdown the manager. It
     * should contain all cleanup operations to ensure a nice shutdown of Phex.
     */
    public void shutdown(){}
    
    /**
     * @return
     */
    public QueryResultMonitor getQueryResultMonitor()
    {
        return queryResultMonitor;
    }
    
    /**
     * Returns the last sent query routing table that contains
     * my and my leafs entries.
     * @return the last sent query routing table.
     */
    public QueryRoutingTable getLastSentQueryRoutingTable()
    {
        return lastSentQueryRoutingTable;
    }

    /**
     * <p>Checks if a route for the GUID is already available. If not associates
     * the Host with the GUID.</p>
     *
     * @param clientID  the GUID to route.
     * @param sender  the Host sending information
     */
    public synchronized boolean checkAndAddToPingRoutingTable( GUID pingGUID,
        Host sender )
    {
        boolean state = pingRoutingTable.checkAndAddRouting( pingGUID, sender );
        return state;
    }

    /**
     * <p>Checks if a route for the GUID is already available. If not associates
     * the Host with the GUID.</p>
     *
     * @param clientID  the GUID to route.
     * @param sender  the Host sending information
     */
    public synchronized boolean checkAndAddToQueryRoutingTable( GUID queryGUID,
        Host sender )
    {
        boolean state = queryRoutingTable.checkAndAddRouting( queryGUID, sender );
        return state;
    }

    /**
     * <p>Associate a Host with the GUID for the servent serving a file.</p>
     *
     * @param clientID  the GUID of a servent publishing a file
     * @param sender  the Host sending information
     */
    public synchronized void addToPushRoutingTable( GUID clientID, Host sender )
    {
        pushRoutingTable.addRouting( clientID, sender );
    }

    public synchronized void removeHost( Host host )
    {
        queryMgr.removeHostQueries( host );
        pingRoutingTable.removeHost( host );
        queryRoutingTable.removeHost( host );
        pushRoutingTable.removeHost( host );
    }

    /**
     * Returns the push routing host for the given GUID or null
     * if no push routing is available or the host is not anymore
     * connected.
     */
    public synchronized Host getPushRouting( GUID clientID )
    {
        return pushRoutingTable.findRouting( clientID );
    }

    /**
     * Returns the ping routing host for the given GUID or null
     * if no push routing is available or the host is not anymore
     * connected.
     */
    public synchronized Host getPingRouting( GUID pingGUID )
    {
        return pingRoutingTable.findRouting( pingGUID );
    }

    /**
     * Returns the query routing pair with host for the given GUID or null
     * if no push routing is available or the host is not anymore
     * connected.
     * 
     * @param queryGUID the GUID of the query reply route to find.
     * @param resultCount the number of results routed together with the query reply of
     *        this query GUID.
     * @return the QueryGUIDRoutingPair that contains the host and routed result count to 
     * 		route the reply or null.
     */
    public synchronized QueryGUIDRoutingPair getQueryRouting( GUID queryGUID, int resultCount )
    {
        return queryRoutingTable.findRoutingForQuerys( queryGUID, resultCount );
    }

    /**
     * Process the query response after IP filtering is done.
     */
    public synchronized void processQueryResponse(Host remoteHost, QueryResponseMsg msg)
    {
        queryMgr.getSearchContainer().processQueryResponse( msg );
        queryMgr.getBackgroundSearchContainer().processQueryResponse( msg );
        
        // snoop download candidates and monitor passive searches.
        queryResultMonitor.processResponse( msg );
    }
    

    /**
     * <p>Called to forward a query to all connected neighbours. This is only
     * done under special conditions.<br>
     * When we are in Leaf mode we hold connections to Ultrapeers (we are there
     * leaf) and usual peers therefore we:<br>
     * - Never forward a query comming from a Ultrapper.
     * - Never forward a query to a ultrapeer.<br>
     * <br>
     * When we are in Ultrapeer mode we hold connections to other Ultrapeers,
     * Leafs and usual peers. The leaf connection usually forward us
     * there QueryRoutingTable, therefore:<br>
     * - Never forward a query that does not match a QRT entry.<br>
     * <br>
     * This strategy is used to separate the broadcast traffic of the peer
     * network from the Ultrapeer/Leaf network and is essential for a correct
     * Ultrapeer proposal support.</p>
     *
     * <p>This does not affect the TTL or hops fields of the message.</p>
     *
     * @param msg the IMsg to forward
     * @param fromHost the Host that originated this message
     */
    public void forwardQuery( QueryMsg query, Host fromHost )
    {
        boolean isShieldedLeaf = hostMgr.isShieldedLeafNode();
        // Never broadcast a message comming from a ultrapeer when in leaf mode!
        if ( isShieldedLeaf && fromHost != null
            && fromHost.isLeafUltrapeerConnection() )
        {
            return;
        }
        
        if ( fromHost.isUltrapeerLeafConnection() )
        {
            queryMgr.sendDynamicQuery( query,
                DynamicQueryConstants.DESIRED_LEAF_RESULTS );
        }
        else if ( !isShieldedLeaf )
        {
            // only forward to ultrapeers if I'm not a leaf.
            if ( query.getHeader().getTTL() > 0 )
            {
                forwardQueryToUltrapeers( query, fromHost );
            }

            // Only forward to leafs if I'm not a leaf itself.
            // Forward query to Leafs regardless of TTL
            // see section 2.4 Ultrapeers and Leaves Single Unit of
            // Gnutella Ultrapeer Query Routing v0.1
            forwardQueryToLeaves( query, fromHost );
        }

        // forward to usual peers.
        if ( query.getHeader().getTTL() > 0 )
        {
            Host[] hosts = hostsContainer.getPeerConnections();
            forwardQuery( query, fromHost, hosts );
        }
    }

    /**
     * Forward query to Leafs regardless of TTL
     * see section 2.4 Ultrapeers and Leaves Single Unit of
     * Gnutella Ultrapeer Query Routing v0.1
     * @param msg query to forward.
     * @param fromHost the host the query comes from and
     *        query is not forwarded to
     */
    public void forwardQueryToLeaves( QueryMsg msg, Host fromHost )
    {
        Host[] hosts = hostsContainer.getLeafConnections();
        forwardQuery(msg, fromHost, hosts);
    }

    /**
     * Forwards a query to the given hosts but never to the from Host.
     * @param msg the query to forward
     * @param fromHost the host the query came from.
     * @param hosts the hosts to forward to.
     */
    private void forwardQuery( QueryMsg msg, Host fromHost, Host[] hosts )
    {
        for ( int i = 0; i < hosts.length; i++ )
        {
            if ( hosts[i] == fromHost )
            {
                continue;
            }
            QueryRoutingTable qrt = hosts[i].getLastReceivedRoutingTable();
            if ( qrt != null && !qrt.containsQuery( msg ) )
            {
                continue;
            }
            hosts[i].queueMessageToSend( msg );
        }
    }

    /**
     * Forwards a query to the given hosts but never to the from Host.
     * @param msg the query to forward
     * @param fromHost the host the query came from.
     * @param hosts the hosts to forward to.
     */
    public void forwardQueryToUltrapeers( QueryMsg msg, Host fromHost )
    {
        Host[] ultrapeers = hostsContainer.getUltrapeerConnections();
        boolean lastHop = msg.getHeader().getTTL() == 1;
        for ( int i = 0; i < ultrapeers.length; i++ )
        {
            if ( ultrapeers[i] == fromHost )
            {
                continue;
            }
            // a query on last hop is forwarded to other Ultrapeers
            // with the use of a possibly available qrt.
            if ( lastHop && ultrapeers[i].isUPQueryRoutingSupported() )
            {
                QueryRoutingTable qrt = ultrapeers[i].
                    getLastReceivedRoutingTable();
                if ( qrt != null && !qrt.containsQuery( msg ) )
                {
                    continue;
                }
            }
            ultrapeers[i].queueMessageToSend( msg );
        }
    }


    /**
     * <p>Called to forward a Ping to all connected neighbours. This is only
     * done under special conditions.<br>
     * When we are in Leaf mode we hold connections to Ultrapeers (we are there
     * leaf) and usuall peers therefore we:<br>
     * - Never broadcast a message comming from a Ultrapper.
     * - Never broadcast a message to a ultrapeer.<br>
     * This strategy is used to separate the broadcast traffic of the peer
     * network from the Ultrapeer/Leaf network and is essential for a correct
     * Ultrapeer proposal support.</p>
     *
     * @param msg the MsgPing to forward
     * @param fromHost the Host that originated this message
     */
    public void forwardPing( PingMsg msg, Host fromHost )
    {
        boolean isShieldedLeaf = hostMgr.isShieldedLeafNode();
        // Never broadcast a message comming from a ultrapeer when in leaf mode!
        if ( isShieldedLeaf && fromHost != null
            && fromHost.isLeafUltrapeerConnection() )
        {
            return;
        }

        Host[] hosts;

        if ( !isShieldedLeaf )
        {   // only forward to ultrapeers if I'm not a leaf.
            hosts = hostsContainer.getUltrapeerConnections();
            forwardPing(msg, fromHost, hosts);

            // only forward to leafs if I'm not a leaf itself.
            hosts = hostsContainer.getLeafConnections();
            forwardPing(msg, fromHost, hosts);
        }

        // forward to usual peers.
        hosts = hostsContainer.getPeerConnections();
        forwardPing(msg, fromHost, hosts);
    }

    /**
     * Forwards a ping to the given hosts but never to the from Host.
     * @param msg the ping to forward
     * @param fromHost the host the ping came from.
     * @param hosts the hosts to forward to.
     */
    private void forwardPing( PingMsg msg, Host fromHost, Host[] hosts )
    {
        for ( int i = 0; i < hosts.length; i++ )
        {
            if ( hosts[i] == fromHost )
            {
                continue;
            }
            hosts[i].queueMessageToSend( msg );
        }
    }

    /**
     * Ping a host
     */
    public void pingHost( Host host )
    {
        pingHost( host, (byte)1 );
    }
    
    /**
     * Ping a host
     */
    public void pingHost( Host host, byte ttl )
    {
        // Send ping msg.
        PingMsg pingMsg = new PingMsg();
        pingMsg.getHeader().setTTL( ttl );
        checkAndAddToPingRoutingTable(
            pingMsg.getHeader().getMsgID(), Host.LOCAL_HOST );
        if ( NLogger.isDebugEnabled( NLoggerNames.OUTGOING_MESSAGES ) )
            NLogger.debug( NLoggerNames.OUTGOING_MESSAGES, "Queueing Ping: "
            + pingMsg.getDebugString() + " - " + pingMsg.getHeader().getDebugString() + " - Host: " + host.toString() );
        host.queueMessageToSend( pingMsg );
    }
    
    public void requestTCPConnectBack()
    {
        DestAddress localAddress = NetworkManager.getInstance().getLocalAddress();
        VendorMsg tcpConnectBack = new TCPConnectBackVMsg( localAddress.getPort() );
        Host[] hosts = hostsContainer.getUltrapeerConnections();
        int sentCount = 0;
        for ( int i = 0; sentCount <= 3 && i < hosts.length; i++ )
        {
            if ( hosts[i].isTCPConnectBackSupported() )
            {
                hosts[i].queueMessageToSend( tcpConnectBack );
                sentCount ++;
            }
        }
    }
    
    public void updateLocalQueryRoutingTable()
    {
        lastSentQueryRoutingTable = QueryRoutingTable.createLocalQueryRoutingTable();
    }
    
    private class QRPUpdateTimer extends TimerTask
    {
        private static final long TIMER_PERIOD = 1000 * 10;

    	public void run()
    	{
            try
            {
                sendQueryRoutingTable();
            }
            catch ( Throwable th )
            {
                NLogger.error( NLoggerNames.GLOBAL, th, th );
            }
    	}
    	
		/**
		 * Sends the query routing table to all network connections that haven't
		 * been updated for a while.
		 */
		private void sendQueryRoutingTable()
		{
			boolean isUltrapeer = hostMgr.isUltrapeer();
			// check if we are a shilded leaf node or a Ultrapeer.
			// Forwarding QRT is not wanted otherwise.
			if ( !( hostMgr.isShieldedLeafNode() || isUltrapeer ) )
			{
				return;
			}

			Host[] hosts = hostsContainer.getUltrapeerConnections();

			// lazy initialize if needed.
			QueryRoutingTable currentTable = null;
			QueryRoutingTable lastSentTable;
			for (int i = 0; i < hosts.length; i++)
			{
				// first check if we are a UP or leaf    supports QRP
				if ( isUltrapeer )
				{
					if ( !hosts[i].isUPQueryRoutingSupported() )
					{
						continue;
					}
				}
				else 
				{
					if ( !hosts[i].isQueryRoutingSupported() )
					{
						continue;
					}
				}
				
				if ( !hosts[i].isQRTableUpdateRequired() )
				{
					continue;
				}
				
				Logger.logMessage( Logger.FINER, Logger.NETWORK,
					"Updating QRTable for: " + hosts[i] );
				if ( currentTable == null )
				{// lazy initialize
					updateLocalQueryRoutingTable();
                    currentTable = lastSentQueryRoutingTable;
				}
				lastSentTable = hosts[i].getLastSentRoutingTable();

				Iterator msgIterator = QueryRoutingTable.buildRouteTableUpdateMsgIterator(
					currentTable, lastSentTable );
				RouteTableUpdateMsg msg;
				while ( msgIterator.hasNext() )
				{
					msg = (RouteTableUpdateMsg) msgIterator.next();
					hosts[i].queueMessageToSend( msg );
				}
				// when setting the last sent routing table the next routing
				// table update time is set.
				hosts[i].setLastSentRoutingTable( currentTable );
			}
		}
    }

    private class HopsFlowTimer extends TimerTask
    {
        private static final long TIMER_DELAY = 1000 * 60 * 2;
        private static final long TIMER_PERIOD = 1000 * 15;
        
        private boolean lastBusyState = false;
        
        public void run()
        {
            try
            {
                if ( !hostsContainer.isShieldedLeafNode() )
                {
                    return;
                }
                Host[] ultrapeers = hostsContainer.getUltrapeerConnections();
                boolean isHostBusy = UploadManager.getInstance().isHostBusy();
                
                byte hopsFlowLimit;
                if ( isHostBusy )
                {
                    hopsFlowLimit = 0;
                }
                else
                {
                    hopsFlowLimit = 5;
                }
                HopsFlowVMsg msg = new HopsFlowVMsg( hopsFlowLimit );
                long now = System.currentTimeMillis();
                for ( int i = 0; i < ultrapeers.length; i++ )
                {
                    if ( ultrapeers[i].isHopsFlowSupported() &&
                         ( isHostBusy != lastBusyState ||
                           ultrapeers[i].getConnectionUpTime( now ) < TIMER_PERIOD*1.1 ) )
                    {
                        ultrapeers[i].queueMessageToSend( msg );
                    }
                }
                lastBusyState = isHostBusy;
            }
            catch ( Throwable th )
            {
                NLogger.error(NLoggerNames.OUTGOING_MESSAGES, th, th);
            }
        }
    }

}