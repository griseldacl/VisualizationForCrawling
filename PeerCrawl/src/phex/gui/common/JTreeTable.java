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
 *  $Id: JTreeTable.java,v 1.7 2005/10/03 00:18:25 gregork Exp $
 */
package phex.gui.common;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import phex.gui.common.table.FWTable;
import phex.gui.models.TreeTableModel;

/**
 * This example shows how to create a simple JTreeTable component, 
 * by using a JTree as a renderer (and editor) for the cells in a 
 * particular column in the JTable.  
 */
public class JTreeTable extends FWTable
{
    /** A subclass of JTree. */
    protected TreeTableCellRenderer tree;
    private TableColumn treeTableColumn;

    public JTreeTable(TreeTableModel treeTableModel )
    {
        // Install a tableModel representing the visible rows in the tree. 
        super( null, null );
        // Create the tree. It will be used as a renderer and editor. 
        tree = new TreeTableCellRenderer(treeTableModel);
        // Install the tree editor renderer and editor. 
        setDefaultRenderer(TreeTableModel.class, tree);
        tree.setRowHeight(getRowHeight());
        
        super.setModel( new TreeTableModelAdapter(treeTableModel, tree) );
        
        // Forces the JTable and JTree to share their row selection models. 
        ListToTreeSelectionModelWrapper selectionWrapper = new ListToTreeSelectionModelWrapper();
        tree.setSelectionModel(selectionWrapper);
        setSelectionModel(selectionWrapper.getListSelectionModel()); 
        
        //find the column that is the treetable
        refreshTreeTableColumn();
        DefaultTreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer() {
            // allows the display of ... on the tree column by substracting
            // x from the column width.
            public void setBounds(int x, int y, int w, int h) {
                super.setBounds(x, y, treeTableColumn.getWidth() - x, h);
            }
        };
        defaultRenderer.setLeafIcon( null );
        defaultRenderer.setOpenIcon( null );
        defaultRenderer.setClosedIcon( null );
        defaultRenderer.setOpaque(false);
        defaultRenderer.setBackground(null);
        defaultRenderer.setBackgroundNonSelectionColor(null);
        tree.setCellRenderer(defaultRenderer);
        setDefaultEditor(TreeTableModel.class, new TreeTableCellEditor());  
        tree.setRootVisible(false); 
        tree.setShowsRootHandles(true);     
        tree.putClientProperty("JTree.lineStyle", "Angled");
        tree.addMouseListener(new UpdateTreeTableSelectionMouseListener());

        this.setShowGrid(false);
        this.setColumnSelectionAllowed(false);
        // No intercell spacing
        setIntercellSpacing(new Dimension(0, 0));
    }
    
    public void setColumnModel( TableColumnModel model )
    {
        super.setColumnModel( model );
        refreshTreeTableColumn();
    }
    
    public Object getNodeOfRow(int row)
    {
        TreePath treePath = tree.getPathForRow(row);
        if ( treePath == null )
        {
            return null;
        }
        return treePath.getLastPathComponent();         
    }
    
    /**
     * Called from ResultPanel columnsChanged() and in constructor
     *
     * Necessary to reassign the treeTableColumn variable used 
     * by the DefaultTreeCellRenderer created in constructor
     */
    public void refreshTreeTableColumn() {
        //find the column that is the treetable
        for( int i = 0; i < getColumnCount(); i++ ) {
            if ( getColumnClass(i) == TreeTableModel.class ) {
                treeTableColumn = getColumnModel().getColumn(i);
                break;
            }
        }
    }

    /* Workaround for BasicTableUI anomaly. Make sure the UI never tries to 
     * paint the editor. The UI currently uses different techniques to 
     * paint the renderers and editors and overriding setBounds() below 
     * is not the right thing to do for an editor. Returning -1 for the 
     * editing row in this case, ensures the editor is never painted. 
     */
    public int getEditingRow()
    {
        return (getColumnClass(editingColumn) == TreeTableModel.class)
            ? -1
            : editingRow;
    }

