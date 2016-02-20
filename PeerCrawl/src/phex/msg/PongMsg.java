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
 *  $Id: PongMsg.java,v 1.4 2005/11/13 10:28:50 gregork Exp $
 */
package phex.msg;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.zip.DataFormatException;

import phex.common.IntObj;
import phex.common.address.*;
import phex.connection.NetworkManager;
import phex.host.*;
import phex.net.presentation.PresentationManager;
import phex.security.PhexSecurityManager;
import phex.share.ShareManager;
import phex.share.SharedFilesService;
import phex.statistic.StatisticProvider;
import phex.statistic.StatisticsManager;
import phex.udp.hostcache.UdpHostCache;
import phex.udp.hostcache.UdpHostCacheManager;
import phex.utils.*;

/**
 * <p>A pong response.</p>
 *
 * <p>This encapsulates a Gnutella message that informs this servent of the
 * vital statistics of a Gnutella host. Pongs should  only ever be received
 * in response to pings.</p>
 *
 * <p>This implementation handles GGEP extension blocks.</p>
 */
public class PongMsg extends Message
{
	private static final int MIN_PONG_DATA_LEN = 14;
	
    /**
     * Vendor code GGEP extension in GUESS format
     */
    private static final byte[] GGEP_VENDOR_CODE = new byte[5];
    static
    {
        // add vendor code 'PHEX'
        GGEP_VENDOR_CODE[ 0 ] = (byte) 0x50;
        GGEP_VENDOR_CODE[ 1 ] = (byte) 0x48;
        GGEP_VENDOR_CODE[ 2 ] = (byte) 0x45;
        GGEP_VENDOR_CODE[ 3 ] = (byte) 0x58;
        GGEP_VENDOR_CODE[4] = IOUtil.serializeGUESSVersionFormat(
            VersionUtils.getMajorVersionNumber(),
            VersionUtils.getMinorVersionNumber() );
    }
    
    /**
     * <p>The un-parsed body of the message.</p>
     */
    private byte[] body;

    private int port;
    private byte[] ip;
    private long fileCount;
    private long fileSizeInKB;
    private int avgDailyUptime;
    private boolean isUltrapeer;
    private GGEPBlock ggepBlock;
    
    /**
     * holds the information that can appear in udp pongs
     */
    private Collection ipPortPairs;
    private Collection packedUdpHostCaches;
    private UdpHostCache udpCache;

    public PongMsg( MsgHeader aHeader, byte[] payload )
    {
        super( aHeader );
        getHeader().setPayloadType( MsgHeader.PONG_PAYLOAD );

        body = payload;
        getHeader().setDataLength( body.length );
        
        avgDailyUptime = -1;

        // parse the body
        parseBody();
    }

    /**
     * <p>Create a new MsgInitResponse.</p>
     *
     * <p>The header will be modified so that its function property becomes
     * MsgHeader.sInitResponse. The header argument is owned by this object.</p>
     *
     * @param header  the MsgHeader to associate with the new message
     */
    private PongMsg( MsgHeader header, byte[] ip, int port, int fileCount,
        int fileSizeInKB, boolean isUltrapeer, GGEPBlock ggepBlock )
    {
        super( header );
        getHeader().setPayloadType( MsgHeader.PONG_PAYLOAD );

        if( ip.length != 4 )
        {
            throw new IllegalArgumentException(
                "Can't accept ip that is not 4 bytes in length: " +
                ip.length );
        }
        this.ip = ip;
        this.port = port;
        this.fileCount = fileCount;
        this.isUltrapeer = isUltrapeer;
        this.ggepBlock = ggepBlock;
        if ( isUltrapeer )
        {
            this.fileSizeInKB = createUltrapeerMarking( fileSizeInKB );
        }
        else
        {
            this.fileSizeInKB = fileSizeInKB;
        }
        buildBody();
        getHeader().setDataLength( body.length );
    }

    /**
     * Get the port that the remote host will listen on.
     *
     * @return  the current port number as a short
     */
    public int getPort()
    {
        return port;
    }

    /**
     * <p>Get the four byte image of the ip address of the Gnutella servent.</p>
     *
     * <p><em>Important:</em> Do not modify the return value. It is owned by
     * this object.</p>
     *
     * @return the four byte image of this servent's ip address
     */
    public byte[] getIP()
    {
        return ip;
    }

