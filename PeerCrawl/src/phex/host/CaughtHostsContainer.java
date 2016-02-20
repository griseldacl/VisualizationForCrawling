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
 *  $Id: CaughtHostsContainer.java,v 1.48 2005/11/21 00:15:47 gregork Exp $
 */
package phex.host;

import java.io.*;
import java.util.*;

import phex.common.Environment;
import phex.common.ServiceManager;
import phex.common.ThreadPool;
import phex.common.address.AddressUtils;
import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.connection.NetworkManager;
import phex.event.AsynchronousDispatcher;
import phex.event.CaughtHostsChangeListener;
import phex.gwebcache.GWebCacheManager;
import phex.msg.PongMsg;
import phex.security.PhexSecurityManager;
import phex.udp.hostcache.UdpHostCacheManager;
import phex.utils.*;

/**
 * Responsible for holding all caught hosts.
 */
public class CaughtHostsContainer
{
    public static final short HIGH_PRIORITY = 2;
    public static final short NORMAL_PRIORITY = 1;
    public static final short LOW_PRIORITY = 0;
        
    private phex.utils.PriorityQueue caughtHosts;
    private HashSet uniqueCaughtHosts;
    
    /**
     * The CatchedHostCache provides a container with a limited size.
     * The container stores CaughtHosts ordered by successful connection
     * probability. When the container is full, the element with the
     * lowest priority is dropped.
     * Access needs to be synchronized on this object.
     */
    private CatchedHostCache catchedHostCache;
    
    /**
     * All listeners interested in events.
     */
    private ArrayList listenerList = new ArrayList( 2 );
    
    private boolean hasChangedSinceLastSave;

    public CaughtHostsContainer()
    {
        int[] capacities = new int[3];
        capacities[ HIGH_PRIORITY ] = (int)Math.round( ServiceManager.sCfg.mNetMaxHostToCatch
            * 2.0 / 3.0 );
        capacities[ NORMAL_PRIORITY ] = (int)Math.round( (ServiceManager.sCfg.mNetMaxHostToCatch
            - capacities[HIGH_PRIORITY]) * 2.0 / 3.0 );
        capacities[ LOW_PRIORITY ] = ServiceManager.sCfg.mNetMaxHostToCatch
            - capacities[HIGH_PRIORITY] - capacities[NORMAL_PRIORITY];
        caughtHosts = new phex.utils.PriorityQueue( capacities );

        uniqueCaughtHosts = new HashSet();
        catchedHostCache = new CatchedHostCache();
        
        Environment.getInstance().scheduleTimerTask(
            new SaveHostsContainerTimer(), SaveHostsContainerTimer.TIMER_PERIOD,
            SaveHostsContainerTimer.TIMER_PERIOD );
    }
    
    /**
     *
     */
    public void initializeCaughtHostsContainer()
    {
        caughtHosts.clear();
        catchedHostCache.clear();
        hasChangedSinceLastSave = false;
        loadHostsFromFile();
    }

    
    /**
     * Adds a caught host based on the information from a pong message.
     * @param pongMsg the pong message to add the caught host from.
     */
    public void addCaughtHost( PongMsg pongMsg )
    {
        byte[] pongIP = pongMsg.getIP();
        int port = pongMsg.getPort();
        DestAddress address = new DefaultDestAddress( pongIP, port );
        
        boolean valid = isValidCaughtHostAddress( address );
        if ( !valid )
        {
            return;
        }
        
        CaughtHost caughtHost = new CaughtHost( address );
        int dailyUptime = pongMsg.getDailyUptime();
        if ( dailyUptime > 0 )
        {
            caughtHost.setDailyUptime( dailyUptime );
        }
        
        short priority;
        if ( pongMsg.isUltrapeerMarked() )
        {
            priority = CaughtHostsContainer.HIGH_PRIORITY;
        }
        else
        {
            priority = CaughtHostsContainer.LOW_PRIORITY;
        }

        addPersistentCaughtHost( caughtHost );
        addToCaughtHostFetcher( caughtHost, priority );
    }
    
    /**
     * Adds a host address with given priority to the caught hosts.
     */
    public void addCaughtHost( DestAddress address, short priority )
    {
        boolean valid = isValidCaughtHostAddress( address );
        if ( !valid )
        {
            return;
        }        
        CaughtHost caughtHost = new CaughtHost( address );
        addPersistentCaughtHost( caughtHost );
        addToCaughtHostFetcher( caughtHost, priority );
    }
    
