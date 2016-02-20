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

import javax.swing.table.AbstractTableModel;

import phex.host.Host;
import phex.msg.QueryMsg;
import phex.query.*;
import phex.utils.Localizer;

public class QueryHistoryMonitorTableModel extends AbstractTableModel
{
    private static final int numColRoutedFrom = 0;
    private static final int numColSearchText = 1;
    private static final int numColHopsTtl = 2;

    private static final String[] tableColumns =
    {
    	Localizer.getString( "SearchMonitorTab_RoutedFrom" ),
    	Localizer.getString( "SearchMonitorTab_SearchText" ),
    	Localizer.getString( "SearchMonitorTab_HopsTtl" )
    };

    private QueryHistoryMonitor history;

    public QueryHistoryMonitorTableModel()
    {
        history = QueryManager.getInstance().getQueryHistoryMonitor();
    }

    public String getColumnName(int col)
    {
        return tableColumns[ col ];
    }

    public int getColumnCount()
    {
        return tableColumns.length;
    }

    public int getRowCount()
    {
        return history.getHistorySize();
    }

    public Object getValueAt(int row, int col)
    {
        QueryMsg searchQuery = history.getSearchQueryAt( row );
        if ( searchQuery == null )
        {
            fireTableRowsDeleted( row, row );
            return null;
        }

        switch (col)
        {
            case numColRoutedFrom:
                Host fromHost = searchQuery.getHeader().getFromHost();
                if ( fromHost == null )
                {
                    return "<Unknown>";
                }
                return fromHost.getHostAddress().getFullHostName();

            case numColSearchText:
                return searchQuery.getSearchString();

            case numColHopsTtl:
                int hops = searchQuery.getHeader().getHopsTaken();
                // remaining ttl + hops
                int ttl = hops + searchQuery.getHeader().getTTL();
                return hops + " / " + ttl;
        }
        return "";
    }

    public Class getColumnClass(int col)
    {
        return String.class;
    }
}