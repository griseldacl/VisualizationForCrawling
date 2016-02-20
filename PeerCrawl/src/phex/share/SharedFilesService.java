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
 *  $Id: SharedFilesService.java,v 1.10 2005/11/19 14:39:35 gregork Exp $
 */
package phex.share;

import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.xml.bind.JAXBException;

import phex.common.*;
import phex.common.file.FileManager;
import phex.common.file.ManagedFile;
import phex.common.file.ManagedFileException;
import phex.event.AsynchronousDispatcher;
import phex.event.ShareChangeListener;
import phex.event.UserMessageListener;
import phex.thex.ThexCalculationWorker;
import phex.utils.*;
import phex.xml.*;

/**
 *
 */
public class SharedFilesService
{
    private ReadWriteLock rwLock;
    
    /**
     * This HashMap maps native File objects to its shared counter part in the 
     * phex system.
     */
    private HashMap /*<File, SharedDirectory>*/ directoryShareMap;
    
    private ArrayList /*<SharedDirectory>*/ sharedDirectories;
    
    /**
     * A maps that maps URNs to the file they belong to. This is for performant
     * searching by urn.
     * When accesseing this object locking via the rwLock object is required.
     */
    private HashMap /*<URN, ShareFile>*/ urnToFileMap;
    
    /**
     * This map contains all absolute file paths as keys for the ShareFile
     * behind it.
     * When accesseing this object locking via the rwLock object is required.
     */
    private HashMap /*<String, ShareFile>*/ nameToFileMap;
    
    /**
     * This lists holds all shared files at there current index position.
     * When files are un-shared during runtime a null is placed at the index
     * position of the removed file. Access via the file index is done using
     * the method getFileByIndex( fileIndex ).
     * When accesseing this object locking via the rwLock object is required.
     */
    private ArrayList /*<ShareFile>*/ indexedSharedFiles;
    
    /**
     * This list contains the shared files without gaps. It is used for direct
     * and straight access via the getFileAt( position ). Also it is used for
     * getFileCount().
     * When accesseing this object locking via the rwLock object is required.
     */
    private ArrayList sharedFiles;

    /**
     * The total size of the shared files.
     */
    private int totalFileSizeKb;
    
    /**
     * A instance of a background runner queue to calculate
     * urns.
     */
    private RunnerQueueWorker urnThexCalculationRunner;
    
    /**
     * Lock object to lock saving of shared file lists.
     */
    private static Object saveSharedFilesLock = new Object();
    
    /**
     * Object that holds the save job instance while a save job is running. The
     * reference is null if the job is not running.
     */
    private SaveSharedFilesJob saveSharedFilesJob;
    
    /**
     * All listeners interested in events.
     */
    private ArrayList listenerList = new ArrayList( 2 );
    
    public SharedFilesService()
    {
        rwLock = new ReadWriteLock();
        urnThexCalculationRunner = new RunnerQueueWorker();
        
        Environment.getInstance().scheduleTimerTask( 
            new FileRescanTimer(), FileRescanTimer.TIMER_PERIOD,
            FileRescanTimer.TIMER_PERIOD );
        
        directoryShareMap = new HashMap();
        sharedDirectories = new ArrayList();
        urnToFileMap = new HashMap();
        nameToFileMap = new HashMap();
        indexedSharedFiles = new ArrayList();
        sharedFiles = new ArrayList();
        totalFileSizeKb = 0;
    }
    
