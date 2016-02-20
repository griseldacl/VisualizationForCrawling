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
 *  $Id: DisconnectPane.java,v 1.5 2005/10/03 00:18:26 gregork Exp $
 */
package phex.gui.dialogs.options;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import phex.common.ServiceManager;
import phex.gui.common.IntegerTextField;
import phex.utils.Localizer;

/**
 *
 * @author  enigma
 */
public class DisconnectPane extends OptionsSettingsPane
{
    private JCheckBox applyPolicyChkbx;
    private IntegerTextField maxSendQueueTF;
    private JSlider dropPacketExceedsSlider;
    private JLabel dropPacketExceedsLabel;
    private IntegerTextField minSharedFilesTF;
    private IntegerTextField minSharedMBTF;
    private JCheckBox emptyVendorStringChkbx;

    private static final String APPLY_POLICY_KEY = "applyPolicy";
    private static final String SEND_QUEUE_KEY = "sendQueue";
    private static final String DROP_PACKET_KEY = "dropPacket";
    private static final String VENDOR_KEY = "vendor";
    private static final String SHARED_FILES_KEY = "sharedFiles";
    private static final String SHARED_MB_KEY = "sharedMb";

    /** Creates a new instance of DisconnectPane */
    public DisconnectPane()
    {
        super( "DisconnectPolicy" );
    }