    /**
     * Adds the caught host to the host catcher that is used for
     * upcomming network connections.
     * The host is added to the top of its priority slot in the host catcher.
     * @param caughtHost the CaughtHost to add.
     * @param priority the priority slot to add the host to.
     */
    private synchronized void addToCaughtHostFetcher( CaughtHost caughtHost,
        short priority )
    {
        // if host is not already in the list
        if ( !uniqueCaughtHosts.contains( caughtHost ) )
        {
            uniqueCaughtHosts.add( caughtHost );

            Object removed = caughtHosts.addToHead( caughtHost, priority );
            if ( removed != null )
            {// cause aging of dropped elements.
                uniqueCaughtHosts.remove( removed );
            }
            fireCaughtHostsChanged( );
        }
    }
    
    /**
     * Adds the caught host to the caught host cache that is stored in the
     * phex.hosts file. The position where the host is added depends on its
     * prioity in the cache.
     * If the host is already part of the cache the daily uptime is updated. 
     * @param caughtHost the caught host to add or update with.
     */
    private void addPersistentCaughtHost( CaughtHost caughtHost )
    {        
        synchronized ( catchedHostCache )
        {
            DestAddress address = caughtHost.getHostAddress();
            CaughtHost existingHost = (CaughtHost)catchedHostCache.getCaughHost( address );
            if ( existingHost == null )
            {
                catchedHostCache.add( caughtHost );
            }
            else
            {
                // update daily uptime..
                // to maintain correct order first remove... 
                catchedHostCache.remove( existingHost );
                // then modify...
                if ( caughtHost.getDailyUptime() > 0 )
                {
                    existingHost.setDailyUptime( caughtHost.getDailyUptime() );
                }
                // then add...
                catchedHostCache.add( existingHost );
            }
            hasChangedSinceLastSave = true;
        }
    }
    
    /**
     * The number of caught hosts that are ready to be used for upcomming
     * network connections.
     * @return the number of caught hosts.
     */
    public synchronized int getCaughtHostsCount()
    {
        return caughtHosts.getSize();
    }
    
    /**
     * Removes and returns the next caught host for network connection.
     * A call to this method also tryes to ensure that there are always
     * enought hosts availble for networks connections. 
     * @return the netxt top most host ready for network connection. 
     */
    public synchronized DestAddress getNextCaughtHost()
    {
        CaughtHost host;
        ensureMinCaughHosts();
        if ( !caughtHosts.isEmpty() )
        {
            host = (CaughtHost)caughtHosts.removeMaxPriority();
            return host.getHostAddress();
        }
        // host list is empty
        // In this case we need to wait until a GWebCache reports IP's hopefully.
        return null;
    }
    
    /**
     * Reports the connection status of a just tried network connection to a
     * HostAddress, this is used to update the CatchedHostCache.
     * @param hostAddress the host address to report the connection status for.
     * @param successfulConnection indicates if the connection was successful or not.
     */
    public void reportConnectionStatus( DestAddress hostAddress,
        boolean successfulConnection )
    {
        boolean valid = isValidCaughtHostAddress( hostAddress );
        if ( !valid )
        {
            return;
        }
        synchronized ( catchedHostCache )
        {
            CaughtHost existingHost = catchedHostCache.getCaughHost( hostAddress );
            if ( existingHost == null )
            {
                existingHost = new CaughtHost( hostAddress ); 
            }
            else
            {
                // to maintain correct order first remove... 
                catchedHostCache.remove( existingHost );
            }
            
            // then modify...
            if (successfulConnection)
            {
                existingHost.setLastSuccessfulConnection( System.currentTimeMillis() );
            }
            else
            {
                existingHost.setLastFailedConnection( System.currentTimeMillis() );
            }
    
            // then add...
            catchedHostCache.add( existingHost );
            hasChangedSinceLastSave = true;
        }
    }

    /**
     * Makes sure that at least one percent of the max number of hosts
     * are in the host catcher otherwise a GWebCache is queried.
     */
    private void ensureMinCaughHosts()
    {
        int minCount = (int)Math.ceil(
            (double)ServiceManager.sCfg.mNetMaxHostToCatch/100.0 );
        if ( caughtHosts.getSize() < minCount )
        {
        	// Query udpHostCache for new hosts
        	UdpHostCacheManager.getInstance().invokeQueryCachesRequest();
        	NLogger.info( NLoggerNames.GLOBAL, " Started a UDP HOST CACHE Query" +
			    " to ensure min no of hosts in the caughthost container");
            // connect GWebCache for new hosts...
            GWebCacheManager.getInstance().invokeQueryMoreHostsRequest( false );
        }
    }
    
