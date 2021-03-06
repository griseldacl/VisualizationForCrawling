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
package phex.gui.renderer;

import java.awt.Component;

import javax.swing.JTable;

import phex.common.TransferDataProvider;
import phex.utils.StringUtils;

public class TransferSizeCellRenderer extends FWTableCellRenderer
{
    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column)
    {
        super.getTableCellRendererComponent(table, value,
            isSelected, hasFocus, row, column);

        if ( value instanceof TransferDataProvider )
        {
            TransferDataProvider provider = (TransferDataProvider) value;

            if ( isSelected )
            {// in case of selection always use default color...
                setForeground( table.getSelectionForeground() );
            }
            else
            {
                CellColorHandler.applyCellColor( provider, this );
            }

            long transferredSize = provider.getTransferredDataSize();
            long transferSize = provider.getTransferDataSize();
            StringBuffer buffer = new StringBuffer();
            buffer.append( StringUtils.FILE_LENGTH_FORMAT.format(transferredSize) );
            if ( transferSize > -1 )
            {
                buffer.append( " / " );
                buffer.append( StringUtils.FILE_LENGTH_FORMAT.format(transferSize) );
            }
            setText( buffer.toString() );
        }
        return this;
    }
}