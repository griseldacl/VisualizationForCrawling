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
 *  $Id: MessageProcessor.java,v 1.15 2005/11/03 17:06:26 gregork Exp $
 */
package phex.msg;

import java.io.IOException;

import phex.msg.vendor.VendorMsg;
import phex.net.connection.Connection;
import phex.utils.IOUtil;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;


public class MessageProcessor
{
    private MessageProcessor()
    {
        
    }
    
    public static Message parseMessage( Connection connection )
        throws IOException, InvalidMessageException
    {
        MsgHeader header = parseMessageHeader( connection, new byte[ MsgHeader.DATA_LENGTH ] );
        if ( header == null )
        {
            throw new IOException("Connection closed by remote host");
        }
        return parseMessage( header, connection );
    }

    public static Message parseMessage( MsgHeader header, Connection connection )
        throws IOException, InvalidMessageException
    {
        byte[] body = readMessageBody( connection, header.getDataLength() );

        Message message = createMessageFromBody( header, body );

        return message;
    }

    public static Message createMessageFromBody( MsgHeader header, byte[] body )
        throws InvalidMessageException
    {
        switch( header.getPayload() )
        {
            case MsgHeader.PING_PAYLOAD:
                return new PingMsg( header, body );
            case MsgHeader.PONG_PAYLOAD:
                return new PongMsg( header, body );
            case MsgHeader.PUSH_PAYLOAD:
                return new PushRequestMsg( header, body );
            case MsgHeader.QUERY_HIT_PAYLOAD:
                return new QueryResponseMsg( header, body );
            case MsgHeader.QUERY_PAYLOAD:
                return new QueryMsg( header, body );
            case MsgHeader.ROUTE_TABLE_UPDATE_PAYLOAD:
                return RouteTableUpdateMsg.parseMessage( header, body );
            case MsgHeader.VENDOR_MESSAGE_PAYLOAD:
            case MsgHeader.STANDARD_VENDOR_MESSAGE_PAYLOAD:
                return VendorMsg.parseMessage( header, body );
            default:
                // unknown message type return null...
        }
        return null;
    }


    public static byte[] readMessageBody( Connection connection, int dataLength )
        throws IOException
    {
        byte[] body = new byte[ dataLength ];

        int dataRead = 0;
        int len;
        int readSize;
        while ( dataRead < dataLength )
        {
            readSize = Math.min( dataLength - dataRead, 1024 );
            len = connection.read( body, dataRead, readSize );
            if ( len == -1 )
            {
                throw new IOException( "Connection closed by remote host" );
            }
            dataRead += len;
        }

        return body;
    }

    public static MsgHeader parseMessageHeader( Connection connection,
        byte[] buffer )
        throws IOException
    {
        int lenRead = 0;
        int len;
        while ( lenRead < MsgHeader.DATA_LENGTH )
        {
            len = connection.read( buffer, lenRead, MsgHeader.DATA_LENGTH - lenRead);
            if ( len == -1 )
            {
                return null;
            }
            lenRead += len;
        }

        byte[] guidArr = new byte[ GUID.DATA_LENGTH ];
        System.arraycopy( buffer, 0, guidArr, 0, GUID.DATA_LENGTH );

        byte payload = buffer[ 16 ];

        byte ttl = buffer[ 17 ];

        byte hops = buffer[ 18 ];

        int dataLength = IOUtil.deserializeIntLE( buffer, 19 );

        MsgHeader header = new MsgHeader( new GUID( guidArr ), payload, ttl, hops,
            dataLength );

        return header;
    }
    
    /**
     * creates a body for a message from a byte array given its header
     * @param MsgHeader
     * @param data byte array
     * @param offset
     * @return body in byte array or null on failure
     * @author Madhu
     */
    public static byte[] createBody( MsgHeader MsgHdr, byte data[], int offset)
    {
        int bodyLength = MsgHdr.getDataLength();  
        byte[] body = new byte[ bodyLength ];
        
        if ( bodyLength > ( data.length - offset ) )
        {
            NLogger.warn( NLoggerNames.UDP_CONNECTION, " Message Data length greater" +
            		" then that of given byte array " + new String( data )
                    );
            return null;
        }
        
        System.arraycopy( data, offset, body, 0, bodyLength );  
        return body;
    }
    
    /**
     * checks if the udp message header is proper ie
     * ttl should be one
     * hops taken should ne zero
     * @param udpMsgHdr
     * @return
     */
    public static boolean isValidUdpMsgHeader( MsgHeader udpMsgHdr )
    {
        if( udpMsgHdr.getTTL() != 1 )
        {
            NLogger.warn( NLoggerNames.UDP_INCOMING_MESSAGES, " Udp Message dropped" +
            		" because ttl is not one " + udpMsgHdr );
            return false;
        }
        
        if( udpMsgHdr.getHopsTaken() != 0 )
        {
            NLogger.warn( NLoggerNames.UDP_INCOMING_MESSAGES, " Udp Message dropped" +
    		" because hops taken exceeds zero " + udpMsgHdr );
            return false;
        }
        
        return true;
    }
}