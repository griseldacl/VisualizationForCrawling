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
 *  $Id: AvailBeginRandSelectionStrategy.java,v 1.1 2005/11/13 10:50:45 gregork Exp $
 */
package phex.download.strategy;

import java.util.Random;

import phex.download.DownloadScope;
import phex.download.DownloadScopeList;
import phex.download.swarming.SWDownloadFile;

/**
 * This download strategy first analyses if there is a usefull scope by 
 * availability. If no scope was found and the file is streamable there is a 
 * 50% chance that the begining is selected otherwise a random segment is choosen.
 */
public class AvailBeginRandSelectionStrategy implements ScopeSelectionStrategy
{
    private static final Random random = new Random();
    private static ScopeSelectionStrategy availStrategy = 
            ScopeSelectionStrategyProvider.AVAILABILITY_SCOPE_SELECTION_STRATEGY;
    private static ScopeSelectionStrategy beginStrategy = 
        ScopeSelectionStrategyProvider.PREFERE_BEGINING_SCOPE_SELECTION_STRATEGY;
    private static ScopeSelectionStrategy randStrategy = 
            ScopeSelectionStrategyProvider.RANDOM_SCOPE_SELECTION_STRATEGY;
    
    public DownloadScope selectDownloadScope( SWDownloadFile downloadFile,
        DownloadScopeList wantedScopeList, long preferredSize )
    {
        DownloadScope scope = availStrategy.selectDownloadScope(
            downloadFile, wantedScopeList, preferredSize);
        
        if ( scope == null && downloadFile.isDestinationStreamable() )
        {
            // choose begining by 50%
            boolean useBegin = random.nextBoolean( );
            if ( useBegin )
            {
                scope = beginStrategy.selectDownloadScope(downloadFile, 
                    wantedScopeList, preferredSize);
            }
        }
        
        if ( scope == null )
        {
            scope = randStrategy.selectDownloadScope(
                downloadFile, wantedScopeList, preferredSize);
        }
        return scope;
    }
}
