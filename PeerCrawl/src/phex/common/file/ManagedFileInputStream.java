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
 *  Created on 10.09.2005
 *  --- CVS Information ---
 *  $Id: ManagedFileInputStream.java,v 1.2 2005/09/16 23:01:20 gregork Exp $
 */
package phex.common.file;

import java.io.*;
import java.nio.ByteBuffer;

import phex.utils.*;

public class ManagedFileInputStream extends InputStream
{   
    private DirectByteBuffer buffer;
    private ManagedFile managedFile;
    private long inputOffset;
    
    public ManagedFileInputStream( ManagedFile managedFile, long inputOffset )
    {
        this.managedFile = managedFile;
        this.inputOffset = inputOffset;
        buffer = DirectByteBufferProvider.requestBuffer( 
            DirectByteBufferProvider.BUFFER_SIZE_64K);
        buffer.flip();// flip buffer to let it appear empty
//        NLogger.debug(NLoggerNames.GLOBAL, "Created ManagedInputStream: Buffer[" 
//            + buffer + "] ManagedFile[" + managedFile + "].");
    }
        
    public int read() throws IOException
    {
        ByteBuffer internalBuffer = buffer.getInternalBuffer();
        if ( !internalBuffer.hasRemaining() )
        {
            fill();
        }
        if ( !internalBuffer.hasRemaining() )
        {
            return -1;
        }
        
        byte b = internalBuffer.get();
        return (int)b;
    }

    public int read(byte b[]) throws IOException
    {
        return this.read(b, 0, b.length);
    }
    
    public int read( byte b[], int offset, int length ) throws IOException
    {
        if ((offset | length | (offset + length) | (b.length - (offset + length))) < 0)
        {
            throw new IndexOutOfBoundsException();
        } 
        else if ( length == 0 )
        {
            return 0;
        }
        
        ByteBuffer internalBuffer = buffer.getInternalBuffer();
        if ( !internalBuffer.hasRemaining() )
        {
            fill();
        }
        if ( !internalBuffer.hasRemaining() )
        {
            return -1;
        }
        int read = 0;
        while ( read < length && internalBuffer.hasRemaining() )
        {
            int toRead = Math.min( length-read, internalBuffer.remaining() );
            internalBuffer.get( b, offset+read, toRead );
            read += toRead;
            if ( !internalBuffer.hasRemaining() )
            {
                fill();
            }
        }
        return read;
    }

    public int available() throws IOException
    {
        ByteBuffer internalBuffer = buffer.getInternalBuffer();
        if ( !internalBuffer.hasRemaining() )
        {
            fill();
        }
        return internalBuffer.remaining();
    }
    
    private void fill() throws IOException
    {
        ByteBuffer internalBuffer = buffer.getInternalBuffer();
        assert !internalBuffer.hasRemaining();
        internalBuffer.clear();
        try
        {
            managedFile.read( buffer, inputOffset );
        }
        catch ( ManagedFileException exp )
        {
            IOException ioExp = new IOException( );
            ioExp.initCause(exp);
            throw ioExp;
        }
        internalBuffer.flip();
        inputOffset += internalBuffer.limit();
    }

    public void close() throws IOException
    {
//        NLogger.debug(NLoggerNames.GLOBAL, "Releasing ManagedInputStream: Buffer[" 
//            + buffer + "] ManagedFile[" + managedFile + "].");
        buffer.release();
    }
}