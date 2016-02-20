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
 *  Created on 06.09.2005
 *  --- CVS Information ---
 *  $Id: ManagedFile.java,v 1.3 2005/10/08 22:55:38 gregork Exp $
 */
package phex.common.file;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import edu.oswego.cs.dl.util.concurrent.ReentrantLock;

import phex.utils.DirectByteBuffer;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

/**
 * Represents a file on the file system with managemend functionality to ensure
 * proper access.
 */
public class ManagedFile implements ReadOnlyManagedFile
{
    private static final int MAX_WRITE_TRIES = 10;
    private static final int WRITE_RETRY_DELAY = 100;
    public static final int READ_ONLY_ACCESS = 0;
    public static final int READ_WRITE_ACCESS = 1;
    
    private ReentrantLock lock;
    private File fsFile;
    private int accessMode;
    private RandomAccessFile raFile;
    
    public ManagedFile( File file )
    {
        fsFile = file;
        lock = new ReentrantLock();
    }
    
    public File getFile()
    {
        return fsFile;
    }
    
    public void setAccessMode( int mode )
        throws ManagedFileException
    {
        // close file handle if in read mode and write is required.
        if ( accessMode == READ_ONLY_ACCESS &&
             mode == READ_WRITE_ACCESS )
        {
            closeFile();
        }
        accessMode = mode;
    }
    
    private void checkOpenFile( )
        throws ManagedFileException
    {
        
        try
        {
            lock.acquire();
        }
        catch ( InterruptedException exp )
        {
            Thread.currentThread().interrupt();
            throw new ManagedFileException( "open failes: interrupted", exp );
        }
        
        try
        {
            // check if already open.
            if ( raFile != null )
            {
                FileManager.getInstance().trackFileInUse(this);
                return;
            }
            FileManager.getInstance().trackFileOpen(this);
        
            try
            {        
                raFile = new RandomAccessFile( fsFile, 
                    accessMode == READ_ONLY_ACCESS ? "r" : "rwd" );
            }
            catch( Exception exp )
            {
                throw new ManagedFileException( "failed to open", exp );
            }
        }
        finally
        {
            lock.release();
        }
    }
    
    public void closeFile( )
        throws ManagedFileException
    {
        // check if already closed.
        if ( raFile == null )
        {
            return;
        }  
        
        try
        {
            lock.acquire();
        }
        catch ( InterruptedException exp )
        {
            Thread.currentThread().interrupt();
            throw new ManagedFileException( "close failes: interrupted", exp );
        }
        try
        {
            try
            {
                NLogger.debug( NLoggerNames.ManagedFile, "Closing file." );
                raFile.close();
            }
            catch( Exception exp )
            {
                throw new ManagedFileException( "failed to close", exp );
            }
        }
        finally
        {
            raFile = null;
            FileManager.getInstance().trackFileClose(this);
            lock.release();
        }
    }
    
    public void write( DirectByteBuffer buffer, long pos ) 
        throws ManagedFileException
    {
        try
        {
            lock.acquire();
        }
        catch ( InterruptedException exp )
        {
            Thread.currentThread().interrupt();
            throw new ManagedFileException( "write failes: interrupted", exp );
        }
        try
        {
            checkOpenFile();
            if (raFile == null)
            {
                throw new ManagedFileException( "write failes: raFile null" );
            }
            FileChannel channel = raFile.getChannel();
            if ( !channel.isOpen() )
            {
                throw new ManagedFileException( "write failes: not open" );
            }
            channel.position( pos );
            
            ByteBuffer byteBuffer = buffer.getInternalBuffer();
            
            int tryCount = 0;
            while ( byteBuffer.position() != byteBuffer.limit() )
            {
                int written = channel.write( byteBuffer );
                if ( written > 0 )
                {
                    tryCount = 0;
                }
                else
                {
                    if ( tryCount >= MAX_WRITE_TRIES )
                    {
                        throw new ManagedFileException( "write failes: max retries" );
                    }
                    // sleep a bit until we retry.
                    try
                    {
                        Thread.sleep( WRITE_RETRY_DELAY * tryCount );
                    }
                    catch( InterruptedException e )
                    {
                        Thread.currentThread().interrupt();
                        throw new ManagedFileException( "write failes: interrupted" );
                    }
                }
            }            
        }
        catch ( Exception exp )
        {
            throw new ManagedFileException( "write fails", exp );
        }
        finally
        {
            lock.release();
        }
    }
    
    
    public void read( DirectByteBuffer buffer, long pos )
        throws ManagedFileException
    {
        try
        {
            lock.acquire();
        }
        catch ( InterruptedException exp )
        {
            Thread.currentThread().interrupt();
            throw new ManagedFileException( "read failes: interrupted", exp );
        }
        
        try
        {
            checkOpenFile();
            if (raFile == null)
            {
                throw new ManagedFileException( "read failes: raFile null" );
            }
            FileChannel channel = raFile.getChannel();
            if ( !channel.isOpen() )
            {
                throw new ManagedFileException( "read failes: not open" );
            }
            
            channel.position( pos );
            ByteBuffer byteBuffer = buffer.getInternalBuffer();
            while ( channel.position() < channel.size() && byteBuffer.hasRemaining() )
            {
                channel.read( byteBuffer );
            }
        }
        catch ( Exception exp )
        {
            throw new ManagedFileException( "read fails", exp );
        }
        finally
        {
            lock.release();
        }
    }

    public void setLength( long newLength ) throws ManagedFileException
    {
        try
        {
            lock.acquire();
        }
        catch ( InterruptedException exp )
        {
            Thread.currentThread().interrupt();
            throw new ManagedFileException( "read failes: interrupted", exp );
        }
        
        try
        {
            checkOpenFile();
            if (raFile == null)
            {
                throw new ManagedFileException( "read failes: raFile null" );
            }
            raFile.setLength( newLength );
        }
        catch ( Exception exp )
        {
            throw new ManagedFileException( "read fails", exp );
        }
        finally
        {
            lock.release();
        }
    }
}