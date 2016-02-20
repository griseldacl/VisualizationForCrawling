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
 *  $Id: ShareFile.java,v 1.41 2005/11/13 10:28:50 gregork Exp $
 */
package phex.share;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;

import phex.common.AlternateLocation;
import phex.common.AlternateLocationContainer;
import phex.common.URN;
import phex.common.address.DestAddress;
import phex.common.address.IpAddress;
import phex.http.Range;
import phex.net.presentation.PresentationManager;
import phex.thex.ShareFileThexData;
import phex.upload.UploadConstants;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;
import phex.xml.ObjectFactory;
import phex.xml.XJBAlternateLocation;
import phex.xml.XJBSharedFile;

public class ShareFile extends SharedResource
{
    /**
     * The unique file index;
     */
    private int fileIndex;

    /**
     * The file size ( file.length() ). Buffered because of performance reasons.
     */
    private Long fileSize;

    /**
     * The urn of the file.
     */
    private URN urn;
    
    /**
     * The THEX data of the shared files.
     */
    private ShareFileThexData thexData;

    /**
     * The absolute file name in lower case for optimization.
     */
    private char[] searchCompareTerm;

    /**
     * The number of times the file was searched for.
     */
    private Integer searchCount;

    /**
     * The number of times the file was requested for upload.
     */
    private Integer uploadCount;

    /**
     * A ArrayList of AlternateLocations of the share file.
     */
    private AlternateLocationContainer alternateLocations;

    /**
     * Creates a new ShareFile with its backed file object.
     * @param aFile the backed file object.
     */
    public ShareFile( File aFile )
    {
        super( aFile );

        fileSize = new Long( systemFile.length() );
        searchCount = new Integer(0);
        uploadCount = new Integer(0);
        
        SharedFilesService shareService = ShareManager.getInstance().getSharedFilesService();
        String searchCompareString = shareService.getSharedFilePath( systemFile );
        searchCompareTerm = searchCompareString.toLowerCase().toCharArray();
    }

    /**
     * Called by subclass to initialize.
     */
    protected ShareFile(long aFileSize)
    {
        fileSize = new Long(aFileSize);
    }

    /**
     * Returns the file urn.
     * @return the file urn.
     */
    public URN getURN()
    {
        return urn;
    }

    /**
     * @param urn The urn to set.
     */
    public void setURN(URN urn)
    {
        this.urn = urn;
    }
    
    /**
     * Returns the sha1 nss value of the urn if available.
     * @return the sha1 nss value of the urn if available.
     */
    public String getSHA1()
    {
        if (urn == null || !urn.isSha1Nid())
        {
            return "";
        }
        return urn.getNamespaceSpecificString();
    }

    /**
     * Returns the unique file index.
     * @return the unique file index.
     */
    public int getFileIndex()
    {
        return fileIndex;
    }

    /**
     * Sets the file index. It must be unique over all ShareFile object
     * @param index the unique file index.
     */
    public void setFileIndex(int index)
    {
        fileIndex = index;
    }

    /**
     * Returns the thex data if they are already available otherwise it schedules
     * calculation in case doCalculation is set to true. Null is returned until
     * the calcualtion results are available.
     * @param doCalculation set to true if the thex calculation should be
     *    performed if ThexData is not yet available.  
     * @return the thex data if already available, null otherwise.
     */
    public ShareFileThexData getThexData( boolean doCalculation )
    {
        if ( thexData == null && urn != null && doCalculation && fileSize.longValue() > 0)
        {// if there is no thex data and we have already calculated SHA1 urn,
         // schedule a calculation worker. 
            ShareManager.getInstance().getSharedFilesService().queueThexCalculation(
                this);
        }
        return thexData;
    }
    
    public void setThexData( ShareFileThexData thexData )
    {
        this.thexData = thexData;
    }
    /**
     * Returns the file size as a long object.
     * @return the file size as a long object.
     */
    public Long getFileSizeObject()
    {
        return fileSize;
    }

    /**
     * Returns the file size.
     * @return the file size.
     */
    public long getFileSize()
    {
        return fileSize.longValue();
    }

    /**
     * Checks if the requested range is satisfiable.
     * @param range the requested range.
     * @return true if the requested range is satisfiable, false otherwise.
     */
    public short getRangeAvailableStatus(Range range)
    {
        long fileSizeVal = fileSize.longValue();
        long startOffset = range.getStartOffset(fileSizeVal);
        if (startOffset < 0 || startOffset >= fileSizeVal)
        {
            return UploadConstants.RANGE_NOT_SATISFIABLE;
        }
        else
        {
            return UploadConstants.RANGE_AVAILABLE;
        }
    }

