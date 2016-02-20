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
 *  $Id: MessagesSupportedVMsg.java,v 1.11 2005/10/03 00:18:28 gregork Exp $
 */
package phex.msg.vendor;

import java.io.*;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import phex.msg.*;
import phex.msg.MsgHeader;
import phex.utils.*;
import phex.utils.IOUtil;

/**
 * 
 * @author gkoukkoullis
 */
public class MessagesSupportedVMsg extends VendorMsg
{
    private static final int VERSION = 0;
    private static MessagesSupportedVMsg myMessagesSupportedVMsg;
    
    private HashSet supportedMessages = new HashSet();

    /**
     * @param header
     * @param version
     * @param data
     */
    public MessagesSupportedVMsg(MsgHeader header, byte[] vendorId,
        int subSelector, int version, byte[] data)
        throws InvalidMessageException
    {
        super(header, vendorId, subSelector, version, data);
        if (version > VERSION)
        {
            throw new InvalidMessageException(
                "Vendor Message 'MessagesSupported' with invalid version: "
                    + version);
        }
        if ((data.length - 2) % 8 != 0)
        {
            throw new InvalidMessageException(
                "Vendor Message 'MessagesSupported' invalid data length: "
                    + data.length);
        }
        ByteArrayInputStream dataStream = new ByteArrayInputStream(data, 0,
            data.length);

        int itemCount;
        try
        {
            itemCount = IOUtil.unsignedShort2Int(IOUtil
                .deserializeShortLE(dataStream));
            byte[] itemBytes = new byte[8];
            for (int i = 0; i < itemCount; i++)
            {
                dataStream.read(itemBytes);
                SupportedMessage message = new SupportedMessage(itemBytes);
                supportedMessages.add(message);
            }
        }
        catch (IOException exp)
        {
            NLogger.error( NLoggerNames.MESSAGE_ENCODE_DECODE, exp, exp );
            throw new InvalidMessageException(exp.getMessage());
        }
    }
    
    /**
     * Create my MessagesSupportedVMsg used to tell others.
     */
    private MessagesSupportedVMsg()
    {
        super(VENDORID_NULL, SUBSELECTOR_MESSAGES_SUPPORTED, VERSION, 
            IOUtil.EMPTY_BYTE_ARRAY );
        createSupportedMsgData( );
    }
    
    public static MessagesSupportedVMsg getMyMsgSupported()
    {
        if ( myMessagesSupportedVMsg == null )
        {
            myMessagesSupportedVMsg = new MessagesSupportedVMsg();
        }
        return myMessagesSupportedVMsg;
    }
    
    public boolean isVendorMessageSupported(byte[] vendorId, int subSelector)
    {
        Iterator iterator = supportedMessages.iterator();
        while (iterator.hasNext())
        {
            SupportedMessage msg = (SupportedMessage) iterator.next();
            if (Arrays.equals(vendorId, msg.getVendorId())
                && subSelector == msg.getSubSelector())
            {
                return true;
            }
        }
        return false;
    }

    public boolean isTCPConnectBackSupported()
    {
        return isVendorMessageSupported(VENDORID_BEAR,
            SUBSELECTOR_TCP_CONNECT_BACK);
    }
    
    public boolean isPushProxySupported()
    {
        return isVendorMessageSupported(VENDORID_LIME,
            SUBSELECTOR_PUSH_PROXY_REQUEST);
    }
    
    public boolean isHopsFlowSupported()
    {
        return isVendorMessageSupported(VENDORID_BEAR,
            SUBSELECTOR_HOPS_FLOW);
    }
    
    private void createSupportedMsgData( )
    {
        SupportedMessage supportedMsg = null;
        
        // TCPConnectBack
        supportedMsg = new SupportedMessage( VENDORID_BEAR,
            SUBSELECTOR_TCP_CONNECT_BACK, TCPConnectBackVMsg.VERSION );
        supportedMessages.add( supportedMsg );
        
        // Hops Flow
        supportedMsg = new SupportedMessage( VENDORID_BEAR,
            SUBSELECTOR_HOPS_FLOW, HopsFlowVMsg.VERSION);
        supportedMessages.add( supportedMsg );
        
        // Push Proxy Request
        supportedMsg = new SupportedMessage( VENDORID_LIME,
            SUBSELECTOR_PUSH_PROXY_REQUEST, PushProxyRequestVMsg.VERSION);
        supportedMessages.add( supportedMsg );
        
        
        
        try 
        {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            IOUtil.serializeShortLE( (short)supportedMessages.size(), outStream );
            
            Iterator iterator = supportedMessages.iterator();
            while ( iterator.hasNext() ) 
            {
                supportedMsg = (SupportedMessage)iterator.next();
                supportedMsg.serialize( outStream );
            }
            byte[] data = outStream.toByteArray();
            setVenderMsgData( data );
        }
        catch (IOException exp)
        {
            // should never happen
            NLogger.error( NLoggerNames.MESSAGE_ENCODE_DECODE, exp, exp );
        }
    }

    class SupportedMessage
    {
        private byte[] vendorId;

        private int subSelector;

        private int version;

        private int hashCode = -1;

        /**
         * @param itemBytes
         */
        public SupportedMessage(byte[] itemBytes)
        {
            vendorId = new byte[4];
            System.arraycopy(itemBytes, 0, vendorId, 0, 4);

            subSelector = IOUtil.unsignedShort2Int(IOUtil.deserializeShortLE(
                itemBytes, 4));
            version = IOUtil.unsignedShort2Int(IOUtil.deserializeShortLE(
                itemBytes, 6));
        }
        
        public SupportedMessage( byte[] vendorId, int subSelector, int version )
        {
            this.vendorId = vendorId;
            this.subSelector = subSelector;
            this.version = version;
        }
        
        public void serialize( OutputStream outStream )
            throws IOException
        {
            outStream.write( vendorId );
            IOUtil.serializeShortLE( (short)subSelector, outStream );
            IOUtil.serializeShortLE( (short)version, outStream );
        }

        /**
         * @return Returns the subSelector.
         */
        public int getSubSelector()
        {
            return subSelector;
        }

        /**
         * @return Returns the vendorId.
         */
        public byte[] getVendorId()
        {
            return vendorId;
        }

        /**
         * @return Returns the version.
         */
        public int getVersion()
        {
            return version;
        }

        public int hashCode()
        {
            if (hashCode == -1)
            {
                int h = 0;
                int vh = (0xFF & vendorId[0]) | (0xFF00 & vendorId[1] << 8)
                    | (0xFF0000 & vendorId[2] << 16) | (vendorId[3] << 24);
                h = ((31 * h) + ((int) (vh ^ (vh >> 32))));
                h = ((31 * h) + ((int) (subSelector ^ (subSelector >> 32))));
                h = ((31 * h) + ((int) (version ^ (version >> 32))));
                hashCode = h;
            }
            return hashCode;
        }
    }
}