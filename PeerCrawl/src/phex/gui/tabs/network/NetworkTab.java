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
 *  $Id: NetworkTab.java,v 1.27 2005/11/13 10:17:45 gregork Exp $
 */
package phex.gui.tabs.network;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.text.Keymap;
import javax.xml.bind.JAXBException;

import phex.chat.ChatManager;
import phex.common.ServiceManager;
import phex.common.ThreadPool;
import phex.common.address.DestAddress;
import phex.common.address.IpAddress;
import phex.common.address.MalformedDestAddressException;
import phex.connection.NetworkManager;
import phex.event.NetworkListener;
import phex.gui.actions.FWAction;
import phex.gui.actions.GUIActionPerformer;
import phex.gui.common.*;
import phex.gui.common.table.FWSortedTableModel;
import phex.gui.common.table.FWTable;
import phex.gui.common.table.FWTableColumnModel;
import phex.gui.models.NetworkTableModel;
import phex.gui.renderer.NetworkRowRenderer;
import phex.gui.tabs.FWTab;
import phex.gwebcache.GWebCacheContainer;
import phex.gwebcache.GWebCacheManager;
import phex.host.*;
import phex.msg.MsgManager;
import phex.net.presentation.PresentationManager;
import phex.utils.Localizer;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;
import phex.xml.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * The NetworkTab Panel.
 */
public class NetworkTab extends FWTab
{
    private static final String NETWORK_TABLE_IDENTIFIER = "NetworkTable";

    private static final Host[] EMPTY_HOST_ARRAY = new Host[0];

    private MainFrame mainFrame;
    private HostManager hostMgr;
    private NetworkHostsContainer hostsContainer;
    private GWebCacheContainer gWebCacheCont;
    private MsgManager msgManager;

    private FWTable networkTable;
    private JScrollPane networkTableScrollPane;
    private FWTableColumnModel networkColumnModel;
    private NetworkTableModel networkModel;
    private FWPopupMenu networkPopup;

    private JLabel myIPLabel;
    private DefaultComboBoxModel connectToComboModel;
    private JComboBox connectToComboBox;
    
    
    

    private JLabel catcherStatLabel;
    private JLabel gWebCacheStatLabel;


    public NetworkTab( MainFrame frame )
    {
        super( MainFrame.NETWORK_TAB_ID, Localizer.getString( "GnutellaNet"),
            GUIRegistry.getInstance().getIconFactory().getIcon( "Network" ),
            Localizer.getString( "TTTGnutellaNet"), Localizer.getChar(
            "GnutellaNetMnemonic"), KeyStroke.getKeyStroke( Localizer.getString(
            "GnutellaNetAccelerator" ) ), MainFrame.NETWORK_TAB_INDEX );
        mainFrame = frame;
        hostMgr = HostManager.getInstance();
        hostsContainer = hostMgr.getNetworkHostsContainer();
        gWebCacheCont = GWebCacheManager.getInstance().getGWebCacheContainer();
        msgManager = MsgManager.getInstance();
    }

    public void initComponent( XJBGUISettings guiSettings )
    {
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
            "2dlu, fill:d:grow, 2dlu", // columns
            "2dlu, fill:d:grow, 4dlu, d, 2dlu"); //rows
        PanelBuilder contentBuilder = new PanelBuilder( layout, this );

        
        //JPanel upperPanel = new FormDebugPanel();
        JPanel upperPanel = new JPanel( );
        FWElegantPanel upperElegantPanel = new FWElegantPanel( Localizer.getString("Connections"),
            upperPanel );
        layout = new FormLayout(
            "0dlu, d, 2dlu, d, 10dlu:grow, d, 2dlu, d, 2dlu, d, 0dlu", // columns
            "fill:d:grow, 3dlu, p"); //rows
        PanelBuilder upperBuilder = new PanelBuilder( layout, upperPanel );

