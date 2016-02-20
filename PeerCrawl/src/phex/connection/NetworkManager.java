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
 *  $Id: NetworkManager.java,v 1.34 2005/11/13 10:06:32 gregork Exp $
 */
package phex.connection;

import java.io.IOException;

import phex.common.*;
import phex.common.address.*;
import phex.event.*;
import phex.gui.common.GUIRegistry;
import phex.gui.common.MainFrame;
import phex.gwebcache.GWebCacheManager;
import phex.host.HostManager;
import phex.net.OIOServer;
import phex.net.OnlineObserver;
import phex.net.Server;
import phex.net.presentation.PresentationManager;
import phex.query.QueryManager;
import phex.udp.hostcache.UdpHostCacheManager;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

public class NetworkManager implements Manager
{
    /**
     * Our singleton instance of the NetworkManager.
     */
    private static NetworkManager instance;
    
    /**
     * The Gnutella Network configuration and settings separation class.
     */
    private GnutellaNetwork gnutellaNetwork;

    /**
     * Indicates if the network is currently joined.
     */
    private boolean isNetworkJoined;

    /**
     * Indicates if we are currently connected to the network.
     */
    private boolean isConnected;

    /**
     * The forced address as it was given from the user.
     */
    private DestAddress forcedAddress;
    
    /**
     * The determined local address of this node.
     */
    private DestAddress localAddress;

    /**
     * The server waiting for incoming connection.
     */
    private Server server;
    
    private OnlineObserver onlineObserver;

    private NetworkManager()
    {
        updateGnutellaNetwork();
        isNetworkJoined = ServiceManager.sCfg.mAutoJoin;
        isConnected = ServiceManager.sCfg.mAutoConnect;
    }

    /**
     * Returns the singleton instance of this NetworkManager class.
     * @return the only NetworkManager instance.
     */
    public static NetworkManager getInstance()
    {
        if ( instance == null )
        {
            instance = new NetworkManager();
        }
        return instance;
    }

    /**
     * This method is called in order to initialize the manager. This method
     * includes all tasks that must be done to intialize all the several
     * manager. Like instantiating the singleton instance of the manager. Inside
     * this method you can't rely on the availability of other managers.
     * 
     * @return true is initialization was successful, false otherwise.
     * @see phex.common.Manager#initialize()
     */
    public boolean initialize()
    {
        onlineObserver = new OnlineObserver();
        return true;
    }

    /**
     * This method is called in order to perform post initialization of the
     * manager. This method includes all tasks that must be done after
     * initializing all the several managers. Inside this method you can rely on
     * the availability of other managers.
     * 
     * @return true is initialization was successful, false otherwise.
     * @see phex.common.Manager#onPostInitialization()
     * 
     */
    public boolean onPostInitialization()
    {
        if (ServiceManager.sCfg.mMyIP.length() > 0)
        {
            IpAddress ip = new IpAddress( 
                AddressUtils.parseIP(ServiceManager.sCfg.mMyIP) );
            setForcedHostIP( ip );
        }
        server = new OIOServer();
        try
        {
            server.startup();
        }
        catch ( IOException exp )
        {
            NLogger.error(NLoggerNames.STARTUP, exp, exp);
        }
        if ( isNetworkJoined )
        {
            HostManager hostMgr = HostManager.getInstance();
            hostMgr.getCaughtHostsContainer().initializeCaughtHostsContainer();
            hostMgr.getFavoritesContainer().initializeFavorites();

            GWebCacheManager gWebCacheMgr = GWebCacheManager.getInstance();
            gWebCacheMgr.getGWebCacheContainer().initializeGWebCacheContainer();
            
            UdpHostCacheManager udpHostCacheMgr = UdpHostCacheManager.getInstance();
            udpHostCacheMgr.getUdpHostCacheContainer().initialize();
        }
        return true;
    }

    /**
     * This method is called after the complete application including GUI
     * completed its startup process. This notification must be used to activate
     * runtime processes that needs to be performed once the application has
     * successfully completed startup.
     * 
     * @see phex.common.Manager#startupCompletedNotify()
     */
    public void startupCompletedNotify()
    {
    }

    /**
     * This method is called in order to cleanly shutdown the manager. It should
     * contain all cleanup operations to ensure a nice shutdown of Phex.
     * 
     * @see phex.common.Manager#shutdown()
     */
    public void shutdown()
    {
        server.shutdown(false);
    }
    
