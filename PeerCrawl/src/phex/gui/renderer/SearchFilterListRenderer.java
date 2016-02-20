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

import java.awt.*;
import javax.swing.*;
import phex.query.*;

public class SearchFilterListRenderer extends FWListCellRenderer
{
    public Component getListCellRendererComponent( JList list,
        Object value, int index, boolean isSelected, boolean cellHasFocus )
    {
        super.getListCellRendererComponent( list, value, index,
            isSelected, cellHasFocus );
        if ( value instanceof SearchFilter )
        {
            SearchFilter filter = (SearchFilter) value;
            setText( filter.getName() );
        }
        return this;
    }
}