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
 *  $Id: UpdateCheckRunner.java,v 1.16 2005/10/03 00:18:29 gregork Exp $
 */
package phex.update;

import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang.SystemUtils;

import phex.common.*;
import phex.event.UpdateNotificationListener;
import phex.gui.common.GUIRegistry;
import phex.share.ShareManager;
import phex.share.SharedFilesService;
import phex.statistic.StatisticProvider;
import phex.statistic.StatisticProviderConstants;
import phex.statistic.StatisticsManager;
import phex.utils.*;
import phex.xml.ObjectFactory;
import phex.xml.XJBPhex;
import phex.xml.XJBUpdateRequest;
import phex.xml.XJBUpdateResponse;
import phex.xml.XMLBuilder;

/**
 * 
 * @author gkoukkoullis
 */
public class UpdateCheckRunner implements Runnable
{
    private static final String UPDATE_CHECK_URL = "http://phex.kouk.de/update/update.php";
    
    
    private Throwable updateCheckError;
    private UpdateNotificationListener listener;
    private String releaseVersion;
    private String betaVersion;
    
    public UpdateCheckRunner( UpdateNotificationListener updateListener )
    {
        listener = updateListener;
    }
    
    public String getReleaseVersion()
    {
        return releaseVersion;
    }

    public String getBetaVersion()
    {
        return betaVersion;
    }

    /**
     * Returns a possible Throwable that could be thrown during the update check
     * or null if no error was caught.
     * @return a possible Throwable that could be thrown during the update check
     * or null if no error was caught.
     */
    public Throwable getUpdateCheckError()
    {
        return updateCheckError;
    }
    
