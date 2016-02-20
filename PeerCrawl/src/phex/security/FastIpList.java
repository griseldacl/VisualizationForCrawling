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
 *  Created on 03.08.2005
 *  --- CVS Information ---
 *  $Id: FastIpList.java,v 1.2 2005/10/03 00:18:28 gregork Exp $
 */
package phex.security;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import phex.utils.IOUtil;

public class FastIpList
{
    /**
     * Stores for each first block of IP addressses a List
     */
    private List[] ipLists;
    
    public FastIpList()
    {
        ipLists = new List[256];
    }
    
    public void add( IPAccessRule rule )
    {
        int pos = IOUtil.unsignedByte2int( rule.getHostIP()[0] );
        if ( ipLists[pos] == null )
        {
            ipLists[pos] = new ArrayList();
        }
        synchronized( ipLists[pos] )
        {
            ipLists[pos].add( rule );
        }
    }
    
    public void remove( IPAccessRule rule )
    {
        int pos = IOUtil.unsignedByte2int( rule.getHostIP()[0] );
        if ( ipLists[pos] == null )
        {
            return;
        }
        synchronized( ipLists[pos] )
        {
            ipLists[pos].remove( rule );
        }
    }
    
    public byte controlHostIPAccess( byte[] hostIP )
    {
        int pos = IOUtil.unsignedByte2int( hostIP[0] );
        if ( ipLists[ pos ] == null )
        {
            return PhexSecurityManager.ACCESS_GRANTED;
        }
        synchronized( ipLists[pos] )
        {
            Iterator iterator = ipLists[pos].iterator();
            IPAccessRule rule;
            while( iterator.hasNext() )
            {
                rule = (IPAccessRule) iterator.next();
                if ( rule.isDisabled() )
                {// skip disabled rules...
                    continue;
                }
                if ( !rule.isHostIPAllowed( hostIP ) )
                {
                    if ( rule.isStrongFilter() )
                    {
                        return PhexSecurityManager.ACCESS_STRONGLY_DENIED;
                    }
                    return PhexSecurityManager.ACCESS_DENIED;
                }
            }
        }
        return PhexSecurityManager.ACCESS_GRANTED;
    }
}
