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
 *  Created on 16.11.2005
 *  --- CVS Information ---
 *  $Id: Rule.java,v 1.1 2005/11/20 00:00:57 gregork Exp $
 */
package phex.rules;

import java.util.ArrayList;
import java.util.List;

import phex.rules.condition.AndConcatCondition;
import phex.rules.condition.Condition;
import phex.rules.consequence.Consequence;

public class Rule
{
    private String name;
    private boolean isActive;
    
    private AndConcatCondition ruleCondition;
    private List consequences;
    
    public Rule()
    {
        ruleCondition = new AndConcatCondition();
        consequences = new ArrayList();
    }
    
    public boolean isActive()
    {
        return isActive;
    }

    public void setActive( boolean isActive )
    {
        this.isActive = isActive;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public Rule addCondition( Condition condition )
    {
        ruleCondition.addCondition(condition);
        return this;
    }
    
    public void addConsequence( Consequence consequence )
    {
        consequences.add(consequence);
    }
}
