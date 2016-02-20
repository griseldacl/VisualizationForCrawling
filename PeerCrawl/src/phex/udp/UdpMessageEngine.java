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
 *  Created on May 11, 2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.udp;

import java.io.IOException;
import java.net.*;

import phex.common.ThreadPool;
import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.common.address.IpAddress;
import phex.connection.NetworkManager;
import phex.host.Host;
import phex.host.HostManager;
import phex.host.NetworkHostsContainer;
import phex.msg.*;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

/**
 * @author Madhu
 */
public class UdpMessageEngine
{
    /**
     *  Max udp packet size we will accept
     *as of now its small as we are dealing only with pings and pongs
     */
    public final static int MAX_PACKET_SIZE = 1024;
    
    /** HINT TO IMPLEMENTATION ABOUT THE SOCKET RECV BUFFER SIZE
     */ 
    public final static int MAX_RECV_BUFFER_SIZE = 16384;
    
    private DatagramSocket udpSocket;
    
    /** 
     * the send msg queue
     */
    UdpMsgQueue sendQueue;
    
    /**
     * The guid routing table for pings
     */
    private UdpGuidRoutingTable pingRoutingTable;
    
    // The network host container
    NetworkHostsContainer netContainer = 
        HostManager.getInstance().getNetworkHostsContainer();
    
    public UdpMessageEngine()
    {
        sendQueue = new UdpMsgQueue();
        
        //create the routing table with a lifetime of the 
        //udp send ping time interval....so a pong will only be accepted if
        //it comes within the period between 0 to 2 * lifetime
        pingRoutingTable = new UdpGuidRoutingTable( 
                UdpConnectionManager.UDP_PING_PERIOD );
        
        // bind a udp socket to the port on which we operate on
        int port = NetworkManager.getInstance().getLocalAddress().getPort();
        try
        {
            udpSocket = new DatagramSocket( port );
            udpSocket.setReceiveBufferSize( MAX_RECV_BUFFER_SIZE );
        }
        catch( SocketException e )
        {
            NLogger.warn( NLoggerNames.UDP_CONNECTION, " Couldnt bind to port "
                    + port, e );
            return;
        }
        
       // add reciever and sender to the thread pool
        ThreadPool.getInstance().addJob( new Reciever(), " UDP MESSAGE RECIEVER " );
        ThreadPool.getInstance().addJob( new Sender(), " UDP MESSAGE SENDER " );
    }
 
    /**
     * adds the message to the send queue
     * @param Msg
     * @param hostAddr
     * @return
     */
    public boolean addMessageToSend( Message msg, DestAddress hostAddr )
    {
        if( msg == null)
        {
            return false;
        }
        
        return sendQueue.addMessage( msg, hostAddr );
    }
    
    /**
     * removes a message from the queue and encapsulates it in
     * a datagram. It blocks if no message is available 
     * @return
     * a datagram on success or null on failure
     */
    private DatagramPacket getDatagramToSend()
    {
        UdpMsgQueue.QueueElement element;
        element = sendQueue.removeMessage();
        
        Message msg = element.getMsg();
        DestAddress address = element.getAddress();
        
//        if( !( netContainer.isConnectedToHost( address ) ) )
//        {
//          //dont send
//          return null;  
//        }
        
        byte[] data = null;
        
        if( msg instanceof PingMsg )
        {
            GUID guid = msg.getHeader().getMsgID();
            if( ! (pingRoutingTable.checkAndAddRouting( guid, address ) ) )
            {
                //could not add to routing table
                NLogger.warn( NLoggerNames.UDP_OUTGOING_MESSAGES,
                        " ping with duplicate guid not sent " + guid 
                        + " for message : " + msg );
                return null;
            }
            PingMsg ping = ( PingMsg )msg;
            data = ping.getbytes();
            
            NLogger.debug( NLoggerNames.UDP_OUTGOING_MESSAGES, 
                    " guid : " + guid + " successfully added to routing table for " +
                    		" udp ping : \n " + msg );
        }
        
        if( msg instanceof PongMsg )
        {
            PongMsg pong = ( PongMsg )msg;
            data = pong.getbytes();
        }
        
        if ( data == null )
        {
            return null;
        }
        
        try
        {
            
            InetAddress ipAddr;
            IpAddress ipAddress = address.getIpAddress();
            if ( ipAddress != null )
            {
                ipAddr = InetAddress.getByAddress( ipAddress.getHostIP() );
            }
            else
            {
                ipAddr = InetAddress.getByName( address.getHostName() );
            }
            int port = address.getPort();
            DatagramPacket packet = new DatagramPacket( data, data.length, 
                    ipAddr, port );
            NLogger.debug( NLoggerNames.UDP_OUTGOING_MESSAGES, " created udp datagram" +
                    " for msg " + msg + " \n to " + ipAddr );
            return packet;
        }
        catch( UnknownHostException e)
        {
            // just report
            NLogger.warn( NLoggerNames.UDP_OUTGOING_MESSAGES, 
                    " Could not create datagram  from message : " + msg, e );
        }
        // if it has reached here then packet is not created
        return null;
    }
    
    /**
     * reads a packet from the socket
     * @return
     * the packet or null if something went wrong
     */
    public DatagramPacket readMessage()
    {
        byte[] data = new byte[ MAX_PACKET_SIZE ]; 
        DatagramPacket packet = new DatagramPacket( data, data.length );
        
        try
        {
            udpSocket.receive( packet );
            return packet;
        }
        catch ( IOException e )
        {
            NLogger.warn( NLoggerNames.UDP_CONNECTION, 
                    " Could not read from udp socket " + udpSocket.getLocalSocketAddress(), e );
            return null;
        }
    }
    
