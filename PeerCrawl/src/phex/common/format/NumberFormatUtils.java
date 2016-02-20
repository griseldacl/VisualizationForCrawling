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
 *  Created on 29.09.2005
 *  --- CVS Information ---
 *  $Id: NumberFormatUtils.java,v 1.3 2005/10/02 23:49:01 gregork Exp $
 */
package phex.common.format;

import java.text.NumberFormat;

import phex.utils.Localizer;

public final class NumberFormatUtils
{
    public static final NumberFormat integerFormat = NumberFormat.getIntegerInstance();
    
    /**
     * Represents 1 Kilo Byte ( 1024 ).
     */
    public static final long ONE_KB = 1024L;
    
    /**
     * Represents 1 Mega Byte ( 1024^2 ).
     */
    public static final long ONE_MB = ONE_KB * 1024L;
    
    /**
     * Represents 1 Giga Byte ( 1024^3 ).
     */
    public static final long ONE_GB = ONE_MB * 1024L;
    
    /**
     * Represents 1 Tera Byte ( 1024^4 ).
     */
    public static final long ONE_TB = ONE_GB * 1024L;

    // dont allow instances
    private NumberFormatUtils() {}

    public static String formatDecimal( double value, int precision )
    {
        String str = String.valueOf( value );
        int idx = str.indexOf('.');
        if ( idx == -1 )
        {
            if ( precision != 0 )
            {
                StringBuffer buffer = new StringBuffer( str );
                // set decimal separator to localized version
                buffer.append( Localizer.getDecimalFormatSymbols().getDecimalSeparator() );
                for ( int i = 0; i < precision; i++ )
                {
                    buffer.append( "0" );
                }
                str = buffer.toString();
            }
        }
        else
        {
            if ( precision == 0 )
            {
                str = str.substring(0, idx);
            }
            else
            {
                int digits = str.length() - idx - 1;
                if ( digits < precision )
                {
                    StringBuffer buffer = new StringBuffer( str );
                    // replace decimal separator with localized version
                    buffer.setCharAt(idx, Localizer.getDecimalFormatSymbols().getDecimalSeparator());
                    for ( int i = 0; i < precision - digits; i++ )
                    {
                        buffer.append( "0" );
                    }
                    str = buffer.toString();
                }
                else if ( digits > precision )
                {
                    StringBuffer buffer = new StringBuffer( str );
                    // replace decimal separator with localized version
                    buffer.setCharAt(idx, Localizer.getDecimalFormatSymbols().getDecimalSeparator());
                    str = buffer.substring( 0, idx + precision + 1);
                }
            }
        }
        return str;
    }
    
    public static String formatSize( long size )
    {
        return integerFormat.format( size );
    }
    
    public static String formatFullByteSize( long size )
    {
        return integerFormat.format( size ) + " " + Localizer.getString( "BytesToken" );
    }

    /**
     * Formats the the size as a most significant number of bytes.
     */
    public static String formatSignificantByteSize( double size )
    {
        String text;
        double divider;
        int precision;
        if (size < NumberFormatUtils.ONE_KB)
        {
            text = Localizer.getString( "BytesToken" );
            divider = 1.0;
            precision = 0;
        }
        else if (size < NumberFormatUtils.ONE_MB)
        {
            text = Localizer.getString( "KBToken" );
            divider = NumberFormatUtils.ONE_KB;
            precision = 1;
        }
        else if (size < NumberFormatUtils.ONE_GB)
        {
            text = Localizer.getString( "MBToken" );
            divider = NumberFormatUtils.ONE_MB;
            precision = 1;
        }
        else if (size < NumberFormatUtils.ONE_TB)
        {
            text = Localizer.getString( "GBToken" );
            divider = NumberFormatUtils.ONE_GB;
            precision = 2;
        }
        else
        {
            text = Localizer.getString( "TBToken" );
            divider = NumberFormatUtils.ONE_TB;
            precision = 3;
        }
        double d = ((double)size) / divider;
        String valStr = formatDecimal(d, precision);
        return valStr + " " + text;
    }

    public static String formatSignificantByteSize( Number number )
    {
        return formatSignificantByteSize( number.doubleValue() );
    };
}