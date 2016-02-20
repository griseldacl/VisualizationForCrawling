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

import java.awt.Component;
import java.awt.Point;
import java.awt.event.*;
import java.util.Enumeration;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.xml.bind.JAXBException;

import phex.gui.actions.FWAction;
import phex.gui.common.*;
import phex.gui.common.table.FWSortedTableModel;
import phex.gui.common.table.FWTable;
import phex.gui.common.table.FWTableColumnModel;
import phex.gui.dialogs.SecurityRuleConfigDialog;
import phex.gui.models.SecurityTableModel;
import phex.gui.renderer.SecurityRuleRowRenderer;
import phex.security.IPAccessRule;
import phex.security.PhexSecurityManager;
import phex.security.SecurityRule;
import phex.utils.Localizer;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;
import phex.xml.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


public class SecurityTab extends FWTab
{
    private static final String SECURITY_TABLE_IDENTIFIER = "SecurityTable";
    private static final SecurityRule[] EMPTY_SECURITYRULE_ARRAY =
        new SecurityRule[0];

    private JPopupMenu securityPopup;
    private SecurityTableModel securityModel;
    private FWTableColumnModel securityColumnModel;
    private FWTable securityTable;
    private JScrollPane securityTableScrollPane;
    private PhexSecurityManager securityMgr;

    public SecurityTab()
    {
        super( MainFrame.SECURITY_TAB_ID, Localizer.getString( "Security" ),
            GUIRegistry.getInstance().getIconFactory().getIcon( "Security" ),
            Localizer.getString( "TTTSecurity" ), Localizer.getChar(
            "SecurityMnemonic"), KeyStroke.getKeyStroke( Localizer.getString(
            "SecurityAccelerator" ) ), MainFrame.SECURITY_TAB_INDEX);
        securityMgr = PhexSecurityManager.getInstance();
    }

    public void initComponent( XJBGUISettings guiSettings )
    {
        CellConstraints cc = new CellConstraints();
        FormLayout tabLayout = new FormLayout("2dlu, fill:d:grow, 2dlu", // columns
            "2dlu, fill:p:grow, 2dlu"); //rows
        PanelBuilder tabBuilder = new PanelBuilder(tabLayout, this);
        JPanel contentPanel = new JPanel();
        FWElegantPanel banner = new FWElegantPanel( Localizer.getString("Security"),
            contentPanel );
        tabBuilder.add(banner, cc.xy(2, 2));
        
        FormLayout contentLayout = new FormLayout(
            "fill:d:grow", // columns
            "fill:d:grow, 1dlu, p"); //rows
        PanelBuilder contentBuilder = new PanelBuilder(contentLayout, contentPanel);
        
        MouseHandler mouseHandler = new MouseHandler();
            
        securityModel = new SecurityTableModel();
        XJBGUITable xjbTable = GUIUtils.getXJBGUITableByIdentifier( guiSettings,
            SECURITY_TABLE_IDENTIFIER );
        buildSecurityTableColumnModel( xjbTable );
        
        securityTable = new FWTable( new FWSortedTableModel( securityModel ),
            securityColumnModel );
        SecurityRuleRowRenderer securityRowRenderer = new SecurityRuleRowRenderer();
        Enumeration enumr = securityColumnModel.getColumns();
        while ( enumr.hasMoreElements() )
        {
            TableColumn column = (TableColumn)enumr.nextElement();
            column.setCellRenderer( securityRowRenderer );
        }
        securityTable.getSelectionModel().addListSelectionListener(
            new SelectionHandler() );
        securityTable.activateAllHeaderActions();
        securityTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        securityTable.addMouseListener( mouseHandler );

        securityTableScrollPane = FWTable.createFWTableScrollPane( securityTable );
        securityTableScrollPane.addMouseListener( mouseHandler );
        
        contentBuilder.add( securityTableScrollPane, cc.xy( 1, 1 ) );

        FWToolBar securityToolbar = new FWToolBar( JToolBar.HORIZONTAL );
        securityToolbar.setBorderPainted( false );
        securityToolbar.setFloatable( false );
        contentBuilder.add( securityToolbar, cc.xy( 1, 3 ) );

        securityPopup = new JPopupMenu();

        // TODO add actions to toolbar and popup
        FWAction action = new NewSecurityRuleAction();
        addTabAction( action );
        securityToolbar.addAction( action );
        securityPopup.add( action );

        action = new EditSecurityRuleAction();
        addTabAction( EDIT_SECURITY_RULE_ACTION_KEY, action );
        securityToolbar.addAction( action );
        securityPopup.add( action );

        action = new RemoveSecurityRuleAction();
        addTabAction( action );
        securityToolbar.addAction( action );
        securityPopup.add( action );
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
        if ( securityTableScrollPane != null )
        {
            FWTable.updateFWTableScrollPane( securityTableScrollPane );
        }
    }

