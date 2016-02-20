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
 *  $Id: SWDownloadFile.java,v 1.142 2005/11/13 22:22:43 gregork Exp $
 */

package phex.download.swarming;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.lang.time.DateUtils;

import phex.common.*;
import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.common.address.MalformedDestAddressException;
import phex.common.bandwidth.BandwidthController;
import phex.common.bandwidth.BandwidthManager;
import phex.common.file.FileManager;
import phex.common.file.ManagedFile;
import phex.common.file.ManagedFileException;
import phex.download.*;
import phex.download.strategy.ScopeSelectionStrategy;
import phex.download.strategy.ScopeSelectionStrategyProvider;
import phex.event.*;
import phex.http.HTTPRangeSet;
import phex.query.ResearchSetting;
import phex.query.Search;
import phex.statistic.SimpleStatisticProvider;
import phex.statistic.StatisticsManager;
import phex.utils.*;
import phex.xml.*;

/**
 * 
 */
public class SWDownloadFile implements TransferDataProvider, SWDownloadConstants
{   
    private static Random random = new Random();
    
    /**
     * A list of missing download scopes.
     */
    private DownloadScopeList missingScopeList;
    
    /**
     * A list of download scopes currently blocked in downloads.
     */
    private DownloadScopeList blockedScopeList;
    
    /**
     * A list of finished download scopes.
     */
    private DownloadScopeList finishedScopeList;
    
    /**
     * This list contains rated download scopes representing the availability
     * of scopes from candidates.
     */
    private RatedDownloadScopeList ratedScopeList;
    
    /**
     * The last time the rated download scope list was build.
     */
    private long ratedScopeListBuildTime;
    
    /**
     * Alternate location container which holds all correctly validate alt locs.
     * A validated alt loc is one which was prooved to connect correctly during
     * the running session.
     */
    private AlternateLocationContainer goodAltLocContainer;
    
    /**
     * Alternate location container which holds all bad alt locs. A bad alt loc
     * is one which was prooved to not be reachable during the running session.
     */
    private AlternateLocationContainer badAltLocContainer;
    
    /**
     * A lock object used to lock access to good and bad download candidate lists.
     */
    private Object candidatesLock = new Object();
    
    /**
     * A list of all download candidates. On access candidatesLock should be locked. 
     */
    private ArrayList allCandidatesList;
    
    /**
     * A list of good download candidates for this download file. Good 
     * candidates are candidates that have been verified to be available,
     * or have been available in the last 24h after a restart of Phex.
     * 
     * 60% of connect tries will go to good candidates. When no good
     * candidates are available we try medium candidates.
     * 
     * On access candidatesLock should be locked.
     */
    private ArrayList goodCandidatesList;
    
    /**
     * A list of medium download candidates for this download file. Medium 
     * candidates are candidates that have not been verified to be available and
     * have not reached the bad candidates requirements yet.
     * 
     * 30% of connect tries will go to medium candidates. When no medium
     * candidates are available we try bad candidates.
     * 
     * On access candidatesLock should be locked.
     */
    private ArrayList mediumCandidatesList;
     
    /**
     * A list of known bad download candidates for this download file. Most 
     * likley they are offline. A candidate reaches bad candidate status if it
     * has failed to connect more then 3 times or is a ignored candidate.
     * 
     * Bad candidates will not be retried for a long time, they have a high
     * error status timeout. Only 10% of connect tries will go to bad
     * candidates.
     * 
     * On access candidatesLock should be locked.
     */
    private ArrayList badCandidatesList;
    
    /**
     * The index of the good candidate which is tried next.
     */
    private int goodCandidatePosition = 0;
    
    /**
     * The index of the good candidate which is tried next.
     */
    private int mediumCandidatePosition = 0;
    
    /**
     * The index of the good candidate which is tried next.
     */
    private int badCandidatePosition = 0;
    
    /**
     * A list of download candidates that are currently in status:
     * STATUS_CANDIDATE_CONNECTING
     * STATUS_CANDIDATE_PUSH_REQUEST
     * STATUS_CANDIDATE_REMOTLY_QUEUED
     * STATUS_CANDIDATE_REQUESTING
     * STATUS_CANDIDATE_DOWNLOADING
     * On access candidatesLock should be locked.
     */
    private ArrayList transferCandidatesList;
    
    /**
     * A list of download candidates that are queued.
     * On access candidatesLock should be locked.
     */
    private Set queuedCandidatesSet;
    
    /**
     * A hash map of candidate worker associations. They represent from worker 
     * allocated download candidates.
     * Modifing access needs to be locked with candidatesLock
     */
    private LinkedMap/*<SWDownloadCandidate,SWDownloadWorker>*/
        allocatedCandidateWorkerMap;
    

    /**
     * A cached buffer object that contains the candidate count.
     * The value is not always up to date and only validated and updated when
     * calling getCandidateCountObject();
     */
    private IntObj candidateCountObj;
    private int downloadingCandidateCount;
    private int queuedCandidateCount;
    private int connectingCandidateCount;

    /**
     * Indicates the last update of downloadingCandidateCount. It should be
     * updated every 2 seconds (CANDIDATE_WORKER_COUNT_TIMEOUT).
     */
    private long lastCandidateWorkerCountUpdate;
    private static final int CANDIDATE_WORKER_COUNT_TIMEOUT = 2 * 1000;

    /**
     * The file size of the download.
     */
    private long fileSize;

    /**
     * The destination file of the finished download. The path leads to the
     * directory where the file is stored after the download is completed. This
     * must not always be the download directory for temporary files.
     */
    private File destinationFile;
    
    /**
     * The incomplete file of the download. 
     */
    private File incompleteFile;
    
    private String destinationPrefix;
    private String destinationSuffix;
    private short destinationType;
    public static final short DESTTYPE_UNKNOWN = 0;
    public static final short DESTTYPE_STREAMABLE = 1;
    public static final short DESTTYPE_UNSTREAMABLE = 2;


    /**
     * The time indicating when the download file was created for download.
     */
    private Date createdDate;

    /**
     * The time indicating the last modification (download) of the download file.
     */
    private Date modifiedDate;

    /**
     * The status if the download.
     */
    private short status;

    /**
     * Transfer start time
     */
    private long transferStartTime;

    /**
     * Transfer stop time
     */
    private long transferStopTime;

    /**
     * Defines the length already downloaded, this is lazy collected from
     * segments when the download is not completed
     */
    private long transferredDataSize;

    /**
     * The time after which the transfer data needs to be updated
     */
    private long transferDataUpdateTime;

    /**
     * Used to store the current progress.
     */
    private Integer currentProgress;

    /**
     * The number of workers currently working on downloading this file
     */
    private short workerCount;

    /**
     * The URN of the download. This is the most unique identifier of the file
     * network wide. If it is none null we should only accept candidates with
     * the same URN and also add this urn to researchs for better results.
     */
    private URN fileURN;
    
    private URI downloadURI;

    /**
     * Settings for the research.
     */
    private ResearchSetting researchSetting;

    /*
     * If preview mode is selected, this is the size at the start of the file to prefer.
     */
    private long previewSize;
    
    /**
     * The download file own bandwidth controller.
     */
    private BandwidthController bandwidthController;
    
    private ScopeSelectionStrategy scopeSelectionStrategy;
    
    private SWDownloadFile()
    {
        allCandidatesList = new ArrayList();
        goodCandidatesList = new ArrayList();
        mediumCandidatesList = new ArrayList();
        badCandidatesList = new ArrayList();
        transferCandidatesList = new ArrayList();
        queuedCandidatesSet = new HashSet();
        allocatedCandidateWorkerMap = new LinkedMap();
        candidateCountObj = new IntObj( 0 );
        downloadingCandidateCount = 0;
        queuedCandidateCount = 0;
        connectingCandidateCount = 0;
        lastCandidateWorkerCountUpdate = 0;
        currentProgress = new Integer( 0 );
        createdDate = modifiedDate = new Date( System.currentTimeMillis() );
        
        status = STATUS_FILE_WAITING;
        bandwidthController = BandwidthController.acquireBandwidthController(
            "DownloadFile-"+toString(), Long.MAX_VALUE );
        bandwidthController.activateShortTransferAvg( 1000, 15 );
        bandwidthController.linkControllerIntoChain(
            BandwidthManager.getInstance().getDownloadBandwidthController() );
    }
    
    public SWDownloadFile( String fullLocalFilename, String searchString,
        long aFileSize, URN aFileURN )
    {
        this();
        initialize(fullLocalFilename, aFileURN, aFileSize, searchString, true);
        try
        {
            initIncompleteFile();
        }
        catch ( FileHandlingException exp )
        {
            NLogger.error( NLoggerNames.Download, exp, exp );
        }
    }
    
    /**
     *
     */
    public SWDownloadFile( URI downloadUri )
        throws URIException
    {
        this();
        this.downloadURI = downloadUri;
        
        String protocol = downloadURI.getScheme();
        if ( "magnet".equals( protocol ) )
        {
            MagnetData magnetData = MagnetData.parseFromURI(downloadURI);
            
            URN urn = MagnetData.lookupSHA1URN(magnetData);
            String fileName = MagnetData.lookupFileName( magnetData );

            String fullfileName = ServiceManager.sCfg.mDownloadDir + File.separator + 
                FileUtils.convertToLocalSystemFilename( fileName );
            String searchTerm;
            if ( magnetData.getKeywordTopic() != null )
            {
                searchTerm = magnetData.getKeywordTopic();
            }
            else
            {
                searchTerm = StringUtils.createNaturalSearchTerm( 
                    MagnetData.lookupSearchName(magnetData) );
            }
            
            initialize(fullfileName, urn, UNKNOWN_FILE_SIZE, searchTerm, true);
            try
            {
                initIncompleteFile();
            }
            catch ( FileHandlingException exp )
            {
                NLogger.error( NLoggerNames.Download, exp, exp );
            }
            
            List urlList = MagnetData.lookupHttpURIs(magnetData);
            Iterator iterator = urlList.iterator();
            while( iterator.hasNext() )
            {
                URI uri = (URI) iterator.next();
                String host = uri.getHost();
                int port = uri.getPort();
                if ( port == -1 )
                { 
                    port = 80;
                }
                DestAddress address = new DefaultDestAddress( host, port );
                SWDownloadCandidate candidate = new SWDownloadCandidate( address,
                    uri, this );
                addDownloadCandidate( candidate );
            }
            
            // fire of a search in case this is a magnet download to get sources.
            if ( urn != null || getCandidatesCount() == 0 )
            {
                startSearchForCandidates();
            }
        }
        else
        {
            String fileName = URLUtil.getFileNameFromUri( downloadURI );
            String fullfileName = ServiceManager.sCfg.mDownloadDir 
                + File.separator + FileUtils.convertToLocalSystemFilename(
                fileName );
            String searchTerm = StringUtils.createNaturalSearchTerm( fileName );
            initialize(fullfileName, null, UNKNOWN_FILE_SIZE, searchTerm, true);
            try
            {
                initIncompleteFile();
            }
            catch ( FileHandlingException exp )
            {
                NLogger.error( NLoggerNames.Download, exp, exp );
            }
            
            String host = downloadURI.getHost();
            if ( host != null )
            {
                int port = downloadURI.getPort();
                if ( port == -1 )
                { 
                    port = 80;
                }
                DestAddress address = new DefaultDestAddress( host, port );
                SWDownloadCandidate candidate = new SWDownloadCandidate( address,
                    downloadURI, this );
                addDownloadCandidate( candidate );
            }
        }
    }
    
