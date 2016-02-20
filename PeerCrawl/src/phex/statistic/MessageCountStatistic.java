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
 *  $Id: MessageCountStatistic.java,v 1.5 2005/10/03 00:18:29 gregork Exp $
 */
package phex.statistic;


public class MessageCountStatistic extends SimpleStatisticProvider implements
    StatisticProviderConstants
{
    public static final SimpleStatisticProvider totalInMsgCounter;
    public static final SimpleStatisticProvider pingMsgInCounter;
    public static final SimpleStatisticProvider pongMsgInCounter;
    public static final SimpleStatisticProvider pushMsgInCounter;
    public static final SimpleStatisticProvider queryMsgInCounter;
    public static final SimpleStatisticProvider queryHitMsgInCounter;

    public static final SimpleStatisticProvider totalOutMsgCounter;
    public static final SimpleStatisticProvider pingMsgOutCounter;
    public static final SimpleStatisticProvider pongMsgOutCounter;
    public static final SimpleStatisticProvider pushMsgOutCounter;
    public static final SimpleStatisticProvider queryMsgOutCounter;
    public static final SimpleStatisticProvider queryHitMsgOutCounter;

    public static final SimpleStatisticProvider dropedMsgTotalCounter;
    public static final SimpleStatisticProvider dropedMsgInCounter;
    public static final SimpleStatisticProvider dropedMsgOutCounter;

    static
    {
        totalInMsgCounter = new MessageCountStatistic();
        pingMsgInCounter = new ChainedSimpleStatisticProvider( totalInMsgCounter );
        pongMsgInCounter = new ChainedSimpleStatisticProvider( totalInMsgCounter );
        pushMsgInCounter = new ChainedSimpleStatisticProvider( totalInMsgCounter );
        queryMsgInCounter = new ChainedSimpleStatisticProvider( totalInMsgCounter );
        queryHitMsgInCounter = new ChainedSimpleStatisticProvider( totalInMsgCounter );

        StatisticsManager.getInstance().registerStatisticProvider(
            TOTALMSG_IN_PROVIDER, totalInMsgCounter );
        StatisticsManager.getInstance().registerStatisticProvider(
            PINGMSG_IN_PROVIDER, pingMsgInCounter );
        StatisticsManager.getInstance().registerStatisticProvider(
            PONGMSG_IN_PROVIDER, pongMsgInCounter );
        StatisticsManager.getInstance().registerStatisticProvider(
            PUSHMSG_IN_PROVIDER, pushMsgInCounter );
        StatisticsManager.getInstance().registerStatisticProvider(
            QUERYMSG_IN_PROVIDER, queryMsgInCounter );
        StatisticsManager.getInstance().registerStatisticProvider(
            QUERYHITMSG_IN_PROVIDER, queryHitMsgInCounter );


        totalOutMsgCounter = new MessageCountStatistic();
        pingMsgOutCounter = new ChainedSimpleStatisticProvider( totalOutMsgCounter );
        pongMsgOutCounter = new ChainedSimpleStatisticProvider( totalOutMsgCounter );
        pushMsgOutCounter = new ChainedSimpleStatisticProvider( totalOutMsgCounter );
        queryMsgOutCounter = new ChainedSimpleStatisticProvider( totalOutMsgCounter );
        queryHitMsgOutCounter = new ChainedSimpleStatisticProvider( totalOutMsgCounter );

        StatisticsManager.getInstance().registerStatisticProvider(
            TOTALMSG_OUT_PROVIDER, totalOutMsgCounter );
        StatisticsManager.getInstance().registerStatisticProvider(
            PINGMSG_OUT_PROVIDER, pingMsgOutCounter );
        StatisticsManager.getInstance().registerStatisticProvider(
            PONGMSG_OUT_PROVIDER, pongMsgOutCounter );
        StatisticsManager.getInstance().registerStatisticProvider(
            PUSHMSG_OUT_PROVIDER, pushMsgOutCounter );
        StatisticsManager.getInstance().registerStatisticProvider(
            QUERYMSG_OUT_PROVIDER, queryMsgOutCounter );
        StatisticsManager.getInstance().registerStatisticProvider(
            QUERYHITMSG_OUT_PROVIDER, queryHitMsgOutCounter );


        dropedMsgTotalCounter = new MessageCountStatistic();
        dropedMsgInCounter = new ChainedSimpleStatisticProvider( dropedMsgTotalCounter );
        dropedMsgOutCounter = new ChainedSimpleStatisticProvider( dropedMsgTotalCounter );

        StatisticsManager.getInstance().registerStatisticProvider(
            DROPEDMSG_TOTAL_PROVIDER, dropedMsgTotalCounter );
        StatisticsManager.getInstance().registerStatisticProvider(
            DROPEDMSG_IN_PROVIDER, dropedMsgInCounter );
        StatisticsManager.getInstance().registerStatisticProvider(
            DROPEDMSG_OUT_PROVIDER, dropedMsgOutCounter );

    }
}