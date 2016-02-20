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
 *  $Id: GUIUtils.java,v 1.33 2005/10/08 17:21:56 gregork Exp $
 */
package phex.gui.common;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.text.Keymap;

import phex.utils.Localizer;
import phex.xml.XJBGUISettings;
import phex.xml.XJBGUITab;
import phex.xml.XJBGUITable;

public final class GUIUtils
{
    public static final Insets EMPTY_INSETS = new Insets( 0, 0, 0, 0 );
    
    private GUIUtils()
    {
    }
    
    /**
     * To ensure that a Keymap sticks with a ComboBoxEditor we need to do a
     * special handling. During a UI update the ComboBox replaces its editor.
     * The new editor will initialize its Keymap during its UI update. After this
     * UI update we are able to set our wanted Keymap on the ComboBoxEditor.
     * Therefore we add a special property change listener to UI updates of the 
     * ComboBox and the ComboBoxEditor. On the occuring UI change event of the
     * ComboBox (during which the editor was replaced), we reassign the property
     * change listener of the ComboBoxEditor. On the occuring UI change event
     * of the ComboBoxEditor (during which the Keymap was initialized) we set
     * our own wanted Keymap.
     * @param keymap the Keymap we like to force on the ComboBoxEditor.
     * @param comboBox the ComboBox with the ComboBoxEditor that should always
     *        use the given Keymap.
     */
    public static void assignKeymapToComboBoxEditor( Keymap keymap, JComboBox comboBox )
    {
        ComboBoxUIChangeListener listener = new ComboBoxUIChangeListener( keymap, comboBox );
        comboBox.addPropertyChangeListener( "UI", listener );
        ComboBoxEditor comboEditor = comboBox.getEditor();
        JTextField editor = ((JTextField)comboEditor.getEditorComponent());
        editor.addPropertyChangeListener( "UI", listener ); 
    }
    
    /**
     * The property change listener used to ensure a Keymap for a ComboBoxEditor
     * of a ComboBox.
     * @see #assignKeymapToComboBoxEditor()
     */
    private static class ComboBoxUIChangeListener implements PropertyChangeListener
    {
        private Keymap keymap;
        private JComboBox comboBox;
        
        public ComboBoxUIChangeListener( Keymap keymap, JComboBox comboBox )
        {
            this.keymap = keymap;
            this.comboBox = comboBox;
        }
        public void propertyChange(PropertyChangeEvent evt)
        {
            if ( !evt.getPropertyName().equals("UI") )
            {
                return;
            }
            
            if ( evt.getSource() == comboBox )
            {
                // during a UI update the comboBox editor changed...
                // reregister property change listener
                ComboBoxEditor comboEditor = comboBox.getEditor();
                JTextField editor = ((JTextField)comboEditor.getEditorComponent());
                editor.addPropertyChangeListener( "UI", this ); 
            }
            else
            {
                // this must be the editor of the comboBox that updates its keymap
                // and UI... reset the keymap...
                ComboBoxEditor comboEditor = comboBox.getEditor();
                JTextField editor = ((JTextField)comboEditor.getEditorComponent());
                editor.setKeymap( keymap );
            }
        }
    }
    
    /**
     * To ensure that a Keymap sticks with a ComboBoxEditor we need to do a
     * special handling. During a UI update the ComboBox replaces its editor.
     * The new editor will initialize its Keymap during its UI update. After this
     * UI update we are able to set our wanted Keymap on the ComboBoxEditor.
     * Therefore we add a special property change listener to UI updates of the 
     * ComboBox and the ComboBoxEditor. On the occuring UI change event of the
     * ComboBox (during which the editor was replaced), we reassign the property
     * change listener of the ComboBoxEditor. On the occuring UI change event
     * of the ComboBoxEditor (during which the Keymap was initialized) we set
     * our own wanted Keymap.
     * @param keymap the Keymap we like to force on the ComboBoxEditor.
     * @param comboBox the ComboBox with the ComboBoxEditor that should always
     *        use the given Keymap.
     */
    public static void assignKeymapToTextField( Keymap keymap, JTextField textField )
    {
        TextFieldUIChangeListener listener = new TextFieldUIChangeListener( keymap, textField );
        textField.addPropertyChangeListener( "UI", listener ); 
    }
    
