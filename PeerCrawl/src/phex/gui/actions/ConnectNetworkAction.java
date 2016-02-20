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
 *  $Id: ConnectNetworkAction.java,v 1.10 2005/11/03 16:33:47 gregork Exp $
 */
package phex.gui.actions;

import java.awt.event.*;


import phex.common.address.DestAddress;
import phex.connection.*;
import phex.event.*;
import phex.gui.common.*;
import phex.host.*;
import phex.utils.*;
import javax.swing.*;




public class ConnectNetworkAction extends FWAction implements NetworkListener
{
    public ConnectNetworkAction()
    {
        super( Localizer.getString( "Connect" ),
            GUIRegistry.getInstance().getIconFactory().getIcon( "Connect" ),
            Localizer.getString( "TTTConnect" ), new Integer(
            Localizer.getChar( "ConnectMnemonic" ) ),
            KeyStroke.getKeyStroke( Localizer.getString( "ConnectAccelerator" ) ) );

        NetworkManager networkMgr = NetworkManager.getInstance();
        networkMgr.addNetworkListener( this );
        setEnabled( !networkMgr.isConnected() );
    }

    public void actionPerformed(ActionEvent e)
    {
        NetworkManager.getInstance().connectToNetwork();
    }

    public void refreshActionState()
    {// global actions are not refreshed
        //setEnabled( ServiceManager.getNetworkManager().isNetworkJoined() );
    }

    /////////////////// Start NetworkListener interface ////////////////////////

    public void connectedToNetwork()
    {
        setEnabled( false );
    }

    public void disconnectedFromNetwork()
    {
        setEnabled( true );
    }
    public void networkIPChanged( DestAddress localAddress )
    { /* not implemented */ }
    //////////////////// End NetworkListener interface /////////////////////////

}
