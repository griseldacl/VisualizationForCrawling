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
package phex.gui.dialogs;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import phex.common.ExpiryDate;
import phex.common.address.AddressUtils;
import phex.gui.common.*;
import phex.security.IPAccessRule;
import phex.security.PhexSecurityManager;
import phex.utils.Localizer;

public class SecurityRuleConfigDialog extends JDialog
{
    private static final long MINUTE = 60000l;
    private static final long HOUR = 3600000l;
    private static final long DAY = 86400000l;


    private IPAccessRule securityRule;

    private JTextField descriptionTF;
    private JCheckBox disableRuleCkBx;
    private JComboBox addressTypeCBox;
    private JLabel firstIPLabel;
    private IPTextField firstIPField;
    private JLabel secondIPLabel;
    private IPTextField secondIPField;
    private JComboBox expiresCBox;
    private JComboBox ruleTypeCBox;
    private IntegerTextField daysTF;
    private JLabel daysLabel;
    private IntegerTextField hoursTF;
    private JLabel hoursLabel;
    private IntegerTextField minutesTF;
    private JLabel minutesLabel;
    private JCheckBox isDeletedOnExpiryCkbx;

    public SecurityRuleConfigDialog()
    {
        this( null );
    }

    public SecurityRuleConfigDialog( IPAccessRule accessRule )
    {
        super( GUIRegistry.getInstance().getMainFrame(),
            Localizer.getString( "NewSecurityRule" ), true );
        securityRule = accessRule;
        prepareComponent();
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
        contentPane.setLayout( new GridBagLayout() );

        GridBagConstraints constraints;

        JPanel pane = new JPanel( new GridBagLayout() );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.insets = new Insets( 0, 0, 0, 0 );
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.BOTH;
        contentPane.add( pane, constraints );

        JPanel topPanel = new JPanel( new GridBagLayout() );
        topPanel.setBorder( BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), Localizer.getString( "SecurityRule" ) ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.insets = new Insets( 2, 2, 2, 2 );
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.BOTH;
        pane.add( topPanel, constraints );

        JLabel descriptionLabel = new JLabel( Localizer.getString( "Description" )
            + Localizer.getString( "ColonSign" ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.insets = new Insets( 0, 2, 2, 4 );
            constraints.anchor = GridBagConstraints.EAST;
        topPanel.add( descriptionLabel, constraints );

        descriptionTF = new JTextField( 20 );
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.insets = new Insets( 0, 0, 2, 2 );
            constraints.anchor = GridBagConstraints.WEST;
        topPanel.add( descriptionTF, constraints );

        disableRuleCkBx = new JCheckBox( Localizer.getString( "DisableSecurityRule" ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 1;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.gridwidth = 2;
            constraints.insets = new Insets( 0, 0, 2, 4 );
            constraints.anchor = GridBagConstraints.WEST;
        topPanel.add( disableRuleCkBx, constraints );

        JPanel middlePanel = new JPanel( new GridBagLayout() );
        middlePanel.setBorder( BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), Localizer.getString( "NetworkAddress" ) ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.weightx = 1;
            constraints.insets = new Insets( 0, 2, 2, 2 );
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
        pane.add( middlePanel, constraints );

        JPanel groupBtnPane = new JPanel( new GridBagLayout() );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 0;
            constraints.weighty = 1;
            constraints.insets = new Insets( 0, 0, 0, 0 );
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
        middlePanel.add( groupBtnPane, constraints );

        JLabel addTypeLabel = new JLabel( Localizer.getString( "AddressType" )
            + Localizer.getString( "ColonSign" ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.insets = new Insets( 0, 2, 0, 4 );
            constraints.anchor = GridBagConstraints.EAST;
        groupBtnPane.add( addTypeLabel, constraints );

        String[] addressTypeArr = new String[]
        {
            Localizer.getString( "SingleAddress" ),
            Localizer.getString( "Network/Range" ),
            Localizer.getString( "Network/Mask" )
        };

        addressTypeCBox = new JComboBox( addressTypeArr );
        addressTypeCBox.addActionListener(
            new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    refreshAddressDisplayState();
                }
            } );
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.insets = new Insets( 0, 0, 0, 0 );
            constraints.anchor = GridBagConstraints.WEST;
        groupBtnPane.add( addressTypeCBox, constraints );

        JPanel ipFieldPane = new JPanel( new GridBagLayout() );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.insets = new Insets( 8, 1, 0, 0 );
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.NORTHWEST;
        middlePanel.add( ipFieldPane, constraints );

        firstIPLabel = new JLabel( Localizer.getString( "HostAddress" )
            + Localizer.getString( "ColonSign" ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.insets = new Insets( 0, 2, 2, 4 );
            constraints.anchor = GridBagConstraints.EAST;
        ipFieldPane.add( firstIPLabel, constraints );

        secondIPLabel = new JLabel( Localizer.getString( "NetworkMask" )
            + Localizer.getString( "ColonSign" ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 1;
            constraints.weighty = 1;
            constraints.insets = new Insets( 0, 2, 0, 4 );
            constraints.anchor = GridBagConstraints.EAST;
        ipFieldPane.add( secondIPLabel, constraints );

        firstIPField = new IPTextField();
            constraints = new GridBagConstraints();
            constraints.gridx = 2;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.insets = new Insets( 0, 0, 2, 2 );
            constraints.anchor = GridBagConstraints.WEST;
        ipFieldPane.add( firstIPField, constraints );

        secondIPField = new IPTextField();
            constraints = new GridBagConstraints();
            constraints.gridx = 2;
            constraints.gridy = 1;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.insets = new Insets( 0, 0, 0, 2 );
            constraints.anchor = GridBagConstraints.WEST;
        ipFieldPane.add( secondIPField, constraints );

        JPanel bottomPanel = new JPanel( new GridBagLayout() );
        bottomPanel.setBorder( BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), Localizer.getString( "Options" ) ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 2;
            constraints.weightx = 1;
            constraints.insets = new Insets( 0, 2, 2, 2 );
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.BOTH;
        pane.add( bottomPanel, constraints );

        JLabel typeLabel = new JLabel( Localizer.getString( "ActionType" )
            + Localizer.getString( "ColonSign" ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.insets = new Insets( 0, 2, 6, 4 );
            constraints.anchor = GridBagConstraints.EAST;
        bottomPanel.add( typeLabel, constraints );

        String[] typeArr = new String[]
        {
            Localizer.getString( "Deny" ),
            Localizer.getString( "Accept" )
        };

        ruleTypeCBox = new JComboBox( typeArr );
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.insets = new Insets( 0, 0, 6, 0 );
            constraints.anchor = GridBagConstraints.WEST;
        bottomPanel.add( ruleTypeCBox, constraints );

        JLabel expiresLabel = new JLabel( Localizer.getString( "Expires" )
            + Localizer.getString( "ColonSign" ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.insets = new Insets( 0, 2, 2, 4 );
            constraints.anchor = GridBagConstraints.EAST;
        bottomPanel.add( expiresLabel, constraints );

        String[] expireArr = new String[]
        {
            Localizer.getString( "Never" ),
            Localizer.getString( "EndOfSession" ),
            Localizer.getString( "After" ) + Localizer.getString( "ColonSign" )
        };

        expiresCBox = new JComboBox( expireArr );
        expiresCBox.addActionListener(
            new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    refreshExpiryDisplayState();
                }
            } );
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 1;
            constraints.weightx = 1;
            constraints.insets = new Insets( 0, 0, 0, 2 );
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.WEST;
        bottomPanel.add( expiresCBox, constraints );

        JPanel afterTimePanel = new JPanel( new GridBagLayout() );
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 2;
            constraints.weightx = 1;
            constraints.insets = new Insets( 2, 0, 0, 0 );
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.BOTH;
        bottomPanel.add( afterTimePanel, constraints );

        daysTF = new IntegerTextField( 4 );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.insets = new Insets( 0, 0, 0, 0 );
            constraints.anchor = GridBagConstraints.WEST;
        afterTimePanel.add( daysTF, constraints );

        daysLabel = new JLabel( Localizer.getString( "Days" ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.insets = new Insets( 0, 2, 0, 0 );
            constraints.anchor = GridBagConstraints.WEST;
        afterTimePanel.add( daysLabel, constraints );

        hoursTF = new IntegerTextField( 4 );
            constraints = new GridBagConstraints();
            constraints.gridx = 2;
            constraints.gridy = 0;
            constraints.insets = new Insets( 0, 4, 0, 0 );
            constraints.anchor = GridBagConstraints.WEST;
        afterTimePanel.add( hoursTF, constraints );

        hoursLabel = new JLabel( Localizer.getString( "Hours" ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 3;
            constraints.gridy = 0;
            constraints.insets = new Insets( 0, 2, 0, 0 );
            constraints.anchor = GridBagConstraints.WEST;
        afterTimePanel.add( hoursLabel, constraints );

        minutesTF = new IntegerTextField( 4 );
            constraints = new GridBagConstraints();
            constraints.gridx = 4;
            constraints.gridy = 0;
            constraints.insets = new Insets( 0, 4, 0, 0 );
            constraints.anchor = GridBagConstraints.WEST;
        afterTimePanel.add( minutesTF, constraints );

        minutesLabel = new JLabel( Localizer.getString( "Minutes" ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 5;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.insets = new Insets( 0, 2, 0, 2 );
            constraints.anchor = GridBagConstraints.WEST;
        afterTimePanel.add( minutesLabel, constraints );


        isDeletedOnExpiryCkbx = new JCheckBox( Localizer.getString(
            "DeleteRuleAfterExpiry" ) );
        isDeletedOnExpiryCkbx.setToolTipText( Localizer.getString( "TTTDeleteRuleAfterExpiry" ) );
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 4;
            constraints.weightx = 1;
            constraints.insets = new Insets( 0, 0, 0, 2 );
            constraints.anchor = GridBagConstraints.NORTHWEST;
        bottomPanel.add( isDeletedOnExpiryCkbx, constraints );

        JPanel buttonPanel = new JPanel( new GridBagLayout() );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 3;
            constraints.weightx = 1;
            constraints.insets = new Insets( 2, 0, 2, 0 );
            constraints.anchor = GridBagConstraints.SOUTHWEST;
            constraints.fill = GridBagConstraints.BOTH;
        pane.add( buttonPanel, constraints );

        JSeparator sep = new JSeparator();
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.gridwidth = 3;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.insets = new Insets( 6, 0, 6, 0 );
            constraints.anchor = GridBagConstraints.CENTER;
            constraints.fill = GridBagConstraints.BOTH;
        buttonPanel.add( sep, constraints );

        ButtonActionHandler actionHandler = new ButtonActionHandler();

        JButton okButton = new JButton( Localizer.getString( "OK" ) );
        okButton.setDefaultCapable( true );
        okButton.setRequestFocusEnabled( true );
        okButton.addActionListener( actionHandler );
        okButton.setActionCommand( "OK" );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.weightx = 1;
            constraints.anchor = GridBagConstraints.EAST;
            constraints.insets = new Insets( 3, 3, 3, 3 );
            constraints.fill = GridBagConstraints.NONE;
        buttonPanel.add( okButton, constraints );

        JButton cancelButton = new JButton( Localizer.getString( "Cancel" ) );
        cancelButton.setRequestFocusEnabled( true );
        cancelButton.addActionListener( actionHandler );
        cancelButton.setActionCommand( "CANCEL" );
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 1;
            constraints.anchor = GridBagConstraints.EAST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets( 3, 3, 3, 3 );
            constraints.weighty = 1;
        buttonPanel.add( cancelButton, constraints );

        // unfortunatly JDialog has no updateUI method... so this goes here...
        GUIUtils.adjustComboBoxHeight( expiresCBox );
        GUIUtils.adjustComboBoxHeight( ruleTypeCBox );
        GUIUtils.adjustComboBoxHeight( addressTypeCBox );

        initContent();

        refreshAddressDisplayState();
        refreshExpiryDisplayState();

        setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );
        getRootPane().setDefaultButton( okButton );
        pack();
        setLocationRelativeTo( getParent() );
    }

    private void closeDialog( )
    {
        setVisible(false);
        dispose();
    }

    private void initContent()
    {
        if ( securityRule == null )
        {
            return;
        }
        descriptionTF.setText( securityRule.getDescription() );
        byte addressType = securityRule.getAddressType();
        switch ( addressType )
        {
            case IPAccessRule.SINGLE_ADDRESS:
                addressTypeCBox.setSelectedIndex( 0 );
                firstIPField.setIPString( AddressUtils.ip2string( securityRule.getHostIP() ) );
                break;
            case IPAccessRule.NETWORK_RANGE:
                addressTypeCBox.setSelectedIndex( 1 );
                firstIPField.setIPString( AddressUtils.ip2string( securityRule.getHostIP() ) );
                secondIPField.setIPString( AddressUtils.ip2string( securityRule.getCompareIP() ) );
                break;
            case IPAccessRule.NETWORK_MASK:
                addressTypeCBox.setSelectedIndex( 2 );
                firstIPField.setIPString( AddressUtils.ip2string( securityRule.getHostIP() ) );
                secondIPField.setIPString( AddressUtils.ip2string( securityRule.getCompareIP() ) );
                break;
        }
        boolean isDenyingRule = securityRule.isDenyingRule();
        if ( isDenyingRule )
        {
            ruleTypeCBox.setSelectedIndex( 0 );
        }
        else
        {
            ruleTypeCBox.setSelectedIndex( 1 );
        }
        disableRuleCkBx.setSelected( securityRule.isDisabled() );
        ExpiryDate expiryDate = securityRule.getExpiryDate();
        if ( expiryDate.isExpiringNever() )
        {
            expiresCBox.setSelectedIndex( 0 );
        }
        else if ( expiryDate.isExpiringEndOfSession() )
        {
            expiresCBox.setSelectedIndex( 1 );
        }
        else
        {
            expiresCBox.setSelectedIndex( 2 );
            initAfterExpiryDateContent( expiryDate );
        }
        isDeletedOnExpiryCkbx.setSelected( securityRule.isDeletedOnExpiry() );

    }

    private void refreshAddressDisplayState()
    {
        if ( Localizer.getString( "SingleAddress" ).equals(
            addressTypeCBox.getSelectedItem() ) )
        {
            firstIPLabel.setText( Localizer.getString( "HostAddress" )
                + Localizer.getString( "ColonSign" ) );
            secondIPLabel.setText( Localizer.getString( "NetworkMask" )
                + Localizer.getString( "ColonSign" ) );

            secondIPLabel.setEnabled( false );
            secondIPField.setEnabled( false );
            secondIPField.setIPString( "255.255.255.255" );
        }
        else if ( Localizer.getString( "Network/Range" ).equals(
            addressTypeCBox.getSelectedItem() ) )
        {
            firstIPLabel.setText( Localizer.getString( "FirstAddress" )
                + Localizer.getString( "ColonSign" ) );
            secondIPLabel.setText( Localizer.getString( "LastAddress" )
                + Localizer.getString( "ColonSign" ) );

            secondIPLabel.setEnabled( true );
            secondIPField.setEnabled( true );
        }
        else if ( Localizer.getString( "Network/Mask" ).equals(
            addressTypeCBox.getSelectedItem() ) )
        {
            firstIPLabel.setText( Localizer.getString( "NetworkAddress" )
                + Localizer.getString( "ColonSign" ) );
            secondIPLabel.setText( Localizer.getString( "NetworkMask" )
                + Localizer.getString( "ColonSign" ) );

            secondIPLabel.setEnabled( true );
            secondIPField.setEnabled( true );
        }
    }

    private void refreshExpiryDisplayState()
    {
        if ( Localizer.getString( "Never" ).equals(
            expiresCBox.getSelectedItem() ) )
        {
            isDeletedOnExpiryCkbx.setEnabled( false );
            daysTF.setEnabled( false );
            daysLabel.setEnabled( false );
            hoursTF.setEnabled( false );
            hoursLabel.setEnabled( false );
            minutesTF.setEnabled( false );
            minutesLabel.setEnabled( false );
        }
        else if ( Localizer.getString( "EndOfSession" ).equals(
            expiresCBox.getSelectedItem() ) )
        {
            isDeletedOnExpiryCkbx.setEnabled( true );
            daysTF.setEnabled( false );
            daysLabel.setEnabled( false );
            hoursTF.setEnabled( false );
            hoursLabel.setEnabled( false );
            minutesTF.setEnabled( false );
            minutesLabel.setEnabled( false );
        }
        else if ( (Localizer.getString( "After" ) + Localizer.getString( "ColonSign" )).equals(
            expiresCBox.getSelectedItem() ) )
        {
            isDeletedOnExpiryCkbx.setEnabled( true );
            daysTF.setEnabled( true );
            daysLabel.setEnabled( true );
            hoursTF.setEnabled( true );
            hoursLabel.setEnabled( true );
            minutesTF.setEnabled( true );
            minutesLabel.setEnabled( true );
        }
    }

    private void validateAndSaveSecurityRule()
    {
        String description = descriptionTF.getText();
        boolean isDenyingRule = ruleTypeCBox.getSelectedIndex() == 0;
        byte addressType;
        byte[] ip;
        byte[] compareIP;
        switch ( addressTypeCBox.getSelectedIndex() )
        {
            case 0:
                addressType = IPAccessRule.SINGLE_ADDRESS;
                ip = firstIPField.getIP();
                compareIP = null;
                break;
            case 1:
                addressType = IPAccessRule.NETWORK_RANGE;
                ip = firstIPField.getIP();
                compareIP = secondIPField.getIP();
                break;
            case 2:
                addressType = IPAccessRule.NETWORK_MASK;
                ip = firstIPField.getIP();
                compareIP = secondIPField.getIP();
                break;
            default:
                throw new RuntimeException( "Unknown address type: " +
                    addressTypeCBox.getSelectedIndex() );
        }
        boolean isDisabled = disableRuleCkBx.isSelected();
        ExpiryDate expiryDate;
        switch ( expiresCBox.getSelectedIndex() )
        {
            // never
            case 0:
                expiryDate = ExpiryDate.NEVER_EXPIRY_DATE;
                break;
            // end of session
            case 1:
                expiryDate = ExpiryDate.SESSION_EXPIRY_DATE;
                break;
            // after
            case 2:
                expiryDate = createAfterExpiryDate();
                break;
            default:
                throw new RuntimeException( "Unknown expiry type: " +
                    expiresCBox.getSelectedIndex() );
        }
        boolean isDeletedOnExpiry = isDeletedOnExpiryCkbx.isSelected();
        if ( securityRule == null )
        {
            PhexSecurityManager.getInstance().createIPAccessRule(
                description, isDenyingRule, addressType, ip, compareIP,
                isDisabled, expiryDate, isDeletedOnExpiry );
        }
        else
        {
            securityRule.setHostIP( ip );
            securityRule.setCompareIP( compareIP );
            securityRule.setDescription( description );
            securityRule.setDenyingRule( isDenyingRule );
            securityRule.setAddressType( addressType );
            securityRule.setDisabled( isDisabled );
            securityRule.setExpiryDate( expiryDate );
            securityRule.setDeleteOnExpiry( isDeletedOnExpiry );
        }
    }

    private void initAfterExpiryDateContent( ExpiryDate expiryDate )
    {
        long time = expiryDate.getTime();
        long currentTime = System.currentTimeMillis();
        long timeDiff = time - currentTime;
        int days;
        int hours;
        int minutes;
        if ( timeDiff <= 0 )
        {
            days = 0;
            hours = 0;
            minutes = 0;
        }
        else
        {
            days = (int)Math.floor((double)(timeDiff / DAY));
            timeDiff -= days * DAY;
            hours = (int)Math.floor((double)(timeDiff / HOUR));
            timeDiff -= hours * HOUR;
            minutes = (int)Math.ceil((double)( timeDiff / MINUTE ));
        }
        daysTF.setText( String.valueOf( days ) );
        hoursTF.setText( String.valueOf( hours ) );
        minutesTF.setText( String.valueOf( minutes ) );
    }

    private ExpiryDate createAfterExpiryDate()
    {
        Integer days = daysTF.getIntegerValue();
        if ( days == null )
        {
            days = new Integer( 0 );
        }
        Integer hours = hoursTF.getIntegerValue();
        if ( hours == null )
        {
            hours = new Integer( 0 );
        }
        Integer minutes = minutesTF.getIntegerValue();
        if ( minutes == null )
        {
            minutes = new Integer( 0 );
        }

        long currentTime = System.currentTimeMillis();
        currentTime += days.intValue() * DAY
            + hours.intValue() * HOUR
            + minutes.intValue() * MINUTE;
        return new ExpiryDate( currentTime );
    }

    private class ButtonActionHandler implements ActionListener
    {
        public void actionPerformed( ActionEvent e )
        {
            if ( e.getActionCommand().equals( "OK" ) )
            {
                validateAndSaveSecurityRule();
            }
            closeDialog( );
        }
    }
}
