/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2005 Phex Development Group
 *  Copyright (C) 2000 William W. Wong williamw@jps.net
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
 *  $Id: ShareManager.java,v 1.87 2005/11/19 14:39:35 gregork Exp $
 */
package phex.share;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import phex.common.Environment;
import phex.common.Manager;
import phex.common.ServiceManager;
import phex.common.URN;
import phex.common.address.DestAddress;
import phex.common.format.NumberFormatUtils;
import phex.connection.NetworkManager;
import phex.http.HTTPHeader;
import phex.http.HTTPHeaderGroup;
import phex.http.HTTPHeaderNames;
import phex.http.HTTPRequest;
import phex.msg.GUID;
import phex.msg.MsgHeader;
import phex.msg.QueryMsg;
import phex.msg.QueryResponseMsg;
import phex.msg.QueryResponseRecord;
import phex.net.connection.Connection;
import phex.share.export.ExportEngine;
import phex.statistic.MessageCountStatistic;
import phex.upload.UploadManager;
import phex.utils.*;

public class ShareManager implements Manager
{
    private static final ShareFile[] EMPTY_SEARCH_RESULT = new ShareFile[0];

    private static final String INDEX_QUERY_STRING = "    ";
    
    private SharedFilesService sharedFilesService;

    private static ShareManager instance;

    private ShareManager()
    {

    }

    public static ShareManager getInstance()
    {
        if ( instance == null )
        {
            instance = new ShareManager();
        }
        return instance;
    }

    /**
     * This method is called in order to initialize the manager. This method
     * includes all tasks that must be done to intialize all the several manager.
     * Like instantiating the singleton instance of the manager. Inside
     * this method you can't rely on the availability of other managers.
     * @return true is initialization was successful, false otherwise.
     */
    public boolean initialize()
    {
        sharedFilesService = new SharedFilesService();
        return true;
    }

    /**
     * This method is called in order to perform post initialization of the
     * manager. This method includes all tasks that must be done after initializing
     * all the several managers. Inside this method you can rely on the
     * availability of other managers.
     * @return true is initialization was successful, false otherwise.
     */
    public boolean onPostInitialization()
    {
        FileRescanRunner.rescan( true, false );
        return true;
    }

    /**
     * This method is called after the complete application including GUI completed
     * its startup process. This notification must be used to activate runtime
     * processes that needs to be performed once the application has successfully
     * completed startup.
     */
    public void startupCompletedNotify()
    {
    }

    /**
     * This method is called in order to cleanly shutdown the manager. It
     * should contain all cleanup operations to ensure a nice shutdown of Phex.
     */
    public void shutdown()
    {
        getSharedFilesService().triggerSaveSharedFiles();
    }
    
    public SharedFilesService getSharedFilesService()
    {
        return sharedFilesService;
    }

    /**
     * Search the sharefile database and get groups of sharefiles.
     * It returns empty results if the query source and the local host
     * are both firewalled. 
     * @param queryMsg the query
     * @return the found results.
     */
    public ShareFile[] handleQuery(QueryMsg queryMsg)
    {
        // Perform search on my list.

        // If the query source and the local host are both firewalled, return no results
        // as per http://groups.yahoo.com/group/the_gdf/files/Proposals/MinSpeed.html
        if (queryMsg.isRequesterFirewalled()
            && !NetworkManager.getInstance().hasConnectedIncoming())
        {
            return EMPTY_SEARCH_RESULT;
        }
        // if all upload slots are filled dont return any search results.
        // This holds away unnecessary connection attempts.
        if ( UploadManager.getInstance().isHostBusy() )
        {
            return EMPTY_SEARCH_RESULT;
        }

        String searchStr = queryMsg.getSearchString();
        if ( searchStr.equals( INDEX_QUERY_STRING ) )
        {
            Logger.logMessage( Logger.FINER, Logger.NETWORK,
                "Index query detected." );
            return sharedFilesService.getSharedFiles();
        }

        // first check for URN query...
        List foundFiles;
        URN[] urns = queryMsg.getQueryURNs();
        if ( urns.length > 0 )
        {
            foundFiles = sharedFilesService.getFilesByURNs( urns );
            if ( foundFiles.size() == urns.length )
            {// we found all requested files by URN.
                // return results and be happy that we are already finished.
                return provideResultData( foundFiles );
            }
        }
        else
        {
            foundFiles = Collections.EMPTY_LIST;
        }

        // if there are no urns or not all urns have a hit check for string query...
        StringTokenizer tokenizer = new StringTokenizer( searchStr );
        ArrayList tokenList = new ArrayList( Math.min( 10,
            tokenizer.countTokens() ) );
        String term;
        // Build search term, max up to 10 terms.
        while (tokenList.size() < 10 && tokenizer.hasMoreElements())
        {
            term = tokenizer.nextToken();
            // ignore terms with less then 2 char length
            if ( term.length() >= 2 )
            {
                tokenList.add( term.toLowerCase() );
            }
        }

        if ( tokenList.size() == 0 )
        {// no string search to do
            return provideResultData( foundFiles );
        }

        ShareFile[] shareFiles = sharedFilesService.getSharedFiles();
        SearchEngine searchEngine = new SearchEngine();

        // searches through the files for each search term. Drops all files that
        // dont match a search term from the possible result list.

        //long start1 = System.currentTimeMillis();
        // all files are possible results
        ArrayList leftFiles = new ArrayList( Arrays.asList( shareFiles ) );

        // go through each term
        for (int i = 0; i < tokenList.size() && leftFiles.size() > 0; i++)
        {
            searchEngine.setPattern( (String) tokenList.get( i ), true );

            // go through each left file in the files array
            for (int j = leftFiles.size() - 1; j >= 0; j--)
            {
                ShareFile shareFile = (ShareFile) leftFiles.get( j );
                // dont share files without calculated urn..
                if ( shareFile.getURN() == null )
                {
                    leftFiles.remove( j );
                    continue;
                }
                searchEngine.setText( shareFile.getSearchCompareTerm(), true );
                if ( !searchEngine.match() )
                {
                    // a term dosn't match remove possible result.
                    leftFiles.remove( j );
                }
            }
        }

        // merge results with urn query results...
        // append all files found by urn to list.
        if ( foundFiles.size() > 0 )
        {
            leftFiles.addAll( foundFiles );
        }

        return provideResultData( leftFiles );
    }

