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
 *  $Id: SubscriptionDownloader.java,v 1.1 2005/11/12 01:07:14 arnebab Exp $
 */
package phex.utils;

import java.util.*;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

import phex.common.Cfg; 
import phex.common.URN;
import phex.common.ServiceManager;
import phex.connection.NetworkManager;
import phex.download.MagnetData;
import phex.download.swarming.SwarmingManager;

 
public class SubscriptionDownloader
{	
	public static void run() throws URIException
	{
		Cfg cfg = ServiceManager.sCfg;
    	ArrayList subscriptionMagnets; 
    	String uriStr; 
    	subscriptionMagnets = new ArrayList( cfg.subscriptionMagnets ); 
    	if ( subscriptionMagnets == null && cfg.default_subscriptionMagnets != null )
    	{
    		subscriptionMagnets.add( cfg.default_subscriptionMagnets ); 
    	}
    	
    	if ( subscriptionMagnets != null )
    	{
    		Iterator iterator = subscriptionMagnets.iterator();
    		while (iterator.hasNext())
        	{
            	uriStr = (String) iterator.next();
            	// createDownload(uriStr); 
            	NetworkManager.getInstance().fireIncomingUriDownload(uriStr);
        	}
        }
	}
	
	private static void createDownload(String uriStr) throws URIException
    {
    	
        if (uriStr.length() == 0)
        {
            return;
        }
        URI uri = new URI( uriStr, true );
        
        // dont add already downloading urns.
        MagnetData magnetData = MagnetData.parseFromURI(uri);
        URN urn = MagnetData.lookupSHA1URN(magnetData);
        SwarmingManager swarmingMgr = SwarmingManager.getInstance();
        if ( !swarmingMgr.isURNDownloaded( urn ) )
        {
            swarmingMgr.addFileToDownload( uri );
        }
    }
}