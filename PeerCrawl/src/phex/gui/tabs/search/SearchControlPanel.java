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
 *  $Id: SearchControlPanel.java,v 1.15 2005/10/03 00:18:27 gregork Exp $
 */
package phex.gui.tabs.search;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.text.Keymap;

import phex.common.Cfg;
import phex.common.MediaType;
import phex.common.ServiceManager;
import phex.common.format.NumberFormatUtils;
import phex.event.SearchChangeEvent;
import phex.event.SearchChangeListener;
import phex.gui.common.*;
import phex.gui.renderer.MediaTypeListRenderer;
import phex.query.*;
import phex.utils.*;

public class SearchControlPanel extends JPanel implements SearchChangeListener
{
    private SizeDefinition[] sizeDefinitions =
    {
        new SizeDefinition( "BytesToken", 1 ),
        new SizeDefinition( "KBToken", NumberFormatUtils.ONE_KB ),
        new SizeDefinition( "MBToken", NumberFormatUtils.ONE_MB ),
        new SizeDefinition( "GBToken", NumberFormatUtils.ONE_GB )
    };
    
    private SearchContainer searchContainer;
    private SearchResultsDataModel displayedDataModel;
    private SearchTab searchTab;
        
    private BoxPanel searchBoxPanel;
    private DefaultComboBoxModel searchComboModel;
    private JComboBox searchTermComboBox;
    private JButton searchButton;
    
    private BoxPanel filterBoxPanel;
    private JTextField filterTextTF;
    private JComboBox mediaTypeComboBox;
    private JTextField minFileSizeTF;
    private JComboBox minFileSizeUnitComboBox;
    private JTextField maxFileSizeTF;
    private JComboBox maxFileSizeUnitComboBox;
    private JButton filterButton;
    
    public SearchControlPanel( SearchTab tab )
    {
        super( new GridBagLayout() );
        searchTab = tab;
        searchContainer = QueryManager.getInstance().getSearchContainer();
        initializeComponent();
        updateUI();
    }
    
    /**
     * Clears the search history in the search control panel and configuration.
     */
    public void clearSearchHistory()
    {
        searchComboModel.removeAllElements();
        ServiceManager.sCfg.searchTermHistory.clear();
        ServiceManager.sCfg.save();
    }


