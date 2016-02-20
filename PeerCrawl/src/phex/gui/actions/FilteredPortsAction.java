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

package phex.gui.actions;

import java.util.*;

import java.awt.event.*;

import phex.common.*;
import phex.dialogues.*;

import phex.gui.common.*;
import phex.utils.*;

public class FilteredPortsAction extends FWAction
{
    public FilteredPortsAction( )
    {
        super( Localizer.getString( "FilteredPorts" ), null,
            Localizer.getString( "TTTFilteredPorts" ),
            new Integer( Localizer.getChar( "FilteredPortsMnemonic" ) ),
            null );
    }

    /**
     * Create the Port Filter dialogue box.  When the box is closed the
     * data will be saved to the properties file if the OK button was pressed
     */
    public void actionPerformed( ActionEvent event )
    {
        DlgPortFilter dlg = new DlgPortFilter(
            GUIRegistry.getInstance().getMainFrame(), "",
            ServiceManager.sCfg.filteredCatcherPorts.iterator());
        dlg.setVisible(true);

        if (!dlg.getCancel())
        {
            //get the ports from the list
            Enumeration ports = dlg.getList();
            ServiceManager.sCfg.filteredCatcherPorts.clear();
            while (ports.hasMoreElements())
            {
                Object port = ports.nextElement();
                ServiceManager.sCfg.filteredCatcherPorts.add(port);
            }
            //save the ports
            ServiceManager.sCfg.save();
        }
    }

    public void refreshActionState()
    {// global actions are not refreshed
        //setEnabled( ServiceManager.getNetworkManager().isNetworkJoined() );
    }
}