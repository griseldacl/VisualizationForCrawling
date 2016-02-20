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
 *  $Id: LookAndFeelUtils.java,v 1.15 2005/10/03 00:18:25 gregork Exp $
 */
package phex.gui.common;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import org.apache.commons.lang.SystemUtils;

import phex.common.Environment;
import phex.utils.ClassUtils;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

import com.jgoodies.looks.LookUtils;
import com.jgoodies.looks.Options;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticTheme;


/**
 * This class holds a collection of Themes with there associated LaF and overs
 * retrival methods.
 * 
 * @author gkoukkoullis
 */
public class LookAndFeelUtils
{
    private static ThemeInfo[] plasticThemes;
    
    public static UIManager.LookAndFeelInfo[] getAvailableLAFs()
    {
        List list = new ArrayList();
        
        if ( SystemUtils.IS_OS_MAC_OSX )
        {
            list.add( new UIManager.LookAndFeelInfo
                ("Macintosh", UIManager.getSystemLookAndFeelClassName() ) );
        }
        
        list.add( new UIManager.LookAndFeelInfo(
            "PlasticXP (default)", Options.PLASTICXP_NAME ) );
        
        list.add( new UIManager.LookAndFeelInfo(
            "Metal", "javax.swing.plaf.metal.MetalLookAndFeel") );
        //list.add( new UIManager.LookAndFeelInfo(
        //    "CDE/Motif", Options.EXT_MOTIF_NAME ) );
            
        if ( SystemUtils.IS_OS_WINDOWS )
        {
            // This LAF will use the Java 1.4.2 avaiable XP look on XP systems
            list.add( new UIManager.LookAndFeelInfo(
                "Windows", "com.sun.java.swing.plaf.windows.WindowsLookAndFeel") );
        }
        
        
        
        // The Java 1.4.2 available GTK+ LAF seems to be buggy and is not working
        // correctly together with the Swing UIDefault constants. Therefore we need
        // to wait with support of it
        
        Class gtkLAFClass;
        try
        {
            gtkLAFClass =
                Class.forName("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
        }
        catch (ClassNotFoundException e)
        {
            gtkLAFClass = null;
        }
        if ( gtkLAFClass != null )
        {
            list.add( new UIManager.LookAndFeelInfo(
                "GTK", "com.sun.java.swing.plaf.gtk.GTKLookAndFeel") );
        }
        
        UIManager.LookAndFeelInfo[] lafs = new UIManager.LookAndFeelInfo[ list.size() ];
        list.toArray( lafs );
        return lafs;
    }

    /**
     * @param lafClassName
     */
    public static ThemeInfo[] getAvailableThemes(String lafClassName)
    {
        if ( lafClassName.equals( Options.PLASTICXP_NAME ) )
        {
            initPlasticThemes();
            return plasticThemes;
        }
        return new ThemeInfo[0];
    }
    
    /**
     * @param lafClassName
     */
    public static ThemeInfo getCurrentTheme( String lafClassName )
    {
        if ( Options.PLASTICXP_NAME.equals( lafClassName ) )
        {
            PlasticTheme myCurrentTheme = PlasticLookAndFeel.getMyCurrentTheme();
            if ( myCurrentTheme == null )
            {
                return null;
            }
            Class clazz = myCurrentTheme.getClass();
            String name = clazz.getName();
            return new ThemeInfo( name, name );
        }
        return null;
    }
    
    public static void setCurrentTheme( String lafClassName, Object theme )
    {
        if ( lafClassName.equals( Options.PLASTICXP_NAME ) )
        {
            PlasticLookAndFeel.setMyCurrentTheme( (PlasticTheme)theme );
            try
            {
                // after setting the theme we must reset the PlasticLAF
                UIManager.setLookAndFeel( UIManager.getLookAndFeel() );
            }
            catch ( UnsupportedLookAndFeelException exp )
            {// this is not expected to happen since we reset a existing LAF
                NLogger.error( NLoggerNames.USER_INTERFACE, exp, exp );
            }
        }
        GUIUtils.updateComponentsUI();
    }
    
    /**
     * Determines the LAF to use for Phex. The LAF is supported and available.
     * It takes three steps:
     * First the given defaultClass is tried.
     * Second the Phex default LAF is tried.
     * Third the Java default is used.
     * @param defaultClass the default class to try.
     * @return the LAF class to use.
     */
    public static LookAndFeel determineLAF( String defaultClass )
    {
        String lafClass = defaultClass;

        // first.. try the requested LAF
        LookAndFeel laf = (LookAndFeel)ClassUtils.newInstanceQuitly( 
            ClassUtils.classForNameQuitly(lafClass) );
        if ( laf != null && laf.isSupportedLookAndFeel() )
        {
            return laf;
        }
        
        // second.. try the Phex default LAF
        lafClass = getDefaultLAFClassName();
        laf = (LookAndFeel)ClassUtils.newInstanceQuitly( 
            ClassUtils.classForNameQuitly(lafClass) );
        if ( laf != null && laf.isSupportedLookAndFeel() )
        {
            return laf;
        }
        
        // third.. try the Swing default LAF
        lafClass = UIManager.getCrossPlatformLookAndFeelClassName();
        laf = (LookAndFeel)ClassUtils.newInstanceQuitly( 
            ClassUtils.classForNameQuitly(lafClass) );
        return laf;
    }
    
    /**
     * Returns the plattform default LAF class name
     */
    private static String getDefaultLAFClassName()
    {
        if( Environment.getInstance().isMacOSX())
        {
            // set the look and feel to System
            return UIManager.getSystemLookAndFeelClassName();
        }
        else
        {
            // set the look and feel to Metal
            //lafClass = UIManager.getCrossPlatformLookAndFeelClassName();
            return Options.PLASTICXP_NAME;
        }
    }

    /**
     * Sets the look and feel with the given class name.
     * @param className the class name of the look and feel to set
     * @throws LookAndFeelFailedException
     */
    public static void setLookAndFeel( String className ) throws LookAndFeelFailedException
    {
        try
        {
            Class lnfClass = Class.forName( className );
            setLookAndFeel( ( LookAndFeel ) ( lnfClass.newInstance() ) );
        }
        catch ( ClassNotFoundException exp )
        {
            NLogger.error( NLoggerNames.USER_INTERFACE, 
                "Class not found: " + className, exp );
            JOptionPane.showMessageDialog( 
                GUIRegistry.getInstance().getMainFrame(),
                "Error loading Look & Feel " + exp, "Error", 
                JOptionPane.ERROR_MESSAGE );
            throw new LookAndFeelFailedException( "Class not found: " + className );
        }
        catch ( IllegalAccessException exp )
        {
            NLogger.error( NLoggerNames.USER_INTERFACE, 
                "Illegal access: " + className, exp );
            JOptionPane.showMessageDialog( 
                GUIRegistry.getInstance().getMainFrame(),
                "Error loading Look & Feel " + exp, "Error", 
                JOptionPane.ERROR_MESSAGE );
            throw new LookAndFeelFailedException( "Illegal access: " + className );
        }
        catch ( InstantiationException exp )
        {
            NLogger.error( NLoggerNames.USER_INTERFACE, 
                "Instantiation failed: " + className, exp );
            JOptionPane.showMessageDialog( 
                GUIRegistry.getInstance().getMainFrame(),
                "Error loading Look & Feel " + exp, "Error", 
                JOptionPane.ERROR_MESSAGE );
            throw new LookAndFeelFailedException( "Instantiation faield: " + className );
        }
        catch ( Throwable th )
        {
            NLogger.error( NLoggerNames.USER_INTERFACE, 
                "Error loading LAF: " + className, th );
            JOptionPane.showMessageDialog( 
                GUIRegistry.getInstance().getMainFrame(),
                "Error loading Look & Feel " + th, "Error", 
                JOptionPane.ERROR_MESSAGE );
        }
    }
    
    public static void setLookAndFeel( LookAndFeel laf ) 
        throws LookAndFeelFailedException
    {
        try
        {
            // don't update LAF if already set...
            if ( laf.getID().equals( UIManager.getLookAndFeel().getID() ) )
            {
                return;
            }
            UIManager.setLookAndFeel( laf );
            GUIUtils.updateComponentsUI();
        }
        catch ( UnsupportedLookAndFeelException exp )
        {
            NLogger.error( NLoggerNames.USER_INTERFACE, 
                "Instantiation faield: " + laf.getName(), exp );
            throw new LookAndFeelFailedException( "Instantiation faield: " + laf.getName() );
        }
    }
    
    private static void initPlasticThemes()
    {
        if ( plasticThemes == null )
        {
            String classPrefix = "com.jgoodies.looks.plastic.theme.";
            plasticThemes = new ThemeInfo[18];
            plasticThemes[0] = new ThemeInfo( "BrownSugar", classPrefix + "BrownSugar" );
            plasticThemes[1] = new ThemeInfo( "DarkStar", classPrefix + "DarkStar" );
            plasticThemes[2] = new ThemeInfo( "DesertBlue", classPrefix + "DesertBlue" );
            if ( !LookUtils.IS_LAF_WINDOWS_XP_ENABLED && LookUtils.IS_OS_WINDOWS_MODERN )
            {
                plasticThemes[3] = new ThemeInfo( "DesertBluer (default)",
                    classPrefix + "DesertBluer" );
            }
            else
            {
                plasticThemes[3] = new ThemeInfo( "DesertBluer", classPrefix + "DesertBluer" );
            }
            plasticThemes[4] = new ThemeInfo( "DesertGreen", classPrefix + "DesertGreen" );
            plasticThemes[5] = new ThemeInfo( "DesertRed", classPrefix + "DesertRed" );
            plasticThemes[6] = new ThemeInfo( "DesertYellow", classPrefix + "DesertYellow" );
            if ( LookUtils.IS_LAF_WINDOWS_XP_ENABLED )
            {
                plasticThemes[7] = new ThemeInfo( "ExperienceBlue (default)",
                    classPrefix + "ExperienceBlue" );
            }
            else
            {
                plasticThemes[7] = new ThemeInfo( "ExperienceBlue",
                    classPrefix + "ExperienceBlue" );
            }
            plasticThemes[8] = new ThemeInfo( "ExperienceGreen", classPrefix + "ExperienceGreen" );
            plasticThemes[9] = new ThemeInfo( "Silver", classPrefix + "Silver" );
            
            if ( !LookUtils.IS_OS_WINDOWS_XP && !LookUtils.IS_OS_WINDOWS_MODERN )
            {
                plasticThemes[10] = new ThemeInfo( "SkyBlue (default)",
                    classPrefix + "SkyBlue" );
            }
            else
            {
                plasticThemes[10] = new ThemeInfo( "SkyBlue", classPrefix + "SkyBlue" );
            }
            plasticThemes[11] = new ThemeInfo( "SkyBluer", classPrefix + "SkyBluer" );
            plasticThemes[12] = new ThemeInfo( "SkyBluerTahoma", classPrefix + "SkyBluerTahoma" );
            plasticThemes[13] = new ThemeInfo( "SkyGreen", classPrefix + "SkyGreen" );
            plasticThemes[14] = new ThemeInfo( "SkyKrupp", classPrefix + "SkyKrupp" );
            plasticThemes[15] = new ThemeInfo( "SkyPink", classPrefix + "SkyPink" );
            plasticThemes[16] = new ThemeInfo( "SkyRed", classPrefix + "SkyRed" );
            plasticThemes[17] = new ThemeInfo( "SkyYellow", classPrefix + "SkyYellow" );
        }
    }
    
    public static class ThemeInfo
    {
        private String name;
        private String className;
        
        public ThemeInfo( String name, String className )
        {
            this.name = name;
            this.className = className;
        }
        
        /**
         * @return
         */
        public String getClassName()
        {
            return className;
        }

        /**
         * @return
         */
        public String getName()
        {
            return name;
        }
        
        public boolean equals( Object o )
        {
            if ( !(o instanceof ThemeInfo ) )
            {
                return false;
            }
            
            return className.equals( ((ThemeInfo)o).className );
            
        }
        
    }
}
