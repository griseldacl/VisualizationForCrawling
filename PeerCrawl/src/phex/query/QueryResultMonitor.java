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
 *  $Id: QueryResultMonitor.java,v 1.13 2005/11/04 20:43:36 gregork Exp $
 */
package phex.query;

import java.util.ArrayList;

import phex.common.ServiceManager;
import phex.common.URN;
import phex.common.address.DestAddress;
import phex.download.RemoteFile;
import phex.download.swarming.SWDownloadFile;
import phex.download.swarming.SwarmingManager;
import phex.event.AsynchronousDispatcher;
import phex.event.SearchDataEvent;
import phex.event.SearchDataListener;
import phex.msg.GUID;
import phex.msg.QueryResponseMsg;
import phex.msg.QueryResponseRecord;

/**
 * This class monitors query results going through the node.
 * The data is used to look into query results and trying to find useful
 * candidates for itself.
 */
public class QueryResultMonitor
{
    /**
     * The list of query hits returned by the query. Contains the RemoteFile
     * objects.
     */
    private ArrayList queryHitList;
    
    /**
     * The passive search filter to look for.
     */
    private SearchFilter searchFilter;
    
    /**
     * All listeners interested in events of this monitor.
     */
    private ArrayList listenerList;
    
    public QueryResultMonitor()
    {
        queryHitList = new ArrayList();
        listenerList = new ArrayList( 2 );
    }
    
    /**
     * Updates the used search filter and forces a filtering of the query hits. 
     * @param aSearchFilter the search filter to use.
     */
    public void updatePassiveSearchFilter( SearchFilter aSearchFilter )
    {
        synchronized( queryHitList )
        {
            searchFilter = aSearchFilter;
            if ( searchFilter != null )
            {
                searchFilter.setLastTimeUsed( System.currentTimeMillis() );
            }
        }
    }
    
    /**
     * 
     */
    public SearchFilter getPassiveSearchFilter()
    {
        return searchFilter;
    }
    
    /**
     * <p>Check a MsgQueryResponse against the current passive search criteria.</p>
     *
     * <p>If the search criteria was empty then all query response messages are
     * captured. If the response lists a file that has not been seen before, the
     * file will be added to the value of getPassiveResults(), and the
     * monitorSearchListener will be informed of a passive hit by invoking
     * passiveResultArrived().</p>
     *
     * @param  the MsgQueryResponse to check for passive hits
     */
    public void processResponse( QueryResponseMsg msg )
    {
        SwarmingManager swarmingMgr = SwarmingManager.getInstance();
        
        long speed = msg.getRemoteHostSpeed();
        GUID rcID = msg.getRemoteClientID();
        DestAddress address = msg.getDestAddress();

        QueryHitHost qhHost = new QueryHitHost( rcID, address, speed );
        qhHost.setQHDFlags( msg.getPushNeededFlag(), msg.getServerBusyFlag(),
            msg.getHasUploadedFlag(), msg.getUploadSpeedFlag() );
        qhHost.setQueryResponseFields( msg );
        
        QueryResponseRecord rec;
        int addStartIdx = queryHitList.size();
        ArrayList newHitList = new ArrayList( );
        int recordCount = msg.getRecordCount();
        for (int i = 0; i < recordCount; i++)
        {
            rec = msg.getMsgRecord(i);
            
            if( ServiceManager.sCfg.enableHitSnooping )
            {
                snoopDownloadCandidates( swarmingMgr, qhHost, rec );
            }
            
            if ( searchFilter != null )
            {
                monitorPassiveSearch( qhHost, rec, newHitList );
            }
        } // for
        
        int addEndIdx = queryHitList.size();
        // if something was added...
        if ( addEndIdx > addStartIdx || newHitList.size() > 0 )
        {
            Object[] newHits = newHitList.toArray();
            fireSearchHitsAdded( addStartIdx, addEndIdx, newHits );
        }
    }

    private void monitorPassiveSearch( QueryHitHost qhHost, QueryResponseRecord rec,
        ArrayList newHitList )
    {
        synchronized( queryHitList )
        {
            int speed = qhHost.getHostSpeed();
            int hostRating = qhHost.getHostRating();
            long fileSize = rec.getFileSize();
            String filename = rec.getFilename();
            boolean isFiltered = searchFilter.isFiltered( fileSize, filename, 
                speed, hostRating );
            
            if ( isFiltered )
            {
                return;
            }
            
            URN urn = rec.getURN();
            int fileIndex = rec.getFileIndex();
            String metaData = rec.getMetaData();
            
            // find duplicate from same host...
            RemoteFile availableHit = findQueryHit( qhHost, urn, filename,
                fileSize, fileIndex );
            short score = Search.calculateSearchScore(
                searchFilter.getFilterString(), filename );
            if ( availableHit != null )
            {
                // update availableHit
                availableHit.updateQueryHitHost( qhHost );
                availableHit.setMetaData( metaData );
            }
            else
            {
                String pathInfo = rec.getPathInfo();
                RemoteFile rfile = new RemoteFile( qhHost, fileIndex, filename, pathInfo,
                    fileSize, urn, metaData, score );
                queryHitList.add( rfile );
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
                    
                    availableHit = findQueryHit( qhHost, urn, filename, fileSize,
                        fileIndex );
                    if ( availableHit != null )
                    {
                        // update availableHit
                        availableHit.updateQueryHitHost( qhHost );
                        availableHit.setMetaData( metaData );
                    }
                    else
                    {
                        RemoteFile rfile = new RemoteFile( qhh, -1, filename, "",
                            fileSize, urn, metaData, score );
                        queryHitList.add( rfile );
                        newHitList.add( rfile );
                    }
                }
            }
        }
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
        
        synchronized( queryHitList )
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

    private QueryHitHost snoopDownloadCandidates( SwarmingManager swarmingMgr,
        QueryHitHost qhHost, QueryResponseRecord rec)
    {
        SWDownloadFile swdlf = swarmingMgr.getDownloadFile( rec.getFileSize(),
            rec.getURN() );
        if ( swdlf != null )
        {// add record as candidate...
            RemoteFile rFile = new RemoteFile( qhHost, rec.getFileIndex(),
                rec.getFilename(), rec.getPathInfo(), rec.getFileSize(),
                rec.getURN(), rec.getMetaData(), (short) -1 );
            swdlf.addDownloadCandidate(rFile);
        }
        return qhHost;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    //// Event Handling
    ////////////////////////////////////////////////////////////////////////////
    
    public void addSearchDataListener( SearchDataListener listener )
    {
        listenerList.add( listener );
    }

    public void removeSearchDataListener( SearchDataListener listener )
    {
        listenerList.remove( listener );
    }
    
    protected void fireSearchHitsAdded( int startIdx, int endIdx, Object[] newHits )
    {        
        SearchDataEvent dataEvent = new SearchDataEvent( this,
            SearchDataEvent.SEARCH_HITS_ADDED, newHits );
        fireSearchDataEvent( dataEvent );
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
}
