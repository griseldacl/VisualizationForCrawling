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
 *  $Id: QueryMsg.java,v 1.1 2005/11/03 17:06:26 gregork Exp $
 */
package phex.msg;

import java.io.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import phex.common.URN;
import phex.utils.*;

/**
 * <p>Encapsulation of a Gnutella query message.</p>
 *
 * <p>Queries are encouraged to be smaller than 256 bytes. Larger messages may
 * be dropped. You can expect messages larger than 4k to be dropped.</P>
 *
 * <p>Currently, there is no support in this class for extentions such as HUGE,
 * XML or GGEP.
 *
 *
 * <p>As of Feb, 2003, this class supports and encapsulates the new Minimum Speed
 * definition, as described at
 *      http://groups.yahoo.com/group/the_gdf/files/Proposals/MinSpeed.html
 * This definition change is encapsulated within this class.</p>
 *
 */
public class QueryMsg extends Message
{
    /**
     * For outgoing queries, specifies whether we want to receive
     * Limewire-style XML metadata results.  For more info, see MsgQuery.java
     */
    public static final boolean IS_PHEX_CAPABLE_OF_XML_RESULTS = false;
    
    /** 
     * MINSPEED_*_BIT - these are the bit numbers for the new
     * meanings of MinSpeed bits.
     */
    private static final int MINSPEED_BITBASED_BIT = 15;
    private static final int MINSPEED_FIREWALL_BIT = 14;
    private static final int MINSPEED_XML_BIT = 13;

    /**
     * This indicates if the min speed field is using the new bit based
     * representation.
     */
    private boolean minSpeedIsBitBased;
    
    /**
     * Indicates if the query requester is firewalled. 
     * Variables represent a bit carried inside the MinSpeed field.
     */
    private boolean requesterIsFirewalled;
    
    /**
     * Indicates if the query requester is capable of receiving XML results. 
     * Variables represent a bit carried inside the MinSpeed field.
     */
    private boolean requesterIsXmlResultsCapable;

    /**
     * <p>The search string for this query.</p>
     *
     * <p>Servents should treat this search string as a list of keywords. They
     * should respond with files that match all keywords. They may chose to only
     * respond with files with all keywords in the query order. Case will be
     * ignored. Wildcards and regular expressions are not supported.</p>
     *
     * <p>If the query is four spaces "    " and TTL=1 and hops=0, it should be
     * interpreted as a request for a complete directory listing.</p>
     */
    private String searchString;

    /**
     * <p>The un-parsed body of the query.</p>
     *
     * <p>For queries that are being forwarded, this body will include all extra
     * data. For queries built using this API, there is no way currently to add
     * extra information to this body.</p>
     */
    private byte[] body;

    /**
     * Defines if the body of the message is already parsed.
     */
    private boolean isParsed;

    /**
     * Contains all urns to search for. This attribute is null if urns are not
     * in the query.
     */
    private List queryURNList;


    /**
     * Create a query with a new header, a given ttl and search string.
     * <p>The header will be modified so that its function property becomes
     * MsgHeader.sQuery. The header argument is owned by this object.</p>
     */
    public QueryMsg( byte ttl, String aSearchString, URN queryURN,
        boolean isRequesterCapableOfXmlResults, boolean isRequesterBehindFirewall )
    {// TODO3 extend query to dispatch a couple of URNs, this can be used
     // for researching multiple candidates for multiple files. But verify
     // that this works with common clients.
        super( new MsgHeader( MsgHeader.QUERY_PAYLOAD, ttl, 0 ) );
        searchString = aSearchString;
        if ( StringUtils.isEmpty( searchString ) )
        {
            searchString = "\\";
        }
        if ( queryURN != null )
        {
            queryURNList = new ArrayList( 1 );
            queryURNList.add( queryURN );
        }
        else
        {
            queryURNList = Collections.EMPTY_LIST;
        }
        
        minSpeedIsBitBased = true;
        requesterIsFirewalled = isRequesterBehindFirewall;
        requesterIsXmlResultsCapable = isRequesterCapableOfXmlResults;

        try
        {
            buildBody();
        }
        catch (IOException e)
        {// should never happen
            NLogger.error( NLoggerNames.MESSAGE_ENCODE_DECODE, e, e );
        }
        getHeader().setDataLength( body.length );
    }