    public void restartServer()
        throws IOException
    {
        server.restart();
    }
    
    public boolean hasConnectedIncoming()
    {
        return server.hasConnectedIncoming();
    }

    /**
     * Connectes to the network if not already connected.
     * If we have not joined a network yet, first a network is joined by
     * calling joinNetwork() first.
     * Fires connectedToNetwork.
     */
    public synchronized void connectToNetwork()
    {
        if ( isConnected ) { return; }
        // isConnectd must be set to true before joining. Otherwise
        // the auto-connect on join feature will cause endless loop.
        isConnected = true;
        // reset observer by faking a successful connection
        onlineObserver.markSuccessfulConnection();
        if ( !isNetworkJoined )
        {
            joinNetwork();
        }
        fireConnectedToNetwork();
    }
    
    /**
     * Disconnectes from the network.
     * Fires disconnectedFromNetwork.
     *
     */
    public synchronized void disconnectNetwork()
    {
        isConnected = false;
        fireDisconnectedFromNetwork();
    }

    /**
     * Initializes and joins the currently configured network.
     * If auto-connect on join is enabled, connection to the network is triggered
     * by calling connectToNetwork()
     */
    public synchronized void joinNetwork()
    {
        updateGnutellaNetwork();

        HostManager hostMgr = HostManager.getInstance();
        hostMgr.getCaughtHostsContainer().initializeCaughtHostsContainer();
        hostMgr.getFavoritesContainer().initializeFavorites();

        GWebCacheManager gWebCacheMgr = GWebCacheManager.getInstance();
        gWebCacheMgr.getGWebCacheContainer().initializeGWebCacheContainer();

        // isNetworkJoined must be set to true before connecting. Otherwise
        // we run into endless loop on connect.
        isNetworkJoined = true;
        if ( !isConnected && ServiceManager.sCfg.mAutoConnect )
        {
            connectToNetwork();
        }

        // TODO2 model -> GUI interaction not allowed...
        // introduce network join event to resolve this.
        MainFrame frame = GUIRegistry.getInstance().getMainFrame();
        frame.setTitle();
    }

    /**
     * Leaves the currently joined network.
     *
     */
    public synchronized void leaveNetwork()
    {
        if ( isNetworkJoined )
        {
            isNetworkJoined = false;
            disconnectNetwork();
            // TODO2 direct model -> GUI interaction not allowed...
            // introduce network join event to resolve this.
            MainFrame frame = GUIRegistry.getInstance().getMainFrame();
            frame.setTitle();
            HostManager hostMgr = HostManager.getInstance();
            hostMgr.removeAllNetworkHosts();
            QueryManager.getInstance().getSearchContainer().stopAllSearches();
            hostMgr.getCaughtHostsContainer().saveHostsContainer();
            hostMgr.getFavoritesContainer().saveFavoriteHosts();
        }
    }

    /**
     * Indicates if we have currently joined a network.
     * @return <code>true</code> if we joined a network, <code>false</code> otherwise.
     */
    public synchronized boolean isNetworkJoined()
    {
        return isNetworkJoined;
    }

    /**
     * Indicates if we are connected to a network.
     * @return <code>true</code> if we are connected to a network, <code>false</code> otherwise.
     */
    public synchronized boolean isConnected()
    {
        return isConnected;
    }

    /**
     * Returns the current network.
     * @return the current network.
     */
    public GnutellaNetwork getGnutellaNetwork()
    {
        return gnutellaNetwork;
    }

    private void updateGnutellaNetwork()
    {
        if ( ServiceManager.sCfg.mCurrentNetwork
            .equals(Cfg.GENERAL_GNUTELLA_NETWORK) )
        {// use general gnutella network.
            if ( gnutellaNetwork == null
                || !(gnutellaNetwork instanceof GeneralGnutellaNetwork) )
            {
                gnutellaNetwork = new GeneralGnutellaNetwork();
            }
        }
        else
        {// use named gnutella network.
            if ( gnutellaNetwork == null
                || !(gnutellaNetwork.getName()
                    .equals(ServiceManager.sCfg.mCurrentNetwork)) )
            {
                gnutellaNetwork = new NamedGnutellaNetwork(
                    ServiceManager.sCfg.mCurrentNetwork);
            }
        }
    }
    
    
    public OnlineObserver getOnlineObserver()
    {
        return onlineObserver;
    }

