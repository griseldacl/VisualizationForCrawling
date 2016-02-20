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
package phex.event;

import java.util.*;


import phex.common.*;
import phex.utils.*;

/**
 * The responsibility of this class is to have a central place where Events are
 * dispatched. Events are either send over the AWT event dispatcher after
 * all pending events are processed or are dispatched by a internal thread
 * responsible for dispatching the event asynchronously.
 *
 * The main reason this class was intgrated was to provide the possibility to
 * dispatch events not using the AWT event dispatcher. This can be very usefull
 * if there is no GUI attached. To disable the AWT event dispatcher and enable
 * the internal dispatcher thread call the method enableDispatcherThread() on
 * initializing.
 */
public final class AsynchronousDispatcher
{
    private static DispatcherRunnable dispatcherRunnable;

    private AsynchronousDispatcher()
    {}

    /**
     * Invokes the runnable either over the AWT event dispatcher or the
     * internal dispatcher thread.
     */
    public static void invokeLater( Runnable runnable )
    {
        if ( dispatcherRunnable == null )
        {
            java.awt.EventQueue.invokeLater( runnable );
        }
        else
        {
            dispatcherRunnable.invokeLater( runnable );
        }
    }

    /**
     * Enables the internal dispatcher thread and disables AWT event dispatching.
     */
    public static void enableDispatcherThread()
    {
        if ( dispatcherRunnable != null )
        {
            dispatcherRunnable = new DispatcherRunnable();
            Thread thread = new Thread( ThreadTracking.rootThreadGroup,
                dispatcherRunnable, "DispatcherRunnable-"
                + Integer.toHexString( dispatcherRunnable.hashCode() ) );
            thread.setDaemon( true );
            thread.start();
        }
    }

    /**
     * The internal dispatcher thread runnable.
     */
    private static class DispatcherRunnable implements Runnable
    {
        private Stack stack;

        public void run()
        {
            stack = new Stack();
            while( true )
            {
                try
                {
                    while( !stack.empty() )
                    {
                        Runnable runnable = (Runnable) stack.pop();
                        runnable.run();
                    }
                    wait();
                }
                catch ( InterruptedException exp )
                {
                }
                catch ( Throwable th )
                {
                    NLogger.error(NLoggerNames.USER_INTERFACE, th, th );
                }
            }
        }

        public synchronized void invokeLater( Runnable runnable )
        {
            stack.add( runnable );
            notify();
        }
    }
}