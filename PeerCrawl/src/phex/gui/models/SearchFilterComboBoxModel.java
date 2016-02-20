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
import phex.query.*;

public class SearchFilterComboBoxModel extends AbstractListModel implements ComboBoxModel
{
    private Object selectedItem;
    private SearchFilterContainer searchFilterContainer;

    public SearchFilterComboBoxModel()
    {
        searchFilterContainer = null;//QueryManager.getInstance().getSearchFilterContainer();
        searchFilterContainer.addSearchFilterListListener( new FilterContainerListener() );
        if ( searchFilterContainer.getSearchFilterCount() > 0 )
        {
            selectedItem = searchFilterContainer.getSearchFilterAt( 0 );
        }
        else
        {
            selectedItem = null;
        }
    }

    public int getSize()
    {
        return searchFilterContainer.getSearchFilterCount();
    }

    public Object getElementAt(int index)
    {
        SearchFilter filter = searchFilterContainer.getSearchFilterAt( index );
        if (filter == null)
        {
            fireIntervalRemoved( this, index, index );
            return null;
        }
        return filter;
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

    public class FilterContainerListener implements SearchFilterListListener
    {
        /**
         * Called if a search filter changed.
         */
        public void searchFilterChanged( int position )
        {
            fireContentsChanged( SearchFilterComboBoxModel.this, position, position );
        }

        /**
         * Called if a search was added.
         */
        public void searchFilterAdded( int position )
        {
            fireIntervalAdded( SearchFilterComboBoxModel.this, position, position );
            setSelectedItem( getElementAt( position ) );
        }

        /**
         * Called if a search was removed.
         */
        public void searchFilterRemoved( int position )
        {
            fireIntervalRemoved( SearchFilterComboBoxModel.this, position, position );

            // we are always removing the selected item set the selection back
            // to the first item ( since J2SE 1.4 )
            setSelectedItem( getElementAt( 0 ) );
        }
    }
}
