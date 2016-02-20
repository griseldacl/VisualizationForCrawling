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
 *  $Id: TestMsgQueryResponse.java,v 1.15 2005/11/04 20:44:14 gregork Exp $
 */
package phex.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.TestCase;
import phex.common.ServiceManager;
import phex.common.URN;
import phex.common.address.DefaultDestAddress;
import phex.msg.*;
import phex.net.connection.Connection;
import phex.query.QueryConstants;
import phex.upload.UploadManager;
import phex.utils.GnutellaInputStream;
import phex.utils.GnutellaOutputStream;


public class TestMsgQueryResponse extends TestCase
{

    public TestMsgQueryResponse(String s)
    {
        super(s);
    }

    protected void setUp()
    {
    }

    protected void tearDown()
    {
    }

    public void testCreateAndParse()
        throws Exception
    {
        MsgHeader header = new MsgHeader( new GUID(), MsgHeader.QUERY_HIT_PAYLOAD,
            (byte)0x7, (byte)0x0, 0 );
        QueryResponseRecord rec = new QueryResponseRecord( 1, new URN(
            "urn:sha1:LO4DP3SD3I3CZZP6PIKG3VCQHG4KTQD2" ), 1, "file" );
        QueryResponseRecord[] recArr =
        {
            rec
        };

        QueryResponseMsg respIn = new QueryResponseMsg( header, new GUID(),
            new DefaultDestAddress( "111.111.111.111", 1111 ), 0, recArr );
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        GnutellaOutputStream gOutStream = new GnutellaOutputStream(outStream);
        respIn.writeMessage( gOutStream );

        byte[] output = outStream.toByteArray();
        //System.out.println( new String(output) + "\n" + HexConverter.toHexString( output ) );

        GnutellaInputStream stream = new GnutellaInputStream(
            new ByteArrayInputStream( output ) );
        Connection connection = new Connection(null, null);
        AccessUtils.setFieldValue( connection, "inputStream", stream );

        QueryResponseMsg respOut = (QueryResponseMsg)MessageProcessor.parseMessage( connection );

        MsgHeader outHeader = respOut.getHeader();
        
        assertEquals( header.getDataLength(), outHeader.getDataLength() );
        assertEquals( header.getHopsTaken(), outHeader.getHopsTaken() );
        assertEquals( header.getMsgID().toHexString(), outHeader.getMsgID().toHexString() );
        assertEquals( header.getPayload(), outHeader.getPayload() );
        assertEquals( header.getTTL(), outHeader.getTTL() );
        assertEquals( respIn.getDestAddress(), respOut.getDestAddress() );
        assertEquals( respIn.getRecordCount(), respOut.getRecordCount() );
        assertEquals( respIn.getRemoteClientID(), respOut.getRemoteClientID() );
        assertEquals( respIn.getRemoteHostSpeed(), respOut.getRemoteHostSpeed() );
        assertEquals( respIn.getUploadSpeedFlag(), respOut.getUploadSpeedFlag() );
        assertEquals( respIn.getPushNeededFlag(), respOut.getPushNeededFlag() );
        assertEquals( ServiceManager.sCfg.mShareBrowseDir, respOut.isBrowseHostSupported() );
        assertEquals( QueryConstants.QHD_UNKNOWN_FLAG, respOut.getHasUploadedFlag() );
        assertEquals( "PHEX", respOut.getVendorCode() );
        assertEquals( true, respOut.isChatSupported() );
        assertEquals( ServiceManager.sCfg.isChatEnabled, respOut.isChatSupported() );

        assertEquals( UploadManager.getInstance().isHostBusy() ?
            QueryConstants.QHD_TRUE_FLAG : QueryConstants.QHD_FALSE_FLAG,
            respOut.getServerBusyFlag() );
    }
}