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
 *  $Id: NIOServer.java,v 1.12 2005/11/13 10:25:37 gregork Exp $
 */
package phex.net;

import java.io.IOException;
import java.net.*;
import java.nio.channels.*;
import java.util.Iterator;

import phex.common.ServiceManager;
import phex.common.ThreadPool;
import phex.common.address.*;
import phex.connection.IncomingConnectionDispatcher;
import phex.connection.NetworkManager;
import phex.host.HostManager;
import phex.host.NetworkHostsContainer;
import phex.net.presentation.PresentationManager;
import phex.net.presentation.def.DefaultSocketFacade;
import phex.security.PhexSecurityException;
import phex.security.PhexSecurityManager;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

//// NOT USED YET ////
public class NIOServer extends Server
{
    private ServerSocketChannel listeningChannel;
    private Selector selector;

    public NIOServer()
    {
        super();
    }

    // The listening thread.
    public void run()
    {
        if (NLogger.isDebugEnabled(NLoggerNames.SERVER))
            NLogger.debug(NLoggerNames.SERVER,
                "Listener started. Listening on: "
                    + serverSocket.getInetAddress().getHostAddress() + ':'
                    + serverSocket.getLocalPort());

        try
        {
            while ( selector.isOpen() )
            {
                selector.select(10 * 1000);
                if ( !selector.isOpen() )
                {
                    break;
                }
    
                Iterator iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext())
                {
                    SelectionKey selKey = (SelectionKey) iterator.next();
                    // Remove it from the list to indicate that it is being processed
                    iterator.remove();
    
                    // Check if it's a connection request
                    if ( !selKey.isAcceptable() )
                    {
                        continue;
                    }
                    
                    // Get channel with connection request
                    ServerSocketChannel ssChannel = (ServerSocketChannel)selKey.channel();
                    try
                    {
                        SocketChannel socketChannel = ssChannel.accept();
                        handleIncomingClientChannel(socketChannel);
                    }
                    catch ( PhexSecurityException exp )
                    {
                        NLogger.debug(NLoggerNames.SERVER, exp );
                    }
                    catch ( IOException exp )
                    {
                        NLogger.debug(NLoggerNames.SERVER, exp, exp);
                    }
                }
            }
        }
        catch ( Exception exp )
        {
            NLogger.error(NLoggerNames.SERVER, exp, exp );
        }

        isRunning = false;
        NLogger.debug( NLoggerNames.SERVER, "Listener stopped.");
        
        PresentationManager presentationMgr = PresentationManager.getInstance();
        DestAddress localAddress = presentationMgr.createHostAddress( 
            new IpAddress(IpAddress.LOCAL_HOST_IP), 
            ServiceManager.sCfg.mListeningPort );
        NetworkManager.getInstance().updateLocalAddress( localAddress );
        synchronized(this)
        {
            notifyAll();
        }
    }

    /**
     * @param socketChannel
     * @throws PhexSecurityException
     * @throws SocketException
     */
    private void handleIncomingClientChannel(SocketChannel socketChannel)
        throws IOException, PhexSecurityException
    {
        socketChannel.configureBlocking(true);
        Socket clientSocket = socketChannel.socket();
        clientSocket.setSoTimeout(ServiceManager.sCfg.socketRWTimeout);

        IpAddress ip = new IpAddress( clientSocket.getInetAddress().getAddress() );
        PresentationManager presentationMgr = PresentationManager.getInstance();
        DestAddress address = presentationMgr.createHostAddress(ip, clientSocket.getPort() );
        NetworkHostsContainer netHostsContainer = HostManager.getInstance()
            .getNetworkHostsContainer();

        // if not already connected and connection is not from a private address.
        // TODO we might like to accept more then two connection in some cases!
        if (!netHostsContainer.isConnectedToHost(address)
            && !address.getIpAddress().isSiteLocalIP())
        {
            hasConnectedIncomming = true;
            lastInConnectionTime = System.currentTimeMillis();
        }
        
        // Set this will defeat the Nagle Algorithm, making short bursts of
        // transmission faster, but will be worse for the overall network.
        // incoming.setTcpNoDelay(true);

        // Create a Host object for the incoming connection
        // and hand it off to a ReadWorker to handle.
        byte access = PhexSecurityManager.getInstance()
            .controlHostAddressAccess(address);
        switch (access)
        {
        case PhexSecurityManager.ACCESS_DENIED:
        case PhexSecurityManager.ACCESS_STRONGLY_DENIED:
            throw new PhexSecurityException("Host access denied: "
                + clientSocket.getInetAddress().getHostAddress());
        }

        NLogger.debug( NLoggerNames.SERVER, 
            "Accepted incoming connection from: "
                + address.getFullHostName());

        // facade socket
        DefaultSocketFacade clientFacade = new DefaultSocketFacade( clientSocket );
        IncomingConnectionDispatcher dispatcher = new IncomingConnectionDispatcher(
            clientFacade );
        ThreadPool.getInstance().addJob( dispatcher,
            "IncomingConnectionDispatcher-" + Integer.toHexString(hashCode()));
    }

    protected synchronized void bind() throws IOException
    {
        assert (listeningChannel == null);

        int port = ServiceManager.sCfg.mListeningPort;
        listeningChannel = ServerSocketChannel.open();
        serverSocket = listeningChannel.socket();
        listeningChannel.configureBlocking(false);

        // Create a listening socket at the port.
        int tries = 0;
        boolean error;
        // try to find new port if port not valid
        do
        {
            error = false;

            try
            {
                NLogger.debug( NLoggerNames.SERVER, "Binding to port " + port);
                serverSocket.bind(new InetSocketAddress(port));
            }
            catch (BindException exp)
            {
                NLogger.debug( NLoggerNames.SERVER, "Binding failed to port " + port);
                if (tries > 10)
                {
                    throw exp;
                }
                error = true;
                port++;
                tries++;
            }
        }
        while (error == true);
        
        IpAddress hostIP = resolveLocalHostIP();
        port = serverSocket.getLocalPort();
        DestAddress address = PresentationManager.getInstance().createHostAddress(
            hostIP, port);
        NetworkManager.getInstance().updateLocalAddress( address );        
        
        selector = Selector.open();
        listeningChannel.register(selector, SelectionKey.OP_ACCEPT);
    }
    
    /**
     * @see phex.net.Server#closeServer()
     */
    protected void closeServer()
    {
        try
        {
            listeningChannel.close();
            SelectionKey key = listeningChannel.keyFor(selector);
            key.cancel();
            selector.close();
        }
        catch (IOException exp)
        {// ignore
        }
        serverSocket = null;
        listeningChannel = null;
    }
}