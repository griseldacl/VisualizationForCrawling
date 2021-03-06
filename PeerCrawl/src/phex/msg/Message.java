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
 *  $Id: Message.java,v 1.11 2005/11/03 17:06:26 gregork Exp $
 */
package phex.msg;

import java.io.*;

import phex.utils.GnutellaOutputStream;


/**
 * A Gnutella network message.
 */
public abstract class Message
{
    private long creationTime;
    private MsgHeader header;

    protected Message( MsgHeader header )
    {
        this.header = header;
        creationTime = System.currentTimeMillis();
    }

    /**
     * Returns this message's header.
     *
     * @return the MsgHeader associated with this message
     */    
    public MsgHeader getHeader()
    {
        return header;
    }

    public long getCreationTime( )
    {
        return creationTime;
    }

    /**
     * This is a dirty workaround for the static myMsgInit of MsgManager
     */
    public void setCreationTime( long time )
    {
        creationTime = time;
    }

    /**
     * Returns a debug representation of the message. Here only available as
     * toString(). This should be overloaded and implemented correctly by
     * subclasses.
     * @return a debug representation of the message.
     */
    public String toDebugString()
    {
        return toString();
    }

    public abstract void writeMessage( GnutellaOutputStream outStream )
        throws IOException;
}


