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
 *  Created on 01.11.2005
 *  --- CVS Information ---
 *  $Id: AddressUtils.java,v 1.5 2005/11/13 10:08:09 gregork Exp $
 */
package phex.common.address;

import phex.net.presentation.PresentationManager;
import phex.security.PhexSecurityManager;

public class AddressUtils
{

    /**
     * Converts the given bytes of an IP to a string representation.
     */
    public static String ip2string(byte[] ip)
    {
        if ( ip == null )
        {
            throw new NullPointerException("Ip is null!");
        }
        assert ip.length == 4;
        
        return (ip[0] & 0xff) + "." 
            + (ip[1] & 0xff) + "." 
            + (ip[2] & 0xff) + "." 
            + (ip[3] & 0xff);
    }

    /**
     * Returns true if the given host name represents a IP address.
     * @param hostName
     * @return true if the given host name represents a IP address.
     */
    public static boolean isIPHostName( String hostName )
    {
        int portSeparatorIdx = hostName.indexOf( ':' );
        if ( portSeparatorIdx != -1 )
        {// cut of port.
            hostName = hostName.substring( 0, portSeparatorIdx );
        }
        char[] data = hostName.toCharArray();
        int hitDots = 0;
        for(int i = 0; i < data.length; i++)
        {
            char c = data[i];
            if (c < 48 || c > 57)
            { // !digit
                return false;
            }
            while(c != '.')
            {
                //      '0'       '9'
                if (c < 48 || c > 57)
                { // !digit
                    return false;
                }
                if (++i >= data.length)
                {
                    break;
                }
                c = data[i];
            }
            hitDots++;
        }
        if(hitDots != 4 || hostName.endsWith("."))
        {
            return false;
        }
        return true;
    }

    /**
     * Parses and validates a host address string in the format
     * ip:port. If no port is given 6346 is default.
     * Validation includes host ip access security check.
     * @param addressString in format ip:port or ip (port default 6346)
     * @return the parsed host address or null in case ther is any kind of error.
     * @throws MalformedDestAddressException
     */
    public static DestAddress parseAndValidateAddress( String addressString,
        boolean isPrivateIpAllowed )
        throws MalformedDestAddressException
    {
        byte[] ip = AddressUtils.parseIP( addressString );
        if ( ip == null )
        {
            throw new MalformedDestAddressException( "Invalid IP: "
                + addressString );
        }
        int port = AddressUtils.parsePort( addressString );
        // Limewire is not setting default port...
        if ( port == -1 )
        {
            port = 6346;
        }
        else if ( !AddressUtils.isPortInRange( port ) )
        {
            throw new MalformedDestAddressException( "Port out of range: "
                + addressString );
        }
        IpAddress ipAddress = new IpAddress( ip );
        DestAddress hostAddress = 
            PresentationManager.getInstance().createHostAddress(ipAddress, port);
        if ( !hostAddress.isValidAddress() )
        {
            throw new MalformedDestAddressException( "Invalid IP: "
                + addressString );
        }
        if ( !isPrivateIpAllowed && hostAddress.isSiteLocalAddress() )
        {
            throw new MalformedDestAddressException( "Private IP: "
                + addressString );
        }
        
        byte access = PhexSecurityManager.getInstance().controlHostIPAccess(
            hostAddress.getIpAddress().getHostIP() );
        switch ( access )
        {
            case PhexSecurityManager.ACCESS_DENIED:
            case PhexSecurityManager.ACCESS_STRONGLY_DENIED:
            throw new MalformedDestAddressException( "Host access denied: "
                + addressString );
        }
        return hostAddress;
    }

    /**
     * Parses a unsigned int value to a byte array containing the IP
     * @param ip
     * @return
     */
    public static byte[] parseIntIP( String ip )
    {
        long IP = Long.parseLong( ip );
        
        // create byte[] from int address
        byte[] addr = new byte[4];
        addr[0] = (byte) ((IP >>> 24) & 0xFF);
        addr[1] = (byte) ((IP >>> 16) & 0xFF);
        addr[2] = (byte) ((IP >>> 8) & 0xFF);
        addr[3] = (byte) (IP & 0xFF);
        return addr;
    }

