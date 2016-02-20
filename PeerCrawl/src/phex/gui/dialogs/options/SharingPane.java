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
 *  $Id: SharingPane.java,v 1.15 2005/11/13 22:20:24 gregork Exp $
 */
package phex.gui.dialogs.options;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import phex.common.ServiceManager;
import phex.gui.common.GUIUtils;
import phex.gui.common.IntegerTextField;
import phex.utils.Localizer;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


public class SharingPane extends OptionsSettingsPane
{
    private static final String SIMULTANEOUS_UPLOADS_KEY = "SimultaneousUploads";
    private static final String UPLOADS_PER_HOST_KEY = "UploadsPerHost";
    private static final String RETURNED_SEARCH_LIMIT_KEY = "ReturnedSearchLimit";
    private static final String QUEUE_LIMIT_KEY = "QueueLimit";
    private static final String MIN_POLL_TIME_KEY = "UploadQueueMinPollTime";
    private static final String MAX_POLL_TIME_KEY = "UploadQueueMaxPollTime";

    private static final String SEARCH_HIT_LIMIT_ERROR_KEY = "SearchHitLimitError";

    private IntegerTextField simultaneousUploadsTF;
    private IntegerTextField uploadsPerHostTF;
    private IntegerTextField returnedSearchLimitTF;
    private JCheckBox allowUploadQueuingChkbx;
    private JLabel queueLimitLabel;
    private IntegerTextField queueLimitTF;
    private JLabel minPollTimeLabel;
    private IntegerTextField minPollTimeTF;
    private JLabel maxPollTimeLabel;
    private IntegerTextField maxPollTimeTF;
    private JCheckBox removeCompletedUploadsChkbx;
    private JCheckBox sharePartialFilesChkbx;
    private JCheckBox allowBrowsingChkbx;

    public SharingPane()
    {
        super( "Sharing" );
    }

