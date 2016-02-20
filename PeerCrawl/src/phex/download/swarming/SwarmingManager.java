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
 *  $Id: SwarmingManager.java,v 1.88 2005/11/19 14:51:24 gregork Exp $
 */
package phex.download.swarming;

import java.io.*;
import java.util.*;

import javax.xml.bind.JAXBException;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

import phex.common.*;
import phex.common.file.FileManager;
import phex.common.file.ManagedFile;
import phex.common.file.ManagedFileException;
import phex.download.MagnetData;
import phex.download.RemoteFile;
import phex.download.log.LogBuffer;
import phex.event.AsynchronousDispatcher;
import phex.event.DownloadFilesChangeListener;
import phex.event.UserMessageListener;
import phex.share.SharedFilesService;
import phex.utils.*;
import phex.xml.*;


public class SwarmingManager implements Manager
{
    public static final short PRIORITY_MOVE_TO_TOP = 0;
    public static final short PRIORITY_MOVE_UP = 1;
    public static final short PRIORITY_MOVE_DOWN = 2;
    public static final short PRIORITY_MOVE_TO_BOTTOM = 3;

    /**
     * Indicates if the swarming manager is shutting down or not.
     */
    private boolean isManagerShutingDown;

    /**
     * The active workers. 
     */
    private ArrayList/*<SWDownloadWorker>*/ workerList;
    
    /**
     * The download list.
     */
    private ArrayList/*<SWDownloadFile>*/ downloadList;
 
    /**
     * A Map that maps URNs to download files they belong to. This is for
     * performant searching by urn.
     * When accesseing this object locking via the downloadList object is
     * required.
     */
    private HashMap/*<URN, SWDownloadFile>*/ urnToDownloadMap;

    private IPCounter ipDownloadCounter;

    /**
     * The temporary worker holds the only worker that is used to check if more
     * workers are required. The temporary worker waits for a valid download set
     * once a valid set is found the worker loses its temporary status and a new
     * temporary worker will be created. This is used to only hold the necessary
     * number of workers.
     */
    private SWDownloadWorker temporaryWorker;
    
    /**
     * The worker launcher is responsible to launche download workers and 
     * to make sure there always are enough workers available.
     */
    private DownloadWorkerLauncher workerLauncher;

    /**
     * Lock object to lock saving of download lists.
     */
    private static Object saveDownloadListLock = new Object();

    /**
     * Object that holds the save job instance while a save job is running. The
     * reference is null if the job is not running.
     */
    private SaveDownloadListJob saveDownloadListJob;
    
    /**
     * Indicates if the download list has changed since the last time it was
     * saved.
     */
    private boolean downloadListChangedSinceSave;
    
    private LogBuffer downloadCandidateLogBuffer;

    private static SwarmingManager instance;
    
    private SharedFilesService sharedFilesService;

    private SwarmingManager()
    {
        downloadListChangedSinceSave = false;
        isManagerShutingDown = false;
    }

    public static SwarmingManager getInstance()
    {
        if ( instance == null )
        {
            instance = new SwarmingManager();
        }
        return instance;
    }

    /**
     * This method is called in order to initialize the manager.  Inside
     * this method you can't rely on the availability of other managers.
     * @return true if initialization was successful, false otherwise.
     */
    public boolean initialize()
    {
        workerList = new ArrayList( 5 );
        downloadList = new ArrayList( 5 );
        urnToDownloadMap = new HashMap();
        ipDownloadCounter = new IPCounter( ServiceManager.sCfg.maxDownloadsPerIP );
        if ( ServiceManager.sCfg.downloadCandidateLogBufferSize > 0 )
        {
            downloadCandidateLogBuffer = new LogBuffer( ServiceManager.sCfg.downloadCandidateLogBufferSize );
        }
        return true;
    }

    /**
     * This method is called in order to perform post initialization of the
     * manager. This method includes all tasks that must be done after initializing
     * all the several managers. Inside this method you can rely on the
     * availability of other managers.
     * @return true if initialization was successful, false otherwise.
     */
    public boolean onPostInitialization()
    {
        LoadDownloadListJob job = new LoadDownloadListJob();
        job.start();
        return true;
    }
    
    /**
     * This method is called after the complete application including GUI completed
     * its startup process. This notification must be used to activate runtime
     * processes that needs to be performed once the application has successfully
     * completed startup.
     */
    public void startupCompletedNotify()
    {
        workerLauncher = new DownloadWorkerLauncher();
        workerLauncher.setDaemon( true );
        workerLauncher.start();
        
        Environment.getInstance().scheduleTimerTask(
            new SaveDownloadListTimer(), SaveDownloadListTimer.TIMER_PERIOD,
            SaveDownloadListTimer.TIMER_PERIOD );
    }

