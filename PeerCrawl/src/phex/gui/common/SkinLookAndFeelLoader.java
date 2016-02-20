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
package phex.gui.common;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import javax.swing.UIManager;

import phex.utils.NLogger;
import phex.utils.NLoggerNames;

/**
 * The class is responsible to try to dynamicly load the skin look and feel if it
 * is available. No imports of any skin look and feel class is necessary and should
 * be used to avoid problems for users that dont like to download the skin laf
 * library.
 * 
 * @deprecated This class is not used in Phex anymore.. Skin look and feel
 * turns out to be a bad solution.
 */
public class SkinLookAndFeelLoader
{
    private static boolean isSkinLAFInstalled = false;
    
    public static void tryLoadingSkinLookAndFeel()
    {
        try
        {
            Class skinLAFClass = Class.forName( "com.l2fprod.gui.plaf.skin.SkinLookAndFeel" );
            Class skinClass = Class.forName( "com.l2fprod.gui.plaf.skin.Skin" );
            Class[] classArgs = new Class[] { URL.class };
            Method loadThemePackMethod = skinLAFClass.getDeclaredMethod( "loadThemePack", classArgs );
            classArgs = new Class[] { skinClass };
            Method setSkinMethod = skinLAFClass.getDeclaredMethod( "setSkin", classArgs );

            // try to load the aqua theme pack and the standard theme pack
            URL aquathemepack = ClassLoader.getSystemClassLoader().getResource(
                "aquathemepack.zip" );
            Object[] objArr = new Object[] { aquathemepack };
            Object skin = loadThemePackMethod.invoke( null, objArr );
            objArr = new Object[] { skin };
            setSkinMethod.invoke( null, objArr );
        }
        catch ( ClassNotFoundException exp )
        {// no skin look and feel installed.
            return;
        }
        catch ( NoSuchMethodException exp )
        {
            NLogger.error( NLoggerNames.USER_INTERFACE, exp, exp );
            return;
        }
        catch ( IllegalAccessException exp )
        {
            NLogger.error( NLoggerNames.USER_INTERFACE, exp, exp );
            return;
        }
        catch ( InvocationTargetException exp )
        {
            NLogger.error( NLoggerNames.USER_INTERFACE, exp, exp );
            return;
        }


        UIManager.LookAndFeelInfo[] ilafs = UIManager.getInstalledLookAndFeels();
        UIManager.LookAndFeelInfo[] rv = new UIManager.LookAndFeelInfo[ ilafs.length+1 ];
        System.arraycopy(ilafs, 0, rv, 0, ilafs.length);
        UIManager.LookAndFeelInfo moo = rv[ ilafs.length - 1 ];
        rv[ilafs.length-1] = new UIManager.LookAndFeelInfo( "Aqua",
            "com.l2fprod.gui.plaf.skin.SkinLookAndFeel");
        rv[ilafs.length] = moo;
        UIManager.setInstalledLookAndFeels(rv);
        
        isSkinLAFInstalled = true;

        /// Skin LAF START ( original code from Jason Winzenried - MamiyaOtaru
        /*try
        {
            // ** jay from here gotta set the skin before the UI is updated
            Skin skin = null;
            //skin = new Skin(SkinLookAndFeel.loadSkin("themes/aquathemepack.zip")); // ** jay
            skin = SkinLookAndFeel.loadThemePack("themes" + File.separator + "aquathemepack.zip");
            //skin = SkinLookAndFeel.loadThemePack("themes" + File.separator + "xplunathemepack.zip");
            //skin = SkinLookAndFeel.loadThemePack("themes" + File.separator + "themepack.zip");
            SkinLookAndFeel.setSkin(skin);
        }
        catch (Exception e)
        {
            Logger.logError( e );
        }*/


        /// Skin LAF END
    }
    
    public static boolean isSkinLAFInstalled()
    {
        return isSkinLAFInstalled;
    }
}