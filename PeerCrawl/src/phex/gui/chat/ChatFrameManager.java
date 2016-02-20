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
package phex.gui.chat;

import java.util.*;


import phex.chat.*;
import phex.event.*;
import phex.utils.*;

public class ChatFrameManager
    implements ChatListener
{
    private HashMap openChatsMap;

    public ChatFrameManager()
    {
        // give room for 3 chat slots
        openChatsMap = new HashMap( 4 );
        ChatManager.getInstance().addChatListener( this );
    }

    public void chatConnectionOpened( ChatEngine chatEngine )
    {
        ChatFrame frame = new ChatFrame( chatEngine );
        frame.setVisible( true );
        openChatsMap.put( chatEngine, frame );
    }

    public void chatMessageReceived( ChatEngine chatEngine, String chatMessage )
    {
        ChatFrame frame = (ChatFrame)openChatsMap.get( chatEngine );
        frame.addChatMessage( chatMessage );
    }

    public void chatConnectionFailed( ChatEngine chatEngine )
    {
        ChatFrame frame = (ChatFrame) ( openChatsMap.remove( chatEngine ) );

        if ( frame != null )
        {
            Object[] args =
            {
                chatEngine.getHostAddress().getHostName()
            };
            frame.addInfoMessage( Localizer.getFormatedString( "ChatConnectionClosed",
                args) );
        }
    }
}