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
 *  Created on 29.10.2005
 *  --- CVS Information ---
 *  $Id: DefaultSocketFacade.java,v 1.3 2005/11/03 23:30:13 gregork Exp $
 */
package phex.net.presentation.def;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.net.presentation.SocketFacade;

public class DefaultSocketFacade implements SocketFacade
{
    private DestAddress remoteAddress;
    private Socket socket;
    
    public DefaultSocketFacade( Socket aSocket )
    {
        socket = aSocket;
    }

    public void setSoTimeout( int socketRWTimeout )
        throws SocketException
    {
        socket.setSoTimeout(socketRWTimeout);
    }
    
    public InputStream getInputStream() throws IOException
    {
        return socket.getInputStream();
    }
    
    public OutputStream getOutputStream() throws IOException
    {
        return socket.getOutputStream();
    }
    
    public void close() throws IOException
    {
        socket.close();
    }
    
    public DestAddress getRemoteAddress()
    {
        if ( remoteAddress == null )
        {
            remoteAddress = new DefaultDestAddress(
                socket.getInetAddress().getHostAddress(), socket.getPort() );
        }
        return remoteAddress;
    }
}
