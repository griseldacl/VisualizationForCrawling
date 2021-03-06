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
 *  $Id: HTMLMultiLinePanel.java,v 1.4 2005/10/03 00:18:25 gregork Exp $
 */
package phex.gui.common;

import java.awt.*;

import javax.swing.*;
import javax.swing.text.html.*;

/**
 * This Panel provides the ability to show a multiline HTML label
 */
public class HTMLMultiLinePanel extends JEditorPane
{
    public HTMLMultiLinePanel(String text)
    {
        super();
        setEditable(false);
        setContentType("text/html");
        setText(text);

        // adjust the used style sheet to match the label style..
        setFontAndColor( UIManager.getFont("Label.font"),
            UIManager.getColor("Label.foreground") );
        setBackground(UIManager.getColor("Label.background"));
    }

    /**
     * Sets the default font and default color. These are set by
     * adding a rule for the body that specifies the font and color.
     * This allows the html to override these should it wish to have
     * a custom font or color.
     */
    private void setFontAndColor(Font font, Color fg)
    {
        StringBuffer rule = null;

        if (font != null)
        {
            rule = new StringBuffer("body { font-family: ");
            rule.append(font.getFamily());
            rule.append(";");
            rule.append(" font-size: ");
            rule.append(font.getSize());
            rule.append("pt");
            if (font.isBold())
            {
                rule.append("; font-weight: 700");
            }
            if (font.isItalic())
            {
                rule.append("; font-style: italic");
            }
        }
        if (fg != null)
        {
            if (rule == null)
            {
                rule = new StringBuffer("body { color: #");
            }
            else
            {
                rule.append("; color: #");
            }
            if (fg.getRed() < 16)
            {
                rule.append('0');
            }
            rule.append(Integer.toHexString(fg.getRed()));
            if (fg.getGreen() < 16)
            {
                rule.append('0');
            }
            rule.append(Integer.toHexString(fg.getGreen()));
            if (fg.getBlue() < 16)
            {
                rule.append('0');
            }
            rule.append(Integer.toHexString(fg.getBlue()));
        }
        if (rule != null)
        {
            rule.append(" }");
            try
            {
                StyleSheet style = ((HTMLDocument)getDocument()).getStyleSheet();                
                style.addRule(rule.toString());
            }
            catch (RuntimeException re)
            {
            }
        }
    }
}