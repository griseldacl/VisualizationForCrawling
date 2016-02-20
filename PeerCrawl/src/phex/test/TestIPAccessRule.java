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
 *  $Id: TestIPAccessRule.java,v 1.12 2005/11/03 23:30:13 gregork Exp $
 */
package phex.test;

import junit.framework.TestCase;
import phex.common.address.DefaultDestAddress;
import phex.security.*;

public class TestIPAccessRule extends TestCase
{
    byte[][] testSingleAddresses;

    public TestIPAccessRule()
    {
    }

    protected void setUp() throws Exception
    {
        testSingleAddresses = new byte[][]
        {
            { 11, 2, 3, 4 },
            { 22, 3, 4, 5 },
            { 33, 5, 6, 7 },
            { 44, 8, 9, 1 },
            { 66, 8, 9, 1 },
            { 77, 8, 9, 1 },
            { 88, 8, 9, 1 },
            { 99, 8, 9, 1 },
        };
    }

    public void testSingleAddress() throws Exception
    {
        for ( int i = 0; i < testSingleAddresses.length; i++ )
        {
            for ( int j = 0; j < testSingleAddresses.length; j++ )
            {
                IPAccessRule rule = new IPAccessRule( "test", true,
                    IPAccessRule.SINGLE_ADDRESS, testSingleAddresses[j], null );

                boolean res = rule.isHostIPAllowed( testSingleAddresses[i] );
                if ( i != j )
                {
                    assertTrue( res );
                }
                else
                {
                    assertFalse( res );
                }
            }
        }

        for ( int i = 0; i < testSingleAddresses.length; i++ )
        {
            for ( int j = 0; j < testSingleAddresses.length; j++ )
            {
                IPAccessRule rule = new IPAccessRule( "test", false,
                    IPAccessRule.SINGLE_ADDRESS, testSingleAddresses[j], null );

                boolean res = rule.isHostIPAllowed( testSingleAddresses[i] );
                if ( i == j )
                {
                    assertTrue( res );
                }
                else
                {
                    assertFalse( res );
                }
            }
        }
    }

    public void testAddressMask() throws Exception
    {
        boolean succ;

        IPAccessRule rule = new IPAccessRule( "test", true, IPAccessRule.NETWORK_MASK,
            new byte[]{ 11, 1, 1, 0 }, new byte[]{ (byte)255, (byte)255, (byte)255, 0 } );
        succ = rule.isHostIPAllowed( new byte[] { 11, 1, 1, 1 } );
        assertFalse( succ );
        succ = rule.isHostIPAllowed( new byte[] { 11, 1, 1, 2 } );
        assertFalse( succ );
        succ = rule.isHostIPAllowed( new byte[] { 11, 1, 2, 1 } );
        assertTrue( succ );
        succ = rule.isHostIPAllowed( new byte[] { 11, 2, 1, 1 } );
        assertTrue( succ );
        succ = rule.isHostIPAllowed( new byte[] { 12, 1, 1, 1 } );
        assertTrue( succ );

        rule = new IPAccessRule( "test", true, IPAccessRule.NETWORK_MASK,
            new byte[]{ 11, 1, 1, 0 }, new byte[]{ (byte)255, (byte)255, (byte)0, 0 } );
        succ = rule.isHostIPAllowed( new byte[] { 11, 1, 1, 1 } );
        assertFalse( succ );
        succ = rule.isHostIPAllowed( new byte[] { 11, 1, 1, 2 } );
        assertFalse( succ );
        succ = rule.isHostIPAllowed( new byte[] { 11, 1, 2, 1 } );
        assertFalse( succ );
        succ = rule.isHostIPAllowed( new byte[] { 11, 2, 1, 1 } );
        assertTrue( succ );
        succ = rule.isHostIPAllowed( new byte[] { 12, 1, 1, 1 } );
        assertTrue( succ );

        rule = new IPAccessRule( "test", true, IPAccessRule.NETWORK_MASK,
            new byte[]{ 11, 1, 1, 0 }, new byte[]{ (byte)255, (byte)0, (byte)0, 0 } );
        succ = rule.isHostIPAllowed( new byte[] { 11, 1, 1, 1 } );
        assertFalse( succ );
        succ = rule.isHostIPAllowed( new byte[] { 11, 1, 1, 2 } );
        assertFalse( succ );
        succ = rule.isHostIPAllowed( new byte[] { 11, 1, 2, 1 } );
        assertFalse( succ );
        succ = rule.isHostIPAllowed( new byte[] { 11, 2, 1, 1 } );
        assertFalse( succ );
        succ = rule.isHostIPAllowed( new byte[] { 12, 1, 1, 1 } );
        assertTrue( succ );

        rule = new IPAccessRule( "test", true, IPAccessRule.NETWORK_MASK,
            new byte[]{ (byte)209, (byte)204, (byte)128, 0 },
            new byte[]{ (byte)255, (byte)255, (byte)255, (byte)192 } );
        succ = rule.isHostIPAllowed(
            new byte[] { (byte)209, (byte)204, (byte)128, 1 } );
        assertFalse( succ );
        succ = rule.isHostIPAllowed(
            new byte[] { (byte)209, (byte)204, (byte)128, 60 } );
        assertFalse( succ );
        succ = rule.isHostIPAllowed(
            new byte[] { (byte)209, (byte)204, (byte)128, 64 } );
        assertTrue( succ );
        succ = rule.isHostIPAllowed(
            new byte[] { (byte)209, (byte)204, (byte)128, (byte)128 } );
        assertTrue( succ );
        succ = rule.isHostIPAllowed(
            new byte[] { (byte)209, (byte)204, (byte)128, (byte)250 } );
        assertTrue( succ );
    }

