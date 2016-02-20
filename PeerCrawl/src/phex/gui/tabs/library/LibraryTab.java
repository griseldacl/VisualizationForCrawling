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
package phex.gui.tabs.library;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang.SystemUtils;

import phex.common.ServiceManager;
import phex.common.ThreadPool;
import phex.common.URN;
import phex.common.format.NumberFormatUtils;
import phex.event.ShareChangeListener;
import phex.gui.actions.FWAction;
import phex.gui.actions.GUIActionPerformer;
import phex.gui.common.*;
import phex.gui.common.table.FWSortedTableModel;
import phex.gui.common.table.FWTable;
import phex.gui.common.table.FWTableColumnModel;
import phex.gui.dialogs.ExportDialog;
import phex.gui.dialogs.FilterLibraryDialog;
import phex.gui.tabs.FWTab;
import phex.share.*;
import phex.utils.*;
import phex.xml.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class LibraryTab extends FWTab
{
    private static final String SHARED_FILES_TABLE_IDENTIFIER = "SharedFilesTable";

    private JLabel sharedFilesLabel;

    private FWTable sharedFilesTable;

    private JTree mainTree;

    //private FileSystemTreeModel fileSystemTreeModel;
    private SharingTreeModel sharingTreeModel;

    private FWPopupMenu fileTreePopup;
    private FWPopupMenu fileTablePopup;

    private JScrollPane sharedFilesTableScrollPane;

    private SharedFilesTableModel sharedFilesModel;

    private FWTableColumnModel sharedFilesColumnModel;

    public LibraryTab()
    {
        super(MainFrame.LIBRARY_TAB_ID, Localizer.getString("Library"),
            GUIRegistry.getInstance().getIconFactory().getIcon("Library"),
            Localizer.getString("TTTLibrary"), Localizer
                .getChar("LibraryMnemonic"), KeyStroke.getKeyStroke(Localizer
                .getString("LibraryAccelerator")), MainFrame.LIBRARY_TAB_INDEX);
    }

    public void initComponent(XJBGUISettings guiSettings)
    {
        CellConstraints cc = new CellConstraints();
        FormLayout tabLayout = new FormLayout("2dlu, fill:d:grow, 2dlu", // columns
            "2dlu, fill:p:grow, 2dlu"); //rows
        PanelBuilder tabBuilder = new PanelBuilder(tabLayout, this);
        JPanel contentPanel = new JPanel();
        FWElegantPanel elegantPanel = new FWElegantPanel( Localizer.getString("Library"),
            contentPanel );
        tabBuilder.add(elegantPanel, cc.xy(2, 2));
        
        FormLayout contentLayout = new FormLayout("fill:d:grow", // columns
            "fill:d:grow"); //rows
        PanelBuilder contentBuilder = new PanelBuilder(contentLayout, contentPanel);
        
        
        MouseHandler mouseHandler = new MouseHandler();
        
        JPanel treePanel = createTreePanel( mouseHandler );
        JPanel tablePanel = createTablePanel( guiSettings, mouseHandler );

        JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT,
            treePanel, tablePanel );
        splitPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        splitPane.setDividerSize(4);
        splitPane.setOneTouchExpandable(false);
        contentBuilder.add(splitPane, cc.xy(1, 1));
       

        sharedFilesLabel = new JLabel( " " );
        sharedFilesLabel.setHorizontalAlignment( JLabel.RIGHT );
        elegantPanel.addHeaderPanelComponent(sharedFilesLabel, BorderLayout.EAST );
        ShareManager.getInstance().getSharedFilesService().addSharedFilesChangeListener(
            new SharedFilesChangeHandler() );
        
        fileTreePopup = new FWPopupMenu();
        fileTablePopup = new FWPopupMenu();
        
        FWAction action;
        
        action = getTabAction( ADD_SHARE_FOLDER_ACTION_KEY );
        fileTreePopup.addAction( action );
        action = getTabAction( REMOVE_SHARE_FOLDER_ACTION_KEY );
        fileTreePopup.addAction( action );
        
        if ( SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_MAC_OSX )
        {
            action = getTabAction( EXPLORE_FOLDER_ACTION_KEY );
            fileTreePopup.addAction( action );
        }
        
        action = getTabAction( OPEN_FILE_ACTION_KEY );
        fileTablePopup.addAction( action );
        
        action = getTabAction( VIEW_BITZI_ACTION_KEY );
        fileTablePopup.addAction( action );
        
        fileTablePopup.addSeparator();
        fileTreePopup.addSeparator();
        
        action = getTabAction( RESCAN_ACTION_KEY );
        fileTablePopup.addAction( action );
        fileTreePopup.addAction( action );
        
        action = getTabAction( EXPORT_ACTION_KEY );
        fileTablePopup.addAction( action );
        fileTreePopup.addAction( action );
        
        action = getTabAction( FILTER_ACTION_KEY );
        fileTablePopup.addAction( action );
        fileTreePopup.addAction( action );
    }
    
    private JPanel createTreePanel(MouseHandler mouseHandler )
    {
        JPanel panel = new JPanel();
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout("fill:d:grow", // columns
            "fill:d:grow, 1dlu, p"); //rows
        PanelBuilder tabBuilder = new PanelBuilder(layout, panel);
        
        sharingTreeModel = new SharingTreeModel();
        mainTree = new JTree( sharingTreeModel );
        mainTree.setMinimumSize( new Dimension( 0, 0 ) );
        mainTree.setRowHeight(0);
        mainTree.setCellRenderer(new SharingTreeRenderer());
        mainTree.addMouseListener(mouseHandler);
        
        mainTree.getSelectionModel().addTreeSelectionListener(
            new SelectionHandler());
        ToolTipManager.sharedInstance().registerComponent( mainTree );
        
        // open up first level of nodes
        TreeNode root = (TreeNode) sharingTreeModel.getRoot();
        int count = root.getChildCount();
        for ( int i = 0; i < count; i++ )
        {
            mainTree.expandPath( new TreePath( new Object[] {root, root.getChildAt(i)} ) );
        }

        JScrollPane treeScrollPane = new JScrollPane(mainTree);
        tabBuilder.add(treeScrollPane, cc.xywh(1, 1, 1, 1));
        
        FWToolBar shareToolbar = new FWToolBar(FWToolBar.HORIZONTAL);
        shareToolbar.setBorderPainted(false);
        shareToolbar.setFloatable(false);
        tabBuilder.add(shareToolbar, cc.xy(1, 3));
        
        FWAction action = new AddShareFolderAction();
        addTabAction( ADD_SHARE_FOLDER_ACTION_KEY, action );
        shareToolbar.addAction( action );
        
        action = new RemoveShareFolderAction();
        addTabAction( REMOVE_SHARE_FOLDER_ACTION_KEY, action );
        shareToolbar.addAction( action );
        
        if ( SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_MAC_OSX )
        {
            action = new ExploreFolderAction();
            addTabAction( EXPLORE_FOLDER_ACTION_KEY, action );
        }
        
        return panel;
    }
    
    private JPanel createTablePanel(XJBGUISettings guiSettings, 
        MouseHandler mouseHandler )
    {
        JPanel panel = new JPanel();
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout("fill:d:grow", // columns
            "fill:d:grow, 1dlu, p"); //rows
        PanelBuilder tabBuilder = new PanelBuilder(layout, panel);
        
        sharedFilesModel = new SharedFilesTableModel();
        XJBGUITable xjbTable = GUIUtils.getXJBGUITableByIdentifier(guiSettings,
            SHARED_FILES_TABLE_IDENTIFIER);
        buildSharedFilesTableColumnModel(xjbTable);
        sharedFilesTable = new FWTable(new FWSortedTableModel(
            sharedFilesModel), sharedFilesColumnModel);
        sharedFilesTable.activateAllHeaderActions();
        sharedFilesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        sharedFilesTable.addMouseListener(mouseHandler);
        sharedFilesTable.getSelectionModel().addListSelectionListener(
            new SelectionHandler());
        sharedFilesTableScrollPane = FWTable
            .createFWTableScrollPane(sharedFilesTable);
        
        tabBuilder.add(sharedFilesTableScrollPane, cc.xy(1, 1));
        
        FWToolBar shareToolbar = new FWToolBar(FWToolBar.HORIZONTAL);
        shareToolbar.setBorderPainted(false);
        shareToolbar.setFloatable(false);
        tabBuilder.add(shareToolbar, cc.xy(1, 3));
        
        FWAction action;
        
        action = new OpenFileAction();
        addTabAction( OPEN_FILE_ACTION_KEY, action );
        shareToolbar.addAction( action );
        
        action = new ViewBitziTicketAction();
        addTabAction( VIEW_BITZI_ACTION_KEY, action );
        shareToolbar.addAction( action );
        
        shareToolbar.addSeparator();
        
        action = new RescanAction();
        addTabAction( RESCAN_ACTION_KEY, action );
        shareToolbar.addAction( action );
        
        action = new ExportAction();
        addTabAction( EXPORT_ACTION_KEY, action );
        shareToolbar.addAction( action );
        
        action = new FilterAction();
        addTabAction( FILTER_ACTION_KEY, action );
        shareToolbar.addAction( action );
        
        return panel;
    }

    /**
     * This is overloaded to update the table size for the progress bar on
     * every UI update. Like font size change!
     */
    public void updateUI()
    {
        super.updateUI();
        if (sharedFilesTableScrollPane != null)
        {
            FWTable.updateFWTableScrollPane(sharedFilesTableScrollPane);
        }
    }

    private LibraryNode getSelectedTreeComponent()
    {
        TreePath path = mainTree.getSelectionPath();
        if (path == null)
        {
            return null;
        }
        LibraryNode node = (LibraryNode) path.getLastPathComponent();
        return node;
    }

    //////////////////////////////////////////////////////////////////////////
    /// XML serializing and deserializing
    //////////////////////////////////////////////////////////////////////////

    private void buildSharedFilesTableColumnModel(XJBGUITable tableSettings)
    {
        int[] columnIds = SharedFilesTableModel.getColumnIdArray();
        XJBGUITableColumnList columnList = null;
        if (tableSettings != null)
        {
            columnList = tableSettings.getTableColumnList();
        }

        sharedFilesColumnModel = new FWTableColumnModel(sharedFilesModel,
            columnIds, columnList);
    }

    public void appendXJBGUISettings(XJBGUISettings xjbSettings)
        throws JAXBException
    {
        super.appendXJBGUISettings(xjbSettings);

        XJBGUITableColumnList xjbList = sharedFilesColumnModel
            .createXJBGUITableColumnList();
        ObjectFactory objFactory = new ObjectFactory();
        XJBGUITable xjbTable = objFactory.createXJBGUITable();
        xjbTable.setTableColumnList(xjbList);
        xjbTable.setTableIdentifier(SHARED_FILES_TABLE_IDENTIFIER);
        xjbSettings.getTableList().getTableList().add(xjbTable);
    }

    //////////////////////////////////////////////////////////////////////////
    /// Actions
    //////////////////////////////////////////////////////////////////////////
    
    private static final String ADD_SHARE_FOLDER_ACTION_KEY = "AddShareFolderAction";
    private static final String REMOVE_SHARE_FOLDER_ACTION_KEY = "RemoveShareFolderAction";
    private static final String RESCAN_ACTION_KEY = "RescanAction";
    private static final String VIEW_BITZI_ACTION_KEY = "ViewBitziTicketAction";
    private static final String EXPORT_ACTION_KEY = "ExportAction";
    private static final String FILTER_ACTION_KEY = "FilterAction";
    private static final String OPEN_FILE_ACTION_KEY = "OpenFileAction";
    private static final String EXPLORE_FOLDER_ACTION_KEY = "ExploreFolderAction";
    
    
    class RescanAction extends FWAction
    {
        RescanAction()
        {
            super(Localizer.getString("LibraryTab_Rescan"), 
                GUIRegistry.getInstance().getIconFactory().getIcon(
                "Refresh"), Localizer.getString("LibraryTab_TTTRescan"));
        }
        
        public void actionPerformed(ActionEvent e)
        {
            sharingTreeModel.updateFileSystem();
            GUIActionPerformer.rescanSharedFiles();
        }

        /**
         * @see phex.gui.actions.FWAction#refreshActionState()
         */
        public void refreshActionState()
        {
        }
    }
    
    private class ExportAction extends FWAction
    {
        ExportAction()
        {
            super(Localizer.getString("LibraryTab_Export"), 
                GUIRegistry.getInstance().getIconFactory().getIcon(
                "Export"), Localizer.getString("LibraryTab_TTTExport"));
        }
        
        public void actionPerformed(ActionEvent e)
        {
            ExportDialog dialog = new ExportDialog();
            dialog.setVisible(true);
        }

        /**
         * @see phex.gui.actions.FWAction#refreshActionState()
         */
        public void refreshActionState()
        {
        }
    }
    
    private class OpenFileAction extends FWAction
    {
        OpenFileAction()
        {
            super(Localizer.getString("LibraryTab_OpenFile"), 
                GUIRegistry.getInstance().getIconFactory().getIcon(
                "Open"), Localizer.getString("LibraryTab_TTTOpenFile"));
            refreshActionState();
        }
        
        public void actionPerformed(ActionEvent e)
        {
            try
            {
                int row = sharedFilesTable.getSelectedRow();
                row = sharedFilesTable.convertRowIndexToModel(row);
                if ( row < 0 )
                {
                    return;
                }
                Object obj = sharedFilesModel.getValueAt(row, SharedFilesTableModel.FILE_MODEL_INDEX);            
                if ( obj == null )
                {
                    return;
                }
                final File file;
                if ( obj instanceof ShareFile )
                {
                    ShareFile sFile = (ShareFile)obj;
                    file = sFile.getSystemFile();
                    
                }
                else if ( obj instanceof File )
                {
                    file = (File)obj;
                }
                else
                {
                    return;
                }
                
                Runnable runnable = new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            SystemShellExecute.launchFile( file );
                        }
                        catch ( IOException exp )
                        {// ignore and do nothing..
                        }
                        catch ( Throwable th )
                        {
                            NLogger.error(NLoggerNames.USER_INTERFACE, th, th);
                        }
                    }
                };
                ThreadPool.getInstance().addJob(runnable, "SystenShellExecute");
            }
            catch ( Throwable th )
            {
                NLogger.error(NLoggerNames.USER_INTERFACE, th, th);
            }
        }

        /**
         * @see phex.gui.actions.FWAction#refreshActionState()
         */
        public void refreshActionState()
        {
            int row = sharedFilesTable.getSelectedRow();
            row = sharedFilesTable.convertRowIndexToModel(row);
            if ( row < 0 )
            {
                setEnabled(false);
                return;
            }
            Object obj = sharedFilesModel.getValueAt(row, SharedFilesTableModel.FILE_MODEL_INDEX);            
            if ( obj == null )
            {
                setEnabled(false);
                return;
            }
            if ( obj instanceof ShareFile ||  obj instanceof File  )
            {
                setEnabled(true);
            }
            else
            {
                setEnabled(false);
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
            int row = sharedFilesTable.getSelectedRow();
            row = sharedFilesTable.convertRowIndexToModel(row);
            if ( row < 0 )
            {
                return;
            }
            
            Object obj = sharedFilesModel.getValueAt(row, SharedFilesTableModel.FILE_MODEL_INDEX);
            
            if ( obj == null || !(obj instanceof ShareFile) )
            {
                return;
            }
            ShareFile sFile = (ShareFile)obj;
            URN urn = sFile.getURN();
            String url = URLUtil.buildBitziLookupURL( urn );
            try
            {
                BrowserLauncher.openURL( url );
            }
            catch ( IOException exp )
            {
                NLogger.warn(NLoggerNames.USER_INTERFACE, exp);

                Object[] dialogOptions = new Object[]
                {
                    Localizer.getString( "Yes" ),
                    Localizer.getString( "No" )
                };

                int choice = JOptionPane.showOptionDialog( LibraryTab.this,
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
            int row = sharedFilesTable.getSelectedRow();
            row = sharedFilesTable.convertRowIndexToModel(row);
            if ( row < 0 )
            {
                setEnabled( false );
                return;
            }
            Object obj = sharedFilesModel.getValueAt(row, SharedFilesTableModel.FILE_MODEL_INDEX);
            if ( obj == null || !(obj instanceof ShareFile) )
            {
                setEnabled( false );
            }
            else
            {
                setEnabled( true );
            }            
        }
    }

    private class FilterAction extends FWAction
    {
        public FilterAction()
        {
            super( Localizer.getString( "LibraryTab_Filter" ),
                GUIRegistry.getInstance().getIconFactory().getIcon("Filter"),
                Localizer.getString( "LibraryTab_TTTFilter" ) );
        }
        
        public void actionPerformed(ActionEvent e)
        {
            FilterLibraryDialog dialog = new FilterLibraryDialog();
            dialog.setVisible(true);
        }

        /**
         * @see phex.gui.actions.FWAction#refreshActionState()
         */
        public void refreshActionState()
        {            
        }
    }
    
    private class ExploreFolderAction extends FWAction
    {
        ExploreFolderAction()
        {
            super(Localizer.getString("LibraryTab_Explore"), 
                GUIRegistry.getInstance().getIconFactory().getIcon("Explore"),
                Localizer.getString("LibraryTab_TTTExplore"));
        }
        
        public void actionPerformed(ActionEvent e)
        {
            TreePath selectionPath = mainTree.getSelectionPath();
            if ( selectionPath == null )
            {
                return;
            }
            Object lastPathComponent = selectionPath.getLastPathComponent();
            if ( !(lastPathComponent instanceof LibraryNode ))
            {
                return;
                
            }
            final File dir = ((LibraryNode)lastPathComponent).getSystemFile();
            if ( dir == null )
            {
                return;
            }
            
            try
            {
                SystemShellExecute.exploreFolder(dir);
            }
            catch (IOException exp)
            {// ignore and do nothing..
            }
        }

        /**
         * @see phex.gui.actions.FWAction#refreshActionState()
         */
        public void refreshActionState()
        {
            TreePath selectionPath = mainTree.getSelectionPath();
            if ( selectionPath == null )
            {
                setEnabled(false);
                return;
            }
            Object lastPathComponent = selectionPath.getLastPathComponent();
            if ( !(lastPathComponent instanceof LibraryNode ))
            {
                setEnabled(false);
                return;
            }
            File file = ((LibraryNode)lastPathComponent).getSystemFile();
            if ( file == null )
            {
                setEnabled(false);
                return;
            }
            setEnabled(true);
        }
    }
    
    /**
     * Starts a download.
     */
    private class AddShareFolderAction extends FWAction
    {
        AddShareFolderAction()
        {
            super( Localizer.getString("LibraryTab_Share"), 
                GUIRegistry.getInstance().getIconFactory().getIcon( "ShareFolder"), 
                Localizer.getString("LibraryTab_TTTShare"));
        }

        public void actionPerformed(ActionEvent e)
        {
            try
            {
                File currentDirectory = null;
                TreePath selectionPath = mainTree.getSelectionPath();
                if ( selectionPath != null )
                {
                    Object lastPathComponent = selectionPath.getLastPathComponent();
                    if ( lastPathComponent instanceof LibraryNode )
                    {
                        currentDirectory = ((LibraryNode)lastPathComponent).getSystemFile();
                    }
                }
                
                final File[] files = FileDialogHandler.openMultipleDirectoryChooser( 
                    LibraryTab.this,
                    Localizer.getString( "LibraryTab_SelectDirectoryToShare" ),
                    Localizer.getString( "LibraryTab_Select" ), 
                    Localizer.getChar( "LibraryTab_SelectMnemonic" ),
                    currentDirectory,
                    Localizer.getString( "LibraryTab_CopyrightWarnTitle" ),
                    Localizer.getString( "LibraryTab_CopyrightWarnMessage" ) );
                if ( files == null )
                {
                    return;
                }
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
                                    shareDirRecursive(files[i]);
                                }
                            }
                            GUIActionPerformer.rescanSharedFiles();
                        }
                        catch ( Throwable th )
                        {
                            NLogger.error(NLoggerNames.USER_INTERFACE, th, th );
                        }
                    }
                };
                ThreadPool.getInstance().addJob(runner, "AddShareFolderAction");
            }
            catch ( Throwable th )
            {
                NLogger.error(NLoggerNames.USER_INTERFACE, th, th );
            }
        }

        public void refreshActionState()
        {
        }
        
        private void shareDirRecursive(File file)
        {
            if (!file.isDirectory())
            {
                return;
            }
            ServiceManager.sCfg.sharedDirectoriesSet.add(file
                .getAbsolutePath());
            File[] dirs = file.listFiles(new DirectoryOnlyFileFilter());
            for (int i = 0; i < dirs.length; i++)
            {
                shareDirRecursive(dirs[i]);
            }
        }
    }
    
    private class RemoveShareFolderAction extends FWAction
    {
        RemoveShareFolderAction()
        {
            super( Localizer.getString("LibraryTab_StopShare"), 
                GUIRegistry.getInstance().getIconFactory().getIcon("ShareFolderClear"), 
                Localizer.getString("LibraryTab_TTTStopShare"));
            refreshActionState();
        }

        public void actionPerformed(ActionEvent e)
        {
            TreePath selectionPath = mainTree.getSelectionPath();
            if ( selectionPath == null )
            {
                return;
            }
            Object lastPathComponent = selectionPath.getLastPathComponent();
            if ( !(lastPathComponent instanceof LibraryNode ))
            {
                return;
                
            }
            final File file = ((LibraryNode)lastPathComponent).getSystemFile();
            if ( file == null )
            {
                return;
            }
            Runnable runner = new Runnable()
            {
                public void run()
                {
                    stopShareDirRecursive(file);
                    GUIActionPerformer.rescanSharedFiles();
                }
            };
            ThreadPool.getInstance().addJob(runner, "RemoveShareFolderAction");
            refreshActionState();
        }
        
        private void stopShareDirRecursive(File file)
        {
            if (!file.isDirectory())
            {
                return;
            }
            ServiceManager.sCfg.sharedDirectoriesSet.remove(file.getAbsolutePath());
            SharedFilesService sharedFilesService = ShareManager
                .getInstance().getSharedFilesService();
            SharedDirectory directory = sharedFilesService.getSharedDirectory(file);
            if (directory == null)
            {
                // in case there is no shared directory here..
                // we can assume there is no shared subdirectory available.
                return;
            }
            
            File[] dirs = file.listFiles(new DirectoryOnlyFileFilter());
            for (int i = 0; i < dirs.length; i++)
            {
                stopShareDirRecursive(dirs[i]);
            }
        }

        public void refreshActionState()
        {
            TreePath selectionPath = mainTree.getSelectionPath();
            if ( selectionPath == null )
            {
                setEnabled(false);
                return;
            }
            Object lastPathComponent = selectionPath.getLastPathComponent();
            if ( !(lastPathComponent instanceof LibraryNode ))
            {
                setEnabled(false);
                return;
            }
            File file = ((LibraryNode)lastPathComponent).getSystemFile();
            if ( file == null )
            {
                setEnabled(false);
                return;
            }
            setEnabled(true);
        }
    }

    //////////////////////////////////////////////////////////////////////////
    /// Listeners
    //////////////////////////////////////////////////////////////////////////

    private class SelectionHandler implements ListSelectionListener,
        TreeSelectionListener
    {
        public void valueChanged(ListSelectionEvent e)
        {
            if (!e.getValueIsAdjusting())
            {
                refreshTabActions();
            }
        }

        public void valueChanged( TreeSelectionEvent e )
        {
            final Object treeRoot = sharingTreeModel.getRoot();
            final Object lastPathComponent = e.getPath().getLastPathComponent();
            if ( lastPathComponent == treeRoot )
            {
                EventQueue.invokeLater( new Runnable()
                {
                    public void run()
                    {
                        mainTree
                            .setSelectionPath( new TreePath(
                                new Object[]
                                { treeRoot,
                                    sharingTreeModel.getChild( treeRoot, 0 ) } ) );
                    }
                } );
                return;
            }

            // run in separate thread.. not event thread to make sure tree selection
            // changes immediately while table needs a little more to update.
            ThreadPool.getInstance().addJob( new Runnable()
            {
                public void run()
                {
                    if ( lastPathComponent instanceof LibraryNode )
                    {
                        // then fill data..
                        sharedFilesModel
                            .setDisplayDirectory( ((LibraryNode) lastPathComponent)
                                .getSystemFile() );
                    }
                    else
                    {
                        sharedFilesModel.setDisplayDirectory( null );
                    }
                }
            }, "LibraryTableUpdate" );
            refreshTabActions();
        }
    }

    private class SharedFilesChangeHandler implements ShareChangeListener
    {
        /**
         * Called if a shared files changed.
         */
        public void sharedDirectoriesChanged()
        {
            updateLabel();
        }

        private void updateLabel()
        {
            SharedFilesService sharedFilesService = ShareManager.getInstance().getSharedFilesService();
            StringBuffer buffer = new StringBuffer();
            buffer.append( '(' );
            buffer.append( sharedFilesService.getFileCount() );
            buffer.append( " / " );
            buffer.append( NumberFormatUtils.formatSignificantByteSize(  
                ((long)sharedFilesService.getTotalFileSizeInKb()) * 1024L ) ).append( ')' );

            sharedFilesLabel.setText( buffer.toString() );
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
                popupMenu((Component) e.getSource(), e.getX(), e.getY());
            }
        }

        public void mousePressed(MouseEvent e)
        {
            if (e.isPopupTrigger())
            {
                popupMenu((Component) e.getSource(), e.getX(), e.getY());
            }
        }

        private void popupMenu(Component source, int x, int y)
        {
            if (source == mainTree)
            {
                refreshTabActions();
                fileTreePopup.show(source, x, y);
            }
            else if (source == sharedFilesTable )
            {
                refreshTabActions();
                fileTablePopup.show(source, x, y);
            }
        }
    }
}