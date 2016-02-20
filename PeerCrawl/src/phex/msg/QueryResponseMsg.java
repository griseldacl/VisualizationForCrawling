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
 */
package phex.msg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;

import phex.common.ServiceManager;
import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.connection.NetworkManager;
import phex.host.HostManager;
import phex.query.QueryConstants;
import phex.upload.UploadManager;
import phex.utils.*;
import phex.xml.XMLUtils;

/**
 * <p>A response to a query message.</p>
 *
 * <p>These should have message IDs matching the query message it is responding
 * to.</p>
 *
 * <p>This class does implement the EQHD extention. It also support the GGEP
 * extension.
 * </p>
 */
public class QueryResponseMsg extends Message implements QueryConstants
{
    private static final boolean INCLUDE_QHD = true;

    /**
     * push flag mask for beashare metadata.
     */
    private static final byte PUSH_NEEDED_MASK=(byte)0x01;
    /**
     * busy flag mask for beashare metadata.
     */
    private static final byte SERVER_BUSY_MASK=(byte)0x04;
    /**
     * upload flag mask for beashare metadata.
     */
    private static final byte HAS_UPLOADED_MASK=(byte)0x08;
    /**
     * speed flag mask for beashare metadata.
     */
    private static final byte UPLOAD_SPEED_MASK=(byte)0x10;
    /**
     * ggep flag mask for QHD.
     */
    private static final byte GGEP_MASK=(byte)0x20;
    /**
     * chat flag mask for limewire and shareaza metadata.
     */
    private static final byte CHAT_SUPPORTED_MASK = (byte)0x01;

    /**
     * <p>The un-parsed body of the query response.</p>
     *
     * <p>For queries that are being forwarded, this body will include all extra
     * data. For queries built using this API, there is no way currently to add
     * extra information to this body.</p>
     */
    private byte[] body;
    
    /**
     * Query response records.
     */
    private QueryResponseRecord[] records;
    
    /**
     * Reported speed of the host
     */
    private long remoteHostSpeed;

    /**
     * The host address for the query response
     */
    private DestAddress destAddress;

    private GUID remoteClientID;

    /**
     * Defines the four character vendor code of the client that offers the
     * file.
     */
    private String vendorCode;

    /**
     * <p>Defines if a push transfer is needed or not or unknown.</p>
     *
     * <p><em>fixme:</em> Why not use a boolean? Same goes for other flags.</p>
     */
    private short pushNeededFlag;

    /**
     * Defines if a server is busy currently or unknown.
     */
    private short serverBusyFlag;

    /**
     * Defines if a the server has already uploaded a file.
     */
    private short hasUploadedFlag;

    /**
     * Defines if the upload speed of a server.
     */
    private short uploadSpeedFlag;

    /**
     * Defines if the server supportes chat.
     */
    private boolean isChatSupported;

    /**
     * Defines if the server supports browse host.
     */
    private boolean isBrowseHostSupported;
    
    /**
     * The push proxy addresses for the GGEP extension of this query 
     * response.
     */
    private DestAddress[] pushProxyAddresses;

    /**
     * Defines if the body of the query response is already parsed.
     */
    private boolean isParsed;

    /**
     * Build a new MsgQueryResponse with a header, a client GUID, host address,
     * port and speed, and an array of MsgResRecord instances representing hits.
     *
     * @param header  the MsgHeader to attach, which will have its function
     *                property set to MsgHeader.sQueryResponse
     * @param clientID     the GUID of the client that requested the query
     * @param destAddress  the InetAddress of the responding servent
     * @param hostPort     the port as short of the responding servent
     * @param speed        the speed of the responding servent
     * @param records      an array of MsgResRecord objects representing hits to
     *                     the query which must be shorter than 256
     * @throws IllegalArgumentException if there are more than 255 records
     * @throws UnknownHostException if host ip address cant be resolved.
     */
    public QueryResponseMsg( MsgHeader header, GUID clientID,
        DestAddress aHostAddress, int speed, QueryResponseRecord records[] )
    {
        super( header );
        if( records.length > 255 )
        {
            throw new IllegalArgumentException(
                "A maximum of 255 records can be associated with a single " +
                "response: " + records.length );
        }

        getHeader().setPayloadType(MsgHeader.QUERY_HIT_PAYLOAD);

        remoteClientID = clientID;
        destAddress = aHostAddress;
        remoteHostSpeed = speed;
        this.records = records;
        
        pushProxyAddresses = HostManager.getInstance().
            getNetworkHostsContainer().getPushProxies();

        // when we are behind firewall or have not accepted an incoming connection
        // like defined in the protocol
        boolean isPushNeeded;
        if ( NetworkManager.getInstance().hasConnectedIncoming() )
        {
            isPushNeeded = false;
            pushNeededFlag = QHD_FALSE_FLAG;
        }
        else
        {
            isPushNeeded = true;
            pushNeededFlag = QHD_TRUE_FLAG;
        }

        boolean isServerBusy = UploadManager.getInstance().isHostBusy();

        try
        {
            buildBody( isPushNeeded, isServerBusy );
        }
        catch (IOException e)
        {// should never happen
            NLogger.error(NLoggerNames.MESSAGE_ENCODE_DECODE, e, e);
        }
        getHeader().setDataLength( body.length );
        isParsed = true;
    }

