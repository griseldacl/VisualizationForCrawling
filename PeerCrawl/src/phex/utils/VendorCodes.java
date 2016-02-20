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
 *  $Id: VendorCodes.java,v 1.4 2005/10/03 00:18:29 gregork Exp $
 */
package phex.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

/**
 *
 */
public class VendorCodes
{
    public static String UNKNOWN = "?";
    private static HashMap vendorNames;
    
    public static String getVendorName( String vendorCode )
    {
        if ( vendorNames == null )
        {
            initVendorNames();
        }
        String name = (String) vendorNames.get( vendorCode );
        if ( name == null )
        {
            return vendorCode;
        }
        return name;
    }
    
    private static synchronized void initVendorNames()
    {
        vendorNames = new HashMap();
        synchronized( vendorNames )
        {
            InputStream stream = Localizer.class.getResourceAsStream( "/phex/resources/VendorCodes.properties" );
            if ( stream == null ) { return; }
            // make sure it is buffered
            stream = new BufferedInputStream( stream );
            Properties props = new Properties();
            try
            {
                props.load( stream );
                vendorNames.putAll( props );
            }
            catch (IOException exp)
            {
            }
            finally
            {
                IOUtil.closeQuietly( stream );
            }
            return;
        }
    }
}
