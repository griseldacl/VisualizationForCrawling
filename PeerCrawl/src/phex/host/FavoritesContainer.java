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
 *  $Id: FavoritesContainer.java,v 1.14 2005/11/03 23:30:14 gregork Exp $
 */
package phex.host;

import java.io.File;
import java.util.*;

import javax.xml.bind.JAXBException;

import phex.common.Environment;
import phex.common.ThreadPool;
import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.common.file.FileManager;
import phex.common.file.ManagedFile;
import phex.common.file.ManagedFileException;
import phex.connection.NetworkManager;
import phex.event.AsynchronousDispatcher;
import phex.event.BookmarkedHostsChangeListener;
import phex.event.UserMessageListener;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;
import phex.utils.VersionUtils;
import phex.xml.*;

/**
 * Holds user bookmarked hosts.
 */
public class FavoritesContainer
{
    /**
     * All listeners interested in events.
     */
    private ArrayList listenerList = new ArrayList( 2 );
    
    private ArrayList favoritesList;
    private boolean hasChangedSinceLastSave;
    
    public FavoritesContainer()
    {
        favoritesList = new ArrayList();
        hasChangedSinceLastSave = false;
        Environment.getInstance().scheduleTimerTask(
            new SaveFavoritesTimer(), SaveFavoritesTimer.TIMER_PERIOD,
            SaveFavoritesTimer.TIMER_PERIOD );
    }
    
    /**
     *
     */
    public void initializeFavorites()
    {
        favoritesList.clear();
        loadFromFile();
    }
    
    /**
     * @param addresses
     */
    public synchronized void addFavorites(DestAddress[] addresses)
    {
        for (int i = 0; i < addresses.length; i++)
        {
            FavoriteHost host = new FavoriteHost( addresses[i] );
            insertBookmarkedHost( host, favoritesList.size() );            
        }
    }
    
    /**
     * @param addresses
     */
    public synchronized void addFavorite(DestAddress address)
    {
        FavoriteHost host = new FavoriteHost( address );
        insertBookmarkedHost( host, favoritesList.size() );            
    }
        
    /**
     * Loads the hosts file phex.hosts.
     */
    private void loadFromFile()
    {
        NLogger.debug( NLoggerNames.Favorites, "Loading favorites file." );
        
        NetworkManager networkMgr = NetworkManager.getInstance();
        File favoritesFile = networkMgr.getGnutellaNetwork().getFavoritesFile();

        XJBPhex phex;
        try
        {
            if ( !favoritesFile.exists() )
            {
                return;
            }
            FileManager fileMgr = FileManager.getInstance();
            ManagedFile managedFile = fileMgr.getReadWriteManagedFile( favoritesFile );
            phex = XMLBuilder.loadXJBPhexFromFile( managedFile );
            if ( phex == null )
            {
                NLogger.debug( NLoggerNames.Favorites,
                    "No bookmarked hosts file found." );
                return;
            }
            XJBFavoritesList hostList = phex.getFavoritesList();
            if ( hostList == null )
            {
                NLogger.warn( NLoggerNames.Favorites,
                    "No XJBBookmarkedHostList found." );
            }
            
            Iterator iterator = hostList.getFavoritesList().iterator();
            while ( iterator.hasNext() )
            {
                XJBFavoriteHost xjbHost = (XJBFavoriteHost)iterator.next();
                int port = xjbHost.getPort();
                
                DestAddress address;
                String hostName = xjbHost.getHostName();
                if ( hostName != null )
                {
                    address = new DefaultDestAddress( hostName, port );
                }
                else
                {
                    byte[] ip = xjbHost.getIp();
                    address = new DefaultDestAddress( ip, port );
                }
                
                FavoriteHost bookmarkedHost = new FavoriteHost( address );
                
                // TODO2 no security checking is done here, assuming the user
                // always wants to have the bookmarked hosts even if faulty
                // but this concept is week... security is needed.
                insertBookmarkedHost( bookmarkedHost, favoritesList.size() );
            }
        }
        catch ( JAXBException exp )
        {
            Throwable linkedException = exp.getLinkedException();
            if ( linkedException != null )
            {
                NLogger.error( NLoggerNames.Favorites, linkedException, linkedException );
            }
            NLogger.error( NLoggerNames.Favorites, exp, exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.FavoritesSettingsLoadFailed, 
                new String[]{ exp.toString() } );
            return;
        }
        catch ( ManagedFileException exp )
        {
            NLogger.error( NLoggerNames.Favorites, exp, exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.FavoritesSettingsLoadFailed, 
                new String[]{ exp.toString() } );
            return;
        }
    }    
    
