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
 *  $Id: DynamicQueryWorker.java,v 1.6 2005/10/03 00:18:28 gregork Exp $
 *
 * Created on 2003-06-06
 */
package phex.query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import phex.host.Host;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

/**
 * The dynamic query worker contains a list of all active dynamic query engines
 * and regulary iterates over them to run through there dynamic query procecss.
 */
public class DynamicQueryWorker implements Runnable
{
	/**
	 * The time between iterations.
	 */
	private static final int WORKER_SLEEP_TIME = 500;
	
    /**
	 * The list of all dynamic query engines.
	 */
	private List queryList;
	
    /**
     * Creates a new dynamic query worker.
     */
    public DynamicQueryWorker()
    { 
    	queryList = new ArrayList();
    }
    
    public void addDynamicQueryEngine( DynamicQueryEngine engine )
    {
        synchronized (queryList)
        {
            queryList.add( engine );
        }
    }
    
    public void removeDynamicQuerysForHost( Host host )
    {
        DynamicQueryEngine queryEngine;
        ArrayList removeList = new ArrayList(); 
        synchronized( queryList )
        {
            Iterator iterator = queryList.iterator();
            while( iterator.hasNext() )
            {
                queryEngine = (DynamicQueryEngine)iterator.next();
                if ( queryEngine.getFromHost() == host )
                {
                    removeList.add( queryEngine );
                }
            }
            queryList.removeAll( removeList );
        }
    }

	public void startQueryWorker()
	{
		Thread thread = new Thread( this,
            "DynamicQueryWorker-" + Integer.toHexString( hashCode() ) );
        thread.setPriority( Thread.NORM_PRIORITY );
		thread.setDaemon( true );
		thread.start();
	}
	
	public void run()
	{
		while ( true )
        {
        	try
        	{
				Thread.sleep( WORKER_SLEEP_TIME );
        	}
        	catch ( InterruptedException exp )
        	{// reset interrupted signal of thread...
        		Thread.currentThread().interrupt();
        	}
        	try
        	{
            	processQueryList();
			}
			catch ( Throwable th )
			{// make sure thread does not stop due to error...
                NLogger.error( NLoggerNames.Query_DynamicQueryWorker, th, th );
			}
        }
	}

    private void processQueryList()
    {
        DynamicQueryEngine[] queryEngines;
        synchronized (queryList)
        {
            int size = queryList.size();
            if (size == 0)
            {
                return;
            }
            queryEngines = new DynamicQueryEngine[size];
            queryList.toArray(queryEngines);
        }
        
        for (int i = 0; i < queryEngines.length; i++)
        {
            if (queryEngines[i].isQueryFinished())
            {
                synchronized (queryList)
                {
                    queryList.remove(queryEngines[i]);
                }
            }
            else
            {
                queryEngines[i].processQuery();
            }
        }
    }
}