    /**
     * <p>Create a new MsgQuery with its header and body.</p>
     *
     * <p>The header will be modified so that its function property becomes
     * MsgHeader.sQuery. The header argument is owned by this object.</p>
     *
     * <p>The body is not parsed directly
     * cause some queries are just forwarded without the need of being completely
     * parsed. This allows the extention data (such as GGEP blocks) to be
     * forwarded despite there being no API to modify these.</p>
     *
     * @param header the MsgHeader to associate with the new message
     * @param aBody the message body
     */
    public QueryMsg( MsgHeader header, byte[] aBody )
    {
        super( header );
        getHeader().setPayloadType(MsgHeader.QUERY_PAYLOAD);
        body = aBody;

        // parse the body
        parseBody();
    }
    
    public QueryMsg( QueryMsg query, byte ttl )
    {
        super( new MsgHeader( MsgHeader.QUERY_PAYLOAD, 0 ) );
        getHeader().copy( query.getHeader() );
        getHeader().setTTL( ttl );
        this.body = query.body;
        this.searchString = query.searchString;
        this.minSpeedIsBitBased = query.minSpeedIsBitBased;
        this.requesterIsFirewalled = query.requesterIsFirewalled;
        this.requesterIsXmlResultsCapable = query.requesterIsXmlResultsCapable;
        this.isParsed = query.isParsed;
        this.queryURNList = new ArrayList( query.queryURNList );
    }

    /**
     * Determine whether the query uses the MinSpeed
     * field as a 'reclaimed' field, where the bits have individual meanings.
     * @return boolean
     */
    private boolean isMinSpeedBitBased()
    {
        return minSpeedIsBitBased;
    }

    /**
     * Determine whether the query source is a firewalled servent.
     * This can only be true when the query is using the new MinSpeed meaning.
     * @return boolean
     */
    public boolean isRequesterFirewalled()
    {
        return minSpeedIsBitBased && requesterIsFirewalled;
    }

    /**
     * Determine whether the query source is capable of handling XML results.
     * This can only be true when the query is using the new MinSpeed meaning.
     * @return boolean
     */
    public boolean isRequesterXmlResultsCapable()
    {
        return minSpeedIsBitBased && requesterIsXmlResultsCapable;
    }

    /**
     * Set whether the query uses the MinSpeed
     * field as a bit-based field, where the bits have individual meanings.
     * @return boolean
     */
    private void setMinSpeedIsBitBased(boolean newValue)
    {
        minSpeedIsBitBased = newValue;
    }

    /**
     * Set whether the query source is a firewalled servent.
     * This field will only be used when MinSpeed is bit-based.
     * @return boolean
     */
    private void setRequesterIsFirewalled(boolean newValue)
    {
        requesterIsFirewalled = newValue;
    }

    /**
     * Set whether the query source is capable of handling XML results.
     * This field will only be used when MinSpeed is bit-based.
     * @return boolean
     */
    private void setRequesterIsXmlResultsCapable(boolean newValue)
    {
        requesterIsXmlResultsCapable = newValue;
    }

    /**
     * Utility method -- Determine whether a particular bit in a short is set.
     *
     * @param shortIn   The value whose bit will be checked
     * @param bitNumber The bit number to check (0 is least significant, 15 is most significant)
     * @return boolean
     */
    private static boolean isBitSet(short shortIn, int bitPos)
    {
        int bitValue = 1 << bitPos;
        return (shortIn & bitValue) != 0;
    }

