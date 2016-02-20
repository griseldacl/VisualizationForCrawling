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
 *  $Id: SharedFilesTableModel.java,v 1.8 2005/10/29 21:01:18 gregork Exp $
 */
package phex.gui.tabs.library;

import java.io.File;
import java.io.FileFilter;
import java.util.Comparator;

import javax.swing.event.TableModelEvent;

import org.apache.commons.collections.comparators.ComparableComparator;

import phex.event.ShareChangeListener;
import phex.event.SharedFilesChangeListener;
import phex.gui.common.GUIRegistry;
import phex.gui.common.LazyEventQueue;
import phex.gui.common.table.FWSortableTableModel;
import phex.gui.renderer.FileSizeCellRenderer;
import phex.share.ShareFile;
import phex.share.ShareManager;
import phex.share.SharedFilesService;
import phex.thex.ShareFileThexData;
import phex.utils.FilesOnlyFileFilter;
import phex.utils.Localizer;

public class SharedFilesTableModel extends FWSortableTableModel
{
    public static final int FILE_COLUMN_ID = 1001;
    public static final int DIRECTORY_COLUMN_ID = 1002;
    public static final int SIZE_COLUMN_ID = 1003;
    public static final int SEARCH_COUNT_COLUMN_ID = 1004;
    public static final int UPLOAD_COUNT_COLUMN_ID = 1005;
    public static final int SHA1_COLUMN_ID = 1006;
	public static final int THEX_COLUMN_ID = 1007;

    public static final int FILE_MODEL_INDEX = 0;
    public static final int DIRECTORY_MODEL_INDEX = 1;
    public static final int SIZE_MODEL_INDEX = 2;
    public static final int SEARCH_COUNT_MODEL_INDEX = 3;
    public static final int UPLOAD_COUNT_MODEL_INDEX = 4;
    public static final int SHA1_MODEL_INDEX = 5;
	public static final int THEX_MODEL_INDEX = 6;

    private static String[] tableColumns;
    private static Class[] tableClasses;

    static
    {
        tableColumns = new String[]
        {
            Localizer.getString( "File" ),
            Localizer.getString( "Directory" ),
            Localizer.getString( "Size" ),
            Localizer.getString( "SearchCount" ),
            Localizer.getString( "UploadCount" ),
            Localizer.getString( "SHA1" ),
			Localizer.getString( "SharedFilesTable_TigerTree" )
        };

        tableClasses = new Class[]
        {
            FileSystemTableCellRenderer.class,
            String.class,
            FileSizeCellRenderer.class,
            Integer.class,
            Integer.class,
            String.class,
            String.class
        };
    }
    
    private FileFilter fileFilter = new FilesOnlyFileFilter();
    private File displayDirectory;
    /**
     * Caching buffer of the files in display directory for performance and memory
     * savings.
     */
    private File[] displayDirectryFiles;
    private SharedFilesService sharedFilesService;

    public SharedFilesTableModel()
    {
        super( tableColumns, tableClasses );
        sharedFilesService = ShareManager.getInstance().getSharedFilesService();
        
        FileSystemChangeListener listener = new FileSystemChangeListener();
        sharedFilesService.addSharedFilesChangeListener(listener);
    }

    /**
     * @param displayDirectory The displayDirectory to set.
     */
    public void setDisplayDirectory(File displayDirectory)
    {
        this.displayDirectory = displayDirectory;
        if ( displayDirectory != null )
        {
            displayDirectryFiles = displayDirectory.listFiles(fileFilter);
        }
        fireTableDataChanged();
    }
    
    public int getRowCount()
    {
        if ( displayDirectory == null )
        {
            return 0;
        }
        if (displayDirectryFiles == null)
        {
            return 0;
        }
        return displayDirectryFiles.length;
    }

