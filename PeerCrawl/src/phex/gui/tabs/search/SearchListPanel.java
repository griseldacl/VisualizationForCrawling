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
 *  $Id: SearchListPanel.java,v 1.16 2005/10/03 00:18:27 gregork Exp $
 */
package phex.gui.tabs.search;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.*;
import javax.swing.event.*;
import javax.xml.bind.JAXBException;

import phex.gui.actions.FWAction;
import phex.gui.common.GUIRegistry;
import phex.gui.common.GUIUtils;
import phex.gui.common.table.FWSortedTableModel;
import phex.gui.common.table.FWTable;
import phex.gui.common.table.FWTableColumnModel;
import phex.gui.models.SearchListTableModel;
import phex.query.QueryManager;
import phex.query.Search;
import phex.query.SearchContainer;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;
import phex.xml.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class SearchListPanel extends JPanel
{
    private static final String SEARCHLIST_TABLE_IDENTIFIER = "SearchListTable";
    
    private SearchContainer searchContainer;
    private SearchTab searchTab;
    
    private FWTable searchListTable;
    private JScrollPane searchListTableScrollPane;
    private FWTableColumnModel searchListColumnModel;
    private SearchListTableModel searchListModel;
    private JPopupMenu searchListPopup;
    
    public SearchListPanel( SearchTab tab )
    {
        super( );
        searchContainer = QueryManager.getInstance().getSearchContainer();
        searchTab = tab;
    }
    
    public void initializeComponent( XJBGUISettings guiSettings )
    {
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
            "fill:d:grow", // columns
            "fill:d:grow"); //rows
        PanelBuilder panelBuilder = new PanelBuilder( layout, this );
        
        searchListModel = new SearchListTableModel();
        searchListModel.addTableModelListener( new SearchListTableListener() );
        XJBGUITable xjbTable = GUIUtils.getXJBGUITableByIdentifier( guiSettings,
            SEARCHLIST_TABLE_IDENTIFIER );
        buildSearchListTableColumnModel( xjbTable );

        MouseHandler mouseHandler = new MouseHandler();
        searchListTable = new FWTable( new FWSortedTableModel( searchListModel ),
            searchListColumnModel );
        searchListTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        searchListTable.activateAllHeaderActions();
        searchListTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        searchListTable.getSelectionModel().addListSelectionListener(new SelectionHandler());
        searchListTable.addMouseListener( mouseHandler );
        GUIRegistry.getInstance().getTableUpdateService().registerTable( searchListTable );
        searchListTableScrollPane = FWTable.createFWTableScrollPane( searchListTable );
        searchListTableScrollPane.addMouseListener( mouseHandler );
        
        panelBuilder.add( searchListTableScrollPane, cc.xy( 1, 1 ) );
        
        // init popup menu
        searchListPopup = new JPopupMenu();
        searchListPopup.add( searchTab.getTabAction(
            SearchTab.CREATE_NEW_SEARCH_ACTION ) );
        searchListPopup.add( searchTab.getTabAction(
            SearchTab.CLEAR_SEARCH_RESULTS_ACTION ) );
        searchListPopup.add( searchTab.getTabAction(
            SearchTab.REMOVE_FILTER_ACTION ) );
        
        FWAction closeSearchAction = searchTab.getTabAction(
            SearchTab.CLOSE_SEARCH_ACTION );
        searchListPopup.add( closeSearchAction );
        searchListTable.getActionMap().put( SearchTab.CLOSE_SEARCH_ACTION, closeSearchAction);
        searchListTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put( 
            (KeyStroke)closeSearchAction.getValue(FWAction.ACCELERATOR_KEY), SearchTab.CLOSE_SEARCH_ACTION );
    }
    
    public void setDisplayedSearch( SearchResultsDataModel searchResultsDataModel )
    {
        if ( searchResultsDataModel == null )
        {
            searchListTable.getSelectionModel().clearSelection();
            return;
        }
        int modelRow = searchContainer.getIndexOfSearch( searchResultsDataModel.getSearch() );
        if ( modelRow != -1 )
        {
            int viewRow = searchListTable.convertRowIndexToView( modelRow );
            if ( viewRow != -1 )
            {
                searchListTable.getSelectionModel().setSelectionInterval( viewRow, viewRow );
            }
            else
            {
                searchListTable.getSelectionModel().clearSelection();
            }
        }
        else
        {
            searchListTable.getSelectionModel().clearSelection();
        }
    }
    
    private Search getSelectedSearch()
    {
        int viewRow = searchListTable.getSelectedRow();
        if ( viewRow < 0 )
        {
            return null;
        }
        int modelRow = searchListTable.convertRowIndexToModel( viewRow );
        Search search = searchContainer.getSearchAt( modelRow );
        return search;
    }
    
    public Dimension getPreferredSize()
    {
        Dimension pref = super.getPreferredSize();
        // show 3 rows plus the header plus a bit
        pref.height = (int)(searchListTable.getRowHeight() * 4.7);
        return pref;
    }
    
    private void buildSearchListTableColumnModel( XJBGUITable tableSettings )
    {
        int[] columnIds = SearchListTableModel.getColumnIdArray();
        XJBGUITableColumnList columnList = null;
        if ( tableSettings != null )
        {
            columnList = tableSettings.getTableColumnList();
        }

        searchListColumnModel = new FWTableColumnModel( searchListModel,
            columnIds, columnList );
    }
    
    /**
     * This is overloaded to update the combo box size on
     * every UI update. Like font size change!
     */
    public void updateUI()
    {
        super.updateUI();
        if ( searchListTableScrollPane != null )
        {
            FWTable.updateFWTableScrollPane( searchListTableScrollPane );
        }
    }
    
    public void appendXJBGUISettings( XJBGUISettings xjbSettings )
        throws JAXBException
    {
        XJBGUITableColumnList xjbList = searchListColumnModel.createXJBGUITableColumnList();
        ObjectFactory objFactory = new ObjectFactory();
        XJBGUITable xjbTable = objFactory.createXJBGUITable();
        xjbTable.setTableColumnList( xjbList );
        xjbTable.setTableIdentifier( SEARCHLIST_TABLE_IDENTIFIER );
        xjbSettings.getTableList().getTableList().add( xjbTable );
    }
    
    ////////////////////////// Start inner classes ///////////////////////////////
    
    private class SearchListTableListener implements TableModelListener
    {
        public void tableChanged(TableModelEvent e)
        {
            if ( e.getType() == TableModelEvent.INSERT )
            {
                Search search = searchContainer.getSearchAt( e.getFirstRow() );
                SearchResultsDataModel dataModel = SearchResultsDataModel.lookupResultDataModel( search );
                searchTab.setDisplayedSearch( dataModel );
            }
        }
        
    }
    
    private class SelectionHandler implements ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent e)
        {
            try
            {
	            if ( e.getValueIsAdjusting() )
	            {
	                return;
	            }
	            searchTab.refreshTabActions();
	            Search search = getSelectedSearch();
	            if ( search == null )
	            {
	                return;
	            }
	            SearchResultsDataModel dataModel = SearchResultsDataModel.lookupResultDataModel( search );
	            searchTab.setDisplayedSearch( dataModel );
            }
            catch ( Exception exp)
            {// catch all handler
                NLogger.error( NLoggerNames.USER_INTERFACE, exp, exp );
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
            if ( source == searchListTable || source == searchListTableScrollPane )
            {
                searchTab.refreshTabActions();
                searchListPopup.show(source, x, y);
            }
        }
    }
    
    ////////////////////////// End inner classes ///////////////////////////////
}
