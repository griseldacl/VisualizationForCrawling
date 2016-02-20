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
 *  Created on 25.03.2005
 *  --- CVS Information ---
 *  $Id: GWebCache.java,v 1.7 2005/11/03 16:23:36 gregork Exp $
 *  File Modified
 */

/**
 * PeerCrawl - Distributed P2P web crawler based on Gnutella Protocol
 * @version 2.0
 * 
 * Developed as part of Masters Project - Spring 2006
 * @author 	Vaibhav Padliya
 * 			College of Computing
 * 			Georgia Tech
 * 
 * @contributor Mudhakar Srivatsa
 * @contributor Mahesh Palekar
 */

package phex.gwebcache;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import phex.common.address.AddressUtils;
import phex.connection.ProtocolNotSupportedException;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

/**
 * Represents a GWebCache.
 */
public class GWebCache
{
//    private static final int MIN_RECONNECT_WAIT = 1000 * 60 * 30; // 30 minutes
	private static final int MIN_RECONNECT_WAIT = 100; // 0.1 second
    private URL url;
    private boolean isPhexCache;
    private long lastRequestTime;
    private int failedInRowCount;
    
    /**
     * Cached hash code of the GWebCache.
     */
    private int hash = 0;
    
    
    public GWebCache( URL url )
        throws IOException
    {
        this( url, false );
    }
    
    public GWebCache( URL url, boolean isPhexCache )
        throws IOException
    {
        if ( url == null )
        {
            throw new NullPointerException( "Null url given.");
        }
        
        // we only support http protocol urls.
        if ( !url.getProtocol().equals( "http" ) )
        {
            throw new ProtocolNotSupportedException(
                "Only http URLs are supported for a GWebCacheConnection" );
        }
        if ( url.getPort() == 80 )
        {
            // rebuild url without port
            url = new URL( url.getProtocol(), url.getHost(), -1, url.getFile() );
        }
        
        GWebCacheContainer container = GWebCacheManager.getInstance().getGWebCacheContainer();
        if ( !isPhexCache && container.isPhexGWebCache( url.toExternalForm() ) )
        {
            NLogger.error( NLoggerNames.GWEBCACHE, "Trying to add Phex cache without Phex flag!");
            isPhexCache = true;
        }
        this.url = url;
        this.isPhexCache = isPhexCache;
    }
    
    public boolean isPhexCache()
    {
        return isPhexCache;
    }

    public URL getUrl()
    {
        return url;
    }
    
    public String getHostDomain()
    {
        String host = url.getHost();
        if ( AddressUtils.isIPHostName(host) )
        {
            return host;
        }
        int topLevelIdx = host.lastIndexOf( '.' );
        int domainIdx = host.lastIndexOf('.', topLevelIdx - 1 );
        if ( domainIdx != -1 )
        {
            return host.substring(domainIdx+1);
        }
        return host;
    }

//    Looking up host ip turns out to be a very slow solution...
//    public byte[] getHostIp()
//    {
//        //hostIP = InetAddress.getByName( url.getHost() ).getAddress();
//        return hostIP;
//    }
    
    
    public int getFailedInRowCount()
    {
        return failedInRowCount;
    }

    public long getLastRequestTime()
    {
        return lastRequestTime;
    }

    public void countConnectionAttempt( boolean isFailed )
    {
        lastRequestTime = System.currentTimeMillis();
        if ( isFailed )
        {
            failedInRowCount ++;
        }
        else
        {
            failedInRowCount = 0;
        }
    }
    
    public long getEarliestReConnectTime()
    {
        return MIN_RECONNECT_WAIT * (failedInRowCount+1) + lastRequestTime; 
    }
    
    public void setFailedInRowCount( int failedInRowCount )
    {
        this.failedInRowCount = failedInRowCount;
    }

    public void setLastRequestTime( long lastRequestTime )
    {
        this.lastRequestTime = lastRequestTime;
    }

    public boolean equals( Object obj )
    {
        if (obj instanceof GWebCache == false)
        {
            return false;
        }
        if ( this == obj ) 
        {
            return true;
        }
        GWebCache gwc = (GWebCache) obj;
        return new EqualsBuilder()
            .append( url.getHost(),  gwc.getUrl().getHost() )
            .isEquals();
    }
    
    public int hashCode( )
    {
        if ( hash == 0 )
        {
            hash = new HashCodeBuilder( 17, 37 ).
                append( url.getHost() ).
                toHashCode();
        }
        return hash;
    }
}