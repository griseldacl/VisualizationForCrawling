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
 *  $Id: UploadManager.java,v 1.29 2005/11/03 16:33:46 gregork Exp $
 */
package phex.upload;

import java.util.ArrayList;
import java.util.TimerTask;

import phex.common.*;
import phex.common.address.DestAddress;
import phex.event.AsynchronousDispatcher;
import phex.event.UploadFilesChangeListener;
import phex.http.HTTPRequest;
import phex.net.connection.Connection;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

public class UploadManager implements Manager
{
    private IPCounter uploadIPCounter;

    private ArrayList uploadStateList;

    private ArrayList queuedStateList;

    private static UploadManager instance;

    private UploadManager()
    {
        uploadIPCounter = new IPCounter( ServiceManager.sCfg.mMaxUploadPerIP );
        uploadStateList = new ArrayList();
        queuedStateList = new ArrayList();
    }

    public static UploadManager getInstance()
    {
        if ( instance == null )
        {
            instance = new UploadManager();
        }
        return instance;
    }

    /**
     * This method is called in order to initialize the manager. This method
     * includes all tasks that must be done to intialize all the several manager.
     * Like instantiating the singleton instance of the manager. Inside
     * this method you can't rely on the availability of other managers.
     * @return true is initialization was successful, false otherwise.
     */
    public boolean initialize()
    {
        return true;
    }

    /**
     * This method is called in order to perform post initialization of the
     * manager. This method includes all tasks that must be done after initializing
     * all the several managers. Inside this method you can rely on the
     * availability of other managers.
     * @return true is initialization was successful, false otherwise.
     */
    public boolean onPostInitialization()
    {
        Environment.getInstance().scheduleTimerTask(
            new CleanUploadStateTimer(), CleanUploadStateTimer.TIMER_PERIOD,
            CleanUploadStateTimer.TIMER_PERIOD );
        return true;
    }

    /**
     * This method is called after the complete application including GUI completed
     * its startup process. This notification must be used to activate runtime
     * processes that needs to be performed once the application has successfully
     * completed startup.
     */
    public void startupCompletedNotify()
    {
    }

    /**
     * This method is called in order to cleanly shutdown the manager. It
     * should contain all cleanup operations to ensure a nice shutdown of Phex.
     */
    public void shutdown(){}

    public void handleUploadRequest( Connection connection, HTTPRequest httpRequest)
    {
        UploadEngine uploadEngine = new UploadEngine( connection,
            httpRequest );
        uploadEngine.startUpload();
    }

    /**
     * Returns true if all upload slots are filled.
     */
    public boolean isHostBusy()
    {
        if ( getUploadingCount() >= ServiceManager.sCfg.mMaxUpload ) { return true; }
        return false;
    }

    /**
     * Returns true if all queue slots are filled.
     */
    public boolean isQueueLimitReached()
    {
        if ( queuedStateList.size() >= ServiceManager.sCfg.maxUploadQueueSize ) { return true; }
        return false;
    }
    
    public boolean validateAndCountIP(DestAddress hostAddress)
    {
        synchronized (uploadIPCounter)
        {
            // update count...
            uploadIPCounter.setMaxCount( ServiceManager.sCfg.mMaxUploadPerIP );
            return uploadIPCounter.validateAndCountIP( hostAddress );
        }
    }

    public void releaseUploadIP(DestAddress hostAddress)
    {
        synchronized (uploadIPCounter)
        {
            uploadIPCounter.relaseIP( hostAddress );
        }
    }

    ///////////////////// Collection access methods ////////////////////////////

    public void addUploadState(UploadState uploadState)
    {
        synchronized (uploadStateList)
        {
            int position = uploadStateList.size();
            uploadStateList.add( uploadState );
            fireUploadFileAdded( position );
        }
    }
    
    public boolean containsUploadState( UploadState uploadState )
    {
        synchronized (uploadStateList)
        {
            return uploadStateList.contains(uploadState);
        }
    }

    /**
     * Returns the number of all files in the upload list. Also with state
     * completed and aborted.
     */
    public int getUploadListSize()
    {
        synchronized (uploadStateList)
        {
            return uploadStateList.size();
        }
    }

    /**
     * Returns only the number of files that are currently getting uploaded.
     * TODO it's better to maintain the number of files in an attribute...
     */
    public int getUploadingCount()
    {
        int count = 0;
        synchronized (uploadStateList)
        {
            for (int i = uploadStateList.size() - 1; i >= 0; i--)
            {
                UploadState state = (UploadState) uploadStateList.get( i );
                if ( state.getStatus() == UploadConstants.STATUS_UPLOADING
                    || state.getStatus() == UploadConstants.STATUS_INITIALIZING )
                {
                    count++;
                }
            }
        }
        return count;
    }

    public UploadState getUploadStateAt(int index)
    {
        synchronized (uploadStateList)
        {
            if ( index < 0 || index >= uploadStateList.size() ) { return null; }
            return (UploadState) uploadStateList.get( index );
        }
    }

