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
 *  $Id: UploadEngine.java,v 1.51 2005/11/03 23:30:12 gregork Exp $
 */
package phex.upload;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

import phex.common.AlternateLocationContainer;
import phex.common.ServiceManager;
import phex.common.URN;
import phex.common.address.AddressUtils;
import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.common.bandwidth.BandwidthController;
import phex.common.bandwidth.BandwidthManager;
import phex.download.swarming.SWDownloadFile;
import phex.download.swarming.SwarmingManager;
import phex.host.HostManager;
import phex.host.NetworkHostsContainer;
import phex.http.*;
import phex.net.connection.Connection;
import phex.net.presentation.SocketFacade;
import phex.share.*;
import phex.statistic.SimpleStatisticProvider;
import phex.statistic.StatisticProviderConstants;
import phex.statistic.StatisticsManager;
import phex.thex.ShareFileThexData;
import phex.utils.*;
import phex.xml.thex.ThexHashTree;
import phex.xml.thex.ThexHashTreeCodec;

import com.onionnetworks.dime.DimeGenerator;
import com.onionnetworks.dime.DimeRecord;

/**
 * The UploadEngine is handling the process of uploading a file. This includes
 * evaluating and responding to the HTTPRequest.
 * 
 * @author Gregor Koukkoullis
 *  
 */
public class UploadEngine
{
    /**
     * The upload buffer size.
     */
    private static final int BUFFER_LENGTH = 16 * 1024;

    /**
     * The upload file access reader.
     */
    private RandomAccessFile raFile;
    
    private final Connection connection;

    /**
     * Indicates whether the upload is queued or not.
     */
    private boolean isUploadQueued;

    /**
     * Represents the timestamp that must be passed before the client is allowed
     * to request again.
     */
    private long minNextPollTime;

    /**
     * The start offset of the upload.
     */
    private long startOffset;

    /**
     * The end offset of the upload ( inclusive )
     */
    private long endOffset;

    /**
     * The offset of the upload file to start the upload from.
     */
    private long fileStartOffset;

    /**
     * The current HTTPRequest.
     */
    private HTTPRequest httpRequest;

    /**
     * The upload info object of the current upload.
     */
    private UploadState uploadState;

    /**
     * Indicates whether the connection to the host ip is counted or not.
     */
    private boolean isIPCounted;

    /**
     * The address of the downloading host.
     */
    private DestAddress hostAddress;

    /**
     * This is a List holding all AltLocs already send to this connection during
     * this session. It is used to make sure the same AltLocs are not send twice
     * to the same connection.
     */
    private Set sendAltLocSet;

    /**
     * Flag indicating if the upload is counted inside this upload engine.
     */
    public boolean isUploadCounted;

    /**
     * Indicates whether the connection is persistent or not. Like Keep-Alive
     * connections.
     */
    private boolean isPersistentConnection;

    /**
     * The ShareFile currently used for upload.
     */
    private ShareFile uploadShareFile;

    public UploadEngine( Connection connection,
        HTTPRequest httpRequest)
    {
        this.connection = connection;
        connection.setBandwidthController(BandwidthManager.getInstance()
            .getUploadBandwidthController());
        this.httpRequest = httpRequest;
        isIPCounted = false;
        isUploadCounted = false;
        sendAltLocSet = new HashSet();
        
        SocketFacade socket = connection.getSocket();
        hostAddress = socket.getRemoteAddress();
        uploadState = new UploadState( hostAddress, VendorCodes.UNKNOWN );
    }

