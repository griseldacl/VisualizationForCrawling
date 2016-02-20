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
package phex.gui.models;

import phex.gui.common.table.*;
import phex.statistic.*;
import phex.utils.Localizer;

public class StatisticsTableModel extends FWSortableTableModel
    implements StatisticProviderConstants
{
    /**
     * The unique column id is not allowed to ever change over Phex releases. It
     * is used when serializing column information. The column id is containd in
     * the identifier field of the TableColumn.
     */
    private static final int NAME_COLUMN_ID = 1001;
    private static final int VALUE_COLUMN_ID = 1002;
    private static final int AVG_COLUMN_ID = 1003;
    private static final int MAX_COLUMN_ID = 1004;

    private static final int NAME_MODEL_INDEX = 0;
    private static final int VALUE_MODEL_INDEX = 1;
    private static final int AVG_MODEL_INDEX = 2;
    private static final int MAX_MODEL_INDEX = 3;

    private static String[] tableColumns;
    private static Class[] tableClasses;

    private static String[] rowKeys;

    /**
     * Initialize super tableColumns field
     */
    static
    {
        tableColumns = new String[]
        {
            Localizer.getString( "Name" ),
            Localizer.getString( "Value" ),
            Localizer.getString( "Avg." ),
            Localizer.getString( "Max." ),
        };

        tableClasses = new Class[]
        {
            String.class,
            String.class,
            String.class,
            String.class,
        };

        rowKeys = new String[]
        {
            TOTAL_BANDWIDTH_PROVIDER,
            NETWORK_BANDWIDTH_PROVIDER,
            DOWNLOAD_BANDWIDTH_PROVIDER,
            UPLOAD_BANDWIDTH_PROVIDER,
            
            HORIZON_HOST_COUNT_PROVIDER,
            HORIZON_FILE_COUNT_PROVIDER,
            HORIZON_FILE_SIZE_PROVIDER,

            TOTALMSG_IN_PROVIDER,
            PINGMSG_IN_PROVIDER,
            PONGMSG_IN_PROVIDER,
            QUERYMSG_IN_PROVIDER,
            QUERYHITMSG_IN_PROVIDER,
            PUSHMSG_IN_PROVIDER,

            TOTALMSG_OUT_PROVIDER,
            PINGMSG_OUT_PROVIDER,
            PONGMSG_OUT_PROVIDER,
            QUERYMSG_OUT_PROVIDER,
            QUERYHITMSG_OUT_PROVIDER,
            PUSHMSG_OUT_PROVIDER,

            DROPEDMSG_TOTAL_PROVIDER,
            DROPEDMSG_IN_PROVIDER,
            DROPEDMSG_OUT_PROVIDER,
            
            PUSH_DOWNLOAD_ATTEMPTS_PROVIDER,
            PUSH_DOWNLOAD_SUCESS_PROVIDER,
            PUSH_DOWNLOAD_FAILURE_PROVIDER,
            
            PUSH_DLDPUSHPROXY_ATTEMPTS_PROVIDER,
            PUSH_DLDPUSHPROXY_SUCESS_PROVIDER,
            
            PUSH_UPLOAD_ATTEMPTS_PROVIDER,
            PUSH_UPLOAD_SUCESS_PROVIDER,
            PUSH_UPLOAD_FAILURE_PROVIDER,

            UPTIME_PROVIDER,
            DAILY_UPTIME_PROVIDER
        };
    }

    private StatisticsManager statisticsMgr;

    public StatisticsTableModel()
    {
        super( tableColumns, tableClasses );
        statisticsMgr = StatisticsManager.getInstance();
    }

    public int getRowCount()
    {
        return rowKeys.length;
    }

    public Object getValueAt( int row, int column )
    {
        if ( column == 0 )
        {
            return Localizer.getString( rowKeys[ row ] );
        }
        else if ( column == 1 )
        {
            StatisticProvider provider = statisticsMgr.getStatisticProvider(
                rowKeys[ row ] );
            if ( provider == null )
            {
                return "";
            }
            Object value = provider.getValue();
            if ( value != null )
            {
                return provider.toStatisticString( value );
            }
        }
        else if ( column == 2 )
        {
            StatisticProvider provider = statisticsMgr.getStatisticProvider(
                rowKeys[ row ] );
            if ( provider == null )
            {
                return "";
            }
            // TODO how do we get the unit back in there??
            Object value = provider.getAverageValue();
            if ( value != null )
            {
                return provider.toStatisticString( value );
            }
        }
        else if ( column == 3 )
        {
            StatisticProvider provider = statisticsMgr.getStatisticProvider(
                rowKeys[ row ] );
            if ( provider == null )
            {
                return "";
            }
            // TODO how do we get the unit back in there??
            Object value = provider.getMaxValue();
            if ( value != null )
            {
                return provider.toStatisticString( value );
            }
        }

        return "";
    }

    /**
     * Maps the unique column id to the model index. This needs to be done to
     * be able identify columns and there index after changes in Phex releases.
     * The unique column id is not allowed to ever change over Phex releases. It
     * is used when serializing column information. The column id is containd in
     * the identifier field of the TableColumn.
     */
    public int mapColumnIDToModelIndex( int columnId )
    {
        switch( columnId )
        {
            case NAME_COLUMN_ID:
                return NAME_MODEL_INDEX;
            case VALUE_COLUMN_ID:
                return VALUE_MODEL_INDEX;
            case AVG_COLUMN_ID:
                return AVG_MODEL_INDEX;
            case MAX_COLUMN_ID:
                return MAX_MODEL_INDEX;
            default:
                return -1;
        }
    }

    /**
     * Indicates if a column is hideable.
     */
    public boolean isColumnHideable( int columnID )
    {
        if ( columnID == NAME_COLUMN_ID )
        {
            return false;
        }
        return true;
    }

    public static int[] getColumnIdArray()
    {
        int[] columnIds = new int[]
        {
            NAME_COLUMN_ID,
            VALUE_COLUMN_ID,
            AVG_COLUMN_ID,
            MAX_COLUMN_ID,
        };
        return columnIds;
    }



}