    /**
     * @param xjbFile
     */
    public SWDownloadFile( XJBSWDownloadFile xjbFile )
    {
        this();
        URN fileUrn = null;
        if ( xjbFile.getFileURN() != null )
        {
            fileUrn = new URN( xjbFile.getFileURN() );
        }
        
        initialize( ServiceManager.sCfg.mDownloadDir + File.separator
            + xjbFile.getLocalFileName(), fileUrn, xjbFile.getFileSize(),
            xjbFile.getSearchTerm(), false );
        
        String incompleteFileName = xjbFile.getIncompleteFileName();
        if ( !StringUtils.isEmpty( incompleteFileName ) )
        {
            incompleteFile = new File( incompleteFileName );
        }
        
        setCreatedDate( new Date( xjbFile.getCreatedTime() ) );
        setDownloadedDate( new Date( xjbFile.getModifiedTime() ) );
        
        setScopeSelectionStrategy( ScopeSelectionStrategyProvider.getByClassName( 
            xjbFile.getScopeSelectionStrategy() ) );

        status = xjbFile.getStatus();
        
        createDownloadScopes( xjbFile );
        //createDownloadSegments( xjbFile );
        createDownloadCandidates( xjbFile );
        forceCollectionOfTransferData();

        verifyStatus();

        if ( isFileCompletedOrMoved() )
        {
            // set transferred data... this is lazy collected from
            // segments when the download is not completed
            transferredDataSize = fileSize;

            // in case some interrupted move occured... or some old downloads 
            // are still sitting there
            if ( isFileCompleted() )
            {
                if ( finishedScopeList.getAggregatedLength() == 0 )
                {
                    setStatus(STATUS_FILE_COMPLETED_MOVED);
                }
            }
        }
    }

    /** 
     * @param fullLocalFilename
     * @param aFileURN The URN of the file. This is the most unique identifier of the file
     * network wide. If it is none null we should only accept candidates with
     * the same URN and also add this urn to researchs for better results.
     * @param aFileSize
     * @param searchTerm
     * @param createSegments
     */
    private void initialize( String fullLocalFilename, URN aFileURN, long aFileSize,
        String searchTerm, boolean createSegments )
    {
        destinationFile = new File( fullLocalFilename );
        updateDestinationData();
        
        scopeSelectionStrategy = ScopeSelectionStrategyProvider.getAvailBeginRandSelectionStrategy();
        
        if ( aFileURN != null )
        {
            this.fileURN = aFileURN;
            initAltLocContainers();
        }
        
        fileSize = aFileSize;
        previewSize = fileSize / 10;
        
        researchSetting = new ResearchSetting( this );
        researchSetting.setSearchTerm( searchTerm );
        
        missingScopeList = new DownloadScopeList();
        blockedScopeList = new DownloadScopeList();
        finishedScopeList = new DownloadScopeList();
        
        if ( fileSize == UNKNOWN_FILE_SIZE )
        {
            missingScopeList.add( new DownloadScope( 0, Long.MAX_VALUE ) );
        }
        else
        {
            missingScopeList.add( new DownloadScope( 0, fileSize - 1 ) );
        }
    }
    
    public void setFileSize( long fileSize )
    {
        this.fileSize = fileSize;
        synchronized ( missingScopeList )
        {
            missingScopeList.remove( new DownloadScope( fileSize, Long.MAX_VALUE) );
        }
        synchronized ( blockedScopeList )
        {
            blockedScopeList.remove( new DownloadScope( fileSize, Long.MAX_VALUE) );
        }
        previewSize = fileSize / 10;
    }
    
    public boolean isDownloadFinished()
    {
        synchronized ( finishedScopeList )
        {
            return isFileCompletedOrMoved() ||
                finishedScopeList.getAggregatedLength() == fileSize;
        }
    }

    /**
     * Used to check if a scope is allocateable. This check is done early
     * before a connection to a candidate is opend. The actual allocation happens
     * after the connection to the candidate is established. Though it can happen
     * that all scops are already allocated until then.
     * @param candidateScopeList the ranges that are wanted for this download if
     *        set to null all ranges are allowed.
     * @return true if there is a scope available. false otherwise.
     */
    public boolean isScopeAllocateable( DownloadScopeList candidateScopeList )
    {
        synchronized( missingScopeList )
        {
            if ( missingScopeList.size() == 0 )
            {
                return false;
            }
            if ( fileSize != UNKNOWN_FILE_SIZE && candidateScopeList != null )
            {
                DownloadScopeList wantedScopeList = 
                    (DownloadScopeList) missingScopeList.clone();
                wantedScopeList.retainAll( candidateScopeList );
                return wantedScopeList.size() > 0;
            }
            else
            {
                return true;
            }
        }        
    }

    /**
     * Used to allocate and reserve a download candidate for a download worker.
     */
    public SWDownloadCandidate allocateDownloadCandidate( SWDownloadWorker worker )
    {
        // random from 0-9
        int val = random.nextInt(10);
        SWDownloadCandidate candidate = null;
        if ( val < 6 )
        {
            candidate = allocateGoodCandidate( worker );
            if ( candidate == null )
            {
                candidate = allocateMediumCandidate( worker );
            }
            if ( candidate == null )
            {
                candidate = allocateBadCandidate( worker );
            }
        }
        else if ( val < 9 )
        {
            candidate = allocateMediumCandidate( worker );
            if ( candidate == null )
            {
                candidate = allocateBadCandidate( worker );
            }
            if ( candidate == null )
            {
                candidate = allocateGoodCandidate( worker );
            }
        }        
        else
        {
            candidate = allocateBadCandidate( worker );
            if ( candidate == null )
            {
                candidate = allocateMediumCandidate( worker );
            }
            if ( candidate == null )
            {
                candidate = allocateGoodCandidate( worker );
            }
        }
        return candidate;
    }
    
    private SWDownloadCandidate allocateGoodCandidate( SWDownloadWorker worker )
    {
        SWDownloadCandidate candidate = null;
        synchronized( candidatesLock )
        {
            int numCandidates = goodCandidatesList.size();
            
            // return quickly if there are no candidates
            if ( numCandidates == 0 )
            {
                return null;
            }

            // sanity check on persistent index, because since the last call to this method,
            // the number of candidates may have decreased, leaving the persistent index
            // out of range.
            if ( goodCandidatePosition >= numCandidates )
            {
                goodCandidatePosition = 0;
            }

            // Iterate over candidates to find the next available
            for (int i=0; i < numCandidates; i++)
            {
                // currentIndex holds the index of the candidate that will be
                // checked for availability
                int currentIndex = i + goodCandidatePosition;
                if (currentIndex >= numCandidates)
                {
                    currentIndex -= numCandidates;
                }
                    
                candidate = (SWDownloadCandidate)goodCandidatesList.get(currentIndex);
                if ( candidate.isAbleToBeAllocated() &&
                     !allocatedCandidateWorkerMap.containsKey( candidate ) )
                {
                    NLogger.debug( NLoggerNames.Download_Candidate_Allocate,
                        "Allocating good candidate " + candidate + " from " + worker );
                    candidate.addToCandidateLog("Allocating as good candidate.");
                   // Sets the segment to be allocated by a worker.
                   allocatedCandidateWorkerMap.put( candidate, worker );
                   goodCandidatePosition = currentIndex + 1;
                   return candidate;
                }
            }
        }

        // No valid candidate found
        // Don't bother updating the persistent index, because it probably
        // doesn't matter where we begin the search from on the next call.
        return null;
    }
    
    private SWDownloadCandidate allocateMediumCandidate( SWDownloadWorker worker )
    {
        SWDownloadCandidate candidate = null;
        synchronized( candidatesLock )
        {
            int numCandidates = mediumCandidatesList.size();
            
            // return quickly if there are no candidates
            if ( numCandidates == 0 )
            {
                return null;
            }

            // sanity check on persistent index, because since the last call to this method,
            // the number of candidates may have decreased, leaving the persistent index
            // out of range.
            if ( mediumCandidatePosition >= numCandidates )
            {
                mediumCandidatePosition = 0;
            }

            // Iterate over candidates to find the next available
            for (int i=0; i < numCandidates; i++)
            {
                // currentIndex holds the index of the candidate that will be
                // checked for availability
                int currentIndex = i + mediumCandidatePosition;
                if (currentIndex >= numCandidates)
                {
                    currentIndex -= numCandidates;
                }
                    
                candidate = (SWDownloadCandidate)mediumCandidatesList.get(currentIndex);
                if ( candidate.isAbleToBeAllocated() &&
                     !allocatedCandidateWorkerMap.containsKey( candidate ) )
                {
                    NLogger.debug( NLoggerNames.Download_Candidate_Allocate,
                        "Allocating medium candidate " + candidate + " from " + worker );
                    candidate.addToCandidateLog("Allocating as medium candidate.");
                   // Sets the segment to be allocated by a worker.
                   allocatedCandidateWorkerMap.put( candidate, worker );
                   mediumCandidatePosition = currentIndex + 1;
                   return candidate;
                }
            }
        }

        // No valid candidate found
        // Don't bother updating the persistent index, because it probably
        // doesn't matter where we begin the search from on the next call.
        return null;
    }
    