    /**
     * Get the number of files served from this servent.
     *
     * @return  a zero or positive integer giving the number of files served
     */
    public long getFileCount()
    {
        return fileCount;
    }

    /**
     * Get the number of bytes served by this servent.
     *
     * @return  the number of bytes served
     */
    public long getFileSizeInKB()
    {
        return fileSizeInKB;
    }
    
    /**
     * @return
     */
    public int getDailyUptime()
    {
        return avgDailyUptime;
    }

    public void writeMessage( GnutellaOutputStream outStream )
        throws IOException
    {
        getHeader().writeHeader( outStream );
        outStream.write( body, 0, body.length );
    }
    
    public byte[] getbytes()
    {
        byte[] data = new byte[ MsgHeader.DATA_LENGTH + body.length ];
        byte[] hdr = getHeader().getBytes();
        System.arraycopy( hdr, 0, data, 0, MsgHeader.DATA_LENGTH );
        System.arraycopy( body, 0, data, MsgHeader.DATA_LENGTH , body.length );
        return data;
    }

    public String getDebugString()
    {
        return "Pong[ IP=" + AddressUtils.ip2string( ip ) +
            ", Port=" + port +
            ", FileCount=" + fileCount +
            ", FileSize=" + fileSizeInKB +
            ", AvgUptime=" + avgDailyUptime +
            ", HEX=[" + HexConverter.toHexString( body ) +
            "] ]";
    }

    private void buildBody()
    {
        byte[] ggepExtension;
        int extensionLength;
        if ( ggepBlock != null )
        {
            try
            {
                ggepExtension = ggepBlock.getBytes();
                extensionLength = ggepExtension.length;
            }
            catch ( IOException exp )
            {
                NLogger.error( NLoggerNames.MESSAGE_ENCODE_DECODE, exp, exp );
                extensionLength = 0;
                ggepExtension = null;
            }
        }
        else
        {
            ggepExtension = null;
            extensionLength = 0;
        }
        
        body = new byte[ 14 + extensionLength ];
        IOUtil.serializeShortLE( (short)port, body, 0 );
        System.arraycopy( ip, 0, body, 2, 4 );
        IOUtil.serializeIntLE( (int)fileCount, body, 6);
        IOUtil.serializeIntLE( (int)fileSizeInKB, body, 10);
        if ( ggepExtension != null )
        {
            System.arraycopy( ggepExtension, 0, body, 14, extensionLength );
        }   
    }

    private void parseBody()
    {
        port = IOUtil.unsignedShort2Int(IOUtil.deserializeShortLE( body, 0 ));

        ip = new byte[4];
        ip[0] = body[2];
        ip[1] = body[3];
        ip[2] = body[4];
        ip[3] = body[5];

        fileCount = IOUtil.unsignedInt2Long( IOUtil.deserializeIntLE( body, 6 ) );
        fileSizeInKB = IOUtil.unsignedInt2Long( IOUtil.deserializeIntLE( body, 10 ) );
        
        // parse possible GGEP data
        if ( body.length <= 14 )
        {
            return;
        }
        
        parseGGEPBlocks();
    }
    
