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
 *  Created on 13.12.2004
 *  --- CVS Information ---
 *  $Id: FileRescanRunner.java,v 1.9 2005/11/19 14:39:34 gregork Exp $
 */
package phex.share;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import phex.common.Cfg;
import phex.common.ServiceManager;
import phex.common.ThreadTracking;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;
import phex.xml.XJBSharedFile;
import phex.xml.XJBSharedLibrary;


public class FileRescanRunner implements Runnable
{
    /**
     * if this thread is alive a rescan is running.
     */
    private static Thread rescanThread;
    
    private SharedFilesService sharedFilesService;
    
    /**
     * 
     */
    private ArrayList /*<File>*/ sharedDirectoryFiles;
        
    private List exclusionPatterns;
    
    /**
     * In between storage for shared directories.
     */
    private HashMap /*<File, SharedDirectory>*/ sharedDirectoryMap;
    private HashSet /*<SharedDirectory>*/ sharedDirectoryList;
    
    private boolean isInitialRescan;
    private HashMap sharedFilesCache;

    private FileRescanRunner( boolean isInitialRescan )
    {
        this.isInitialRescan = isInitialRescan;
        sharedFilesService = ShareManager.getInstance().getSharedFilesService();
        sharedDirectoryFiles = new ArrayList( 5 );
        exclusionPatterns = new ArrayList();
        sharedDirectoryMap = new HashMap();
        sharedDirectoryList = new HashSet( 5 );
    }
    
    /**
     * Rescans the shared files. Locking is done in the called methods to
     * have gaps between a rescanning session.
     * You can specifiy if this is a initial rescan or not. On a initial
     * rescan all SharedFiles are dropped and the stored shared files infos are
     * loaded to access urn infos.
     */
    public static void rescan( boolean isInitialRescan, boolean allowInterrupt )
    {
        if ( allowInterrupt && rescanThread != null && rescanThread.isAlive() )
        {
            NLogger.debug(NLoggerNames.LIBRARY_SCANNER,
                "Interrupting rescan thread." );
            // interrupt running thread to restart rescan...
            rescanThread.interrupt();
            // join interrupted thread and wait till its dead before
            // rescan starts
            try
            {
                NLogger.debug(NLoggerNames.LIBRARY_SCANNER,
                    "Waiting for interrupted rescan thread." );
                rescanThread.join();
            }
            catch ( InterruptedException exp )
            {
                NLogger.warn( NLoggerNames.LIBRARY_SCANNER, exp, exp );
            }
        }
        if ( rescanThread == null || !rescanThread.isAlive() )
        {
            FileRescanRunner runner = new FileRescanRunner( isInitialRescan );
            rescanThread = new Thread( ThreadTracking.rootThreadGroup, runner,
                "FileRescanRunner-" + Integer.toHexString( runner.hashCode() ) );
            rescanThread.setDaemon( true );
            rescanThread.setPriority( Thread.MIN_PRIORITY );
            rescanThread.start();
        }
    }

    public void run()
    {
        NLogger.debug( NLoggerNames.LIBRARY_SCANNER,
            "Staring file rescan (Initial: " + isInitialRescan + ")." );

        Cfg cfg = ServiceManager.sCfg;
        sharedDirectoryFiles = new ArrayList( cfg.sharedDirectoriesSet );
        setExclusionFilter( cfg.libraryExclusionRegExList );

        if ( rescanThread.isInterrupted() )
        {
            return;
        }

        if ( isInitialRescan )
        {
            sharedFilesService.clearSharedFiles();
            if ( rescanThread.isInterrupted() )
            {
                return;
            }
            buildSharedFilesCache();
        }
        else
        {
            removeUnsharedFiles();
        }
        if ( rescanThread.isInterrupted() )
        {
            return;
        }

        HashMap scannedDirMap = new HashMap();

        Iterator iterator = sharedDirectoryFiles.iterator();
        while (iterator.hasNext())
        {
            String dirStr = (String) iterator.next();
            File dir = new File( dirStr );
            scanDir( dir, scannedDirMap );
            if ( rescanThread.isInterrupted() )
            {
                return;
            }
        }
        sharedFilesService.updateSharedDirecotries( sharedDirectoryMap, 
            sharedDirectoryList );
        sharedFilesService.triggerSaveSharedFiles();
    }