        networkModel = new NetworkTableModel();
        XJBGUITable xmlTable = GUIUtils.getXJBGUITableByIdentifier( guiSettings,
            NETWORK_TABLE_IDENTIFIER );
        buildNetworkTableColumnModel( xmlTable );
        networkTable = new FWTable( new FWSortedTableModel( networkModel ),
            networkColumnModel );
        
        // TODO3 try for a improced table sorting strategy.
        //((FWSortedTableModel)networkTable.getModel()).setTable( networkTable );
        
        networkTable.activateAllHeaderActions();
        networkTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        networkTable.getSelectionModel().addListSelectionListener(
            new SelectionHandler() );
        MouseHandler mouseHandler = new MouseHandler();
        networkTable.addMouseListener( mouseHandler );
        networkTableScrollPane = FWTable.createFWTableScrollPane( networkTable );
        networkTableScrollPane.addMouseListener( mouseHandler );
        
        upperBuilder.add( networkTableScrollPane, cc.xywh( 2, 1, 9, 1 ) );
        
        JLabel label = new JLabel( Localizer.getString( "NetworkTab_MyAddress" ) );
        upperBuilder.add( label, cc.xy( 2, 3 ) );
        myIPLabel = new JLabel( "" );
        myIPLabel.addMouseListener( new MouseAdapter()
            {
            public void mouseReleased(MouseEvent e)
            {
                if (e.isPopupTrigger())
                {
                    popupMenu((Component)e.getSource(), e.getX(), e.getY());
                }
            }

            public void mousePressed(MouseEvent e)
            {
                if (e.isPopupTrigger())
                {
                    popupMenu((Component)e.getSource(), e.getX(), e.getY());
                }
            }

            private void popupMenu(Component source, int x, int y)
            {
                JPopupMenu menu = new JPopupMenu();
                menu.add( new CopyMyIpAction() );
                menu.show( source, x, y );
            }
            });
        upperBuilder.add( myIPLabel, cc.xy( 4, 3 ) );

        label = new JLabel( Localizer.getString( "ConnectTo" )
            + Localizer.getChar( "ColonSign" ) );
        upperBuilder.add( label, cc.xy( 6, 3 ) );

// TODO2 add connection and disconnect network buttons to ConnectTo status line
//       because it is not available from toolbar anymore...

        ConnectToHostHandler connectToHostHandler = new ConnectToHostHandler();
                
        connectToComboModel = new DefaultComboBoxModel(
            ServiceManager.sCfg.connectToHistory.toArray() );
        connectToComboBox = new JComboBox( connectToComboModel );
        connectToComboBox.setEditable( true );
        JTextField editor = ((JTextField)connectToComboBox.getEditor().getEditorComponent());
        Keymap keymap = JTextField.addKeymap( "ConnectToEditor", editor.getKeymap() );
        editor.setKeymap( keymap );
        keymap.addActionForKeyStroke( KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            connectToHostHandler );
        GUIUtils.assignKeymapToComboBoxEditor( keymap, connectToComboBox );
        connectToComboBox.setSelectedItem( "" );
        connectToComboBox.setPrototypeDisplayValue("123.123.123.123:12345");
        upperBuilder.add( connectToComboBox, cc.xy( 8, 3 ) );

        JButton connectHostButton = new JButton( Localizer.getString( "Connect" ) );
        connectHostButton.addActionListener( connectToHostHandler );
        upperBuilder.add( connectHostButton, cc.xy( 10, 3 ) );
        
        /////////////////////////// Lower Panel ////////////////////////////////

        JPanel lowerPanel = new JPanel();
        //JPanel lowerPanel = new FormDebugPanel();
        layout = new FormLayout(
            "d, fill:10dlu:grow, d", // columns
            "top:p"); //rows
        layout.setColumnGroups( new int[][]{{1, 3}} );
        PanelBuilder lowerBuilder = new PanelBuilder( layout, lowerPanel );

