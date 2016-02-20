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
 *  $Id: LinkLabel.java,v 1.6 2005/10/03 00:18:25 gregork Exp $
 */
package phex.gui.common;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.beans.PropertyChangeEvent;

import javax.swing.*;

import phex.gui.actions.FWAction;

/**
 * A simple Label that offers rollover effect and a action that is triggered when 
 * clicked on.
 */
public class LinkLabel extends JLabel implements MouseListener
{
    private boolean isFontBold;
    private boolean isRollover;
    private Action action;
    
    /**
     * Create a new link label.
     * @param text the text to display.
     * @param linkAction the action to trigger when clicked on.
     */
    public LinkLabel( FWAction linkAction )
    {
        super( (String)linkAction.getValue( FWAction.NAME ) );
        action = linkAction;
        registerActionChangeListener(action);
        setToolTipText( (String)action.getValue( FWAction.TOOL_TIP_TEXT ) );
        setEnabled( action.isEnabled() );
        isRollover = false;
        isFontBold = false;
        addMouseListener( this );
        setCursor( Cursor.getPredefinedCursor( Cursor.HAND_CURSOR ) );
    }
    
    public void setFontBold( boolean isBold )
    {
        isFontBold = isBold;
        
        Font currentFont = getFont();
        if ( currentFont.isBold() != isFontBold )
        {
            if ( isFontBold )
            {
                setFont( UIManager.getFont("Label.font").deriveFont( Font.BOLD ) );
            }
            else
            {
                setFont( UIManager.getFont("Label.font") );
            }
        }
    }
    
    public void initialize()
    {
        setFontBold( isFontBold );
    }
    
    public void updateUI()
    {
        super.updateUI();
        initialize();
    }
    
    public Color getForeground()
    {
        if ( isRollover )
        {
            return PhexColors.getLinkLabelRolloverForeground();
        }
        else
        {
            return super.getForeground();
        }
    }
    
    // TODO3 if btn needs to be removed... unregister!
    private void registerActionChangeListener( Action a )
    {
        PropertyChangeListener actionPropertyChangeListener = new ActionChangedListener();
        a.addPropertyChangeListener(actionPropertyChangeListener);
    }

    public void mouseEntered(MouseEvent e)
    {
        isRollover = true;
        repaint();
    }

    public void mouseExited(MouseEvent e)
    {
        isRollover = false;
        repaint();
    }

    public void mouseClicked(MouseEvent e)
    {
        if ( action != null )
        {
            action.actionPerformed( new ActionEvent( this, 0, "" ) );
        }
    }
    
    public void mousePressed(MouseEvent e)
    {}
    public void mouseReleased(MouseEvent e)
    {}
    
    private class ActionChangedListener implements PropertyChangeListener
    {
        ActionChangedListener( )
        {
            super();
        }

        public void propertyChange(PropertyChangeEvent e)
        {
            String propertyName = e.getPropertyName();
            if ( propertyName.equals(Action.NAME))
            {
                String text = (String) e.getNewValue();
                setText( text );
                repaint();
            }
            else if ( propertyName.equals("enabled"))
            {
                Boolean enabledState = (Boolean) e.getNewValue();
                setEnabled(enabledState.booleanValue());
                repaint();
            }
            else if ( propertyName.equals( FWAction.TOOL_TIP_TEXT ))
            {
                String text = (String) e.getNewValue();
                setToolTipText( text );
                repaint();
            }
        }
    }
}