    public String getSharedFilePath( File file )
    {
        rwLock.readLock();
        try
        {            
            File highestDir = file.getParentFile();
            Iterator iterator = directoryShareMap.keySet().iterator();
            while ( iterator.hasNext() )
            {
                File dir = (File) iterator.next();
                SharedDirectory sharedDir = (SharedDirectory) directoryShareMap.get(dir);
                if ( sharedDir.getType() == SharedDirectory.SHARED_DIRECTORY
                  && FileUtils.isChildOfDir( file, dir )
                  && FileUtils.isChildOfDir(highestDir, dir))
                {
                    highestDir = dir;
                }
            }
            // also share the shared dir itself.
            File highestParent = highestDir.getParentFile();
            if ( highestParent != null )
            {
                highestDir = highestParent;
            }
            String pathStr = highestDir.getAbsolutePath();
            int length = pathStr.length();
            if ( !pathStr.endsWith( File.separator ) )
            {
                length++;
            }
            return file.getAbsolutePath().substring( length );
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { NLogger.error( NLoggerNames.Sharing, exp, exp ); }
        }
    }
    
    /**
     * Returns the a shared file by name. If the given name is null or a
     * file with this name is not found then null is returned.
     */
    public ShareFile getFileByName( String name )
    {
        rwLock.readLock();
        try
        {
            return (ShareFile) nameToFileMap.get( name );
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { NLogger.error( NLoggerNames.Sharing, exp, exp ); }
        }
    }
    
    /**
     * Returns the a shared file by file. If the given file is null or a
     * file is not found then null is returned.
     */
    public ShareFile getShareFileByFile( File file )
    {
        return getFileByName( file.getAbsolutePath() );
    }
    
    /**
     * Gets the file at the given index in the shared file list.
     * To access via the file index use the method getFileByIndex( fileIndex )
     */
    public ShareFile getFileAt( int index )
    {
        rwLock.readLock();
        try
        {
            if ( index >= sharedFiles.size() )
            {
                return null;
            }
            return (ShareFile) sharedFiles.get( index );
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { NLogger.error( NLoggerNames.Sharing, exp, exp ); }
        }
    }
    
    /**
     * Returns an array of all shared files.
     */
    public ShareFile[] getSharedFiles()
    {
        rwLock.readLock();
        try
        {
            ShareFile[] array = new ShareFile[ sharedFiles.size() ];
            array = (ShareFile[])sharedFiles.toArray( array );
            return array;
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { NLogger.error( NLoggerNames.Sharing, exp, exp ); }
        }
    }
    
    /**
     * Returns the shared files count.
     */
    public int getFileCount()
    {
        rwLock.readLock();
        try
        {
            return sharedFiles.size();
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { NLogger.error( NLoggerNames.Sharing, exp, exp ); }
        }
    }
    
    /**
     * Returns the total size of all shared files in KB.
     */
    public int getTotalFileSizeInKb()
    {
        rwLock.readLock();
        try
        {
            return totalFileSizeKb;
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { NLogger.error( NLoggerNames.Sharing, exp, exp ); }
        }
    }
    
    /**
     * Returns a shared file by its index number.
     */
    public ShareFile getFileByIndex( int fileIndex )
        throws IndexOutOfBoundsException
    {
        rwLock.readLock();
        try
        {
            if ( fileIndex >= indexedSharedFiles.size() )
            {
                return null;
            }
            return (ShareFile) indexedSharedFiles.get( fileIndex );
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { NLogger.error( NLoggerNames.Sharing, exp, exp ); }
        }
    }
    
    /**
     * Returns a shared file by its urn. If the given urn is null or a
     * file with this URN is not found then null is returned.
     */
    public ShareFile getFileByURN( URN fileURN )
        throws IndexOutOfBoundsException
    {
        rwLock.readLock();
        try
        {
            if ( fileURN == null )
            {
                return null;
            }
            return (ShareFile) urnToFileMap.get( fileURN );
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { NLogger.error( NLoggerNames.Sharing, exp, exp ); }
        }
    }
    
    /**
     * Returns whether a file with the given URN is shared or not. 
     * @return true when a file with the given URN is shared, false otherwise.
     */
    public boolean isURNShared( URN fileURN )
        throws IndexOutOfBoundsException
    {
        rwLock.readLock();
        try
        {
            if ( fileURN == null )
            {
                return false;
            }
            return urnToFileMap.containsKey( fileURN );
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { NLogger.error( NLoggerNames.Sharing, exp, exp ); }
        }
    }
    
