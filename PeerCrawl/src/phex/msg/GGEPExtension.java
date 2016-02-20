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
 *  $Id: GGEPExtension.java,v 1.9 2005/11/03 23:30:12 gregork Exp $
 */
package phex.msg;


import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.utils.*;

/**
 * Used for parsing GGEP extension data.
 */
public class GGEPExtension
{
    /**
     * Parses the HostAddresses of the ALT GGEP extension.
     * Returns null incase the parsing fails.
     * @param ggepBlocks the GGEP Blocks to look in
     * @return the parsed HostAddresses
     */
    public static DestAddress[] parseAltExtensionData( GGEPBlock[] ggepBlocks )
    {
        // The payload is an array of 6-byte entries.  The first 4 bytes encode
        // the IP of the server (in big-endian, as usual), and the remaining
        // 2 bytes encode the port (in little-endian).
        byte[] altLocData = GGEPBlock.getExtensionDataInBlocks( ggepBlocks,
            GGEPBlock.ALTERNATE_LOCATIONS_HEADER_ID );
            
        if ( altLocData == null )
        {
            return null;
        }
            
        // check for valid length
        if ( altLocData.length % 6 != 0 )
        {
            NLogger.warn(NLoggerNames.Network,
                "Invalid ALT GGEPBlock length: " + HexConverter.toHexString( altLocData ) );
            return null;
        }
        
        int count = altLocData.length / 6;
        
        DestAddress[] addresses = new DestAddress[ count ];
        int port;
        int offset;
        byte[] ip;
        for ( int i = 0; i < count; i ++ )
        {
            offset = i * 6;
            ip = new byte[4];
            ip[0] = altLocData[ offset ];
            ip[1] = altLocData[ offset + 1 ];
            ip[2] = altLocData[ offset + 2 ];
            ip[3] = altLocData[ offset + 3 ];
            port = IOUtil.unsignedShort2Int(IOUtil.deserializeShortLE( altLocData, offset + 4 ));
            addresses[i] = new DefaultDestAddress( ip, port );
        }
        
        return addresses;
    }
    
    public static DestAddress[] parsePushProxyExtensionData( GGEPBlock[] ggepBlocks )
    {
        // The payload is an array of 6-byte entries.  The first 4 bytes encode
        // the IP of the server (in big-endian, as usual), and the remaining
        // 2 bytes encode the port (in little-endian).
        byte[] data = GGEPBlock.getExtensionDataInBlocks( ggepBlocks, GGEPBlock.PUSH_PROXY_HEADER_ID );
        if ( data == null )
        {
            return null;
        }
        
        // check for valid length
        if ( data.length % 6 != 0 )
        {
            NLogger.warn(NLoggerNames.Network,
                "Invalid PushProxy GGEPBlock length: " + HexConverter.toHexString( data ) );
            return null;
        }
        
        int count = data.length / 6;
        
        DestAddress[] addresses = new DestAddress[ count ];
        int port;
        int offset;
        byte[] ip;
        for ( int i = 0; i < count; i ++ )
        {
            offset = i * 6;
            ip = new byte[4];
            ip[0] = data[ offset ];
            ip[1] = data[ offset + 1 ];
            ip[2] = data[ offset + 2 ];
            ip[3] = data[ offset + 3 ];
            port = IOUtil.unsignedShort2Int(IOUtil.deserializeShortLE( data, offset + 4 ));
            addresses[i] = new DefaultDestAddress( ip, port );
        }
        return addresses;        
    }
}