    /**
     * Utility method -- Set a bit in a short.
     *
     * @param shortIn   The value whose bit may be changed.
     * @param bitNumber The bit number to set/clear (0 is least significant, 15 is most significant)
     * @return boolean
     */
    private static short setBit(short shortIn, int bitPos)
    {
        short mask = (short) (1 << bitPos);
        return (short) (shortIn | mask);
    }

    /**
     * Version of setBit() which accepts Objects (for testing)
     * @param shortIn
     * @param bitPos
     * @return short
     */
    private static short setBit(Short shortIn, Integer bitPos)
    {
        return setBit(shortIn.shortValue(), bitPos.intValue());
    }

    /**
     * Version of isBitSet() which accepts Objects (for testing)
     * @param shortIn
     * @param bitPos
     * @return boolean
     */
    private static boolean isBitSet(Short shortIn, Integer bitPos)
    {
        return isBitSet(shortIn.shortValue(), bitPos.intValue());
    }

    /**
     * Returns a Iterator of queried URNs to look for.
     */
    public URN[] getQueryURNs()
    {
        URN[] urns = new URN[ queryURNList.size() ];
        return (URN[]) queryURNList.toArray( urns );
    }

    /**
     * Indicates if the query carrys query urns.
     * @return true if the query carrys query urns.
     */
    public boolean hasQueryURNs()
    {
        return !queryURNList.isEmpty();
    }

    /**
     * <p>Get the search string for this query.</p>
     *
     * <p>Servents should treat this search string as a list of keywords. They
     * should respond with files that match all keywords. They may chose to only
     * respond with files with all keywords in the query order. Case will be
     * ignored. Wildcards and regular expressions are not supported.</p>
     *
     * <p>If the query is four spaces "    " and TTL=1 and hops=0, it should be
     * interpreted as a request for a complete directory listing.</p>
     *
     * @return  the String representation of the query
     */
    public String getSearchString()
    {
        return searchString;
    }
    
    public void writeMessage( GnutellaOutputStream outStream )
        throws IOException
    {
        getHeader().writeHeader( outStream );
        outStream.write( body, 0, body.length );
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer(100);

        buf.append("[")
            .append(getHeader())
            .append(", MinSpeedIsBitBased=")
            .append(minSpeedIsBitBased);
        if (minSpeedIsBitBased)
        {
            buf.append(", RequesterIsFirewalled=")
            .append(requesterIsFirewalled)
            .append(", RequesterIsXmlResultsCapable=")
            .append(requesterIsXmlResultsCapable);
        }

        buf.append(", SearchString=")
            .append(searchString)
            .append("]");

        return buf.toString();
    }

    private void buildBody()
        throws IOException
    {
        ByteArrayOutputStream bodyStream = new ByteArrayOutputStream( );
        short complexMinSpeed = buildComplexMinSpeed();
        // we send min speed in big endian byte order since we only care for
        // bit based min speed and go according to bit based min speed specs.
        IOUtil.serializeShort( complexMinSpeed, bodyStream );
        
        bodyStream.write( searchString.getBytes("UTF-8") );
        bodyStream.write( 0 );
        
        if ( queryURNList.size() == 0 )
        {
            // request sha1 URLs only
            bodyStream.write( (URN.URN_PREFIX + URN.SHA1).getBytes() );
        }
        else
        {// we query for urns... add list content to body...
            Iterator iterator = queryURNList.iterator();
            while ( iterator.hasNext() )
            {
                URN urn = (URN)iterator.next();
                bodyStream.write( urn.getAsString().getBytes() );
            }
        }
        bodyStream.write( 0 );
        
        body = bodyStream.toByteArray();
    }

