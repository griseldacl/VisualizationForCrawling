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
 *  $Id: UdpHostCacheContainer.java,v 1.8 2005/11/13 10:26:04 gregork Exp $
 */
package phex.udp.hostcache;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import phex.common.address.DestAddress;
import phex.common.address.IpAddress;
import phex.common.address.MalformedDestAddressException;
import phex.connection.NetworkManager;
import phex.net.presentation.PresentationManager;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

/**
 * Container of collected udp host caches.
 */
public class UdpHostCacheContainer
{
    private static int MIN_UDP_HOST_CACHE_SIZE = 20;
    
    /**
     * contains all the known functional caches in this session.
     * The list is always sorted in the ascending order of fail count.
     * So remember to sort it if we independently modify the list
     */
    private List functionalUdpCaches;
    
    /**
     * All the udpHostCaches not in functional udp caches container,
     * it contains all caches to be tried or which have failed.
     * The list is always sorted in the ascending order of fail count.
     * So remember to sort it if we independently modify the list
     */
    private List generalUdpCaches;
    
    /**
     * List of Default udp Host Caches
     */
    private static final List defaultCaches;
    static 
    {
        defaultCaches = new ArrayList();
        //add a list of default caches to this list
    }
    
    /**
     * the comparator used to sort the list of caches
     */
    public static final Comparator cacheComparator = 
        new UdpHostCacheComparator();
    
    public UdpHostCacheContainer() 
    {
        functionalUdpCaches = new ArrayList();
        generalUdpCaches = new ArrayList();
        NLogger.debug( NLoggerNames.UDP_HOST_CACHE, " Started up UDP HOST CACHE CONTAINER ");
    }
    
    public void initialize()
    {
        //first clear caches 
        functionalUdpCaches.clear();
        generalUdpCaches.clear();
        
        //first load from file 
        loadCachesFromFile();
        
        //now add the default caches
        generalUdpCaches.addAll( defaultCaches );
        
        NLogger.debug( NLoggerNames.UDP_HOST_CACHE, " Initialized UDP HOST CACHE CONTAINER ");
    }
    
    /**
     * <p>Adds a working cache to the functional container,
     * first it checks if it is already present in the general host cache list,
     * if it is found then it is removed from the general host cache list and 
     * then addded to the functional udp host cache container </p>
     * 
     * <p>it also <b>resets the fail count</b> to zero before adding the cache </p>
     * @param Hostcache
     * @return
     * true on success false if already present or otherwise
     */
    public boolean addFunctionalCache( UdpHostCache cache )
    {
        //first check n remove in  general Udp Cache container
        synchronized( generalUdpCaches )
        {
            if( generalUdpCaches.contains( cache ) )
            {
                generalUdpCaches.remove( cache );
            }
        }
        cache.resetFailCount();
        NLogger.info( NLoggerNames.UDP_HOST_CACHE, " Adding a UDP Host Cache" +
        " to the FUNCTIONAL Container");
        return addTo( cache, functionalUdpCaches );
    }
    
    /**
     * adds a cache, into the general container
     * <p>
     * 	if the cache has a failure count of more then the max allowed
     * 	it is not added ,provided we have atleast a total of  
     * 	MIN_UDP_HOST_CACHE_SIZE elements in both the containers 
     * @param cache
     * @return
     */
    public boolean addCache( UdpHostCache cache )
    {
        int hostCacheCount = generalUdpCaches.size() + functionalUdpCaches.size();
        
        // failure count exceeds max permissible limit
        if( ( cache.getFailCount() >= UdpHostCache.MAX_FAIL_COUNT )
                && ( hostCacheCount > MIN_UDP_HOST_CACHE_SIZE ) )
        {
            NLogger.info( NLoggerNames.UDP_HOST_CACHE, " Udp host cache dropped due " +
                    "to failure count : " + cache );
            return false;
        }
        
        NLogger.info( NLoggerNames.UDP_HOST_CACHE, " Adding a UDP Host Cache" +
        " to the GENERAL HOST CACHE CONTAINER ");
        return addTo( cache, generalUdpCaches );
    }
    
