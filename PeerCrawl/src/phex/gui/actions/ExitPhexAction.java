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

import java.awt.event.ActionEvent;

import javax.swing.*;

import phex.common.*;
import phex.gui.common.*;
import phex.gui.dialogs.CloseOptionsDialog;
import phex.utils.*;

public class ExitPhexAction extends FWAction
{
    public ExitPhexAction()
    {
        super( Localizer.getString( "Exit" ),
            GUIRegistry.getInstance().getIconFactory().getIcon( "Close" ),
            Localizer.getString( "TTTExitPhex" ), new Integer(
            Localizer.getChar( "ExitMnemonic") ),
            KeyStroke.getKeyStroke( Localizer.getString( "ExitAccelerator" ) ) );
    }

    public void actionPerformed(ActionEvent e)
    {
        try
        {
            performCloseGUIAction();
        }
        catch ( Throwable th )
        {
            NLogger.error( NLoggerNames.USER_INTERFACE, th, th );
        }
    }

    /**
     * Shortcut method for calls when no object reference is available.
     * Not nice but as long as we dont have a new clean global registry it helps out.
     */
    public static void performCloseGUIAction()
    {
        if( ServiceManager.sCfg.showCloseOptionsDialog )
        {
            CloseOptionsDialog dialog = new CloseOptionsDialog();
            dialog.show();
            if ( !dialog.isOkActivated() )
            {// cancel close operation if ok was not activated.
                return;
            }
        }

        if( ServiceManager.sCfg.minimizeToBackground )
        {
            minimizeToBackground();
        }
        else
        {
            shutdown();
        }
    }

    private static void minimizeToBackground()
    {
        GUIRegistry registry = GUIRegistry.getInstance();
        DesktopIndicator indicator = registry.getDesktopIndicator();
        MainFrame frame = registry.getMainFrame();

        // minimize...
        if ( frame.getState() != JFrame.ICONIFIED )
        {
            frame.setState( JFrame.ICONIFIED );
        }

        if ( indicator != null )
        {// systray support
            indicator.showIndicator();
            // hide
            frame.hide();
        }
    }

    /**
     * Shortcut method for calls when no object reference is available.
     */
    public static void shutdown()
    {
            /*//TODO reintegrate this warning message
            DownloadManager dm = ServiceManager.getDownloadManager();
            ShareManager sm = ServiceManager.getShareManager();
            HostManager hm = HostManager.getInstance();

            if ( dm.getDownloadingCount() > 0 ||
                 sm.getUploadFileContainer().getUploadFileCount() > 0 )
            {
                int	option = JOptionPane.showConfirmDialog(
                    mFrame,
                    "There are files being downloaded or uploaded.  Exit anyway?",
                    "Confirmation",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.INFORMATION_MESSAGE);

                if (option != JOptionPane.YES_OPTION)
                {
                    // Don't proceed.
                    return;
                }
            }*/

        // catch all possible exception to make 100% sure Phex is shutting down
        // even if there are error.
        try
        {
            Environment.getInstance().shutdownManagers( );
            ServiceManager.sCfg.save();
        }
        catch ( Exception exp )
        {
            NLogger.error( NLoggerNames.USER_INTERFACE, exp, exp );
        }

        GUIRegistry registry = GUIRegistry.getInstance();
        try
        {
            // stop desktop indicator
            DesktopIndicator indicator = registry.getDesktopIndicator();
            if ( indicator != null )
            {
                indicator.hideIndicator();
                indicator.removeIndicator();
            }
        }
        catch ( Exception exp )
        {
            NLogger.error( NLoggerNames.USER_INTERFACE, exp, exp );
        }
        try
        {
            registry.saveGUISettings();
        }
        catch ( Exception exp )
        {
            NLogger.error( NLoggerNames.USER_INTERFACE, exp, exp );
        }
        registry.getMainFrame().dispose();
        
        try
        {
            Environment.getInstance().shutdownManagers( );
        }
        catch ( Exception exp )
        {
            NLogger.error( NLoggerNames.USER_INTERFACE, exp, exp );
        }
        System.exit( 0 );
    }

    public void refreshActionState()
    {// global actions are not refreshed
    }
}