    /**
     * Overridden to pass the new rowHeight to the tree.
     */
    public void setRowHeight(int rowHeight)
    {
        super.setRowHeight(rowHeight);
        if (tree != null && tree.getRowHeight() != rowHeight)
        {
            tree.setRowHeight(getRowHeight());
        }
    }

    /**
     * Returns the tree that is being shared between the model.
     */
    public JTree getTree()
    {
        return tree;
    }

    private final class UpdateTreeTableSelectionMouseListener extends MouseAdapter
    {
        public void mousePressed(MouseEvent e)
        {
            TreePath path = tree.getPathForLocation(e.getX(), e.getY());
            if (path != null)
            {
                if (tree.isPathSelected(path))
                    tree.removeSelectionPath(path);
                else
                    tree.addSelectionPath(path);
            }
        }
    }

    /**
     * A TreeCellRenderer that displays a JTree.
     */
    public class TreeTableCellRenderer
        extends JTree
        implements TableCellRenderer
    {
        /** Last table/tree row asked to renderer. */
        protected int visibleRow;

        public TreeTableCellRenderer(TreeModel model)
        {
            super(model);
        }

        /**
         * updateUI is overridden to set the colors of the Tree's renderer
         * to match that of the table.
         */
        public void updateUI()
        {
            super.updateUI();
            // Make the tree's cell renderer use the table's cell selection
            // colors. 
            TreeCellRenderer tcr = getCellRenderer();
            if (tcr instanceof DefaultTreeCellRenderer)
            {
                DefaultTreeCellRenderer dtcr = ((DefaultTreeCellRenderer)tcr);
                // For 1.1 uncomment this, 1.2 has a bug that will cause an
                // exception to be thrown if the border selection color is
                // null.
                dtcr.setBorderSelectionColor(null);
                dtcr.setTextSelectionColor(
                    UIManager.getColor("Table.selectionForeground"));
                dtcr.setBackgroundSelectionColor(
                    UIManager.getColor("Table.selectionBackground"));
            }
        }

        /**
         * Sets the row height of the tree, and forwards the row height to
         * the table.
         */
        public void setRowHeight(int rowHeight)
        {
            if (rowHeight > 0)
            {
                super.setRowHeight(rowHeight);
                if (JTreeTable.this != null
                    && JTreeTable.this.getRowHeight() != rowHeight)
                {
                    JTreeTable.this.setRowHeight(getRowHeight());
                }
            }
        }

        /**
         * This is overridden to set the height to match that of the JTable.
         */
        public void setBounds(int x, int y, int w, int h)
        {
            super.setBounds(x, 0, w, JTreeTable.this.getHeight());
        }

        /**
         * Sublcassed to translate the graphics such that the last visible
         * row will be drawn at 0,0.
         */
        public void paint(Graphics g)
        {
            g.translate(0, -visibleRow * getRowHeight());
            super.paint(g);
        }

        /**
         * TreeCellRenderer method. Overridden to update the visible row.
         */
        public Component getTableCellRendererComponent( JTable table,
            Object value, boolean isSelected, boolean hasFocus, int row,
            int column)
        {
            if (isSelected)
                setBackground(table.getSelectionBackground());
            else
                setBackground(table.getBackground());

            visibleRow = row;
            return this;
        }
    }

