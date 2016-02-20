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
package phex.upload;

import java.io.IOException;

import phex.common.ThreadPool;
import phex.common.bandwidth.BandwidthController;
import phex.common.bandwidth.BandwidthManager;
import phex.http.HTTPMessageException;
import phex.http.HTTPProcessor;
import phex.http.HTTPRequest;
import phex.msg.PushRequestMsg;
import phex.net.connection.Connection;
import phex.net.connection.OIOSocketFactory;
import phex.net.presentation.SocketFacade;
import phex.share.ShareFile;
import phex.share.ShareManager;
import phex.statistic.UploadDownloadCountStatistic;
import phex.utils.*;

public class PushWorker implements Runnable
{
    private static final int PUSH_TIMEOUT = 45000;

    private final PushRequestMsg pushMsg;

    private Connection connection;

    public PushWorker(PushRequestMsg msg)
    {
        pushMsg = msg;
        ThreadPool.getInstance().addJob( this,
            "PushWorker-" + Integer.toHexString( hashCode() ) );
        UploadDownloadCountStatistic.pushUploadAttempts.increment( 1 );
    }

    public void run()
    {
        HTTPRequest httpRequest;
        try
        {
            httpRequest = connectAndGetRequest();
            if ( httpRequest == null )
            {
                UploadDownloadCountStatistic.pushUploadFailure.increment( 1 );
                return;
            }
            handleRequest( httpRequest );

            Logger.logMessage( Logger.FINER, Logger.UPLOAD,
                "PushWorker finished" );
        }
        catch (Exception exp)
        {
            NLogger.error( NLoggerNames.PUSH, exp, exp );
            return;
        }
        finally
        {
            if ( connection != null )
            {
                connection.disconnect();
            }
        }
    }

    /**
     * @param httpRequest
     * @throws IOException
     */
    private void handleRequest(HTTPRequest httpRequest)
    {
        Logger.logMessage( Logger.FINE, Logger.UPLOAD, "Handle PUSH request: "
            + httpRequest.buildHTTPRequestString() );
        UploadDownloadCountStatistic.pushUploadSuccess.increment( 1 );
        if ( httpRequest.isGnutellaRequest() )
        {
            UploadManager.getInstance().handleUploadRequest(
                connection, httpRequest );
        }
        else
        {
            // Handle the HTTP GET as a normal HTTP GET to upload file.
            // This is most likely a usual browse host request.
            ShareManager.getInstance().httpRequestHandler( connection,
                httpRequest );
        }
    }

    /**
     * @return
     */
    private HTTPRequest connectAndGetRequest()
    {
        try
        {
            HTTPRequest httpRequest;
            Logger.logMessage( Logger.FINE, Logger.UPLOAD,
                "Try PUSH connect to: " + pushMsg.getRequestAddress() );
            SocketFacade sock = OIOSocketFactory.connect( pushMsg.getRequestAddress(),
                PUSH_TIMEOUT );
            BandwidthController bwController = BandwidthManager.getInstance()
                .getUploadBandwidthController();
            connection = new Connection( sock, bwController );
            sendGIV( connection );
            httpRequest = HTTPProcessor.parseHTTPRequest( connection );
            return httpRequest;
        }
        catch (IOException exp)
        {
            Logger.logMessage( Logger.FINER, Logger.UPLOAD, exp );
            return null;
        }
        catch (HTTPMessageException exp)
        {
            Logger.logMessage( Logger.FINER, Logger.UPLOAD, exp );
            return null;
        }
    }

    /**
     * @param connection
     * @param sfile
     * @throws IOException
     */
    private void sendGIV(Connection connection) throws IOException
    {
        ShareManager shareMgr = ShareManager.getInstance();
        // I only give out file indexes in the int range
        ShareFile sfile = shareMgr.getSharedFilesService().getFileByIndex(
            (int) pushMsg.getFileIndex() );

        // Send the push greeting.
        // GIV <file-ref-num>:<ClientID GUID in hexdec>/<filename>\n\n
        StringBuffer buffer = new StringBuffer( 100 );
        buffer.append( "GIV " );
        buffer.append( pushMsg.getFileIndex() );
        buffer.append( ':' );
        buffer.append( pushMsg.getClientGUID().toHexString() );
        buffer.append( '/' );
        if ( sfile != null )
        {
            buffer.append( URLCodecUtils.encodeURL( sfile.getFileName() ) );
        }
        Logger.logMessage( Logger.FINE, Logger.UPLOAD, "Send GIV: "
            + buffer.toString() );
        byte[] buf = new byte[buffer.length() + 2];
        int len = IOUtil.serializeString( buffer.toString(), buf, 0 );
        buf[len++] = (byte) '\n';
        buf[len++] = (byte) '\n';
        connection.write(buf, 0, len);
        connection.flush();
    }
}