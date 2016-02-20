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

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;

import org.apache.commons.collections.comparators.ComparableComparator;

import phex.common.ShortObj;
import phex.common.format.NumberFormatUtils;
import phex.download.DownloadScopeList;
import phex.download.swarming.*;
import phex.event.DownloadCandidatesChangeListener;
import phex.gui.common.GUIRegistry;
import phex.gui.common.LazyEventQueue;
import phex.gui.common.table.FWSortableTableModel;
import phex.gui.common.table.FWTable;
import phex.gui.comparator.HostAddressComparator;
import phex.gui.renderer.HostAddressCellRenderer;
import phex.gui.renderer.ScopeProgressCellRenderer;
import phex.utils.Localizer;

public class SWCandidateTableModel extends FWSortableTableModel
{
    /**
     * The unique column id is not allowed to ever change over Phex releases. It
     * is used when serializing column information. The column id is containd in
     * the identifier field of the TableColumn.
     */
    public static final int HOST_COLUMN_ID = 1001;
    public static final int VENDOR_COLUMN_ID = 1002;
    public static final int PROGRESS_COLUMN_ID = 1003;
    public static final int TOTAL_DOWNLOAD_COLUMN_ID = 1004;
    public static final int RATE_COLUMN_ID = 1005;
    public static final int STATUS_COLUMN_ID = 1006;

    public static final int HOST_MODEL_INDEX = 0;
    public static final int VENDOR_MODEL_INDEX = 1;
    public static final int PROGRESS_MODEL_INDEX = 2;
    public static final int TOTAL_DOWNLOAD_MODEL_INDEX = 3;
    public static final int RATE_MODEL_INDEX = 4;
    public static final int STATUS_MODEL_INDEX = 5;

    private static String[] tableColumns;
    private static Class[] tableClasses;

    /**
     * Initialize super tableColumns field
     */
    static
    {
        tableColumns = new String[]
        {
            Localizer.getString( "SharingHost" ),
            Localizer.getString( "Vendor" ),
            Localizer.getString( "DownloadTable_Available" ),
            Localizer.getString( "DownloadTable_DownloadTotal" ),
            Localizer.getString( "Rate" ),
            Localizer.getString( "Status" )
        };

        tableClasses = new Class[]
        {
            HostAddressCellRenderer.class,
            String.class,
            ScopeProgressCellRenderer.class,
            String.class,
            String.class,
            String.class
        };
    }

    private SwarmingManager swarmingMgr;

    /**
     * The currently displayed download file of the model.
     */
    private SWDownloadFile downloadFile;

    private FWTable downloadTable;

    private DownloadCandidatesListener changeListener;

    /**
     * @param downloadTable The constructor takes the download JTable. This is
     * necessary to get informed of the selection changes of the download table.
     */
    public SWCandidateTableModel( FWTable aDownloadTable )
    {
        super( tableColumns, tableClasses );
        swarmingMgr = SwarmingManager.getInstance();
        downloadTable = aDownloadTable;
        downloadTable.getSelectionModel().addListSelectionListener(
            new DownloadSelectionChangeHandler() );
        changeListener = new DownloadCandidatesListener();
    }

    /**
     * Returns the download file that is currently displayed.
     */
    public SWDownloadFile getDownloadFile()
    {
        return downloadFile;
    }

    public int getRowCount()
    {
        if ( downloadFile == null )
        {
            return 0;
        }
        return downloadFile.getCandidatesCount();
    }

