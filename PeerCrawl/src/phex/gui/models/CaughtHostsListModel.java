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

import phex.host.*;

/**
 * @deprecated CaughtHost are not able to be displayed anymore currently.
 *
 */
public class CaughtHostsListModel extends AbstractListModel
{
    private HostManager hostMgr;

    public CaughtHostsListModel()
    {
        hostMgr = HostManager.getInstance();
        //hostMgr.addCaughtHostsChangeListener( new CaughtHostsListener() );
    }

    public int getSize()
    {
        return hostMgr.getCaughtHostsContainer().getCaughtHostsCount();
    }

    public Object getElementAt( int row )
    {
        /*String host = hostMgr.getCaughtHostAt( row );
        if ( host == null )
        {
            fireIntervalRemoved( this, row, row );
            host = "";
        }
        return host;*/
        return null;
    }

    /*private class CaughtHostsListener
        implements CaughtHostsChangeListener
    {
        private LazyEventQueue lazyEventQueue;

        public CaughtHostsListener()
        {
            lazyEventQueue = GUIRegistry.getInstance().getLazyEventQueue();
        }

        public void caughtHostAdded( int position )
        {
            lazyEventQueue.addListDataEvent( new ListDataEvent( CaughtHostsListModel.this,
                ListDataEvent.INTERVAL_ADDED, position, position ) );
        }

        public void caughtHostRemoved( int position )
        {
            fireIntervalRemoved( CaughtHostsListModel.this, position, position );
        }

        public void autoConnectHostAdded( int position )
        {//ignore
        }
        public void autoConnectHostRemoved( int position )
        {//ignore
        }
    }*/
}