    private SWDownloadCandidate allocateBadCandidate( SWDownloadWorker worker )
    {
        SWDownloadCandidate candidate = null;
        synchronized( candidatesLock )
        {
            int numCandidates = badCandidatesList.size();
            
            // return quickly if there are no candidates
            if ( numCandidates == 0 )
            {
                return null;
            }

            // sanity check on persistent index, because since the last call to this method,
            // the number of candidates may have decreased, leaving the persistent index
            // out of range.
            if ( badCandidatePosition >= numCandidates )
            {
                badCandidatePosition = 0;
            }

            // Iterate over candidates to find the next available
            for (int i=0; i < numCandidates; i++)
            {
                // currentIndex holds the index of the candidate that will be
                // checked for availability
                int currentIndex = i + badCandidatePosition;
                if (currentIndex >= numCandidates)
                {
                    currentIndex -= numCandidates;
                }
                    
                candidate = (SWDownloadCandidate)badCandidatesList.get(currentIndex);
                if ( candidate.isAbleToBeAllocated() &&
                     !allocatedCandidateWorkerMap.containsKey( candidate ) )
                {
                    NLogger.debug( NLoggerNames.Download_Candidate_Allocate,
                        "Allocating bad candidate " + candidate + " from " + worker );
                    candidate.addToCandidateLog("Allocating as bad candidate.");
                    // Sets the segment to be allocated by a worker.
                    allocatedCandidateWorkerMap.put( candidate, worker );
                    badCandidatePosition = currentIndex + 1;
                    return candidate;
                }
            }
        }

        // No valid candidate found
        // Don't bother updating the persistent index, because it probably
        // doesn't matter where we begin the search from on the next call.
        return null;
    }

    /**
     * Releases a allocated download segment.
     */
    public void releaseDownloadCandidate( SWDownloadCandidate candidate )
    {
        synchronized( candidatesLock )
        {
            SwarmingManager.getInstance().releaseCandidateIP( candidate );
            NLogger.debug( NLoggerNames.Download_Candidate_Allocate,
                "Release allocation " + candidate + "." );
            // Sets the segment to be not allocated by a worker.
            allocatedCandidateWorkerMap.remove( candidate );
        }
    }

    public boolean addDownloadCandidate( RemoteFile remoteFile )
    {
        SWDownloadCandidate candidate = new SWDownloadCandidate( remoteFile, this );
        return addDownloadCandidate( candidate );
    }

    public boolean addDownloadCandidate( AlternateLocation altLoc )
    {
        URN altLocURN = altLoc.getURN();
        if ( fileURN != null && !altLocURN.equals( fileURN ) )
        {
            NLogger.debug( NLoggerNames.Download_File,
                "AlternateLocation URN does not match!" );
            return false;
        }

        // use get request class to parse url
        DestAddress hostAddress = altLoc.getHostAddress();
        if ( hostAddress.isLocalHost() )
        {// dont add myself as candidate.
            return false;
        }
        SWDownloadCandidate candidate = new SWDownloadCandidate( hostAddress,
            0, null, altLocURN, this );
        return addDownloadCandidate( candidate );
    }

    

    protected boolean addDownloadCandidate( SWDownloadCandidate candidate )
    {
        URN candidateURN = candidate.getResourceURN();
        // update the urn of the file if null
        if ( fileURN == null && candidateURN != null )
        {
            fileURN = candidateURN;
            initAltLocContainers();
        }
        if ( ( fileURN != null && candidateURN != null )
            && !fileURN.equals( candidateURN ) )
        {// make sure URNs match!
            NLogger.debug( NLoggerNames.Download_File,
                "Candidate URN to add does not match!" );
            return false;
        }

        synchronized( candidatesLock )
        {
            if ( allCandidatesList.contains( candidate ) )
            {
                //NLogger.debug( NLoggerNames.DOWNLOAD,
                //    "Duplicate download candidate" );
                return false;
            }
            NLogger.debug( NLoggerNames.Download_File,
                "Adding download candidate " + candidate );
            int pos = allCandidatesList.size();
            allCandidatesList.add( candidate );
            fireDownloadCandidateAdded( pos );
            mediumCandidatesList.add( candidate );
        }

        SwarmingManager.getInstance().notifyWaitingWorkers();

        return true;
    }
    
    /**
     * Makes the given download candidate as a good candidate, in case it was a
     * bad one...
     * 
     * @param candidate the candidate to make good.
     */
    public void markCandidateGood( SWDownloadCandidate candidate )
    {
        if ( candidate == null )
        {
            throw new NullPointerException( "Candidate is null.");
        }
        synchronized( candidatesLock )
        {
            int pos = badCandidatesList.indexOf( candidate );
            if ( pos >= 0 )
            {
                // remove from bad...
                badCandidatesList.remove( pos );
            }
            pos = mediumCandidatesList.indexOf( candidate );
            if ( pos >= 0 )
            {
                // remove from medium...
                mediumCandidatesList.remove( pos );
            }
            
            // ...and add to good.
            if ( !goodCandidatesList.contains( candidate ) )
            {
                goodCandidatesList.add( candidate );
                NLogger.debug( NLoggerNames.Download_File,
                    "Moving candidate to good list: " + candidate.getHostAddress() );
                candidate.addToCandidateLog("Moving candidate to good list.");
            }
        }
    }
    
    /**
     * Makes the given download candidate as a medium candidate, in case it was a
     * good or bad one...
     * 
     * @param candidate the candidate to make medium.
     */
    public void markCandidateMedium( SWDownloadCandidate candidate )
    {
        if ( candidate == null )
        {
            throw new NullPointerException( "Candidate is null.");
        }
        synchronized( candidatesLock )
        {
            int pos = badCandidatesList.indexOf( candidate );
            if ( pos >= 0 )
            {
                // remove from bad...
                badCandidatesList.remove( pos );
            }
            pos = goodCandidatesList.indexOf( candidate );
            if ( pos >= 0 )
            {
                // remove from good...
                goodCandidatesList.remove( pos );
            }
            
            // ...and add to good.
            if ( !mediumCandidatesList.contains( candidate ) )
            {
                mediumCandidatesList.add( candidate );
                NLogger.debug( NLoggerNames.Download_File,
                    "Moving candidate to medium list: " + candidate.getHostAddress() );
                candidate.addToCandidateLog("Moving candidate to medium list.");
            }
        }
    }

    /**
     * Makes the given download candidate a bad download candidate. This operation
     * will NOT stop a running download of this candidate.
     * The candidate might be used again.
     * 
     * @param candidate the candidate to make bad.
     */
    public void markCandidateBad( SWDownloadCandidate candidate )
    {
        if ( candidate == null )
        {
            throw new NullPointerException( "Candidate is null.");
        }
        synchronized( candidatesLock )
        {
            int pos = goodCandidatesList.indexOf( candidate );
            if ( pos >= 0 )
            {
                // remove from good...
                goodCandidatesList.remove( pos );
            }
            pos = mediumCandidatesList.indexOf( candidate );
            if ( pos >= 0 )
            {
                // remove from medium...
                mediumCandidatesList.remove( pos );
            }
            
            // ...and add to bad.
            if ( !badCandidatesList.contains( candidate ) )
            {
                badCandidatesList.add( candidate );
                NLogger.debug( NLoggerNames.Download_File,
                    "Moving candidate to bad list: " + candidate.getHostAddress() );
                candidate.addToCandidateLog("Moving candidate to bad list.");
            }
        }
        candidate.setStatus( STATUS_CANDIDATE_BAD );
    }
    
    /**
     * Makes the given download candidate a ignored download candidate. This operation
     * will NOT stop a running download of this candidate.
     * the candidate is not looked at when searching for possible candidates in 
     * the bad list.
     * 
     * @param candidate the candidate to make bad.
     */
    public void markCandidateIgnored( SWDownloadCandidate candidate, String reason )
    {
        if ( candidate == null )
        {
            throw new NullPointerException( "Candidate is null.");
        }
        synchronized( candidatesLock )
        {
            int pos = goodCandidatesList.indexOf( candidate );
            if ( pos >= 0 )
            {
                // remove from good...
                goodCandidatesList.remove( pos );
            }
            pos = mediumCandidatesList.indexOf( candidate );
            if ( pos >= 0 )
            {
                // remove from medium...
                mediumCandidatesList.remove( pos );
            }
            
            // ...and add to bad.
            if ( !badCandidatesList.contains( candidate ) )
            {
                badCandidatesList.add( candidate );
                NLogger.debug( NLoggerNames.Download_File,
                    "Moving candidate to bad list: " + candidate.getHostAddress() );
                candidate.addToCandidateLog("Moving candidate to bad list (ignoring).");
            }
        }
        candidate.setStatus( STATUS_CANDIDATE_IGNORED, -1, reason );
    }
    
    /**
     * Possible transfer status:
     * STATUS_CANDIDATE_CONNECTING
     * STATUS_CANDIDATE_PUSH_REQUEST
     * STATUS_CANDIDATE_REMOTLY_QUEUED
     * STATUS_CANDIDATE_REQUESTING
     * STATUS_CANDIDATE_DOWNLOADING
     */
    public void candidateStatusChanged( SWDownloadCandidate candidate,
        int oldStatus, int newStatus )
    {
        synchronized ( candidatesLock )
        {
            // TODO2 it seems that sometime candidates do not get removed correctly
            // of this list.. there status was Connection failed (8)
            switch( oldStatus )
            {
                case STATUS_CANDIDATE_CONNECTING:
                case STATUS_CANDIDATE_PUSH_REQUEST:
                case STATUS_CANDIDATE_REMOTLY_QUEUED:
                case STATUS_CANDIDATE_REQUESTING:
                case STATUS_CANDIDATE_DOWNLOADING:
                transferCandidatesList.remove(candidate);
            }
            
            switch( newStatus )
            {
                case STATUS_CANDIDATE_CONNECTING:
                case STATUS_CANDIDATE_PUSH_REQUEST:
                case STATUS_CANDIDATE_REMOTLY_QUEUED:
                case STATUS_CANDIDATE_REQUESTING:
                case STATUS_CANDIDATE_DOWNLOADING:
                transferCandidatesList.add(candidate);
            }
        }
        fireDownloadCandidateChanged(candidate);
    }
    