    /**
     * The property change listener used to ensure a Keymap for a ComboBoxEditor
     * of a ComboBox.
     * @see #assignKeymapToComboBoxEditor()
     */
    private static class TextFieldUIChangeListener implements PropertyChangeListener
    {
        private Keymap keymap;
        private JTextField textField;
        
        public TextFieldUIChangeListener( Keymap keymap, JTextField textField )
        {
            this.keymap = keymap;
            this.textField = textField;
        }
        public void propertyChange(PropertyChangeEvent evt)
        {
            if ( !evt.getPropertyName().equals("UI") )
            {
                return;
            }
                        
            // this must be the editor of the comboBox that updates its keymap
            // and UI... reset the keymap...
            textField.setKeymap( keymap );
        }
    }

    /**
     * Sets the window location in the center relative to the location of
     * relativeWindow.
     */
    public static void setWindowLocationRelativeTo( Window window,
        Window relativeWindow )
    {
        Rectangle windowBounds = window.getBounds();
        Dimension rwSize = relativeWindow.getSize();
        Point rwLoc = relativeWindow.getLocation();

        int dx = rwLoc.x + (( rwSize.width - windowBounds.width ) >> 1 );
        int dy = rwLoc.y + (( rwSize.height - windowBounds.height ) >> 1 );
        Dimension ss = window.getToolkit().getScreenSize();

        if ( dy + windowBounds.height > ss.height)
        {
            dy = ss.height - windowBounds.height;
            dx = rwLoc.x < ( ss.width >> 1 ) ? rwLoc.x + rwSize.width :
                rwLoc.x - windowBounds.width;
        }
        if ( dx + windowBounds.width > ss.width )
        {
            dx = ss.width - windowBounds.width;
        }
        if (dx < 0)
        {
            dx = 0;
        }
        if (dy < 0)
        {
            dy = 0;
        }
        window.setLocation( dx, dy );
    }

    public static XJBGUITable getXJBGUITableByIdentifier( XJBGUISettings guiSettings,
        String tableIdentifier )
    {
        if ( guiSettings == null )
        {
            return null;
        }
        XJBGUITable xjbTable;
        Iterator iterator = guiSettings.getTableList().getTableList().iterator();
        while( iterator.hasNext() )
        {
            xjbTable = (XJBGUITable)iterator.next();
            if ( xjbTable.getTableIdentifier().equals( tableIdentifier ) )
            {
                return xjbTable;
            }
        }
        return null;
    }

    public static XJBGUITab getXJBGUITabById( XJBGUISettings guiSettings,
        int tabID )
    {
        if ( guiSettings == null )
        {
            return null;
        }
        XJBGUITab xjbTab;
        Iterator iterator = guiSettings.getTabList().iterator();
        while( iterator.hasNext() )
        {
            xjbTab = (XJBGUITab)iterator.next();
            if ( xjbTab.getTabID() == tabID )
            {
                return xjbTab;
            }
        }
        return null;
    }

    public static void adjustComboBoxHeight( JComboBox comboBox )
    {
        if ( comboBox == null )
        {
            return;
        }
        Font font = (Font) UIManager.getDefaults().get( "ComboBox.font" );
        if ( comboBox != null && font != null )
        {
            Dimension uiSize = comboBox.getUI().getPreferredSize( comboBox );
            FontMetrics fontMetrics = comboBox.getFontMetrics( font );
            int height = fontMetrics.getHeight() + fontMetrics.getDescent() + 3;
            comboBox.setPreferredSize( new Dimension( uiSize.width + 4, height ) );
        }
    }

    public static void adjustTableProgresssBarHeight( JTable table )
    {
        Font progressFont = (Font) UIManager.getDefaults().get( "ProgressBar.font" );
        FontMetrics fontMetrics = table.getFontMetrics( progressFont );
        // no descent used since numbers have no descent...
        int height = fontMetrics.getHeight() + fontMetrics.getDescent();
        table.setRowHeight( height );
    }

    public static void showErrorMessage( String message )
    {
        showErrorMessage( GUIRegistry.getInstance().getMainFrame(), message );
    }

