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
 *  $Id: OptionsDialog.java,v 1.25 2005/11/19 14:51:24 gregork Exp $
 */
package phex.gui.dialogs.options;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.HashMap;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;

import phex.common.ServiceManager;
import phex.gui.actions.GUIActionPerformer;
import phex.gui.common.GUIRegistry;
import phex.utils.Localizer;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class OptionsDialog extends JDialog
{
    private JPanel contentPanel;
    /**
     * The panel builder used to update the option view pane.
     */
    private PanelBuilder optionViewBuilder;

    /**
     * Represents the right hand side pane that shows the options corresponding
     * to the selected entry on the tree.
     */
    private JPanel optionViewPane;

    private JTree optionTree;

    /**
     * This array should contain all setting panes that need to be validated in
     * the order of validation.
     */
    private OptionsSettingsPane[] settingsPanes = null;

    /**
     * The dictionary is filled with values while validating and applying the
     * changes done to the OptionsSettingsPanes.
     */
    private HashMap inputDictionary;

    public OptionsDialog()
    {
        super( GUIRegistry.getInstance().getMainFrame(),
            Localizer.getString( "PhexOptions" ), false );
        inputDictionary = new HashMap();
        prepareComponent();
    }

    public void setOptionView( OptionsSettingsPane pane )
    {
        pane.prepareForDisplay();

        optionViewPane.removeAll();
        CellConstraints cc = new CellConstraints();
        optionViewBuilder.add( pane, cc.xy( 1, 1 )  );

        getContentPane().validate();
        pane.doLayout();
        pane.revalidate();

        // sometime the size of a option pane can freak out...
        // here we adjust the size of the dialog to have the pane and tree
        // layed out right...
        Dimension prefSize = getPreferredSize();
        Dimension currSize = getSize();
        if ( prefSize.width  > currSize.width
          || prefSize.height > currSize.height )
        {
            currSize.setSize(
                Math.max( prefSize.width, currSize.width ),
                Math.max( prefSize.height, currSize.height ) );
            setSize( currSize );
            doLayout();
        }
        contentPanel.repaint();
    }

    private void prepareComponent()
    {
        addWindowListener(new WindowAdapter()
            {
                public void windowClosing( WindowEvent evt )
                {
                    closeDialog( );
                }
            }
        );

        Container contentPane = getContentPane();
        contentPane.setLayout( new BorderLayout() );
        contentPanel = new JPanel();
        //JPanel contentPanel = new FormDebugPanel();
        contentPane.add( contentPanel, BorderLayout.CENTER );
        
        FormLayout layout = new FormLayout(
            "3dlu, fill:p, 6dlu, fill:d:grow, 3dlu", // columns
            "6dlu, fill:d:grow, 3dlu, d, 3dlu, d, 6dlu" ); //row
        contentPanel.setLayout( layout );
        
        PanelBuilder builder = new PanelBuilder( layout, contentPanel );
        CellConstraints cc = new CellConstraints();
        
        // hidden panel to update LAF changes accordingly. 
        SpecialLAFPanel pane = new SpecialLAFPanel();
        builder.add( pane, cc.xy(1,1) );

        optionTree = new JTree( createOptionTreeModel() );
        optionTree.setCellRenderer( new OptionsTreeCellRenderer() );
        // no root
        optionTree.setRootVisible( false );
        // this is no nice but fast way to expand the tree
        optionTree.expandRow( 2 );
        optionTree.expandRow( 1 );
        optionTree.expandRow( 0 );
        // single selection mode
        optionTree.getSelectionModel().setSelectionMode(
            TreeSelectionModel.SINGLE_TREE_SELECTION );
        // show lines between leafs
        optionTree.putClientProperty("JTree.lineStyle", "Angled");
        // selection listener
        optionTree.addTreeSelectionListener( new OptionSelectionListener() );

        //JScrollPane scrollPane = new JScrollPane( optionTree );
        builder.add( optionTree, cc.xy( 2, 2 ) );

        optionViewPane = new JPanel();
        //optionViewPane = new FormDebugPanel();
        FormLayout optionViewLayout = new FormLayout(
            "fill:d:grow", "fill:d:grow");
        optionViewBuilder = new PanelBuilder( optionViewLayout, optionViewPane );
        builder.add( optionViewPane, cc.xy( 4, 2 ) );
        
        JSeparator sep = new JSeparator();
        builder.add( sep, cc.xywh( 2, 4, 3, 1 ) );
        
        JButton okBtn = new JButton( Localizer.getString( "OK" ) );
        okBtn.addActionListener( new OkButtonListener() );
        JButton applyBtn = new JButton( Localizer.getString( "Apply" ) );
        applyBtn.addActionListener( new ApplyButtonListener() );
        JButton cancelBtn = new JButton( Localizer.getString( "Cancel" ) );
        cancelBtn.addActionListener( new CancelButtonListener() );
        JPanel btnPanel = ButtonBarFactory.buildOKCancelApplyBar( okBtn,
            cancelBtn, applyBtn );
        builder.add( btnPanel, cc.xywh( 2, 6, 3, 1 ) );
        
        setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );
        
        optionTree.setSelectionRow( 0 );
        pack();
        setLocationRelativeTo( GUIRegistry.getInstance().getMainFrame() );        
    }

    private boolean isAllInputValid()
    {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)optionTree.getModel().getRoot();
        for ( int i = 0; i < settingsPanes.length; i++ )
        {
            // don't verify if not displayed...
            if ( !settingsPanes[i].isSettingsPaneDisplayed() )
            {
                continue;
            }

            settingsPanes[i].checkInput( inputDictionary );
            if ( settingsPanes[i].isInputValid( inputDictionary ) )
            {
                continue;
            }

            // in error case....

            // find tree node with pane...
            // this is not nice but the easiest solution that I found and works
            // enumerate depth first through whole tree till error node found...
            Enumeration enumr = root.depthFirstEnumeration();
            while ( enumr.hasMoreElements() )
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumr.nextElement();
                Object obj = node.getUserObject();
                if ( obj instanceof OptionsSettingsPane )
                {
                    if ( obj == settingsPanes[i] )
                    {
                        optionTree.setSelectionPath( new TreePath( node.getPath() ) );
                        break;
                    }
                }
            }
            setOptionView( settingsPanes[i] );
            settingsPanes[i].displayErrorMessage( inputDictionary );

            return false;
        }
        return true;
    }

    private void saveAndApplyAllChanges()
    {
        for ( int i = 0; i < settingsPanes.length; i++ )
        {
            // don't apply if not displayed...
            if ( !settingsPanes[i].isSettingsPaneDisplayed() )
            {
                continue;
            }

            settingsPanes[i].saveAndApplyChanges( inputDictionary );
        }
        if ( OptionsSettingsPane.isSaveConfigTriggered( inputDictionary ) )
        {
            ServiceManager.sCfg.save();
        }
        if ( OptionsSettingsPane.isSharedFilesRescanTriggered( inputDictionary ) )
        {
            GUIActionPerformer.rescanSharedFiles();
        }
    }

    private void closeDialog( )
    {
        setVisible(false);
        dispose();
    }

    /**
     * + General Settings
     *   + Network
     *   + Proxy
     *   + Bandwidth
     *   + Filters
     * + Download/Upload Settings
     *   + Download
     *   + Upload
     *   + Directories
     * + User Interface
     *   + General
     *   + Language
     *   + Prompts
     * + Debug
     */
    private TreeModel createOptionTreeModel()
    {
        int pos = 0;
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode parent;

        settingsPanes = new OptionsSettingsPane[ 14 ];
        settingsPanes[ pos ] = new MainTextPane();
        parent = new DefaultMutableTreeNode( settingsPanes[ pos ] );
        root.add( parent );
        settingsPanes[ ++pos ] = new NetworkPane();
        parent.add( new DefaultMutableTreeNode( settingsPanes[ pos ] ) );
        settingsPanes[ ++pos ] = new ProxyPane();
        parent.add( new DefaultMutableTreeNode( settingsPanes[ pos ] ) );
        settingsPanes[ ++pos ] = new BandwidthPane();
        parent.add( new DefaultMutableTreeNode( settingsPanes[ pos ] ) );
        settingsPanes[ ++pos ] = new DisconnectPane();
        parent.add( new DefaultMutableTreeNode( settingsPanes[ pos ] ) );
        /*settingsPanes[ ++pos ] = new FiltersPane();
        parent.add( new DefaultMutableTreeNode( settingsPanes[ pos ] ) );*/

        settingsPanes[ ++pos ] = new GeneralTextPane(
            "DownloadSharingSettings", "DownloadSharingSettingsText",
            "DownloadSharingSettings"  );
        parent = new DefaultMutableTreeNode( settingsPanes[ pos ] );
        root.add(parent);
        settingsPanes[ ++pos ] = new DownloadPane();
        parent.add( new DefaultMutableTreeNode( settingsPanes[ pos ] ) );
        settingsPanes[ ++pos ] = new SharingPane();
        parent.add( new DefaultMutableTreeNode( settingsPanes[ pos ] ) );
        settingsPanes[ ++pos ] = new DirectoriesPane();
        parent.add( new DefaultMutableTreeNode( settingsPanes[ pos ] ) );

        settingsPanes[ ++pos ] = new GeneralTextPane(
            "UserInterface", "UserInterfaceText",
            "UserInterface"  );
        parent = new DefaultMutableTreeNode( settingsPanes[ pos ] );
        root.add(parent);
        settingsPanes[ ++pos ] = new GeneralUIPane();
        parent.add(new DefaultMutableTreeNode( settingsPanes[ pos ] ));
        settingsPanes[ ++pos ] = new LanguagePane();
        parent.add(new DefaultMutableTreeNode( settingsPanes[ pos ] ));
        settingsPanes[ ++pos ] = new DisplayPromptsPane();
        parent.add(new DefaultMutableTreeNode( settingsPanes[ pos ] ));
        
        settingsPanes[ ++pos ] = new DebugPane();
        parent = new DefaultMutableTreeNode( settingsPanes[ pos ] );
        root.add( parent );
        return new DefaultTreeModel(root);
    }

    private final class CancelButtonListener implements ActionListener
    {
        public void actionPerformed( ActionEvent e )
        {
            try
            {
                closeDialog();
            }
            catch ( Throwable th )
            {
                NLogger.error(NLoggerNames.USER_INTERFACE, th, th );
            }
        }
    }

    private final class ApplyButtonListener implements ActionListener
    {
        public void actionPerformed( ActionEvent e )
        {
            try
            {
                if ( isAllInputValid() )
                {
                    saveAndApplyAllChanges();
                }
            }
            catch ( Throwable th )
            {
                NLogger.error(NLoggerNames.USER_INTERFACE, th, th );
            }
        }
    }

    private final class OkButtonListener implements ActionListener
    {
        public void actionPerformed( ActionEvent e )
        {
            try
            {
                if ( isAllInputValid() )
                {
                    saveAndApplyAllChanges();
                    closeDialog();
                }
            }
            catch ( Throwable th )
            {
                NLogger.error(NLoggerNames.USER_INTERFACE, th, th );
            }
        }
    }

    class OptionSelectionListener implements TreeSelectionListener
    {
        public void valueChanged( TreeSelectionEvent e )
        {
            try
            {
                TreePath path = e.getPath();
                DefaultMutableTreeNode selectionNode = (DefaultMutableTreeNode)path.getLastPathComponent();
                Object obj = selectionNode.getUserObject();
                if ( obj instanceof OptionsSettingsPane )
                {
                    setOptionView( (OptionsSettingsPane) obj );
                }
            }
            catch ( Throwable th )
            {
                NLogger.error(NLoggerNames.USER_INTERFACE, th, th );
            }
        }
    }

    /**
     * This class is used to have a more powerfull updateUI method to update
     * none displayed component like the settings panes.
     */
    class SpecialLAFPanel extends JPanel
    {
        private boolean inited = true;

        public SpecialLAFPanel()
        {
            super();
            inited = true;
        }

        public void updateUI()
        {
            if ( inited && settingsPanes != null )
            {
                // go through settings panes and update ui
                for ( int i = 0; i < settingsPanes.length; i++ )
                {
                    if ( settingsPanes[i] != null )
                    {
                        // don't do anything if not displayed...
                        if ( !settingsPanes[i].isSettingsPaneDisplayed() )
                        {
                            continue;
                        }
                        SwingUtilities.updateComponentTreeUI( settingsPanes[i] );
                    }
                }
            }
            super.updateUI();
        }
    }
}