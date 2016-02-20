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
 *  $Id$
 */
package phex.test;

import junit.framework.TestCase;
import phex.msg.QueryMsg;

public class TestMsgQuery extends TestCase
{
    protected void setUp()
    {
    }

    protected void tearDown()
    {
    }

    public void testIsBitSet()
        throws Throwable
    {
        Boolean state;
        state =
            (Boolean) AccessUtils.invokeMethod(
                QueryMsg.class,
                "isBitSet",
                new Object[] { new Short( (short) 0x0001), new Integer(0)});
        assertTrue(state.booleanValue());
        state =
            (Boolean) AccessUtils.invokeMethod(
                QueryMsg.class,
                "isBitSet",
                new Object[] { new Short( (short) 0x0001), new Integer(15)});
        assertFalse(state.booleanValue());

        state =
            (Boolean) AccessUtils.invokeMethod(
                QueryMsg.class,
                "isBitSet",
                new Object[] { new Short( (short) 0x8000), new Integer(15)});
        assertTrue(state.booleanValue());
    }
    
    public void testSetBit()
        throws Throwable
    {
        Short result;
        result =
            (Short) AccessUtils.invokeMethod(
                QueryMsg.class,
                "setBit",
                new Object[] { new Short( (short) 0x0000), new Integer(0)});
        assertEquals(result.shortValue(), 0x0001);
        // ....
    }

    public void testSettingAndCheckingBits()
        throws Throwable
    {
        Short myShort = new Short( (short) 0x0000);

        Boolean state;

        // -----------------------------------------------------------------------------------------        
        // Set bit number 0
        myShort =  (Short) AccessUtils.invokeMethod(QueryMsg.class, "setBit",
            new Object[] { myShort, new Integer(0)});

        // Check it (should be set)
        state = (Boolean) AccessUtils.invokeMethod(QueryMsg.class, "isBitSet",
                new Object[] { myShort, new Integer(0)});
        assertTrue(state.booleanValue());

        // Check other bits (should be clear)
        for(int i=1; i<16; i++ )
        {
            Integer myInt = new Integer(i);
            state = (Boolean) AccessUtils.invokeMethod(QueryMsg.class, "isBitSet",
                    new Object[] { myShort, myInt});
            assertFalse(state.booleanValue());
        }

        // -----------------------------------------------------------------------------------------        
        // Set bit number 1
        myShort =  (Short) AccessUtils.invokeMethod(QueryMsg.class, "setBit",
            new Object[] { myShort, new Integer(1)});

        // Check bits 0-1 (should be set)
        for(int i=0; i<2; i++)
        {
            Integer myInt = new Integer(i);
            state = (Boolean) AccessUtils.invokeMethod(QueryMsg.class, "isBitSet",
                    new Object[] { myShort, myInt});
            assertTrue(state.booleanValue());
        }

        // Check other bits (should be clear)
        for(int i=2; i<16; i++)
        {
            Integer myInt = new Integer(i);
            state = (Boolean) AccessUtils.invokeMethod(QueryMsg.class, "isBitSet",
                    new Object[] { myShort, myInt});
            assertFalse(state.booleanValue());
        }

        // -----------------------------------------------------------------------------------------        
        // Set bit number 15 (highest)
        myShort =  (Short) AccessUtils.invokeMethod(QueryMsg.class, "setBit",
            new Object[] { myShort, new Integer(15)});

        // Check bits 0-1 (should be set)
        for(int i=0; i<2; i++)
        {
            Integer myInt = new Integer(i);
            state = (Boolean) AccessUtils.invokeMethod(QueryMsg.class, "isBitSet",
                    new Object[] { myShort, myInt});
            assertTrue(state.booleanValue());
        }

        // Check bit 15 (should be set)
        state = (Boolean) AccessUtils.invokeMethod(QueryMsg.class, "isBitSet",
                new Object[] { myShort, new Integer(15)});
        assertTrue(state.booleanValue());

        // Check other bits (should be clear)
        for(int i=2; i<15; i++)
        {
            Integer myInt = new Integer(i);
            state = (Boolean) AccessUtils.invokeMethod(QueryMsg.class, "isBitSet",
                    new Object[] { myShort, myInt});
            assertFalse(state.booleanValue());
        }

    }

}
