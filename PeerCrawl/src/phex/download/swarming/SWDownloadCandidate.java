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
 *  $Id: SWDownloadCandidate.java,v 1.78 2005/11/13 22:27:45 gregork Exp $
 */
package phex.download.swarming;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

import phex.common.ServiceManager;
import phex.common.ShortObj;
import phex.common.URN;
import phex.common.address.DestAddress;
import phex.common.address.IpAddress;
import phex.common.address.MalformedDestAddressException;
import phex.download.DownloadScope;
import phex.download.DownloadScopeList;
import phex.download.RemoteFile;
import phex.download.log.LogRecord;
import phex.http.HTTPRangeSet;
import phex.http.Range;
import phex.http.XQueueParameters;
import phex.msg.GUID;
import phex.net.presentation.PresentationManager;
import phex.query.QueryHitHost;
import phex.utils.*;
import phex.xml.ObjectFactory;
import phex.xml.XJBSWDownloadCandidate;
import phex.xml.XMLUtils;

/**
 * A representation of a download candidate. A download candidate contains all
 * information required for a given endpoint that can offer us data for download.
 * <p>
 * 
 */
public class SWDownloadCandidate implements SWDownloadConstants
{
    /**
     * The time this download candidate was first added to the list of download
     * candidates of the corresponding download file. This is the first creation
     * time of the SWDownloadCandidate object.
     */
    //private Date firstSeenDate;
    
    /**
     * The time a connection to this download candidate could be established
     * successful.
     */
    private long lastConnectionTime;
        
    /**
     * The number of failed connection tries to this download candidate, since
     * the last successful connection.
     */
    private int failedConnectionTries;
    
    /** 
     * The GUID of the client used for push requests.
     */
    private GUID guid;

    /**
     * The file index of the file at the download candidate.
     * This is the old style identifier for candidates without
     * urn.
     */
    private long fileIndex;
    
    /**
     * The resource URN of the file. If known this will be used
     * for download requests.
     */
    private URN resourceURN;
    
    /**
     * A complete download URI to use. This is necessary for standard
     * uri downloads like in http urls.
     */
    private URI downloadURI;

    /**
     * The name of the file at the download candidate.
     */
    private String fileName;

    /**
     * Rate at which the last segment was transferred
     */
    private int lastTransferRateBPS;

    /**
     * The host address of the download candidate.
     */
    private DestAddress hostAddress;

    /**
     * The status of the download.
     */
    private ShortObj statusObj;
    
    /**
     * A possible status reason.
     */
    private String statusReason;

    /**
     * The last error status of the download to track status changes.
     */
    private short errorStatus;

    /**
     * The time after which the current status times out and things continue.
     */
    private long statusTimeout;

    /**
     * Counts the number of times the status keeps repeating.
     */
    private int errorStatusRepetition;

    /**
     * The vendor of the client running at the candidate.
     */
    private String vendor;
    
    private boolean isG2FeatureAdded;

    /**
     * Defines if a push is needed for this candidate.
     */
    private boolean isPushNeeded;
    
    /**
     * The addresses of push proxies of this host or null
     * if not available.
     */
    private DestAddress[] pushProxyAddresses;

    /**
     * Defines if the candidate supports chat connections.
     */
    private boolean isChatSupported;

    /**
     * The available range set of the candidate file.
     */
    private DownloadScopeList availableScopeList;

    /**
     * The time the available range was last updated.
     */
    private long availableRangeSetTime = 0;

    /**
     * The download file that this segment belongs to.
     */
    private SWDownloadFile downloadFile;

    /**
     * The download segment that is currently assiciated by this download
     * candidate or null if no association exists.
     */
    private SWDownloadSegment downloadSegment;

    /**
     * The parameters that are available in case the candidate is remotly queueing
     * our download request. This is referenced for information purpose only.
     */
    private XQueueParameters xQueueParameters;
    
    /**
     * This is a Map holding all AltLocs already send to this candidate during
     * this session. It is used to make sure the same AltLocs are not send twice
     * to the same candidate. The list is lazy initialized on first access.
     */
    private Set sendAltLocSet;
    
