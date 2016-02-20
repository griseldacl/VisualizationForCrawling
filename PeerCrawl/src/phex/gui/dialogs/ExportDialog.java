/* Created on 20.11.2004 */
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
 *  $Id: ExportDialog.java,v 1.9 2005/10/03 00:18:25 gregork Exp $
 */
package phex.gui.dialogs;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.lang.SystemUtils;

import phex.gui.common.DialogBanner;
import phex.gui.common.GUIRegistry;
import phex.share.ShareManager;
import phex.share.export.ExportEngine;
import phex.utils.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 *
 */
public class ExportDialog extends JDialog
{
    private JTextField customExportFormatTF;
    private JTextField outputFileTF;
    private JRadioButton defaultHTMLExport;
    private JRadioButton magmaYAMLHTMLExport;
    private JRadioButton rssXMLHTMLExport;
    private JRadioButton customExport;
    private JCheckBox magnetInclXs;

    /**
     * @throws java.awt.HeadlessException
     */
    public ExportDialog() throws HeadlessException
    {
        super( GUIRegistry.getInstance().getMainFrame(),
            Localizer.getString( "ExportDialog_DialogTitle" ), false );
        prepareComponent();
    }
    
    /**
     * 
     */
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
        CellConstraints cc = new CellConstraints();
        Container contentPane = getContentPane();
        contentPane.setLayout( new BorderLayout() );
        JPanel contentPanel = new JPanel();
        //JPanel contentPanel = new FormDebugPanel();
        contentPane.add( contentPanel, BorderLayout.CENTER );
        
        FormLayout layout = new FormLayout(
            "4dlu, d, 2dlu, d, 2dlu, d, 2dlu, fill:d:grow, 2dlu, d, 4dlu", // columns
            "p, 10dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p," + // 9 rows
            "10dlu, p, 3dlu, p," + // 4 rows
            "10dlu, p, 3dlu, p," + // 4 rows
            "10dlu, p, 3dlu, p 6dlu" ); // 5 row
        PanelBuilder builder = new PanelBuilder( layout, contentPanel );
        
        DialogBanner banner = new DialogBanner( Localizer.getString("ExportDialog_BannerHeader"),
            Localizer.getString("ExportDialog_BannerSubHeader") );
        builder.add( banner, cc.xywh( 1, 1, 9, 1 ));
        
        ExportTypeListener radioListener = new ExportTypeListener();
        builder.addSeparator( Localizer.getString( "ExportDialog_ExportFormat" ),
            cc.xywh( 2, 3, 7, 1 ) );
        defaultHTMLExport = new JRadioButton( Localizer.getString( "ExportDialog_DefaultHTMLExport" ) );
        defaultHTMLExport.setSelected( true );
        defaultHTMLExport.addActionListener(radioListener);
        magmaYAMLHTMLExport = new JRadioButton( Localizer.getString( "ExportDialog_MagmaYAMLExport" ) );
        magmaYAMLHTMLExport.addActionListener(radioListener);
        rssXMLHTMLExport = new JRadioButton( Localizer.getString( "ExportDialog_RSSXMLExport" ) );
        rssXMLHTMLExport.addActionListener(radioListener);
        customExport = new JRadioButton( Localizer.getString( "ExportDialog_CustomExportFormat" ) );
        ButtonGroup exportFormatGroup = new ButtonGroup();
        exportFormatGroup.add( defaultHTMLExport );
        exportFormatGroup.add( magmaYAMLHTMLExport );
        exportFormatGroup.add( rssXMLHTMLExport );
        exportFormatGroup.add( customExport );
        defaultHTMLExport.setSelected( true );
        builder.add( defaultHTMLExport, cc.xywh( 4, 5, 3, 1 ) );
        builder.add( magmaYAMLHTMLExport, cc.xywh( 4, 7, 3, 1 ) );
        builder.add( rssXMLHTMLExport, cc.xywh( 4, 9, 3, 1 ) );
        builder.add( customExport, cc.xy( 4, 11 ) );
        customExportFormatTF = new JTextField( 40 );
        builder.add( customExportFormatTF, cc.xywh( 6, 11, 1, 1 ));
        JButton browseCustomFormat = new JButton( Localizer.getString( "ExportDialog_Browse" ) );
        browseCustomFormat.addActionListener( new BrowseCustomFileBtnListener());
        builder.add( browseCustomFormat, cc.xy( 8, 11 ) );
        
        
        builder.addSeparator( Localizer.getString( "ExportDialog_Output" ),
            cc.xywh( 2, 13, 7, 1 ) );
        builder.addLabel( Localizer.getString( "ExportDialog_FileName" ), cc.xy( 4, 15, "right, center") );
        outputFileTF = new JTextField( 40 );
        File defOutFile = new File( SystemUtils.USER_HOME, "shared_files.html" );
        outputFileTF.setText( defOutFile.getAbsolutePath() );
        builder.add( outputFileTF, cc.xywh( 6, 15, 1, 1 ));
        JButton browseOutFile = new JButton( Localizer.getString( "ExportDialog_Browse" ) );
        browseOutFile.addActionListener( new BrowseOutFileBtnListener());
        builder.add( browseOutFile, cc.xy( 8, 15 ) );
        
