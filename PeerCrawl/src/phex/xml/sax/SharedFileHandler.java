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
 *  Created on 18.11.2005
 *  --- CVS Information ---
 *  $Id: SharedFileHandler.java,v 1.1 2005/11/19 23:53:17 gregork Exp $
 */
package phex.xml.sax;

import java.io.CharArrayWriter;

import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import phex.utils.NLogger;
import phex.xml.XJBSharedFile;
import phex.xml.impl.XJBAlternateLocationImpl;

/**
 * 
 */
public class SharedFileHandler extends DefaultHandler
{
    public static final String THIS_TAG_NAME = "SF";

    private CharArrayWriter text = new CharArrayWriter();

    private SAXParser parser;

    private XJBSharedFile xjbFile;

    private DefaultHandler parent;

    public SharedFileHandler( XJBSharedFile xjbFile, Attributes attributes,
        DefaultHandler parent, SAXParser parser )
    {
        this.xjbFile = xjbFile;
        this.parser = parser;
        this.parent = parent;
    }

    /**
     * Receive notification of the start of an element.
     *
     * @param name The element type name.
     * @param attributes The specified or defaulted attributes.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ContentHandler#startElement
     */
    public void startElement( String uri, String localName, String qName,
        Attributes attributes ) throws SAXException
    {
        text.reset();
        if ( qName.equals( AlternateLocationHandler.THIS_TAG_NAME ) )
        {
            XJBAlternateLocationImpl altloc = new XJBAlternateLocationImpl();
            xjbFile.getAltLoc().add( altloc );
            
            AlternateLocationHandler handler = new AlternateLocationHandler( 
                altloc, attributes, this, parser );
            parser.getXMLReader().setContentHandler( handler );
        }
        return;
    }

    public void endElement( String uri, String localName, String qName )
        throws SAXException
    {
        if ( qName.equals( "FID" ) )
        {
            xjbFile.setFileName( text.toString() );
        }
        else if ( qName.equals( "SHA1" ) )
        {
            xjbFile.setSHA1( text.toString() );
        }
        else if ( qName.equals( "TxRH" ) )
        {
            xjbFile.setThexRootHash( text.toString() );
        }
        else if ( qName.equals( "TxD" ) )
        {
            try
            {
                xjbFile.setThexTreeDepth( Integer.parseInt( text.toString() ) );
            }
            catch (NumberFormatException exp)
            {
                NLogger.error( SharedFileHandler.class, exp, exp );
            }
        }
        else if ( qName.equals( "TxLLN" ) )
        {
            xjbFile.setThexLowestLevelNodes( text.toString() );
        }
        else if ( qName.equals( "LM" ) )
        {
            try
            {
                xjbFile.setLastModified( Long.parseLong( text.toString() ) );
            }
            catch (NumberFormatException exp)
            {
                NLogger.error( SharedFileHandler.class, exp, exp );
            }
        }
        else if ( qName.equals( "HC" ) )
        {
            try
            {
                xjbFile.setHitCount( Integer.parseInt( text.toString() ) );
            }
            catch (NumberFormatException exp)
            {
                NLogger.error( SharedFileHandler.class, exp, exp );
            }
        }
        else if ( qName.equals( "UC" ) )
        {
            try
            {
                xjbFile.setUploadCount( Integer.parseInt( text.toString() ) );
            }
            catch (NumberFormatException exp)
            {
                NLogger.error( SharedFileHandler.class, exp, exp );
            }
        }
        else if ( qName.equals( THIS_TAG_NAME ) )
        {
            parser.getXMLReader().setContentHandler( parent );
        }
    }

    public InputSource resolveEntity( String publicId, String systemId )
    {
        return null;
    }

    public void characters( char[] ch, int start, int length )
    {
        text.write( ch, start, length );
    }
}