    /**
     * Blocking operation which saves the bookmarked hosts if they changed since
     * the last save operation.
     */
    public synchronized void saveFavoriteHosts( )
    {
        if ( !hasChangedSinceLastSave )
        {
            return;
        }
        
        try
        {
            ObjectFactory objFactory = new ObjectFactory();
            XJBPhex phex = objFactory.createPhexElement();
            phex.setPhexVersion( VersionUtils.getFullProgramVersion() );
            
            XJBFavoritesList xjbList = objFactory.createXJBFavoritesList();
            phex.setFavoritesList( xjbList );
            
            List list = xjbList.getFavoritesList();
            Iterator iterator = favoritesList.iterator();
            while( iterator.hasNext() )
            {
                FavoriteHost host = (FavoriteHost)iterator.next();
                XJBFavoriteHost xjbHost = objFactory.createXJBFavoriteHost();
                DestAddress address = host.getHostAddress();
                if ( address.isIpHostName() )
                {
                    xjbHost.setIp( address.getIpAddress().getHostIP() );
                }
                else
                {
                    xjbHost.setHostName( address.getHostName() );
                }
                xjbHost.setPort( address.getPort() );
                
                list.add( xjbHost );
            }
            
            NetworkManager networkMgr = NetworkManager.getInstance();
            File bookmarkFile =  networkMgr.getGnutellaNetwork().getFavoritesFile();
            ManagedFile managedFile = FileManager.getInstance().getReadWriteManagedFile( bookmarkFile );
            
            XMLBuilder.saveToFile( managedFile, phex );
            hasChangedSinceLastSave = false;
        }
        catch ( JAXBException exp )
        {
            // TODO during close this message is never displayed since application
            // will exit too fast. A solution to delay exit process in case 
            // SlideInWindows are open needs to be found.
            NLogger.error( NLoggerNames.Favorites, exp, exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.FavoritesSettingsSaveFailed, 
                new String[]{ exp.toString() } );
        }
        catch ( ManagedFileException exp )
        {
            // TODO during close this message is never displayed since application
            // will exit too fast. A solution to delay exit process in case 
            // SlideInWindows are open needs to be found.
            NLogger.error( NLoggerNames.Favorites, exp, exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.FavoritesSettingsSaveFailed, 
                new String[]{ exp.toString() } );
        }
    }
    
    public synchronized int getBookmarkedHostsCount()
    {
        return favoritesList.size();
    }

    public synchronized FavoriteHost getBookmarkedHostAt( int index )
    {
        if ( index >= favoritesList.size() )
        {
            return null;
        }
        return (FavoriteHost)favoritesList.get( index );
    }

    /**
     * inserts a auto connect host and fires the add event..
     */
    private synchronized void insertBookmarkedHost( FavoriteHost host, int position )
    {
        // if host is not already in the list
        if ( !favoritesList.contains( host ) )
        {
            favoritesList.add( position, host );
            hasChangedSinceLastSave = true;
            fireBookmarkedHostAdded( position );
        }
    }

    public synchronized void removeBookmarkedHost( FavoriteHost host )
    {
        int position = favoritesList.indexOf( host );
        if ( position >= 0 )
        {
            favoritesList.remove( position );
            fireBookmarkedHostRemoved( position );
            hasChangedSinceLastSave = true;
        }
    }

    ////////////////////////END Auto connect host methods //////////////////////
    
    
    ///////////////////// START event handling methods ////////////////////////
    public void addBookmarkedHostsChangeListener( BookmarkedHostsChangeListener listener )
    {
        listenerList.add( listener );
    }

    public void removeBookmarkedHostsChangeListener( BookmarkedHostsChangeListener listener )
    {
        listenerList.remove( listener );
    }


    private void fireBookmarkedHostAdded( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                BookmarkedHostsChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (BookmarkedHostsChangeListener)listeners[ i ];
                    listener.bookmarkedHostAdded( position );
                }
            }
        });
    }

    private void fireBookmarkedHostRemoved( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                BookmarkedHostsChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (BookmarkedHostsChangeListener)listeners[ i ];
                    listener.bookmarkedHostRemoved( position );
                }
            }
        });
    }

    ////////////////////// END event handling methods //////////////////////////
    
    ////////////////////// START inner classes //////////////////////////
    
    private class SaveFavoritesRunner implements Runnable
    {
        public void run()
        {
            saveFavoriteHosts();
        }
    }
    
    private class SaveFavoritesTimer extends TimerTask
    {
        // once per minute
        public static final long TIMER_PERIOD = 1000 * 60;
        
        public void run()
        {
            try
            {
                // trigger the save inside a background job
                ThreadPool.getInstance().addJob( new SaveFavoritesRunner(),
                    "SaveBookmarkedHosts" );
            }
            catch ( Throwable th )
            {
                NLogger.error( NLoggerNames.Favorites, th, th );
            }
        }
    }

}