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
 *  Created on 14.11.2005
 *  --- CVS Information ---
 *  $Id: NotCondition.java,v 1.1 2005/11/20 00:00:57 gregork Exp $
 */
package phex.rules.condition;

import phex.download.RemoteFile;
import phex.query.Search;

/**
 * Applies a NOT condition to the given search filter. That means that the 
 * filter results of the given search filter is reversed.
 */
public class NotCondition implements Condition
{
    private Condition filter;
    
    public NotCondition( Condition filter )
    {
        this.filter = filter;
    }

    public boolean isMatched( Search search, RemoteFile remoteFile )
    {
        return !filter.isMatched(null, remoteFile);
    }

}
