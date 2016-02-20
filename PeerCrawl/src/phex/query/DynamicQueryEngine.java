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
 *  $Id: DynamicQueryEngine.java,v 1.12 2005/11/03 17:06:26 gregork Exp $
 */
package phex.query;

import java.util.*;

import phex.common.QueryRoutingTable;
import phex.host.*;
import phex.msg.*;

/**
 * The DynamicQueryEngine is responsible for submitting the query 
 * over time to different hosts with different ttls depending on the
 * number of results received.
 * Queries are first checked against connected leaves, then probe 
 * queries with ttl of 1 are sent to ultrapeers to determine the file
 * popularity, the last step is to sent regular queries with adjusted
 * ttl to single ultrapeers.
 * Only Ultrapeers use this engine to submit there queries. Leaves are
 * sending there queries in the old style by broadcasting them to all 
 * there connections. The Ultrapeer of the leaf should then start the
 * dynamic query process for its leaf.
 */
public class DynamicQueryEngine implements DynamicQueryConstants
{
    private MsgManager messageMgr;
    private HostManager hostMgr;
    private NetworkHostsContainer hostsContainer;
    
    /**
     * Indicates if the dynamic query process has started.
     */
    private boolean isDynamicQueryStarted;
    
    /**
     * Indicates if the dynamiy query process was forced to
     * stop. Like after user interaction.
     */
    private boolean isDynamicQueryStopped;
    
    /**
     * Indicates if leaves are queried.
     */
    private boolean areLeavesQueried;
    
    /**
     * Indicates if the probe query is sent.
     */
    private boolean isProbeQuerySent;
    
    /**
     * The number of millis a query is running before it times out.
     */
	private long queryTimeout;

    /**
     * The time when the query was started.
     */
    private long queryStartTime;
    
    /**
     * The time when the next query process step is
     * taken.
     */
    private long nextProcessTime;
    
    /**
     * The time to wait per hop between dynamic query iterations.
     */
    private int timeToWaitPerHop;

    /**
	 * The number of results desired to gain.
	 */
	private int desiredResults;
    
    /**
     * The number of results already received.
     */
    private int receivedResults;
    
    /**
     * The estimated number of host in the horizon that have been reached
     * by this query.
     */
    private int estimatedQueriedHorizon;
    
    /**
     * A list of hosts that we send a standard query too.
     */
    private List queriedHosts;
    
    /**
     * The base query of this DynamicQueryEngine. Form it
     * new queries are build.
     */
    private QueryMsg query;

    /**
     * Constructs a new DynamicQueryEngine.
     * @param query the query to base the process on.
     */
    public DynamicQueryEngine( QueryMsg query, int desiredResults )
    {
        messageMgr = MsgManager.getInstance();
        hostMgr = HostManager.getInstance();
        hostsContainer = hostMgr.getNetworkHostsContainer();
        
        this.query = query;
        this.desiredResults = desiredResults;
        isDynamicQueryStarted = false;
        isDynamicQueryStopped = false;
        areLeavesQueried = false;
        isProbeQuerySent = false;
        queryTimeout = DEFAULT_QUERY_TIMEOUT;
        timeToWaitPerHop = DEFAULT_TIME_TO_WAIT_PER_HOP;
        estimatedQueriedHorizon = 1;
        receivedResults = 0;
        queriedHosts = new ArrayList();
        
        
        if ( query.hasQueryURNs() )
        {// adjust desired results
            desiredResults = DESIRED_HASH_RESULTS;
        }
    }
    
    /**
     * Increments the received result count.
     * @param inc the value to increment the received result count.
     */
    public void incrementResultCount( int inc )
    {
        receivedResults += inc;
    }
    
    /**
	 * @return
	 */
	public int getResultCount()
	{
		return receivedResults;
	}
    
    /**
     * Forces to stop the dynamic query. Like after user interaction.
     */
    public void stopQuery()
    {
        isDynamicQueryStopped = true;
    }
    
    /**
     * Returns if there are already enough results routed to meet the 
     * desired results.
     * @return true if there are already enough results routed to meet the 
     *         desired results, false otherwise.
     */
    public boolean areEnoughResultsRouted()
    {
        // check if we received enough results...
        return receivedResults >= desiredResults;
    }
    
    /**
     * Returns the host this query is originaly comming from.
     * @return the host this query is originaly comming from.
     */
    public Host getFromHost()
    {
        return query.getHeader().getFromHost();
    }

    /**
     * Returns whether this query engine has finished its query process. This
     * can be the case when we received enough results, reached the maximal 
     * estimated horizon, or the query is already running for a too long time.
     * 
     * @return true if the query engine has finished its query process, false
     *         otherwise.
     */
    public boolean isQueryFinished()
    {
        // check if query has started.
        if ( !isDynamicQueryStarted )
        {
            return false;
        }
        if ( isDynamicQueryStopped )
        {
            return true;
        }
        
        // check if we received enough results...
        if( receivedResults >= desiredResults )
        {
            return true;
        }
        
        if ( estimatedQueriedHorizon > MAX_ESTIMATED_QUERY_HORIZON )
        {
            return true;
        }
        
        // check if the query has timed out
        long currentTime = System.currentTimeMillis();
        if ( currentTime > queryStartTime + queryTimeout )
        {
            return true;
        }
        
        return false;
    }
    
