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
 *  $Id: FileAdministration.java,v 1.43 2005/10/03 00:18:28 gregork Exp $
 */
package phex.share;


/**
 * A administrator of the shared files. It provides all necessary accessor
 * methods to administrate the shared files.
 */
public class FileAdministration
{

    

    ///////////////////// START event handling methods ////////////////////////

//    private void fireSharedFileChanged( final int position )
//    {
//        // invoke update in event dispatcher
//        AsynchronousDispatcher.invokeLater(
//        new Runnable()
//        {
//            public void run()
//            {
//                Object[] listeners = listenerList.toArray();
//                SharedFilesChangeListener listener;
//                // Process the listeners last to first, notifying
//                // those that are interested in this event
//                for ( int i = listeners.length - 1; i >= 0; i-- )
//                {
//                    listener = (SharedFilesChangeListener)listeners[ i ];
//                    listener.sharedFileChanged( position );
//                }
//            }
//        });
//    }
//
//    private void fireSharedFileAdded( final int position )
//    {
//        // invoke update in event dispatcher
//        AsynchronousDispatcher.invokeLater(
//        new Runnable()
//        {
//            public void run()
//            {
//                Object[] listeners = listenerList.toArray();
//                SharedFilesChangeListener listener;
//                // Process the listeners last to first, notifying
//                // those that are interested in this event
//                for ( int i = listeners.length - 1; i >= 0; i-- )
//                {
//                    listener = (SharedFilesChangeListener)listeners[ i ];
//                    listener.sharedFileAdded( position );
//                }
//            }
//        });
//    }
//
//    private void fireSharedFileRemoved( final int position )
//    {
//        // invoke update in event dispatcher
//        AsynchronousDispatcher.invokeLater(
//        new Runnable()
//        {
//            public void run()
//            {
//                Object[] listeners = listenerList.toArray();
//                SharedFilesChangeListener listener;
//                // Process the listeners last to first, notifying
//                // those that are interested in this event
//                for ( int i = listeners.length - 1; i >= 0; i-- )
//                {
//                    listener = (SharedFilesChangeListener)listeners[ i ];
//                    listener.sharedFileRemoved( position );
//                }
//            }
//        });
//    }
//
//    private void fireAllSharedFilesChanged( )
//    {
//        // invoke update in event dispatcher
//        AsynchronousDispatcher.invokeLater(
//        new Runnable()
//        {
//            public void run()
//            {
//                Object[] listeners = listenerList.toArray();
//                SharedFilesChangeListener listener;
//                // Process the listeners last to first, notifying
//                // those that are interested in this event
//                for ( int i = listeners.length - 1; i >= 0; i-- )
//                {
//                    listener = (SharedFilesChangeListener)listeners[ i ];
//                    listener.allSharedFilesChanged( );
//                }
//            }
//        });
//    }
//
//    public void fireSharedFileChanged( ShareFile file )
//    {
//        /*int position = sharedFiles.indexOf( file );
//        if ( position >= 0 )
//        {
//            fireSharedFileChanged( position );
//        }*/
//    }
    ///////////////////// END event handling methods ////////////////////////
}