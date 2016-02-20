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
 *  $Id: IPAccessRule.java,v 1.16 2005/11/03 16:23:36 gregork Exp $
 */
package phex.security;

import java.util.Arrays;

import javax.xml.bind.JAXBException;

import phex.common.address.AddressUtils;
import phex.xml.ObjectFactory;
import phex.xml.XJBIPAccessRule;
import phex.xml.XJBSecurityRule;

// http://www.telusplanet.net/public/sparkman/netcalc.htm
// http://www.bearshare.com/Hostiles.txt
public class IPAccessRule extends SecurityRule
{
    public static final byte SINGLE_ADDRESS = 1;
    public static final byte NETWORK_MASK = 2;
    public static final byte NETWORK_RANGE = 3;

    private byte addressType;
    private byte[] ip;
    private byte[] compareIP;
    private String addressString;

    /**
     *
     * @param type the type of ip. This can be SINGLE_ADDRESS, NETWORK_MASK or
     *        NETWORK_RANGE.
     * @param ip a ip.
     * @param compareIP the ip to compare to. It should be null if type is
     *        SINGLE_ADDRESS, a network mask if type is NETWORK_MASK, or if
     *        type is NETWORK_RANGE a second ip to define the range.
     */
    public IPAccessRule( String description, boolean isDenyingRule,
        byte type, byte[] ip, byte[] compareIP )
    {
        this( description, isDenyingRule, type, ip, compareIP, false, false, false);
    }
    
    /**
    *
    * @param type the type of ip. This can be SINGLE_ADDRESS, NETWORK_MASK or
    *        NETWORK_RANGE.
    * @param ip a ip.
    * @param compareIP the ip to compare to. It should be null if type is
    *        SINGLE_ADDRESS, a network mask if type is NETWORK_MASK, or if
    *        type is NETWORK_RANGE a second ip to define the range.
    */
   public IPAccessRule( String description, boolean isDenyingRule,
       byte type, byte[] ip, byte[] compareIP, boolean isSystemRule,
       boolean isStrongFilter, boolean isDisabled )
   {
       super( description, isDenyingRule, isSystemRule, isStrongFilter,
           isDisabled );
       if ( ip == null )
       {
           throw new NullPointerException( "IP is null." );
       }
       this.addressType = type;
       if (   type == NETWORK_RANGE &&
           (  (ip[0]&0xFF) > (compareIP[0]&0xFF) || (ip[1]&0xFF) > (compareIP[1]&0xFF)
           || (ip[2]&0xFF) > (compareIP[2]&0xFF) || (ip[3]&0xFF) > (compareIP[3]&0xFF) ) )
       {
           this.ip = compareIP;
           this.compareIP = ip;
       }
       else
       {
           this.ip = ip;
           this.compareIP = compareIP;
       }
   }

    public IPAccessRule( XJBIPAccessRule xjbRule )
    {
        super( xjbRule );
        addressType = xjbRule.getAddressType();
        ip = xjbRule.getIp();
        compareIP = xjbRule.getCompareIP();
    }

    public boolean isHostIPAllowed( byte[] hostIP )
    {
        boolean isMatched;
        switch( addressType )
        {
            case SINGLE_ADDRESS:
                isMatched = Arrays.equals( hostIP, ip );
                break;
            case NETWORK_MASK:
                isMatched = (
                    (ip[0]&compareIP[0]) == (hostIP[0]&compareIP[0]) &&
                    (ip[1]&compareIP[1]) == (hostIP[1]&compareIP[1]) &&
                    (ip[2]&compareIP[2]) == (hostIP[2]&compareIP[2]) &&
                    (ip[3]&compareIP[3]) == (hostIP[3]&compareIP[3]) );
                break;
            case NETWORK_RANGE:
                isMatched = (
                    (hostIP[0]&0xFF) >= (ip[0]&0xFF) && (hostIP[0]&0xFF) <= (compareIP[0]&0xFF) &&
                    (hostIP[1]&0xFF) >= (ip[1]&0xFF) && (hostIP[1]&0xFF) <= (compareIP[1]&0xFF) &&
                    (hostIP[2]&0xFF) >= (ip[2]&0xFF) && (hostIP[2]&0xFF) <= (compareIP[2]&0xFF) &&
                    (hostIP[3]&0xFF) >= (ip[3]&0xFF) && (hostIP[3]&0xFF) <= (compareIP[3]&0xFF) );
                break;
            default:
                throw new IllegalArgumentException( "Unknown type: " + addressType );
        }

        if ( isMatched )
        {
            incrementTriggerCount();
        }

        // isDenyingRule | isMatched => isAllowed
        // true          | true      => false
        // true          | false     => true
        // false         | true      => true
        // false         | false     => false
        return isDenyingRule ^ isMatched;
    }

