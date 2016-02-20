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
package phex.gui.tabs.download;

import java.util.Comparator;

import javax.swing.event.TableModelEvent;

import org.apache.commons.collections.comparators.ReverseComparator;

import phex.common.format.NumberFormatUtils;
import phex.download.swarming.SWDownloadFile;
import phex.download.swarming.SWDownloadInfo;
import phex.download.swarming.SwarmingManager;
import phex.event.DownloadFilesChangeListener;
import phex.gui.common.GUIRegistry;
import phex.gui.common.LazyEventQueue;
import phex.gui.common.table.FWSortableTableModel;
import phex.gui.common.table.FWSortedTableModel;
import phex.gui.comparator.ETAComparator;
import phex.gui.comparator.TransferSizeComparator;
import phex.gui.renderer.*;
import phex.utils.Localizer;

public class SWDownloadTableModel extends FWSortableTableModel
{   
    /**
     * The unique column id is not allowed to ever change over Phex releases. It
     * is used when serializing column information. The column id is containd in
     * the identifier field of the TableColumn.
     */
    private static final int FILE_COLUMN_ID = 1001;
    private static final int PROGRESS_COLUMN_ID = 1002;
    private static final int SIZE_COLUMN_ID = 1003;
    private static final int RATE_COLUMN_ID = 1004;
    private static final int STATUS_COLUMN_ID = 1005;
    private static final int CANDIDATE_COUNT_COLUMN_ID = 1006;
    private static final int SEARCH_TERM_COLUMN_ID = 1007;
    private static final int SHA1_COLUMN_ID = 1008;
    private static final int PRIORITY_COLUMN_ID = 1009;
    private static final int CREATED_TIME_COLUMN_ID = 1010;
    private static final int DOWNLOADED_TIME_COLUMN_ID = 1011;
    private static final int ETA_COLUMN_ID = 1012;

    private static final int FILE_MODEL_INDEX = 0;
    private static final int PROGRESS_MODEL_INDEX = 1;
    private static final int SIZE_MODEL_INDEX = 2;
    private static final int RATE_MODEL_INDEX = 3;
    private static final int ETA_MODEL_INDEX = 4;
    private static final int CANDIDATE_COUNT_MODEL_INDEX = 5;
    private static final int STATUS_MODEL_INDEX = 6;
    private static final int PRIORITY_MODEL_INDEX = 7;
    private static final int SEARCH_TERM_MODEL_INDEX = 8;
    private static final int CREATED_TIME_MODEL_INDEX = 9;
    private static final int DOWNLOADED_TIME_MODEL_INDEX = 10;
    private static final int SHA1_MODEL_INDEX = 11;

    private static String[] tableColumns;
    private static Class[] tableClasses;

    /**
     * Initialize super tableColumns field
     */
    static
    {
        tableColumns = new String[]
        {
            Localizer.getString( "File" ),
            Localizer.getString( "PercentSign" ),
            Localizer.getString( "Size" ),
            Localizer.getString( "Rate" ),
            Localizer.getString( "DownloadTable_ETA" ),
            Localizer.getString( "NumberOfCandidates" ),
            Localizer.getString( "Status" ),
            Localizer.getString( "Priority" ),
            Localizer.getString( "SearchTerm" ),
            Localizer.getString( "Created" ),
            Localizer.getString( "Downloaded" ),
            Localizer.getString( "SHA1" )
        };

        tableClasses = new Class[]
        {
            String.class,
            ProgressCellRenderer.class,
            TransferSizeCellRenderer.class,
            String.class,
            ETACellRenderer.class,
            String.class,
            String.class,
            Integer.class,
            String.class,
            DateCellRenderer.class,
            DateCellRenderer.class,
            String.class
        };
    }

    private SwarmingManager swarmingMgr;

    public SWDownloadTableModel()
    {
        super( tableColumns, tableClasses );
        swarmingMgr = SwarmingManager.getInstance();
        swarmingMgr.addDownloadFilesChangeListener( new DownloadFilesListener() );
    }

    public int getRowCount()
    {
        return swarmingMgr.getDownloadFileCount();
    }

