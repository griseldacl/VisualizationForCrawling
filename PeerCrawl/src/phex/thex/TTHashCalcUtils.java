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
 *  Created on 02.06.2005
 *  --- CVS Information ---
 *  $Id: TTHashCalcUtils.java,v 1.3 2005/10/03 00:18:29 gregork Exp $
 */
package phex.thex;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;

import phex.common.ServiceManager;
import phex.common.format.NumberFormatUtils;
import phex.share.ShareFile;
import phex.utils.*;

import com.bitzi.util.Tiger;
import com.bitzi.util.TigerTree;

/**
 *
 */
public class TTHashCalcUtils
{
    private static transient final byte MERKLE_IH_PREFIX = 0x01;
    private static final int THEX_BLOCK_SIZE = 1024;
    
    /**
     * Calculates the ShareFileThexData of the given ShareFile.
     */
    public static void calculateShareFileThexData( ShareFile shareFile )
        throws IOException 
    {
        if ( shareFile.getThexData( false ) != null )
        {
            return;
        }
        
        long fileSize = shareFile.getFileSize();
        int levels = getTreeLevels( fileSize );
        int nodeSize = getTreeNodeSize( fileSize, levels );
        
        BufferedInputStream inStream = new BufferedInputStream( 
            new FileInputStream( shareFile.getSystemFile() ) );
        List lowestLevelNodes = calculateTigerTreeNodes( nodeSize, fileSize, inStream );
        
        // calculate the full Merkle tree.
        List merkleTreeNodes = calculateMerkleParentNodes( lowestLevelNodes );
        byte[] rootHash = (byte[])((List)merkleTreeNodes.get(0)).get(0);
        int depth = merkleTreeNodes.size() - 1;
        
        ShareFileThexData data = new ShareFileThexData( shareFile, rootHash, lowestLevelNodes, 
            depth );
        shareFile.setThexData( data );
    }
    
    /**
     * Returns the number of levels to use for the hash tree.
     * We use a static table, this is much faster then the original level loop.
     * @param fileSize
     * @return the number of levels.
     */
    private static int getTreeLevels(long fileSize)
    {
        if (fileSize < 256 * NumberFormatUtils.ONE_KB)
            return 0;
        else if (fileSize < 512 * NumberFormatUtils.ONE_KB)
            return 1;
        else if (fileSize < NumberFormatUtils.ONE_MB)
            return 2;
        else if (fileSize < 2 * NumberFormatUtils.ONE_MB)
            return 3;
        else if (fileSize < 4 * NumberFormatUtils.ONE_MB)
            return 4;
        else if (fileSize < 8 * NumberFormatUtils.ONE_MB)
            return 5;
        else if (fileSize < 16 * NumberFormatUtils.ONE_MB)
            return 6;
        else if (fileSize < 32 * NumberFormatUtils.ONE_MB)
            return 7;
        else if (fileSize < 64 * NumberFormatUtils.ONE_MB)
            return 8;
        else if (fileSize < 256 * NumberFormatUtils.ONE_MB)
            return 9;
        else if (fileSize < 1024 * NumberFormatUtils.ONE_MB)
            return 10;
        else
            return 11;
    }

    /**
     * Returns the tree node size.
     * The result should be the next larger power of 2 from the file size.  
     */
    private static int getTreeNodeSize(long fileSize, int depth)
    {
        // max number of nodes
        int nodes = (int)Math.pow(2, depth);
        
        // "calculated" file part node size
        int fileNodeSize = (int)Math.ceil( (double)fileSize / (double)nodes );
        
        // Node size must be the next larger power of 2 from the file size.
        int pow = IOUtil.calculateCeilLog2( fileNodeSize );
        int nodeSize = (int)Math.pow(2, pow);
        return nodeSize;
    }
    
    
    /**
     * Calcualtes the TigerTree nodes with the given nodeSize. 
     * 
     * @param nodeSize Must be 2^n (n>=10) for Merkle HashTree
     */
    private static List calculateTigerTreeNodes( int nodeSize, long fileSize,
        InputStream inStream ) throws IOException
    {
        int thexCalculationMode = ServiceManager.sCfg.thexCalculationMode;
        int nodeCount = (int)Math.ceil((double)fileSize/(double)nodeSize);
        ArrayList nodeList = new ArrayList( nodeCount );
        
        MessageDigest tigerTreeDigest = new TigerTree();
        long totalRead = 0;
        int readCount = 0;
        byte[] buffer = new byte[THEX_BLOCK_SIZE * 128];
        while ( totalRead < fileSize && readCount != -1)
        {
            tigerTreeDigest.reset();
            int nodePos = 0;
            // read out of an single node... and add digest to nodeList
            long start = System.currentTimeMillis();
            while ( nodePos < nodeSize 
                    && (readCount = inStream.read( buffer ) ) != -1 )
            {
                tigerTreeDigest.update( buffer, 0, readCount );
                nodePos += readCount;
                totalRead += readCount;
                try
                {
                    long end = System.currentTimeMillis();
                    Thread.sleep( (end - start) * thexCalculationMode );
                }
                catch (InterruptedException ie)
                {
                    // reset interrupted flag
                    Thread.currentThread().interrupt();
                    throw new IOException( "Hashing file interrupted." );
                }
                start = System.currentTimeMillis();
            }
            nodeList.add(tigerTreeDigest.digest());
            
            if ( readCount == -1 && totalRead != fileSize )
            {
                NLogger.error( NLoggerNames.Thex_Calculation, "Hashing file failed.");
                throw new IOException( "Hashing file failed." );
            }
        }
        return nodeList;
    }
    
    /*
     * Iterative method to generate the parent nodes of an arbitrary
     * depth.
     *
     * The 0th element of the returned List will always be a List of size
     * 1, containing a byte[] of the root hash.
     */
    public static List calculateMerkleParentNodes( List childNodes )
    {
        List merkleTreeNodes = new ArrayList();
        merkleTreeNodes.add( Collections.unmodifiableList( childNodes ) );
        
        List tmpNodes = childNodes;
        while ( tmpNodes.size() > 1 )
        {
            // calculate Merkle hash tree parent for node
            MessageDigest md = new Tiger();
            int size = (int) Math.ceil(tmpNodes.size() / 2.0);
            List parentNodes = new ArrayList(size);
            Iterator iterator = tmpNodes.iterator();
            while ( iterator.hasNext() )
            {
                byte[] left = (byte[]) iterator.next();
                if ( iterator.hasNext() )
                {
                    byte[] right = (byte[]) iterator.next();
                    md.reset();
                    md.update(MERKLE_IH_PREFIX);
                    md.update(left, 0, left.length);
                    md.update(right, 0, right.length);
                    byte[] result = md.digest();
                    parentNodes.add(result);
                }
                else
                {
                    parentNodes.add(left);
                }
            }
            merkleTreeNodes.add(0, parentNodes);
            tmpNodes = parentNodes;
        }
        return merkleTreeNodes;
    }
}
