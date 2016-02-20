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
package phex.statistic;

import java.util.*;
import phex.common.*;
import phex.common.bandwidth.BandwidthManager;

public class StatisticsManager implements Manager, StatisticProviderConstants
{
    private HashMap statisticProviderMap;

    private static StatisticsManager instance;

    private StatisticsManager()
    {
        statisticProviderMap = new HashMap();
    }

    public void registerStatisticProvider( String name, StatisticProvider provider )
    {
        statisticProviderMap.put( name, provider );
    }

    public StatisticProvider getStatisticProvider( String name )
    {
        return (StatisticProvider)statisticProviderMap.get( name );
    }

    ////////////////////////////////////////////////////////////////////////////
    /// Manager methods
    ////////////////////////////////////////////////////////////////////////////

    public static StatisticsManager getInstance()
    {
        if ( instance == null )
        {
            instance = new StatisticsManager();
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
        registerStatisticProvider( UPTIME_PROVIDER,
            new UptimeStatisticProvider() );
        registerStatisticProvider( DAILY_UPTIME_PROVIDER,
            new DailyUptimeStatisticProvider() );
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
        BandwidthManager manager = BandwidthManager.getInstance();
        registerStatisticProvider( TOTAL_BANDWIDTH_PROVIDER,
            new TransferAverageStatisticProvider( manager.getPhexBandwidthController() ) );
        registerStatisticProvider( NETWORK_BANDWIDTH_PROVIDER,
            new TransferAverageStatisticProvider( manager.getNetworkBandwidthController() ) );
        registerStatisticProvider( DOWNLOAD_BANDWIDTH_PROVIDER,
            new TransferAverageStatisticProvider( manager.getDownloadBandwidthController() ) );
        registerStatisticProvider( UPLOAD_BANDWIDTH_PROVIDER,
            new TransferAverageStatisticProvider( manager.getUploadBandwidthController() ) );
        
        UploadDownloadCountStatistic.touch();
        
        registerStatisticProvider( HORIZON_HOST_COUNT_PROVIDER,
                HorizonStatisticProvider.HORIZON_HOST_COUNT_PROVIDER );
        registerStatisticProvider( HORIZON_FILE_COUNT_PROVIDER,
                HorizonStatisticProvider.HORIZON_FILE_COUNT_PROVIDER );
        registerStatisticProvider( HORIZON_FILE_SIZE_PROVIDER,
                HorizonStatisticProvider.HORIZON_FILE_SIZE_PROVIDER );
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
        UptimeStatisticProvider uptimeProvider = (UptimeStatisticProvider)
            getStatisticProvider( UPTIME_PROVIDER );
        uptimeProvider.saveUptimeStats();
        
        DailyUptimeStatisticProvider dailyUptimeProvider =
            (DailyUptimeStatisticProvider)getStatisticProvider( DAILY_UPTIME_PROVIDER );
        dailyUptimeProvider.shutdown();
        
        UploadDownloadCountStatistic.saveStats();
    }
}