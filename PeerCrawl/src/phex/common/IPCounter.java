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
 */
package phex.common;

import java.util.HashMap;

import phex.common.address.DestAddress;


/**
 * The class is able to count connections to IPs. This is usefull to track how
 * many parallel uploads or downloads to one IP are tried.
 */
public class IPCounter
{
    private HashMap ipCountMap;

    /**
     * The max number of times a IP is allowed.
     */
    private int maxCount;

    public IPCounter( int maxCount )
    {
        ipCountMap = new HashMap();
        this.maxCount = maxCount;
    }
    
    public synchronized void setMaxCount( int val )
    {
        maxCount = val;
    }

    public synchronized boolean validateAndCountIP( DestAddress address )
    {
        Integer count = (Integer)ipCountMap.get( address );
        if ( count != null )
        {
            if ( count.intValue() == maxCount )
            {
                return false;
            }
            ipCountMap.put( address, new Integer( count.intValue() + 1 ) );
        }
        else
        {
            ipCountMap.put( address, new Integer( 1 ) );
        }
        return true;
    }

    public synchronized void relaseIP( DestAddress address )
    {
        Integer count = (Integer)ipCountMap.get( address );
        if ( count != null )
        {
            if ( count.intValue() == 1 )
            {
                ipCountMap.remove( address );
                return;
            }
            ipCountMap.put( address, new Integer( count.intValue() - 1 ) );
        }
    }
}