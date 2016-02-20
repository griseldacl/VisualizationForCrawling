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
 *  $Id: SearchDataEvent.java,v 1.5 2005/10/03 00:18:25 gregork Exp $
 */
package phex.event;

import java.util.EventObject;

/**
 * 
 */
public class SearchDataEvent extends EventObject
{
    public static final short SEARCH_STARTED = 0;
    public static final short SEARCH_HITS_ADDED = 1;
    public static final short SEARCH_CHANGED = 2;
    public static final short SEARCH_STOPED = 3;

    /**
     * The type of the search change event. This can be:
     * SEARCH_STARTED, SEARCH_HITS_ADDED, SEARCH_STOPED.
     */
    private short type;
    
    /**
     * The search data
     */
    private Object[] searchData;

    /**
     * 
     * @param source
     * @param aType The type of the search change event. This can be:
     * SEARCH_STARTED, SEARCH_HITS_ADDED, SEARCH_FILTERED or SEARCH_STOPED.
     */
    public SearchDataEvent( Object source, short aType )
    {
        this( source, aType, null );
    }

    /**
     * 
     * @param source
     * @param aType The type of the search change event. This can be:
     * SEARCH_STARTED, SEARCH_HITS_ADDED, SEARCH_FILTERED or SEARCH_STOPED.
     * @param aStartIdx
     * @param aEndIdx
     */
    public SearchDataEvent( Object source, short aType, Object[] data )
    {
        super( source );
        type = aType;
        searchData = data;
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

    /**
     * The provided search data.
     * @return
     */
    public Object[] getSearchData()
    {
        return searchData;
    }
}
