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
package phex.host;

import java.util.TimerTask;

import phex.common.Environment;
import phex.common.LongObj;
import phex.common.ServiceManager;
import phex.connection.NetworkManager;
import phex.statistic.StatisticProviderConstants;
import phex.statistic.StatisticsManager;
import phex.statistic.UptimeStatisticProvider;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

public class UltrapeerCapabilityChecker extends TimerTask
{
    /**
     * The time periode in millis to wait between checks.
     */
    private static final long TIMER_PERIOD = 10 * 1000;
    private static final long TWENTY_MINUTES = 20 * 60 * 1000;
    private static final long TEN_MINUTES = 10 * 60 * 1000;
    private static final long ONE_HOUR = 60 * 60 * 1000;
    private static final long TWO_HOURS = 2 * 60 * 60 * 1000;

    private boolean isUltrapeerCapable;
    private boolean isUltrapeerOS;
    private UptimeStatisticProvider uptimeProvider;

    public UltrapeerCapabilityChecker()
    {
        Environment env = Environment.getInstance();
        isUltrapeerOS = env.isUltrapeerOS();

        uptimeProvider = (UptimeStatisticProvider)StatisticsManager.getInstance().
            getStatisticProvider( StatisticProviderConstants.UPTIME_PROVIDER );
        env.scheduleTimerTask( this, 0, TIMER_PERIOD );
    }

    /**
     * Provided run implementation of TimerTask.
     */
    public void run()
    {
        try
        {
            checkIfUltrapeerCapable();
        }
        catch ( Throwable th )
        {
            NLogger.error(NLoggerNames.GLOBAL, th, th );
        }
    }

    private void checkIfUltrapeerCapable()
    {
        boolean isCapable =
            // the first check if we are allowed to become a ultrapeer at all...
            // if not we dont need to continue checking...
            ServiceManager.sCfg.allowToBecomeUP &&
            // host should not be firewalled.
            NetworkManager.getInstance().hasConnectedIncoming() &&
            // host should provide a Ultrapeer capable OS
            isUltrapeerOS &&
            // the connection speed should be more then single ISDN
            ServiceManager.sCfg.networkSpeedKbps > 64 &&
            // also we should provide at least 10KB network bandwidth
            ServiceManager.sCfg.mNetMaxRate > 10 * 1024 &&
            // and at least 14KB total bandwidth (because network bandwidth might
            // be set to unlimited)
            ServiceManager.sCfg.maxTotalBandwidth > 14 * 1024 &&
            // the current uptime should be at least 20 minutes or 10 minutes in avg.
            ( ((LongObj)uptimeProvider.getValue()).getValue() > TWENTY_MINUTES ||
              ((LongObj)uptimeProvider.getAverageValue()).getValue() > TEN_MINUTES );

        isUltrapeerCapable = isCapable;
    }

    public boolean isUltrapeerCapable()
    {
        return isUltrapeerCapable;
    }
}