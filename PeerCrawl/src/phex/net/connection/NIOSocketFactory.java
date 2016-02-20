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
 *  Created on 05.03.2005
 *  --- CVS Information ---
 *  $Id: NIOSocketFactory.java,v 1.6 2005/11/03 16:33:45 gregork Exp $
 */
package phex.net.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;

import phex.common.ServiceManager;
import phex.common.address.DestAddress;
import phex.utils.IOUtil;

//// NOT USED YET ////
/**
 * Unfortunatly we are not able to use NIO Channels in blocking mode 
 * transparent with the current Phex implementation.
 * This is because during a blocking read operation the reader thread owns a
 * lock to the channel, when trying to perform a write operation the wait 
 * for the channel lock blocks our write attemp until the read returns.
 * This can cause a connection abort.
 */
// TODO NIOSocketFactory is missing maxConcurrentConnectAttempts
public class NIOSocketFactory
{
    private NIOSocketFactory()
    {
    }

    public static Socket connectNIO(DestAddress address) throws IOException
    {
        return connectNIO(address.getHostName(), address.getPort(),
            ServiceManager.sCfg.socketConnectTimeout);
    }

    public static Socket connectNIO(DestAddress address, int connectTimeout)
        throws IOException
    {
        return connectNIO(address.getHostName(), address.getPort(), connectTimeout);
    }

    /**
     * Opens a socket with a timeout in millis
     */
    public static Socket connectNIO(String host, int port, int connectTimeout)
        throws IOException
    {
        if (port < 0 || port > 0xFFFF)
        {
            throw new IOException("Wrong host address (port out of range: "
                + port + " )");
        }
        
        if ( ServiceManager.sCfg.mProxyUse )
        {
            return connectSock5NIO( host, port );
        }
        
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(true);
        Socket socket = channel.socket();
        socket.connect(new InetSocketAddress(host, port), connectTimeout);
        socket.setSoTimeout(ServiceManager.sCfg.socketRWTimeout);
        
        return socket;
    }

    private static Socket connectSock5NIO(String host, int port)
        throws IOException
    {
        Socket socket = null;
        InputStream is = null;
        OutputStream os = null;

        try
        {
            SocketChannel channel = SocketChannel.open();
            channel.configureBlocking(true);
            socket = channel.socket();
            socket.connect(
                new InetSocketAddress(ServiceManager.sCfg.mProxyHost,
                    ServiceManager.sCfg.mProxyPort),
                ServiceManager.sCfg.socketConnectTimeout );
            socket.setSoTimeout(ServiceManager.sCfg.socketRWTimeout);
            
            is = socket.getInputStream();
            os = socket.getOutputStream();

            byte[] header;
            if (ServiceManager.sCfg.mProxyUserName.length() > 0)
            {
                header = new byte[4];
                header[0] = (byte) 0x05; // version
                header[1] = (byte) 0x02; // method counts
                header[2] = (byte) 0x00; // method no authentication
                header[3] = (byte) 0x02; // method user/pw authentication
            }
            else
            {
                header = new byte[3];
                header[0] = (byte) 0x05; // version
                header[1] = (byte) 0x01; // method counts
                header[2] = (byte) 0x00; // method no authentication
            }
            os.write(header, 0, header.length);

            int servVersion = is.read();
            if (servVersion != 0x05)
            {
                throw new IOException("Invalid SOCKS server version: "
                    + servVersion);
                /*StringBuffer buffer = new StringBuffer( );
                 buffer.append( (char) servVersion );
                 while ( servVersion != -1 )
                 {
                 servVersion = is.read();
                 buffer.append( (char) servVersion );
                 }
                 throw new IOException("Invalid response from Socks5 proxy server: " +
                 buffer.toString() );
                 */
            }

            byte servMethod = (byte) is.read();
            if (servMethod == (byte) 0xFF)
            {
                throw new IOException("SOCKS: No acceptable authentication.");
            }
            if (servMethod == 0x00)
            {// no authentication..
            }
            else if (servMethod == 0x02)
            {
                authenticateUserPassword(is, os);
            }
            else
            {
                throw new IOException(
                    "Unknown SOCKS5 authentication method required.");
            }

            // send request...
            byte[] request = new byte[10];
            request[0] = (byte) 0x05; // version
            request[1] = (byte) 0x01; // command connect
            request[2] = (byte) 0x00; // reserved
            request[3] = (byte) 0x01; // address type IPv4
            IOUtil.serializeIP(host, request, 4);
            request[8] = (byte) (port >> 8); // port
            request[9] = (byte) (port); // port
            os.write(request, 0, request.length);

            // reply...
            int version = is.read(); // version
            int status = is.read(); // status
            is.read(); // reserved
            int atype = is.read(); // address type

            if (atype == 1)
            {// ipv4 address
                is.read();
                is.read();
                is.read();
                is.read();
            }
            else if (atype == 3)
            {// domain name
                int len = is.read();
                if (len < 0) len += 256;
                while (len > 0)
                {
                    is.read();
                    len--;
                }
            }
            else if (atype == 4)
            {// ipv6 address
                for (int i = 0; i < 16; i++)
                    is.read();
            }
            else
            {
                throw new IOException("Invalid return address type for SOCKS5");
            }
            is.read(); // port
            is.read(); // port

            if (version != 0x05)
            {
                throw new IOException("Invalid SOCKS server version: "
                    + version);
            }

            switch (status)
            {
            case 0x00:
                return socket;
            case 0x01:
                throw new IOException("SOCKS: General SOCKS server failure");
            case 0x02:
                throw new IOException(
                    "SOCKS: Connection not allowed by ruleset");
            case 0x03:
                throw new IOException("SOCKS: Network unreachable");
            case 0x04:
                throw new IOException("SOCKS: Host unreachable");
            case 0x05:
                throw new IOException("SOCKS: Connection refused");
            case 0x06:
                throw new IOException("SOCKS: TTL expired");
            case 0x07:
                throw new IOException("SOCKS: Command not supported");
            case 0x08:
                throw new IOException("SOCKS: Address type not supported");
            }
            throw new IOException("SOCKS: Unknown status response: " + status);
        }
        catch (Exception exp)
        {
            IOUtil.closeQuietly(is);
            IOUtil.closeQuietly(os);
            IOUtil.closeQuietly(socket);
            if (exp instanceof IOException)
            {
                throw (IOException) exp;
            }
            else
            {
                throw new IOException("Error: " + exp.getMessage());
            }
        }
    }

    private static void authenticateUserPassword(InputStream is, OutputStream os)
        throws IOException
    {
        String userName = ServiceManager.sCfg.mProxyUserName;
        String password = ServiceManager.sCfg.mProxyPassword;
        byte[] buffer = new byte[3 + userName.length() + password.length()];

        int pos = 0;
        buffer[pos++] = (byte) 0x01;
        buffer[pos++] = (byte) userName.length();
        pos = IOUtil.serializeString(userName, buffer, pos);
        buffer[pos++] = (byte) password.length();
        pos = IOUtil.serializeString(password, buffer, pos);
        os.write(buffer, 0, pos);

        if (is.read() == 1 && is.read() == 0)
        {
            return;
        }

        throw new IOException("Proxy server authentication failed.");
    }
}