    public void startUpload()
    {
        NLogger.debug( UploadEngine.class, "Starts upload.");
        try
        {
            do
            {
                try
                {
                    boolean accepted = evaluateHTTPRequest();
                    if (!accepted)
                    {
                        return;
                    }

                    if (  !isUploadQueued 
                       && !httpRequest.getRequestMethod().equals( HTTPRequest.HEAD_REQUEST ) )
                    {
                        GnutellaRequest gRequest = httpRequest.getGnutellaRequest();
                        if ( gRequest.isTigerTreeRequest() )
                        {
                            sendDIMEMessage( uploadShareFile );
                        }
                        else
                        {
                            sendBinaryData();
                        }
                    }
                }
                catch (IOException exp)
                {
                    // in the case of evaluateHTTPRequest() and sendBinaryData()
                    // we handle a IOException as a aborted status
                    uploadState.setStatus(UploadConstants.STATUS_ABORTED);
                    throw exp;
                }

                if (isPersistentConnection)
                {
                    // in case of readNextHTTPRequest() we handle a IOException
                    // not as a aborted status since it could mean the connection is
                    // not kept alive.
                    try
                    {
                        readNextHTTPRequest();
                    }
                    catch ( IOException exp )
                    {
                        NLogger.debug( UploadEngine.class, exp );
                        // apperently the remote side does not want to continue..
                        // finish the persistent connection status and quite.
                        isPersistentConnection = false;
                    }
                }
            }
            while (isPersistentConnection);
        }
        catch (Exception exp)
        {// catch all thats left...
            uploadState.setStatus(UploadConstants.STATUS_ABORTED);
            NLogger.warn(UploadEngine.class, exp, exp);
        }
        finally
        {
            stopUpload();
            UploadManager uploadMgr = UploadManager.getInstance();
            if (isIPCounted)
            {
                uploadMgr.releaseUploadIP(hostAddress);
            }
            // set to null to give free for gc
            uploadState.setUploadEngine(null);

            if (isUploadQueued)
            {
                uploadMgr.removeQueuedUpload(uploadState);
            }
        }
    }

    public void stopUpload()
    {
        // disconnecting this connection will cause a IOException in the upload
        // thead and will result in cleaning up the download.
        connection.disconnect();
        IOUtil.closeQuietly( raFile );
    }

    private void sendBinaryData() throws IOException
    {
        NLogger.debug(UploadEngine.class, 
            "About to send binary range: " + startOffset + " to " + endOffset);

        // the upload is actually starting now..
        // if not yet done we are counting this upload.
        if (!isUploadCounted)
        {
            // count upload even if upload is just starting and not finished
            // yet.
            // swarming uploads fail often anyway.
            uploadShareFile.incUploadCount();
            // Increment the completed uploads count
            StatisticsManager statMgr = StatisticsManager.getInstance();
            SimpleStatisticProvider provider = (SimpleStatisticProvider) statMgr
                .getStatisticProvider(StatisticProviderConstants.SESSION_UPLOAD_COUNT_PROVIDER);
            provider.increment(1);
            isUploadCounted = true;
        }

        uploadState.setStatus(UploadConstants.STATUS_UPLOADING);
        // open file
        File sourceFile = uploadState.getUploadFile();
        raFile = new RandomAccessFile(sourceFile, "r");
        raFile.seek(fileStartOffset);

        BandwidthController throttleController = BandwidthManager.getInstance()
            .getUploadBandwidthController();

        long lengthToUpload = endOffset - startOffset + 1;
        long lengthUploaded = 0;
        int lengthRead = -1;
        int likeToSend;
        OutputStream outStream = connection.getOutputStream();
        byte[] buffer = new byte[BUFFER_LENGTH];
        while (lengthToUpload > 0)
        {
            // make sure we dont send more then requested
            likeToSend = (int) Math.min(lengthToUpload, (long) BUFFER_LENGTH);
            // we may be throttled to less than this amount
            int available = throttleController.getAvailableByteCount( true );
            int ableToSend = Math.min( likeToSend, available );
            NLogger.debug( UploadEngine.class, 
                "Reading in " + ableToSend + " bytes at " + 
                raFile.getFilePointer() + " from " + sourceFile.getName());
            lengthRead = raFile.read(buffer, 0, ableToSend);
            //  lengthRead should equal ableToSend. I can't see a reason
            //  why it wouldn't....
            if (lengthRead == -1)
            {
                break;
            }

            outStream.write(buffer, 0, lengthRead);

            lengthToUpload -= lengthRead;
            lengthUploaded += lengthRead;

            uploadState.setTransferredDataSize(lengthUploaded);
        }
        uploadState.setStatus(UploadConstants.STATUS_COMPLETED);
    }

