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
 *  $Id: Connection.java,v 1.5 2005/10/29 18:05:39 gregork Exp $
 */
package phex.net.connection;

import java.io.IOException;

import phex.common.bandwidth.BandwidthController;
import phex.connection.ConnectionClosedException;
import phex.net.presentation.SocketFacade;
import phex.utils.*;

/**
 * 
 */
public class Connection
{   
    private SocketFacade socket;
    
    private BandwidthController bandwidthController;
    
    private BandwidthInputStream bandwidthInputStream;
    private BandwidthOutputStream bandwidthOutputStream;
    
    private GnutellaInputStream inputStream;
    private GnutellaOutputStream outputStream;
    
    
    /**
     * Creates a new Connection object for the given socket.
     * 
     * The standard BandwidthController used is the NetworkBandwidthController.
     * @param socket
     */
    public Connection( SocketFacade socket, BandwidthController bandwidthController )
    {
        this.socket = socket;
        this.bandwidthController = bandwidthController;
    }
    
    public void setBandwidthController( BandwidthController bandwidthController )
    {
        this.bandwidthController = bandwidthController;
        if ( bandwidthInputStream != null )
        {
            bandwidthInputStream.setBandwidthController( bandwidthController );
        }
        if ( bandwidthOutputStream != null )
        {
            bandwidthOutputStream.setBandwidthController( bandwidthController );
        }
    }
    
    public SocketFacade getSocket()
    {
        return socket;
    }
    
    public GnutellaInputStream getInputStream()
        throws IOException
    {
        if ( inputStream == null )
        {
            if ( socket == null )
            {
                throw new ConnectionClosedException( "Connection already closed" );
            }
            bandwidthInputStream = new BandwidthInputStream(socket
                .getInputStream(), bandwidthController);
            inputStream = new GnutellaInputStream( bandwidthInputStream );
        }
        return inputStream;
    }
    
    public int readPeek() throws IOException
    {
        return getInputStream().peek();
    }
    
    public String readLine() throws IOException
    {
        String line = getInputStream().readLine();
        return line;
    }
    
    public int read( byte[] b, int offset, int length ) throws IOException
    {
        int val = getInputStream().read(b, offset, length );
        return val;
    }

    public GnutellaOutputStream getOutputStream()
        throws IOException
    {
        if ( outputStream == null )
        {
            if ( socket == null )
            {
                throw new ConnectionClosedException( "Connection already closed" );
            }
            bandwidthOutputStream = new BandwidthOutputStream(socket
                .getOutputStream(), bandwidthController);
            outputStream = new GnutellaOutputStream( bandwidthOutputStream );
        }
        return outputStream;
    }
    
    public void write( byte[] b, int offset, int length ) throws IOException
    {
        getOutputStream().write(b, offset, length );
    }
    
    public void write( byte[] b ) throws IOException
    {
        getOutputStream().write( b );
    }
    
    public void flush() throws IOException
    {
        getOutputStream().flush();
    }
    
    public void disconnect()
    {
        NLogger.debug(NLoggerNames.Network, "Disconnecting socket " + socket );
        IOUtil.closeQuietly( inputStream );
        IOUtil.closeQuietly( outputStream );
        IOUtil.closeQuietly( socket );
        inputStream = null;
        outputStream = null;
        socket = null;        
    }
}
