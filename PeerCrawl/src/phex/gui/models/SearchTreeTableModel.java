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
 *  $Id: SearchTreeTableModel.java,v 1.13 2005/10/03 00:18:26 gregork Exp $
 */
package phex.gui.models;

import phex.common.format.HostSpeedFormatUtils;
import phex.download.RemoteFile;
import phex.gui.renderer.FileSizeCellRenderer;
import phex.gui.renderer.HostAddressCellRenderer;
import phex.gui.tabs.search.SearchResultElement;
import phex.gui.tabs.search.SearchResultElementComparator;
import phex.utils.Localizer;

/**
 * 
 */
public class SearchTreeTableModel extends AbstractTreeTableModel
    implements TreeTableModel, ISortableModel
{
    /**
     * The unique column id is not allowed to ever change over Phex releases. It
     * is used when serializing column information. The column id is containd in
     * the identifier field of the TableColumn.
     */
    public static final int HOST_COLUMN_ID = 1001;
    public static final int FILE_COLUMN_ID = 1002;
    public static final int EXTENSION_COLUMN_ID = 1003;
    public static final int SIZE_COLUMN_ID = 1004;
    public static final int SCORE_COLUMN_ID = 1005;
    public static final int HOST_RATING_COLUMN_ID = 1006;
    public static final int HOST_SPEED_COLUMN_ID = 1007;
    public static final int HOST_VENDOR_COLUMN_ID = 1008;
    public static final int META_DATA_COLUMN_ID = 1009;
    public static final int SHA1_COLUMN_ID = 1010;

    public static final int FILE_MODEL_INDEX = 0;
    public static final int EXTENSION_MODEL_INDEX = 1;
    public static final int SIZE_MODEL_INDEX = 2;
    public static final int SCORE_MODEL_INDEX = 3;
    public static final int HOST_RATING_MODEL_INDEX = 4;
    public static final int HOST_SPEED_MODEL_INDEX = 5;
    public static final int HOST_MODEL_INDEX = 6;
    public static final int HOST_VENDOR_MODEL_INDEX = 7;
    public static final int META_DATA_MODEL_INDEX = 8;
    public static final int SHA1_MODEL_INDEX = 9;

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
            Localizer.getString( "Type" ),
            Localizer.getString( "Size" ),
            Localizer.getString( "Score" ),
            Localizer.getString( "Rating" ),
            Localizer.getString( "HostSpeed" ),
            Localizer.getString( "SharingHost" ),
            Localizer.getString( "Vendor" ),
            Localizer.getString( "Information" ),
            Localizer.getString( "SHA1" )
        };

        tableClasses = new Class[]
        {
            TreeTableModel.class,
            String.class,
            FileSizeCellRenderer.class,
            Short.class,
            Short.class,
            Integer.class,
            HostAddressCellRenderer.class,
            String.class,
            String.class,
            String.class
        };
    }
    
    private static Object TREE_ROOT = new Object();
    
    private ISearchDataModel displayedDataModel;
    
    /**
     * The column index to sort.
     */
    private int sortedColumn;
    
    /**
     * The sorting direction.
     */
    private boolean isAscending;
    
    public SearchTreeTableModel()
    {
        super( TREE_ROOT, tableColumns, tableClasses );
        sortedColumn = -1;
    }
    
    public void setDisplayedSearch( ISearchDataModel dataModel )
    {
        // otherwise no need to update...
        if ( displayedDataModel != dataModel )
        {
            // unregister...
            if ( displayedDataModel != null )
            {
                displayedDataModel.setVisualizationModel( null );
            }
            displayedDataModel = dataModel;
            fireTreeStructureChanged( TREE_ROOT, new Object[]{TREE_ROOT}, null, null );
            if ( displayedDataModel != null )
            {
                sortByColumn( sortedColumn, isAscending );
                displayedDataModel.setVisualizationModel( this );
            }
        }
    }
    
    /**
     * @return
     */
    public ISearchDataModel getDisplayedResultsData()
    { 
        return displayedDataModel;
    }
    
    /* (non-Javadoc)
     * @see phex.gui.models.TreeTableModel#getValueAt(java.lang.Object, int)
     */
    public Object getValueAt(Object node, int column)
    {
        switch ( column )
        {
            // for all rows in common...
            case FILE_MODEL_INDEX:
                RemoteFile remoteFile = getRemoteFile( node );
                return remoteFile.getDisplayName();
            case EXTENSION_MODEL_INDEX:
                return getRemoteFile( node ).getFileExt();
            case SIZE_MODEL_INDEX:
                return getRemoteFile( node ).getFileSizeObject();
            case META_DATA_MODEL_INDEX:
                return getRemoteFile( node ).getMetaData();
            case SHA1_MODEL_INDEX:
                return getRemoteFile( node ).getSHA1();
                
            // for rows depending on grouped result count
            case SCORE_MODEL_INDEX: 
                if ( node instanceof SearchResultElement)
                {
                    return ((SearchResultElement)node).getValue( SCORE_MODEL_INDEX );
                }
                else
                {
                    return ((RemoteFile)node).getScore();
                }
            case HOST_RATING_MODEL_INDEX:
                if ( node instanceof SearchResultElement)
                {
                    return ((SearchResultElement)node).getValue( HOST_RATING_MODEL_INDEX );
                }
                else
                {
                    return ((RemoteFile)node).getQueryHitHost().getHostRatingObject();
                }
            case HOST_SPEED_MODEL_INDEX:
                if ( node instanceof SearchResultElement)
                {
                    return HostSpeedFormatUtils.formatHostSpeed( 
                        ((Number)((SearchResultElement)node).getValue( HOST_SPEED_MODEL_INDEX ) ).longValue());
                }
                else
                {
                    return HostSpeedFormatUtils.formatHostSpeed( 
                        ((RemoteFile)node).getSpeedObject().longValue() );
                }
            case HOST_MODEL_INDEX:
                if ( node instanceof SearchResultElement)
                {
                    return ((SearchResultElement)node).getValue( HOST_MODEL_INDEX );
                }
                else
                {
                    return ((RemoteFile)node).getHostAddress();
                }
            case HOST_VENDOR_MODEL_INDEX:
                if ( node instanceof SearchResultElement)
                {
                    return ((SearchResultElement)node).getValue( HOST_VENDOR_MODEL_INDEX );
                }
                else
                {
                    return ((RemoteFile)node).getQueryHitHost().getVendor();
                }
        }
        return "";
    }
    
    /**
     * Returns the remote file of the node. In case of SearchResultElement
     * the single remote file is returned.
     * @param node
     * @return
     */
    private RemoteFile getRemoteFile( Object node )
    {
        if ( node instanceof SearchResultElement)
        {
            return ((SearchResultElement)node).getSingleRemoteFile();
        }
        else
        {
            return (RemoteFile)node;
        }
    }

    /**
     * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
     */
    public Object getChild(Object parent, int index)
    {
        Object child;
        if ( parent == TREE_ROOT )
        {
            child = displayedDataModel.getSearchElementAt( index );
        }
        else //must be: if ( parent instanceof SearchResultElement)
        {
            child = ((SearchResultElement)parent).getRemoteFileAt( index );
        }
        return child;
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
     */
    public int getChildCount(Object parent)
    {
        int count;
        if ( parent == TREE_ROOT && displayedDataModel != null)
        {
            count = displayedDataModel.getSearchElementCount();
        }
        else if ( parent instanceof SearchResultElement)
        {
            count = ((SearchResultElement)parent).getRemoteFileListCount();
        }
        else
        {
            count = 0;
        }
        return count;
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
            case FILE_COLUMN_ID:
                return FILE_MODEL_INDEX;
            case EXTENSION_COLUMN_ID:
                return EXTENSION_MODEL_INDEX;
            case SIZE_COLUMN_ID:
                return SIZE_MODEL_INDEX;
            case SCORE_COLUMN_ID:
                return SCORE_MODEL_INDEX;
            case HOST_RATING_COLUMN_ID:
                return HOST_RATING_MODEL_INDEX;
            case HOST_SPEED_COLUMN_ID:
                return HOST_SPEED_MODEL_INDEX;
            case HOST_VENDOR_COLUMN_ID:
                return HOST_VENDOR_MODEL_INDEX;
            case META_DATA_COLUMN_ID:
                return META_DATA_MODEL_INDEX;
            case SHA1_COLUMN_ID:
                return SHA1_MODEL_INDEX;
            default:
                return -1;
        }
    }

    /**
     * Indicates if a column is hideable.
     */
    public boolean isColumnHideable( int columnId )
    {
        if ( columnId == FILE_COLUMN_ID )
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
            EXTENSION_COLUMN_ID,
            SIZE_COLUMN_ID,
            SCORE_COLUMN_ID,
            HOST_RATING_COLUMN_ID,
            HOST_SPEED_COLUMN_ID,
            HOST_COLUMN_ID,
            HOST_VENDOR_COLUMN_ID,
            META_DATA_COLUMN_ID,
            SHA1_COLUMN_ID
        };
        return columnIds;
    }

    //////////////////////////////////// Start Implementation of ISortableModel /////////////////////////////////
    
    /**
     * @see phex.gui.models.ISortableModel#getSortByColumn()
     */
    public int getSortByColumn()
    {
        return sortedColumn;
    }

    /**
     * @see phex.gui.models.ISortableModel#isSortedAscending()
     */
    public boolean isSortedAscending()
    {
        return isAscending;
    }

    /**
     * @see phex.gui.models.ISortableModel#sortByColumn(int, boolean)
     */
    public void sortByColumn(int column, boolean isSortedAscending)
    {
        sortedColumn = column;
        isAscending = isSortedAscending;
        if ( displayedDataModel == null )
        {
            return;
        }
        switch ( column )
        {
            case SIZE_MODEL_INDEX:
                displayedDataModel.setSortBy( SearchResultElementComparator.SORT_BY_SIZE, isSortedAscending );
                break;
            case FILE_MODEL_INDEX:
                displayedDataModel.setSortBy( SearchResultElementComparator.SORT_BY_FILE, isSortedAscending );
                break;
            case EXTENSION_MODEL_INDEX:
                displayedDataModel.setSortBy( SearchResultElementComparator.SORT_BY_EXTENSION, isSortedAscending );
                break;
            case SHA1_MODEL_INDEX:
                displayedDataModel.setSortBy( SearchResultElementComparator.SORT_BY_SHA1, isSortedAscending );
                break;
            case HOST_MODEL_INDEX:
                displayedDataModel.setSortBy( SearchResultElementComparator.SORT_BY_HOST, isSortedAscending );
                break;
            case SCORE_MODEL_INDEX:
                displayedDataModel.setSortBy( SearchResultElementComparator.SORT_BY_SCORE, isSortedAscending );
                break;
            case HOST_RATING_MODEL_INDEX:
                displayedDataModel.setSortBy( SearchResultElementComparator.SORT_BY_RATING, isSortedAscending );
                break;
            case HOST_SPEED_MODEL_INDEX:
                displayedDataModel.setSortBy( SearchResultElementComparator.SORT_BY_SPEED, isSortedAscending );
                break;
            case HOST_VENDOR_MODEL_INDEX:
                displayedDataModel.setSortBy( SearchResultElementComparator.SORT_BY_VENDOR, isSortedAscending );
                break;
            case META_DATA_MODEL_INDEX:
                displayedDataModel.setSortBy( SearchResultElementComparator.SORT_BY_META_DATA, isSortedAscending );
                break;
        }
    }
    
    //////////////////////////////////// End Implementation of ISortableModel /////////////////////////////////
}