    /**
     * Makes the given download candidate a bad download candidate. This operation
     * will NOT stop a running download of this candidate.
     * 
     * @param candidate the candidate to make bad.
     */
    public void addBadAltLoc( SWDownloadCandidate candidate )
    {
        URN candidateURN = candidate.getResourceURN();
        if ( candidateURN != null && fileURN != null )
        {
            
            AlternateLocation altLoc = new AlternateLocation(
                candidate.getHostAddress(), candidateURN );
            
            // remove good alt loc    
            goodAltLocContainer.removeAlternateLocation( altLoc );
            
            // add bad alt loc
            badAltLocContainer.addAlternateLocation( altLoc );            
        }
        NLogger.debug( NLoggerNames.Download_File,
            "Adding bad alt loc: " + candidate.getHostAddress() );
    }
    
    /**
     * Makes the given download candidate a bad download candidate. This operation
     * will NOT stop a running download of this candidate.
     * 
     * @param candidate the candidate to make bad.
     */
    public void addGoodAltLoc( SWDownloadCandidate candidate )
    {
        URN candidateURN = candidate.getResourceURN();
        if ( candidateURN != null && fileURN != null )
        {
            
            AlternateLocation altLoc = new AlternateLocation(
                candidate.getHostAddress(), candidateURN );
            
            // remove bad alt loc    
            badAltLocContainer.removeAlternateLocation( altLoc );
            
            // add good alt loc
            goodAltLocContainer.addAlternateLocation( altLoc );            
        }
        NLogger.debug( NLoggerNames.Download_File,
            "Adding good alt loc: " + candidate.getHostAddress() );
    }
    
    /**
     * Returns the bandwidth controller of this download
     * @return
     */
    public BandwidthController getBandwidthController()
    {
        return bandwidthController;
    }
    
    public void setDownloadThrottlingRate( int speedInBytes )
    {
        bandwidthController.setThrottlingRate(speedInBytes);
    }
    
    public long getDownloadThrottlingRate(  )
    {
        return bandwidthController.getThrottlingRate();
    }
    
    /**
     * Returns the transfer speed from the bandwidth controller of this download.
     * @return
     */
    public long getTransferSpeed()
    {
        return bandwidthController.getShortTransferAvg().getAverage();
    }

    /**
     * Returns the number of available candidates
     */
    public int getCandidatesCount()
    {
        return allCandidatesList.size();
    }

    /**
     * Returns the number of available candidates as an Integer object.
     */
    public IntObj getCandidatesCountObject()
    {
        if ( candidateCountObj.intValue() != allCandidatesList.size() )
        {
            candidateCountObj.setValue( allCandidatesList.size() );
        }
        return candidateCountObj;
    }
    
    public int getDownloadingCandidatesCount()
    {
        updateCandidateWorkerCounts();
        return downloadingCandidateCount;
    }
    
    public int getQueuedCandidatesCount()
    {
        updateCandidateWorkerCounts();
        return queuedCandidateCount;
    }
    
    public int getConnectingCandidatesCount()
    {
        updateCandidateWorkerCounts();
        return connectingCandidateCount;
    }
    
    private void updateCandidateWorkerCounts()
    {
        // to save performance search in transfer list for 
        // downloading candidates.
        synchronized( candidatesLock )
        {
            long now = System.currentTimeMillis();
            if ( lastCandidateWorkerCountUpdate +
                 CANDIDATE_WORKER_COUNT_TIMEOUT > now )
            {
                return;
            }
            int downloadingCount = 0;
            int queuedCount = 0;
            int connectingCount = 0;
            Iterator iterator = transferCandidatesList.iterator();
            while ( iterator.hasNext() )
            {
                SWDownloadCandidate candidate = (SWDownloadCandidate) iterator.next();
                int status = candidate.getStatus();
                switch ( status )
                {
                case STATUS_CANDIDATE_DOWNLOADING:
                    downloadingCount ++;
                    break;
                case STATUS_CANDIDATE_REMOTLY_QUEUED:
                    queuedCount ++;
                    break;
                case STATUS_CANDIDATE_CONNECTING:
                    connectingCount ++;
                    break;
                }
            }
            queuedCandidateCount = queuedCount;
            downloadingCandidateCount = downloadingCount;
            connectingCandidateCount = connectingCount;
            // calculation could take a while.. therefor we dont use cached
            // time.
            lastCandidateWorkerCountUpdate = System.currentTimeMillis();
        }
    }
    
    /**
     * The number of candidates in the good candidate list.
     * @return
     */
    public int getGoodCandidateCount()
    {
        return goodCandidatesList.size();
    }
    
    /**
     * The number of candidates in the bad candidate list.
     * @return
     */
    public int getBadCandidateCount()
    {
        return badCandidatesList.size();
    }

    /**
     * Gets the candidate at the given position. Or null if the index is not
     * available.
     */
    public SWDownloadCandidate getCandidate( int index )
    {
        if ( index < 0 || index >= allCandidatesList.size() )
        {
            return null;
        }
        return (SWDownloadCandidate) allCandidatesList.get( index );
    }

    /**
     * Returns the container of all known good alternate download locations or null
     * if the download has no valid file urn.
     * @return the container of all known good alternate download locations or null
     * if the download has no valid file urn.
     */
    public AlternateLocationContainer getGoodAltLocContainer()
    {
        return goodAltLocContainer;
    }
    
    /**
     * Returns the container of all known bad alternate download locations or null
     * if the download has no valid file urn.
     * @return the container of all known bad alternate download locations or null
     * if the download has no valid file urn.
     */
    public AlternateLocationContainer getBadAltLocContainer()
    {
        return badAltLocContainer;
    }
    
    public int getTransferCandidateCount()
    {
        return transferCandidatesList.size();
    }
    
    public SWDownloadCandidate getTransferCandidate( int index )
    {
        if ( index < 0 || index >= transferCandidatesList.size() )
        {
            return null;
        }
        return (SWDownloadCandidate)transferCandidatesList.get( index );
    }
    
    public void setScopeSelectionStrategy( ScopeSelectionStrategy strategy )
    {
        scopeSelectionStrategy = strategy;
    }
    
    public ScopeSelectionStrategy getScopeSelectionStrategy()
    {
        return scopeSelectionStrategy;
    }
    
    public URI getURI()
    {
        return downloadURI;
    }

    public URN getFileURN()
    {
        return fileURN;
    }

    public Date getCreatedDate()
    {
        return createdDate;
    }
    
    protected void setCreatedDate( Date createdDate )
    {
        this.createdDate = createdDate; 
    }

    public Date getDownloadedDate()
    {
        return modifiedDate;
    }
    
    protected void setDownloadedDate( Date modifiedDate )
    {
        this.modifiedDate = modifiedDate; 
    }

    public HTTPRangeSet createAvailableRangeSet()
    {
        HTTPRangeSet rangeSet = new HTTPRangeSet();
        synchronized( finishedScopeList )
        {
            DownloadScope scope;
            Iterator iterator = finishedScopeList.getScopeIterator();
            while( iterator.hasNext() )
            {
                scope = (DownloadScope)iterator.next();
                rangeSet.addRange( scope.getStart(),
                        scope.getEnd() );
            }
        }
        return rangeSet;
    }

    /**
     * The research settings.
     */
    public ResearchSetting getResearchSetting()
    {
        return researchSetting;
    }

    /**
     * This method is used when a user triggers a search from the user interface
     * it should not be used for any automatic searching! Automatic searching
     * is done vie the ResearchService class.
     */
    public void startSearchForCandidates()
    {
        if ( isFileCompletedOrMoved() )
        {
            return;
        }
        researchSetting.stopSearch();
        // user triggered search with default timeout
        researchSetting.startSearch( Search.DEFAULT_SEARCH_TIMEOUT );
    }

    public void setStatus( short newStatus )
    {
        // dont care for same status
        if ( status == newStatus )
        {
            return;
        }
        // lock downloadCandidates since verifyStatus changes status with
        // downloadCandidate lock.
        synchronized( candidatesLock )
        {
            NLogger.debug( NLoggerNames.Download_File, "DownloadFile Status: " +
                SWDownloadInfo.getDownloadFileStatusString(newStatus) + " (" +
                + newStatus + ")." );
            switch( newStatus )
            {
                case STATUS_FILE_COMPLETED:
                    // count the completed download
                    SimpleStatisticProvider provider = (SimpleStatisticProvider)
                        StatisticsManager.getInstance().getStatisticProvider(
                        StatisticsManager.SESSION_DOWNLOAD_COUNT_PROVIDER );
                    provider.increment( 1 );
                case STATUS_FILE_WAITING:
                case STATUS_FILE_STOPPED:
                    downloadStopNotify();
                    break;
                case STATUS_FILE_DOWNLOADING:
                    // only trigger if the status is not already set to downloading
                    downloadStartNotify();
                    break;
            }
            status = newStatus;
            SwarmingManager.getInstance().fireDownloadFileChanged( this );
        }
    }

    /**
     * Returns the current status of the download file.
     */
    public short getStatus()
    {
        return status;
    }

    public boolean isAbleToBeAllocated()
    {
        return !isDownloadStopped() &&
               !isFileCompletedOrMoved() &&
               workerCount <= ServiceManager.sCfg.maxWorkerPerDownload;
    }

    /**
     * Raises the number of downloading worker for this file by one.
     */
    public void incrementWorkerCount()
    {
        workerCount ++;
    }

    /**
     * Lowers the number of downloading worker for this file by one
     */
    public void decrementWorkerCount()
    {
        workerCount --;
    }

