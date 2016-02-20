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
 *  $Id: GWebCacheManager.java,v 1.13 2005/11/04 20:43:35 gregork Exp $
 *  File Modified
 */

/**
 * PeerCrawl - Distributed P2P web crawler based on Gnutella Protocol
 * @version 2.0
 * 
 * Developed as part of Masters Project - Spring 2006
 * @author 	Vaibhav Padliya
 * 			College of Computing
 * 			Georgia Tech
 * 
 * @contributor Mudhakar Srivatsa
 * @contributor Mahesh Palekar
 */

package phex.gwebcache;

import java.util.TimerTask;

import phex.common.Environment;
import phex.common.Manager;
import phex.common.ThreadPool;
import phex.common.address.DestAddress;
import phex.common.address.IpAddress;
import phex.connection.NetworkManager;
import phex.host.HostManager;
import phex.host.NetworkHostsContainer;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

/**
 * 
 */
public class GWebCacheManager implements Manager
{
    private static GWebCacheManager instance;
    private GWebCacheContainer gWebCacheContainer;
    
    /**
     * Lock to make sure not more then one thread request is running in parallel
     * otherwise it could happen that we create thread after thread while each
     * one takes a long time to come back.
     */
    private boolean isThreadRequestRunning = false;
    
    public GWebCacheContainer getGWebCacheContainer()
    {
        return gWebCacheContainer;
    }
    
    /**
     * Starts a query for more hosts in an extra thread.
     */
    public synchronized void invokeQueryMoreHostsRequest( boolean preferPhex )
    {
        // we dont want multiple thread request to run at once. If one thread
        // request is running others are blocked.
        if ( isThreadRequestRunning )
        {
            return;
        }
        isThreadRequestRunning = true;
        Runnable runner = new QueryHostsRunner( preferPhex );
        ThreadPool.getInstance().addJob( runner,
            "GWebCacheQuery-" + Integer.toHexString(runner.hashCode()) );
    }
    /**
     * Starts a update of GWebCaches in an extra thread.
     */
    public void invokeUpdateRemoteGWebCache( final DestAddress myHostAddress, boolean preferPhex )
    {
        Runnable runner = new UpdateGWebCacheRunner(myHostAddress, preferPhex);
        ThreadPool.getInstance().addJob( runner,
            "GWebCacheQuery-" + Integer.toHexString(runner.hashCode()) );
    }
    /**
     * Starts a query for more GWebCaches in an extra thread.
     */
    public synchronized void invokeQueryMoreGWebCachesRequest( boolean preferPhex )
    {
        // we dont want multiple thread request to run at once. If one thread
        // request is running others are blocked.
        if ( isThreadRequestRunning )
        {
            return;
        }
        isThreadRequestRunning = true;
        Runnable runner = new QueryGWebCachesRunner( preferPhex );
        ThreadPool.getInstance().addJob( runner,
            "GWebCacheQuery-" + Integer.toHexString(runner.hashCode()) );
    }    
    
    ////////////////////////////////////////////////////////////////////////////
    /// Manager methods
    ////////////////////////////////////////////////////////////////////////////
    
    public static GWebCacheManager getInstance()
    {
        if ( instance == null )
        {
            instance = new GWebCacheManager();
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
        gWebCacheContainer = new GWebCacheContainer();
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
        Environment.getInstance().scheduleTimerTask( 
            new QueryGWebCacheTimer(), 0,
            QueryGWebCacheTimer.TIMER_PERIOD );
        Environment.getInstance().scheduleTimerTask( 
            new UpdateGWebCacheTimer(), UpdateGWebCacheTimer.TIMER_PERIOD,
            UpdateGWebCacheTimer.TIMER_PERIOD );
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
    public void shutdown()
    {
    }
    
    ////////////////////////////////////////////////////////////////////////////
    /// Inner classes
    ////////////////////////////////////////////////////////////////////////////
    private final class QueryGWebCachesRunner implements Runnable
    {
        private boolean preferPhex;

        /**
         * @param preferPhex
         */
        public QueryGWebCachesRunner( boolean preferPhex )
        {
            this.preferPhex = preferPhex;
        }

        public void run()
        {
            try
            {
                gWebCacheContainer.queryMoreGWebCaches( preferPhex );
            }
            finally
            {
                isThreadRequestRunning = false;
            }
        }
    }
    
    private final class UpdateGWebCacheRunner implements Runnable
    {
        private final DestAddress myHostAddress;
        private boolean preferPhex;
        
        /**
         * @param preferPhex
         */
        private UpdateGWebCacheRunner(DestAddress myHostAddress, boolean preferPhex)
        {
            super();
            this.myHostAddress = myHostAddress;
            this.preferPhex = preferPhex;
        }
        
        public void run()
        {
            gWebCacheContainer.updateRemoteGWebCache( myHostAddress, preferPhex );
        }
    }
    
    private final class QueryHostsRunner implements Runnable
    {
        private boolean preferPhex;
        
        /**
         * @param preferPhex
         */
        public QueryHostsRunner( boolean preferPhex )
        {
            this.preferPhex = preferPhex;
        }
        
        public void run()
        {
            try
            {
                gWebCacheContainer.queryMoreHosts( preferPhex );
            }
            finally
            {
                isThreadRequestRunning = false;
            }
        }
    }
    
    private final class QueryGWebCacheTimer extends TimerTask
    {
        // every 10 minutes
        public static final long TIMER_PERIOD = 1000 * 60 * 10;

        public void run()
        {
            try
            {
                // no gwebcache actions if we have no auto connect and are
                // not connected to any host
                NetworkHostsContainer networkHostsCont =
                    HostManager.getInstance().getNetworkHostsContainer();
                if ( NetworkManager.getInstance().isConnected() ||
                     networkHostsCont.getTotalConnectionCount() > 0 )
                {
                    invokeQueryMoreGWebCachesRequest( false );
                    invokeQueryMoreHostsRequest( true );
                }
            }
            catch ( Throwable th)
            {
                NLogger.error( NLoggerNames.GWEBCACHE, th, th );
            }
        }
    }
    
    private final class UpdateGWebCacheTimer extends TimerTask
    {
        // once per hour
//        public static final long TIMER_PERIOD = 1000 * 60 * 60;
    	// once per minute
    	public static final long TIMER_PERIOD = 1000 * 60;

        public void run()
        {
            // no gwebcache actions if we have no auto connect and are
            // not connected to any host
            NetworkHostsContainer networkHostsCont =
                HostManager.getInstance().getNetworkHostsContainer();
            NetworkManager networkMgr = NetworkManager.getInstance();
            
            if ( networkMgr.isConnected() ||
                 networkHostsCont.getTotalConnectionCount() > 0 )
            {
                DestAddress localAddress = null;
                if ( networkMgr.hasConnectedIncoming() )
                {
                    localAddress = networkMgr.getLocalAddress();
                    IpAddress localIp = localAddress.getIpAddress();
                    if ( localIp != null && localIp.isSiteLocalIP() )
                    {
                        localAddress = null;
                    }
                }
                // even when localAddress is null update a GWebCache with
                // a new GWebCache URL.
                invokeUpdateRemoteGWebCache( localAddress, true );
            }
        }
    }
}
