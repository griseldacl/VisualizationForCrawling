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

import javax.swing.table.AbstractTableModel;


/**
 * This is a framework class that is used to support the creation of table
 * models. Currently the implementation is empty but in the future you will
 * find here support for a improved sorting and other
 * usefull table model functionalities.
 */
public abstract class FWTableModel extends AbstractTableModel
{
    protected String[] tableColumns;
    protected Class[] tableClasses;

    /**
     * Creates a new FWTableModel.
     */
    public FWTableModel( String[] theTableColumns, Class[] theTableClasses )
    {
        tableColumns = theTableColumns;
        tableClasses = theTableClasses;
    }

    public String getColumnName(int column)
    {
        return tableColumns[ column ];
    }

    public int getColumnCount()
    {
        return tableColumns.length;
    }

    public Class getColumnClass( int column )
    {
        Class clazz = tableClasses[ column ];
        if ( clazz == null )
        {
            clazz = String.class;
        }
        return clazz;
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
        throw new UnsupportedOperationException( "Needs to be overloaded by the model." );
    }

    /**
     * Indicates if a column is hideable.
     */
    public boolean isColumnHideable( int columnId )
    {
        return true;
    }
}