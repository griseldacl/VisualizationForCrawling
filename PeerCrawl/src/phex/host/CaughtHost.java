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
 *  $Id: CaughtHost.java,v 1.6 2005/11/03 16:33:46 gregork Exp $
 */
package phex.host;

import phex.common.IntObj;
import phex.common.address.DestAddress;

/**
 * 
 */
public class CaughtHost
{
    private static IntObj CAUGHT_HOST_COUNTER = new IntObj(0);
    
    /**
     * The counter give each CaughtHost a unique id..
     */
    private int counter;
    private long lastFailedConnection;
    private long lastSuccessfulConnection;
    private int avgDailyUptime;
    private DestAddress hostAddress;
    
    /**
     * @param address
     */
    public CaughtHost( DestAddress address  )
    {
        synchronized ( CAUGHT_HOST_COUNTER )
        {
            this.counter = CAUGHT_HOST_COUNTER.inc(1);
        }
        hostAddress = address;
        lastFailedConnection = -1;
        lastSuccessfulConnection = -1;
    }
    
    public DestAddress getHostAddress()
    {
        return hostAddress;
    }
        
    /**
     * @param dailyUptime
     */
    public void setDailyUptime(int dailyUptime)
    {
        avgDailyUptime = dailyUptime;
    }
    
    /**
     * @return
     */
    public int getDailyUptime()
    {
        return avgDailyUptime;
    }
    
    public boolean equals( Object o )
    {
        if ( !(o instanceof CaughtHost ) )
        {
            return false;
        }
        return hostAddress.equals( ((CaughtHost)o).hostAddress );
    }
    
    public int hashCode()
    {
        return hostAddress.hashCode();
    }
    
    /**
     * Returns 1 if the last connection was successful, -1 if the last
     * connectio failed or 0 if not connected.
     * @return
     */
    public int getConnectionTimeRating()
    {
        if ( lastSuccessfulConnection == -1 && lastFailedConnection == -1 )
        {
            return 0;
        }
        if ( lastFailedConnection > lastSuccessfulConnection )
        {
            return -1;
        }
        else
        {
            return 1;
        }
    }
    
    /**
     * @param l
     */
    public void setLastFailedConnection(long l)
    {
        lastFailedConnection = l;
    }

    /**
     * @param l
     */
    public void setLastSuccessfulConnection(long l)
    {
        lastSuccessfulConnection = l;
    }
    /**
     * @return
     */
    public long getLastFailedConnection()
    {
        return lastFailedConnection;
    }

    /**
     * @return
     */
    public long getLastSuccessfulConnection()
    {
        return lastSuccessfulConnection;
    }
    
    public String toString()
    {
        return "CaughtHost[" + hostAddress.toString() + ",Failed=" + 
            lastFailedConnection + ",Successful=" + lastSuccessfulConnection +
            ",Uptime=" + avgDailyUptime + "]";
    }
    /**
     * The counter give each CaughtHost a unique id. This is only introduced
     * to be used in a comparator.
     * @return
     */
    public int getCounter()
    {
        return counter;
    }

}