    private ShareFile[] provideResultData(List resultFileList)
    {
        int resultCount = resultFileList.size();
        if ( resultCount == 0 ) { return EMPTY_SEARCH_RESULT; }
        // verify max return data size..
        if ( resultCount > ServiceManager.sCfg.mUploadMaxSearch )
        {
            int toIndex = resultCount - ServiceManager.sCfg.mUploadMaxSearch;
            // limit list to max contain number of files.
            // remove from begining of list. the end of list contains the
            // urn results. We like to keep those
            resultFileList.subList( 0, toIndex ).clear();
            resultCount = resultFileList.size();
        }

        ShareFile[] resultFiles = new ShareFile[resultCount];
        resultFiles = (ShareFile[]) resultFileList.toArray( resultFiles );

        // increment search count for files in list
        for (int i = 0; i < resultCount; i++)
        {
            ShareFile shareFile = resultFiles[i];
            shareFile.incSearchCount();
        }
        return resultFiles;
    }

    // Called by ReadWorker to handle a HTTP GET request from the remote host.
    public void httpRequestHandler(Connection connection,
        HTTPRequest httpRequest)
    {
        // GET / HTTP/1.1 (Browse Host request)
        if ( httpRequest.getRequestMethod().equals( "GET" ) &&
             httpRequest.getRequestURI().equals( "/" ) )
        {
            // The remote host just want the index.html.
            // Return a list of shared files.
            try
            {
                sendFileListing( httpRequest, connection );
            }
            catch (IOException exp)
            {
                Logger.logMessage( Logger.FINER, Logger.UPLOAD, exp );
            }
            return;
        }
        sendString( connection, buildErrorHTTP( "404 Not Found",
            "File not found." ) );
    }

    private void sendString(Connection connection, String html)
    {
        try
        {
            // Prepare HTTP response
            byte[] outbuf = new byte[html.length()];
            int lenToSend = IOUtil.serializeString( html, outbuf, 0 );
            int lenSent = 0;
            int len;
            // Write HTTP response
            while (lenSent < lenToSend)
            {
                len = lenToSend - lenSent;

                if ( len > 1024 )
                {
                    len = 1024;
                }
                connection.write( outbuf, lenSent, len );
                lenSent += len;
            }
            connection.flush();

            // Wait a bit before closing the connection.
            // Somehow the remote gnutella won't read the last
            // buffer if closing the connection too soon.
            Thread.sleep( 2000 );
            connection.disconnect();
        }
        catch (Exception exp)
        {
            NLogger.error(NLoggerNames.GLOBAL, 
                "Exception whily trying to send sting: '" + html + "'",
                exp );
        }
    }

    private String buildErrorHTTP(String statusStr, String errMsg)
    {
        StringBuffer content = new StringBuffer( 300 );
        content.append( "<html><head><title>PHEX</title></head><body>" );
        content.append( errMsg );
        content.append( "<hr>Visit the Phex website at " );
        content.append( "<a href=\"http://phex.sourceforge.net\">http://phex.sourceforge.net</a>." );
        content.append( "</body>" );
        content.append( "</html>" );

        StringBuffer buf = new StringBuffer( 300 );
        buf.append( "HTTP/1.1 " ).append( statusStr ).append( HTTPRequest.CRLF );
        buf.append( "Server: " ).append( Environment.getPhexVendor() ).append( HTTPRequest.CRLF );
        buf.append( "Connection: close" ).append( HTTPRequest.CRLF );
        buf.append( "Content-Type: text/plain" ).append( HTTPRequest.CRLF );
        buf.append( "Content-Length: " ).append( content.length() ).append( HTTPRequest.CRLF );
        buf.append( "\r\n" );
        buf.append( content.toString() );
        return buf.toString();
    }

