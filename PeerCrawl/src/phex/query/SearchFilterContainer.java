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

import java.io.*;
import java.util.*;

import javax.xml.bind.*;

import phex.common.*;
import phex.event.*;
import phex.utils.*;
import phex.xml.*;

public class SearchFilterContainer
{
    private ArrayList searchFilterList;

    public SearchFilterContainer()
    {
        searchFilterList = new ArrayList();
        loadSearchFilters();
    }

    /**
     * Returns the number of available search filters.
     * @return the number of available search filters.
     */
    public int getSearchFilterCount()
    {
        return searchFilterList.size();
    }

    /**
     * Returns the search filter at index.
     * @param index the index
     * @return the search filter at index.
     */
    public SearchFilter getSearchFilterAt( int index )
    {
        if ( index < 0 || index >= searchFilterList.size() )
        {
            return null;
        }
        return (SearchFilter) searchFilterList.get( index );
    }

    /**
     * Returns a search filter for name. If no search filter could be found
     * null is returned.
     * @param name The name of the search filter to look for
     * @return the SearchFilter if found or null.
     */
    public SearchFilter getSearchFilterWithName( String name )
    {
        name = name.trim();
        SearchFilter filter;
        for ( int i = searchFilterList.size() - 1; i >= 0; i-- )
        {
            filter = (SearchFilter)searchFilterList.get( i );
            if ( filter.getName().equals( name ) )
            {
                return filter;
            }
        }
        return null;
    }

    /**
     * Adds a new search filter to the list.
     * @param searchFilter The search filter to add.
     */
    public void addSearchFilter( SearchFilter searchFilter )
    {
        searchFilterList.add( 0, searchFilter );
        fireSearchFilterAdded( 0 );
    }

    /**
     * Removes a search filter from the list.
     * @param searchFilter The search filter to remove.
     */
    public void removeSearchFilter( SearchFilter searchFilter )
    {
        int idx = searchFilterList.indexOf( searchFilter );
        if ( idx > -1 )
        {
            searchFilterList.remove( idx );
            saveSearchFilters();
            fireSearchFilterRemoved( idx );
        }
    }

    public void saveSearchFilters()
    {
//        Logger.logMessage( Logger.CONFIG, Logger.SEARCH,
//            "Saving search filters..." );
//            
//        if ( searchFilterList.size() == 0 )
//        {
//            // no filters to save available...
//            // skip save and delete possible old file
//            File file = Environment.getInstance().getPhexConfigFile(
//                EnvironmentConstants.XML_FILTER_LIST_FILE_NAME );
//            file.delete();
//        }
//
//        // JAXB-beta way
//        try
//        {
//            ObjectFactory objFactory = new ObjectFactory();
//            XJBPhex phex = objFactory.createPhexElement();
//            phex.setPhexVersion( VersionUtils.getFullProgramVersion() );
//
//            XJBSearchFilters xjbFilters = objFactory.createXJBSearchFilters();
//            phex.setSearchFilters( xjbFilters );
//
//            List xjbFilterList = xjbFilters.getSearchFilterList();
//            SearchFilter filter;
//            XJBSearchFilter xjbFilter;
//            for ( int i = searchFilterList.size() - 1; i >= 0; i-- )
//            {
//                filter = (SearchFilter)searchFilterList.get( i );
//                xjbFilter = filter.createXJBSearchFilter();
//                xjbFilterList.add( xjbFilter );
//            }
//
//            File file = Environment.getInstance().getPhexConfigFile(
//                EnvironmentConstants.XML_FILTER_LIST_FILE_NAME );
//            XMLBuilder.saveToFile( file, phex );
//        }
//        catch ( JAXBException exp )
//        {
//            // TODO bring a GUI message that file cant be created
//            Logger.logError( exp );
//        }
//
    }

    public void loadSearchFilters()
    {
//        Logger.logMessage( Logger.CONFIG, Logger.SEARCH,
//            "Loading search filters..." );
//
//        // JAXB-BETA way
//        File file = Environment.getInstance().getPhexConfigFile(
//            EnvironmentConstants.XML_FILTER_LIST_FILE_NAME );
//        XJBPhex phex;
//        try
//        {
//            phex = XMLBuilder.loadXJBPhexFromFile( file );
//            if ( phex == null )
//            {
//                return;
//            }
//        }
//        catch ( JAXBException exp )
//        {
//            // TODO bring a GUI message that file cant be created
//            Logger.logError( exp );
//            return;
//        }
//
//        List xjbFilterList = phex.getSearchFilters().getSearchFilterList();
//        SearchFilter filter;
//        XJBSearchFilter xjbFilter;
//        Iterator iterator = xjbFilterList.iterator();
//        while( iterator.hasNext() )
//        {
//            try
//            {
//                xjbFilter = (XJBSearchFilter)iterator.next();
//                filter = SearchFilter.createFromxJBSearchFilter( xjbFilter );
//                searchFilterList.add( filter );
//            }
//            catch ( Exception exp )
//            {// catch all exception in case we have an error in the XML
//                Logger.logError( exp,
//                    "Error loading a search filter from XML." );
//            }
//        }

    }




    ///////////////////// START event handling methods ////////////////////////

    /**
     * All listeners interested in events.
     */
    private ArrayList listenerList = new ArrayList( 2 );

    public void addSearchFilterListListener( SearchFilterListListener listener )
    {
        listenerList.add( listener );
    }

    public void removeSearchListChangeListener( SearchFilterListListener listener )
    {
        listenerList.remove( listener );
    }

    protected void fireSearchFilterChanged( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                SearchFilterListListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (SearchFilterListListener)listeners[ i ];
                    listener.searchFilterChanged( position );
                }
            }
        });
    }

    protected void fireSearchFilterAdded( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                SearchFilterListListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (SearchFilterListListener)listeners[ i ];
                    listener.searchFilterAdded( position );
                }
            }
        });
    }

    protected void fireSearchFilterRemoved( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                SearchFilterListListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (SearchFilterListListener)listeners[ i ];
                    listener.searchFilterRemoved( position );
                }
            }
        });
    }

    protected void fireSearchFilterChanged( SearchFilter search )
    {
        int position = searchFilterList.indexOf( search );
        if ( position >= 0 )
        {
            fireSearchFilterChanged( position );
        }
    }
    ///////////////////// END event handling methods ////////////////////////
}