    /**
     * The total amount of data downloaded from this candidate.
     */
    private long totalDownloadSize;
    
    public SWDownloadCandidate( RemoteFile remoteFile,
        SWDownloadFile aDownloadFile )
    {
        availableScopeList = null;
        downloadFile = aDownloadFile;
        fileIndex = remoteFile.getFileIndex();
        fileName = remoteFile.getFilename();
        resourceURN = remoteFile.getURN();
        guid = remoteFile.getRemoteClientID();
        QueryHitHost qhHost = remoteFile.getQueryHitHost();
        vendor = qhHost.getVendor();
        isPushNeeded = qhHost.isPushNeeded();
        hostAddress = remoteFile.getHostAddress();
        isChatSupported = qhHost.isChatSupported();
        pushProxyAddresses = qhHost.getPushProxyAddresses();
        statusObj = new ShortObj( STATUS_CANDIDATE_WAITING );
        lastTransferRateBPS = 0;
        totalDownloadSize = 0;
        lastConnectionTime = 0;
    }

    public SWDownloadCandidate( DestAddress aHostAddress, long aFileIndex,
        String aFileName, URN aResourceURN, SWDownloadFile aDownloadFile )
    {
        availableScopeList = null;
        downloadFile = aDownloadFile;
        fileIndex = aFileIndex;
        fileName = aFileName;
        resourceURN = aResourceURN;
        guid = null;
        vendor = null;
        isPushNeeded = false;
        // assume chat is supported but we dont know...
        isChatSupported = true;
        hostAddress = aHostAddress;
        statusObj = new ShortObj( STATUS_CANDIDATE_WAITING );
        lastTransferRateBPS = 0;
        totalDownloadSize = 0;
        lastConnectionTime = 0;
        /*setAvailableRangeSet(new HTTPRangeSet(0, downloadFile.getTotalDataSize())); */
    }
    
    /**
     * @param address
     * @param downloadUrl
     * @param file
     */
    public SWDownloadCandidate(DestAddress address, 
        URI downloadUri, SWDownloadFile file)
        throws URIException
    {
        availableScopeList = null;
        downloadFile = file;
        fileName = URLUtil.getPathQueryFromUri( downloadUri );
        this.downloadURI = downloadUri;
        resourceURN = null;
        guid = null;
        vendor = null;
        isPushNeeded = false;
        // assume chat is supported but we dont know...
        isChatSupported = true;
        hostAddress = address;
        statusObj = new ShortObj( STATUS_CANDIDATE_WAITING );
        lastTransferRateBPS = 0;
        totalDownloadSize = 0;
        lastConnectionTime = 0;
    }

