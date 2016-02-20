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
 *  $Id: ExpiryDate.java,v 1.7 2005/10/03 00:18:22 gregork Exp $
 */
package phex.common;

import java.util.*;

/**
 * A expiry date is used to hold a timestamp of an expiry or the indication that
 * it never expires or expires at the end of the session.
 *
 * <p>@author Gregor Koukkoullis</p>
 */
public class ExpiryDate extends Date
{
    public static final long EXPIRES_NEVER = Long.MAX_VALUE;
    public static final long EXPIRES_END_OF_SESSION = -1;

    public static final ExpiryDate NEVER_EXPIRY_DATE = new ExpiryDate( EXPIRES_NEVER );
    public static final ExpiryDate SESSION_EXPIRY_DATE = new ExpiryDate( EXPIRES_END_OF_SESSION );

    /**
     * @param expiryDate The date in millis after which this rule expires, use EXPIRES_NEVER
     * (Long.MAX_VALUE) for indefinite (never), or EXPIRES_END_OF_SESSION (-1)
     * for end of session.
     */
    public ExpiryDate( long expiryDate )
    {
        super( expiryDate );
        /*
        TODO3 would be nice if we could integrate this check...
        but it would keep us from using user entered values that match...
        if ( expiryDate == EXPIRES_END_OF_SESSION ||
             expiryDate == EXPIRES_NEVER )
        {
            throw new IllegalArgumentException(
                "Timestamp conflicts with never or end of session indicator." );
        }*/
    }

    public boolean isExpiringEndOfSession()
    {
        return getTime() == EXPIRES_END_OF_SESSION;
    }

    public boolean isExpiringNever()
    {
        return getTime() == EXPIRES_NEVER;
    }
    
    public boolean isExpired()
    {
        if ( isExpiringEndOfSession() || isExpiringNever() )
        {
            return false;
        }
        
        return getTime() < System.currentTimeMillis();
    }
}