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
package phex.chat;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import phex.common.Environment;
import phex.common.ServiceManager;
import phex.common.ThreadPool;
import phex.common.address.DestAddress;
import phex.common.bandwidth.BandwidthManager;
import phex.net.connection.OIOSocketFactory;
import phex.net.presentation.SocketFacade;
import phex.utils.*;

/**
 * TODO
 * 18:04:46.0000: Network(Fine)! Read Chat header: User-Agent: Shareaza 1.6.0.0
18:04:46.0050: Network(Fine)! Read Chat header: X-Nickname: squirrel
18:04:46.0060: Network(Fine)! Read Chat header: X-Geo-Loc: lat=52.530,long=13.400,country="Germany",city="Berlin, Berlin"
 * <p> </p>
 * <p> </p>
 * <p>Copyright: Copyright (c) 2002 Gregor Koukkoullis</p>
 * <p> </p>
 * @author Gregor Koukkoullis
 *
 */

public class ChatEngine
{
    private boolean isOutgoingConnection;
    private SocketFacade socket;
    private GnutellaInputStream chatReader;
    private BufferedWriter chatWriter;
    private String chatNick;

    /**
     * The host address of the chat connection.
     */
    private DestAddress hostAddress;

    /**
     * For incomming chat requests
     */
    public ChatEngine( SocketFacade aSocket, GnutellaInputStream gInStream,
        DestAddress aHostAddress )
        throws IOException
    {
        socket = aSocket;
        hostAddress = aHostAddress;
        chatReader = gInStream;
        BandwidthOutputStream bandwidthOutStream = new BandwidthOutputStream(
            socket.getOutputStream(), 
            BandwidthManager.getInstance().getNetworkBandwidthController() );
        chatWriter = new BufferedWriter( new OutputStreamWriter(
            bandwidthOutStream ) );
        chatNick = hostAddress.getHostName();
        finalizeHandshake();
        isOutgoingConnection = false;
    }

    /**
     * For outgoing chat requests. We need to connect to the host address first.
     */
    public ChatEngine( DestAddress aHostAddress )
    {
        hostAddress = aHostAddress;
        isOutgoingConnection = true;
        chatNick = hostAddress.getHostName();
    }

    public void startChat()
    {
        ChatReadWorker worker = new ChatReadWorker();
        ThreadPool.getInstance().addJob( worker,
            "ChatReadWorker-" + Integer.toHexString(worker.hashCode()));
    }

    public void stopChat()
    {
        if ( chatReader != null )
        {
            chatReader.close();
            chatReader = null;
        }
        IOUtil.closeQuietly( chatWriter );
        chatWriter = null;
        IOUtil.closeQuietly( socket );
        socket = null;
        ChatManager.getInstance().fireChatConnectionFailed( this );
    }

    public boolean isConnected()
    {
        return socket != null;
    }

    /**
     * Returns the host address the engine is connected to
     */
    public DestAddress getHostAddress()
    {
        return hostAddress;
    }

    public String getChatNick()
    {
        return chatNick;
    }

    /**
     * Sends a chat message to the connected servent
     */
    public void sendChatMessage( String message )
    {
        if ( chatWriter == null )
        {
            ChatManager.getInstance().fireChatConnectionFailed( ChatEngine.this );
        }
        try
        {
            chatWriter.write( message + "\n" );
            chatWriter.flush();
        }
        catch ( IOException exp )
        {
            NLogger.warn( ChatEngine.class, exp, exp );
            stopChat();
        }
    }

