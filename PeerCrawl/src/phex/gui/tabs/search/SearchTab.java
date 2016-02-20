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
 *  $Id: SearchTab.java,v 1.15 2005/10/03 00:18:27 gregork Exp $
 */
package phex.gui.tabs.search;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.*;
import javax.xml.bind.JAXBException;

import phex.gui.actions.FWAction;
import phex.gui.common.*;
import phex.gui.common.GUIRegistry;
import phex.gui.common.IconFactory;
import phex.gui.common.MainFrame;
import phex.gui.tabs.FWTab;
import phex.query.QueryManager;
import phex.query.SearchContainer;
import phex.query.SearchFilter;
import phex.utils.Localizer;
import phex.xml.XJBGUISettings;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 *
 */
public class SearchTab extends FWTab
{
    private SearchResultsDataModel displayedDataModel;
    
    private SearchListPanel searchListPanel;
    private SearchControlPanel searchControlPanel;
    private SearchResultsPanel searchResultPanel;
    
    public SearchTab( )
    {
        super( MainFrame.SEARCH_TAB_ID, Localizer.getString( "Search" ),
            GUIRegistry.getInstance().getIconFactory().getIcon( "Search" ),
            Localizer.getString( "TTTSearchTab" ), Localizer.getChar(
            "SearchMnemonic"), KeyStroke.getKeyStroke( Localizer.getString(
            "SearchAccelerator" ) ), MainFrame.SEARCH_TAB_INDEX );
    }
    
    public void initComponent( XJBGUISettings guiSettings )
    {
        addTabAction( CLEAR_SEARCH_RESULTS_ACTION, new ClearSearchResultsAction() );
        addTabAction( CREATE_NEW_SEARCH_ACTION, new CreateNewSearchAction() );
        addTabAction( REMOVE_FILTER_ACTION, new RemoveFilterAction() );
        addTabAction( CLOSE_SEARCH_ACTION, new CloseSearchAction() );
        
        CellConstraints cc = new CellConstraints();
        FormLayout tabLayout = new FormLayout("2dlu, fill:d:grow, 2dlu", // columns
            "2dlu, fill:p:grow, 2dlu"); //rows
        PanelBuilder tabBuilder = new PanelBuilder(tabLayout, this);
        JPanel contentPanel = new JPanel();
        FWElegantPanel banner = new FWElegantPanel( Localizer.getString("Search"),
            contentPanel );
        tabBuilder.add(banner, cc.xy(2, 2));
        
        FormLayout contentLayout = new FormLayout(
            "fill:d:grow", // columns
            "fill:d:grow"); //rows
        PanelBuilder contentBuilder = new PanelBuilder(contentLayout, contentPanel);
        
        searchListPanel = new SearchListPanel( this );
        searchListPanel.initializeComponent( guiSettings );
        
        JPanel lowerPanel = new JPanel();
        FormLayout lowerLayout = new FormLayout(
            "d, 1dlu, fill:d:grow", // columns
            "fill:d:grow"); //rows
        PanelBuilder lowerBuilder = new PanelBuilder( lowerLayout, lowerPanel );
        searchControlPanel = new SearchControlPanel( this );
        lowerBuilder.add( searchControlPanel, cc.xy( 1, 1  ) );
        searchResultPanel = new SearchResultsPanel( this );
        searchResultPanel.initializeComponent( guiSettings );
        lowerBuilder.add( searchResultPanel, cc.xy( 3, 1  ) );
        
        
        Dimension dim = new Dimension( 400, 300 );
        lowerPanel.setPreferredSize( dim );
        lowerPanel.setMinimumSize( new Dimension( 0, 0 ) );
        searchListPanel.setMinimumSize( new Dimension( 0, 0 ) );
        
        JSplitPane splitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT,
            searchListPanel, lowerPanel );
        splitPane.setBorder( BorderFactory.createEmptyBorder( 0, 0, 0, 0) );
        splitPane.setDividerSize( 4 );
        splitPane.setOneTouchExpandable( false );

