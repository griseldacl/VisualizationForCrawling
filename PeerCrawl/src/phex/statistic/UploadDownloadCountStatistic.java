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
 *  $Id: UploadDownloadCountStatistic.java,v 1.8 2005/10/03 00:18:29 gregork Exp $
 */
package phex.statistic;

import phex.common.*;
import phex.common.LongObj;


/**
 * This class is a simple container for the number of completed uploads and downloads.
 * 
 * @author Randy Schnedler
 *
 */
public class UploadDownloadCountStatistic extends SimpleStatisticProvider
    implements StatisticProviderConstants
{
    public static final SimpleStatisticProvider sessionUploadCount;
    public static final UploadDownloadCountStatistic totalUploadCount;
    public static final SimpleStatisticProvider sessionDownloadCount;
    public static final UploadDownloadCountStatistic totalDownloadCount;
    
    public static final SimpleStatisticProvider pushDownloadAttempts;
    public static final SimpleStatisticProvider pushDownloadSuccess;
    public static final SimpleStatisticProvider pushDownloadFailure;
    public static final SimpleStatisticProvider pushDldPushProxyAttempts;
    public static final SimpleStatisticProvider pushDldPushProxySuccess;
    
    public static final SimpleStatisticProvider pushUploadAttempts;
    public static final SimpleStatisticProvider pushUploadSuccess;
    public static final SimpleStatisticProvider pushUploadFailure;

    static
    {
        totalUploadCount = new UploadDownloadCountStatistic();
        totalUploadCount.setValue( ServiceManager.sCfg.totalUploadCount );
        sessionUploadCount = new ChainedSimpleStatisticProvider( 
            totalUploadCount );
        
        totalDownloadCount = new UploadDownloadCountStatistic();
        totalDownloadCount.setValue( ServiceManager.sCfg.totalDownloadCount );
        sessionDownloadCount = new ChainedSimpleStatisticProvider(
            totalDownloadCount );
        
        pushDownloadAttempts = new UploadDownloadCountStatistic();
        pushDownloadSuccess = new UploadDownloadCountStatistic();
        pushDownloadFailure = new UploadDownloadCountStatistic();
        
        pushDldPushProxyAttempts = new UploadDownloadCountStatistic();
        pushDldPushProxySuccess = new UploadDownloadCountStatistic();
        
        pushUploadAttempts = new UploadDownloadCountStatistic();
        pushUploadSuccess = new UploadDownloadCountStatistic();
        pushUploadFailure = new UploadDownloadCountStatistic();
        
        StatisticsManager.getInstance().registerStatisticProvider(
            SESSION_UPLOAD_COUNT_PROVIDER, sessionUploadCount );
        StatisticsManager.getInstance().registerStatisticProvider(
            TOTAL_UPLOAD_COUNT_PROVIDER, totalUploadCount );
        StatisticsManager.getInstance().registerStatisticProvider(
            SESSION_DOWNLOAD_COUNT_PROVIDER, sessionDownloadCount );
        StatisticsManager.getInstance().registerStatisticProvider(
            TOTAL_DOWNLOAD_COUNT_PROVIDER, totalDownloadCount );
        
        StatisticsManager.getInstance().registerStatisticProvider(
                PUSH_DOWNLOAD_ATTEMPTS_PROVIDER, pushDownloadAttempts );
        StatisticsManager.getInstance().registerStatisticProvider(
                PUSH_DOWNLOAD_SUCESS_PROVIDER, pushDownloadSuccess );
        StatisticsManager.getInstance().registerStatisticProvider(
                PUSH_DOWNLOAD_FAILURE_PROVIDER, pushDownloadFailure );
        StatisticsManager.getInstance().registerStatisticProvider(
            PUSH_DLDPUSHPROXY_ATTEMPTS_PROVIDER, pushDldPushProxyAttempts );
        StatisticsManager.getInstance().registerStatisticProvider(
            PUSH_DLDPUSHPROXY_SUCESS_PROVIDER, pushDldPushProxySuccess );
        
        StatisticsManager.getInstance().registerStatisticProvider(
                PUSH_UPLOAD_ATTEMPTS_PROVIDER, pushUploadAttempts );
        StatisticsManager.getInstance().registerStatisticProvider(
                PUSH_UPLOAD_SUCESS_PROVIDER, pushUploadSuccess );
        StatisticsManager.getInstance().registerStatisticProvider(
                PUSH_UPLOAD_FAILURE_PROVIDER, pushUploadFailure );
    }
    
    public static void saveStats()
    {
        ServiceManager.sCfg.totalDownloadCount =(int)((LongObj)totalDownloadCount.getValue()).value;
        ServiceManager.sCfg.totalUploadCount =(int)((LongObj)totalUploadCount.getValue()).value;
    }
    
    /**
     * Bad workaround to init this class
     * TODO3 we need to find a way to solve this static initializer problem in stats
     */
    public static void touch()
    {
    }
}