    /**
     * Tries a very basic calculation about the search progress.
     * @return
     */
    public int getProgress()
    {
        // check if query has started.
        if ( !isDynamicQueryStarted )
        {
            return 0;
        }
        if ( isDynamicQueryStopped )
        {
            return 100;
        }
        
        int resultProgress = (int)((double)receivedResults / (double)desiredResults
            * (double)100 );

        int horizonProgress = (int)((double)estimatedQueriedHorizon
            / (double)MAX_ESTIMATED_QUERY_HORIZON * (double)100 );
        
        // time progress...
        long currentTime = System.currentTimeMillis();
        int timeProgress = (int)(100 - (double)( queryStartTime + queryTimeout - currentTime )
            / (double)queryTimeout * (double)100 );
            
        // return the max of all these
        return Math.min( Math.max( resultProgress,
            Math.max( horizonProgress, timeProgress ) ), 100);
    }

    /**
     * Runs the dynamic query process. This method is called
     * regulary from the DynamicQueryWorker to continue the
     * query process.
     */
    public void processQuery()
    {
        long currentTime = System.currentTimeMillis();
        if ( currentTime < nextProcessTime )
        {// not our turn to query now...
            return;
        }
        
        if ( !isDynamicQueryStarted )
        {
            isDynamicQueryStarted = true;
            queryStartTime = currentTime;
            
            // first sent to leaves... if ultrapeer.
            // we always are a ultrapeer since only ultrapeer use dynamic query...
            //if ( hostMgr.isUltrapeer() )
            {
                boolean sentToLeaves = processQueryToLeaves();
                if ( sentToLeaves )
                {
                    nextProcessTime = System.currentTimeMillis() + timeToWaitPerHop;
                    return;
                }
            }
        }
        
        // after leaves are querierd... send a probe query...
        if ( !isProbeQuerySent )
        {
            processProbeQuery();
        }
        else
        {
            processStandardQuery();
        }
    }
    
    /**
     * Processes the standard query. This is the last step of the
     * dynamic query process after leaves are queried and probe
     * queries are sent.
     */
    private void processStandardQuery()
    {
        Host[] ultrapeers = hostsContainer.getUltrapeerConnections();
        // the number of connections that have not yet received the query
        // used to calculate TTL
        int notQueriedHosts = 0;
        Host hostToQuery = null;
        for ( int i = 0; i < ultrapeers.length; i++ )
        {
            // sort out all not stable and all 
            // connections we already queried.
            if ( !ultrapeers[i].isConnectionStable() ||
                 queriedHosts.contains( ultrapeers[i] ) )
            {
                continue;
            }
            notQueriedHosts ++;
            // found a host to query next...
            hostToQuery = ultrapeers[i];
        }
        
        if ( notQueriedHosts == 0 || hostToQuery == null )
        {// no hosts to query found... try again later...
            nextProcessTime = System.currentTimeMillis() + 5000;
            return;
        }
        
        byte maxTTL = hostToQuery.getMaxTTL();
        int degree = hostToQuery.getUltrapeerDegree();
        byte ttl = calculateTTL( maxTTL, degree, notQueriedHosts );
        
        
        if ( ttl == 1 && hostToQuery.isUPQueryRoutingSupported() )
        {// if we have a UP query routing connection we can
         // pre check if a query with ttl of 1 makes sense.
         // if there would be no hit we set the ttl to 2.
            QueryRoutingTable qrt = hostToQuery.getLastReceivedRoutingTable();
            if ( qrt == null || !qrt.containsQuery( query ) )
            {
                ttl = 2;
            }
        }
        
        QueryMsg newQuery = new QueryMsg( query, ttl );
        hostToQuery.queueMessageToSend( newQuery );
        queriedHosts.add( hostToQuery );

        // calculate the estimated reached horizon
        estimatedQueriedHorizon = calculateEstimatedHorizon( degree, ttl );
        nextProcessTime = System.currentTimeMillis() + (ttl * timeToWaitPerHop);
        
        adjustTimeToWaitPerHop();
    }