    /**
     * Validates a host address if it is acceptable for the host catcher.
     * A valid address has a valid port and ip, has no localhost ip or 
     * private ip, and has a port that is not user banned.
     * @param address the address to validate
     * @return true if the address is valid, false otherwise.
     */
    private boolean isValidCaughtHostAddress( DestAddress address )
    {
        if (   address.isLocalHost() 
            || !address.isValidAddress() 
            || address.isSiteLocalAddress() )
        {
            return false;
        }
        if ( IPUtils.isPortInUserInvalidList( address ) )
        {
            return false;
        }
        return true;
    }
    
    /**
     * Loads the hosts file phex.hosts.
     */
    private void loadHostsFromFile()
    {
        Logger.logMessage( Logger.FINE, Logger.NETWORK,
            "Loading hosts file." );
        try
        {
            NetworkManager networkMgr = NetworkManager.getInstance();
            File file = networkMgr.getGnutellaNetwork().getHostsFile();
            BufferedReader br;
            if ( file.exists() )
            {
                br = new BufferedReader( new FileReader(file) );
            }
            else
            {
                Logger.logMessage( Logger.FINE, Logger.NETWORK,
                    "Load default hosts file." );
                InputStream inStream = ClassLoader.getSystemResourceAsStream(
                    "phex/resources/PeerCrawl.hosts" );
                if ( inStream != null )
                {
                    br = new BufferedReader( new InputStreamReader( inStream ) );
                }
                else
                {
                    Logger.logMessage( Logger.FINE, Logger.NETWORK,
                        "Default PeerCrawl Hosts file not found." );
                    return;
                }
            }

            PhexSecurityManager securityMgr = PhexSecurityManager.getInstance();
            String line;
            short usedPriority = LOW_PRIORITY;
            while ( (line = br.readLine()) != null )
            {
                if ( line.startsWith("#") )
                {
                    continue;
                }
                
                CaughtHost caughtHost = parseCaughtHostFromLine( line );
                if ( caughtHost == null )
                {
                    continue;
                }
                
                byte access = securityMgr.controlHostAddressAccess( caughtHost.getHostAddress() );
                switch ( access )
                {
                    case PhexSecurityManager.ACCESS_DENIED:
                    case PhexSecurityManager.ACCESS_STRONGLY_DENIED:
                        // skip host address...
                        continue;
                }
                if ( IPUtils.isPortInUserInvalidList( caughtHost.getHostAddress() ) )
                {
                    continue;
                }
                addPersistentCaughtHost( caughtHost );
                if ( usedPriority != HIGH_PRIORITY && caughtHosts.isFull( usedPriority ) )
                {
                    // goes from lowest priority (0) to highest (2)
                    usedPriority ++;
                }
                addToCaughtHostFetcher( caughtHost, usedPriority );
            }
            br.close();
        }
        catch ( IOException exp )
        {
            NLogger.warn(CaughtHostsContainer.class, exp, exp);
        }
    }
    
    /**
     * Returns the CaughtHost that can be parsed from the line, or 
     * null if parsing failed for some reason.
     * @param line
     * @return
     */
    private CaughtHost parseCaughtHostFromLine( String line )
    {
        // tokenize line
        // line format can be:
        // IP:port         or:
        // IP:port,lastFailedConnection,lastSuccessfulConnection,dailyUptime
        StringTokenizer tokenizer = new StringTokenizer( line, "," );
        int tokenCount = tokenizer.countTokens();
        
        String hostAddressStr;
        int dailyUptime;
        long lastFailedconnection;
        long lastSuccessfulConnection;
        if ( tokenCount == 1 )
        {
            hostAddressStr = line;
            dailyUptime = -1;
            lastFailedconnection = -1;
            lastSuccessfulConnection = -1;                    
        }
        else if ( tokenCount == 4 )
        {
            hostAddressStr = tokenizer.nextToken();
            try
            {
                lastFailedconnection = Long.parseLong( tokenizer.nextToken() );
            }
            catch ( NumberFormatException exp )
            {
                lastFailedconnection = -1;
            }
            try
            {
                lastSuccessfulConnection = Long.parseLong( tokenizer.nextToken() );
            }
            catch ( NumberFormatException exp )
            {
                lastSuccessfulConnection = -1;
            }
            try
            {
                dailyUptime = Integer.parseInt( tokenizer.nextToken() );
            }
            catch ( NumberFormatException exp )
            {
                dailyUptime = -1;
            }
        }
        else
        {// Unknown format
            NLogger.warn( CaughtHostsContainer.class, 
                "Unknown HostCache line format: " + line);
            return null;
        }
        byte[] ip = AddressUtils.parseIP( hostAddressStr );
        if ( ip == null )
        {
            return null;
        }
        int port = AddressUtils.parsePort( hostAddressStr );
        if ( port == -1 )
        {
            return null;
        }
        DestAddress hostAddress = new DefaultDestAddress( ip, port );
        CaughtHost caughtHost = new CaughtHost( hostAddress );
        
        if ( dailyUptime > 0 )
        {
            caughtHost.setDailyUptime( dailyUptime );
        }
        if ( lastFailedconnection > 0 )
        {
            caughtHost.setLastFailedConnection( lastFailedconnection );
        }
        if ( lastSuccessfulConnection > 0 )
        {
            caughtHost.setLastSuccessfulConnection( lastSuccessfulConnection );
        }
        
        return caughtHost;
    }
    