    public static void showErrorMessage( String message, String title )
    {
        showErrorMessage( null, message, title );
    }

    public static void showErrorMessage( Component parent, String message )
    {
        showErrorMessage( parent, message, Localizer.getString( "Error" ) );
    }

    public static void showErrorMessage( Component parent, String message,
        String title )
    {
        if ( parent == null )
        {
            parent = GUIRegistry.getInstance().getMainFrame();
        }
        JOptionPane.showMessageDialog( parent, message,
            title, JOptionPane.ERROR_MESSAGE );
    }

    /**
    *    Center the window on the screen
    *
    *    @param win        The window object to position.
    *    @param offset    The amount to offset from the center of the screen.
    */
    public static void centerWindowOnScreen( Window win )
    {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension winSize = win.getSize();
        Rectangle rect = new Rectangle(
            (screenSize.width - winSize.width) / 2,
            (screenSize.height - winSize.height) / 2,
            winSize.width, winSize.height );
        win.setBounds(rect);
    }

    // Center Window on screen
    public static void centerAndSizeWindow( Window win, int fraction, int base)
    {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width  = screenSize.width * fraction / base;
        int height = screenSize.height * fraction / base;
    
        // for debug
        //width = 800;
        //height = 600;
    
        Rectangle rect = new Rectangle( (screenSize.width - width) / 2,
            (screenSize.height - height) / 2, width, height );
        win.setBounds(rect);
    }
    
    public static void updateComponentsUI()
    {
        PhexColors.updateColors();
        MainFrame frame = GUIRegistry.getInstance().getMainFrame();
        if ( frame == null )
        {
            return;
        }
        SwingUtilities.updateComponentTreeUI( frame );

        // go through child windows
        Window[] windows = frame.getOwnedWindows();
        for ( int j = 0; j < windows.length; j++ )
        {
            SwingUtilities.updateComponentTreeUI( windows[j] );
        }
    }
    
    /**
     * Creates a new <code>Color</code> that is a brighter version of this
     * <code>Color</code>. This method is the same implementation
     * java.awt.Color#brighter is usind except it has a configurable factor.
     * The java.awt.Color default facotr is 0.7
     * @return     a new <code>Color</code> object that is  
     *                 a brighter version of this <code>Color</code>.
     * @see        java.awt.Color#darker
     */
    public static Color brighterColor( Color color, double factor ) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        /* From 2D group:
         * 1. black.brighter() should return grey
         * 2. applying brighter to blue will always return blue, brighter
         * 3. non pure color (non zero rgb) will eventually return white
         */
        int i = (int)(1.0/(1.0-factor));
        if ( r == 0 && g == 0 && b == 0) {
           return new Color(i, i, i);
        }
        if ( r > 0 && r < i ) r = i;
        if ( g > 0 && g < i ) g = i;
        if ( b > 0 && b < i ) b = i;

        return new Color(Math.min((int)(r/factor), 255),
                         Math.min((int)(g/factor), 255),
                         Math.min((int)(b/factor), 255));
    }

    /**
     * Creates a new <code>Color</code> that is a darker version of this
     * <code>Color</code>. This method is the same implementation
     * java.awt.Color#darker is usind except it has a configurable factor.
     * The java.awt.Color default facotr is 0.7
     * @return  a new <code>Color</code> object that is 
     *                    a darker version of this <code>Color</code>.
     * @see        java.awt.Color#brighter
     */
    public static Color darkerColor( Color color, double factor )
    {
        return new Color(Math.max((int)(color.getRed()  * factor), 0), 
             Math.max((int)(color.getGreen()* factor), 0),
             Math.max((int)(color.getBlue() * factor), 0));
    }
    
    public static void showMainFrame()
    {
        GUIRegistry registry = GUIRegistry.getInstance();
        MainFrame mainFrame = registry.getMainFrame();
        mainFrame.setVisible(true);
        
        DesktopIndicator indicator = registry.getDesktopIndicator();
        if ( indicator != null )
        {
            indicator.hideIndicator();
        }
        if ( mainFrame.getState() != JFrame.NORMAL )
        {
            mainFrame.setState( Frame.NORMAL );
        }
        mainFrame.toFront();
        mainFrame.requestFocus();
    }
}