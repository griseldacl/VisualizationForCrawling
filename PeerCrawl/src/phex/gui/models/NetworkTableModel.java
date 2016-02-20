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

import java.util.Comparator;

import phex.common.format.NumberFormatUtils;
import phex.common.format.TimeFormatUtils;
import phex.event.*;
import phex.gui.common.table.*;
import phex.gui.comparator.HostAddressComparator;
import phex.gui.renderer.HostAddressCellRenderer;
import phex.host.*;
import phex.utils.*;

public class NetworkTableModel extends FWSortableTableModel
{
    /**
     * The unique column id is not allowed to ever change over Phex releases. It
     * is used when serializing column information. The column id is containd in
     * the identifier field of the TableColumn.
     */
    public static final int HOST_COLUMN_ID = 1001;
    public static final int VENDOR_COLUMN_ID = 1002;
    public static final int TYPE_COLUMN_ID = 1003;
    public static final int RECEIVED_DROPPED_COLUMN_ID = 1004;
    public static final int SENT_QUEUED_COLUMN_ID = 1005;
    public static final int SHARED_COLUMN_ID = 1007;
    public static final int UPTIME_COLUMN_ID = 1008;
    public static final int STATUS_COLUMN_ID = 1009;
    public static final int MODE_COLUMN_ID = 1010;

    public static final int HOST_MODEL_INDEX = 0;
    public static final int VENDOR_MODEL_INDEX = 1;
    public static final int TYPE_MODEL_INDEX = 2;
    public static final int MODE_MODEL_INDEX = 3;
    public static final int RECEIVED_DROPPED_MODEL_INDEX = 4;
    public static final int SENT_QUEUED_MODEL_INDEX = 5;
    public static final int SHARED_MODEL_INDEX = 6;
    public static final int UPTIME_MODEL_INDEX = 7;
    public static final int STATUS_MODEL_INDEX = 8;

    private static String[] tableColumns;
    private static Class[] tableClasses;

    static
    {
        tableColumns = new String[]
        {
            Localizer.getString( "RemoteHost" ),
            Localizer.getString( "Vendor" ),
            Localizer.getString( "Type" ),
            Localizer.getString( "Mode" ),
            Localizer.getString( "ReceivedDropped" ),
            Localizer.getString( "SentQueuedDropped" ),
            Localizer.getString( "Shared" ),
            Localizer.getString( "Uptime" ),
            Localizer.getString( "Status" )
        };

        tableClasses = new Class[]
        {
             HostAddressCellRenderer.class,
             String.class,
             String.class,
             String.class,
             String.class,
             String.class,
             String.class,
             String.class,
             String.class,
        };
    }

    private NetworkHostsContainer hostsContainer;

    public NetworkTableModel()
    {
        super( tableColumns, tableClasses );
        HostManager hostMgr = HostManager.getInstance();
        hostsContainer = hostMgr.getNetworkHostsContainer();
        hostsContainer.addNetworkHostsChangeListener( new NetworkHostsListener() );
    }

    public int getRowCount()
    {
        return hostsContainer.getNetworkHostCount();
    }

    public Object getValueAt(int row, int col)
    {
        Host host = hostsContainer.getNetworkHostAt( row );
        if ( host == null )
        {
            fireTableRowsDeleted( row, row );
            return "";
        }

        switch (col)
        {
            case HOST_MODEL_INDEX:
                return host.getHostAddress();

            case VENDOR_MODEL_INDEX:
                return host.getVendor();

            case TYPE_MODEL_INDEX:
                Object[] args = new Object[]
                {
                    host.getType()
                };
                return Localizer.getFormatedString( "HostType", args );

            case MODE_MODEL_INDEX:
                if ( !host.isConnected() )
                {
                    return "";
                }
                if ( host.isUltrapeer() )
                {
                    String mode = Localizer.getString( "Ultrapeer" );
                    if ( host.getPushProxyAddress() != null )
                    {
                        mode += " (PP)";
                    }
                    return mode;
                }
                else if ( host.isUltrapeerLeafConnection() )
                {
                    return Localizer.getString( "Leaf" );
                }
                else
                {
                    return Localizer.getString( "Peer" );
                }
            case RECEIVED_DROPPED_MODEL_INDEX:
                return String.valueOf(host.getReceivedCount() + " (" + String.valueOf(host.getDropCount()) + ")");

            case SENT_QUEUED_MODEL_INDEX:
                return String.valueOf( host.getSentCount() ) + " / "
                     + String.valueOf( host.getSendQueueLength() ) + " / "
                     + String.valueOf( host.getSendDropCount() );

            case SHARED_MODEL_INDEX:
                if (host.getFileCount() == -1)
                {
                    return "";
                }
                else
                {
                    return host.getFileCount() + "/" + NumberFormatUtils.formatSignificantByteSize(
                        ((long)host.getTotalSize()) * 1024L );
                }
            case UPTIME_MODEL_INDEX:
                long upSeconds = host.getConnectionUpTime( System.currentTimeMillis() ) / 1000;
                return TimeFormatUtils.formatSignificantElapsedTime( upSeconds );

            case STATUS_MODEL_INDEX:
                return HostInfo.getHostStatusString(host);
        }
        return "";
    }
    
