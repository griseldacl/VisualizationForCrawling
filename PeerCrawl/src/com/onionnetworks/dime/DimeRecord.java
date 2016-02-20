//Dime 1.0.3 2003-03-05 http://www.onionnetworks/developers
package com.onionnetworks.dime;

import java.io.*;

/**
 * Represents a version 1 chunked or non-chunked DIME record.  The DIME
 * specification can be found at:
 * http://search.ietf.org/internet-drafts/draft-nielsen-dime-02.txt .  
 * <p>
 * The non-chunked records can exceed the DIME specification's maximum payload
 * size of 2^32-1 bytes and will be chunked upon transmission.
 *
 * @author Ry4an (ry4an@onionnetworks.com)
 */
public class DimeRecord
{

    // It makes me sad that the spec allows payloads too large to fit into
    // a Java byte array
    public static final long MAX_MAX_PAYLOAD_SIZE = 4294967295L; // 2^32-1

    // this is the biggest size that will fit into a byte array
    public static final int DEFAULT_MAX_PAYLOAD_SIZE = Integer.MAX_VALUE;

    /** Only DIME version 1 is parsed (or in existance) */
    public static final int VERSION = 1;

    // first octets
    private static final int VERSION_MASK = 0xF8;

    private static final int VERSION_SHIFT = 3;

    private static final int MB_MASK = 0x04;

    private static final int ME_MASK = 0x02;

    private static final int CF_MASK = 0x01;

    // second octet
    private static final int TYPE_T_MASK = 0xf0;

    private static final int TYPE_T_SHIFT = 4;

    private static final int RESERVED_MASK = 0x0f;

    private TypeNameFormat tnf;

    private byte[] id;

    private byte[] type;

    private boolean first; // true if this is the first record in a message

    private boolean last; // true if this is the last record in a message

    private boolean continued; // true if more of this record follows

    private long payloadLength = -1;

    private InputStream payload;

    private SafeFiniteInputStream fis = null; // only for getPayload

    /**
     * Convert data from a byte array into a DimeRecord.  
     *
     * @param buf the byte array to be used as payload
     * @param t the type name format indicating the type of the format string
     * @param tn the type name of a format indicated by the TypeNameFormat
     * @param id The optional ID for this DimeRecord.
     */
    public DimeRecord( byte[] buf, TypeNameFormat t, String tn, String i )
    {
        this( new ByteArrayInputStream( buf ), buf.length, t, tn, i );
    }

    /**
     * Convert data from a stream of specified length into a DimeRecord.  
     * The stream is not closed.
     *
     * @param is the stream to be fully consumed to make the payload
     * @param len the length of the data in bytes to read from the stream
     * @param t the type name format indicating the type of the format string
     * @param tn the type name of a format indicated by the TypeNameFormat
     * @param id The optional ID for this DimeRecord.
     */
    public DimeRecord( InputStream is, long len, TypeNameFormat t, String tn,
        String i )
    {
        tnf = t;
        type = tn.getBytes();
        if ( i != null )
        {
            id = i.getBytes();
        }
        else
        {
            id = new byte[0];
        }
        payloadLength = len;
        payload = is;
    }