    public UploadState[] getUploadStatesAt(int[] indices)
    {
        synchronized (uploadStateList)
        {
            int length = indices.length;
            UploadState[] states = new UploadState[length];
            int listSize = uploadStateList.size();
            for (int i = 0; i < length; i++)
            {
                if ( indices[i] < 0 || indices[i] >= listSize )
                {
                    states[i] = null;
                }
                else
                {
                    states[i] = (UploadState) uploadStateList.get( indices[i] );
                }
            }
            return states;
        }
    }

    public void removeUploadState(UploadState state)
    {
        state.stopUpload();
        synchronized (uploadStateList)
        {
            int idx = uploadStateList.indexOf( state );
            if ( idx != -1 )
            {
                uploadStateList.remove( idx );
                fireUploadFileRemoved( idx );
            }
        }

        synchronized (queuedStateList)
        {
            int idx = queuedStateList.indexOf( state );
            if ( idx != -1 )
            {
                queuedStateList.remove( idx );
                //fireQueuedFileRemoved( idx );
            }
        }
    }

    /**
     * Removes uploads that not have the status STATUS_UPLOADING or
     * STATUS_INITIALIZING.
     */
    public void cleanUploadStateList()
    {
        synchronized (uploadStateList)
        {
            for (int i = uploadStateList.size() - 1; i >= 0; i--)
            {
                UploadState state = (UploadState) uploadStateList.get( i );
                short status = state.getStatus();
                if ( status == UploadConstants.STATUS_UPLOADING
                    || status == UploadConstants.STATUS_INITIALIZING )
                {
                    continue;
                }
                uploadStateList.remove( i );
                fireUploadFileRemoved( i );
            }
        }
    }

    private class CleanUploadStateTimer extends TimerTask
    {
        private static final long TIMER_PERIOD = 1000 * 10;

        public void run()
        {
            try
            {
                if ( ServiceManager.sCfg.mUploadAutoRemoveCompleted )
                {
                    cleanUploadStateList();
                }
            }
            catch ( Throwable th )
            {
                NLogger.error( NLoggerNames.UPLOAD, th, th );
            }
        }
    }

    ////////////////////// queue collection methods ////////////////////////////

    public int addQueuedUpload(UploadState uploadState)
    {
        int position;
        synchronized (queuedStateList)
        {
            position = queuedStateList.size();
            queuedStateList.add( uploadState );
        }
        fireUploadQueueChanged();
        //dumpQueueInfo();
        return position;
    }

    public void removeQueuedUpload(UploadState uploadState)
    {
        int position;
        synchronized (queuedStateList)
        {
            position = queuedStateList.indexOf( uploadState );
            if ( position != -1 )
            {
                queuedStateList.remove( position );
                fireUploadQueueChanged();
            }
        }
        //dumpQueueInfo();
    }

    public int getQueuedPosition(UploadState state)
    {
        synchronized (queuedStateList)
        {
            return queuedStateList.indexOf( state );
        }
    }

    /**
     * Returns the number of all files in the upload queue list.
     */
    public int getUploadQueueSize()
    {
        synchronized (queuedStateList)
        {
            return queuedStateList.size();
        }
    }

    /*public void dumpQueueInfo()
     {
     System.out.println( "---------------------------------" );
     synchronized( queuedStateList )
     {
     Iterator iterator = queuedStateList.iterator();
     while( iterator.hasNext() )
     {
     Object obj = iterator.next();
     System.out.println( obj );
     }
     }
     System.out.println( "---------------------------------" );
     }*/

    ///////////////////// START event handling methods /////////////////////////
    /**
     * All listeners interested in events.
     */
    private ArrayList listenerList = new ArrayList( 2 );

    public void addUploadFilesChangeListener(UploadFilesChangeListener listener)
    {
        listenerList.add( listener );
    }

    public void removeUploadFilesChangeListener(
        UploadFilesChangeListener listener)
    {
        listenerList.remove( listener );
    }

    private void fireUploadFileChanged(final int position)
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater( new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                UploadFilesChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for (int i = listeners.length - 1; i >= 0; i--)
                {
                    listener = (UploadFilesChangeListener) listeners[i];
                    listener.uploadFileChanged( position );
                }
            }
        } );
    }

    private void fireUploadFileAdded(final int position)
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater( new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                UploadFilesChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for (int i = listeners.length - 1; i >= 0; i--)
                {
                    listener = (UploadFilesChangeListener) listeners[i];
                    listener.uploadFileAdded( position );
                }
            }
        } );
    }

    private void fireUploadQueueChanged()
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater( new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                UploadFilesChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for (int i = listeners.length - 1; i >= 0; i--)
                {
                    listener = (UploadFilesChangeListener) listeners[i];
                    listener.uploadQueueChanged();
                }
            }
        } );
    }

    private void fireUploadFileRemoved(final int position)
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater( new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                UploadFilesChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for (int i = listeners.length - 1; i >= 0; i--)
                {
                    listener = (UploadFilesChangeListener) listeners[i];
                    listener.uploadFileRemoved( position );
                }
            }
        } );
    }

    public void fireUploadFileChanged(UploadState file)
    {
        synchronized (uploadStateList)
        {
            int position = uploadStateList.indexOf( file );
            if ( position >= 0 )
            {
                fireUploadFileChanged( position );
            }
        }
    }
    ///////////////////// END event handling methods ////////////////////////
}