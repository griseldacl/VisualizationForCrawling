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
 *  $Id: PhexXmlSaxParser.java,v 1.1 2005/11/19 23:53:17 gregork Exp $
 */
package phex.xml.sax;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import phex.utils.NLogger;
import phex.utils.NLoggerNames;
import phex.xml.XJBPhex;
import phex.xml.impl.XJBPhexImpl;

/**
 * 
 */
public class PhexXmlSaxParser
{
    public static XJBPhex parsePhexXml( InputStream inStream )   
        throws IOException
    {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try
        {
            SAXParser saxParser = spf.newSAXParser();
        
            XJBPhex xjbPhex = new XJBPhexImpl();
            saxParser.parse( new InputSource( inStream ),
                new PhexSAXHandler( xjbPhex, saxParser ) );
            return xjbPhex;
        }
        catch ( ParserConfigurationException exp )
        {
            NLogger.error( NLoggerNames.GLOBAL, exp, exp );
            throw new IOException( "Parsing Thex HashTree failed." );
        }
        catch ( SAXException exp )
        {
            NLogger.error( NLoggerNames.GLOBAL, exp, exp );
            throw new IOException( "Parsing Thex HashTree failed." );
        }
    }
    
    public static void main( String[] args ) 
        throws IOException, JAXBException
    {
        long start = System.currentTimeMillis();
        for ( int i = 0; i < 1000; i ++ )
        {
            InputStream inStream = new FileInputStream("C:\\temp\\resp.xml");
            parsePhexXml(inStream);
        }
        long end = System.currentTimeMillis();
        System.out.println((end-start));
        
        start = System.currentTimeMillis();
        JAXBContext jc = JAXBContext.newInstance( "phex.xml" );
        for ( int i = 0; i < 1000; i ++ )
        {
            InputStream inStream = new FileInputStream("C:\\temp\\resp.xml");
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            unmarshaller.unmarshal( inStream );
        }
        end = System.currentTimeMillis();
        
        System.out.println((end-start));

    }
}
