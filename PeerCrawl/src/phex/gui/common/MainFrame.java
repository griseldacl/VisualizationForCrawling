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
 *  $Id: MainFrame.java,v 1.29 2005/10/03 00:18:25 gregork Exp $
 */
package phex.gui.common;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.xml.bind.JAXBException;


import phex.common.*;
import phex.connection.NetworkManager;
import phex.gui.actions.*;
import phex.gui.dialogs.RespectCopyrightDialog;
import phex.gui.statusbar.*;
import phex.gui.tabs.*;
import phex.gui.tabs.download.SWDownloadTab;
import phex.gui.tabs.library.LibraryTab;
import phex.gui.tabs.network.*;
import phex.gui.tabs.search.SearchTab;
import phex.gui.tabs.search.monitor.*;
import phex.utils.*;
import phex.xml.*;

public class MainFrame extends JFrame
{
    public static final int NETWORK_TAB_ID = 1000;
    public static final int SEARCH_TAB_ID = 1101;
    public static final int DOWNLOAD_TAB_ID = 1003;
    public static final int UPLOAD_TAB_ID = 1004;
    public static final int SECURITY_TAB_ID = 1005;
    public static final int STATISTICS_TAB_ID = 1006;
    public static final int LIBRARY_TAB_ID = 1007;
    public static final int SEARCH_MONITOR_TAB_ID = 1008;
    public static final int RESULT_MONITOR_TAB_ID = 1009;

    public static final int NETWORK_TAB_INDEX = 0;
    public static final int SEARCH_TAB_INDEX = 1;
    public static final int DOWNLOAD_TAB_INDEX = 3;
    public static final int SHARE_TAB_INDEX = 4;
    public static final int LIBRARY_TAB_INDEX = 5;
    public static final int SECURITY_TAB_INDEX = 6;
    public static final int STATISTICS_TAB_INDEX = 7;
    public static final int SEARCH_MONITOR_TAB_INDEX = 8;
    public static final int RESULT_MONITOR_TAB_INDEX = 9;
    

    private JTabbedPane tabbedPane;
    private JPanel logoPanel;
    private Timer refresher;

    private FWToolBar toolbar;
    private NetworkTab networkTab;
    private SearchTab searchTab;
    private UploadTab uploadTab;
    private LibraryTab libraryTab;
    private SWDownloadTab swDownloadTab;
    private SecurityTab securityTab;
    private StatisticsTab statisticsTab;
    private SearchMonitorTab searchMonitorTab;
    private ResultMonitorTab resultMonitorTab;
    private StatusBar statusBar;

    public MainFrame( SplashWindow splash, XJBGUISettings guiSettings )
    {
        super( );
        
        Icon frameIcon = GUIRegistry.getInstance().getIconFactory().getIcon(
            "Phex16" );
        if (frameIcon != null)
        {
            setIconImage( ((ImageIcon)frameIcon).getImage() );
        }

        //SkinLookAndFeelLoader.tryLoadingSkinLookAndFeel();
        //GUIUtils.setLookAndFeel( "com.jgoodies.looks.windows.ExtWindowsLookAndFeel" );
        //GUIUtils.setLookAndFeel( "com.jgoodies.looks.plastic.PlasticLookAndFeel" );
        //GUIUtils.setLookAndFeel( "com.jgoodies.looks.plastic.Plastic3DLookAndFeel" );
        //GUIUtils.setLookAndFeel( "com.jgoodies.looks.plastic.PlasticXPLookAndFeel" );
        
        setupComponents( guiSettings );
/*
        Hashtable ht = UIManager.getDefaults();
        Enumeration enumr = ht.keys();
        while (enumr.hasMoreElements())
        {
            Object	key = enumr.nextElement();
            //System.out.println(key + "=" + ht.get(key));
        }
*/
        setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );
        addWindowListener( new WindowHandler() );

