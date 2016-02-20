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
 *  $Id: DownloadEngine.java,v 1.87 2005/11/13 10:08:09 gregork Exp $
 */
package phex.download;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.SocketException;
import java.util.*;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.httpclient.ChunkedInputStream;

import phex.common.*;
import phex.common.address.*;
import phex.common.bandwidth.BandwidthController;
import phex.common.file.ManagedFile;
import phex.common.file.ManagedFileException;
import phex.connection.ConnectionFailedException;
import phex.connection.NetworkManager;
import phex.download.swarming.*;
import phex.host.UnusableHostException;
import phex.http.*;
import phex.net.connection.Connection;
import phex.net.connection.OIOSocketFactory;
import phex.net.presentation.PresentationManager;
import phex.net.presentation.SocketFacade;
import phex.utils.*;

/**
 * This class is responsible to download a file using a HTTP connection.
 * The DownloadEngine is usually managed by a SWDownloadWorker.
 */
public class DownloadEngine
{
    private static final int BUFFER_LENGTH = 16 * 1024;
    private SWDownloadCandidate candidate;
    private SWDownloadSegment segment;

    /**
     * The download file object the big parent of all the download stuff of this
     * file.
     */
    private SWDownloadFile downloadFile;

    private Connection connection;
    private SocketFacade socket;
    private InputStream inStream;
    private boolean isKeepAliveSupported;
    private boolean isDownloadSuccessful;
    
    /**
     * Indicates if the download was stoped from externally.
     * Usually per user request.
     */
    private boolean isDownloadStopped;
    
    private ContentRange replyContentRange;
    private long replyContentLength;

    /**
     * Create a download engine
     * @param aDownloadFile the file to download
     * @param aCandidate the candidate to download the file from.
     */
    public DownloadEngine( SWDownloadFile aDownloadFile,
        SWDownloadCandidate aCandidate )
    {
        downloadFile = aDownloadFile;
        candidate = aCandidate;
    }
    
    /**
     * Sets a already available socket connection for the download engine and
     * prepares the DownloadEngine to use it.
     * @param socket
     * @throws IOException
     */
    public void setSocket( SocketFacade socket )
        throws IOException
    {
        assert this.socket != null;
        this.socket = socket;
        prepareConnection();
    }

    /**
     * Connects a unconnected Download engine and prepares the DownloadEngine for use.
     * @param timeout
     * @throws IOException
     * @throws InterruptedException 
     */
    public void connect( int timeout )
        throws IOException
    {
        assert socket == null;
        
        DestAddress address = candidate.getHostAddress();
        
        try
        {
            candidate.addToCandidateLog( "Wait for connect slot " + address.getHostName() + ":"
                + address.getPort() );
            NLogger.debug(NLoggerNames.Download_Engine,
                "Wait for connect slot " + address.getHostName() + ":"
                + address.getPort() );
            
            Runnable acquireCallback = new Runnable() {
                public void run()
                {
                    DestAddress address = candidate.getHostAddress();
                    candidate.addToCandidateLog( "Connecting to " + address.getHostName() + ":"
                        + address.getPort() );
                    NLogger.debug(NLoggerNames.Download_Engine,
                        "Connecting to " + address.getHostName() + ":"
                        + address.getPort() );
                    candidate.setStatus(SWDownloadConstants.STATUS_CANDIDATE_CONNECTING);
                }
            };
            
            socket = OIOSocketFactory.connect( address, timeout,
                acquireCallback );
            
        }
        catch ( SocketException exp )
        {// indicates a general communication error while connecting
            throw new ConnectionFailedException( exp.getMessage() );
        }
        prepareConnection( );
    }
    
    private void prepareConnection()
        throws IOException
    {
        BandwidthController bwController = downloadFile.getBandwidthController();
        connection = new Connection( socket, bwController );
        candidate.addToCandidateLog( "Connected successfully to " 
            + candidate.getHostAddress() + "." );
        candidate.setLastConnectionTime( System.currentTimeMillis() );
        NLogger.debug(NLoggerNames.Download_Engine,
            "Download Engine @" + Integer.toHexString(hashCode()) + " connected successfully to " 
            + candidate.getHostAddress() + ".");
        
        if ( isDownloadStopped )
        {
            throw new DownloadStoppedException( );
        }
    }

