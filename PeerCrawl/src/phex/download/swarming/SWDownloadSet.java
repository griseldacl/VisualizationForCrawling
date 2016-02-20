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
package phex.download.swarming;

import phex.download.DownloadScope;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

/**
 * The download set is used to hold everything that is needed for a doing a swarm
 * download. This is the download file, the download segment and the download
 * candidate.
 */
public class SWDownloadSet
{
    private SWDownloadFile downloadFile;
    private DownloadScope downloadScope;
    private SWDownloadSegment downloadSegment;
    private SWDownloadCandidate downloadCandidate;

    public SWDownloadSet( SWDownloadFile aDownloadFile,
        SWDownloadCandidate aDownloadCandidate )
    {
        downloadFile = aDownloadFile;
        downloadCandidate = aDownloadCandidate;
        downloadScope = null;
    }

    public SWDownloadFile getDownloadFile()
    {
        return downloadFile;
    }

    public SWDownloadCandidate getDownloadCandidate()
    {
        return downloadCandidate;
    }

    public SWDownloadSegment allocateDownloadSegment( SWDownloadWorker worker )
    {
        NLogger.debug(NLoggerNames.Download_DownloadSet, "Allocate segment for: " + worker + " on set " + this);
        if ( downloadScope == null )
        {
            downloadScope = downloadFile.allocateDownloadScope( 
                downloadCandidate.getAvailableScopeList(), 
                downloadCandidate.getPreferredSegmentSize(),
                downloadCandidate.getSpeed());
            
            if ( downloadScope != null )
            {
                if ( downloadScope.getEnd() == Long.MAX_VALUE )
                {
                    downloadSegment = new SWDownloadSegment( downloadFile,
                        downloadScope.getStart(), SWDownloadConstants.UNKNOWN_FILE_SIZE );
                }
                else
                {
                    downloadSegment = new SWDownloadSegment( downloadFile,
                        downloadScope.getStart(), downloadScope.getLength() );
                }
                downloadCandidate.associateDownloadSegment( downloadSegment );
            }
            // sanity check to make sure!
            NLogger.debug(NLoggerNames.Download_DownloadSet, "Allocated segment: " + downloadSegment + " on set " + this);
        }
        if ( downloadSegment == null )
        {
            return null;
        }
        return downloadSegment;
    }

    public DownloadScope getDownloadScope()
    {
        return downloadScope;
    }
    
    public SWDownloadSegment getDownloadSegment()
    {
        return downloadSegment;
    }

    /**
     * Releases a allocated download segment.
     */
    public void releaseDownloadSegment( )
    {
        if ( downloadSegment != null )
        {
            NLogger.debug(NLoggerNames.Download_DownloadSet, 
                "Release file download segment: " + downloadSegment + " on set " + this);
            downloadFile.releaseDownloadScope( downloadScope, downloadSegment );
            downloadSegment = null;
            downloadScope = null;
        }
        NLogger.debug(NLoggerNames.Download_DownloadSet, "Release candidate download segment on set " + this);
        downloadCandidate.releaseDownloadSegment( );
    }

    /**
     * Releases a allocated download set.
     */
    public void releaseDownloadSet( )
    {
        NLogger.debug(NLoggerNames.Download_DownloadSet, "Release download set on set " + this );
        releaseDownloadSegment();
        downloadFile.releaseDownloadCandidate( downloadCandidate );
        downloadFile.decrementWorkerCount();
    }

    public String toString()
    {
        return "[DownloadSet@" + Integer.toHexString(hashCode()) +": (Segment: " + downloadSegment + " - Candidate: "
            + downloadCandidate + " - File: " + downloadFile + ")]";
    }
}