    /**
     * <p>Create a query response with its header and body.</p>
     *
     * <p>The header becomes owned by this message. Its function property will
     * be set to MsgHeader.sQueryResponse.</p>
     *
     * <p>The body is not parsed directly
     * cause some queries are just forwarded without the need of beeing completely
     * parsed. This allows the extention data (such as GGEP blocks) to be
     * forwarded despite there being no API to modify these.</p>
     *
     * @param header  the MsgHeader to use as header
     * @throws InvalidMessageException
     */
    public QueryResponseMsg( MsgHeader header, byte[] aBody )
    	throws InvalidMessageException
    {
        super( header );
        getHeader().setPayloadType(MsgHeader.QUERY_HIT_PAYLOAD);

        body = aBody;
        header.setDataLength( body.length );
        
        // validate port
        int port = IOUtil.unsignedShort2Int( IOUtil.deserializeShortLE( body, 1 ) );
        destAddress = new DefaultDestAddress( getHostIP(), port );
        if ( !destAddress.isValidAddress() )
        {
            throw new InvalidMessageException( "Invalid address: " + destAddress );
        }
        
        isParsed = false;
    }

    private void buildBody( boolean isPushNeeded, boolean isServerBusy )
        throws IOException
    {
        ByteArrayOutputStream bodyStream = new ByteArrayOutputStream( );
        int recordCount = records.length;
        bodyStream.write( (byte) recordCount );
        
        IOUtil.serializeShortLE( (short)destAddress.getPort(), bodyStream );
        byte[] ipAddress = destAddress.getIpAddress().getHostIP();
        bodyStream.write( ipAddress );
        IOUtil.serializeIntLE( (int)remoteHostSpeed, bodyStream );

        for (int i = 0; i < recordCount; i++)
        {
            records[ i ].write( bodyStream );
        }

        if ( INCLUDE_QHD )
        {
            // add vendor code 'PHEX'
            bodyStream.write( (byte) 0x50 );
            bodyStream.write( (byte) 0x48 );
            bodyStream.write( (byte) 0x45 );
            bodyStream.write( (byte) 0x58 );
            // open data length
            bodyStream.write( (byte) 2 );
            // open data flags
            byte isPushNeededByte = (byte) 0;
            if ( isPushNeeded )
            {
                isPushNeededByte = PUSH_NEEDED_MASK;
            }
            byte isServerBusyByte = (byte) 0;
            if ( isServerBusy )
            {
                isServerBusyByte = SERVER_BUSY_MASK;
            }
            byte isGGEPUsedByte = (byte)0;
            if ( ServiceManager.sCfg.mShareBrowseDir 
              || ( pushProxyAddresses != null && pushProxyAddresses.length > 0 ) )
            {
                isGGEPUsedByte = GGEP_MASK;
            }

            bodyStream.write( (byte) (
                  isPushNeededByte
                | SERVER_BUSY_MASK
                | 0 //HAS_UPLOADED_MASK we dont know that yet
                // we know we never measured that speed
                | UPLOAD_SPEED_MASK
                | GGEP_MASK ) );
            bodyStream.write( (byte) (
                  PUSH_NEEDED_MASK
                | isServerBusyByte
                | 0 //(hasUploadedSuccessfully ? HAS_UPLOADED_MASK : 0)
                // we know we never measured that speed
                | 0 //(isSpeedMeasured ? UPLOAD_SPEED_MASK : 0));
                | isGGEPUsedByte ) );

            // private QHD area
            // mark for chat able.
            if ( ServiceManager.sCfg.isChatEnabled )
            {
                bodyStream.write( (byte) 0x01 );
            }
            else
            {
                bodyStream.write( (byte) 0x00 );
            }

            //GGEP block
            byte[] ggepBytes = GGEPBlock.getQueryReplyGGEPBlock( 
                ServiceManager.sCfg.mShareBrowseDir, pushProxyAddresses );
            if ( ggepBytes.length > 0 )
            {
                bodyStream.write( ggepBytes );
            }
        }
        remoteClientID.write( bodyStream );
        
        body = bodyStream.toByteArray();
    }

