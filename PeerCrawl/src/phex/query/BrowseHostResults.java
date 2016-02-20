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

import java.io.*;
import java.util.ArrayList;


import phex.common.*;
import phex.common.address.DestAddress;
import phex.connection.*;
import phex.download.*;
import phex.host.*;
import phex.msg.*;
import phex.utils.*;


public class BrowseHostResults extends Search
{
    public static final short NO_ERROR = 0;
    public static final short CONNECTION_ERROR = 1;
    public static final short BROWSE_HOST_ERROR = 2;

    private short browseHostError;
    private DestAddress hostAddress;
    private GUID hostGUID;

    public BrowseHostResults( DestAddress hostAddress, GUID aHostGUID )
    {
        super( Localizer.getFormatedString( "BrowsingHost",
            new Object[]{ hostAddress.getFullHostName() } ) );
        this.hostAddress = hostAddress;
        hostGUID = aHostGUID;
    }

    public void startSearching()
    {
        isSearching = true;
        browseHostError = NO_ERROR;
        Runnable runner = new Runnable()
        {
            public void run()
            {
                BrowseHostConnection connection = new BrowseHostConnection(
                    hostAddress, hostGUID, BrowseHostResults.this );
                try
                {
                    connection.sendBrowseHostRequest();
                }
                catch ( BrowseHostException exp )
                {
                    Logger.logMessage( Logger.FINEST, Logger.SEARCH, exp);
                    browseHostError = BROWSE_HOST_ERROR;
                    stopSearching();
                }
                catch ( IOException exp )
                {// TODO integrate error handling if no results have been returned
                    Logger.logMessage( Logger.FINEST, Logger.SEARCH, exp);
                    browseHostError = CONNECTION_ERROR;
                    stopSearching();
                }
            }
        };
        ThreadPool.getInstance().addJob( runner,
            "BrowseHostConnection-" + Integer.toHexString(runner.hashCode()) );
        fireSearchStarted();
    }

    public short getBrowseHostError()
    {
        return browseHostError;
    }

    public void stopSearching()
    {
        isSearching = false;
        fireSearchStoped();
    }
    
    public int getProgress()
    {
        if ( !isSearching )
        {
            return 100;
        }
        else
        {
            return 0;
        }
    }

    public void processResponse( QueryResponseMsg msg )
    {
        long speed = msg.getRemoteHostSpeed();
        GUID rcID = msg.getRemoteClientID();

        DestAddress address = msg.getDestAddress();

        QueryHitHost qhHost = new QueryHitHost( rcID, address, speed );
        qhHost.setQHDFlags( msg.getPushNeededFlag(), msg.getServerBusyFlag(),
            msg.getHasUploadedFlag(), msg.getUploadSpeedFlag() );
        qhHost.setQueryResponseFields( msg );
        
        int hostRating = qhHost.getHostRating();

        QueryResponseRecord rec;
        RemoteFile rfile;

        int addStartIdx = queryHitList.size();
        int recordCount = msg.getRecordCount();
        ArrayList newHitList = new ArrayList( recordCount );
        for (int i = 0; i < recordCount; i++)
        {
            rec = msg.getMsgRecord(i);

            synchronized( displayedQueryHitList )
            {
                boolean isRecFiltered = false;
                if ( searchFilter != null )
                {
                    isRecFiltered = searchFilter.isFiltered(
                        rec.getFileSize(), rec.getFilename(), speed, hostRating );
                }

                if ( isRecFiltered )
                {
                    continue;
                }
                String filename = rec.getFilename();
                String pathInfo = rec.getPathInfo();
                long fileSize = rec.getFileSize();
                URN urn = rec.getURN();
                int fileIndex = rec.getFileIndex();

                rfile = new RemoteFile( qhHost, fileIndex, filename, pathInfo,
                    fileSize, urn, rec.getMetaData(), (short)100 );
                queryHitList.add( rfile );
                if ( !isRecFiltered )
                {
                    displayedQueryHitList.add( rfile );
                }
                newHitList.add( rfile );
            }
        }
        int addEndIdx = queryHitList.size();
        // if something was added...
        if ( addEndIdx > addStartIdx || newHitList.size() > 0 )
        {
            Object[] newHits = newHitList.toArray();
            fireSearchHitsAdded( addStartIdx, addEndIdx, newHits );
        }
    }
}