    ///////////////////// START local IP handling /////////////////////////////

    
    /**
     * Returns the current local address. This will be the forced address
     * in case a forced address is set
     * @return the current determined local address or the user set forced address.
     */
    public DestAddress getLocalAddress()
    {
        if ( forcedAddress != null ) { return forcedAddress; }
        return localAddress;
    }

    /**
     * Updates the local address when there is no forced ip set.
     */
    public void updateLocalAddress( DestAddress updateAddress )
    {
        if ( forcedAddress != null )
        {
            // we have a forced address the local address has no value.
            return;
        }
        if ( localAddress == null || !localAddress.equals( updateAddress ) )
        { // init local address
            updateAddress.setPort( server.getListeningLocalPort() );
            localAddress = updateAddress;
            server.resetFirewallCheck();
            fireNetworkIPChanged();
        }
    }

    /**
     * Sets the forced IP in the configuration. This call is not saving the
     * configuration!
     */
    public void setForcedHostIP(IpAddress forcedHostIP)
    {
        PresentationManager presentationMgr = PresentationManager.getInstance();
        if ( forcedHostIP == null )
        {// clear forcedHostIP and init localAddress
            forcedAddress = null;
            ServiceManager.sCfg.mMyIP = "";
            IpAddress hostIP = server.resolveLocalHostIP();
            int port = server.getListeningLocalPort();
            DestAddress address = presentationMgr.createHostAddress( 
                hostIP, port );
            updateLocalAddress( address );
            return;
        }
        if ( !forcedHostIP.isValidIP() )
        { 
            throw new IllegalArgumentException( 
                "Invalid IP " + forcedHostIP );
        }

        ServiceManager.sCfg.mMyIP = forcedHostIP.getFormatedString();
        
        forcedAddress = presentationMgr.createHostAddress( forcedHostIP, 
            server.getListeningLocalPort() );
        fireNetworkIPChanged();
    }

    ///////////////////// START event handling methods ////////////////////////

    /**
     * The listeners interested in events.
     */
    private EventListenerList listenerList = new EventListenerList();

    public void addNetworkListener(NetworkListener listener)
    {
        listenerList.add(NetworkListener.class, listener);
    }

    public void removeNetworkListener(NetworkListener listener)
    {
        listenerList.remove(NetworkListener.class, listener);
    }
    
    public void addLoopbackListener(LoopbackListener listener)
    {
        listenerList.add(LoopbackListener.class, listener);
    }

    public void removeLoopbackListener(LoopbackListener listener)
    {
        listenerList.remove(LoopbackListener.class, listener);
    }

    public void fireConnectedToNetwork()
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.getListeners(NetworkListener.class);
                NetworkListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for (int i = listeners.length - 1; i >= 0; i--)
                {
                    listener = (NetworkListener) listeners[i];
                    listener.connectedToNetwork();
                }
            }
        });
    }

    public void fireDisconnectedFromNetwork()
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.getListeners(NetworkListener.class);
                NetworkListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for (int i = listeners.length - 1; i >= 0; i--)
                {
                    listener = (NetworkListener) listeners[i];
                    listener.disconnectedFromNetwork();
                }
            }
        });
    }

    public void fireNetworkIPChanged()
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.getListeners(NetworkListener.class);
                NetworkListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for (int i = listeners.length - 1; i >= 0; i--)
                {
                    listener = (NetworkListener) listeners[i];
                    listener.networkIPChanged(getLocalAddress());
                }
            }
        });
    }
    
    public void fireIncomingUriDownload( final String uri )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.getListeners(LoopbackListener.class);
                LoopbackListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for (int i = listeners.length - 1; i >= 0; i--)
                {
                    listener = (LoopbackListener) listeners[i];
                    listener.incommingUriDownload( uri );
                }
            }
        });
    }
    
    public void fireIncomingMagmaDownload( final String magmaFile )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.getListeners(LoopbackListener.class);
                LoopbackListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for (int i = listeners.length - 1; i >= 0; i--)
                {
                    listener = (LoopbackListener) listeners[i];
                    listener.incommingMagmaDownload( magmaFile );
                }
            }
        });
    }
    
    public void fireIncomingRSSDownload( final String rssFile )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.getListeners(LoopbackListener.class);
                LoopbackListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for (int i = listeners.length - 1; i >= 0; i--)
                {
                    listener = (LoopbackListener) listeners[i];
                    listener.incommingRSSDownload( rssFile );
                }
            }
        });
    }
    ///////////////////// END event handling methods ////////////////////////
}