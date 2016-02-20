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
 *  Created on 16.09.2005
 *  --- CVS Information ---
 *  $Id: DownloadScope.java,v 1.3 2005/09/20 00:16:59 gregork Exp $
 */
package phex.download;

/**
 * A DownloadScope represents a area inside a download file.
 * Limited by a start and end offset.
 */
public class DownloadScope
{
    /**
     * The start offset of the download scope, inclusive.
     * Expected: start <= end
     */
    private long start;
    
    /**
     * The end offset of the download scope, inclusive.
     * Expected: start <= end
     */
    private long end;
    
    public DownloadScope( long startOffset, long endOffset )
    {
        if ( endOffset < startOffset )
        {
            throw new IllegalArgumentException("endOffset < startOffset : " 
                + startOffset + " " + endOffset );
        }
        this.start = startOffset;
        this.end = endOffset;
    }
    
    public long getEnd()
    {
        return end;
    }

    public long getStart()
    {
        return start;
    }
    
    public long getLength()
    {
        return end-start + 1;
    }

    /**
     * scope:  |--|    |--|      |--|
     * this: |------|  |----|  |----|
     * @param scope
     * @return
     */
    public boolean contains( DownloadScope scope )
    {
        return start <= scope.start && end >= scope.end;
    }
    
    /**
     * scope: |--|      |--|
     * this:    |--|       |--|
     * Determines if the given scope is a neighbor before this segment.
     * A neighbor either directly precedes this segment or precedes and 
     * overlapps this segment partially.  
     * @param scope
     * @return
     */
    public boolean isNeighborBefore( DownloadScope scope )
    {
        return start <= scope.end+1 && start > scope.start;
    }
    
    /**
     * scope:   |--|      |--|
     * this:  |--|     |--|
     * Determines if the given scope is a neighbor after this segment.
     * A neighbor either directly follows this segment or overlapps and
     * follows this segment partially.  
     * @param scope
     * @return
     */
    public boolean isNeighborAfter( DownloadScope scope )
    {
        return end >= scope.start-1 && end < scope.end;
    }
    
    /**
     * scope:  |---|        |---|
     * this:      |---|  |---|
     * @param scope
     * @return
     */
    public boolean isOverlapping( DownloadScope scope )
    {
        return end >= scope.start && start <= scope.end;
    }
    
    public boolean equals( Object obj )
    {
        if ( !(obj instanceof DownloadScope) )
        {
            return false;
        }
        DownloadScope scope = (DownloadScope) obj;
        return scope.start == start && scope.end == end;
    }
    
    public String toString()
    {
        return "[DownloadScope: start:" + start + ",end:" + end 
            + "@" + Integer.toHexString(hashCode()) + "]";
    }
}
