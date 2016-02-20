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
package phex.gui.models;

import javax.swing.*;

import phex.event.*;
import phex.host.*;

public class FavoritesListModel extends AbstractListModel
{
    private FavoritesContainer favoritesContainer;

    public FavoritesListModel()
    {
        HostManager hostMgr = HostManager.getInstance();
        favoritesContainer = hostMgr.getFavoritesContainer();
        favoritesContainer.addBookmarkedHostsChangeListener( new FavoritesListener() );
    }

    public int getSize()
    {
        return favoritesContainer.getBookmarkedHostsCount();
    }

    public Object getElementAt( int row )
    {
        FavoriteHost host = favoritesContainer.getBookmarkedHostAt( row );
        if ( host == null )
        {
            fireIntervalRemoved( this, row, row );
            return "";
        }
        return host;
    }

    private class FavoritesListener
        implements BookmarkedHostsChangeListener
    {
        public void bookmarkedHostAdded( int position )
        {
            fireIntervalAdded( this, position, position );
        }
        public void bookmarkedHostRemoved( int position )
        {
            fireIntervalRemoved( this, position, position );
        }
    }
}