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
 *  Created on 17.02.2005
 *  --- CVS Information ---
 *  $Id: DirectByteBufferProvider.java,v 1.3 2005/10/03 00:18:29 gregork Exp $
 */
package phex.utils;

import java.nio.ByteBuffer;
import java.util.*;

import phex.common.Environment;

/**
 * TODO find out the usuall requested buffer sizes and provide storage slots for 
 * these sizes.
 */
public class DirectByteBufferProvider
{
    public static final int BUFFER_SIZE_64K = 64 * 1024;
    private static final DirectByteBufferProvider instance;

    private Map sizeToBufferListMap;

    /**
     * Max use 10MB in buffer cache.
     */
    private static final long MAX_TOTAL_BUFFER_SIZE = 10 * 1024 * 1024;

    static
    {
        instance = new DirectByteBufferProvider();
    }

    private DirectByteBufferProvider()
    {
        sizeToBufferListMap = new LinkedHashMap();
        
        Environment.getInstance().scheduleTimerTask(new CleanupCheckerTask(),
            CleanupCheckerTask.TIMER_PERIOD, CleanupCheckerTask.TIMER_PERIOD);
    }

    public static DirectByteBuffer requestBuffer(int sizeInBytes)
    {
        return instance.requestDirectByteBuffer(sizeInBytes);
    }

    protected synchronized void releaseDirectByteBuffer(
        DirectByteBuffer directByteBuffer)
    {
        //NLogger.debug( NLoggerNames.GLOBAL, "Releasing DirectByteBuffer:" + directByteBuffer );
        debugDump();
        ByteBuffer buffer = directByteBuffer.getInternalBuffer();
        int kb = (int) Math.ceil(buffer.capacity() / 1024.0);
        Integer key = new Integer(kb);
        Set set = (Set) sizeToBufferListMap.get(key);
        if (set == null)
        {
            set = new HashSet();
            sizeToBufferListMap.put(key, set);
        }
        set.add(directByteBuffer);
        debugDump();
    }

    private synchronized DirectByteBuffer requestDirectByteBuffer(int capacity)
    {
        //NLogger.debug( NLoggerNames.GLOBAL, "Requesting DirectByteBuffer:" + capacity );
        debugDump();
        
        int kb = (int) Math.ceil(capacity / 1024.0);
        Integer key = new Integer(kb);
        Set set = (Set) sizeToBufferListMap.get(key);
        if (set == null)
        {
            set = new HashSet();
            sizeToBufferListMap.put(key, set);
        }
        DirectByteBuffer directByteBuffer;
        if (set.isEmpty())
        {
            directByteBuffer = allocateDirectByteBuffer(kb * 1024);
        }
        else
        {
            directByteBuffer = (DirectByteBuffer) set.iterator().next();
            set.remove(directByteBuffer);
        }
        ByteBuffer buffer = directByteBuffer.getInternalBuffer();
        buffer.clear();
        buffer.limit(capacity);
        
        //NLogger.debug( NLoggerNames.GLOBAL, "Requested DirectByteBuffer:" + directByteBuffer );
        debugDump();
        return directByteBuffer;
    }

    /**
     * @param kb
     * @return
     */
    private synchronized DirectByteBuffer allocateDirectByteBuffer(int capacity)
    {
        ByteBuffer byteBuffer;
        try
        {
            byteBuffer = ByteBuffer.allocateDirect(capacity);
        }
        catch (OutOfMemoryError err)
        {
            clearBuffers();
            System.runFinalization();
            System.gc();
            Thread.yield();
            try
            {
                byteBuffer = ByteBuffer.allocateDirect(capacity);
            }
            catch (OutOfMemoryError err2)
            {
                NLogger
                    .error(NLoggerNames.GLOBAL,
                        "Out of memory while trying to allocated direct byte buffer.");
                throw err2;
            }
        }
        DirectByteBuffer directByteBuffer = new DirectByteBuffer(byteBuffer,
            this);
        return directByteBuffer;
    }

    /**
     * Clears available buffers of the provider.
     */
    private synchronized void clearBuffers()
    {
        Iterator iterator = sizeToBufferListMap.values().iterator();
        while (iterator.hasNext())
        {
            List list = (List) iterator.next();
            list.clear();
        }
    }

    private synchronized void cleanupChecker()
    {
        long bytesUsed = 0;
        Iterator iterator = sizeToBufferListMap.keySet().iterator();
        while (iterator.hasNext())
        {
            Integer key = (Integer) iterator.next();
            HashSet set = (HashSet) sizeToBufferListMap.get(key);

            bytesUsed += key.intValue() * set.size() * 1024;
        }

        if (bytesUsed > MAX_TOTAL_BUFFER_SIZE)
        {
            performCleanup(bytesUsed - MAX_TOTAL_BUFFER_SIZE);
        }
    }

    private synchronized void performCleanup(long bytesToFree)
    {
        // we remove buffers from sizes with the highest amount of cached buffers
        int mapSize = sizeToBufferListMap.size();
        int highestCount = 0;
        int[] bufferSizeArr = new int[mapSize];
        List[] bufferListArr = new List[mapSize];

        int i = 0;
        Iterator listIterator = sizeToBufferListMap.keySet().iterator();
        while (listIterator.hasNext())
        {
            Integer key = (Integer) listIterator.next();
            List list = (List) sizeToBufferListMap.get(key);
            bufferSizeArr[i] = key.intValue() * 1024;
            bufferListArr[i] = list;
            int listSize = list.size();
            if (listSize > highestCount)
            {
                highestCount = listSize;
            }
            i++;
        }

        long bytesFreed = 0;
        while (bytesFreed < bytesToFree && highestCount > 0)
        {
            for (i = 0; i < mapSize; i++)
            {
                // if this is the largest buffer list remove 
                // one buffer.
                if (bufferListArr[i].size() == highestCount)
                {
                    bufferListArr[i].remove(bufferListArr[i].size() - 1);
                    bytesFreed += bufferSizeArr[i];
                }
            }
            highestCount--;
        }
    }
    
    private synchronized void debugDump()
    {
//        Iterator keys = sizeToBufferListMap.keySet().iterator();
//        while ( keys.hasNext() )
//        {
//            Integer key = (Integer) keys.next();
//            System.out.println( "------------------------" );
//            System.out.println( "Key: " + key.intValue() );
//            Set keySet = (Set) sizeToBufferListMap.get(key);
//            Iterator bufferIterator = keySet.iterator();
//            while ( bufferIterator.hasNext() )
//            {
//                DirectByteBuffer buffer = (DirectByteBuffer) bufferIterator.next();
//                System.out.println( buffer );
//            }
//        }
    }

    private class CleanupCheckerTask extends TimerTask
    {
        private static final int TIMER_PERIOD = 10 * 60 * 1000;

        /**
         * @see java.util.TimerTask#run()
         */
        public void run()
        {
            cleanupChecker();
        }

    }
}