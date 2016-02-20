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
 *  $Id: MacOsxHandler.java,v 1.5 2005/10/03 00:18:26 gregork Exp $
 */
package phex.gui.macosx;

import java.io.File;

import phex.gui.actions.ExitPhexAction;
import phex.gui.actions.NewDownloadAction;
import phex.gui.common.GUIRegistry;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

//import com.apple.eawt.ApplicationAdapter;
import com.apple.mrj.MRJApplicationUtils;
import com.apple.mrj.MRJOpenDocumentHandler;
import com.apple.mrj.MRJQuitHandler;

/**
 * Registers the Quit handler of Mac OS X menu to close Phex correct.
 * 
 * Who knows why but the new ApplicationAdapter seems not to work correctly. Therefore
 * we use the MRJQuitHandler 2005-03-05
 */
public class MacOsxHandler /*extends ApplicationAdapter*/ implements MRJQuitHandler, MRJOpenDocumentHandler
{
    public MacOsxHandler()
    {
        //Logger.logError(Logger.GLOBAL, "Registering new ApplicationListener" );
        //Application application = new Application();
        //application.addApplicationListener(this);
        
        NLogger.debug(NLoggerNames.NATIV_MACOSX, "Registering old MacOsX QuitHandler.");
        MRJApplicationUtils.registerQuitHandler( this );
        NLogger.debug(NLoggerNames.NATIV_MACOSX, "Registering old MacOsX OpenDocumentHandler.");
        MRJApplicationUtils.registerOpenDocumentHandler( this );
    }

//    /**
//     * @see com.apple.eawt.ApplicationListener#handleOpenDocument(com.apple.eawt.ApplicationEvent)
//     */
//    public void handleOpenDocument(ApplicationEvent event)
//    {
//        Logger.logError(Logger.GLOBAL, "Called handleOpenDocument with " + event );
//        String file = event.getFilename();
//        NetworkManager.getInstance().fireIncomingUriDownload( file );
//    }
//
//    /**
//     * @see com.apple.eawt.ApplicationListener#handleQuit(com.apple.eawt.ApplicationEvent)
//     */
//    public void handleQuit(ApplicationEvent event)
//    {
//        Logger.logError(Logger.GLOBAL, "Called handleQuit with " + event );
//        ExitPhexAction.performCloseGUIAction();
//    }

    /**
     * @see com.apple.mrj.MRJQuitHandler#handleQuit()
     */
    public void handleQuit()
    {
        NLogger.debug(NLoggerNames.NATIV_MACOSX, "Called old MacOsX quit handler.");
        ExitPhexAction.performCloseGUIAction();
    }

    /**
     * @see com.apple.mrj.MRJOpenDocumentHandler#handleOpenFile(java.io.File)
     */
    public void handleOpenFile(File file)
    {
        NLogger.debug(NLoggerNames.NATIV_MACOSX, "Called old MacOsX open file handler: " + file);
        String absFileName = file.getAbsolutePath();
        if ( absFileName.endsWith(".magma") )
        {
            NewDownloadAction action = (NewDownloadAction) 
                GUIRegistry.getInstance().getGlobalAction(GUIRegistry.NEW_DOWNLOAD_ACTION);
            action.incommingMagmaDownload(absFileName);
        }
        if ( absFileName.endsWith(".xml") )
        {
            NewDownloadAction action = (NewDownloadAction) 
                GUIRegistry.getInstance().getGlobalAction(GUIRegistry.NEW_DOWNLOAD_ACTION);
            action.incommingRSSDownload(absFileName);
        }
    }
}