    private void handlePing( MsgHeader header, byte[] data, Host fromHost )
    {
        
        PingMsg udpPing = null;
        try
        {
            udpPing = PingMsg.createUdpPingMsg( header, data, MsgHeader.DATA_LENGTH, fromHost );
            NLogger.debug( NLoggerNames.UDP_INCOMING_MESSAGES, " Recieved Udp Ping " +
            		"Msg From " + fromHost + " : " + udpPing );
        }
        catch ( InvalidMessageException e )
        {
            //just ignore 
            return;
        }
        
        respondToPing( udpPing );
    }
    
    private void respondToPing( PingMsg udpPing )
    {
        PongMsg pong = PongMsg.createUdpPongMsg( udpPing );
        //add to send queue
        DestAddress address = udpPing.getHeader().getFromHost().getHostAddress(); 
        addMessageToSend( pong, address );
        NLogger.info( NLoggerNames.UDP_OUTGOING_MESSAGES,
                "added to send queue Udp Pong :" + pong + " \n \t to " + address );
    }
    
    private void handlePong( MsgHeader header, byte[] data, Host fromHost )
    {
        // first check if we had sent a ping to recieve a pong
        GUID guid = header.getMsgID();
        DestAddress address = pingRoutingTable.getAndRemoveRouting( guid ); 
        if( address == null )
        {
            // did not find routing for this pong
            NLogger.warn( NLoggerNames.UDP_INCOMING_MESSAGES, " Recieved Udp Pong " +
            		" with Guid not found in the routing table : " + header 
            		+ " \n \t Ignoring pong");
            return;
        }
        
        // thought of comparing the address in the table to the pong packet's address
        // but since its udp the packet can come from any interface of the packet's host
        // so just be happy that u sent a ping with the same guid
        
        PongMsg udpPong = null;
        try
        {
            udpPong = PongMsg.createUdpPongMsg( header, data, MsgHeader.DATA_LENGTH, fromHost );
            NLogger.debug( NLoggerNames.UDP_INCOMING_MESSAGES, " Recieved Udp Pong " +
            		"Msg From " + fromHost + " : " + udpPong );
        }
        catch ( InvalidMessageException e )
        {
            //just ignore 
            return;
        }
        
        HostManager.getInstance().getCaughtHostsContainer().addCaughtHost( udpPong );
    }  
    
    
    /** runs as a thread and sends messages that
     * have been queued up
     * @author Madhu
     */ 
    class Sender implements Runnable
    {
        public void run()
        {
            while( true )
            {
                DatagramPacket packet = getDatagramToSend();
                
                if( packet == null )
                {
                    continue;
                }
                
                try
                {
                    udpSocket.send( packet );                    
                }
                catch ( IOException e )
                {
                    NLogger.warn( NLoggerNames.UDP_OUTGOING_MESSAGES, 
                            "Sending udp message " + packet + "failed ", e );
                }
            }
        }
    }
    
    /**
     * runs as a thread and picks up udp messages sent to us
     * @author Madhu
     */
    class Reciever implements Runnable
    {
        public void run()
        {
            DatagramPacket packet;
            while( true )
            {
                packet = readMessage();
                if( packet == null )
                {
                    continue;
                }
                
                byte[] packetData = packet.getData();
                int pktDataLength = packet.getLength();
                byte[] ip = packet.getAddress().getAddress();
                IpAddress ipAddress = new IpAddress( ip );
                int port = packet.getPort();
                DestAddress address = new DefaultDestAddress( ip, 
                						port );
                
                //the data size shud be atleast a message's Header length 
                if( pktDataLength < MsgHeader.DATA_LENGTH )
                {
                    continue;
                }
                
//                //we only accept packets from hosts we are still connected to
//                //this is a sort of a crude restriction on the messages we can recieve 
//                if( ! ( netContainer.isConnectedToHost( address ) ) )
//                {
//                    continue;
//                }
                
                NetworkHostsContainer netContainer =  HostManager.getInstance().getNetworkHostsContainer();
                Host fromHost = netContainer.getNetworkHost( address );
                
                if( fromHost == null )
                {
                    fromHost = new Host( address );
                }
                
                MsgHeader header = null;
                try
                {
                    header = MsgHeader.createMsgHeader( packetData, 0 );
                }
                catch ( Exception e )
                {
                    NLogger.warn( NLoggerNames.UDP_INCOMING_MESSAGES,
                            " Failed to create udp pong from datagram ", e );
                }
                
                // check if the header data length is valid 
                if( header.getDataLength() > ( pktDataLength - MsgHeader.DATA_LENGTH ) )
                {
                	NLogger.warn( NLoggerNames.UDP_INCOMING_MESSAGES,
                	 		" Msg Header Data length is invalid " );
                }
                
                // Now check the payload field and take appropriate action
                switch( header.getPayload() )
                {
                    case MsgHeader.PING_PAYLOAD :
                        NLogger.debug( NLoggerNames.UDP_INCOMING_MESSAGES, " Recvd Ping from : " + 
                                address );
                    	handlePing( header, packetData, fromHost );
                    	break;
                    case MsgHeader.PONG_PAYLOAD :
                        NLogger.debug( NLoggerNames.UDP_INCOMING_MESSAGES, " Recvd Pong from : " + 
                                address );
                    	handlePong( header, packetData, fromHost );
                    	break;
                    default:
                        NLogger.debug( NLoggerNames.UDP_INCOMING_MESSAGES, " Recvd unrecognized Msg from : " + 
                                address );
                    	break;
                }
            }
        }
    }
}
