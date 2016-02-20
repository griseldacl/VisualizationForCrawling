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
import java.io.File;

import javax.swing.*;

import phex.common.*;
import phex.common.Cfg;
import phex.common.FileHandlingException;
import phex.common.ServiceManager;
import phex.download.RemoteFile;
import phex.download.swarming.SWDownloadFile;
import phex.download.swarming.SwarmingManager;
import phex.gui.common.GUIRegistry;
import phex.gui.common.GUIUtils;
import phex.gui.common.MainFrame;
import phex.utils.*;
import phex.utils.FileUtils;
import phex.utils.Localizer;
import phex.utils.StringUtils;

public class DownloadConfigDialog extends JDialog
{
    private JTextField filenameTF;
    private JCheckBox renameOldFileCBx;
    private JTextField researchTermTF;
    private JCheckBox switchToDownldCBx;
    
    /**
     * Used to verify externaly if this dialog was canceled.
     */
    private boolean isDialogCanceled;

    /**
     * The remote file for the not started download to be configured.
     * Either the remoteFile or the downloadFile is given for configuration
     * never both.
     */
    private RemoteFile remoteFile;

    /**
     * The download file for a already queued download to be configured.
     * Either the remoteFile or the downloadFile is given for configuration
     * never both.
     */
    private SWDownloadFile downloadFile;

    private DownloadConfigDialog()
    {
        super( GUIRegistry.getInstance().getMainFrame(),
            Localizer.getString( "DownloadConfiguration" ), true );
        isDialogCanceled = false;
    }

    /**
     * Allows the configuration of a file before it is added to the download list.
     */
    public DownloadConfigDialog( RemoteFile aRemoteFile )
    {
        this();
        remoteFile = aRemoteFile;
        prepareComponent();
    }

