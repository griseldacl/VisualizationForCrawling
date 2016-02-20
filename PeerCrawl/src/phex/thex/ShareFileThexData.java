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
 *  Created on 17.06.2005
 *  --- CVS Information ---
 *  $Id: ShareFileThexData.java,v 1.3 2005/10/03 00:18:29 gregork Exp $
 */
package phex.thex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import org.apache.commons.codec.binary.Base64;

import phex.share.ShareFile;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

import com.bitzi.util.Base32;

/**
 *
 */
public class ShareFileThexData
{
    private ShareFile shareFile;
    private String rootHash;
    private List /*<byte[]>*/ lowestLevelNodes;
    private int treeDepth;
    

    /**
     * 
     */
    public ShareFileThexData( ShareFile shareFile, byte[] rootHash, 
        List lowestLevelNodes, int depth )
    {
        this.shareFile = shareFile;
        this.rootHash = Base32.encode( rootHash );
        this.lowestLevelNodes = lowestLevelNodes;
        this.treeDepth = depth;
    }
    
    public ShareFileThexData( ShareFile shareFile, String rootHash, 
        String xjbLowestLevelNodes, int depth )
    {
        this.shareFile = shareFile;
        this.rootHash = rootHash;
        this.lowestLevelNodes = parseXJBLowestLevelNodes( xjbLowestLevelNodes );
        this.treeDepth = depth;
    }
    
    public String getRootHash()
    {
        return rootHash;
    }
    
    public int getTreeDepth()
    {
        return treeDepth;
    }
    
    public byte[] getSerializedTreeNodes()
    {// TODO2 validate how often this is called and if it makes sense to store
     // array temporary in WeakReference
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        List allNodes = TTHashCalcUtils.calculateMerkleParentNodes( 
            lowestLevelNodes );
        Iterator iterator = allNodes.iterator();
        try
        {
            while ( iterator.hasNext() )
            {
                Iterator subIterator = ((List) iterator.next()).iterator();
                while ( subIterator.hasNext() )
                {
                    outStream.write( (byte[]) subIterator.next() );
                }
            }
        }
        catch (IOException exp)
        {// this should never happen!
            NLogger.error(NLoggerNames.GLOBAL, exp, exp );
            throw new RuntimeException(exp);
        }
        return outStream.toByteArray();
    }
    
    public String getXJBLowestLevelNodes()
    {
        Iterator iterator = lowestLevelNodes.iterator();
        StringBuffer xjbString = new StringBuffer();
        while( iterator.hasNext() )
        {
            byte[] nodeData = (byte[])iterator.next();
            String node = new String( Base64.encodeBase64( nodeData ) );
            xjbString.append( node );
            xjbString.append( "-" );
            
        }
        return xjbString.toString();
    }
    
    /**
     * @param rootHash2
     * @param xjbLowestLevelNodes
     * @param depth
     */
    public void updateFromCache( String rootHash, String xjbLowestLevelNodes, int depth )
    {
        this.rootHash = rootHash;
        this.lowestLevelNodes = parseXJBLowestLevelNodes( xjbLowestLevelNodes );
        this.treeDepth = depth;
    }
    
    private static List parseXJBLowestLevelNodes( String xjbString )
    {
        StringTokenizer tokenizer = new StringTokenizer( xjbString, "-");
        List list = new ArrayList();
        while ( tokenizer.hasMoreTokens() )
        {
            String node = tokenizer.nextToken();
            byte[] nodeData = Base64.decodeBase64( node.getBytes() );
            list.add( nodeData );
        }
        return list;
    }
    
    
    
//    public int calculateTotalNodeCount()
//    {
//        int prev = lowestLevelNodes.size();
//        int count = prev;
//        for ( int i = treeDepth - 1; i >= 0; i++ )
//        {
//            prev = (int)Math.ceil( prev / 2.0 );
//            count += prev;
//        }
//        return count;
//    }
}
