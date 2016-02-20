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
 *  $Id: CatchedHostCache.java,v 1.8 2005/11/03 16:33:46 gregork Exp $
 */
package phex.host;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import phex.common.ServiceManager;
import phex.common.address.DestAddress;

/**
 * This container caches CatchedHosts in a sorted collection. CatchedHosts
 * are sorted in probability of successful connection.
 * The cache has a limited size and lowest elements are dropped when the max size
 * has been reached.
 */
public class CatchedHostCache
{
    private TreeSet sortedHosts;
    private HashMap addressHostMapping;
    
    public CatchedHostCache( )
    {
        sortedHosts = new TreeSet( new CaughtHostComparator() );
        addressHostMapping = new HashMap();
    }
    
    /**
     * Returns the cached CaughtHost associated by this HostAddress.
     * @param address the HostAddress to look up the CaughtHost for.
     * @return the CaughtHost of the given HostAddress or null if not available.
     */
    public synchronized CaughtHost getCaughHost( DestAddress address )
    {
        return (CaughtHost)addressHostMapping.get( address );
    }
    
    /**
     * Adds the given CaughtHost to the host cache if no already present. If the
     * cache is full the element with the lowest successful connection
     * probability is dropped.
     * @param host the CaughtHost to add.
     */
    public synchronized void add( CaughtHost host )
    {
        if ( addressHostMapping.containsKey( host.getHostAddress() ) )
        {
            return;
        }
        
        if ( sortedHosts.size() < ServiceManager.sCfg.mNetMaxHostToCatch )
        {
            addressHostMapping.put( host.getHostAddress(), host );
            sortedHosts.add( host );
        }
        else
        {
            addressHostMapping.put( host.getHostAddress(), host );
            sortedHosts.add( host );
            check();
            if ( sortedHosts.size() >= ServiceManager.sCfg.mNetMaxHostToCatch )
            {
                CaughtHost dropObject = (CaughtHost)sortedHosts.first(); 
                remove( dropObject );
            }
        }
        check();
    }
    
    /**
     * Removes the CaughtHost from the host cache.
     * @param host the CaughtHost to remove.
     */
    public synchronized void remove( CaughtHost host )
    {
        CaughtHost value = (CaughtHost)addressHostMapping.remove(
            host.getHostAddress() );
        if ( value != null )
        {
            sortedHosts.remove( value );
        }
        check();
    }
    
    /**
     * Clears the complete host cache.
     */
    public synchronized void clear()
    {
        sortedHosts.clear();
        addressHostMapping.clear();
    }
    
    /**
     * Returns a iterator of all CaughtHost reverse ordered by the successful
     * connection probability.
     * @return a reverse ordered iterator of all CaughtHost.
     */
    public synchronized Iterator iterator()
    {
        return sortedHosts.iterator();
    }
    
    private void check()
    {
        /*if ( addressHostMapping.size() != sortedHosts.size() )
        {
            System.out.println("OUT OF SYNC");
        }*/
    }
}