    /**
     * Called when preparing this settings pane for display the first time. Can
     * be overriden to implement the look of the settings pane.
     */
    protected void prepareComponent()
    {
        FormLayout layout = new FormLayout(
            "10dlu, right:d, 2dlu, d, " +
            "10dlu, right:d, 2dlu, d, 2dlu:grow", // columns
            "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 9dlu, " + // rows
            "p, 3dlu, p, 3dlu, p, 3dlu, p" ); 
        layout.setRowGroups( new int[][]{{3, 5, 7, 9, 13, 15, 17}} );
        
        setLayout( layout );
        
        PanelBuilder builder = new PanelBuilder( layout, this );
        CellConstraints cc = new CellConstraints();
        CellConstraints cc2 = new CellConstraints();
        
        builder.addSeparator( Localizer.getString( "GeneralUploadSettings" ),
            cc.xywh( 1, 1, 9, 1 ) );
        
        simultaneousUploadsTF = new IntegerTextField(
            String.valueOf( ServiceManager.sCfg.mMaxUpload ), 6, 3 );    
        builder.addLabel( Localizer.getString( "MaxParallelUploads" ) + ": ",
            cc.xy( 2, 3 ), simultaneousUploadsTF, cc2.xy( 4, 3 ) );
        
        uploadsPerHostTF = new IntegerTextField(
            String.valueOf( ServiceManager.sCfg.mMaxUploadPerIP ), 6, 2 );
        builder.addLabel( Localizer.getString( "MaxUploadsPerHost" ) + ": ",
            cc.xy( 6, 3 ), uploadsPerHostTF, cc2.xy( 8, 3 ) );
        
        builder.addLabel( Localizer.getString( "MaxReturnedSearchHits" ) + ": ",
            cc.xy( 2, 5 ) );
        returnedSearchLimitTF = new IntegerTextField(
            String.valueOf( ServiceManager.sCfg.mUploadMaxSearch ), 6, 3 );
        builder.add( returnedSearchLimitTF, cc.xy( 4, 5 ) );

        allowBrowsingChkbx = new JCheckBox(
            Localizer.getString( "AllowBrowsingDirectories" ),
            ServiceManager.sCfg.mShareBrowseDir );
        builder.add( allowBrowsingChkbx, cc.xywh( 2, 7, 3, 1 ) );

        sharePartialFilesChkbx = new JCheckBox(
            Localizer.getString( "SharePartialFiles" ),
            ServiceManager.sCfg.arePartialFilesShared );
        builder.add( sharePartialFilesChkbx, cc.xywh( 6, 7, 3, 1 ) );
        
        removeCompletedUploadsChkbx = new JCheckBox(
            Localizer.getString( "AutoCleanFinishedUploads" ),
            ServiceManager.sCfg.mUploadAutoRemoveCompleted );
        builder.add( removeCompletedUploadsChkbx, cc.xywh( 2, 9, 3, 1 ) );

        builder.addSeparator( Localizer.getString( "UploadQueuing" ),
            cc.xywh( 1, 11, 9, 1 ) );

        allowUploadQueuingChkbx = new JCheckBox( Localizer.getString(
            "AllowUploadQueuing" ), ServiceManager.sCfg.allowUploadQueuing );
        allowUploadQueuingChkbx.addActionListener(
            new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    refreshEnableState();
                }
            } );
        builder.add( allowUploadQueuingChkbx, cc.xywh( 2, 13, 4, 1 ) );
        
        queueLimitLabel = builder.addLabel(
            Localizer.getString( "MaxQueueLength" ) + ": ", cc.xy( 2, 15 ) );
        queueLimitTF = new IntegerTextField(
            String.valueOf( ServiceManager.sCfg.maxUploadQueueSize ), 6, 2 );
        builder.add( queueLimitTF, cc.xy( 4, 15 ) );
        
        minPollTimeLabel = builder.addLabel(
            Localizer.getString( "MinPollTime" ) + ": ", cc.xy( 2, 17 )  );        
        minPollTimeTF = new IntegerTextField(
            String.valueOf( ServiceManager.sCfg.minUploadQueuePollTime ), 6, 3 );
        builder.add( minPollTimeTF, cc.xy( 4, 17 )  );
        
        
        maxPollTimeLabel = builder.addLabel( 
            Localizer.getString( "MaxPollTime" ) + ": ", cc.xy( 6, 17 ) );
        maxPollTimeTF = new IntegerTextField(
            String.valueOf( ServiceManager.sCfg.maxUploadQueuePollTime ), 6, 3 );
        builder.add( maxPollTimeTF, cc.xy( 8, 17 )  );
        
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
            String simultaneousUploadsStr = simultaneousUploadsTF.getText();
            Integer simultaneousUploads = new Integer( simultaneousUploadsStr );
            inputDic.put( SIMULTANEOUS_UPLOADS_KEY, simultaneousUploads );
        }
        catch ( NumberFormatException exp )
        {
            inputDic.put( NUMBER_FORMAT_ERROR_KEY, simultaneousUploadsTF );
            setInputValid( inputDic, false );
            return;
        }

        try
        {
            String uploadsPerHostStr = uploadsPerHostTF.getText();
            Integer uploadsPerHost = new Integer( uploadsPerHostStr );
            inputDic.put( UPLOADS_PER_HOST_KEY, uploadsPerHost );
        }
        catch ( NumberFormatException exp )
        {
            inputDic.put( NUMBER_FORMAT_ERROR_KEY, uploadsPerHostTF );
            setInputValid( inputDic, false );
            return;
        }

        try
        {
            String returnedSearchLimitStr = returnedSearchLimitTF.getText();
            Integer returnedSearchLimit = new Integer( returnedSearchLimitStr );
            if ( returnedSearchLimit.intValue() > 255 )
            {
                inputDic.put( SEARCH_HIT_LIMIT_ERROR_KEY, returnedSearchLimit );
                setInputValid( inputDic, false );
                return;
            }
            inputDic.put( RETURNED_SEARCH_LIMIT_KEY, returnedSearchLimit );
        }
        catch ( NumberFormatException exp )
        {
            inputDic.put( NUMBER_FORMAT_ERROR_KEY, returnedSearchLimitTF );
            setInputValid( inputDic, false );
            return;
        }

        try
        {
            String queueLimitStr = queueLimitTF.getText();
            Integer queueLimit = new Integer( queueLimitStr );
            inputDic.put( QUEUE_LIMIT_KEY, queueLimit );
        }
        catch ( NumberFormatException exp )
        {
            inputDic.put( NUMBER_FORMAT_ERROR_KEY, queueLimitTF );
            setInputValid( inputDic, false );
            return;
        }

        try
        {
            String minPollTimeStr = minPollTimeTF.getText();
            Integer minPollTime = new Integer( minPollTimeStr );
            inputDic.put( MIN_POLL_TIME_KEY, minPollTime );
        }
        catch ( NumberFormatException exp )
        {
            inputDic.put( NUMBER_FORMAT_ERROR_KEY, minPollTimeTF );
            setInputValid( inputDic, false );
            return;
        }

        try
        {
            String maxPollTimeStr = maxPollTimeTF.getText();
            Integer maxPollTime = new Integer( maxPollTimeStr );
            inputDic.put( MAX_POLL_TIME_KEY, maxPollTime );
        }
        catch ( NumberFormatException exp )
        {
            inputDic.put( NUMBER_FORMAT_ERROR_KEY, maxPollTimeTF );
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
        else if ( inputDic.containsKey( SEARCH_HIT_LIMIT_ERROR_KEY ) )
        {
            returnedSearchLimitTF.setText( "255" );
            returnedSearchLimitTF.requestFocus();
            returnedSearchLimitTF.selectAll();
            GUIUtils.showErrorMessage(
                Localizer.getString( "ExceedSearchHitLimit" ),
                Localizer.getString( "Error" ) );
        }
    }

    /**
     * Override this method if you like to apply and save changes made on
     * settings pane. To trigger saving of the configuration if any value was
     * changed call triggerConfigSave().
     */
    public void saveAndApplyChanges( HashMap inputDic )
    {
        Integer simultaneousUploadsInt = (Integer) inputDic.get(
            SIMULTANEOUS_UPLOADS_KEY );
        // ensure to have at least 1 simultaneous upload
        int simultaneousUploads = Math.max( 1, simultaneousUploadsInt.intValue() );
        if ( ServiceManager.sCfg.mMaxUpload != simultaneousUploads )
        {
            ServiceManager.sCfg.mMaxUpload = simultaneousUploads;
            OptionsSettingsPane.triggerConfigSave( inputDic );
        }

        Integer uploadsPerHostInt = (Integer) inputDic.get(
            UPLOADS_PER_HOST_KEY );
        // ensure to have at least 1 upload per host
        int uploadsPerHost = Math.max( 1, uploadsPerHostInt.intValue() );
        if ( ServiceManager.sCfg.mMaxUploadPerIP != uploadsPerHost )
        {
            ServiceManager.sCfg.mMaxUploadPerIP = uploadsPerHost;
            OptionsSettingsPane.triggerConfigSave( inputDic );
        }

        Integer returnedSearchLimitInt = (Integer) inputDic.get(
            RETURNED_SEARCH_LIMIT_KEY );
        int returnedSearchLimit = returnedSearchLimitInt.intValue();
        if ( ServiceManager.sCfg.mUploadMaxSearch != returnedSearchLimit )
        {
            ServiceManager.sCfg.mUploadMaxSearch = returnedSearchLimit;
            OptionsSettingsPane.triggerConfigSave( inputDic );
        }

        boolean allowUploadQueuing = allowUploadQueuingChkbx.isSelected();
        if ( ServiceManager.sCfg.allowUploadQueuing !=
             allowUploadQueuing )
        {
            ServiceManager.sCfg.allowUploadQueuing = allowUploadQueuing;
            OptionsSettingsPane.triggerConfigSave( inputDic );
        }

        Integer queueLimitInt = (Integer) inputDic.get( QUEUE_LIMIT_KEY );
        int queueLimit = queueLimitInt.intValue();
        if ( ServiceManager.sCfg.maxUploadQueueSize != queueLimit )
        {
            ServiceManager.sCfg.maxUploadQueueSize = queueLimit;
            OptionsSettingsPane.triggerConfigSave( inputDic );
        }

        Integer minPollTimeInt = (Integer) inputDic.get( MIN_POLL_TIME_KEY );
        int minPollTime = minPollTimeInt.intValue();
        if ( ServiceManager.sCfg.minUploadQueuePollTime != minPollTime )
        {
            ServiceManager.sCfg.minUploadQueuePollTime = minPollTime;
            OptionsSettingsPane.triggerConfigSave( inputDic );
        }

        Integer maxPollTimeInt = (Integer) inputDic.get( MAX_POLL_TIME_KEY );
        int maxPollTime = maxPollTimeInt.intValue();
        if ( ServiceManager.sCfg.maxUploadQueuePollTime != maxPollTime )
        {
            ServiceManager.sCfg.maxUploadQueuePollTime = maxPollTime;
            OptionsSettingsPane.triggerConfigSave( inputDic );
        }

        boolean sharePartialFiles = sharePartialFilesChkbx.isSelected();
        if ( ServiceManager.sCfg.arePartialFilesShared !=
             sharePartialFiles )
        {
            ServiceManager.sCfg.arePartialFilesShared = sharePartialFiles;
            OptionsSettingsPane.triggerConfigSave( inputDic );
        }

        boolean removeCompletedUploads = removeCompletedUploadsChkbx.isSelected();
        if ( ServiceManager.sCfg.mUploadAutoRemoveCompleted !=
             removeCompletedUploads )
        {
            ServiceManager.sCfg.mUploadAutoRemoveCompleted = removeCompletedUploads;
            OptionsSettingsPane.triggerConfigSave( inputDic );
        }

        boolean allowBrowsing = allowBrowsingChkbx.isSelected();
        if ( ServiceManager.sCfg.mShareBrowseDir != allowBrowsing )
        {
            ServiceManager.sCfg.mShareBrowseDir = allowBrowsing;
            OptionsSettingsPane.triggerConfigSave( inputDic );
        }
    }

    private void refreshEnableState()
    {
        boolean enableState = allowUploadQueuingChkbx.isSelected();
        queueLimitLabel.setEnabled( enableState );
        queueLimitTF.setEnabled( enableState );
        minPollTimeLabel.setEnabled( enableState );
        minPollTimeTF.setEnabled( enableState );
        maxPollTimeLabel.setEnabled( enableState );
        maxPollTimeTF.setEnabled( enableState );
    }
}