    private SecurityRule[] getSelectedSecurityRules()
    {
        if ( securityTable.getSelectedRowCount() == 0 )
        {
            return EMPTY_SECURITYRULE_ARRAY;
        }
        int[] viewIndices = securityTable.getSelectedRows();
        int[] modelIndices = securityTable.convertRowIndicesToModel( viewIndices );
        SecurityRule[] files = securityMgr.getIPAccessRulesAt( modelIndices );
        return files;
    }


    //////////////////////////////////////////////////////////////////////////
    /// XML serializing and deserializing
    //////////////////////////////////////////////////////////////////////////

    private void buildSecurityTableColumnModel( XJBGUITable tableSettings )
    {
        int[] columnIds = SecurityTableModel.getColumnIdArray();
        XJBGUITableColumnList columnList = null;
        if ( tableSettings != null )
        {
            columnList = tableSettings.getTableColumnList();
        }

        securityColumnModel = new FWTableColumnModel( securityModel, columnIds,
            columnList );
    }

    public void appendXJBGUISettings( XJBGUISettings xjbSettings )
        throws JAXBException
    {
        super.appendXJBGUISettings( xjbSettings );
        XJBGUITableColumnList xjbList = securityColumnModel.createXJBGUITableColumnList();
        ObjectFactory objFactory = new ObjectFactory();
        XJBGUITable xjbTable = objFactory.createXJBGUITable();
        xjbTable.setTableColumnList( xjbList );
        xjbTable.setTableIdentifier( SECURITY_TABLE_IDENTIFIER );
        xjbSettings.getTableList().getTableList().add( xjbTable );
    }

    //////////////////////////////////////////////////////////////////////////
    /// Table Listeners
    //////////////////////////////////////////////////////////////////////////