    /**
     * Sends the probe query to ultrapeers to check the
     * file popularity.
     */
    private void processProbeQuery()
    {
        Host[] ultrapeers = hostsContainer.getUltrapeerConnections();
        List directHitList = new ArrayList( ultrapeers.length );
        List failedList = new ArrayList( ultrapeers.length );
        
        for ( int i = 0; i < ultrapeers.length; i++ )
        {
            QueryRoutingTable qrt = ultrapeers[i].getLastReceivedRoutingTable();
            if ( ultrapeers[i].isUPQueryRoutingSupported() && qrt != null )
            {
                if ( qrt.containsQuery( query ) )
                {
                    directHitList.add( ultrapeers[i] );
                }
                else
                {// add to the end of failed list (low priority)
                    failedList.add( ultrapeers[i] );
                }
            }
            else
            {// add to the top of failed list (high priority)
                failedList.add( 0, ultrapeers[i] );
            }
        }
        
        int directProbeSize = directHitList.size();
        int failedProbeSize = 0;

        // send probe query to direct hits (max. 10)... with ttl = 1
        int toIdx = Math.min( 10, directProbeSize );
        sendProbeQueryToHosts( directHitList.subList( 0, toIdx ), (byte)1 );
        directProbeSize = toIdx;
        
        
        // we have not enough direct hits... 
        // probe with some of the failed hosts... with ttl = 2
        if ( directProbeSize < 4 )
        {
            toIdx = Math.min( 3, failedList.size() );
            sendProbeQueryToHosts( failedList.subList( 0, toIdx ), (byte)2 );

            failedProbeSize = toIdx;
        }
        
        // special rule for probe... wait per connection
        nextProcessTime = System.currentTimeMillis() +
            timeToWaitPerHop * ( directProbeSize + failedProbeSize );
        
        isProbeQuerySent = true;
    }
    
    /**
     * Sends querys to my leaves.
     * @return true if the query was forwarded to any leave, false otherwise.
     */
    private boolean processQueryToLeaves()
    {
        QueryRoutingTable qrt = messageMgr.getLastSentQueryRoutingTable();
        if ( qrt != null && qrt.containsQuery( query ) )
        {
            QueryMsg newQuery = new QueryMsg( query, (byte)1 );
            estimatedQueriedHorizon += hostsContainer.getLeafConnectionCount();
            messageMgr.forwardQueryToLeaves( newQuery,
                newQuery.getHeader().getFromHost() );
            return true;
        }
        return false;
    }
    
    /**
     * Sends the probe query to the given host list.
     * @param hostList the host list to send the query to.
     * @param ttl the ttl to use with the query.
     */
    public void sendProbeQueryToHosts( List hostList, byte ttl )
    {
        Iterator iterator = hostList.iterator();
        QueryMsg newQuery = new QueryMsg( query, (byte)1 );
        
        while( iterator.hasNext() )
        {
            Host host = (Host)iterator.next();
            host.queueMessageToSend( newQuery );
            // remember that we queried the host
            queriedHosts.add( host );
            
            // calculate the estimated reached horizon
            int degree = host.getUltrapeerDegree();
            estimatedQueriedHorizon = calculateEstimatedHorizon( degree, ttl );
        }
    }
    
    /**
     * Calculates the estimated reached horizon with a query with the
     * given ttl and a given intra ultrapeer connection degree. 
     * hosts(degree,ttl) = Sum[(degree-1)^i, 0 <= i <= ttl-1]
     * @return the estimated number of hosts queried.
     */
    private int calculateEstimatedHorizon( int degree, byte ttl )
    {
        int hostCount = 0;
        while ( ttl > 0 )
        {
            hostCount += Math.pow( degree - 1, ttl - 1 );
            ttl --;
        }
        return hostCount;
    }
    
    /**
     * Calculates the used ttl for the next query. The calculation
     * is based on the availabe connection count, the received results,
     * desired results, estimated queriered horizon, max ttl and intra
     * ultrapeer connection degree.
     * @param maxTTL the max ttl allowed on the host to query.
     * @param degree the intra ultrapeer connection degree of the host to query.
     * @param connectionCount the number of available connections to query.
     * @return the ttl to use.
     */
    private byte calculateTTL( byte maxTTL, int degree, int connectionCount )
    {
        double resultsPerHost = (double)receivedResults / (double)estimatedQueriedHorizon;
        int missingResults = desiredResults - receivedResults;
        
        int hostsNeededToQuery;
        if ( resultsPerHost == 0 )
        {
            hostsNeededToQuery = 50000;
        }
        else
        {
            hostsNeededToQuery = (int)((double)missingResults / resultsPerHost);
        }
        int hostsPerConnection = hostsNeededToQuery / connectionCount;
        
        for( byte i = 1; i < 6; i++ )
        {
            if( i > maxTTL )
            {
                return maxTTL;
            } 

            int hosts = (int)(16.0 * calculateEstimatedHorizon(degree, i) );            
            if( hosts >= hostsPerConnection )
            {
                return i;
            }
        }
        return maxTTL;
    }
    
    /**
     * Adjusts the time to wait per hop. This is done if
     * there have not been enough results received after
     * a while and we want to go faster through the available
     * hosts.
     */
    private void adjustTimeToWaitPerHop()
    {
        if ( timeToWaitPerHop > 100 &&
            ( System.currentTimeMillis() - queryStartTime ) > TIMETOWAIT_ADJUSTMENT_DELAY )
        {
            double ratio;
            if ( receivedResults == 0 )
            {
                ratio = 20;
            }
            else
            {
                ratio = Math.max( 20, (desiredResults/2)/receivedResults );
            } 
            timeToWaitPerHop -= (int)((double)TIMETOWAIT_ADJUSTMENT * ratio);
            if ( timeToWaitPerHop < 100 )
            {
               timeToWaitPerHop = 100;
            }
        }
    }
}
