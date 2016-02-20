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
 *  $Id: SWDownloadTab.java,v 1.14 2005/11/19 20:59:34 gregork Exp $
 */
package phex.gui.tabs.download;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.xml.bind.JAXBException;

import phex.chat.ChatManager;
import phex.common.ServiceManager;
import phex.common.ThreadPool;
import phex.common.URN;
import phex.common.address.DestAddress;
import phex.common.format.NumberFormatUtils;
import phex.download.log.LogBuffer;
import phex.download.strategy.*;
import phex.download.swarming.SWDownloadCandidate;
import phex.download.swarming.SWDownloadFile;
import phex.download.swarming.SwarmingManager;
import phex.event.DownloadFilesChangeListener;
import phex.gui.actions.FWAction;
import phex.gui.actions.FWToggleAction;
import phex.gui.actions.GUIActionPerformer;
import phex.gui.common.*;
import phex.gui.common.table.FWSortedTableModel;
import phex.gui.common.table.FWTable;
import phex.gui.common.table.FWTableColumnModel;
import phex.gui.dialogs.DownloadConfigDialog;
import phex.gui.dialogs.LogBufferDialog;
import phex.gui.tabs.FWTab;
import phex.query.ResearchSetting;
import phex.utils.*;
import phex.xml.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


public class SWDownloadTab extends FWTab
{
    private static final String DOWNLOAD_TABLE_IDENTIFIER = "DownloadTable";
    private static final String CANDIDATE_TABLE_IDENTIFIER = "CandidateTable";
    private static final SWDownloadFile[] EMPTY_DOWNLOADFILE_ARRAY =
        new SWDownloadFile[0];
    private static final SWDownloadCandidate[] EMPTY_DOWNLOADCANDIDATE_ARRAY =
        new SWDownloadCandidate[0];

    private SwarmingManager swarmingMgr;

    private FWTable downloadTable;
    private JScrollPane downloadTableScrollPane;
    private FWTableColumnModel downloadColumnModel;
    private SWDownloadTableModel downloadModel;

    private JTabbedPane downloadDetails;
    private JPopupMenu downloadPopup;
    private JMenu downloadPopupSpeedMenu;
    
    private FWElegantPanel overviewElegantPanel;
    private DownloadOverviewPanel downloadOverviewPanel;
    
    private FWElegantPanel transfersElegantPanel;
    private DownloadTransfersPanel transfersPanel;

    private PanelBuilder candidatePanelBuilder;
    private FWElegantPanel candidateElegantPanel;
    private FWTable candidateTable;
    private JScrollPane candidateTableScrollPane;
    private FWTableColumnModel candidateColumnModel;
    private SWCandidateTableModel candidateModel;
    private JPopupMenu candidatePopup;

    private MainFrame mainFrame;
    

    
    public SWDownloadTab( MainFrame frame )
    {
        super( MainFrame.DOWNLOAD_TAB_ID, Localizer.getString( "Download" ),
            GUIRegistry.getInstance().getIconFactory().getIcon( "Download" ),
            Localizer.getString( "TTTDownloadTab" ),Localizer.getChar(
            "DownloadMnemonic"), KeyStroke.getKeyStroke( Localizer.getString(
            "DownloadAccelerator" ) ), MainFrame.DOWNLOAD_TAB_INDEX );
        swarmingMgr = SwarmingManager.getInstance();
        swarmingMgr.addDownloadFilesChangeListener( new DownloadFilesChangeHandler() );
        mainFrame = frame;
    }

    public void initComponent( XJBGUISettings guiSettings )
    {
        MouseHandler mouseHandler = new MouseHandler();
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
            "2dlu, fill:d:grow, 2dlu", // columns
            "2dlu, fill:d:grow, 2dlu"); //rows
        PanelBuilder tabBuilder = new PanelBuilder( layout, this );

        JPanel downloadTablePanel = initDownloadTablePanel( guiSettings, mouseHandler );
        initDownloadOverviewPanel( );
        JPanel downloadCandidatePanel = initDownloadCandidatePanel(guiSettings, mouseHandler);
        initDownloadTransferPanel( guiSettings );
        
        
        downloadDetails = new JTabbedPane( JTabbedPane.BOTTOM );
        downloadDetails.setBorder( BorderFactory.createEmptyBorder( 2, 0, 0, 0) );
        downloadDetails.addTab( Localizer.getString( "DownloadTab_Overview" ),
            overviewElegantPanel );
        downloadDetails.addTab( Localizer.getString( "DownloadTab_Transfers"),
            transfersElegantPanel );
        downloadDetails.addTab( Localizer.getString( "Candidates"),
            downloadCandidatePanel );

        // Workaround for very strange j2se 1.4 split pane layout behaivor
        Dimension dim = new Dimension( 400, 300 );
        downloadTablePanel.setPreferredSize( dim );
        downloadDetails.setPreferredSize( dim );
        dim = new Dimension( 0, 0 );
        downloadTablePanel.setMinimumSize( dim );
        downloadDetails.setMinimumSize( dim );
        