    /**
     * Returns the most comparator that is used for sorting of the cell values
     * in the column. This is used by the FWSortedTableModel to perform the
     * sorting. If not overwritten the method returns null causing the
     * FWSortedTableModel to use a NaturalComparator. It expects all Objects that
     * are returned from getComparableValueAt() to implement the Comparable interface.
     *
     */
    public Comparator getColumnComparator( int column )
    {
        switch( column )
        {
            case HOST_MODEL_INDEX:
                return new HostAddressComparator();
            // for all other columns use default comparator
            default:
                return null;
        }
    }

    /**
     * Returns an attribute value that is used for comparing on sorting
     * for the cell at row and column. If not overwritten the call is forwarded
     * to getValueAt().
     * The returned Object is compared via the Comparator returned from
     * getColumnComparator(). If no comparator is specified the returned Object
     * must implement the Comparable interface.
     */
    public Object getComparableValueAt( int row, int column )
    {
        switch ( column )
        {
            case UPTIME_MODEL_INDEX:
                Host host = hostsContainer.getNetworkHostAt( row );
                if ( host == null )
                {
                    return new Long( Long.MIN_VALUE );
                }
                return host.getConnectionUpTimeObject( System.currentTimeMillis() );
        }
        return getValueAt( row, column );
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
            case HOST_COLUMN_ID:
                return HOST_MODEL_INDEX;
            case VENDOR_COLUMN_ID:
                return VENDOR_MODEL_INDEX;
            case TYPE_COLUMN_ID:
                return TYPE_MODEL_INDEX;
            case MODE_COLUMN_ID:
                return MODE_MODEL_INDEX;
            case RECEIVED_DROPPED_COLUMN_ID:
                return RECEIVED_DROPPED_MODEL_INDEX;
            case SENT_QUEUED_COLUMN_ID:
                return SENT_QUEUED_MODEL_INDEX;
            case SHARED_COLUMN_ID:
                return SHARED_MODEL_INDEX;
            case UPTIME_COLUMN_ID:
                return UPTIME_MODEL_INDEX;
            case STATUS_COLUMN_ID:
                return STATUS_MODEL_INDEX;
            default:
                return -1;
        }
    }

    /**
     * Indicates if a column is hideable.
     */
    public boolean isColumnHideable( int columnID )
    {
        if ( columnID == HOST_COLUMN_ID )
        {
            return false;
        }
        return true;
    }

    public static int[] getColumnIdArray()
    {
        int[] columnIds = new int[]
        {
            HOST_COLUMN_ID,
            VENDOR_COLUMN_ID,
            TYPE_COLUMN_ID,
            MODE_COLUMN_ID,
            RECEIVED_DROPPED_COLUMN_ID,
            SENT_QUEUED_COLUMN_ID,
            SHARED_COLUMN_ID,
            UPTIME_COLUMN_ID,
            STATUS_COLUMN_ID,
        };
        return columnIds;
    }

    private class NetworkHostsListener implements NetworkHostsChangeListener
    {
        public void networkHostChanged( final int position )
        {
            fireTableRowsUpdated( position, position );
        }

        public void networkHostAdded( final int position )
        {
            fireTableRowsInserted( position, position );
        }

        public void networkHostRemoved( final int position )
        {
            fireTableRowsDeleted( position, position );
        }
    }
}