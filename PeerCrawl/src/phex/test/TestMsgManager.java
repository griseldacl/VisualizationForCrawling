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
 *  $Id: TestMsgManager.java,v 1.14 2005/11/03 23:30:13 gregork Exp $
 */
package phex.test;

import java.net.Socket;

import junit.framework.TestCase;
import phex.common.address.DefaultDestAddress;
import phex.host.*;
import phex.msg.*;
import phex.net.connection.Connection;
import phex.net.presentation.def.DefaultSocketFacade;
import phex.utils.QueryGUIDRoutingPair;


public class TestMsgManager extends TestCase
{
    private MsgManager msgmanager;

    public TestMsgManager(String s)
    {
        super(s);
    }

    protected void setUp()
    {
        msgmanager = MsgManager.getInstance();
    }

    protected void tearDown()
    {
    }

    public void testAddToPushRoutingTable()
        throws Exception
    {
        GUID pushClientGUID = new GUID( );
        Host pushHost = new Host( new DefaultDestAddress( "1.1.1.1", 1111 ) );
        // to fake a connection
        pushHost.setConnection( new Connection( new DummySocket(), null ) );
        msgmanager.addToPushRoutingTable( pushClientGUID, pushHost );

        Host host = msgmanager.getPushRouting( pushClientGUID );
        assertEquals( pushHost, host );
    }

    public void testCheckAndAddToPingRoutingTable()
        throws Exception
    {
        GUID pingGUID = new GUID();
        Host pingHost =  new Host( new DefaultDestAddress( "2.2.2.2", 2222 ) );
        // to fake a connection
        pingHost.setConnection( new Connection( new DummySocket(), null ) );

        boolean pingCheckValue = msgmanager.checkAndAddToPingRoutingTable(
            pingGUID, pingHost );
        assertEquals( true, pingCheckValue );
        pingCheckValue = msgmanager.checkAndAddToPingRoutingTable(
            pingGUID, pingHost );
        assertEquals( false, pingCheckValue );

        Host host = msgmanager.getPingRouting( pingGUID );
        assertEquals( pingHost, host );
    }

    public void testCheckAndAddToQueryRoutingTable()
        throws Exception
    {
        GUID queryGUID = new GUID();
        Host queryHost =  new Host( new DefaultDestAddress( "3.3.3.3", 3333 ) );
        // to fake a connection
        queryHost.setConnection( new Connection( new DummySocket(), null ) );

        boolean queryCheckValue = msgmanager.checkAndAddToQueryRoutingTable(
            queryGUID, queryHost );
        assertEquals( true, queryCheckValue );
        queryCheckValue = msgmanager.checkAndAddToQueryRoutingTable(
            queryGUID, queryHost );
        assertEquals( false, queryCheckValue );

        QueryGUIDRoutingPair pair = msgmanager.getQueryRouting( queryGUID, 0 );
        assertEquals( queryHost, pair.getHost() );
    }

    private class DummySocket extends DefaultSocketFacade
    {
        DummySocket()
        {
            super( new Socket() );
        }
    }
}