    /**
     * <p>Get the number of response records attached to this response.</p>
     *
     * <p><em>fixme:</em> This should be a byte, not an int. It must be within
     * the range 0-255.</p>
     */
    public short getRecordCount()
    {
        // instead of parsing the complete body we will only read out the
        // record length. This is much more performant.
        return (short)IOUtil.unsignedByte2int( body[0] );
    }

    /**
     * Returns the IP address of the host as a byte array.
     * The body is not parsed to get this data.
     */
    public byte[] getHostIP()
    {
        byte[] ip = new byte[4];
        // the ip starts at byte 3
        ip[0] = body[3];
        ip[1] = body[4];
        ip[2] = body[5];
        ip[3] = body[6];
        return ip;
    }

    public DestAddress getDestAddress()
    {
        return destAddress;
    }

    /**
     * Get the speed of the remote servent in kbits/sec.
     *
     * @return the remote host speed
     */
    public long getRemoteHostSpeed()
    {
        parseBody();
        return remoteHostSpeed;
    }

    /**
     * <p>If present, return the vendor code of the servent, otherwise null.</p>
     *
     * <p>For example, responses arrising from phex will have a vendor code of
     * "PHEX".</p>
     *
     * @return the vendor code
     */
    public String getVendorCode()
    {
        parseBody();
        return vendorCode;
    }

    /**
     * <p>States wether the servent will require push to fetch data.</p>
     *
     * <p>A servent will require push if it can not accept TCP connections to
     * fetch the matching resource because, for example, it is the other side of
     * a fire wall.</p>
     * Since this field can hold three states ( QHD_TRUE_FLAG, QHD_FALSE_FLAG,
     * QHD_UNKNOWN_FLAG ) and all three states are evaluated a boolean can't be used.
     *
     * @return a short that will be one of the QueryConstants QHD_UNKNOWN_FLAG,
     *         QHD_TRUE_FLAG or QHD_FALSE_FLAG
     */
    public short getPushNeededFlag()
    {
        parseBody();
        return pushNeededFlag;
    }

    /**
     * <p>States wether the servent is currently buisy.</p>
     *
     * <p>A servent is buisy if all available upload slots are filled.</p>
     *
     * Since this field can hold three states ( QHD_TRUE_FLAG, QHD_FALSE_FLAG,
     * QHD_UNKNOWN_FLAG ) and all three states are evaluated a boolean can't be used.
     *
     * @return a short that will be one of the QueryConstants QHD_UNKNOWN_FLAG,
     *         QHD_TRUE_FLAG or QHD_FALSE_FLAG
     */
    public short getServerBusyFlag()
    {
        parseBody();
        return serverBusyFlag;
    }

    /**
     * <p>States wether the servent has ever successfuly uploaded any file.</p>
     *
     * Since this field can hold three states ( QHD_TRUE_FLAG, QHD_FALSE_FLAG,
     * QHD_UNKNOWN_FLAG ) and all three states are evaluated a boolean can't be used.
     *
     * @return a short that will be one of the QueryConstants QHD_UNKNOWN_FLAG,
     *         QHD_TRUE_FLAG or QHD_FALSE_FLAG
     */
    public short getHasUploadedFlag()
    {
        parseBody();
        return hasUploadedFlag;
    }

    /**
     * <p>States wether the servent calculates its upload speed as the highest
     * average upload speed of the last 10 uploads, or as a user-defined value.
     * </p>
     * Since this field can hold three states ( QHD_TRUE_FLAG, QHD_FALSE_FLAG,
     * QHD_UNKNOWN_FLAG ) and all three states are evaluated a boolean can't be used.
     *
     * @return a short that will be one of the QueryConstants QHD_UNKNOWN_FLAG,
     *         QHD_TRUE_FLAG or QHD_FALSE_FLAG
     */
    public short getUploadSpeedFlag()
    {
        parseBody();
        return uploadSpeedFlag;
    }

    /**
     * <p>States wether the servent supportes chat connections.</p>
     *
     * @return true if a servent supports chat connections false otherwise.
     */
    public boolean isChatSupported()
    {
        parseBody();
        return isChatSupported;
    }

