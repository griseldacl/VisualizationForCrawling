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
 *  $Id: ScopeSelectionStrategyProvider.java,v 1.3 2005/11/13 10:50:45 gregork Exp $
 */
package phex.download.strategy;

public class ScopeSelectionStrategyProvider
{
    protected static AvailabilityScopeSelectionStrategy 
        AVAILABILITY_SCOPE_SELECTION_STRATEGY = new AvailabilityScopeSelectionStrategy();
    protected static RandomScopeSelectionStrategy 
        RANDOM_SCOPE_SELECTION_STRATEGY = new RandomScopeSelectionStrategy();
    protected static PrefereBeginingScopeSelectionStrategy 
        PREFERE_BEGINING_SCOPE_SELECTION_STRATEGY = new PrefereBeginingScopeSelectionStrategy();
    protected static PrefereEndScopeSelectionStrategy 
        PREFERE_END_SCOPE_SELECTION_STRATEGY = new PrefereEndScopeSelectionStrategy();
    
    protected static AvailRandSelectionStrategy 
        AVAIL_RAND_SELECTION_STRATEGY = new AvailRandSelectionStrategy();
    
    protected static AvailBeginRandSelectionStrategy 
        AVAIL_BEGIN_RAND_SELECTION_STRATEGY = new AvailBeginRandSelectionStrategy();
    protected static BeginAvailRandSelectionStrategy 
         BEGIN_AVAIL_RAND_SELECTION_STRATEGY = new BeginAvailRandSelectionStrategy();
    protected static BeginEndAvailRandSelectionStrategy 
         BEGIN_END_AVAIL_RAND_SELECTION_STRATEGY = new BeginEndAvailRandSelectionStrategy();
    
    public static ScopeSelectionStrategy getAvailBeginRandSelectionStrategy()
    {
        return AVAIL_BEGIN_RAND_SELECTION_STRATEGY;
    }
    
    public static ScopeSelectionStrategy getBeginAvailRandSelectionStrategy()
    {
        return BEGIN_AVAIL_RAND_SELECTION_STRATEGY;
    }
    
    public static ScopeSelectionStrategy getBeginEndAvailRandSelectionStrategy()
    {
        return BEGIN_END_AVAIL_RAND_SELECTION_STRATEGY;
    }
    
    public static ScopeSelectionStrategy getRandomSelectionStrategy()
    {
        return RANDOM_SCOPE_SELECTION_STRATEGY;
    }

    public static ScopeSelectionStrategy getByClassName( String scopeSelectionStrategy )
    {
        if ( BEGIN_AVAIL_RAND_SELECTION_STRATEGY.getClass().getName().equals(
            scopeSelectionStrategy) )
        {
            return BEGIN_AVAIL_RAND_SELECTION_STRATEGY;
        }
        else if ( BEGIN_END_AVAIL_RAND_SELECTION_STRATEGY.getClass().getName().equals(
            scopeSelectionStrategy) )
        {
            return BEGIN_END_AVAIL_RAND_SELECTION_STRATEGY;
        }
        else if ( RANDOM_SCOPE_SELECTION_STRATEGY.getClass().getName().equals(
            scopeSelectionStrategy) )
        {
            return RANDOM_SCOPE_SELECTION_STRATEGY;
        }
        else
        {// default
            return AVAIL_BEGIN_RAND_SELECTION_STRATEGY;
        }
    }
}