    /** Called when preparing this settings pane for display the first time. Can
     * be overriden to implement the look of the settings pane.
     */
    protected void prepareComponent()
    {
        GridBagConstraints constraints;
        setLayout( new GridBagLayout() );

        JPanel generalPanel = new JPanel( new GridBagLayout() );
        generalPanel.setBorder( BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            Localizer.getString( "DisconnectPolicy" ) ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.insets = new Insets( 3, 0, 5, 0 );
            constraints.weightx = 1;
            constraints.weighty = 1;
        add( generalPanel, constraints );

        applyPolicyChkbx = new JCheckBox(
            Localizer.getString( "DisconnectNodesAccordingRules" ),
            ServiceManager.sCfg.mDisconnectApplyPolicy );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 2;
            constraints.gridwidth = 4;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets( 0, 0, 5, 0 );
            constraints.weightx = 1;
        generalPanel.add( applyPolicyChkbx, constraints );

        emptyVendorStringChkbx = new JCheckBox(
            Localizer.getString( "DisconnectNoVendorNodes" ),
            ServiceManager.sCfg.isNoVendorNodeDisconnected );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 3;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets( 0, 15, 3, 0 );
            constraints.gridwidth = 4;
        generalPanel.add( emptyVendorStringChkbx, constraints );

        JLabel label = new JLabel( Localizer.getString( "SendQueueExceeds" ) +
            Localizer.getString("ColonSign") + " " );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 4;
            constraints.anchor = GridBagConstraints.NORTHEAST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets( 0, 15, 3, 5 );
        generalPanel.add( label, constraints );

        maxSendQueueTF = new IntegerTextField(
            String.valueOf( ServiceManager.sCfg.mNetMaxSendQueue ), 3, 3 );
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 4;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets( 0, 0, 3, 0 );
        generalPanel.add( maxSendQueueTF, constraints );


        label = new JLabel( Localizer.getString( "MinSharedFiles") +
            Localizer.getString("ColonSign") + " "  );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 5;
            constraints.anchor = GridBagConstraints.NORTHEAST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets( 0, 15, 3, 5 );
        generalPanel.add( label, constraints );

        minSharedFilesTF = new IntegerTextField(
            String.valueOf( ServiceManager.sCfg.freeloaderFiles ), 4, 4 );
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 5;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets( 0, 0, 3, 0 );
        generalPanel.add( minSharedFilesTF, constraints );

        label = new JLabel( Localizer.getString( "MinSharedMB") +
            Localizer.getString("ColonSign") + " " );
            constraints = new GridBagConstraints();
            constraints.gridx = 2;
            constraints.gridy = 5;
            constraints.anchor = GridBagConstraints.NORTHEAST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets( 0, 15, 3, 5 );
        generalPanel.add( label, constraints );

        minSharedMBTF = new IntegerTextField(
            String.valueOf( ServiceManager.sCfg.freeloaderShareSize ), 4, 4 );
            constraints = new GridBagConstraints();
            constraints.gridx = 3;
            constraints.gridy = 5;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.weightx = 1;
            constraints.insets = new Insets( 0, 0, 3, 0 );
        generalPanel.add( minSharedMBTF, constraints );




        label = new JLabel( Localizer.getString( "DropPacketExceeds" ) +
            Localizer.getString("ColonSign") + " ");
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 6;
            constraints.anchor = GridBagConstraints.NORTHEAST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets( 0, 15, 3, 5 );
        generalPanel.add( label, constraints );


        JPanel percentPanel = new JPanel(new GridBagLayout());
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 6;
            constraints.gridwidth = 3;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.insets = new Insets( 0, 0, 3, 0 );
            constraints.weightx = 1;
            constraints.weighty = 1;
        generalPanel.add( percentPanel, constraints );


        dropPacketExceedsLabel = new JLabel( String.valueOf(
              ServiceManager.sCfg.mDisconnectDropRatio )
            + Localizer.getString("PercentSign") );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets( 0, 0, 3, 5 );
        percentPanel.add( dropPacketExceedsLabel, constraints );

        dropPacketExceedsSlider = new JSlider(0, 100,
            ServiceManager.sCfg.mDisconnectDropRatio );
        dropPacketExceedsSlider.addChangeListener( new ChangeListener()
            {
                public void stateChanged(ChangeEvent changeEvent)
                {
                    dropPacketExceedsLabel.setText(
                        String.valueOf(dropPacketExceedsSlider.getValue())
                        + Localizer.getString("PercentSign") );
                }
            } );
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.weightx = 1;
            constraints.insets = new Insets( 0, 0, 3, 0 );
        percentPanel.add( dropPacketExceedsSlider, constraints );


    }

    public void checkInput( HashMap inputDic )
    {
        try{
            inputDic.put(SEND_QUEUE_KEY, new Integer(maxSendQueueTF.getText()));
        }catch(NumberFormatException exp){
            inputDic.put( NUMBER_FORMAT_ERROR_KEY, maxSendQueueTF );
            setInputValid( inputDic, false );
            return;
        }
        try{
            inputDic.put(SHARED_FILES_KEY, new Integer(minSharedFilesTF.getText()));
        }catch(NumberFormatException exp){
            inputDic.put( NUMBER_FORMAT_ERROR_KEY, minSharedFilesTF );
            setInputValid( inputDic, false );
            return;
        }
        try
        {
            inputDic.put(SHARED_MB_KEY, new Integer(minSharedMBTF.getText()));
        }catch(NumberFormatException exp){
            inputDic.put( NUMBER_FORMAT_ERROR_KEY, minSharedMBTF );
            setInputValid( inputDic, false );
            return;
        }

        inputDic.put(DROP_PACKET_KEY, new Integer(dropPacketExceedsSlider.getValue()));
        inputDic.put(APPLY_POLICY_KEY, Boolean.valueOf( applyPolicyChkbx.isSelected() ) );
        inputDic.put(VENDOR_KEY, Boolean.valueOf( emptyVendorStringChkbx.isSelected() ) );
        setInputValid( inputDic, true );
    }

    public void saveAndApplyChanges( HashMap inputDic )
    {
        boolean changed = false;
        Boolean applyPolicy = (Boolean)inputDic.get(APPLY_POLICY_KEY);
        Integer sendQueue = (Integer)inputDic.get(SEND_QUEUE_KEY);
        Integer dropPacket = (Integer)inputDic.get(DROP_PACKET_KEY);
        Boolean vendor = (Boolean)inputDic.get(VENDOR_KEY);
        Integer sharedFiles = (Integer)inputDic.get(SHARED_FILES_KEY);
        Integer sharedMb = (Integer)inputDic.get(SHARED_MB_KEY);

        if (    (applyPolicy!=null)
             && (applyPolicy.booleanValue() != ServiceManager.sCfg.mDisconnectApplyPolicy))
        {
            ServiceManager.sCfg.mDisconnectApplyPolicy = applyPolicy.booleanValue();
            changed = true;
        }
        if (    (sendQueue!=null)
             && (sendQueue.intValue() != ServiceManager.sCfg.mNetMaxSendQueue))
        {
            ServiceManager.sCfg.mNetMaxSendQueue = sendQueue.intValue();
            changed = true;
        }
        if (    (dropPacket!=null)
             && (dropPacket.intValue() != ServiceManager.sCfg.mDisconnectDropRatio))
        {
            ServiceManager.sCfg.mDisconnectDropRatio = dropPacket.intValue();
            changed = true;
        }
        if (    (vendor!=null)
             && (vendor.booleanValue() != ServiceManager.sCfg.isNoVendorNodeDisconnected))
        {
            ServiceManager.sCfg.isNoVendorNodeDisconnected = vendor.booleanValue();
            changed = true;
        }
        if (    (sharedFiles!=null)
             && (sharedFiles.intValue() != ServiceManager.sCfg.freeloaderFiles))
        {
            ServiceManager.sCfg.freeloaderFiles = sharedFiles.intValue();
            changed = true;
        }
        if (    (sharedMb!=null)
             && (sharedMb.intValue() != ServiceManager.sCfg.freeloaderShareSize))
        {
            ServiceManager.sCfg.freeloaderShareSize = sharedMb.intValue();
            changed = true;
        }

        if (changed)
            OptionsSettingsPane.triggerConfigSave( inputDic );
    }
}
