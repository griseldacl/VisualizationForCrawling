
/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2005 Phex Development Group
 *  Copyright (C) 2000 William W. Wong
 *  williamw@jps.net
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
package phex.download;


import phex.common.URN;
import phex.common.address.DestAddress;
import phex.msg.GUID;
import phex.query.QueryHitHost;

/**
 * A file existing on a remote gnutella host.
 */
public class RemoteFile
{
    /**
     * The QueryHitHost containing all information of the host this
     * file belongs to.
     */
    private QueryHitHost qhHost;

    /**
     * The old-style remote file index to identify the file.
     */
    private int fileIndex;
    
    /**
     * The original filename from the query hit.
     */
    private String filename;
    
    /**
     * Possible path info comming from the 'PATH' GGEP extension.
     */
    private String pathInfo;
    
    /**
     * The file display name. Its a possibly cleaned and modified file name to be
     * displayed to the user.
     */
    private String displayName;
    
    private String fileExtension = null;
    
    /**
     * The reported file size. (read-only)
     */
    private Long fileSize;
    private URN urn;
    private String metaData;
    private boolean isInDownloadQueue;
    
    /**
     * Indicates if the RemoteFile is displayed on the interface.
     * Introduced for search result filtering.
     */
    private boolean isDisplayed;

    /**
     * The score defines how well the search term matches the filename.
     */
    private Short score;

    /**
     * Create a new RemoteFile with all data initialised.
     *
     * @param aQhHost the QueryHitHost encapsulating information about the
     *        query hit
     * @param fileIndex  the int index of the file
     * @param fileName  the String name of the file
     * @param pathInfo possible file path information.
     * @param fileSize  the file size as a long
     * @param aUrn a urn of the remote file.
     * @param aMetaData file meta data.
     * @param aScore - The score defines how well the search term matches the
     *                 filename.
     */
    public RemoteFile( QueryHitHost aQhHost, int fileIndex, String fileName,
        String pathInfo, long fileSize, URN aUrn, String aMetaData, short aScore )
    {
        qhHost = aQhHost;
        this.fileIndex = fileIndex;
        filename = fileName;
        this.pathInfo = pathInfo; 
        this.fileSize = new Long( fileSize );
        urn = aUrn;
        metaData = aMetaData;
        isInDownloadQueue = false;
        score = new Short( aScore );
        
        buildDisplayName();
    }

    /**
     * Create a shallow copy of another RemoteFile instance.
     *
     * @param b  the RemoteFile to copy
     */
    public RemoteFile(RemoteFile b)
    {
        copy( b );
    }

    /**
     * Copy all information from a RemoteFile into this instance.
     *
     * @param b  the RemoteFile to copy data from
     */
    public void copy(RemoteFile b)
    {
        qhHost = b.qhHost;
        fileIndex = b.fileIndex;
        urn = b.urn;
        filename = b.filename;
        fileSize = b.fileSize;
        isInDownloadQueue = b.isInDownloadQueue;
        score = b.score;
        pathInfo = b.pathInfo;
        displayName = b.displayName;
    }

    /**
     * Retreive the current GUID of the remote client.
     *
     * @return the remote client GUID
     */
    public GUID getRemoteClientID()
    {
        return qhHost.getHostGUID();
    }

    /**
     * Get the remote file index.
     *
     * @return the file index
     */
    public int getFileIndex()
    {
        return fileIndex;
    }

    /**
     * Get the remote file name.
     * This may include path information as well as a file name.
     *
     * @return the file name
     */
    public String getFilename()
    {
        return filename;
    }
    
    /**
     * Returns possible path info of the remote file comming from the GGEP
     * extension.
     * @return
     */
    public String getPathInfo()
    {
        return pathInfo;
    }
    
    /**
     * The display name of the RemoteFile. This is 
     * getPathInfo() + getShortName()
     * @return
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Get the file extention.
     * This will be the substring of the full file name trailing the last '.'
     * character, or the empty string if there is none.
     *
     * @return the file extention
     */
    public String getFileExt()
    {
        if (fileExtension != null)
            return fileExtension;

        int index = filename.lastIndexOf(".");

        if (index > -1)
        {
            fileExtension = filename.substring(index + 1, filename.length());
        }
        else
        {
            fileExtension = "";
        }

        return fileExtension;
    }