    public List getFilesByURNs( URN[] urns )
    {
        rwLock.readLock();
        try
        {
            ArrayList results = new ArrayList( urns.length );
            for( int i = 0; i < urns.length; i++ )
            {
                Object obj = urnToFileMap.get( urns[i] );
                if ( obj != null )
                {
                    results.add( obj );
                }
            }
            return results;
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { NLogger.error( NLoggerNames.Sharing, exp, exp ); }
        }
    }
    
    /**
     * Adds a shared file if its not already shared.
     * Its importent that the file owns a valid urn when being added.
     */
    public void addSharedFile( ShareFile shareFile )
    {
        File file = shareFile.getSystemFile();
        // check if file is already there
        if ( getFileByName( file.getAbsolutePath() ) != null )
        {
            return;
        }
        
        rwLock.writeLock();
        int position;
        try
        {
            position = indexedSharedFiles.size();
            shareFile.setFileIndex( position );
            //shareFile.setAlias((String)aliasMapping.get(file.getName()));
            //addWordIndex(file.getName(), fileNumber, wordFiles);
            indexedSharedFiles.add( shareFile );
            sharedFiles.add( shareFile );

            // dont add to urn map yet since urns get calculated in background.
            nameToFileMap.put( file.getAbsolutePath(), shareFile );
            totalFileSizeKb += file.length() / 1024;
        }
        finally
        {
            try{ rwLock.writeUnlock(); }
            catch (IllegalAccessException exp )
            { NLogger.error( NLoggerNames.Sharing, exp, exp ); }
        }
        //fireSharedFileAdded( position );
    }
    
    /**
     * Removed a shared file if its shared.
     */
    public void removeSharedFile( ShareFile shareFile )
    {
        rwLock.writeLock();
        int position;
        try
        {
            // clear index position...
            int fileIndex = shareFile.getFileIndex();
            indexedSharedFiles.set( fileIndex, null );

            // remove name to file map
            File file = shareFile.getSystemFile();
            urnToFileMap.remove( shareFile.getURN() );
            nameToFileMap.remove( file.getAbsolutePath() );

            // try to find shareFile in access list
            position = sharedFiles.indexOf( shareFile );
            if ( position != -1 )
            {// if removed update data
                sharedFiles.remove( position );
                totalFileSizeKb -= shareFile.getFileSize() / 1024;
            }
        }
        finally
        {
            try{ rwLock.writeUnlock(); }
            catch (IllegalAccessException exp )
            { NLogger.error( NLoggerNames.Sharing, exp, exp ); }
        }
        //if ( position != -1 )
        //{// if removed fire events
        //    fireSharedFileRemoved( position );
        //}
    }
    
    /**
     * Adds a shared file if its not already shared.
     */
    public void updateSharedDirecotries( HashMap sharedDirectoryMap,
        HashSet sharedDirectoryList )
    {
        rwLock.writeLock();
        try
        {
            directoryShareMap.clear();
            directoryShareMap.putAll(sharedDirectoryMap);
            sharedDirectories.clear();
            sharedDirectories.addAll(sharedDirectoryList);
            sharedDirectoriesChanged();
        }
        finally
        {
            try{ rwLock.writeUnlock(); }
            catch (IllegalAccessException exp )
            { NLogger.error( NLoggerNames.Sharing, exp, exp ); }
        }
    }
    
    /**
     * Do not modify!
     * @return
     */
    public SharedDirectory[] getSharedDirectories()
    {
        rwLock.readLock();
        try
        {
            SharedDirectory[] array = new SharedDirectory[ sharedDirectories.size() ];
            array = (SharedDirectory[])sharedDirectories.toArray( array );
            return array;
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { NLogger.error( NLoggerNames.Sharing, exp, exp ); }
        }
    }
    
