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
 *  $Id: UploadState.java,v 1.20 2005/11/03 16:33:46 gregork Exp $
 */
package phex.upload;

import java.io.File;

import phex.common.TransferDataProvider;
import phex.common.URN;
import phex.common.address.DestAddress;
import phex.common.bandwidth.TransferAverage;
import phex.utils.Logger;


public class UploadState implements TransferDataProvider, UploadConstants
{
    /**
     * The upload manager.
     */
    private UploadManager uploadManager;

    /**
     * Defines the length already uploaded.
     */
    private long transferredDataSize;

    /**
     * Used to store the current progress.
     */
    private Integer currentProgress;

    /**
     * The status of the upload
     */
    private short status;

    /**
     * The host address that request the upload.
     */
    private DestAddress hostAddress;

    /**
     * The vendor that requests the upload.
     */
    private String vendor;

    private String fileName;
    private URN fileURN;
    private File uploadFile;

    /**
     * The upload engine working on this upload or null if not available.
     */
    private UploadEngine uploadEngine;

    /*
     * Total data sent previously with the same connection
     */
    private long previousSegmentsSize;
    
    private TransferAverage transferAverage;

    /**
     * This is used to create a upload state object that is used for displaying
     * it in the upload queue.
     * @param hostAddress the host address of the host.
     * @param vendor the vendor string of the host.
     */
    public UploadState( DestAddress hostAddress, String vendor )
    {
        this( hostAddress, vendor, null, null, null, -1 );
    }

    public UploadState( DestAddress hostAddress, String vendor,
        String fileName, URN fileURN, File uploadFile, long contentLength )
    {
        uploadManager = UploadManager.getInstance();
        transferredDataSize = 0;
        previousSegmentsSize = 0;
        currentProgress = new Integer( 0 );

        this.hostAddress = hostAddress;
        this.vendor = vendor;
        this.fileName = fileName;
        this.fileURN = fileURN;
        this.uploadFile = uploadFile;
        transferLength = contentLength;
        status = STATUS_INITIALIZING;
        
        transferAverage = new TransferAverage( 1000, 10 );
    }
    
    public void update( DestAddress hostAddress, String vendor )
    {
        this.hostAddress = hostAddress;
        this.vendor = vendor;
    }

    public void update( DestAddress hostAddress, String vendor, String fileName )
    {
        this.hostAddress = hostAddress;
        this.vendor = vendor;
        this.fileName = fileName;
    }

    public void update( DestAddress hostAddress, String vendor,
        String fileName, URN fileURN, File uploadFile, long contentLength )
    {
        this.hostAddress = hostAddress;
        this.vendor = vendor;
        this.fileName = fileName;
        this.fileURN = fileURN;
        this.uploadFile = uploadFile;
        transferLength = contentLength;
    }

    public File getUploadFile()
    {
        return uploadFile;
    }

    public String getVendor()
    {
        return vendor;
    }

    public String getFileName()
    {
        return fileName;
    }

    public URN getFileURN()
    {
        return fileURN;
    }

    public DestAddress getHostAddress()
    {
        return hostAddress;
    }

    public short getStatus()
    {
        return status;
    }

    public void setStatus( short newStatus )
    {
        // dont care for same status
        if ( status == newStatus )
        {
            return;
        }
        Logger.logMessage( Logger.FINE, Logger.UPLOAD, "UploadState Status "
            + newStatus );
        switch( newStatus )
        {
            case STATUS_COMPLETED:
            case STATUS_ABORTED:
                uploadStopNotify();
                break;
            case STATUS_UPLOADING:
                // only trigger if the status is not already set to downloading
                uploadStartNotify();
                break;
        }
        this.status = newStatus;
        uploadManager.fireUploadFileChanged( this );
    }

    /**
     * Indicate that the download is just starting.
     * Triggered internally when status changes to STATUS_FILE_DOWNLOADING.
     */
    private void uploadStartNotify( )
    {
    }

    /**
     * Indicate that the download is no longer running.
     * Triggered internally when status is set to STATUS_FILE_COMPLETED or
     * STATUS_FILE_QUEUED.
     */
    private void uploadStopNotify( )
    {
    }


    public boolean isUploadRunning()
    {
        return status == STATUS_UPLOADING;
    }

    public void setUploadEngine( UploadEngine uploadEngine )
    {
        this.uploadEngine = uploadEngine;
    }

    public void setTransferredDataSize( long aTransferredSize )
    {
        long diff = aTransferredSize - transferredDataSize;
        if ( diff < 0 ) // a new block is being transferred
        {
            previousSegmentsSize += transferredDataSize;
            transferAverage.addValue(aTransferredSize);
        } 
        else
        {
            transferAverage.addValue(diff);
        }
        transferredDataSize = aTransferredSize;
        uploadManager.fireUploadFileChanged( this );
    }

    /**
     * Returns the progress in percent. If mStatus == sCompleted will always be 100%.
     */
    public Integer getProgress()
    {
        int percentage;

        if( status == UploadConstants.STATUS_COMPLETED )
        {
            percentage = 100;
        }
        else
        {
            long toTransfer = getTransferDataSize();
            percentage = (int)(getTransferredDataSize() * 100L / (toTransfer == 0L ? 1L : toTransfer));
        }

        if ( currentProgress.intValue() != percentage )
        {
            // only create new object if necessary
            currentProgress = new Integer( percentage );
        }

        return currentProgress;
    }

    public void stopUpload()
    {
        if ( uploadEngine != null )
        {
            uploadEngine.stopUpload();
        }
        setStatus( STATUS_ABORTED );
    }
    
    /**
     * Returns the transfer speed from the bandwidth controller of this download.
     * @return
     */
    public long getTransferSpeed()
    {
        return transferAverage.getAverage();
    }

    ////////////////////// TransferDataProvider Interface //////////////////////

    private long transferLength;

    public long getTransferDataSize()
    {
        return transferLength + previousSegmentsSize;
    }

    /**
     * Indicate how much of the file has been uploaded on this transfer.
     */
    public long getTransferredDataSize()
    {
        return transferredDataSize + previousSegmentsSize;
    }

    /**
     * This is the total size of the available data. Even if its not important
     * for the transfer itself.
     */
    public long getTotalDataSize()
    {
        // in case of upload this is the same as the size that is transfered.
        return getTransferDataSize();
    }

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
            case STATUS_UPLOADING:
                return TransferDataProvider.TRANSFER_RUNNING;
            case STATUS_ABORTED:
                return TransferDataProvider.TRANSFER_ERROR;
            case STATUS_COMPLETED:
                return TransferDataProvider.TRANSFER_COMPLETED;
            default:
                return TransferDataProvider.TRANSFER_NOT_RUNNING;
        }
    }
}