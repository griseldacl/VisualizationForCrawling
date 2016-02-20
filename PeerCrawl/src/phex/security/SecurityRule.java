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
 *  $Id: SecurityRule.java,v 1.11 2005/10/03 00:18:28 gregork Exp $
 */
package phex.security;

import javax.xml.bind.*;

import phex.common.*;
import phex.xml.*;

/**
 *
 * <ul>
 * <li>A rule has a ID to identify it on import and export.
 * <li>A rule has a name to describe it.
 * <li>A rule defines if access should be DENYED or ALLOWED.
 * <li>A rule has a counter that indicates how often it was triggered.
 * <li>A rule has a set expiry time. This can be a fixed lifetime, end of
 *     session, or indefinite.
 * <li>A rule can be enabled or disabled.
 * <li>When a rule expires it can be deleted or disabled.
 * </ul>
 *
 * <p>Copyright: Copyright (c) 2002 Gregor Koukkoullis</p>
 * @author Gregor Koukkoullis
 */
public abstract class SecurityRule
{
    /**
     * A description of the rule.
     */
    protected String description;

    /**
     * A system rule is not editable by the user.
     */
    protected boolean isSystemRule;

    /**
     * A strong filter is a rule that will filter traffic out of the Gnutella
     * network! It must only be used together with a system rule and is nothing
     * a user can define or edit! Use is e.g. filtering of hosts spamming the
     * network with faulty files.
     */
    protected boolean isStrongFilter;

    /**
     * When set to true checks that match the rule will fail.
     * When set to false all checks that do not match the rule will fail.
     */
    protected boolean isDenyingRule;

    /**
     * If set to true this rule is disabled.
     */
    protected boolean isDisabled;

    /**
     * Indicates that the rule was triggered. All checks that match the rule
     * will increment the trigger.
     */
    protected Integer triggerCount;

    /**
     * The expiry date that indicates when this expires. It can be a timestamp,
     * at the end of the session or never.
     */
    protected ExpiryDate expiryDate;

    /**
     * Flags if the rule is to be deleted on expiry or only disabled.
     */
    protected boolean isDeletedOnExpiry;

    public SecurityRule( String description, boolean isDenyingRule )
    {
        this( description, isDenyingRule, false, false, false );
    }
    
    public SecurityRule( String description, boolean isDenyingRule,
        boolean isSystemRule, boolean isStrongFilter, boolean isDisabled )
    {
        this.description = description;
        this.isDenyingRule = isDenyingRule;

        this.isSystemRule = isSystemRule;
        this.isStrongFilter = isStrongFilter;
        this.isDisabled = isDisabled;
        expiryDate = ExpiryDate.NEVER_EXPIRY_DATE;
        isDeletedOnExpiry = false;
        triggerCount = new Integer(0);
    }

    public SecurityRule( XJBIPAccessRule xjbRule )
    {
        description = xjbRule.getDescription();
        isDenyingRule = xjbRule.isDenyingRule();
        isDisabled = xjbRule.isDisabled();
        long expiryTime = xjbRule.getExpiryDate();
        // reuse available expiry date objects...
        if ( expiryTime == ExpiryDate.EXPIRES_NEVER )
        {
            expiryDate = ExpiryDate.NEVER_EXPIRY_DATE;
        }
        else if ( expiryTime == ExpiryDate.EXPIRES_END_OF_SESSION )
        {
            expiryDate = ExpiryDate.SESSION_EXPIRY_DATE;
        }
        else
        {
            expiryDate = new ExpiryDate( expiryTime );
        }
        isDeletedOnExpiry = xjbRule.isDeletedOnExpiry();
        triggerCount = new Integer( xjbRule.getTriggerCount() );

        isSystemRule = false;
        isStrongFilter = false;
    }


    /**
     * A value of true sets this rule to be disabled, a value of true enables the
     * rule.
     */
    public void setDisabled( boolean isDisabled )
    {
        if ( this.isDisabled != isDisabled )
        {
            this.isDisabled = isDisabled;
            PhexSecurityManager.getInstance().fireSecurityRuleChanged( this );
        }
    }