    public SWDownloadCandidate( XJBSWDownloadCandidate xjbCandidate,
        SWDownloadFile aDownloadFile ) 
        throws MalformedDestAddressException
    {
        availableScopeList = null;
        downloadFile = aDownloadFile;
        fileIndex = xjbCandidate.getFileIndex();
        fileName = xjbCandidate.getFileName();
        lastTransferRateBPS = 0;
        totalDownloadSize = 0;
        /* setAvailableRangeSet(new HTTPRangeSet(0, downloadFile.getTotalDataSize())); */

        String guidHexStr = xjbCandidate.getGUID();
        if ( guidHexStr != null )
        {
            guid = new GUID( guidHexStr );
        }
        String downloadUriStr = xjbCandidate.getDownloadURI();
        if ( downloadUriStr != null )
        {
            try
            {
                downloadURI = new URI( xjbCandidate.getDownloadURI(), true );
            }
            catch ( URIException exp )
            {
                NLogger.warn(NLoggerNames.Download_Candidate,
                    "Malformed URI in: " + aDownloadFile.toString() +
                    " - " + xjbCandidate.getDownloadURI() + " - " + this.toString(),
                    exp );
                // continue anyway.. download candidate might still be usefull
            }
        }
        vendor = xjbCandidate.getVendor();
        isPushNeeded = xjbCandidate.isPushNeeded();
        isChatSupported = xjbCandidate.isChatSupported();
        
        if ( xjbCandidate.isSetLastConnectionTime() )
        {
            lastConnectionTime = xjbCandidate.getLastConnectionTime();
        }
        else
        {
            lastConnectionTime = 0;
        }

        try
        {
            hostAddress = PresentationManager.getInstance()
                .createHostAddress( xjbCandidate.getRemoteHost(), IpAddress.DEFAULT_PORT );
        }
        catch ( MalformedDestAddressException exp )
        {
            NLogger.warn(NLoggerNames.Download_Candidate,
                "Malformed host address in: " + aDownloadFile.toString() +
                " - " + xjbCandidate.getRemoteHost() + " - " + this.toString(), exp );
            throw exp;
        }
        resourceURN = aDownloadFile.getFileURN();
        
        if ( xjbCandidate.getConnectionFailedRepetition() > 0 )
        {
            errorStatus = STATUS_CANDIDATE_CONNECTION_FAILED;
            statusObj = new ShortObj( STATUS_CANDIDATE_CONNECTION_FAILED );
            errorStatusRepetition = xjbCandidate.getConnectionFailedRepetition();
            failedConnectionTries = errorStatusRepetition;
        }
        else
        {
            statusObj = new ShortObj( STATUS_CANDIDATE_WAITING );
        }
    }
    
    

    /**
     * Returns the url necessary for the download request.
     * @return the download url.
     */
    public String getDownloadRequestUrl()
    {
        String requestUrl;
        if ( downloadURI != null )
        {
            try
            {
                // dont use whole uri.. only file and query part..!?
                requestUrl = URLUtil.getPathQueryFromUri( downloadURI );
                return requestUrl;
            }
            catch (URIException e)
            {// failed to use uri.. try other request urls..
                NLogger.warn( NLoggerNames.Download_Candidate, e, e );
            }
        }
        
        if ( resourceURN != null )
        {
            requestUrl = URLUtil.buildName2ResourceURL( resourceURN );
        }
        else
        {
            // build standard old style gnutella request.
            String fileIndexStr = String.valueOf( fileIndex );
            StringBuffer urlBuffer = new StringBuffer( 6 + fileIndexStr.length()
                + fileName.length() );
            urlBuffer.append( "/get/" );
            urlBuffer.append( fileIndexStr );
            urlBuffer.append( '/' );            
            urlBuffer.append( URLCodecUtils.encodeURL( fileName ) );
            requestUrl = urlBuffer.toString();
        }
        return requestUrl;
    }
    
    public SWDownloadFile getDownloadFile()
    {
        return downloadFile;
    }
    
    public long getSpeed()
    {
        return lastTransferRateBPS;
    }

    /**
     * Returns the HostAddress of the download candidate
     */
    public DestAddress getHostAddress()
    {
        return hostAddress;
    }

    /**
     * Returns the name of the file at the download candidate.
     */
    public String getFileName()
    {
        return fileName;
    }

    /**
     * Returns the resource URN of the file at the download candidate.
     */
    public URN getResourceURN()
    {
        return resourceURN;
    }

    /**
     * Returns the GUID of the candidate for push requests.
     */
    public GUID getGUID()
    {
        return guid;
    }

    /**
     * Returns the file index of the file at the download candidate.
     */
    public long getFileIndex()
    {
        return fileIndex;
    }
    
    public long getTotalDownloadSize()
    {
        return totalDownloadSize;
    }
    
    public void incTotalDownloadSize( int val )
    {
        totalDownloadSize += val;
    }

    /**
     * Returns the time in millis until the the status timesout.
     */
    public long getStatusTimeLeft()
    {
        long timeLeft = statusTimeout - System.currentTimeMillis();
        if ( timeLeft < 0 )
        {
            timeLeft = 0;
        }
        return timeLeft;
    }

    /**
     * Returns the current status of the candidate.
     */
    public short getStatus()
    {
        return statusObj.getValue();
    }
    
    public ShortObj getStatusObj()
    {
        return statusObj;
    }
    
