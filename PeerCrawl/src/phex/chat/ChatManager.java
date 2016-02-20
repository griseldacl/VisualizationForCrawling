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
 */
package phex.chat;

import java.io.IOException;
import java.util.ArrayList;

import phex.common.Manager;
import phex.common.ServiceManager;
import phex.common.address.DestAddress;
import phex.event.AsynchronousDispatcher;
import phex.event.ChatListener;
import phex.net.presentation.SocketFacade;
import phex.utils.GnutellaInputStream;
import phex.utils.Logger;

public class ChatManager implements Manager
{
    private static ChatManager instance;

    private ChatManager()
    {
    }

    public static ChatManager getInstance()
    {
        if ( instance == null )
        {
            instance = new ChatManager();
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

    /**
     * Opens a new chat connection to start a instant message chat.
     */
    public void openChat( DestAddress hostAddress )
    {
        // initialize the chat engine that reads and send the chat data
        // for a new HostAddress.
        ChatEngine chatEngine = new ChatEngine( hostAddress );
        chatEngine.startChat();
        fireChatConnectionOpened( chatEngine );
    }

    /**
     * Accepts a connection to start a instant message chat.
     */
    public void acceptChat( SocketFacade socket, GnutellaInputStream gInStream,
        DestAddress hostAddress )
    {
        if ( !ServiceManager.sCfg.isChatEnabled )
        {
            try
            {
                socket.close();
            }
            catch ( IOException exp )
            {// ignore
            }
            return;
        }

        // initialize the chat engine that reads and send the chat data
        // over the connected socket.
        try
        {
            ChatEngine chatEngine = new ChatEngine( socket, gInStream,
                hostAddress );
            chatEngine.startChat();
            fireChatConnectionOpened( chatEngine );
        }
        catch ( IOException exp )
        {
            Logger.logMessage( Logger.FINE, Logger.NETWORK, exp );
            try
            {
                socket.close();
            }
            catch ( IOException exp2 )
            {// ignore
            }
            return;
        }
    }

    ///////////////////// START event handling methods ////////////////////////

    /**
     * The listeners interested in events.
     */
    private ArrayList listenerList = new ArrayList( 1 );

    public void addChatListener( ChatListener listener )
    {
        listenerList.add( listener );
    }

    public void removeChatListener( ChatListener listener )
    {
        listenerList.remove( listener );
    }

    /**
     * Fires if a new chat connection was opened.
     */
    public void fireChatConnectionOpened( final ChatEngine chatEngine )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                ChatListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (ChatListener)listeners[ i ];
                    listener.chatConnectionOpened( chatEngine );
                }
            }
        });
    }

    /**
     * Fires a event if a chat connection was failed to opened or a opened chat
     * connection was closed.
     */
    public void fireChatConnectionFailed( final ChatEngine chatEngine )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                ChatListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (ChatListener)listeners[ i ];
                    listener.chatConnectionFailed( chatEngine );
                }
            }
        });
    }

    /**
     * Fires if a message for a openend chat connection was received.
     */
    public void fireChatMessageReceived( final ChatEngine chatEngine,
        final String chatMessage)
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                ChatListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (ChatListener)listeners[ i ];
                    listener.chatMessageReceived( chatEngine, chatMessage );
                }
            }
        });
    }
    ///////////////////// END event handling methods ////////////////////////
}