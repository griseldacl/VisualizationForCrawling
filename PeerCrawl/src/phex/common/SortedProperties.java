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

import java.util.*;

/*
 * Make the properties get written out in order
 */
public class SortedProperties extends Properties
{
    SortedProperties()
    {
        super();
    }

    public Enumeration keys()
    {
        Enumeration e = super.keys();
        ArrayList myList = new ArrayList(80);
        while (e.hasMoreElements() )
        {
            myList.add(e.nextElement());
        }
        Collections.sort(myList);
        return new I2E(myList.iterator());
    }

    // A little class that makes an iterator look like an enumeration
    public class I2E implements Enumeration
    {
        private Iterator i;
        public I2E (Iterator i)
        { this.i = i; }
        public boolean hasMoreElements() {return i.hasNext();}
        public Object nextElement() {return i.next();}
    }
}