    public String getStatusReason()
    {
        return statusReason;
    }
    
    /**
     * Indicates how often the last error status repeated. The last error status
     * must must not be the current status, but is the current status in case the
     * current status is a error status.
     * @return how often the last error status repeated
     */
    /*public int getErrorStatusRepetition()
    {
        return errorStatusRepetition;
    }*/
    
    /**
     * Returns the number of failed connection tries since the last successful
     * connection.
     * @return the number of failed connection tries since the last successful
     * connection.
     */
    public int getFailedConnectionTries()
    {
        return failedConnectionTries;
    }

    /**
     * The download candidate vendor.
     * @return
     */
    public String getVendor()
    {
        return vendor;
    }

    public void setVendor( String aVendor )
    {
        if ( vendor == null || !vendor.equals( aVendor ) )
        {
            // verify characters - this is used to remove invalid xml characters
            for ( int i = 0; i < aVendor.length(); i++ )
            {
                if ( !XMLUtils.isXmlChar( aVendor.charAt( i ) ) )
                {
                    return;
                }
            }
            vendor = aVendor;
            addToCandidateLog( "Set vendor to: " + vendor );
            downloadFile.fireDownloadCandidateChanged( this );
        }
    }
    
    public boolean isG2FeatureAdded()
    {
        return isG2FeatureAdded;
    }
    
    public void setG2FeatureAdded( boolean state )
    {
        isG2FeatureAdded = state;
    }

    public void updateXQueueParameters( XQueueParameters newXQueueParameters )
    {
        if ( xQueueParameters == null )
        {
            xQueueParameters = newXQueueParameters;
        }
        else
        {
            xQueueParameters.update( newXQueueParameters );
        }
    }

    public XQueueParameters getXQueueParameters()
    {
        return xQueueParameters;
    }

    public boolean isPushNeeded()
    {
        return isPushNeeded;
    }
    
    /**
     * Returns the array of push proxies of this connection
     * or null if there are no available.
     * @return
     */
    public DestAddress[] getPushProxyAddresses()
    {
        return pushProxyAddresses;
    }
    
    public void setPushProxyAddresses( DestAddress[] addresses )
    {
        pushProxyAddresses = addresses;
    }

    public long getLastConnectionTime()
    {
        return lastConnectionTime;
    }

    public void setLastConnectionTime( long lastConnectionTime )
    {
        this.lastConnectionTime = lastConnectionTime;
    }

    public boolean isChatSupported()
    {
        return isChatSupported;
    }

    public void setChatSupported( boolean state )
    {
        isChatSupported = state;
    }
    
    /**
     * Returns true if the candidate is remotly queued, false otherwise.
     * @return true is the candidate is remotly queued, false otherwise.
     */
    public boolean isRemotlyQueued()
    {
        return statusObj.value == STATUS_CANDIDATE_REMOTLY_QUEUED;
    }
    
    /**
     * Returns true if the candidate range is unavailable, false otherwise.
     * @return true is the candidate range is unavailable, false otherwise.
     */
    public boolean isRangeUnavailable()
    {
        return statusObj.value == STATUS_CANDIDATE_RANGE_UNAVAILABLE;
    }
    
    /**
     * Returns true if the candidate is downloading, false otherwise.
     * @return true is the candidate is downloading, false otherwise.
     */
    public boolean isDownloading()
    {
        return statusObj.value == STATUS_CANDIDATE_DOWNLOADING;
    }
    
    /**
     * Returns the list of alt locs already send to this connection. 
     * @return the list of alt locs already send to this connection.
     */
    public Set getSendAltLocsSet()
    {
        if ( sendAltLocSet == null )
        {// TODO2 use something like a LRUMap. But current LRUMap uses maxSize
         // as initial hash size. This would be much to big in most cases!
         // Currently this HashSet has no size boundry. We would need our own
         // LRUMap implementation with a low initial size and a different max size.
            sendAltLocSet = new HashSet();
        }
        return sendAltLocSet;
    }