    /**
     * This method is called in order to cleanly shutdown the manager. It
     * should contain all cleanup operations to ensure a nice shutdown of Phex.
     */
    public void shutdown()
    {
        // shutdown all workers before shuting down the manager and doing the 
        // final save..
        // this will ensure more consistent download list data.
        synchronized( this )
        {
            isManagerShutingDown = true;
        }
        // in case startup is still on the way workerLauncher might be null
        if ( workerLauncher != null )
        {
            workerLauncher.triggerCycle();
        }
        
        SWDownloadWorker[] workers = new SWDownloadWorker[ workerList.size() ];
        workerList.toArray(workers);
        for (int i = 0; i < workers.length; i++)
        {
            SWDownloadWorker worker = workers[i];            
            worker.stopWorker();
        }
        for (int i = 0; i < workers.length; i++)
        {
            SWDownloadWorker worker = workers[i];
            if ( worker.isInsideCriticalSection() )
            {
                worker.waitTillFinished();
            }
        }
        shutdownForceSaveDownloadList();
    }

    public synchronized SWDownloadFile addFileToDownload( RemoteFile remoteFile,
        String filename, String searchTerm )
    {
        SWDownloadFile downloadFile = new SWDownloadFile( filename,
            searchTerm, remoteFile.getFileSize(), remoteFile.getURN() );
        downloadFile.addDownloadCandidate( remoteFile );
        int pos;
        synchronized ( downloadList )
        {
            pos = downloadList.size();
            downloadList.add( downloadFile );
            URN urn = downloadFile.getFileURN();
            if ( urn != null )
            {
                urnToDownloadMap.put( urn, downloadFile );
            }
        }
        fireDownloadFileAdded( pos );
        downloadFile.setStatus( SWDownloadConstants.STATUS_FILE_WAITING );
        workerLauncher.triggerCycle();

        // save in xml
        triggerSaveDownloadList( true );

        return downloadFile;
    }
    
    /**
     * Adds a uri for download.
     * 
     * No checking is done if the file is already downloaded or shared, this
     * should be done by the caller prior calling this method to ensure proper
     * handling of this situation.
     * 
     * @param uri
     * @return
     * @throws URIException
     * @throws AlreadyDownloadingException in case the uri is already downloaded.
     */
    public synchronized SWDownloadFile addFileToDownload( URI uri )
        throws URIException
    {
        if ( NLogger.isDebugEnabled(NLoggerNames.Download_Manager))
        { NLogger.debug(NLoggerNames.Download_Manager,
            "Adding new download by URI: " + uri.toString()); }
        
        SWDownloadFile downloadFile = new SWDownloadFile( uri );
        URN urn = downloadFile.getFileURN();
            
        int pos;
        synchronized ( downloadList )
        {
            pos = downloadList.size();
            downloadList.add( downloadFile );
            if ( urn != null )
            {
                assert !isURNDownloaded(urn);
                urnToDownloadMap.put( urn, downloadFile );
            }
        }
        fireDownloadFileAdded( pos );
        downloadFile.setStatus( SWDownloadConstants.STATUS_FILE_WAITING );
        workerLauncher.triggerCycle();

        // save in xml
        triggerSaveDownloadList( true );
		return downloadFile;
    }
    
    public synchronized void addMagmaToDownload( File magmaFile )
        throws URIException, IOException
    {
        BufferedInputStream inStream = new BufferedInputStream( 
            new FileInputStream( magmaFile ) );
        MagmaParser parser = new MagmaParser( inStream );
        parser.start();

        List magnetList = parser.getMagnets();
        Iterator iter = magnetList.iterator();
        while (iter.hasNext())
        {
            String magnet = (String) iter.next();
            URI uri = new URI( magnet, true );

            MagnetData magnetData = MagnetData.parseFromURI(uri);
            URN urn = MagnetData.lookupSHA1URN(magnetData);
            // dont add already downloading urns.
            if ( !isURNDownloaded( urn ) )
            {
                SwarmingManager.getInstance().addFileToDownload( uri );
            }
        }
    }

    /**
     * Removes the download file from the download list. Stops all running downlads
     * and deletes all incomplete download files.
     */
    public void removeDownloadFile( SWDownloadFile file )
    {
        removeDownloadFileInternal( file );
        // save in xml
        triggerSaveDownloadList( true );
    }
    
    /**
     * Removes the download files from the download list. Stops all running downlads
     * and deletes all incomplete download files.
     */
    public void removeDownloadFiles( SWDownloadFile[] files )
    {
        for ( int i = 0; i < files.length; i++ )
        {
            removeDownloadFileInternal( files[i] );
        }
        // save in xml
        triggerSaveDownloadList( true );
    }

    /**
     * Removes the download files from the download list. Stops all running downlads
     * and deletes all incomplete download files. Block until all possible workers
     * are stopped.
     * @param file
     */
    private void removeDownloadFileInternal( SWDownloadFile file )
    {
        if ( !file.isFileCompletedOrMoved() && !file.isDownloadStopped() )
        {
            // blocks until all workers are stopped.
            file.stopDownload();
        }
        int pos;
        synchronized( downloadList )
        {
            pos = downloadList.indexOf( file );
            if ( pos >= 0 )
            {
                downloadList.remove( pos );
                fireDownloadFileRemoved( pos );
            }
            URN urn = file.getFileURN();
            if ( urn != null )
            {
                urnToDownloadMap.remove( urn );
            }
        }
        // possibly blocks until all workers are stopped.
        file.removeIncompleteDownloadFile();
    }

    public Integer getDownloadPriority( SWDownloadFile file )
    {
        int pos = downloadList.indexOf( file );
        if ( pos >= 0 )
        {
            return new Integer( pos );
        }
        else
        {
            return null;
        }
    }
    