    public void exchangeHTTPHandshake( SWDownloadSegment aSegment )
        throws IOException, UnusableHostException, HTTPMessageException
    {
        NetworkManager networkMgr = NetworkManager.getInstance();
        isDownloadSuccessful = false;
        segment = aSegment;
        long downloadOffset = segment.getTransferStartPosition();

        OutputStreamWriter writer = new OutputStreamWriter(
            connection.getOutputStream() );
        // reset to default input stream
        inStream = connection.getInputStream();

        String requestUrl = candidate.getDownloadRequestUrl();
        
        HTTPRequest request = new HTTPRequest( "GET", requestUrl, true );
        request.addHeader( new HTTPHeader( HTTPHeaderNames.HOST,
            candidate.getHostAddress().getFullHostName() ) );
        request.addHeader( new HTTPHeader( GnutellaHeaderNames.LISTEN_IP,
             networkMgr.getLocalAddress().getFullHostName() ) );
        long segmentEndOffset = segment.getEnd();
        if ( segmentEndOffset == -1 )
        {// create header with open end
            request.addHeader( new HTTPHeader( HTTPHeaderNames.RANGE,
                "bytes=" + downloadOffset + "-" ) );
        }
        else
        {
            request.addHeader( new HTTPHeader( HTTPHeaderNames.RANGE,
                "bytes=" + downloadOffset + "-" + segmentEndOffset ) );
        }
        request.addHeader( new HTTPHeader( GnutellaHeaderNames.X_QUEUE,
            "0.1" ) );
        // request a HTTP keep alive connection, needed for queuing to work.
        request.addHeader( new HTTPHeader( HTTPHeaderNames.CONNECTION,
            "Keep-Alive" ) );
        if ( candidate.isG2FeatureAdded() )
        {
            request.addHeader( new HTTPHeader( "X-Features",
                "g2/1.0" ) );
        }

        buildAltLocRequestHeader(request);

        if ( ServiceManager.sCfg.isChatEnabled )
        {
            DestAddress ha = networkMgr.getLocalAddress();
            IpAddress myIp = ha.getIpAddress();
            if ( myIp == null || !myIp.isSiteLocalIP() )
            {
                request.addHeader( new HTTPHeader( "Chat", ha.getFullHostName() ) );
            }
        }

        String httpRequestStr = request.buildHTTPRequestString();

        NLogger.debug(NLoggerNames.Download_Engine,
            "HTTP Request to: " + candidate.getHostAddress() + "\n" + httpRequestStr );
        candidate.addToCandidateLog( "HTTP Request:\n" + httpRequestStr );
        // write request...
        writer.write( httpRequestStr );
        writer.flush();

        HTTPResponse response = HTTPProcessor.parseHTTPResponse( connection );
        if ( NLogger.isDebugEnabled( NLoggerNames.Download_Engine ) )
        {
            NLogger.debug(NLoggerNames.Download_Engine,
                "HTTP Response from: " + candidate.getHostAddress() + "\n" 
                + response.buildHTTPResponseString() );
        }
        if ( ServiceManager.sCfg.downloadCandidateLogBufferSize > 0 )
        {
            candidate.addToCandidateLog( "HTTP Response:\n" 
                + response.buildHTTPResponseString() );
        }

        HTTPHeader header = response.getHeader( HTTPHeaderNames.SERVER );
        if ( header != null )
        {
            candidate.setVendor( header.getValue() );
        }
        
        header = response.getHeader( HTTPHeaderNames.TRANSFER_ENCODING );
        if ( header != null )
        {
            if ( header.getValue().equals("chunked") )
            {
                inStream = new ChunkedInputStream( connection.getInputStream() );
            }
        }

        replyContentRange = null;
        header = response.getHeader( HTTPHeaderNames.CONTENT_RANGE );
        if ( header != null )
        {
            replyContentRange = parseContentRange( header.getValue() );
            // startPos of -1 indicates '*' (free to choose)
            if ( replyContentRange.startPos != -1 && 
                 replyContentRange.startPos != downloadOffset )
            {
                throw new IOException( "Invalid 'CONTENT-RANGE' start offset." );
            }
        }
        
        replyContentLength = -1;
        header = response.getHeader( HTTPHeaderNames.CONTENT_LENGTH );
        if ( header != null )
        {
            try
            {
                replyContentLength = header.longValue();
            }
            catch ( NumberFormatException exp )
            { //unknown 
            }
        }
        
        URN downloadFileURN = downloadFile.getFileURN();
        ArrayList contentURNHeaders = new ArrayList();
        header = response.getHeader( GnutellaHeaderNames.X_GNUTELLA_CONTENT_URN );
        if ( header != null )
        {
            contentURNHeaders.add( header );
        }
        // Shareaza 1.8.10.4 send also a bitprint urn in multiple X-Content-URN headers!
        HTTPHeader[] headers = response.getHeaders( GnutellaHeaderNames.X_CONTENT_URN );
        CollectionUtils.addAll( contentURNHeaders, headers );
        if ( downloadFileURN != null )
        {
            Iterator contentURNIterator = contentURNHeaders.iterator();
            while ( contentURNIterator.hasNext() )
            {
                header = (HTTPHeader)contentURNIterator.next();
                String contentURNStr = header.getValue();
                // check if I can understand urn.
                if ( URN.isValidURN( contentURNStr ) )
                {
                    URN contentURN = new URN( contentURNStr );
                    if ( !downloadFileURN.equals( contentURN ) )
                    {
                        throw new IOException( "Required URN and content URN do not match." );
                    }
                }
            }
        }

        // check Limewire chat support header.
        header = response.getHeader( GnutellaHeaderNames.CHAT );
        if ( header != null )
        {
            candidate.setChatSupported( true );
        }
        // read out REMOTE-IP header... to update my IP
        header = response.getHeader( GnutellaHeaderNames.REMOTE_IP );
        if ( header != null )
        {
            byte[] remoteIP = AddressUtils.parseIP( header.getValue() );
            if ( remoteIP != null )
            {
                IpAddress ip = new IpAddress( remoteIP );
                DestAddress address = PresentationManager.getInstance().createHostAddress(ip, -1);
                networkMgr.updateLocalAddress( address );                
            }
        }
        
        int httpCode = response.getStatusCode();

        // read available ranges
        header = response.getHeader( GnutellaHeaderNames.X_AVAILABLE_RANGES );
        if ( header != null )
        {
            HTTPRangeSet availableRanges =
                HTTPRangeSet.parseHTTPRangeSet( header.getValue() );
            if ( availableRanges == null )
            {// failed to parse... give more detailed error report
                NLogger.error(NLoggerNames.Download_Engine,
                    "Failed to parse X-Available-Ranges in "
                    + candidate.getVendor() + " request: "
                    + response.buildHTTPResponseString() );
            }
            candidate.setAvailableRangeSet( availableRanges );
        }
        else if ( httpCode >= 200 && httpCode < 300 
            && downloadFile.getTotalDataSize() != SWDownloadConstants.UNKNOWN_FILE_SIZE)
        {// OK header and no available range header.. we assume candidate
         // shares the whole file
            candidate.setAvailableRangeSet( new HTTPRangeSet(
                0, downloadFile.getTotalDataSize() - 1) );
        }
        
        // collect alternate locations...
        List altLocList = new ArrayList();
        headers = response.getHeaders( GnutellaHeaderNames.ALT_LOC );
        List altLocTmpList = AlternateLocationContainer.parseUriResAltLocFromHTTPHeaders( headers );
        altLocList.addAll( altLocTmpList );
        
        headers = response.getHeaders( GnutellaHeaderNames.X_ALT_LOC );
        altLocTmpList = AlternateLocationContainer.parseUriResAltLocFromHTTPHeaders( headers );
        altLocList.addAll( altLocTmpList );
        
        headers = response.getHeaders( GnutellaHeaderNames.X_ALT );
        altLocTmpList = AlternateLocationContainer.parseCompactIpAltLocFromHTTPHeaders( headers,
            downloadFileURN );
        altLocList.addAll( altLocTmpList );
        
        // TODO1 huh?? dont we pare X-NALT???? 
        
        Iterator iterator = altLocList.iterator();
        while( iterator.hasNext() )
        {
            downloadFile.addDownloadCandidate( (AlternateLocation)iterator.next() );
        }
        
        // collect push proxies.
        // first the old headers..
        headers = response.getHeaders( "X-Pushproxies" );
        handlePushProxyHeaders( headers );
        headers = response.getHeaders( "X-Push-Proxies" );
        handlePushProxyHeaders( headers );
        // now the standard header
        headers = response.getHeaders( GnutellaHeaderNames.X_PUSH_PROXY );
        handlePushProxyHeaders( headers );
        
        updateKeepAliveSupport( response );

        if ( httpCode >= 200 && httpCode < 300 )
        {// code accepted
            
            // check if we can accept the urn...
            if ( contentURNHeaders.size() == 0 
                && requestUrl.startsWith(GnutellaRequest.GNUTELLA_URI_RES_PREFIX) )
            {// we requested a download via /uri-res resource urn.
             // we expect that the result contains a x-gnutella-content-urn
             // or Shareaza X-Content-URN header.
                throw new IOException(
                    "Response to uri-res request without valid Content-URN header." );
            }
            
            // check if we need and can update our file and segment size.
            if ( downloadFile.getTotalDataSize() == SWDownloadConstants.UNKNOWN_FILE_SIZE )
            {
                // we have a file with an unknown data size. For aditional check assert
                // certain parameters
                assert( segment.getTotalDataSize() == -1 );
                // Now verify if we have the great chance to update our file data!
                if ( replyContentRange != null && 
                     replyContentRange.totalLength != SWDownloadConstants.UNKNOWN_FILE_SIZE )
                {
                    downloadFile.setFileSize( replyContentRange.totalLength );
                    // we learned the file size. To allow normal segment use 
                    // interrupt the download!
                    stopDownload();
                    throw new ReconnectException();
                }
            }
            
            // connection successfully finished
            NLogger.debug(NLoggerNames.Download_Engine,
                "HTTP Handshake successfull.");
            return;
        }
        // check error type
        else if ( httpCode == 503 )
        {// 503 -> host is busy (this can also be returned when remotly queued)
            header = response.getHeader( GnutellaHeaderNames.X_QUEUE );
            XQueueParameters xQueueParameters = null;
            if ( header != null )
            {
                xQueueParameters = XQueueParameters.parseHTTPRangeSet( header.getValue() );
            }
            // check for persistent connection (gtk-gnutella uses queuing with 'Connection: close')
            if ( xQueueParameters != null && isKeepAliveSupported )
            {
                throw new RemotelyQueuedException( xQueueParameters );
            }
            else
            {
                header = response.getHeader( HTTPHeaderNames.RETRY_AFTER );
                if ( header != null )
                {
                    int delta = HTTPRetryAfter.parseDeltaInSeconds( header );
                    if ( delta > 0 )
                    {
                        throw new HostBusyException( delta );
                    }
                }
                throw new HostBusyException();
            }
        }
        else if ( httpCode == HTTPCodes.HTTP_401_UNAUTHORIZED 
               || httpCode == HTTPCodes.HTTP_403_FORBIDDEN )
        {
            if ( "Network Disabled".equals( response.getStatusReason() ) )
            {
                if ( candidate.isG2FeatureAdded() )
                {
                    // already tried G2 but no success.. better give up and dont hammer..
                    throw new UnusableHostException( "Request Forbidden" );
                }
                else
                {
                    // we have not tried G2 but we could..
                    candidate.setG2FeatureAdded( true );
                    throw new HostBusyException( 60 * 5 );
                }
            }
            else
            {
                throw new UnusableHostException( "Request Forbidden" );
            }
        }
        else if ( httpCode == 408 )
        {
            // 408 -> Time out. Try later?
            throw new HostBusyException();
        }
        else if ( httpCode == 404 || httpCode == 410 )
        {// 404: File not found / 410: Host not sharing
            throw new FileNotAvailableException();
        }
        else if ( httpCode == 416 )
        {// 416: Requested Range Unavailable
            header = response.getHeader( HTTPHeaderNames.RETRY_AFTER );
            if ( header != null )
            {
                int delta = HTTPRetryAfter.parseDeltaInSeconds( header );
                if ( delta > 0 )
                {
                    throw new RangeUnavailableException( delta );
                }
            }
            throw new RangeUnavailableException();
        }
        else
        {
            throw new IOException( "Unknown HTTP code: " + httpCode );
        }
    }

