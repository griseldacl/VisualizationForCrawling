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
 *  $Id: DownloadPane.java,v 1.23 2005/10/21 16:55:57 gregork Exp $
 */
package phex.gui.dialogs.options;

import java.util.HashMap;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import phex.common.ServiceManager;
import phex.gui.common.IntegerTextField;
import phex.utils.Localizer;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class DownloadPane extends OptionsSettingsPane
{
    private static final String WORKER_PER_DOWNLOAD_KEY = "WorkerPerDownload";
    private static final String MAX_TOTAL_WORKER_KEY = "TotalWorker";
    private static final String PUSH_TIMEOUT_KEY = "PushTimeout";
    private static final String INITIAL_SEGMENT_SIZE_KEY = "InitialSegmentSize";
    private static final String SEGMENT_TRANSFER_TIME_KEY = "SegmentTransferTime";

    private IntegerTextField totalWorkersTF;
    private IntegerTextField workerPerDownloadTF;
    private IntegerTextField pushTimeoutTF;
    private IntegerTextField initialSegmentSizeTF;
    private IntegerTextField segmentTransferTimeTF;
    private JCheckBox readoutMagmasChkbx;
    private JCheckBox removeCompletedDownloadsChkbx;
    private JCheckBox enableHitSnoopingChkbx;

    public DownloadPane()
    {
        super( "Download" );
    }

    /**
     * Called when preparing this settings pane for display the first time. Can
     * be overriden to implement the look of the settings pane.
     */
    protected void prepareComponent()
    {
        FormLayout layout = new FormLayout(
            "10dlu, right:d, 2dlu, d, 2dlu:grow", // columns
            "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p"); // rows
        layout.setRowGroups( new int[][]{{3, 5, 7, 9, 11, 13, 15, 17}} );
        setLayout( layout );
        
        PanelBuilder builder = new PanelBuilder( layout, this );
        CellConstraints cc = new CellConstraints();
        
        builder.addSeparator( Localizer.getString( "GeneralDownloadSettings" ),
            cc.xywh( 1, 1, 5, 1 ) );
        
        JLabel label = builder.addLabel( Localizer.getString( "DownloadSettings_TotalParallelDownloads" ) 
            + ": ", cc.xy( 2, 3 ) );
        label.setToolTipText( Localizer.getString( "DownloadSettings_TTTTotalParallelDownloads" ) );
        totalWorkersTF = new IntegerTextField(
            String.valueOf( ServiceManager.sCfg.maxTotalDownloadWorker ), 6, 2 );
        totalWorkersTF.setToolTipText( 
            Localizer.getString( "DownloadSettings_TTTTotalParallelDownloads" ) );
        builder.add( totalWorkersTF, cc.xy( 4, 3 ) );
        
        label = builder.addLabel( 
            Localizer.getString( "DownloadSettings_ParallelDownloadsPerFile" ) + ": ",
            cc.xy( 2, 5 ) );
        label.setToolTipText( 
            Localizer.getString( "DownloadSettings_TTTParallelDownloadsPerFile" ) );
        workerPerDownloadTF = new IntegerTextField(
            String.valueOf( ServiceManager.sCfg.maxWorkerPerDownload ), 6, 2 );
        workerPerDownloadTF.setToolTipText( 
            Localizer.getString( "DownloadSettings_TTTParallelDownloadsPerFile" ) );
        builder.add( workerPerDownloadTF, cc.xy( 4, 5 ) );
        
        label = builder.addLabel( 
            Localizer.getString( "DownloadSettings_InitialSegmentSizeKb" ) + ": ",
            cc.xy( 2, 7 ) );
        label.setToolTipText( 
            Localizer.getString( "DownloadSettings_TTTInitialSegmentSizeKb" ) );
        initialSegmentSizeTF = new IntegerTextField(
            String.valueOf( ServiceManager.sCfg.initialSegmentSize / 1024), 6, 4 );
        initialSegmentSizeTF.setToolTipText( 
            Localizer.getString( "DownloadSettings_TTTInitialSegmentSizeKb" ) );
        builder.add( initialSegmentSizeTF, cc.xy( 4, 7 ) );

        label = builder.addLabel( Localizer.getString( "DownloadSettings_SegmentTransferTimeSec" ) + ": ",
            cc.xy( 2, 9 ) );
        label.setToolTipText( 
            Localizer.getString( "DownloadSettings_TTTSegmentTransferTimeSec" ) );
        segmentTransferTimeTF = new IntegerTextField(
            String.valueOf( ServiceManager.sCfg.segmentTransferTime ), 6, 3 );
        segmentTransferTimeTF.setToolTipText( 
            Localizer.getString( "DownloadSettings_TTTSegmentTransferTimeSec" ) );
        builder.add( segmentTransferTimeTF, cc.xy( 4, 9 ) );

        builder.addLabel( Localizer.getString( "PushTimeout" ) + ": ",
            cc.xy( 2, 11 ) );
        pushTimeoutTF = new IntegerTextField(
            String.valueOf( ServiceManager.sCfg.mPushTransferTimeout / 1000 ), 6, 3 );
        builder.add( pushTimeoutTF, cc.xy( 4, 11 ) );
        
        readoutMagmasChkbx = new JCheckBox(
            Localizer.getString( "DownloadSettings_ReadoutDownloadedMagmas" ),
            ServiceManager.sCfg.autoReadoutDownloadedMagma );
        readoutMagmasChkbx.setToolTipText( 
            Localizer.getString("DownloadSettings_TTTReadoutDownloadedMagmas") );
        builder.add( readoutMagmasChkbx, cc.xywh( 2, 13, 4, 1 ) );
        
        removeCompletedDownloadsChkbx = new JCheckBox(
            Localizer.getString( "DownloadSettings_AutoRemoveCompletedDownloads" ),
            ServiceManager.sCfg.mDownloadAutoRemoveCompleted );
        removeCompletedDownloadsChkbx.setToolTipText( 
            Localizer.getString("DownloadSettings_TTTAutoRemoveCompletedDownloads") );
        builder.add( removeCompletedDownloadsChkbx, cc.xywh( 2, 15, 4, 1 ) );

        enableHitSnoopingChkbx = new JCheckBox(
            Localizer.getString( "DownloadSettings_EnableHitSnooping" ),
            ServiceManager.sCfg.enableHitSnooping );
        enableHitSnoopingChkbx.setToolTipText( 
            Localizer.getString("DownloadSettings_TTTEnableHitSnooping") );
        builder.add( enableHitSnoopingChkbx, cc.xywh( 2, 17, 4, 1 ) );
            
        initConfigValues();
        refreshEnableState();
    }

    /**
     * Override this method if you like to verify inputs before storing them.
     * A input dictionary is given to the pane. It can be used to store values
     * like error flags or prepared values for saving. The dictionary is given
     * to every settings pane checkInput(), displayErrorMessage() and
     * saveAndApplyChanges() method.
     * When the input has been flaged as invalid with the method setInputValid()
     * the method displayErrorMessage() is called directly after return of
     * checkInput() and the focus is given to settings pane.
     * After checking all settings pane without any error the method
     * saveAndApplyChanges() is called for all settings panes to save the
     * changes.
     */
    public void checkInput( HashMap inputDic )
    {
        try
        {
            String workerPerDownloadStr = workerPerDownloadTF.getText();
            Short workerPerDownload = new Short( workerPerDownloadStr );
            inputDic.put( WORKER_PER_DOWNLOAD_KEY, workerPerDownload );
        }
        catch ( NumberFormatException exp )
        {
            inputDic.put( NUMBER_FORMAT_ERROR_KEY, workerPerDownloadTF );
            setInputValid( inputDic, false );
            return;
        }

        try
        {
            String totalWorkersStr = totalWorkersTF.getText();
            Short totalWorkers = new Short( totalWorkersStr );
            inputDic.put( MAX_TOTAL_WORKER_KEY, totalWorkers );
        }
        catch ( NumberFormatException exp )
        {
            inputDic.put( NUMBER_FORMAT_ERROR_KEY, totalWorkersTF );
            setInputValid( inputDic, false );
            return;
        }

        try
        {
            String initialSegmentSizeStr = initialSegmentSizeTF.getText();
            Short initialSegmentSize = new Short( initialSegmentSizeStr );
            inputDic.put( INITIAL_SEGMENT_SIZE_KEY, initialSegmentSize );
        }
        catch ( NumberFormatException exp )
        {
            inputDic.put( NUMBER_FORMAT_ERROR_KEY, initialSegmentSizeTF );
            setInputValid( inputDic, false );
            return;
        }

        try
        {
            String segmentTransferTimeStr = segmentTransferTimeTF.getText();
            Short segmentTransferTime = new Short( segmentTransferTimeStr );
            inputDic.put( SEGMENT_TRANSFER_TIME_KEY, segmentTransferTime );
        }
        catch ( NumberFormatException exp )
        {
            inputDic.put( NUMBER_FORMAT_ERROR_KEY, segmentTransferTimeTF );
            setInputValid( inputDic, false );
            return;
        }

        try
        {
            String pushTimeoutStr = pushTimeoutTF.getText();
            Integer pushTimeout = new Integer( pushTimeoutStr );
            inputDic.put( PUSH_TIMEOUT_KEY, pushTimeout );
        }
        catch ( NumberFormatException exp )
        {
            inputDic.put( NUMBER_FORMAT_ERROR_KEY, pushTimeoutTF );
            setInputValid( inputDic, false );
            return;
        }

        setInputValid( inputDic, true );
    }

    /**
     * When isInputValid() returns a false this method is called.
     * The input dictionary should contain the settings pane specific information
     * of the error.
     * The settings pane should override this method to display a error
     * message. Before calling the method the focus is given to the
     * settings pane.
     */
    public void displayErrorMessage( HashMap inputDic )
    {
        if ( inputDic.containsKey( NUMBER_FORMAT_ERROR_KEY ) )
        {
            displayNumberFormatError( inputDic );
        }
    }

    /**
     * Override this method if you like to apply and save changes made on
     * settings pane. To trigger saving of the configuration if any value was
     * changed call triggerConfigSave().
     */
    public void saveAndApplyChanges( HashMap inputDic )
    {
        Short totalWorkerInt = (Short) inputDic.get(
            MAX_TOTAL_WORKER_KEY );
        short totalWorker = totalWorkerInt.shortValue();
        if ( ServiceManager.sCfg.maxTotalDownloadWorker != totalWorker )
        {
            ServiceManager.sCfg.maxTotalDownloadWorker = totalWorker;
            OptionsSettingsPane.triggerConfigSave( inputDic );
        }

        Short initialSegmentSizeInt = (Short) inputDic.get(
            INITIAL_SEGMENT_SIZE_KEY );
        short initialSegmentSize = initialSegmentSizeInt.shortValue();
        if ( ServiceManager.sCfg.initialSegmentSize != initialSegmentSize * 1024 )
        {
            ServiceManager.sCfg.initialSegmentSize = initialSegmentSize * 1024;
            OptionsSettingsPane.triggerConfigSave( inputDic );
        }

        Short segmentTransferTimeInt = (Short) inputDic.get(
            SEGMENT_TRANSFER_TIME_KEY );
        short segmentTransferTime = segmentTransferTimeInt.shortValue();
        if ( ServiceManager.sCfg.segmentTransferTime != segmentTransferTime )
        {
            ServiceManager.sCfg.segmentTransferTime = segmentTransferTime;
            OptionsSettingsPane.triggerConfigSave( inputDic );
        }

        Short maxWorkerPerDownloadInt = (Short) inputDic.get(
            WORKER_PER_DOWNLOAD_KEY );
        short maxWorkerPerDownload = maxWorkerPerDownloadInt.shortValue();
        if ( ServiceManager.sCfg.maxWorkerPerDownload != maxWorkerPerDownload )
        {
            ServiceManager.sCfg.maxWorkerPerDownload = maxWorkerPerDownload;
            OptionsSettingsPane.triggerConfigSave( inputDic );
        }

        Integer pushTimeoutInt = (Integer) inputDic.get(
            PUSH_TIMEOUT_KEY );
        int pushTimeout = pushTimeoutInt.intValue();
        if ( ServiceManager.sCfg.mPushTransferTimeout != pushTimeout * 1000 )
        {
            ServiceManager.sCfg.mPushTransferTimeout = pushTimeout * 1000;
            OptionsSettingsPane.triggerConfigSave( inputDic );
        }
        
        boolean readoutMagmas = readoutMagmasChkbx.isSelected();
        if ( ServiceManager.sCfg.autoReadoutDownloadedMagma !=
             readoutMagmas )
        {
            ServiceManager.sCfg.mDownloadAutoRemoveCompleted = readoutMagmas;
            OptionsSettingsPane.triggerConfigSave( inputDic );
        }

        boolean removeCompletedDownloads = removeCompletedDownloadsChkbx.isSelected();
        if ( ServiceManager.sCfg.mDownloadAutoRemoveCompleted !=
             removeCompletedDownloads )
        {
            ServiceManager.sCfg.mDownloadAutoRemoveCompleted = removeCompletedDownloads;
            OptionsSettingsPane.triggerConfigSave( inputDic );
        }

        boolean enableHitSnooping = enableHitSnoopingChkbx.isSelected();
        if ( ServiceManager.sCfg.enableHitSnooping !=
             enableHitSnooping )
        {
             ServiceManager.sCfg.enableHitSnooping = enableHitSnooping;
             OptionsSettingsPane.triggerConfigSave( inputDic );
        }
    }

    private void refreshEnableState()
    {
    }

    private void initConfigValues()
    {
    }
}
