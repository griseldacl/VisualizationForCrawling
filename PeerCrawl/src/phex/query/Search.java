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
 *  $Id: Search.java,v 1.53 2005/11/19 14:42:04 gregork Exp $
 */
package phex.query;

import java.util.*;

import phex.common.*;
import phex.common.address.DestAddress;
import phex.connection.NetworkManager;
import phex.download.RemoteFile;
import phex.event.*;
import phex.msg.*;
import phex.utils.*;

public class Search
{
    public static final long DEFAULT_SEARCH_TIMEOUT = 5 * 60 * 1000; // 5 minutes
    
    /**
     * The time when the search was started.
     */
    private long startTime;

    /**
     * The String that is beeing searched for.
     */
    private String searchString;

    /**
     * The URN that is beeing searched for.
     */
    private URN searchURN;

    /**
     * The search filter used for local filtering.
     */
    protected SearchFilter searchFilter;
    
    /**
     * The dynamic query engine that actually runs the search in case
     * a dynamic query is used. This can attribute can be null in case
     * no dynamic query is used (if we are a leaf). 
     */
    private DynamicQueryEngine queryEngine;

    /**
     * The MsgQuery object that forms the query for this search.
     */
    private QueryMsg queryMsg;

    /**
     * The list of query hits returned by the query. Contains the RemoteFile
     * objects.
     */
    protected ArrayList queryHitList;

    /**
     * The list of query hits after filtering the queryHitList using the
     * searchFilterConstraints. Contains the RemoteFile objects.
     * This object also acts as the lock for the filter process.
     */
    protected ArrayList displayedQueryHitList;
    
    /**
     * A cached buffer object that contains the query hit count.
     * The value is not always up to date and only validated and updated when
     * calling getQueryHitCountObj();
     */
    private IntObj queryHitCountObj;
    
    /**
     * A cached buffer object that contains the locally filtered hit count.
     * The value is not always up to date and only validated and updated when
     * calling getLocallyFilteredCountObj();
     */
    private IntObj locallyFilteredCountObj;
    
    /**
     * A cached buffer object that contains the current progress of the search.
     * The value is not always up to date and only validated and updated when
     * calling getProgressObj();
     */
    private IntObj progressObj;

    /**
     * If set to true all received query hits are directly filtered against the
     * searchFilterConstraints. Afterwards the search can't be refined.
     */
    private boolean isPermanentlyFiltered;

    /**
     * The status of the search.
     */
    protected boolean isSearching;

    /**
     * All listeners interested in events of this search.
     */
    private ArrayList listenerList = new ArrayList( 2 );


    public Search( String aSearchString )
    {
        this( aSearchString, null );
    }

    public Search( String aSearchString, URN aSearchURN )
    {
        isSearching = false;
        queryHitList = new ArrayList();
        displayedQueryHitList = new ArrayList();
        queryHitCountObj = new IntObj();
        locallyFilteredCountObj = new IntObj();
        progressObj = new IntObj();
        searchString = aSearchString;
        searchURN = aSearchURN;

        boolean isPhexBehindFirewall = !NetworkManager.getInstance()
            .hasConnectedIncoming();
        queryMsg = new QueryMsg( (byte)ServiceManager.sCfg.ttl, searchString,
            searchURN, QueryMsg.IS_PHEX_CAPABLE_OF_XML_RESULTS,
            isPhexBehindFirewall);

        searchFilter = null;
    }
    
    public void setSearchString( String aSearchString )
    {
        searchString = aSearchString;
        searchURN = null;
        boolean isPhexBehindFirewall = !NetworkManager.getInstance()
            .hasConnectedIncoming();
        queryMsg = new QueryMsg( (byte)ServiceManager.sCfg.ttl, searchString,
            searchURN, QueryMsg.IS_PHEX_CAPABLE_OF_XML_RESULTS,
            isPhexBehindFirewall);
        fireSearchChanged();
        updateFilteredQueryList();
    }

    /**
     * Updates the used search filter and forces a filtering of the query hits. 
     * @param aSearchFilter the search filter to use.
     */
    public void updateSearchFilter( SearchFilter aSearchFilter )
    {
        synchronized( displayedQueryHitList )
        {
            searchFilter = aSearchFilter;
            searchFilter.setLastTimeUsed( System.currentTimeMillis() );
            updateFilteredQueryList();
        }
    }

    /**
     * Flags if the query hits are permanently filtered by the used
     * search filter.
     * @param state true to permanently filter, false for none destructive filtering.
     */
    public void setPermanentlyFilter( boolean state )
    {
        isPermanentlyFiltered = state;
    }

    public String getSearchString()
    {
        return searchString;
    }
    
