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
 *  Created on 20.09.2005
 *  --- CVS Information ---
 *  $Id: AvailRandSelectionStrategy.java,v 1.2 2005/09/22 11:33:36 gregork Exp $
 */
package phex.download.strategy;

import phex.download.DownloadScope;
import phex.download.DownloadScopeList;
import phex.download.swarming.SWDownloadFile;

public class AvailRandSelectionStrategy implements ScopeSelectionStrategy
{
    private static ScopeSelectionStrategy availStrategy = 
            ScopeSelectionStrategyProvider.AVAILABILITY_SCOPE_SELECTION_STRATEGY;
    private static ScopeSelectionStrategy randStrategy = 
            ScopeSelectionStrategyProvider.RANDOM_SCOPE_SELECTION_STRATEGY;
    
    public DownloadScope selectDownloadScope( SWDownloadFile downloadFile,
        DownloadScopeList wantedScopeList, long preferredSize )
    {
        DownloadScope scope = availStrategy.selectDownloadScope(
            downloadFile, wantedScopeList, preferredSize);
        if ( scope == null )
        {
            scope = randStrategy.selectDownloadScope(
                downloadFile, wantedScopeList, preferredSize);
        }
        return scope;
    }
}