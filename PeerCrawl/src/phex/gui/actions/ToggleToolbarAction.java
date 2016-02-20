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
 *  $Id: ToggleToolbarAction.java,v 1.3 2005/10/03 00:18:25 gregork Exp $
 */
package phex.gui.actions;

import java.awt.event.ActionEvent;

import phex.gui.common.GUIRegistry;
import phex.utils.Localizer;

/**
 * 
 */
public class ToggleToolbarAction extends FWToggleAction
{
    /**
     *
     */
    public ToggleToolbarAction( boolean isToolbarVisible )
    {
        super( Localizer.getString( "ToggleToolbarAction" ),
               null, null, null,  null, null );
        setSelected( isToolbarVisible );
        updateTooltip();
    }
    
    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    {
        boolean state = !isSelected();
        setSelected( state );
        GUIRegistry.getInstance().getMainFrame().setToolbarVisible( state );
        updateTooltip();
    }
    
    private void updateTooltip()
    {
        if ( isSelected() )
        {
            setToolTipText( Localizer.getString( "TTTToggleToolbarActionHide" ) );
        }
        else
        {
            setToolTipText( Localizer.getString( "TTTToggleToolbarActionShow" ) );
        }
    }
    
    /**
     * @see phex.gui.actions.FWAction#refreshActionState()
     */
    public void refreshActionState()
    {// global actions are not refreshed
    }
}
