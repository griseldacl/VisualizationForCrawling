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
 *  $Id: ExportEngine.java,v 1.5 2005/10/03 00:18:28 gregork Exp $
 */
package phex.share.export;

import java.io.*;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import phex.common.ThreadPool;
import phex.utils.IOUtil;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

/**
 *
 */
public class ExportEngine
{
    public static final String USE_MAGNET_URL_WITH_XS = "UseMagnetURLWithXS";
    private Map exportOptions = null;
    
    /**
     * Known options:
     *  - UseMagnetURLWithXS = true
     * @param options
     */
    public void setExportOptions( Map options )
    {
        exportOptions = options;
    }
    
    public void startExport( InputStream styleSheetStream, OutputStream destinationStream )
    {
        PipedOutputStream pipedOutStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream();
        try
        {
            pipedOutStream.connect( pipedInputStream );
            
            SharedFilesPipeFiller fillerRunnable = new SharedFilesPipeFiller( pipedOutStream,
                exportOptions );
            ThreadPool.getInstance().addJob( fillerRunnable, "SharedFilesPipeFiller" );
    
            
            StreamSource styleSheetSource = new StreamSource( styleSheetStream );
            StreamSource dataSource = new StreamSource( pipedInputStream );
            StreamResult result = new StreamResult( destinationStream );
            try
            {
                Transformer transformer = TransformerFactory.newInstance()
                    .newTransformer(styleSheetSource);
                transformer.transform(dataSource, result);
            }
            catch (TransformerException exp)
            {
                NLogger.error( NLoggerNames.Sharing , exp, exp );
            }
        }
        catch ( IOException exp )
        {
            NLogger.error( NLoggerNames.Sharing , exp, exp );
        }
        finally
        {
            IOUtil.closeQuietly( pipedInputStream );
            IOUtil.closeQuietly( pipedOutStream );
        }
    }
}