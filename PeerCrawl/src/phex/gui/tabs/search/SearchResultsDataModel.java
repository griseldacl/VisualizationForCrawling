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
 *  $Id: SearchResultsDataModel.java,v 1.10 2005/10/03 00:18:27 gregork Exp $
 */
package phex.gui.tabs.search;

import java.util.*;

import phex.common.IntObj;
import phex.download.RemoteFile;
import phex.event.SearchDataEvent;
import phex.event.SearchDataListener;
import phex.gui.models.ISearchDataModel;
import phex.gui.models.SearchTreeTableModel;
import phex.query.Search;
import phex.query.SearchFilter;

/**
 * This data model is doing the transition between the search result comming
 * from phex.query.Search and the data that is displayed through 
 * phex.gui.tabs.search.SearchResultsPanel.
 * Every instance of phex.query.Search that is displayed on the UI has its
 * associated phex.gui.tabs.search.SearchResultsDataModel. The lookup and
 * association is also handled in this class through static helpers.
 */
public class SearchResultsDataModel implements SearchDataListener, ISearchDataModel
{
    /**
     * To allow easy lookup this HashMap maps a search object to
     * its corresponding SearchResultDataModel. 
     */
    private static final HashMap searchToDataModelMap = new HashMap();
    
    /**
     * A list of all search results in form of RemoteFile. The list must be
     * locked before beeing modified.
     */
    private ArrayList/*<RemoteFile>*/ allRemoteFiles;
    
    /**
     * A set to identify all search results.
     * The list allSearchResultSHA1Set must be locked before modifing this.
     */
    private HashSet/*<String>*/ allSearchResultSHA1Set;
    
    /**
     * The number of SearchResult elements identified. To identify the count
     * the allSearchResultSHA1Set is used.
     */
    private int allSearchResultCount;
    
    /**
     * A list of sorted and filtered search results. This list must always be
     * in a sorted and filtered state. All inserts and removes must ensure that
     * the list maintains to be sorted and filtered. The list must be locked before
     * beeing modified.
     */
    private ArrayList/*<SearchResultElement>*/ displayedSearchResults;
    
    /**
     * A map to ensure performant grouping of search results. The key of the 
     * map is the SHA1 value of the RemoteFile. The value is the SearchResultElement
     * that is the group of this SHA1.
     * The SHA1 value is used for grouping the RemoteFiles
     */
    private HashMap/*<String,SearchResultElement>*/ displayedSearchResultSHA1Map;
    
    /**
     * The visualization model that needs to be updated on data model
     * changes or null if currently not visible.
     */
    private SearchTreeTableModel visualizationModel;
    
    private SearchResultElementComparator comparator;
    
    private Search search;
    
    /**
     * The search filter used for local filtering.
     */
    protected SearchFilter searchFilter;
    
    private IntObj searchElementCountObj;
    private IntObj filteredElementCountObj;
    
    private SearchResultsDataModel( Search search )
    {
        this.search = search;
        allRemoteFiles = new ArrayList();
        allSearchResultSHA1Set = new HashSet();
        allSearchResultCount = 0;
        displayedSearchResults = new ArrayList();
        displayedSearchResultSHA1Map = new HashMap();
        searchElementCountObj = new IntObj();
        filteredElementCountObj = new IntObj();
        comparator = new SearchResultElementComparator();
        search.addSearchDataListener( this );
        searchFilter = null;
    }
    
    public int getSearchElementCount()
    {
        return displayedSearchResults.size();
    }
    
    public IntObj getSearchElementCountObj()
    {
        if ( searchElementCountObj.intValue() != displayedSearchResults.size() )
        {
            searchElementCountObj.setValue( displayedSearchResults.size() );
        }
        return searchElementCountObj;
    }
    
    /**
     * Returns the number of hits that get locally filtered because:
     * - the file size is out of bounds
     * - the search string contains a filtered term
     * - the media type does not fit.
     */
    public IntObj getFilteredElementCountObj()
    {        
        int count = getFilteredElementCount();
        if ( filteredElementCountObj.intValue() != count )
        {
            filteredElementCountObj.setValue( count );
        }
        return filteredElementCountObj;
    }
    
