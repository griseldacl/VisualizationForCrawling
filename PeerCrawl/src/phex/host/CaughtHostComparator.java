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
 *  $Id: CaughtHostComparator.java,v 1.5 2005/10/03 00:18:27 gregork Exp $
 */
package phex.host;

import java.util.Comparator;

/**
 * This class is responsible for comparing two CaughtHost instances.
 * The Comparator will determine which instance has a higher probability
 * of a successful connection.
 */
public class CaughtHostComparator implements Comparator
{
    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Object o1, Object o2)
    {
        if ( o1 == o2 || o1.equals(o2))
        {
            return 0;
        }
        CaughtHost host1 = (CaughtHost)o1;
        CaughtHost host2 = (CaughtHost)o2;
        
        // first check if last connection was failed or successfull
        int h1Rating = host1.getConnectionTimeRating();
        int h2Rating = host2.getConnectionTimeRating();
        int diff = h1Rating - h2Rating;
        if ( diff != 0 )
        {
            return diff;
        }
        // second compare by daily uptime if known.
        diff = host1.getDailyUptime() - host2.getDailyUptime();
        if ( diff != 0 )
        {
            return diff;
        }
        // thrid compare which host has the latest successful connection
        diff = (int)(host1.getLastSuccessfulConnection() - host2.getLastSuccessfulConnection());
        if ( diff != 0)
        {
            return diff;
        }
        // no use the unique identification counter, it is used 
        // to have a constant difference between instances with no
        // other comparable difference.
        return host1.getCounter() - host2.getCounter();
    }    
}
