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
 *  $Id: VendorMessageConstants.java,v 1.10 2005/10/08 14:24:19 gregork Exp $
 */
package phex.msg.vendor;

/**
 * 
 */
public interface VendorMessageConstants
{
    public static final int SUBSELECTOR_MESSAGES_SUPPORTED = 0;
    
    // VENDOR BEAR
    public static final int SUBSELECTOR_HOPS_FLOW = 4;
    public static final int SUBSELECTOR_HORIZON_PING = 5;
    public static final int SUBSELECTOR_TCP_CONNECT_BACK = 7;
    
    //  VENDOR LIME
    public static final int SUBSELECTOR_CAPABILITIES = 10;
    public static final int SUBSELECTOR_PUSH_PROXY_REQUEST = 21;
    public static final int SUBSELECTOR_PUSH_PROXY_ACKNOWLEDGEMENT = 22;
    
    public static final byte[] VENDORID_NULL = { 0, 0, 0, 0 };
    public static final byte[] VENDORID_BEAR = { 'B', 'E', 'A', 'R' };
    public static final byte[] VENDORID_LIME = { 'L', 'I', 'M', 'E' };
}