    /**
     * Returns the number of hits that get locally filtered because:
     * - the file size is out of bounds
     * - the search string contains a filtered term
     * - the media type does not fit.
     */
    public int getFilteredElementCount()
    {   
        return allSearchResultCount - displayedSearchResults.size();
    }

    
    public SearchResultElement getSearchElementAt( int index )
    {
        if ( index < 0 || index >= displayedSearchResults.size() )
        {
            return null;
        }
        return (SearchResultElement)displayedSearchResults.get( index );
    }
    
    private void addSearchResults( Object[] newSearchResults )
    {
        synchronized( allRemoteFiles )
        {
        synchronized( displayedSearchResults )
        {
            RemoteFile remoteFile;
            for ( int i = 0; i < newSearchResults.length; i++ )
            {
                remoteFile = (RemoteFile)newSearchResults[i];
                addSearchResultForDisplay( remoteFile );
                addSearchResultToAll( remoteFile );
            }
        }
        }
    }
    
    private void addSearchResultToAll( RemoteFile remoteFile )
    {
        allRemoteFiles.add( remoteFile );
        
        String sha1 = remoteFile.getSHA1();        
        boolean found = false;
        if ( sha1 != null )
        {
            found = allSearchResultSHA1Set.contains( sha1 );
        }
        if ( !found )
        {
            allSearchResultCount ++;
            if ( sha1 != null && sha1.length() > 0 )
            {
                allSearchResultSHA1Set.add( sha1 );
            } 
        }
    }

    private void addSearchResultForDisplay(RemoteFile remoteFile)
    {
        boolean isFiltered = isFiltered( remoteFile );
        if ( isFiltered )
        {
            return;
        }
        
        SearchResultElement resultElement = null;
        String sha1 = remoteFile.getSHA1();
        if ( sha1 != null )
        {
            resultElement = (SearchResultElement)displayedSearchResultSHA1Map.get( sha1 );
        }
        if ( resultElement != null )
        {            
            resultElement.addRemoteFile( remoteFile );
            fireSearchResultAdded(remoteFile, resultElement);
        }
        else
        {
            resultElement = new SearchResultElement( remoteFile );
            // search for the right position to add this search result.
            int index = Collections.binarySearch( displayedSearchResults,
                resultElement, comparator );
            if (index <= 0)
            {
                if ( sha1 != null && sha1.length() > 0 )
                {
                    displayedSearchResultSHA1Map.put( sha1, resultElement );
                } 
                displayedSearchResults.add(-index-1, resultElement);
                fireNewSearchResultAdded(resultElement, -index-1);
            }
        }
    }
    
    /**
     * 
     * @param sortField Must be one of the SearchElementComparater static fields.
     */
    public void setSortBy( int sortField, boolean isSortedAscending )
    {
        synchronized( displayedSearchResults )
        {
            comparator.setSortField( sortField, isSortedAscending );
            Collections.sort( displayedSearchResults, comparator );
            fireAllSearchResultsChanged();
        }
    }
    
    /**
     * Sets the visualiztion model that needs to be updated on data model
     * changes or null if currently not visible.
     * @param model the new visualization model or null if not visible.
     */
    public void setVisualizationModel( SearchTreeTableModel model )
    {
        visualizationModel = model;
    }
    
    /**
     * Updates the used search filter and forces a filtering of the query hits. 
     * @param aSearchFilter the search filter to use.
     */
    public void updateSearchFilter( SearchFilter aSearchFilter )
    {
        synchronized( allRemoteFiles )
        {
        synchronized( displayedSearchResults )
        {
            searchFilter = aSearchFilter;
            searchFilter.setLastTimeUsed( System.currentTimeMillis() );
            updateFilteredQueryList();
        }
        }
    }
    
    /**
     * Clears all set filter conditions of the search filter.
     */
    public void clearSearchFilter()
    {
        synchronized( allRemoteFiles )
        {
        synchronized( displayedSearchResults )
        {
            searchFilter = null;
            updateFilteredQueryList();
        }
        }
    }
    