    private void finalizeHandshake()
        throws IOException
    {
        socket.setSoTimeout( ServiceManager.sCfg.socketConnectTimeout );

        // read the header that have been left in the stream after accepting the
        // connection.
        String line;
        String upLine;
        do
        {
            line = chatReader.readLine();
            Logger.logMessage( Logger.FINE, Logger.NETWORK, "Read Chat header: " + line );
            if ( line == null )
            {
                throw new IOException( "No handshake response from chat partner." );
            }
            upLine = line.toUpperCase();
            if ( upLine.startsWith( "X-NICKNAME:" ) )
            {

                chatNick = line.substring(11).trim();
                Logger.logMessage( Logger.FINE, Logger.NETWORK, "Chat Nick: " + chatNick );
            }
        }
        while ( line.length() > 0 );

        // we respond with "CHAT/0.1 200 OK\r\n\r\n" to finish the handshake.
        Logger.logMessage( Logger.FINE, Logger.NETWORK, "Sending: CHAT/0.1 200 OK");
        chatWriter.write( "CHAT/0.1 200 OK\r\n" +
            "User-Agent: " + Environment.getPhexVendor() + "\r\n" +
            //"X-Nickname: " + StrUtil.getAppNameVersion() + "\r\n" +
            "\r\n" );
        chatWriter.flush();

        // assume we read the final "CHAT/0.1 200 OK" followed by possible headers.
        do
        {
            line = chatReader.readLine();
            Logger.logMessage( Logger.FINE, Logger.NETWORK, "Read Chat response: " + line );
            if ( line == null )
            {
                throw new IOException( "No handshake response from chat partner." );
            }
        }
        while ( line.length() > 0 );

        socket.setSoTimeout( 0 );
        // chat connection open notification will be fired through the chat manager
    }

    private void connectOutgoingChat()
        throws IOException
    {
        Logger.logMessage( Logger.FINE, Logger.NETWORK,
            "Connect outgoing to: " + hostAddress );
        socket = OIOSocketFactory.connect( hostAddress,
            ServiceManager.sCfg.mNetConnectionTimeout );
    
        BandwidthOutputStream bandwidthOutStream = new BandwidthOutputStream(
            socket.getOutputStream(), 
            BandwidthManager.getInstance().getNetworkBandwidthController() );
        chatWriter = new BufferedWriter( new OutputStreamWriter(
            bandwidthOutStream ) );

        // initialize the chat connection handshake
        // First send "CHAT CONNECT/0.1\r\n" and header data
        // only header currently is user agent.
        String message = "CHAT CONNECT/0.1\r\n" +
            "User-Agent: " + Environment.getPhexVendor() + "\r\n" +
            //"X-Nickname: " + StrUtil.getAppNameVersion() + "\r\n" +
            "\r\n";
        Logger.logMessage( Logger.FINE, Logger.NETWORK, "Sending: " + message );
        chatWriter.write( message );
        chatWriter.flush();

        BandwidthInputStream bandwidthInputStream = new BandwidthInputStream(
            socket.getInputStream(), 
            BandwidthManager.getInstance().getNetworkBandwidthController());
        chatReader = new GnutellaInputStream(bandwidthInputStream);

        // assume we read "CHAT/0.1 200 OK" and header data
        // TODO: check string and status, don't check for version since it might
        //       change only for "CHAT" and "200"
        String line;
        String upLine;
        do
        {
            line = chatReader.readLine();
            Logger.logMessage( Logger.FINE, Logger.NETWORK, "Read Chat header: " + line );
            if ( line == null )
            {
                throw new IOException( "No handshake response from chat partner." );
            }
            upLine = line.toUpperCase();
            if ( upLine.startsWith( "X-NICKNAME:" ) )
            {
                chatNick = line.substring(11).trim();
            }
        }
        while ( line.length() > 0 );

        // we respond with "CHAT/0.1 200 OK\r\n\r\n" to finish the handshake.
        chatWriter.write( "CHAT/0.1 200 OK\r\n" +
            "User-Agent: " + Environment.getPhexVendor() + "\r\n\r\n" );
        chatWriter.flush();
        socket.setSoTimeout( 0 );
        // chat connection open notification will be fired through the chat manager
    }

    /**
     * Hide the reading thread from the engine implementation.
     */
    private class ChatReadWorker implements Runnable
    {
        public void run()
        {
            if ( isOutgoingConnection )
            {
                try
                {
                    connectOutgoingChat();
                }
                catch ( IOException exp )
                {
                    stopChat();
                    return;
                }
            }

            ChatManager chatManager = ChatManager.getInstance();
            String str;
            while ( true )
            {
                try
                {
                    str = chatReader.readLine();
                    if ( str == null )
                    {
                        throw new IOException( "Remote host diconnected chat." );
                    }
                    if ( str.length() == 0 )
                    {
                        continue;
                    }
                    Logger.logMessage( Logger.FINER, Logger.NETWORK,
                        "Reading chat message: " + str );
                    chatManager.fireChatMessageReceived( ChatEngine.this, str );
                }
                catch ( IOException exp )
                {
                    Logger.logMessage( Logger.FINER, Logger.NETWORK, exp );
                    stopChat();
                    break;
                }
            }
        }
    }
}