        NetFavoritesPanel favoritesPanel = new NetFavoritesPanel();
        lowerBuilder.add( favoritesPanel, cc.xy( 1, 1 ) );
        
                
        JPanel cacheStatusPanel = new JPanel( );
        //JPanel cacheStatusPanel = new FormDebugPanel();
        layout = new FormLayout(
            "8dlu, right:d, 2dlu, right:d, 2dlu, d, 2dlu:grow, 8dlu", // columns
            "p, 3dlu, p, 3dlu, p, 3dlu, bottom:p:grow"); //rows
        PanelBuilder cacheStatusBuilder = new PanelBuilder( layout, cacheStatusPanel );
        lowerBuilder.add( cacheStatusPanel, cc.xy( 3, 1 ) );
        
        cacheStatusBuilder.addSeparator( Localizer.getString( "NetworkTab_ConnectionInfo" ),
            cc.xywh( 1, 1, 8, 1 ) );
            
        cacheStatusBuilder.addLabel( Localizer.getString( "NetworkTab_HostCacheContains" ), 
            cc.xy( 2, 3 ) );
        catcherStatLabel = new JLabel(  );
        cacheStatusBuilder.add( catcherStatLabel, cc.xy( 4, 3 ) );
        cacheStatusBuilder.addLabel( Localizer.getString( "NetworkTab_Hosts" ), 
            cc.xy( 6, 3 ) );

        cacheStatusBuilder.addLabel( Localizer.getString( "NetworkTab_GWebCacheContains" ), 
            cc.xy( 2, 5 ) );
        gWebCacheStatLabel = new JLabel(  );
        cacheStatusBuilder.add( gWebCacheStatLabel, cc.xy( 4, 5 ) );
        cacheStatusBuilder.addLabel( Localizer.getString( "NetworkTab_Caches" ), 
            cc.xy( 6, 5 ) );
        
