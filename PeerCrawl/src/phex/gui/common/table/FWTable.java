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

import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;

import phex.gui.common.GUIRegistry;
import phex.gui.renderer.DefaultPhexCellRenderers;

public class FWTable extends JTable
{
    private boolean isColumnResizeToFitEnabled;
    private boolean isColumnSortingEnabled;
    private JPopupMenu headerPopup;

    public FWTable( TableModel dataModel, FWTableColumnModel columnModel )
    {
        super( dataModel, columnModel );
        isColumnResizeToFitEnabled = false;
        isColumnSortingEnabled = false;
        tableHeader.addMouseListener( new MouseHandler() );
        // Set up cell renderers. Just use our standard default renderer to class relationships.
        DefaultPhexCellRenderers.setDefaultPhexCellRenderers( this );
    }

    public void updateUI()
    {
        super.updateUI();
        GUIRegistry guiRegistry = GUIRegistry.getInstance();

        boolean showHorizontalLines = guiRegistry.getShowTableHorizontalLines();
        boolean showVerticalLines = guiRegistry.getShowTableVerticalLines();

        setShowHorizontalLines( showHorizontalLines );
        setShowVerticalLines( showVerticalLines );

        int intercellWidth = showVerticalLines ? 1 : 0;
        int intercellHeight = showHorizontalLines ? 1 : 0;
        // when lines are not shown this hides the little gap between cells
        // when lines are shows a gap is created to display the line
        setIntercellSpacing( new Dimension( intercellWidth, intercellHeight ) );
    }

    public static JScrollPane createFWTableScrollPane( JTable table )
    {
        JScrollPane tableScrollPane = new JScrollPane( table );
        updateFWTableScrollPane( tableScrollPane );
        return tableScrollPane;
    }

    public static void updateFWTableScrollPane( JScrollPane tableScrollPane )
    {
        // this is a very strange behavior here. if I set
        // viewport.setBackground( (Color)UIManager.getDefaults().get(
        //      "window" ) );
        // it is not working but this strange code works somehow...
        // so we need to create a new color object out of the returned ColorUIResource
        Color color = (Color)UIManager.getDefaults().get( "window" );
        Color newColor = new Color( color.getRGB() );
        JViewport viewport = tableScrollPane.getViewport();
        viewport.setBackground( newColor );
        viewport.setOpaque( true );
    }

    public void activateAllHeaderActions( )
    {
        // activates a right click popup menu for the table header to hide and show
        // columns.
        activateHeaderPopupMenu();
        activateColumnResizeToFit();
        activateColumnSorting();
    }

    public void activateColumnSorting()
    {
        setColumnSelectionAllowed( false );
        isColumnSortingEnabled = true;
        FWSortedTableModel sortedModel = (FWSortedTableModel)dataModel;
        sortedModel.setTableHeader( tableHeader );
    }

    /**
     * Activates the column resize to fit that occures when clicking between two
     * columns.
     */
    public void activateColumnResizeToFit( )
    {
        // activate the column resize to fit
        isColumnResizeToFitEnabled = true;
    }

    /**
     * Activates right click popup menu for the table header to hide and show
     * columns.
     */
    public void activateHeaderPopupMenu( )
    {
        headerPopup = new JPopupMenu();
        PopupMenuActionHandler actionHandler = new PopupMenuActionHandler();
        FWTableColumn column;
        ColumnCheckBoxMenuItem chkItem;
        Iterator iterator = ((FWTableColumnModel)columnModel).createAllColumnsIterator();
        while( iterator.hasNext() )
        {
            column = (FWTableColumn)iterator.next();
            chkItem = new ColumnCheckBoxMenuItem( column );
            chkItem.setEnabled( column.isHideable() );
            chkItem.addActionListener( actionHandler );
            headerPopup.add( chkItem );
        }
    }

