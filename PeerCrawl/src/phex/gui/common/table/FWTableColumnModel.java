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
package phex.gui.common.table;

import java.util.*;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.xml.bind.JAXBException;

import phex.utils.NLogger;
import phex.utils.NLoggerNames;
import phex.xml.ObjectFactory;
import phex.xml.XJBGUITableColumn;
import phex.xml.XJBGUITableColumnList;

/**
 * TableColumnModel that is able to hold visible and invisible table columns
 * to support customizable tables.
 */
public class FWTableColumnModel extends DefaultTableColumnModel
{
    /**
     * Contains all table columns, visible and invisible columns.
     */
    private ArrayList allTableColumns;

    /**
     * @param columnIdArray A array containing the required column ids for this
     *        column model.
     */
    public FWTableColumnModel( FWTableModel tableModel, int[] columnIdArray )
    {
        this( tableModel, columnIdArray, null );
    }

    public FWTableColumnModel( FWTableModel tableModel, int[] columnIdArray,
        XJBGUITableColumnList xjbColumnList )
    {
        super();
        allTableColumns = new ArrayList();
        buildColumnsFromColumnIDs( columnIdArray, tableModel, xjbColumnList );
    }

    public void addColumn( FWTableColumn aColumn )
    {
        if ( aColumn.isVisible() )
        {
            super.addColumn( aColumn );
        }
        allTableColumns.add( aColumn );
    }

    /**
     * Shows a column on the view.
     */
    public void showColumn( FWTableColumn aColumn )
    {
        if ( !aColumn.isVisible() )
        {
            aColumn.setVisible( true );
            super.addColumn( aColumn );
            int pos = getColumnCount() - 1;
            int modelPos = aColumn.getModelIndex();

            if ( pos == 0 )
            {
                // first column... no move...
                return;
            }

            // check prev column to find right position to show
            TableColumn tempCol = getColumn( pos - 1 );
            int tempIdx = tempCol.getModelIndex();
            while ( tempIdx > modelPos )
            {
                pos --;
                if ( pos == 0 )
                {// we are at the first pos...
                    break;
                }
                tempCol = getColumn( pos - 1 );
                tempIdx = tempCol.getModelIndex();
            }
            if ( pos < getColumnCount() - 1 )
            {
                moveColumn( getColumnCount() - 1, pos  );
            }
        }
    }

    /**
     * Hides a column from the view.
     */
    public void hideColumn( FWTableColumn aColumn )
    {
        if ( aColumn.isHideable() && aColumn.isVisible() )
        {
            removeColumn( aColumn );
            aColumn.setVisible( false );
        }
    }

    /**
     * Returns the table column by identifier. This must not be a visible TableColumn.
     */
    public FWTableColumn getColumn( Object identifier )
    {
        Iterator iterator = allTableColumns.iterator();
        FWTableColumn aColumn;

        while ( iterator.hasNext() )
        {
            aColumn = (FWTableColumn)iterator.next();
            // Compare them this way in case the column's identifier is null.
            if ( identifier.equals( aColumn.getIdentifier() ) )
            {
                return aColumn;
            }
        }
        return null;
    }

    /**
     * Returns an iterator over all table columns, containing visible and
     * invisible columns.
     */
    public Iterator createAllColumnsIterator()
    {
        return allTableColumns.iterator();
    }

    public XJBGUITableColumnList createXJBGUITableColumnList()
        throws JAXBException
    {
        ObjectFactory objFactory = new ObjectFactory();
        XJBGUITableColumnList colList = objFactory.createXJBGUITableColumnList();
        java.util.List list = colList.getTableColumnList();

        // cols need to be sorrted by visible index to be able to build up
        // the table on restart without running into index out of bounds.
        Collections.sort( allTableColumns, new VisibleTableColumnComparator() );
        Iterator iterator = allTableColumns.iterator();
        while( iterator.hasNext() )
        {
            FWTableColumn column = (FWTableColumn)iterator.next();
            XJBGUITableColumn xjbColumn = column.createXJBGUITableColumn();
            if ( column.isVisible() )
            {
                xjbColumn.setVisibleIndex( getColumnIndex( column.getIdentifier() ) );
            }
            list.add( xjbColumn );
        }
        return colList;
    }

