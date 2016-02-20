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
 *  $Id: BeginEndAvailRandSelectionStrategy.java,v 1.3 2005/09/22 11:33:36 gregork Exp $
 */
package phex.download.strategy;

import java.util.Random;

import phex.download.DownloadScope;
import phex.download.DownloadScopeList;
import phex.download.swarming.SWDownloadFile;

public class BeginEndAvailRandSelectionStrategy implements ScopeSelectionStrategy
{
    private static final Random random = new Random();
    private static ScopeSelectionStrategy beginStrategy = 
            ScopeSelectionStrategyProvider.PREFERE_BEGINING_SCOPE_SELECTION_STRATEGY;
    private static ScopeSelectionStrategy endStrategy = 
            ScopeSelectionStrategyProvider.PREFERE_END_SCOPE_SELECTION_STRATEGY;
    private static ScopeSelectionStrategy availRandStrategy = 
            ScopeSelectionStrategyProvider.AVAIL_RAND_SELECTION_STRATEGY;
    
    public DownloadScope selectDownloadScope( SWDownloadFile downloadFile,
        DownloadScopeList wantedScopeList, long preferredSize )
    {
        DownloadScope scope = null;
        // choose begin/end by 50%
        boolean useBeginEnd = random.nextBoolean( );
        if ( useBeginEnd )
        {
            boolean useBegin = random.nextBoolean( );
            if ( useBegin )
            {
                scope = beginStrategy.selectDownloadScope(
                    downloadFile, wantedScopeList, preferredSize );
            }
            else
            {
                scope = endStrategy.selectDownloadScope(
                    downloadFile, wantedScopeList, preferredSize );
            }
        }
        if ( scope == null )
        {
            scope = availRandStrategy.selectDownloadScope(
                downloadFile, wantedScopeList, preferredSize);
        }
        return scope;
    }
}
