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
 *  $Id: GUIRegistry.java,v 1.45 2005/10/03 00:18:25 gregork Exp $
 */
package phex.gui.common;

import java.io.File;
import java.util.HashMap;

import javax.swing.*;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang.SystemUtils;

import phex.common.Environment;
import phex.common.EnvironmentConstants;
import phex.common.ServiceManager;
import phex.common.file.FileManager;
import phex.common.file.ManagedFile;
import phex.common.file.ManagedFileException;
import phex.event.UserMessageListener;
import phex.gui.actions.*;
import phex.gui.chat.ChatFrameManager;
import phex.gui.macosx.MacOsxHandler;
import phex.update.UpdateManager;
import phex.utils.*;
import phex.xml.*;

public class GUIRegistry implements GUIRegistryConstants
{
    // singleton
    private static GUIRegistry instance = new GUIRegistry();

    /**
     * Contains the global actions of this app together with a retrieval key.
     */
    private HashMap globalActionMap;

    private LazyEventQueue lazyEventQueue;
    private TableUpdateService tableUpdateService;
    private DesktopIndicator desktopIndicator;
    private IconFactory iconFactory;
    private ChatFrameManager chatFrameManager;
    private MainFrame mainFrame;


    private boolean showTableHorizontalLines;
    private boolean showTableVerticalLines;
    private boolean useLogBandwidthSlider;
    private boolean showRespectCopyrightNotice;

    private GUIRegistry()
    {
    }

    public static GUIRegistry getInstance()
    {
        return instance;
    }
    
    public void initialize()
    {
        // make sure you never need to keep a reference of XJBGUISettings
        // by a class attributes...
        XJBGUISettings guiSettings = loadGUISettings();
        initializeGUISettings( guiSettings );

        iconFactory = new IconFactory( ICON_RESOURCE_NAME );
        // only systray support on windows...
        if ( Environment.getInstance().isWindowsOS() )
        {
            try
            {
                desktopIndicator = new DesktopIndicator();
            }
            catch(UnsupportedOperationException x)
            {
                desktopIndicator = null;
            }
        }
        
        if ( SystemUtils.IS_OS_MAC_OSX )
        {
            new MacOsxHandler();
        }
        initializeGlobalActions();
        chatFrameManager = new ChatFrameManager();
        try 
        {
            mainFrame = new MainFrame( null, guiSettings );
            NLogger.debug( NLoggerNames.USER_INTERFACE, "GUIRegistry initialized." );
        } 
        catch ( java.awt.HeadlessException ex ) 
        {
        }

        GUIUpdateNotificationListener listener = new GUIUpdateNotificationListener();
        UpdateManager.getInstance().triggerAutoBackgroundCheck( listener );
        
        Environment.getInstance().setUserMessageListener( new GUIUserMessageListener() );
    }

    /**
     * Returns the DesktopIndicator responsible for systray support.
     * Method might return null if no systray is supported.
     */
    public DesktopIndicator getDesktopIndicator()
    {
        return desktopIndicator;
    }

    public MainFrame getMainFrame()
    {
        return mainFrame;
    }

    public IconFactory getIconFactory()
    {
        return iconFactory;
    }

    public LazyEventQueue getLazyEventQueue()
    {
        if ( lazyEventQueue == null )
        {
            lazyEventQueue = new LazyEventQueue();
        }
        return lazyEventQueue;
    }

    public TableUpdateService getTableUpdateService()
    {
        if ( tableUpdateService == null )
        {
            tableUpdateService = new TableUpdateService();
        }
        return tableUpdateService;
    }

    public FWAction getGlobalAction( String actionKey )
    {
        return (FWAction) globalActionMap.get( actionKey );
    }
    
    public String getUsedLAFClass()
    {
        return UIManager.getLookAndFeel().getClass().getName();
    }

    /**
     * Returns true if the tables draw horizontal lines between cells, false if
     * they don't. The default is false for MacOSX and Windows, true for others.
     * @return true if the tables draw horizontal lines between cells, false
     * if they don't.
     */
    public boolean getShowTableHorizontalLines()
    {
        return showTableHorizontalLines;
    }

    /**
     * Sets whether the tables draw horizontal lines between cells. If
     * showHorizontalLines is true it does; if it is false it doesn't.
     * @param showHorizontalLines
     */
    public void setShowTableHorizontalLines( boolean showHorizontalLines )
    {
        showTableHorizontalLines = showHorizontalLines;
    }

    /**
     * Returns true if the tables draw vertical lines between cells, false if
     * they don't. The default is false for MacOSX and Windows, true for others.
     * @return true if the tables draw vertical lines between cells, false
     * if they don't.
     */
    public boolean getShowTableVerticalLines()
    {
        return showTableVerticalLines;
    }

    /**
     * Sets whether the tables draw vertical lines between cells. If
     * showVerticalLines is true it does; if it is false it doesn't.
     * @param showVerticalLines
     */
    public void setShowTableVerticalLines( boolean showVerticalLines )
    {
        showTableVerticalLines = showVerticalLines;
    }


