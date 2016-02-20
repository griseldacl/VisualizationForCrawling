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
 *  $Id: SWDownloadWorker.java,v 1.83 2005/11/13 22:21:24 gregork Exp $
 */
package phex.download.swarming;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.List;

import phex.common.ServiceManager;
import phex.common.ThreadPool;
import phex.common.address.IpAddress;
import phex.connection.ConnectionFailedException;
import phex.connection.NetworkManager;
import phex.download.*;
import phex.host.UnusableHostException;
import phex.http.HTTPMessageException;
import phex.net.presentation.SocketFacade;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

public class SWDownloadWorker implements Runnable
{
    /**
     * A temporary worker indicates a worker that is used to wait for a valid
     * download set. Only one temporary worker should be in the system. Once
     * a valid download set is found the worker will lose its temporary status.
     * This flag will help to limit the worker count and only hold as many
     * workers as required and necessary.
     */
    private boolean isTemporaryWorker;

    private boolean isRunning;
    
    private ThreadPool.Job threadJob;

    private DownloadEngine downloadEngine;
    
    /**
     * Indicates if the download worker is inside the critical download section.
     * The critical section is the section in which modifications to the download
     * segemnt occure. Before a Phex shutdown a worker thread needs to finish 
     * cleanly to ensure that not curruption to segment data occures. (Invalid
     * segment sizes).
     */
    private boolean insideCriticalSection;
    
    /**
     * Indicates if the download was stoped from externally.
     * Usually per user request.
     */
    private boolean isDownloadStopped;

    public SWDownloadWorker()
    {
    }

    /**
     * Sets the temporary worker status.
     * @param state
     * @see isTemporaryWorker
     */
    public void setTemporaryWorker(boolean state)
    {
        isTemporaryWorker = state;
    }

    /**
     * Returns the temporary worker status.
     * @return the temporary worker status.
     */
    public boolean isTemporaryWorker()
    {
        return isTemporaryWorker;
    }

    public boolean isInsideCriticalSection()
    {
        return insideCriticalSection;
    }
    
