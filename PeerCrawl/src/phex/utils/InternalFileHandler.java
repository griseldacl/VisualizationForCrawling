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
 *  $Id: InternalFileHandler.java,v 1.3 2005/10/21 16:55:57 gregork Exp $
 */
package phex.utils;

import java.io.*;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.httpclient.URI;

import phex.common.URN;
import phex.download.MagnetData;
import phex.download.swarming.SwarmingManager;


/**
 * Offers internal handling for files like magma-lists, rss-feeds, podcasts and similar. 
 */
public final class InternalFileHandler
{
    public static void magmaReadout( File file )
    {
    	try
        {
            BufferedInputStream inStream = new BufferedInputStream( new FileInputStream( file ) );
            MagmaParser parser = new MagmaParser( inStream );
            parser.start();

            List magnetList = parser.getMagnets();
            Iterator iter = magnetList.iterator();
            while (iter.hasNext())
            {
                String magnet = (String) iter.next();
                URI uri = new URI( magnet, true );
                // dont add already downloading urns.
                MagnetData magnetData = MagnetData.parseFromURI(uri);
                URN urn = MagnetData.lookupSHA1URN(magnetData);
                SwarmingManager swarmingMgr = SwarmingManager.getInstance();
                if ( !swarmingMgr.isURNDownloaded( urn ) )
                {
                    swarmingMgr.addFileToDownload( uri );
                }
            }
/*            String uuri = parser.getUpdateURI(); 
            if ( uuri != null) 
            { 
            	URI uri = new URI( uuri, true ); 
            	sheduledReadout(uri, 60000); 
            }
            
            */
        
        }
        catch (IOException exp)
        {
            NLogger.warn(NLoggerNames.MAGMA, exp.getMessage(), exp);
        }
    }
    
    public static void sheduledReadout( URI uri, long time )
    {
    	try 
    	{
    		// dont add already downloading urns.
            MagnetData magnetData = MagnetData.parseFromURI(uri);
            URN urn = MagnetData.lookupSHA1URN(magnetData);
            SwarmingManager swarmingMgr = SwarmingManager.getInstance();
            if ( !swarmingMgr.isURNDownloaded( urn ) )
            {
                swarmingMgr.addFileToDownload( uri );
            }
    	}
        catch (IOException exp)
        {
            NLogger.warn(NLoggerNames.MAGMA, exp.getMessage(), exp);
        }
    }

}