    public Object getValueAt( int row, int column )
    {
        SWDownloadFile download = swarmingMgr.getDownloadFile( row );
        if ( download == null )
        {
            fireTableRowsDeleted( row, row );
            return null;
        }

        switch (column)
        {
        case FILE_MODEL_INDEX:
            return download.getDestinationFile().getName();
        case PROGRESS_MODEL_INDEX:
            return download.getProgress();
        case SIZE_MODEL_INDEX:
            // 2: handled by TransferSizeCellRenderer
            return download;
        case RATE_MODEL_INDEX:
        {
            long maxRate = download.getDownloadThrottlingRate();
            String maxRateStr;
            if ( maxRate >= Integer.MAX_VALUE )
            {
                maxRateStr = Localizer.getDecimalFormatSymbols().getInfinity();
            }
            else
            {
                maxRateStr = NumberFormatUtils.formatSignificantByteSize( maxRate) 
                    + Localizer.getString( "PerSec" );
            }
            return NumberFormatUtils.formatSignificantByteSize( 
                download.getTransferSpeed() ) + Localizer.getString( "PerSec" )
                + " (" + maxRateStr + ")";
        }
        case ETA_MODEL_INDEX:
            return download;
        case CANDIDATE_COUNT_MODEL_INDEX:
            return String.valueOf(download.getDownloadingCandidatesCount())
                + " / "
                + String.valueOf(download.getQueuedCandidatesCount())
                + " / " + String.valueOf(download.getCandidatesCountObject());
        case STATUS_MODEL_INDEX:
            return SWDownloadInfo.getDownloadFileStatusString(download
                .getStatus());
        case PRIORITY_MODEL_INDEX:
            return swarmingMgr.getDownloadPriority(download);
        case SEARCH_TERM_MODEL_INDEX:
            return download.getResearchSetting().getSearchTerm();
        case CREATED_TIME_MODEL_INDEX:
            return download.getCreatedDate();
        case DOWNLOADED_TIME_MODEL_INDEX:
            return download.getDownloadedDate();
        case SHA1_MODEL_INDEX:
            return download.getResearchSetting().getSHA1();
        default:
            return "";
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
        SWDownloadFile download = swarmingMgr.getDownloadFile( row );
        if ( download == null )
        {
            return "";
        }
        switch ( column )
        {
            case CANDIDATE_COUNT_MODEL_INDEX:
                return download.getCandidatesCountObject();
            case RATE_MODEL_INDEX:
                return new Long( download.getTransferSpeed() );
            case ETA_MODEL_INDEX:
                return download;
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
            case CANDIDATE_COUNT_MODEL_INDEX:
            	return FWSortedTableModel.REVERSE_COMPARABLE_COMPARATOR;
            case PROGRESS_MODEL_INDEX:
                return FWSortedTableModel.REVERSE_COMPARABLE_COMPARATOR;
            case RATE_MODEL_INDEX:
            	return FWSortedTableModel.REVERSE_COMPARABLE_COMPARATOR;
            case ETA_MODEL_INDEX:
                return new ETAComparator();
            case SIZE_MODEL_INDEX:
                return new ReverseComparator( new TransferSizeComparator() );
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
            case CANDIDATE_COUNT_COLUMN_ID:
                return CANDIDATE_COUNT_MODEL_INDEX;
            case STATUS_COLUMN_ID:
                return STATUS_MODEL_INDEX;
            case PRIORITY_COLUMN_ID:
                return PRIORITY_MODEL_INDEX;
            case SEARCH_TERM_COLUMN_ID:
                return SEARCH_TERM_MODEL_INDEX;
            case CREATED_TIME_COLUMN_ID:
                return CREATED_TIME_MODEL_INDEX;
            case DOWNLOADED_TIME_COLUMN_ID:
                return DOWNLOADED_TIME_MODEL_INDEX;
            case SHA1_COLUMN_ID:
                return SHA1_MODEL_INDEX;
            default:
                return -1;
        }
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

    public static int[] getColumnIdArray()
    {
        int[] columnIds = new int[]
        {
            FILE_COLUMN_ID,
            PROGRESS_COLUMN_ID,
            SIZE_COLUMN_ID,
            RATE_COLUMN_ID,
            ETA_COLUMN_ID,
            CANDIDATE_COUNT_COLUMN_ID,
            STATUS_COLUMN_ID,
            PRIORITY_COLUMN_ID,
            SEARCH_TERM_COLUMN_ID,
            CREATED_TIME_COLUMN_ID,
            DOWNLOADED_TIME_COLUMN_ID,
            SHA1_COLUMN_ID
        };
        return columnIds;
    }

    private class DownloadFilesListener
        implements DownloadFilesChangeListener
    {
        private LazyEventQueue lazyEventQueue;

        public DownloadFilesListener()
        {
            lazyEventQueue = GUIRegistry.getInstance().getLazyEventQueue();
        }

        public void downloadFileChanged( final int position )
        {
            lazyEventQueue.addTableModelEvent(
                new TableModelEvent( SWDownloadTableModel.this, position, position,
                    TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE ) );
        }

        public void downloadFileAdded( final int position )
        {
            fireTableChanged( new TableModelEvent(SWDownloadTableModel.this,
                position, position, TableModelEvent.ALL_COLUMNS,
                TableModelEvent.INSERT ) );
        }

        public void downloadFileRemoved( final int position )
        {
            fireTableChanged( new TableModelEvent(SWDownloadTableModel.this,
                position, position, TableModelEvent.ALL_COLUMNS,
                TableModelEvent.DELETE ) );
        }
    }
}