    protected void buildColumnsFromColumnIDs( int[] columnIdArray,
        FWTableModel tableModel, XJBGUITableColumnList xjbColumnList )
    {
        ArrayList newColumnList = new ArrayList( columnIdArray.length );
        for ( int i = 0; i < columnIdArray.length; i++ )
        {
            FWTableColumn column = null;
            if ( xjbColumnList != null )
            {
                column = buildColumnFromXML( columnIdArray[ i ], tableModel, xjbColumnList );
            }
            if ( column == null )
            {
                column = buildDefaultColumn( columnIdArray[ i ], tableModel );
                newColumnList.add( column );
            }
        }
        arrangeColumns( xjbColumnList, newColumnList, tableModel );
    }

    protected FWTableColumn buildColumnFromXML( int columnId,
        FWTableModel tableModel, XJBGUITableColumnList xjbColumnList )
    {
        if ( xjbColumnList == null )
        {
            return null;
        }
        try
        {
            FWTableColumn column = null;
            XJBGUITableColumn xjbColumn;
            Iterator iterator = xjbColumnList.getTableColumnList().iterator();
            while( iterator.hasNext() )
            {
                xjbColumn = (XJBGUITableColumn)iterator.next();
                if ( columnId == xjbColumn.getColumnID() )
                {
                    int modelIndex = tableModel.mapColumnIDToModelIndex( columnId );
                    column = new FWTableColumn( modelIndex, tableModel.getColumnName(
                        modelIndex ), new Integer( columnId ) );
                    column.setHideable( tableModel.isColumnHideable( columnId ) );
                    column.setVisible( xjbColumn.isVisible() );
                    column.setPreferredWidth( xjbColumn.getWidth() );
                    addColumn( column );
                    return column;
                }
            }
            return null;
        }
        catch ( Exception exp )
        {
            NLogger.error( NLoggerNames.USER_INTERFACE, exp, exp );
            return null;
        }
    }

    protected FWTableColumn buildDefaultColumn( int columnId,
        FWTableModel tableModel )
    {
        int columnIndex = tableModel.mapColumnIDToModelIndex( columnId );
        FWTableColumn column = new FWTableColumn( columnIndex,
            tableModel.getColumnName( columnIndex ), new Integer( columnId ) );
        column.setHideable( tableModel.isColumnHideable( columnId ) );
        addColumn( column );
        return column;
    }

    protected void arrangeColumns( XJBGUITableColumnList xjbColumnList,
        ArrayList newColumnList, FWTableModel tableModel )
    {
        if ( xjbColumnList != null )
        {
            XJBGUITableColumn xjbColumn;
            Iterator iterator = xjbColumnList.getTableColumnList().iterator();
            while( iterator.hasNext() )
            {
                try
                {
                    xjbColumn = (XJBGUITableColumn)iterator.next();
                    int columnId = xjbColumn.getColumnID();
                    // verify if columnId is still used, column might have been
                    // removed from model already... modelIndex must be >= 0 to
                    // indicate that column is used.
                    int modelIndex = tableModel.mapColumnIDToModelIndex( columnId );
                    if ( xjbColumn.isVisible() && modelIndex >= 0)
                    {
                        int colIdx = getColumnIndex( new Integer( columnId ) );
                        int visibleIndex = xjbColumn.getVisibleIndex();
                        visibleIndex = Math.min( visibleIndex, getColumnCount() - 1 );
                        moveColumn( colIdx, visibleIndex );
                    }
                }
                catch ( Exception exp )
                {// catch any exception to maintain process
                    NLogger.error( NLoggerNames.USER_INTERFACE, exp, exp );
                }
            }
        }
        Iterator iterator = newColumnList.iterator();
        while( iterator.hasNext() )
        {
            FWTableColumn column = (FWTableColumn) iterator.next();

            int currentIdx = getColumnIndex( column.getIdentifier() );
            int newIdx = Math.min( tableModel.mapColumnIDToModelIndex(
                ((Integer)column.getIdentifier()).intValue() ),
                getColumnCount() - 1 );
            moveColumn( currentIdx, newIdx );
        }
    }

    private class VisibleTableColumnComparator implements Comparator
    {
        public int compare( Object obj1, Object obj2 )
        {
            FWTableColumn col1 = (FWTableColumn)obj1;
            FWTableColumn col2 = (FWTableColumn)obj2;
            if ( !col1.isVisible() )
            {
                // if col1 is not visible then col2 is larger (if visible or not)
                return -1;
            }
            if ( !col2.isVisible() )
            {
                // if col2 is not visible then col1 is larger (if visible or not)
                return 1;
            }

            // both cols are visible determine the higher index
            int col1Idx = getColumnIndex( col1.getIdentifier() );
            int col2Idx = getColumnIndex( col2.getIdentifier() );

            return col1Idx - col2Idx;
        }
    }
}