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
 *  $Id: TestMsgPong.java,v 1.6 2005/11/03 17:06:26 gregork Exp $
 */
package phex.test;

import junit.framework.TestCase;
import phex.msg.PongMsg;

/**
 * 
 */
public class TestMsgPong extends TestCase
{
    protected void setUp()
    {
    }

    protected void tearDown()
    {
    }

    public void testUltrapeerMarking()
        throws Throwable
    {
        Integer result;
        result = (Integer) AccessUtils.invokeMethod( PongMsg.class,
            "createUltrapeerMarking", new Object[]{ new Integer(0) },
            new Class[] {int.class} );
        assertEquals( 8, result.intValue() );
        
        result = (Integer) AccessUtils.invokeMethod( PongMsg.class,
            "createUltrapeerMarking", new Object[]{ new Integer(1536) },
            new Class[] {int.class} );
        assertEquals( 2048, result.intValue() );
        
        result = (Integer) AccessUtils.invokeMethod( PongMsg.class,
            "createUltrapeerMarking", new Object[]{ new Integer(1535) },
            new Class[] {int.class} );
        assertEquals( 1024, result.intValue() );
        
        result = (Integer) AccessUtils.invokeMethod( PongMsg.class,
            "createUltrapeerMarking", new Object[]{ new Integer(100663296) },
            new Class[] {int.class} );
        assertEquals( 134217728, result.intValue() );
        
        result = (Integer) AccessUtils.invokeMethod( PongMsg.class,
            "createUltrapeerMarking", new Object[]{ new Integer(100663295) },
            new Class[] {int.class} );
        assertEquals( 67108864, result.intValue() );
    }
}