        final JButton queryWebCache = new JButton( Localizer.getString( "QueryGWebCache" ) );
        queryWebCache.setToolTipText( Localizer.getString( "TTTQueryGWebCache" ) );
        queryWebCache.addActionListener( new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    queryWebCache.setEnabled( false );
                    Runnable runner = new Runnable()
                    {
                        public void run()
                        {
                            try
                            {
                                gWebCacheCont.queryMoreHosts( false );
                                gWebCacheCont.queryMoreGWebCaches( false );
                            }
                            catch ( Throwable th )
                            {
                                NLogger.error( NLoggerNames.GLOBAL, th, th );
                            }
                            finally
                            {
                                queryWebCache.setEnabled( true );
                            }
                        }
                    };
                    ThreadPool.getInstance().addJob( runner,
                        "UserGWebCacheQuery-" + Integer.toHexString(runner.hashCode()) );
                }
            } );
        cacheStatusBuilder.add( queryWebCache, cc.xywh( 2, 7, 5, 1 ) );

        // Workaround for very strange j2se 1.4 split pane layout behaivor
        /*Dimension nullDim = new Dimension( 0, 0 );
        upperPanel.setMinimumSize( nullDim );
        lowerPanel.setMinimumSize( nullDim );

        Dimension dim = new Dimension( 400, 200 );
        upperPanel.setPreferredSize( dim );
        lowerPanel.setPreferredSize( dim );

        JSplitPane splitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT, upperPanel,
            lowerPanel );
        splitPane.setBorder( BorderFactory.createEmptyBorder( 4, 4, 4, 4) );
        splitPane.setDividerSize( 4 );
        splitPane.setOneTouchExpandable( false );
        splitPane.setResizeWeight( 0.5 );
        splitPane.setDividerLocation( 0.25 );*/
        
        contentBuilder.add( upperElegantPanel, cc.xy( 2, 2 ) );
        contentBuilder.add( lowerPanel, cc.xy( 2, 4 ) );
        //add(BorderLayout.CENTER, upperPanel );
        //add(BorderLayout.SOUTH, lowerPanel );

        // Set up cell renderer to provide the correct colour based on connection.
        NetworkRowRenderer networkRowRenderer = new NetworkRowRenderer();
        Enumeration enumr = networkColumnModel.getColumns();
        while ( enumr.hasMoreElements() )
        {
            TableColumn column = (TableColumn)enumr.nextElement();
            column.setCellRenderer( networkRowRenderer );
        }

        // Setup popup menu...
        networkPopup = new FWPopupMenu();

        FWAction action;
        action = new DisconnectHostAction();
        addTabAction( DISCONNECT_HOST_ACTION_KEY, action );
        
        networkTable.getActionMap().put( DISCONNECT_HOST_ACTION_KEY, action);
        networkTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put( 
            (KeyStroke)action.getValue(FWAction.ACCELERATOR_KEY), DISCONNECT_HOST_ACTION_KEY );
        networkPopup.addAction( action );

        networkPopup.addSeparator();
        
        action = new AddToFavoritesAction();
        addTabAction( ADD_TO_FAVORITES_ACTION_KEY, action );
        networkPopup.addAction( action );

        action = new BrowseHostAction();
        addTabAction( BROWSE_HOST_ACTION_KEY, action );
        //networkToolbar.addAction( action );
        networkPopup.addAction( action );

        action = new ChatToHostAction();
        addTabAction( CHAT_TO_HOST_ACTION_KEY, action );
        //networkToolbar.addAction( action );
        networkPopup.addAction( action );

        action = new BanHostAction();
        addTabAction( IGNORE_HOST_ACTION_KEY, action );
        //networkToolbar.addAction( action );
        networkPopup.addAction( action );

        networkPopup.addSeparator();


        JMenu netMenu = new JMenu( Localizer.getString( "Network" ) );
        netMenu.add( GUIRegistry.getInstance().getGlobalAction(
            GUIRegistry.CONNECT_NETWORK_ACTION ) );
        netMenu.add( GUIRegistry.getInstance().getGlobalAction(
            GUIRegistry.DISCONNECT_NETWORK_ACTION ) );
        /*netMenu.add( GUIRegistry.getInstance().getGlobalAction(
            GUIRegistry.JOIN_NETWORK_ACTION ) );*/
        networkPopup.add( netMenu );

        NetworkManager networkMgr = NetworkManager.getInstance();
        IPChangedListener ipListener = new IPChangedListener();
        ipListener.networkIPChanged( networkMgr.getLocalAddress() );
        networkMgr.addNetworkListener( ipListener );
    }

    /**
     * This is overloaded to update the combo box size on
     * every UI update. Like font size change!
     */
    public void updateUI()
    {
        super.updateUI();

        if ( connectToComboBox != null )
        {
            GUIUtils.adjustComboBoxHeight( connectToComboBox );
            ListCellRenderer renderer = connectToComboBox.getRenderer();
            if ( renderer != null )
            {
                FontMetrics fm = connectToComboBox.getFontMetrics( connectToComboBox.getFont() );
                int width = fm.getMaxAdvance() * 15;
                Dimension dim = connectToComboBox.getMaximumSize();
                dim.width = Math.min( width, dim.width );

                dim = connectToComboBox.getPreferredSize();
                dim.width = Math.min( width, dim.width );
            }
        }

        if ( networkTableScrollPane != null )
        {
            FWTable.updateFWTableScrollPane( networkTableScrollPane );
        }
    }

    /**
     * Stores NetworkTab settings in XJB object model.
     */
    public void appendXJBGUISettings( XJBGUISettings xjbSettings )
        throws JAXBException
    {
        super.appendXJBGUISettings( xjbSettings );
        XJBGUITableColumnList xjbList = networkColumnModel.createXJBGUITableColumnList();
        ObjectFactory objFactory = new ObjectFactory();
        XJBGUITable xjbTable = objFactory.createXJBGUITable();
        xjbTable.setTableColumnList( xjbList );
        xjbTable.setTableIdentifier( NETWORK_TABLE_IDENTIFIER );
        xjbSettings.getTableList().getTableList().add( xjbTable );
    }

    /**
     * Method is called when the tab will be selected in the tabbed pane. Can be
     * overloaded to do some action.
     */
    public void tabSelectedNotify()
    {
        refresh();
    }

    /**
     * Refreshes the content of the catcherStatLabel, the gWebCacheStatLabel and
     * the networkTable.
     */
    public void refresh()
    {
        catcherStatLabel.setText( String.valueOf( 
            hostMgr.getCaughtHostsContainer().getCaughtHostsCount() ) );
        gWebCacheStatLabel.setText( String.valueOf( gWebCacheCont.getGWebCacheCount() ) );
        networkModel.fireTableDataChanged();
    }

    /**
     * Builds the network table column model out of the table settings XJB object.
     * @param tableSettings
     */
    private void buildNetworkTableColumnModel( XJBGUITable tableSettings )
    {
        int[] columnIds = NetworkTableModel.getColumnIdArray();
        XJBGUITableColumnList columnList = null;
        if ( tableSettings != null )
        {
            columnList = tableSettings.getTableColumnList();
        }

        networkColumnModel = new FWTableColumnModel( networkModel,
            columnIds, columnList );
    }

    

    private Host[] getSelectedHosts()
    {
        int[] viewRows = networkTable.getSelectedRows();
        if ( viewRows.length == 0 )
        {
            return EMPTY_HOST_ARRAY;
        }
        int[] modelRows = networkTable.convertRowIndicesToModel( viewRows );

        Host[] hosts = hostsContainer.getNetworkHostsAt( modelRows );
        return hosts;
    }

    private Host getSelectedHost()
    {
        int viewRow = networkTable.getSelectedRow();
        int modelRow = networkTable.convertRowIndexToModel( viewRow );
        if ( modelRow < 0 )
        {
            return null;
        }
        Host hosts = hostsContainer.getNetworkHostAt( modelRow );
        return hosts;
    }

    //////////////////////// Actions ///////////////////////////////////////////

    private static final String DISCONNECT_HOST_ACTION_KEY = "DisconnectHostAction";
    private static final String IGNORE_HOST_ACTION_KEY = "IgnoreHostAction";
    private static final String CHAT_TO_HOST_ACTION_KEY = "ChatToHostAction";
    private static final String BROWSE_HOST_ACTION_KEY = "BrowseHostAction";
    private static final String ADD_TO_FAVORITES_ACTION_KEY = "AddToFavoritesAction";

    private class DisconnectHostAction extends FWAction
    {
        public DisconnectHostAction()
        {
            super( Localizer.getString( "DisconnectHost" ),
                GUIRegistry.getInstance().getIconFactory().getIcon("DisconnectHost"),
                Localizer.getString( "TTTDisconnectHost" ), null, 
                KeyStroke.getKeyStroke( KeyEvent.VK_DELETE, 0 ) );
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            Host[] hosts = getSelectedHosts();
            hostMgr.removeNetworkHosts( hosts );
        }

        public void refreshActionState()
        {
            if ( networkTable.getSelectedRowCount() > 0 )
            {
                setEnabled( true );
            }
            else
            {
                setEnabled( false );
            }
        }
    }

    private class BanHostAction extends FWAction
    {
        public BanHostAction()
        {
            super( Localizer.getString( "BanHost" ),
                GUIRegistry.getInstance().getIconFactory().getIcon( "Ban" ),
                Localizer.getString( "TTTBanHost" ) );
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            try
            {
                Host[] hosts = getSelectedHosts();
                hostMgr.removeNetworkHosts( hosts );
                
                final DestAddress[] addresses = new DestAddress[hosts.length];
                for (int i = 0; i < hosts.length; i++)
                {
                    addresses[ i ] = hosts[i].getHostAddress();
                }
                Runnable runner = new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            GUIActionPerformer.banHosts( addresses );
                        }
                        catch ( Throwable th )
                        {
                            NLogger.error(NLoggerNames.USER_INTERFACE, th, th);
                        }
                    }
                };
                ThreadPool.getInstance().addJob(runner, "BanHostsAction" );
            }
            catch ( Throwable th )
            {
                NLogger.error(NLoggerNames.USER_INTERFACE, th, th);
            }
            refreshActionState();
        }

        public void refreshActionState()
        {
            if ( networkTable.getSelectedRowCount() > 0 )
            {
                setEnabled( true );
            }
            else
            {
                setEnabled( false );
            }
        }
    }
    
    private class AddToFavoritesAction extends FWAction
    {
        public AddToFavoritesAction()
        {
            super( Localizer.getString( "AddToFavorites" ),
                GUIRegistry.getInstance().getIconFactory().getIcon( "FavoriteHost" ),
                Localizer.getString( "TTTAddToFavorites" ) );
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            Host[] hosts = getSelectedHosts();
            DestAddress[] addresses = new DestAddress[hosts.length];
            for (int i = 0; i < hosts.length; i++)
            {
                addresses[ i ] = hosts[i].getHostAddress();
            }
            GUIActionPerformer.addHostsToFavorites( addresses );
        }

        public void refreshActionState()
        {
            if ( networkTable.getSelectedRowCount() > 0 )
            {
                setEnabled( true );
            }
            else
            {
                setEnabled( false );
            }
        }
    }

    private class ChatToHostAction extends FWAction
    {
        public ChatToHostAction()
        {
            super( Localizer.getString( "ChatToHost" ),
                GUIRegistry.getInstance().getIconFactory().getIcon("Chat"),
                Localizer.getString( "TTTChatToHost" ) );
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            Host host = getSelectedHost();
            if ( host == null )
            {
                return;
            }

            ChatManager.getInstance().openChat( host.getHostAddress() );
        }

        public void refreshActionState()
        {
            if ( networkTable.getSelectedRowCount() == 1 )
            {
                Host host = getSelectedHost();
                if ( host != null )
                {
                    setEnabled( true );
                    return;
                }
            }
            setEnabled( false );
        }
    }

    private class BrowseHostAction extends FWAction
    {
        public BrowseHostAction()
        {
            super( Localizer.getString( "BrowseHost" ),
                GUIRegistry.getInstance().getIconFactory().getIcon("BrowseHost"),
                Localizer.getString( "TTTBrowseHost" ) );
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            Host host = getSelectedHost();
            if ( host == null )
            {
                return;
            }
            GUIActionPerformer.browseHost( host.getHostAddress() );
        }

        public void refreshActionState()
        {
            if ( networkTable.getSelectedRowCount() == 1 )
            {
                Host host = getSelectedHost();
                if ( host != null )
                {
                    setEnabled( true );
                    return;
                }
            }
            setEnabled( false );
        }
    }


    /////////////////////// inner classes //////////////////////////////

    private class IPChangedListener implements NetworkListener
    {
        public void networkIPChanged( DestAddress localAddress )
        {
            String newAddress = localAddress.getFullHostName();
            if ( newAddress.equals( myIPLabel.getText() ) )
            {
                return;
            }
            myIPLabel.setText( newAddress );
            String countryCode = localAddress.getCountryCode();
            Icon icon = null;
            if ( countryCode != null && countryCode.length() > 0 )
            {
                icon = GUIRegistry.getInstance().getIconFactory().getIcon(
                    "Flag_" + countryCode );
            }
            myIPLabel.setIcon( icon );
        }

        public void connectedToNetwork()
        { /* not implemented */ }
        public void disconnectedFromNetwork()
        { /* not implemented */ }
    }

    private class SelectionHandler implements ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent e)
        {
            if ( !e.getValueIsAdjusting() )
            {
                refreshTabActions();
            }
        }
    }

    private class ConnectToHostHandler extends AbstractAction implements ActionListener
    {
        public void actionPerformed( ActionEvent e )
        {
            String str = (String)connectToComboBox.getEditor().getItem();
            connectToComboModel.setSelectedItem( str );
            str = str.trim();
            if ( str.length() == 0 )
            {
                return;
            }

            int idx = connectToComboModel.getIndexOf( str );
            if ( idx < 0 )
            {
                connectToComboModel.insertElementAt( str, 0 );
                if ( connectToComboModel.getSize() >
                    ServiceManager.sCfg.maxConnectToHistorySize )
                {
                    connectToComboModel.removeElementAt(
                        connectToComboModel.getSize() - 1 );
                }
                saveConnectToHostList();
            }
            else if ( idx > 0 )
            {
                connectToComboModel.removeElementAt( idx );
                connectToComboModel.insertElementAt( str, 0 );
                saveConnectToHostList();
            }
            connectToHost( str );
            connectToComboBox.setSelectedItem( "" );
        }

        private void connectToHost( String hostAddr )
        {
            if (hostAddr.length() == 0)
            {
                return;
            }
            StringTokenizer	tokens = new StringTokenizer(hostAddr, ";");
            String firstHost = tokens.nextToken();

            // Add new host and connect.
            try
            {
                DestAddress address = PresentationManager.getInstance().createHostAddress(
                    firstHost, IpAddress.DEFAULT_PORT );
                hostMgr.getNetworkHostsContainer().createOutgoingConnectionToHost(
                    address );
            }
            catch ( MalformedDestAddressException exp )
            {
            }
            networkModel.fireTableDataChanged();

            while (tokens.hasMoreTokens())
            {
                String hostString = tokens.nextToken();
                try
                {
                    DestAddress address = PresentationManager.getInstance().createHostAddress(
                        hostString, IpAddress.DEFAULT_PORT );
                    hostMgr.getCaughtHostsContainer().addCaughtHost( address,
                        CaughtHostsContainer.HIGH_PRIORITY );
                }
                catch (MalformedDestAddressException exp)
                {
                }                
            }
        }

        private void saveConnectToHostList()
        {
            int length = connectToComboModel.getSize();
            ArrayList ipList = new ArrayList( length );
            for ( int i = 0; i < length; i++ )
            {
                ipList.add( connectToComboModel.getElementAt( i ) );
            }
            ServiceManager.sCfg.connectToHistory.clear();
            ServiceManager.sCfg.connectToHistory.addAll( ipList );
            ServiceManager.sCfg.save();
        }
    }

    private class MouseHandler extends MouseAdapter implements MouseListener
    {
        public void mouseReleased(MouseEvent e)
        {
            if (e.isPopupTrigger())
            {
                popupMenu((Component)e.getSource(), e.getX(), e.getY());
            }
        }

        public void mousePressed(MouseEvent e)
        {
            if (e.isPopupTrigger())
            {
                popupMenu((Component)e.getSource(), e.getX(), e.getY());
            }
        }

        private void popupMenu(Component source, int x, int y)
        {
            if (source == networkTable || source == networkTableScrollPane )
            {
                networkPopup.show(source, x, y);
            }
        }
    }
    
    //////////////////////////////////////////////////////////////////////////
    /// Actions
    //////////////////////////////////////////////////////////////////////////

    /**
     * Starts a download.
     */
    class CopyMyIpAction extends FWAction
    {
        CopyMyIpAction()
        {
            super( Localizer.getString( "Copy" ),
                IconFactory.EMPTY_IMAGE_16,
                Localizer.getString( "TTTCopyMyIP" ) );
        }

        public void actionPerformed( ActionEvent e )
        {
            DestAddress address = NetworkManager.getInstance().getLocalAddress();
            StringSelection data = new StringSelection( address.getFullHostName() );
            Clipboard clipboard =
              Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(data, data);
        }

        /**
         * @see phex.gui.actions.FWAction#refreshActionState()
         */
        public void refreshActionState()
        {   
        }
    }
}