    public Object getValueAt( int row, int column )
    {
        SWDownloadCandidate candidate = downloadFile.getCandidate( row );
        if ( candidate == null )
        {
            fireTableRowsDeleted( row, row );
            return "";
        }
        
        switch( column )
        {
            case 0:
                return candidate.getHostAddress();
            case 1:
                return candidate.getVendor();
            case PROGRESS_MODEL_INDEX:
                return candidate;
            case TOTAL_DOWNLOAD_MODEL_INDEX:
                return NumberFormatUtils.formatSignificantByteSize( 
                    candidate.getTotalDownloadSize() );
            case RATE_MODEL_INDEX:
            {
                SWDownloadSegment segment = candidate.getDownloadSegment();
                if ( segment == null )
                {
                    return null;
                }
                return NumberFormatUtils.formatSignificantByteSize( 
                    segment.getTransferSpeed() ) + Localizer.getString( "PerSec" );
            }
            case STATUS_MODEL_INDEX:
                return SWDownloadInfo.getDownloadCandidateStatusString(
                    candidate );
            default:
                return "";
        }
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
        SWDownloadCandidate candidate = downloadFile.getCandidate( row );
        if ( candidate == null )
        {
            return "";
        }
        
        switch( column )
        {
            case PROGRESS_MODEL_INDEX:
            {
                DownloadScopeList availableScopeList = candidate.getAvailableScopeList();
                if ( availableScopeList == null )
                {
                    return null;
                }
                return new Long( availableScopeList.getAggregatedLength() );
            }
            case TOTAL_DOWNLOAD_MODEL_INDEX:
                return new Long( candidate.getTotalDownloadSize() );
            case STATUS_MODEL_INDEX:
                ShortObj status = candidate.getStatusObj();
                if ( status.value ==
                     SWDownloadConstants.STATUS_CANDIDATE_REMOTLY_QUEUED )
                {
                    int queuePosition = candidate.getXQueueParameters().getPosition().intValue();
                    Double doubObj = new Double( status.doubleValue() + 1.0 -
                        Math.min( (double)queuePosition, (double)10000 ) / 10000.0 );
                    return doubObj;
                }
                else
                {
                    long timeLeft = candidate.getStatusTimeLeft();
                    double val;
                    if ( timeLeft == 0 )
                    {
                        val = status.doubleValue();
                    }
                    else
                    {// timeLeft is not 0.. checked above..
                        val = status.doubleValue() - 1.0 
                            + 1 / (double)timeLeft;
                    }
                    return new Double( val );
                }
            case RATE_MODEL_INDEX:
            {
                SWDownloadSegment segment = candidate.getDownloadSegment();
                if ( segment == null )
                {
                    return null;
                }
                return new Long( segment.getTransferSpeed() );
            }
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
            case PROGRESS_COLUMN_ID:
                return PROGRESS_MODEL_INDEX;
            case TOTAL_DOWNLOAD_COLUMN_ID:
                return TOTAL_DOWNLOAD_MODEL_INDEX;
            case RATE_COLUMN_ID:
                return RATE_MODEL_INDEX;
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
            PROGRESS_COLUMN_ID,
            TOTAL_DOWNLOAD_COLUMN_ID,
            RATE_COLUMN_ID,
            STATUS_COLUMN_ID
        };
        return columnIds;
    }

    private class DownloadSelectionChangeHandler implements ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent e)
        {
            if ( !e.getValueIsAdjusting() )
            {
                ListSelectionModel model = (ListSelectionModel) e.getSource();
                int viewIdx = model.getMinSelectionIndex();
                int modelIdx = downloadTable.convertRowIndexToModel( viewIdx );
                
                SWDownloadFile tmpDownloadFile = swarmingMgr.getDownloadFile( modelIdx );
                if ( tmpDownloadFile != downloadFile )
                {
                    if ( downloadFile != null )
                    {
                        downloadFile.removeDownloadCandidatesChangeListener(
                            changeListener );
                    }
                    downloadFile = tmpDownloadFile;
                    if ( downloadFile != null )
                    {
                        downloadFile.addDownloadCandidatesChangeListener(
                            changeListener );
                    }
                    fireTableDataChanged();
                }
                //Logger.logMessage( Logger.WARNING, Logger.GLOBAL, "select Index "
                //    + idx + " file " + downloadFile);
            }
        }
    }

    private class DownloadCandidatesListener
        implements DownloadCandidatesChangeListener
    {
        private LazyEventQueue lazyEventQueue;

        public DownloadCandidatesListener()
        {
            lazyEventQueue = GUIRegistry.getInstance().getLazyEventQueue();
        }

        /**
         * Called if a download file changed.
         */
        public void downloadCandidateChanged( int position )
        {
            fireTableCellUpdated( position, position );
            /*lazyEventQueue.addTableModelEvent(
                new TableModelEvent( SWCandidateTableModel.this, position, position,
                    TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE ) );*/
        }

        /**
         * Called if a download file was added.
         */
        public void downloadCandidateAdded( int position )
        {
            fireTableChanged( new TableModelEvent(SWCandidateTableModel.this,
                position, position, TableModelEvent.ALL_COLUMNS,
                TableModelEvent.INSERT ) );
        }

        /**
         * Called if a download file was removed.
         */
        public void downloadCandidateRemoved( int position )
        {
            fireTableChanged( new TableModelEvent(SWCandidateTableModel.this,
                position, position, TableModelEvent.ALL_COLUMNS,
                TableModelEvent.DELETE ) );
        }
    }
}