    /**
     * Adds a shared file if its not already shared.
     * Its importent that the file owns a valid urn when being added.
     */
    public SharedDirectory getSharedDirectory( File file )
    {
        if ( !file.isDirectory() )
        {
            return null;
        }
        rwLock.readLock();
        try
        {
            SharedResource resource = (SharedResource) directoryShareMap.get(file);
            if ( resource instanceof SharedDirectory )
            {
                return (SharedDirectory)resource;
            }
            return null;
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { NLogger.error( NLoggerNames.Sharing, exp, exp ); }
        }
    }
    
    /**
     * Adds a urn to file mapping for this ShareFile. When calling make sure
     * the urn is already set.
     * @param shareFile
     */
    public void addUrn2FileMapping( ShareFile shareFile )
    {
        assert( shareFile.getURN() != null );
        urnToFileMap.put( shareFile.getURN(), shareFile );
    }
    
    /**
     * Queues a share file for calculating the urn.
     * @param shareFile
     */
    public void queueUrnCalculation( ShareFile shareFile )
    {
        UrnCalculationWorker worker = new UrnCalculationWorker(
            shareFile);
        urnThexCalculationRunner.add( worker );
    }
    
    /**
     * Queues a share file for calculating the Thex.
     * @param shareFile
     */
    public void queueThexCalculation( ShareFile shareFile )
    {
        ThexCalculationWorker worker = new ThexCalculationWorker(
            shareFile);
        urnThexCalculationRunner.add( worker );
    }
    
    /**
     * Clears the complete shared file list. Without making information
     * persistent.
     */
    public void clearSharedFiles()
    {
        rwLock.writeLock();
        try
        {
            urnThexCalculationRunner.stopAndClear();
            sharedFiles.clear();
            indexedSharedFiles.clear();
            urnToFileMap.clear();
            nameToFileMap.clear();
            totalFileSizeKb = 0;
        }
        finally
        {
            try{ rwLock.writeUnlock(); }
            catch (IllegalAccessException exp )
            { NLogger.error( NLoggerNames.Sharing, exp, exp ); }
        }
        //fireAllSharedFilesChanged();
    }
    
    /**
     * Triggers a save of the download list. The call is not blocking and returns
     * directly, the save process is running in parallel.
     */
    public void triggerSaveSharedFiles( )
    {
        NLogger.debug(NLoggerNames.Sharing,
            "Trigger save shared files..." );
        synchronized( saveSharedFilesLock )
        {
            if ( saveSharedFilesJob != null )
            {
                // save shared files is already in progress. we rerequest a save.
                saveSharedFilesJob.triggerFollowUpSave();
            }
            else
            {
                saveSharedFilesJob = new SaveSharedFilesJob();
                saveSharedFilesJob.start();
            }
        }
    }
    
    public XJBSharedLibrary loadSharedLibrary()
    {
        NLogger.debug( NLoggerNames.Sharing,
            "Load shared library configuration file." );
        File file = Environment.getInstance().getPhexConfigFile(
            EnvironmentConstants.XML_SHARED_LIBRARY_FILE_NAME );

        ObjectFactory objFactory = new ObjectFactory();
        XJBPhex phex;        
        try
        {
            ManagedFile managedFile = FileManager.getInstance().getReadWriteManagedFile( file );
            phex = XMLBuilder.loadXJBPhexFromFile( managedFile );
            if ( phex == null )
            {
                NLogger.debug( NLoggerNames.Sharing,
                    "No shared library configuration file found." );
                return objFactory.createXJBSharedLibrary();
            }
        }
        catch ( JAXBException exp )
        {
            Throwable linkedException = exp.getLinkedException();
            if ( linkedException != null )
            {
                NLogger.error( NLoggerNames.Sharing, linkedException, linkedException );
            }
            NLogger.error( NLoggerNames.Sharing, exp, exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.SharedFilesLoadFailed, 
                new String[]{ exp.toString() } );
            try
            {
                return objFactory.createXJBSharedLibrary();
            }
            catch ( JAXBException exp2 )
            {
                NLogger.error( NLoggerNames.Sharing, exp2, exp2 );
                throw new RuntimeException( exp2.getMessage() );
            }
        }
        catch ( ManagedFileException exp )
        {
            NLogger.error( NLoggerNames.Sharing, exp, exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.SharedFilesLoadFailed, 
                new String[]{ exp.toString() } );
            try
            {
                return objFactory.createXJBSharedLibrary();
            }
            catch ( JAXBException exp2 )
            {
                NLogger.error( NLoggerNames.Sharing, exp2, exp2 );
                throw new RuntimeException( exp2.getMessage() );
            }
        }

        // update old download list
        XJBSharedLibrary sharedLibrary = phex.getSharedLibrary();
        return sharedLibrary;
    }
        