    /**
     * Allows the configuration of a file that is already added to the download list.
     */
    public DownloadConfigDialog( SWDownloadFile aDownloadFile )
    {
        this();
        downloadFile = aDownloadFile;
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
        JPanel pane = new JPanel();
        pane.setLayout( new GridBagLayout() );
            constraints = new GridBagConstraints();
            constraints.fill = GridBagConstraints.BOTH;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.insets = new Insets( 10, 10, 10, 10 );
        contentPane.add( pane, constraints );

        StringBuffer filenameBuffer = new StringBuffer();
        filenameBuffer.append( Localizer.getString( "File" ) );
        filenameBuffer.append( ":  " );
        if ( remoteFile != null )
        {
            filenameBuffer.append( remoteFile.getFilename() );
        }
        else if ( downloadFile != null )
        {
            filenameBuffer.append( downloadFile.getDestinationFileName() );
        }

        JLabel label = new JLabel( filenameBuffer.toString() );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.anchor = GridBagConstraints.NORTHWEST;
        pane.add( label, constraints );

        StringBuffer filesizeBuffer = new StringBuffer();
        filesizeBuffer.append( Localizer.getString( "FileSize" ) );
        filesizeBuffer.append( ":  " );
        if ( remoteFile != null )
        {
            filesizeBuffer.append( remoteFile.getFileSizeObject() );
        }
        else if ( downloadFile != null )
        {
            filesizeBuffer.append( downloadFile.getTotalDataSize() );
        }
        filesizeBuffer.append( ' ' );
        filesizeBuffer.append( Localizer.getString( "Bytes" ) );

        label = new JLabel( filesizeBuffer.toString() );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.insets = new Insets( 3, 0, 3, 0 );
            constraints.anchor = GridBagConstraints.NORTHWEST;
        pane.add( label, constraints );

        JSeparator sep = new JSeparator();
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 2;
            constraints.weightx = 1;
            constraints.insets = new Insets( 6, 0, 6, 0 );
            constraints.anchor = GridBagConstraints.SOUTH;
            constraints.fill = GridBagConstraints.BOTH;
        pane.add( sep, constraints );

        JPanel panel = new JPanel();
        panel.setLayout( new GridBagLayout() );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 3;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.insets = new Insets( 3, 0, 3, 0 );
        pane.add( panel, constraints );

        label = new JLabel( Localizer.getString( "LocalFileName" ) + ":  " );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.anchor = GridBagConstraints.NORTHWEST;
        panel.add( label, constraints );

        String filenameStr = null;
        if ( remoteFile != null )
        {
            filenameStr = remoteFile.getFilename();
        }
        else if ( downloadFile != null )
        {
            filenameStr = downloadFile.getDestinationFileName();
        }
        filenameTF = new JTextField( filenameStr, 30 );
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add( filenameTF, constraints );

        if ( downloadFile != null )
        {
            renameOldFileCBx = new JCheckBox(
                Localizer.getString( "RenameFile" ) );
            renameOldFileCBx.setSelected( true );
                constraints = new GridBagConstraints();
                constraints.gridx = 1;
                constraints.gridy = 1;
                constraints.gridwidth = 2;
                constraints.anchor = GridBagConstraints.NORTHEAST;
            panel.add( renameOldFileCBx, constraints );
        }

        panel = new JPanel();
        panel.setLayout( new GridBagLayout() );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 4;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.insets = new Insets( 3, 0, 3, 0 );
        pane.add( panel, constraints );

        label = new JLabel( Localizer.getString( "ResearchTerm" ) + ":  " );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.anchor = GridBagConstraints.NORTHWEST;
        panel.add( label, constraints );

        String searchTerm = null;
        if ( remoteFile != null )
        {
            searchTerm = StringUtils.createNaturalSearchTerm(
                remoteFile.getDisplayName() );
        }
        else if ( downloadFile != null )
        {
            searchTerm = downloadFile.getResearchSetting().getSearchTerm();
        }

        researchTermTF = new JTextField( searchTerm, 30 );
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add( researchTermTF, constraints );

        // only display switch when on upcomming download config.
        if ( remoteFile != null )
        {
            switchToDownldCBx = new JCheckBox(
                Localizer.getString( "SwitchToDownloadTab" ) );
            switchToDownldCBx.setSelected( true );
                constraints = new GridBagConstraints();
                constraints.gridx = 0;
                constraints.gridy = 5;
                constraints.weightx = 1;
                constraints.anchor = GridBagConstraints.NORTHWEST;
                constraints.insets = new Insets( 3, 0, 3, 0 );
            pane.add( switchToDownldCBx, constraints );
        }

        JPanel btnPane = new JPanel();
        btnPane.setLayout( new GridBagLayout() );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 6;
            constraints.gridwidth = 2;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.insets = new Insets( 2, 0, 2, 0 );
            constraints.fill = GridBagConstraints.BOTH;
            constraints.anchor = GridBagConstraints.SOUTHEAST;
        pane.add( btnPane, constraints );

        /*JPanel filler = new JPanel();
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.gridwidth = 2;
            constraints.weighty = 1;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.anchor = GridBagConstraints.SOUTH;
        btnPane.add( filler, constraints );*/

        sep = new JSeparator();
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.gridwidth = 2;
            constraints.weighty = 1;
            constraints.insets = new Insets( 6, 0, 6, 0 );
            constraints.anchor = GridBagConstraints.SOUTH;
            constraints.fill = GridBagConstraints.HORIZONTAL;
        btnPane.add( sep, constraints );

        String buttonTxt = null;
        if ( remoteFile != null )
        {
            buttonTxt =  Localizer.getString( "Download" );
        }
        else if ( downloadFile != null )
        {
            buttonTxt =  Localizer.getString( "Change" );
        }
        JButton okBtn = new JButton( buttonTxt );
        okBtn.addActionListener( new OkBtnActionListener() );
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 2;
            constraints.weightx = 1;
            constraints.insets = new Insets( 3, 3, 3, 3 );
            constraints.anchor = GridBagConstraints.NORTHEAST;
        btnPane.add( okBtn, constraints );
        okBtn.setDefaultCapable( true );
        getRootPane().setDefaultButton( okBtn );

        JButton cancelBtn = new JButton( Localizer.getString( "Cancel" ) );
        cancelBtn.addActionListener( new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    isDialogCanceled = true;
                    closeDialog();
                }
            } );
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 2;
            constraints.insets = new Insets( 3, 3, 3, 3 );
            constraints.anchor = GridBagConstraints.NORTHEAST;
        btnPane.add( cancelBtn, constraints );

        pack();
        pane.setMinimumSize( pane.getPreferredSize() );
        setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );
        setLocationRelativeTo( GUIRegistry.getInstance().getMainFrame() );
    }

    private boolean isInputValidAndUpdated()
    {
        final String localFilename = filenameTF.getText().trim();
        if ( localFilename.length() == 0 )
        {
            GUIUtils.showErrorMessage( this,
                Localizer.getString( "NoFileName" ) );
            filenameTF.requestFocus();
            return false;
        }

        final String researchTerm = researchTermTF.getText().trim();
        if ( researchTerm.length() < Cfg.MIN_SEARCH_TERM_LENGTH )
        {
            Object[] objArr = new Object[ 1 ];
            objArr[ 0 ] = new Integer( Cfg.MIN_SEARCH_TERM_LENGTH );
            GUIUtils.showErrorMessage( this,
                Localizer.getFormatedString( "MinSearchTerm", objArr ) );
            researchTermTF.requestFocus();
            return false;
        }

        if ( remoteFile != null )
        {
            SwarmingManager.getInstance().addFileToDownload( remoteFile,
                ServiceManager.sCfg.mDownloadDir + File.separator
                + FileUtils.convertToLocalSystemFilename( localFilename ),
                researchTerm );
            remoteFile.setInDownloadQueue( true );                    
        }
        else if ( downloadFile != null )
        {

            if ( !downloadFile.getDestinationFileName().equals( localFilename ) )
            {
                boolean rename = renameOldFileCBx.isSelected();
                try
                {
                    File destFile;
                    destFile = new File( ServiceManager.sCfg.mDownloadDir +
                        File.separator + FileUtils.convertToLocalSystemFilename(
                        localFilename ) );
                    downloadFile.setDestinationFile( destFile, rename );
                }
                catch ( FileHandlingException exp )
                {
                    // setting the new local filename failed. show error
                    if ( exp.getType() == FileHandlingException.FILE_ALREADY_EXISTS )
                    {
                        Object[] objArr = new Object[ 1 ];
                        objArr[ 0 ] = localFilename;
                        GUIUtils.showErrorMessage( this,
                            Localizer.getFormatedString( "FileAlreadyExists", objArr ) );
                    }
                    else if ( exp.getType() == FileHandlingException.RENAME_FAILED )
                    {
                        GUIUtils.showErrorMessage( this,
                            Localizer.getString( "FileRenameFailed" ) );
                    }
                    else
                    {
                        exp.printStackTrace();
                    }
                }
            }
            downloadFile.getResearchSetting().setSearchTerm( researchTerm );
        }
        return true;
    }

    private void closeDialogAfterDownload()
    {
        MainFrame frame = GUIRegistry.getInstance().getMainFrame();
        if ( remoteFile != null && switchToDownldCBx.isSelected() )
        {
            frame.setSelectedTab( MainFrame.DOWNLOAD_TAB_ID );
        }
        closeDialog();
    }

    private void closeDialog( )
    {
        setVisible(false);
        dispose();
    }
    
    private final class OkBtnActionListener implements ActionListener
    {
        public void actionPerformed( ActionEvent e )
        {
            Runnable runner = new Runnable()
            {
                public void run()
                {
                    try
                    {
                        if ( isInputValidAndUpdated() )
                        {
                            closeDialogAfterDownload();
                        }
                
                    }
                    catch ( Throwable th )
                    {
                        NLogger.error(NLoggerNames.USER_INTERFACE, th, th);
                    }
                }
            };
            ThreadPool.getInstance().addJob(runner, "DownloadConfigAction" );
        }
    }

    public boolean isDialogCanceled()
    {
        return isDialogCanceled;
    }
}