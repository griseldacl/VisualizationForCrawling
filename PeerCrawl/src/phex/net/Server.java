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
 *  Created on 07.03.2005
 *  --- CVS Information ---
 *  $Id: Server.java,v 1.8 2005/11/13 10:25:37 gregork Exp $
 */
package phex.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.TimerTask;

import phex.common.Environment;
import phex.common.ThreadPool;
import phex.common.address.IpAddress;
import phex.host.HostManager;
import phex.host.NetworkHostsContainer;
import phex.msg.MsgManager;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;

/**
 *
 */
public abstract class Server implements Runnable
{
    protected ServerSocket serverSocket;

    protected boolean isRunning;
    /**
     * Indicates if a incomming connection was seen
     */
    protected boolean hasConnectedIncomming;
    /**
     * The time the last incomming connection was seen.
     */
    protected long lastInConnectionTime;
    /**
     * The last time a TCP connect back reqest check was sent.
     */
    protected long lastFirewallCheckTime;
    
    public Server()
    {
        hasConnectedIncomming = false;
        lastInConnectionTime = -1;
        isRunning = false;
        
        Environment.getInstance().scheduleTimerTask( 
            new FirewallCheckTimer(), FirewallCheckTimer.TIMER_PERIOD,
            FirewallCheckTimer.TIMER_PERIOD );
    }

    public synchronized void startup() throws IOException
    {
        if (isRunning)
        {
            return;
        }
        NLogger.debug( NLoggerNames.SERVER, "Starting listener");
        isRunning = true;
        
        bind();
        
        ThreadPool.getInstance().addJob(this,
            "IncommingListener-" + Integer.toHexString(hashCode()));
    }
    
    protected abstract void bind() throws IOException;
    protected abstract void closeServer();

    public synchronized void restart() throws IOException
    {
        shutdown( true );
        startup();
    }

    public synchronized void shutdown(boolean waitForCompleted)
    {
        // not running, already dead or been requested to die.
        if (!isRunning)
        {
            return;
        }
        NLogger.debug( NLoggerNames.SERVER, "Shutting down listener");
        
        closeServer();
        
        if (waitForCompleted)
        {
            // Wait until the thread is dead.
            while (isRunning)
            {
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {
                    break;
                }
            }
        }
    }

    public boolean getRunning()
    {
        return isRunning;
    }

    public boolean hasConnectedIncoming()
    {
        return hasConnectedIncomming;
    }
    
    public IpAddress resolveLocalHostIP()
    {
        byte[] ip = null;
        InetAddress addr = serverSocket.getInetAddress();
        ip = addr.getAddress();
        if (ip[0] == 0 && ip[1] == 0 && ip[2] == 0 && ip[3] == 0)
        {
            ip = IpAddress.LOCAL_HOST_IP;
        }
        IpAddress ipAddress = new IpAddress( ip );
        return ipAddress;
    }
    
    public int getListeningLocalPort()
    {
        return serverSocket.getLocalPort();
    }
    
    public void resetFirewallCheck()
    {
        lastFirewallCheckTime = 0;
    }

    private class FirewallCheckTimer extends TimerTask
    {
        // once per 10 minutes
        public static final long TIMER_PERIOD = 1000 * 60 * 5;

        private static final long CHECK_TIME = 1000 * 60 * 15;

        public void run()
        {
            try
            {
                long now = System.currentTimeMillis();

                if ((hasConnectedIncomming && now - lastInConnectionTime > CHECK_TIME)
                    || (!hasConnectedIncomming && now - lastFirewallCheckTime > CHECK_TIME))
                {
                    NetworkHostsContainer netHostsContainer = HostManager
                        .getInstance().getNetworkHostsContainer();
                    if (netHostsContainer.getUltrapeerConnectionCount() <= 2)
                    {
                        return;
                    }
                    lastFirewallCheckTime = now;
                    MsgManager.getInstance().requestTCPConnectBack();
                    Environment.getInstance().scheduleTimerTask(
                        new IncommingCheckRunner(),
                        IncommingCheckRunner.TIMER_PERIOD );
                }
            }
            catch (Throwable th)
            {
                NLogger.error( NLoggerNames.SERVER, th, th );
            }
        }
    }

    private class IncommingCheckRunner extends TimerTask
    {
        // after 45 sec.
        public static final long TIMER_PERIOD = 1000 * 45;

        public void run()
        {
            try
            {
                long now = System.currentTimeMillis();
                if (now - lastInConnectionTime > TIMER_PERIOD)
                {
                    hasConnectedIncomming = false;
                }
            }
            catch ( Throwable th )
            {
                NLogger.error( NLoggerNames.SERVER, th, th );
            }
        }
    }
}