    /**
     * Trys to parse the given string. The String must represent a numerical IP
     * address in the format %d.%d.%d.%d. A possible attached port will be cut of.
     * @return the ip represented in a byte[] or null if not able to parse the ip.
     */
    public static byte[] parseIP( String hostIp )
    {
        /* The string (probably) represents a numerical IP address.
         * Parse it into an int, don't do uneeded reverese lookup,
         * leave hostName null, don't cache.  If it isn't an IP address,
         * (i.e., not "%d.%d.%d.%d") or if any element > 0xFF,
         * it is a hostname and we return null.
         * This seems to be 100% compliant to the RFC1123 spec.
         */
        int portSeparatorIdx = hostIp.indexOf( ':' );
        if ( portSeparatorIdx != -1 )
        {// cut of port.
            hostIp = hostIp.substring( 0, portSeparatorIdx );
        }
    
        char[] data = hostIp.toCharArray();
        int IP = 0x00;
        int hitDots = 0;
    
        for(int i = 0; i < data.length; i++)
        {
            char c = data[i];
            if (c < 48 || c > 57)
            { // !digit
                return null;
            }
            int b = 0x00;
            while(c != '.')
            {
                //      '0'       '9'
                if (c < 48 || c > 57)
                { // !digit
                    return null;
                }
                b = b * 10 + c - '0';
    
                if (++i >= data.length)
                {
                    break;
                }
                c = data[i];
            }
            if(b > 0xFF)
            { /* bogus - bigger than a byte */
                return null;
            }
            IP = (IP << 8) + b;
            hitDots++;
        }
        if(hitDots != 4 || hostIp.endsWith("."))
        {
            return null;
        }
        // create byte[] from int address
        byte[] addr = new byte[4];
        addr[0] = (byte) ((IP >>> 24) & 0xFF);
        addr[1] = (byte) ((IP >>> 16) & 0xFF);
        addr[2] = (byte) ((IP >>> 8) & 0xFF);
        addr[3] = (byte) (IP & 0xFF);
        return addr;
    }

    /**
     * Trys to parse the port of a host string. If no port could be parsed -1 is
     * returned.
     * @throws MalformedDestAddressException if port is out of range.
     */
    public static int parsePort( String hostName ) 
    {
        int portIdx = hostName.indexOf( ':' );
        if ( portIdx == -1 )
        {
            return -1;
        }
    
        String portString = hostName.substring( portIdx + 1);
        char[] data = portString.toCharArray();
        int port = 0;
        for ( int i = 0; i < data.length; i++ )
        {
            char c = data[i];
            //      '0'       '9'
            if (c < 48 || c > 57)
            { // !digit
                break;
            }
            // shift left and add value
            port = port * 10 + c - '0';
        }
        // no port or out of range
        if ( !isPortInRange( port ) )
        {
            return -1;
        }
        return port;
    }

    public static String toIntValueString( byte[] ip )
    {
        int v1 =  ip[3]        & 0xFF;
        int v2 = (ip[2] <<  8) & 0xFF00;
        int v3 = (ip[1] << 16) & 0xFF0000;
        int v4 = (ip[0] << 24);
        long ipValue = ((long)(v4|v3|v2|v1)) & 0x00000000FFFFFFFFl;
        return String.valueOf( ipValue );
    }

    /**
     * Validates a port value if it is in range ( 1 - 65535 )
     * 
     * @param port the port to verify in int value. Unsigned short ports must be
     * converted to singned int to let this function work correctly.
     * @return true if the port is in range, false otherwise.
     */
    public static boolean isPortInRange( int port )
    {
        return ( port & 0xFFFF0000 ) == 0 && port != 0;
    }
}