    /**
     * Get the remote file size.
     *
     * @return the file size
     */
    public long getFileSize()
    {
        return fileSize.longValue();
    }

    /**
     * Get the remote file size as a Long object.
     *
     * @return the file size as a Long
     */
    public Long getFileSizeObject()
    {
        return fileSize;
    }

    /**
     * Get the query hit host or null.
     *
     * @return the query hit host object
     */
    public QueryHitHost getQueryHitHost()
    {
        return qhHost;
    }

    /**
     * Sets the query hit host.
     * @param qhHost the new query hit host.
     */
    public void setQueryHitHost( QueryHitHost qhHost )
    {
        this.qhHost = qhHost;
    }
    
    /**
     * Updates missing QueryHitHost information
     * @param qhHost
     */
    public void updateQueryHitHost( QueryHitHost host )
    {
        if ( qhHost.getHostGUID() == null )
        {
            qhHost.setHostGUID( host.getHostGUID() );
        }
        if ( qhHost.getHostSpeed() == -1 )
        {
            qhHost.setHostSpeed( host.getHostSpeed() );
        }
    }

    /**
     * Get the remote host address.
     *
     * @return a HostAddress for the remote host
     */
    public DestAddress getHostAddress()
    {
        return qhHost.getHostAddress();
    }

    public String getMetaData()
    {
        return metaData;
    }

    public void setMetaData( String data )
    {
        metaData = data;
    }

    public URN getURN()
    {
        return urn;
    }

    public String getSHA1()
    {
        if ( urn == null || !urn.isSha1Nid() )
        {
            return "";
        }
        return urn.getNamespaceSpecificString();
    }

    /**
     * Return the remote host speed.
     *
     * @return the host speed
     */
    public int getSpeed()
    {
        return qhHost.getHostSpeed();
    }


    /**
     * Return the remote host speed as an Integer instance.
     *
     * @return the remote host speed as an Integer
     */
    public Long getSpeedObject()
    {
        return qhHost.getHostSpeedObject();
    }

    /**
     * Find out if this is in a download queue.
     *
     * @return true if this is in a download queue, false otherwise
     */
    public boolean isInDownloadQueue()
    {
        return isInDownloadQueue;
    }

    /**
     * Set wether this is in a download queue.
     *
     * @param inDownloadQueue  the new boolean flag stating if this RemoteFile
     *        is in a download queue
     */
    public void setInDownloadQueue(boolean inDownloadQueue)
    {
        isInDownloadQueue = inDownloadQueue;
    }

    /**
     * The score defines how well the search term matches the filename.
     */
    public Short getScore()
    {
        return score;
    }

    public boolean equals( Object obj )
    {
        if ( !(obj instanceof RemoteFile) )
        {
            return false;
        }
        RemoteFile b = (RemoteFile) obj;
        return qhHost.getHostAddress().equals( b.qhHost.getHostAddress() )
            && fileIndex == b.fileIndex;
    }
    
    public int hashCode()
    {
        int h = 0;
        h = (31 * h) + qhHost.getHostAddress().hashCode();
        h = (127 * h) + fileIndex;
        return h;
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer( super.toString() );
        buffer.append( "  " );
        buffer.append( qhHost.getHostAddress() );
        buffer.append( "  " );
        buffer.append( filename );
        buffer.append( "  " );
        buffer.append( fileIndex );
        return buffer.toString();
    }
    
    /**
     * Creates the display name of the remote file. If we know a 
     * path of the file the path is prefixed to the filename,
     * otherwise just the filename is used to define the display
     * name.
     */
    private void buildDisplayName()
    {
        StringBuffer buffer = new StringBuffer( 128 );
        
        if ( pathInfo != null && pathInfo.length() > 0 )
        {
            String displayPath = pathInfo.replace( '\\', '/' );
            buffer.append( displayPath );
            if ( !displayPath.endsWith("/") )
            {
                buffer.append( "/" );
            }
        }
        // Some servents just return the path along with the filename.
        // Path info is platform-dependent.
        String displayFileName = filename.replace( '\\', '/' );
        // we do not cut the path anymore it might be informative.
        buffer.append( displayFileName );
        
        displayName = buffer.toString();
    }
}