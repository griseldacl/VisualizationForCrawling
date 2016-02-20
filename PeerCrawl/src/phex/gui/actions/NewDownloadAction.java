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
 *  $Id: NewDownloadAction.java,v 1.7 2005/10/03 00:18:25 gregork Exp $
 */
package phex.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.KeyStroke;

import phex.connection.NetworkManager;
import phex.event.LoopbackListener;
import phex.gui.common.GUIRegistry;
import phex.gui.common.GUIUtils;
import phex.gui.dialogs.NewDownloadDialog;
import phex.utils.Localizer;

/**
 *
 */
public class NewDownloadAction extends FWAction implements LoopbackListener
{
    public NewDownloadAction()
    {
        super( Localizer.getString( "GlobalAction_NewDownload" ),
            GUIRegistry.getInstance().getIconFactory().getIcon( "Download" ),
            Localizer.getString( "GlobalAction_TTTNewDownload" ),
            new Integer(Localizer.getChar( "GlobalAction_NewDownloadMnemonic") ),
            KeyStroke.getKeyStroke( Localizer.getString( "GlobalAction_NewDownloadAccelerator" ) ) );
        
        NetworkManager networkMgr = NetworkManager.getInstance();
        networkMgr.addLoopbackListener( this );
    }
    
    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    {
        NewDownloadDialog dialog = new NewDownloadDialog( );
        dialog.show();
    }
    
    /**
     * @see phex.gui.actions.FWAction#refreshActionState()
     */
    public void refreshActionState()
    {
    }
    
    /////////////////// Start NetworkListener interface ////////////////////////
    public void incommingUriDownload(String uri)
    {
        NewDownloadDialog dialog = new NewDownloadDialog( uri,
            NewDownloadDialog.URI_DOWNLOAD );
        GUIUtils.showMainFrame();
        dialog.show();
        dialog.toFront();
    }
    
    public void incommingMagmaDownload(String uri)
    {
        NewDownloadDialog dialog = new NewDownloadDialog( uri, 
            NewDownloadDialog.MAGMA_DOWNLOAD );
        GUIUtils.showMainFrame();
        dialog.show();
        dialog.toFront();
    }
    
    public void incommingRSSDownload(String uri)
    {
        NewDownloadDialog dialog = new NewDownloadDialog( uri, 
            NewDownloadDialog.RSS_DOWNLOAD );
        GUIUtils.showMainFrame();
        dialog.show();
        dialog.toFront();
    }
    //////////////////// End NetworkListener interface /////////////////////////
}
