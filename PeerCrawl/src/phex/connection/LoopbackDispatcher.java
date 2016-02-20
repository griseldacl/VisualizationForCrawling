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
 *  $Id: LoopbackDispatcher.java,v 1.7 2005/11/04 20:43:11 gregork Exp $
 */
package phex.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import phex.common.ServiceManager;
import phex.utils.*;


/**
 * The class is used as a entry point on Phex startup to check if a running
 * Phex instance is available to process command line args.
 */
public class LoopbackDispatcher
{
    public static boolean dispatchMagmaFile( String fileName )
    {
        int port = ServiceManager.sCfg.mListeningPort;
        Socket socket = null;
        InputStream inStream = null;
        OutputStream outStream = null;
        try
        {
            socket = new Socket();
            socket.connect( new InetSocketAddress( "127.0.0.1", port ), 1000 );
            socket.setSoTimeout( 1000 );
            outStream = socket.getOutputStream();
            
            outStream.write( (IncomingConnectionDispatcher.MAGMA_DOWNLOAD_PREFIX + " " + fileName + "\r\n").getBytes() );
            outStream.flush();
            
            inStream = socket.getInputStream();
            GnutellaInputStream gInStream = new GnutellaInputStream( inStream );
            String responseLine = gInStream.readLine();
            if ( responseLine != null && responseLine.equals("OK" ) )
            {
                return true;
            }
        }
        catch ( IOException exp )
        {// ignore
            NLogger.warn(NLoggerNames.LOOPBACK, exp, exp);
        }
        finally
        {
            IOUtil.closeQuietly( inStream );
            IOUtil.closeQuietly( outStream );
            IOUtil.closeQuietly( socket );
        }
        return false;
    }
    
    public static boolean dispatchRSSFile( String fileName )
    {
        int port = ServiceManager.sCfg.mListeningPort;
        Socket socket = null;
        InputStream inStream = null;
        OutputStream outStream = null;
        try
        {
            socket = new Socket();
            socket.connect( new InetSocketAddress( "127.0.0.1", port ), 1000 );
            socket.setSoTimeout( 1000 );
            outStream = socket.getOutputStream();
            
            outStream.write( (IncomingConnectionDispatcher.RSS_DOWNLOAD_PREFIX + " " + fileName + "\r\n").getBytes() );
            outStream.flush();
            
            inStream = socket.getInputStream();
            GnutellaInputStream gInStream = new GnutellaInputStream( inStream );
            String responseLine = gInStream.readLine();
            if ( responseLine != null && responseLine.equals("OK" ) )
            {
                return true;
            }
        }
        catch ( IOException exp )
        {// ignore
            NLogger.warn(NLoggerNames.LOOPBACK, exp, exp);
        }
        finally
        {
            IOUtil.closeQuietly( inStream );
            IOUtil.closeQuietly( outStream );
            IOUtil.closeQuietly( socket );
        }
        return false;
    }
    
    public static boolean dispatchUri( String uri )
    {
        int port = ServiceManager.sCfg.mListeningPort;
        Socket socket = null;
        InputStream inStream = null;
        OutputStream outStream = null;
        try
        {
            socket = new Socket();
            socket.connect( new InetSocketAddress( "127.0.0.1", port ), 1000 );
            socket.setSoTimeout( 1000 );
            outStream = socket.getOutputStream();
            
            outStream.write( (IncomingConnectionDispatcher.URI_DOWNLOAD_PREFIX + " " + uri + "\r\n").getBytes() );
            outStream.flush();
            
            inStream = socket.getInputStream();
            GnutellaInputStream gInStream = new GnutellaInputStream( inStream );
            String responseLine = gInStream.readLine();
            if ( responseLine != null && responseLine.equals("OK" ) )
            {
                return true;
            }
        }
        catch ( IOException exp )
        {// ignore
        }
        finally
        {
            IOUtil.closeQuietly( inStream );
            IOUtil.closeQuietly( outStream );
            IOUtil.closeQuietly( socket );
        }
        return false;
    }
}
