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
 *  Created on 14.03.2005
 *  --- CVS Information ---
 *  $Id: OnlineObserver.java,v 1.8 2005/10/19 23:27:09 gregork Exp $
 */
package phex.net;

import phex.common.ServiceManager;
import phex.connection.NetworkManager;
import phex.host.HostManager;
import phex.host.NetworkHostsContainer;
import phex.udp.hostcache.UdpHostCacheManager;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

/**
 * This class tries to observers the online status of a connection.
 * If a certain amount of connection fail due to socket connection 
 * failure the online observer assumes a missing online connection 
 * and disconnects from network.
 */
public class OnlineObserver
{
    /**
     * The number of failed connections in a row.
     */
    private int failedConnections;
    
    public OnlineObserver()
    {
        failedConnections = 0;
    }
    
    public void markFailedConnection()
    {
        // only count if there are no active connections in the network
        NetworkHostsContainer networkHostsContainer = 
            HostManager.getInstance().getNetworkHostsContainer();
        if ( networkHostsContainer.getTotalConnectionCount() > 0 )
        {
            failedConnections = 0;
            return;
        }
        
        failedConnections ++;
        if ( NLogger.isDebugEnabled( NLoggerNames.ONLINE_OBSERVER ) &&
             failedConnections % 5 == 0 )
        {
            NLogger.debug(NLoggerNames.ONLINE_OBSERVER,
                "Observed " + failedConnections + " failed connections.");
        }
        
        //if we have between 15 to 20 failed connections query udp host cache
        if( failedConnections % 20 == 0 )
        {
            NLogger.info( NLoggerNames.ONLINE_OBSERVER, 
                "Started a UDP HOST CACHE Query due to increasing failed connections");
        	UdpHostCacheManager.getInstance().invokeQueryCachesRequest();
        }
        
        if ( failedConnections > ServiceManager.sCfg.offlineConnectionFailureCount )
        {
            NLogger.debug(NLoggerNames.ONLINE_OBSERVER,
                "Too many connections failed.. disconnecting network.");
            NetworkManager.getInstance().disconnectNetwork();
        }
    }
    
    public void markSuccessfulConnection()
    {
        failedConnections = 0;
    }
}