    /**
     * Updates the priorities of the download files according to the order in 
     * the given download file array.
     * @param files the download file array.
     */
    public void updateDownloadFilePriorities( SWDownloadFile[] files )
    {
        synchronized( downloadList )
        {
            for ( int i = 0; i < files.length; i++ )
            {
                int pos = downloadList.indexOf( files[i] );
                if ( pos >= 0 )
                {
                    int newPos = i;
                    if ( newPos < 0 || newPos >= downloadList.size() )
                    {
                        newPos = pos;
                    }
                    downloadList.remove( pos );
                    downloadList.add( newPos, files[i] );
                    fireDownloadFileRemoved( pos );
                    fireDownloadFileAdded( newPos );
                }
            }
        }
    }

    /**
     * Moves the download file in the hierarchy.
     * 
     * @param moveDirection The move direction. Use one of the constants 
     *        PRIORITY_MOVE_TO_TOP, PRIORITY_MOVE_UP, PRIORITY_MOVE_DOWN or
     *        PRIORITY_MOVE_TO_BOTTOM.
     * @param file The SWDownloadFile to move the priority for.
     * @return the new position.
     */
    public int moveDownloadFilePriority( SWDownloadFile file, short moveDirection )
    {
        synchronized( downloadList )
        {
            int pos = downloadList.indexOf( file );
            if ( pos >= 0 )
            {
                int newPos = pos;
                switch ( moveDirection )
                {
                    case PRIORITY_MOVE_UP:
                        newPos --;
                        break;
                    case PRIORITY_MOVE_DOWN:
                        newPos ++;
                        break;
                    case PRIORITY_MOVE_TO_TOP:
                        newPos = 0;
                        break;
                    case PRIORITY_MOVE_TO_BOTTOM:
                        newPos = downloadList.size() - 1;
                        break;
                }
                if ( newPos < 0 || newPos >= downloadList.size() )
                {
                    return pos;
                }

                downloadList.remove( pos );
                downloadList.add( newPos, file );
                fireDownloadFileRemoved( pos );
                fireDownloadFileAdded( newPos );
                return newPos;
            }
            return pos;
        }
    }

    /**
     * Returns the count of the download files
     */
    public int getDownloadFileCount()
    {
        return downloadList.size();
    }

    /**
     * Returns the count of the download files with the given status.
     */
    public int getDownloadFileCount( int status )
    {
        int count = 0;
        synchronized( downloadList )
        {
            Iterator iterator = downloadList.iterator();
            while( iterator.hasNext() )
            {
                SWDownloadFile file = (SWDownloadFile)iterator.next();
                if ( file.getStatus() == status )
                {
                    count ++;
                }
            }
        }
        return count;
    }
    
    /**
     * Returns the number of download files with a active status, this is:
     * STATUS_FILE_WAITING
     * STATUS_FILE_DOWNLOADING
     * STATUS_FILE_QUEUED
     * STATUS_FILE_COMPLETED -> completet but not yet moved.
     * 
     * A not active status would then be:
     * STATUS_FILE_STOPPED
     * STATUS_FILE_COMPLETED_MOVED
     */
    private int getActiveDownloadFileCount( )
    {
        int count = 0;
        synchronized( downloadList )
        {
            Iterator iterator = downloadList.iterator();
            while( iterator.hasNext() )
            {
                SWDownloadFile file = (SWDownloadFile)iterator.next();
                switch ( file.getStatus() )
                {
                    case SWDownloadConstants.STATUS_FILE_STOPPED:
                    case SWDownloadConstants.STATUS_FILE_COMPLETED_MOVED:
                        break;
                    default:
                        count ++;
                }
            }
        }
        return count;
    }

    /**
     * Returns a download file at the given index or null if not available.
     */
    public SWDownloadFile getDownloadFile( int index )
    {
        synchronized( downloadList )
        {
            if ( index < 0 || index >= downloadList.size() )
            {
                return null;
            }
            return (SWDownloadFile) downloadList.get( index );
        }
    }

    /**
     * Returns all download files at the given indices. In case one of the
     * indices is out of bounds the returned download file array contains a 
     * null object in at the corresponding position.
     * @param indices the indices to get the download files for.
     * @return Array of SWDownloadFiles, can contain null objects.
     */
    public SWDownloadFile[] getDownloadFilesAt( int[] indices )
    {
        synchronized( downloadList )
        {
            int length = indices.length;
            SWDownloadFile[] files = new SWDownloadFile[ length ];
            for ( int i = 0; i < length; i++ )
            {
                if ( indices[i] < 0 || indices[i] >= downloadList.size() )
                {
                    files[i] = null;
                }
                else
                {
                    files[i] = (SWDownloadFile)downloadList.get( indices[i] );
                }
            }
            return files;
        }
    }

    /**
     * Returns a download files matching the given fileSize and urn.
     * This is used to find a existing download file for new search results.
     * The additional check for fileSize is a security test to identify faulty
     * search results with faked URNs.
     * @param fileSize the required file size
     * @param matchURN the required URN we need to match.
     * @return the found SWDownloadFile or null if not found.
     */
    public SWDownloadFile getDownloadFile( long fileSize, URN matchURN )
    {
        synchronized( downloadList )
        {
            SWDownloadFile file = getDownloadFileByURN( matchURN );
            if ( file != null && file.getTotalDataSize() == fileSize )
            {
                return file;
            }
            return null;
        }
    }