    ///////////////////// START event handling methods ////////////////////////
    public void addSharedFilesChangeListener( ShareChangeListener listener )
    {
        listenerList.add( listener );
    }

    public void removeSharedFilesChangeListener( ShareChangeListener listener )
    {
        listenerList.remove( listener );
    }
    
    private void sharedDirectoriesChanged()
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                ShareChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for (int i = listeners.length - 1; i >= 0; i--)
                {
                    listener = (ShareChangeListener) listeners[i];
                    listener.sharedDirectoriesChanged();
                }
            }
        });
    }
    ///////////////////// END event handling methods /////////////////////////
    
    ///////////////////// START inner classes        /////////////////////////
    
    private class FileRescanTimer extends TimerTask
    {
        // once per minute
        public static final long TIMER_PERIOD = 1000 * 60;

        public void run()
        {
            try
            {
                FileRescanRunner.rescan( false, false );
            }
            catch ( Throwable th )
            {
                NLogger.error( NLoggerNames.Sharing, th, th );
            }
        }
    }
    
    private class SaveSharedFilesJob extends Thread
    {
        private boolean isFollowUpSaveTriggered;

        public SaveSharedFilesJob()
        {
            super( ThreadTracking.rootThreadGroup, "SaveSharedFilesJob" );
            setPriority( Thread.MIN_PRIORITY );
        }

        public void triggerFollowUpSave()
        {
            isFollowUpSaveTriggered = true;
        }

        /**
         * Saving of the download list is done asynchronously to make sure that there
         * will be no deadlocks happening
         */
        public void run()
        {
            do
            {
                NLogger.debug( NLoggerNames.Sharing, "Saving shared library." );
                isFollowUpSaveTriggered = false;
                rwLock.readLock();
                try
                {
                    ObjectFactory objFactory = new ObjectFactory();
                    // workaround for phex root... must be at a higher position for global use
                    XJBPhex xphex = objFactory.createPhexElement();

                    XJBSharedLibrary library = createXJBSharedLibrary();
                    xphex.setSharedLibrary( library );
                    xphex.setPhexVersion( VersionUtils.getFullProgramVersion() );

                    // first save into temporary file...
                    File tmpFile = Environment.getInstance().getPhexConfigFile(
                        EnvironmentConstants.XML_SHARED_LIBRARY_FILE_NAME
                            + ".tmp" );
                    ManagedFile managedFile = FileManager.getInstance()
                        .getReadWriteManagedFile( tmpFile );
                    XMLBuilder.saveToFile( managedFile, xphex );

                    // after saving copy temporary file to real file.
                    File libraryFile = Environment.getInstance()
                        .getPhexConfigFile(
                            EnvironmentConstants.XML_SHARED_LIBRARY_FILE_NAME );
                    FileUtils.copyFile( tmpFile, libraryFile );

                    //File zipFile = Environment.getInstance().getPhexConfigFile(
                    //    EnvironmentConstants.XML_SHARED_LIBRARY_FILE_NAME + ".def" );
                    //OutputStream out = new DeflaterOutputStream( new FileOutputStream( zipFile ) );
                    //FileInputStream inStream = new FileInputStream( libraryFile );
                    //int c;
                    //byte[] buffer = new byte[16*1024];
                    //while ( (c = inStream.read( buffer )) != -1 )
                    //{
                    //    out.write(buffer, 0, c);
                    //}
                }
                catch (JAXBException exp)
                {
                    // TODO during close this message is never displayed since application
                    // will exit too fast. A solution to delay exit process in case 
                    // SlideInWindows are open needs to be found.
                    Throwable linkedException = exp.getLinkedException();
                    if ( linkedException != null )
                    {
                        NLogger.error( NLoggerNames.Sharing, linkedException,
                            linkedException );
                    }
                    NLogger.error( NLoggerNames.Sharing, exp, exp );
                    Environment.getInstance().fireDisplayUserMessage(
                        UserMessageListener.SharedFilesSaveFailed, new String[]
                        { exp.toString() } );
                }
                catch (ManagedFileException exp)
                {
                    if ( exp.getCause() instanceof InterruptedException )
                    { // the thread was interrupted and requested to stop, most likley
                        // by user request.
                        NLogger.debug( NLoggerNames.Sharing, exp );
                    }
                    else
                    {
                        // TODO during close this message is never displayed since application
                        // will exit too fast. A solution to delay exit process in case 
                        // SlideInWindows are open needs to be found.
                        NLogger.error( NLoggerNames.Sharing, exp, exp );
                        Environment.getInstance().fireDisplayUserMessage(
                            UserMessageListener.SharedFilesSaveFailed,
                            new String[]
                            { exp.toString() } );
                        NLogger.error( NLoggerNames.Sharing, exp, exp );
                    }
                }
                catch (IOException exp)
                {
                    NLogger.error( NLoggerNames.Sharing, exp, exp );
                    Environment.getInstance().fireDisplayUserMessage(
                        UserMessageListener.SharedFilesSaveFailed, new String[]
                        { exp.toString() } );
                }
                finally
                {
                    try
                    {
                        rwLock.readUnlock();
                    }
                    catch (IllegalAccessException exp)
                    {
                        NLogger.error( NLoggerNames.Sharing, exp, exp );
                    }
                }
            }
            while ( isFollowUpSaveTriggered );
            NLogger.debug( NLoggerNames.Sharing,
                "Finished saving download list..." );

            synchronized ( saveSharedFilesLock )
            {
                // give created instance free once we are finished..
                saveSharedFilesJob = null;
            }
        }

        private XJBSharedLibrary createXJBSharedLibrary() throws JAXBException
        {
            ObjectFactory objFactory = new ObjectFactory();
            XJBSharedLibrary library = objFactory.createXJBSharedLibrary();
            rwLock.readLock();
            try
            {
                Iterator iterator = sharedFiles.iterator();
                List sharedFileList = library.getSharedFileList();
                while ( iterator.hasNext() )
                {
                    try
                    {
                        ShareFile file = (ShareFile) iterator.next();
                        if ( file.getURN() == null )
                        {
                            continue;
                        }
                        XJBSharedFile xjbFile = file.createXJBSharedFile();
                        sharedFileList.add( xjbFile );
                    }
                    catch (Exception exp)
                    {
                        NLogger.error( NLoggerNames.Sharing,
                            "SharedFile skipped due to error.", exp );
                    }
                }
            }
            finally
            {
                try
                {
                    rwLock.readUnlock();
                }
                catch (IllegalAccessException exp)
                {
                    NLogger.error( NLoggerNames.Sharing, exp, exp );
                }
            }
            return library;
        }
    }

    ///////////////////// END inner classes   ////////////////////////
}
