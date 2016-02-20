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
 *  Created on 20.09.2005
 *  --- CVS Information ---
 *  $Id: DownloadOverviewPanel.java,v 1.7 2005/10/28 14:32:05 gregork Exp $
 */
package phex.gui.tabs.download;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;

import javax.swing.*;

import org.apache.commons.lang.SystemUtils;

import phex.common.format.NumberFormatUtils;
import phex.download.swarming.SWDownloadFile;
import phex.gui.common.*;
import phex.utils.*;
import phex.utils.Localizer;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;
import phex.xml.XJBGUISettings;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class DownloadOverviewPanel extends JPanel
{
    private DateFormat dateFormat;
    
    private SWDownloadFile lastDownloadFile;
    
    private MultiScopeProgressBar progressBar;
    private JLabel progressLabel;
    private JLabel downloadedLabel;
    private JLabel remainingLabel;
    private JLabel etaLabel;
    private JLabel createdLabel;
    private JLabel totalSizeLabel;
    private JLabel lastDownloadedLabel;
    private JLabel downloadRateLabel;
    private JLabel maxRateLabel;
    private JLabel downloadingCandidatesLabel;
    private JLabel queuedCandidatesLabel;
    private JLabel connectingCandidatesLabel;
    private JLabel goodCandidatesLabel;
    private JLabel badCandidatesLabel;
    private JLabel totalCandidatesLabel;

    private JTextField fileNameTxt;
    private JTextField incompleteFileTxt;

    public DownloadOverviewPanel()
    {
        dateFormat = DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.SHORT );
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
    
    public void initializeComponent(  XJBGUISettings guiSettings )
    {
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
            "6dlu, fill:d:grow, 6dlu", // columns
            "6dlu, p, 6dlu, p, 6dlu, p"); //rows
        PanelBuilder panelBuilder = new PanelBuilder( layout, this );
        
        JPanel progressPanel = buildProgressPanel();
        panelBuilder.add( progressPanel, cc.xy( 2, 2 ) );
        
        JPanel infoPanel = buildInfoPanel();
        panelBuilder.add( infoPanel, cc.xy( 2, 4 ) );
        
        JPanel info2Panel = buildInfo2Panel();
        panelBuilder.add( info2Panel, cc.xy( 2, 6 ) );
    }
    
    private JPanel buildProgressPanel()
    {
        JPanel subPanel = new JPanel();
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
            "d, 2dlu, fill:d:grow, 2dlu, right:25dlu", // columns
            "fill:15dlu"); //rows
        PanelBuilder panelBuilder = new PanelBuilder( layout, subPanel );
        
        JLabel label = new JLabel( Localizer.getString("DownloadOverview_Progress") );
        panelBuilder.add( label, cc.xy( 1, 1 ) );
        
        progressBar = new MultiScopeProgressBar();
        panelBuilder.add( progressBar, cc.xy( 3, 1 ) );
        
        progressLabel = new JLabel( " 100 %");
        panelBuilder.add( progressLabel, cc.xy( 5, 1 ) );
        
        return subPanel;
    }
    
    private JPanel buildInfoPanel()
    {
        JPanel subPanel = new JPanel();
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
            "d, 4dlu, right:d, " +
            "fill:8dlu:grow, d, 4dlu, right:d, " +
            "fill:8dlu:grow, d, 4dlu, right:d", // columns
            
            "p, 2dlu, p, 2dlu, p, 2dlu, p, 6dlu, p, 2dlu, p, 2dlu, p"); //rows
        layout.setColumnGroups( new int[][] { {1,5,9}, {3,7,11} } );
        PanelBuilder panelBuilder = new PanelBuilder( layout, subPanel );
        
        panelBuilder.addSeparator(Localizer.getString("DownloadOverview_Transfer"),
            cc.xywh( 1, 1, layout.getColumnCount(), 1 ) );
        
        JLabel label = new JLabel( Localizer.getString("DownloadOverview_Downloaded") );
        panelBuilder.add( label, cc.xy( 1, 3 ) );
        downloadedLabel = new JLabel( );
        panelBuilder.add( downloadedLabel, cc.xy( 3, 3 ) );
        
        label = new JLabel( Localizer.getString("DownloadOverview_Remaining") );
        panelBuilder.add( label, cc.xy( 5, 3 ) );
        remainingLabel = new JLabel();
        panelBuilder.add( remainingLabel, cc.xy( 7, 3 ) );
        
        label = new JLabel( Localizer.getString("DownloadOverview_DownloadRate") );
        panelBuilder.add( label, cc.xy( 9, 3 ) );
        downloadRateLabel = new JLabel();
        panelBuilder.add( downloadRateLabel, cc.xy( 11, 3 ) );
        
        label = new JLabel( Localizer.getString("DownloadOverview_FileSize") );
        panelBuilder.add( label, cc.xy( 1, 5 ) );
        totalSizeLabel = new JLabel();
        panelBuilder.add( totalSizeLabel, cc.xy( 3, 5 ) );
        
        label = new JLabel( Localizer.getString("DownloadOverview_EstimatedTime") );
        panelBuilder.add( label, cc.xy( 5, 5 ) );
        etaLabel = new JLabel();
        panelBuilder.add( etaLabel, cc.xy( 7, 5 ) );
        
        label = new JLabel( Localizer.getString("DownloadOverview_MaximalRate") );
        panelBuilder.add( label, cc.xy( 9, 5 ) );
        maxRateLabel = new JLabel();
        panelBuilder.add( maxRateLabel, cc.xy( 11, 5 ) );
        
        label = new JLabel( Localizer.getString("DownloadOverview_LastDownloaded") );
        panelBuilder.add( label, cc.xy( 1, 7 ) );
        lastDownloadedLabel = new JLabel();
        panelBuilder.add( lastDownloadedLabel, cc.xy( 3, 7 ) );
        
        label = new JLabel( Localizer.getString("DownloadOverview_Created") );
        panelBuilder.add( label, cc.xy( 9, 7 ) );
        createdLabel = new JLabel();
        panelBuilder.add( createdLabel, cc.xy( 11, 7 ) );
        
        panelBuilder.addSeparator(Localizer.getString("DownloadOverview_Candidates"),
            cc.xywh( 1, 9, 11, 1 ) );
        
        label = new JLabel( Localizer.getString("DownloadOverview_DownloadingCandidates") );
        panelBuilder.add( label, cc.xy( 1, 11 ) );
        downloadingCandidatesLabel = new JLabel();
        panelBuilder.add( downloadingCandidatesLabel, cc.xy( 3, 11 ) );
        
        label = new JLabel( Localizer.getString("DownloadOverview_QueuedCandidates") );
        panelBuilder.add( label, cc.xy( 5, 11 ) );
        queuedCandidatesLabel = new JLabel();
        panelBuilder.add( queuedCandidatesLabel, cc.xy( 7, 11 ) );
        
        label = new JLabel( Localizer.getString("DownloadOverview_ConnectingCandidates") );
        panelBuilder.add( label, cc.xy( 9, 11 ) );
        connectingCandidatesLabel = new JLabel();
        panelBuilder.add( connectingCandidatesLabel, cc.xy( 11, 11 ) );
        
        label = new JLabel( Localizer.getString("DownloadOverview_GoodCandidates") );
        panelBuilder.add( label, cc.xy( 1, 13 ) );
        goodCandidatesLabel = new JLabel();
        panelBuilder.add( goodCandidatesLabel, cc.xy( 3, 13 ) );
        
        label = new JLabel( Localizer.getString("DownloadOverview_BadCandidates") );
        panelBuilder.add( label, cc.xy( 5, 13 ) );
        badCandidatesLabel = new JLabel();
        panelBuilder.add( badCandidatesLabel, cc.xy( 7, 13 ) );
        
        label = new JLabel( Localizer.getString("DownloadOverview_TotalCandidates") );
        panelBuilder.add( label, cc.xy( 9, 13 ) );
        totalCandidatesLabel = new JLabel();
        panelBuilder.add( totalCandidatesLabel, cc.xy( 11, 13 ) );
        
        return subPanel;
    }
    
    private JPanel buildInfo2Panel()
    {
        JPanel subPanel = new JPanel();
        CellConstraints cc = new CellConstraints();
        
        String systemExtraCols = "";
        if ( SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_MAC_OSX )
        {
            systemExtraCols = ", 4dlu, d";
        }
        FormLayout layout = new FormLayout(
            "d, 4dlu, 1dlu:grow" + systemExtraCols, // columns
            "p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p"); //rows
        PanelBuilder panelBuilder = new PanelBuilder( layout, subPanel );
        
        panelBuilder.addSeparator(Localizer.getString("DownloadOverview_Information"),
            cc.xywh( 1, 1, layout.getColumnCount(), 1 ) );
        
        JLabel label = new JLabel( Localizer.getString("DownloadOverview_FileName") );
        panelBuilder.add( label, cc.xy( 1, 3 ) );
        fileNameTxt = new JTextField( );
        fileNameTxt.setEditable(false);
        fileNameTxt.setFont( UIManager.getFont("Label.font") );
        fileNameTxt.setForeground( UIManager.getColor("Label.foreground") );
        fileNameTxt.setBackground( UIManager.getColor("Label.background") );
        fileNameTxt.setMinimumSize(new Dimension(0,0));
        panelBuilder.add( fileNameTxt, cc.xy( 3, 3 ) );
        
        label = new JLabel( Localizer.getString("DownloadOverview_IncompleteFile") );
        panelBuilder.add( label, cc.xy( 1, 5 ) );
        incompleteFileTxt = new JTextField();
        incompleteFileTxt.setEditable(false);
        incompleteFileTxt.setFont( UIManager.getFont("Label.font") );
        incompleteFileTxt.setForeground( UIManager.getColor("Label.foreground") );
        incompleteFileTxt.setBackground( UIManager.getColor("Label.background") );
        panelBuilder.add( incompleteFileTxt, cc.xy( 3, 5 ) );
        
        if ( SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_MAC_OSX )
        {
            JButton exploreFileBtn = new JButton( 
                GUIRegistry.getInstance().getIconFactory().getIcon("Explore") );
            exploreFileBtn.setToolTipText( 
                Localizer.getString( "DownloadOverview_Explore" ) );
            exploreFileBtn.setMargin(GUIUtils.EMPTY_INSETS);
            exploreFileBtn.addActionListener( new ExploreActionListener() );
            panelBuilder.add( exploreFileBtn, cc.xy( 5, 5 ) );
        }
        
        return subPanel;
    }

    public void updateDownloadFileInfo( SWDownloadFile file )
    {
        lastDownloadFile = file;
        updateInterface();
    }
    
    private void updateInterface()
    {
        if ( lastDownloadFile == null )
        {
            progressBar.setDownloadFile( null );
            progressLabel.setText("");
            downloadedLabel.setText("");
            downloadedLabel.setToolTipText("");
            remainingLabel.setText("");
            remainingLabel.setToolTipText("");
            totalSizeLabel.setText("");
            totalSizeLabel.setToolTipText("");
            createdLabel.setText("");
            lastDownloadedLabel.setText("");
            downloadRateLabel.setText("");
            downloadRateLabel.setToolTipText("");
            maxRateLabel.setText("");
            downloadingCandidatesLabel.setText( "" );
            queuedCandidatesLabel.setText( "" );
            connectingCandidatesLabel.setText( "" );
            goodCandidatesLabel.setText( "" );
            badCandidatesLabel.setText( "" );
            totalCandidatesLabel.setText( "" );
            fileNameTxt.setText("");
            incompleteFileTxt.setText("");
            return;
        }
        progressBar.setDownloadFile( lastDownloadFile );
        progressLabel.setText( lastDownloadFile.getProgress().toString() + " %" );
        
        downloadedLabel.setText( NumberFormatUtils.formatSignificantByteSize( 
            lastDownloadFile.getTransferredDataSize() ) );
        downloadedLabel.setToolTipText( NumberFormatUtils.formatFullByteSize( 
            lastDownloadFile.getTransferredDataSize() ) );
        
        long remaining = lastDownloadFile.getTotalDataSize() - lastDownloadFile.getTransferredDataSize();
        remainingLabel.setText( 
            NumberFormatUtils.formatSignificantByteSize( remaining ) );
        remainingLabel.setToolTipText( 
            NumberFormatUtils.formatFullByteSize( remaining ) );
        
        totalSizeLabel.setText( NumberFormatUtils.formatSignificantByteSize( 
            lastDownloadFile.getTotalDataSize() ) );
        totalSizeLabel.setToolTipText( NumberFormatUtils.formatFullByteSize(
            lastDownloadFile.getTotalDataSize() ) );
        
        createdLabel.setText( dateFormat.format( lastDownloadFile.getCreatedDate() ) );
        lastDownloadedLabel.setText( dateFormat.format( lastDownloadFile.getDownloadedDate() ) );
        
        downloadRateLabel.setText( NumberFormatUtils.formatSignificantByteSize( 
            lastDownloadFile.getTransferSpeed() ) + Localizer.getString( "PerSec" ) );
        downloadRateLabel.setToolTipText( NumberFormatUtils.formatFullByteSize( 
            lastDownloadFile.getTransferSpeed() ) + Localizer.getString( "PerSec" ) );
        
        long maxRate = lastDownloadFile.getDownloadThrottlingRate();
        String maxRateStr;
        if ( maxRate >= Integer.MAX_VALUE )
        {
            maxRateStr = Localizer.getDecimalFormatSymbols().getInfinity();
        }
        else
        {
            maxRateStr = NumberFormatUtils.formatSignificantByteSize( maxRate) 
                + Localizer.getString( "PerSec" );
        }
        maxRateLabel.setText( maxRateStr );
        
        downloadingCandidatesLabel.setText( String.valueOf(
            lastDownloadFile.getDownloadingCandidatesCount() ) );
        queuedCandidatesLabel.setText( String.valueOf(
            lastDownloadFile.getQueuedCandidatesCount() ) );
        connectingCandidatesLabel.setText( String.valueOf(
            lastDownloadFile.getConnectingCandidatesCount() ) );
        goodCandidatesLabel.setText( String.valueOf(
            lastDownloadFile.getGoodCandidateCount() ) );
        badCandidatesLabel.setText( String.valueOf(
            lastDownloadFile.getBadCandidateCount() ) );
        totalCandidatesLabel.setText( 
            String.valueOf( lastDownloadFile.getCandidatesCount() ) );
        
        String destFile = lastDownloadFile.getDestinationFileName();
        if ( !fileNameTxt.getText().equals( destFile ) )
        {
            fileNameTxt.setText( destFile );
            fileNameTxt.setCaretPosition(0);
        }
        String path = lastDownloadFile.getIncompleteFile().getAbsolutePath();
        if ( !incompleteFileTxt.getText().equals( path ) )
        {
            incompleteFileTxt.setText( path );
            incompleteFileTxt.setCaretPosition(0);
        }
    }
    
    public class ExploreActionListener implements ActionListener
    {
        public void actionPerformed( ActionEvent e )
        {
            if ( lastDownloadFile == null )
            {
                return;
            }
            final File file = lastDownloadFile.getIncompleteFile();
            if ( file == null )
            {
                return;
            }
            
            File dir = file.getParentFile();
            try
            {
                SystemShellExecute.exploreFolder( dir );
            }
            catch (IOException exp)
            {// ignore and do nothing..
            }
        }
    }
}