    /**
     * Clears all search results of the search. This includes filtered and not
     * filtered search results.
     */
    public void clearSearchResults()
    {
        synchronized( allRemoteFiles )
        {
        synchronized( displayedSearchResults )
        {
                allRemoteFiles.clear();
                allSearchResultSHA1Set.clear();
                allSearchResultCount = 0;
                displayedSearchResults.clear();
                displayedSearchResultSHA1Map.clear();
                fireAllSearchResultsChanged();
                // update search workaround to update search count...
                search.fireSearchChanged();
        }
        }
    }
    
    public Search getSearch()
    {
        return search;
    }
    
    /**
     * Returns the used search filter, or null if no search
     * filter is used.
     * @return the used search filter, or null.
     */
    public SearchFilter getSearchFilter( )
    {
        return searchFilter;
    }
    
    private boolean isFiltered( RemoteFile remoteFile )
    {
        boolean isFiltered = false;
        if ( searchFilter != null )
        {
            isFiltered = searchFilter.isFiltered( remoteFile );
        }
        return isFiltered;
    }
    
    private void updateFilteredQueryList()
    {
        synchronized( allRemoteFiles )
        {
        synchronized( displayedSearchResults )
        {
            displayedSearchResultSHA1Map.clear();
            displayedSearchResults.clear();
            fireAllSearchResultsChanged();
            Iterator iterator = allRemoteFiles.iterator();
            while( iterator.hasNext() )
            {
                RemoteFile file = (RemoteFile)iterator.next();
                addSearchResultForDisplay( file );
            }
            // update search workaround to update search count...
            search.fireSearchChanged();
        }
        }
    }


    /**
     * @see phex.event.SearchChangeListener#searchChanged(phex.event.SearchChangeEvent)
     */
    public void searchDataChanged( SearchDataEvent e )
    {
        if ( e.getType() == SearchDataEvent.SEARCH_HITS_ADDED )
        {
            Object[] newSearchResults = e.getSearchData();
            addSearchResults( newSearchResults );
        }
    }
    
    ///////////////////////// START Event forwarding ///////////////////////////////
    
    private void fireAllSearchResultsChanged()
    {
        if ( visualizationModel == null )
        {
            return;
        }
        Object[] path = new Object[]
        {
            visualizationModel.getRoot()
        };
        
        visualizationModel.fireTreeStructureChanged( this, path, null, null );
    }
    
    private void fireNewSearchResultAdded(
        SearchResultElement resultElement,
        int index)
    {
        if ( visualizationModel == null )
        {
            return;
        }
        Object[] path = new Object[]
        {
            visualizationModel.getRoot()
        };
        int[] indices = new int[]
        {
            index
        };
        Object[] changes = new Object[]
        {
            resultElement
        };
        //visualizationModel.fireTreeNodesInserted(
        //    this, path, indices, changes );
        
        if ( displayedSearchResults.size() == 1 )
        { // this was the first element added
            visualizationModel.fireTreeStructureChanged( this, path, indices, changes );
        }
        else
        {
            visualizationModel.fireTreeNodesInserted(
                this, path, indices, changes );
        }
    }

    private void fireSearchResultAdded(
        RemoteFile remoteFile,
        SearchResultElement resultElement)
    {
        if ( visualizationModel == null )
        {
            return;
        }
        Object[] path = new Object[]
        {
            visualizationModel.getRoot(),
            resultElement
        };
        Object[] changes = new Object[]
        {
            remoteFile
        };
        visualizationModel.fireTreeNodesInserted(
            this, path, null, changes );
    }
    
    ///////////////////////// END Event forwarding ///////////////////////////////
    
    //////////////////////// START Static Lookup methods ///////////////////////////
    
    public static SearchResultsDataModel registerNewSearch( Search search )
    {
        SearchResultsDataModel dataModel = new SearchResultsDataModel( search );
        searchToDataModelMap.put( search, dataModel );
        return dataModel;
    }
    
    public static SearchResultsDataModel lookupResultDataModel( Search search )
    {
        return (SearchResultsDataModel) searchToDataModelMap.get( search );
    }
    
    //////////////////////// END Static Lookup methods ///////////////////////////
}
