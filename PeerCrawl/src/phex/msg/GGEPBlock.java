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
 *  $Id: GGEPBlock.java,v 1.17 2005/11/03 16:33:45 gregork Exp $
 */
package phex.msg;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.DataFormatException;

import phex.common.address.DestAddress;
import phex.common.address.IpAddress;
import phex.utils.*;

/**
 * Implementation for GGEP
 * Document Revision Version 0.51
 * Protocol Version 0.5
 * <p> </p>
 * <p> </p>
 * <p>Copyright: Copyright (c) 2002 Gregor Koukkoullis</p>
 * <p> </p>
 * @author Gregor Koukkoullis
 *
 */
public class GGEPBlock
{
    /**
     * This is a magic number is used to help distinguish GGEP extensions from
     * legacy data which may exist.  It must be set to the value 0xC3.
     */
    public static final byte MAGIC_NUMBER = (byte) 0xC3;

    /**
     * Browse host GGEP extension header ID.
     */
    public static final String BROWSE_HOST_HEADER_ID = "BH";
    public static final String ALTERNATE_LOCATIONS_HEADER_ID = "ALT";
    public static final String AVARAGE_DAILY_UPTIME = "DU";
    public static final String ULTRAPEER_ID = "UP";
    public static final String VENDOR_CODE_ID = "VC";
    public static final String PATH_INFO_HEADER_ID = "PATH";
    public static final String PUSH_PROXY_HEADER_ID = "PUSH";
    public static final String UDP_HOST_CACHE_UDPHC = "UDPHC";
    public static final String UDP_HOST_CACHE_IPP = "IPP";
    public static final String UDP_HOST_CACHE_SCP = "SCP";
    public static final String UDP_HOST_CACHE_PHC = "PHC";
    public static final String PHEX_EXTENDED_DESTINATION = "PHEX.EXDST";

    private HashMap headerToDataMap;

    public GGEPBlock( )
    {
        headerToDataMap = new HashMap( 3 );
    }

    public void debugDump()
    {
        System.out.println( "--------------------------------------" );
        Iterator iterator = headerToDataMap.keySet().iterator();
        while( iterator.hasNext() )
        {
            Object next = iterator.next();
            System.out.println( next + " = " + headerToDataMap.get( next ) );
        }
        System.out.println( "--------------------------------------" );
    }

    /**
     * Adds a GGEP extension to a extension block without a data segment.
     * @param header the header name of the extension
     */
    private void addExtension( String header )
    {
        addExtension( header, "".getBytes() );
    }

    /**
     * Adds a GGEP extension to a extension block with a data segment.
     * @param header the header name of the extension
     * @param data the data of the extension.
     */
    public void addExtension( String header, byte[] data )
    {
        headerToDataMap.put( header, data );
    }
    
    /** 
     * Adds a GGEP extension to a extension block with an integer value.
     * @param header the header name of the extension
     * @param value the integer data, it should be an unsigned integer value
     */
    public void addExtension( String header, int value )
    {
        addExtension( header, IOUtil.serializeInt2MinLE( value ) );
    }
    
    public byte[] getExtensionData( String header )
    {
        return (byte[])headerToDataMap.get( header );
    }
    
    /**
     * Checks if the data associated with a header is in compressed form.
     * If it is compressed then the bit 5 ( starting from 0 )
     * of the header Flags is set
     * @author Madhu
     */
    private int checkIfCompressed( String header, int headerFlags )
    {
        if( header.equals( UDP_HOST_CACHE_PHC ) )
        {
                headerFlags = headerFlags | 0x20;
        }
        return headerFlags;
    }
    
    
    /**
     * Returns the byte representation of the GGEP block.
     * @return the byte representation of the GGEP block.
     */
    public byte[] getBytes() throws IOException
    {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream( 30 );
        outStream.write( MAGIC_NUMBER );

        Iterator iterator = headerToDataMap.keySet().iterator();
        while( iterator.hasNext() )
        {
            String headerKey = (String)iterator.next();
            byte[] dataBytes = (byte[])headerToDataMap.get( headerKey );

            int headerFlags = 0x00;
            
            // needed if we add compressed data with a headerKey. 
            // For ex the PHC extension
            headerFlags = checkIfCompressed( headerKey, headerFlags );
            
            if ( !iterator.hasNext() )
            {
                headerFlags = headerFlags | 0x80;
            }
            byte[] headerBytes = headerKey.getBytes();
            headerFlags = headerFlags | headerBytes.length;
            outStream.write( headerFlags );
            outStream.write( headerBytes );

            int dataLength = dataBytes.length;
            int tmp = dataLength & 0x3f000;
            // first byte...
            if ( tmp != 0 )
            {
                // shift left to drop of non relevant bytes...
                tmp = tmp >> 12;
                tmp = 0x80 | tmp;
                outStream.write( tmp );
            }
            tmp = dataLength & 0xFC0;
            if ( tmp != 0 )
            {
                // shift left to drop of non relevant bytes...
                tmp = tmp >> 6;
                tmp = 0x80 | tmp;
                outStream.write( tmp );
            }

            tmp = dataLength & 0x3F;
            tmp = 0x40 | tmp;
            outStream.write( tmp );

            if ( dataLength > 0 )
            {
                outStream.write( dataBytes );
            }
        }

        return outStream.toByteArray();
    }