    /**
     * Tries a very basic calculation about the search progress.
     * @return
     */
    public int getProgress()
    {
        if ( !isSearching )
        {
            return 100;
        }
        if ( queryEngine != null )
        {
            return queryEngine.getProgress();
        }
        else
        {
            long currentTime = System.currentTimeMillis();
            // time progress...
            int timeProgress = (int)(100 - (double)( startTime + DEFAULT_SEARCH_TIMEOUT - currentTime )
                / (double)DEFAULT_SEARCH_TIMEOUT * (double)100 );
//          return the max of all these
            return Math.min( timeProgress, 100);
        }
    }
    
    public IntObj getProgressObj()
    {
        int progress = getProgress();
        if ( progressObj.value != progress )
        {
            progressObj.value = progress;
        }
        return progressObj;
    }

    /**
     * Returns the query hit count.
     */
    public int getQueryHitCount()
    {
        synchronized ( displayedQueryHitList )
        {
            return displayedQueryHitList.size();
        }
    }
    
    /**
     * Returns the query hit count object.
     */
    public IntObj getQueryHitCountObj()
    {
        synchronized ( displayedQueryHitList )
        {
            if ( queryHitCountObj.intValue() != displayedQueryHitList.size() )
            {
                queryHitCountObj.setValue( displayedQueryHitList.size() );
            }
            return queryHitCountObj;
        }
    }

    /**
     * Returns the query hit at the given index.
     */
    public RemoteFile getQueryHit( int index )
    {
        synchronized ( displayedQueryHitList )
        {
            if ( index < 0 || index >= displayedQueryHitList.size() )
            {
                return null;
            }
            return (RemoteFile) displayedQueryHitList.get( index );
        }
    }

    /**
     * Returns the query hits at the given indices.
     */
    public RemoteFile[] getQueryHits( int[] indices )
    {
        synchronized ( displayedQueryHitList )
        {
            RemoteFile[] results = new RemoteFile[ indices.length ];
            for ( int i = 0; i < indices.length; i++ )
            {
                results[i] = (RemoteFile)displayedQueryHitList.get( indices[i] );
            }
            return results;
        }
    }
    
    public void removeQueryHit( RemoteFile remoteFile )
    {
        queryHitList.remove( remoteFile );
        displayedQueryHitList.remove( remoteFile );
    }
    
    public void filterQueryHit( RemoteFile remoteFile )
    {
        displayedQueryHitList.remove( remoteFile );
    }

    /**
     * Returns the number of hits that gets locally filtered because:
     * - the file size is out of bounds
     * - the search string contains a filtered term
     * - the media type does not fit.
     */
    public int getLocallyFilteredCount()
    {
        synchronized ( queryHitList )
        {
            synchronized ( displayedQueryHitList )
            {
                return queryHitList.size() - displayedQueryHitList.size();
            }
        }
    }
   
    public boolean isSearching()
    {
        return isSearching;
    }
    
    public void checkForSearchTimeout( long currentTime )
    {
        if ( queryEngine != null )
        {
            if ( queryEngine.isQueryFinished() )
            {
                stopSearching();
            }
        }
        else if ( currentTime > startTime + DEFAULT_SEARCH_TIMEOUT )
        {
            // timed out stop search
            stopSearching();
        }
    }

    public void startSearching()
    {
        AsynchronousDispatcher.invokeLater( new Runnable()
        {
            public void run()
            {
                startTime = System.currentTimeMillis();
                // set the creation time just before we send the query this
                // will prevent the query to timeout before it could be send
                queryMsg.setCreationTime( startTime );
                Logger.logMessage( Logger.FINER, Logger.SEARCH,
                    "Sending Query " + queryMsg );
                queryEngine = QueryManager.getInstance().sendMyQuery( queryMsg );
                isSearching = true;
                fireSearchStarted();
            }
        } );
    }

    public void stopSearching()
    {
        if ( !isSearching )
        {// already stoped
            return;
        }
        isSearching = false;
        if ( queryEngine != null )
        {
            queryEngine.stopQuery();
        }
        fireSearchStoped();
    }


