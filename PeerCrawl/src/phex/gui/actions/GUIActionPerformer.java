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
 *  $Id: GUIActionPerformer.java,v 1.11 2005/11/19 14:51:24 gregork Exp $
 */
package phex.gui.actions;

import org.apache.commons.lang.time.DateUtils;

import phex.common.ExpiryDate;
import phex.common.ThreadPool;
import phex.common.address.DestAddress;
import phex.common.address.IpAddress;
import phex.gui.common.GUIRegistry;
import phex.gui.common.MainFrame;
import phex.gui.tabs.search.SearchResultsDataModel;
import phex.gui.tabs.search.SearchTab;
import phex.host.FavoritesContainer;
import phex.host.HostManager;
import phex.query.BrowseHostResults;
import phex.query.QueryManager;
import phex.query.SearchContainer;
import phex.security.IPAccessRule;
import phex.security.PhexSecurityManager;
import phex.share.FileRescanRunner;
import phex.utils.Localizer;

/**
 * A class containing access method with a defined interface to
 * run the functionality of actions called from different tabs.
 * Many tabs provide the same action functions like BrowseHost, 
 * eventhough each tab has its own way on how to enable/disable
 * and resolve these actions, the base functionality of the action
 * stays the same. This class provides a basis to hold this base 
 * functionality of actions.
 */
public class GUIActionPerformer
{
    /**
     * Initiats the browse host request and switches to the search tab.
     * @param hostAddress the host address to browse.
     */
    public static void browseHost( DestAddress hostAddress )
    {
        SearchContainer searchContainer =
            QueryManager.getInstance().getSearchContainer();
        BrowseHostResults result = searchContainer.createBrowseHostSearch(
            hostAddress, null );
        SearchResultsDataModel searchResultsDataModel = 
            SearchResultsDataModel.registerNewSearch( result );
        MainFrame mainFrame = GUIRegistry.getInstance().getMainFrame();
        SearchTab searchTab = (SearchTab)mainFrame.getTab(
            MainFrame.SEARCH_TAB_ID );
        mainFrame.setSelectedTab( MainFrame.SEARCH_TAB_ID );
        searchTab.setDisplayedSearch( searchResultsDataModel );
    }
    
    public static void banHosts( DestAddress[] addresses )
    {
        PhexSecurityManager securityMgr = PhexSecurityManager.getInstance();
        long expiryTime = System.currentTimeMillis()
            + DateUtils.MILLIS_PER_DAY * 7;
        ExpiryDate expiryDate = new ExpiryDate( expiryTime );
        for ( int i = 0; i < addresses.length; i++ )
        {
            IpAddress ip = addresses[i].getIpAddress();
            if ( ip == null )
            {
                continue;
            }
            securityMgr.createIPAccessRule( Localizer.getString(
                "UserBanned" ), true, IPAccessRule.SINGLE_ADDRESS,
                ip.getHostIP(), null, false,
                expiryDate, true );
        }
    }
    
    public static void addHostsToFavorites( DestAddress[] addresses )
    {
        HostManager mgr = HostManager.getInstance();
        FavoritesContainer container = mgr.getFavoritesContainer();
        container.addFavorites( addresses );
    }
    
    public static void rescanSharedFiles()
    {
        Runnable runnable = new Runnable()
        {
            public void run()
            {
                FileRescanRunner.rescan(true, true);
            }
        };
        ThreadPool.getInstance().addJob(runnable, "SharedFilesRescanExecute");
    }
}