    /**
     * Blocking operation which saves the caught hosts and auto connect hosts
     * if they changed since the last save operation.
     */
    public void saveHostsContainer( )
    {
        if ( !hasChangedSinceLastSave )
        {
            return;
        }
        saveCaughtHosts();
        hasChangedSinceLastSave = false;
    }

    /**
     * The caught hosts are saved from the persistent host cache container in 
     * reverse order. Since when loaded back the last element will be on top of
     * the caught host priority queue. 
     */
    private void saveCaughtHosts()
    {
        NLogger.debug(CaughtHostsContainer.class, "Start saving caught hosts." );
        try
        {
            NetworkManager networkMgr = NetworkManager.getInstance();
            File file = networkMgr.getGnutellaNetwork().getHostsFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            
            synchronized( catchedHostCache )
            {
                Iterator iterator = catchedHostCache.iterator();
                while( iterator.hasNext() )
                {
                    CaughtHost host = (CaughtHost)iterator.next();
                    DestAddress hostAddress = host.getHostAddress();
                    
                    // line format can be:
                    // IP:port         or:
                    // IP:port,lastFailedConnection,lastSuccessfulConnection,
                    //         dailyUptime
                    bw.write( hostAddress.getFullHostName() +
                        "," + host.getLastFailedConnection() +
                        "," + host.getLastSuccessfulConnection() +
                        "," + host.getDailyUptime()  );
                    
                    bw.newLine();
                }
            }
            bw.close();
        }
        catch ( IOException exp )
        {
            NLogger.error( CaughtHostsContainer.class, exp, exp );
        }
        NLogger.debug(CaughtHostsContainer.class, "Finish saving caught hosts." );
    }
    
    ///////////////////// START event handling methods ////////////////////////
    public void addCaughtHostsChangeListener( CaughtHostsChangeListener listener )
    {
        listenerList.add( listener );
    }

    public void removeCaughtHostsChangeListener( CaughtHostsChangeListener listener )
    {
        listenerList.remove( listener );
    }

    /*private void fireCaughtHostAdded( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                CaughtHostsChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (CaughtHostsChangeListener)listeners[ i ];
                    listener.caughtHostAdded( position );
                }
            }
        });
    }

    private void fireCaughtHostRemoved( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                CaughtHostsChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (CaughtHostsChangeListener)listeners[ i ];
                    listener.caughtHostRemoved( position );
                }
            }
        });
    }*/

    private void fireCaughtHostsChanged( )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                CaughtHostsChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (CaughtHostsChangeListener)listeners[ i ];
                    listener.caughtHostsChanged( );
                }
            }
        });
    }

    private void fireAutoConnectHostAdded( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                CaughtHostsChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (CaughtHostsChangeListener)listeners[ i ];
                    listener.autoConnectHostAdded( position );
                }
            }
        });
    }

    private void fireAutoConnectHostRemoved( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                CaughtHostsChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (CaughtHostsChangeListener)listeners[ i ];
                    listener.autoConnectHostRemoved( position );
                }
            }
        });
    }

    ////////////////////// END event handling methods //////////////////////////
    
    ////////////////////// START inner classes //////////////////////////
    
    private class SaveHostsContainerRunner implements Runnable
    {
        public void run()
        {
            saveHostsContainer();
        }
    }
    
    private class SaveHostsContainerTimer extends TimerTask
    {
        // once per minute
        public static final long TIMER_PERIOD = 1000 * 60;
        
        public void run()
        {
            try
            {
                // trigger the save inside a background job to not
                // slow down the timer too much
                ThreadPool.getInstance().addJob( new SaveHostsContainerRunner(),
                    "SaveHostsContainer" );
            }
            catch ( Throwable th )
            {
                NLogger.error( CaughtHostsContainer.class, th, th );
            }
        }
    }
}