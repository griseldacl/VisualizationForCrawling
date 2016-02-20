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
 *  $Id: AlternateLocationContainer.java,v 1.19 2005/11/03 16:33:45 gregork Exp $
 */
package phex.common;

import java.util.*;

import javax.xml.bind.JAXBException;

import org.apache.commons.collections.SequencedHashMap;

import phex.common.address.DestAddress;
import phex.http.HTTPHeader;
import phex.utils.Logger;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;
import phex.xml.ObjectFactory;
import phex.xml.XJBAlternateLocation;

/**
 * A AlternateLocationContainer that helps holder to add, sort and access
 * AlternateLocations.
 *
 * @author Gregor Koukkoullis
 */
public class AlternateLocationContainer
{
    public static final int MAX_ALT_LOC_COUNT = 100;
    
    // Dummy value to associate with an Object in the backing Map
    private static final Object PRESENT = new Object();

    /**
     * The urn each alternate location must match to be accepted.
     */
    private URN urn;

    /**
     * The map of AlternateLocations. It is lazy initialized on first use and
     * sorted by timestamp.<br>
     * We are using a SequencedHashMap that allows use to remove the alt loc
     * which has not been seen (put) into the map for the longest time.
     */
    private SequencedHashMap altLocationMap;

    public AlternateLocationContainer( URN urn )
    {
        if( urn == null )
        {
            throw new NullPointerException( "URN must be provided" );
        }
        this.urn = urn;
    }

    public void addContainer( AlternateLocationContainer cont )
    {
        if ( cont.urn == null || !cont.urn.equals( urn ) )
        {
            // dont add container with wrong or not existing sha1.
            // This is a implementation error.
            throw new IllegalArgumentException(
                "Trying to add container with not matching urns" );

        }

        // this check prevents a NullPointerException during putAll();
        if ( cont.altLocationMap == null )
        {// nothing to add..
            return;
        }
        initMap();
        synchronized( cont.altLocationMap )
        {
            Iterator iterator = cont.altLocationMap.keySet().iterator();
            AlternateLocation tempAltLoc;
            while( iterator.hasNext() )
            {
                tempAltLoc = (AlternateLocation)iterator.next();
                addAlternateLocation( tempAltLoc );
            }
        }
    }

    public void addAlternateLocation( AlternateLocation altLoc )
    {
        URN altLocURN = altLoc.getURN();
        if ( altLocURN == null || !altLocURN.equals( urn ) )
        {// dont add alt location with wrong or not existing sha1
            Logger.logMessage( Logger.WARNING, Logger.DOWNLOAD,
                "Cant add alt-location with not matching URN to container " );
            return;
        }

        initMap();
        synchronized( altLocationMap )
        {
            // thanks to AlternateLocation hashCode implementation the map ensures
            // that each alt loc is only once present. The put operation of a 
            // duplicate alt loc is required here to update the SequencedHashMap
            // internal order of elements. This ensures that alt loc not seen
            // for a long time get removed from the map.
            altLocationMap.put( altLoc, PRESENT );
            // make sure we have not more then 100 alt locations.
            if ( altLocationMap.size() > MAX_ALT_LOC_COUNT )
            {// drop last element
                Object firstKey = altLocationMap.getFirstKey();
                altLocationMap.remove( firstKey );
            }
        }
    }
    
    public void removeAlternateLocation( AlternateLocation altLoc )
    {
        URN altLocURN = altLoc.getURN();
        if ( altLocURN == null || !altLocURN.equals( urn ) )
        {// dont add alt location with wrong or not existing sha1
            Logger.logMessage( Logger.WARNING, Logger.DOWNLOAD,
                "Cant remove alt-location with not matching URN to container " );
            return;
        }
        initMap();
        synchronized( altLocationMap )
        {
            altLocationMap.remove( altLoc );
        }
    }

    public void addFromUriResHTTPHeaders( HTTPHeader[] headers )
    {
        List altLocList = parseUriResAltLocFromHTTPHeaders( headers );
        Iterator iterator = altLocList.iterator();
        while( iterator.hasNext() )
        {
            addAlternateLocation( (AlternateLocation)iterator.next() );
        }
    }
    
    public void addFromCompactIpHTTPHeaders( HTTPHeader[] headers, URN urn )
    {
        List altLocList = parseCompactIpAltLocFromHTTPHeaders( headers, urn );
        Iterator iterator = altLocList.iterator();
        while( iterator.hasNext() )
        {
            addAlternateLocation( (AlternateLocation)iterator.next() );
        }
    }

    /**
     * Returns a HTTPHeader array for this host address. This will include all
     * alternate locations except the one that equals the host address and the
     * one contained in the sendAltLocList. The newly send alt locs are added
     * to the sendAltLocList inside this call.
     * 
     * @param the name of the http header to generate, use X-ALT or X-NAlt.
     * @param hostAddress the host address that gets the alternate locations.
     * @param sendAltLocList a list of already send alt locs.
     * @return a HTTPHeader array for this host address
     */
    public HTTPHeader getAltLocHTTPHeaderForAddress( String headerName,
        DestAddress hostAddress, Set sendAltLocSet )
    {
        if ( isEmpty() )
        {
            return null;
        }
        int count = 0;
        StringBuffer headerValue = new StringBuffer();
        synchronized ( altLocationMap )
        {
            Iterator iterator = altLocationMap.keySet().iterator();
            while( iterator.hasNext() )
            {
                AlternateLocation altLoc = (AlternateLocation)iterator.next();
                
                // filter out alt locations to given host address..
                if ( hostAddress.getHostName().equals( altLoc.getHostAddress().getHostName() ) )
                {
                    continue;
                }
                // filter out already send alt locs...
                if ( sendAltLocSet.contains( altLoc ) )
                {
                    continue;
                }
                
                if ( count > 0 )
                {
                    headerValue.append( "," );
                }
                headerValue.append( altLoc.getHTTPString() );
                sendAltLocSet.add( altLoc );
                count ++;
                if ( count == 10 )
                {
                    break;
                }
            }
        }
        if ( headerValue.length() > 0 )
        {
            HTTPHeader altLocHeader = new HTTPHeader( headerName,
                headerValue.toString() );
            return altLocHeader;
        }
        else
        {
            return null;
        }
    }

