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
 *  Created on May 4, 2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.udp;

import java.io.IOException;

import phex.common.Manager;
import phex.common.address.DestAddress;
import phex.msg.PingMsg;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

/**
 * @author Madhu
 *
 */
public class UdpConnectionManager implements Manager
{
    //every 2 minutes
    public final static int UDP_PING_PERIOD = 1000 * 3 * 10;
    
    private static UdpConnectionManager instance;
    
    private UdpMessageEngine udpMsgEngine;
    
    public static UdpConnectionManager getInstance()
    {
        if ( instance == null )
        {
            instance = new UdpConnectionManager();
        }
        return instance;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    /// Manager methods
    ////////////////////////////////////////////////////////////////////////////
    
    /**
         * This method is called in order to initialize the manager. This method
         * includes all tasks that must be done to intialize all the several manager.
         * Like instantiating the singleton instance of the manager. Inside
         * this method you can't rely on the availability of other managers.
         * @return true is initialization was successful, false otherwise.
         */
    public boolean initialize() 
    {
        NLogger.info( NLoggerNames.UDP_CONNECTION,"Started up Udp Connection Manager");
        return true;  
    }	

        /**
         * This method is called in order to perform post initialization of the
         * manager. This method includes all tasks that must be done after initializing
         * all the several managers. Inside this method you can rely on the
         * availability of other managers.
         * @return true is initialization was successful, false otherwise.
         */
    public boolean onPostInitialization() 
    {
        udpMsgEngine = new UdpMessageEngine();
        return true;
    }
        
    /**
     * This method is called after the complete application including GUI completed
     * its startup process. This notification must be used to activate runtime
     * processes that needs to be performed once the application has successfully
     * completed startup.
     */
    public void startupCompletedNotify()
    {
        //ThreadPool.getInstance().addJob( new SendUdpPingRunner(), " udp sender ");
    }

        /**
         * This method is called in order to cleanly shutdown the manager. It
         * should contain all cleanup operations to ensure a nice shutdown of Phex.
         */
    public void shutdown() {}    
    
    /////////////////////////////////////////////////
    
    /**
     * This method sends a udp ping to the given host address 
     * @throws IOException
     */
    public void sendUdpPing( DestAddress address ) throws IOException
    {
        PingMsg udpPing = PingMsg.createUdpPingMsg();
        udpMsgEngine.addMessageToSend( udpPing, address );
        NLogger.debug( NLoggerNames.UDP_OUTGOING_MESSAGES," Sent Udp Ping to" + address +  
                " : " +  udpPing + " with Scp Byte : " + udpPing.getScpByte()[0]
                );
    }
    
// dont udp ping hosts too regulary!
//    private final class SendUdpPingRunner extends Thread
//    {
//        public void run()
//        {
//            while ( true )
//            {
//                Host[] addresses = HostManager.getInstance().getNetworkHostsContainer().getUltrapeerConnections();
//                for ( int i =0 ; i < addresses.length ; i++ )
//                {
//                    try
//                    {
//                        sendUdpPing( addresses[i].getHostAddress() );
//                    }
//                    catch (IOException e) {
//                        continue;
//                    }                                        
//                }
//                try
//                {
//                    sleep( UDP_PING_PERIOD );
//                }
//                catch ( InterruptedException e)
//                {
//                 //do nothing   
//                }
//            }
//        }
//    }
}
