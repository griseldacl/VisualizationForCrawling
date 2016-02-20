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
 *  Created on 05.05.2005
 *  --- CVS Information ---
 *  $Id: MultiScopeProgressBar.java,v 1.9 2005/10/08 17:21:56 gregork Exp $
 */
package phex.gui.common;

import java.awt.*;
import java.util.Iterator;

import javax.swing.JPanel;
import javax.swing.UIManager;

import phex.download.DownloadScope;
import phex.download.DownloadScopeList;
import phex.download.swarming.SWDownloadCandidate;
import phex.download.swarming.SWDownloadFile;

/**
 *
 */
public class MultiScopeProgressBar extends JPanel 
{
    private long fileSize;
    public DownloadScopeList primaryScopeList;
    public DownloadScopeList secondaryScopeList;
    
    /**
     * 
     */
    public MultiScopeProgressBar( )
    {
        super( );
        setBackground( PhexColors.getScopeProgressBarBackground() );
        setForeground( PhexColors.getScopeProgressBarForeground() );
        setBorder( UIManager.getBorder( "ProgressBar.border" ) );
    }
    
    public void setDownloadFile( SWDownloadFile file )
    {
        if ( file == null )
        {
            this.fileSize = 0;
            this.primaryScopeList = null;
            this.secondaryScopeList = null;
        }
        else
        {
            this.fileSize = file.getTotalDataSize();
            this.primaryScopeList = file.getFinishedScopeList();
            this.secondaryScopeList = file.getBlockedScopeList();
        }
        repaint();
    }
    
    public void setCandidate( SWDownloadCandidate candidate )
    {
        this.fileSize = candidate.getDownloadFile().getTotalDataSize();
        this.primaryScopeList = candidate.getAvailableScopeList();
        repaint();
    }
    
    /* These rectangles/insets are allocated once for all 
     * paintComponent() calls.  Re-using rectangles rather than 
     * allocating them in each paint call substantially reduced the time
     * it took paint to run.  Obviously, this method can't be re-entered.
     */
    private static Rectangle viewRect = new Rectangle();
    
    protected void paintComponent( Graphics g )
    {
        // paint background.
        g.setColor( getBackground() );
        g.fillRect( 0, 0, getWidth(), getHeight() );
        
        Insets i = getInsets();
        viewRect.x = i.left;
        viewRect.y = i.top;
        viewRect.width = getWidth() - (i.right + viewRect.x);
        viewRect.height = getHeight() - (i.bottom + viewRect.y);
        
        // paint gradient
        Graphics2D g2 = (Graphics2D)g;
        
        if ( primaryScopeList != null )
        {
            paintScopeList(primaryScopeList, getForeground(), g2);
        }
        if ( secondaryScopeList != null )
        {
            paintScopeList(secondaryScopeList, 
                PhexColors.getScopeProgressBarSecondaryForeground(), g2);
        }
    }
    
    private void paintScopeList( DownloadScopeList scopeList, Color baseColor, Graphics2D g2 )
    {
        float valPerPixel = (fileSize-1) / viewRect.width;
        Iterator iterator = scopeList.getScopeIterator();
        Color useColor;
        while( iterator.hasNext() )
        {
            DownloadScope scope = (DownloadScope) iterator.next();
            float startPx = (float)scope.getStart() / valPerPixel;
            float endPx = (float)scope.getEnd() / valPerPixel;
            float width = endPx - startPx;
            int alpha = (int)Math.min( 245*width, 245 )+10;
            if ( alpha < 255 )
            {
                useColor = new Color( baseColor.getRed(), baseColor.getGreen(),
                    baseColor.getBlue(), alpha );
            }
            else
            {
                useColor = baseColor;
            }
            g2.setColor( useColor );
            int rectWidth = (int)Math.max( width, 1 );
            g2.fillRect(viewRect.x + (int)startPx, viewRect.y, rectWidth, viewRect.height);
        }
    }
}