    public void initializeComponent()
    {
        GridBagConstraints constraints;
        Insets btnInsets = new Insets( 1, 1, 1, 3 );
        
        searchBoxPanel = new BoxPanel( Localizer.getString( "Search" ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.BOTH;
        add( searchBoxPanel, constraints );
        
        JLabel label = new JLabel( Localizer.getString( "EnterSearchTerm" ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.gridwidth = 2;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.insets = new Insets( 0, 5, 0, 5 );
        searchBoxPanel.addContent( label, constraints );
        
        SubmitSearchHandler submitSearchHandler = new SubmitSearchHandler();
        
        searchComboModel = new DefaultComboBoxModel(
            ServiceManager.sCfg.searchTermHistory.toArray() );
        searchTermComboBox = new JComboBox( searchComboModel );
        searchTermComboBox.setEditable( true );
        JTextField editor = ((JTextField)searchTermComboBox.getEditor().getEditorComponent());
        Keymap keymap = JTextField.addKeymap( "SearchTermEditor", editor.getKeymap() );
        editor.setKeymap( keymap );
        keymap.addActionForKeyStroke( KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            submitSearchHandler );
        GUIUtils.assignKeymapToComboBoxEditor( keymap, searchTermComboBox );
        searchTermComboBox.setSelectedItem( "" );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.gridwidth = 2;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.insets = new Insets( 0, 5, 5, 5 );
        searchBoxPanel.addContent( searchTermComboBox, constraints );
        
        searchButton = new JButton( Localizer.getString( "StartSearch" ),
            GUIRegistry.getInstance().getIconFactory().getIcon("Search" ) );
        searchButton.setCursor( Cursor.getPredefinedCursor( Cursor.HAND_CURSOR ) );
        searchButton.setToolTipText( Localizer.getString( "TTTStartSearch") );
        searchButton.setMargin( btnInsets );
        searchButton.addActionListener( submitSearchHandler );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 2;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets( 0, 5, 12, 5 );
        searchBoxPanel.addContent( searchButton, constraints );
        
        LinkLabel newSearchLink = new LinkLabel(
            searchTab.getTabAction( SearchTab.CREATE_NEW_SEARCH_ACTION ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 3;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets( 0, 5, 0, 5 );
        searchBoxPanel.addContent( newSearchLink, constraints );
        
        LinkLabel closeSearchLink = new LinkLabel(
            searchTab.getTabAction( SearchTab.CLOSE_SEARCH_ACTION ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 4;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets( 0, 5, 5, 5 );
        searchBoxPanel.addContent( closeSearchLink, constraints );

        filterBoxPanel = new BoxPanel( Localizer.getString( "FilterResults" ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.weightx = 0;
            constraints.weighty = 1;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
        add( filterBoxPanel, constraints );
        
        
        label = new JLabel( Localizer.getString( "FilterText" ) );
        label.setToolTipText( Localizer.getString("TTTFilterText") );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.insets = new Insets( 0, 5, 0, 5 );
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
        filterBoxPanel.addContent( label, constraints );
        
        ButtonActionHandler btnActionHandler = new ButtonActionHandler();
        
        filterTextTF = new JTextField( 8 );
        filterTextTF.setToolTipText( Localizer.getString("TTTFilterText") );
        keymap = JTextField.addKeymap( "SearchFilterTextEditor", filterTextTF.getKeymap() );
        filterTextTF.setKeymap( keymap );
        keymap.addActionForKeyStroke( KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            btnActionHandler );
        GUIUtils.assignKeymapToTextField( keymap, filterTextTF );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.insets = new Insets( 0, 5, 5, 5 );
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
        filterBoxPanel.addContent( filterTextTF, constraints );
        
        label = new JLabel( Localizer.getString( "FileType" ) );
        label.setToolTipText( Localizer.getString("TTTFileType") );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 2;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.insets = new Insets( 0, 5, 0, 5 );
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
        filterBoxPanel.addContent( label, constraints );
        
        mediaTypeComboBox = new JComboBox( MediaType.getAllMediaTypes() );
        mediaTypeComboBox.setRenderer( new MediaTypeListRenderer() );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 3;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.insets = new Insets( 0, 5, 5, 5 );
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
        filterBoxPanel.addContent( mediaTypeComboBox, constraints );
        
        label = new JLabel( Localizer.getString( "MinFileSize" ) );
        label.setToolTipText( Localizer.getString( "TTTMinFileSize" ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 4;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.insets = new Insets( 0, 5, 0, 5 );
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.NONE;
        filterBoxPanel.addContent( label, constraints );
        
        JPanel subPanel = new JPanel( new GridBagLayout() );
        subPanel.setOpaque( false );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 5;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.insets = new Insets( 0, 5, 5, 5 );
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
        filterBoxPanel.addContent( subPanel, constraints );
        
        minFileSizeTF = new IntegerTextField( 9 );
        minFileSizeTF.setToolTipText( Localizer.getString( "TTTMinFileSize" ) );
        keymap = JTextField.getKeymap( "SearchFilterTextEditor" );
        minFileSizeTF.setKeymap( keymap );
        GUIUtils.assignKeymapToTextField( keymap, minFileSizeTF );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.insets = new Insets( 0, 0, 0, 3 );
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.NONE;
        subPanel.add( minFileSizeTF, constraints );
        
        minFileSizeUnitComboBox = new JComboBox( sizeDefinitions );
        minFileSizeUnitComboBox.setToolTipText( Localizer.getString( "TTTMinFileSize" ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.insets = new Insets( 0, 0, 0, 5 );
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.NONE;
        subPanel.add( minFileSizeUnitComboBox, constraints );
        
        label = new JLabel( Localizer.getString( "MaxFileSize" ) );
        label.setToolTipText( Localizer.getString( "TTTMaxFileSize" ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 6;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.insets = new Insets( 0, 5, 0, 5 );
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.NONE;
        filterBoxPanel.addContent( label, constraints );
        
        subPanel = new JPanel( new GridBagLayout() );
        subPanel.setOpaque( false );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 7;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.insets = new Insets( 0, 5, 5, 5 );
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
        filterBoxPanel.addContent( subPanel, constraints );
        
        maxFileSizeTF = new IntegerTextField( 9 );
        maxFileSizeTF.setToolTipText( Localizer.getString( "TTTMaxFileSize" ) );
        keymap = JTextField.getKeymap( "SearchFilterTextEditor" );
        maxFileSizeTF.setKeymap( keymap );
        GUIUtils.assignKeymapToTextField( keymap, maxFileSizeTF );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.insets = new Insets( 0, 0, 0, 3 );
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.NONE;
        subPanel.add( maxFileSizeTF, constraints );
        
        maxFileSizeUnitComboBox = new JComboBox( sizeDefinitions );
        maxFileSizeUnitComboBox.setToolTipText( Localizer.getString( "TTTMaxFileSize" ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.insets = new Insets( 0, 0, 0, 5 );
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.NONE;
        subPanel.add( maxFileSizeUnitComboBox, constraints );
        
        filterButton = new JButton( Localizer.getString( "FilterResults" ),
            GUIRegistry.getInstance().getIconFactory().getIcon("Filter" ) );
        filterButton.setToolTipText( Localizer.getString( "TTTFilterResults") );
        filterButton.setCursor( Cursor.getPredefinedCursor( Cursor.HAND_CURSOR ) );
        filterButton.setMargin( btnInsets );
        filterButton.addActionListener( btnActionHandler );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 8;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.insets = new Insets( 0, 5, 12, 5 );
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.NONE;
        filterBoxPanel.addContent( filterButton, constraints );
        
        LinkLabel removeFilterLink = new LinkLabel(
            searchTab.getTabAction( SearchTab.REMOVE_FILTER_ACTION ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 9;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets( 0, 5, 5, 5 );
        filterBoxPanel.addContent( removeFilterLink, constraints );
    }
    
    public void setDisplayedSearch( SearchResultsDataModel searchResultsDataModel )
    {
        // otherwise no need to update...
        if ( displayedDataModel != searchResultsDataModel )
        {
            if ( displayedDataModel != null )
            {
                displayedDataModel.getSearch().removeSearchChangeListener( this );
            }
            displayedDataModel = searchResultsDataModel;
            if ( displayedDataModel != null )
            {
                displayedDataModel.getSearch().addSearchChangeListener( this );
            }
            updateControlPanel();
        }
    }

    private void updateControlPanel()
    {
        if ( displayedDataModel != null )
        {
            searchBoxPanel.setHeaderText( Localizer.getString( "EditSearch" ) );
            Search search = displayedDataModel.getSearch();
            String searchString = search.getSearchString();
            searchTermComboBox.setSelectedItem( searchString );
            ((JTextField)searchTermComboBox.getEditor().getEditorComponent()).setText(
                searchString );
            
            if ( search.isSearching() )
            {
                searchButton.setText( Localizer.getString( "StopSearch" ) );
                searchButton.setToolTipText( Localizer.getString( "TTTStopSearch" ) );
                searchTermComboBox.setEnabled( false );
            }
            else
            {
                searchButton.setText( Localizer.getString( "StartSearch" ) );
                searchButton.setToolTipText( Localizer.getString( "TTTStartSearch" ) );
                if ( search instanceof BrowseHostResults )
                {// for browsing we have special rules and dont allow search
                 // term change...
                    searchTermComboBox.setEnabled( false );
                }
                else
                {
                    searchTermComboBox.setEnabled( true );
                }
            }
           
            SearchFilter filter = displayedDataModel.getSearchFilter();
            if ( filter != null )
            {
                filterTextTF.setText( filter.getFilterString() );
                mediaTypeComboBox.setSelectedItem( filter.getMediaType() );
                // calculations to find the right unit settings for the file size...
                long minFileSize = filter.getMinFileSize();
                if ( minFileSize > 0 )
                {
                    // initialize to bytes
                    minFileSizeUnitComboBox.setSelectedIndex( 0 );
                    SizeDefinition currentDef;
                    long mod;
                    for ( int i = sizeDefinitions.length - 1; i >= 0; i-- )
                    {
                        currentDef = sizeDefinitions[ i ];
                        mod = minFileSize % currentDef.getMultiplier();
                        if ( mod == 0 )
                        {
                            minFileSizeUnitComboBox.setSelectedIndex( i );
                            // found a unit recalculate the min file size
                            minFileSize = minFileSize / currentDef.getMultiplier();
                            break;
                        }
                    }
                    minFileSizeTF.setText( String.valueOf( minFileSize ) );
                }
                
                // calculations to find the right unit settings for the file size...
                long maxFileSize = filter.getMaxFileSize();
                if ( maxFileSize > 0 )
                {
                    // initialize to bytes
                    maxFileSizeUnitComboBox.setSelectedIndex( 0 );
                    long mod;
                    SizeDefinition currentDef;
                    for ( int i = sizeDefinitions.length - 1; i >= 0; i-- )
                    {
                        currentDef = sizeDefinitions[ i ];
                        mod = maxFileSize % currentDef.getMultiplier();
                        if ( mod == 0 )
                        {
                            maxFileSizeUnitComboBox.setSelectedIndex( i );
                            // found a unit recalculate the min file size
                            maxFileSize = maxFileSize / currentDef.getMultiplier();
                            break;
                        }
                    }
                    maxFileSizeTF.setText( String.valueOf( maxFileSize ) );
                }
            }
            else
            {
                filterTextTF.setText( "" );
                mediaTypeComboBox.setSelectedIndex( 0 );
                minFileSizeTF.setText( "" );
                maxFileSizeTF.setText( "" );
            }
        }
        else
        {// this is the case for a new search.
            searchBoxPanel.setHeaderText( Localizer.getString( "Search" ) );
            searchTermComboBox.setSelectedItem( null );
            ((JTextField)searchTermComboBox.getEditor().getEditorComponent()).setText( "" );
            searchButton.setText( Localizer.getString( "StartSearch" ) );
            searchButton.setToolTipText( Localizer.getString( "TTTStartSearch" ) );
            searchTermComboBox.setEnabled( true );
            
            filterTextTF.setText( "" );
            mediaTypeComboBox.setSelectedIndex( 0 );
            minFileSizeTF.setText( "" );
            maxFileSizeTF.setText( "" );
        }
    }
    
    /*public Dimension getMinimumSize()
    {
        Dimension pref = super.getPreferredSize();
        //pref.width += 20;
        return pref;
    }*/
    
    /**
     * This is overloaded to update the combo box size on
     * every UI update. Like font size change!
     */
    public void updateUI()
    {
        super.updateUI();
        
        Color shadow = UIManager.getColor( "controlDkShadow" );
        Color window = UIManager.getColor( "window" );
        setBorder( BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder( 0, 0, 1, 1, window ),
            BorderFactory.createMatteBorder( 1, 1, 1, 1, shadow ) ) );
        setBackground( window );

        GUIUtils.adjustComboBoxHeight( searchTermComboBox );
        GUIUtils.adjustComboBoxHeight( mediaTypeComboBox );
        GUIUtils.adjustComboBoxHeight( minFileSizeUnitComboBox );
        GUIUtils.adjustComboBoxHeight( maxFileSizeUnitComboBox );
        if ( searchTermComboBox != null )
        {// adjust combobox width
            ListCellRenderer renderer = searchTermComboBox.getRenderer();
            if ( renderer != null )
            {
                FontMetrics fm = searchTermComboBox.getFontMetrics( searchTermComboBox.getFont() );
                int maxWidth = fm.getMaxAdvance() * 8;
                int minWidth = fm.getMaxAdvance() * 6;
                Dimension dim = searchTermComboBox.getMaximumSize();
                dim.width = Math.max( minWidth, Math.min( maxWidth, dim.width ) );
                searchTermComboBox.setMaximumSize( dim );
                dim = searchTermComboBox.getPreferredSize();
                dim.width = Math.max( minWidth, Math.min( maxWidth, dim.width ) );
                searchTermComboBox.setPreferredSize( dim );
            }
        }
    }
    
    /////////////////////////////// Start SearchChangeListener /////////////////////////////
    
    /**
     * Notifys us about a change to the currently displayed search.
     * 
     * @see phex.event.SearchChangeListener#searchChanged(phex.event.SearchChangeEvent)
     */
    public void searchChanged( SearchChangeEvent e )
    {
        short type = e.getType();
        switch (type)
        {
            case SearchChangeEvent.SEARCH_STARTED:
            case SearchChangeEvent.SEARCH_STOPED:
            case SearchChangeEvent.SEARCH_CHANGED:
                updateControlPanel();            
        }
    }
    
    /////////////////////////////// End SearchChangeListener /////////////////////////////
    
    /////////////////////////////// Start Inner classes /////////////////////////////
    
    /**
     * Submits a new search.
     */
    private class SubmitSearchHandler extends AbstractAction implements ActionListener
    {
        public void actionPerformed( ActionEvent e )
        {
            if ( displayedDataModel != null && displayedDataModel.getSearch().isSearching() )
            {
                displayedDataModel.getSearch().stopSearching();
                return;
            }
            
            String searchStr = (String)searchTermComboBox.getEditor().getItem();
            searchComboModel.setSelectedItem( searchStr );
            searchStr = searchStr.trim();
            if ( searchStr.length() == 0 )
            {
                return;
            }

            if ( searchStr.length() < Cfg.MIN_SEARCH_TERM_LENGTH )
            {
                Object[] objArr = new Object[ 1 ];
                objArr[ 0 ] = new Integer( Cfg.MIN_SEARCH_TERM_LENGTH );
                GUIUtils.showErrorMessage( Localizer.getFormatedString(
                        "MinSearchTerm", objArr ) );
                searchTermComboBox.getEditor().selectAll();
                try
                {
                    ((JComponent)searchTermComboBox.getEditor().getEditorComponent()).requestFocus();
                }
                catch ( Exception exp )
                {
                    NLogger.error( NLoggerNames.USER_INTERFACE, 
                        searchTermComboBox.getEditor().toString(), exp );
                }
                
                return;
            }

            // try to find a existing and running search with the same search string
            // and select it if found.
            Search existingSearch = searchContainer.getRunningSearch( searchStr );
            if ( existingSearch != null )
            {
                SearchResultsDataModel searchResultsDataModel = 
                    SearchResultsDataModel.lookupResultDataModel( existingSearch );
                searchTab.setDisplayedSearch( searchResultsDataModel );
                return;
            }
            
            if ( displayedDataModel == null )
            {
                Search newSearch = searchContainer.createSearch( searchStr );
                SearchResultsDataModel searchResultsDataModel = 
                    SearchResultsDataModel.registerNewSearch( newSearch );
                searchTab.setDisplayedSearch( searchResultsDataModel );
            }
            else
            {
                displayedDataModel.getSearch().setSearchString( searchStr );
                displayedDataModel.getSearch().startSearching();
            }
            
            int idx = searchComboModel.getIndexOf( searchStr );
            if ( idx < 0 )
            {
                searchComboModel.insertElementAt( searchStr, 0 );
                if ( searchComboModel.getSize() >
                    ServiceManager.sCfg.maxConnectToHistorySize )
                {
                    searchComboModel.removeElementAt(
                        searchComboModel.getSize() - 1 );
                }
                saveSearchList();
            }
            else if ( idx > 0 )
            {
                searchComboModel.removeElementAt( idx );
                searchComboModel.insertElementAt( searchStr, 0 );
                saveSearchList();
            }
        }

        private void saveSearchList()
        {
            int length = searchComboModel.getSize();
            ArrayList searchList = new ArrayList( length );
            for ( int i = 0; i < length; i++ )
            {
                searchList.add( searchComboModel.getElementAt( i ) );
            }
            ServiceManager.sCfg.searchTermHistory.clear();
            ServiceManager.sCfg.searchTermHistory.addAll( searchList );
            ServiceManager.sCfg.save();
        }
    }
    
    private class ButtonActionHandler extends AbstractAction implements ActionListener
    {
        public void actionPerformed( ActionEvent e )
        {
            filterSearchResults();
        }
        
        private void filterSearchResults()
        {
            if ( displayedDataModel == null )
            {
                return;
            }
            
            String searchFilter = filterTextTF.getText().trim();
            String minSizeStr = minFileSizeTF.getText().trim();
            String maxSizeStr = maxFileSizeTF.getText().trim();
            MediaType mediaType = (MediaType) mediaTypeComboBox.getSelectedItem();
            
            long finalMinSize = -1;
            try
            {
                if ( minSizeStr.length() > 0 )
                {
                    long minSize = Integer.parseInt( minSizeStr );
                    SizeDefinition sizeDef = (SizeDefinition)minFileSizeUnitComboBox.getSelectedItem();
                    long minSizeMultiplier = sizeDef.getMultiplier();
                    finalMinSize = minSizeMultiplier * minSize;
                }
            }
            catch ( NumberFormatException exp )
            {
                displayWrongNumberFormatError( minFileSizeTF );
                return;
            }
            
            
            long finalMaxSize = -1;
            try
            {
                if ( maxSizeStr.length() > 0 )
                {
                    long maxSize = Integer.parseInt( maxSizeStr );
                    SizeDefinition sizeDef = (SizeDefinition)maxFileSizeUnitComboBox.getSelectedItem();
                    long maxSizeMultiplier = sizeDef.getMultiplier();
                    finalMaxSize = maxSizeMultiplier * maxSize;
                }
            }
            catch ( NumberFormatException exp )
            {
                displayWrongNumberFormatError( maxFileSizeTF );
                return;
            }
            
            SearchFilter filter = displayedDataModel.getSearchFilter();
            if ( filter == null )
            {
                filter = new SearchFilter();
            }
            filter.updateSearchFilter( searchFilter, mediaType, finalMinSize, finalMaxSize );
            
            displayedDataModel.updateSearchFilter( filter );
            searchTab.getTabAction( SearchTab.REMOVE_FILTER_ACTION ).refreshActionState();
        }
        
        private void displayWrongNumberFormatError(JTextField textField)
        {
            textField.requestFocus();
            textField.selectAll();
            JOptionPane.showMessageDialog( searchTab,
                Localizer.getString( "WrongNumberFormat" ),
                Localizer.getString( "FormatError" ), JOptionPane.ERROR_MESSAGE  );
        }
    }
    
    private class SizeDefinition
    {
        private String representation;
        private long multiplier;

        public SizeDefinition( String aRepresentation, long aMultiplier )
        {
            representation = Localizer.getString( aRepresentation );
            multiplier = aMultiplier;
        }

        public long getMultiplier()
        {
            return multiplier;
        }

        public String toString()
        {
            return representation;
        }
    }
    
    /////////////////////////////// End Inner classes /////////////////////////////
}
