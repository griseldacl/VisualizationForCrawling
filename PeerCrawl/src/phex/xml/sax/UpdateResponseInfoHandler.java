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
 *  $Id: UpdateResponseInfoHandler.java,v 1.1 2005/11/19 23:53:17 gregork Exp $
 */
package phex.xml.sax;

import java.io.CharArrayWriter;

import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import phex.xml.XJBUpdateResponse;

/**
 * 
 */
public class UpdateResponseInfoHandler extends DefaultHandler
{   
    private static final String THIS_TAG_NAME = "info";
    
    private CharArrayWriter text = new CharArrayWriter();
    private SAXParser parser;
    private XJBUpdateResponse.InfoType xjbInfo;
    private DefaultHandler parent;
    
    public UpdateResponseInfoHandler( XJBUpdateResponse.InfoType info, 
        Attributes attributes, DefaultHandler parent, SAXParser parser )
    {
        this.xjbInfo = info;
        xjbInfo.setId( attributes.getValue( "id" ) );
        
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
        Attributes attributes)
        throws SAXException
    {
        text.reset();
        return;
    }
    
    public void endElement(String uri, String localName, String qName) 
        throws SAXException
    {
        if ( qName.equals( "header" ) )
        {
            xjbInfo.setHeader( text.toString() );
        }
        else if ( qName.equals( "text" ) )
        {
            xjbInfo.setText( text.toString() );
        }
        else if ( qName.equals( THIS_TAG_NAME ) )
        {
            parser.getXMLReader().setContentHandler( parent );
        }
    }
     
     public InputSource resolveEntity(String publicId,
        String systemId)
     {
         return null; 
     }
     
     public void characters(char[] ch, int start, int length)
     {
         text.write( ch,start,length );
     }
}
