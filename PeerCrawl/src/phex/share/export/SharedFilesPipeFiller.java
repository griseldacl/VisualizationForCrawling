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
 *  Created on 19.11.2004
 *  --- CVS Information ---
 *  $Id: SharedFilesPipeFiller.java,v 1.8 2005/11/03 16:33:48 gregork Exp $
 */
package phex.share.export;

import java.io.*;
import java.util.Iterator;
import java.util.Map;

import phex.common.URN;
import phex.common.address.DestAddress;
import phex.connection.NetworkManager;
import phex.share.ShareFile;
import phex.share.ShareManager;
import phex.share.SharedFilesService;
import phex.utils.*;
import phex.xml.XMLUtils;

/**
 * <shared-file-export>
 *   <export-options>
 *     <option name='UseMagnetURLWithXS'>true</option>
 *   </export-options>
 *   <shared-file-list>
 *     <shared-file> 
 *       <index>1</index>
 *       <name>phex_2.0.0.76.exe</name>
 *       <search-compare-term>phex 2.0.0.76.exe</search-compare-term>
 *       <search-count>100</search-count>
 *       <sha1>T2SXTXXCTJKIDMDVONPRHPXH4NOZRBT4</sha1>
 *       <upload-count>10</upload-count>
 *       <file-size>2252314</file-size>
 *       <urn>urn:sha1:T2SXTXXCTJKIDMDVONPRHPXH4NOZRBT4</urn>
 *     
 *       ... for more check the source...  
 * 
 *     </shared-file>
 *   </shared-file-list>
 * </shared-file-export>
 */
public class SharedFilesPipeFiller implements Runnable
{
    private Writer utf8Writer;
    private Map exportOptions;
    
    public SharedFilesPipeFiller( OutputStream outStream, Map options )
    {
        try
        {
            utf8Writer = new OutputStreamWriter( outStream, "UTF-8" );
        }
        catch (UnsupportedEncodingException e)
        {
            assert( false );
        }
        exportOptions = options;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
        try
        {
            DestAddress localAddress = NetworkManager.getInstance().getLocalAddress();
            utf8Writer.write("<shared-file-export>" );
            if ( exportOptions != null && exportOptions.size() > 0 )
            {
                utf8Writer.write("<export-options>");
                Iterator optionIterator = exportOptions.keySet().iterator();
                while( optionIterator.hasNext() )
                {
                    String optionName = (String)optionIterator.next();
                    utf8Writer.write("<option name='" + optionName +  "'>");
                    utf8Writer.write( exportOptions.get(optionName).toString() );
                    utf8Writer.write("</option>");
                }
                utf8Writer.write("</export-options>");
            }
            utf8Writer.write("<shared-file-list>");
            SharedFilesService sharedFilesService = ShareManager.getInstance().getSharedFilesService();
            ShareFile[] sharedFiles = sharedFilesService.getSharedFiles();
            for (int i = 0; i < sharedFiles.length; i++)
            {
                ShareFile file = sharedFiles[i];
                
                URN urn = file.getURN();
                if ( urn == null )
                {// we skip reporting of null urn files... they dont make sense...
                    continue;
                }
                
                utf8Writer.write("<shared-file>");
                utf8Writer.write("<index>"); 
                XMLUtils.writeEncoded( utf8Writer, String.valueOf( file.getFileIndex() ) ); 
                utf8Writer.write("</index>");
                
                utf8Writer.write("<name>");
                XMLUtils.writeEncoded( utf8Writer, file.getFileName() ); 
                utf8Writer.write("</name>");
                
                utf8Writer.write("<name-urlenc>");
                XMLUtils.writeEncoded( utf8Writer, 
                    URLCodecUtils.encodeURL( file.getFileName() ) ); 
                utf8Writer.write("</name-urlenc>");
                
                utf8Writer.write("<search-compare-term>");
                XMLUtils.writeEncoded( utf8Writer, String.valueOf( file.getSearchCompareTerm() ) ); 
                utf8Writer.write("</search-compare-term>>");
                
                utf8Writer.write("<search-count>");
                XMLUtils.writeEncoded( utf8Writer, String.valueOf( file.getSearchCount() ) );
                utf8Writer.write("</search-count>");
                
                utf8Writer.write("<sha1>");
                XMLUtils.writeEncoded( utf8Writer, file.getSHA1() );
                utf8Writer.write("</sha1>");
                
                utf8Writer.write("<upload-count>");
                XMLUtils.writeEncoded( utf8Writer, String.valueOf( file.getUploadCount() ) ); 
                utf8Writer.write("</upload-count>");
                
                utf8Writer.write("<file-size>");
                XMLUtils.writeEncoded( utf8Writer, String.valueOf( file.getFileSize() ) ); 
                utf8Writer.write("</file-size>");
                
                utf8Writer.write("<urn>"); 
                XMLUtils.writeEncoded( utf8Writer, urn.getAsString() ); 
                utf8Writer.write("</urn>");
                
                utf8Writer.write("<magnet-url>");
                if ( exportOptions != null && 
                     "true".equals( exportOptions.get( ExportEngine.USE_MAGNET_URL_WITH_XS ) ) )
                {
                    XMLUtils.writeEncoded( utf8Writer, URLUtil.buildMagnetURLWithXS( 
                        file.getSHA1(), file.getFileName(), localAddress ) );
                }
                else
                {
                    XMLUtils.writeEncoded( utf8Writer, URLUtil.buildMagnetURL( 
                        file.getSHA1(), file.getFileName() ) );
                }
                utf8Writer.write("</magnet-url>");
                
                utf8Writer.write("<name2res-url>"); 
                XMLUtils.writeEncoded( utf8Writer, URLUtil.buildFullName2ResourceURL( 
                    localAddress, urn ) ); 
                utf8Writer.write("</name2res-url>");
                
                utf8Writer.write("</shared-file>");
            }
            utf8Writer.write("</shared-file-list>");
            utf8Writer.write("</shared-file-export>" );
        }
        catch ( IOException exp )
        {
            NLogger.error( NLoggerNames.Sharing , exp, exp );
        }
        finally
        {
            IOUtil.closeQuietly(utf8Writer);
        }
    }
    
}
