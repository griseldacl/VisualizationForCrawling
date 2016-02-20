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
import phex.download.*;
import phex.download.swarming.*;
import phex.event.*;

public class ResearchSetting
{
    private long lastResearchStartTime;

    /**
     * The count of research that didn't return any new results.
     */
    private int noNewResultsCount;
    private int totalResearchCount;

    /**
     * The term to search for.
     */
    private String searchTerm;

    private Search search;

    /**
     * When the search has new results. This flag is true.
     */
    private boolean hasNewSearchResults;


    // Since currently the only one how uses the research setting is the
    // download file this solution is ok... later we need to find a different
    // way
    private SWDownloadFile downloadFile;
    private ResearchChangeListener researchChangeListener;

    public ResearchSetting( SWDownloadFile file )
    {
        downloadFile = file;
        researchChangeListener = new ResearchChangeListener();
    }

    public long getLastResearchStartTime()
    {
        return lastResearchStartTime;
    }

    public void setLastResearchStartTime( long time )
    {
        lastResearchStartTime = time;
    }

    public int getNoNewResultsCount()
    {
        return noNewResultsCount;
    }

    public String getSearchTerm()
    {
        return searchTerm;
    }

    public void setSearchTerm( String term )
    {
        searchTerm = term;
    }

    public String getSHA1()
    {
        URN searchURN = downloadFile.getFileURN();
        if ( searchURN == null || !searchURN.isSha1Nid() )
        {
            return "";
        }
        return searchURN.getNamespaceSpecificString();
    }

    public void startSearch( long searchTimeout )
    {
        if ( search != null && search.isSearching() )
        {
            return;
        }

        if ( searchTerm.length() < Cfg.MIN_SEARCH_TERM_LENGTH &&
            downloadFile.getFileURN() == null)
        {
            return;
        }
        hasNewSearchResults = false;
        BackgroundSearchContainer backgroundSearchContainer =
            QueryManager.getInstance().getBackgroundSearchContainer();
        // Since Limewire is not adding urns to QRT anymore URN queries even with
        // string turn out to not work very good.. therefore we are not trying
        // urn queries if we have a decent search term available.
        URN queryURN = null;
        if ( searchTerm.length() < Cfg.MIN_SEARCH_TERM_LENGTH 
             && downloadFile.getFileURN() != null )
        {
            queryURN = downloadFile.getFileURN();
        }
        search = backgroundSearchContainer.createSearch( searchTerm,
            queryURN, downloadFile.getTotalDataSize(),
            downloadFile.getTotalDataSize(), searchTimeout );
        search.addSearchChangeListener( researchChangeListener );
        totalResearchCount ++;
        long currentTime = System.currentTimeMillis();
        lastResearchStartTime = currentTime;
    }

    public int getTotalResearchCount()
    {
        return totalResearchCount;
    }

    public void stopSearch()
    {
        if ( search == null || !search.isSearching() )
        {
            return;
        }
        search.stopSearching();
    }

    public int getSearchHitCount()
    {
        return search.getQueryHitCount();
    }
    
    public int getSearchProgress()
    {
        return search.getProgress();
    }

    public boolean isSearchRunning()
    {
        if ( search == null )
        {
            return false;
        }
        return search.isSearching();
    }

    public class ResearchChangeListener implements SearchChangeListener
    {
        public void searchChanged( final SearchChangeEvent event )
        {
            AsynchronousDispatcher.invokeLater( new Runnable()
            {
                public void run()
                {
                    // after search has stoped check if we found any thing.
                    if ( event.getType() == SearchChangeEvent.SEARCH_STOPED )
                    {
                        if ( hasNewSearchResults == false )
                        {   // no new results...
                            noNewResultsCount ++;
                        }
                        else
                        {
                            noNewResultsCount = 0;
                        }
                    }

                    if ( event.getType() != SearchChangeEvent.SEARCH_HITS_ADDED )
                    {
                        SwarmingManager.getInstance().fireDownloadFileChanged(
                            downloadFile );
                        return;
                    }

                    // Adds a file from a backgroundsearch to the candidates list.
                    Search search = (Search) event.getSource();
                    int startIdx = event.getStartIndex();
                    int endIdx = event.getEndIndex();
                    for ( int i = startIdx; i < endIdx; i++ )
                    {
                        RemoteFile rFile = search.getQueryHit( i );
                        boolean isAdded = downloadFile.addDownloadCandidate( rFile );
                        if ( isAdded )
                        {
                            hasNewSearchResults = true;
                        }
                    }
                    SwarmingManager.getInstance().fireDownloadFileChanged(
                        downloadFile );
                }
            });
        }
    }
}