    /**
     * <p>States wether the servent supportes browse host.</p>
     *
     * @return true if a servent supports browse host connections false otherwise.
     */
    public boolean isBrowseHostSupported()
    {
        parseBody();
        return isBrowseHostSupported;
    }
    
    /**
     * Returns the collected PushProxy addresses from the GGEP
     * extension or null if no addresses are found.
     * @return push proxy addresses or null.
     */
    public DestAddress[] getPushProxyAddresses()
    {
        parseBody();
        return pushProxyAddresses;
    }

    /**
     * Get the GUID of the remote servent.
     *
     * @return the GUID of the remote client
     */
    public GUID getRemoteClientID()
    {
        if ( remoteClientID == null )
        {
            parseRemoteClientID();
        }
        return remoteClientID;
    }

    /**
     * Get the i'th MsgResRecord that encapsulates the i'th hit to the query in
     * this response.
     *
     * @param i  the index of the record to fetch
     * @throws IndexOutOfBoundsException  if i is negative or not less than
     *         getRecordCount()
     */
    public QueryResponseRecord getMsgRecord( int i )
    {
        parseBody();
        return records[ i ];
    }

    /**
     * Make an independant copy of a MsgQueryResponse into this message.
     *
     * @param b  the MsgQueryResponse to copy all data from
     */
    public void copy(QueryResponseMsg b)
    {
        getHeader().copy(b.getHeader());
        destAddress = b.destAddress;
        remoteHostSpeed = b.remoteHostSpeed;
        remoteClientID = b.remoteClientID;
        int recordCount = b.records.length;
        records = new QueryResponseRecord[ recordCount ];
        for (int i = 0; i < recordCount; i++)
        {
            QueryResponseRecord rec = new QueryResponseRecord();
            rec.copy( b.records[ i ] );
            records[ i ] = rec;
        }
        body = b.body;
    }
    
    public void writeMessage( GnutellaOutputStream outStream )
        throws IOException
    {
        getHeader().writeHeader( outStream );
        outStream.write( body, 0, body.length );
    }