    public void testAddressRange() throws Exception
    {
        boolean succ;

        IPAccessRule rule = new IPAccessRule( "test", true, IPAccessRule.NETWORK_RANGE,
            new byte[]{ 11, 1, 1, 2 }, new byte[]{ (byte)11, (byte)1, (byte)1, 3 } );
        succ = rule.isHostIPAllowed( new byte[] { 11, 1, 1, 1 } );
        assertTrue( succ );
        succ = rule.isHostIPAllowed( new byte[] { 11, 1, 1, 2 } );
        assertFalse( succ );
        succ = rule.isHostIPAllowed( new byte[] { 11, 1, 1, 3 } );
        assertFalse( succ );
        succ = rule.isHostIPAllowed( new byte[] { 11, 1, 2, 1 } );
        assertTrue( succ );
        succ = rule.isHostIPAllowed( new byte[] { 11, 2, 1, 1 } );
        assertTrue( succ );
        succ = rule.isHostIPAllowed( new byte[] { 12, 1, 1, 1 } );
        assertTrue( succ );

        rule = new IPAccessRule( "test", false, IPAccessRule.NETWORK_RANGE,
            new byte[]{ 11, 1, 1, 2 }, new byte[]{ (byte)11, (byte)1, (byte)1, 3 } );
        succ = rule.isHostIPAllowed( new byte[] { 11, 1, 1, 1 } );
        assertFalse( succ );
        succ = rule.isHostIPAllowed( new byte[] { 11, 1, 1, 2 } );
        assertTrue( succ );
        succ = rule.isHostIPAllowed( new byte[] { 11, 1, 1, 3 } );
        assertTrue( succ );
        succ = rule.isHostIPAllowed( new byte[] { 11, 1, 2, 1 } );
        assertFalse( succ );
        succ = rule.isHostIPAllowed( new byte[] { 11, 2, 1, 1 } );
        assertFalse( succ );
        succ = rule.isHostIPAllowed( new byte[] { 12, 1, 1, 1 } );
        assertFalse( succ );
        succ = rule.isHostIPAllowed( new byte[] { 11, 1, 1, (byte)254 } );
        assertFalse( succ );
    }

    public void testHostileHostList() throws Exception
    {
        PhexSecurityManager manager = PhexSecurityManager.getInstance();
        long start = System.currentTimeMillis();
        //for ( int i = 0; i<10000; i++)
        {
            byte ret = manager.controlHostAddressAccess( new DefaultDestAddress( "12.12.12.12", 80 ) );
            assertEquals( ret, PhexSecurityManager.ACCESS_GRANTED );
    
            ret = manager.controlHostAddressAccess( new DefaultDestAddress( "32.1.2.3", 80 ) );
            assertEquals( ret, PhexSecurityManager.ACCESS_STRONGLY_DENIED );
    
            ret = manager.controlHostAddressAccess( new DefaultDestAddress( "216.34.123.56", 80 ) );
            assertEquals( ret, PhexSecurityManager.ACCESS_STRONGLY_DENIED );
        }
        long end = System.currentTimeMillis();
        System.out.println( "Time: " + (end -start) );
    }
}