    /**
     * Evaluates the http request and send appropiate http responses. Returns
     * true if the request was accepted and the upload can continue.
     * 
     * @return true if the request was accepted and the upload can continue.
     * @throws IOException
     */
    private boolean evaluateHTTPRequest() throws IOException
    {
        NLogger.debug( UploadEngine.class, "HTTP Request: "
            + httpRequest.buildHTTPRequestString());

        GnutellaRequest gRequest = httpRequest.getGnutellaRequest();
        if ( gRequest == null )
        {
            throw new IOException("Not a Gnutella file request.");
        }

        HTTPHeader header;
        if (gRequest.getURN() == null)
        {
            header = httpRequest
                .getHeader(GnutellaHeaderNames.X_GNUTELLA_CONTENT_URN);
            if (header != null)
            {
                if (URN.isValidURN(header.getValue()))
                {
                    URN urn = new URN(header.getValue());
                    gRequest.setContentURN(urn);
                }
            }
        }

        SocketFacade socket = connection.getSocket();
        int port = -1;
        header = httpRequest.getHeader(GnutellaHeaderNames.LISTEN_IP);
        if (header == null)
        {
            header = httpRequest.getHeader(GnutellaHeaderNames.X_MY_ADDRESS);
        }
        if (header != null)
        {
            // parse port
            port = AddressUtils.parsePort(header.getValue());
            if (port <= 0)
            {
                port = socket.getRemoteAddress().getPort();
            }
        }
        
        hostAddress = new DefaultDestAddress(socket.getRemoteAddress().getHostName(),
            port);
        UploadManager uploadMgr = UploadManager.getInstance();

        HTTPResponse httpResponse;
        if (!isIPCounted)
        {
            boolean succ = uploadMgr.validateAndCountIP(hostAddress);
            if (!succ)
            {
                httpResponse = new HTTPResponse((short) 503,
                    "Upload Limit Reached for IP", true);
                sendHTTPResponse(httpResponse);
                return false;
            }
            isIPCounted = true;
        }

        ShareFile requestedShareFile = findShareFile(gRequest);
        if (requestedShareFile == null)
        {
            httpResponse = new HTTPResponse((short) 404, "File not found", true);
            sendHTTPResponse(httpResponse);
            return false;
        }

        isUploadQueued = false;
        if (uploadMgr.isHostBusy())
        {
            header = httpRequest.getHeader(GnutellaHeaderNames.X_QUEUE);
            if (header == null || !ServiceManager.sCfg.allowUploadQueuing
                || uploadMgr.isQueueLimitReached())
            {// queueing is not supported
                httpResponse = new HTTPResponse((short) 503,
                    "Upload Limit Reached", true);
                addAltLocResponseHeader(httpResponse, requestedShareFile);
                sendHTTPResponse(httpResponse);
                return false;
            }
            isUploadQueued = true;
        }

        HTTPHeader rangeHeader = null;
        HTTPRangeSet uploadRange = null;
        Range uploadRangeEntry = null;
        if ( !gRequest.isTigerTreeRequest() )
        {
            rangeHeader = httpRequest.getHeader(HTTPHeaderNames.RANGE);
            if (rangeHeader != null)
            {
                uploadRange = HTTPRangeSet.parseHTTPRangeSet(rangeHeader
                    .getValue());
                if (uploadRange == null)
                {
                    // this is not 416 Requested Range Not Satisfiable since
                    // we have a parsing error on the requested range.
                    httpResponse = new HTTPResponse((short) 500,
                        "Requested Range Not Parseable", true);
                    addAltLocResponseHeader(httpResponse, requestedShareFile);
                    sendHTTPResponse(httpResponse);
                    return false;
                }
            }
            else
            {
                uploadRange = new HTTPRangeSet(0, HTTPRangeSet.NOT_SET);
            }
            uploadRangeEntry = uploadRange.getFirstRange();
            short rangeStatus = requestedShareFile
                .getRangeAvailableStatus(uploadRangeEntry);

            if (rangeStatus != UploadConstants.RANGE_AVAILABLE)
            {
                if (rangeStatus == UploadConstants.RANGE_NOT_AVAILABLE)
                {
                    httpResponse = new HTTPResponse((short) 503,
                        "Requested Range Not Available", true);
                }
                else
                //case: if ( rangeStatus ==
                // UploadConstants.RANGE_NOT_SATISFIABLE )
                {
                    httpResponse = new HTTPResponse((short) 416,
                        "Requested Range Not Satisfiable", true);
                }
                if (requestedShareFile instanceof PartialShareFile)
                {
                    // TODO we could check if the partial file is progressing
                    // and
                    // return a 416 when the range will not come available soon.
                    PartialShareFile pShareFile = (PartialShareFile) requestedShareFile;
                    httpResponse.addHeader(new HTTPHeader(
                        GnutellaHeaderNames.X_AVAILABLE_RANGES, pShareFile
                            .buildXAvailableRangesString()));
                }
                addAltLocResponseHeader(httpResponse, requestedShareFile);
                sendHTTPResponse(httpResponse);
                return false;

            }
        }
        // everything is right... collect upload infos
        String vendor = null;
        header = httpRequest.getHeader(HTTPHeaderNames.USER_AGENT);
        if (header != null)
        {
            vendor = header.getValue();
        }
        else
        {
            vendor = "";
        }

        // check for persistent connection...
        // a connection is assumed to be persistent if its a HTTP 1.1 connection
        // with no 'Connection: close' header. Or a HTTP connection with
        // Connection: Keep-Alive header.
        header = httpRequest.getHeader(HTTPHeaderNames.CONNECTION);
        if (HTTPRequest.HTTP_11.equals(httpRequest.getHTTPVersion()))
        {
            if (header != null && header.getValue().equalsIgnoreCase("CLOSE"))
            {
                isPersistentConnection = false;
            }
            else
            {
                isPersistentConnection = true;
            }
        }
        else
        {
            if (header != null
                && header.getValue().equalsIgnoreCase("KEEP-ALIVE"))
            {
                isPersistentConnection = true;
            }
            else
            {
                isPersistentConnection = false;
            }
        }

        if (isUploadQueued)
        {
            // queueing is supported
            int queuePosition;
            uploadState.update(hostAddress, vendor);
            queuePosition = uploadMgr.getQueuedPosition(uploadState) + 1;
            if ( queuePosition < 0 )
            {// missing in queue list
                queuePosition = uploadMgr.addQueuedUpload(uploadState) + 1;
            }
            uploadState.setStatus(UploadConstants.STATUS_QUEUED);

            int queueLength = uploadMgr.getUploadQueueSize();
            int uploadLimit = ServiceManager.sCfg.mMaxUpload;
            int pollMin = ServiceManager.sCfg.minUploadQueuePollTime;
            int pollMax = ServiceManager.sCfg.maxUploadQueuePollTime;

            httpResponse = new HTTPResponse((short) 503, "Remotely Queued",
                true);
            addAltLocResponseHeader(httpResponse, requestedShareFile);

            XQueueParameters xQueueParas = new XQueueParameters(queuePosition,
                queueLength, uploadLimit, pollMin, pollMax);
            httpResponse.addHeader(new HTTPHeader(GnutellaHeaderNames.X_QUEUE,
                xQueueParas.buildHTTPString()));
            sendHTTPResponse(httpResponse);

            socket.setSoTimeout(pollMax * 1000);
            minNextPollTime = System.currentTimeMillis() + pollMin * 1000;
            return true;
        }

        // standard upload...

        HTTPHeader additionalResponseHeader = null;
        long contentLength = 0;
        if ( !gRequest.isTigerTreeRequest() )
        {
            if (requestedShareFile instanceof PartialShareFile)
            {
                PartialShareFile pShareFile = (PartialShareFile) requestedShareFile;

                // call adjusts uploadRangeEntry to fit...
                pShareFile.findFittingPartForRange(uploadRangeEntry);
                fileStartOffset = pShareFile.getFileStartOffset();
                additionalResponseHeader = new HTTPHeader(
                    GnutellaHeaderNames.X_AVAILABLE_RANGES, pShareFile
                        .buildXAvailableRangesString());
            }
            else
            {
                fileStartOffset = uploadRangeEntry
                    .getStartOffset(requestedShareFile.getFileSize());
            }
            startOffset = uploadRangeEntry.getStartOffset(requestedShareFile
                .getFileSize());
            endOffset = uploadRangeEntry.getEndOffset(requestedShareFile
                .getFileSize());
            contentLength = endOffset - startOffset + 1;
        }

        URN sharedFileURN = requestedShareFile.getURN();
        if ( gRequest.isTigerTreeRequest() )
        {
            uploadState.update(hostAddress, vendor, 
                requestedShareFile.getFileName());
        }
        else
        {
            uploadState.update(hostAddress, vendor, requestedShareFile
                .getFileName(), sharedFileURN,
                requestedShareFile.getSystemFile(), contentLength);
        }
        if ( !uploadMgr.containsUploadState(uploadState) )
        {
            uploadMgr.addUploadState(uploadState);
        }
        uploadState.setUploadEngine(this);

        // form ok response...
        if ( !gRequest.isTigerTreeRequest() )
        {
            if (startOffset == 0
                && endOffset == requestedShareFile.getFileSize() - 1)
            {
                httpResponse = new HTTPResponse((short) 200, "OK", true);
            }
            else
            {
                httpResponse = new HTTPResponse((short) 206, "Partial Content",
                    true);
            }

            if (additionalResponseHeader != null)
            {
                httpResponse.addHeader(additionalResponseHeader);
            }
        }
        else
        {
            httpResponse = new HTTPResponse((short) 200, "OK", true);
        }

        // TODO for browser request we might like to return explicite content
        // types:
        // contentType = MimeTypeMapping.getMimeTypeForExtension( ext );
        httpResponse.addHeader(new HTTPHeader(HTTPHeaderNames.CONTENT_TYPE,
            "application/binary"));
        if ( !gRequest.isTigerTreeRequest() )
        {
            httpResponse.addHeader(new HTTPHeader(
                HTTPHeaderNames.CONTENT_LENGTH, String.valueOf(contentLength)));

            httpResponse.addHeader(new HTTPHeader(
                HTTPHeaderNames.CONTENT_RANGE, "bytes " + startOffset + "-"
                    + endOffset + "/" + requestedShareFile.getFileSize()));
        }
        
        // in case of tiger tree request we need to close the connection since
        // we dont know the content length of the dime here already...
        // TODO: we could pre calculate the dime length in this case!
        if ( isPersistentConnection && !gRequest.isTigerTreeRequest() )
        {
            httpResponse.addHeader( new HTTPHeader( HTTPHeaderNames.CONNECTION,
                "Keep-Alive" ) );
        }
        else
        {
            httpResponse.addHeader(new HTTPHeader(HTTPHeaderNames.CONNECTION,
                "close"));
            isPersistentConnection = false;
        }

        if ( sharedFileURN != null )
        {
            httpResponse.addHeader(new HTTPHeader(
                GnutellaHeaderNames.X_GNUTELLA_CONTENT_URN, sharedFileURN
                    .getAsString()));
        }
        if ( !gRequest.isTigerTreeRequest() )
        {
            addAltLocResponseHeader(httpResponse, requestedShareFile);
            addPushProxyResponseHeader( httpResponse );
        }
        if ( sharedFileURN != null && !gRequest.isTigerTreeRequest() )
        {
            // collect alternate locations from request...
            AlternateLocationContainer altLocContainer = requestedShareFile
                .getAltLocContainer();
            HTTPHeader[] headers = httpRequest
                .getHeaders(GnutellaHeaderNames.ALT_LOC);
            altLocContainer.addFromUriResHTTPHeaders(headers);
            headers = httpRequest.getHeaders(GnutellaHeaderNames.X_ALT_LOC);
            altLocContainer.addFromUriResHTTPHeaders(headers);
            headers = httpRequest.getHeaders(GnutellaHeaderNames.X_ALT);
            altLocContainer.addFromCompactIpHTTPHeaders(headers, sharedFileURN);

            // add thex download url
            ShareFileThexData thexData = requestedShareFile.getThexData( true );
            if ( thexData != null )
            {
                String thexRootHash = thexData.getRootHash();
                HTTPHeader thexHeader = new HTTPHeader( GnutellaHeaderNames.X_THEX_URI, 
                    URLUtil.buildName2ResThexURL( sharedFileURN, thexRootHash ) );
                httpResponse.addHeader( thexHeader );
            }
        }
                
        sendHTTPResponse(httpResponse);
        socket.setSoTimeout(ServiceManager.sCfg.socketConnectTimeout);
        // check if we need to count again..
        // this is the case if we have no PartialShareFile (same
        // PartialShareFile is never equal again, since its always a new instance) 
        // and the ShareFile is not equal to the requested file.
        if (   uploadShareFile != null
            && !(requestedShareFile instanceof PartialShareFile)
            && uploadShareFile != requestedShareFile)
        {
            isUploadCounted = false;
        }
        uploadShareFile = requestedShareFile;
        minNextPollTime = -1;
        return true;
    }