    /**
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
        try
        {            
            performUpdateCheck();
        }
        catch ( Throwable exp )
        {
            updateCheckError = exp;
            NLogger.warn(NLoggerNames.GLOBAL, exp, exp );
        }
    }
    
    private void performUpdateCheck()
    {
        URL url;
        XJBPhex xjbPhex;
        try
        {
            url = new URL( UPDATE_CHECK_URL );
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setUseCaches( false );
            connection.setRequestProperty( "User-Agent", Environment.getPhexVendor() );
            connection.setRequestMethod( "POST" );
            connection.setDoOutput( true );
            connection.setRequestProperty( "Content-Type",
                "text/xml; charset=UTF-8" );
                        
            OutputStream outStream = connection.getOutputStream();
            byte[] data = buildXMLUpdateRequest();
            if ( data == null )
            {
                throw new IOException( "Missing XML update data" );
            }
            outStream.write( data );
            if ( NLogger.isDebugEnabled( NLoggerNames.UpdateCheck_Request ) )
            {
                NLogger.debug(NLoggerNames.UpdateCheck_Request, new String(data) );
            }
            
            // dont need to buffer stream already done by Properties.load()
            InputStream inStream = connection.getInputStream();
            if ( NLogger.isDebugEnabled( NLoggerNames.UpdateCheck_Response ) )
            {
                byte[] respData = IOUtil.toByteArray( inStream );
                NLogger.debug(NLoggerNames.UpdateCheck_Response, new String(respData) );
                inStream = new ByteArrayInputStream( respData );
            }
            xjbPhex = XMLBuilder.readXJBPhexFromStream( inStream );
        }
        catch ( MalformedURLException exp )
        {
            updateCheckError = exp;
            NLogger.warn(NLoggerNames.GLOBAL, exp, exp );
            assert false;
            throw new RuntimeException( );
        }
        catch ( UnknownHostException exp )
        {
            // can't find way to host
            // this maybe means we have no internet connection
            updateCheckError = exp;
            NLogger.warn(NLoggerNames.GLOBAL, exp, exp );
            return;
        }
        catch ( SocketException exp )
        {
            // can't connect... maybe a proxy is in the way...
            updateCheckError = exp;
            NLogger.warn(NLoggerNames.GLOBAL, exp, exp );
            return;
        }
        catch ( IOException exp )
        {
            updateCheckError = exp;
            NLogger.warn(NLoggerNames.GLOBAL, exp, exp );
            return;
        }
        catch ( JAXBException exp )
        {
            updateCheckError = exp;
            NLogger.warn(NLoggerNames.GLOBAL, exp, exp );
            return;
        }

        ServiceManager.sCfg.lastUpdateCheckTime = System.currentTimeMillis();
        
        XJBUpdateResponse response = xjbPhex.getUpdateResponse();
        List versionList = response.getVersionList();
        Iterator versionIterator = versionList.iterator();
        XJBUpdateResponse.VersionType latestReleaseVersion = null;
        XJBUpdateResponse.VersionType latestBetaVersion = null;
        
        while ( versionIterator.hasNext() )
        {
            XJBUpdateResponse.VersionType currentVersion =
                (XJBUpdateResponse.VersionType)versionIterator.next();
            if ( currentVersion.isBeta() )
            {
                if ( latestBetaVersion == null || VersionUtils.compare(
                    currentVersion.getId(), latestBetaVersion.getId() ) > 0 )
                {
                    latestBetaVersion = currentVersion;
                }
            }
            else
            {
                if ( latestReleaseVersion == null || VersionUtils.compare(
                    currentVersion.getId(), latestReleaseVersion.getId() ) > 0 )
                {
                    latestReleaseVersion = currentVersion;
                }
            }
        }
        
        
        betaVersion = "0";
        releaseVersion = "0";
        if ( latestBetaVersion != null )
        {
            betaVersion = latestBetaVersion.getId();
        }
        if ( latestReleaseVersion != null )
        {
            releaseVersion = latestReleaseVersion.getId();
        }
        
        int releaseCompare = 0;
        int betaCompare = 0;
        betaCompare = VersionUtils.compare( betaVersion,
            VersionUtils.getFullProgramVersion() );
        releaseCompare = VersionUtils.compare( releaseVersion,
            VersionUtils.getFullProgramVersion() );
        
        if ( releaseCompare <= 0 && betaCompare <= 0 )
        {
            ServiceManager.sCfg.save();
            return;
        }
        
        betaCompare = VersionUtils.compare( betaVersion,
            ServiceManager.sCfg.lastBetaUpdateCheckVersion );        
        releaseCompare = VersionUtils.compare( releaseVersion,
            ServiceManager.sCfg.lastUpdateCheckVersion );

        int verDiff = VersionUtils.compare( betaVersion,
            releaseVersion );

        boolean triggerUpdateNotification = false;
        if ( releaseCompare > 0 )
        {
            ServiceManager.sCfg.lastUpdateCheckVersion = releaseVersion;
            triggerUpdateNotification = true;
        }
        if ( betaCompare > 0 )
        {
            ServiceManager.sCfg.lastBetaUpdateCheckVersion = betaVersion;
            triggerUpdateNotification = true;
        }

        if ( verDiff > 0 )
        {
            // reset release version since beta is more up-to-date
            releaseVersion = null;
        }
        else
        {
            // reset beta version since release is the one to go.
            betaVersion = null;
        }

        ServiceManager.sCfg.save();
        if ( triggerUpdateNotification )
        {
            fireUpdateNotification();
        }
    }
    
    private void fireUpdateNotification()
    {
        listener.updateNotification( this );
    }
    
    private byte[] buildXMLUpdateRequest()
    {
        try
        {
            Cfg cfg = ServiceManager.sCfg;
            ObjectFactory objFactory = new ObjectFactory();
            XJBPhex xjbPhex = objFactory.createPhexElement();
            XJBUpdateRequest xjbRequest = objFactory.createXJBUpdateRequest();
            xjbPhex.setUpdateRequest( xjbRequest );
            
            xjbRequest.setCurrentVersion( VersionUtils.getFullProgramVersion() );
            xjbRequest.setStartupCount( cfg.totalStartupCounter );
            xjbRequest.setLafUsed( GUIRegistry.getInstance().getUsedLAFClass() );
            xjbRequest.setJavaVersion( System.getProperty( "java.version" ) );
            xjbRequest.setOperatingSystem( SystemUtils.OS_NAME );
            
            xjbRequest.setHostid( cfg.mProgramClientID.toHexString() );
            xjbRequest.setShowBetaInfo( cfg.showBetaUpdateNotification );
            xjbRequest.setLastInfoId( cfg.lastShownUpdateInfoId );
            
            String lastCheckVersion;
            if ( VersionUtils.compare( cfg.lastUpdateCheckVersion,
                 cfg.lastBetaUpdateCheckVersion ) > 0 )
            {
                lastCheckVersion = cfg.lastUpdateCheckVersion;
            }
            else
            {
                lastCheckVersion = cfg.lastBetaUpdateCheckVersion;
            }
            xjbRequest.setLastCheckVersion( lastCheckVersion );
            
            StatisticsManager statMgr = StatisticsManager.getInstance();
            
            StatisticProvider uptimeProvider = statMgr.getStatisticProvider(
                StatisticsManager.UPTIME_PROVIDER );
            xjbRequest.setAvgUptime(
                ((LongObj)uptimeProvider.getAverageValue()).value );
            
            StatisticProvider dailyUptimeProvider = statMgr.getStatisticProvider(
                StatisticsManager.DAILY_UPTIME_PROVIDER );
            xjbRequest.setDailyAvgUptime(
                ((IntObj)dailyUptimeProvider.getValue()).value );
                
            StatisticProvider downloadProvider = statMgr.getStatisticProvider(
                StatisticProviderConstants.TOTAL_DOWNLOAD_COUNT_PROVIDER );
            xjbRequest.setDownloadCount(
                (int)((LongObj)downloadProvider.getValue()).value );
                
            StatisticProvider uploadProvider = statMgr.getStatisticProvider(
                StatisticProviderConstants.TOTAL_UPLOAD_COUNT_PROVIDER );
            xjbRequest.setUploadCount(
                (int)((LongObj)uploadProvider.getValue()).value );
                
            SharedFilesService sharedFilesService = ShareManager.getInstance().getSharedFilesService();
            xjbRequest.setSharedFiles( sharedFilesService.getFileCount() );
            xjbRequest.setSharedSize( sharedFilesService.getTotalFileSizeInKb() );
            
            xjbRequest.setErrorLog( getErrorLogFileTail() );
            
            return XMLBuilder.serializeToBytes( xjbPhex );
        }
        catch ( JAXBException exp )
        {
            NLogger.error(NLoggerNames.GLOBAL, exp, exp );
            return null;
        }
    }
    
    private String getErrorLogFileTail()
    {
        try
        {
            File logFile = Environment.getInstance().getPhexConfigFile( "phex.error.log" );
            if ( !logFile.exists() )
            {
                return null;
            }
            RandomAccessFile raf = new RandomAccessFile( logFile, "r" );
            long pos = Math.max( raf.length() - 10 * 1024, 0 );
            raf.seek(pos);
            byte[] buffer = new byte[10*1024];
            int lenRead = raf.read( buffer );
            return new String( buffer, 0, lenRead );
        }
        catch ( IOException exp )
        {
            NLogger.error(NLoggerNames.GLOBAL, exp, exp );
            return exp.toString();
        }
    }
}