    private void buildAltLocRequestHeader(HTTPRequest request)
    {
        URN downloadFileURN = downloadFile.getFileURN();
        if ( downloadFileURN == null )
        {
            return;
        }
        
        // add good alt loc http header
        
        AlternateLocationContainer altLocContainer = new AlternateLocationContainer(
            downloadFileURN );
        // downloadFile.getGoodAltLocContainer() always returns a alt-loc container
        // when downloadFileURN != null
        altLocContainer.addContainer( downloadFile.getGoodAltLocContainer() );

        // create a temp copy of the container and add local alt location
        // if partial file sharing is active and we are not covered by a firewall
        if ( ServiceManager.sCfg.arePartialFilesShared &&
             NetworkManager.getInstance().hasConnectedIncoming() )
        {
            // add the local peer to the alt loc on creation, but only if its
            // not a private IP
            DestAddress ha = NetworkManager.getInstance().getLocalAddress();
            IpAddress myIp = ha.getIpAddress();
            if ( myIp == null || !myIp.isSiteLocalIP() )
            {
                AlternateLocation newAltLoc = new AlternateLocation( ha,
                    downloadFileURN );
                altLocContainer.addAlternateLocation( newAltLoc );
            }
        }
        
        HTTPHeader header = altLocContainer.getAltLocHTTPHeaderForAddress(
            GnutellaHeaderNames.X_ALT, candidate.getHostAddress(),
            candidate.getSendAltLocsSet() );
        if ( header != null )
        {
            request.addHeader( header );
        }
        
        // add bad alt loc http header
        
        // downloadFile.getBadAltLocContainer() always returns a alt-loc container
        // when downloadFileURN != null
        altLocContainer = downloadFile.getBadAltLocContainer();
        header = altLocContainer.getAltLocHTTPHeaderForAddress(
            GnutellaHeaderNames.X_NALT, candidate.getHostAddress(),
            candidate.getSendAltLocsSet() );
        if ( header != null )
        {
            request.addHeader( header );
        }
        
    }

