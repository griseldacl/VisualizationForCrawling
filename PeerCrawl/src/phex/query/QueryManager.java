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
package phex.query;


import java.util.TimerTask;

import phex.common.Environment;
import phex.common.Manager;
import phex.host.Host;
import phex.host.HostManager;
import phex.msg.MsgManager;
import phex.msg.QueryMsg;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;


public class QueryManager implements Manager
{
    private QueryHistoryMonitor queryMonitor;
    private SearchContainer searchContainer;
    private BackgroundSearchContainer backgroundSearchContainer;
    //private ResearchService researchService;
    //private SearchFilterContainer searchFilterContainer;
    private DynamicQueryWorker dynamicQueryWorker;

    private static QueryManager instance;

    public QueryManager()
    {
    }

    public static QueryManager getInstance()
    {
        if ( instance == null )
        {
            instance = new QueryManager();
        }
        return instance;
    }

    /**
     * This method is called in order to initialize the manager. This method
     * includes all tasks that must be done to intialize all the several manager.
     * Like instantiating the singleton instance of the manager. Inside
     * this method you can't rely on the availability of other managers.
     * @return true is initialization was successful, false otherwise.
     */
    public boolean initialize()
    {
        queryMonitor = new QueryHistoryMonitor();
        searchContainer = new SearchContainer();
        backgroundSearchContainer = new BackgroundSearchContainer();        
        //researchService = new ResearchService( new ResearchServiceConfig() );
        //searchFilterContainer = new SearchFilterContainer();        
        dynamicQueryWorker = new DynamicQueryWorker();
        return true;
    }

    /**
     * This method is called in order to perform post initialization of the
     * manager. This method includes all tasks that must be done after initializing
     * all the several managers. Inside this method you can rely on the
     * availability of other managers.
     * @return true is initialization was successful, false otherwise.
     */
    public boolean onPostInitialization()
    {
        dynamicQueryWorker.startQueryWorker();
        return true;
    }
    
    /**
     * This method is called after the complete application including GUI completed
     * its startup process. This notification must be used to activate runtime
     * processes that needs to be performed once the application has successfully
     * completed startup.
     */
    public void startupCompletedNotify()
    {
        Environment.getInstance().scheduleTimerTask( 
            new ExpiredSearchCheckTimer(), ExpiredSearchCheckTimer.TIMER_PERIOD,
            ExpiredSearchCheckTimer.TIMER_PERIOD );
    }

    /**
     * This method is called in order to cleanly shutdown the manager. It
     * should contain all cleanup operations to ensure a nice shutdown of Phex.
     */
    public void shutdown()
    {
        queryMonitor.shutdown();
    }

    public QueryHistoryMonitor getQueryHistoryMonitor()
    {
        return queryMonitor;
    }

    public SearchContainer getSearchContainer()
    {
        return searchContainer;
    }

    public BackgroundSearchContainer getBackgroundSearchContainer()
    {
        return backgroundSearchContainer;
    }

/*    public ResearchService getResearchService()
    {
        return researchService;
    }
*/
    
    /**
     * Removes all running queries for this host.
     * @param host the host to remove its queries for.
     */
    public void removeHostQueries( Host host )
    {
        if ( host.isUltrapeerLeafConnection() )
        {
            dynamicQueryWorker.removeDynamicQuerysForHost( host );
        }
    }
    
    /**
     * Sends a dynamic query using the dynamic query engine.
     * @param query the query to send.
     * @param desiredResults the number of results desired.
     */
    public DynamicQueryEngine sendDynamicQuery( QueryMsg query, int desiredResults )
    {
        DynamicQueryEngine engine = new DynamicQueryEngine( query, desiredResults );
        dynamicQueryWorker.addDynamicQueryEngine( engine );
        return engine;
    }
    
    /**
     * Sends a query for this host, usually initiated by the user.
     * @param query the query to send.
     * @return the possible dynamic query engine used, or null if no
     *         dynamic query is initiated.
     */
    public DynamicQueryEngine sendMyQuery( QueryMsg queryMsg )
    {
        MsgManager msgMgr = MsgManager.getInstance();
        // add my own query to seen list.
        msgMgr.checkAndAddToQueryRoutingTable( queryMsg.getHeader().getMsgID(),
            Host.LOCAL_HOST );
            
        // FIXME if only connected to peers no queries get forwarded!!
        // but usually we dont connect to 0.4 peers only anymore...
        if ( HostManager.getInstance().isUltrapeer() )
        {
            return sendDynamicQuery( queryMsg,
                DynamicQueryConstants.DESIRED_ULTRAPEER_RESULTS );
        }
        else
        {
            msgMgr.forwardQueryToUltrapeers( queryMsg, null );
            return null;
        }
    }

    /*public SearchFilterContainer getSearchFilterContainer()
    {
        return searchFilterContainer;
    }*/
    
    /**
     * Stops all searches where the timeout has passed.
     */
    private class ExpiredSearchCheckTimer extends TimerTask
    {

        public static final long TIMER_PERIOD = 5000;

        /**
         * @see java.util.TimerTask#run()
         */
        public void run()
        {
            try
            {
                // Stops all searches where the timeout has passed.
                long currentTime = System.currentTimeMillis();
                searchContainer.stopExpiredSearches( currentTime );
                backgroundSearchContainer.stopExpiredSearches( currentTime );
            }
            catch ( Throwable th )
            {
                NLogger.error( NLoggerNames.GLOBAL, th, th );
            }
        }
    }
}