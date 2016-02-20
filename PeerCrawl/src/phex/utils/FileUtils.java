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
 *  $Id: FileUtils.java,v 1.26 2005/10/27 22:12:24 gregork Exp $
 */
package phex.utils;

import java.io.*;

import phex.common.FileHandlingException;

/**
 * Offers static util methods for file handling, like creating file path...
 */
public final class FileUtils
{
    private static final int BUFFER_LENGTH = 256 * 1024;

    /**
     * Dont create any instance!
     */
    private FileUtils()
    {
    }
    
    public static String getFileExtension( File file )
    {
        String name = file.getName();
        return getFileExtension(name);
    }
    
    public static String getFileExtension( String fileName )
    {
        int idx = fileName.lastIndexOf( '.' );
        if ( idx == -1 )
        {
            return "";
        }
        else
        {
            return fileName.substring(idx + 1);
        }
    }
    
    public static String replaceFileExtension( String fileName, String newExtension )
    {
        int idx = fileName.lastIndexOf( '.' );
        if ( idx == -1 )
        {
            return fileName + "." + newExtension;
        }
        else
        {
            return fileName.substring(0, idx + 1) + newExtension;
        }
    }

    /**
     * Since we are only supporting J2SE 1.2 and J2SE is not available on
     * MACOS 9 and MACOS X is supporting 255 characters we are shorting only
     * for 255 on MAC.
     *
     * MACOS X filename: 255
     * Windows pathlength: 260
     *         filelength: 255
     */
    public static String convertToLocalSystemFilename(String filename)
    {
        // we generally cut of at 255 it will suit everybody and we have no
        // os comparing stuff to do....
        // TODO we need to improve things here to keep up with the window pathlength
        // handling... but for now we just help the mac guys..
        filename = filename.replace( '/', '_' );
        return filename.substring( 0, Math.min( 255, filename.length() ) );
        /*if ( OS_NAME.indexOf("MAC") != -1)
        {
            return makeShortName(filename, 255);
        }
        else
        {
            return filename;
        }*/
    }