    public void startDownload( ) throws IOException
    {
        String snapshotOfSegment;
        NLogger.debug(NLoggerNames.Download_Engine,
            "Download Engine starts download.");
        
        DirectByteBuffer directByteBuffer = null;
        LengthLimitedInputStream downloadStream = null;
        try
        {
            ManagedFile destFile = downloadFile.getIncompleteDownloadFile();
            directByteBuffer = DirectByteBufferProvider.requestBuffer(
                DirectByteBufferProvider.BUFFER_SIZE_64K );
            
            segment.downloadStartNotify();
            snapshotOfSegment = segment.toString();
            
            // determine the length to download, we start with the MAX
            // which would cause a download until the stream ends.
            long downloadLengthLeft = Long.MAX_VALUE;
            // maybe we know the file size
            if ( replyContentRange != null && replyContentRange.totalLength != -1 )
            {
                downloadLengthLeft = replyContentRange.totalLength;
            }
            // maybe we know a reply content length
            if ( replyContentLength != -1 )
            {
                downloadLengthLeft = Math.min( replyContentLength, downloadLengthLeft );
            }
            // maybe the segment has a smaler length (usually not the case)
            long segmentDataSizeLeft = segment.getTransferDataSizeLeft();
            if ( segmentDataSizeLeft != -1 )
            {
                downloadLengthLeft = Math.min( segmentDataSizeLeft, downloadLengthLeft );
            }
            
            downloadStream = new LengthLimitedInputStream( 
                inStream, downloadLengthLeft );
            long fileOffset = segment.getStart() + segment.getTransferredDataSize();
            long lengthDownloaded = segment.getTransferredDataSize();
            int len;
            byte[] buffer = new byte[BUFFER_LENGTH];
            while( (len = downloadStream.read( buffer, 0, BUFFER_LENGTH )) > 0 )
            {
                synchronized (segment)
                {
                    long tmpCheckLength = lengthDownloaded + len;
                    if ( tmpCheckLength < segment.getTransferredDataSize() )
                    {
                        NLogger.error(NLoggerNames.Download_Engine, 
                            "TransferredDataSize would be going down! " + 
                            " ll " + downloadLengthLeft + " l " + len + " ld "
                            + lengthDownloaded + " gtds "
                            + segment.getTransferredDataSize()
                            + " seg: " + segment
                            + " originally: " + snapshotOfSegment);
                        throw new IOException( "TransferredDataSize would be going down!" );
                    }
                    else if (segment.getTransferDataSize() > -1 
                        && tmpCheckLength > segment.getTransferDataSize())
                    {
                        NLogger.error(NLoggerNames.Download_Engine, 
                            "TransferredDataSize would be larger then segment! " +
                            " ll " + downloadLengthLeft + " l " + len + " ld "
                            + lengthDownloaded + " gtds "
                            + segment.getTransferredDataSize()
                            + " seg: " + segment
                            + " originally: " + snapshotOfSegment);
                        throw new IOException( "TransferredDataSize would be larger then segment!" );
                    }
                    
                    directByteBuffer.put( buffer, 0, len );
                    directByteBuffer.flip();
                    destFile.write( directByteBuffer, fileOffset );
                    fileOffset += directByteBuffer.limit();
                    directByteBuffer.clear();

                    lengthDownloaded += len;
                    segment.setTransferredDataSize( lengthDownloaded );
                    candidate.incTotalDownloadSize( len );

                    // get transfer size since it might have changed in the meantime.
                    segmentDataSizeLeft = segment.getTransferDataSizeLeft();
                    if ( segmentDataSizeLeft != -1 )
                    {
                        downloadLengthLeft = Math.min( segmentDataSizeLeft, downloadLengthLeft );
                        downloadStream.setLengthLimit(downloadLengthLeft);
                    }
                }
            }
            isDownloadSuccessful = true;
            // if we successful downloaded and we still dont know the total file size,
            // we can assume that the file was completly downloaded.
            if ( downloadFile.getTotalDataSize() == SWDownloadConstants.UNKNOWN_FILE_SIZE )
            {
                // we have a file with an unknown data size. For aditional check assert
                // certain parameters
                assert( segment.getTotalDataSize() == -1 );
                downloadFile.setFileSize( segment.getTransferredDataSize() );
            }
        }
        catch ( FileHandlingException exp )
        {
            NLogger.error(NLoggerNames.Download_Engine, exp, exp);
            IOException ioExp = new IOException( exp.getMessage() );
            ioExp.initCause(exp);
            throw ioExp;
        }
        catch ( ManagedFileException exp )
        {
            if ( Thread.currentThread().isInterrupted() )
            {
                return;
            }
            NLogger.error(NLoggerNames.Download_Engine, exp, exp);
            IOException ioExp = new IOException( exp.getMessage() );
            ioExp.initCause(exp);
            throw ioExp;
        }
        finally
        {// dont close managed file since it might be used by parallel threads.
            if ( directByteBuffer != null )
            {
                directByteBuffer.release();
            }
            
            boolean isAcceptingNextSegment = isAcceptingNextSegment();
            candidate.addToCandidateLog( "Is accepting next segment: " + isAcceptingNextSegment );
            // this is for keep alive support...
            if ( isAcceptingNextSegment )
            {
                // only need to close and consume left overs if we plan to
                // continue using this connection.
                downloadStream.close();
            }
            else
            {
                stopDownload();
            }
        }
    }

