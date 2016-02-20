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
 *  $Id: HorizonStatisticProvider.java,v 1.3 2005/10/03 00:18:29 gregork Exp $
 */
package phex.statistic;

import phex.common.HorizonTracker;
import phex.common.IntObj;
import phex.common.LongObj;

/**
 *
 */
public class HorizonStatisticProvider implements StatisticProvider
{
    private static final short HOST_COUNT = 0;
    private static final short FILE_COUNT = 1;
    private static final short FILE_SIZE = 2;
    
    public static HorizonStatisticProvider HORIZON_HOST_COUNT_PROVIDER = 
        new HorizonStatisticProvider( HOST_COUNT );
    public static HorizonStatisticProvider HORIZON_FILE_COUNT_PROVIDER = 
        new HorizonStatisticProvider( FILE_COUNT );
    public static HorizonStatisticProvider HORIZON_FILE_SIZE_PROVIDER = 
        new HorizonStatisticProvider( FILE_SIZE );
    
    private short type;
    private LongObj valueObj;
    private IntObj avgObj;
    
    public HorizonStatisticProvider( short type )
    {
        this.type = type;
        valueObj = new LongObj();
        switch ( type )
        {
        case FILE_COUNT:
        case FILE_SIZE:    
            avgObj = new IntObj();
        }
    }
    
    /**
     * @see phex.statistic.StatisticProvider#getValue()
     */
    public Object getValue()
    {
        switch ( type )
        {
        case HOST_COUNT:
            valueObj.value = HorizonTracker.getInstance().getTotalHostCount();
            break;
        case FILE_COUNT:
            valueObj.value = HorizonTracker.getInstance().getTotalFileCount();
            break;
        case FILE_SIZE:
            valueObj.value = HorizonTracker.getInstance().getTotalFileSize();
            break;
        }
        return valueObj;
    }

    /**
     * @see phex.statistic.StatisticProvider#getAverageValue()
     */
    public Object getAverageValue()
    {
        HorizonTracker tracker;
        switch ( type )
        {
        case FILE_SIZE:
            tracker = HorizonTracker.getInstance();
            long count = tracker.getTotalFileCount();
            if ( count != 0 )
            {
                int val = (int)((double)tracker.getTotalFileSize() / (double)count);
                avgObj.value = val;
            }
            break;
        case FILE_COUNT:
            tracker = HorizonTracker.getInstance();
            int hostCount = tracker.getTotalHostCount();
            if ( hostCount != 0 )
            {
                int val = (int)((double)tracker.getTotalFileCount() / (double)hostCount);
                avgObj.value = val;
            }
            break;            
        }
        return avgObj;
    }

    /**
     * @see phex.statistic.StatisticProvider#getMaxValue()
     */
    public Object getMaxValue()
    {
        return null;
    }

    /**
     * @see phex.statistic.StatisticProvider#toStatisticString(java.lang.Object)
     */
    public String toStatisticString(Object value)
    {
        return value.toString();
    }
}