    /**
     * @return Returns the isLogarithmicBandwidthSliderUsed.
     */
    public boolean useLogBandwidthSlider()
    {
        return useLogBandwidthSlider;
    }
    
    /**
     * @param isLogarithmicBandwidthSliderUsed The isLogarithmicBandwidthSliderUsed to set.
     */
    public void setLogBandwidthSliderUsed(
        boolean useLogBandwidthSlider)
    {
        this.useLogBandwidthSlider = useLogBandwidthSlider;
    }
    
    /**
     * @return Returns the showRespectCopyrightNotice.
     */
    public boolean isRespectCopyrightNoticeShown()
    {
        return showRespectCopyrightNotice;
    }
    
    /**
     * @param showRespectCopyrightNotice The showRespectCopyrightNotice to set.
     */
    public void setRespectCopyrightNoticeShown(
        boolean showRespectCopyrightNotice)
    {
        this.showRespectCopyrightNotice = showRespectCopyrightNotice;
    }
    
    /**
     * Loads the XJBGUISettings object or null if its not existing or a parsing
     * error occures.
     * @return the XJBGUISettings object.
     */
    private XJBGUISettings loadGUISettings()
    {
        XJBPhex phex;
        try
        {
            NLogger.debug( NLoggerNames.USER_INTERFACE,
                "Load gui settings file." );
    
            File file = Environment.getInstance().getPhexConfigFile(
                EnvironmentConstants.XML_GUI_SETTINGS_FILE_NAME );
            ManagedFile managedFile = FileManager.getInstance().getReadWriteManagedFile( file );
            phex = XMLBuilder.loadXJBPhexFromFile( managedFile );
            if ( phex == null )
            {
                NLogger.debug( NLoggerNames.USER_INTERFACE,
                    "No gui settings configuration file found." );
                return null;
            }
            updateGUISettings( phex );
            return phex.getGuiSettings();
        }
        catch ( JAXBException exp )
        {
            Throwable linkedException = exp.getLinkedException();
            if ( linkedException != null )
            {
                NLogger.error( NLoggerNames.USER_INTERFACE, linkedException, linkedException );
            }
            NLogger.error( NLoggerNames.USER_INTERFACE, exp, exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.GuiSettingsLoadFailed, 
                new String[]{ exp.toString() } );
            return null;
        }
        catch ( ManagedFileException exp )
        {
            NLogger.error( NLoggerNames.USER_INTERFACE, exp, exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.GuiSettingsLoadFailed, 
                new String[]{ exp.toString() } );
            return null;
        }
    }

    /**
     * Saves the XJBGUISettings object or null if its not existing or a parsing
     * error occures.
     * @return the XJBGUISettings object.
     */
    public void saveGUISettings()
    {
        NLogger.debug( NLoggerNames.USER_INTERFACE,
            "Saving gui settings..." );

        // JAXB-beta way
        try
        {
            ObjectFactory objFactory = new ObjectFactory();
            XJBPhex phex = objFactory.createPhexElement();
            phex.setPhexVersion( VersionUtils.getFullProgramVersion() );

            XJBGUISettings xjbSettings = objFactory.createXJBGUISettings();
            xjbSettings.setLogBandwidthSliderUsed( useLogBandwidthSlider );
            xjbSettings.setRespectCopyrightNoticeShown( showRespectCopyrightNotice );
            phex.setGuiSettings( xjbSettings );

            XJBGUITableList xjbTableList = objFactory.createXJBGUITableList();
            xjbTableList.setShowHorizontalLines( showTableHorizontalLines );
            xjbTableList.setShowVerticalLines( showTableVerticalLines );
            xjbSettings.setTableList( xjbTableList );
            
            xjbSettings.setLookAndFeelClass( getUsedLAFClass() );

            mainFrame.saveGUISettings( xjbSettings );

            File file = Environment.getInstance().getPhexConfigFile(
                EnvironmentConstants.XML_GUI_SETTINGS_FILE_NAME );
            ManagedFile managedFile = FileManager.getInstance().getReadWriteManagedFile( file );
            XMLBuilder.saveToFile( managedFile, phex );
        }
        catch ( JAXBException exp )
        {
            // TODO during close this message is never displayed since application
            // will exit too fast. A solution to delay exit process in case 
            // SlideInWindows are open needs to be found.
            NLogger.error( NLoggerNames.USER_INTERFACE, exp, exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.GuiSettingsSaveFailed, 
                new String[]{ exp.toString() } );
        }
        catch ( ManagedFileException exp )
        {
            // TODO during close this message is never displayed since application
            // will exit too fast. A solution to delay exit process in case 
            // SlideInWindows are open needs to be found.
            NLogger.error( NLoggerNames.USER_INTERFACE, exp, exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.GuiSettingsSaveFailed, 
                new String[]{ exp.toString() } );
        }
    }