    /**
     * The methods checks all download candidates if there is still a download
     * going on or if the download was completed.
     * The status is set appropriately. If the download is completed the status
     * is set to STATUS_FILE_COMPLETED.
     */
    public void verifyStatus()
    {
        forceCollectionOfTransferData();
        
        synchronized( candidatesLock )
        {
            if ( !isFileCompletedOrMoved() && 
                 getTransferDataSize() == getTransferredDataSize() )
            {
                setStatus( STATUS_FILE_COMPLETED );
                // stop running search...
                researchSetting.stopSearch();
                return;
            }
            
            SWDownloadCandidate candidate;
            Iterator iterator = allocatedCandidateWorkerMap.keySet().iterator();
            int highestStatus = STATUS_CANDIDATE_WAITING;
            while ( iterator.hasNext() && highestStatus != STATUS_CANDIDATE_DOWNLOADING)
            {
                candidate = (SWDownloadCandidate)iterator.next();
                switch ( candidate.getStatus() )
                {
                    case STATUS_CANDIDATE_DOWNLOADING:
                    {
                        highestStatus = STATUS_CANDIDATE_DOWNLOADING;
                        break;
                    }
                    case STATUS_CANDIDATE_REMOTLY_QUEUED:
                    {
                        highestStatus = STATUS_CANDIDATE_REMOTLY_QUEUED;
                        break;
                    }
                }
            }

            // when we are here no download is running...
            // we dont need to set status if file is stopped or completed
            if ( !isDownloadStopped() && !isFileCompletedOrMoved() )
            {
                switch ( highestStatus )
                {
                    case STATUS_CANDIDATE_REMOTLY_QUEUED:
                        setStatus( STATUS_FILE_QUEUED );
                        break;
                    case STATUS_CANDIDATE_DOWNLOADING:
                        setStatus( STATUS_FILE_DOWNLOADING );
                        break;
                    default:
                        setStatus( STATUS_FILE_WAITING );
                        break;
                }
            }
        }
    }

    public boolean isFileCompleted()
    {
        return status == STATUS_FILE_COMPLETED;
    }
    
    public boolean isFileCompletedMoved()
    {
        return status == STATUS_FILE_COMPLETED_MOVED;
    }
    
    public boolean isFileCompletedOrMoved()
    {
        return status == STATUS_FILE_COMPLETED_MOVED ||
               status == STATUS_FILE_COMPLETED;
    }

    public boolean isDownloadInProgress()
    {
        return status == STATUS_FILE_DOWNLOADING;
    }

    public boolean isDownloadStopped()
    {
        return status == STATUS_FILE_STOPPED;
    }
    
    /**
     * The incomplete file of the download. A single incomplete file is used
     * for each download file.
     * 
     * @throws FileHandlingException 
     */
    public ManagedFile getIncompleteDownloadFile() 
        throws ManagedFileException, FileHandlingException
    {
        initIncompleteFile();
        return FileManager.getInstance().getReadWriteManagedFile(incompleteFile);
    }
    
    public File getIncompleteFile( )
    {
        return incompleteFile;
    }

    /**
     * The destination file of the finished download. The path leads to the
     * directory where the file is stored after the download is completed. This
     * must not always be the download directory for temporary files.
     */
    public File getDestinationFile( )
    {
        return destinationFile;
    }

    /**
     * Returns the file name of the destination file.
     */
    public String getDestinationFileName( )
    {
        return destinationFile.getName();
    }
    
    public boolean isDestinationStreamable()
    {
        return MediaType.getStreamableMediaType().isFilenameOf( 
            getDestinationFileName() );
    }

    /**
     * Sets a new destination file name.
     * Be aware to only do this when the download is stopped. Otherwise you might
     * screw the download!
     */
    public void setDestinationFile( File aDestinationFile, boolean rename )
        throws FileHandlingException
    {
        if ( aDestinationFile.compareTo( destinationFile ) == 0 )
        {
            return;
        }
        if ( rename )
        {
            // first check if download mgr has any running download with this
            // filename
            if ( SwarmingManager.getInstance().isNewLocalFilenameUsed(
                this, aDestinationFile ) )
            {
                // cant rename to file that already exists
                throw new FileHandlingException(
                    FileHandlingException.FILE_ALREADY_EXISTS );
            }
            initIncompleteFile();
            File newIncompleteFile = createIncompleteFile( aDestinationFile );
            if ( incompleteFile.exists() )
            {
                FileUtils.renameLocalFile( incompleteFile, newIncompleteFile );
            }
            incompleteFile = newIncompleteFile;
            
            FileUtils.renameLocalFile( destinationFile, aDestinationFile );
        }
        destinationFile = aDestinationFile;
        updateDestinationData();
    }

    /**
     * Recalculate prefix, suffix & filetype values.
     * To be called when the destinationFile is updated.
     */
    private void updateDestinationData()
    {
        Pattern p = Pattern.compile("^(.*)(\\..+?)$");
        Matcher m = p.matcher( destinationFile.getName() );
        if ( m.matches() == true )
        {
            destinationPrefix = new String( m.group(1) + " ");
            destinationSuffix = m.group(2);
        }
        else 
        {
            destinationPrefix = destinationFile.getName();
            destinationSuffix = new String( "???" );
        }
        if ( Executer.matches(ServiceManager.sCfg.streamableSuffixes.iterator(), destinationSuffix ) != null )
        {
            NLogger.debug( NLoggerNames.Download_File, "Appears to be a streamable file.");
            destinationType = DESTTYPE_STREAMABLE;
        } 
        else if ( Executer.matches(ServiceManager.sCfg.unstreamableSuffixes.iterator(), destinationSuffix ) != null )
        {
            NLogger.debug( NLoggerNames.Download_File, "Appears to be an unstreamable file.");
            destinationType = DESTTYPE_UNSTREAMABLE;
        }
        else 
        {
            NLogger.debug( NLoggerNames.Download_File, "Not recognised as either streamable or unstreamable.");
            destinationType = DESTTYPE_UNKNOWN;
        }
    }
    
    /**
     * Returns a file that is ready for preview. If the download is still running
     * a copy of the incomplete file is made and returned, if the download is
     * complete the complete file is returned, if the file cant be previewed 
     * null is returned.
     * @return a file for preview, or null.
     */
    public File getPreviewFile()
    {
        if ( isFileCompletedOrMoved() )
        {
            return destinationFile;
        }
        
        if ( finishedScopeList.size() == 0 )
        {// nothing downloaded yet.
            return null;
        }
        
        DownloadScope scope = finishedScopeList.getScopeAt( 0 );
        if ( scope.getStart() != 0 )
        {// file begining not available.
            return null;
        }
        long previewLength = scope.getEnd();
        
        StringBuffer fullFileNameBuf = new StringBuffer();
        fullFileNameBuf.append( ServiceManager.sCfg.incompleteDir );
        fullFileNameBuf.append( File.separatorChar );
        fullFileNameBuf.append( "PREVIEW" );
        fullFileNameBuf.append( "-" );
        fullFileNameBuf.append( destinationFile.getName() );
        File previewFile = new File( fullFileNameBuf.toString() );
        previewFile.deleteOnExit();
        
        try
        {
            FileUtils.copyFile(incompleteFile, previewFile, previewLength);
            return previewFile;
        }
        catch ( IOException exp )
        {
            return null;
        }
    }
    
    /**
     * Returns whether a preview of this file is already possible.
     * @return
     */
    public boolean isPreviewPossible()
    {
        if ( isFileCompletedOrMoved() )
        {
            return true;
        }
        
        if ( finishedScopeList.size() == 0 )
        {// nothing downloaded yet.
            return false;
        }
        
        DownloadScope scope = finishedScopeList.getScopeAt( 0 );
        if ( scope.getStart() != 0 )
        {// file begining not available.
            return false;
        }

        // TODO2 validate file extensions if they are previewable...
        
        return true;
    }

    /**
     * Moves a completed download file to the destination file.
     */
    public synchronized void moveToDestinationFile()
    {
        boolean isRenamed = false;
        synchronized( missingScopeList )
        {
            if ( isFileCompletedMoved() )
            {// somebody else did it already before me...
                return;
            }
            
            // this is an assertion... go crazy if fails...
            if ( missingScopeList.size() > 0 && status == STATUS_FILE_COMPLETED)
            {
                throw new RuntimeException( "There must be exactly one download segment (found " + missingScopeList.size() + 
                    ") and the download must be completed to move to destination file '" + destinationFile.getName() + "'" );
            }
            
            File destFile = destinationFile;
            // this is a bug workaround when the destination file is not a absolut
            // pathname.
            if( !destFile.isAbsolute() )
            {
                 destFile = new File( ServiceManager.sCfg.mDownloadDir, destinationFile.getName() );
            }

            // find a free file spot...
            int tryCount = 0;
            while( destFile.exists() )
            {
                tryCount ++;
                StringBuffer newName = new StringBuffer();
                newName.append( destinationFile.getParent() );
                newName.append( File.separatorChar );
                newName.append( '(' );
                newName.append( tryCount );
                newName.append( ") " );
                newName.append( destinationFile.getName() );
                destFile = new File( newName.toString()  );
            }
            NLogger.debug(NLoggerNames.Download_File,
                "Renaming final segment from " + incompleteFile.getAbsolutePath()
                + " to " + destFile.getAbsoluteFile() + ".");
            isRenamed = incompleteFile.renameTo( destFile );
            if ( isRenamed )
            {
                NLogger.debug(NLoggerNames.Download_File,
                    "File " + destinationFile.getName()
                        + " has been moved to its final location" );                
            }
            else
            {
                NLogger.debug(NLoggerNames.Download_File, "Renaming from "
                    + incompleteFile.getAbsolutePath() + " to " + destFile.getAbsolutePath()
                    + " failed.");
            }
            setStatus(STATUS_FILE_COMPLETED_MOVED);
        }
        
        // this executes a command after completion.
        if ( isRenamed )
        {
            Executer exec = new Executer ( destinationFile, 
                ServiceManager.sCfg.completionNotifyMethod );
            ThreadPool.getInstance().addJob( exec, "DownloadExecuter" );
        }

        // Interprets a downloaded magma-list in Phex automatically. 
		if ( isRenamed && ServiceManager.sCfg.autoReadoutDownloadedMagma 
             && destinationFile.getName().endsWith(".magma") )
		{
            try
            {
                SwarmingManager.getInstance().addMagmaToDownload( 
                    destinationFile );
            }
            catch (IOException exp)
            {// TODO2 bring UI message.
                NLogger.error(NLoggerNames.MAGMA, exp.getMessage(), exp);
            }
		}
        
        // auto remove download file if set
        if ( ServiceManager.sCfg.mDownloadAutoRemoveCompleted )
        {
            SwarmingManager.getInstance().removeDownloadFile( this );
            // removeDownloadFile triggers save...
        }
        else
        {
            SwarmingManager.getInstance().notifyDownloadListChange();
        } 
    }