    public void processResponse( QueryResponseMsg msg )
    {
        //we like to receive results even if the query was stopped already.
        
        // check if it is a response for this query?
        if (!msg.getHeader().getMsgID().equals( queryMsg.getHeader().getMsgID()))
        {
            return;
        }

        // remoteHost.log("Got response to my query.  " + msg);
        long speed = msg.getRemoteHostSpeed();
        GUID rcID = msg.getRemoteClientID();
        DestAddress address = msg.getDestAddress();

        QueryHitHost qhHost = new QueryHitHost( rcID, address, speed );
        qhHost.setQHDFlags( msg.getPushNeededFlag(), msg.getServerBusyFlag(),
            msg.getHasUploadedFlag(), msg.getUploadSpeedFlag() );
        qhHost.setQueryResponseFields( msg );
        
        int hostRating = qhHost.getHostRating();

        QueryResponseRecord rec;
        RemoteFile rfile;

        int addStartIdx = queryHitList.size();
        int recordCount = msg.getRecordCount();
        ArrayList newHitList = new ArrayList( recordCount );
        for (int i = 0; i < recordCount; i++)
        {
            rec = msg.getMsgRecord(i);

            // verify record when using a urn query
            // this acts like a filter but there seem to be no need to make this
            // not permanet...
            if ( searchURN != null && rec.getURN() != null )
            {
                if ( !searchURN.equals( rec.getURN() ) )
                {
                    continue;
                }
            }

            synchronized( displayedQueryHitList )
            {
                boolean isRecFiltered = false;
                
                long fileSize = rec.getFileSize();
                String filename = rec.getFilename();
                
                if ( searchFilter != null )
                {
                    isRecFiltered = searchFilter.isFiltered(
                        fileSize, filename, speed, hostRating );
                }

                if ( isRecFiltered && isPermanentlyFiltered )
                {
                    continue;
                }
                
                URN urn = rec.getURN();
                int fileIndex = rec.getFileIndex();
                String metaData = rec.getMetaData();
                short score = Search.calculateSearchScore( searchString, filename );
                
                // find duplicate from same host...
                RemoteFile availableHit = findQueryHit( qhHost, urn, filename,
                    fileSize, fileIndex );
                
                if ( availableHit != null )
                {
                    // update availableHit
                    availableHit.updateQueryHitHost( qhHost );
                    availableHit.setMetaData( metaData );
                }
                else
                {
                    String pathInfo = rec.getPathInfo();
                    rfile = new RemoteFile( qhHost, fileIndex, filename, pathInfo,
                        fileSize, urn, metaData, score );
                    queryHitList.add( rfile );
                    if ( !isRecFiltered )
                    {
                        displayedQueryHitList.add( rfile );
                    }
                    newHitList.add( rfile );
                }
                // handle possible AlternateLocations
                DestAddress[] alternateLocations = rec.getAlternateLocations();
                if ( urn != null && alternateLocations != null)
                {
                    for ( int j = 0; j < alternateLocations.length; j++ )
                    {
                        // find duplicate from same host...
                        QueryHitHost qhh = new QueryHitHost( null, alternateLocations[j], -1 );
                        
                        availableHit = findQueryHit( qhHost, urn, filename, fileSize, fileIndex );
                        if ( availableHit != null )
                        {
                            // update availableHit
                            availableHit.updateQueryHitHost( qhHost );
                            availableHit.setMetaData( metaData );
                        }
                        else
                        {
                            rfile = new RemoteFile( qhh, -1, filename, "", fileSize, urn, metaData, score );
                            queryHitList.add( rfile );
                            if ( !isRecFiltered )
                            {
                                displayedQueryHitList.add( rfile );
                            }
                            newHitList.add( rfile );
                        }
                    }
                }
            }
        }
        int addEndIdx = queryHitList.size();
        // if something was added...
        if ( addEndIdx > addStartIdx || newHitList.size() > 0 )
        {
            if ( queryEngine != null )
            {
                queryEngine.incrementResultCount( newHitList.size() );
            }
            Object[] newHits = newHitList.toArray();
            fireSearchHitsAdded( addStartIdx, addEndIdx, newHits );
        }
    }

    /**
     * This methods calculates the score of a search result. The return value is
     * between 0 and 100. A value of 100 means all terms of the search string
     * are matched 100% in the result string.
     */
    public static short calculateSearchScore( String searchStr, String resultStr )
    {
        double tokenCount = 0;
        double hitCount = 0;
        StringTokenizer tokens = new StringTokenizer( searchStr );
        SearchEngine searchEngine = new SearchEngine();
        searchEngine.setText(resultStr, false);
        while ( tokens.hasMoreTokens() )
        {
            String token = tokens.nextToken();
            tokenCount ++;
            searchEngine.setPattern( token, false );
            if ( searchEngine.match() )
            {
                hitCount ++;
            }
        }
        double perc = hitCount / tokenCount * 100;
        return (short) perc;
    }