    /**
     * Initializes global actions that need or can be available before the main
     * frame is initialized.
     */
    private void initializeGlobalActions()
    {
        // required capacity is calculated by
        // Math.ceil( actionCount * 1 / 0.75 )
        // -> actionCount = 10 -> capacity =  14
        globalActionMap = new HashMap( 2 );

        FWAction action = new ExitPhexAction();
        globalActionMap.put( EXIT_PHEX_ACTION, action );
        
        action = new NewDownloadAction();
        globalActionMap.put( NEW_DOWNLOAD_ACTION, action );

        action = new ConnectNetworkAction();
        globalActionMap.put( CONNECT_NETWORK_ACTION, action );

        action = new DisconnectNetworkAction();
        globalActionMap.put( DISCONNECT_NETWORK_ACTION, action );

        action = new SwitchNetworkAction();
        globalActionMap.put( SWITCH_NETWORK_ACTION, action );
    }

    private void initializeGUISettings( XJBGUISettings guiSettings )
    {
        // set default values...
        Environment env = Environment.getInstance();
        if ( env.isMacOSX() || env.isWindowsOS() )
        {
            showTableHorizontalLines = false;
            showTableVerticalLines = false;
        }
        else
        {
            showTableHorizontalLines = true;
            showTableVerticalLines = true;
        }
        useLogBandwidthSlider = false;
        showRespectCopyrightNotice = true;

        // sets old values from old cfg...
        ToolTipManager.sharedInstance().setEnabled(
            ServiceManager.sCfg.mUIDisplayTooltip );
            
        String userLafClass;
        // load values from gui new settings if available.
        if ( guiSettings != null )
        {
            if ( guiSettings.isSetLogBandwidthSliderUsed() )
            {
                useLogBandwidthSlider = guiSettings.isLogBandwidthSliderUsed();
            }
            if ( guiSettings.isSetRespectCopyrightNoticeShown() )
            {
                showRespectCopyrightNotice = guiSettings.isRespectCopyrightNoticeShown();
            }
            
            XJBGUITableList tableList = guiSettings.getTableList();
            if ( guiSettings.getTableList().isSetShowHorizontalLines() )
            {
                showTableHorizontalLines = tableList.isShowHorizontalLines();
            }
            if ( guiSettings.getTableList().isSetShowVerticalLines() )
            {
                showTableVerticalLines = tableList.isShowVerticalLines();
            }
            userLafClass = guiSettings.getLookAndFeelClass();
        }
        else
        {
            userLafClass = null;
        }
        
        LookAndFeel laf = LookAndFeelUtils.determineLAF(userLafClass);
        String phexLafClass = laf.getClass().getName();
        if ( userLafClass != null && !phexLafClass.equals( userLafClass ) )
        {// in case we had to switch LAF show error.
            JOptionPane.showMessageDialog( 
                GUIRegistry.getInstance().getMainFrame(),
                Localizer.getString("LAF_ErrorLoadingSwitchToDefault"),
                Localizer.getString("Error"), 
                JOptionPane.ERROR_MESSAGE );
        }
        
        if ( phexLafClass.equals( UIManager.getLookAndFeel().getClass().getName()))
        {
            // in case correct laf is already set just update UI!
            // this must be done to get colors correctly initialized!
            GUIUtils.updateComponentsUI();
        }
        else
        {
            try 
            {
                LookAndFeelUtils.setLookAndFeel( laf );
            }
            catch ( ExceptionInInitializerError ex ) 
            {
                // headless mode
            }
            catch (LookAndFeelFailedException e)
            {// this is supposed to never happen.. since the LAF
             // should already be tested to function.
                assert( false );
            }
        }
    }
    
    ///////////////// Settings updates /////////////////////////
    
    private void updateGUISettings( XJBPhex xjbPhex )
    {
        if ( xjbPhex == null )
        {
            return;
        }
        String version = xjbPhex.getPhexVersion();
        if ( VersionUtils.compare( VersionUtils.getFullProgramVersion(), version) != 0 )
        {
            // a Phex version change... reactivate respect copyright dialog
            reactivateRespectCopyright( xjbPhex );
        }
        if ( VersionUtils.compare( "2.1.6.82", version) > 0 )
        {
            updatesForBuild82( xjbPhex );
        }
    }
    
    private void updatesForBuild82( XJBPhex xjbPhex )
    {
        XJBGUISettings guiSettings = xjbPhex.getGuiSettings();
        if ( guiSettings == null )
        {
            return;
        }
        String userLafClass = guiSettings.getLookAndFeelClass();
        if ( userLafClass == null )
        {
            return;
        }
        if ( userLafClass.startsWith( "com.jgoodies.plaf" ) )
        {
            userLafClass = StringUtils.replace(userLafClass, 
                "com.jgoodies.plaf.", "com.jgoodies.looks.", 1 );
            guiSettings.setLookAndFeelClass(userLafClass);
        }
    }
    
    /**
     * Reactivate respect copyright dialog on a Phex version change.
     * @param xjbPhex
     */
    private void reactivateRespectCopyright( XJBPhex xjbPhex )
    {
        XJBGUISettings guiSettings = xjbPhex.getGuiSettings();
        if ( guiSettings == null )
        {
            return;
        }
        guiSettings.setRespectCopyrightNoticeShown(true);
    }
}