    private void parseBody()
    {
        try
        {
            ByteArrayInputStream inStream = new ByteArrayInputStream(body);
            // Get & parse the MinSpeed field
            // we read min speed in big endian byte order since we only care for
            // bit based min speed and go according to bit based min speed specs.
            short minSpeedField = IOUtil.deserializeShort(inStream);
            parseMinSpeed(minSpeedField);

            byte[] queryBytes = IOUtil.readBytesToNull(inStream);
            searchString = new String(queryBytes, "UTF-8");

            // read extension area... each extension is seperated by a 0x1c FS
            // "file separator" character

            byte[] extensionBytes = IOUtil.readBytesToNull(inStream);

            // TODO2 check about encodings.. should work on byte[] instead.
            String extensionArea = new String(extensionBytes);

            // tokenize extension area
            StringTokenizer tokenizer = new StringTokenizer(extensionArea,
                "\u001c");
            while (tokenizer.hasMoreTokens())
            {
                String extensionToken = tokenizer.nextToken();
                // first check if this is a query by URN
                // ( urn:<NID>:<NSS> )
                if (URN.isValidURN(extensionToken))
                {
                    URN urn = new URN(extensionToken);
                    if (queryURNList == null)
                    {
                        queryURNList = new ArrayList(tokenizer.countTokens());
                    }
                    queryURNList.add(urn);

                    // we dont track URN type request. we always return sha1 type urns
                    // on query answers since there is currently only one known urn type
                }
                // otherwise is must be URN type request or extension meta data or GGEP.. or???
            }
            // no URLs in query.. use empty list.
            if (queryURNList == null)
            {
                queryURNList = Collections.EMPTY_LIST;
            }
        }
        catch (IOException e)
        {
            NLogger.error( NLoggerNames.MESSAGE_ENCODE_DECODE, e, e );
        }
    }

    /**
     *
     * @return short
     */
    private void parseMinSpeed(short minSpeedIn)
    {
        minSpeedIsBitBased = isBitSet(minSpeedIn, MINSPEED_BITBASED_BIT);

        if (isMinSpeedBitBased())
        // Incoming MinSpeed IS bit-based
        {
            requesterIsFirewalled = isBitSet(minSpeedIn, MINSPEED_FIREWALL_BIT);
            requesterIsXmlResultsCapable = isBitSet(minSpeedIn, MINSPEED_XML_BIT);
        }
/*
        if (Logger.isLevelLogged(Logger.FINEST))
        {
            String bin = Integer.toBinaryString(minSpeedIn);
            if (bin.length() > 16) bin = bin.substring(16);
            StringBuffer buf = new StringBuffer(100);
            buf.append("MsgQuery.parseMinSpeed: MinSpeed=")
                .append(minSpeedIn)
                .append(" (")
                .append(bin)
                .append(")  --  ")
                .append("     bit-based: ")
                .append(minSpeedIsBitBased)
                .append("     firewalled: ")
                .append(requesterIsFirewalled)
                .append("     xml: ")
                .append(requesterIsXmlResultsCapable);

            Logger.logMessage(Logger.FINEST, Logger.NETWORK, buf.toString());
        }
*/
    }

    /**
     * This method takes all of the discreet local variables and compiles them into
     * a short, in the bit-based format for MinSpeed.  It is called when the query is
     * being serialized for output to another servent.
     * @return short containing all of the appropriate bits set
     */
    private short buildComplexMinSpeed()
    {
        // Start with a real minimum speed numeric value
        short complexMinSpeed = (short) 0;

        // Set any appropriate bits
        if (minSpeedIsBitBased)
        {
            // First, indicate that the field is bit-ased
           complexMinSpeed = setBit(complexMinSpeed, MINSPEED_BITBASED_BIT);

            // Firewall bit
            if (requesterIsFirewalled)
            {
                complexMinSpeed = setBit(complexMinSpeed, MINSPEED_FIREWALL_BIT);
            }

            // XML bit
            if (requesterIsXmlResultsCapable)
            {
                complexMinSpeed = setBit(complexMinSpeed, MINSPEED_XML_BIT);
            }
        }

        return complexMinSpeed;
    }

}