    /**
     * Creates a new DimeRecord from the provided InputStream.  The stream
     * is not closed and is not read past the length of the first available
     * DIME record.
     *
     * @param is the stream to extract the DIME record from
     * @return the newly createed DimeRecord
     * @throws IOException on failure to read
     */
    protected static DimeRecord extract( InputStream is ) throws IOException
    {

        int octet1, octet2;

        // first octet
        if ( (octet1 = is.read()) == -1 )
        {
            throw new IOException( "Unexpected end of stream" );
        }
        //System.out.println("octet1 = " + bytes(octet1));

        boolean last = ((octet1 & ME_MASK) != 0);
        boolean continued = ((octet1 & CF_MASK) != 0);
        boolean first = ((octet1 & MB_MASK) != 0);

        int version = (octet1 & VERSION_MASK) >>> VERSION_SHIFT;
        //System.out.println("version=" + version);

        if ( version != VERSION )
        {
            throw new IOException( "Unparsable Version: " + version );
        }

        // second octet
        if ( (octet2 = is.read()) == -1 )
        {
            throw new IOException( "Unexpected end of stream" );
        }
        //System.out.println("octet2 = " + bytes(octet2));

        TypeNameFormat tnf = TypeNameFormat
            .get( (octet2 & TYPE_T_MASK) >>> (TYPE_T_SHIFT) );

        if ( (octet2 & RESERVED_MASK) != 0 )
        {
            throw new IOException( "Reserved header space must be all zero" );
        }

        // octets 3 & 4 (options_length)
        int optLen = readUnsignedShort( is );

        // octets 5 & 6 (id length)
        int idLen = readUnsignedShort( is );
        //System.out.println("Id length=" + idLen);

        // octets 7 & 8 (type length)
        int typeLen = readUnsignedShort( is );
        //System.out.println("Type length=" + typeLen);

        // octets 9, 10, 11, & 12 (data length)
        long payloadLength = readUnsignedInt( is );

        // get opt
        byte[] opt = new byte[optLen];
        int pos = 0;
        while ( pos < opt.length )
        {
            int i = is.read( opt, pos, opt.length - pos );
            if ( i == -1 )
            {
                throw new IOException( "Unexpected end of stream" );
            }
            pos += i;
        }

        // skip opt padding
        byte[] pad = new byte[(4 - (optLen % 4)) % 4];
        pos = 0;
        while ( pos < pad.length )
        {
            int i = is.read( pad, pos, pad.length - pos );
            if ( i == -1 )
            {
                throw new IOException( "Unexpected end of stream" );
            }
            pos += i;
        }

        // get id
        byte[] id = new byte[idLen];
        pos = 0;
        while ( pos < id.length )
        {
            int i = is.read( id, pos, id.length - pos );
            if ( i == -1 )
            {
                throw new IOException( "Unexpected end of stream" );
            }
            pos += i;
        }

        // skip id padding
        pad = new byte[(4 - (idLen % 4)) % 4];
        pos = 0;
        while ( pos < pad.length )
        {
            int i = is.read( pad, pos, pad.length - pos );
            if ( i == -1 )
            {
                throw new IOException( "Unexpected end of stream" );
            }
            pos += i;
        }

        // read type
        byte[] type = new byte[typeLen];
        pos = 0;
        while ( pos < type.length )
        {
            int i = is.read( type, pos, type.length - pos );
            if ( i == -1 )
            {
                throw new IOException( "Unexpected end of stream" );
            }
            pos += i;
        }

        // read type
        pad = new byte[((4 - (typeLen % 4)) % 4)];
        pos = 0;
        while ( pos < pad.length )
        {
            int i = is.read( pad, pos, pad.length - pos );
            if ( i == -1 )
            {
                throw new IOException( "Unexpected end of stream" );
            }
            pos += i;
        }

        DimeRecord dr = new DimeRecord( is, payloadLength, tnf, new String(
            type ), new String( id ) );
        dr.last = last;
        dr.first = first;
        dr.continued = continued;
        return dr;
    }

    /**
     * The full DIME record with correct headers is sent through the provided
     * OutputStream.  This method can only be called once and may not be called
     * if <code>getPayload</code> has been called.  If this DimeRecord's
     * payload exceeds the provided maximum length then multiple concatenated
     * DIME records will be produced on the stream.
     *
     * @param os the stream down which to send the DIME records
     * @param length the maximum payload length per record.  Cannot exceede
     *               MAX_MAX_PAYLOAD_SIZE
     * @param begin set to true if this is the first record in a message
     * @param end set to true if this is the last record in a message
     * @throws IOException on failure to read or write
     */
    protected synchronized void produce( OutputStream os, long length,
        boolean begin, boolean end ) throws IOException
    {
        //System.out.println("prod:os, " + length + ", " + begin + ", " + end);
        if ( payload == null )
        {
            throw new IllegalStateException(
                "produce or getPayload already called" );
        }

        if ( length > MAX_MAX_PAYLOAD_SIZE )
        {
            throw new IllegalArgumentException(
                "payload size cannot be bigger than "
                    + DimeRecord.MAX_MAX_PAYLOAD_SIZE );
        }

        long remaining = payloadLength;
        byte[] myId = id;
        byte[] myType = type;
        TypeNameFormat myTnf = tnf;

        //System.out.println("myId.len = " + myId.length);
        //System.out.println("myType.len = " + myType.length);

        while ( remaining > 0 )
        {
            //System.out.println("Remaining = " + remaining);
            int header = VERSION;
            header <<= VERSION_SHIFT;

            if ( begin )
            { // first in message
                //System.out.println("begin set");
                header |= MB_MASK;
                begin = false;
            }
            if ( end && (remaining <= length) )
            { // last of chunk too
                header |= ME_MASK;
                //System.out.println("end set");
            }
            if ( remaining > length )
            { // first or middle in a chunk
                header |= CF_MASK;
                //System.out.println("chunk set");
            }
            header <<= TYPE_T_SHIFT;
            header |= myTnf.toInt();
            header <<= 4;
            // set reserved here
            header <<= 16;
            // TODO set options length here
            os.write( getBytes( header ) ); // first 4 bytes
            header = 0;
            header |= myId.length;
            header <<= 16;
            header |= myType.length;
            os.write( getBytes( header ) ); // second 4 bytes

            // data length
            long chunkLen = Math.min( length, remaining );
            os.write( getBytes( (int) (0xffffffffL & chunkLen) ) );

            // TODO write options here

            // id  (emptied if not first chunk)
            os.write( myId );
            os.write( getPad( myId.length ) );

            // type (emptied if not first chunk)
            os.write( myType );
            os.write( getPad( myType.length ) );

            // clear them for subsequent chunks
            myId = myType = new byte[0];
            myTnf = TypeNameFormat.UNCHANGED;

            // data
            byte[] buff = new byte[8192];
            long done = 0;
            while ( done < chunkLen )
            {
                int got = payload.read( buff, 0, (int) Math.min( buff.length,
                    chunkLen - done ) );
                if ( got == -1 )
                {
                    throw new IOException( "Unexcepted end of payload" );
                }
                os.write( buff, 0, got );
                done += got;
            }
            os.write( getPad( chunkLen ) );

            remaining -= length;
        }

        payload = null; // consumed
        os.flush();
    }

