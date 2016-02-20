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
 *  $Id: DisconnectNetworkAction.java,v 1.14 2005/11/03 16:33:47 gregork Exp $
 */
package phex.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.KeyStroke;


import phex.common.address.DestAddress;
import phex.connection.NetworkManager;
import phex.event.NetworkListener;
import phex.gui.common.GUIRegistry;
import phex.utils.Localizer;



public class DisconnectNetworkAction extends FWAction implements NetworkListener
{
    public DisconnectNetworkAction()
    {
        super( Localizer.getString( "Disconnect" ),
            GUIRegistry.getInstance().getIconFactory().getIcon( "Disconnect" ),
            Localizer.getString( "TTTDisconnect" ), new Integer(
            Localizer.getChar( "DisconnectMnemonic") ),
            KeyStroke.getKeyStroke( Localizer.getString( "DisconnectAccelerator" ) ) );

        NetworkManager networkMgr = NetworkManager.getInstance();
        networkMgr.addNetworkListener( this );
        setEnabled( networkMgr.isConnected() );
    }

    public void actionPerformed(ActionEvent e)
    {
        NetworkManager.getInstance().disconnectNetwork();
    }

    public void refreshActionState()
    {// global actions are not refreshed
        //setEnabled( ServiceManager.getNetworkManager().isNetworkJoined() );
    }

    /////////////////// Start NetworkListener interface ////////////////////////

    public void connectedToNetwork()
    {
        setEnabled( true );
    }

    public void disconnectedFromNetwork()
    {
        setEnabled( false );
    }
    public void networkIPChanged( DestAddress localAddress )
    { /* not implemented */ }
    //////////////////// End NetworkListener interface /////////////////////////
}