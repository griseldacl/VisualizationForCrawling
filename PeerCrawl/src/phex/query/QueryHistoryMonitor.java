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
 *  $Id: QueryHistoryMonitor.java,v 1.13 2005/11/03 17:06:26 gregork Exp $
 */
package phex.query;

import java.io.*;


import phex.common.*;
import phex.event.*;
import phex.msg.*;
import phex.utils.*;

public class QueryHistoryMonitor
{
    private Cfg configuration;
    private CircularQueue historyQueue;
    private BufferedWriter fileWriter;

    /**
     * Right now we have only one single listener. If more are needed apply
     * the same design that can be found in NetworkHostContainer
     */
    private QueryHistoryChangeListener changeListener;

    public QueryHistoryMonitor()
    {
        configuration = ServiceManager.sCfg;
        historyQueue = new CircularQueue( configuration.searchHistoryLength,
            configuration.searchHistoryLength );
        updateFileMonitoring();
    }

    public void shutdown()
    {
        if ( fileWriter != null )
        {
            try
            {

                fileWriter.close();
                Logger.logMessage( Logger.INFO, Logger.SEARCH,
                    "End QueryMonitoring to file." );
            }
            catch ( IOException exp )
            {
                NLogger.error( NLoggerNames.GLOBAL, exp, exp );
            }
            fileWriter = null;
        }
    }

    private void updateFileMonitoring()
    {
        // START this is a special implementation for Raster...
        if (   configuration.monitorSearchHistory
            && configuration.searchMonitorFile.length() > 0 )
        {
            try
            {
                File file = new File( configuration.searchMonitorFile );
                File parent = file.getParentFile();
                if ( parent != null )
                {
                    parent.mkdirs();
                }
                file.createNewFile();
                fileWriter = new BufferedWriter( new FileWriter(
                    file.getAbsolutePath(), true ) );
                Logger.logMessage( Logger.INFO, Logger.SEARCH,
                    "Start QueryMonitoring to " + file.getAbsolutePath() );
            }
            catch ( IOException exp )
            {
                NLogger.error( NLoggerNames.GLOBAL, exp, exp );
            }
        }
        else
        {
            shutdown();
        }
        // END this is a special implementation for Raster...
    }

    public void setHistoryMonitored( boolean state )
    {
        configuration.monitorSearchHistory = state;
        updateFileMonitoring();
        configuration.save();
    }

    public boolean isHistoryMonitored( )
    {
        return configuration.monitorSearchHistory;
    }

    public synchronized void setMaxHistroySize( int size )
    {
        if ( size == historyQueue.getCapacity() )
        {
            return;
        }
        historyQueue = new CircularQueue( size, size );
        configuration.searchHistoryLength = size;
        configuration.save();
    }

    public synchronized int getMaxHistorySize()
    {
        return historyQueue.getCapacity();
    }

    public synchronized int getHistorySize()
    {
        return historyQueue.getSize();
    }

    public synchronized QueryMsg getSearchQueryAt( int index )
    {
        return (QueryMsg) historyQueue.get( index );
    }

    public synchronized void addSearchQuery( QueryMsg query )
    {
        if ( configuration.monitorSearchHistory )
        {
            if ( query.getSearchString().length() > 0 )
            {
                historyQueue.addToHead( query );
                fireQueryHistoryChanged( );
            }

            // START this is a special implementation for Raster...
            if ( fileWriter != null )
            {
                try
                {
                    fileWriter.write( query.getSearchString() );
                    /*fileWriter.write( query.getSearchString() + "\t" +
                        query.getHeader().getHopsTaken() + "\t" +
                        query.getHeader().getTTL() + "\t");
                    URN[] urns = query.getQueryURNs();
                    for ( int i = 0; i < urns.length; i++ )
                    {
                        if ( urns[i].isSha1Nid() )
                        {
                            fileWriter.write( urns[i].getAsString() );
                            break;
                        }
                    }*/
                    fileWriter.newLine();
                }
                catch ( IOException exp )
                {
                    Logger.logWarning( exp );
                }
            }
            // END this is a special implementation for Raster...
        }
    }

    ///////////////////// START event handling methods ////////////////////////
    public void setQueryHistoryChangeListener( QueryHistoryChangeListener listener )
    {
        if ( changeListener != null )
        {
            throw new RuntimeException( "CaughtHostChangedListener already used!!" );
        }
        changeListener = listener;
    }

    private void fireQueryHistoryChanged( )
    {
        changeListener.queryHistoryChanged( );
    }
    ///////////////////// END event handling methods ////////////////////////
}