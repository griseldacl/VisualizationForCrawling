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
package phex.gui.comparator;

import phex.utils.*;
import java.util.*;

public class IPComparator implements Comparator
{
    public int compare( Object o1, Object o2 )
    {
        byte[] ip1 = (byte[])o1;
        byte[] ip2 = (byte[])o2;

        long ip1l = IOUtil.unsignedInt2Long( IOUtil.deserializeInt( ip1, 0 ) );
        long ip2l = IOUtil.unsignedInt2Long( IOUtil.deserializeInt( ip2, 0 ) );


        if ( ip1l < ip2l )
        {
            return -1;
        }
        // only if rate and object is equal return 0
        else
        {
            return 1;
        }
    }
}