    private void parseGGEPBlocks()
    {
        GGEPBlock[] ggepBlocks;
        try
        {
            ggepBlocks = GGEPBlock.parseGGEPBlocks( body, 14 );
        }
        catch ( InvalidGGEPBlockException exp )
        {// ignore and continue parsing...
            NLogger.warn( NLoggerNames.MESSAGE_ENCODE_DECODE, exp );
            return;
        }
        
        if ( GGEPBlock.isExtensionHeaderInBlocks( ggepBlocks, GGEPBlock.AVARAGE_DAILY_UPTIME ) )
        {
            byte[] data = GGEPBlock.getExtensionDataInBlocks( ggepBlocks, GGEPBlock.AVARAGE_DAILY_UPTIME );
            if ( data != null )
            {
                try
                {
                    avgDailyUptime = IOUtil.deserializeIntLE( data, 0, data.length );
                }
                catch ( NumberFormatException exp )
                {
                    NLogger.warn( NLoggerNames.MESSAGE_ENCODE_DECODE, "Invalid average uptime GGEP extension data: " +
                        HexConverter.toHexString( data ), exp );
                    avgDailyUptime = -1;
                }
                catch ( IllegalArgumentException exp )
                {
                    // 5 bytes invalid average uptime infomation are most likley comming from Bearshare
                    NLogger.warn( NLoggerNames.MESSAGE_ENCODE_DECODE, "Invalid average uptime GGEP extension data: " +
                        HexConverter.toHexString( data ) );
                    avgDailyUptime = -1;
                }
            }
        }
        // checks for the UP ggep extension
        // if its present and the host has free slots its added to the respective free slots container 
        // in NetworkHostContainer ....as of now we ignore the actual count of the slots available        
        
        if ( GGEPBlock.isExtensionHeaderInBlocks( ggepBlocks, GGEPBlock.ULTRAPEER_ID ))
        {
            byte[] data = GGEPBlock.getExtensionDataInBlocks( ggepBlocks, GGEPBlock.ULTRAPEER_ID );
            if ( data != null )
            {
            	if( data.length >= 3 )
            	{
            		NetworkHostsContainer hostsContainer = HostManager.getInstance().getNetworkHostsContainer();
            		Host thisHost = this.getHeader().getFromHost();
            		if( data[1] > 0 )
            		{
            			hostsContainer.addToFreeLeafSlotSet( thisHost );
            		}
            		if( data[2] > 0 )
            		{
            			hostsContainer.addToFreeUltrapeerSlotSet( thisHost );
            		}
            	}
            }
        }
        
        // try to get any ip port pairs which may be present in udp pongs 
        if ( GGEPBlock.isExtensionHeaderInBlocks( ggepBlocks, GGEPBlock.UDP_HOST_CACHE_IPP ) )
        {
            byte[] data = GGEPBlock.getExtensionDataInBlocks( ggepBlocks, GGEPBlock.UDP_HOST_CACHE_IPP );
            if ( data != null )
            {
                try
                {
                    ipPortPairs = unpackIpPortData( data );
                    processIpPortPairs();
                }
                catch ( InvalidGGEPBlockException e )
                {
                    //ignore and continue parsing...
                    NLogger.warn( NLoggerNames.MESSAGE_ENCODE_DECODE, e );
                }
            }
        }
        
        // try to get any packed udp host cache list which may be present in udp pongs
        if( GGEPBlock.isExtensionHeaderInBlocks( ggepBlocks, GGEPBlock.UDP_HOST_CACHE_PHC))
        {
            byte[] data = GGEPBlock.getExtensionDataInBlocks( ggepBlocks, GGEPBlock.UDP_HOST_CACHE_PHC );
            if ( data != null )
            {
                try
                {
                    data = IOUtil.inflate( data );
                    String packedData = new String( data );
                    parsePackedHostCache( packedData );
                    processPackedHostCaches();
                }
                catch (DataFormatException exp)
                {
                    NLogger.error(NLoggerNames.UDP_INCOMING_MESSAGES, exp, exp);
                }
            }
        }
        
        //check if the sending host is a udp host cache n add to
        //the  functional udp host cache container
        if( GGEPBlock.isExtensionHeaderInBlocks( ggepBlocks, GGEPBlock.UDP_HOST_CACHE_UDPHC))
        {
            byte[] data = GGEPBlock.getExtensionDataInBlocks( ggepBlocks, GGEPBlock.UDP_HOST_CACHE_UDPHC );
            try
            {
                DestAddress address;
                boolean dnsNameAvailable = false;
                if ( data != null )
                {
                	if( data.length > 0 )
                	{
                		dnsNameAvailable = true;
                	}
                }
                if( dnsNameAvailable )
                {
                	address = new DefaultDestAddress( new String( data ), port );
                }
                else
                {
                    address = new DefaultDestAddress( ip, port );
                }
                //add to the container
                udpCache = new UdpHostCache( address );
                NLogger.info( NLoggerNames.UDP_INCOMING_MESSAGES, " Found a Udp " +
                        "Host Cache : " + udpCache );
                UdpHostCacheManager udpHostCacheMgr = UdpHostCacheManager.getInstance();
                udpHostCacheMgr.getUdpHostCacheContainer().addFunctionalCache( udpCache );                
            }
            catch( IllegalArgumentException e)
            {
                NLogger.warn( NLoggerNames.UDP_HOST_CACHE, "INVALID Udp Host Cache found " +
                		"and ignored " + e);                
            }
        }        
                
    }


