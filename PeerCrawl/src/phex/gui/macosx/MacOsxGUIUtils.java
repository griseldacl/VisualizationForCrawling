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
 *  Created on 28.08.2005
 *  --- CVS Information ---
 *  $Id: MacOsxGUIUtils.java,v 1.1 2005/08/28 19:47:54 gregork Exp $
 */
package phex.gui.macosx;

import java.awt.FileDialog;

import net.roydesign.ui.FolderDialog;
import phex.gui.common.GUIRegistry;

public class MacOsxGUIUtils
{
    /**
     * Create folder dialog here. This prevents 
     * NoClassDefFoundError on Windows systems since the import of the
     * required OS X classes is elsewhere.
     */
    public static final FileDialog createFolderDialog()
    {
        return new FolderDialog( GUIRegistry.getInstance().getMainFrame() );
    }
}
