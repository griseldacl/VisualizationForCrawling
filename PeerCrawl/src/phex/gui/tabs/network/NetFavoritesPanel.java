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
 *  Created on 02.04.2004
 *  --- CVS Information ---
 *  $Id: NetFavoritesPanel.java,v 1.9 2005/11/13 10:17:45 gregork Exp $
 */
package phex.gui.tabs.network;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import phex.common.address.DestAddress;
import phex.common.address.IpAddress;
import phex.common.address.MalformedDestAddressException;
import phex.gui.models.FavoritesListModel;
import phex.host.FavoriteHost;
import phex.host.HostManager;
import phex.net.presentation.PresentationManager;
import phex.utils.Localizer;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * 
 * @author gkoukkoullis
 */
public class NetFavoritesPanel
    extends JPanel
    //extends FormDebugPanel
{
    private JTextField newFavoriteHostTF;
    private JButton addToFavoritesHostBtn;
    
    private JList favoritesList;
    
    private JButton removeFromFavoritesHostBtn;
    private JButton connectToFavoritesHostBtn;
    
    public NetFavoritesPanel()
    {
        init();
    }
    
    private void init()
    {
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
            "8dlu, d, 2dlu, d, 8dlu", // columns
            "p, 3dlu, p, 4dlu, p, 2dlu, p, 2dlu, p:grow"); //rows
        PanelBuilder favoritesBuilder = new PanelBuilder( layout, this );
        
        favoritesBuilder.addSeparator( Localizer.getString( "NetworkTab_Favorites" ),
            cc.xywh( 1, 1, 5, 1 ) );
        
        newFavoriteHostTF = new JTextField( 20 );
        favoritesBuilder.add( newFavoriteHostTF, cc.xy( 2, 3 ) );
        
        addToFavoritesHostBtn = new JButton( Localizer.getString( "Add" ) );
        addToFavoritesHostBtn.addActionListener( new AddToFavoritesHostAction() );
        favoritesBuilder.add( addToFavoritesHostBtn, cc.xy( 4, 3 ) );
        
        favoritesList = new JList( new FavoritesListModel() );
        favoritesList.setPrototypeCellValue( "123.123.123.123:12345" );
        favoritesList.setVisibleRowCount( 5 );
        favoritesList.setCellRenderer( new FavoritesListRenderer() );
        favoritesBuilder.add( new JScrollPane( favoritesList ), cc.xywh( 2, 5, 1, 5 ) );
        
        connectToFavoritesHostBtn = new JButton( Localizer.getString( "Connect" ) );
        connectToFavoritesHostBtn.addActionListener( new ConnectToFavoritesHostAction());
        favoritesBuilder.add( connectToFavoritesHostBtn, cc.xy( 4, 5 ) );
        
        removeFromFavoritesHostBtn = new JButton( Localizer.getString( "Remove" ) );
        removeFromFavoritesHostBtn.addActionListener( new RemoveFromFavoritesHostAction());
        favoritesBuilder.add( removeFromFavoritesHostBtn, cc.xy( 4, 7 ) );
    }
    
    private final class RemoveFromFavoritesHostAction implements ActionListener
    {
        public void actionPerformed( ActionEvent e )
        {
            try
            {
                FavoriteHost host = (FavoriteHost) favoritesList.getSelectedValue();
                HostManager hostMgr = HostManager.getInstance();
                hostMgr.getFavoritesContainer().removeBookmarkedHost( host );
            }
            catch ( Exception exp )
            {// catch all errors left
                NLogger.error( NLoggerNames.USER_INTERFACE, exp, exp );
            }
        }
    }
    
    private final class ConnectToFavoritesHostAction implements ActionListener
    {
        public void actionPerformed( ActionEvent e )
        {
            try
            {
                FavoriteHost host = (FavoriteHost) favoritesList.getSelectedValue();
                if ( host == null )
                {// nothing to do..
                    return;
                }
                HostManager hostMgr = HostManager.getInstance();
                // Add new host and connect.
                hostMgr.getNetworkHostsContainer().createOutgoingConnectionToHost(
                    host.getHostAddress() );
            }
            catch ( Exception exp )
            {// catch all errors left
                NLogger.error( NLoggerNames.USER_INTERFACE, exp, exp );
            }
        }
    }
    
    private final class AddToFavoritesHostAction implements ActionListener
    {
        public void actionPerformed( ActionEvent e )
        {
            try
            {
                String host = newFavoriteHostTF.getText();
                host = host.trim();
                if ( host.length() > 0 )
                {
                    try
                    {
                        DestAddress address = PresentationManager.getInstance()
                            .createHostAddress( host, IpAddress.DEFAULT_PORT );
                        HostManager hostMgr = HostManager.getInstance();
                        hostMgr.getFavoritesContainer().addFavorite( 
                            address );
                        newFavoriteHostTF.setText("");
                    }
                    catch ( MalformedDestAddressException exp )
                    {// TODO2 bring friendly error message about wrong format.
                    }
                }                
            }
            catch ( Exception exp )
            {// catch all errors left
                NLogger.error( NLoggerNames.USER_INTERFACE, exp, exp );
            }
        }
    }
}