    /**
     * gets the list of udp host cache addresses from the 
     * packed host cache ggep extension
     * @param packedHostCaches
     */
    private void parsePackedHostCache( String packedHostCaches )
    {
        PresentationManager netPresMgr = PresentationManager.getInstance();
        String[] hostCaches = packedHostCaches.split( "\n" );
        packedUdpHostCaches = new HashSet( hostCaches.length );
        
        for( int i=0; i < hostCaches.length; i++ )
        {
            //find the position of the first key/value pair if any
            int pos = hostCaches[i].indexOf( "&" );
            try
            {
                DestAddress address;
                //no key/value pair found
                if( pos == -1)
                {
                    address = netPresMgr.createHostAddress(hostCaches[i], IpAddress.DEFAULT_PORT);
                }
                else 
                {
                    //key/value pair found, but just ignore as of now
                    String temp = hostCaches[i].substring( 0, pos );
                    address = netPresMgr.createHostAddress(temp, IpAddress.DEFAULT_PORT);
                }
                UdpHostCache cache = new UdpHostCache( address );
                packedUdpHostCaches.add( cache );
            }
            catch ( MalformedDestAddressException e ) 
            {
                // just ignore and continue with next string
                NLogger.warn( NLoggerNames.UDP_HOST_CACHE, " Ignored " +
                		"One Host Cache address in a packed host cache list  "
                        + e
                        );
                continue;
            }
        }        
    }
    
    /**
     * adds the list of phc's into the general udp host cache container
     */
    private void processPackedHostCaches()
    {
        if( (packedUdpHostCaches == null) || (packedUdpHostCaches.size() == 0 ) )
        {
            //nothing to do
            return;
        }
        
        UdpHostCacheManager udpHostMgr = UdpHostCacheManager.getInstance();
        for( Iterator phc = packedUdpHostCaches.iterator(); phc.hasNext(); )
        {
            UdpHostCache cache = ( UdpHostCache )phc.next();
            udpHostMgr.getUdpHostCacheContainer().addCache( cache );
            NLogger.info( NLoggerNames.UDP_INCOMING_MESSAGES, " Addded a udp host cache from a " +
                    "Packed Host Cache Container  : " + cache ); 
        }
    }
    
    /**
     * unpack ip port data from a data array
     * its length should be atleast 6 bytes 
     * ip : 4 bytes and port : 2 bytes
     * @throws InvalidGGEPBlockException
     * @author Madhu
     *
     */
    private Collection unpackIpPortData( byte[] data )
    	throws InvalidGGEPBlockException
    {
        final int FIELD_SIZE = 6;
        
        if (data.length % FIELD_SIZE != 0)
    		throw new InvalidGGEPBlockException("invalid IPPORT EXTENSION DATA IN PONG");
    	
    	int size = data.length/FIELD_SIZE;    	
    	Collection ipPortCollection = new ArrayList( size );
    	int index;
    	
    	for (int i=0; i<size; i++) 
    	{
    	    index = i*FIELD_SIZE;
    	    byte[] ip = new byte[4];
    	    System.arraycopy(data, index, ip, 0, 4);
    		int port = IOUtil.unsignedShort2Int(
    		        IOUtil.deserializeShortLE( data, index + 4 )
    		        );
    		try
            {
     		   PhexSecurityManager securityManager = PhexSecurityManager.getInstance();
                byte access = securityManager.controlHostIPAccess( ip );
                if ( access == PhexSecurityManager.ACCESS_GRANTED ) 
                {
                    byte[] HostIp = ip;                    
                    DestAddress current = new DefaultDestAddress( HostIp, port );
                    if( !(ipPortCollection.contains(current)) )
                    {
                        ipPortCollection.add( current );	
                        NLogger.debug( NLoggerNames.UDP_INCOMING_MESSAGES, 
                                " Found ip port Host : " + current   
                            	);
                    }
                }
            }
            catch ( Exception e )
            {
                //just ignore and move on
                continue;
            }    		   
    	}
    	
    	return ipPortCollection;
    }

    /**
     * packs ip port data into a data array
     * @param ipPortCollection
     * @return ip port byte array 
     * @throws UnknownHostException
     */    
    private static byte[] packIpPortData( Collection ipPortCollection )
    {
        final int FIELD_SIZE = 6;
        byte[] data = new byte[ipPortCollection.size() * FIELD_SIZE];
        int offset = 0;
        
        for( Iterator i = ipPortCollection.iterator(); i.hasNext(); ) 
        {
            Host next = ( Host)i.next();
            DestAddress address = next.getHostAddress();
            byte[] addr = address.getIpAddress().getHostIP();
            int port = address.getPort();
            System.arraycopy(addr, 0, data, offset, 4);
            offset += 4;
            IOUtil.serializeShortLE( (short)port, data, offset);
            offset += 2;
        }
        return data;    
    }
    
