/*
 *  PHEX - The pure-java Gnutella-servent
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
package phex.msg;

import java.io.IOException;

import phex.host.Host;
import phex.host.HostManager;
import phex.utils.GnutellaOutputStream;
import phex.utils.HexConverter;
import phex.utils.IOUtil;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;


/**
 * <p>A Gnutella Ping message.</p>
 *
 * <p>This represents a ping message. It informs other Gnutella nodes that this
 * node wants to know about them. The responses to this will be pongs,
 * encapsulated by the MsgInitResponse class.</p>
 *
 * <p>This implementation handles GGEP extension blocks.</p>
 */
public class PingMsg extends Message
{
    /**
     * <p>The un-parsed body of the message.</p>
     * It might include the optional payload.
     */
    private byte[] body;
    private byte[] udpScpByte = null;
    
    public static final byte UDP_SCP_MASK = 0x1;
    public static final byte UDP_SCP_LEAF = 0x0;
    public static final byte UDP_SCP_ULTRAPEER = 0x1;

    /**
     * Create a new init message with a default header.
     */
    public PingMsg()
    {
        super( new MsgHeader( MsgHeader.PING_PAYLOAD, 0 ) );
        body = IOUtil.EMPTY_BYTE_ARRAY;
    }

    /**
     * <p>Create a new init message using a header.</p>
     *
     * <p>This will set the function property of the header to MsgHeader.sInit.
     * </p>
     *
     * @param header  the MsgHeader to use
     */
    public PingMsg( MsgHeader aHeader, byte[] aBody )
    {
        super( aHeader );
        getHeader().setPayloadType( MsgHeader.PING_PAYLOAD );
        body = aBody;
        getHeader().setDataLength( body.length );        
    }
    
     /**
     * Create a Udp ping messsage 
     * it contains the scp flag and is sent over udp to all the 
     * connected hosts
     * @return a brand new udp ping message with ttl = 1
     * @throws IOException
     * @author Madhu
     */
    public static PingMsg createUdpPingMsg() throws IOException
    {
        // first set the data field for  the scp extension
        byte[] data = new byte[1];
        HostManager hostMgr = HostManager.getInstance();
        if( hostMgr.isUltrapeer() )
        {
            data[0] = UDP_SCP_ULTRAPEER;
        }
        else
        {
            data[0] = UDP_SCP_LEAF;
        }       
        
        GGEPBlock scpExtension = new GGEPBlock();        
        scpExtension.addExtension( GGEPBlock.UDP_HOST_CACHE_SCP, data );
        
        byte[] body = scpExtension.getBytes();
        
        PingMsg udpPingMsg = new PingMsg();
        udpPingMsg.getHeader().setTTL( (byte) 1 );
        udpPingMsg.getHeader().setDataLength( body.length );
        udpPingMsg.body = body;
        udpPingMsg.udpScpByte = data; 
        
        NLogger.debug( NLoggerNames.UDP_OUTGOING_MESSAGES, " Created UDP Ping " + 
                udpPingMsg.toString()
                );
        
       return udpPingMsg;        
    }
    
    /**
     * creates a Udp Ping Message from a bytes array
     * @param a byte array containing the actual ping message
     * @author Madhu  
     */
    public static PingMsg createUdpPingMsg( byte[] bytesMsg, Host fromHost )
    throws InvalidMessageException
    {
        MsgHeader msgHdr = MsgHeader.createMsgHeader( bytesMsg, 0 );
        return createUdpPingMsg( msgHdr, bytesMsg, MsgHeader.DATA_LENGTH, fromHost );
    }
    
    public static PingMsg createUdpPingMsg( MsgHeader msgHdr, byte[] data, int offset, Host fromHost ) 
    	throws InvalidMessageException 
    {
        if( ! ( MessageProcessor.isValidUdpMsgHeader( msgHdr ) ) )
        {
            NLogger.warn( NLoggerNames.UDP_INCOMING_MESSAGES, " Could not create udp ping " +
            		"from given byte array. Message Verification failed " + new String( data )
                    );
            throw new InvalidMessageException( " Could not create Msg Header " +
            		"while trying to create udp ping Msg. Message Verification failed "
                    );
        }
        
        msgHdr.setFromHost( fromHost );
        
        byte[] body = MessageProcessor.createBody( msgHdr, data, offset);
        
        if ( body == null )
        {
            throw new InvalidMessageException( " Could not create Msg Body while trying to" +
            		" create udp ping Msg"
                    );
        }
        PingMsg udpPing = new PingMsg( msgHdr, body );  
        udpPing.parseGGEPBlocks();   
        
        return udpPing;
    }
    
    public void writeMessage( GnutellaOutputStream outStream )
        throws IOException
    {
        getHeader().writeHeader( outStream );
        outStream.write( body, 0, body.length );
    }
    
    public byte[] getbytes()
    {
        byte[] data = new byte[ MsgHeader.DATA_LENGTH + body.length ];
        byte[] hdr = getHeader().getBytes();
        System.arraycopy( hdr, 0, data, 0, MsgHeader.DATA_LENGTH );
        System.arraycopy( body, 0, data, MsgHeader.DATA_LENGTH , body.length );
        return data;
    }

    private void parseGGEPBlocks()
    {
        GGEPBlock[] ggepBlocks;
        try
        {
            ggepBlocks = GGEPBlock.parseGGEPBlocks( body, 0 );
        }
        catch ( InvalidGGEPBlockException exp )
        {// ignore and continue parsing...
            NLogger.warn( NLoggerNames.MESSAGE_ENCODE_DECODE, exp );
            return;
        }
        if ( GGEPBlock.isExtensionHeaderInBlocks( ggepBlocks, GGEPBlock.UDP_HOST_CACHE_SCP ) )
        {
            byte[] data = GGEPBlock.getExtensionDataInBlocks( ggepBlocks, GGEPBlock.UDP_HOST_CACHE_SCP );
            if ( data != null )
            {
              udpScpByte = data;  
            }
        }            
    }    
    
    public byte[] getScpByte()
    {
        if( udpScpByte == null || udpScpByte.length < 1 )
            return null;
        
        return udpScpByte;
    }
    
    public String toString()
    {
        return	getDebugString();
    }

    public String getDebugString()
    {
        return "Ping[ HEX=[" + HexConverter.toHexString( body ) +
            "] ]";
    }

}

