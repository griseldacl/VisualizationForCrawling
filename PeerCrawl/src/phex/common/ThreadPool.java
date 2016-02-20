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
package phex.common;

import java.util.LinkedList;

import phex.utils.Logger;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

/**
 * Manage a set of threads that will process a series of runnables.
 */
public class ThreadPool
{
    private static ThreadPool instance;

    /**
     * Use this to get the static thread pool instance.
     */
    public static ThreadPool getInstance()
    {
        if(instance == null)
        {
            instance = new ThreadPool();
        }
        return instance;
    }

    private long suicideTime;
    private long spawnTime;
    private LinkedList jobs;
    private int activeThreads;
    private boolean askedToStop;

    /**
     * <p>Creates a new thread pool.</p>
     *
     * <p>You will normally use ThreadPool.getInstance() to retrieve the global
     * thread pool. However, if you require an independent job queue and list of
     * threads, then you can invoke the constructor directly to get an independent
     * instance.</p>
     */
    public ThreadPool()
    {
        // 20 seconds of inactivity and a worker gives up
        suicideTime = 20000;
        // half a second for the oldest job in the queue before
        // spawning a worker to handle it
        spawnTime = 500;
        jobs = null;
        activeThreads = 0;
        askedToStop = false;
    }

    public void shutDown()
    {
        synchronized(this)
        {
            askedToStop = true;
        }
    }

    public Job addJob( Runnable jobRunnable, String jobName )
    {
        synchronized(this)
        {
            // first job ever - make a worker and the global job monitor
            if( jobs == null )
            {
                jobs = new LinkedList();
                Worker w = new Worker();
                w.setDaemon( false );
                w.start();

                JobMonitor jm = new JobMonitor();
                jm.setDaemon( true );
                jm.start();
            }
            Job job = new Job( jobRunnable, jobName );
            jobs.addLast( job );
            notifyAll();
            return job;
        }
    }

    public static void trackThreadHandling()
    {
        Logger.logMessage( Logger.FINEST, Logger.GLOBAL,
                           "Active Threads in pool: " + instance.activeThreads );
    }

    public class Job
    {
        private final Runnable runnable;
        private final String jobName;
        private final long requestTime;
        private Thread associatedThread;

        public Job(Runnable runnable, String aJobName )
        {
            this.runnable = runnable;
            jobName = aJobName;
            this.requestTime = System.currentTimeMillis();
        }

        public Runnable getRunnable()
        {
            return runnable;
        }

        public String getJobName()
        {
            return jobName;
        }

        public long getRequestTime()
        {
            return requestTime;
        }
        
        /**
         * Returns the associated thread, if available.
         * @return
         */
        public synchronized Thread getAssociatedThread()
        {
            return associatedThread;
        }
        
        private synchronized void associateThread( Thread thread )
        {
            associatedThread = thread;
        }
        
        private synchronized void releaseAssociateThread()
        {
            if ( associatedThread == null )
            {
                return;
            }
            Thread temp = associatedThread;
            associatedThread = null;
            synchronized ( temp )
            {
                temp.notifyAll();
            }
        }
        
        public void waitForAssociatedThreadRelease()
        {
            try
            {
                synchronized ( associatedThread )
                {
                    while ( associatedThread != null )
                    {
                        associatedThread.wait( 5000 );
                    }
                }
            }
            catch (InterruptedException e)
            {
                NLogger.error( NLoggerNames.Download_Worker, e, e );
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Create new worker threads if any job has been sitting in the job for longer
     * than 'spawnTime' (1/2 second), create a new worker to handle it.
     */
    private class JobMonitor extends Thread
    {
        JobMonitor()
        {
            super( ThreadTracking.threadPoolGroup, "JobMonitor" );
            setPriority(Thread.NORM_PRIORITY);
        }

        public void run()
        {
            while( true )
            {
                synchronized( ThreadPool.this )
                {
                    if( !jobs.isEmpty() )
                    {
                        Job job = (Job) jobs.getFirst();
                        long time = System.currentTimeMillis();
                        long diff = time - job.getRequestTime();
                        if(diff > spawnTime)
                        {
                            Worker w = new Worker();
                            w.setDaemon(false);
                            w.start();
                        }
                    }
                    try
                    {
                        ThreadPool.this.wait(100);
                    }
                    catch (InterruptedException ie)
                    {
                    }
                }
            }
        }
    }

    private class Worker extends Thread
    {
        Worker()
        {
            super( ThreadTracking.threadPoolGroup, "ThreadPool.Worker" );
            setPriority(Thread.NORM_PRIORITY);
        }

        public void run()
        {
        	Job job = null;
            activeThreads ++;
            while(true)
            {
                synchronized( ThreadPool.this )
                {
                    long startedWaiting = System.currentTimeMillis();
                    
                    // As long as no jobs are waiting, check to see if it's time to end this thread.
                    while( jobs.isEmpty() )
                    {
                        // if inactive for too long then give up
                        long timePassed = System.currentTimeMillis() - startedWaiting;
                        long timeLeft = suicideTime - timePassed;
                        if( timeLeft < 0 )
                        {
                            activeThreads --;
                            return;
                        }

                        try
                        {
                            // wait for a job until we should really be dying
                            ThreadPool.this.wait(timeLeft);
                        }
                        catch (InterruptedException ie)
                        {
                            // don't care why - lets loop till we have something to do
                        }
                    }
                    if( askedToStop )
                    {
                        activeThreads --;
                        return;
                    }
                    job = (Job) jobs.removeFirst();
                }
                try
                {
                    job.associateThread( this );
                    setName( job.getJobName() );
                    job.getRunnable().run();
                }
                catch (Throwable t)
                {
                    NLogger.error( NLoggerNames.GLOBAL, t, t);
                }
                finally
                {
                    job.releaseAssociateThread();
                }
                setName( "ThreadPool.Worker-" +
                    Integer.toHexString( hashCode() ) );
            }
        }
    }

/*    public static void main( String args[] )
            throws Exception
    {
        ThreadPool pool = ThreadPool.getInstance();

        for ( int i = 0; i < 100; i++ )
        {
            pool.addJob( new Runnable()
            {
                public void run()
                {
                    try
                    {
                        System.out.println( "Start Thread" );
                        Thread.sleep( 10000 );
                        System.out.println( "Stop Thread" );
                    }
                    catch ( Exception exp )
                    {}
                }
            }, "Test" );
            System.out.println( pool.jobs.size() + " " + pool.activeThreads);
            Thread.sleep( 600 );
            System.out.println( pool.jobs.size() + " " + pool.activeThreads);
            Thread.sleep( 600 );
        }
        while ( pool.activeThreads > 0 )
        {
            System.out.println( pool.jobs.size() + " " + pool.activeThreads);
            Thread.sleep( 1000 );
        }
    }*/
}