    /**
     * The progress of the download. Its calculated from the total file size
     * compared to the transfered file size.
     * @return the progress of the download or -1 if it cant be determined.
     */
    public Integer getProgress()
    {
        int percentage;
        if ( isFileCompletedOrMoved() )
        {
            percentage = 100;
        }
        else
        {
            long tmpTransferDataSize = fileSize;
            if ( tmpTransferDataSize == UNKNOWN_FILE_SIZE 
              || tmpTransferDataSize == 0 )
            {
                percentage = -1;
            }
            else
            {
                percentage = (int)( transferredDataSize * 100L / tmpTransferDataSize );
            }
        }

        if ( currentProgress.intValue() != percentage )
        {
            // only create new object if necessary
            currentProgress = new Integer( percentage );
        }
        return currentProgress;
    }

    public void startDownload()
    {
        setStatus( STATUS_FILE_WAITING );
        verifyStatus();
        SwarmingManager.getInstance().notifyWaitingWorkers();
    }
    
    /**
     * Stops a possible running download from the given candidate. The method
     * blocks until its worker completly stopped.
     * @param candidate the candidate to stop the possibly running download from.
     */
    public void stopDownload( SWDownloadCandidate candidate )
    {
        SWDownloadWorker worker;
        synchronized ( candidatesLock )
        {
            worker = (SWDownloadWorker)allocatedCandidateWorkerMap.get(
                candidate );
        }
        if ( worker != null )
        {
            worker.stopWorker();
            worker.waitTillFinished();
        }
    }

    /**
     * Method call blocks until all workers are stopped.
     */
    public void stopDownload()
    {
        setStatus( STATUS_FILE_STOPPED );        
        stopAllWorkers( true );
    }
    
    /**
     * Stops all active workers of this download.
     * If waitTillFinished is true the method blocks till all workers are completely
     * stopped.
     * 
     * @param waitTillFinished if true the method blocks till all workers are 
     *                         completely stopped.
     */
    private void stopAllWorkers( boolean waitTillFinished )
    {
        SWDownloadWorker[] workers;
        synchronized ( candidatesLock )
        {
            Collection workerColl = allocatedCandidateWorkerMap.values();
            workers = new SWDownloadWorker[ workerColl.size() ];
            workerColl.toArray( workers );
            
            for (int i = 0; i < workers.length; i++)
            {
                SWDownloadWorker worker = workers[i];            
                worker.stopWorker();
            }
        }
        if ( !waitTillFinished )
        {
            return;
        }
        for (int i = 0; i < workers.length; i++)
        {
            SWDownloadWorker worker = workers[i];
            if ( worker.isInsideCriticalSection() )
            {
                worker.waitTillFinished();
            }
        }
    }

    /**
     * Removes incomplete download file from disk of a stopped download file.
     * In case segments are still allocated by a worker the method blocks until
     * all workers are stopped. 
     */
    public void removeIncompleteDownloadFile()
    {
        if ( isFileCompletedOrMoved() )
        {
            return;
        }
        if ( status != STATUS_FILE_STOPPED )
        {
            NLogger.error( NLoggerNames.Download_File,
                "Can't clean temp files of not stopped download file.");
            return;
        }
        
        stopAllWorkers( true );

        if ( incompleteFile != null && incompleteFile.exists() )
        {
            boolean succ = incompleteFile.delete();
            if ( !succ )
            {
                NLogger.debug( NLoggerNames.Download_Segment, 
                    "Segment " + toString() + ": Failed to delete " 
                    + incompleteFile + ".");
            }
        }
    }

    /**
     * Indicate that the download is just starting.
     * Triggered internally when status changes to STATUS_FILE_DOWNLOADING.
     */
    private void downloadStartNotify( )
    {
        transferStartTime = System.currentTimeMillis();
        modifiedDate = new Date( transferStartTime );
        transferStopTime = 0;
    }

    /**
     * Indicate that the download is no longer running.
     * Triggered internally when status is set to STATUS_FILE_COMPLETED or
     * STATUS_FILE_QUEUED.
     */
    private void downloadStopNotify( )
    {
        // Ignore nested calls.
        if( transferStopTime == 0 )
        {
            transferStopTime = System.currentTimeMillis();
            if ( status == STATUS_FILE_DOWNLOADING )
            {// only update if the status is currently switched from downloading to stopped..
                modifiedDate = new Date( transferStopTime );
            }
        }
        SwarmingManager.getInstance().notifyDownloadListChange();
        try
        {
            getIncompleteDownloadFile().closeFile();
        }
        catch ( FileHandlingException exp )
        {
            NLogger.error(NLoggerNames.Download_File, exp, exp);
        }
        catch ( ManagedFileException exp )
        {
            if ( exp.getCause() instanceof InterruptedException )
            { // the thread was interrupted and requested to stop, most likley
              // by user request.
                NLogger.debug(NLoggerNames.Download_File, exp );
            }
            else
            {
                NLogger.error(NLoggerNames.Download_File, exp, exp);
            }
        }
    }

    /**
     * Forces the collection of transfer data to have real time results. Only
     * call this very rarely and only when real time transfer data is necessary.
     * Normaly you would use lazyCollectTransferData for this task. It only
     * performes updates when some time (1 sec) is passed since the last update.
     */
    public void forceCollectionOfTransferData()
    {
        long tmpTransferredDataSize = 0;
        synchronized( finishedScopeList )
        {
            tmpTransferredDataSize = finishedScopeList.getAggregatedLength();
        }
        Iterator iterator = transferCandidatesList.iterator();
        while ( iterator.hasNext() )
        {
            SWDownloadCandidate candidate = (SWDownloadCandidate) iterator.next();
            SWDownloadSegment segment = candidate.getDownloadSegment();
            if ( segment != null )
            {
                tmpTransferredDataSize += segment.getTransferredDataSize();
            }
        }
        transferredDataSize = tmpTransferredDataSize;
        // next update in min 1000 millis...
        transferDataUpdateTime = System.currentTimeMillis() + 1000;
    }

    /**
     * Collects the transfer data if a certain time has passed or isForced is set
     * to true.
     */
    private void lazyCollectTransferData()
    {
        long now = System.currentTimeMillis();
        
        // dont collect if file is completed
        if ( isFileCompletedOrMoved() ||
             transferDataUpdateTime > now )
        {
            return;
        }
        forceCollectionOfTransferData();
    }
    
    public DownloadScopeList getMissingScopeList()
    {
        synchronized ( missingScopeList )
        {
            return missingScopeList;
        }
    }
    
    public DownloadScopeList getFinishedScopeList()
    {
        synchronized ( finishedScopeList )
        {
            return finishedScopeList;
        }
    }
    
    public DownloadScopeList getBlockedScopeList() 
    {
        synchronized ( blockedScopeList )
        {
            return blockedScopeList;
        }
    }
    
    public RatedDownloadScopeList getRatedScopeList()
    {
        long now = System.currentTimeMillis();
        if ( ratedScopeListBuildTime + RATED_SCOPE_LIST_TIMEOUT > now )
        {
            return ratedScopeList;
        }
        if ( ratedScopeList == null )
        {
            ratedScopeList = new RatedDownloadScopeList();
        }
        else
        {
            ratedScopeList.clear();
        }
        ratedScopeList.addAll( missingScopeList );
        rateDownloadScopeList( ratedScopeList );
        ratedScopeListBuildTime = System.currentTimeMillis();
        
        return ratedScopeList;
    }
    
    private void rateDownloadScopeList( RatedDownloadScopeList ratedScopeList)
    {
        long oldestConnectTime = System.currentTimeMillis() - BAD_CANDIDATE_STATUS_TIMEOUT;
        synchronized( candidatesLock )
        {
            Iterator iterator = goodCandidatesList.iterator();
            while( iterator.hasNext() )
            {
                SWDownloadCandidate candidate = (SWDownloadCandidate)iterator.next();
                if ( candidate.getLastConnectionTime() == 0 ||
                     candidate.getLastConnectionTime() < oldestConnectTime )
                {
                    continue;
                }
                DownloadScopeList availableScopeList = candidate.getAvailableScopeList();
                if ( availableScopeList == null )
                {
                    availableScopeList = new DownloadScopeList();
                    availableScopeList.add( new DownloadScope( 0, fileSize - 1)  );
                }
                ratedScopeList.rateDownloadScopeList( availableScopeList, 
                    candidate.getSpeed() );
            }
        }
    }
    
    /**
     * Used to allocate and reserve a download segment for a download worker.
     * @param wantedRangeSet the ranges that are wanted for this download if
     *        set to null all ranges are allowed.
     */
    public DownloadScope allocateDownloadScope( DownloadScopeList candidateScopeList, 
        long preferredSegmentSize, long speed )
    {
        DownloadScope result = null;
        boolean retry;
        do
        {
            retry = false;
            // ignore wanted range set if file size is unknown
            if ( fileSize != UNKNOWN_FILE_SIZE )
            {
                result = allocateSegmentForRangeSet( candidateScopeList, preferredSegmentSize );
                NLogger.debug( NLoggerNames.Download_Segment_Allocate, 
                    "Allocated: " + result );
            }
            else
            {
                result = allocateSegment( preferredSegmentSize );
                NLogger.debug( NLoggerNames.Download_Segment_Allocate, 
                    "Allocated: " + result );
            }
            
            /*
            TODO2 segment hijacking like we done in the old download concept is
            not what we want anymore for the new concept. Instead we like to
            double download certain slow or left over download scopes to raise
            overall download speed.
            */
        } while (retry);
        return result;
    }
    
