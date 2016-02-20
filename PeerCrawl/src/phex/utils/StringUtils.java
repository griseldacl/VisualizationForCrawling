/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2005 Phex Development Group
 *                2001 Peter Hunnisett (hunnise@yahoo.com)
 *                2000 William W. Wong (williamw@jps.net)
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
package phex.utils;

import java.text.NumberFormat;
import java.util.Random;
import java.util.StringTokenizer;

import phex.common.Cfg;

/**
* A collection of custom utilities to modify or specially display Strings.
**/

public final class StringUtils
{
    public static final String FILE_DELIMITERS = " -._+/*()[]\\";

    public static NumberFormat FILE_LENGTH_FORMAT = NumberFormat.getInstance();
    
    /**
     * <p>Checks if a String is empty ("") or null.</p>
     *
     *
     * @param val  the String to check, may be null
     * @return <code>true</code> if the String is empty or null
     */
    public static boolean isEmpty( String val )
    {
        return ( val == null || val.length() == 0 );
    }

    /**
     * Converts points and dashes to spacesand removes possible
     * file extensions. This should be used to create search-terms
     * from filenames.
     **/
    public static String createNaturalSearchTerm( String searchTerm )
    {
        // Is there any reason why the extension was cut of?? 
        // since build 84 we dont cut of the extension anymore 
        //int extensionIdx = searchTerm.lastIndexOf(".");
        //if ( extensionIdx >= 0 )
        //{
        //    // cut of file extension
        //    searchTerm = searchTerm.substring( 0, extensionIdx );
        //}

        StringTokenizer tokenizer = new StringTokenizer( searchTerm, ",-._()[]" );
        int tokenCount = tokenizer.countTokens();
        if ( tokenCount == 0 )
        {// no tokens -> the term must be already natural.
            return searchTerm;
        }

        String token;
        StringBuffer searchTermBuffer = new StringBuffer( searchTerm.length() );
        while( tokenizer.hasMoreTokens() )
        {
            token = tokenizer.nextToken().trim();
            if ( token.length() >= Cfg.MIN_SEARCH_TERM_LENGTH )
            {
                searchTermBuffer.append( token );
                searchTermBuffer.append( ' ' );
            }
        }
        return searchTermBuffer.toString();
    }
    
    /**
     * <p>Joins the elements of the provided array into a single String
     * containing the provided list of elements.</p>
     *
     * <p>No delimiter is added before or after the list.
     * A <code>null</code> separator is the same as an empty String ("").
     * Null objects or empty strings within the array are represented by
     * empty strings.</p>
     *
     * <pre>
     * StringUtils.join(null, *)                = null
     * StringUtils.join([], *)                  = ""
     * StringUtils.join([null], *)              = ""
     * StringUtils.join(["a", "b", "c"], "--")  = "a--b--c"
     * StringUtils.join(["a", "b", "c"], null)  = "abc"
     * StringUtils.join(["a", "b", "c"], "")    = "abc"
     * StringUtils.join([null, "", "a"], ',')   = ",,a"
     * </pre>
     *
     * @param array  the array of values to join together, may be null
     * @param separator  the separator character to use, null treated as ""
     * @return the joined String, <code>null</code> if null array input
     * 
     * Taken from org.apache.commons.lang.StringUtils
     */
    public static String join(Object[] array, String separator)
    {
        if ( array == null ) { return null; }
        if ( separator == null )
        {
            separator = "";
        }
        int arraySize = array.length;

        // ArraySize ==  0: Len = 0
        // ArraySize > 0:   Len = NofStrings *(len(firstString) + len(separator))
        //           (Assuming that all Strings are roughly equally long)
        int bufSize = ((arraySize == 0) ? 0
            : arraySize
                * ((array[0] == null ? 16 : array[0].toString().length()) + separator.length()));

        StringBuffer buf = new StringBuffer( bufSize );

        for (int i = 0; i < arraySize; i++)
        {
            if ( i > 0 )
            {
                buf.append( separator );
            }
            if ( array[i] != null )
            {
                buf.append( array[i] );
            }
        }
        return buf.toString();
    }
    
    /**
     * <p>Replaces a String with another String inside a larger String,
     * for the first <code>max</code> values of the search String.</p>
     *
     * <p>A <code>null</code> reference passed to this method is a no-op.</p>
     *
     * <pre>
     * StringUtils.replace(null, *, *, *)         = null
     * StringUtils.replace("", *, *, *)           = ""
     * StringUtils.replace("any", null, *, *)     = "any"
     * StringUtils.replace("any", *, null, *)     = "any"
     * StringUtils.replace("any", "", *, *)       = "any"
     * StringUtils.replace("any", *, *, 0)        = "any"
     * StringUtils.replace("abaa", "a", null, -1) = "abaa"
     * StringUtils.replace("abaa", "a", "", -1)   = "b"
     * StringUtils.replace("abaa", "a", "z", 0)   = "abaa"
     * StringUtils.replace("abaa", "a", "z", 1)   = "zbaa"
     * StringUtils.replace("abaa", "a", "z", 2)   = "zbza"
     * StringUtils.replace("abaa", "a", "z", -1)  = "zbzz"
     * </pre>
     *
     * @param text  text to search and replace in, may be null
     * @param repl  the String to search for, may be null
     * @param with  the String to replace with, may be null
     * @param max  maximum number of values to replace, or <code>-1</code> if no maximum
     * @return the text with any replacements processed,
     *  <code>null</code> if null String input
     */
    public static String replace(String text, String repl, String with, int max) 
    {
        if ( isEmpty( text ) || isEmpty( repl ) || with == null || max == 0)
        {
            return text;
        }
        int start = 0;
        int end = text.indexOf(repl, start);
        if ( end == -1 )
        {
            return text;
        }
        
        int increase = with.length() - repl.length();
        increase =  (increase < 0 ? 0 : increase );
        increase *= (max < 0 ? 16 : (max > 64 ? 64 : max ) );
        
        StringBuffer buf = new StringBuffer( text.length() + increase );
        while ( end != -1)
        {
            buf.append(text.substring(start, end)).append(with);
            start = end + repl.length();
            if (--max == 0)
            {
                break;
            }
            end = text.indexOf( repl, start );
        }
        buf.append(text.substring(start));
        return buf.toString();
    }
    
    
    
    private static Random randomizer = new Random();
    private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6',
        '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    
    public static String generateRandomUUIDString()
    {
        byte[] uuid = new byte[16];
        randomizer.nextBytes( uuid );
        uuid[6] &= (byte) 0x0F; //0000 1111 
        uuid[6] |= (byte) 0x40; //0100 0000 set version 4 (random)
        
        uuid[8] &= (byte) 0x3F; //0011 1111
        uuid[8] |= (byte) 0x80; //1000 0000
        
        // generate string rep...
        StringBuffer buf = new StringBuffer( 36 );
        for(int i = 0; i < 16; i++)
        {
            int val = uuid[i] & 0xFF;
            buf.append( HEX_CHARS[ val >> 4 ] );
            buf.append( HEX_CHARS[ val & 0x0F ] );
        }
        buf.insert(8, '-');
        buf.insert(13, '-');
        buf.insert(18, '-');
        buf.insert(23, '-');
        // -> 00000000-0000-0000-0000-000000000000
        
        return buf.toString();        
    }
}