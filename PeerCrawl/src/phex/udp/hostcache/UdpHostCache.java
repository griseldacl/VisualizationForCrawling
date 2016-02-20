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
 *  $Id: UdpHostCache.java,v 1.7 2005/11/03 23:30:12 gregork Exp $
 */
package phex.udp.hostcache;

import java.io.IOException;

import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.udp.UdpConnectionManager;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

/**
 * Represents the Udp Host Cache
 */
public class UdpHostCache
{
  /**
   * Maximum permissible failure count on a cache
   */  
    public static final int MAX_FAIL_COUNT = 3; 
    
    private DestAddress address;
  
    /**
     *  maintains the no of times this cache has consecutively failed
     *  if the cache fails continously for three times it is removed	
     */
    private int failedCount;
    
    public UdpHostCache( DestAddress addr )
    {
        address = addr;
        failedCount = 0;
    }
    
    public UdpHostCache( DestAddress addr , int failCount)
    {
        address = addr;
        failedCount = failCount;
    }
    
    public UdpHostCache( String aHostName, int aPort, int failCount )
    throws IllegalArgumentException
    {
        address = new DefaultDestAddress( aHostName, aPort );
        failedCount = failCount;
    }
    
    public void pingCache()
    {
        try
        {
            UdpConnectionManager.getInstance().sendUdpPing( this.address );
        }
        catch ( IOException e )
        {
            NLogger.warn( NLoggerNames.UDP_HOST_CACHE , " could not ping  " +
                    "Udp Host Cache : " + address + " \n ", e );
        }
    }
    
    // operations related to failure count
    
    public void incrementFailCount()
    {
        if ( failedCount < MAX_FAIL_COUNT )
        {
            failedCount++;
        }
    }
    
    public void decrementFailCount()
    {
        failedCount--; 
    }
    
    public int getFailCount()
    {
        return failedCount;
    }
    
    public void resetFailCount()
    {
        failedCount = 0;
    }
    
    
    public DestAddress getHostAddress()
    {
        return this.address;
    }
    
    /**
     * Does not take into consideration failure count 
     */
    public boolean equals( Object obj )
    {
        if ( obj instanceof UdpHostCache )
        {
            UdpHostCache objCache = ( UdpHostCache )obj; 
            return this.address.equals( objCache.address );
        }
        return false;
    }
    
    public int hashCode()
    {
        return this.address.hashCode();
    }
    
    public String toString()
    {
        String str = " Host Address : " + address + " [ failure count : " + 
        failedCount + " ] "; 
        
        return str;
    }
}
