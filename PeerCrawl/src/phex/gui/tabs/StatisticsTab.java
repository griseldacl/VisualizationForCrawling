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
package phex.gui.tabs;

import javax.swing.*;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.xml.bind.JAXBException;

import phex.gui.common.*;
import phex.gui.common.GUIRegistry;
import phex.gui.common.GUIUtils;
import phex.gui.common.MainFrame;
import phex.gui.common.table.FWSortedTableModel;
import phex.gui.common.table.FWTable;
import phex.gui.common.table.FWTableColumnModel;
import phex.gui.models.StatisticsTableModel;
import phex.utils.Localizer;
import phex.xml.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class StatisticsTab extends FWTab
{
    private static final String STATISTICS_TABLE_IDENTIFIER = "StatisticsTable";

    private StatisticsTableModel statisticsModel;
    private FWTableColumnModel statisticsColumnModel;
    private FWTable statisticsTable;
    private JScrollPane statisticsTableScrollPane;

    public StatisticsTab()
    {
        super( MainFrame.STATISTICS_TAB_ID, Localizer.getString( "Statistics" ),
            GUIRegistry.getInstance().getIconFactory().getIcon( "Statistics" ),
            Localizer.getString( "TTTStatistics" ), Localizer.getChar(
            "StatisticsMnemonic"), KeyStroke.getKeyStroke( Localizer.getString(
            "StatisticsAccelerator" ) ), MainFrame.STATISTICS_TAB_INDEX);
    }

    public void initComponent( XJBGUISettings guiSettings )
    {
        CellConstraints cc = new CellConstraints();
        FormLayout tabLayout = new FormLayout("2dlu, fill:d:grow, 2dlu", // columns
            "2dlu, fill:p:grow, 2dlu"); //rows
        PanelBuilder tabBuilder = new PanelBuilder(tabLayout, this);
        JPanel contentPanel = new JPanel();
        FWElegantPanel banner = new FWElegantPanel( Localizer.getString("Statistics"),
            contentPanel );
        tabBuilder.add(banner, cc.xy(2, 2));
        
        FormLayout contentLayout = new FormLayout(
            "fill:d:grow", // columns
            "fill:d:grow"); //rows
        PanelBuilder contentBuilder = new PanelBuilder(contentLayout, contentPanel);
        
        statisticsModel = new StatisticsTableModel();
        XJBGUITable xjbTable = GUIUtils.getXJBGUITableByIdentifier( guiSettings,
            STATISTICS_TABLE_IDENTIFIER );
        buildStatisticsTableColumnModel( xjbTable );

        statisticsTable = new FWTable( new FWSortedTableModel( statisticsModel ),
            statisticsColumnModel );
        statisticsTable.activateAllHeaderActions();
        statisticsTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        GUIRegistry.getInstance().getTableUpdateService().registerTable( statisticsTable );

        statisticsTableScrollPane = FWTable.createFWTableScrollPane( statisticsTable );
        contentBuilder.add( statisticsTableScrollPane, cc.xy( 1, 1 ) );
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

    public void updateUI()
    {
        super.updateUI();
        if ( statisticsTableScrollPane != null )
        {
            FWTable.updateFWTableScrollPane( statisticsTableScrollPane );
        }
    }

    //////////////////////////////////////////////////////////////////////////
    /// XML serializing and deserializing
    //////////////////////////////////////////////////////////////////////////

    private void buildStatisticsTableColumnModel( XJBGUITable tableSettings )
    {
        int[] columnIds = StatisticsTableModel.getColumnIdArray();
        XJBGUITableColumnList columnList = null;
        if ( tableSettings != null )
        {
            columnList = tableSettings.getTableColumnList();
        }

        statisticsColumnModel = new FWTableColumnModel( statisticsModel, columnIds,
            columnList );
    }

    public void appendXJBGUISettings( XJBGUISettings xjbSettings )
        throws JAXBException
    {
        super.appendXJBGUISettings( xjbSettings );
        XJBGUITableColumnList xjbList = statisticsColumnModel.createXJBGUITableColumnList();
        ObjectFactory objFactory = new ObjectFactory();
        XJBGUITable xjbTable = objFactory.createXJBGUITable();
        xjbTable.setTableColumnList( xjbList );
        xjbTable.setTableIdentifier( STATISTICS_TABLE_IDENTIFIER );
        xjbSettings.getTableList().getTableList().add( xjbTable );
    }


}