    private void addAltLocResponseHeader(HTTPResponse httpResponse,
        ShareFile shareFile)
    {
        HTTPHeader header;
        AlternateLocationContainer altLocContainer = shareFile
            .getAltLocContainer();
        header = altLocContainer.getAltLocHTTPHeaderForAddress(
            GnutellaHeaderNames.X_ALT, hostAddress, sendAltLocSet);
        if (header != null)
        {
            httpResponse.addHeader(header);
        }
    }
    
    private void addPushProxyResponseHeader(HTTPResponse httpResponse)
    {
        HostManager hostManager = HostManager.getInstance();
        NetworkHostsContainer networkHostsContainer = 
            hostManager.getNetworkHostsContainer();
        DestAddress[] pushProxyAddresses = networkHostsContainer.getPushProxies();
        if ( pushProxyAddresses == null )
        {
            return;
        }
        StringBuffer headerValue = new StringBuffer();
        int count = Math.min( 4, pushProxyAddresses.length);
        for (int i = 0; i < count; i++)
        {
            if ( i > 0 )
            {
                headerValue.append( "," );
            }
            headerValue.append( pushProxyAddresses[i].getFullHostName() );
        }        
        if ( headerValue.length() > 0 )
        {
            HTTPHeader header = new HTTPHeader( GnutellaHeaderNames.X_PUSH_PROXY,
                headerValue.toString() );
            httpResponse.addHeader( header );
        }
    }

