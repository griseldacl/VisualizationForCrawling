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
 *  Created on 13.12.2004
 *  --- CVS Information ---
 *  $Id: UrnCalculationWorker.java,v 1.7 2005/11/19 14:39:36 gregork Exp $
 */
package phex.share;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;

import phex.common.AlternateLocationContainer;
import phex.common.ServiceManager;
import phex.common.URN;
import phex.download.swarming.SWDownloadFile;
import phex.download.swarming.SwarmingManager;
import phex.utils.IOUtil;
import phex.utils.Logger;

import com.bitzi.util.Base32;
import com.bitzi.util.SHA1;


class UrnCalculationWorker implements Runnable
{
    private ShareFile shareFile;
    
    UrnCalculationWorker( ShareFile shareFile )
    {
        this.shareFile = shareFile;
    }
    
    public void run()
    {
        boolean succ = calculateURN();
        // if calculation succeded
        if ( succ )
        {        
            SharedFilesService sharedFilesService = ShareManager.getInstance().getSharedFilesService();
            // add the urn to the map to share by urn
            sharedFilesService.addUrn2FileMapping( shareFile );
            sharedFilesService.triggerSaveSharedFiles();
        }
    }
    
    /**
     * Calculates the URN of the file for HUGE support. This method can take
     * some time for large files. For URN calculation a SHA-1 digest is created
     * over the complete file and the SHA-1 digest is translated into a Base32
     * representation.
     */
    private boolean calculateURN()
    {
        int urnCalculationMode = ServiceManager.sCfg.urnCalculationMode;
        FileInputStream inStream = null;
        try
        {
            inStream = new FileInputStream( shareFile.getSystemFile() );
            MessageDigest messageDigest = new SHA1();
            byte[] buffer = new byte[64 * 1024];
            int length;
            long start = System.currentTimeMillis();
            long start2 = System.currentTimeMillis();
            while ((length = inStream.read(buffer)) != -1)
            {
                messageDigest.update(buffer, 0, length);
                long end2 = System.currentTimeMillis();
                try
                {
                    Thread.sleep((end2 - start2) * urnCalculationMode);
                }
                catch (InterruptedException exp)
                {
                    // reset interrupted flag
                    Thread.currentThread().interrupt();
                    return false;
                }
                start2 = System.currentTimeMillis();
            }
            inStream.close();
            byte[] shaDigest = messageDigest.digest();
            long end = System.currentTimeMillis();
            URN urn = new URN("urn:sha1:" + Base32.encode(shaDigest));
            shareFile.setURN( urn );
            if ( Logger.isLevelTypeLogged( Logger.FINEST, Logger.UPLOAD ) )
            {
                Logger.logMessage(Logger.FINEST, Logger.UPLOAD, "SHA1 time: "
                    + (end - start) + " size: " + shareFile.getSystemFile().length());
            }
            
            // check if we find a download with the same urn and capture alt locs
            // from it
            SwarmingManager swarmingMgr = SwarmingManager.getInstance();
            SWDownloadFile file = swarmingMgr.getDownloadFileByURN( urn );
            if ( file != null )
            {
                AlternateLocationContainer altCont = file.getGoodAltLocContainer();
                shareFile.getAltLocContainer().addContainer( altCont );
            }
            
            return true;
        }
        catch (IOException e)
        {// dont care... no urn could be calculated...
            Logger.logMessage(Logger.FINE, Logger.UPLOAD, e);
            return false;
        }
        finally
        {
            IOUtil.closeQuietly( inStream );
        }
    }
}