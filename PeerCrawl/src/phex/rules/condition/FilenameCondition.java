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
 *  $Id: FilenameCondition.java,v 1.1 2005/11/20 00:00:57 gregork Exp $
 */
package phex.rules.condition;

import java.util.*;

import phex.download.RemoteFile;
import phex.query.Search;

/**
 * Filters all files matching the expression.
 */
public class FilenameCondition implements Condition
{
    private Set terms;
    
    /**
     * Indicates if case should be ignored.
     */
    private boolean ignoreCase;
        
    /**
     * @param expression
     * @param case1
     */
    public FilenameCondition( )
    {
        terms = new LinkedHashSet();
    }
    
    public FilenameCondition addTerm( String term )
    {
        term = term.toLowerCase();
        terms.add( term );
        return this;
    }

    public boolean isMatched( Search search, RemoteFile remoteFile )
    {
        String filename = remoteFile.getFilename();
        filename = filename.toLowerCase();
        
        Iterator iterator = terms.iterator();
        while( iterator.hasNext() )
        {
            String term = (String) iterator.next();
            if ( filename.indexOf( term ) != -1 )
            {
                return true;
            }
        }
        return false;
    }
}
