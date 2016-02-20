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



/**
 * This class assists in tracking Thread use.
 * <p> </p>
 * <p> </p>
 * <p>Copyright: Copyright (c) 2002 Gregor Koukkoullis</p>
 * <p> </p>
 * @author Gregor Koukkoullis
 *
 */
public class ThreadTracking
{
    public static ThreadGroup threadPoolGroup;
    public static ThreadGroup rootThreadGroup;

    private static ThreadGroup systemGroup;

    public static void initialize()
    {
        // we want the system thread group
        systemGroup = Thread.currentThread().getThreadGroup();
        while ( systemGroup.getParent() != null )
        {// not the system thread group.. go up one step
            systemGroup = systemGroup.getParent();
        }
        rootThreadGroup = new ThreadGroup( systemGroup, "PhexRoot" );
        threadPoolGroup = new ThreadGroup( systemGroup, "PhexThreadPool" );
    }

    /*public static void dumpFullThreadLog()
    {
        if ( !Logger.isLevelLogged( Logger.FINEST ) )
        {
            return;
        }
        int count = systemGroup.activeCount();
        Thread[] threads = new Thread[ count ];
        count = systemGroup.enumerate( threads, true );
        Logger.logMessage( Logger.FINEST, Logger.GLOBAL,
            "------------------- Start Full Thread Dump -------------------" );
        for ( int i = 0; i < count; i++ )
        {
            Logger.logMessage( Logger.FINEST, Logger.GLOBAL, threads[ i ].toString() );
        }
        Logger.logMessage( Logger.FINEST, Logger.GLOBAL,
            "-------------------- End Full Thread Dump --------------------" );
    }*/
}