    /**
     * Sets the available range set.
     * @param newRangeSet the available range set.
     */
    public void setAvailableRangeSet(HTTPRangeSet newRangeSet)
    {
        // dont do anything if we have no range set and the scope list is
        // uninitialized
        if ( (newRangeSet == null || newRangeSet.getRangeSet().size() == 0)
           && availableScopeList == null )
        {
            return;
        }
        
        if ( newRangeSet == null )
        {
            // not only clear existing scope list but set it to NULL
            availableScopeList = null;
            return;
        }
        
        // lazy initialize if necessary
        if ( availableScopeList == null )
        {
            availableScopeList = new DownloadScopeList();
        }
        else
        {
            // clear known ranges.
            availableScopeList.clear();
        }

        // add..
        long fileSize = downloadFile.getTotalDataSize();
        Iterator iterator = newRangeSet.getIterator();
        while ( iterator.hasNext() )
        {
            Range range = (Range) iterator.next();
            DownloadScope scope = new DownloadScope( 
                range.getStartOffset(fileSize),
                range.getEndOffset(fileSize) );
            availableScopeList.add( scope );
        }
        availableRangeSetTime = System.currentTimeMillis();
        
        NLogger.debug( NLoggerNames.Download_File_RangePriority,
            "Added new rangeset for " + downloadFile.getDestinationFileName() 
            + ": " + newRangeSet);
        
    }

    /**
     * Returns the available range set or null if not set.
     * @return the available range set or null if not set.
     */
    public DownloadScopeList getAvailableScopeList( )
    {
        if ( System.currentTimeMillis() >
            availableRangeSetTime + AVAILABLE_RANGE_SET_TIMEOUT )
        {
            setAvailableRangeSet( null );
        }
        return availableScopeList;
    }

    public boolean equals( Object obj )
    {
        if ( obj instanceof SWDownloadCandidate )
        {
            return equals( (SWDownloadCandidate) obj );
        }
        return false;
    }

    public boolean equals( SWDownloadCandidate candidate )
    {
        return hostAddress.equals( candidate.hostAddress );
    }

    /**
     * Sets the status of the candidate and fulfills the required actions
     * necessary for that status. E.g. setting the statusTime.
     * @param newStatus the status to set from 
     *        SWDownloadConstants.STATUS_CANDIDATE_*
     */
    public void setStatus( short newStatus )
    {
        setStatus( newStatus, -1, null );
    }
    
    /**
     * Sets the status of the candidate and fulfills the required actions
     * necessary for that status. E.g. setting the statusTime.
     * @param newStatus the status to set from 
     *        SWDownloadConstants.STATUS_CANDIDATE_*
     * @param statusSeconds the time in seconds the status should last.
     */
    public void setStatus( short newStatus, int statusSeconds )
    {
        setStatus( newStatus, statusSeconds, null );
    }
    
