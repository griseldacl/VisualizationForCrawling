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
import javax.swing.event.*;

import phex.event.*;
import phex.gui.common.*;
import phex.query.*;

public class SearchComboBoxModel extends AbstractListModel implements ComboBoxModel
{
    private Object selectedItem = null;
    private SearchContainer searchContainer;

    public SearchComboBoxModel()
    {
        searchContainer = QueryManager.getInstance().getSearchContainer();
        searchContainer.addSearchListChangeListener( new SearchContainerChangeListener() );
    }

    public int getSize()
    {
        return searchContainer.getSearchCount();
    }

    public Object getElementAt(int index)
    {
        Search search = searchContainer.getSearchAt( index );
        if (search == null)
        {
            fireIntervalRemoved( this, index, index );
            return null;
        }
        return search;
    }

    public void setSelectedItem(Object anItem)
    {
        selectedItem = anItem;
        fireContentsChanged( this, -1, -1 );
    }

    public Object getSelectedItem()
    {
        return selectedItem;
    }

    public class SearchContainerChangeListener implements SearchListChangeListener
    {
        private LazyEventQueue lazyEventQueue;

        public SearchContainerChangeListener()
        {
            lazyEventQueue = GUIRegistry.getInstance().getLazyEventQueue();
        }

        /**
         * Called if a search changed.
         */
        public void searchChanged( int position )
        {
            lazyEventQueue.addListDataEvent( new ListDataEvent( SearchComboBoxModel.this,
                ListDataEvent.CONTENTS_CHANGED, position, position ) );
        }

        /**
         * Called if a search was added.
         */
        public void searchAdded( int position )
        {
            fireIntervalAdded( SearchComboBoxModel.this, position, position );
            setSelectedItem( getElementAt( position ) );
        }

        /**
         * Called if a search was removed.
         */
        public void searchRemoved( int position )
        {
            fireIntervalRemoved( SearchComboBoxModel.this, position, position );

            // we are always removing the selected item set the selection back
            // to the first item ( since J2SE 1.4 )
            setSelectedItem( getElementAt( 0 ) );
        }
    }
}