    private void readNextHTTPRequest() throws IOException
    {
        try
        {
            httpRequest = HTTPProcessor.parseHTTPRequest( connection );
            if (minNextPollTime > 0
                && System.currentTimeMillis() < minNextPollTime)
            {// the request came too soon. Disconnect faulty client...
                throw new IOException("Queued host is requesting too soon.");
            }
        }
        catch (HTTPMessageException exp)
        {
            throw new IOException("Invalid HTTP Message: " + exp.getMessage());
        }
    }

    private void sendHTTPResponse(HTTPResponse httpResponse) throws IOException
    {
        String httpResponseStr = httpResponse.buildHTTPResponseString();
        NLogger.debug( UploadEngine.class, "HTTP Response: "
            + httpResponseStr);
        connection.write( httpResponseStr.getBytes() );
    }

    private ShareFile findShareFile(GnutellaRequest gRequest)
    {
        ShareFile shareFile = null;

        SharedFilesService sharedFilesService = ShareManager.getInstance()
            .getSharedFilesService();

        // first check for a URN
        URN requestURN = gRequest.getURN();
        if (requestURN != null)
        {// get request contains urn
            if (!(requestURN.isSha1Nid()))
            {
                requestURN = new URN("urn:sha1:" + requestURN.getSHA1Nss());
            }
            shareFile = sharedFilesService.getFileByURN(requestURN);
            // look for partials..
            if (shareFile == null && ServiceManager.sCfg.arePartialFilesShared)
            {
                SwarmingManager swMgr = SwarmingManager.getInstance();
                SWDownloadFile dwFile = swMgr.getDownloadFileByURN(requestURN);
                if (dwFile != null)
                {
                    shareFile = new PartialShareFile(dwFile);
                }
            }
        }
        // file index is -1 when parsing was wrong
        else if (gRequest.getFileIndex() != -1)
        {
            int index = gRequest.getFileIndex();
            shareFile = sharedFilesService.getFileByIndex(index);
            if (shareFile != null)
            {
                String shareFileName = shareFile.getFileName();
                // if filename dosn't match
                if (!gRequest.getFileName().equalsIgnoreCase(shareFileName))
                {
                    NLogger.debug( UploadEngine.class, 
                        "Requested index '" + index + "' with filename '"
                            + shareFileName
                            + "' dosn't match request filename '"
                            + gRequest.getFileName() + "'.");
                    shareFile = null;
                }
            }
            else
            {
                // TODO currently this will not work right because the file hash
                // contains
                // the full path name informations of the file. But we only look
                // for the filename.
                // TODO this should be also used if the index returns a file
                // with a different filename then the requested filename
                if (gRequest.getFileName() != null)
                {
                    shareFile = sharedFilesService.getFileByName(gRequest.getFileName());
                }
            }
        }
        return shareFile;
    }

