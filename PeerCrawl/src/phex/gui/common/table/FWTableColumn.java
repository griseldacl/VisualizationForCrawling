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

import javax.swing.*;
import javax.swing.table.*;
import javax.xml.bind.*;

import phex.xml.*;


/**
 * There is currently no property change support...
 * Unfortunately TableColumn does not offer the property change support for
 * subclasses. If change support is neccessary it needs to be integrated separatly.
 */
public class FWTableColumn extends TableColumn
{
    /**
     * Indicates if this column is currently shown.
     */
    private boolean isVisible;

    /**
     * Indicates if this column can be hide in the view.
     */
    private boolean isHideable;

    private boolean isSortingAscending;

    public FWTableColumn( int modelIndex, Object headerValue, Integer identifier )
    {
        super( modelIndex );
        setHeaderValue( headerValue );
        setIdentifier( identifier );
        isVisible = true;
        isHideable = true;
        isSortingAscending = false;
    }

    /**
     * Indicates if this column is currently shown.
     */
    public boolean isVisible()
    {
        return isVisible;
    }

    /**
     * Indicates if this column can be hide in the view.
     */
    public boolean isHideable()
    {
        return isHideable;
    }

    /**
     * Makes the column to be hideable. Dont call this after initializing and using
     * the FWTableColumnModel... since we have no
     * event model for this currently... Unfortunately TableColumn does not offer
     * the property change support for subclasses.
     */
    public void setHideable( boolean state )
    {
        isHideable = state;
    }

    /**
     * Sets the column as hidden.
     * This should only be called by the FWTableColumnModel since we have no
     * event model for this currently... Unfortunately TableColumn does not offer
     * the property change support for subclasses.
     */
    public void setVisible( boolean state )
    {
        isVisible = state;
    }

    /**
     * Reverses the sorting order and returns if the new sorting order is ascending.
     */
    public boolean reverseSortingOrder()
    {
        isSortingAscending = !isSortingAscending;
        return isSortingAscending;
    }

    public void sizeWidthToFitData( JTable table, TableModel model )
    {
        TableCellRenderer aCellRenderer = cellRenderer;
        if ( cellRenderer == null )
        {
            aCellRenderer = table.getDefaultRenderer( model.getColumnClass( modelIndex ) );
        }
        int maxWidth = 0;
        Component component;
        int rowCount = model.getRowCount();
        for ( int i = 0; i < rowCount; i++ )
        {
            Object value = model.getValueAt( i, modelIndex );
            component = aCellRenderer.getTableCellRendererComponent( table, value, false,
                false, i, modelIndex );
            maxWidth = Math.max( component.getPreferredSize().width + 4, maxWidth );
        }

        setPreferredWidth( maxWidth );
    }

    public XJBGUITableColumn createXJBGUITableColumn()
        throws JAXBException
    {
        ObjectFactory objFactory = new ObjectFactory();
        XJBGUITableColumn xjbColumn = objFactory.createXJBGUITableColumn();
        xjbColumn.setColumnID( ((Integer)getIdentifier()).intValue() );
        xjbColumn.setVisible( isVisible );
        xjbColumn.setWidth( getWidth() );
        return xjbColumn;
    }
}