    /**
     * Processes the ip port information and adds the correspnding
     * hosts to the caught host container with high priority     *
     */
    private void processIpPortPairs()
    {
        if( ipPortPairs == null || ipPortPairs.size() == 0)
        {
            return;
        }
        
        CaughtHostsContainer container =
            HostManager.getInstance().getCaughtHostsContainer();
        
        for( Iterator ipPort = ipPortPairs.iterator(); ipPort.hasNext(); )
        {
            DestAddress address = (DestAddress) ipPort.next();
            // We expect only ultrapeers
            container.addCaughtHost( address, CaughtHostsContainer.HIGH_PRIORITY );
            NLogger.debug( NLoggerNames.UDP_INCOMING_MESSAGES, 
		            " ADDING to Caught Host Container ip port Host : " + address  
		            );
        }
    }
    
    /**
     * Returns true if this pong is marking a ultrapeer. This is the case when
     * fileSizeInKB is a power of two but at least 8.
     * @return true if this pong is marking a ultrapeer.
     */
    public boolean isUltrapeerMarked()
    {
        if ( fileSizeInKB < 8 )
        {
            return false;
        }
        return ( fileSizeInKB & (fileSizeInKB - 1 ) ) == 0;
    }
    
    /**
     * creates a udp pong message from a given byte array
     * @throws InvalidMessageException
     * @author Madhu
     *
     */
    public static PongMsg createUdpPongMsg( byte[] bytesMsg, Host fromHost ) 
    	throws InvalidMessageException
    {
        MsgHeader msgHdr = MsgHeader.createMsgHeader( bytesMsg, 0 );
        return createUdpPongMsg( msgHdr, bytesMsg, MsgHeader.DATA_LENGTH, fromHost );
    }
    
