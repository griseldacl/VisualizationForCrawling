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
 *  $Id: ISortableModel.java,v 1.5 2005/10/03 00:18:26 gregork Exp $
 */
package phex.gui.models;

/**
 * 
 */
public interface ISortableModel
{

    /**
     * Can be used to indicate which column is sorted.
     * @return the index of the column
     */
    public int getSortByColumn();

    /**
     * Can be used to indicate the sort direction.
     * @return the sort direction
     */
    public boolean isSortedAscending();
    
    /**
     * Sorts the given column with the given direction...
     * @param column the column to sort
     * @param isSortedAscending the direction.
     */
    public void sortByColumn( int column, boolean isSortedAscending );
}
