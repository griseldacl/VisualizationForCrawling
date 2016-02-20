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
 *  $Id: OIOServer.java,v 1.11 2005/11/13 10:25:37 gregork Exp $
 */
package phex.net;

import java.io.IOException;
import java.net.*;

import phex.common.ServiceManager;
import phex.common.ThreadPool;
import phex.common.address.*;
import phex.connection.IncomingConnectionDispatcher;
import phex.connection.NetworkManager;
import phex.host.HostManager;
import phex.host.NetworkHostsContainer;
import phex.net.presentation.PresentationManager;
import phex.net.presentation.SocketFacade;
import phex.net.presentation.def.DefaultSocketFacade;
import phex.security.PhexSecurityException;
import phex.security.PhexSecurityManager;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

public class OIOServer extends Server
{
    public OIOServer()
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
            while ( serverSocket != null && !serverSocket.isClosed() )
            {  
                try
                {
                    Socket incoming = serverSocket.accept();
                    // create facade...
                    DefaultSocketFacade incomingFacade = new DefaultSocketFacade( 
                        incoming );
                    handleIncomingSocket( incomingFacade );
                }
                catch ( SocketException exp )
                {
                    NLogger.debug(NLoggerNames.SERVER, exp );
                }
                catch ( PhexSecurityException exp )
                {
                    NLogger.debug(NLoggerNames.SERVER, exp );
                }
                catch (IOException exp)
                {
                    NLogger.error(NLoggerNames.SERVER, exp, exp);
                }
            }
        }
        catch ( Exception exp )
        {
            NLogger.error(NLoggerNames.SERVER, exp, exp );
        }

        isRunning = false;
        NLogger.debug( NLoggerNames.SERVER, "Listener stopped.");
        DestAddress address = PresentationManager.getInstance().createHostAddress(
            new IpAddress(IpAddress.LOCAL_HOST_IP), DefaultDestAddress.DEFAULT_PORT );
        NetworkManager.getInstance().updateLocalAddress( address );
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
    private void handleIncomingSocket(SocketFacade clientSocket )
        throws IOException, PhexSecurityException
    {        
        clientSocket.setSoTimeout(ServiceManager.sCfg.socketRWTimeout);

        DestAddress address = clientSocket.getRemoteAddress();
        NetworkHostsContainer netHostsContainer = HostManager.getInstance()
            .getNetworkHostsContainer();

        // if not already connected and connection is not from a private address.
        IpAddress remoteIp = address.getIpAddress();
        assert remoteIp != null;
        if (!netHostsContainer.isConnectedToHost(address)
            && !remoteIp.isSiteLocalIP() )
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
                throw new PhexSecurityException("Host access denied: " + address );
        }

        NLogger.debug( NLoggerNames.SERVER, 
            "Accepted incoming connection from: "
                + address.getFullHostName());

        IncomingConnectionDispatcher dispatcher = new IncomingConnectionDispatcher(
            clientSocket );
        ThreadPool.getInstance().addJob( dispatcher,
            "IncomingConnectionDispatcher-" + Integer.toHexString(hashCode()));
    }

    protected synchronized void bind() throws IOException
    {
        assert (serverSocket == null);

        int port = ServiceManager.sCfg.mListeningPort;
        serverSocket = new ServerSocket();

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
                if (tries > 50)
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
    }
    
    protected synchronized void closeServer()
    {
        if ( serverSocket != null )
        {
            try
            {
                serverSocket.close();
            }
            catch (IOException exp)
            {// ignore
            }
            serverSocket = null;
        }
    }
}