    /**
     * Maps the index of the row in the view at viewRowIndex to the index of the
     * row in the model. Returns the index of the corresponding row in the
     * model. If viewRowIndex is less than zero, returns viewRowIndex.
     *
     * Mapping of the index is necessary if the view might display a different
     * ordering of rows due to sorting or filtering of rows. If there is no
     * different ordering between the view and the model then the view index is
     * returned directly.
     *
     * @params viewRowIndex - the index of the row in the view
     * @returns the index of the corresponding row in the model
     **/
    public int convertRowIndexToModel( int viewRowIndex )
    {
        if ( isColumnSortingEnabled )
        {
            return ((FWSortedTableModel)dataModel).getModelIndex(
                viewRowIndex );
        }
        else
        {
            return viewRowIndex;
        }
    }

    /**
     * Maps the indices of the rows in the view at viewRowIndices to the indices
     * of the rows in the model. Returns the indices of the corresponding rows
     * in the model. If a view row index is less than zero, it is coppied.
     *
     * Mapping of the indices is necessary if the view might display a different
     * ordering of rows due to sorting or filtering of rows. If there is no
     * different ordering between the view and the model then the viewRowIndices
     * is returned, otherwise a new array with the mapped indices is returned.
     *
     * @params viewRowIndices - the indices of the rows in the view
     * @returns the indices of the corresponding rows in the model
     **/
    public int[] convertRowIndicesToModel( int[] viewRowIndices )
    {
        if ( isColumnSortingEnabled )
        {
            FWSortedTableModel sortedModel = (FWSortedTableModel)dataModel;
            int[] modelRowIndices = new int[ viewRowIndices.length ];
            for ( int i=0; i < viewRowIndices.length; i++ )
            {
                modelRowIndices[ i ] = sortedModel.getModelIndex(
                    viewRowIndices[ i ] );
            }
            return modelRowIndices;
        }
        else
        {
            return viewRowIndices;
        }
    }
    
    /**
     * Maps the index of the row in the model at modelRowIndex to the index of the
     * row in the view. Returns the index of the corresponding row in the
     * view. If modelRowIndex is less than zero, returns modelRowIndex.
     *
     * Mapping of the index is necessary if the view might display a different
     * ordering of rows due to sorting or filtering of rows. If there is no
     * different ordering between the view and the model then the view index is
     * returned directly.
     *
     * @params modelRowIndex - the index of the row in the model
     * @returns the index of the corresponding row in the view
     **/
    public int convertRowIndexToView( int modelRowIndex )
    {
        if ( isColumnSortingEnabled )
        {
            return ((FWSortedTableModel)dataModel).getViewIndex(
                modelRowIndex );
        }
        else
        {
            return modelRowIndex;
        }
    }

    /**
     * Returns the index of the visible columns where the given point in the
     * column margin area and shows the resizing cursor.
     */
    public FWTableColumn getResizingColumn( Point p )
    {
        int column = tableHeader.columnAtPoint( p );
        if (column == -1)
        {
            return null;
        }
        Rectangle r = tableHeader.getHeaderRect(column);
        r.grow(-3, 0);
        if (r.contains(p))
        {
            return null;
        }
        int midPoint = r.x + r.width/2;
        int columnIndex;
        if( tableHeader.getComponentOrientation().isLeftToRight() )
        {
            columnIndex = (p.x < midPoint) ? column - 1 : column;
        }
        else
        {
            columnIndex = (p.x < midPoint) ? column : column - 1;
        }
        if (columnIndex == -1)
        {
            return null;
        }

        return (FWTableColumn)tableHeader.getColumnModel().getColumn(columnIndex);
    }
    