    /**
     * Allocates a download segment and assignes it to the given worker.
     * @param worker the worker that likes to allocate a segment for download.
     * @return a download segment ready to download.
     */
    private DownloadScope allocateSegment( long preferredSize )
    {
        NLogger.debug( NLoggerNames.Download_Segment_Allocate,
            "allocateSegment() size:" + preferredSize);
        synchronized( missingScopeList )
        {
            Iterator iterator = missingScopeList.getScopeIterator();
            while( iterator.hasNext() )
            {
                DownloadScope scope = (DownloadScope)iterator.next();
                missingScopeList.remove( scope );
                synchronized( scope )
                {
                    // ignore prefered size if fileSize is unknown
                    if ( fileSize != UNKNOWN_FILE_SIZE && 
                         scope.getLength() > preferredSize )
                    {
                        DownloadScope beforeScope = new DownloadScope( 
                            scope.getStart(), scope.getStart() + preferredSize - 1 );
                        DownloadScope afterScope = new DownloadScope( 
                            scope.getStart() + preferredSize, scope.getEnd() );
                        missingScopeList.add( afterScope );
                        scope = beforeScope;
                    }
                }
                synchronized( blockedScopeList )
                {
                    blockedScopeList.add( scope );
                }
                return scope;
            }
            return null;
        }
    }
    
    private DownloadScope allocateSegmentForRangeSet( 
        DownloadScopeList candidateScopeList, long preferredSize)
    {
        assert fileSize != UNKNOWN_FILE_SIZE : 
            "Cant allocate segment for range set with unknown end.";
        NLogger.debug( NLoggerNames.Download_Segment_Allocate,
            "allocateSegmentForRangeSet() size: " + preferredSize);
        
        if( candidateScopeList == null )
        {
            // create scope covering the whole file.
            candidateScopeList = new DownloadScopeList();
            candidateScopeList.add( new DownloadScope( 0, fileSize - 1 ) );
        }
        synchronized( missingScopeList )
        {
            DownloadScopeList wantedScopeList = (DownloadScopeList)missingScopeList.clone();
            wantedScopeList.addAll( missingScopeList );
            wantedScopeList.retainAll( candidateScopeList );
            if ( wantedScopeList.size() == 0 )
            {
                return null;
            }
            
            DownloadScope scope = scopeSelectionStrategy.selectDownloadScope(
                this, wantedScopeList, preferredSize );
            if ( scope == null )
            {
                return null;
            }
            missingScopeList.remove( scope );
            synchronized( blockedScopeList )
            {
                blockedScopeList.add( scope );
            }
            return scope;
        }
    }
    
    /**
     * Releases a allocated download segment.
     */
    public void releaseDownloadScope( DownloadScope downloadScope, 
        SWDownloadSegment downloadSegment )
    {
        if ( downloadScope.getEnd() == Long.MAX_VALUE && fileSize != UNKNOWN_FILE_SIZE )
        {
            downloadScope = new DownloadScope( downloadScope.getStart(),
                fileSize - 1 );
        }
        
        DownloadScope finishedScope = null;
        DownloadScope missingScope = null;
        long transferredSize = downloadSegment.getTransferredDataSize();
        if ( transferredSize == 0 )
        {// just give back the scope to missing.
            missingScope = downloadScope;
        }
        else
        {
            finishedScope = new DownloadScope( downloadScope.getStart(),
                downloadScope.getStart() + transferredSize - 1);
            if ( transferredSize < downloadScope.getLength() )
            {
                missingScope = new DownloadScope(
                    downloadScope.getStart() + transferredSize,
                    downloadScope.getEnd() );
            }
        }
        synchronized( blockedScopeList )
        {
            blockedScopeList.remove( downloadScope );
        }
        synchronized ( missingScopeList )
        {            
            if ( missingScope != null )
            {
                missingScopeList.add( missingScope );
            }
        }
        synchronized ( finishedScopeList )
        {            
            if ( finishedScope != null )
            {
                finishedScopeList.add( finishedScope );
            }
        }
    }
    
    public boolean addAndValidateQueuedCandidate( SWDownloadCandidate candidate )
    {        
        int maxWorkers = Math.min( ServiceManager.sCfg.maxTotalDownloadWorker, 
            ServiceManager.sCfg.maxWorkerPerDownload );
        int maxQueuedWorkers = (int)Math.max( Math.floor( 
            maxWorkers - (maxWorkers*0.2) ), 1 );
        synchronized ( candidatesLock )
        {
            int queuedCount = queuedCandidatesSet.size();
            if ( queuedCount < maxQueuedWorkers )
            {
                candidate.addToCandidateLog("Accept queued candidate (" + 
                    queuedCount +"/"+maxQueuedWorkers + ")");
                queuedCandidatesSet.add(candidate);
                return true;
            }
            
            if ( queuedCandidatesSet.contains(candidate) )
            {
                return true;
            }
            
            // find a bad slot to drop...
            SWDownloadCandidate highestCandidate = null;
            int highestPos = -1;
            Iterator iterator = queuedCandidatesSet.iterator();
            while ( iterator.hasNext() )
            {
                SWDownloadCandidate altCandidate = (SWDownloadCandidate) iterator.next();
                int altPos = altCandidate.getXQueueParameters().getPosition().intValue();
                if ( altPos > highestPos )
                {
                    highestCandidate = altCandidate;
                    highestPos = altPos;
                }
            }
            
            int candidatePos = candidate.getXQueueParameters().getPosition().intValue();
            if ( highestCandidate != null && highestPos > candidatePos )
            {
                // TODO1 use a higher timeout to honor queue pos
                highestCandidate.addToCandidateLog(
                    "Drop queued candidate - new alternative: " + 
                    highestPos +" - " + candidatePos + ")");
                highestCandidate.setStatus( SWDownloadConstants.STATUS_CANDIDATE_BUSY, -1 );
                SWDownloadWorker worker = (SWDownloadWorker)allocatedCandidateWorkerMap.get(
                    highestCandidate);
                if ( worker != null )
                {
                    worker.stopWorker();
                }
                queuedCandidatesSet.add(candidate);
                return true;
            }
            else
            {
                // TODO1 use a higher timeout to honor queue pos
                candidate.addToCandidateLog(
                    "Drop queued candidate - existing alternative: " + 
                    candidatePos +" - " + highestPos );
                candidate.setStatus( SWDownloadConstants.STATUS_CANDIDATE_BUSY, -1 );
                return false;
            }
        }
    }
    
    public void removeQueuedCandidate( SWDownloadCandidate candidate )
    { 
        synchronized ( candidatesLock )
        {
            queuedCandidatesSet.remove(candidate);
        }
    }

    private void initAltLocContainers()
    {
        goodAltLocContainer = new AlternateLocationContainer( fileURN );
        badAltLocContainer = new AlternateLocationContainer( fileURN );
    }
    
    /**
     * Creates the incomplete file object.
     * The filename contains the localFilename of the download file and the
     * segment code. The first segment of the file should not modify the
     * original file extension. This allows applications to open the file.
     * All other segments should modify the extension since it should not be
     * possible to open a segment that is not the first one.
     * For performance reasons the segment keeps hold to its File reference.
     * @throws FileHandlingException 
     */
    private void initIncompleteFile() throws FileHandlingException
        
    {
        if ( incompleteFile != null )
        {
            return;
        }
        File destFile = getDestinationFile();
        try
        {
            incompleteFile = createIncompleteFile( destFile );
        }
        catch ( FileHandlingException exp )
        {
            String filename = exp.getFileName();
            Throwable cause = exp.getCause();
            String errorStr = cause != null ? cause.toString() : "Unknown";
            stopDownload();
            Environment.getInstance().fireDisplayUserMessage(
                UserMessageListener.SegmentCreateIncompleteFileFailed, 
                new String[] {filename, errorStr});
            throw exp;
        }
    }
    
    public static File createIncompleteFile( File destinationFile ) throws FileHandlingException
    {
        int tryCount = 0;
        File tryFile;
        boolean succ;
        IOException lastExp = null;
        do
        {
            StringBuffer fullFileNameBuf = new StringBuffer();
            fullFileNameBuf.append( ServiceManager.sCfg.incompleteDir );
            fullFileNameBuf.append( File.separatorChar );
            fullFileNameBuf.append( "INCOMPLETE" );
            if ( tryCount > 0 )
            {
                fullFileNameBuf.append( '(' );
                fullFileNameBuf.append( String.valueOf( tryCount ) );
                fullFileNameBuf.append( ')' );
            }
            fullFileNameBuf.append( "-" );
            fullFileNameBuf.append( destinationFile.getName() );

            tryFile = new File( fullFileNameBuf.toString() );
            tryFile.getParentFile().mkdirs();
            tryCount ++;
            try
            {
                succ = tryFile.createNewFile();
            }
            catch (IOException exp)
            {
                lastExp = exp;
                succ = false;
            }
        }
        while ( !succ && tryCount < 50 );
        if ( lastExp != null )
        {
            NLogger.error( NLoggerNames.Download, lastExp, lastExp );
        }
        if ( !succ )
        {
            NLogger.error( NLoggerNames.Download, 
                "Tryied " + tryCount + " times to create a segment file. Giving up" );
            throw new FileHandlingException( FileHandlingException.CREATE_FILE_FAILED,
                tryFile.getAbsolutePath(), lastExp );
        }
        
        return tryFile;
    }
    
    private void createDownloadScopes( XJBSWDownloadFile xjbFile )
    {
        //clean scops
        missingScopeList.clear();
        blockedScopeList.clear();
        finishedScopeList.clear();
        
        Iterator iterator = xjbFile.getFinishedScopesList().iterator();
        while( iterator.hasNext() )
        {
            XJBDownloadScope xjbScope = (XJBDownloadScope) iterator.next();
            DownloadScope downloadScope = new DownloadScope( xjbScope.getStart(),
                xjbScope.getEnd() );
            finishedScopeList.add( downloadScope );
        }
        
        // build missing scope list.
        if ( fileSize == UNKNOWN_FILE_SIZE )
        {
            missingScopeList.add( new DownloadScope( 0, Long.MAX_VALUE ) );
        }
        else
        {
            missingScopeList.add( new DownloadScope( 0, fileSize - 1 ) );
        }
        // remove finished scopes...
        missingScopeList.removeAll( finishedScopeList );
    }

