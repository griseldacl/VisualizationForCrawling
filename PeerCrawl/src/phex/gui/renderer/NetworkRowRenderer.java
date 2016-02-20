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
 *  $Id: NetworkRowRenderer.java,v 1.5 2005/11/13 10:16:59 gregork Exp $
 */
package phex.gui.renderer;

import java.awt.*;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import phex.gui.common.table.FWTable;
import phex.host.*;

/**
 * 
 */
public class NetworkRowRenderer implements TableCellRenderer
{
    private static final Color FAILED_COLOR = Color.gray;
    private static final Color CONNECTING_COLOR = new Color( 0x7F, 0x00, 0x00 );
    private static final Color CONNECTED_COLOR = new Color( 0x00, 0x7F, 0x00 );
    
    private NetworkHostsContainer hostsContainer;
    
    public NetworkRowRenderer( )
    {
        HostManager hostMgr = HostManager.getInstance();
        hostsContainer = hostMgr.getNetworkHostsContainer();
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column)
    {
        TableCellRenderer renderer = table.getDefaultRenderer(
            table.getColumnClass( column ) );
        Component comp = renderer.getTableCellRendererComponent( table, value,
            isSelected, hasFocus, row, column );
        FWTable fwTable = (FWTable) table;        
        comp.setForeground( table.getSelectionForeground() );

        if (row < hostsContainer.getNetworkHostCount() )
        {
            int modelRow = fwTable.convertRowIndexToModel( row );            
            Host host = (Host)hostsContainer.getNetworkHostAt( modelRow );
            if ( host == null )
            {
                return comp;
            }
            switch (host.getStatus())
            {
                case HostConstants.STATUS_HOST_NOT_CONNECTED:
                    break;

                case HostConstants.STATUS_HOST_ERROR:
                case HostConstants.STATUS_HOST_DISCONNECTED:
                    comp.setForeground( FAILED_COLOR );
                    break;

                case HostConstants.STATUS_HOST_CONNECTING:
                case HostConstants.STATUS_HOST_ACCEPTING:
                    comp.setForeground( CONNECTING_COLOR );
                    break;

                case HostConstants.STATUS_HOST_CONNECTED:
                    comp.setForeground( CONNECTED_COLOR );
                    break;
            }
        }
        return comp;
    }    
}