    /**
     * Appends the fileToAppend on the destination file. The file that is appended
     * will be removed afterwards.
     */
    public static void appendFile( File destination, File fileToAppend )
        throws IOException
    {
        long destFileLength = destination.length();
        long appendFileLength = fileToAppend.length();
        // open files
        FileInputStream inStream = new FileInputStream( fileToAppend );
        try
        {
            RandomAccessFile destFile = new RandomAccessFile( destination, "rw" );
            try
            {
                // extend file length... this causes dramatical performance boost since
                // contents is streamed into already freed space.
                destFile.setLength( destFileLength + appendFileLength );
                destFile.seek( destFileLength );
                byte[] buffer = new byte[ (int)Math.min( BUFFER_LENGTH, appendFileLength ) ];
                int length;
                while ( -1 != (length = inStream.read(buffer)) )
                {
                    long start2 = System.currentTimeMillis();
                    destFile.write( buffer, 0, length );
                    long end2 = System.currentTimeMillis();
                    try
                    {
                        Thread.sleep( (end2 - start2) * 2 );
                    }
                    catch ( InterruptedException exp )
                    {
                        // reset interrupted flag
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            finally
            {
                destFile.close();
                IOUtil.closeQuietly( destFile );
            }
        }
        finally
        {
            IOUtil.closeQuietly( inStream );
        }
        boolean succ = fileToAppend.delete();
        if ( !succ )
        {// if delete not successfull, trigger delete on exit.
            fileToAppend.deleteOnExit();
        }
    }
    
    /**
     * Copys the source file to the destination file. Old contents of the
     * destination file will be overwritten.
     * This is a optimized version of the org.apache.commons.io.FileUtils.copy
     * source, with larger file buffer for a faster copy process.
     */
    public static void copyFile( File source, File destination )
        throws IOException
    {
        copyFile( source, destination, source.length() );
        
        if (source.length() != destination.length())
        {
            String message = "Failed to copy full contents from " + source
                + " to " + destination;
            throw new IOException(message);
        }
    }

    /**
     * Copys the source file to the destination file. Old contents of the
     * destination file will be overwritten.
     * This is a optimized version of the org.apache.commons.io.FileUtils.copy
     * source, with larger file buffer for a faster copy process.
     */
    public static void copyFile( File source, File destination, long copyLength )
        throws IOException
    {   
        // check source exists
        if (!source.exists())
        {
            String message = "File " + source + " does not exist";
            throw new FileNotFoundException(message);
        }

        //does destinations directory exist ?
        if (destination.getParentFile() != null
            && !destination.getParentFile().exists())
        {
            destination.getParentFile().mkdirs();
        }

        //make sure we can write to destination
        if (destination.exists() && !destination.canWrite())
        {
            String message = "Unable to open file " + destination
                + " for writing.";
            throw new IOException(message);
        }

        //makes sure it is not the same file
        if (source.getCanonicalPath().equals(destination.getCanonicalPath()))
        {
            String message = "Unable to write file " + source + " on itself.";
            throw new IOException(message);
        }

        FileInputStream input = null;
        FileOutputStream output = null;
        try
        {
            input = new FileInputStream(source);
            output = new FileOutputStream(destination);
            long lengthLeft = copyLength;
            byte[] buffer = new byte[(int) Math.min(BUFFER_LENGTH,
                lengthLeft + 1)];
            int read;
            while ( lengthLeft > 0 )
            {
                read = input.read(buffer);
                if ( read == -1 )
                {
                    break;
                }
                lengthLeft -= read;
                output.write(buffer, 0, read);
            }            
        }
        finally
        {
            IOUtil.closeQuietly(input);
            IOUtil.closeQuietly(output);
        }

        //file copy should preserve file date
        destination.setLastModified(source.lastModified());
    }
    
    /**
     * Splits the source file at the splitpoint into the destination file.
     * The result will be the source file containing data to the split point and
     * the destination file containing the data from the split point to the end
     * of the file.
     * @param source the source file to split.
     * @param destination the destination file to split into.
     * @param splitPoint the split point byte position inside the file.
     * When the file size is 10 and the splitPoint is 3 the source file will contain
     * the first 3 bytes while the destination file will contain the last 7 bytes.
     * @throws IOException in case of a IOException during split operation.
     */
    public static void splitFile( File source, File destination, long splitPoint )
        throws IOException
    {
        // open files
        RandomAccessFile sourceFile = new RandomAccessFile( source, "rw" );
        try
        {
            FileOutputStream outStream = new FileOutputStream( destination );
            try
            {
                sourceFile.seek( splitPoint );
                byte[] buffer = new byte[ (int)Math.min( BUFFER_LENGTH, source.length() + 1) ];
                int length;
                while ( -1 != (length = sourceFile.read(buffer)) )
                {
                    outStream.write( buffer, 0, length );
                }
                sourceFile.setLength( splitPoint );
            }
            finally
            {
                IOUtil.closeQuietly(outStream);
            }
        }
        finally
        {
            IOUtil.closeQuietly(sourceFile);
        }
    }

    /**
     * Renames a file from its old filename to a new filename.
     */
    public static void renameLocalFile( File currentFile, File newFile )
        throws FileHandlingException
    {
        if ( newFile.exists() )
        {
            // cant rename to file that already exists
            throw new FileHandlingException(
                FileHandlingException.FILE_ALREADY_EXISTS );
        }

        if ( currentFile.exists() )
        {
            if ( !currentFile.renameTo( newFile ) )
            {
                // I don't know why it fails... :-(
                throw new FileHandlingException(
                    FileHandlingException.RENAME_FAILED );
            }
        }
    }

    public static void truncateFile( File file, long size )
        throws IOException
    {
        if ( size < 0 )
        {
            throw new IllegalArgumentException( "File size < 0: " + size );
        }
        if ( file.exists()
          && file.length() > size )
        {
            RandomAccessFile raf = new RandomAccessFile( file, "rw" );
            try
            {
                raf.setLength( size );
            }
            finally
            {
                IOUtil.closeQuietly(raf);
            }
        }
    }

    /**
     * Checks if subDir is a sub directory of maybeParentDir.
     */
    public static boolean isChildOfDir( File maybeChild, File maybeParentDir )
    {
        // it can't be a sub dir if they don't start the same way...
        if ( !maybeChild.getAbsolutePath().startsWith( maybeParentDir.getAbsolutePath() ) )
        {
            return false;
        }
        return isChildOfDirInternal( maybeChild, maybeParentDir );
    }

    /**
     * Checks if subDir is a sub directory of maybeParentDir. Used for internal
     * processing.
     */
    private static boolean isChildOfDirInternal( File maybeChild, File maybeParentDir )
    {
        File parent = maybeChild.getParentFile();
        // no parent dir... cant be sub
        if ( parent == null )
        {
            return false;
        }
        // parent equals we have a sub dir
        if ( parent.equals( maybeParentDir ) )
        {
            return true;
        }
        // go up one level and check again
        return isChildOfDirInternal( parent, maybeParentDir );
    }
}