    private void buildSharedFilesCache()
    {
        sharedFilesCache = new HashMap();
        XJBSharedLibrary library = sharedFilesService.loadSharedLibrary();
        Iterator iterator = library.getSharedFileList().iterator();
        while ( iterator.hasNext() && !rescanThread.isInterrupted() )
        {
            XJBSharedFile cachedFile = (XJBSharedFile) iterator.next();
            sharedFilesCache.put( cachedFile.getFileName(), cachedFile );
        }
    }

    /**
     * Scans a directory for files to share.
     * @param dir the directory to scan.
     * @param scannedDirs the directorys already scanned. This is used to
     *        keep it from scanning continuously through of symbolicly
     *        linked directorys in unix systems. See sf bug #603736
     * @param recursive whether we scan this directory recursive or not
     */
    private void scanDir( File dir, HashMap scannedDirMap )
    {
        // verify if dir was already scanned.
        String canonicalPath;
        try
        {
            canonicalPath = dir.getCanonicalPath();
        }
        catch ( IOException exp )
        {
            NLogger.warn( NLoggerNames.LIBRARY_SCANNER, exp, exp );
            return;
        }
        if ( scannedDirMap.containsKey( canonicalPath ) )
        {// directory was already scanned...
            return;
        }
        else
        {// not scanned... now add it as scanned...
            scannedDirMap.put( canonicalPath, "" );
        }

        if ( dir.isDirectory() )
        {
            handleScannedDir( dir );
        }

        File[] files = dir.listFiles();

        if ( files == null )
        {
            NLogger.error( NLoggerNames.LIBRARY_SCANNER, 
                "'" + dir + "' is not a directory." );
            return;
        }

        for (int j = 0; j < files.length && !rescanThread.isInterrupted(); j++)
        {
            if ( isFileInvalid( files[j] ) )
            {
                continue;
            }

            if ( files[j].isFile() )
            {
                handleScannedFile( files[j] );
            }
            // not recursive
            //else if ( files[j].isDirectory() )
            //{
            //    scanDir( files[j], scannedDirMap );
            //}
        }
    }
    
    private void handleScannedDir( File file )
    {
        if ( rescanThread.isInterrupted() )
        {
            return;
        }
        SharedDirectory sharedDirectory;
        if ( isInitialRescan )
        {
            sharedDirectory = (SharedDirectory) sharedDirectoryMap.get(file);
            if ( sharedDirectory == null )
            {
                sharedDirectory = new SharedDirectory( file );
                sharedDirectory.setType(SharedDirectory.SHARED_DIRECTORY);
                sharedDirectoryMap.put(file, sharedDirectory);
                sharedDirectoryList.add(sharedDirectory);
            }
            else
            {
                sharedDirectory.setType(SharedDirectory.SHARED_DIRECTORY);
            }
        }
        else
        {
            sharedDirectory = (SharedDirectory) sharedDirectoryMap.get(file);
            if ( sharedDirectory == null )
            {
                sharedDirectory = new SharedDirectory( file );
                sharedDirectory.setType(SharedDirectory.SHARED_DIRECTORY);
                sharedDirectoryMap.put(file, sharedDirectory);
                sharedDirectoryList.add(sharedDirectory);
            }
            else
            {
                sharedDirectory.setType(SharedDirectory.SHARED_DIRECTORY);
            }
        }
        
        // set parents to partialy shared.
        File parent = file.getParentFile();
        while( parent != null )
        {
            if ( parent.isDirectory() )
            {
                sharedDirectory = (SharedDirectory) sharedDirectoryMap.get(parent);
                if ( sharedDirectory == null )
                {
                    sharedDirectory = new SharedDirectory( parent );
                    sharedDirectory.setType(SharedDirectory.UNSHARED_PARENT_DIRECTORY);
                    sharedDirectoryMap.put(parent, sharedDirectory);
                    sharedDirectoryList.add(sharedDirectory);
                }                
            }
            parent = parent.getParentFile();
        }
    }

