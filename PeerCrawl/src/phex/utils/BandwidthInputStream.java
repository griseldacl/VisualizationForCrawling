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
 *  $Id: BandwidthInputStream.java,v 1.6 2005/10/03 00:18:29 gregork Exp $
 */
package phex.utils;

import java.io.IOException;
import java.io.InputStream;

import phex.common.bandwidth.BandwidthController;

/**
 *
 */
public class BandwidthInputStream extends InputStream
{
    private InputStream inStream;

    private BandwidthController bandwidthController;

    public BandwidthInputStream(InputStream aInputStream,
        BandwidthController aBandwidthController)
    {
        inStream = aInputStream;
        bandwidthController = aBandwidthController;
    }
    
    /**
     * @param bandwidthController The bandwidthController to set.
     */
    public void setBandwidthController(BandwidthController bandwidthController)
    {
        this.bandwidthController = bandwidthController;
    }

    public int read() throws IOException
    {
        bandwidthController.getAvailableByteCount( true );
        int val = inStream.read();
        bandwidthController.markBytesUsed( 1 );
        return val;
    }

    public int read(byte b[]) throws IOException
    {
        return this.read(b, 0, b.length);
    }
    
    public int read(byte b[], int off, int len) throws IOException
    {
        
        int available = bandwidthController.getAvailableByteCount( len, true );
        try
        {
            int readLen = inStream.read(b, off, available );
            bandwidthController.markBytesUsed( readLen );
            return readLen;
        }
        catch ( ArrayIndexOutOfBoundsException exp )
        {
            // for debug the exeption reason:
            NLogger.error(NLoggerNames.BANDWIDTH, exp, exp);
            NLogger.error(NLoggerNames.BANDWIDTH, "buffer:" + b.length + " off " 
                + off + " len " + len + " avail " + available );
            
            // nothing was read...
            return 0;
        }
    }

    public int available() throws IOException
    {
        return inStream.available();
    }

    public void close() throws IOException
    {
        inStream.close();
    }
}