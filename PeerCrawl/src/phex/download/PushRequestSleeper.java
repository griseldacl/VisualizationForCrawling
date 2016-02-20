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
 *  $Id: PushRequestSleeper.java,v 1.20 2005/11/03 17:06:27 gregork Exp $
 */
package phex.download;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import phex.common.ServiceManager;
import phex.common.address.DestAddress;
import phex.connection.NetworkManager;
import phex.host.Host;
import phex.http.GnutellaHeaderNames;
import phex.http.HTTPHeaderNames;
import phex.http.HttpClientFactory;
import phex.msg.GUID;
import phex.msg.MsgManager;
import phex.msg.PushRequestMsg;
import phex.net.presentation.SocketFacade;
import phex.statistic.UploadDownloadCountStatistic;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

public class PushRequestSleeper
{
    private GUID clientGUID;
    private long fileIndex;
    private DestAddress[] pushProxyAddresses;

    /**
     * The connection of the remote servent after he conntacts us using the
     * PUSH request.
     */
    private SocketFacade givenSocket;

    public PushRequestSleeper( GUID aClientGUID, long aFileIndex,
        DestAddress[] pushProxyAddresses )
    {
        clientGUID = aClientGUID;
        fileIndex = aFileIndex;
        this.pushProxyAddresses = pushProxyAddresses;
    }

    public GUID getGUID()
    {
        return clientGUID;
    }

    /**
     * Returns the file index of the push request.
     */
    public long getFileIndex()
    {
        return fileIndex;
    }

    /**
     * we dont care about index or file name.. importent is that we have a
     * open connection and we try to request what we want through it...
     * @param aGivenSocket
     * @param givenGUID
     * @return
     */
    public synchronized boolean acceptGIVConnection( SocketFacade aGivenSocket, GUID givenGUID )
    {
        if ( !clientGUID.equals( givenGUID ) )
        {
            return false;
        }

        // we have a give from the requested host with the correct id and file
        // name
        givenSocket = aGivenSocket;
        // wake up the sleeper
        notify();
        return true;
    }

    /**
     * Request the candidate socket via a push request. This call blocks until
     * the request times out or the requested host answers.
     * Null is returned if the connection cant be made.
     */
    public synchronized SocketFacade requestSocketViaPush()
    {
        boolean succ = false;
        try
        {
            if ( pushProxyAddresses != null && pushProxyAddresses.length > 0 )
            {
                succ = requestViaPushProxies();
            }
            
            if ( !succ )
            {
                succ = requestViaPushRoute();
            }
            
            if ( !succ )
            {
                return null;
            }
            
            try
            {
                // wait until the host connects to use or the timeout is reached
                wait( ServiceManager.sCfg.mPushTransferTimeout );
            }
            catch ( InterruptedException exp )
            {// reset interruption
                Thread.currentThread().interrupt();
            }
            // no socket given during sleeping time.
            if ( givenSocket == null )
            {
                return null;
            }
            return givenSocket;
        }
        finally
        {
            PushHandler.unregisterPushRequestSleeper( this );
        }
    }
    
    private boolean requestViaPushProxies()
    {
        DestAddress myAddress = NetworkManager.getInstance().getLocalAddress();
        
        // format: /gnet/push-proxy?guid=<ServentIdAsABase16UrlEncodedString>
        String requestPart = "/gnet/push-proxy?guid=" + clientGUID.toHexString();
        
        if ( pushProxyAddresses.length > 0 )
        {
            UploadDownloadCountStatistic.pushDldPushProxyAttempts.increment(1);
        }
        
        for (int i = 0; i < pushProxyAddresses.length; i++)
        {
            String urlStr = "http://" + 
                    pushProxyAddresses[i].getFullHostName() + requestPart;
            if ( NLogger.isDebugEnabled( NLoggerNames.PUSH ) )
                NLogger.debug( NLoggerNames.PUSH, "PUSH via push proxy: " + urlStr );
            
            HttpClient httpClient = HttpClientFactory.createHttpClient();
            httpClient.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, 
                new DefaultHttpMethodRetryHandler( 1, false ) );
            HeadMethod method = null;
            try
            {
                method = new HeadMethod( urlStr );
                method.addRequestHeader( GnutellaHeaderNames.X_NODE,
                    myAddress.getFullHostName() );
                method.addRequestHeader( "Cache-Control", "no-cache");
                method.addRequestHeader( HTTPHeaderNames.CONNECTION,
                    "close" );
                
                int responseCode = httpClient.executeMethod( method );
                
                if ( NLogger.isDebugEnabled( NLoggerNames.PUSH ) )
                    NLogger.debug( NLoggerNames.PUSH, "PUSH via push proxy response code: "
                        + responseCode + " ("+urlStr+")" );
                
                // if 202
                if ( responseCode == HttpURLConnection.HTTP_ACCEPTED )
                {
                    UploadDownloadCountStatistic.pushDldPushProxySuccess.increment(1);
                    return true;
                }
            }
            catch ( IOException exp )
            {
                if ( NLogger.isWarnEnabled( NLoggerNames.PUSH ) )
                    NLogger.warn( NLoggerNames.PUSH, exp );
            }
            finally
            {
                if ( method != null )
                {
                    method.releaseConnection();
                }
            }
        }
        return false;
    }
    
    /**
     * <p>Prepares and sends a push request via the push route.</p>
     *
     * <p>This will attempt to queue a push message to send back in response to
     * a query hit that needs push to fetch the file. This is used to help obtain
     * a socket to download a file from.</p>
     */
    private boolean requestViaPushRoute()
    {
        DestAddress localAddress = NetworkManager.getInstance().getLocalAddress();
        // pushing only works if we have a valid IP to use in the push message.
        if( localAddress.getIpAddress() == null )
        {
            NLogger.warn( NLoggerNames.PUSH, "Local address has no IP to use for PUSH." );
            return false;
        }
        // according to the_gdf it is all right to send a push with a private
        // local address
        // http://groups.yahoo.com/group/the_gdf/message/14305
        PushRequestMsg push = new PushRequestMsg( clientGUID, fileIndex,
            localAddress );
        
        // Route the PushRequest msg.
        Host returnHost = MsgManager.getInstance().getPushRouting( clientGUID );
        
        // no push route it will not work...
        if (returnHost == null)
        {
            if ( NLogger.isDebugEnabled( NLoggerNames.PUSH ) )
                NLogger.debug( NLoggerNames.PUSH, "No PUSH route for " + clientGUID + "." );
            return false;
        }
        if ( NLogger.isDebugEnabled( NLoggerNames.PUSH ) )
            NLogger.debug( NLoggerNames.PUSH, "Push route for "
            + clientGUID + " is " + returnHost );
        returnHost.queueMessageToSend( push );
        return true;
    }
}