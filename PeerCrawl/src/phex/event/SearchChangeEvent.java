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
 *  $Id: SearchChangeEvent.java,v 1.7 2005/10/03 00:18:25 gregork Exp $
 */
package phex.event;

import java.util.EventObject;

public class SearchChangeEvent extends EventObject
{
    public static final short SEARCH_STARTED = 0;
    public static final short SEARCH_HITS_ADDED = 1;
    public static final short SEARCH_FILTERED = 2;
    public static final short SEARCH_CHANGED = 3;
    public static final short SEARCH_STOPED = 4;

    /**
     * The type of the search change event. This can be:
     * SEARCH_STARTED, SEARCH_HITS_ADDED, SEARCH_FILTERED or SEARCH_STOPED.
     */
    private short type;
    private int startIdx;
    private int endIdx;

    /**
     * 
     * @param source
     * @param aType The type of the search change event. This can be:
     * SEARCH_STARTED, SEARCH_HITS_ADDED, SEARCH_FILTERED or SEARCH_STOPED.
     */
    public SearchChangeEvent( Object source, short aType )
    {
        this( source, aType, -1, -1 );
    }

    /**
     * 
     * @param source
     * @param aType The type of the search change event. This can be:
     * SEARCH_STARTED, SEARCH_HITS_ADDED, SEARCH_FILTERED or SEARCH_STOPED.
     * @param aStartIdx
     * @param aEndIdx
     */
    public SearchChangeEvent( Object source, short aType, int aStartIdx,
        int aEndIdx )
    {
        super( source );
        type = aType;
        startIdx = aStartIdx;
        endIdx = aEndIdx;
    }

    /**
     * The type of the search change event. This can be:
     * SEARCH_STARTED, SEARCH_HITS_ADDED, SEARCH_FILTERED or SEARCH_STOPED.
     * @return the type of the search change event.
     */
    public short getType()
    {
        return type;
    }

    public int getStartIndex()
    {
        return startIdx;
    }

    public int getEndIndex()
    {
        return endIdx;
    }

}