    public Object getValueAt(int row, int col)
    {
        if ( displayDirectory == null )
        {
            return "";
        }
        if ( row >= displayDirectryFiles.length )
        {
            fireTableRowsDeleted( row, row );
            return "";
        }
        ShareFile shareFile = sharedFilesService.getShareFileByFile( displayDirectryFiles[row] );
        if ( shareFile == null )
        {
            switch ( col )
            {
                case FILE_MODEL_INDEX:
                    return displayDirectryFiles[row];

                case DIRECTORY_MODEL_INDEX:
                    return displayDirectryFiles[row].getParent();

                case SIZE_MODEL_INDEX:
                    return new Long( displayDirectryFiles[row].length() );

                case SEARCH_COUNT_MODEL_INDEX:
                case UPLOAD_COUNT_MODEL_INDEX:
                    return null;
                case SHA1_MODEL_INDEX:
                case THEX_MODEL_INDEX:
                    return "";
            }
        }
        else
        {
            switch ( col )
            {
                case FILE_MODEL_INDEX:
                    return shareFile;
    
                case DIRECTORY_MODEL_INDEX:
                    return shareFile.getSystemFile().getParent();
    
                case SIZE_MODEL_INDEX:
                    return shareFile.getFileSizeObject();
    
                case SEARCH_COUNT_MODEL_INDEX:
                    return new Integer( shareFile.getSearchCount() );
    
                case UPLOAD_COUNT_MODEL_INDEX:
                    return new Integer( shareFile.getUploadCount() );
                case SHA1_MODEL_INDEX:
                    return shareFile.getSHA1();
                case THEX_MODEL_INDEX:
                    ShareFileThexData thexData = shareFile.getThexData(false);
                    return thexData != null ? thexData.getRootHash() : "";
            }
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
        switch ( column )
        {
            case SIZE_MODEL_INDEX:
                return ComparableComparator.getInstance();
        }
        return null;
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
            case DIRECTORY_COLUMN_ID:
                return DIRECTORY_MODEL_INDEX;
            case SIZE_COLUMN_ID:
                return SIZE_MODEL_INDEX;
            case SEARCH_COUNT_COLUMN_ID:
                return SEARCH_COUNT_MODEL_INDEX;
            case UPLOAD_COUNT_COLUMN_ID:
                return UPLOAD_COUNT_MODEL_INDEX;
            case SHA1_COLUMN_ID:
                return SHA1_MODEL_INDEX;
			case THEX_COLUMN_ID:
				return THEX_MODEL_INDEX;
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
            DIRECTORY_COLUMN_ID,
            SIZE_COLUMN_ID,
            SEARCH_COUNT_COLUMN_ID,
            UPLOAD_COUNT_COLUMN_ID,
            SHA1_COLUMN_ID,
			THEX_COLUMN_ID
        };
        return columnIds;
    }
    
    public class FileSystemChangeListener implements ShareChangeListener
    {
        /**
         * @see phex.event.ShareChangeListener#sharedDirectoriesChanged()
         */
        public void sharedDirectoriesChanged()
        {
            fireTableDataChanged();
        }
    }

    private class SharedFilesListener implements SharedFilesChangeListener
    {
        private LazyEventQueue lazyEventQueue;

        public SharedFilesListener()
        {
            lazyEventQueue = GUIRegistry.getInstance().getLazyEventQueue();
        }

        /**
         * Called if a shared file changed.
         */
        public void sharedFileChanged( int position )
        {
            fireTableCellUpdated( position, position );
        }

        /**
         * Called if a shared file was added.
         */
        public void sharedFileAdded( int position )
        {
            lazyEventQueue.addTableModelEvent(
                new TableModelEvent( SharedFilesTableModel.this,
                    position, position, TableModelEvent.ALL_COLUMNS,
                    TableModelEvent.INSERT ) );
        }

        /**
         * Called if a shared file was removed.
         */
        public void sharedFileRemoved( int position )
        {
            lazyEventQueue.addTableModelEvent(
                new TableModelEvent( SharedFilesTableModel.this,
                    position, position, TableModelEvent.ALL_COLUMNS,
                    TableModelEvent.DELETE ) );
        }

        /**
         * Called if all shared files changed.
         */
        public void allSharedFilesChanged( )
        {
            fireTableDataChanged();
        }
    }
}