    private class SelectionHandler implements ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent e)
        {
            try
            {
                refreshTabActions();
            }
            catch ( Throwable th )
            {
                NLogger.error(NLoggerNames.USER_INTERFACE, th, th);
            }
            
        }
    }

    /**
     * Handles Mouse events to display popup menues.
     */
    private class MouseHandler extends MouseAdapter implements MouseListener
    {
        public void mouseClicked(MouseEvent e)
        {
            try
            {
                if (e.getClickCount() == 2)
                {
                    if (e.getSource() == securityTable)
                    {
                        getTabAction(EDIT_SECURITY_RULE_ACTION_KEY)
                            .actionPerformed(null);
                    }
                }
            }
            catch (Throwable th)
            {
                NLogger.error(NLoggerNames.USER_INTERFACE, th, th);
            }
        }

        public void mouseReleased(MouseEvent e)
        {
            try
            {
                if (e.isPopupTrigger())
                {
                    popupMenu((Component) e.getSource(), e.getX(), e.getY());
                }
            }
            catch (Throwable th)
            {
                NLogger.error(NLoggerNames.USER_INTERFACE, th, th);
            }
        }

        public void mousePressed(MouseEvent e)
        {
            try
            {
                if (SwingUtilities.isRightMouseButton(e))
                {
                    Component source = (Component) e.getSource();
                    if (source == securityTable)
                    {
                        Point p = e.getPoint();
                        int row = securityTable.rowAtPoint(p);
                        int column = securityTable.columnAtPoint(p);
                        securityTable
                            .changeSelection(row, column, false, false);
                    }
                }
            }
            catch (Throwable th)
            {
                NLogger.error(NLoggerNames.USER_INTERFACE, th, th);
            }
        }

        private void popupMenu(Component source, int x, int y)
        {
            if (source == securityTable || source == securityTableScrollPane)
            {
                securityPopup.show(source, x, y);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    //// Start Actions
    ////////////////////////////////////////////////////////////////////////////

    private static final String EDIT_SECURITY_RULE_ACTION_KEY = "EditSecurityRuleAction";

    public class NewSecurityRuleAction extends FWAction
    {
        public NewSecurityRuleAction()
        {
            super(Localizer.getString("NewSecurityRule"), GUIRegistry
                .getInstance().getIconFactory().getIcon("New"), Localizer
                .getString("TTTNewSecurityRule"));
            setEnabled(true);
        }

        public void actionPerformed(ActionEvent e)
        {
            try
            {
                SecurityRuleConfigDialog dialog = new SecurityRuleConfigDialog();
                dialog.show();
            }
            catch (Throwable th)
            {
                NLogger.error(NLoggerNames.USER_INTERFACE, th, th);
            }
        }

        public void refreshActionState()
        {
        }
    }

    public class EditSecurityRuleAction extends FWAction
    {
        public EditSecurityRuleAction()
        {
            super(Localizer.getString("EditSecurityRule"), GUIRegistry
                .getInstance().getIconFactory().getIcon("Edit"), Localizer
                .getString("TTTEditSecurityRule"));
            refreshActionState();
        }

        public void actionPerformed(ActionEvent e)
        {
            try
            {
                if (securityTable.getSelectedRowCount() != 1)
                {
                    return;
                }
                int viewIdx = securityTable.getSelectedRow();
                int modelIdx = securityTable.convertRowIndexToModel(viewIdx);
                IPAccessRule rule = securityMgr.getIPAccessRule(modelIdx);
                if (rule == null || rule.isSystemRule())
                {
                    return;
                }
                SecurityRuleConfigDialog dialog = new SecurityRuleConfigDialog(
                    rule);
                dialog.show();
            }
            catch (Throwable th)
            {
                NLogger.error(NLoggerNames.USER_INTERFACE, th, th);
            }
        }

        public void refreshActionState()
        {
            if (securityTable.getSelectedRowCount() == 1)
            {
                int viewIdx = securityTable.getSelectedRow();
                int modelIdx = securityTable.convertRowIndexToModel(viewIdx);
                IPAccessRule rule = securityMgr.getIPAccessRule(modelIdx);
                if (rule == null || rule.isSystemRule())
                {
                    setEnabled(false);
                }
                else
                {
                    setEnabled(true);
                }
            }
            else
            {
                setEnabled(false);
            }
        }
    }

    public class RemoveSecurityRuleAction extends FWAction
    {
        public RemoveSecurityRuleAction()
        {
            super(Localizer.getString("RemoveSecurityRule"), GUIRegistry
                .getInstance().getIconFactory().getIcon("Remove"), Localizer
                .getString("TTTRemoveSecurityRule"));
            refreshActionState();
        }

        public void actionPerformed(ActionEvent e)
        {
            try
            {
                if (securityTable.getSelectedRow() < 0)
                {
                    setEnabled(false);
                    return;
                }
                SecurityRule[] securityRules = getSelectedSecurityRules();
                for (int i = 0; i < securityRules.length; i++)
                {
                    if (securityRules[i] != null
                        && !securityRules[i].isSystemRule())
                    {
                        PhexSecurityManager.getInstance().removeSecurityRule(
                            securityRules[i]);
                    }
                }
            }
            catch (Throwable th)
            {
                NLogger.error(NLoggerNames.USER_INTERFACE, th, th);
            }
        }

        public void refreshActionState()
        {
            int row = securityTable.getSelectedRow();
            if (row < 0)
            {
                setEnabled(false);
                return;
            }

            SecurityRule[] securityRules = getSelectedSecurityRules();
            for (int i = 0; i < securityRules.length; i++)
            {
                if (securityRules[i] != null
                    && !securityRules[i].isSystemRule())
                {
                    setEnabled(true);
                    return;
                }
            }
            setEnabled(false);
        }
    }

}