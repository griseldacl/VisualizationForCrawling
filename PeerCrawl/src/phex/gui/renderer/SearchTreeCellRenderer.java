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
 *  $Id: SearchTreeCellRenderer.java,v 1.9 2005/10/03 00:18:26 gregork Exp $
 */
package phex.gui.renderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.font.LineMetrics;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import phex.common.URN;
import phex.download.RemoteFile;
import phex.download.swarming.SwarmingManager;
import phex.gui.tabs.search.SearchResultElement;
import phex.share.ShareManager;
import phex.share.SharedFilesService;

/**
 * 
 */
public class SearchTreeCellRenderer extends DefaultTreeCellRenderer
{
    private static final Color DOWNLOAD_COLOR = new Color( 0x00, 0x7F, 0x00 );
    private static final Color SHARE_COLOR = Color.lightGray;
    
    private SharedFilesService sharedFilesService;
    private SwarmingManager swarmingMgr;
    
    private JTree tree;
    
    public SearchTreeCellRenderer()
    {
        sharedFilesService = ShareManager.getInstance().getSharedFilesService();
        swarmingMgr = SwarmingManager.getInstance();
        setClosedIcon( new LabeledIcon( getClosedIcon(), null ) );
    }

    /**
     * Gets the font of this component.
     * @return this component's font; if a font has not been set
     * for this component, the font of its parent is returned
     */
    public Font getFont()
    {
        Font font = super.getFont();
        if (font == null && tree != null)
        {
            // Strive to return a non-null value, otherwise the html support
            // will typically pick up the wrong font in certain situations.
            font = tree.getFont();
        }
        return font;
    }

    /**
      * Configures the renderer based on the passed in components.
      * The value is set from messaging the tree with
      * <code>convertValueToText</code>, which ultimately invokes
      * <code>toString</code> on <code>value</code>.
      * The foreground color is set based on the selection and the icon
      * is set based on on leaf and expanded.
      */
    public Component getTreeCellRendererComponent( JTree tree, Object value,
        boolean sel, boolean expanded, boolean leaf, int row,
        boolean hasFocus )
    {
        if (sel)
            setForeground(getTextSelectionColor());
        else
            setForeground(getTextNonSelectionColor());
            
        String stringValue = convertValueToText(value, sel, expanded, leaf, row, hasFocus);

        this.tree = tree;
        this.hasFocus = hasFocus;
        setText(stringValue);
        
        // There needs to be a way to specify disabled icons.
        if (!tree.isEnabled())
        {
            setEnabled(false);
            if (leaf)
            {
                setDisabledIcon(getLeafIcon());
            }
            else if (expanded)
            {
                setDisabledIcon(getOpenIcon());
            }
            else
            {
                setDisabledIcon(getClosedIcon());
            }
        }
        else
        {
            setEnabled(true);
            if (leaf)
            {
                setIcon(getLeafIcon());
            }
            else if (expanded)
            {
                setIcon(getOpenIcon());
            }
            else
            {
                LabeledIcon icon = (LabeledIcon) getClosedIcon();
                if ( value instanceof SearchResultElement )
                {
                    int count = ((SearchResultElement)value).getRemoteFileListCount();
                    icon.setLabel( String.valueOf( count ) );
                }
                else
                {
                    icon.setLabel(null);
                }
                setIcon( icon );
            }
        }
        setComponentOrientation(tree.getComponentOrientation());

        selected = sel;

        return this;
    }
    
    /**
     * Called by the renderers to convert the specified value to
     * text. This implementation returns <code>value.toString</code>, ignoring
     * all other arguments. To control the conversion, subclass this 
     * method and use any of the arguments you need.
     * 
     * @param value the <code>Object</code> to convert to text
     * @param selected true if the node is selected
     * @param expanded true if the node is expanded
     * @param leaf  true if the node is a leaf node
     * @param row  an integer specifying the node's display row, where 0 is 
     *             the first row in the display
     * @param hasFocus true if the node has the focus
     * @return the <code>String</code> representation of the node's value
     */
    public String convertValueToText(Object value, boolean selected,
                                     boolean expanded, boolean leaf, int row,
                                     boolean hasFocus)
    {
        RemoteFile remoteFile; 
        if ( value instanceof SearchResultElement )
        {
            remoteFile = ((SearchResultElement)value).getSingleRemoteFile();
        }
        else if ( value instanceof RemoteFile )
        {
            remoteFile = (RemoteFile)value;
        }
        else
        {
            remoteFile = null;
        }
        if(remoteFile == null)
        {
            return "";
        }
        
        // adjust component colors
        URN urn = remoteFile.getURN();
        boolean isShared = sharedFilesService.isURNShared( urn );
        if ( isShared )
        {
            setForeground( SHARE_COLOR );
        }
        else
        {
            boolean isDownloaded = swarmingMgr.isURNDownloaded( urn );
            if ( isDownloaded )
            {
                setForeground( DOWNLOAD_COLOR );
            }
        }
        
        return remoteFile.getDisplayName();
    }
    
    public class LabeledIcon implements Icon
    {
        private Icon delegate;
        private String label;
        
        public LabeledIcon( Icon delegate, String label )
        {
            this.delegate = delegate;
            this.label = label;
        }
        
        public void setLabel( String label )
        {
            this.label = label;
        }

        /**
         * @see javax.swing.Icon#getIconHeight()
         */
        public int getIconHeight()
        {
            return delegate.getIconHeight();
        }

        /**
         * @see javax.swing.Icon#getIconWidth()
         */
        public int getIconWidth()
        {
            return delegate.getIconWidth();
        }

        /**
         * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
         */
        public void paintIcon(Component c, Graphics g, int x, int y)
        {
            delegate.paintIcon( c, g, x, y );
            if ( label != null )
            {
                Font originalFont = g.getFont();
                
                Font font = g.getFont();
                font = font.deriveFont( font.getSize2D() - 4 );
                g.setFont( font );
                
                FontMetrics metrics = g.getFontMetrics();
                LineMetrics lineMetrics = metrics.getLineMetrics(label, g);
                //g.setColor(Color.RED);
                g.drawString( label, 
                    (int)(x + delegate.getIconWidth() / 2
                    - metrics.stringWidth( label ) / 2 - 1), 
                    (int)(y + delegate.getIconHeight() / 2 
                    + lineMetrics.getHeight() / 2 - 1) );
                
                g.setFont( originalFont );
            }
        }
    }
}