    private static int readUnsignedShort( InputStream in ) throws IOException
    {
        int i1 = in.read();
        int i2 = in.read();
        if ( i1 == -1 || i2 == -1 )
        {
            throw new IOException( "unexpected end of stream" );
        }
        return (i1 << 8) + i2;
    }

    private static long readUnsignedInt( InputStream in ) throws IOException
    {
        int i1 = in.read();
        int i2 = in.read();
        int i3 = in.read();
        int i4 = in.read();
        if ( i1 == -1 || i2 == -1 || i3 == -1 || i4 == -1 )
        {
            throw new IOException( "unexpected end of stream" );
        }
        return (i1 << 24) | (i2 << 16) | (i3 << 8) | i4;
    }

    /*
    private static String bytes( int x )
    {
        StringBuffer sb = new StringBuffer();
        for ( int i = 0; i < 32; i++ )
        {
            sb.append( (x & 0x80000000) >>> 31 );
            x <<= 1;
            if ( ((i + 1) % 8) == 0 )
            {
                sb.append( " " );
            }
        }
        return sb.toString();
    }
    */

    /**
     * @param the number of bytes already written
     * @return an array of zero bytes which makes total size % 4 == 0 
     */
    private byte[] getPad( long size )
    {
        byte[] retval = new byte[(4 - (int) (size % 4)) % 4];
        return retval;
    }

    /**
     * Get the payload from this Record.  This method may only be called once,
     * and may not be called if <code>produce</code> has already been called.
     * This payload will not exceede the max payload size of a record.  If
     * <code>isContinued</code> returns true then the payload if the next
     * DimeRecord should be appended to this one when building a complete
     * entry.
     *
     * @return a stream contaiing the record payload
     */
    public synchronized InputStream getPayload()
    {
        if ( payload == null )
        {
            throw new IllegalStateException(
                "produce or getPayload already called" );
        }

        InputStream data = payload;
        payload = null;
        return (fis = new SafeFiniteInputStream( data, payloadLength ));
    }

    /**
     * This is only available if the DimeRecord was constructed by 
     * <code>extract</code> or <code>produce</code> has been called.
     * @return length in bytes of payload.
     */
    public long getPayloadLength()
    {
        return payloadLength;
    }

    /**
     * Tells you if the full payload has been read.  
     * @throws IllegalStateException if getPayload() hasn't yet been called
     * @return true if it's now safe to get the next record
     */
    public synchronized boolean isConsumed()
    {
        if ( fis == null )
        {
            throw new IllegalStateException( "Must call getPayload() first" );
        }
        return fis.isDone();
    }

    /**
     * @return true this is the start or middle of a chunked entry
     */
    public boolean isContinued()
    {
        return continued;
    }

    /**
     * @return unique id of this record.   May be null.
     */
    public String getId()
    {
        return id == null ? null : (id.length == 0 ? null : new String( id ));
    }

    /**
     * @return type string of this record
     */
    public String getType()
    {
        return (type.length == 0 ? null : new String( type ));
    }

    /**
     * @return tells how to interpret results of getType
     */
    public TypeNameFormat getTypeNameFormat()
    {
        return tnf;
    }

    /**
     * @return true if this is the last record in a message
     */
    public boolean isLast()
    {
        return last;
    }