    public void stopDownload()
    {
        NLogger.debug(NLoggerNames.Download_Engine,
            "Closing pipe and socket and telling segment we've stopped.");
        isDownloadStopped = true;
        candidate.addToCandidateLog( "Stop download." );
        IOUtil.closeQuietly(inStream);

        if ( segment != null )
        {
            segment.downloadStopNotify();
        }
        // dont close managed file since it might be used by parallel threads.
        if ( connection != null )
        {
            connection.disconnect();
        }
        IOUtil.closeQuietly(socket);
    }

    /**
     * Indicates whether the connection is keept alive and the next http request
     * can be send.
     * @return true if the next http request can be send.
     */
    public boolean isAcceptingNextSegment()
    {
        return isDownloadSuccessful && isKeepAliveSupported && replyContentLength != -1;
    }

    /**
     * We only care for the start offset since this is the importent point to
     * begin the download from. Wherever it ends we try to download as long as we
     * stay connected or until we reach our goal.
     *
     * Possible Content-Range Headers ( maybe not complete / header is upper
     * cased by Phex )
     *
     *   Content-range:bytes abc-def/xyz
     *   Content-range:bytes abc-def/*
     *   Content-range:bytes *\/xyz
     *   Content-range: bytes=abc-def/xyz (wrong but older Phex version and old clients use this)
     *
     * @param contentRangeLine the content range value
     * @throws WrongHTTPHeaderException if the content range line has wrong format.
     * @return the content range start offset.
     */
    private ContentRange parseContentRange( String contentRangeLine )
        throws WrongHTTPHeaderException
    {
        try
        {
            ContentRange range = new ContentRange();
            contentRangeLine = contentRangeLine.toLowerCase();
            // skip over bytes plus extra char
            int idx = contentRangeLine.indexOf( "bytes" ) + 6;
            String rangeStr = contentRangeLine.substring( idx ).trim();
            
            int slashIdx = rangeStr.indexOf('/');
            String leadingPart = rangeStr.substring(0, slashIdx);
            String trailingPart = rangeStr.substring( slashIdx + 1 );
            
            // ?????/*
            if ( trailingPart.charAt(0) == '*' )
            {
                range.totalLength = -1;
            }
            else // ?????/789
            {
                long fileLength = Long.parseLong(trailingPart);
                range.totalLength = fileLength;
            }
            
            // */???
            if ( leadingPart.charAt( 0 ) == '*' )
            {
                // startPos of -1 indicates '*' (free to choose)
                range.startPos = -1;
                range.endPos = range.totalLength;
            }
            else
            {
                // 123-456/???
                int dashIdx = rangeStr.indexOf( '-' );
                String startOffsetStr = leadingPart.substring( 0, dashIdx );
                long startOffset = Long.parseLong( startOffsetStr );
                String endOffsetStr = leadingPart.substring( dashIdx+1 );
                long endOffset = Long.parseLong( endOffsetStr );
                range.startPos = startOffset;
                range.endPos = endOffset;
            }
            return range;
        }
        catch ( NumberFormatException exp )
        {
            NLogger.warn(NLoggerNames.Download_Engine, exp, exp);
            throw new WrongHTTPHeaderException(
                "Number error while parsing content range: " + contentRangeLine );
        }
        catch ( IndexOutOfBoundsException exp )
        {
            throw new WrongHTTPHeaderException(
                "Error while parsing content range: " + contentRangeLine );
        }
    }
    
