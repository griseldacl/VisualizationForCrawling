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
 *  $Id: RunnerQueueWorker.java,v 1.7 2005/11/19 14:44:07 gregork Exp $
 */
package phex.common;

import java.util.Vector;

import phex.utils.NLogger;
import phex.utils.NLoggerNames;

/**
 * This class represents a queue of Runnables that will
 * get served one after another.
 * A own thread is used for execusion.
 */
public class RunnerQueueWorker
{
    private boolean isInterrupted;
    private Vector queue;
    private Thread runnerThread;
    
    public RunnerQueueWorker()
    {
        queue = new Vector();
        isInterrupted = false;
    }
    
    /**
     * Clears the queue.
     */
    public synchronized void stopAndClear()
    {
       queue.clear();
       if ( runnerThread != null )
       {
           runnerThread.interrupt();
           isInterrupted = true;
       }
    }
    
    /**
     * Adds a runnable to be processed.
     */
    public synchronized void add(Runnable runable)
    {
        queue.add( runable );
        notify();
        if( runnerThread == null )
        {
            createRunner();
        }
    }

    private synchronized void createRunner()
    {
        isInterrupted = false;
        runnerThread = new Thread( new QueueWorker() );
        runnerThread.setPriority(Thread.NORM_PRIORITY);
        runnerThread.setDaemon( true );
        runnerThread.start();
    }
    
    private class QueueWorker implements Runnable 
    {
        public void run() 
        {
            try
            {
                while( true ) 
                {
                    Runnable next = (Runnable)queue.remove(0);
                    try
                    {
                        next.run();
                    }
                    catch ( Throwable th )
                    {
                        NLogger.error(NLoggerNames.GLOBAL, th, th);
                    }
                    
                    synchronized(RunnerQueueWorker.this) 
                    {
                        if( !queue.isEmpty() && !isInterrupted )
                        {
                            continue;
                        }
                        try 
                        {
                            // wait a short while for possible notify
                            RunnerQueueWorker.this.wait(5 * 1000);
                        } 
                        catch(InterruptedException exp) 
                        {//ignore and take next from queue
                         // if its still full stopAndClear()
                        }
                        if( !queue.isEmpty() && !isInterrupted )
                        {
                            continue;
                        }
                        runnerThread = null;
                        break;
                    }
                }
            } 
            catch ( Throwable th )
            {
                runnerThread = null;
                NLogger.error( NLoggerNames.GLOBAL, th, th );
            }
            // safty check
            if ( !queue.isEmpty() )
            {// oups... somebody is left we need to restart..
                createRunner();
            }
        }
    }
}