    /**
     * Thanks to John Russel for finding a bug in this method.
     * @return true if this is the first record in a message
     */
    public boolean isFirst()
    {
        return first;
    }

    /**
     * @return info about the DimeRecord for debugging purposes
     */
    public String toString()
    {
        return "DimeRecord(id='" + new String( id ) + "', tnf='" + tnf
            + "', type='" + new String( type ) + "', length=" + payloadLength
            + ", first=" + first + ", last=" + last + ", continued="
            + continued + ")";
    }

    public static final byte[] getBytes( int i )
    {
        byte b[] = new byte[4];
        b[0] = (byte) (i >>> 24 & 255);
        b[1] = (byte) (i >>> 16 & 255);
        b[2] = (byte) (i >>> 8 & 255);
        b[3] = (byte) (i & 255);
        return b;
    }

    /**
     * Represents the format of the data found in the type field.  Since there
     * will never exist more than the 5 static instances created here, one can
     * get away with using reference equality instead of .equals.
     */
    public static class TypeNameFormat
    {
        public static final TypeNameFormat UNCHANGED = new TypeNameFormat( 0 );

        public static final TypeNameFormat MEDIA_TYPE = new TypeNameFormat( 1 );

        public static final TypeNameFormat URI = new TypeNameFormat( 2 );

        public static final TypeNameFormat UNKNOWN = new TypeNameFormat( 3 );

        public static final TypeNameFormat NONE = new TypeNameFormat( 4 );

        private int val;

        private TypeNameFormat( int i )
        {
            val = i;
        }

        public int toInt()
        {
            return val;
        }

        public static TypeNameFormat get( int v )
        {
            switch ( v )
            {
            case 0:
                return UNCHANGED;
            case 1:
                return MEDIA_TYPE;
            case 2:
                return URI;
            case 3:
                return UNKNOWN;
            case 4:
                return NONE;
            }
            throw new IllegalArgumentException( "unrecognized value: " + v );
        }

        public boolean equals( Object o )
        {
            return (o instanceof TypeNameFormat)
                && (((TypeNameFormat) o).val == val);
        }

        public int hashCode()
        {
            return toInt();
        }

        public boolean isUnchanged()
        {
            return (val == 0);
        }

        public boolean isMediaType()
        {
            return (val == 1);
        }

        public boolean isUri()
        {
            return (val == 2);
        }

        public boolean isUnknown()
        {
            return (val == 3);
        }

        public boolean isNone()
        {
            return (val == 4);
        }

        /**
         * For debugging use only
         * @return String representation of TNF value
         */
        public String toString()
        {
            switch ( val )
            {
            case 0:
                return "unchanged";
            case 1:
                return "media_type";
            case 2:
                return "uri";
            case 3:
                return "unknown";
            case 4:
                return "none";
            }
            throw new IllegalStateException( "Unknown value" );
        }
    }

    /**
     * Just like FiniteInputStream except .close() seeks to the end
     * <b>instead</B> of closing underlying stream.
     */
    public class SafeFiniteInputStream extends FilterInputStream
    {
        protected long left;

        int pad;

        public SafeFiniteInputStream( InputStream is, long count )
        {
            super( is );
            if ( is == null ) throw new NullPointerException();
            if ( count < 0L )
            {
                throw new IllegalArgumentException( "count must be > 0" );
            }

            left = count;
            pad = (4 - (int) (count % 4)) % 4;
        }

        public void close() throws IOException
        {
            skip( left ); // does checkDone()
        }

        public long skip( long n ) throws IOException
        {
            long result = super.in.skip( Math.min( n, left ) );
            left -= result;
            checkDone();
            return result;
        }

        public int read() throws IOException
        {
            byte b[] = new byte[1];
            if ( read( b, 0, 1 ) == -1 )
                return -1;
            else
                return b[0] & 255;
        }

        public int read( byte[] b, int off, int len ) throws IOException
        {
            if ( left == 0L && len > 0 ) return -1;
            int c = super.in.read( b, off, (int) Math.min( len, left ) );
            if ( c < 0 )
            {
                throw new EOFException();
            }
            else
            {
                left -= c;
            }
            checkDone();
            return c;
        }

        private synchronized void checkDone() throws IOException
        {
            while ( (left == 0) && (pad != 0) )
            {
                pad -= in.skip( pad );
            }
        }

        public synchronized boolean isDone()
        {
            return (left == 0 && pad == 0);
        }

        public int available() throws IOException
        {
            return (int) Math.min( super.in.available(), left );
        }
    }
}
