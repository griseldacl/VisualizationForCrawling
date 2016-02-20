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
 *  $Id: SearchListTableModel.java,v 1.11 2005/10/29 21:01:18 gregork Exp $
 */
package phex.gui.models;

import java.util.Comparator;

import javax.swing.event.TableModelEvent;

import org.apache.commons.collections.comparators.ComparableComparator;

import phex.common.IntObj;
import phex.event.SearchListChangeListener;
import phex.gui.common.GUIRegistry;
import phex.gui.common.LazyEventQueue;
import phex.gui.common.table.FWSortableTableModel;
import phex.gui.renderer.ProgressCellRenderer;
import phex.gui.tabs.search.SearchResultsDataModel;
import phex.query.QueryManager;
import phex.query.Search;
import phex.query.SearchContainer;
import phex.utils.Localizer;

public class SearchListTableModel extends FWSortableTableModel implements SearchListChangeListener
{
    /**
     * The unique column id is not allowed to ever change over Phex releases. It
     * is used when serializing column information. The column id is containd in
     * the identifier field of the TableColumn.
     */
    public static final int SEARCH_TERM_COLUMN_ID = 1001;
    public static final int RESULT_COUNT_COLUMN_ID = 1002;
    public static final int FILTERED_COUNT_COLUMN_ID = 1003;
    public static final int PROGRESS_COLUMN_ID = 1004;
    public static final int STATUS_COLUMN_ID = 1005;

    public static final int SEARCH_TERM_MODEL_INDEX = 0;
    public static final int RESULT_COUNT_MODEL_INDEX = 1;
    public static final int FILTERED_COUNT_MODEL_INDEX = 2;
    public static final int PROGRESS_MODEL_INDEX = 3;
    public static final int STATUS_MODEL_INDEX = 4;

    private static String[] tableColumns;
    private static Class[] tableClasses;

    /**
     * Initialize super tableColumns field
     */
    static
    {
        tableColumns = new String[]
        {
            Localizer.getString( "SearchTerm" ),
            Localizer.getString( "Results" ),
            Localizer.getString( "Filtered" ),
            Localizer.getString( "Progress" ),
            Localizer.getString( "Status" )
        };

        tableClasses = new Class[]
        {
            String.class,
            IntObj.class,
            IntObj.class,
            ProgressCellRenderer.class,
            String.class
        };
    }

    private SearchContainer searchContainer;

    public SearchListTableModel( )
    {
        super( tableColumns, tableClasses );
        searchContainer = QueryManager.getInstance().getSearchContainer();
        searchContainer.addSearchListChangeListener( this );
    }

    public int getRowCount()
    {
        return searchContainer.getSearchCount();
    }

    public Object getValueAt(int row, int col)
    {
        Search search = searchContainer.getSearchAt( row );
        if (search == null)
        {
            return "";
        }
        SearchResultsDataModel dataModel;
        switch (col)
        {
            case SEARCH_TERM_MODEL_INDEX:
                return search.getSearchString();
            case RESULT_COUNT_MODEL_INDEX:
                dataModel = SearchResultsDataModel.lookupResultDataModel( search );
                return dataModel.getSearchElementCountObj();
            case FILTERED_COUNT_MODEL_INDEX:
                dataModel = SearchResultsDataModel.lookupResultDataModel( search );
                return dataModel.getFilteredElementCountObj();
            case PROGRESS_MODEL_INDEX:
                return search.getProgressObj();
            case STATUS_MODEL_INDEX:
                if (search.isSearching())
                {
                    return Localizer.getString("Searching");
                }
                else
                {
                    return Localizer.getString("Search_Stopped");
                }
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
            case PROGRESS_MODEL_INDEX:
                return ComparableComparator.getInstance();
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
            case SEARCH_TERM_COLUMN_ID:
                return SEARCH_TERM_MODEL_INDEX;
            case RESULT_COUNT_COLUMN_ID:
                return RESULT_COUNT_MODEL_INDEX;
            case FILTERED_COUNT_COLUMN_ID:
                return FILTERED_COUNT_MODEL_INDEX;
            case PROGRESS_COLUMN_ID:
                return PROGRESS_MODEL_INDEX;
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
        if ( columnID == SEARCH_TERM_COLUMN_ID )
        {
            return false;
        }
        return true;
    }

    public static int[] getColumnIdArray()
    {
        int[] columnIds = new int[]
        {
            SEARCH_TERM_COLUMN_ID,
            RESULT_COUNT_COLUMN_ID,
            FILTERED_COUNT_COLUMN_ID,
            PROGRESS_COLUMN_ID,
            STATUS_COLUMN_ID
        };
        return columnIds;
    }
    
    
    ///////////////////////// START event handling ////////////////////////////

    private LazyEventQueue lazyEventQueue = GUIRegistry.getInstance().getLazyEventQueue();

    /**
     * Called if a search changed.
     */
    public void searchChanged( int position )
    {
        lazyEventQueue.addTableModelEvent(
            new TableModelEvent( this, position, position,
            TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE ) );
    }

    /**
     * Called if a search was added.
     */
    public void searchAdded( int position )
    {
        fireTableChanged(
            new TableModelEvent( this, position, position,
            TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT ) );
    }

    /**
     * Called if a search was removed.
     */
    public void searchRemoved( int position )
    {
        fireTableChanged(
            new TableModelEvent(this, position, position,
            TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE ) );
    }

    ///////////////////////// END event handling ////////////////////////////
}