    /**
     * Invoked when this table's <code>TableModel</code> generates
     * a <code>TableModelEvent</code>.
     * The <code>TableModelEvent</code> should be constructed in the
     * coordinate system of the model; the appropriate mapping to the
     * view coordinate system is performed by this <code>JTable</code>
     * when it receives the event.
     * <p>
     * Application code will not use these methods explicitly, they
     * are used internally by <code>JTable</code>.
     * <p>
     * Note that as of 1.3, this method clears the selection, if any.
     */
    public void tableChanged(TableModelEvent e) {
        if (e == null || e.getFirstRow() == TableModelEvent.HEADER_ROW)
        {
            super.tableChanged(e);
            return;
        }

		// The totalRowHeight calculated below will be incorrect if
		// there are variable height rows. Repaint the visible region,
		// but don't return as a revalidate may be necessary as well.
		//if (rowModel != null) {
		//    repaint();
		//}

        if (e.getType() == TableModelEvent.INSERT || e.getType() == TableModelEvent.DELETE)
        {
            super.tableChanged(e);
            return;
        }
        
        
       
        
        //System.out.println(e.getColumn() + " " + e.getFirstRow() + " " + e.getLastRow() + " " +
        //    e.getType() + " " + e.getSource());

        int modelColumn = e.getColumn();
        int start = e.getFirstRow();
        int end = e.getLastRow();

        Rectangle dirtyRegion;
        if (modelColumn == TableModelEvent.ALL_COLUMNS)
        {
            // 1 or more rows changed
            dirtyRegion = new Rectangle(0, start * getRowHeight(),
                                        getColumnModel().getTotalColumnWidth(), 0);
        }
        else
        {
            // A cell or column of cells has changed.
            // Unlike the rest of the methods in the JTable, the TableModelEvent
            // uses the coordinate system of the model instead of the view.
            // This is the only place in the JTable where this "reverse mapping"
            // is used.
            int column = convertColumnIndexToView(modelColumn);
            dirtyRegion = getCellRect(start, column, false);
        }

        // Now adjust the height of the dirty region according to the value of "end".
        // Check for Integer.MAX_VALUE as this will cause an overflow.
        if (end != Integer.MAX_VALUE)
        {
            dirtyRegion.height = (end-start+1)*getRowHeight();
            	repaint(dirtyRegion.x, dirtyRegion.y, dirtyRegion.width, dirtyRegion.height);
        }
        // In fact, if the end is Integer.MAX_VALUE we need to revalidate anyway
        // because the scrollbar may need repainting.
        else
        {
            // we remove the clearSelection call here to keep the current selection
            // active, it seems no bug is resulting of this.
            //clearSelection();
            resizeAndRepaint();
            //rowModel = null;
        }
    }

    private class MouseHandler extends MouseInputAdapter
    {
        public void mouseClicked( MouseEvent e )
        {
            FWTableColumn column = getResizingColumn( e.getPoint() );
            int clickCount = e.getClickCount();

            
            if ( clickCount >= 2 )
            {
                handleColumnResizeToFit( column );
            }
        }

        public void mouseReleased( MouseEvent e )
        {
            if ( e.isPopupTrigger() )
            {
                popupMenu((Component)e.getSource(), e.getX(), e.getY());
            }
        }

        public void mousePressed( MouseEvent e )
        {
            if ( e.isPopupTrigger() )
            {
                popupMenu((Component)e.getSource(), e.getX(), e.getY());
            }
        }

        /**
         * Handles double click event on the header for column resizing,
         * when click is between two columns and resize to fit is enabled.
         */
        private void handleColumnResizeToFit( FWTableColumn column )
        {
            if ( !isColumnResizeToFitEnabled )
            {
                return;
            }
            // handles double click event on the header with column resizing...

            if ( column != null )
            {
                column.sizeWidthToFitData( FWTable.this, dataModel );
            }
        }

        /**
         * Shows the popup menu if enabled.
         */
        private void popupMenu(Component source, int x, int y)
        {
            if ( headerPopup != null )
            {
                headerPopup.show(source, x, y);
            }
        }
    }

    private class PopupMenuActionHandler implements ActionListener
    {
        public void actionPerformed( ActionEvent e )
        {
            ColumnCheckBoxMenuItem item = (ColumnCheckBoxMenuItem)e.getSource();
            FWTableColumn column = item.getTableColumn( );
            if ( item.getState() )
            {
                ((FWTableColumnModel)columnModel).showColumn( column );
            }
            else
            {
                ((FWTableColumnModel)columnModel).hideColumn( column );
            }
        }
    }

    private class ColumnCheckBoxMenuItem extends JCheckBoxMenuItem
    {
        private FWTableColumn column;

        public ColumnCheckBoxMenuItem( FWTableColumn tableColumn )
        {
            super( (String)tableColumn.getHeaderValue(), tableColumn.isVisible() );
            column = tableColumn;
        }

        public FWTableColumn getTableColumn()
        {
            return column;
        }
    }
}