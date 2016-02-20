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
package phex.xml;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.xml.bind.*;

import phex.common.file.*;
import phex.utils.IOUtil;
import phex.utils.NLogger;

/**
 * Helper class to create, generate and parse a xml Document from various
 * resources.
 * @author Gregor Koukkoullis
 */
public class XMLBuilder
{

    /**
     * Trys to load the XJBPhex marshalled object from the given file.
     * If the file doesn't exist, null is returned.
     * @param aFile the file to load the XJBPhex object from.
     * @return the XJBPhex object or null if file doesn't exist.
     */
    public static XJBPhex loadXJBPhexFromFile( ManagedFile managedFile )
        throws JAXBException
    {
        if ( !managedFile.getFile().exists() )
        {
            return null;
        }
        //NLogger.debug(NLoggerNames.GLOBAL, "Loading XJBPhex from: " + managedFile );
        XJBPhex phex;
        InputStream inStream = null;
        try
        {
            inStream = new ManagedFileInputStream( managedFile, 0 );

            // create a JAXBContext
            JAXBContext jc = JAXBContext.newInstance( "phex.xml" );

            // create an Unmarshaller
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            unmarshaller.setEventHandler( new TolerantValidationEventHandler() );
            phex = (XJBPhex)unmarshaller.unmarshal( inStream );
            return phex;
        }        
        finally
        {
            IOUtil.closeQuietly( inStream );
            IOUtil.closeQuietly( managedFile );
        }
    }
        
    /**
     * Trys to read the XJBPhex marshalled object from the given stream.
     * @param inStream the stream to read the XJBPhex object from.
     * @return the XJBPhex object.
     */
    public static XJBPhex readXJBPhexFromStream( InputStream inStream )
        throws JAXBException
    {
        XJBPhex phex;
        
        // create a JAXBContext
        JAXBContext jc = JAXBContext.newInstance( "phex.xml" );

        // create an Unmarshaller
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        unmarshaller.setEventHandler( new TolerantValidationEventHandler() );
        phex = (XJBPhex)unmarshaller.unmarshal( inStream );
        return phex;
    }
    
    public static byte[] serializeToBytes( XJBPhex xjbPhex )
        throws JAXBException
    {
        ByteArrayOutputStream bos = null;
    
        // create a JAXBContext
        JAXBContext jc = JAXBContext.newInstance( "phex.xml" );

        // marshal to file
        Marshaller m = jc.createMarshaller();
        m.setProperty( "jaxb.formatted.output", Boolean.TRUE );

        bos = new ByteArrayOutputStream( );
        m.marshal( xjbPhex, bos );
        return bos.toByteArray();
    }
    
    public static void saveToFile( ManagedFile managedFile, XJBPhex xjbPhex )
        throws JAXBException, ManagedFileException
    {
        ManagedFileOutputStream outStream = null;
        try
        {
            managedFile.setLength( 0 );
            outStream = new ManagedFileOutputStream( managedFile, 0 );
            
            // create a JAXBContext
            // JAXBContext needs a valid ClassLoader. Since we experience troubles
            // with class loader when in Thread comming from native (DesktopIndicator)
            // we make sure here that there is a valid class loader.
            ClassLoader cl = determineClassLoader();
            JAXBContext jc = JAXBContext.newInstance( "phex.xml", cl );

            // marshal to file
            Marshaller m = jc.createMarshaller();
            m.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
            m.marshal( xjbPhex, outStream );
        }
        finally
        {
            IOUtil.closeQuietly( outStream );
            IOUtil.closeQuietly( managedFile );
        }
    }
    