    /**
     * Sets the status of the candidate and fulfills the required actions
     * necessary for that status. E.g. setting the statusTime.
     * @param newStatus the status to set from 
     *        SWDownloadConstants.STATUS_CANDIDATE_*
     * @param statusSeconds the time in seconds the status should last only
     *        used for STATUS_CANDIDATE_REMOTLY_QUEUED.
     * @param aStatusReason a status reason to be displayed to the user.
     */
    public void setStatus( short newStatus, int statusSeconds, String aStatusReason )
    {
        // dont care for same status
        if ( statusObj.value == newStatus )
        {
            return;
        }
        int oldStatus = statusObj.value;
        statusObj.value = newStatus;
        
        long newStatusTimeout;
        statusTimeout = newStatusTimeout = System.currentTimeMillis();
        switch( statusObj.value )
        {
            case STATUS_CANDIDATE_BAD:
                newStatusTimeout += BAD_CANDIDATE_STATUS_TIMEOUT;
                break;
            case STATUS_CANDIDATE_IGNORED:
                newStatusTimeout = Long.MAX_VALUE;
                break;
            case STATUS_CANDIDATE_CONNECTING:
                //connectionTries ++;
                break;
            case STATUS_CANDIDATE_CONNECTION_FAILED:
                failedConnectionTries ++;
                if ( failedConnectionTries >= IGNORE_CANDIDATE_CONNECTION_TRIES )
                {// we have tried long enough to connect to this candidate, ignore
                 // it for the future (causes delete after session).
                    downloadFile.markCandidateIgnored( this, 
                        "CandidateStatusReason_ConnectionFailed" );
                    // markCandidateIgnored updates the statusTimeout 
                    // and statusReason, this value is reset here to not 
                    // overwrite it...
                    newStatusTimeout = statusTimeout;
                    aStatusReason = statusReason;
                    break;
                }
                else if ( failedConnectionTries >= BAD_CANDIDATE_CONNECTION_TRIES )
                {
                    // we dont remove candidates but put them into a bad list.
                    // once we see a new X-Alt the candidate is valid again.
                    // candidates might go into the bad list quickly.
                    // when no "good" candidates are avaiable we might also try additional
                    // bad list connects, every 3 hours or so...
                    downloadFile.markCandidateBad( this );
                    // markCandidateBad updates the statusTimeout, this value is
                    // reset here to not overwrite it...
                    newStatusTimeout = statusTimeout;
                    break;
                }
                else
                {
                    downloadFile.markCandidateMedium( this );
                    newStatusTimeout += calculateConnectionFailedTimeout();
                    break;
                }
                // watch... no break here...
            case STATUS_CANDIDATE_REQUESTING:
                failedConnectionTries = 0;
                break;
            case STATUS_CANDIDATE_BUSY:
            case STATUS_CANDIDATE_RANGE_UNAVAILABLE:
            case STATUS_CANDIDATE_REMOTLY_QUEUED:
                failedConnectionTries = 0;
                if ( statusSeconds > 0 )
                {
                    newStatusTimeout += statusSeconds * 1000;
                }
                else
                {
                    newStatusTimeout += determineErrorStatusTimeout( statusObj.value );
                }
                break;
            case STATUS_CANDIDATE_PUSH_REQUEST:
                newStatusTimeout += ServiceManager.sCfg.mPushTransferTimeout;
                break;
            case STATUS_CANDIDATE_DOWNLOADING:
                // clear the current error status.
                errorStatus = STATUS_CLEARED;
                failedConnectionTries = 0;
                break;
            
        }
        this.statusReason = aStatusReason;
        
        NLogger.debug( NLoggerNames.Download_Candidate, 
            "Setting status to " + newStatus + " and raise timeout from "
            + statusTimeout + " to " + newStatusTimeout + "(" + 
            (newStatusTimeout-statusTimeout) + ") Reason:" + aStatusReason + ".");
        addToCandidateLog( "Setting status to " 
            + SWDownloadInfo.getDownloadCandidateStatusString(this) 
            + " and raise timeout from " + statusTimeout + " to " 
            + newStatusTimeout + "(" + (newStatusTimeout-statusTimeout) 
            + ") Reason:" + aStatusReason + ".");
        
        statusTimeout = newStatusTimeout;
        downloadFile.candidateStatusChanged( this, oldStatus, newStatus );
    }
     
    /**
     * Calculates the timeout between the last failed connection and the next
     * connection try. 
     * @return the number of millies to wait till the status expires.
     */
    private long calculateConnectionFailedTimeout()
    {   
        // Once we are over BAD_CANDIDATE_CONNECTION_TRIES we dont call this 
        // method anymore and use BAD_CANDIDATE_STATUS_TIMEOUT instead.
        
        // When CONNECTION_FAILED_STEP_TIME time is 2 it gives a sequence of
        // tries: 1, 2,  3,|  4,  5,  6,   7,   8,   9,  10
        //    to: 2, 4,  8,| 16, 32, 64, 128, 128, 128, 128
        //   to2: 2, 6, 12,| 22, 40, 74, 140, 142, 144, 146 
        // 
        // to1 - would cause the values to double on each repetition.
        //       CONNECTION_FAILED_STEP_TIME * (long)Math.pow( 2, 
        //           Math.min( failedConnectionTries - 1, 7 ) );
        // to2 - would add a raising penalty to to1 on each repetition
        //       CONNECTION_FAILED_STEP_TIME * (long)Math.pow( 2,
        //           Math.min( failedConnectionTries - 1, 7 ) ) 
        //           + (failedConnectionTries - 1) * 2;
        //
        // We use to2 currently:
        return CONNECTION_FAILED_STEP_TIME * (long)Math.pow( 2,
            Math.min( failedConnectionTries - 1, 7 ) ) 
            + (failedConnectionTries - 1) * 2;
    }

