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
 *  $Id: TestThexXjbXml.java,v 1.4 2005/10/03 00:18:29 gregork Exp $
 */
package phex.test;

import java.io.ByteArrayInputStream;

import junit.framework.TestCase;
import phex.xml.thex.*;

/**
 * 
 * @author gkoukkoullis
 */
public class TestThexXjbXml extends TestCase
{

    /**
     * Constructor for TestThexXjbXml.
     * @param arg0
     */
    public TestThexXjbXml(String arg0)
    {
        super(arg0);
    }
    
    public void testThexSAXHandler()
        throws Exception
    {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
            "<!DOCTYPE hashtree SYSTEM \"http://open-content.net/spec/thex/thex.dtd\">"+
            "<hashtree>"+
            "<file size='1146045066' segmentsize='1024'/>"+
            "<digest algorithm='http://www.w3.org/2000/09/xmldsig#sha1' outputsize='20'/>"+
            "<serializedtree depth='22' type='http://open-content.net/spec/thex/breadthfirst' uri='uuid:09233523-345b-4351-b623-5dsf35sgs5d6'/>"+
            "</hashtree>";
        
        // parse template...    
        ThexHashTree hashTree = ThexHashTreeCodec.parseThexHashTreeXML(
            new ByteArrayInputStream( xml.getBytes() ) );
        
        // generate xml...
        byte[] output = ThexHashTreeCodec.generateThexHashTreeXML( hashTree );
        
        // verify that XML is parsable
        hashTree = ThexHashTreeCodec.parseThexHashTreeXML(
            new ByteArrayInputStream( output ) );
    }
}