    private void handleScannedFile( File file )
    {
        ShareFile shareFile;
        if ( isInitialRescan )
        {
            shareFile = new ShareFile( file );
            // Try to find cached file info
            XJBSharedFile xjbFile = (XJBSharedFile)sharedFilesCache.get(
                file.getAbsolutePath() );
            if ( xjbFile != null &&
                 xjbFile.getLastModified() == file.lastModified() )
            {
                shareFile.updateFromCache( xjbFile );
                // add the urn to the map to share by urn
                sharedFilesService.addUrn2FileMapping(shareFile);
            }
            else
            {
                sharedFilesService.queueUrnCalculation( shareFile );
                if ( rescanThread.isInterrupted() )
                {
                    return;
                }
            }
            sharedFilesService.addSharedFile( shareFile );
        }
        else
        {
            // try to find file in already existing share
            shareFile = sharedFilesService.getFileByName( file.getAbsolutePath() );
            if ( shareFile == null )
            {// create new file
                shareFile = new ShareFile( file );
                sharedFilesService.queueUrnCalculation(shareFile);
                if ( rescanThread.isInterrupted() )
                {
                    return;
                }
                sharedFilesService.addSharedFile( shareFile );
            }
        }
    }

    private void removeUnsharedFiles()
    {
        ShareFile[] sharedFiles = sharedFilesService.getSharedFiles();
        for (int i = 0; i < sharedFiles.length && !rescanThread.isInterrupted(); i++)
        {
            File file = sharedFiles[i].getSystemFile();
            if (!isInSharedDirectory(file) || !file.exists())
            {
                sharedFilesService.removeSharedFile(sharedFiles[i]);
            }
        }
    }

    private boolean isInSharedDirectory( File file )
    {
        Iterator iterator = sharedDirectoryFiles.iterator();
        while ( iterator.hasNext() )
        {
            String sharedDir = (String) iterator.next();
            File sharedDirFile = new File( sharedDir );
            if ( file.getParentFile().equals(sharedDirFile) )
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Called when rescanning files
     */
    private void setSharedDirectories( String dirs )
    {
        StringTokenizer tokens = new StringTokenizer(dirs, ";");
        int count = tokens.countTokens();

        sharedDirectoryFiles.clear();
        sharedDirectoryFiles.ensureCapacity(count);

        while (tokens.hasMoreTokens())
        {
            File dir = new File(tokens.nextToken().trim());
            if (!sharedDirectoryFiles.contains(dir))
            {
                sharedDirectoryFiles.add(dir);
            }
        }
    }
    
    private void setExclusionFilter( List exclusionList )
    {
        exclusionPatterns.clear();
        Iterator iterator = exclusionList.iterator();
        while (iterator.hasNext())
        {
            String regExp = (String) iterator.next();
            try
            {
                Pattern pattern = Pattern.compile( regExp );
                exclusionPatterns.add(pattern);
            }
            catch ( PatternSyntaxException exp )
            {
                NLogger.error( NLoggerNames.LIBRARY_SCANNER, exp, exp );
            }
        }
    }
    
    /**
     * In case the user is sharing the download directory, skip the
     * download-in-progress files, index files and alias files. Even though
     * the user should not be able to configure the download directory as shared
     * directory since the new option dialog.
     */
    public boolean isFileInvalid(File file)
    {
        // In case the user is sharing the download directory,
        // skip the download-in-progress files.
        if (file.getName().toLowerCase().endsWith(".dl"))
        {
            return true;
        }
        if ( isExcludedRegExp(file) )
        {
            return true;
        }
        return false;
    }
    
    /**
     * To match all = .*
     * To match none = (?!pagefile\.sys).*
     * @param file
     * @return
     */
    private boolean isExcludedRegExp( File file )
    {
        // use filter only for files not directories
        if ( file.isDirectory() )
        {
            return true;
        }

        String name = file.getName();
        Iterator patternIterator = exclusionPatterns.iterator();
        while (patternIterator.hasNext())
        {
            Pattern pattern = (Pattern) patternIterator.next();
            Matcher m = pattern.matcher( name );
            if ( m.matches() )
            {
                return true;
            }
        }
        return false;
    }
}