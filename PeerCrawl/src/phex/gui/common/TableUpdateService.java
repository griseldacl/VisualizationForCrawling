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
package phex.gui.common;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

/**
 * All here registered tables will be updated every 2 seconds.
 */
public class TableUpdateService
{
    private HashSet tableSet;

    public TableUpdateService()
    {
        tableSet = new HashSet();
        
        ActionListener updateTablesAction = new ActionListener()
        {
            public void actionPerformed( ActionEvent e )
            {
                updateTables();
            }
        };
        Timer timer = new Timer( 2000, updateTablesAction );
        timer.start();
    }

    public synchronized void registerTable( JTable table )
    {
        tableSet.add( table );
    }

    public synchronized void unregisterTable( JTable table )
    {
        tableSet.remove( table );
    }

    public synchronized void updateTables()
    {
        Iterator iterator = tableSet.iterator();
        while ( iterator.hasNext() )
        {
            JTable table = (JTable) iterator.next();
            TableModel model = table.getModel();
            if ( model.getRowCount() > 0 )
            {
                if ( model instanceof AbstractTableModel )
                {
                    ((AbstractTableModel)model).fireTableRowsUpdated(
                        0, model.getRowCount() );
                }
            }
        }
    }
}