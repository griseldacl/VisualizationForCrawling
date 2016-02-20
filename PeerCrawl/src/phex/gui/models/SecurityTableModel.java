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
package phex.gui.models;

import java.util.*;

import javax.swing.event.*;

import phex.event.*;
import phex.gui.common.*;
import phex.gui.common.table.*;
import phex.gui.comparator.*;
import phex.gui.renderer.*;
import phex.security.*;
import phex.utils.*;

public class SecurityTableModel extends FWSortableTableModel implements SecurityRulesChangeListener
{
    /**
     * The unique column id is not allowed to ever change over Phex releases. It
     * is used when serializing column information. The column id is containd in
     * the identifier field of the TableColumn.
     */
    private static final int ADDRESS_COLUMN_ID = 1001;
    private static final int TYPE_COLUMN_ID = 1002;
    private static final int EXPIRES_COLUMN_ID = 1003;
    private static final int TRIGGER_COUNT_COLUMN_ID = 1004;
    private static final int DESCRIPTION_COLUMN_ID = 1005;

    private static final int ADDRESS_MODEL_INDEX = 0;
    private static final int TYPE_MODEL_INDEX = 1;
    private static final int EXPIRES_MODEL_INDEX = 2;
    private static final int TRIGGER_COUNT_MODEL_INDEX = 3;
    private static final int DESCRIPTION_MODEL_INDEX = 4;

    private static String[] tableColumns;
    private static Class[] tableClasses;

    /**
     * Initialize super tableColumns field
     */
    static
    {
        tableColumns = new String[]
        {
            Localizer.getString( "Address" ),
            Localizer.getString( "Type" ),
            Localizer.getString( "Expires" ),
            Localizer.getString( "TriggerCount" ),
            Localizer.getString( "Description" )
        };

        tableClasses = new Class[]
        {
            String.class,
            String.class,
            DateCellRenderer.class,
            Integer.class,
            String.class,
        };
    }

    private PhexSecurityManager securityMgr;

    public SecurityTableModel()
    {
        super( tableColumns, tableClasses );
        securityMgr = PhexSecurityManager.getInstance();
        securityMgr.addUploadFilesChangeListener( this );
    }

    public int getRowCount()
    {
        return securityMgr.getIPAccessRuleCount();
    }

    public Object getValueAt( int row, int column )
    {
        IPAccessRule rule = securityMgr.getIPAccessRule( row );
        if ( rule == null )
        {
            fireTableRowsDeleted( row, row );
            return "";
        }

        switch( column )
        {
            case ADDRESS_MODEL_INDEX:
                return rule.getAddressString();
            case TYPE_MODEL_INDEX:
                if ( rule.isDenyingRule() )
                {
                    return Localizer.getString( "Deny" );
                }
                else
                {
                    return Localizer.getString( "Accept" );
                }
            case EXPIRES_MODEL_INDEX:
                return rule.getExpiryDate();
            case TRIGGER_COUNT_MODEL_INDEX:
                return rule.getTriggerCountObject();
            case DESCRIPTION_MODEL_INDEX:
                return rule.getDescription();
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
            case ADDRESS_MODEL_INDEX:
                return new IPComparator();
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
        switch ( column )
        {
            case ADDRESS_MODEL_INDEX:
                IPAccessRule rule = securityMgr.getIPAccessRule( row );
                if ( rule == null )
                {
                    return new Long( Long.MIN_VALUE );
                }
                return rule.getHostIP();
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
            case ADDRESS_COLUMN_ID:
                return ADDRESS_MODEL_INDEX;
            case TYPE_COLUMN_ID:
                return TYPE_MODEL_INDEX;
            case EXPIRES_COLUMN_ID:
                return EXPIRES_MODEL_INDEX;
            case TRIGGER_COUNT_COLUMN_ID:
                return TRIGGER_COUNT_MODEL_INDEX;
            case DESCRIPTION_COLUMN_ID:
                return DESCRIPTION_MODEL_INDEX;
            default:
                return -1;
        }
    }

    /**
     * Indicates if a column is hideable.
     */
    public boolean isColumnHideable( int columnID )
    {
        if ( columnID == ADDRESS_COLUMN_ID )
        {
            return false;
        }
        return true;
    }

    public static int[] getColumnIdArray()
    {
        int[] columnIds = new int[]
        {
            ADDRESS_COLUMN_ID,
            TYPE_COLUMN_ID,
            EXPIRES_COLUMN_ID,
            TRIGGER_COUNT_COLUMN_ID,
            DESCRIPTION_COLUMN_ID,
        };
        return columnIds;
    }

    ///////////////////////// START event handling ////////////////////////////


    private LazyEventQueue lazyEventQueue = GUIRegistry.getInstance().getLazyEventQueue();

    /**
     * Called if a security rule changed.
     */
    public void securityRuleChanged( int position )
    {
        lazyEventQueue.addTableModelEvent(
            new TableModelEvent( this, position, position,
            TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE ) );
    }

    /**
     * Called if a security rule was added.
     */
    public void securityRuleAdded( int position )
    {
        fireTableChanged(
            new TableModelEvent( this, position, position,
            TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT ) );
    }

    /**
     * Called if a security rule was removed.
     */
    public void securityRuleRemoved( int position )
    {
        fireTableChanged(
            new TableModelEvent(this, position, position,
            TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE ) );
    }

    ///////////////////////// END event handling ////////////////////////////
}