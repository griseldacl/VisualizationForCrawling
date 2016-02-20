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
 *  $Id: UdpHostCacheManager.java,v 1.4 2005/10/19 23:23:55 gregork Exp $
 */
package phex.udp.hostcache;

import phex.common.Manager;
import phex.common.ThreadPool;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

/**
 *
 */
public class UdpHostCacheManager implements Manager
{
    private static UdpHostCacheManager instance;
    
    private UdpHostCacheContainer udpHostCacheContainer;
    
    /**
     * Lock to make sure not more then one thread request is running in parallel
     * otherwise it could happen that we create thread after thread while each
     * one takes a long time to come back.
     */
    private boolean isThreadRequestRunning = false;
    
    public static UdpHostCacheManager getInstance()
    {
        if ( instance == null )
        {
            instance = new UdpHostCacheManager();
        }
        return instance;
    }
    
    public UdpHostCacheContainer getUdpHostCacheContainer()
    {
        return udpHostCacheContainer;
    }
    
    /**
     * Indicates if we are a udp host cache. 
     */
    public boolean isUdpHostCache()
    {
        // TODO implement logik to determine if we are udp host cache capable.
        return false;
    }

    /**
     * Starts a query for more hosts in an extra thread.
     */
    public synchronized void invokeQueryCachesRequest()
    {
        // we dont want multiple thread request to run at once. If one thread
        // request is running others are blocked.
        if ( isThreadRequestRunning )
        {
            return;
        }
        isThreadRequestRunning = true;
        Runnable runner = new QueryCachesRunner();
        ThreadPool.getInstance().addJob( runner,
            "UdpHostCacheQuery-" + Integer.toHexString(runner.hashCode()) );
    }
    
    ////////////////  MANAGER METHODS  ///////////////
    /**
     * This method is called in order to initialize the manager. This method
     * includes all tasks that must be done to intialize all the several manager.
     * Like instantiating the singleton instance of the manager. Inside
     * this method you can't rely on the availability of other managers.
     * @return true is initialization was successful, false otherwise.
     */
    public boolean initialize()
    {
        udpHostCacheContainer = new UdpHostCacheContainer();
        NLogger.info( NLoggerNames.UDP_HOST_CACHE, "Starting Udp Host Cache Manager");
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
//        testUdp();
    }

    /**
     * This method is called in order to cleanly shutdown the manager. It
     * should contain all cleanup operations to ensure a nice shutdown of Phex.
     * It is called before the GUI closes.
     */
    public void shutdown() 
    {
        NLogger.info( NLoggerNames.UDP_HOST_CACHE, " UDP HOST CACHE MANAGER " +
        		" SHUTTING DOWN. Writing caches to file");
        udpHostCacheContainer.saveCachesToFile();
    }
    
    private final class QueryCachesRunner implements Runnable
    {
        public void run()
        {
            udpHostCacheContainer.queryMoreHosts( );
            isThreadRequestRunning = false;
        }
    }
    
    ///////////////////////////////////////////
//    private void testUdp()
//    {
//        class udpThread implements Runnable
//        {
//            public void run()
//            {
//                while( true )
//                {
//                    try
//                    {
//                        sleep( 1000 * 50 );
//                    }
//                    catch ( InterruptedException e1 )
//                    {
//                        // TODO Auto-generated catch block
//                        e1.printStackTrace();
//                    }
//                    invokeQueryCachesRequest();
//                }
//            }
//        
//                    
//                    Host[] addresses = HostManager.getInstance().getNetworkHostsContainer().getUltrapeerConnections();
//                    for ( int i =0 ; i < addresses.length ; i++ )
//                    {
//                        udpHostCacheContainer.addCache( new UdpHostCache( addresses[i].getHostAddress() ) );
//                        udpHostCacheContainer.addFunctionalCache( new UdpHostCache( addresses[i].getHostAddress() ) );
//                    }
////                    try
////                    {
////                        MsgPing ping = MsgPing.createUdpPingMsg();
////                        MsgPong pong = MsgPong.createUdpPongMsg( ping );
////                        for( int j = 0; j < 10; j++ )
////                        {
////                            UdpConnectionManager.getInstance().sendUdpPing( new HostAddress( "127.0.0.1", 11022));
////                            UdpConnectionManager.getInstance().sendUdpPing( new HostAddress( "127.0.0.1", 11022));
////                        }
////                    }
////                    catch (IOException e)
////                    {
////                    }                                        
//                    udpHostCacheContainer.queryMoreHosts();
//                }
//            }
////            
//            private synchronized void sleep ( long timeSec )
//            	throws InterruptedException
//            {
//                wait( timeSec );
//            }
//        }
//    }
//        ThreadPool.getInstance().addJob( new udpThread(), " TEST UDP THREAD STARTED " );
//    }
}
