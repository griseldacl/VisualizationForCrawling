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

import javax.swing.event.TableModelEvent;

import org.apache.commons.collections.comparators.ComparableComparator;

import phex.common.format.NumberFormatUtils;
import phex.event.UploadFilesChangeListener;
import phex.gui.common.GUIRegistry;
import phex.gui.common.LazyEventQueue;
import phex.gui.common.table.FWSortableTableModel;
import phex.gui.comparator.HostAddressComparator;
import phex.gui.comparator.TransferSizeComparator;
import phex.gui.renderer.*;
import phex.upload.UploadManager;
import phex.upload.UploadState;
import phex.upload.UploadStatusInfo;
import phex.utils.Localizer;

public class UploadFilesTableModel extends FWSortableTableModel
{
    /**
     * The unique column id is not allowed to ever change over Phex releases. It
     * is used when serializing column information. The column id is containd in
     * the identifier field of the TableColumn.
     */
    public static final int HOST_COLUMN_ID = 1001;
    public static final int VENDOR_COLUMN_ID = 1002;
    public static final int FILE_COLUMN_ID = 1003;
    public static final int PROGRESS_COLUMN_ID = 1004;
    public static final int SIZE_COLUMN_ID = 1005;
    public static final int RATE_COLUMN_ID = 1006;
    public static final int STATUS_COLUMN_ID = 1007;
    public static final int ETA_COLUMN_ID = 1008;

    public static final int HOST_MODEL_INDEX = 0;
    public static final int VENDOR_MODEL_INDEX = 1;
    public static final int FILE_MODEL_INDEX = 2;
    public static final int PROGRESS_MODEL_INDEX = 3;
    public static final int SIZE_MODEL_INDEX = 4;
    public static final int RATE_MODEL_INDEX = 5;
    public static final int ETA_MODEL_INDEX = 6;
    public static final int STATUS_MODEL_INDEX = 7;

    private static String[] tableColumns;
    private static Class[] tableClasses;

    static
    {
        tableColumns = new String[]
        {
            Localizer.getString( "Host" ),
            Localizer.getString( "Vendor" ),
            Localizer.getString( "File" ),
            Localizer.getString( "PercentSign" ),
            Localizer.getString( "Size" ),
            Localizer.getString( "Rate" ),
            Localizer.getString( "UploadTable_ETA" ),
            Localizer.getString( "Status" )
        };

        tableClasses = new Class[]
        {
            HostAddressCellRenderer.class,
            String.class,
            String.class,
            ProgressCellRenderer.class,
            TransferSizeCellRenderer.class,
            String.class,
            ETACellRenderer.class,
            String.class
        };
    }

    private UploadManager uploadMgr;

    public UploadFilesTableModel()
    {
        super( tableColumns, tableClasses );
        uploadMgr = UploadManager.getInstance();
        uploadMgr.addUploadFilesChangeListener( new UploadFilesListener() );
    }

    public int getRowCount()
    {
        return uploadMgr.getUploadListSize();
    }

    public Object getValueAt( int row, int col )
    {
        UploadState uploadState = uploadMgr.getUploadStateAt( row );
        if ( uploadState == null )
        {
            fireTableRowsDeleted( row, row );
            return "";
        }

        switch ( col )
        {
            case HOST_MODEL_INDEX:
                return uploadState.getHostAddress();

            case VENDOR_MODEL_INDEX:
                String vendor = uploadState.getVendor();
                if ( vendor == null )
                {
                    return "";
                }
                else
                {
                    return vendor;
                }

            case FILE_MODEL_INDEX:
                return uploadState.getFileName();

            case PROGRESS_MODEL_INDEX:
                return uploadState.getProgress();

            case SIZE_MODEL_INDEX:
                return uploadState;
            case RATE_MODEL_INDEX:
            {
                return NumberFormatUtils.formatSignificantByteSize( 
                    uploadState.getTransferSpeed() ) + Localizer.getString( "PerSec" );
            }
            case ETA_MODEL_INDEX:
                return uploadState;
            case STATUS_MODEL_INDEX:
                return UploadStatusInfo.getUploadStatusString( uploadState.getStatus() );
        }
        return "";
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
        UploadState uploadState = uploadMgr.getUploadStateAt( row );
        if ( uploadState == null )
        {
            return "";
        }
        switch ( column )
        {
            case RATE_MODEL_INDEX:
                return new Long( uploadState.getTransferSpeed() );
        }
        return getValueAt( row, column );
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
            case PROGRESS_MODEL_INDEX:
                return ComparableComparator.getInstance();
            case SIZE_MODEL_INDEX:
                return new TransferSizeComparator();
            // for all other columns use default comparator
            default:
                return null;
        }
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
            case FILE_COLUMN_ID:
                return FILE_MODEL_INDEX;
            case PROGRESS_COLUMN_ID:
                return PROGRESS_MODEL_INDEX;
            case SIZE_COLUMN_ID:
                return SIZE_MODEL_INDEX;
            case RATE_COLUMN_ID:
                return RATE_MODEL_INDEX;
            case ETA_COLUMN_ID:
                return ETA_MODEL_INDEX;
            case STATUS_COLUMN_ID:
                return STATUS_MODEL_INDEX;
            default:
                return -1;
        }
    }
    
    public static int[] getColumnIdArray()
    {
        int[] columnIds = new int[]
        {
            HOST_COLUMN_ID,
            VENDOR_COLUMN_ID,
            FILE_COLUMN_ID,
            PROGRESS_COLUMN_ID,
            SIZE_COLUMN_ID,
            RATE_COLUMN_ID,
            ETA_COLUMN_ID,
            STATUS_COLUMN_ID
        };
        return columnIds;
    }

    /**
     * Indicates if a column is hideable.
     */
    public boolean isColumnHideable( int columnID )
    {
        if ( columnID == FILE_COLUMN_ID )
        {
            return false;
        }
        return true;
    }

    private class UploadFilesListener
        implements UploadFilesChangeListener
    {
        private LazyEventQueue lazyEventQueue;

        public UploadFilesListener()
        {
            lazyEventQueue = GUIRegistry.getInstance().getLazyEventQueue();
        }

        public void uploadFileChanged( final int position )
        {
            lazyEventQueue.addTableModelEvent(
                new TableModelEvent( UploadFilesTableModel.this, position, position,
                    TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE ) );
        }

        public void uploadFileAdded( final int position )
        {
            fireTableChanged(
                new TableModelEvent(UploadFilesTableModel.this, position, position,
                    TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT ) );
        }

        /**
         * Called if the upload queue was changed.
         */
        public void uploadQueueChanged()
        {
        }

        public void uploadFileRemoved( final int position )
        {
            fireTableChanged(
                new TableModelEvent(UploadFilesTableModel.this, position, position,
                    TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE ) );
        }
    }
}