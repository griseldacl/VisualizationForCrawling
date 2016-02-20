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
 *  Created on 21.08.2005
 *  --- CVS Information ---
 *  $Id: GWebCacheComparator.java,v 1.3 2005/10/03 00:18:27 gregork Exp $
 */
package phex.gwebcache;

import java.util.Comparator;

public class GWebCacheComparator implements Comparator
{
    public int compare( Object o1, Object o2 )
    {
        GWebCache cache1 = (GWebCache) o1;
        GWebCache cache2 = (GWebCache) o2;
        
        if ( cache1.equals(cache2) )
        {
            return 0;
        }
        long diff = cache1.getEarliestReConnectTime() - cache2.getEarliestReConnectTime();
        if ( diff == 0)
        {
            return cache1.hashCode() - cache2.hashCode();
        }
        else if ( diff > 0 )
        {
            return 1;
        }
        else
        {
            return -1;
        }
    }

}