        contentBuilder.add( splitPane, cc.xy( 1, 1 ) );
    }
    
    /**
     * Updates the displayedSearch for parts of the search UI. 
     * @param search the displayed search to show on all parts of the search UI.
     */
    public void setDisplayedSearch( final SearchResultsDataModel searchResultsDataModel )
    {
        if ( displayedDataModel == searchResultsDataModel )
        {
            return;
        }
        displayedDataModel = searchResultsDataModel;
        searchResultPanel.setDisplayedSearch( displayedDataModel );
        searchListPanel.setDisplayedSearch( displayedDataModel );
        searchControlPanel.setDisplayedSearch( displayedDataModel );
        refreshTabActions();
    }
    
    /**
     * Clears the search history in the search control panel and configuration.
     */
    public void clearSearchHistory()
    {
        searchControlPanel.clearSearchHistory();
    }

    
    public void appendXJBGUISettings( XJBGUISettings xjbSettings )
        throws JAXBException
    {
        super.appendXJBGUISettings( xjbSettings );
        searchListPanel.appendXJBGUISettings( xjbSettings );
        searchResultPanel.appendXJBGUISettings( xjbSettings );
    }
    
    /////////////////////// Start Tab Actions///////////////////////////////////
    
    public static final String CLEAR_SEARCH_RESULTS_ACTION = "ClearSearchResultsAction";
    public static final String CREATE_NEW_SEARCH_ACTION = "CreateNewSearchAction";
    public static final String CLOSE_SEARCH_ACTION = "CloseSearchAction";
    public static final String REMOVE_FILTER_ACTION = "RemoveFilterAction";
    
    private class CloseSearchAction extends FWAction
    {
        private SearchContainer searchContainer;
        
        public CloseSearchAction()
        {
            super( Localizer.getString( "CloseSearch" ),
                GUIRegistry.getInstance().getIconFactory().getIcon("Close"),
                Localizer.getString( "TTTCloseSearch" ), null, 
                KeyStroke.getKeyStroke( KeyEvent.VK_DELETE, 0 ) );
            searchContainer = QueryManager.getInstance().getSearchContainer();
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            if ( displayedDataModel != null )
            {
                searchContainer.removeSearch( displayedDataModel.getSearch() );
                setDisplayedSearch( null );
            }
        }

        public void refreshActionState()
        {
            if ( displayedDataModel == null )
            {
                setEnabled( false );
            }
            else
            {
                setEnabled( true );
            }
        }
    }
    
    private class ClearSearchResultsAction extends FWAction
    {
        public ClearSearchResultsAction()
        {
            super( Localizer.getString( "ClearSearchResults" ),
                GUIRegistry.getInstance().getIconFactory().getIcon("Remove"),
                Localizer.getString( "TTTClearSearchResults" ) );
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            if ( displayedDataModel != null )
            {
                displayedDataModel.clearSearchResults();
            }
        }

        public void refreshActionState()
        {
            if ( displayedDataModel == null ||
                  displayedDataModel.getSearchElementCount()
                + displayedDataModel.getFilteredElementCount() == 0)
            {
                setEnabled( false );
            }
            else
            {
                setEnabled( true );
            }
        }
    }
    
    private class RemoveFilterAction extends FWAction
    {
        public RemoveFilterAction()
        {
            super( Localizer.getString( "RemoveSearchFilter" ), IconFactory.EMPTY_IMAGE_16,
                Localizer.getString( "TTTRemoveSearchFilter" ) );
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            if ( displayedDataModel != null )
            {
                displayedDataModel.clearSearchFilter();
                refreshTabActions();
            }
        }

        public void refreshActionState()
        {
            
            if ( displayedDataModel == null )
            {
                setEnabled( false );
            }
            else
            {
                SearchFilter filter = displayedDataModel.getSearchFilter();
                setEnabled( filter!=null );
            }
        }
    }
    
    private class CreateNewSearchAction extends FWAction
    {
        public CreateNewSearchAction()
        {
            super( Localizer.getString( "CreateNewSearch" ),
                GUIRegistry.getInstance().getIconFactory().getIcon("Search"),
                Localizer.getString( "TTTCreateNewSearch" ) );
            refreshActionState();
        }

        public void actionPerformed( ActionEvent e )
        {
            setDisplayedSearch( null );
        }

        public void refreshActionState()
        {
            setEnabled( true );
        }
    }
    
    /////////////////////// End Tab Actions///////////////////////////////////
}