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
 *  $Id: SearchResultElement.java,v 1.6 2005/10/03 00:18:27 gregork Exp $
 */
package phex.gui.tabs.search;

import java.util.ArrayList;

import phex.common.IntObj;
import phex.common.ShortObj;
import phex.download.RemoteFile;
import phex.gui.models.SearchTreeTableModel;
import phex.utils.Localizer;

/**
 * This is a element that is part of the search result data model.
 * It is able to hold a single search results in form of a RemoteFile as well as
 * a list of RemoteFiles that represent the same on different hosts.
 */
public class SearchResultElement
{
    /**
     * The single search result element.
     */
    private RemoteFile remoteFile;
    
    /**
     * The list of search results that this can hold.
     */
    private ArrayList/*<RemoteFile>*/ remoteFileList;
    
    private ShortObj bestScore;
    private ShortObj bestRating;
    private IntObj bestSpeed;
    
    public SearchResultElement( RemoteFile aRemoteFile )
    {
        this.remoteFile = aRemoteFile;
        // set best values...
        bestScore = new ShortObj( remoteFile.getScore().shortValue() );
        bestRating = new ShortObj( remoteFile.getQueryHitHost().getHostRating() );
        bestSpeed = new IntObj( remoteFile.getSpeed() );
        remoteFileList = new ArrayList(2);
    }
    
    public void addRemoteFile( RemoteFile aRemoteFile )
    {
        synchronized( remoteFileList )
        {
            if ( remoteFileList.size() == 0 )
            {
                // first add single search result.
                remoteFileList.add( remoteFile );                
            }
            // now add additional search result...
            remoteFileList.add( aRemoteFile );
            
            // check and update best values
            if ( aRemoteFile.getScore().shortValue() > bestScore.value )
            {
                bestScore.value = aRemoteFile.getScore().shortValue();
            }
            if ( aRemoteFile.getQueryHitHost().getHostRating() > bestRating.value )
            {
                bestRating.value = aRemoteFile.getQueryHitHost().getHostRating();
            }
            if ( aRemoteFile.getSpeed() > bestSpeed.value )
            {
                bestSpeed.value = aRemoteFile.getSpeed();
            }
        }
    }
    
    public int getRemoteFileListCount()
    {
        return remoteFileList.size();
    }
    
    public RemoteFile getRemoteFileAt( int index )
    {
        if ( index < 0 || index >= remoteFileList.size() )
        {
            return null;
        }
        return (RemoteFile)remoteFileList.get( index );
    }
    
    public RemoteFile getSingleRemoteFile()
    {
        return remoteFile;
    }
    
    /**
     * @return
     */
    public RemoteFile[] getRemoteFiles()
    {
        if ( remoteFileList.size() == 0 )
        {
            return new RemoteFile[]{ remoteFile };
        }
        synchronized( remoteFileList )
        {
            RemoteFile[] remoteFiles = new RemoteFile[ remoteFileList.size() ];
            remoteFileList.toArray( remoteFiles );
            return remoteFiles;
        }
    }
    
    /**
     * Returns the displayed value in the table depending of the amount of
     * RemoteFiles collected.
     * This is currently only used for the values SCORE_MODEL_INDEX,
     * HOST_RATING_MODEL_INDEX, HOST_SPEED_MODEL_INDEX, HOST_MODEL_INDEX,
     * HOST_VENDOR_MODEL_INDEX.
     * @param modelIndex
     * @return
     */
    public Object getValue( int modelIndex )
    {
        switch (modelIndex)
        {
            case SearchTreeTableModel.SCORE_MODEL_INDEX:
                return bestScore;
            case SearchTreeTableModel.HOST_RATING_MODEL_INDEX:
                return bestRating;
            case SearchTreeTableModel.HOST_SPEED_MODEL_INDEX:
                return bestSpeed;
            case SearchTreeTableModel.HOST_MODEL_INDEX:
            {
                int listSize = remoteFileList.size();
	            if ( remoteFileList.size() > 0 )
	            {
	                return Localizer.getFormatedString( "NumberOfHosts", new Object[]{new Integer(listSize)} );
	            }
                return remoteFile.getQueryHitHost().getHostAddress();
            }
            case SearchTreeTableModel.HOST_VENDOR_MODEL_INDEX:
            {
                int listSize = remoteFileList.size();
	            if ( remoteFileList.size() > 0 )
	            {
	                return Localizer.getFormatedString( "NumberOfHosts", new Object[]{new Integer(listSize)} );
	            }
                return remoteFile.getQueryHitHost().getVendor();
            }
        }
        return "";
    }
}
