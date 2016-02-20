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
 *  $Id: Ip2CountryManager.java,v 1.9 2005/11/03 16:23:34 gregork Exp $
 */
package phex.common;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.list.UnmodifiableList;

import phex.common.address.AddressUtils;
import phex.common.address.IpAddress;
import phex.utils.*;

/**
 * 
 * @author gkoukkoullis
 */
public class Ip2CountryManager implements Manager
{
    private static Ip2CountryManager instance;
    
    /**
     * Indicates if the database is fully loaded or not.
     */
    private boolean isLoaded;
    private List ipCountryRangeList;
    
    

    private Ip2CountryManager()
    {
        isLoaded = false;
        ipCountryRangeList = new ArrayList();
    }

    public static Ip2CountryManager getInstance()
    {
        if ( instance == null )
        {
            instance = new Ip2CountryManager();
        }
        return instance;
    }

    /**
     * This method is called in order to initialize the manager.  Inside
     * this method you can't rely on the availability of other managers.
     * @return true if initialization was successful, false otherwise.
     */
    public boolean initialize()
    {
        return true;
    }

    /**
     * This method is called in order to perform post initialization of the
     * manager. This method includes all tasks that must be done after initializing
     * all the several managers. Inside this method you can rely on the
     * availability of other managers.
     * @return true if initialization was successful, false otherwise.
     */
    public boolean onPostInitialization()
    {
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
        Runnable runnable = new Runnable()
        {
            public void run()
            {
                loadIp2CountryDB();
            }
        };
        ThreadPool.getInstance().addJob( runnable, "IP2CountryLoader" );
    }

    /**
     * This method is called in order to cleanly shutdown the manager. It
     * should contain all cleanup operations to ensure a nice shutdown of Phex.
     */
    public void shutdown(){}
    
    /**
     * Returns the country code if found, empty string if not found, and null
     * if DB has not been loaded yet.
     * @param address
     * @return
     */
    public String getCountryCode( IpAddress address )
    {
        if ( !isLoaded )
        {
            return null;
        }
        IpCountryRange range = binarySearch( address.getHostIP() );
        if ( range == null )
        {
            return "";
        }
        return range.countryCode;
    }
    
    private void loadIp2CountryDB()
    {
        InputStream inStream = ClassLoader.getSystemResourceAsStream(
            "phex/resources/ip2country.csv" );
        if ( inStream == null )
        {
            Logger.logMessage( Logger.FINE, Logger.NETWORK,
                "Default GWebCache file not found." );
            return;
        }
        BufferedReader reader = new BufferedReader( new InputStreamReader( inStream ) );
        
        ArrayList initialList = new ArrayList( 5000 );
        IpCountryRange range;
        String line;
        try
        {
            line = reader.readLine();
            while( line != null )
            {
                range = new IpCountryRange( line );
                initialList.add( range );
                line = reader.readLine();
            }
        }
        catch (IOException exp)
        {
            NLogger.error( NLoggerNames.GLOBAL, exp, exp );
        }
        finally
        {
            IOUtil.closeQuietly(reader);
        }
        initialList.trimToSize();
        Collections.sort( initialList );
        ipCountryRangeList = UnmodifiableList.decorate( initialList );
        isLoaded = true;
    }
    
    private IpCountryRange binarySearch( byte[] hostIp )
    {
        int low = 0;
        int high = ipCountryRangeList.size() - 1;
    
        while (low <= high)
        {
            int mid = (low + high) >> 1;
            IpCountryRange midVal = (IpCountryRange)ipCountryRangeList.get( mid );
            int cmp = midVal.compareHostAddress( hostIp );
            if (cmp < 0)
            {
                low = mid + 1;
            }
            else if (cmp > 0)
            {
                high = mid - 1;
            }
            else
            {
                return midVal; // key found
            }
        }
        return null;  // key not found
    }
    
    private class IpCountryRange implements Comparable
    {
        byte[] from;
        byte[] to;
        String countryCode;
        
        public IpCountryRange( String line )
        {
            // "33996344","33996351","GB"
            int startIdx, endIdx;
            startIdx = 0;
            
            endIdx = line.indexOf( (int)',', startIdx );
            from = AddressUtils.parseIntIP( line.substring( startIdx, endIdx ) );
            
            startIdx = endIdx + 1;
            endIdx = line.indexOf( (int)',', startIdx );
            to = AddressUtils.parseIntIP( line.substring( startIdx, endIdx ) );
            
            startIdx = endIdx + 1;
            countryCode = line.substring( startIdx );            
        }
        
        public int compareHostAddress( byte[] hostIp )
        {
            long hostIpL;
            hostIpL = IOUtil.unsignedInt2Long(
                IOUtil.deserializeInt( hostIp, 0));
            long fromIpL = IOUtil.unsignedInt2Long( IOUtil.deserializeInt( from, 0 ) );
            long cmp = hostIpL - fromIpL;
            if ( cmp == 0 )
            {
                return 0;
            }
            if ( cmp < 0 )
            {// host Ip is lower..
                return 1;
            }
            
            // validate to range..
            long toIpL = IOUtil.unsignedInt2Long( IOUtil.deserializeInt( to, 0 ) );
            cmp = hostIpL - toIpL;
            if ( cmp == 0 || cmp < 0)
            {// we are between from and to
                return 0;
            }
            else
            {// host Ip is higher..
                return -1;
            }
        }
        
        public int compareTo(Object o)
        {
            if ( o == this )
            {
                return 0;
            }
            IpCountryRange range = (IpCountryRange)o;
            
            byte[] ip1 = (byte[])from;
            byte[] ip2 = (byte[])range.from;

            long ip1l = IOUtil.unsignedInt2Long( IOUtil.deserializeInt( ip1, 0 ) );
            long ip2l = IOUtil.unsignedInt2Long( IOUtil.deserializeInt( ip2, 0 ) );

            if ( ip1l < ip2l )
            {
                return -1;
            }
            // only if rate and object is equal return 0
            else
            {
                return 1;
            }
        }
    }
}
