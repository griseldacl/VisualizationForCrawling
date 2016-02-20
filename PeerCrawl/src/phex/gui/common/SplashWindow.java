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
 *  $Id: SplashWindow.java,v 1.8 2005/10/03 00:18:25 gregork Exp $
 */
package phex.gui.common;

import java.awt.*;

import javax.swing.*;

public class SplashWindow extends JWindow
{
    public static final String SPLASH_IMAGE_NAME = "/phex/resources/splash.jpg";

    private Image image;

    public SplashWindow()
    {
        super( );
       
        ImageIcon icon = new ImageIcon( SplashWindow.class.getResource(
            SPLASH_IMAGE_NAME ) );
        image = icon.getImage();

        setSize( image.getWidth( null ) + 4, image.getHeight( null ) + 4 );
        GUIUtils.centerWindowOnScreen( this );
    }

    public void showSplash()
    {
        setVisible( true );
        repaint();
        toFront();
    }

    public void paint( Graphics g )
    {
        g.drawImage( image, 2, 2, null );
        g.setColor( Color.white );
        g.drawLine( 0, 0, 0, image.getHeight( null ) + 3 );
        g.drawLine( 0, 0, image.getWidth( null ) + 3, 0 );
        g.setColor( Color.lightGray );
        g.drawLine( 1, 1, 1, image.getHeight( null ) + 2 );
        g.drawLine( 1, 1, image.getWidth( null ) + 2, 1 );


        g.setColor( Color.black );
        g.drawLine( 0, image.getHeight( null ) + 3,
            image.getWidth( null ) + 3, image.getHeight( null ) + 3 );
        g.drawLine( image.getWidth( null ) + 3, 0,
            image.getWidth( null ) + 3, image.getHeight( null ) + 3 );

        g.setColor( Color.darkGray );
        g.drawLine( 1, image.getHeight( null ) + 2,
            image.getWidth( null ) + 2, image.getHeight( null ) + 2 );
        g.drawLine( image.getWidth( null ) + 2, 1,
            image.getWidth( null ) + 2, image.getHeight( null ) + 2 );

/*
        g.setColor( Color.lightGray );
        g.drawRect( 0, 0, image.getWidth( null ) + 3, image.getHeight( null ) + 3 );
        g.setColor( Color.darkGray );
        g.drawRect( 1, 1, image.getWidth( null ) + 1, image.getHeight( null ) + 1 );
            */
    }    
}
