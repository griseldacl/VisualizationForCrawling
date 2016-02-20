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
 *  $Id: PhexXmlSaxWriter.java,v 1.1 2005/11/19 23:53:17 gregork Exp $
 */
package phex.xml.sax;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import phex.xml.XJBFavoriteHost;
import phex.xml.XJBFavoritesList;
import phex.xml.XJBPhex;

/**
 * 
 */
public class PhexXmlSaxWriter
{
    private AttributesImpl attributes = new AttributesImpl();
    private TransformerHandler transHandler;
    
    public void writePhexXml( OutputStream outStream, XJBPhex xjbPhex ) 
        throws SAXException, TransformerConfigurationException
    {
        StreamResult streamResult = new StreamResult(outStream);
        SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        transHandler = tf.newTransformerHandler();
        Transformer transformer = transHandler.getTransformer();
        transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2"); 
        transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
        transHandler.setResult( streamResult );
        
        
        transHandler.startDocument();
        
        attributes.addAttribute( "", "", "phex-version", "CDATA", 
            xjbPhex.getPhexVersion() );
        startElm( "phex", attributes );
        
        XJBFavoritesList favorites = xjbPhex.getFavoritesList();
        handleFavorites( favorites );
        
        endElm( "phex" );
        
        transHandler.endDocument();
    }
    
    private void handleFavorites( XJBFavoritesList favorites )
        throws SAXException
    {
        if ( favorites == null )
        {
            return;
        }
        Iterator iterator = favorites.getFavoritesList().iterator();
        while( iterator.hasNext() )
        {
            XJBFavoriteHost favHost = (XJBFavoriteHost) iterator.next();
            startElm( "favorite-host", null );
            
            String val = favHost.getHostName();
            if( val != null )
            {
                startElm( "host-name", null );
                elmText( val );
                endElm( "host-name" );
            }
            
            byte[] ip = favHost.getIp();
            if( ip != null )
            {
                startElm( "ip", null );
                elmHexBinary( ip );
                endElm( "ip" );
            }
            
            int port = favHost.getPort();
            // TODO1 how to verify that the port is set?
//            if( favHost. )
//            {
                startElm( "port", null );
                elmInt(port);
                endElm( "port" );
//            }
            
            endElm( "favorite-host" );
        }
    }
    
    private void startElm( String name, AttributesImpl atts ) throws SAXException
    {
        if ( atts == null )
        {
            attributes.clear();
            atts = attributes;
        }
        transHandler.startElement( "", "", name, atts );
    }
    
    private void elmText( String text ) throws SAXException
    {
        transHandler.characters( text.toCharArray(), 0, text.length() );
    }
    
    private void elmInt( int val ) throws SAXException
    {
        elmText( String.valueOf(val) );
    }
    
    private void elmHexBinary( byte[] data ) throws SAXException
    {
        StringBuffer r = new StringBuffer(data.length * 2);
        for(int i = 0; i < data.length; i++)
        {
            r.append( hexBinaryEncode(data[i] >> 4));
            r.append( hexBinaryEncode(data[i] & 0xf));
        }
        elmText( r.toString() );
    }
    
    private char hexBinaryEncode( int ch )
    {
        ch &= 0xf;
        if(ch < 10)
            return (char)(48 + ch);
        else
            return (char)(65 + (ch - 10));
    }
    
    private void endElm( String name ) throws SAXException
    {
        transHandler.endElement( "", "", name );
    }
    
    

    public static void main( String[] args ) 
        throws IOException, TransformerConfigurationException, SAXException
    {
//        OutputStream outStream = new FileOutputStream("C:\\temp\\respout.xml");
//        writePhexXml(outStream, null);
    }
}
