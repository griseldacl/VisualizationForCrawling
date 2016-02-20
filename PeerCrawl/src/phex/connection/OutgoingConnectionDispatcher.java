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
package phex.connection;

import java.io.IOException;

import phex.common.address.DestAddress;
import phex.host.*;
import phex.net.OnlineObserver;
import phex.utils.NLogger;
import phex.utils.NLoggerNames;


public class OutgoingConnectionDispatcher implements Runnable
{
    public DestAddress hostAddress;

    public OutgoingConnectionDispatcher( )
    {
    }
    
    public void setHostAddressToConnect( DestAddress hostAddress )
    {
        this.hostAddress = hostAddress;
    }

    public void run()
    {
        try
        {
            if ( hostAddress == null )
            {// fetch new host
                connectToNextCaughtHost();
            }
            else
            {
                connectToHostAddress();
            }
        }
        catch ( Throwable th )
        {
            NLogger.error(NLoggerNames.OUT_CONNECTION, th, th);
        }
    }
    
    private void connectToHostAddress()
    {
        NetworkHostsContainer networkContainer =
            HostManager.getInstance().getNetworkHostsContainer();
        Host host = new Host( hostAddress );
        host.setType( Host.TYPE_OUTGOING );
        networkContainer.addNetworkHost( host );
        
        OnlineObserver onlineObserver = NetworkManager.getInstance().getOnlineObserver();
        try
        {
            ConnectionEngine engine = new ConnectionEngine( host );
            engine.initializeOutgoingConnection();
            engine.processIncomingData();
        }
//        catch ( SocketException exp )
//        {
//            onlineObserver.markFailedConnection();
//            outgoingHost.setStatus( Host.STATUS_HOST_ERROR, exp.getMessage() );
//            HostManager.getInstance().disconnectHost( outgoingHost );
//            Logger.logMessage( Logger.FINEST, Logger.NETWORK, exp.toString() );
//        }
//        catch ( ConnectionClosedException exp )
//        {
//            outgoingHost.setStatus( Host.STATUS_HOST_ERROR, exp.getMessage() );
//            HostManager.getInstance().disconnectHost( outgoingHost );
//            Logger.logMessage( Logger.FINEST, Logger.NETWORK, exp.toString() );
//        }
//        catch ( ProtocolNotSupportedException exp )
//        {
//            outgoingHost.setStatus( Host.STATUS_HOST_ERROR, exp.getMessage() );
//            HostManager.getInstance().disconnectHost( outgoingHost );
//            Logger.logMessage( Logger.FINEST, Logger.NETWORK, exp.toString() );
//        }
        catch ( IOException exp )
        {
            onlineObserver.markFailedConnection();
            host.setStatus( HostConstants.STATUS_HOST_ERROR, exp.getMessage() );
            HostManager.getInstance().disconnectHost( host );
            NLogger.debug(NLoggerNames.OUT_CONNECTION, exp);
        }
        catch (Exception exp)
        {
            host.setStatus(HostConstants.STATUS_HOST_ERROR, exp.getMessage());
            HostManager.getInstance().disconnectHost( host );
            NLogger.warn(NLoggerNames.OUT_CONNECTION, exp, exp);
        }
    }
    
    private void connectToNextCaughtHost( )
    {
        NetworkHostsContainer networkContainer =
            HostManager.getInstance().getNetworkHostsContainer();
        CaughtHostsContainer caughtHostContainer =
            HostManager.getInstance().getCaughtHostsContainer();
        DestAddress caughtHost;
        do
        {
            caughtHost = caughtHostContainer.getNextCaughtHost();
            if ( caughtHost == null )
            {
                // nothing is available... just return..
                return;
            }
        }
        while ( networkContainer.isConnectedToHost( caughtHost ) );
        
        Host host = new Host( caughtHost );
        host.setType( Host.TYPE_OUTGOING );
        networkContainer.addNetworkHost( host );

        OnlineObserver onlineObserver = NetworkManager.getInstance().getOnlineObserver();
        ConnectionEngine engine = new ConnectionEngine( host );
        try
        {
            engine.initializeOutgoingConnection();
        }
        catch ( ConnectionRejectedException exp )
        {
            caughtHostContainer.reportConnectionStatus( caughtHost, true );
            host.setStatus( HostConstants.STATUS_HOST_ERROR, exp.getMessage() );
            HostManager.getInstance().disconnectHost( host );
            NLogger.debug(NLoggerNames.OUT_CONNECTION, exp);
            return;
        }
        catch ( IOException exp )
        {
            onlineObserver.markFailedConnection();
            caughtHostContainer.reportConnectionStatus( caughtHost, false );
            host.setStatus( HostConstants.STATUS_HOST_ERROR, exp.getMessage() );
            HostManager.getInstance().disconnectHost( host );
            NLogger.debug(NLoggerNames.OUT_CONNECTION, exp);
            return;
        }
        catch (Exception exp)
        {
            caughtHostContainer.reportConnectionStatus( caughtHost, false );
            host.setStatus(HostConstants.STATUS_HOST_ERROR, exp.getMessage());
            HostManager.getInstance().disconnectHost( host );
            NLogger.warn(NLoggerNames.OUT_CONNECTION, exp, exp);
            return;
        }
        
        caughtHostContainer.reportConnectionStatus( caughtHost, true );
        try
        {
            engine.processIncomingData();
        }
        catch ( IOException exp )
        {
            host.setStatus( HostConstants.STATUS_HOST_ERROR, exp.getMessage() );
            HostManager.getInstance().disconnectHost( host );
            NLogger.debug(NLoggerNames.OUT_CONNECTION, exp);
        }
        catch (Exception exp)
        {
            host.setStatus(HostConstants.STATUS_HOST_ERROR, exp.getMessage());
            HostManager.getInstance().disconnectHost( host );
            NLogger.warn(NLoggerNames.OUT_CONNECTION, exp, exp);
        }
    }
}