    /**
     * Checks if the extension with the given headerID is available.
     * @param header
     * @return
     */
    public boolean isExtensionAvailable( String headerID )
    {
        return headerToDataMap.containsKey( headerID );
    }


    //////////////////////// Static helpers ////////////////////////////////////


    private static byte[] browseHostGGEPBlock;

    public static byte[] getQueryReplyGGEPBlock( 
        boolean isBrowseHostSupported, DestAddress[] pushProxyAddresses )
    {
        if ( pushProxyAddresses != null && pushProxyAddresses.length > 0 )
        {// we need to create the GGEP block in realtime.
            GGEPBlock ggepBlock = new GGEPBlock();
            if ( isBrowseHostSupported )
            {
                ggepBlock.addExtension( GGEPBlock.BROWSE_HOST_HEADER_ID );
            }
            
            try
            {
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                int count = Math.min( pushProxyAddresses.length, 4 );
                for ( int i = 0; i < count; i++ )
                {
                    IpAddress ip = pushProxyAddresses[i].getIpAddress();
                    if ( ip != null )
                    {
                        outStream.write( ip.getHostIP() );
                        IOUtil.serializeShortLE( 
                            (short)pushProxyAddresses[i].getPort(), outStream );
                    }
                }
                if ( outStream.size() > 0 )
                {
                    ggepBlock.addExtension( GGEPBlock.PUSH_PROXY_HEADER_ID,
                        outStream.toByteArray() );
                }
                byte[] data = ggepBlock.getBytes();
                return data;
            }
            catch ( IOException exp )
            {// this should never occure..
                NLogger.error( NLoggerNames.MESSAGE_ENCODE_DECODE, exp, exp );
                return IOUtil.EMPTY_BYTE_ARRAY;
            }
        }
        else if ( isBrowseHostSupported )
        {
            if ( browseHostGGEPBlock == null )
            {
                GGEPBlock ggepBlock = new GGEPBlock();
                ggepBlock.addExtension( GGEPBlock.BROWSE_HOST_HEADER_ID );
                try
                {
                    browseHostGGEPBlock = ggepBlock.getBytes();
                }
                catch ( IOException exp )
                {
                    NLogger.error( NLoggerNames.MESSAGE_ENCODE_DECODE, exp, exp );
                }
            }
            return browseHostGGEPBlock;
        }
        else
        {
            return IOUtil.EMPTY_BYTE_ARRAY;
        }
    }