    /**
     * Returns the number of alternate locations in the container.
     * @return the number of alternate locations in the container.
     */
    public synchronized int getSize()
    {
        if ( altLocationMap == null )
        {
            return 0;
        }
        else
        {
            return altLocationMap.size();
        }
    }

    /**
     * Indicates whether the container is empty or not.
     * @return <code>true</code> if the container is empty, <code>false</code>
     *         otherwise.
     */
    public synchronized boolean isEmpty()
    {
        if ( altLocationMap == null )
        {
            return true;
        }
        else
        {
            return altLocationMap.isEmpty();
        }
    }

    public synchronized void createXJBAlternateLocationList( List list )
        throws JAXBException
    {
        if ( altLocationMap == null )
        {
            return;
        }
        Iterator iterator = altLocationMap.keySet().iterator();
        ObjectFactory objFactory = new ObjectFactory();
        while( iterator.hasNext() )
        {
            AlternateLocation altLoc = (AlternateLocation)iterator.next();
            XJBAlternateLocation xjbAltLoc =  objFactory.createXJBAlternateLocation();
            xjbAltLoc.setHostAddress( altLoc.getHostAddress().getFullHostName() );
            xjbAltLoc.setURN( altLoc.getURN().getAsString() );
            list.add( xjbAltLoc );
        }
    }

    /**
     * Returns a string representation of the object.
     * @return a string representation of the object.
     */
    public synchronized String toString()
    {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("Alt-Locations(SHA1: ");
        stringBuffer.append( urn.getAsString() );
        stringBuffer.append(")=[ ");
        if ( altLocationMap != null )
        {
            Iterator iterator = altLocationMap.keySet().iterator();
            AlternateLocation altLoc;
            while( iterator.hasNext() )
            {
                altLoc = (AlternateLocation)iterator.next();
                stringBuffer.append( altLoc.toString() );
                stringBuffer.append(", ");
            }
        }
        stringBuffer.append( " ]" );
        return stringBuffer.toString();
    }


    private void initMap()
    {
        if ( altLocationMap == null )
        {
            altLocationMap = new SequencedHashMap();
        }
    }
    
    /**
     * Parses AlternateLocations from the values of the given http headers and
     * returns a List containing the results. 
     * @param headers HTTPHeaderGroup to parse the AlternateLocations from
     * @return List containing all AlternateLocations found.
     */
    public static List parseUriResAltLocFromHTTPHeaders( HTTPHeader[] headers )
    {
        if ( headers.length == 0 )
        {
            return Collections.EMPTY_LIST;
        }
        List altLocList = new ArrayList();
        StringTokenizer tokenizer;
        for ( int i = 0; i < headers.length; i++ )
        {
            HTTPHeader header = headers[i];
            tokenizer = new StringTokenizer( header.getValue(), ",");

            while( tokenizer.hasMoreTokens() )
            {
                try
                {
                    String altLocationStr = tokenizer.nextToken().trim();
                    AlternateLocation altLocation = AlternateLocation.parseUriResAltLoc(
                        altLocationStr );
                    if ( altLocation == null )
                    {
                        continue;
                    }
                    altLocList.add( altLocation );
                }
                // TODO filter this out
                // this is currently just to make sure we have no untested error cases.
                // and the download will not fail because of this.
                catch ( Exception exp )
                {
                    NLogger.error( NLoggerNames.GLOBAL, exp, exp );
                }
            }
        }
        return altLocList;
    }
    
    /**
     * Parses AlternateLocations from the values of the given http headers and
     * returns a List containing the results. 
     * @param headers HTTPHeaderGroup to parse the AlternateLocations from
     * @return List containing all AlternateLocations found.
     */
    public static List parseCompactIpAltLocFromHTTPHeaders( HTTPHeader[] headers, URN urn )
    {
        if ( headers.length == 0 )
        {
            return Collections.EMPTY_LIST;
        }
        List altLocList = new ArrayList();
        StringTokenizer tokenizer;
        for ( int i = 0; i < headers.length; i++ )
        {
            HTTPHeader header = headers[i];
            tokenizer = new StringTokenizer( header.getValue(), ",");

            while( tokenizer.hasMoreTokens() )
            {
                try
                {
                    String altLocationStr = tokenizer.nextToken().trim();
                    AlternateLocation altLocation = AlternateLocation.parseCompactIpAltLoc(
                        altLocationStr, urn );
                    if ( altLocation == null )
                    {
                        continue;
                    }
                    altLocList.add( altLocation );
                }
                // TODO filter this out
                // this is currently just to make sure we have no untested error cases.
                // and the download will not fail because of this.
                catch ( Exception exp )
                {
                    NLogger.error( NLoggerNames.GLOBAL, exp, exp );
                }
            }
        }
        return altLocList;
    }
}