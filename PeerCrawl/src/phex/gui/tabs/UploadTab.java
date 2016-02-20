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
 *  $Id: UploadTab.java,v 1.12 2005/11/03 16:33:49 gregork Exp $
 */
package phex.gui.tabs;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.IOException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.xml.bind.JAXBException;

import phex.chat.ChatManager;
import phex.common.ThreadPool;
import phex.common.address.DestAddress;
import phex.event.UploadFilesChangeListener;
import phex.gui.actions.FWAction;
import phex.gui.actions.GUIActionPerformer;
import phex.gui.common.*;
import phex.gui.common.table.FWSortedTableModel;
import phex.gui.common.table.FWTable;
import phex.gui.common.table.FWTableColumnModel;
import phex.gui.models.UploadFilesTableModel;
import phex.upload.UploadManager;
import phex.upload.UploadState;
import phex.utils.*;
import phex.xml.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class UploadTab extends FWTab
{
    private static final String UPLOAD_TABLE_IDENTIFIER = "UploadTable";
    
    private static final UploadState[] EMPTY_UPLOADSTATE_ARRAY = new UploadState[0];

    private UploadManager uploadManager;
    private MainFrame mainFrame;

    private FWTable uploadTable;
    private JScrollPane uploadTableScrollPane;
    private UploadFilesTableModel uploadModel;
    private FWTableColumnModel uploadColumnModel;
    private JPopupMenu uploadPopup;

    public UploadTab( MainFrame frame )
    {
        super( MainFrame.UPLOAD_TAB_ID, Localizer.getString( "Upload" ),
            GUIRegistry.getInstance().getIconFactory().getIcon( "Upload" ),
            Localizer.getString( "TTTUpload" ),Localizer.getChar(
            "UploadMnemonic"), KeyStroke.getKeyStroke( Localizer.getString(
            "UploadAccelerator" ) ), MainFrame.SHARE_TAB_INDEX);
        mainFrame = frame;
        uploadManager = UploadManager.getInstance();
        uploadManager.addUploadFilesChangeListener( new UploadStateChangeHandler() );
    }

    public void initComponent( XJBGUISettings guiSettings )
    {
        CellConstraints cc = new CellConstraints();
        FormLayout tabLayout = new FormLayout("2dlu, fill:d:grow, 2dlu", // columns
            "2dlu, fill:p:grow, 2dlu"); //rows
        PanelBuilder tabBuilder = new PanelBuilder(tabLayout, this);
        JPanel contentPanel = new JPanel();
        FWElegantPanel banner = new FWElegantPanel( Localizer.getString("Uploads"),
            contentPanel );
        tabBuilder.add(banner, cc.xy(2, 2));
        
        FormLayout contentLayout = new FormLayout(
            "fill:d:grow", // columns
            "fill:d:grow, 1dlu, p"); //rows
        PanelBuilder contentBuilder = new PanelBuilder(contentLayout, contentPanel);
        
        uploadModel = new UploadFilesTableModel();
        XJBGUITable xjbTable = GUIUtils.getXJBGUITableByIdentifier( guiSettings,
            UPLOAD_TABLE_IDENTIFIER );
        buildUploadTableColumnModel( xjbTable );

        MouseHandler mouseHandler = new MouseHandler();
        uploadTable = new FWTable( new FWSortedTableModel( uploadModel ),
            uploadColumnModel );
        uploadTable.activateAllHeaderActions();
        uploadTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        uploadTable.getSelectionModel().addListSelectionListener(
            new SelectionHandler() );
        uploadTable.addMouseListener( mouseHandler );
        uploadTableScrollPane = FWTable.createFWTableScrollPane( uploadTable );
        uploadTableScrollPane.addMouseListener( mouseHandler );
        contentBuilder.add( uploadTableScrollPane, cc.xy( 1, 1 ) );

        // increase table height a bit to display progress bar string better...
        GUIUtils.adjustTableProgresssBarHeight( uploadTable );
        
        FWToolBar uploadToolbar = new FWToolBar( JToolBar.HORIZONTAL );
        uploadToolbar.setBorderPainted( false );
        uploadToolbar.setFloatable( false );
        contentBuilder.add( uploadToolbar, cc.xy( 1, 3 ) );

        uploadPopup = new JPopupMenu();

        // add actions to toolbar
        FWAction action = new AbortUploadAction();
        addTabAction( action );
        uploadToolbar.addAction( action );
        uploadPopup.add( action );

        action = new RemoveUploadAction();
        addTabAction( action );
        uploadToolbar.addAction( action );
        uploadPopup.add( action );

        action = new ViewBitziTicketAction();
        addTabAction( action );
        uploadPopup.add( action );

        uploadToolbar.addSeparator();
        uploadPopup.addSeparator();
        
        action = new AddToFavoritesAction();
        addTabAction( action );
        uploadPopup.add( action );

        action = new BrowseHostAction();
        addTabAction( action );
        uploadToolbar.addAction( action );
        uploadPopup.add( action );

        action = new ChatToHostAction();
        addTabAction( action );
        uploadToolbar.addAction( action );
        uploadPopup.add( action );

        action = new BanHostAction();
        addTabAction( action );
        uploadToolbar.addAction( action );
        uploadPopup.add( action );

        uploadToolbar.addSeparator();
        uploadPopup.addSeparator();

        action = new ClearUploadsAction();
        addTabAction( action );
        uploadToolbar.addAction( action );
        uploadPopup.add( action );
    }
    
    /**
     * Indicates if this tab is visible by default, when there is no known 
     * visible setting from the user.
     * @return true if visible by default false otherwise.
     */
    public boolean isVisibleByDefault()
    {
        return false;
    }

    /**
     * This is overloaded to update the table size for the progress bar on
     * every UI update. Like font size change!
     */
    public void updateUI()
    {
        super.updateUI();
        if ( uploadTable != null )
        {
            // increase table height a bit to display progress bar string better...
            GUIUtils.adjustTableProgresssBarHeight( uploadTable );
        }
        if ( uploadTableScrollPane != null )
        {
            FWTable.updateFWTableScrollPane( uploadTableScrollPane );
        }
    }

    private UploadState[] getSelectedUploadStates()
    {
        int[] viewRows = uploadTable.getSelectedRows();
        if ( viewRows.length == 0 )
        {
            return EMPTY_UPLOADSTATE_ARRAY;
        }
        int[] modelRows = uploadTable.convertRowIndicesToModel( viewRows );

        UploadState[] states = uploadManager.getUploadStatesAt( modelRows );
        return states;
    }

    private UploadState getSelectedUploadState()
    {
        int viewRow = uploadTable.getSelectedRow();
        int modelRow = uploadTable.convertRowIndexToModel( viewRow );
        UploadState state = uploadManager.getUploadStateAt( modelRow );
        return state;
    }

    //////////////////////////////////////////////////////////////////////////
    /// XML serializing and deserializing
    //////////////////////////////////////////////////////////////////////////

    private void buildUploadTableColumnModel( XJBGUITable tableSettings )
    {
        int[] columnIds = UploadFilesTableModel.getColumnIdArray();
        XJBGUITableColumnList columnList = null;
        if ( tableSettings != null )
        {
            columnList = tableSettings.getTableColumnList();
        }

        uploadColumnModel = new FWTableColumnModel( uploadModel, columnIds,
            columnList );
    }

    public void appendXJBGUISettings( XJBGUISettings xjbSettings )
        throws JAXBException
    {
        super.appendXJBGUISettings( xjbSettings );
        XJBGUITableColumnList xjbList = uploadColumnModel.createXJBGUITableColumnList();
        ObjectFactory objFactory = new ObjectFactory();
        XJBGUITable xjbTable = objFactory.createXJBGUITable();
        xjbTable.setTableColumnList( xjbList );
        xjbTable.setTableIdentifier( UPLOAD_TABLE_IDENTIFIER );
        xjbSettings.getTableList().getTableList().add( xjbTable );
    }

    //////////////////////////////////////////////////////////////////////////
    /// Actions
    //////////////////////////////////////////////////////////////////////////

    private class AbortUploadAction extends FWAction
    {
        AbortUploadAction()
        {
            super( Localizer.getString( "AbortUpload" ),
                GUIRegistry.getInstance().getIconFactory().getIcon("Stop"),
                Localizer.getString( "TTTAbortUpload" ) );
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            UploadState[] states = getSelectedUploadStates();
            for ( int i = 0; i < states.length; i++ )
            {
                if ( states[i] != null && states[i].isUploadRunning() )
                {
                    states[i].stopUpload();
                }

            }
            refreshActionState();
        }

        public void refreshActionState()
        {
            UploadState[] states = getSelectedUploadStates();
            boolean state = false;
            for ( int i = 0; i < states.length; i++ )
            {
                if ( states[i] != null && states[i].isUploadRunning() )
                {
                    state = true;
                    break;
                }
            }
            setEnabled( state );
        }
    }

    private class RemoveUploadAction extends FWAction
    {
        RemoveUploadAction()
        {
            super( Localizer.getString( "RemoveUpload" ),
                GUIRegistry.getInstance().getIconFactory().getIcon("Remove"),
                Localizer.getString( "TTTRemoveUpload" ) );
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            UploadState[] states = getSelectedUploadStates();
            for ( int i = 0; i < states.length; i++ )
            {
                if ( states[i] != null )
                {
                    uploadManager.removeUploadState( states[i] );
                }
            }
            refreshActionState();
        }

        public void refreshActionState()
        {
            if ( uploadTable.getRowCount() > 0 )
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
            UploadState state = getSelectedUploadState();
            if ( state == null )
            {
                return;
            }

            ChatManager.getInstance().openChat( state.getHostAddress() );
        }

        public void refreshActionState()
        {
            if ( uploadTable.getSelectedRowCount() == 1 )
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
        BanHostAction()
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
                UploadState[] states = getSelectedUploadStates();
                final DestAddress[] addresses = new DestAddress[states.length];
                for (int i = 0; i < states.length; i++)
                {
                    uploadManager.removeUploadState( states[i] );
                    addresses[ i ] = states[i].getHostAddress();
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
            if ( uploadTable.getRowCount() > 0 )
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
            UploadState[] states = getSelectedUploadStates();
            DestAddress[] addresses = new DestAddress[states.length];
            for (int i = 0; i < states.length; i++)
            {
                addresses[ i ] = states[i].getHostAddress();
            }
            GUIActionPerformer.addHostsToFavorites( addresses );
        }

        public void refreshActionState()
        {
            if ( uploadTable.getSelectedRowCount() == 1 )
            {
                setEnabled( true );
            }
            else
            {
                setEnabled( false );
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
            UploadState state = getSelectedUploadState();
            if ( state == null )
            {
                return;
            }
            String url = URLUtil.buildBitziLookupURL(
                state.getFileURN() );
            try
            {
                BrowserLauncher.openURL( url );
            }
            catch ( IOException exp )
            {
                NLogger.warn( NLoggerNames.USER_INTERFACE, exp, exp);

                Object[] dialogOptions = new Object[]
                {
                    Localizer.getString( "Yes" ),
                    Localizer.getString( "No" )
                };

                int choice = JOptionPane.showOptionDialog( UploadTab.this,
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
            if ( uploadTable.getSelectedRowCount() == 1 )
            {
                setEnabled( true );
            }
            else
            {
                setEnabled( false );
            }
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
            UploadState state = getSelectedUploadState();
            if ( state == null )
            {
                return;
            }
            GUIActionPerformer.browseHost( state.getHostAddress() );
        }

        public void refreshActionState()
        {
            if ( uploadTable.getSelectedRowCount() == 1 )
            {
                setEnabled( true );
            }
            else
            {
                setEnabled( false );
            }
        }
    }

    class ClearUploadsAction extends FWAction
    {
        ClearUploadsAction()
        {
            super( Localizer.getString( "ClearCompleted" ),
                GUIRegistry.getInstance().getIconFactory().getIcon("Trash"),
                Localizer.getString( "TTTClearCompleted" ) );
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            uploadManager.cleanUploadStateList();
        }

        public void refreshActionState()
        {
            if ( uploadTable.getRowCount() > 0 )
            {
                setEnabled( true );
            }
            else
            {
                setEnabled( false );
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////
    /// Listeners
    //////////////////////////////////////////////////////////////////////////

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

    private class UploadStateChangeHandler implements UploadFilesChangeListener
    {
        /**
         * Called if a upload file changed.
         */
        public void uploadFileChanged( int position )
        {
            refreshTabActions();
        }

        /**
         * Called if a upload file was added.
         */
        public void uploadFileAdded( int position )
        {
            refreshTabActions();
        }

        /**
         * Called if the upload queue was changed.
         */
        public void uploadQueueChanged()
        {
        }


        /**
         * Called if a upload file was removed.
         */
        public void uploadFileRemoved( int position )
        {
            refreshTabActions();
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
            if ( source == uploadTable || source == uploadTableScrollPane )
            {
                uploadPopup.show(source, x, y);
            }
        }
    }
}