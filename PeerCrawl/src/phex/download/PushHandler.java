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
package phex.download;

import java.util.ArrayList;
import java.util.Iterator;

import phex.common.address.DestAddress;
import phex.download.swarming.SWDownloadCandidate;
import phex.msg.GUID;
import phex.net.presentation.SocketFacade;
import phex.statistic.UploadDownloadCountStatistic;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

public class PushHandler
{
    /**
     * This is a stand alone global class responsible for push handling.
     */
    private static PushHandler singleton = new PushHandler();

    /**
     * A list is used instead of a map since this will never contain many
     * entrys. And it's hard to create a usefull key since the file index might
     * change.
     */
    private ArrayList pushSleeperList;

    private PushHandler()
    {
        pushSleeperList = new ArrayList(5);
    }

    public static void handleIncommingGIV(SocketFacade aGivenSocket, GUID givenGUID,
        String givenFileName)
    {
        singleton.internalHandleIncommingGIV(aGivenSocket, givenGUID,
            givenFileName);
    }

    public static SocketFacade requestSocketViaPush(
        SWDownloadCandidate downloadCandidate )
    {
        if ( downloadCandidate.getGUID() == null ) { return null; }
        return singleton.internalRequestSocketViaPush(
            downloadCandidate.getGUID(),
            downloadCandidate.getFileIndex(), 
            downloadCandidate.getPushProxyAddresses() );
    }

    /**
     *
     * @param aClientGUID
     * @param aFileIndex
     * @param aFileName
     * @return Returns null if push request failes.
     */
    public static SocketFacade requestSocketViaPush(GUID aClientGUID,
        long aFileIndex )
    {
        return singleton.internalRequestSocketViaPush(aClientGUID, aFileIndex, null);
    }

    public static void unregisterPushRequestSleeper(PushRequestSleeper sleeper)
    {
        singleton.internalUnregisterPushRequestSleeper(sleeper);
    }

    private void internalHandleIncommingGIV(SocketFacade aGivenSocket,
        GUID givenGUID, String givenFileName)
    {
        if ( NLogger.isDebugEnabled( NLoggerNames.PUSH ) )
            NLogger.debug( NLoggerNames.PUSH, 
                "Handle incomming GIV response: " + " - " + givenFileName);
        synchronized (pushSleeperList)
        {
            Iterator iterator = pushSleeperList.iterator();
            while (iterator.hasNext())
            {
                boolean succ = ((PushRequestSleeper) iterator.next())
                    .acceptGIVConnection(aGivenSocket, givenGUID);
                if ( succ )
                {
                    if ( NLogger.isDebugEnabled( NLoggerNames.PUSH ) )
                        NLogger.debug( NLoggerNames.PUSH, 
                            "Accepted GIV response: " + " - " + givenFileName);
                    return;
                }
            }
        }
        if ( NLogger.isDebugEnabled( NLoggerNames.PUSH ) )
            NLogger.debug( NLoggerNames.PUSH, 
                "No Push request for GIV found: " + givenFileName);
    }

    private SocketFacade internalRequestSocketViaPush(GUID aClientGUID,
        long aFileIndex, DestAddress[] pushProxyAddresses )
    {
        if ( NLogger.isDebugEnabled( NLoggerNames.PUSH ) )
            NLogger.debug( NLoggerNames.PUSH, "Perform PUSH request..." );
        
        UploadDownloadCountStatistic.pushDownloadAttempts.increment(1);
        PushRequestSleeper pushSleeper = new PushRequestSleeper(aClientGUID,
            aFileIndex, pushProxyAddresses );
        synchronized (pushSleeperList)
        {
            pushSleeperList.add(pushSleeper);
        }
        SocketFacade socket = pushSleeper.requestSocketViaPush();
        if ( socket == null )
        {
            if ( NLogger.isDebugEnabled( NLoggerNames.PUSH ) )
                NLogger.debug( NLoggerNames.PUSH, "PUSH request failed." );
            UploadDownloadCountStatistic.pushDownloadFailure.increment(1);
        }
        else
        {
            if ( NLogger.isDebugEnabled( NLoggerNames.PUSH ) )
                NLogger.debug( NLoggerNames.PUSH, "PUSH request successful." );
            UploadDownloadCountStatistic.pushDownloadSuccess.increment(1);
        }
        return socket;
    }

    private void internalUnregisterPushRequestSleeper(PushRequestSleeper sleeper)
    {
        synchronized (pushSleeperList)
        {
            pushSleeperList.remove(sleeper);
        }
    }
}