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
 *  Created on 12.11.2005
 *  --- CVS Information ---
 *  $Id: HostInfo.java,v 1.1 2005/11/13 10:07:08 gregork Exp $
 */
package phex.host;

import phex.utils.Localizer;

public class HostInfo implements HostConstants
{
    /**
     * Returns a localized string of the status for the given host.
     */
    public static String getHostStatusString( Host host )
    {
        int status = host.getStatus();
        String lastStatusMsg = host.getLastStatusMsg();
        switch ( status )
        {
        case STATUS_HOST_NOT_CONNECTED:
            return Localizer.getString( "NetHostStatus_NotConnected" );

        case STATUS_HOST_ERROR:
        {
            Object[] args =
            { lastStatusMsg };
            return Localizer.getFormatedString( "NetHostStatus_Error", args );
        }
        case STATUS_HOST_CONNECTING:
        {
            Object[] args = { lastStatusMsg != null ? lastStatusMsg : "" };
            return Localizer.getFormatedString( "NetHostStatus_Connecting",
                args );
        }
        case STATUS_HOST_ACCEPTING:
        {
            Object[] args =
            { lastStatusMsg };
            return Localizer
                .getFormatedString( "NetHostStatus_Accepting", args );
        }
        case STATUS_HOST_CONNECTED:
        {
            Object[] args = 
            { host.isSendQueueInRed() ? new Integer( 1 ) : new Integer( 0 ) };
            return Localizer.getFormatedString( "NetHostStatus_Connected", args );
        }
        case STATUS_HOST_DISCONNECTED:
            return Localizer.getString( "NetHostStatus_Disconnected" );
        }

        return Localizer.getString( "NetHostStatus_Unknown" );
    }
}