        DesktopIndicator indicator = GUIRegistry.getInstance().getDesktopIndicator();
        // if sys tray supported
        if ( indicator != null )
        {
            indicator.addDesktopIndicatorListener( new DesktopIndicatorHandler() );
        }

        pack();
        initFrameSize(guiSettings);

        setTitle();

        refresher = new javax.swing.Timer(1000, new RefreshHandler());
        refresher.start();
    }

    private void initFrameSize(XJBGUISettings guiSettings)
    {
        GUIUtils.centerAndSizeWindow(this, 7, 8);
        if ( guiSettings == null )
        {
            return;
        }
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle bounds = getBounds();
        if ( guiSettings.isSetWindowWidth() )
        {
            bounds.width = Math.min( screenSize.width, guiSettings.getWindowWidth() );
        }
        if ( guiSettings.isSetWindowHeight() )
        {
            bounds.height = Math.min( screenSize.height, guiSettings.getWindowHeight() );
        }
        if ( guiSettings.isSetWindowPosX() )
        {
            int posX = guiSettings.getWindowPosX();
            bounds.x = Math.max( 0, Math.min( posX+bounds.width,
                (int)screenSize.getWidth() )-bounds.width );
        }
        if ( guiSettings.isSetWindowPosY() )
        {
            int posY = guiSettings.getWindowPosY();
            bounds.y = Math.max( 0, Math.min( posY+bounds.height,
                (int)screenSize.getHeight() )-bounds.height );
        }
        Logger.logMessage( Logger.FINE, Logger.GUI, "Frame position: " + bounds );
        setBounds( bounds );
    }

    private void setupComponents( XJBGUISettings guiSettings )
    {
        tabbedPane = new JTabbedPane();
        tabbedPane.setMinimumSize(new Dimension(50, 50));
        tabbedPane.addChangeListener( new ChangeListener()
            {
                public void stateChanged( ChangeEvent e )
                {
                    Component comp = tabbedPane.getSelectedComponent();
                    if ( comp instanceof FWTab )
                    {
                        ((FWTab)comp).tabSelectedNotify();
                    }
                }
            } );

        // Net Tab
//long start = System.currentTimeMillis();
        networkTab = new NetworkTab( this );
        networkTab.initComponent( guiSettings );
//long stop = System.currentTimeMillis();
//System.out.println( "net " + (stop - start) + "" );
        initializeTab( networkTab, NETWORK_TAB_ID, guiSettings );

        // Search Tab
//start = System.currentTimeMillis();
        searchTab = new SearchTab( );
        searchTab.initComponent( guiSettings );
//stop = System.currentTimeMillis();
//System.out.println( "search " + (stop - start) + "" );
        initializeTab( searchTab, SEARCH_TAB_ID, guiSettings );

        //  SWDownload Tab
        swDownloadTab = new SWDownloadTab( this );
        swDownloadTab.initComponent( guiSettings );
        initializeTab( swDownloadTab, DOWNLOAD_TAB_ID, guiSettings );

        //  Upload Tab
        uploadTab = new UploadTab( this );
        uploadTab.initComponent( guiSettings );
        initializeTab( uploadTab, UPLOAD_TAB_ID, guiSettings );
        
        //  Library Tab
        libraryTab = new LibraryTab( );
        libraryTab.initComponent( guiSettings );
        initializeTab( libraryTab, LIBRARY_TAB_ID, guiSettings );

        //  Security Tab
        securityTab = new SecurityTab( );
        securityTab.initComponent( guiSettings );
        initializeTab( securityTab, SECURITY_TAB_ID, guiSettings );

        //  Statistics Tab
        statisticsTab = new StatisticsTab( );
        statisticsTab.initComponent( guiSettings );
        initializeTab( statisticsTab, STATISTICS_TAB_ID, guiSettings );
        
        //  Search Monitor Tab
        searchMonitorTab = new SearchMonitorTab( );
        searchMonitorTab.initComponent( guiSettings );
        initializeTab( searchMonitorTab, SEARCH_MONITOR_TAB_ID, guiSettings );
        
        //  Result Monitor Tab
        resultMonitorTab = new ResultMonitorTab( );
        resultMonitorTab.initComponent( guiSettings );
        initializeTab( resultMonitorTab, RESULT_MONITOR_TAB_ID, guiSettings );

        if ( tabbedPane.getTabCount() == 0 )
        {
            getContentPane().add( BorderLayout.CENTER, getLogoPanel() );
        }
        else
        {
            tabbedPane.setSelectedIndex( 0 );
            getContentPane().add(BorderLayout.CENTER, tabbedPane);
        }

        // menu bar
        JMenuBar menubar = createMenuBar( guiSettings );
        setJMenuBar( menubar );

        // toolbar
        boolean isToolbarVisible = true;
        if ( guiSettings != null && guiSettings.isSetToolbarVisible() )
        {
            isToolbarVisible = guiSettings.isToolbarVisible();
        }
        if ( isToolbarVisible )
        {
            setToolbarVisible( true );
        }
        
        // Status Bar
        createStatusBar();
    }

    /**
     * Creates the status bar of the main frame.
     */
    private void createStatusBar()
    {
        statusBar = new StatusBar();        
        statusBar.addZone("ConnectionsZone", new ConnectionsZone(), "*");
        statusBar.addZone("DownloadZone", new DownloadZone(), "");
        statusBar.addZone("UploadZone", new UploadZone(), "");
        getContentPane().add( BorderLayout.SOUTH, statusBar );
        ActionListener updateStatusBarAction = new ActionListener()
        {
            public void actionPerformed( ActionEvent e )
            {
                try
                {
                    ((ConnectionsZone)statusBar.getZone("ConnectionsZone")).updateZone();
                    ((DownloadZone)statusBar.getZone("DownloadZone")).updateZone();
                    ((UploadZone)statusBar.getZone("UploadZone")).updateZone();
                }
                catch ( Throwable th )
                {
                    NLogger.error(NLoggerNames.USER_INTERFACE, th, th);
                }
            }
        };
        Timer timer = new Timer( 2000, updateStatusBarAction );
        timer.start();
    }

    public void setTitle()
    {
        StringBuffer buffer = new StringBuffer( Environment.getPhexVendor() );
        NetworkManager networkMgr = NetworkManager.getInstance();
        if ( networkMgr.isNetworkJoined() )
        {
            buffer.append(" - ");
            buffer.append( networkMgr.getGnutellaNetwork().getName() );
        }

        if (ServiceManager.sCfg.mProxyUse)
        {
            buffer.append( "  (via Proxy Server)" );
        }
        super.setTitle( buffer.toString() );
    }

    public void setTabVisible( final FWTab tab, boolean state )
    {
        if ( state )
        {
            int tabCount = tabbedPane.getTabCount();
            int modelPos = tab.getIndex();
            int pos;
            if ( tabCount == 0 )
            {
                pos = 0;
            }
            else
            {
                pos = tabCount;
                // check prev tab to find right position to show
                FWTab tmpTab = (FWTab) tabbedPane.getComponentAt( pos - 1 );
                int tmpIdx = tmpTab.getIndex();
                while( tmpIdx > modelPos )
                {
                    pos --;
                    if ( pos == 0 )
                    {//we are at the first position.
                        break;
                    }
                    tmpTab = (FWTab) tabbedPane.getComponentAt( pos - 1 );
                    tmpIdx = tmpTab.getIndex();
                }
            }

            int orgTabCount = tabbedPane.getTabCount();


            if ( orgTabCount == 0 )
            {
                getContentPane().remove( getLogoPanel() );
                getContentPane().add( BorderLayout.CENTER, tabbedPane );
                tabbedPane.setVisible( true );
                getContentPane().invalidate();
                getContentPane().repaint();
            }
            tabbedPane.insertTab( tab.getName(), tab.getIcon(), tab,
                tab.getToolTip(), pos );
            tabbedPane.setSelectedIndex( pos );
            if ( orgTabCount == 0 )
            {
                tab.setVisible( true );
            }
        }
        else
        {
            tabbedPane.remove( tab );

            if ( tabbedPane.getTabCount() == 0 )
            {
                getContentPane().remove( tabbedPane );
                getContentPane().add( BorderLayout.CENTER, getLogoPanel() );
                getContentPane().invalidate();
                getContentPane().repaint();
                tabbedPane.setVisible( false );
            }
        }
    }

    public void setSelectedTab( int tabID )
    {
        FWTab tab = getTab( tabID );
        setSelectedTab( tab );
    }

    public FWTab getSelectedTab()
    {
        if ( tabbedPane.getTabCount() == 0 )
        {
            return null;
        }
        return (FWTab)tabbedPane.getSelectedComponent();
    }

    public void setSelectedTab( FWTab tab )
    {
        // Sanity check
        if (tab == null)
        {
            return;
        }

        // If the tab is not visible, then first make it visible.
        FWToggleAction action = tab.getToggleTabViewAction();
        // hope the action selected state is always matching the tab visible state...
        if ( !action.isSelected() )
        {
            action.actionPerformed( new ActionEvent( this, 0, null ) );
        }

        // Select the tab.
        tabbedPane.setSelectedComponent( tab );
    }

    public FWTab getTab( int tabID )
    {
        switch( tabID )
        {
            case NETWORK_TAB_ID:
                return networkTab;
            //case SEARCH_TAB_ID:
            //    return searchTab;
            case SEARCH_TAB_ID:
                return searchTab;
            case DOWNLOAD_TAB_ID:
                return swDownloadTab;
            case UPLOAD_TAB_ID:
                return uploadTab;
            case LIBRARY_TAB_ID:
                return libraryTab;
            case SEARCH_MONITOR_TAB_ID:
                return searchMonitorTab;
            case RESULT_MONITOR_TAB_ID:
                return resultMonitorTab;
            default:
                Logger.logWarning( Logger.GUI, "Unknown tab id: " + tabID );
                return null;
        }
    }

    public NetworkTab getNetworkTab()
    {
        return networkTab;
    }

    /*public SearchTab getSearchTab()
    {
        return searchTab;
    }*/

    public void saveGUISettings( XJBGUISettings xjbSettings )
        throws JAXBException
    {
        Rectangle bounds = getBounds();
        xjbSettings.setWindowHeight( bounds.height );
        xjbSettings.setWindowWidth( bounds.width );
        xjbSettings.setWindowPosX( bounds.x );
        xjbSettings.setWindowPosY( bounds.y );
        xjbSettings.setToolbarVisible( toolbar != null );
        networkTab.appendXJBGUISettings( xjbSettings );
        searchTab.appendXJBGUISettings( xjbSettings );
        searchTab.appendXJBGUISettings( xjbSettings );
        swDownloadTab.appendXJBGUISettings( xjbSettings );
        uploadTab.appendXJBGUISettings( xjbSettings );
        libraryTab.appendXJBGUISettings( xjbSettings );
        securityTab.appendXJBGUISettings( xjbSettings );
        statisticsTab.appendXJBGUISettings( xjbSettings );
        searchMonitorTab.appendXJBGUISettings( xjbSettings );
        resultMonitorTab.appendXJBGUISettings( xjbSettings );
    }
    
    private void initializeTab( FWTab tab, int tabID, XJBGUISettings guiSettings )
    {
//long start = System.currentTimeMillis();
        XJBGUITab xjbTab = GUIUtils.getXJBGUITabById( guiSettings, tabID );
        boolean state = tab.isVisibleByDefault();
        if ( xjbTab != null && xjbTab.isSetVisible() )
        {
            state = xjbTab.isVisible();
        }
        setTabVisible( tab, state );
//long stop = System.currentTimeMillis();
//System.out.println( tabID + " - " + (stop - start) + "" );
    }

    private JPanel getLogoPanel()
    {
        if ( logoPanel == null )
        {
			ImageIcon icon = new ImageIcon( MainFrame.class.getResource(
				SplashWindow.SPLASH_IMAGE_NAME ) );
			Image image = icon.getImage();
            logoPanel = new FWLogoPanel( image );
            logoPanel.setBorder( BorderFactory.createLoweredBevelBorder() );
        }
        return logoPanel;
    }
    
    public boolean isToolbarVisible()
    {
        return toolbar != null;
    }

    public void setToolbarVisible( boolean state )
    {
        if ( state )
        {
            if ( toolbar != null)
            {
                //already visible
                return;
            }
            toolbar = new FWToolBar( JToolBar.HORIZONTAL );
            toolbar.setShowText( false );
    
            toolbar.addAction( GUIRegistry.getInstance().getGlobalAction(
                GUIRegistry.EXIT_PHEX_ACTION ) );
            toolbar.addSeparator();
            toolbar.addAction( GUIRegistry.getInstance().getGlobalAction(
                GUIRegistry.CONNECT_NETWORK_ACTION ) );
            toolbar.addAction( GUIRegistry.getInstance().getGlobalAction(
                GUIRegistry.DISCONNECT_NETWORK_ACTION ) );
            toolbar.addSeparator();
    
            toolbar.addAction( networkTab.getToggleTabViewAction() );
            //toolbar.addAction( searchTab.getToggleTabViewAction() );
            toolbar.addAction( searchTab.getToggleTabViewAction() );
            toolbar.addAction( swDownloadTab.getToggleTabViewAction() );
            toolbar.addAction( uploadTab.getToggleTabViewAction() );
            toolbar.addAction( libraryTab.getToggleTabViewAction() );
            toolbar.addAction( securityTab.getToggleTabViewAction() );
            toolbar.addAction( statisticsTab.getToggleTabViewAction() );
            toolbar.addAction( searchMonitorTab.getToggleTabViewAction() );
            toolbar.addAction( resultMonitorTab.getToggleTabViewAction() );
            getContentPane().add(BorderLayout.NORTH, toolbar);
            getContentPane().validate();
        }
        else
        {
            getContentPane().remove( toolbar );
            getContentPane().validate();
            toolbar = null;
        }
    }

    private JMenuBar createMenuBar( XJBGUISettings xjbSettings )
    {
        JMenuBar menuBar = new JMenuBar();

        JMenu networkMenu = new JMenu( Localizer.getString( "Network" ) );
        networkMenu.setMnemonic( Localizer.getChar( "NetworkMnemonic" ) );
        
        FWAction action;
        GUIRegistry guiRegistry = GUIRegistry.getInstance();
        FWMenu newMenu = new FWMenu( Localizer.getString( "New" ));
        action = guiRegistry.getGlobalAction(
            GUIRegistryConstants.NEW_DOWNLOAD_ACTION );
        newMenu.addAction( action );
        networkMenu.add( newMenu );
        action = guiRegistry.getGlobalAction( "ConnectNetworkAction" );
        networkMenu.add( action );
        action = guiRegistry.getGlobalAction( "DisconnectNetworkAction" );
        networkMenu.add( action );
        networkMenu.addSeparator();
        action = guiRegistry.getGlobalAction( "SwitchNetworkAction" );
        networkMenu.add( action );
        networkMenu.addSeparator();
        action = guiRegistry.getGlobalAction( "ExitPhexAction" );
        networkMenu.add( action );
        menuBar.add( networkMenu );

        FWMenu viewMenu = new FWMenu( Localizer.getString( "View" ) );
        viewMenu.setMnemonic( Localizer.getChar( "ViewMnemonic" ) );
        boolean isToolbarVisible = true;
        if ( xjbSettings != null && xjbSettings.isSetToolbarVisible() )
        {
            isToolbarVisible = xjbSettings.isToolbarVisible();
        }
        viewMenu.addAction( new ToggleToolbarAction( isToolbarVisible ) );
        viewMenu.addSeparator();
        viewMenu.addAction( networkTab.getToggleTabViewAction() );
        viewMenu.addAction( searchTab.getToggleTabViewAction() );
        viewMenu.addAction( swDownloadTab.getToggleTabViewAction() );
        viewMenu.addAction( uploadTab.getToggleTabViewAction() );
        viewMenu.addAction( libraryTab.getToggleTabViewAction() );
        viewMenu.addAction( securityTab.getToggleTabViewAction() );
        viewMenu.addAction( statisticsTab.getToggleTabViewAction() );
        viewMenu.addAction( searchMonitorTab.getToggleTabViewAction() );
        viewMenu.addAction( resultMonitorTab.getToggleTabViewAction() );
        menuBar.add( viewMenu );

        FWMenu settingsMenu = new FWMenu( Localizer.getString( "Settings" ) );
        settingsMenu.setMnemonic( Localizer.getChar( "SettingsMnemonic" ) );
        action = new ViewOptionsAction();
        settingsMenu.addAction( action );
        settingsMenu.addSeparator();
        action = new FilteredPortsAction();
        settingsMenu.addAction( action );
        settingsMenu.addSeparator();
        action = new RescanSharedFilesAction();
        settingsMenu.addAction( action );
        menuBar.add( settingsMenu );

        FWMenu helpMenu = new FWMenu( Localizer.getString( "Help" ) );
        helpMenu.setMnemonic( Localizer.getChar( "HelpMnemonic" ) );
        helpMenu.addAction( new OpenURLAction( Localizer.getString(
            "PhexHomepage" ), "http://phex.kouk.de",
            guiRegistry.getIconFactory().getIcon( "Network" ),
            Localizer.getString( "TTTPhexHomepage" ), new Integer(
            Localizer.getChar( "PhexHomepageMnemonic" ) ), null ) );
        helpMenu.addAction( new OpenURLAction( Localizer.getString(
            "PhexForum" ), "http://www.gnutellaforums.com/forumdisplay.php?s=&forumid=16",
            guiRegistry.getIconFactory().getIcon( "Network" ),
            Localizer.getString( "TTTPhexForum" ), new Integer(
            Localizer.getChar( "PhexForumMnemonic" ) ), null ) );
        helpMenu.addSeparator();
        helpMenu.addAction( new ViewAboutAction( ) );
        menuBar.add( helpMenu );

//TODO2 intergrate improved find in tables... see ActionQueryFindResult


        return menuBar;
    }

    /**
     * Class to handle the WindowClosing event on the main frame.
     *
     */
    private class WindowHandler extends WindowAdapter
    {
        /**
         * Just delegate to the ExitPhexAction acion.
         */
        public void windowClosing(WindowEvent e)
        {
            ExitPhexAction.performCloseGUIAction();
        }
        
        public void windowOpened(WindowEvent e)
        {
            if ( GUIRegistry.getInstance().isRespectCopyrightNoticeShown() )
            {
                RespectCopyrightDialog dialog = new RespectCopyrightDialog( );
                dialog.setVisible(true);
            }
        }
    }

    private class DesktopIndicatorHandler implements DesktopIndicatorListener
    {
        public void onDesktopIndicatorClicked( DesktopIndicator source )
        {
            setVisible(true);
            source.hideIndicator();
            if ( MainFrame.this.getState() != JFrame.NORMAL )
            {
                MainFrame.this.setState( Frame.NORMAL );
            }
            MainFrame.this.requestFocus();
        }
    }

    private class RefreshHandler implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            networkTab.refresh();
        }
    }
}