    public String createPackedHostCaches()
    {
        final int PACKED_CACHES_SIZE = 8;
        
        int count = 0;
        StringBuffer packedCaches = new StringBuffer( PACKED_CACHES_SIZE * 21 );
        synchronized( functionalUdpCaches )
        {
            for ( Iterator udphc = functionalUdpCaches.iterator(); 
            udphc.hasNext() && count < PACKED_CACHES_SIZE; count++ )
            {
                UdpHostCache cache = ( UdpHostCache )udphc.next();
                DestAddress address = cache.getHostAddress(); 
                String ipString  = address.getFullHostName();
                // add to the packed cache
                packedCaches.append( ipString );
                packedCaches.append( "\n" );
            }
        }
        return packedCaches.toString();
    }
    
    /**
     *	<p>Queries for hosts from udp host caches</p>
     *	<p>
     *	It generally uses two functional caches 
     *	and one cache in the general container, 
     *	whose status we dont know
     *	so we can find out more functional caches
     *	These caches are at the head of the sorted list
     *	</p>
     *	<br>
     *	<p>
     *	If no functional caches are available, like at startup,
     *	it uses all caches from the general container
     *	</p>
     * 	<p>
     * 	Caches pinged are removed , their failure count incremented 
     *  and then added to general host cache container.
     * 	This is done on the assumption that the cache will fail.
     * </p>
     * <br>
     * <p>
     *  If we do get back a reply Pong
     * 	It will anyway contain the udphc field and will be processed and added back to 
     * 	the functional host cache container by the UdpMessageEngine
     * 	</p> 
     * @author Madhu
     */
    public void queryMoreHosts()
    {
        final int NO_OF_CACHES_TO_PING = 3;
        
        int pingedCacheCount = 0;
        
        // hunt for NO_OF_CACHES_TO_PING - 1 functional caches 
        for( int i = 0; i < NO_OF_CACHES_TO_PING - 1;  i++ )
        {
            synchronized( functionalUdpCaches )
            {
                if( functionalUdpCaches.isEmpty() )
                {
                    break;
                }
                // k its not empty
                UdpHostCache cache = ( UdpHostCache )functionalUdpCaches.remove( 0 );
                if( cache != null )
                {
                    queryCache( cache );
                    pingedCacheCount++ ;
                }
            }
        }
        
        // get the remaining from the general host cache container
        for( int i = pingedCacheCount; i <= NO_OF_CACHES_TO_PING; i++ )
        {
            synchronized( generalUdpCaches )
            {
                if( generalUdpCaches.isEmpty() )
                {
                    break;
                }
                // k its not empty
                UdpHostCache cache = ( UdpHostCache )generalUdpCaches.remove( 0 ) ;
                if( cache != null )
                {
                    queryCache( cache );
                }
            }
        }
    }
    
    /**
     * pings cache,
     * increments their failure count( assuming its going to fail )
     * adds it to the general container
     * <br>
     * just see to it that this function is called only for caches 
     * 	which are removed from any containers
     * @param Host Cache
     */
    private void queryCache( UdpHostCache cache )
    {
        NLogger.info( NLoggerNames.UDP_HOST_CACHE, " Pinging UDP Host Cache :" +
        cache );
        // ping 
        cache.pingCache();
        //assumed it has failed
        cache.incrementFailCount();
        // add to general cache container
        addCache( cache );
    }
    
    private void loadCachesFromFile()
    {
        try
        {
            NetworkManager networkMgr = NetworkManager.getInstance();
            File file = networkMgr.getGnutellaNetwork().getUdpHostCacheFile();
            if ( !file.exists() )
            {
                return;
            }
            
            BufferedReader br = new BufferedReader( new FileReader(file) );
            String line;
            synchronized( generalUdpCaches )
            {
                while ( (line = br.readLine()) != null)
                {
                    if ( line.startsWith("#") )
                    {
                        continue;
                    }
                    insertCache( line );
                }
            }
            br.close();
        }
        catch ( IOException e )
        {
            NLogger.warn( NLoggerNames.UDP_HOST_CACHE, " Loading Udp Host Caches " +
                    " from file FAILED ", e );
        }
    }
    