    /**
     * Returns the host ip that is used. In case of a SINGLE_ADDRESS address type
     * this is simply the ip used to control the access for. For NETWORK_RANGE
     * address type this is the starting address inclusive. For NETWORK_MASK this
     * is the ip to mask.
     * @return the host ip that is used.
     */
    public byte[] getHostIP()
    {
        return ip;
    }

    /**
     * Sets the host ip to use. In case of a SINGLE_ADDRESS address type
     * this is simply the ip used to control the access for. For NETWORK_RANGE
     * address type this is the starting address inclusive. For NETWORK_MASK this
     * is the ip to mask.
     * @param ip the host ip to use.
     */
    public void setHostIP( byte[] ip )
    {
        if ( !Arrays.equals( this.ip, ip ) )
        {
            this.ip = ip;
            addressString = null;
            PhexSecurityManager.getInstance().fireSecurityRuleChanged( this );
        }
    }

    /**
     * Returns the compare ip that is used. In case of a SINGLE_ADDRESS it is
     * usually not set and can be ignored. For NETWORK_RANGE address type this
     * is the ending address inclusive. For NETWORK_MASK this is the mask to use.
     * @return the compare ip that is used.
     */
    public byte[] getCompareIP()
    {
        return compareIP;
    }

    /**
     * Sets the compare ip that is used. In case of a SINGLE_ADDRESS it is
     * usually set to null and can be ignored. For NETWORK_RANGE address type this
     * is the ending address inclusive. For NETWORK_MASK this is the mask to use.
     * @param ip the compare ip that is used.
     */
    public void setCompareIP( byte[] ip )
    {
        if ( !Arrays.equals( compareIP, ip ) )
        {
            this.compareIP = ip;
            addressString = null;
            PhexSecurityManager.getInstance().fireSecurityRuleChanged( this );
        }
    }


    /**
     * Returns the address type of this IPAccessRule. This can be SINGLE_ADDRESS,
     * NETWORK_RANGE or NETWORK_MASK.
     * @return the address type of this IPAccessRule.
     */
    public byte getAddressType()
    {
        return addressType;
    }

    /**
     * Sets the address type of the IPAccessRule. Supported types are SINGLE_ADDRESS,
     * NETWORK_RANGE and NETWORK_MASK.
     * @param addressType the new address type to use.
     */
    public void setAddressType( byte addressType )
    {
        if ( this.addressType != addressType )
        {
            this.addressType = addressType;
            addressString = null;
            PhexSecurityManager.getInstance().fireSecurityRuleChanged( this );
        }
    }

    public String getAddressString()
    {
        if ( addressString == null )
        {
            switch( addressType )
            {
                case SINGLE_ADDRESS:
                    addressString = AddressUtils.ip2string( ip );
                    break;
                case NETWORK_MASK:
                    addressString = AddressUtils.ip2string( ip ) + '/'
                        + AddressUtils.ip2string( compareIP );
                    break;
                case NETWORK_RANGE:
                    addressString =  AddressUtils.ip2string( ip ) + '-'
                        + AddressUtils.ip2string( compareIP );
                    break;
                default:
                    throw new IllegalArgumentException( "Unknown type: " + addressType );
            }
        }
        return addressString;
    }

    public boolean equals( Object obj )
    {
        if ( !(obj instanceof IPAccessRule ) )
        {
            return false;
        }
        IPAccessRule rule = (IPAccessRule)obj;
        return rule.addressType == addressType &&
            Arrays.equals( rule.ip, ip ) &&
            Arrays.equals( rule.compareIP, compareIP ) &&
            super.equals( rule );
    }

    public XJBSecurityRule createXJBSecurityRule()
        throws JAXBException
    {
        ObjectFactory objFactory = new ObjectFactory();
        XJBIPAccessRule xjbRule = objFactory.createXJBIPAccessRule();
        if ( !isSystemRule )
        {
            xjbRule.setDescription( description );
            xjbRule.setAddressType( addressType );
            xjbRule.setExpiryDate( expiryDate.getTime() );
            xjbRule.setDeletedOnExpiry( isDeletedOnExpiry );
            xjbRule.setDenyingRule( isDenyingRule );
            xjbRule.setDisabled( isDisabled );
        }
        else
        {
            xjbRule.setSystemRule( true );
        }
        xjbRule.setIp( ip );
        xjbRule.setCompareIP( compareIP );
        xjbRule.setTriggerCount( triggerCount.intValue() );

        return xjbRule;
    }
}