    /**
     * TreeTableCellEditor implementation. Component returned is the
     * JTree.
     */
    public class TreeTableCellEditor extends AbstractCellEditor
        implements TableCellEditor
    {
        public Component getTableCellEditorComponent( JTable table, Object value,
            boolean isSelected, int r, int c )
        {
            return tree;
        }

        /**
         * Overridden to return false, and if the event is a mouse event
         * it is forwarded to the tree.<p>
         * The behavior for this is debatable, and should really be offered
         * as a property. By returning false, all keyboard actions are
         * implemented in terms of the table. By returning true, the
         * tree would get a chance to do something with the keyboard
         * events. For the most part this is ok. But for certain keys,
         * such as left/right, the tree will expand/collapse where as
         * the table focus should really move to a different column. Page
         * up/down should also be implemented in terms of the table.
         * By returning false this also has the added benefit that clicking
         * outside of the bounds of the tree node, but still in the tree
         * column will select the row, whereas if this returned true
         * that wouldn't be the case.
         * <p>By returning false we are also enforcing the policy that
         * the tree will never be editable (at least by a key sequence).
         */
        public boolean isCellEditable(EventObject e)
        {
            if (e instanceof MouseEvent)
            {
                for (int counter = getColumnCount() - 1;
                    counter >= 0;
                    counter--)
                {
                    if (getColumnClass(counter) == TreeTableModel.class)
                    {
                        MouseEvent me = (MouseEvent)e;
                        MouseEvent newME = new MouseEvent( tree,
                            me.getID(), me.getWhen(), me.getModifiers(),
                            me.getX() - getCellRect(0, counter, true).x,
                            me.getY(), me.getClickCount(), me.isPopupTrigger());
                        tree.dispatchEvent(newME);
                        break;
                    }
                }
            }
            return false;
        }

        public Object getCellEditorValue()
        {
            // Not used...
            return null;
        }
    }

    /**
     * ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel
     * to listen for changes in the ListSelectionModel it maintains. Once
     * a change in the ListSelectionModel happens, the paths are updated
     * in the DefaultTreeSelectionModel.
     */
    class ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel
    {
        /** Set to true when we are updating the ListSelectionModel. */
        protected boolean updatingListSelectionModel;

        public ListToTreeSelectionModelWrapper()
        {
            super();
            getListSelectionModel().addListSelectionListener(
                createListSelectionListener());
        }

        /**
         * Returns the list selection model. ListToTreeSelectionModelWrapper
         * listens for changes to this model and updates the selected paths
         * accordingly.
         */
        ListSelectionModel getListSelectionModel()
        {
            return listSelectionModel;
        }

        /**
         * This is overridden to set <code>updatingListSelectionModel</code>
         * and message super. This is the only place DefaultTreeSelectionModel
         * alters the ListSelectionModel.
         */
        public void resetRowSelection()
        {
            if (!updatingListSelectionModel)
            {
                updatingListSelectionModel = true;
                try
                {
                    super.resetRowSelection();
                }
                finally
                {
                    updatingListSelectionModel = false;
                }
            }
            // Notice how we don't message super if
            // updatingListSelectionModel is true. If
            // updatingListSelectionModel is true, it implies the
            // ListSelectionModel has already been updated and the
            // paths are the only thing that needs to be updated.
        }

        /**
         * Creates and returns an instance of ListSelectionHandler.
         */
        protected ListSelectionListener createListSelectionListener()
        {
            return new ListSelectionHandler();
        }

        /**
         * If <code>updatingListSelectionModel</code> is false, this will
         * reset the selected paths from the selected rows in the list
         * selection model.
         */
        protected void updateSelectedPathsFromSelectedRows()
        {
            if (!updatingListSelectionModel)
            {
                updatingListSelectionModel = true;
                try
                {
                    // This is way expensive, ListSelectionModel needs an
                    // enumerator for iterating.
                    int min = listSelectionModel.getMinSelectionIndex();
                    int max = listSelectionModel.getMaxSelectionIndex();

                    clearSelection();
                    if (min != -1 && max != -1)
                    {
                        for (int counter = min; counter <= max; counter++)
                        {
                            if (listSelectionModel.isSelectedIndex(counter))
                            {
                                TreePath selPath = tree.getPathForRow(counter);

                                if (selPath != null)
                                {
                                    addSelectionPath(selPath);
                                }
                            }
                        }
                    }
                }
                finally
                {
                    updatingListSelectionModel = false;
                }
            }
        }

        /**
         * Class responsible for calling updateSelectedPathsFromSelectedRows
         * when the selection of the list changse.
         */
        class ListSelectionHandler implements ListSelectionListener
        {
            public void valueChanged(ListSelectionEvent e)
            {
                updateSelectedPathsFromSelectedRows();
            }
        }
    }
}