    private void sendDIMEMessage(ShareFile shareFile) throws IOException
    {
        uploadState.setStatus(UploadConstants.STATUS_UPLOADING);
        
        //I have to select the serialization of this shareFile
        ShareFileThexData thexData = shareFile.getThexData( true );
        String uuidStr = StringUtils.generateRandomUUIDString();

        //We get the THEX metadata
        ThexHashTree hashTree = new ThexHashTree();
        hashTree.setFileSize( String.valueOf( shareFile.getFileSize() ) );
        hashTree.setFileSegmentSize("1024");
        hashTree.setDigestAlgorithm("http://open-content.net/spec/digest/tiger");
        hashTree.setDigestOutputSize("24");
        hashTree.setSerializedTreeDepth( String.valueOf( thexData.getTreeDepth() ) );
        hashTree.setSerializedTreeType("http://open-content.net/spec/thex/breadthfirst");
        hashTree.setSerializedTreeUri("uuid:" + uuidStr);

        String type ="http://open-content.net/spec/thex/breadthfirst";
        byte[] metadata = ThexHashTreeCodec.generateThexHashTreeXML( hashTree );
        byte[] serialization = thexData.getSerializedTreeNodes();
        
        DimeGenerator dg = new DimeGenerator( connection.getOutputStream() );
        DimeRecord dr = new DimeRecord(metadata, DimeRecord.TypeNameFormat.MEDIA_TYPE, type, null);
        dg.addRecord(dr, false);
        DimeRecord dr2 = new DimeRecord(serialization, DimeRecord.TypeNameFormat.URI, type, null);
        dg.addRecord(dr2, true);
        connection.flush();
        
        uploadState.setStatus(UploadConstants.STATUS_COMPLETED);
    }
}