    public void run()
    {
        SwarmingManager swarmingMgr = SwarmingManager.getInstance();
        try
        {
            SWDownloadSet downloadSet;
            
            while (isRunning)
            {
                boolean isStopped = swarmingMgr.checkToStopWorker(this);
                if ( isStopped )
                {
                    break;
                }
                
                List list = swarmingMgr.getCompletedDownloadFiles();
                moveDownloadFilesToDestination( list );
                
                isDownloadStopped = false;
                NLogger.debug(NLoggerNames.Download_Worker, 
                    " - Allocating DownloadSet - " + this );
                downloadSet = swarmingMgr.allocateDownloadSet(this);
                if ( downloadSet == null )
                {
                    if ( isTemporaryWorker )
                    {
                        try
                        {
                            swarmingMgr.waitForNotify();
                        }
                        catch (InterruptedException e)
                        {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        continue;
                    }
                    else
                    {
                        // no download set aquired after handling last download...
                        // break away from further trying...
                        break;
                    }
                }
                NLogger.debug(NLoggerNames.Download_Worker,
                    "Allocated DownloadSet: "
                    + downloadSet.toString() + " - " + this);
                try
                {
                    handleDownload(downloadSet);
                }
                finally
                {
                    if ( downloadSet != null )
                    {
                        NLogger.debug(NLoggerNames.Download_Worker,
                            "Releasing DownloadSet: "
                                + downloadSet.toString() + " - " + this);
                        downloadSet.releaseDownloadSet();
                    }
                }
            }
        }
        finally
        {
            // if the worker should run... give notice about crash
            swarmingMgr.notifyWorkerShoutdown(this, !isRunning);
            
            NLogger.debug(NLoggerNames.Download_Worker,
                "Download worker finished: " + this);
        }
    }

    public void startWorker()
    {
        isRunning = true;
        threadJob = ThreadPool.getInstance().addJob( this,
            "SWDownloadWorker-" + Integer.toHexString(hashCode()) );
        NLogger.debug(NLoggerNames.Download_Worker,
            "Started SWDownloadWorker " + this);
    }

    public void stopWorker()
    {
        NLogger.debug(NLoggerNames.Download_Worker,
            "Download worker has been instructed to stop running: " + this);
        isRunning = false;
        isDownloadStopped = true;
        stopDownloadInternal();
        Thread thread = threadJob.getAssociatedThread();
        if ( thread != null )
        {
            thread.interrupt();
        }
    }
    
    public void waitTillFinished()
    {
        while( threadJob.getAssociatedThread() != null )
        {
            threadJob.waitForAssociatedThreadRelease();  
        }
    }

    public boolean isRunning()
    {
        return isRunning;
    }
    
    private void stopDownloadInternal()
    {
        if ( downloadEngine != null )
        {
            NLogger.debug(NLoggerNames.Download_Worker,
                "Download worker has been instructed to stop downloading: " + this);
            downloadEngine.stopDownload();
            downloadEngine = null;
        }
    }

    /**
     * Handles a specific SWDownloadSet to start the download for.
     * @param downloadSet the download set containing the download configuration.
     */
    private void handleDownload(SWDownloadSet downloadSet)
    {
        NLogger.debug(NLoggerNames.Download_Worker,
            "handleDownload() with: " + downloadSet + " - " + this);
        SWDownloadFile downloadFile = downloadSet.getDownloadFile();
        SWDownloadCandidate downloadCandidate = downloadSet
            .getDownloadCandidate();
        
        if (downloadCandidate.getResourceURN() == null 
            && downloadCandidate.getFileIndex() < 0)
        {
            NLogger.warn(NLoggerNames.Download_Worker,
                "Download candidate has no associated file and URN is null:"
                    + downloadCandidate);
            downloadCandidate.addToCandidateLog(
                "Download candidate has no associated file and URN is null.");
            return;
        }
        
        if ( !isRunning || isDownloadStopped )
        {
            return;
        }
        
        if ( downloadCandidate.isPushNeeded() )
        {
            connectDownloadEngineViaPush( downloadSet, false );
        }
        else
        {
            connectDownloadEngine(downloadSet);
        }

        if ( downloadEngine == null ) { return; }
        if ( !isRunning || isDownloadStopped ) { return; }
        try
        {
            insideCriticalSection = true;
            startDownload(downloadSet);
        }
        catch (IOException exp)
        {
            // this is only temporary for testing to let the download sleep for a while...
            // downloadCandidate.setStatus( SWDownloadConstants.STATUS_CANDIDATE_BUSY );
            downloadCandidate.addToCandidateLog( exp.toString() );
            downloadCandidate
                .setStatus(SWDownloadConstants.STATUS_CANDIDATE_WAITING);
            NLogger.debug( NLoggerNames.Download_Worker, downloadCandidate );
            NLogger.debug( NLoggerNames.Download_Worker, exp.toString(), exp);
        }
        finally
        {
            // unset possible queued candidate...
            downloadFile.removeQueuedCandidate( downloadCandidate );
            
            stopDownloadInternal();
            NLogger.debug(NLoggerNames.Download_Worker,
                "Releasing DownloadSegment: " + downloadSet.toString() + " - " + this);
            downloadSet.releaseDownloadSegment();
            // segment download completed
            downloadFile.verifyStatus();
            if ( downloadFile.isFileCompleted() )
            {
                downloadFile.moveToDestinationFile();
            }
            insideCriticalSection = false;
        }
    }

    /**
     * Connects the download engine to the host with a direct connection.
     */
    private void connectDownloadEngine(SWDownloadSet downloadSet)
    {
        if ( !isRunning || isDownloadStopped ) { return; }
        
        NLogger.debug(NLoggerNames.Download_Worker,
            "connectDownloadEngine with: " + downloadSet + " - " + this);
        SWDownloadCandidate downloadCandidate = downloadSet
            .getDownloadCandidate();
        SWDownloadFile downloadFile = downloadSet.getDownloadFile();

        // invalidate the download engine
        downloadEngine = null;
        downloadEngine = new DownloadEngine(downloadFile, downloadCandidate);
        if ( !isRunning || isDownloadStopped ) { return; }
        try
        {
            // this call sets the STATUS_CANDIDATE_CONNECTING status when it is
            // performaing the connect operation.
            downloadEngine.connect( ServiceManager.sCfg.socketConnectTimeout );
        }
        catch (ConnectionFailedException exp)
        {
            // indicates a general communication error while connecting
            downloadCandidate.addToCandidateLog( exp.toString() );
            NLogger.debug( NLoggerNames.Download_Worker, exp.toString() );
            
            // trying push - setting failed status is directed to connectDownloadEngineViaPush()
            connectDownloadEngineViaPush( downloadSet, true );
            return;
        }
        catch (SocketTimeoutException exp)
        {
            // indicates a general communication error while connecting
            downloadCandidate.addToCandidateLog( exp.toString() );
            NLogger.debug( NLoggerNames.Download_Worker, exp.toString() );

            // trying push - setting failed status is directed to connectDownloadEngineViaPush()
            connectDownloadEngineViaPush( downloadSet, true );
            return;
        }
        catch ( DownloadStoppedException exp )
        {
            downloadCandidate.addToCandidateLog( exp.toString() );
            downloadCandidate.setStatus(
                SWDownloadConstants.STATUS_CANDIDATE_CONNECTION_FAILED );
            NLogger.debug( NLoggerNames.Download_Worker, exp.toString() );
            // stop download and set to null so that download is not starting.
            stopDownloadInternal();
            return;
        }
        catch (IOException exp)
        {
            downloadCandidate.addToCandidateLog( exp.toString() );
            // TODO3 log this as error to handle different cases on some try 
            // again on others remove
            NLogger.error( NLoggerNames.Download_Worker, "HardError at Host: "
                + downloadCandidate.getHostAddress()
                + " Vendor: " + downloadCandidate.getVendor(),
                exp );

            // stop download and set to null so that download is not starting.
            stopDownloadInternal();

            // unknown error trying a push - setting failed status is directed
            // to connectDownloadEngineViaPush()
            connectDownloadEngineViaPush( downloadSet, true );
            return;
        }
    }

    /**
     * Connectes the download engine via a push request.
     * @param downloadSet the download set to use.
     * @param failedBefore if true a previous standard connection try failed just
     *        before this try, false otherwise. This is used to determine the right
     *        status combinations to set and prevent double count of a single failed
     *        connection try.
     */
    private void connectDownloadEngineViaPush( SWDownloadSet downloadSet, boolean failedBefore )
    {
        if ( !isRunning || isDownloadStopped ) { return; }
        
        NLogger.debug(NLoggerNames.Download_Worker,
            "connectDownloadEngineViaPush with: " + downloadSet + " - " + this);
        SWDownloadCandidate downloadCandidate = downloadSet.getDownloadCandidate();
        SWDownloadFile downloadFile = downloadSet.getDownloadFile();

        // invalidate the download engine
        downloadEngine = null;
        
        IpAddress ipAddress = downloadCandidate.getHostAddress().getIpAddress();
        // indicate if the candidate might be reachable through LAN
        boolean isLANReachable = ServiceManager.sCfg.connectedToLAN 
            && ipAddress != null && ipAddress.isSiteLocalIP();

        // if we are behind a firewall there is no chance to successfully push
        // if the candidate is not reachable through lan.
        if ( !NetworkManager.getInstance().hasConnectedIncoming() 
             && !isLANReachable )
        {
            NLogger.debug( NLoggerNames.Download_Worker,
                this.toString() + downloadCandidate.toString()
                + " Cant PUSH -> I'm firewalled and candidate not reachable by LAN" );
            downloadCandidate.addToCandidateLog( 
                "Cant PUSH -> I'm firewalled and candidate not reachable by LAN" );
            downloadCandidate.setStatus(
                SWDownloadConstants.STATUS_CANDIDATE_CONNECTION_FAILED);
            // candidate must have push. can't directly connect
            if ( downloadCandidate.isPushNeeded() )
            {
                downloadFile.markCandidateBad( downloadCandidate );
                //downloadFile.markCandidateIgnored( downloadCandidate, 
                //    "CandidateStatusReason_PushRequired");
                // no bad alt loc in this case... others might connect correct...
            }
            return;
        }
        if ( downloadCandidate.getGUID() == null )
        {
            NLogger.debug( NLoggerNames.Download_Worker,
                this.toString() + downloadCandidate.toString()
                + " Cant PUSH -> No candidate GUID." );
            downloadCandidate.addToCandidateLog( 
                "Cant PUSH -> No candidate GUID." );
            downloadCandidate.setStatus(
                SWDownloadConstants.STATUS_CANDIDATE_CONNECTION_FAILED);
            return;
        }
        if ( !isRunning || isDownloadStopped ) 
        {
            if ( failedBefore )
            {
                downloadCandidate.setStatus(SWDownloadConstants.STATUS_CANDIDATE_CONNECTION_FAILED);
            }
            return;
        }
        downloadCandidate.setStatus(
            SWDownloadConstants.STATUS_CANDIDATE_PUSH_REQUEST);
        SocketFacade socket = PushHandler.requestSocketViaPush( downloadCandidate );
        if ( socket == null )
        {
            downloadCandidate.setStatus(
                SWDownloadConstants.STATUS_CANDIDATE_CONNECTION_FAILED);
            // candidate must have push. cant directly connect
            if ( downloadCandidate.isPushNeeded() )
            {
                downloadFile.markCandidateIgnored( downloadCandidate, 
                    "CandidateStatusReason_PushRouteFailed" );
                // no bad alt loc in this case... others might connect correct...
            }
            NLogger.debug( NLoggerNames.Download_Worker,
                "Push request fails for candidate: " + downloadCandidate );
            downloadCandidate.addToCandidateLog( 
                "Push request fails for candidate: " + downloadCandidate );
            return;
        }
        downloadEngine = new DownloadEngine( downloadFile,
            downloadCandidate );
        if ( !isRunning || isDownloadStopped ) { return; }
        try
        {
            downloadEngine.setSocket( socket );
        }
        catch ( IOException exp )
        {
            downloadCandidate.addToCandidateLog( exp.toString() );
            downloadCandidate.setStatus(
                SWDownloadConstants.STATUS_CANDIDATE_CONNECTION_FAILED );
            // TODO3 log this as error to handle different cases on some try 
            // again on others remove
            NLogger.error( NLoggerNames.Download_Worker, "HardError at Host: "
                + downloadCandidate.getHostAddress()
                + " Vendor: " + downloadCandidate.getVendor(), exp );
            // stop and set to null so that download is not starting.
            stopDownloadInternal();
            return;
        }
    }

    private void exchangeHTTPHandshake(SWDownloadSet downloadSet)
    {
        NLogger.debug(NLoggerNames.Download_Worker,
            "exchangeHTTPHandshake with: " + downloadSet + " - " + this);
        SWDownloadCandidate downloadCandidate = downloadSet
            .getDownloadCandidate();
        SWDownloadFile downloadFile = downloadSet.getDownloadFile();
        SWDownloadSegment downloadSegment;
        try
        {
            downloadCandidate.setStatus(
                SWDownloadConstants.STATUS_CANDIDATE_REQUESTING );
            downloadSegment = downloadSet.allocateDownloadSegment(this);
            if ( downloadSegment == null )
            {// no more segments found...
                NLogger.debug(NLoggerNames.Download_Worker,
                    "No segment to allocate found");
                downloadCandidate.addToCandidateLog( "No segment to allocate found." );
                downloadCandidate.setStatus(
                    SWDownloadConstants.STATUS_CANDIDATE_WAITING );
                // set to null so that download is not starting.
                downloadEngine = null;
                return;
            }
            else if ( downloadEngine == null )
            {
                downloadCandidate.addToCandidateLog( "Connection interrupted." );
                downloadCandidate.setStatus(SWDownloadConstants.STATUS_CANDIDATE_WAITING);
                return;
            }
            downloadEngine.exchangeHTTPHandshake(downloadSegment);
        }
        catch ( ReconnectException exp )
        {
            downloadCandidate.addToCandidateLog( exp.toString() );
            NLogger.debug( NLoggerNames.Download_Worker, downloadCandidate
                + " " + exp.getMessage());
            // a simple reconnect should be enough here...
            // stop and set to null so that download is not starting.
            stopDownloadInternal();
            downloadCandidate.setStatus(SWDownloadConstants.STATUS_CANDIDATE_WAITING);
        }
        catch (RemotelyQueuedException exp)
        {
            downloadCandidate.addToCandidateLog( exp.toString() );
            
            // dont set download engine to null... we need it to stay connected.

            // release download segment... for others... we will get a new one on
            // next try.
            NLogger.debug(NLoggerNames.Download_Worker,
                "Releasing DownloadSegment (Queued): " + downloadSet.toString() 
                + " - " + this);
            downloadSet.releaseDownloadSegment();
            // must first set queue parameters to update waiting time when settings
            // status.
            downloadCandidate.updateXQueueParameters(exp.getXQueueParameters());
            downloadCandidate.setStatus(
                SWDownloadConstants.STATUS_CANDIDATE_REMOTLY_QUEUED);
        }
        catch ( RangeUnavailableException exp )
        {
            downloadCandidate.addToCandidateLog( exp.toString() );
            NLogger.debug( NLoggerNames.Download_Worker, exp.toString() + " :: "
                + downloadCandidate  );
            
            boolean isScopeListAvailable = 
                downloadCandidate.getAvailableScopeList() != null &&
                downloadCandidate.getAvailableScopeList().size() > 0;
            // we can retry immediatly if we have a filled available scope list
            // and a scope in this list is allocatable.
            if ( isScopeListAvailable && 
                 downloadFile.isScopeAllocateable( downloadCandidate.getAvailableScopeList() ) )
            {
                // release download segment... for others... we will get a new one on
                // next try.
                NLogger.debug(NLoggerNames.Download_Worker,
                    "Releasing DownloadSegment (Range): " + downloadSet.toString() 
                    + " - " + this);
                downloadSet.releaseDownloadSegment();
                downloadCandidate.setStatus( 
                    SWDownloadConstants.STATUS_CANDIDATE_RANGE_UNAVAILABLE);
            }
            else
            {
                // stop and set to null so that download is not starting.
                stopDownloadInternal();
                int waitTime = exp.getWaitTimeInSeconds() > 0 ? exp.getWaitTimeInSeconds() : -1;
                downloadCandidate.setStatus( 
                    SWDownloadConstants.STATUS_CANDIDATE_RANGE_UNAVAILABLE,
                    waitTime );
            }
        }
        catch ( HostBusyException exp )
        {
            NLogger.debug( NLoggerNames.Download_Worker, downloadCandidate
                + " " + exp.getMessage());
            // stop and set to null so that download is not starting.
            stopDownloadInternal();
            downloadCandidate.setStatus(
                SWDownloadConstants.STATUS_CANDIDATE_BUSY, exp
                    .getWaitTimeInSeconds());
            return;
        }
        catch (UnusableHostException exp)
        {
            downloadCandidate.addToCandidateLog( exp.toString() );
            // stop and set to null so that download is not starting.
            stopDownloadInternal();
            // file not available or wrong http header.
            NLogger.debug( NLoggerNames.Download_Worker, exp, exp );
            NLogger.debug( NLoggerNames.Download_Worker,
                "Removing download candidate: " + downloadCandidate);
            if ( exp instanceof FileNotAvailableException )
            {
                downloadFile.markCandidateIgnored(downloadCandidate, 
                    "CandidateStatusReason_FileNotFound");
            }
            else
            {
                downloadFile.markCandidateIgnored(downloadCandidate, 
                    "CandidateStatusReason_Unusable");
            }
            downloadFile.addBadAltLoc(downloadCandidate);
            return;
        }
        catch (HTTPMessageException exp)
        {
            downloadCandidate.addToCandidateLog( exp.toString() );
            // stop and set to null so that download is not starting.
            stopDownloadInternal();
            // file not available or wrong http header.
            NLogger.warn( NLoggerNames.Download_Worker, exp, exp );
            downloadFile.markCandidateIgnored(downloadCandidate, 
                "CandidateStatusReason_HTTPError");
            downloadFile.addBadAltLoc(downloadCandidate);
            return;
        }
        catch (SocketTimeoutException exp)
        {
            downloadCandidate.addToCandidateLog( exp.toString() );
            NLogger.debug( NLoggerNames.Download_Worker, exp, exp );
            downloadCandidate.setStatus(
                SWDownloadConstants.STATUS_CANDIDATE_CONNECTION_FAILED);
            // stop and set to null so that download is not starting.
            stopDownloadInternal();
            return;
        }
        catch (SocketException exp)
        {
            downloadCandidate.addToCandidateLog( exp.toString() );
            NLogger.debug( NLoggerNames.Download_Worker, exp, exp );
            downloadCandidate.setStatus(
                SWDownloadConstants.STATUS_CANDIDATE_CONNECTION_FAILED);
            // stop and set to null so that download is not starting.
            stopDownloadInternal();
            return;
        }
        catch (IOException exp)
        {
            downloadCandidate.addToCandidateLog( exp.toString() );
            downloadCandidate.setStatus(
                SWDownloadConstants.STATUS_CANDIDATE_CONNECTION_FAILED);
            // TODO3 handle different cases on some try again on others remove
            NLogger.warn( NLoggerNames.Download_Worker,
                "Error at Host: "
                + downloadCandidate.getHostAddress()
                + " Vendor: " + downloadCandidate.getVendor(), exp );
            // stop and set to null so that download is not starting.
            stopDownloadInternal();
            return;
        }
    }

    /**
     * Execute the actual download routine.
     */
    private void startDownload(SWDownloadSet downloadSet) throws IOException
    {
        NLogger.debug(NLoggerNames.Download_Worker,
            "startDownload with: " + downloadSet + " - " + this);
        SWDownloadFile downloadFile = downloadSet.getDownloadFile();
        SWDownloadCandidate downloadCandidate = downloadSet
            .getDownloadCandidate();
        
        downloadCandidate.addToCandidateLog( "Start download." );
        
        // we came that far proves that we can successful connect to this candidate
        // we can use it as good alt loc
        // in cases where the http handshake revises this determination the
        // alt loc will be adjusted accordingly.
        downloadFile.addGoodAltLoc(downloadCandidate);
        downloadFile.markCandidateGood(downloadCandidate);
        
        /*if ( downloadFile.getTotalDataSize() 
            == SWDownloadConstants.UNKNOWN_FILE_SIZE && 
            downloadFile instanceof URLDownloadFile )
        {
            // request file size via head...
            //determineFileSizeViaHead( (URLDownloadFile)downloadFile,
            //    downloadCandidate );
        }*/
        
        do
        {
            do
            {
                exchangeHTTPHandshake(downloadSet);
                if ( downloadEngine == null ) { return; }
                if ( downloadCandidate.isRemotlyQueued() )
                {
                    boolean succ = downloadFile.addAndValidateQueuedCandidate(
                        downloadCandidate );
                    if ( !succ )
                    {
                        // stop and set to null so that download is not starting.
                        stopDownloadInternal();
                        return;
                    }
                    try
                    {
                        Thread.sleep(downloadCandidate.getXQueueParameters()
                            .getRequestSleepTime());
                        if ( downloadEngine == null )
                        {// download stopped in the meantime.
                            NLogger.debug( NLoggerNames.Download_Worker,
                                "Download stopped while waiting for queue.");
                            return;
                        }
                    }
                    catch (InterruptedException exp)
                    {// interrupted while sleeping
                        NLogger.debug( NLoggerNames.Download_Worker,
                            "Interrupted Worker sleeping for queue.");
                        downloadCandidate
                            .setStatus(SWDownloadConstants.STATUS_CANDIDATE_CONNECTION_FAILED);
                        // stop and set to null so that download is not starting.
                        stopDownloadInternal();
                        return;
                    }
                }
            }
            while (downloadCandidate.isRemotlyQueued() || downloadCandidate.isRangeUnavailable() );
            
            // unset possible queued candidate...
            downloadFile.removeQueuedCandidate( downloadCandidate );
            
            downloadFile.setStatus(SWDownloadConstants.STATUS_FILE_DOWNLOADING);
            downloadSet.getDownloadCandidate().setStatus(
                SWDownloadConstants.STATUS_CANDIDATE_DOWNLOADING);

            SWDownloadSegment downloadSegment = downloadSet
                .getDownloadSegment();
            downloadEngine.startDownload( );

            // segment download completed
            // release segment
            downloadCandidate.addToCandidateLog( 
                "Completed a segment which started at " + downloadSegment.getStart() 
                + " and was downloaded at a rate of " + downloadSegment.getLongTermTransferRate() );
            NLogger.debug( NLoggerNames.Download_Worker,
                    "Completed a segment which started at " + downloadSegment.getStart() 
                    + " and was downloaded at a rate of " + downloadSegment.getLongTermTransferRate());
            NLogger.debug(NLoggerNames.Download_Worker,
                "Releasing DownloadSegment (completed): " + downloadSet.toString() 
                + " - " + this);
            downloadSet.releaseDownloadSegment();
        }
        while (isRunning && downloadEngine != null 
            && downloadEngine.isAcceptingNextSegment() 
            && downloadFile.isScopeAllocateable( downloadCandidate.getAvailableScopeList() ) 
            && !isDownloadStopped );
        
        downloadCandidate
            .setStatus(SWDownloadConstants.STATUS_CANDIDATE_WAITING);        
    }
    
    /**
     * @param list
     */
    private void moveDownloadFilesToDestination(List list)
    {
        insideCriticalSection = true;
        try
        {
            Iterator iter = list.iterator();
            while (iter.hasNext())
            {
                SWDownloadFile downloadFile = (SWDownloadFile) iter.next();
                if ( downloadFile.isFileCompleted() )
                {
                    if( downloadFile.isDownloadFinished() )
                    {
                        downloadFile.moveToDestinationFile();
                    }
                    else
                    {
                        // otherwise something with the segment merge failed...
                        // we stop the download...
                        downloadFile.stopDownload();
                    }
                }
            }
        }
        finally
        {
            insideCriticalSection = false;
        }
    }

    public String toString()
    {
        return "[SWDownloadWorker@" + Integer.toHexString(hashCode()) + ":running:" + isRunning + ",tempWorker:"
            + isTemporaryWorker + ",engine:" + downloadEngine + "]";
    }
    
    /*private void determineFileSizeViaHead(URLDownloadFile downloadFile, 
        SWDownloadCandidate candidate )
        throws IOException
    {
        HttpURLConnection httpConnection = null;
        try
        {
            URL url = downloadFile.getURL();
            httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setRequestMethod("GET");
            httpConnection.setRequestProperty( HTTPHeaderNames.USER_AGENT,
                Environment.getPhexVendor() );
            httpConnection.connect();
            int responseCode = httpConnection.getResponseCode();
            if ( responseCode != HttpURLConnection.HTTP_OK )
            {
                throw new UnusableHostException( "Wrong response code: " + responseCode );
            }
            int length = httpConnection.getContentLength();
            if ( length < 0 )
            {
                throw new UnusableHostException( "Invalid length" );
            }
            downloadFile.setFileSize( length );
        }
        catch (UnusableHostException exp)
        {
            // stop and set to null so that download is not starting.
            downloadEngine.stopDownload();
            downloadEngine = null;
            // file not available or wrong http header.
            Logger.logMessage(Logger.FINE, Logger.DOWNLOAD_NET, exp);
            Logger.logMessage(Logger.FINE, Logger.DOWNLOAD_NET,
                "Removing download candidate: " + candidate);
            downloadFile.markCandidateBad(candidate, true);
            downloadFile.addBadAltLoc(candidate);
            return;
        }
        catch ( IOException exp )
        {
            candidate.setStatus(
                SWDownloadConstants.STATUS_CANDIDATE_CONNECTION_FAILED);
            Logger.logMessage(Logger.WARNING, Logger.DOWNLOAD_NET, exp,
                "Error at Host: "
                    + candidate.getHostAddress().getFullHostName()
                    + " Vendor: " + candidate.getVendor());
            // stop and set to null so that download is not starting.
            downloadEngine.stopDownload();
            downloadEngine = null;
            return;
        }
        finally
        {
            if ( httpConnection != null )
            {
                httpConnection.disconnect();
            }
        }
    }*/
}