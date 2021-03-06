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
 *  $Id: HostConstants.java,v 1.1 2005/11/13 10:07:08 gregork Exp $
 */
package phex.host;

public interface HostConstants
{

    public static final int STATUS_HOST_NOT_CONNECTED = 0;
    public static final int STATUS_HOST_ERROR = 1;
    public static final int STATUS_HOST_CONNECTING = 2;
    public static final int STATUS_HOST_ACCEPTING = 3;
    public static final int STATUS_HOST_CONNECTED = 4;
    public static final int STATUS_HOST_DISCONNECTED = 5;

}
