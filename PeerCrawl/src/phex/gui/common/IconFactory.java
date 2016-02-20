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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.*;
import java.util.*;

import javax.swing.*;

import phex.utils.*;

/**
 * This class is used to provide an easy way to define and get Icons
 * that can be shown in the gui.
 */
public class IconFactory
{
    private static final ImageIcon MISSING_IMAGE =
        new ImageIcon( IconFactory.class.getResource("/phex/gui/resources/image-failed.gif") );
    public static final ImageIcon EMPTY_IMAGE_16;
    
    static
    {
        BufferedImage bufImg = new BufferedImage( 16, 16, BufferedImage.TYPE_INT_ARGB );
        /*int[] rgbArray = new int[16*16];
        Arrays.fill( rgbArray, 0x00ff00 );
        bufImg.setRGB( 0, 0, 16, 16, rgbArray, 0, 16 );*/
        EMPTY_IMAGE_16 = new ImageIcon( bufImg );
    }

    /**
     * This table makes it possible that each icon is
     * only loaded once then it is stored in the table
     * then they can be fetched using the constant for it
     */
    private Hashtable table = new Hashtable();

    private ResourceBundle resource;

    public IconFactory( String resourceFileName )
    {
        resource = PropertyResourceBundle.getBundle( resourceFileName );
    }

    /**
     * the method to fetch an icon using the keys defined
     * in Images.properties file.
     */
    public Icon getIcon( String key )
    {
        TKIcon icon = (TKIcon) table.get( key );

        if ( icon == null )
        {  // if not in table instanciate it
            icon = new TKIcon( key );
            table.put( key, icon );
        }
        return icon;
    }

   /**
    * The class used to define an icon. It wrapps the functionality
    * of only loading a image when required.
    */
    class TKIcon extends ImageIcon
    {
        private String key;
        private boolean loaded;

        TKIcon( String aKey )
        {
            super();
            key = aKey;
            loaded = false;
        }

        /**
         * returns the image (overloaded from ImageIcon)
         */
        public Image getImage()
        {
            loadIcon();
            return super.getImage();
        }

        /**
         * paints the icon (overloaded from ImageIcon)
         * if its not already loaded then load it
         */
        public void paintIcon( Component c, Graphics g, int x, int y )
        {
            loadIcon();
            super.paintIcon( c, g, x, y );
        }

        /**
         * returns the width of an icon depending on it's offset
         * (overloaded from ImageIcon)
         */
        public int getIconWidth()
        {
            loadIcon();
            return super.getIconWidth();
        }

        /**
         * returns the width of an icon depending on it's offset
         * (overloaded from ImageIcon)
         */
        public int getIconHeight()
        {
            loadIcon();
            return super.getIconHeight();
        }

        /**
         * loads the icon with its path
         */
        private void loadIcon()
        {
            // watch out that icon is not loaded twice!
            if ( loaded )
            {
                return;
            }

            try
            {
                if ( key != null )
                {
                    String imgURLStr;
                    if ( key.startsWith( "Flag_" ) )
                    {// Flags are special located in flags direcotry.
                        imgURLStr = "/phex/gui/resources/flags/"
                            + key.substring(5).toLowerCase() + ".png";
                    }
                    else
                    {
                        imgURLStr = resource.getString( key );
                    }
                    
                    URL imgURL = null;
                    if ( imgURLStr != null )
                    {
                        imgURL = IconFactory.class.getResource( imgURLStr );
                    }
                    Image image = null;
                    if ( imgURL != null )
                    {
                        image = Toolkit.getDefaultToolkit().createImage( imgURL );
                    }
                    if ( image == null )
                    {
                        Logger.logMessage( Logger.WARNING, Logger.GUI,
                            "Can't find image for key: " + key + " URL: " + imgURLStr );
                        image = MISSING_IMAGE.getImage();
                    }
                    super.setImage( image );
                    loaded = true;
                }
            }
            catch ( Exception exp )
            {
                NLogger.error( NLoggerNames.USER_INTERFACE, exp, exp );
                // TODO integrate gui utilis
                //GUIUtilities.handleException( exp );
            }
        }
    }
}