    /**
     * Maintains error status tracking. This is needed to track repeting errors
     * that will be handled by flexible status timeouts.
     * @returns the livetime of the status.
     */
    private long determineErrorStatusTimeout( short aErrorStatus )
    {
        if ( errorStatus == aErrorStatus )
        {
            errorStatusRepetition ++;
        }
        else
        {
            errorStatus = aErrorStatus;
            errorStatusRepetition = 0;
        }
        switch( errorStatus )
        {
            case STATUS_CANDIDATE_BUSY:
                // we can add here a step thing for each retry with a top limit
                // and a shorter start sleep time but currently we keep it like this.
                return HOST_BUSY_SLEEP_TIME;
                    /*( statusRepetition + 1 ) * */
            case STATUS_CANDIDATE_RANGE_UNAVAILABLE:
                // when step time is 1 it gives a sequence of
                // 1, 2, 4, 8, 16, 32...
                // this would cause the values to double on each repetition.
                return RANGE_UNAVAILABLE_STEP_TIME * (long)Math.pow( 2, errorStatusRepetition );
            case STATUS_CANDIDATE_REMOTLY_QUEUED:
                if ( xQueueParameters == null )
                {
                    return 0;
                }
                else
                {
                    return xQueueParameters.getRequestSleepTime();
                }
            default:
                NLogger.warn( NLoggerNames.Download_Candidate, "Unknown error status: " + errorStatus );
                return 0;
        }
    }

    /**
     * Manualy forces a connection retry. This sets the candidate to QUEUED when
     * it is the state BUSY, RANGE_UNAVAILABLE or CONNECTION_FAILED.
     * It will also decrease the errorStatusRepetition to not let the timeout
     * increase when the errorStatus remains.
     * The method should only be called from user triggered GUI action.
     */
    public void manualConnectionRetry()
    {
        if ( statusObj.value != STATUS_CANDIDATE_BUSY &&
             statusObj.value != STATUS_CANDIDATE_CONNECTION_FAILED &&
             statusObj.value != STATUS_CANDIDATE_RANGE_UNAVAILABLE &&
             statusObj.value != STATUS_CANDIDATE_BAD &&
             statusObj.value != STATUS_CANDIDATE_IGNORED )
        {
            return;
        }
        setStatus( STATUS_CANDIDATE_WAITING );
        SwarmingManager.getInstance().notifyWaitingWorkers();
    }
        
    /**
     * Returns if the candidate is able to be allocated. To be allocated a
     * candidate must not have a worker assigned and the nextRetryTime must be
     * passed.
     * @param currentTime is given for performance reason. We don't need to
     *        get the system time so often.
     */
    public boolean isAbleToBeAllocated( )
    {
        // Do not allow allocation if it's too slow!
        if (lastTransferRateBPS < ServiceManager.sCfg.minimumAllowedTransferRate
            && lastTransferRateBPS > 0)
        {
            addToCandidateLog( "Refusing candidate allocation as last transfer rate was only " 
                + lastTransferRateBPS + " bps");
            NLogger.debug( NLoggerNames.Download_Candidate_Allocate,
                "Refusing candidate allocation as last transfer rate was only " 
                + lastTransferRateBPS + " bps");
            return false;
        }
        long currentTime = System.currentTimeMillis();
        return statusTimeout <= currentTime;
    }

    public void associateDownloadSegment( SWDownloadSegment aSegment )
    {
        downloadSegment = aSegment;
    }

