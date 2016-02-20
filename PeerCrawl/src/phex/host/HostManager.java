
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

package phex.host;

import java.io.IOException;
import java.util.TimerTask;

import phex.common.Environment;
import phex.common.Manager;
import phex.common.ServiceManager;
import phex.connection.ConnectionEngine;
import phex.connection.ConnectionObserver;
import phex.connection.NetworkManager;
import phex.event.NetworkHostsChangeListener;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

/**
 * Responsible for managing caught host and the network neighbourhood.
 */
final public class HostManager implements Manager
{
    private static final int MAX_PARALLEL_CONNECTION_TRIES = 20;
    private static HostManager instance;
    private NetworkManager networkMgr;

    private CaughtHostsContainer caughtHostsContainer;
    private NetworkHostsContainer networkHostsContainer;
    private FavoritesContainer favoritesContainer;
    private UltrapeerCapabilityChecker upChecker;

    private HostManager()
    {
        favoritesContainer = new FavoritesContainer();
        caughtHostsContainer = new CaughtHostsContainer();
    }

    public static HostManager getInstance()
    {
        if ( instance == null )
        {
            instance = new HostManager();
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
        networkMgr = NetworkManager.getInstance();

        networkHostsContainer = new NetworkHostsContainer();

        ConnectionObserver observer = new ConnectionObserver();
        observer.start();

        // Register the NetworkHostsContainer as a NetworkListener
        NetworkManager networkMgr = NetworkManager.getInstance();
        networkMgr.addNetworkListener( networkHostsContainer );

        upChecker = new UltrapeerCapabilityChecker();

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
        Environment.getInstance().scheduleTimerTask( 
            new HostCheckTimer(), HostCheckTimer.TIMER_PERIOD,
            HostCheckTimer.TIMER_PERIOD );
    }

    /**
     * This method is called in order to cleanly shutdown the manager. It
     * should contain all cleanup operations to ensure a nice shutdown of Phex.
     */
    public void shutdown()
    {
        try
        {
            caughtHostsContainer.saveHostsContainer();
            favoritesContainer.saveFavoriteHosts();
        }
        catch ( Exception exp )
        {
            NLogger.error( NLoggerNames.GLOBAL, exp, exp );
        }
    }

    public void acceptIncomingConnection( Host host, String requestLine )
        throws IOException
    {
        ConnectionEngine engine = new ConnectionEngine( host );
        engine.initializeIncomingConnection( requestLine );
        engine.processIncomingData();
    }
    
    /**
     * @return
     */
    public FavoritesContainer getFavoritesContainer()
    {
        return favoritesContainer;
    }

    public CaughtHostsContainer getCaughtHostsContainer()
    {
        return caughtHostsContainer;
    }

    /////////////// START NetworkHostsContainer wrapper methods ///////////////////////

    public NetworkHostsContainer getNetworkHostsContainer()
    {
        return networkHostsContainer;
    }

    public boolean isShieldedLeafNode()
    {
        return networkHostsContainer.isShieldedLeafNode();
    }

    /**
     * Returns true if this node is allowed to become a ultrapeer, false otherwise.
     * @return true if this node is allowed to become a ultrapeer, false otherwise.
     */
    public boolean isAbleToBecomeUltrapeer()
    {
        // when we already are a ultrapeer we must be able to become one..
        if ( isUltrapeer() )
        {
            return true;
        }
        return !isShieldedLeafNode() && ( ServiceManager.sCfg.allowToBecomeUP &&
            upChecker.isUltrapeerCapable() );
    }

    /**
     * Returns true if this node is currently a ultrapeer, false otherwise.
     * This node is currently a ultrapeer if it is forced to be a ultrapeer or
     * has leaf connections.
     * @return true if the node is currently a ultrapeer, false otherwise.
     */
    public boolean isUltrapeer()
    {
        return ( ServiceManager.sCfg.allowToBecomeUP &&
            ServiceManager.sCfg.forceToBeUltrapeer ) ||
            // if we have leaf connections we are a ultrapeer.
            networkHostsContainer.hasLeafConnections();
    }

    /**
     * Indicates if the peer advertises through pongs that it has incomming slots
     * available.
     * @return true if it will advertise incomming slots. False otherwise.
     */
    public boolean areIncommingSlotsAdvertised()
    {
        if ( networkHostsContainer.isShieldedLeafNode() )
        {   // when shielded leaf we dont like many incomming request therefore
            // we claim to not have any slots available...
            return false;
        }
        return networkHostsContainer.hasPeerSlotsAvailable() ||
            networkHostsContainer.hasUltrapeerSlotsAvailable() ||
            networkHostsContainer.hasLeafSlotsAvailable();
    }


    /**
     * The method checks if we are able to go into leaf state. This is
     * necessary to react accordingly to the "X-UltrapeerNeeded: false" header.
     *
     * @return true if we are able to switch to Leaf state, false otherwise.
     */
    public boolean isAbleToBecomeLeafNode()
    {
        if ( !ServiceManager.sCfg.allowToBecomeLeaf )
        {
            return false;
        }
        // we are not able to become a leaf if we are able to become a ultrapeer
        // this includes that we might already are a ultrapeer and we have any
        // leaf or ultrapeer connections.
        if ( isAbleToBecomeUltrapeer() &&
            ( networkHostsContainer.hasLeafConnections() ||
              networkHostsContainer.hasUltrapeerConnections() ) )
        {
            return false;
        }
        return true;
    }

    public void addConnectedHost( Host host )
    {
        networkHostsContainer.addConnectedHost( host );
    }

    public void disconnectHost( Host host )
    {
        networkHostsContainer.disconnectHost( host );
    }

    public void addIncomingHost( Host host )
    {
        networkHostsContainer.addIncomingHost( host );
    }

    public void removeAllNetworkHosts()
    {
        networkHostsContainer.removeAllNetworkHosts();
    }

    public void removeNetworkHosts( Host[] hosts )
    {
        networkHostsContainer.removeNetworkHosts( hosts );
    }

    public void removeNetworkHost( Host host )
    {
        networkHostsContainer.removeNetworkHost( host );
    }


    public void addNetworkHostsChangeListener( NetworkHostsChangeListener listener )
    {
        networkHostsContainer.addNetworkHostsChangeListener( listener );
    }

    public void removeNetworkHostsChangeListener( NetworkHostsChangeListener listener )
    {
        networkHostsContainer.removeNetworkHostsChangeListener( listener );
    }
    /////////////// END NetworkHostsContainer wrapper methods ///////////////////////


    public void doAutoConnectCheck()
    {
        if ( !networkMgr.isNetworkJoined() || !networkMgr.isConnected() )
        {
            return;
        }

        int hostCount;
        int requiredHostCount;

        if ( isAbleToBecomeUltrapeer() )
        {
            // as a ultrapeer I'm primary searching for Ultrapeers only...
            // to make sure I'm well connected...
            hostCount = networkHostsContainer.getUltrapeerConnectionCount();
            requiredHostCount = ServiceManager.sCfg.up2upConnections;
        }
        else if ( isShieldedLeafNode() || ServiceManager.sCfg.allowToBecomeLeaf )
        {
            // as a leaf I'm primary searching for Ultrapeers only...
            hostCount = networkHostsContainer.getUltrapeerConnectionCount();
            requiredHostCount = ServiceManager.sCfg.leaf2upConnections;
        }
        else
        {
            hostCount = networkHostsContainer.getTotalConnectionCount();
            requiredHostCount = ServiceManager.sCfg.peerConnections;
        }
        // count the number of missing connection tryes this is the required count
        // minus the available count. The result is multiplied by four to raise the
        // connection try count.
        int missingCount = ( requiredHostCount - hostCount ) * 4;
        
        // find out the number of hosts where a connection is currently tried...
        int allHostCount = networkHostsContainer.getNetworkHostCount( );
        int errorHostCount = networkHostsContainer.getNetworkHostCount(
            HostConstants.STATUS_HOST_ERROR);
        // make sure the value is not negative.
        int currentTryCount = Math.max( 0, allHostCount - hostCount - errorHostCount );
        
        // we will never try more then a reasonable parallel tries..
        int upperLimit = Math.min( MAX_PARALLEL_CONNECTION_TRIES,
            ServiceManager.sCfg.maxConcurrentConnectAttempts ) - currentTryCount;
        
        int outConnectCount = Math.min( missingCount-currentTryCount, 
            upperLimit );
        if ( outConnectCount > 0 )
        {
            NLogger.debug( HostManager.class, 
                "Auto-connect to " + outConnectCount + " new hosts.");
            networkHostsContainer.createOutConnectionToNextHosts( outConnectCount );
        }
    }    
    
    private class HostCheckTimer extends TimerTask
    {

        public static final long TIMER_PERIOD = 2000;

        /**
         * @see java.util.TimerTask#run()
         */
        public void run()
        {
            try
            {
                doAutoConnectCheck();
                networkHostsContainer.periodicallyCheckHosts();
            }
            catch ( Throwable th )
            {
                NLogger.error( NLoggerNames.GLOBAL, th, th );
            }
        }
    }
}