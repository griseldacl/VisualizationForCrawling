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
package phex.http;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import phex.utils.NLogger;
import phex.utils.NLoggerNames;


public class HTTPRangeSet
{
    private static final String BYTES = "bytes";
    public static final int NOT_SET = -1;

    private ArrayList rangeList;

    public HTTPRangeSet()
    {
        rangeList = new ArrayList( 4 );
    }

    /**
     * Creates a HTTPRange object for suffix lengths like '-500' which requests
     * the last 500 bytes of a file.
     * @param suffixLength the suffix length.
     */
    public HTTPRangeSet( long suffixLength )
    {
        this();
        Range entry = new Range( suffixLength );
        rangeList.add( entry );
    }

    /**
     * Creates a HTTPRange object with a start and end offset. The end offset
     * can be NOT_SET to form request like '100-'
     * @param startOffset the start offset.
     * @param endOffset the end offset.
     */
    public HTTPRangeSet( long startOffset, long endOffset )
    {
        this();
        Range entry = new Range( startOffset, endOffset );
        rangeList.add( entry );
    }

    public int size()
    {
        return rangeList.size();
    }

    public void addRange( long suffixLength )
    {
        Range entry = new Range( suffixLength );
        rangeList.add( entry );
    }

    public void addRange( long startOffset, long endOffset )
    {
        Range entry = new Range( startOffset, endOffset );
        rangeList.add( entry );
    }

    public Range getFirstRange()
    {
        if ( rangeList.size() > 0 )
        {
            return (Range)rangeList.get( 0 );
        }
        return null;
    }

    public Iterator getIterator()
    {
        return rangeList.iterator();
    }

    public Collection getRangeSet()
    {
        return rangeList;
    }

    public String buildXAvailableRangesString()
    {
        if ( rangeList.size() == 0 )
        {
            return null;
        }
        StringBuffer buffer = new StringBuffer( 30 );
        buffer.append( "bytes " );
        Iterator iterator = rangeList.iterator();
        Range range;
        while( iterator.hasNext() )
        {
            range = (Range)iterator.next();
            buffer.append( range.buildHTTPRangeString() );
            if ( iterator.hasNext() )
            {
                buffer.append( ',' );
            }
        }
        return buffer.toString();
    }

    /**
     * Trys to parse the http range.
     * Returns null if there is a parsing error.
     * @param httpRangeValue
     * @return
     */
    public static HTTPRangeSet parseHTTPRangeSet( String httpRangeSetValue )
    {
        httpRangeSetValue = httpRangeSetValue.toLowerCase();
        if ( !httpRangeSetValue.startsWith( BYTES ) )
        {
            return null;
        }
        
        // this is to fix Limewire sending 'X-Available-Ranges: bytes'
        // with no further values for empty ranges.
        if ( httpRangeSetValue.length() < 6 )
        {
            return new HTTPRangeSet();
        }

        // cut of 'bytes=' or 'bytes '
        try
        {
            httpRangeSetValue = httpRangeSetValue.substring( 6 ).trim();
        }
        catch ( StringIndexOutOfBoundsException exp )
        {
            NLogger.error( NLoggerNames.GLOBAL, 
                "Invalid RangeSet value: '" + httpRangeSetValue + "'.", exp );
            return null;
        }
        int startIdx = 0;
        int colonIdx;
        String httpRangeValue;
        HTTPRangeSet httpRangeSet = new HTTPRangeSet();
        do
        {
            colonIdx = httpRangeSetValue.indexOf( ',', startIdx );
            if ( colonIdx == -1 )
            {
                colonIdx = httpRangeSetValue.length();
            }
            httpRangeValue = httpRangeSetValue.substring( startIdx, colonIdx ).trim();

            if ( httpRangeValue.charAt( 0 ) == '-')
            { // we have a suffix byte range request in the form: 'bytes=-500'.
              // This is a request for the last 500 bytes
                int endIdx = httpRangeValue.indexOf( ' ' );
                if ( endIdx == -1 )
                {
                    endIdx = httpRangeValue.length();
                }
                // cut of '-' and possible unused end values.
                String suffixByteRangeStr = httpRangeValue.substring( 1, endIdx );
                try
                {
                    long suffixLength = Long.parseLong( suffixByteRangeStr );
                    httpRangeSet.addRange( suffixLength );
                }
                catch (NumberFormatException exp )
                {
                    return null;
                }
            }
            else
            {// we have a byte range request in the form: 'bytes=100-500' or 'bytes=100-'.
             // This is a request for bytes 100-500 or 100 to file end.
                int dashIdx = httpRangeValue.indexOf( '-' );
                long startOffset;
                long endOffset;
                try
                {
                    String startOffsetStr = httpRangeValue.substring( 0, dashIdx );
                    startOffset = Long.parseLong( startOffsetStr );
                    if ( dashIdx + 1 >= httpRangeValue.length() )
                    {
                        httpRangeSet.addRange( startOffset, NOT_SET );
                    }
                    else
                    {
                        String endOffsetStr = httpRangeValue.substring( dashIdx + 1 );
                        endOffset = Long.parseLong( endOffsetStr );
                        httpRangeSet.addRange( startOffset, endOffset );
                    }
                }
                catch ( IndexOutOfBoundsException exp )
                {
                    return null;
                }
                catch ( NumberFormatException exp )
                {
                    return null;
                }
            }
            // parse the next range
            startIdx = colonIdx + 1;
        }
        while( colonIdx != httpRangeSetValue.length() );
        return httpRangeSet;
    }

    public static void main( String args[] )
    {
        Iterator iterator = parseHTTPRangeSet( "bytes=434176-2640890" ).getIterator();
        while( iterator.hasNext() )
        {
            System.out.println( iterator.next() );
        }

        iterator = parseHTTPRangeSet( "bytes=434176-2640890,1-4,-7,7-10" ).getIterator();
        while( iterator.hasNext() )
        {
            System.out.println( iterator.next() );
        }
    }
}