    public boolean isDisabled()
    {
        return isDisabled;
    }

    /**
     * Sets the expiry date that indicates when this rule expires. It can be a
     * at a time, at the end of the session or never.
     */
    public void setExpiryDate( ExpiryDate expiryDate )
    {
        if ( !this.expiryDate.equals(expiryDate) )
        {
            this.expiryDate = expiryDate;
            PhexSecurityManager.getInstance().fireSecurityRuleChanged( this );
        }
    }

    /**
     * Returns the expiry date that indicates when this rule expires. It can be a
     * at a time, at the end of the session or never.
     * @return the expiry date that indicates when this rule expires.
     */
    public ExpiryDate getExpiryDate()
    {
        return expiryDate;
    }

    /**
     * Returns the number of times the rule was triggered. All checks that
     * match the rule will increment the trigger.
     * @return the number of times the rule was triggered.
     */
    public Integer getTriggerCountObject()
    {
        return triggerCount;
    }

    /**
     * Increments the trigger count by one.
     */
    protected void incrementTriggerCount()
    {
        triggerCount = new Integer( triggerCount.intValue() + 1 );
        PhexSecurityManager.getInstance().fireSecurityRuleChanged( this );
    }

    public void setTriggerCount( int count )
    {
        triggerCount = new Integer( count );
    }

    /**
     * Returns true when checks that match the rule will fail.
     * Returns false when all checks that do not match the rule will fail.
     * @return true when checks that match the rule will fail,
     *         false when all checks that do not match the rule will fail.
     */
    public boolean isDenyingRule()
    {
        return isDenyingRule;
    }

    /**
     * If set to true then checks that match the rule will fail.
     * If set to false then all checks that do not match the rule will fail.
     */
    public void setDenyingRule( boolean isDenyingRule )
    {
        if ( this.isDenyingRule != isDenyingRule )
        {
            this.isDenyingRule = isDenyingRule;
            PhexSecurityManager.getInstance().fireSecurityRuleChanged( this );
        }
    }

    /**
     * Returns the description of the rule.
     * @return the description of the rule.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Sets the description of the rule.
     */
    public void setDescription( String aDescription )
    {
        if ( ! this.description.equals( aDescription ) )
        {
            description = aDescription;
            PhexSecurityManager.getInstance().fireSecurityRuleChanged( this );
        }
    }


    public void setDeleteOnExpiry( boolean isDeletedOnExpiry )
    {
        if ( this.isDeletedOnExpiry != isDeletedOnExpiry )
        {
            this.isDeletedOnExpiry = isDeletedOnExpiry;
            PhexSecurityManager.getInstance().fireSecurityRuleChanged( this );
        }
    }

    public boolean isDeletedOnExpiry()
    {
        return isDeletedOnExpiry;
    }

    public void setSystemRule( boolean isSystemRule )
    {
        if ( this.isSystemRule != isSystemRule )
        {
            this.isSystemRule = isSystemRule;
            PhexSecurityManager.getInstance().fireSecurityRuleChanged( this );
        }
    }

    public boolean isSystemRule()
    {
        return isSystemRule;
    }

    public void setStrongFilter( boolean isStrongFilter )
    {
        if ( this.isStrongFilter != isStrongFilter )
        {
            this.isStrongFilter = isStrongFilter;
            PhexSecurityManager.getInstance().fireSecurityRuleChanged( this );
        }
    }

    public boolean isStrongFilter()
    {
        return isStrongFilter;
    }

    public boolean equals( Object obj )
    {
        if ( !(obj instanceof SecurityRule ) )
        {
            return false;
        }
        SecurityRule rule = (SecurityRule)obj;
        return description.equals( rule.description ) &&
            rule.isSystemRule == isSystemRule &&
            rule.isStrongFilter == isStrongFilter &&
            rule.expiryDate.equals( expiryDate ) &&
            rule.isDeletedOnExpiry == isDeletedOnExpiry &&
            rule.isDenyingRule == isDenyingRule &&
            rule.isDisabled == isDisabled;
    }

    public abstract XJBSecurityRule createXJBSecurityRule() throws JAXBException;
}