    /**
     * Returns the preferred (ie: largest) segment size to use for this candidate.
     * This will be DEFAULT_SEGMENT_SIZE initially, but will then be calculated so that if
     * the transfer rate for the previous segment were maintained, the next segment
     * would take DEFAULT_SEGMENT_TIME seconds.
     * The value MAXIMUM_ALLOWED_SEGMENT_SIZE is respected.
     */
    public long getPreferredSegmentSize()
    {
        // default is rate * seconds
        long result = lastTransferRateBPS * ServiceManager.sCfg.segmentTransferTime;
        // round the result up to the next multiple of segmentMultiple
        long remainder = (-result) % ServiceManager.sCfg.segmentMultiple;
        result += remainder;

        // No previous segment has been transferred
        if (lastTransferRateBPS == 0)
            result = ServiceManager.sCfg.initialSegmentSize;

        // Too fast (ie: segment would be bigger than allowed
        if (result > ServiceManager.sCfg.maximumSegmentSize)
            result = ServiceManager.sCfg.maximumSegmentSize;
            
        if ( result < 1 )
        {
            NLogger.warn( NLoggerNames.Download_Candidate,
                "Preferred size looks strange. bps=" + lastTransferRateBPS 
                + " and stt=" + ServiceManager.sCfg.segmentTransferTime);
            result = ServiceManager.sCfg.initialSegmentSize;
        }
        NLogger.debug( NLoggerNames.Download_Candidate,
            "Preferred segment size is " + result);
        return result;
    }

    public void releaseDownloadSegment()
    {
        if ( downloadSegment != null )
        {
            lastTransferRateBPS = downloadSegment.getLongTermTransferRate();
            downloadSegment = null;
        }
    }

    /**
     * Proivdes the caller with the currently associated segment or null if no
     * association is available.
     * @return the associated segment or null.
     */
    public SWDownloadSegment getDownloadSegment()
    {
        return downloadSegment;
    }

    // new JAXB way
    public XJBSWDownloadCandidate createXJBSWDownloadCandidate()
        throws JAXBException
    {
        ObjectFactory objFactory = new ObjectFactory();
        XJBSWDownloadCandidate xjbCandidate = objFactory.createXJBSWDownloadCandidate();
        xjbCandidate.setFileIndex( fileIndex );
        xjbCandidate.setFileName( fileName );
        if ( guid != null )
        {
            xjbCandidate.setGUID( guid.toHexString() );
        }
        if ( downloadURI != null )
        {
            xjbCandidate.setDownloadURI( downloadURI.getEscapedURI() );
        }
        xjbCandidate.setPushNeeded( isPushNeeded );
        xjbCandidate.setChatSupported( isChatSupported );
        xjbCandidate.setRemoteHost( hostAddress.getFullHostName() );
        xjbCandidate.setVendor( vendor );
        if ( lastConnectionTime > 0 )
        {
            xjbCandidate.setLastConnectionTime( lastConnectionTime );
        }
        
        // also maintain count how often a connection was failed in a row...
        //if ( failedConnectionTries >= BAD_CANDIDATE_CONNECTION_TRIES )
        if ( failedConnectionTries > 0 )
        {
            xjbCandidate.setConnectionFailedRepetition( failedConnectionTries );
        }
        /*else
        {
            xjbCandidate.setConnectionFailedRepetition( 0 );
        }*/
        return xjbCandidate;
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer( "[Candidate: ");
        if ( vendor != null )
        {
            buffer.append( vendor );
            buffer.append( ',' );
        }
        buffer.append( "Adr:" );
        buffer.append( hostAddress );
        buffer.append( " ->" );
        buffer.append( super.toString() );
        buffer.append( "]" );
        return buffer.toString();
    }
    
    public void addToCandidateLog( String message )
    {
        if ( ServiceManager.sCfg.downloadCandidateLogBufferSize > 0 )
        {
            LogRecord record = new LogRecord( this, message );
            SwarmingManager.getInstance().getCandidateLogBuffer().addLogRecord( record );
        }
    }
}