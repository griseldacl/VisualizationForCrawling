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
 *  $Id: FileSizeCellRenderer.java,v 1.6 2005/10/03 00:18:26 gregork Exp $
 */
package phex.gui.renderer;

import javax.swing.JLabel;

import phex.utils.StringUtils;

/**
 * <p>Copyright: Copyright (c) 2003 Gregor Koukkoullis</p>
 * <p>@author Gregor Koukkoullis</p>
 * <p> </p>
 * <p> </p>
 *  not attributable
 *
 */

public class FileSizeCellRenderer extends FWTableCellRenderer
{
    public FileSizeCellRenderer()
    {
        super();
        setHorizontalAlignment( JLabel.RIGHT );
    }

    /**
     * Sets the string for the cell being rendered to <code>value</code>.
     *
     * @param value  the string value for this cell; if value is
     *		<code>null</code> it sets the text value to an empty string
     * @see JLabel#setText
     *
     */
    protected void setValue( Object value )
    {
        if ( value instanceof Number )
        {
            setText( StringUtils.FILE_LENGTH_FORMAT.format( value ) );
        }
    }
}