        builder.addSeparator( Localizer.getString( "ExportDialog_Options" ),
            cc.xywh( 2, 17, 7, 1 ) );
        magnetInclXs = new JCheckBox( Localizer.getString( "ExportDialog_MagnetIncludeXS" ) );
        magnetInclXs.setToolTipText( Localizer.getString( "ExportDialog_TTTMagnetIncludeXS" ) );
        builder.add( magnetInclXs, cc.xywh( 4, 19, 3, 1 ) );
        
        builder.add( new JSeparator(), cc.xywh( 1, 21, 9, 1 ) );
        
        JButton cancelBtn = new JButton( Localizer.getString( "Cancel" ));
        cancelBtn.addActionListener( new CancelBtnListener() );
        JButton okBtn = new JButton( Localizer.getString( "OK" ) );
        okBtn.addActionListener( new OkBtnListener());
        JPanel btnPanel = ButtonBarFactory.buildOKCancelBar( okBtn, cancelBtn);
        builder.add( btnPanel, cc.xywh( 2, 23, 7, 1 ) );
        
        pack();
        setLocationRelativeTo( getParent() );
    }
    
    private void closeDialog( )
    {
        setVisible(false);
        dispose();
    }
    
    private void startExport()
    {
        String outFileName = outputFileTF.getText();
        File file = new File( outFileName );
        InputStream inStream = null;
        OutputStream outStream = null;
        try
        {
            outStream = new BufferedOutputStream( new FileOutputStream( file ) );
            if ( defaultHTMLExport.isSelected() )
            {
                inStream = ClassLoader.getSystemResourceAsStream(
                    "phex/resources/defaultSharedFilesHTMLExport.xsl" );
            }
            else if ( magmaYAMLHTMLExport.isSelected() )
            {
                inStream = ClassLoader.getSystemResourceAsStream(
                    "phex/resources/magmaSharedFilesYAMLExport.xsl" );
            }
            else if ( rssXMLHTMLExport.isSelected() )
            {
                inStream = ClassLoader.getSystemResourceAsStream(
                    "phex/resources/rssSharedFilesXMLExport.xsl" );
            }

            else if ( customExport.isSelected() )
            {
                String styleFileName = customExportFormatTF.getText();
                File styleFile = new File( styleFileName );
                inStream = new BufferedInputStream( new FileInputStream( styleFile ) );
            }
            else 
            {
                return;
            }
            
            Map exportOptions = null;
            if ( magnetInclXs.isSelected() )
            {
                exportOptions = new HashMap();
                exportOptions.put( ExportEngine.USE_MAGNET_URL_WITH_XS, "true" );
            }
            
            ShareManager.getInstance().exportSharedFiles(inStream, outStream, exportOptions );
        }
        catch ( IOException exp )
        {
            NLogger.error( NLoggerNames.USER_INTERFACE, exp, exp );
        }
        finally
        {
            IOUtil.closeQuietly(inStream);
            IOUtil.closeQuietly(outStream);
        }
    }
    
    private final class OkBtnListener implements ActionListener
    {
        public void actionPerformed( ActionEvent e )
        {
            try
            {
                startExport();
                closeDialog();
            }
            catch ( Throwable th )
            {
                NLogger.error( NLoggerNames.USER_INTERFACE, th, th );
            }
        }
    }

    private final class CancelBtnListener implements ActionListener
    {
        public void actionPerformed( ActionEvent e )
        {
            try
            {
                closeDialog();
            }
            catch ( Throwable th )
            {
                NLogger.error( NLoggerNames.USER_INTERFACE, th, th );
            }
        }
    }
    
    private final class BrowseOutFileBtnListener implements ActionListener
    {
        public void actionPerformed( ActionEvent e )
        {
            try
            {
                if ( SystemUtils.IS_OS_MAC_OSX )
                {
                    FileDialog dia = new FileDialog( GUIRegistry.getInstance().getMainFrame(),
                        Localizer.getString( "ExportDialog_SelectOutputFile" ), FileDialog.SAVE );
                    dia.show();
                    String filename = dia.getDirectory() + dia.getFile();
                    filename = adjustFileExtension( filename );
                    outputFileTF.setText( filename );
                }
                else
                {
                    JFileChooser chooser = new JFileChooser( );
                    chooser.setDialogTitle(Localizer.getString( "ExportDialog_SelectOutputFile" ));
                    chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
                    chooser.setMultiSelectionEnabled(false);
                    int rc = chooser.showSaveDialog( ExportDialog.this );
                    if ( rc == JFileChooser.APPROVE_OPTION )
                    {
                        File file = chooser.getSelectedFile();
                        String filename = adjustFileExtension( file.getAbsolutePath() );
                        outputFileTF.setText( filename );
                    }
                }
            }
            catch ( Throwable th )
            {
                NLogger.error( NLoggerNames.USER_INTERFACE, th, th );
            }
        }

        /**
         * @param absolutePath
         * @return
         */
        private String adjustFileExtension(String filename)
        {
            String ext = FileUtils.getFileExtension(filename);
            if ( defaultHTMLExport.isSelected() )
            {
                if ( !(ext.equals("htm") || ext.equals("html")) )
                {
                    filename = filename + ".html";
                }
            }
            else if ( magmaYAMLHTMLExport.isSelected() )
            {
                if ( !ext.equals("magma") )
                {
                    filename = filename + ".magma";
                }
            }
            else if ( rssXMLHTMLExport.isSelected() )
            {
                if ( !ext.equals("xml") )
                {
                    filename = filename + ".rss.xml";
                }
            }
            return filename;
        }
    }
    
    private final class BrowseCustomFileBtnListener implements ActionListener
    {
        public void actionPerformed( ActionEvent e )
        {
            try
            {
                customExport.setSelected(true);
                if ( SystemUtils.IS_OS_MAC_OSX )
                {
                    FileDialog dia = new FileDialog( GUIRegistry.getInstance().getMainFrame(),
                        Localizer.getString( "ExportDialog_SelectCustomStyleFile" ), FileDialog.LOAD );
                    dia.show();
                    customExportFormatTF.setText( dia.getDirectory() + dia.getFile() );
                }
                else
                {
                    JFileChooser chooser = new JFileChooser( );
                    chooser.setDialogTitle(Localizer.getString( "ExportDialog_SelectCustomStyleFile" ));
                    chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
                    chooser.setMultiSelectionEnabled(false);
                    chooser.setFileFilter( new FileFilter() {
                        public boolean accept(File file)
                        {
                            return file.isDirectory() || FileUtils.getFileExtension(file).equalsIgnoreCase("XSL");
                        }
    
                        public String getDescription()
                        {
                            return "XSL-Stylesheet";
                        }} );
                    int rc = chooser.showOpenDialog( ExportDialog.this );
                    if ( rc == JFileChooser.APPROVE_OPTION )
                    {
                        File file = chooser.getSelectedFile();
                        customExportFormatTF.setText( file.getAbsolutePath() );
                    }
                }
            }
            catch ( Throwable th )
            {
                NLogger.error( NLoggerNames.USER_INTERFACE, th, th );
            }
        }
    }
    
    private final class ExportTypeListener implements ActionListener
    {
        public void actionPerformed( ActionEvent e )
        {
            try
            {
                String filename = outputFileTF.getText( );
                String ext = FileUtils.getFileExtension(filename);
                if ( defaultHTMLExport.isSelected() )
                {
                    if ( !(ext.equals("htm") || ext.equals("html")) )
                    {
                        filename = FileUtils.replaceFileExtension( filename, "html" );
                    }
                }
                else if ( magmaYAMLHTMLExport.isSelected() )
                {
                    if ( !ext.equals("magma") )
                    {
                        filename = FileUtils.replaceFileExtension( filename, "magma" );
                    }
                }
                else if ( rssXMLHTMLExport.isSelected() )
                {
                    if ( !ext.equals("xml") )
                    {
                        filename = FileUtils.replaceFileExtension( filename, "xml" );
                    }
                }
                outputFileTF.setText( filename );
            }
            catch ( Throwable th )
            {
                NLogger.error( NLoggerNames.USER_INTERFACE, th, th );
            }
        }
    }
}