        JSplitPane splitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT, downloadTablePanel,
            downloadDetails );
        splitPane.setBorder( BorderFactory.createEmptyBorder( 0, 0, 0, 0) );
        splitPane.setDividerSize( 4 );
        splitPane.setOneTouchExpandable( false );
        splitPane.setDividerLocation( 0.5 );
        splitPane.setResizeWeight( 0.5 );

        tabBuilder.add( splitPane, cc.xy( 2, 2 ) );

        // increase table height a bit to display progress bar string better...
        GUIUtils.adjustTableProgresssBarHeight( downloadTable );
        GUIUtils.adjustTableProgresssBarHeight( candidateTable );

        //mDownloadMgr.addDownloadFilesChangeListener( new DownloadFilesChangeHandler() );
    }
    
    private void initDownloadOverviewPanel()
    {
        downloadOverviewPanel = new DownloadOverviewPanel();
        downloadOverviewPanel.initializeComponent( null );
        overviewElegantPanel = new FWElegantPanel( Localizer.getString("DownloadTab_DownloadOverview"),
            downloadOverviewPanel );
    }
    
    private void initDownloadTransferPanel( XJBGUISettings guiSettings )
    {
        transfersPanel = new DownloadTransfersPanel();
        transfersPanel.initializeComponent( guiSettings );
        transfersElegantPanel = new FWElegantPanel( Localizer.getString("DownloadTab_DownloadTransfers"),
            transfersPanel );
    }

    private JPanel initDownloadCandidatePanel(
        XJBGUISettings guiSettings,
        MouseHandler mouseHandler)
    {
        JPanel downloadCandidatePanel = new JPanel( );
        
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
            "fill:d:grow", // columns
            "fill:d:grow, 1dlu, p"); //rows
        candidatePanelBuilder = new PanelBuilder( layout, downloadCandidatePanel );
        
        candidateModel = new SWCandidateTableModel( downloadTable );
        XJBGUITable xjbTable = GUIUtils.getXJBGUITableByIdentifier( guiSettings,
            CANDIDATE_TABLE_IDENTIFIER );
        buildCandidateTableColumnModel( xjbTable );
        
        candidateTable = new FWTable( new FWSortedTableModel( candidateModel ),
            candidateColumnModel );
        candidateTable.activateAllHeaderActions();
        candidateTable.getSelectionModel().addListSelectionListener(
            new CandidateSelectionHandler() );
        candidateTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        candidateTable.addMouseListener( mouseHandler );
        GUIRegistry.getInstance().getTableUpdateService().registerTable( candidateTable );
        candidateTableScrollPane = FWTable.createFWTableScrollPane( candidateTable );
        candidateTableScrollPane.addMouseListener( mouseHandler );
        candidatePanelBuilder.add( candidateTableScrollPane, cc.xy( 1, 1 ) );
        
        FWToolBar candidateToolbar = new FWToolBar( JToolBar.HORIZONTAL );
        candidateToolbar.setBorderPainted( false );
        candidateToolbar.setFloatable( false );
        candidatePanelBuilder.add( candidateToolbar, cc.xy( 1, 3 ) );
        
        candidatePopup = new JPopupMenu();
        
        // add actions to toolbar and popup
        FWAction action = new RetryCandidateAction();
        addTabAction( action );
        candidateToolbar.addAction( action );
        candidatePopup.add( action );
        
        action = new RemoveCandidateAction();
        addTabAction( action );
        candidateToolbar.addAction( action );
        candidatePopup.add( action );
        candidateTable.getActionMap().put( action, action);
        candidateTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put( 
            (KeyStroke)action.getValue(FWAction.ACCELERATOR_KEY), action );
        
        candidatePopup.addSeparator();
        candidateToolbar.addSeparator();
        
        action = new AddToFavoritesAction();
        addTabAction( action );
        candidatePopup.add( action );
        
        action = new BrowseHostAction();
        addTabAction( action );
        candidateToolbar.addAction( action );
        candidatePopup.add( action );
        
        action = new ChatToHostAction();
        addTabAction( action );
        candidateToolbar.addAction( action );
        candidatePopup.add( action );
        
        action = new BanHostAction();
        addTabAction( action );
        candidateToolbar.addAction( action );
        candidatePopup.add( action );
        
        if ( ServiceManager.sCfg.downloadCandidateLogBufferSize > 0 )
        {
            action = new CandidateLogAction();
            addTabAction( action );
            candidateToolbar.addAction( action );
            candidatePopup.add( action );
        }
            
        candidateElegantPanel = new FWElegantPanel( Localizer.getString("DownloadCandidates"),
            downloadCandidatePanel );
        return candidateElegantPanel;
    }
    
    private JPanel initDownloadTablePanel( XJBGUISettings guiSettings, MouseHandler mouseHandler )
    {
        JPanel downloadTablePanel = new JPanel( );
        
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
            "fill:d:grow", // columns
            "fill:d:grow, 1dlu, p"); //rows
        PanelBuilder tabBuilder = new PanelBuilder( layout, downloadTablePanel );

        downloadModel = new SWDownloadTableModel();
        XJBGUITable xjbTable = GUIUtils.getXJBGUITableByIdentifier( guiSettings,
            DOWNLOAD_TABLE_IDENTIFIER );
        buildDownloadTableColumnModel( xjbTable );

        downloadTable = new FWTable( new FWSortedTableModel( downloadModel ),
            downloadColumnModel );
        downloadTable.activateAllHeaderActions();
        downloadTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        downloadTable.getSelectionModel().addListSelectionListener(
            new DownloadSelectionHandler() );
        downloadTable.addMouseListener( mouseHandler );
        GUIRegistry.getInstance().getTableUpdateService().registerTable( downloadTable );
        downloadTableScrollPane = FWTable.createFWTableScrollPane( downloadTable );
        downloadTableScrollPane.addMouseListener( mouseHandler );
        tabBuilder.add( downloadTableScrollPane, cc.xy( 1, 1 ) );
        
        FWToolBar fileToolbar = new FWToolBar( JToolBar.HORIZONTAL );
        fileToolbar.setBorderPainted( false );
        fileToolbar.setFloatable( false );
        tabBuilder.add( fileToolbar, cc.xy( 1, 3 ) );

        downloadPopup = new JPopupMenu();

        // add actions to toolbar
        FWAction action = new StartDownloadAction();
        addTabAction( action );
        fileToolbar.addAction( action );
        downloadPopup.add( action );

        action = new StopDownloadAction();
        addTabAction( action );
        fileToolbar.addAction( action );
        downloadPopup.add( action );

        action = new RemoveDownloadAction();
        addTabAction( action );
        fileToolbar.addAction( action );
        downloadPopup.add( action );
        downloadTable.getActionMap().put( action, action);
        downloadTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put( 
            (KeyStroke)action.getValue(FWAction.ACCELERATOR_KEY), action );

        fileToolbar.addSeparator();
        downloadPopup.addSeparator();

        action = new ConfigureDownloadAction();
        addTabAction( action );
        fileToolbar.addAction( action );
        downloadPopup.add( action );

        action = new SearchCandidatesAction();
        addTabAction( action );
        fileToolbar.addAction( action );
        downloadPopup.add( action );
        
        downloadPopupSpeedMenu = new JMenu( Localizer.getString("DownloadTab_DownloadSpeed") );
        downloadPopup.add( downloadPopupSpeedMenu );

        JMenu priorityMenu = new JMenu( Localizer.getString("Priority") );
        action = new MoveDownloadPriorityAction(
            MoveDownloadPriorityAction.PRIORITY_MOVE_TO_TOP );
        priorityMenu.add( action );
        addTabAction( action );
        action = new MoveDownloadPriorityAction( 
            MoveDownloadPriorityAction.PRIORITY_MOVE_UP );
        priorityMenu.add( action );
        addTabAction( action );
        action = new MoveDownloadPriorityAction(
            MoveDownloadPriorityAction.PRIORITY_RESORT );
        priorityMenu.add( action );
        addTabAction( action );
        action = new MoveDownloadPriorityAction( 
            MoveDownloadPriorityAction.PRIORITY_MOVE_DOWN );
        priorityMenu.add( action );
        addTabAction( action );
        action = new MoveDownloadPriorityAction(
            MoveDownloadPriorityAction.PRIORITY_MOVE_TO_BOTTOM );
        priorityMenu.add( action );
        addTabAction( action );
        downloadPopup.add( priorityMenu );
        
        FWMenu orderingMenu = new FWMenu( Localizer.getString("DownloadTab_Strategy") );

        action = new SelectStrategyAction( SelectStrategyAction.AVAILABILITY );
        orderingMenu.addAction( action );
        addTabAction( action );

        action = new SelectStrategyAction( SelectStrategyAction.PRIORITIZE_BEGINNING );
        orderingMenu.addAction( action );
        addTabAction( action );

        action = new SelectStrategyAction( SelectStrategyAction.PRIORITIZE_BEGINNING_END );
        orderingMenu.addAction( action );
        addTabAction( action );
        
        action = new SelectStrategyAction( SelectStrategyAction.RANDOM );
        orderingMenu.addAction( action );
        addTabAction( action );

        downloadPopup.add( orderingMenu );

        action = new GeneratePreviewAction();
        addTabAction( action );
        fileToolbar.addAction( action );
        downloadPopup.add( action );



        action = new ViewBitziTicketAction( );
        addTabAction( action );
        downloadPopup.add( action );
        
        FWElegantPanel elegantPanel = new FWElegantPanel( Localizer.getString("DownloadFiles"),
            downloadTablePanel );
        return elegantPanel;
    }

    /**
     * This is overloaded to update the table size for the progress bar on
     * every UI update. Like font size change!
     */
    public void updateUI()
    {
        super.updateUI();
        if ( downloadTable != null )
        {
            // increase table height a bit to display progress bar string better...
            GUIUtils.adjustTableProgresssBarHeight( downloadTable );
        }
        if ( downloadTableScrollPane != null )
        {
            FWTable.updateFWTableScrollPane( downloadTableScrollPane );
        }
        if ( candidateTableScrollPane != null )
        {
            FWTable.updateFWTableScrollPane( candidateTableScrollPane );
        }
    }

    /**
     * Updates dynamic information when changes occure. Called
     * when data of the selected download changes or selection of the download
     * table changes.
     */
    private void updateDownloadFileInfo( int changedPos )
    {
        // map view to model
        int selectedRow = downloadTable.getSelectedRow();
        int modelRow = downloadTable.convertRowIndexToModel(
            selectedRow );
        refreshTabActions();
        
        SWDownloadFile file = swarmingMgr.getDownloadFile(
            modelRow );
        downloadOverviewPanel.updateDownloadFileInfo( file );
        transfersPanel.updateDownloadFile( file );
        
        if ( modelRow == changedPos )
        {
            if ( file != null )
            {
                ResearchSetting researchSetting = file.getResearchSetting();
                if ( researchSetting.isSearchRunning() )
                {
                    Object[] args =
                    {
                        new Integer( researchSetting.getSearchHitCount() ),
                        new Integer( researchSetting.getSearchProgress() )
                    };
                    StringBuffer buffer = new StringBuffer( Localizer.getString(
                        "DownloadCandidates" ) );
                    buffer.append( ' ' );
                    buffer.append( Localizer.getFormatedString(
                        "CandidatesSearchingExt", args  )  );
                    
                    candidateElegantPanel.setTitle( buffer.toString() );
                }
                else
                {
                    candidateElegantPanel.setTitle( Localizer.getString( "DownloadCandidates" ) );
                }
            }
        }
    }

    private SWDownloadFile[] getSelectedDownloadFiles()
    {
        if ( downloadTable.getSelectedRowCount() == 0 )
        {
            return EMPTY_DOWNLOADFILE_ARRAY;
        }
        int[] viewIndices = downloadTable.getSelectedRows();
        int[] modelIndices = downloadTable.convertRowIndicesToModel( viewIndices );
        SWDownloadFile[] files = swarmingMgr.getDownloadFilesAt( modelIndices );
        return files;
    }

    private SWDownloadFile getSelectedDownloadFile()
    {
        int viewIndex = downloadTable.getSelectedRow();
        if ( viewIndex < 0 )
        {
            return null;
        }
        int modelIndex = downloadTable.convertRowIndexToModel( viewIndex );
        SWDownloadFile file = swarmingMgr.getDownloadFile( modelIndex );
        return file;
    }

    private SWDownloadCandidate[] getSelectedDownloadCandidates()
    {
        if ( candidateTable.getSelectedRowCount() == 0 )
        {
            return EMPTY_DOWNLOADCANDIDATE_ARRAY;
        }
        int[] viewIndices = candidateTable.getSelectedRows();
        int[] modelIndices = candidateTable.convertRowIndicesToModel( viewIndices );
        SWDownloadCandidate[] candidates = new SWDownloadCandidate[ modelIndices.length ];
        SWDownloadFile downloadFile = candidateModel.getDownloadFile();
        for ( int i = 0; i < candidates.length; i++ )
        {
            candidates[i] = downloadFile.getCandidate( modelIndices[i] );
        }
        return candidates;
    }

    private SWDownloadCandidate getSelectedDownloadCandidate()
    {
        int viewIndex = candidateTable.getSelectedRow();
        int modelIndex = candidateTable.convertRowIndexToModel( viewIndex );
        if ( modelIndex < 0 )
        {
            return null;
        }

        SWDownloadFile downloadFile = candidateModel.getDownloadFile();
        SWDownloadCandidate candidate = downloadFile.getCandidate( modelIndex );
        return candidate;
    }

    //////////////////////////////////////////////////////////////////////////
    /// XML serializing and deserializing
    //////////////////////////////////////////////////////////////////////////

    private void buildDownloadTableColumnModel( XJBGUITable tableSettings )
    {
        int[] columnIds = SWDownloadTableModel.getColumnIdArray();
        XJBGUITableColumnList columnList = null;
        if ( tableSettings != null )
        {
            columnList = tableSettings.getTableColumnList();
        }

        downloadColumnModel = new FWTableColumnModel( downloadModel, columnIds,
            columnList );
    }

    private void buildCandidateTableColumnModel( XJBGUITable tableSettings )
    {
        int[] columnIds = SWCandidateTableModel.getColumnIdArray();
        XJBGUITableColumnList columnList = null;
        if ( tableSettings != null )
        {
            columnList = tableSettings.getTableColumnList();
        }

        candidateColumnModel = new FWTableColumnModel( candidateModel,
            columnIds, columnList );
    }

    public void appendXJBGUISettings( XJBGUISettings xjbSettings )
        throws JAXBException
    {
        super.appendXJBGUISettings( xjbSettings );
        XJBGUITableColumnList xjbList = downloadColumnModel.createXJBGUITableColumnList();
        ObjectFactory objFactory = new ObjectFactory();
        XJBGUITable xjbTable = objFactory.createXJBGUITable();
        xjbTable.setTableColumnList( xjbList );
        xjbTable.setTableIdentifier( DOWNLOAD_TABLE_IDENTIFIER );
        xjbSettings.getTableList().getTableList().add( xjbTable );

        xjbList = candidateColumnModel.createXJBGUITableColumnList();
        xjbTable = objFactory.createXJBGUITable();
        xjbTable.setTableColumnList( xjbList );
        xjbTable.setTableIdentifier( CANDIDATE_TABLE_IDENTIFIER );
        xjbSettings.getTableList().getTableList().add( xjbTable );
        
        transfersPanel.appendXJBGUISettings( xjbSettings );
    }


    //////////////////////////////////////////////////////////////////////////
    /// Table Listeners
    //////////////////////////////////////////////////////////////////////////

    /**
     * Selection listener for download file table.
     */
    class DownloadSelectionHandler implements ListSelectionListener
    {
        public void valueChanged( ListSelectionEvent e )
        {
            try
            {
	            updateDownloadFileInfo( -1 );
            }
            catch ( Exception exp )
            {
                NLogger.error( NLoggerNames.USER_INTERFACE, exp, exp );
            }
        }
    }

    /**
     * Selection listener for download candidate table.
     */
    class CandidateSelectionHandler implements ListSelectionListener
    {
        public void valueChanged( ListSelectionEvent e )
        {
            if ( !e.getValueIsAdjusting() )
            {
                refreshTabActions();
            }
        }
    }

    private class MouseHandler extends MouseAdapter implements MouseListener
    {
        public void mouseClicked(MouseEvent e)
        {
        }

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
            if ( source == downloadTable || source == downloadTableScrollPane )
            {
                refreshTabActions();
                prepareDownloadPopup();
                downloadPopup.show( source, x, y );
            }
            else if ( source == candidateTable || source == candidateTableScrollPane )
            {
                candidatePopup.show( source, x, y );
            }
        }
        
        public void prepareDownloadPopup()
        {
            downloadPopupSpeedMenu.removeAll();
            if ( downloadTable.getSelectedRowCount() > 0 )
            {
                downloadPopupSpeedMenu.setEnabled( true );
            }
            else
            {
                downloadPopupSpeedMenu.setEnabled( false );
                return;
            }
            
            FWAction action;
            int downloadBw = ServiceManager.sCfg.mDownloadMaxBandwidth;
            if ( downloadBw > ServiceManager.sCfg.maxTotalBandwidth )
            {
                downloadBw = ServiceManager.sCfg.maxTotalBandwidth;
            }
            if ( downloadBw >= Integer.MAX_VALUE )
            {
                downloadBw = (int)(500 * NumberFormatUtils.ONE_KB);
            }
            double downloadBwFraction = downloadBw / 10.0;
            action = new SetDownloadSpeedAction( Integer.MAX_VALUE );
            downloadPopupSpeedMenu.add( action );
            for ( int i = 10; i > 0; i-- )
            {
                action = new SetDownloadSpeedAction( (int)(downloadBwFraction * i) );
                downloadPopupSpeedMenu.add( action );
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////
    /// Phex Listeners
    //////////////////////////////////////////////////////////////////////////

    private class DownloadFilesChangeHandler
        implements DownloadFilesChangeListener
    {
        /**
         * Called if a download file changed.
         */
        public void downloadFileChanged( int position )
        {
            updateDownloadFileInfo( position );
        }

        /**
         * Called if a download file was added.
         */
        public void downloadFileAdded( int position )
        {// don't care
        }

        /**
         * Called if a download file was removed.
         */
        public void downloadFileRemoved( int position )
        {// don't care
        }
    }

    //////////////////////////////////////////////////////////////////////////
    /// Actions
    //////////////////////////////////////////////////////////////////////////

    /**
     * Starts a download.
     */
    class StartDownloadAction extends FWAction
    {
        StartDownloadAction()
        {
            super( Localizer.getString( "StartDownload" ),
                GUIRegistry.getInstance().getIconFactory().getIcon("Start"),
                Localizer.getString( "TTTStartDownload" ) );
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            SWDownloadFile[] files = getSelectedDownloadFiles();
            for ( int i = 0; i < files.length; i++ )
            {
                if ( files[i] != null && files[i].isDownloadStopped() )
                {
                    files[i].startDownload();
                }

            }
            refreshActionState();
        }

        public void refreshActionState()
        {
            SWDownloadFile[] files = getSelectedDownloadFiles();
            boolean state = false;
            for ( int i = 0; i < files.length; i++ )
            {
                if ( files[i] != null && files[i].isDownloadStopped() )
                {
                    state = true;
                    break;
                }
            }
            setEnabled( state );
        }
    }

    /**
     * Stops a download.
     */
    class StopDownloadAction extends FWAction
    {
        StopDownloadAction()
        {
            super( Localizer.getString( "StopDownload" ),
                GUIRegistry.getInstance().getIconFactory().getIcon("Stop"),
                Localizer.getString( "TTTStopDownload" ) );
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            final SWDownloadFile[] files = getSelectedDownloadFiles();
            Runnable runner = new Runnable()
            {
                public void run()
                {
                    try
                    {
                        for ( int i = 0; i < files.length; i++ )
                        {
                            if ( files[i] != null && !files[i].isFileCompletedOrMoved()
                             && !files[i].isDownloadStopped() )
                            {
                                files[i].stopDownload();
                            }
                        }
                        refreshActionState();
                    }
                    catch ( Throwable th )
                    {
                        NLogger.error(NLoggerNames.USER_INTERFACE, th, th);
                    }
                }
            };
            ThreadPool.getInstance().addJob(runner, "StopDownloadFiles" );
        }

        public void refreshActionState()
        {
            SWDownloadFile[] files = getSelectedDownloadFiles();
            boolean state = false;
            for ( int i = 0; i < files.length; i++ )
            {
                if ( files[i] != null && !files[i].isFileCompletedOrMoved()
                  && !files[i].isDownloadStopped() )
                {
                    state = true;
                    break;
                }
            }
            setEnabled( state );
        }
    }

    class ConfigureDownloadAction extends FWAction
    {
        ConfigureDownloadAction()
        {
            super( Localizer.getString( "ConfigureDownload" ),
                GUIRegistry.getInstance().getIconFactory().getIcon("ConfigDownload"),
                Localizer.getString( "TTTConfigureDownload" ) );
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            
            if ( downloadTable.getSelectedRowCount() != 1 )
            {
                setEnabled( false);
                return;
            }
            int viewIdx = downloadTable.getSelectedRow();
            int modelIdx = downloadTable.convertRowIndexToModel( viewIdx );
            SWDownloadFile dfile = swarmingMgr.getDownloadFile( modelIdx );

            if ( dfile != null )
            {
                if ( dfile.isDownloadInProgress() )
                {
                    JOptionPane.showMessageDialog( SWDownloadTab.this,
                        Localizer.getString( "NoConfigDownloadInProgress" ),
                        Localizer.getString( "DownloadInProgress" ),
                        JOptionPane.WARNING_MESSAGE );
                    return;
                }
                short oldStatus = dfile.getStatus();
                dfile.stopDownload();
                DownloadConfigDialog dialog = new DownloadConfigDialog( dfile );
                dialog.setVisible( true ); 
                dfile.setStatus( oldStatus );
            }
        }

        public void refreshActionState()
        {
            if ( downloadTable.getSelectedRowCount() == 1 )
            {
                setEnabled( true );
            }
            else
            {
                setEnabled( false );
            }
        }
    }

    /**
     * Removes a download from the list.
     */
    class RemoveDownloadAction extends FWAction
    {
        RemoveDownloadAction()
        {
            super( Localizer.getString( "RemoveDownload" ),
                GUIRegistry.getInstance().getIconFactory().getIcon("Remove"),
                Localizer.getString( "TTTRemoveDownload" ), null, 
                KeyStroke.getKeyStroke( KeyEvent.VK_DELETE, 0 ) );
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            SWDownloadFile[] files = getSelectedDownloadFiles();

            ArrayList warningFiles = new ArrayList( files.length );
            ArrayList removeFiles = new ArrayList( files.length );
            for( int i = 0; i < files.length; i++ )
            {
                if ( files[i] == null )
                {
                    continue;
                }
                if ( files[i].isFileCompletedMoved() )
                {
                    removeFiles.add( files[i] );
                }
                else if ( files[i].isFileCompleted() )
                {
                    // ignore the intermediate state of a file between beeing
                    // completed and beeing completed moved
                }
                // if download not started.
                else if ( files[i].getTransferredDataSize() == 0 )
                {
                    removeFiles.add( files[i] );
                }
                else
                {// if not completed... schedule for warning
                    warningFiles.add( files[i] );
                }
            }

            Integer warningSize = new Integer( warningFiles.size() );
            for ( int i = 0; i < warningSize.intValue(); i++ )
            {
                SWDownloadFile file = (SWDownloadFile) warningFiles.get( i );
                // force collection of transfer data to get up to date results...
                file.forceCollectionOfTransferData();
                Object[] warningParams = new Object[]
                {
                    file.getDestinationFile().getName(),
                    NumberFormatUtils.formatSignificantByteSize( file.getTransferredDataSize() ),
                    NumberFormatUtils.formatSignificantByteSize( file.getTransferDataSize() )
                };
                Object[] titleParams = new Object[]
                {
                    new Integer( i + 1 ),
                    warningSize
                };

                Object[] dialogOptions;
                if ( warningSize.intValue() - i > 1 )
                {
                    dialogOptions = new Object[]
                    {
                        Localizer.getString( "Yes" ),
                        Localizer.getString( "No" ),
                        Localizer.getString( "YesToAll" ),
                        Localizer.getString( "NoToAll" ),
                    };
                }
                else
                {
                    dialogOptions = new Object[]
                    {
                        Localizer.getString( "Yes" ),
                        Localizer.getString( "No" )
                    };
                }

                int choice = JOptionPane.showOptionDialog( mainFrame,
                    Localizer.getFormatedString( "RemoveDownloadWarning", warningParams),
                    Localizer.getFormatedString( "RemoveDownloadTitle", titleParams ),
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    dialogOptions, Localizer.getString( "Yes" ) );
                if ( choice == 0 )
                {
                    removeFiles.add( file );
                }
                else if ( choice == 2 )
                {
                    removeFiles.addAll( warningFiles.subList( i, warningFiles.size() ) );
                    break;
                }
                else if ( choice == 3 )
                {
                    break;
                }
            }

            // do the remove...
            if ( removeFiles.size() > 0 )
            {
                final SWDownloadFile[] filesToRemove = new SWDownloadFile[ removeFiles.size() ];
                removeFiles.toArray( filesToRemove );
                Runnable runner = new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            swarmingMgr.removeDownloadFiles( filesToRemove );
                        }
                        catch ( Throwable th )
                        {
                            NLogger.error(NLoggerNames.USER_INTERFACE, th, th);
                        }
                    }
                };
                ThreadPool.getInstance().addJob(runner, "RemoveDownloadFiles" );
                downloadTable.getSelectionModel().clearSelection();
            }
        }

        public void refreshActionState()
        {
            int row = downloadTable.getSelectedRow();
            if ( row < 0 )
            {
                setEnabled( false );
            }
            else
            {
                setEnabled( true );
            }
        }
    }
    
    /**
     * 
     */
    class SetDownloadSpeedAction extends FWAction
    {
        private int speedInBytes;

        SetDownloadSpeedAction( int speedInBytes )
        {
            super( );
            this.speedInBytes = speedInBytes;
            String name;
            if ( speedInBytes >= Integer.MAX_VALUE )
            {
                name = Localizer.getString( "DownloadTab_NoLimit" ) + " (" +
                    Localizer.getDecimalFormatSymbols().getInfinity() + ")";
            }
            else
            {
                name = NumberFormatUtils.formatSignificantByteSize( speedInBytes ) 
                    + Localizer.getString( "PerSec" );
            }
            setName( name );
        }

        public void actionPerformed( ActionEvent e )
        {
            SWDownloadFile[] files = getSelectedDownloadFiles();
            for ( int i = 0; i < files.length; i++ )
            {
                if ( files[i] != null )
                {
                    files[i].setDownloadThrottlingRate( speedInBytes );
                }
            }
        }

        public void refreshActionState()
        {
        }
    }

    /**
     * Moves a download in the list.
     */
    class MoveDownloadPriorityAction extends FWAction
    {
        private static final short PRIORITY_MOVE_TO_TOP =
            SwarmingManager.PRIORITY_MOVE_TO_TOP;
        private static final short PRIORITY_MOVE_UP =
            SwarmingManager.PRIORITY_MOVE_UP;
        private static final short PRIORITY_MOVE_DOWN =
            SwarmingManager.PRIORITY_MOVE_DOWN;
        private static final short PRIORITY_MOVE_TO_BOTTOM =
            SwarmingManager.PRIORITY_MOVE_TO_BOTTOM;
        private static final short PRIORITY_RESORT = 100;
        private short moveDirection;

        MoveDownloadPriorityAction( short type )
        {
            super( );
            moveDirection = type;
            switch( type )
            {
                case PRIORITY_MOVE_TO_TOP:
                    setName( Localizer.getString( "MoveToTop" ) );
                    setSmallIcon( GUIRegistry.getInstance().
                        getIconFactory().getIcon( "MoveToTop") );
                    setToolTipText( Localizer.getString( "TTTMoveToTop" ) );
                    break;
                case PRIORITY_MOVE_UP:
                    setName( Localizer.getString( "MoveUp" ) );
                    setSmallIcon( GUIRegistry.getInstance().
                        getIconFactory().getIcon( "MoveUp") );
                    setToolTipText( Localizer.getString( "TTTMoveUp" ) );
                    break;
                case PRIORITY_MOVE_DOWN:
                    setName( Localizer.getString( "MoveDown" ) );
                    setSmallIcon( GUIRegistry.getInstance().
                        getIconFactory().getIcon( "MoveDown") );
                    setToolTipText( Localizer.getString( "TTTMoveDown" ) );
                    break;
                case PRIORITY_MOVE_TO_BOTTOM:
                    setName( Localizer.getString( "MoveToBottom" ) );
                    setSmallIcon( GUIRegistry.getInstance().
                        getIconFactory().getIcon( "MoveToBottom") );
                    setToolTipText( Localizer.getString( "TTTMoveToBottom" ) );
                    break;
                case PRIORITY_RESORT:
                    setName( Localizer.getString( "Resort" ) );
                    setSmallIcon( GUIRegistry.getInstance().
                        getIconFactory().getIcon( "Resort") );
                    setToolTipText( Localizer.getString( "TTTResort" ) );
                    break;
            }
			refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            try
            {
                if ( moveDirection == PRIORITY_RESORT )
                {
                    performPriorityResort();
                }
                else
                {
                    performPriorityUpdate();
                }
            }
            catch (Throwable th)
            {
                NLogger.error( NLoggerNames.USER_INTERFACE, th, th );
            }
        }
        
        /**
         * 
         */
        private void performPriorityResort()
        {
            int[] viewIndices = new int[downloadTable.getRowCount()];
            for ( int i = 0; i < viewIndices.length; i++ )
            {
                viewIndices[i] = i;
            }
            final int[] modelIndices = downloadTable.convertRowIndicesToModel( 
                viewIndices );
            Runnable runner = new Runnable()
            {
                public void run()
                {
                    try
                    {
                        SWDownloadFile[] files = swarmingMgr.getDownloadFilesAt(
                            modelIndices );
                        swarmingMgr.updateDownloadFilePriorities( files );
                    }
                    catch (Throwable th)
                    {
                        NLogger.error( NLoggerNames.USER_INTERFACE, th, th );
                    }
                }
            };
            ThreadPool.getInstance().addJob( runner, "ResortPriority" );
        }

        /**
         * 
         */
        private void performPriorityUpdate()
        {
            final SWDownloadFile[] files = getSelectedDownloadFiles();
            Runnable runner = new Runnable()
            {
                public void run()
                {
                    try
                    {
                        for ( int i = 0; i < files.length; i++ )
                        {
                            if ( files[i] != null )
                            {
                                swarmingMgr.moveDownloadFilePriority( files[i],
                                    moveDirection );
                            }
                        }
                    }
                    catch (Throwable th)
                    {
                        NLogger.error( NLoggerNames.USER_INTERFACE, th, th );
                    }
                }
            };
            ThreadPool.getInstance().addJob( runner, "UpdatePriority" );
        }

        public void refreshActionState()
        {
            if ( moveDirection == PRIORITY_RESORT )
            {
                setEnabled( true );
            }
            else
            {
                if ( downloadTable.getSelectedRowCount() > 0 )
                {
                    setEnabled( true );
                }
                else
                {
                    setEnabled( false );
                }
            }
        }
    }
    
    class SelectStrategyAction extends FWToggleAction
    {
        private static final short AVAILABILITY = 0;
        private static final short PRIORITIZE_BEGINNING = 1;
        private static final short PRIORITIZE_BEGINNING_END = 2;
        private static final short RANDOM = 3;
        private short strategy;

        SelectStrategyAction( short strategy )
        {
            super( );
            this.strategy = strategy;
            switch( strategy )
            {
                case AVAILABILITY:
                    setName( Localizer.getString( "DownloadTab_StrategyAvailability" ) );
                    break;
                case PRIORITIZE_BEGINNING:
                    setName( Localizer.getString( "DownloadTab_StrategyBeginning" ) );
                    break;
                case PRIORITIZE_BEGINNING_END:
                    setName( Localizer.getString( "DownloadTab_StrategyBeginningEnd" ) );
                    break;
                case RANDOM:
                    setName( Localizer.getString( "DownloadTab_StrategyRandom" ) );
                    break;
            }
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            ScopeSelectionStrategy selectionStrategy;
            switch( strategy )
            {
                case AVAILABILITY:
                    selectionStrategy = ScopeSelectionStrategyProvider.getAvailBeginRandSelectionStrategy();
                    break;
                case PRIORITIZE_BEGINNING:
                    selectionStrategy = ScopeSelectionStrategyProvider.getBeginAvailRandSelectionStrategy();
                    break;
                case PRIORITIZE_BEGINNING_END:
                    selectionStrategy = ScopeSelectionStrategyProvider.getBeginEndAvailRandSelectionStrategy();
                    break;
                default:
                    selectionStrategy = ScopeSelectionStrategyProvider.getRandomSelectionStrategy();
                    break;
            }
            SWDownloadFile[] files = getSelectedDownloadFiles();
            for ( int i = 0; i < files.length; i++ )
            {
                if ( files[i] == null )
                {
                    continue;
                }
                files[i].setScopeSelectionStrategy( selectionStrategy );
            }
        }

        public void refreshActionState()
        {
            if ( downloadTable.getSelectedRowCount() == 1 )
            {
                setEnabled( true );
                SWDownloadFile file = getSelectedDownloadFile();
                ScopeSelectionStrategy curStrat = file.getScopeSelectionStrategy();
                switch( strategy )
                {
                    case AVAILABILITY:
                        setSelected( curStrat instanceof AvailBeginRandSelectionStrategy );
                        break;
                    case PRIORITIZE_BEGINNING:
                        setSelected( curStrat instanceof BeginAvailRandSelectionStrategy );
                        break;
                    case PRIORITIZE_BEGINNING_END:
                        setSelected( curStrat instanceof BeginEndAvailRandSelectionStrategy );
                        break;
                    default:
                        setSelected( curStrat instanceof RandomScopeSelectionStrategy );
                        break;
                }
            }
            else
            {
                setEnabled( false );
            }
        }
    }

    class GeneratePreviewAction extends FWAction
    {
        GeneratePreviewAction ( )
        {
            super( Localizer.getString( "DownloadTab_PreviewDownload" ),
                GUIRegistry.getInstance().getIconFactory().getIcon("Open"),
                Localizer.getString( "DownloadTab_TTTPreviewDownload" ) );
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            try
            {
                final SWDownloadFile file = getSelectedDownloadFile();
                if ( file == null ) return;
                Runnable runner = new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            File previewFile = file.getPreviewFile();
                            SystemShellExecute.launchFile( previewFile );
                        }
                        catch ( Throwable th )
                        {
                            NLogger.error(NLoggerNames.USER_INTERFACE, th, th);
                        }
                    }
                };
                ThreadPool.getInstance().addJob(runner, "GenerateDownloadPreview" );
            }
            catch ( Throwable th )
            {
                NLogger.error( NLoggerNames.Download, th, th);
            }
        }

        public void refreshActionState()
        {
            try
            {
                SWDownloadFile file = getSelectedDownloadFile();
                if ( file == null )
                {
                    // no file, no do
                    setEnabled ( false );
                }
                else
                {
                    setEnabled( file.isPreviewPossible() );
                }
            }
            catch ( Throwable th )
            {
                NLogger.error( NLoggerNames.Download, th, th);
            }
        }
    }    

    private class ViewBitziTicketAction extends FWAction
    {
        public ViewBitziTicketAction()
        {
            super( Localizer.getString( "ViewBitziTicket" ),
                GUIRegistry.getInstance().getIconFactory().getIcon("Bitzi"),
                Localizer.getString( "TTTViewBitziTicket" ) );
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            SWDownloadFile file = getSelectedDownloadFile();
            if ( file == null )
            {
                return;
            }
            URN urn = file.getFileURN();
            if ( urn == null )
            {
                return;
            }
            String url = URLUtil.buildBitziLookupURL( urn );
            try
            {
                BrowserLauncher.openURL( url );
            }
            catch ( IOException exp )
            {
                NLogger.warn(NLoggerNames.USER_INTERFACE, exp, exp);
                Object[] dialogOptions = new Object[]
                {
                    Localizer.getString( "Yes" ),
                    Localizer.getString( "No" )
                };

                int choice = JOptionPane.showOptionDialog( SWDownloadTab.this,
                    Localizer.getString( "FailedToLaunchBrowserURLInClipboard" ),
                    Localizer.getString( "FailedToLaunchBrowser" ),
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    dialogOptions, Localizer.getString( "Yes" ) );
                if ( choice == 0 )
                {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                        new StringSelection( url ), null);
                }
            }
        }

        public void refreshActionState()
        {
            if ( downloadTable.getSelectedRowCount() == 1 )
            {
                setEnabled( true );
            }
            else
            {
                setEnabled( false );
            }
        }
    }

    /**
     * Removes a candidate from the list.
     */
    class RemoveCandidateAction extends FWAction
    {
        RemoveCandidateAction()
        {
            super( Localizer.getString( "RemoveCandidate" ),
                GUIRegistry.getInstance().getIconFactory().getIcon("Remove"),
                Localizer.getString( "TTTRemoveCandidate" ), null, 
                KeyStroke.getKeyStroke( KeyEvent.VK_DELETE, 0 ) );
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            final SWDownloadCandidate[] candidates = getSelectedDownloadCandidates();
            final SWDownloadFile file = candidateModel.getDownloadFile();
            Runnable runner = new Runnable()
            {
                public void run()
                {
                    try
                    {
                        for ( int i = 0; i < candidates.length; i++ )
                        {
                            file.stopDownload( candidates[i] );
                            file.markCandidateIgnored( candidates[i], "CandidateStatusReason_ByUser" );
                        }
                        refreshActionState();
                    }
                    catch ( Throwable th )
                    {
                        NLogger.error(NLoggerNames.USER_INTERFACE, th, th);
                    }
                }
            };
            ThreadPool.getInstance().addJob(runner, "RemoveDownloadCandidate" );

        }

        public void refreshActionState()
        {
            int downloadRow = downloadTable.getSelectedRow();
            int candidateRow = candidateTable.getSelectedRow();
            if ( downloadRow < 0 || candidateRow < 0 )
            {
                setEnabled( false );
            }
            else
            {
                setEnabled( true );
            }
        }
    }

    /**
     * Schedules the candidate to retry a connection for download.
     */
    class RetryCandidateAction extends FWAction
    {
        RetryCandidateAction()
        {
            super( Localizer.getString( "RetryCandidate" ),
                GUIRegistry.getInstance().getIconFactory().getIcon("ReconnectHost"),
                Localizer.getString( "TTTRetryCandidate" ) );
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            SWDownloadCandidate[] candidates = getSelectedDownloadCandidates();
            for ( int i = 0; i < candidates.length; i++ )
            {
                candidates[i].manualConnectionRetry();
            }
        }

        public void refreshActionState()
        {
            int downloadRow = downloadTable.getSelectedRow();
            int candidateRow = candidateTable.getSelectedRow();
            if ( downloadRow < 0 || candidateRow < 0 )
            {
                setEnabled( false );
            }
            else
            {
                setEnabled( true );
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
            SWDownloadCandidate candidate = getSelectedDownloadCandidate();
            if ( candidate == null )
            {
                return;
            }
            if ( !candidate.isChatSupported() )
            {
                return;
            }

            ChatManager.getInstance().openChat( candidate.getHostAddress() );
        }

        public void refreshActionState()
        {
            if ( candidateTable.getSelectedRowCount() == 1 )
            {
                SWDownloadCandidate candidate = getSelectedDownloadCandidate();
                if ( candidate != null && candidate.isChatSupported() )
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
            SWDownloadCandidate candidate = getSelectedDownloadCandidate();
            if ( candidate == null )
            {
                return;
            }
            GUIActionPerformer.browseHost( candidate.getHostAddress() );
        }

        public void refreshActionState()
        {
            if ( candidateTable.getSelectedRowCount() == 1 )
            {
                SWDownloadCandidate candidate = getSelectedDownloadCandidate();
                if ( candidate != null )
                {
                    setEnabled( true );
                    return;
                }
            }
            setEnabled( false );
        }
    }

    private class BanHostAction extends FWAction
    {
        public BanHostAction()
        {
            super( Localizer.getString( "BanHost" ),
                GUIRegistry.getInstance().getIconFactory().getIcon("Ban"),
                Localizer.getString( "TTTBanHost" ) );
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            try
            {
                SWDownloadCandidate[] candidates = getSelectedDownloadCandidates();
                SWDownloadFile file = candidateModel.getDownloadFile();
                
                final DestAddress[] addresses = new DestAddress[candidates.length];
                for (int i = 0; i < candidates.length; i++)
                {
                    file.stopDownload( candidates[i] );
                    file.markCandidateIgnored( candidates[i], "CandidateStatusReason_ByUser" );
                    addresses[ i ] = candidates[i].getHostAddress();
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
                refreshActionState();
            }
            catch ( Throwable th )
            {
                NLogger.error(NLoggerNames.USER_INTERFACE, th, th);
            }
        }

        public void refreshActionState()
        {
            int downloadRow = downloadTable.getSelectedRow();
            int candidateRow = candidateTable.getSelectedRow();
            if ( downloadRow < 0 || candidateRow < 0 )
            {
                setEnabled( false );
            }
            else
            {
                setEnabled( true );
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
            SWDownloadCandidate[] candidates = getSelectedDownloadCandidates();
            
            DestAddress[] addresses = new DestAddress[candidates.length];
            for (int i = 0; i < candidates.length; i++)
            {
                addresses[ i ] = candidates[i].getHostAddress();
            }
            GUIActionPerformer.addHostsToFavorites( addresses );            
        }

        public void refreshActionState()
        {
            int downloadRow = downloadTable.getSelectedRow();
            int candidateRow = candidateTable.getSelectedRow();
            if ( downloadRow < 0 || candidateRow < 0 )
            {
                setEnabled( false );
            }
            else
            {
                setEnabled( true );
            }
        }
    }


    /**
     * Manually search for new candidates.
     */
    private class SearchCandidatesAction extends FWAction
    {
        public SearchCandidatesAction()
        {
            super( Localizer.getString( "Search" ),
                GUIRegistry.getInstance().getIconFactory().getIcon("Search"),
                Localizer.getString( "TTTSearchCandidates" ) );
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            try
            {
                int viewIdx = downloadTable.getSelectedRow();
                int modelIdx = downloadTable.convertRowIndexToModel( viewIdx );
                SWDownloadFile file = swarmingMgr.getDownloadFile( modelIdx );
                file.startSearchForCandidates();
            }
            catch ( Throwable th )
            {
                NLogger.error(NLoggerNames.Download, th, th );
            }
        }

        public void refreshActionState()
        {
            if ( downloadTable.getSelectedRowCount() != 1 )
            {
                setEnabled( false);
                return;
            }
            int viewIdx = downloadTable.getSelectedRow();
            int modelIdx = downloadTable.convertRowIndexToModel( viewIdx );
            SWDownloadFile file = swarmingMgr.getDownloadFile( modelIdx );

            if ( file == null )
            {
                setEnabled( false );
            }
            else
            {
                if ( file.getResearchSetting().isSearchRunning() ||
                     file.isFileCompletedOrMoved() )
                {
                    setEnabled( false );
                }
                else
                {
                    setEnabled( true );
                }
            }
        }
    }
    
    private class CandidateLogAction extends FWAction
    {
        public CandidateLogAction()
        {
            super( Localizer.getString( "DownloadTab_ViewLog" ),
                null,
                Localizer.getString( "DownloadTab_TTTViewLog" ) );
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            try
            {
                SWDownloadCandidate candidate = getSelectedDownloadCandidate();
                LogBuffer buffer = SwarmingManager.getInstance().getCandidateLogBuffer();
                
                Collection logRecords = buffer.getLogRecords( candidate );
                if ( logRecords != null )
                {
                    LogBufferDialog dialog = new LogBufferDialog( logRecords );
                    dialog.setVisible( true );
                }
            }
            catch ( Throwable th )
            {
                NLogger.error(NLoggerNames.Download, th, th );
            }
        }

        public void refreshActionState()
        {
            if ( candidateTable.getSelectedRowCount() == 1 )
            {
                SWDownloadCandidate candidate = getSelectedDownloadCandidate();
                if ( candidate != null )
                {
                    setEnabled( true );
                    return;
                }
            }
            setEnabled( false );
        }
    }
}