    /**
     * Returns the container of all known alternate download locations.
     * @return the container of all known alternate download locations.
     */
    public AlternateLocationContainer getAltLocContainer()
    {
        if (alternateLocations == null)
        {// initialize when first time requested.
            alternateLocations = new AlternateLocationContainer(urn);
        }
        return alternateLocations;
    }

    /**
     * Returns the number of times the file was searched for as an Integer object.
     * @return the number of times the file was searched for as an Integer object.
     */
    public Integer getSearchCountObject()
    {
        return searchCount;
    }

    /**
     * Returns the number of times the file was searched for.
     * @return the number of times the file was searched for.
     */
    public int getSearchCount()
    {
        return searchCount.intValue();
    }

    /**
     * Increments the search counter by one.
     */
    public void incSearchCount()
    {
        searchCount = new Integer(searchCount.intValue() + 1);
    }

    /**
     * Returns the number of times the file was uploaded as an Integer object.
     * @return the number of times the file was uploaded as an Integer object.
     */
    public Integer getUploadCountObject()
    {
        return uploadCount;
    }

    /**
     * Returns the number of times the file was uploaded.
     * @return the number of times the file was uploaded.
     */
    public int getUploadCount()
    {
        return uploadCount.intValue();
    }

    /**
     * Increments the upload counter by one.
     */
    public void incUploadCount()
    {
        uploadCount = new Integer(uploadCount.intValue() + 1);
    }

    /**
     * Returns the search compare term as an char[] in lower case for optimization.
     * @return the search compare term as an char[] in lower case for optimization.
     */
    public char[] getSearchCompareTerm()
    {
        return searchCompareTerm;
    }

    /**
     * Updates the searchCount, uploadCount and urn from the cached XMLSharedFile
     * object that is used to make ShareFile data persistend.
     * @param xjbFile the cached XJBSharedFile
     * object that is used to make ShareFile data persistend.
     */
    public void updateFromCache(XJBSharedFile xjbFile)
    {
        searchCount = new Integer(xjbFile.getHitCount());
        uploadCount = new Integer(xjbFile.getUploadCount());
        urn = new URN("urn:sha1:" + xjbFile.getSHA1());
        
        String rootHash = xjbFile.getThexRootHash();
        if ( rootHash != null )
        {
            String xjbLowestLevelNodes = xjbFile.getThexLowestLevelNodes();
            int depth = xjbFile.getThexTreeDepth();
            if ( thexData == null )
            {
                thexData = new ShareFileThexData( this, rootHash, xjbLowestLevelNodes, 
                    depth );
            }
            else
            {
                thexData.updateFromCache( rootHash, xjbLowestLevelNodes, depth );
            }
        }
        List list = xjbFile.getAltLoc();
        Iterator iterator = list.iterator();
        while (iterator.hasNext())
        {
            try
            {
                XJBAlternateLocation xjbAltLoc = (XJBAlternateLocation) iterator
                    .next();
                String hostAddress = xjbAltLoc.getHostAddress();
                String urn = xjbAltLoc.getURN();
                if (urn != null)
                {
                    DestAddress address = PresentationManager.getInstance().
                        createHostAddress( hostAddress, IpAddress.DEFAULT_PORT );
                    AlternateLocation altLoc = new AlternateLocation( address,
                        new URN(urn) );
                    getAltLocContainer().addAlternateLocation(altLoc);
                }
            }
            catch (Exception exp)
            {
                NLogger.error( NLoggerNames.GLOBAL, 
                    "AlternateLocation skipped due to error.", exp );
            }
        }
    }

    public XJBSharedFile createXJBSharedFile() throws JAXBException
    {
        ObjectFactory objFactory = new ObjectFactory();
        XJBSharedFile xjbFile = objFactory.createXJBSharedFile();
        xjbFile.setFileName(systemFile.getAbsolutePath());
        xjbFile.setSHA1(getSHA1());
        if ( thexData != null )
        {
            xjbFile.setThexTreeDepth( thexData.getTreeDepth() );
            xjbFile.setThexRootHash( thexData.getRootHash() );
            xjbFile.setThexLowestLevelNodes( thexData.getXJBLowestLevelNodes() );
        }
        xjbFile.setLastModified(systemFile.lastModified());
        xjbFile.setHitCount(searchCount.intValue());
        xjbFile.setUploadCount(uploadCount.intValue());

        if (alternateLocations != null)
        {
            alternateLocations.createXJBAlternateLocationList(xjbFile
                .getAltLoc());
        }
        return xjbFile;
    }

    public String toString()
    {
        return super.toString() + " " + getFileName() + "  " + fileIndex;
    }
}
