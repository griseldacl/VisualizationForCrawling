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

import java.util.*;


import phex.common.*;
import phex.common.address.DestAddress;
import phex.event.*;
import phex.host.*;
import phex.msg.*;


public class SearchContainer
{
    /**
     * All listeners interested in events.
     */
    private ArrayList listenerList = new ArrayList( 2 );

    // to let the background search container share.
    protected ArrayList searchList;

    private SearchChangeListener searchChangeListener;

    public SearchContainer()
    {
        searchChangeListener = new SingleSearchChangeListener();
        searchList = new ArrayList();
    }

    /**
     * Create a Search object and call startSearching on it.
     */
    public synchronized Search createSearch( String queryStr )
    {
        Search search = new Search( queryStr );
        insertToSearchList( search, 0 );

        if ( searchList.size() > ServiceManager.sCfg.mSearchMaxConcurrent )
        {
            int idx = searchList.size() - 1;
            removeFromSearchList( idx );
        }

        search.startSearching();

        return search;
    }

    public synchronized BrowseHostResults createBrowseHostSearch(
        DestAddress hostAddress, GUID hostGUID )
    {
        BrowseHostResults search = new BrowseHostResults( hostAddress, hostGUID );
        insertToSearchList( search, 0 );

        if ( searchList.size() > ServiceManager.sCfg.mSearchMaxConcurrent )
        {
            int idx = searchList.size() - 1;
            removeFromSearchList( idx );
        }
        search.startSearching();

        return search;
    }

    /**
     * The only method allowed to actually add a search to the list.
     */
    protected synchronized void insertToSearchList( Search search, int position )
    {
        search.addSearchChangeListener( searchChangeListener );
        searchList.add( position, search );
        fireSearchAdded( position );
    }

    /**
     * The only method allowed to actually remove a search from the list.
     * The search is stopped before it's removed
     */
    protected synchronized void removeFromSearchList( int index )
    {
        Search search = getSearchAt( index );
        search.stopSearching();
        search.removeSearchChangeListener( searchChangeListener );
        searchList.remove( index  );
        fireSearchRemoved( index );
    }

    /**
     * Returns the first found existing Search with the specified search string
     * if it is still searching. If there is no running search with the given
     * search string is found null is returned.
     */
    public synchronized Search getRunningSearch( String searchString )
    {
        Iterator iterator = searchList.iterator();
        while ( iterator.hasNext() )
        {
            Search search = (Search)iterator.next();
            if (   search.isSearching()
                && search.getSearchString().equals( searchString ) )
            {
                return search;
            }
        }
        return null;
    }

    public synchronized int getSearchCount()
    {
        return searchList.size();
    }
    
    public synchronized int getIndexOfSearch( Search search )
    {
        int size = searchList.size();
        for ( int i = 0; i < size; i++ )
        {
            if ( search == searchList.get( i ) )
            {
                return i;
            }
        }
        return -1;
    }

    public synchronized Search getSearchAt( int index )
    {
        if (index < 0 || index >= getSearchCount() )
        {
            return null;
        }
        return (Search)searchList.get( index );
    }

    /**
     * Removes the search from the search list. The search will be stoped before
     * it's removed.
     */
    public synchronized void removeSearch( Search search )
    {
        int index = searchList.indexOf( search );
        // if a search was found.
        if ( index >= 0 )
        {
            removeFromSearchList( index );
        }
    }

    public synchronized void removeSearch( int index )
    {
        removeFromSearchList( index );
    }

    /**
     * Stops all searches where the timeout has passed.
     */
    public synchronized void stopExpiredSearches( long currentTime )
    {
        for (int i = 0; i < searchList.size(); i++)
        {
            ( (Search)searchList.get( i ) ).checkForSearchTimeout( currentTime );
        }
    }

    public synchronized void stopAllSearches()
    {
        for (int i = 0; i < searchList.size(); i++)
        {
            ( (Search)searchList.get( i ) ).stopSearching();
        }
    }

    public synchronized void removeAllSearches()
    {
        for ( int i = searchList.size() - 1; i >= 0; i-- )
        {
            removeFromSearchList( i );
        }
    }

    /**
     * Process the query response. No IP filtering is done from here on.
     */
    public synchronized void processQueryResponse( QueryResponseMsg msg )
    {
        // TODO this can be optimized by checking for the msgID of the query..
        for (int i = 0; i < searchList.size(); i++)
        {
            Search search = (Search)searchList.get( i );
            if ( search instanceof BrowseHostResults )
            {
                continue;
            }
            search.processResponse( msg );
        }
    }

    ///////////////////// START event handling methods ////////////////////////

    public void addSearchListChangeListener( SearchListChangeListener listener )
    {
        listenerList.add( listener );
    }

    public void removeSearchListChangeListener( SearchListChangeListener listener )
    {
        listenerList.remove( listener );
    }

    protected void fireSearchChanged( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                SearchListChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (SearchListChangeListener)listeners[ i ];
                    listener.searchChanged( position );
                }
            }
        });
    }

    protected void fireSearchAdded( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                SearchListChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (SearchListChangeListener)listeners[ i ];
                    listener.searchAdded( position );
                }
            }
        });
    }

    protected void fireSearchRemoved( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                SearchListChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (SearchListChangeListener)listeners[ i ];
                    listener.searchRemoved( position );
                }
            }
        });
    }

    protected void setSearchChangeListener( SearchChangeListener listener )
    {
        searchChangeListener = listener;
    }

    private class SingleSearchChangeListener implements SearchChangeListener
    {
        public void searchChanged( SearchChangeEvent event )
        {
            Search source = (Search) event.getSource();
            fireSearchChanged( source );
        }
    }

    protected void fireSearchChanged( Search search )
    {
        int position = searchList.indexOf( search );
        if ( position >= 0 )
        {
            fireSearchChanged( position );
        }
    }
    ///////////////////// END event handling methods ////////////////////////
}