    public static PongMsg createUdpPongMsg( MsgHeader msgHdr, byte[] data, int offset, Host fromHost ) 
		throws InvalidMessageException 
	{
        if( ! ( MessageProcessor.isValidUdpMsgHeader( msgHdr ) ) )
        {
            NLogger.warn( NLoggerNames.UDP_INCOMING_MESSAGES, " Could not create udp pong " +
            		"from given byte array. Message Verification failed " + new String( data )
                    );
            throw new InvalidMessageException( " Could not create Msg Header " +
            		"while trying to create udp pong Msg. Message Verification failed "
                    );
        }
        
        msgHdr.setFromHost( fromHost );
        
        if( msgHdr.getDataLength() < MIN_PONG_DATA_LEN )
        {
        	 throw new InvalidMessageException( " Could not create Msg Body while trying to" +
            		" create udp pong Msg"
                    );
        }
        
        byte[] body = MessageProcessor.createBody( msgHdr, data, offset );
        if ( body == null )
        {
            throw new InvalidMessageException( " Could not create Msg Body while trying to" +
            		" create udp pong Msg"
                    );
        }
        
        return new PongMsg( msgHdr, body );  
	}
    
    
    /**
     * create a pong response message for a given ping
     * @param ping
     * @return
     */
    public static PongMsg createUdpPongMsg( PingMsg ping )
    {
        StatisticsManager statMgr = StatisticsManager.getInstance();
        NetworkManager networkMgr = NetworkManager.getInstance();
        ShareManager shareMgr = ShareManager.getInstance();
        HostManager hostMgr = HostManager.getInstance();
        
        SharedFilesService sharedFilesService = shareMgr.getSharedFilesService();
        int fileCount = sharedFilesService.getFileCount();
        int fileSize = sharedFilesService.getTotalFileSizeInKb();
        
        StatisticProvider uptimeProvider = statMgr.getStatisticProvider(
            StatisticsManager.DAILY_UPTIME_PROVIDER );
        int avgDailyUptime = ((IntObj)uptimeProvider.getValue()).intValue();
        boolean isUltrapeer = hostMgr.isUltrapeer();
        GGEPBlock ggepBlock = createMyGGEPBlock(avgDailyUptime, isUltrapeer);
        
        byte[] scpByte = ping.getScpByte();
        Collection ipPortPairs = null;
        NetworkHostsContainer netContainer = hostMgr.getNetworkHostsContainer();
        switch( scpByte[0] & PingMsg.UDP_SCP_MASK )
        {
            case PingMsg.UDP_SCP_ULTRAPEER: 
                ipPortPairs = netContainer.getFreeUltrapeerSlotHosts();
                break;
            case PingMsg.UDP_SCP_LEAF:
                ipPortPairs = netContainer.getFreeLeafSlotHosts();
                break;
        }
        addUdpPongGGEPExt( ipPortPairs, ggepBlock );
        
        
        DestAddress localAddress = networkMgr.getLocalAddress();
        IpAddress ipAddress = localAddress.getIpAddress();
        if( ipAddress == null )
        {
            throw new IllegalArgumentException( "Can't accept null ip." );
        }
        byte[] pongIp = ipAddress.getHostIP();        
        short pongPort = (short)localAddress.getPort();
        
        // Construct pingResponse msg.  Copy the original ping's GUID.
        MsgHeader newHeader = new MsgHeader( ping.getHeader().getMsgID(),
                MsgHeader.PONG_PAYLOAD, (byte)1, (byte)0, 0 );
        PongMsg udpPong = new PongMsg( newHeader, pongIp, pongPort, 
            fileCount, fileSize, isUltrapeer, ggepBlock );
        NLogger.info( NLoggerNames.UDP_OUTGOING_MESSAGES, "Created udp pong " +
            " in response to ping: " + udpPong );
        return udpPong;
    }
    
    
    public static PongMsg createMyOutgoingPong( GUID msgId, byte ttl )
    {
        StatisticsManager statMgr = StatisticsManager.getInstance();
        NetworkManager networkMgr = NetworkManager.getInstance();
        ShareManager shareMgr = ShareManager.getInstance();
        HostManager hostMgr = HostManager.getInstance();
        
        SharedFilesService sharedFilesService = shareMgr.getSharedFilesService();
        int fileCount = sharedFilesService.getFileCount();
        int fileSize = sharedFilesService.getTotalFileSizeInKb();
        
        StatisticProvider uptimeProvider = statMgr.getStatisticProvider(
                StatisticsManager.DAILY_UPTIME_PROVIDER );
        int avgDailyUptime = ((IntObj)uptimeProvider.getValue()).intValue();
        boolean isUltrapeer = hostMgr.isUltrapeer();
        GGEPBlock ggepBlock = createMyGGEPBlock( avgDailyUptime, isUltrapeer );
        
        DestAddress localAddress = networkMgr.getLocalAddress();
        IpAddress localIp = localAddress.getIpAddress();
        byte[] pongIp;
        if ( localIp == null )
        {
            pongIp = IpAddress.UNSET_IP;
            // in case we hace a unset ip address we need to use the Phex.EXTDEST
            // GGEP extension to specify our pong destination.
            addPhexExtendedDestinationGGEP( localAddress, ggepBlock );
        }
        else
        {
            pongIp = localIp.getHostIP();
        }
        
        MsgHeader header = new MsgHeader( msgId, MsgHeader.PONG_PAYLOAD,
            ttl, (byte)0, 0 );
        PongMsg pong = new PongMsg( header, pongIp, (short)localAddress.getPort(), 
            fileCount, fileSize, isUltrapeer, ggepBlock );
        return pong;
    }
    
    public static PongMsg createOtherLeafsOutgoingPong( GUID msgId, byte ttl, 
        byte hops, DestAddress address )
    {
        MsgHeader header = new MsgHeader( msgId, MsgHeader.PONG_PAYLOAD,
            ttl, (byte)hops, 0 );
        GGEPBlock ggepBlock = null;
        
        IpAddress ip = address.getIpAddress();
        byte[] pongIp;
        if ( ip == null )
        {
            pongIp = IpAddress.UNSET_IP;
            // in case we hace a unset ip address we need to use the Phex.EXTDEST
            // GGEP extension to specify our pong destination.
            ggepBlock = new GGEPBlock();
            addPhexExtendedDestinationGGEP( address, ggepBlock );
        }
        else
        {
            pongIp = ip.getHostIP();
        }
        PongMsg pong = new PongMsg( header, pongIp, (short)address.getPort(), 
            0, 0, false, ggepBlock );
        return pong;
    }
    