    private void createDownloadCandidates( XJBSWDownloadFile xjbFile )
    {
        synchronized( candidatesLock )
        {
            long before24h = System.currentTimeMillis() - DateUtils.MILLIS_PER_DAY;
            // clean candidates
            allCandidatesList.clear();
            goodCandidatesList.clear();
            mediumCandidatesList.clear();
            badCandidatesList.clear();

            // add xml definitions...
            XJBSWDownloadCandidate xjbCandidate;
            SWDownloadCandidate candidate;
            Iterator iterator = xjbFile.getCandidateList().iterator();
            while( iterator.hasNext() )
            {
                try
                {
                    xjbCandidate = (XJBSWDownloadCandidate)iterator.next();
                    candidate = new SWDownloadCandidate( xjbCandidate, this );
                    NLogger.debug(NLoggerNames.Download_File,
                        "Adding download candidate " + candidate );
                    
                    int pos = allCandidatesList.size();
                    allCandidatesList.add( candidate );
                    fireDownloadCandidateAdded( pos );
                    if ( xjbCandidate.getConnectionFailedRepetition() >= BAD_CANDIDATE_CONNECTION_TRIES )
                    {
                        badCandidatesList.add( candidate );
                    }
                    else if ( xjbCandidate.getLastConnectionTime() > before24h )
                    {
                        goodCandidatesList.add( candidate );
                    }
                    else
                    {
                        mediumCandidatesList.add( candidate );
                    }
                }
                catch ( MalformedDestAddressException exp )
                {
                    NLogger.warn(NLoggerNames.Download_File, exp, exp );
                }
                catch ( Exception exp )
                {// catch all exception in case we have an error in the XML
                    NLogger.error(NLoggerNames.Download_File, 
                        "Error loading a download candidate from XML.", exp );
                }
            }
            
            Collections.sort(goodCandidatesList, new InitialCandidatesComparator());
            Collections.sort(mediumCandidatesList, new InitialCandidatesComparator());
        }
    }
    
    public XJBSWDownloadFile createXJBSWDownloadFile()
        throws JAXBException
    {
        ObjectFactory objFactory = new ObjectFactory();
        XJBSWDownloadFile xjbFile = objFactory.createXJBSWDownloadFile();
        xjbFile.setLocalFileName( destinationFile.getName() );
        xjbFile.setIncompleteFileName( incompleteFile.getAbsolutePath() );
        xjbFile.setFileSize( fileSize );
        xjbFile.setSearchTerm( researchSetting.getSearchTerm() );
        xjbFile.setCreatedTime( createdDate.getTime() );
        xjbFile.setModifiedTime( modifiedDate.getTime() );
        if ( fileURN != null )
        {
            xjbFile.setFileURN( fileURN.getAsString() );
        }
        xjbFile.setScopeSelectionStrategy( scopeSelectionStrategy.getClass().getName() );
        xjbFile.setStatus( status );
        synchronized ( candidatesLock )
        {
            List list = xjbFile.getCandidateList();
            Iterator iterator = goodCandidatesList.iterator();
            while( iterator.hasNext() )
            {
                SWDownloadCandidate candidate = (SWDownloadCandidate)iterator.next();
                XJBSWDownloadCandidate xjbCandidate =
                    candidate.createXJBSWDownloadCandidate();
                list.add( xjbCandidate );
            }
            iterator = mediumCandidatesList.iterator();
            while( iterator.hasNext() )
            {
                SWDownloadCandidate candidate = (SWDownloadCandidate)iterator.next();
                XJBSWDownloadCandidate xjbCandidate =
                    candidate.createXJBSWDownloadCandidate();
                list.add( xjbCandidate );
            }
            iterator = badCandidatesList.iterator();
            while( iterator.hasNext() )
            {
                SWDownloadCandidate candidate = (SWDownloadCandidate)iterator.next();
                if ( candidate.getStatus() == STATUS_CANDIDATE_IGNORED )
                {
                    continue;
                }
                XJBSWDownloadCandidate xjbCandidate =
                    candidate.createXJBSWDownloadCandidate();
                list.add( xjbCandidate );
            }
        }
        
        Iterator iterator = finishedScopeList.getScopeIterator();
        List list = xjbFile.getFinishedScopesList();
        while( iterator.hasNext() )
        {
            DownloadScope scope = (DownloadScope)iterator.next();
            XJBDownloadScope xjbScope = objFactory.createXJBDownloadScope();
            xjbScope.setStart( scope.getStart() );
            xjbScope.setEnd( scope.getEnd() );
            list.add( xjbScope );
        }
        return xjbFile;
    }
    
    //////// START TransferDataProvider Interface ///////////

    /**
     * Not implemented... uses own transfer rate calculation
     */
    public void setTransferRateTimestamp( long timestamp )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Not implemented... uses own transfer rate calculation
     */
    public int getShortTermTransferRate()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the long term data transfer rate in bytes. This is the rate of
     * the transfer since the last start of the transfer. This means after a
     * transfer was interrupted and is resumed again the calculation restarts.
     */
    public int getLongTermTransferRate()
    {
        return (int)getTransferSpeed();
    }

    /**
     * Return the data transfer status.
     * It can be TRANSFER_RUNNING, TRANSFER_NOT_RUNNING, TRANSFER_COMPLETED,
     * TRANSFER_ERROR.
     */
    public short getDataTransferStatus()
    {
        switch ( status )
        {
        case STATUS_FILE_DOWNLOADING:
            return TRANSFER_RUNNING;
        case STATUS_FILE_COMPLETED:
        case STATUS_FILE_COMPLETED_MOVED:
            return TRANSFER_COMPLETED;
        default:
            return TRANSFER_NOT_RUNNING;
        }
    }

    /**
     * Return the size of data that is attempting to be transfered. This is
     * NOT necessarily the full size of the file as could be the case during
     * a download resumption.
     */
    public long getTransferDataSize()
    {
        return getTotalDataSize();
    }

    /**
     * This is the total size of the file. Even if its not importend
     * for the transfer itself.
     * Dont change the meaning of this it is way importent for researching and
     * adding candidates!!
     */
    public long getTotalDataSize()
    {
        return fileSize;
    }

    /**
     * Defines the length already downloaded, this is lazy collected from
     * segments when the download is not completed
     */
    public long getTransferredDataSize()
    {
        lazyCollectTransferData();
        return transferredDataSize;
    }
    
    //////// END TransferDataProvider Interface ///////////

    ///////////////////// START event handling methods ////////////////////////

    private EventListenerList listenerList = new EventListenerList();

    public void addDownloadCandidatesChangeListener(
        DownloadCandidatesChangeListener listener )
    {
        listenerList.add( DownloadCandidatesChangeListener.class, listener );
    }

    public void removeDownloadCandidatesChangeListener(
        DownloadCandidatesChangeListener listener )
    {
        listenerList.remove( DownloadCandidatesChangeListener.class, listener );
    }

    private void fireDownloadCandidateChanged( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.getListeners(
                    DownloadCandidatesChangeListener.class );
                DownloadCandidatesChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (DownloadCandidatesChangeListener)listeners[ i ];
                    listener.downloadCandidateChanged( position );
                }
            }
        });
    }

    private void fireDownloadCandidateAdded( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.getListeners(
                    DownloadCandidatesChangeListener.class );
                DownloadCandidatesChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (DownloadCandidatesChangeListener)listeners[ i ];
                    listener.downloadCandidateAdded( position );
                }
            }
        });
    }

    private void fireDownloadCandidateRemoved( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.getListeners(
                    DownloadCandidatesChangeListener.class );
                DownloadCandidatesChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (DownloadCandidatesChangeListener)listeners[ i ];
                    listener.downloadCandidateRemoved( position );
                }
            }
        });
    }

    public void fireDownloadCandidateChanged( SWDownloadCandidate candidate )
    {
        int position = allCandidatesList.indexOf( candidate );
        if ( position >= 0 )
        {
            fireDownloadCandidateChanged( position );
        }
    }

    
    public void addDownloadSegmentChangeListener(
        DownloadSegmentsChangeListener listener )
    {
        listenerList.add( DownloadSegmentsChangeListener.class, listener );
    }

    public void removeDownloadSegmentChangeListener(
        DownloadSegmentsChangeListener listener )
    {
        listenerList.remove( DownloadSegmentsChangeListener.class, listener );
    }

    private void fireDownloadSegmentChanged( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.getListeners(
                    DownloadSegmentsChangeListener.class );
                DownloadSegmentsChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (DownloadSegmentsChangeListener)listeners[ i ];
                    listener.downloadSegmentChanged( position );
                }
            }
        });
    }

    private void fireDownloadSegmentAdded( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.getListeners(
                    DownloadSegmentsChangeListener.class );
                DownloadSegmentsChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (DownloadSegmentsChangeListener)listeners[ i ];
                    listener.downloadSegmentAdded( position );
                }
            }
        });
    }

    private void fireDownloadSegmentRemoved( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.getListeners(
                    DownloadSegmentsChangeListener.class );
                DownloadSegmentsChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (DownloadSegmentsChangeListener)listeners[ i ];
                    listener.downloadSegmentRemoved( position );
                }
            }
        });
    }

    ///////////////////// END event handling methods ////////////////////////
    private static class InitialCandidatesComparator implements Comparator
    {
        public int compare( Object obj1, Object obj2 )
        {
            if ( obj1 == obj2 || obj1.equals(obj2) )
            {
                return 0;
            }
            SWDownloadCandidate candidate1 = (SWDownloadCandidate)obj1;
            SWDownloadCandidate candidate2 = (SWDownloadCandidate)obj2;
            long diff = candidate1.getLastConnectionTime() 
                - candidate2.getLastConnectionTime();
            if ( diff < 0 )
            {
                return 1;
            }
            else if ( diff > 0 )
            {
                return -1;
            }
            else
            {
                return candidate1.hashCode() - candidate2.hashCode();
            }
        }
    }
    
    private static class SWDownloadSegmentComparatorByStartPosition implements Comparator
    {
        public int compare( Object obj1, Object obj2 )
        {
            XJBSWDownloadSegment segment1 = (XJBSWDownloadSegment)obj1;
            XJBSWDownloadSegment segment2 = (XJBSWDownloadSegment)obj2;
            long diff = segment1.getStartPosition() - segment2.getStartPosition();
            // this is done to maintain long/int type size safety...
            if ( diff < 0 )
            {
                return -1;
            }
            else if ( diff > 0 )
            {
                return 1;
            }
            else
            {
                return 0;
            }
        }
    }
}