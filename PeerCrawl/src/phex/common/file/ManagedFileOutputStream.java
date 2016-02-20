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
 *  $Id: ManagedFileOutputStream.java,v 1.2 2005/11/19 14:44:08 gregork Exp $
 */
package phex.common.file;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import phex.utils.DirectByteBuffer;
import phex.utils.DirectByteBufferProvider;

public class ManagedFileOutputStream extends OutputStream
{   
    private DirectByteBuffer buffer;
    private ManagedFile managedFile;
    private long outputPosition;
    
    public ManagedFileOutputStream( ManagedFile managedFile, long outputPosition )
    {
        this.managedFile = managedFile;
        this.outputPosition = outputPosition;
        buffer = DirectByteBufferProvider.requestBuffer( 
            DirectByteBufferProvider.BUFFER_SIZE_64K);
    }
    
    public void write( int b ) throws IOException
    {
        ByteBuffer internalBuffer = buffer.getInternalBuffer();
        if ( !internalBuffer.hasRemaining() )
        {
            flush();
        }
        internalBuffer.put( (byte)b );
    }
    
    public void write(byte[] b, int offset, int length) throws IOException
    {
        if ((offset < 0) || (offset > b.length) || (length < 0) ||
            ((offset + length) > b.length) || ((offset + length) < 0)) 
        {
            throw new IndexOutOfBoundsException();
        }
        else if (length == 0) 
        {
            return;
        }
        
        ByteBuffer internalBuffer = buffer.getInternalBuffer();
        int written = 0;
        while ( written < length )
        {
            int toWrite = Math.min( length-written, internalBuffer.remaining() );
            internalBuffer.put( b, offset+written, toWrite );
            written += toWrite;
            if ( !internalBuffer.hasRemaining() )
            {
                flush();
            }
        }
    }
    
    public void flush() throws IOException
    {
        ByteBuffer internalBuffer = buffer.getInternalBuffer();
        internalBuffer.flip();
        try
        {
            managedFile.write(buffer, outputPosition );
        }
        catch ( ManagedFileException exp )
        {
            IOException ioExp = new IOException( "ManagedFileException: " 
                + exp.getMessage());
            ioExp.initCause(exp);
            throw ioExp;
        }
        outputPosition += internalBuffer.limit();
        internalBuffer.clear();
    }

    public void close() throws IOException
    {
        flush();
        buffer.release();
    }
}