    /**
     * Returns a download file only identified by the URN. This is used to
     * service partial download requests.
     */
    public SWDownloadFile getDownloadFileByURN( URN matchURN )
    {
        SWDownloadFile file;
        synchronized( downloadList )
        {
            file = (SWDownloadFile)urnToDownloadMap.get( matchURN );
            return file;
        }
    }
    
    /**
     * Returns whether a download file with the given URN exists.
     * @return true when a download file with the given URN exists, false otherwise.
     */
    public boolean isURNDownloaded( URN matchURN )
    {
        if ( matchURN == null )
        {
            return false;
        }
        synchronized( downloadList )
        {
            return urnToDownloadMap.containsKey( matchURN );            
        }
    }

    public void releaseCandidateIP( SWDownloadCandidate candidate )
    {
        ipDownloadCounter.relaseIP( candidate.getHostAddress() );
    }
    
    /**
     * Returns a list of completed but not yet moved download files. This method 
     * is called by a download worker to finish up completed download files.
     * @return a list with completed download files.
     */
    public synchronized List getCompletedDownloadFiles()
    {
        synchronized( downloadList )
        {
            SWDownloadFile downloadFile = null;
            Iterator iterator = downloadList.iterator();
            ArrayList list = new ArrayList( 2 );
            while ( iterator.hasNext() )
            {
                downloadFile = (SWDownloadFile) iterator.next();
                if ( downloadFile.isFileCompleted() )
                {
                    list.add( downloadFile );
                }
            }
            return list;
        }
    }

    /**
     * Allocated a download set. The method will block until a complete download
     * set can be obtained.
     */
    public synchronized SWDownloadSet allocateDownloadSet( SWDownloadWorker worker )
    {
        synchronized( downloadList )
        {
            SWDownloadFile downloadFile = null;
            SWDownloadCandidate downloadCandidate = null;

            Iterator iterator = downloadList.iterator();
            while ( iterator.hasNext() )
            {
                downloadFile = (SWDownloadFile) iterator.next();
                if ( !downloadFile.isAbleToBeAllocated() )
                {
                    //Logger.logMessage( Logger.FINEST, Logger.DOWNLOAD,
                    //    "Download file not able to be allocated: "
                    //    + downloadFile );
                    continue;
                }
                                
                // Pre check if there is any segment allocateable...
                boolean segmentAvailable = downloadFile.isScopeAllocateable( null );
                if ( !segmentAvailable )
                {
                    continue;
                }
                downloadCandidate = downloadFile.allocateDownloadCandidate( worker );
                if ( downloadCandidate == null )
                {
                    //Logger.logMessage( Logger.FINEST, Logger.DOWNLOAD,
                    //    "Allocating DownloadSet - No download candidate. "
                    //    + worker.toString() );
                    continue;
                }
                // make sure we dont download more than X times from
                // the same host
                ipDownloadCounter.setMaxCount( ServiceManager.sCfg.maxDownloadsPerIP );
                boolean succ = ipDownloadCounter.validateAndCountIP( 
                    downloadCandidate.getHostAddress() );
                if ( !succ )
                {
                    downloadFile.releaseDownloadCandidate( downloadCandidate );
                    continue;
                }

                // Only check if there would be a segment allocateable for this candidate...
                boolean segmentAllocateable = downloadFile.isScopeAllocateable(
                    downloadCandidate.getAvailableScopeList() );
                if ( !segmentAllocateable )
                {
                    //Logger.logMessage( Logger.FINER, Logger.DOWNLOAD,
                    //    "Allocating DownloadSet - No download segment. "
                    //    + worker.toString() );
                    downloadFile.releaseDownloadCandidate( downloadCandidate );
                    continue;
                }

                downloadFile.incrementWorkerCount();

                // build download set
                SWDownloadSet set = new SWDownloadSet( downloadFile,
                    downloadCandidate );
                if ( worker == temporaryWorker )
                {
                    unsetTemporaryWorker();
                }
                return set;
            }
        }
        return null;
    }

    /**
     * Checks if a new local file for the given download file is already
     * used in any other download file ( except the given one of course )
     * If no download file is given the file is checked against all download
     * files.
     */
    public boolean isNewLocalFilenameUsed( SWDownloadFile downloadFile,
        File newLocalFile )
    {
        // Check for duplicate filename in the existing files to download.
        int size = downloadList.size();
        for ( int i = 0; i < size; i++ )
        {
            SWDownloadFile existingFile = (SWDownloadFile)downloadList.get( i );

            // check file name if downloadFile is null or existingFile is
            // not the downloadFile
            if ( downloadFile == null || !(existingFile == downloadFile) )
            {
                if ( existingFile.getDestinationFile().compareTo( newLocalFile ) == 0 )
                {
                    // filename is already used
                    return true;
                }
            }
        }
        return false;
    }
    
    public LogBuffer getCandidateLogBuffer()
    {
        return downloadCandidateLogBuffer;
    }
    