    /**
     * creates a udpHostCache object and then adds it to the 
     * generalUdpCaches container
     * @param hostString
     */
    private void insertCache( String hostString ) 
    {
        try
        {
            DestAddress address = PresentationManager.getInstance().createHostAddress(
                hostString, IpAddress.DEFAULT_PORT);
            UdpHostCache cache = new UdpHostCache( address );
            addCache( cache );
        }
        catch ( MalformedDestAddressException e )
        {
            NLogger.warn( NLoggerNames.UDP_HOST_CACHE, " Could not create cache to add to the container" +
                    " from the host string : " + hostString, e );
        }
    }
    
    /**
     * adds a UdpHostcache to a HostCache Container in a thread safe manner
     * checks if already present and adds only if not present
     * <b>It then sorts the list in ascending order of fail count</b>  
     * @param Hostcache
     * @param CacheContainer
     * @return
     * true if added succesfully, 
     * false if not added
     */
    private boolean addTo( UdpHostCache cache, List cacheContainer )
    {
        synchronized ( cacheContainer )
        {
            if ( ! ( cacheContainer.contains( cache )) )
            {
                cacheContainer.add( cache );
                Collections.sort( cacheContainer, cacheComparator );
                NLogger.info( NLoggerNames.UDP_HOST_CACHE, " Added UdpHostCache  : " + cache ); 
                return true;
            }
        }
        return false;
    }
    
    /**
     * updates a UdpHostcache in a HostCache Container in a thread safe manner.
     * If present it removes the previous entry and adds a new one
     * otherwise it adds it as usual
     * <b>It then sorts the list in ascending order of fail count</b>
     * @param Hostcache
     * @param CacheContainer
     * @return
     * true if added succesfully, 
     * false if not added
     */
    private boolean update( UdpHostCache cache, List cacheContainer )
    {
        synchronized ( cacheContainer )
        {
            if ( (cacheContainer.contains( cache )) )
            {
                cacheContainer.remove( cache );
            }
            cacheContainer.add( cache );
            Collections.sort( cacheContainer, cacheComparator );
            NLogger.info( NLoggerNames.UDP_HOST_CACHE, " Added UdpHostCache  : " + cache ); 
            return true;
        }
    }
    
    public void saveCachesToFile()
    {
        try
        {
            NetworkManager networkMgr = NetworkManager.getInstance();
            File file = networkMgr.getGnutellaNetwork().getUdpHostCacheFile();
            BufferedWriter writer = new BufferedWriter( new FileWriter( file ) );
            
            writeCachesToFile( writer, functionalUdpCaches );
            writeCachesToFile( writer, generalUdpCaches );
            
            writer.flush();
            writer.close();
        }
        catch ( IOException exp )
        {
            NLogger.warn( NLoggerNames.UDP_HOST_CACHE, " Saving Udp Host Caches " +
                    " to file FAILED ", exp );
        }
    }
    
    private void writeCachesToFile(  BufferedWriter writer, List cacheContainer ) 
    throws IOException
    {
        synchronized( cacheContainer )
        {
            for ( Iterator udpHC = cacheContainer.iterator(); udpHC.hasNext(); )
            {
                UdpHostCache cache = ( UdpHostCache )udpHC.next();
                DestAddress address = cache.getHostAddress(); 
                String ipString  = address.getFullHostName();
                writer.write( ipString );
                writer.newLine();
            }
        }
    }
    
//    /**
//     * removes a hostcache from the head of the list in a thread safe manner
//     * @param CacheContainer
//     * @return
//     * A Udp host cache if found, 
//     * Null if container is empty
//     */
//    private UdpHostCache removeFrom( List cacheContainer )
//    {
//        synchronized ( cacheContainer )
//        {
//            if( !(cacheContainer.isEmpty()) )
//            {
//                UdpHostCache cache;
//                cache = ( UdpHostCache ) cacheContainer.remove( 0 );
//                return cache;
//            }
//        }
//        return null;
//    }
    
}