    private void sendFileListing(HTTPRequest httpRequest, Connection connection)
        throws IOException
    {
        GnutellaOutputStream outStream = connection.getOutputStream();
        
        if ( !ServiceManager.sCfg.mShareBrowseDir )
        {
            HTTPHeaderGroup headers = HTTPHeaderGroup.createDefaultResponseHeaders();
            String response = createHTTPResponse( "403 Browsing disabled", headers );
            connection.write( response.getBytes() );
            connection.flush();
            connection.disconnect();
            return;
        }
        
        HTTPHeader acceptHeader = httpRequest.getHeader( "Accept" );
        if ( acceptHeader == null )
        {
            HTTPHeaderGroup headers = HTTPHeaderGroup.createDefaultResponseHeaders();
            String response = createHTTPResponse( "406 Not Acceptable", headers );
            connection.write( response.getBytes() );
            connection.flush();
            connection.disconnect();
            return;
        }
        String acceptHeaderStr = acceptHeader.getValue();
        if ( acceptHeaderStr.indexOf( "application/x-gnutella-packets" ) != -1 )
        {// return file listing via gnutella packages...
            HTTPHeaderGroup headers = HTTPHeaderGroup.createDefaultResponseHeaders();
            headers.addHeader( new HTTPHeader( HTTPHeaderNames.CONTENT_TYPE,
                "application/x-gnutella-packets" ) );
            headers.addHeader( new HTTPHeader( HTTPHeaderNames.CONNECTION,
                "close" ) );
            String response = createHTTPResponse( "200 OK", headers );
            connection.write( response.getBytes() );
            connection.flush();

            // now send QueryReplys...
            ShareFile[] shareFiles = sharedFilesService.getSharedFiles();

            MsgHeader header = new MsgHeader( new GUID(),
                MsgHeader.QUERY_HIT_PAYLOAD, (byte) 2, (byte) 0, -1 );

            QueryResponseRecord record;
            ShareFile sfile;
            int sendCount = 0;
            while (sendCount < shareFiles.length)
            {
                int currentSendCount = Math.min( 255, shareFiles.length
                    - sendCount );
                QueryResponseRecord[] records = new QueryResponseRecord[currentSendCount];
                for (int i = 0; i < currentSendCount; i++)
                {
                    sfile = shareFiles[sendCount + i];
                    record = new QueryResponseRecord( sfile.getFileIndex(),
                        sfile.getURN(), (int) sfile.getFileSize(),
                        sfile.getFileName() );
                    records[i] = record;
                }

                DestAddress hostAddress = NetworkManager.getInstance()
                    .getLocalAddress();
                QueryResponseMsg queryResponse = new QueryResponseMsg( header,
                    ServiceManager.sCfg.mProgramClientID, hostAddress,
                    Math.round( ServiceManager.sCfg.mUploadMaxBandwidth
                        / NumberFormatUtils.ONE_KB * 8), records );

                // send msg over the wire 
                queryResponse.writeMessage( outStream );
                // and count message
                MessageCountStatistic.queryHitMsgOutCounter.increment( 1 );

                sendCount += currentSendCount;
            }
            connection.flush();
        }
        else if ( acceptHeaderStr.indexOf( "text/html" ) != -1
            || acceptHeaderStr.indexOf( "*/*" ) != -1 )
        {// return file listing via html page...
            HTTPHeaderGroup headers = HTTPHeaderGroup.createDefaultResponseHeaders();
            headers.addHeader( new HTTPHeader( HTTPHeaderNames.CONTENT_TYPE,
                "text/html; charset=iso-8859-1" ) );
            headers.addHeader( new HTTPHeader( HTTPHeaderNames.CONNECTION,
                "close" ) );
            String response = createHTTPResponse( "200 OK", headers );
            connection.write( response.getBytes() );
            connection.flush();

            // now send html
            exportSharedFiles( outStream );
            
            connection.flush();
        }
        // close connection as indicated in the header
        connection.disconnect();
    }

    private String createHTTPResponse(String code, HTTPHeaderGroup header)
    {
        StringBuffer buffer = new StringBuffer( 100 );
        buffer.append( "HTTP/1.1 " );
        buffer.append( code );
        buffer.append( "\r\n" );
        buffer.append( header.buildHTTPHeaderString() );
        buffer.append( "\r\n" );
        return buffer.toString();
    }
    
    /**
     * 
     */
    public void exportSharedFiles( OutputStream outStream )
    {
        InputStream inStream = ClassLoader.getSystemResourceAsStream(
            "phex/resources/defaultSharedFilesHTMLExport.xsl" );
            //"phex/resources/magmaSharedFilesYAMLExport.xsl" );
        exportSharedFiles( inStream, outStream, null );
    }

    /**
     * 
     */
    public void exportSharedFiles( InputStream styleSheetStream, OutputStream outStream,
        Map exportOptions )
    {
        ExportEngine engine = new ExportEngine();
        engine.setExportOptions(exportOptions);
        engine.startExport( styleSheetStream, outStream );
    }
}