    /**
     * Notifies the download manager about a change in the download list which
     * requires a download list save.
     */
    public void notifyDownloadListChange()
    {
        downloadListChangedSinceSave = true;
    }
    
    /**
     * Triggers a save of the download list. The call is not blocking and returns
     * directly, the save process is running in parallel.
     */
    private void triggerSaveDownloadList( boolean force )
    {
        if ( !force && !downloadListChangedSinceSave )
        {// not changed, no save needed
            return;
        }
        NLogger.debug(NLoggerNames.Download_Manager,
            "Trigger save download list..." );
        synchronized( saveDownloadListLock )
        {
            if ( saveDownloadListJob != null )
            {
                // save download list is already in progress. we rerequest a save.
                saveDownloadListJob.triggerFollowUpSave();
            }
            else
            {
                saveDownloadListJob = new SaveDownloadListJob();
                saveDownloadListJob.start();
            }
        }
    }

    /**
     * Forces a save of the download list. The call returns after the save is
     * completed. Only the shutdown routine is allowed to call this method!
     */
    private void shutdownForceSaveDownloadList()
    {
        NLogger.debug(NLoggerNames.Download_Manager,
            "Force save download list..." );
        synchronized( saveDownloadListLock )
        {
            if ( saveDownloadListJob == null )
            {
                saveDownloadListJob = new SaveDownloadListJob();
                saveDownloadListJob.start();
            }
            else
            {
                saveDownloadListJob.triggerFollowUpSave();
            }
        }
        try
        {
            if ( saveDownloadListJob != null )
            {
                try
                {
                    saveDownloadListJob.setPriority( Thread.MAX_PRIORITY );
                    saveDownloadListJob.join();
                }
                catch ( NullPointerException exp )
                {// thread might be already finished and has set itself to null.
                }
            }
        }
        catch ( InterruptedException exp )
        {
            NLogger.error( NLoggerNames.Download_Manager, exp, exp );
        }
    }

    /**
     * Unsets the current temporary worker since it became active
     * and creates a new temporary woker to continue worker count requirement check.
     */
    private synchronized void unsetTemporaryWorker()
    {
        temporaryWorker.setTemporaryWorker( false );
        temporaryWorker = null;
        workerLauncher.triggerCycle();
    }

    /**
     * Notifys all workers that are waiting to start downloading.
     */
    public synchronized void notifyWaitingWorkers()
    {
        notifyAll();
    }

    public synchronized void waitForNotify() 
        throws InterruptedException
    {
        wait( 2000 );
    }
    
    private int getRequiredDownloadWorkerCount()
    {
        return Math.min(
            getActiveDownloadFileCount() * ServiceManager.sCfg.maxWorkerPerDownload,
            ServiceManager.sCfg.maxTotalDownloadWorker );
    }

    /**
     * Checks if there are too many workers and stops the worker if needed.
     * Returns true if worker will be stopped false otherwise.
     * Also verifies if there are enough workers available and triggers worker
     * creating if necessary.
     */
    public synchronized boolean checkToStopWorker( SWDownloadWorker worker )
    {
        int requiredCount = getRequiredDownloadWorkerCount();
        if ( isManagerShutingDown || workerList.size() > requiredCount )
        {
            if ( worker.isRunning() )
            {// if not already stopped
                worker.stopWorker();
                workerList.remove( worker );
				if ( worker.isTemporaryWorker() )
				{
					temporaryWorker = null;
				}
            }
            return true;
        }
        // also stop worker if thread is interrupted.
        if ( Thread.interrupted() )
        {
            return true;
        }
        return false;
    }

    /**
     * Called from worker if it unexpectedly shuts down.
     */
    public void notifyWorkerShoutdown( SWDownloadWorker worker, boolean isExpected )
    {
        NLogger.debug( NLoggerNames.Download_Manager,
            "Worker shutdown: " + worker + ", expected: " + isExpected );
        worker.stopWorker();
        workerList.remove( worker );
		if ( worker.isTemporaryWorker() )
		{
			temporaryWorker = null;
		}
        workerLauncher.triggerCycle();
    }

    ///////////////////// START event handling methods ////////////////////////

    /**
     * All listeners interested in events.
     */
    private ArrayList listenerList = new ArrayList( 2 );

    public void addDownloadFilesChangeListener( DownloadFilesChangeListener listener )
    {
        listenerList.add( listener );
    }

    public void removeDownloadFilesChangeListener( DownloadFilesChangeListener listener )
    {
        listenerList.remove( listener );
    }