    private static ClassLoader determineClassLoader()
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if ( cl != null )
        {
            return cl;
        }
        cl = JAXBContext.class.getClassLoader();
        if ( cl != null )
        {
            return cl;
        }
        cl = ClassLoader.getSystemClassLoader();
        return cl;
    }
    
    private static class TolerantValidationEventHandler implements ValidationEventHandler
    {
        public boolean handleEvent( ValidationEvent e )
        {
            NLogger.debug( XMLBuilder.class, "Encountered validation event: "
                + e.getMessage() + " " + e.getSeverity() );
            return e.getSeverity() != ValidationEvent.FATAL_ERROR; 
        }
    }

    /**
     * @param fos
     * @throws IOException
     */
    // there are too many bugs in the J2SE to make this work reliable on 
    // different plattforms and files systems
    /*
    private static FileLock lockFile(FileChannel channel, long size, boolean shared) throws IOException
    {
        FileLock lock = null;
        int lockTryCount = 0;
        while (lock==null)
        {
            lock = channel.tryLock( 0, size, shared );
            if ( lock == null )
            {
                lockTryCount ++;
                if ( lockTryCount > 10 )
                {
                    throw new IOException( "Locking file failed.");
                }
                try
                {
                    Thread.sleep( 1000 );
                }
                catch (InterruptedException e)
                {
                    Thread.interrupted();
                }
            }
        }
        return lock;
    }
    */


    /*private static final DocumentBuilder documentBuilder;

    static
    {
        try
        {
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch ( ParserConfigurationException exp )
        {
            exp.printStackTrace();
            throw new RuntimeException( exp.getMessage() );
        }
    }*/

    /*public static XmlDocument createNewDocument()
    {
        return (XmlDocument) documentBuilder.newDocument();
    }

    public static Document loadFromSystemResource(String name)
    {
        InputStream stream = ClassLoader.getSystemResourceAsStream(name);
        if (stream == null)
        {
            return null;
        }
        BufferedInputStream bufferedStream = new BufferedInputStream(stream);
        return loadFromStream(bufferedStream);
    }

    public static Document loadFromResource( String resourceName )
    {
        InputStream stream =
            XMLBuilder.class.getResourceAsStream( resourceName );
        if ( stream != null )
        {
            BufferedInputStream bStream = new BufferedInputStream( stream );
            return loadFromStream( bStream );
        }
        else
        {
            System.out.println( "Can't find resource: " + resourceName + ".");
            return null;
        }
    }

    public static Document loadFromFile( File aFile )
    {
        BufferedInputStream bufferedStream = null;
        try
        {
            FileInputStream stream = new FileInputStream( aFile );
            if (stream == null)
            {
                return null;
            }
            bufferedStream = new BufferedInputStream(stream);
            // synchronize device... before reading...
            stream.getFD().sync();
            Document doc = loadFromStream(bufferedStream);

            return doc;
        }
        // TO DO should throw a exception instead... more clean!
        catch ( IOException exp )
        {
            Logger.logError( exp );
            return null;
        }
        finally
        {
            if ( bufferedStream != null )
            {
                try
                {
                    bufferedStream.close();
                }
                catch ( IOException exp )
                {
                }
            }
        }
    }*/

    /*public static void saveToFile( File aFile, XmlDocument doc )
        throws XMLException
    {
        BufferedWriter writer = null;
        try
        {
            // write backup file
            FileOutputStream fos = new FileOutputStream( aFile );
            writer = new BufferedWriter( new OutputStreamWriter( fos, "UTF8" ) );
            // synchronize device... before writing...
            fos.getFD().sync();
            doc.write( writer, "UTF-8" );
        }
        catch ( IOException exp )
        {
            Logger.logError( exp );
            throw new XMLException( exp.getMessage() );
        }
        finally
        {
            if ( writer != null )
            {
                try
                {
                    writer.close();
                }
                catch ( IOException exp )
                {
                }
            }
        }
    }

    public static Document loadFromStream(InputStream stream)
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try
        {
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(stream);
        }
        catch (SAXParseException exp)
        {
            Logger.logError( exp );
        }
        catch (Exception exp)
        {
            Logger.logError( exp );
        }
        return null;
    }*/
}
