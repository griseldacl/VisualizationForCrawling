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
 *  $Id: TreeTableModelAdapter.java,v 1.7 2005/10/03 00:18:25 gregork Exp $
 */
package phex.gui.common;

import java.awt.EventQueue;
import java.util.Comparator;

import javax.swing.JTree;
import javax.swing.event.*;
import javax.swing.tree.TreePath;

import phex.gui.common.table.FWTableModel;
import phex.gui.models.ISortableModel;
import phex.gui.models.TreeTableModel;

/**
 * This is a wrapper class takes a TreeTableModel and implements 
 * the table model interface. The implementation is trivial, with 
 * all of the event dispatching support provided by the superclass: 
 * the AbstractTableModel. 
 */
public class TreeTableModelAdapter extends FWTableModel implements ISortableModel
{
    JTree tree;
    TreeTableModel treeTableModel;

    public TreeTableModelAdapter(TreeTableModel treeTableModel, JTree tree)
    {
        super(null, null);
        this.tree = tree;
        this.treeTableModel = treeTableModel;

        tree.addTreeExpansionListener(new TreeExpansionListener()
        {
            // Don't use fireTableRowsInserted() here; the selection model
            // would get updated twice. 
            public void treeExpanded(TreeExpansionEvent event)
            {
                fireTableDataChanged();
            }
            public void treeCollapsed(TreeExpansionEvent event)
            {
                fireTableDataChanged();
            }
        });

        // Install a TreeModelListener that can update the table when
        // tree changes. We use delayedFireTableDataChanged as we can
        // not be guaranteed the tree will have finished processing
        // the event before us.
        treeTableModel.addTreeModelListener(new TreeModelListener()
        {
            public void treeNodesChanged(TreeModelEvent e)
            {
                delayedFireTableDataChanged();
            }

            public void treeNodesInserted(TreeModelEvent e)
            {
                delayedFireTableDataChanged();
            }

            public void treeNodesRemoved(TreeModelEvent e)
            {
                delayedFireTableDataChanged();
            }

            public void treeStructureChanged(TreeModelEvent e)
            {
                delayedFireTableDataChanged();
            }
        });
    }

    // Wrappers, implementing TableModel interface. 

    public int getColumnCount()
    {
        return treeTableModel.getColumnCount();
    }

    public String getColumnName(int column)
    {
        return treeTableModel.getColumnName(column);
    }

    public Class getColumnClass(int column)
    {
        return treeTableModel.getColumnClass(column);
    }

    public int getRowCount()
    {
        return tree.getRowCount();
    }

    protected Object nodeForRow(int row)
    {
        TreePath treePath = tree.getPathForRow(row);
        return treePath.getLastPathComponent();
    }

    public Object getValueAt(int row, int column)
    {
        return treeTableModel.getValueAt(nodeForRow(row), column);
    }

    public boolean isCellEditable(int row, int column)
    {
        return treeTableModel.isCellEditable(nodeForRow(row), column);
    }

    public void setValueAt(Object value, int row, int column)
    {
        treeTableModel.setValueAt(value, nodeForRow(row), column);
    }

    /**
      * Returns the most comparator that is used for sorting of the cell values
      * in the column. This is used by the FWSortedTableModel to perform the
      * sorting. If not overwritten the method returns null causing the
      * FWSortedTableModel to use a NaturalComparator. It expects all Objects that
      * are returned from getComparableValueAt() to implement the Comparable interface.
      *
      */
    public Comparator getColumnComparator(int column)
    {
        return treeTableModel.getColumnComparator(column);
    }

    /**
     * Returns an attribute value that is used for comparing on sorting
     * for the cell at row and column. If not overwritten the call is forwarded
     * to getValueAt().
     * The returned Object is compared via the Comparator returned from
     * getColumnComparator(). If no comparator is specified the returned Object
     * must implement the Comparable interface.
     */
    public Object getComparableValueAt(int row, int column)
    {
        return treeTableModel.getComparableValueAt(nodeForRow(row), column);
    }

    /**
     * Maps the unique column id to the model index. This needs to be done to
     * be able identify columns and there index after changes in Phex releases.
     * The unique column id is not allowed to ever change over Phex releases. It
     * is used when serializing column information. The column id is containd in
     * the identifier field of the TableColumn.
     */
    public int mapColumnIDToModelIndex(int columnId)
    {
        return treeTableModel.mapColumnIDToModelIndex(columnId);
    }

    /**
     * Indicates if a column is hideable.
     */
    public boolean isColumnHideable(int columnId)
    {
        return treeTableModel.isColumnHideable(columnId);
    }

    public void fireTableDataChanged()
    {
        super.fireTableDataChanged();
    }

    /**
     * Invokes fireTableDataChanged after all the pending events have been
     * processed. EventQueue.invokeLater is used to handle this.
     */
    protected void delayedFireTableDataChanged()
    {
        EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                fireTableDataChanged();
            }
        });
    }

    /**
     * @see phex.gui.models.ISortableModel#getSortByColumn()
     */
    public int getSortByColumn()
    {
        if ( treeTableModel instanceof ISortableModel )
        {
            return ((ISortableModel)treeTableModel).getSortByColumn(); 
        }
        else
        {
            throw new UnsupportedOperationException( "TreeTableModel not an ISortableModel." );
        }
    }

    /* (non-Javadoc)
     * @see phex.gui.models.ISortableModel#isSortedAscending()
     */
    public boolean isSortedAscending()
    {
        if ( treeTableModel instanceof ISortableModel )
        {
            return ((ISortableModel)treeTableModel).isSortedAscending(); 
        }
        else
        {
            throw new UnsupportedOperationException( "TreeTableModel not an ISortableModel." );
        }
    }

    /* (non-Javadoc)
     * @see phex.gui.models.ISortableModel#sortByColumn(int, boolean)
     */
    public void sortByColumn(int column, boolean isSortedAscending)
    {
        if ( treeTableModel instanceof ISortableModel )
        {
            ((ISortableModel)treeTableModel).sortByColumn( column, isSortedAscending);
        }
        else
        {
            throw new UnsupportedOperationException( "TreeTableModel not an ISortableModel." );
        }
    }
}
