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
 *  Created on 11.08.2005
 *  --- CVS Information ---
 *  $Id: ConnectionsZone.java,v 1.2 2005/10/03 00:18:26 gregork Exp $
 */
package phex.gui.statusbar;

import javax.swing.*;

import phex.connection.NetworkManager;
import phex.gui.common.GUIRegistry;
import phex.gui.common.IconFactory;
import phex.host.HostManager;
import phex.host.NetworkHostsContainer;
import phex.utils.Localizer;

public class ConnectionsZone extends JPanel
{
    private NetworkHostsContainer hostsContainer;
    private NetworkManager networkMgr;
    
    private JLabel connectionLabel;
    private Icon connectedIcon;
    private Icon firewalledIcon;
    private Icon disconnectedIcon;

    public ConnectionsZone()
    {
        super(  );
        SpringLayout layout = new SpringLayout();
        setLayout( layout );
        
        networkMgr = NetworkManager.getInstance();
        HostManager hostMgr = HostManager.getInstance();
        hostsContainer = hostMgr.getNetworkHostsContainer();
        
        IconFactory factory = GUIRegistry.getInstance().getIconFactory();
        connectedIcon = factory.getIcon( "Network" );
        firewalledIcon = factory.getIcon( "Firewalled" );
        disconnectedIcon = factory.getIcon( "Disconnect" );
        
        connectionLabel = new JLabel();
        connectionLabel.setIcon( GUIRegistry.getInstance().getIconFactory()
            .getIcon( "UploadSmall" ) );
        add( connectionLabel );
        
        updateZone();
        
        layout.putConstraint(SpringLayout.NORTH, connectionLabel, 2, SpringLayout.NORTH, this );
        layout.putConstraint(SpringLayout.WEST, connectionLabel, 5, SpringLayout.WEST, this );
        layout.putConstraint(SpringLayout.EAST, this, 5, SpringLayout.EAST, connectionLabel );
        layout.putConstraint(SpringLayout.SOUTH, this, 2, SpringLayout.SOUTH, connectionLabel );
    }

    public void updateZone()
    {
        int hostCount = hostsContainer.getTotalConnectionCount();
        if ( hostCount > 0 )
        {
            if ( networkMgr.hasConnectedIncoming() )
            {
                connectionLabel.setIcon( connectedIcon );
            }
            else
            {
                connectionLabel.setIcon( firewalledIcon );
            }
        }
        else
        {
            connectionLabel.setIcon( disconnectedIcon );
        }

        Object[] args = new Object[]
        {
            new Integer( hostCount )
        };
        String text = Localizer.getFormatedString( "StatusBar_Connections", args );
        connectionLabel.setText( text );
        connectionLabel.setToolTipText( Localizer.getString( "StatusBar_TTTConnections" ) );
        
        validate();
    }
}