    /**
     * Returns if the extension is available in any GGEP block.
     * @param ggepBlocks
     * @param header
     * @return
     */
    public static boolean isExtensionHeaderInBlocks( GGEPBlock[] ggepBlocks,
        String headerID )
    {
        for (int i = 0; i < ggepBlocks.length; i++ )
        {
            if ( ggepBlocks[i].isExtensionAvailable( headerID ) )
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns the extension if available in any GGEP block or null if not available.
     * @param ggepBlocks
     * @param header
     * @return the extension if available in any GGEP block or null if not available.
     */
    public static byte[] getExtensionDataInBlocks( GGEPBlock[] ggepBlocks,
        String headerID )
    {
        for (int i = 0; i < ggepBlocks.length; i++ )
        {
            if ( ggepBlocks[i].isExtensionAvailable( headerID ) )
            {
                return ggepBlocks[i].getExtensionData( headerID );
            }
        }
        return null;
    }

    public static GGEPBlock[] parseGGEPBlocks( byte[] body, int offset )
        throws InvalidGGEPBlockException
    {
        GGEPParser parser = new GGEPParser();
        return parser.parseGGEPBlocks( body, offset );
    }
    
    public static GGEPBlock[] parseGGEPBlocks( PushbackInputStream inStream )
        throws InvalidGGEPBlockException, IOException
    {
        GGEPParser parser = new GGEPParser();
        return parser.parseGGEPBlocks( inStream );
    }
    
    public static void debugDumpBlocks( GGEPBlock[] ggepBlocks )
    {
        for (int i = 0; i < ggepBlocks.length; i++ )
        {
            ggepBlocks[i].debugDump();
        }
    }


    private static class GGEPParser
    {
        private int offset;
        private ArrayList ggepList;

        public GGEPParser( )
        {
            ggepList = new ArrayList( 3 );
        }
        
        public GGEPBlock[] parseGGEPBlocks( PushbackInputStream inStream )
            throws InvalidGGEPBlockException, IOException
        {
            // the ggep specification requires us to support more then one GGEP
            // extension:
            // 'Extension blocks may contain an arbitrary number of GGEP blocks
            // packed one against another.'
            byte b;
            while ( true )
            {
                b = (byte)inStream.read();
                if ( b == -1 )
                {
                    break;
                }
                else if ( b != MAGIC_NUMBER )
                {
                    // not ggep anymore
                    // push back and break...
                    inStream.unread(b);
                    break;
                }
                ggepList.add( parseGGEPBlock( inStream ) );
            }

            GGEPBlock[] ggepArray = new GGEPBlock[ ggepList.size() ];
            ggepList.toArray( ggepArray );
            return ggepArray;
        }
        
        private GGEPBlock parseGGEPBlock( InputStream inStream )
            throws InvalidGGEPBlockException, IOException
        {
            GGEPBlock ggepBlock = new GGEPBlock();

            boolean isLastExtension = false;
            int b;
            while ( !isLastExtension)
            {
                // parse the extension header flags. They must be in form:
                // - 7: Last Extension
                // - 6: Encoding
                // - 5: Compression
                // - 4: Reserved ( must be 0 )
                // - 3-0: ID Len ( 1-15 )

                // validate extension byte
                b = inStream.read();
                if ( (b & 0x10) != 0)
                {
                    throw new InvalidGGEPBlockException();
                }
                // last bit in header
                isLastExtension = (b & 0x80) != 0;
                boolean isEncoded = (b & 0x40) != 0;
                boolean isCompressed = (b & 0x20) != 0;

                // first 4 bit
                short headerLength = (short) (b & 0x0F);
                if ( headerLength == 0 )
                {// 0 not allowed...
                    throw new InvalidGGEPBlockException();
                }
                
                byte[] headerData = new byte[ headerLength ];
                inStream.read( headerData, 0, headerLength );
                
                // parse the rest of the extension header.
                String header = new String( headerData, 0, headerLength );

                // parse the data length
                int dataLength = parseDataLength( inStream );
                byte[] dataArr = null;
                try
                {
                    if ( dataLength > 0 )
                    {
                        // get data as byte array...
                        dataArr = new byte[ dataLength ];
                        inStream.read( dataArr, 0, dataLength );
                        
                        if ( isCompressed )
                        {
                            // use zlib inflator to decompress
                            dataArr = IOUtil.inflate( dataArr );
                        }
    
                        if ( isEncoded )
                        {
                            dataArr = IOUtil.cobsDecode( dataArr );
                        }
                    }
                    else
                    {
                        dataArr = new byte[0];
                    }
                    ggepBlock.addExtension( header, dataArr );
                }
                catch ( DataFormatException exp )
                {// in case the inflate data format does not work.
                    if ( NLogger.isWarnEnabled( GGEPBlock.class ) ) 
                    {
                        NLogger.warn(GGEPBlock.class,
                            "Invalid GGEP data format. Header: '" +
                            header + "' Data: '"
                            + HexConverter.toHexString(dataArr) + "'.", exp );
                    }
                }
            }
            return ggepBlock;
        }
        
        /**
         * Code taken form GGEP specification document.
         * @param body
         * @return
         * @throws IllegalGGEPBlockException
         */
        private int parseDataLength( InputStream inStream )
            throws InvalidGGEPBlockException, IOException
        {
            int length = 0;
            int byteCount = 0;
            byte currentByte;
            do
            {
                byteCount ++;
                if ( byteCount > 3 )
                {
                    throw new InvalidGGEPBlockException();
                }
                currentByte = (byte)inStream.read();
                length = (length << 6) | ( currentByte & 0x3F );
            }
            while ( 0x40 != (currentByte & 0x40) );
            return length;
        }

        public GGEPBlock[] parseGGEPBlocks( byte[] body, int aOffset )
            throws InvalidGGEPBlockException
        {
            offset = aOffset;
            // the ggep specification requires us to support more then one GGEP
            // extension:
            // 'Extension blocks may contain an arbitrary number of GGEP blocks
            // packed one against another.'
            while ( body.length > offset && body[ offset ] == MAGIC_NUMBER )
            {
                // skip magic number
                offset ++;
                ggepList.add( parseGGEPBlock( body ) );
            }

            GGEPBlock[] ggepArray = new GGEPBlock[ ggepList.size() ];
            ggepList.toArray( ggepArray );
            return ggepArray;
        }

        private GGEPBlock parseGGEPBlock( byte[] body )
            throws InvalidGGEPBlockException
        {
            GGEPBlock ggepBlock = new GGEPBlock();

            boolean isLastExtension = false;
            while ( !isLastExtension)
            {
                // parse the extension header flags. They must be in form:
                // - 7: Last Extension
                // - 6: Encoding
                // - 5: Compression
                // - 4: Reserved ( must be 0 )
                // - 3-0: ID Len ( 1-15 )

                // validate extension byte
                if ( body.length > offset && (body[offset] & 0x10) != 0)
                {
                    throw new InvalidGGEPBlockException();
                }
                // last bit in header
                isLastExtension = (body[offset] & 0x80) != 0;
                boolean isEncoded = (body[offset] & 0x40) != 0;
                boolean isCompressed = (body[offset] & 0x20) != 0;

                // first 4 bit
                short headerLength = (short) (body[offset] & 0x0F);
                if ( headerLength == 0 )
                {// 0 not allowed...
                    throw new InvalidGGEPBlockException();
                }
                offset ++;

                // parse the rest of the extension header.
                String header = new String( body, offset, headerLength );
                offset += headerLength;

                // parse the data length
                int dataLength = parseDataLength( body );
                byte[] dataArr = null;
                try
                {
                    if ( dataLength > 0 )
                    {
                        // get data as byte array...
                        dataArr = new byte[ dataLength ];
                        System.arraycopy( body, offset, dataArr, 0, dataLength);
                        offset += dataLength;
                        
                        if ( isCompressed )
                        {
                            // use zlib inflator to decompress
                            dataArr = IOUtil.inflate( dataArr );
                        }
    
                        if ( isEncoded )
                        {
                            dataArr = IOUtil.cobsDecode( dataArr );
                        }
                    }
                    else
                    {
                        dataArr = new byte[0];
                    }
                    ggepBlock.addExtension( header, dataArr );
                }
                catch ( DataFormatException exp )
                {// in case the inflate data format does not work.
                    if ( NLogger.isWarnEnabled( GGEPBlock.class ) ) 
                    {
                        NLogger.warn(GGEPBlock.class,
                            "Invalid GGEP data format. Header: '" +
                            header + "' Data: '"
                            + HexConverter.toHexString(dataArr) + "'.", exp );
                    }
                }
            }
            return ggepBlock;
        }

        /**
         * Code taken form GGEP specification document.
         * @param body
         * @return
         * @throws IllegalGGEPBlockException
         */
        private int parseDataLength( byte[] body )
            throws InvalidGGEPBlockException
        {
            int length = 0;
            int byteCount = 0;
            byte currentByte;
            do
            {
                byteCount ++;
                if ( byteCount > 3 )
                {
                    throw new InvalidGGEPBlockException();
                }
                currentByte = body[ offset ];
                offset ++;
                length = (length << 6) | ( currentByte & 0x3F );
            }
            while ( 0x40 != (currentByte & 0x40) );
            return length;
        }
    }
}