    private void updateKeepAliveSupport( HTTPResponse response )
    {
        // check if Keep-Alive connection is accepted
        HTTPHeader header = response.getHeader( HTTPHeaderNames.CONNECTION );
        if ( header != null )
        {
            if ( header.getValue().equalsIgnoreCase( "close" ) )
            {
                isKeepAliveSupported = false;
                return;
            }
            else if ( header.getValue().equalsIgnoreCase( "keep-alive" ) )
            {
                isKeepAliveSupported = true;
                return;
            }
        }
        // missing or unknown connection header do the HTTP method default.
        if ( response.getHTTPVersion().equals("HTTP/1.1") )
        {
            isKeepAliveSupported = true;
        }
        else
        {
            isKeepAliveSupported = false;
        }
    }
    
    public void handlePushProxyHeaders( HTTPHeader[] headers )
    {
        if ( headers == null || headers.length == 0 )
        {
            return;
        }
        List proxyList = new ArrayList();
        StringTokenizer tokenizer;
        for ( int i = 0; i < headers.length; i++ )
        {
            HTTPHeader header = headers[i];
            tokenizer = new StringTokenizer( header.getValue(), ",");
            while( tokenizer.hasMoreTokens() )
            {
                String addressStr = tokenizer.nextToken().trim();
                DestAddress address;
                try
                {
                    address = AddressUtils.parseAndValidateAddress( 
                        addressStr, false );
                    proxyList.add( address );
                }
                catch (MalformedDestAddressException exp)
                {
                    NLogger.debug(NLoggerNames.Download_Engine,
                        "Malformed alt-location URL: " + exp.getMessage() );
                }
            }
        }
        if ( proxyList.size() == 0 )
        {
            return;
        }
        DestAddress[] pushProxyAddresses = new DestAddress[ proxyList.size() ];
        proxyList.toArray( pushProxyAddresses );
        candidate.setPushProxyAddresses( pushProxyAddresses );
    }
    
    private class ContentRange
    {
        /**
         * startPos of -1 indicates '*' (free to choose)
         */
        long startPos;
        long endPos;
        long totalLength;
    }

}
