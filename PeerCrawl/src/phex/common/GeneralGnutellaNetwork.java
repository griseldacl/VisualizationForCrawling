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
 *  $Id: GeneralGnutellaNetwork.java,v 1.4 2005/10/03 00:18:22 gregork Exp $
 */
package phex.common;

import java.io.File;

import phex.connection.ConnectionConstants;

/**
 * The representation of the general Gnutella network.
 */
public class GeneralGnutellaNetwork extends GnutellaNetwork
{    
    public String getName()
    {
        return Cfg.GENERAL_GNUTELLA_NETWORK;
    }
    
    /**
     * @see phex.common.GnutellaNetwork#getHostsFile()
     */
    public File getHostsFile()
    {
        return Environment.getInstance().getPhexConfigFile(
            EnvironmentConstants.HOSTS_FILE_NAME );
    }
    
    /**
     * @see phex.common.GnutellaNetwork#getBookmarkedHostsFile()
     */
    public File getFavoritesFile()
    {
        return Environment.getInstance().getPhexConfigFile(
            EnvironmentConstants.XML_FAVORITES_FILE_NAME );
    }

    /**
     * @see phex.common.GnutellaNetwork#getGWebCacheFile()
     */
    public File getGWebCacheFile()
    {
        return Environment.getInstance().getPhexConfigFile(
            EnvironmentConstants.G_WEB_CACHE_FILE_NAME );
    }
    
    /**
     * @see phex.common.GnutellaNetwork#getUdpHostCacheFile()
     */
    public File getUdpHostCacheFile()
    {
        return Environment.getInstance().getPhexConfigFile(
            EnvironmentConstants.UDP_HOST_CACHE_FILE_NAME );
    }
    
    public String getNetworkGreeting()
    {
        return ConnectionConstants.GNUTELLA_CONNECT;
    }
}