    private void fireDownloadFileChanged( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                DownloadFilesChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (DownloadFilesChangeListener)listeners[ i ];
                    listener.downloadFileChanged( position );
                }
            }
        });
    }

    private void fireDownloadFileAdded( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                DownloadFilesChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (DownloadFilesChangeListener)listeners[ i ];
                    listener.downloadFileAdded( position );
                }
            }
        });
    }

    private void fireDownloadFileRemoved( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                DownloadFilesChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (DownloadFilesChangeListener)listeners[ i ];
                    listener.downloadFileRemoved( position );
                }
            }
        });
    }

    public void fireDownloadFileChanged( SWDownloadFile file )
    {
        int position = downloadList.indexOf( file );
        if ( position >= 0 )
        {
            fireDownloadFileChanged( position );
        }
    }
    ///////////////////// END event handling methods ////////////////////////
    
    
    /**
     * Class is responsible for launching and stopping download worker as 
     * required.
     */
    private class DownloadWorkerLauncher extends Thread
    {
        public DownloadWorkerLauncher()
        {
            super( "DownloadWorkerLauncher" );
        }
        
        public void run()
        {
            while ( !isManagerShutingDown )
            {
                try
                {
                    createRequiredWorker();
                    waitForNextCycle();
                }
                catch ( Throwable th )
                {
                    NLogger.error(NLoggerNames.Download_Manager, th, th );
                }
            }
        }
        
        public synchronized void triggerCycle()
        {
            this.notify();
        }
        
        private synchronized void waitForNextCycle() 
            throws InterruptedException
        {
            wait( 2000 );
        }
        
        public void createRequiredWorker()
        {
            synchronized ( SwarmingManager.this )
            {
                int requiredCount = getRequiredDownloadWorkerCount();
                if ( temporaryWorker == null && workerList.size() < requiredCount )
                {// we have not enough workers... create some more
                    temporaryWorker = new SWDownloadWorker( );
                    temporaryWorker.setTemporaryWorker( true );
                    temporaryWorker.startWorker();
                    workerList.add( temporaryWorker );
                    NLogger.debug( NLoggerNames.Download_Manager, 
                        "Creating new worker: " + temporaryWorker 
                        + " for a total of: " + workerList.size() );
                }
            }
        }        
    }
    
    
    
    private class LoadDownloadListJob extends Thread
    {
        public LoadDownloadListJob()
        {
            super("LoadDownloadListJob");
        }
        
        public void run()
        {
            try
            {
                loadDownloadList();
            }
            catch ( Throwable th )
            {
                NLogger.error( NLoggerNames.Download_Manager, th, th );
            }
        }
        
        private void loadDownloadList()
        {
            NLogger.debug(NLoggerNames.Download_Manager,
                "Loading download list..." );
    
            // JAXB-BETA way
            File downloadFile = Environment.getInstance().getPhexConfigFile(
                EnvironmentConstants.XML_DOWNLOAD_FILE_NAME );
            File downloadFileBak = new File(downloadFile.getAbsolutePath() + ".bak");
    
            if ( !downloadFile.exists() && !downloadFileBak.exists() )
            {
                NLogger.debug(NLoggerNames.Download_Manager,
                    "No download list file found." );
                return;
            }
            
            XJBPhex phex;
            try
            {
                NLogger.debug(NLoggerNames.Download_Manager,
                    "Try to load from default download list." );
                FileManager fileMgr = FileManager.getInstance();
                ManagedFile managedFile = fileMgr.getReadWriteManagedFile( downloadFile );
                phex = XMLBuilder.loadXJBPhexFromFile( managedFile );
    
                if ( phex == null )
                {
                    NLogger.debug(NLoggerNames.Download_Manager,
                        "Try to load from backup download list." );
                    ManagedFile managedFileBak = fileMgr.getReadWriteManagedFile( downloadFileBak );
                    phex = XMLBuilder.loadXJBPhexFromFile( managedFileBak );
                }
                if ( phex == null )
                {
                    NLogger.debug( NLoggerNames.Download_Manager,
                        "No download settings file found." );
                    return;
                }
                
                XJBSWDownloadList list = phex.getSWDownloadList();
                if (list != null)
                {
                    String phexVersion = phex.getPhexVersion();
                    // we have a different download list format after Phex 2.6.4.89.
                    if ( VersionUtils.compare("2.6.4.89", phexVersion ) >= 0 )
                    {
                        load2_6_4_89SWDownloadList( list );
                    }
                    else
                    {
                        loadXJBSWDownloadList( list );
                    }
                }
                else
                {
                    NLogger.debug(NLoggerNames.Download_Manager,
                        "No SWDownloadList found.");
                }
                notifyWaitingWorkers();
            }
            catch ( JAXBException exp )
            {
                Throwable linkedException = exp.getLinkedException();
                if ( linkedException != null )
                {
                    NLogger.error( NLoggerNames.Download_Manager, linkedException, linkedException );
                }
                NLogger.error( NLoggerNames.Download_Manager, exp, exp );
                Environment.getInstance().fireDisplayUserMessage( 
                    UserMessageListener.DownloadSettingsLoadFailed, 
                    new String[]{ exp.toString() } );
                return;
            }
            catch ( ManagedFileException exp )
            {
                NLogger.error( NLoggerNames.Download_Manager, exp, exp );
                Environment.getInstance().fireDisplayUserMessage( 
                    UserMessageListener.DownloadSettingsLoadFailed, 
                    new String[]{ exp.toString() } );
                return;
            }
        }
        
        private void loadXJBSWDownloadList( XJBSWDownloadList list )
        {
            synchronized( downloadList )
            {
                NLogger.debug(NLoggerNames.Download_Manager,
                    "Loading SWDownload xml" );
                downloadList.clear();
                urnToDownloadMap.clear();
                SWDownloadFile file;
                XJBSWDownloadFile xjbFile;
                Iterator iterator = list.getSWDownloadFileList().iterator();
                
                while( iterator.hasNext() )
                {
                    try
                    {
                        xjbFile = (XJBSWDownloadFile) iterator.next();
                        file = new SWDownloadFile( xjbFile );
                        int pos = downloadList.size();
                        downloadList.add( file );
                        URN urn = file.getFileURN();
                        if ( urn != null )
                        {
                            urnToDownloadMap.put( urn, file );
                        }
                        NLogger.debug(NLoggerNames.Download_Manager,
                            "Loaded SWDownloadFile: " + file );
                        fireDownloadFileAdded( pos );
                    }
                    catch ( Exception exp )
                    {// catch all exception in case we have an error in the XML
                        NLogger.error( NLoggerNames.Download_Manager, 
                            "Error loading a download file from XML.", exp );
                    }
                }
            }
        }
        
        /**
         * @deprecated since 2.6.4.89
         * @param list
         */
        private void load2_6_4_89SWDownloadList( XJBSWDownloadList list )
        {
            synchronized( downloadList )
            {
                NLogger.debug(NLoggerNames.Download_Manager,
                    "Loading old download xml." );
                ObjectFactory objFactory = new ObjectFactory();
                downloadList.clear();
                urnToDownloadMap.clear();
                SWDownloadFile file;
                XJBSWDownloadFile xjbFile;
                XJBSWDownloadSegment xjbSegment;
                Iterator iterator = list.getSWDownloadFileList().iterator();
                while( iterator.hasNext() )
                {
                    try
                    {
                        xjbFile = (XJBSWDownloadFile) iterator.next();
                        
                        String destFileName = ServiceManager.sCfg.mDownloadDir 
                            + File.separator + xjbFile.getLocalFileName();
                        File destFile = new File( destFileName );
                        File incFile = SWDownloadFile.createIncompleteFile( destFile );
                        RandomAccessFile raFile = new RandomAccessFile( incFile, "rwd" );
                        
                        XJBSWDownloadFile xjbNewFile = objFactory.createXJBSWDownloadFile();
                        xjbNewFile.setIncompleteFileName( incFile.getAbsolutePath() );
                        xjbNewFile.setCreatedTime( xjbFile.getCreatedTime() );
                        xjbNewFile.setFileSize( xjbFile.getFileSize() );
                        xjbNewFile.setFileURN( xjbFile.getFileURN() );
                        xjbNewFile.setLocalFileName( xjbFile.getLocalFileName() );
                        xjbNewFile.setModifiedTime( xjbFile.getModifiedTime() );
                        xjbNewFile.setSearchTerm( xjbFile.getSearchTerm() );
                        xjbNewFile.setStatus( xjbFile.getStatus() );
                        xjbNewFile.getCandidateList().addAll( xjbFile.getCandidateList() );
                        
                        ArrayList tempSegmentList = new ArrayList();
                        Iterator segmentIterator = xjbFile.getSegmentList().iterator();
                        while ( segmentIterator.hasNext() )
                        {// copy segments into a sorted List... then link segments correctly...
                            tempSegmentList.add( segmentIterator.next() );
                        }
                        Collections.sort( tempSegmentList, new SWDownloadSegmentComparatorByStartPosition() );

                        // merge segments into a single file.
                        segmentIterator = tempSegmentList.iterator();
                        while ( segmentIterator.hasNext() )
                        {
                            try
                            {
                                xjbSegment = (XJBSWDownloadSegment) segmentIterator.next();
                                long transferDataSize = xjbSegment.getLength();
                                long startPos = xjbSegment.getStartPosition();
                                long transferredDataSize = 0;
                                String incompleteFileName = xjbSegment.getIncompleteFileName();
                                if ( !StringUtils.isEmpty( incompleteFileName ) )
                                {
                                    File incompleteFile = new File( xjbSegment.getIncompleteFileName() );
                                    if ( incompleteFile.exists() )
                                    {
                                        transferredDataSize = incompleteFile.length();
                                        if ( transferredDataSize == 0 )
                                        {
                                            continue;
                                        }
                                        if ( transferDataSize != SWDownloadConstants.UNKNOWN_FILE_SIZE && 
                                             transferredDataSize > transferDataSize )
                                        {// log error... truncate original file and reduce transferred data size
                                            try
                                            {
                                                FileUtils.truncateFile( incompleteFile, transferDataSize );
                                                transferredDataSize = transferDataSize;
                                            }
                                            catch ( IOException exp )
                                            {
                                                NLogger.error( NLoggerNames.Download, exp, exp);
                                            }
                                        }
                                        // write segment into download file.
                                        raFile.setLength( startPos + transferredDataSize );
                                        raFile.seek( startPos );
                                        FileInputStream fIn = new FileInputStream( incompleteFile );
                                        byte[] buffer = new byte[16*1024];
                                        int len;
                                        while( (len = fIn.read( buffer, 0, buffer.length )) > 0 )
                                        {
                                            raFile.write(buffer, 0, len);
                                        }
                                        fIn.close();
                                        incompleteFile.delete();
                                        XJBDownloadScope scope = objFactory.createXJBDownloadScope();
                                        scope.setStart( startPos );
                                        scope.setEnd( startPos + transferredDataSize -1 );
                                        xjbNewFile.getFinishedScopesList().add( scope );
                                    }
                                }
                            }
                            catch ( Throwable th )
                            {
                                NLogger.error(NLoggerNames.Download_Manager,
                                    "Failed to convert segment.", th );
                            }
                        }
                        raFile.close();
                        
                        file = new SWDownloadFile( xjbNewFile );
                        int pos = downloadList.size();
                        downloadList.add( file );
                        URN urn = file.getFileURN();
                        if ( urn != null )
                        {
                            urnToDownloadMap.put( urn, file );
                        }
                        NLogger.debug(NLoggerNames.Download_Manager,
                            "Converted SWDownloadFile: " + file );
                        fireDownloadFileAdded( pos );
                    }
                    catch ( Exception exp )
                    {// catch all exception in case we have an error in the XML
                        NLogger.error( NLoggerNames.Download_Manager, 
                            "Failed to convert a download file from XML.", exp );
                    }
                }
            }
        }
    }
    
    private class SaveDownloadListTimer extends TimerTask
    {
        // once per minute
        public static final long TIMER_PERIOD = 1000 * 20;
        
        public void run()
        {
            try
            {
                triggerSaveDownloadList( false );
            }
            catch ( Throwable th )
            {
                NLogger.error( NLoggerNames.Download_Manager, th, th );
            }
        }
    }

    private class SaveDownloadListJob extends Thread
    {
        private boolean isFollowUpSaveTriggered;

        public SaveDownloadListJob()
        {
            super( ThreadTracking.rootThreadGroup, "SaveDownloadListJob" );
            setPriority( Thread.MIN_PRIORITY );
        }

        public void triggerFollowUpSave()
        {
            isFollowUpSaveTriggered = true;
        }

        /**
         * Saving of the download list is done asynchronously to make sure that there
         * will be no deadlocks happening
         */
        public void run()
        {
            do
            {
                NLogger.debug(NLoggerNames.Download_Manager,
                    "Start saving download list..." );
                downloadListChangedSinceSave = false;
                isFollowUpSaveTriggered = false;
                // JAXB-beta way
                try
                {
                    ObjectFactory objFactory = new ObjectFactory();
                    XJBPhex phex = objFactory.createPhexElement();

                    XJBSWDownloadList list = createXJBSWDownloadList();
                    phex.setSWDownloadList( list );
                    phex.setPhexVersion( VersionUtils.getFullProgramVersion() );

                    File downloadFile = Environment.getInstance().getPhexConfigFile(
                        EnvironmentConstants.XML_DOWNLOAD_FILE_NAME );
                    File downloadFileBak = new File(downloadFile.getAbsolutePath() + ".bak");
                    
                    ManagedFile managedFile = FileManager.getInstance().getReadWriteManagedFile( downloadFileBak );
                    XMLBuilder.saveToFile( managedFile, phex );

                    // Write to a backup file and copy over to ensure that at
                    // least one valid download file always exists.
                    FileUtils.copyFile( downloadFileBak, downloadFile );
                }
                catch ( JAXBException exp )
                {
                    // TODO3 during close this message is never displayed since application
                    // will exit too fast. A solution to delay exit process in case 
                    // SlideInWindows are open needs to be found.
                    NLogger.error( NLoggerNames.Download_Manager, exp, exp );
                    Environment.getInstance().fireDisplayUserMessage( 
                        UserMessageListener.DownloadSettingsSaveFailed, 
                        new String[]{ exp.toString() } );
                }
                catch ( ManagedFileException exp )
                {
                    // TODO3 during close this message is never displayed since application
                    // will exit too fast. A solution to delay exit process in case 
                    // SlideInWindows are open needs to be found.
                    NLogger.error( NLoggerNames.Download_Manager, exp, exp );
                    Environment.getInstance().fireDisplayUserMessage( 
                        UserMessageListener.DownloadSettingsSaveFailed, 
                        new String[]{ exp.toString() } );
                }
                catch ( IOException exp )
                {
                    // TODO3 during close this message is never displayed since application
                    // will exit too fast. A solution to delay exit process in case 
                    // SlideInWindows are open needs to be found.
                    NLogger.error( NLoggerNames.Download_Manager, exp, exp );
                    Environment.getInstance().fireDisplayUserMessage( 
                        UserMessageListener.DownloadSettingsSaveFailed, 
                        new String[]{ exp.toString() } );
                }
            }
            while( isFollowUpSaveTriggered );
            NLogger.debug(NLoggerNames.Download_Manager,
                "Finished saving download list..." );

            synchronized( saveDownloadListLock )
            {
                // give created instance free once we are finished..
                saveDownloadListJob = null;
            }
        }

        private XJBSWDownloadList createXJBSWDownloadList()
            throws JAXBException
        {
            ObjectFactory objFactory = new ObjectFactory();
            XJBSWDownloadList swDownloadList = objFactory.createXJBSWDownloadList();
            synchronized( downloadList )
            {
                Iterator iterator = downloadList.iterator();
                List list = swDownloadList.getSWDownloadFileList();
                while ( iterator.hasNext() )
                {
                    SWDownloadFile file = (SWDownloadFile) iterator.next();
                    list.add( file.createXJBSWDownloadFile() );
                }
            }
            return swDownloadList;
        }
    }
    
    /**
     * @deprecated since 2.6.4.89
     */
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
