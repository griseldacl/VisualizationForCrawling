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
 *  $Id: UnknownMsg.java,v 1.1 2005/11/03 17:06:26 gregork Exp $
 */
package phex.msg;

import java.io.*;


import phex.utils.*;

/**
 * <p>Messages of unknown type.</p>
 *
 * <p>This would appear to destroy the function property of the header if not
 * careful. I may be missing something, though.</p>
 */
public class UnknownMsg extends Message
{
    private byte[]		mBody;

    public UnknownMsg( MsgHeader header, byte[] payload )
    {
        super( header );
        mBody = payload;
        getHeader().setDataLength( mBody.length );
    }

    public byte[] getBody()
    {
        return mBody;
    }
    
    public void writeMessage( GnutellaOutputStream outStream )
        throws IOException
    {
        getHeader().writeHeader( outStream );
        outStream.write( mBody, 0, mBody.length );
    }
}

