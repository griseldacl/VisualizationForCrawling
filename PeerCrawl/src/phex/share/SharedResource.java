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
 *  Created on 13.12.2004
 *  --- CVS Information ---
 *  $Id: SharedResource.java,v 1.3 2005/10/03 00:18:28 gregork Exp $
 */
package phex.share;

import java.io.File;

/**
 * Represents a shared resource on the file system.
 * This can be a file or directory.
 */
public abstract class SharedResource
{
    protected File systemFile;
    
    public SharedResource( File file )
    {
        systemFile = file;
    }
    
    /**
     * Only called from subclasses like PartialShareFile.
     */
    protected SharedResource( )
    {
    }

    /**
     * Returns the backed file object.
     * @return the backed file object.
     */
    public File getSystemFile()
    {
        return systemFile;
    }

    /**
     * Returns the file name without path information.
     * @return the file name without path information.
     */
    public String getFileName()
    {
        return systemFile.getName();
    }
    
    public boolean equals( SharedResource resource )
    {
        return resource.systemFile.equals( systemFile );
    }
    
    public int hashCode()
    {
        return systemFile.hashCode();
    }
}