    private static GGEPBlock createMyGGEPBlock( int avgDailyUptime,
        boolean isUltrapeer )
    {
        GGEPBlock ggepBlock = new GGEPBlock();
        // add daily avg. uptime.
        if ( avgDailyUptime > 0 )
        {
            ggepBlock.addExtension( GGEPBlock.AVARAGE_DAILY_UPTIME, avgDailyUptime );
        }
        
        // add UP GGEP extension.
        if ( isUltrapeer )
        {
            byte[] upExtension = new byte[3];
            upExtension[0] = IOUtil.serializeGUESSVersionFormat(
                VersionUtils.getUltrapeerMajorVersionNumber(),
                VersionUtils.getUltrapeerMinorVersionNumber() ); 
                
            NetworkHostsContainer networkHostsContainer = HostManager.getInstance().getNetworkHostsContainer();
            upExtension[1] = (byte) networkHostsContainer.getOpenLeafSlotsCount();
            upExtension[2] = (byte) networkHostsContainer.getOpenUltrapeerSlotsCount();
            
            ggepBlock.addExtension( GGEPBlock.ULTRAPEER_ID, upExtension );
        }
        
        // add vendor info
        ggepBlock.addExtension( GGEPBlock.VENDOR_CODE_ID, GGEP_VENDOR_CODE );
        return ggepBlock;
    }
    
    private static void addUdpPongGGEPExt( Collection ipPortPairs,
        GGEPBlock ggepBlock )
    {
        // add ip port info if asked for
        if( ipPortPairs != null )
        {
            byte[] ipPortData = packIpPortData( ipPortPairs );
            if( ipPortData.length >= 6 )
            {
                ggepBlock.addExtension( GGEPBlock.UDP_HOST_CACHE_IPP, ipPortData );
            }
        }
        
        UdpHostCacheManager uhcMgr = UdpHostCacheManager.getInstance();
        if( uhcMgr.isUdpHostCache() )
        {
            NetworkManager netMgr = NetworkManager.getInstance();
            DestAddress uhcAddress = netMgr.getLocalAddress();
            byte[] data;
            
            // check if we have dns name
            if( uhcAddress.isIpHostName() )
            {
                data = new byte[0];
            }
            else
            {
                data = uhcAddress.getHostName().getBytes();
            }
            // now add the ggep extension udphc
            ggepBlock.addExtension( GGEPBlock.UDP_HOST_CACHE_UDPHC, data );
            NLogger.debug( NLoggerNames.UDP_OUTGOING_MESSAGES, "UDP HOST CACHE extension added to outgoing pongs");
        }
        
        // if we want Packed Host Caches the data should be added in compressed form
        String packedCacheString = uhcMgr.getUdpHostCacheContainer().createPackedHostCaches();
        if( packedCacheString.length() > 0 )
        {
            byte[] data = IOUtil.deflate( packedCacheString.getBytes() );
            ggepBlock.addExtension( GGEPBlock.UDP_HOST_CACHE_PHC, data );
            NLogger.debug( NLoggerNames.UDP_OUTGOING_MESSAGES, " PACKED HOST CACHE extension added to outgoing pongs ");
        }
    }
    
    private static void addPhexExtendedDestinationGGEP( DestAddress address,
        GGEPBlock ggepBlock )
    {
        // TODO1 this is totally experimental and needs to be optimised
        // to use correct byte encoding!
        ggepBlock.addExtension( GGEPBlock.PHEX_EXTENDED_DESTINATION, 
            address.getHostName().getBytes() );
    }
    
    
    /**
     * Sets the ultrapeer kbytes field for ultrapeers.
     * This is done by returning the nearest power of two of the kbytes field.
     * A kbytes value of 1536 would return 1024.
     *                   1535              512.
     */
    private static int createUltrapeerMarking( int kbytes )
    {
        if ( kbytes < 12 )
        {
            return 8;
        }
        // first get the bit count of the value and substract 1...
        int bitCount = IOUtil.determineBitCount( kbytes );
        // calculate the power of two...
        int power = (int)Math.pow( 2, bitCount );
        // now determine the border value of the exponent...
        int minBorder = power - (power / 4);
        if ( kbytes < minBorder )
        {
            power = (int)Math.pow( 2, bitCount-1 );
        }
        return power;
    }
}