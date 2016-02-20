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
 *  Created on 02.10.2005
 *  --- CVS Information ---
 *  $Id: DownloadTransfersPanel.java,v 1.3 2005/10/08 18:29:27 gregork Exp $
 */
package phex.gui.tabs.download;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.xml.bind.JAXBException;

import phex.download.swarming.SWDownloadFile;
import phex.gui.common.GUIRegistry;
import phex.gui.common.GUIUtils;
import phex.gui.common.table.FWSortedTableModel;
import phex.gui.common.table.FWTable;
import phex.gui.common.table.FWTableColumnModel;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;
import phex.xml.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class DownloadTransfersPanel extends JPanel
{
    private static final String TRANSFER_TABLE_IDENTIFIER = "TransferTable";
    
    private SWDownloadFile lastDownloadFile;
    
    private FWTable transferTable;
    private FWTableColumnModel transferColumnModel;
    private JScrollPane transferTableScrollPane;
    private DownloadTransferTableModel transferModel;

    public DownloadTransfersPanel()
    {
    }
    
    public void initializeComponent(  XJBGUISettings guiSettings )
    {
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
            "fill:d:grow", // columns
            "fill:d:grow"); //rows
        PanelBuilder tabBuilder = new PanelBuilder( layout, this );
        
        transferModel = new DownloadTransferTableModel( );
        XJBGUITable xjbTable = GUIUtils.getXJBGUITableByIdentifier( guiSettings,
            TRANSFER_TABLE_IDENTIFIER );
        buildTransferTableColumnModel( xjbTable );
        
        transferTable = new FWTable( new FWSortedTableModel( transferModel ),
            transferColumnModel );
        transferTable.activateAllHeaderActions();
        transferTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        transferTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        GUIRegistry.getInstance().getTableUpdateService().registerTable( transferTable );
        transferTableScrollPane = FWTable.createFWTableScrollPane( transferTable );
        
        tabBuilder.add( transferTableScrollPane, cc.xy( 1, 1 ) );
        
        GUIUtils.adjustTableProgresssBarHeight( transferTable );
        
        ActionListener updateInterfaceAction = new ActionListener()
        {
            public void actionPerformed( ActionEvent e )
            {
                try
                {
                    updateInterface();
                }
                catch ( Throwable th )
                {
                    NLogger.error(NLoggerNames.USER_INTERFACE, th, th);
                }
            }
        };
        Timer timer = new Timer( 2000, updateInterfaceAction );
        timer.start();
    }
    
    public void updateDownloadFile( SWDownloadFile file )
    {
        lastDownloadFile = file;
        transferModel.updateDownloadFile( file );
        updateInterface();
    }
    
    private void updateInterface()
    {
        transferModel.fireTableRowsUpdated( 0, transferModel.getRowCount() );
    }
    
    /**
     * This is overloaded to update the table size for the progress bar on
     * every UI update. Like font size change!
     */
    public void updateUI()
    {
        super.updateUI();
        if ( transferTable != null )
        {
            // increase table height a bit to display progress bar string better...
            GUIUtils.adjustTableProgresssBarHeight( transferTable );
        }
        if ( transferTableScrollPane != null )
        {
            FWTable.updateFWTableScrollPane( transferTableScrollPane );
        }
    }
    
    
    //////////////////////////////////////////////////////////////////////////
    /// XML serializing and deserializing
    //////////////////////////////////////////////////////////////////////////
    
    private void buildTransferTableColumnModel( XJBGUITable tableSettings )
    {
        int[] columnIds = DownloadTransferTableModel.getColumnIdArray();
        XJBGUITableColumnList columnList = null;
        if ( tableSettings != null )
        {
            columnList = tableSettings.getTableColumnList();
        }

        transferColumnModel = new FWTableColumnModel( transferModel, columnIds,
            columnList );
    }
    
    public void appendXJBGUISettings( XJBGUISettings xjbSettings )
        throws JAXBException
    {
        XJBGUITableColumnList xjbList = transferColumnModel.createXJBGUITableColumnList();
        ObjectFactory objFactory = new ObjectFactory();
        XJBGUITable xjbTable = objFactory.createXJBGUITable();
        xjbTable.setTableColumnList( xjbList );
        xjbTable.setTableIdentifier( TRANSFER_TABLE_IDENTIFIER );
        xjbSettings.getTableList().getTableList().add( xjbTable );
    }
}
