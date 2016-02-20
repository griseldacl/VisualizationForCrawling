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
package phex.query;

import phex.common.*;
import phex.event.*;

public class BackgroundSearchContainer extends SearchContainer
{
    public BackgroundSearchContainer()
    {
        setSearchChangeListener( new SingleSearchChangeListener() );
    }

    public synchronized Search createSearch( String queryStr )
    {
        throw new UnsupportedOperationException( );
    }

    public synchronized Search createSearch( String queryStr, URN queryURN,
        long minFileSize, long maxFileSize, long searchTimeout )
    {
        Search search = new Search( queryStr, queryURN );
        search.setPermanentlyFilter( true );
        SearchFilter filter = new SearchFilter( "TempSearchFilter" );
        filter.updateSearchFilter( minFileSize, maxFileSize );
        search.updateSearchFilter( filter );

        int idx = searchList.size();
        insertToSearchList( search, idx );
        search.startSearching();
        return search;
    }

    private class SingleSearchChangeListener implements SearchChangeListener
    {
        public void searchChanged( SearchChangeEvent event )
        {
            Search source = (Search) event.getSource();
            fireSearchChanged( source );
            if ( event.getType() == SearchChangeEvent.SEARCH_STOPED )
            {
                removeSearch( source );
            }
        }
    }
}