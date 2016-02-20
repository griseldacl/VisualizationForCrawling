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
 *  $Id: ViewAboutAction.java,v 1.4 2005/10/03 00:18:25 gregork Exp $
 */
package phex.gui.actions;


import java.awt.event.ActionEvent;

import phex.gui.common.GUIRegistry;
import phex.gui.dialogs.AboutDialog;
import phex.utils.Localizer;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;


/**
*	The ActionViewAbout class performs the operation of displaying the About
*	dialog box describing this program.
*/
public class ViewAboutAction extends FWAction
{
    public ViewAboutAction( )
    {
        super( Localizer.getString( "AboutPhex_AboutPhex" ),
            GUIRegistry.getInstance().getIconFactory().getIcon( "About16" ),
            Localizer.getString( "AboutPhex_TTTAboutPhex" ),
            new Integer( Localizer.getChar( "AboutPhex_AboutPhexMnemonic" ) ),
            null );
    }

    public void actionPerformed(ActionEvent event)
    {        
        try
        {
            AboutDialog dialog = new AboutDialog();
            dialog.setVisible(true);
        }
        catch ( Throwable th )
        {
            NLogger.error( NLoggerNames.USER_INTERFACE, th, th );
        }        
    }

    public void refreshActionState()
    {
    }
}