    /**
     * Trys to find a query hit in the search results. It will first check for
     * hostGUID and URN if no URN is provided it will use fileName, fileSize and
     * fileIndex to indentify a file.
     * If not query hit is found null is returned.
     * @param hostGUID the host GUID to look for.
     * @param urn the host URN to look for.
     * @param fileName The file name to look for if no URN is provided.
     * @param fileSize The file size to look for if no URN is provided.
     * @param fileIndex The file index to look for if no URN is provided.
     * @return The RemoteFile if found or null otherwise.
     */
    private RemoteFile findQueryHit( QueryHitHost qhh, URN urn, String fileName,
        long fileSize, int fileIndex  )
    {
        GUID fileHostGUID;
        GUID hostGUID = qhh.getHostGUID();
        DestAddress hostAddress = qhh.getHostAddress();
        
        synchronized( displayedQueryHitList )
        {
            int size = queryHitList.size();
            for ( int i = 0; i < size; i++ )
            {
                RemoteFile file = (RemoteFile)queryHitList.get( i );
                
                fileHostGUID = file.getQueryHitHost().getHostGUID();
                // first try by comparing GUIDs if possible
                if ( fileHostGUID != null && hostGUID != null )
                {
                    if ( !fileHostGUID.equals( hostGUID ) )
                    {
                        continue;
                    }
                }
                else
                {// now try by comparing IP:port
                    DestAddress fileHostAddress = file.getQueryHitHost().getHostAddress();
                    if ( !fileHostAddress.equals( hostAddress ) )
                    {
                        continue;
                    }
                }
                
                if ( urn != null && file.getURN() != null )
                {
                    if ( urn.equals( file.getURN() ) )
                    {
                        return file;
                    }
                }
                else
                {
                    if ( fileIndex == file.getFileIndex() &&
                         fileSize == file.getFileSize() &&
                         fileName.equals( file.getFilename() ) )
                    {
                        return file;
                    }
                }
            }
        }
        return null;
    }

    private void updateFilteredQueryList()
    {
        synchronized( displayedQueryHitList )
        {
            displayedQueryHitList.clear();
            Iterator iterator = queryHitList.iterator();
            while( iterator.hasNext() )
            {
                RemoteFile host = (RemoteFile)iterator.next();
                boolean isFiltered = false;
                if ( searchFilter != null )
                {
                    isFiltered = searchFilter.isFiltered( host.getFileSize(),
                        host.getFilename(), host.getSpeed(),
                        host.getQueryHitHost().getHostRating() );
                }
                if ( !isFiltered )
                {
                    displayedQueryHitList.add( host );
                }
            }
            fireSearchFiltered();
        }
    }
    
    public String toString()
    {
        return "[Search:" + searchString + "," + super.toString() + "]";
    }

    ///////////////////// START event handling methods ////////////////////////

    public void addSearchChangeListener( SearchChangeListener listener )
    {
        listenerList.add( listener );
    }

    public void removeSearchChangeListener( SearchChangeListener listener )
    {
        listenerList.remove( listener );
    }
    
    public void addSearchDataListener( SearchDataListener listener )
    {
        listenerList.add( listener );
    }

    public void removeSearchDataListener( SearchDataListener listener )
    {
        listenerList.remove( listener );
    }

    protected void fireSearchStarted()
    {
        SearchChangeEvent searchChangeEvent =
            new SearchChangeEvent( this, SearchChangeEvent.SEARCH_STARTED );
        fireSearchChangeEvent( searchChangeEvent );
    }

    protected void fireSearchStoped()
    {
        SearchChangeEvent searchChangeEvent =
            new SearchChangeEvent( this, SearchChangeEvent.SEARCH_STOPED );
        fireSearchChangeEvent( searchChangeEvent );
    }

    protected void fireSearchFiltered()
    {
        SearchChangeEvent searchChangeEvent =
            new SearchChangeEvent( this, SearchChangeEvent.SEARCH_FILTERED );
        fireSearchChangeEvent( searchChangeEvent );
    }
    
    public void fireSearchChanged()
    {
        SearchChangeEvent searchChangeEvent =
            new SearchChangeEvent( this, SearchChangeEvent.SEARCH_CHANGED );
        fireSearchChangeEvent( searchChangeEvent );
    }

    protected void fireSearchHitsAdded( int startIdx, int endIdx, Object[] newHits )
    {
        SearchChangeEvent searchChangeEvent = new SearchChangeEvent( this,
            SearchChangeEvent.SEARCH_HITS_ADDED, startIdx, endIdx );
        fireSearchChangeEvent( searchChangeEvent );
        
        SearchDataEvent dataEvent = new SearchDataEvent( this, SearchDataEvent.SEARCH_HITS_ADDED,
            newHits );
        fireSearchDataEvent( dataEvent );
    }

    private void fireSearchChangeEvent( final SearchChangeEvent searchChangeEvent )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                SearchChangeListener listener;

                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    if ( listeners[ i ] instanceof SearchChangeListener )
                    {
                        listener = (SearchChangeListener)listeners[ i ];
                        listener.searchChanged( searchChangeEvent );
                    }
                    
                }
            }
        });
    }
    
    private void fireSearchDataEvent( final SearchDataEvent searchDataEvent )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                SearchDataListener listener;

                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    if ( listeners[ i ] instanceof SearchDataListener )
                    {
                        listener = (SearchDataListener)listeners[ i ];
                        listener.searchDataChanged( searchDataEvent );
                    }
                    
                }
            }
        });
    }
    
    ///////////////////// END event handling methods ////////////////////////
}