    private void parseBody()
    {
        if ( isParsed )
        {
            return;
        }
        // Already read the header.
        int offset = 0;
        byte n = body[offset++];
        short recordCount = (short)(n < 0 ? 256 + n : n);

        // parsing of port and ip is done on demand in getHostAddress() and
        // getHostIP()
        offset += 2; // skip port
        offset += 4; // skip ip

        try
        {
            long speed = IOUtil.unsignedInt2Long( IOUtil.deserializeIntLE(body, offset) );
            remoteHostSpeed = speed;
            offset += 4;

            records = new QueryResponseRecord[ recordCount ];
            for (int i = 0; i < recordCount; i++)
            {
                QueryResponseRecord rec = new QueryResponseRecord();
                offset = rec.deserialize(body, offset);
                records[ i ] = rec;
            }

            // Handle Bearshare meta informations. The format is documented in
            // the GnutellaProtocol04.pdf document
            pushNeededFlag = QHD_UNKNOWN_FLAG;
            serverBusyFlag = QHD_UNKNOWN_FLAG;
            hasUploadedFlag = QHD_UNKNOWN_FLAG;
            uploadSpeedFlag = QHD_UNKNOWN_FLAG;
            // GGEP extensions
            isBrowseHostSupported = false;

            if ( offset <= (getHeader().getDataLength() - 16 - 4 - 2) )
            {
                // parse meta data
                // Use ISO encoding for two bytes characters on some platforms.
                vendorCode = new String( body, offset, 4, "ISO-8859-1");
                if ( !isVendorCodeValid( vendorCode ) )
                {
                    String hexVendorCode = HexConverter.toHexString( body, offset, 4 );
                    Logger.logMessage( Logger.WARNING, Logger.NETWORK, 
                        getHeader().getFromHost(),
                        "Illegal QHD vendor code found: " + vendorCode + " ("
                        + hexVendorCode + "). Body: " +
                        HexConverter.toHexString( body ) );
                    vendorCode = hexVendorCode;
                }
                offset += 4;

                int openDataLength = IOUtil.unsignedByte2int( body[ offset ] );
                offset += 1;

                // parse upload speed, have uploaded, busy and push
                if ( openDataLength > 1)
                {   // if we have a flag byte
                    byte flag1 = body[ offset ];
                    byte flag2 = body[ offset + 1];

                    // check if push flag is meaningfull do it reversed from other checks
                    if ( ( flag2 & PUSH_NEEDED_MASK ) != 0 )
                    {
                        if ( ( flag1 & PUSH_NEEDED_MASK ) != 0 )
                        {
                            pushNeededFlag = QHD_TRUE_FLAG;
                        }
                        else
                        {
                            pushNeededFlag = QHD_FALSE_FLAG;
                        }
                    }

                    // check if server busy flag meaningfull
                    if ((flag1 & SERVER_BUSY_MASK) != 0)
                    {
                        if ( (flag2 & SERVER_BUSY_MASK) != 0 )
                        {
                            serverBusyFlag = QHD_TRUE_FLAG;
                        }
                        else
                        {
                            serverBusyFlag = QHD_FALSE_FLAG;
                        }
                    }

                    // check if the uploaded flag is meaningfull
                    if ((flag1 & HAS_UPLOADED_MASK) != 0)
                    {
                        if ( (flag2 & HAS_UPLOADED_MASK) != 0 )
                        {
                            hasUploadedFlag = QHD_TRUE_FLAG;
                        }
                        else
                        {
                            hasUploadedFlag = QHD_FALSE_FLAG;
                        }
                    }
                    if ((flag1 & UPLOAD_SPEED_MASK) != 0 )
                    {
                        if ( (flag2 & UPLOAD_SPEED_MASK) != 0 )
                        {
                            uploadSpeedFlag = QHD_TRUE_FLAG;
                        }
                        else
                        {
                            uploadSpeedFlag = QHD_FALSE_FLAG;
                        }
                    }
                    if ((flag1 & GGEP_MASK) != 0 && (flag2 & GGEP_MASK) !=0 )
                    {// parse GGEP area should follow after open data area but
                     // we can't be sure...
                        int ggepMagicIndex = offset + 2;
                        // search for real magic index
                        while ( ggepMagicIndex < body.length )
                        {
                            if ( body[ ggepMagicIndex ] == GGEPBlock.MAGIC_NUMBER )
                            {
                                // found index!
                                break;
                            }
                            ggepMagicIndex ++;
                        }
                        GGEPBlock[] ggepBlocks = null;
                        try
                        {
                            // if there are GGEPs, see if Browse Host supported...
                            ggepBlocks = GGEPBlock.parseGGEPBlocks( body, ggepMagicIndex );
                            isBrowseHostSupported = GGEPBlock.isExtensionHeaderInBlocks(
                                ggepBlocks, GGEPBlock.BROWSE_HOST_HEADER_ID );
                            pushProxyAddresses = GGEPExtension.parsePushProxyExtensionData( ggepBlocks );
                        }
                        catch ( InvalidGGEPBlockException exp )
                        {// ignore and continue parsing...
                            Logger.logMessage( Logger.FINE, Logger.NETWORK, exp );
                        }
                    }
                }
                // skip unknown open data length
                offset += openDataLength;

                //Parse private area of Limewire and Shareaza to read out chat
                //flag. If chatflag is 0x1 chat is supported, if 0x0 its not.
                int privateDataLength = body.length - offset - 16;
                if ( privateDataLength > 0 &&
                   ( vendorCode.equals("LIME") || vendorCode.equals("RAZA")
                  || vendorCode.equals("PHEX") ) )
                {
                    byte flag = body[ offset ];
                    isChatSupported = ( flag & CHAT_SUPPORTED_MASK ) != 0;
                }

                //System.out.println( (mHeader.getDataLen() -16 - 4 -2) + "  " + offset + "  " +
                //    /*new String( body, offset, mHeader.getDataLen() - offset) + "   " +*/
                //    openDataLength + "  " + pushNeededFlag + "  " + serverBusyFlag + "  " +
                //    uploadSpeedFlag + "  " + hasUploadedFlag + "  " + vendorCode );
            }
            parseRemoteClientID();
            isParsed = true;
        }
        catch ( java.io.UnsupportedEncodingException exp )
        {
            return;
        }
    }

    private void parseRemoteClientID()
    {
        if ( remoteClientID == null )
        {
            remoteClientID = new GUID();
        }
        remoteClientID.deserialize(body, getHeader().getDataLength() - GUID.DATA_LENGTH);
    }

    private boolean isVendorCodeValid( String vendorCode )
    {
        // verify length
        if ( vendorCode.length() != 4 )
        {
            return false;
        }
        // verify characters
        for ( int i = 0; i < 4; i++ )
        {
            if ( !XMLUtils.isXmlChar( vendorCode.charAt( i ) ) )
            {
                return false;
            }
        }
        return true;
    }
}
