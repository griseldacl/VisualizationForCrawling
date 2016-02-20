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
 *  $Id: Host.java,v 1.63 2005/11/13 10:18:58 gregork Exp $
 */
package phex.host;

import java.io.IOException;

import phex.common.QueryRoutingTable;
import phex.common.ServiceManager;
import phex.common.ThreadPool;
import phex.common.address.DestAddress;
import phex.connection.ConnectionClosedException;
import phex.connection.MessageQueue;
import phex.connection.NetworkManager;
import phex.msg.Message;
import phex.msg.MsgHeader;
import phex.msg.QueryMsg;
import phex.msg.vendor.MessagesSupportedVMsg;
import phex.net.connection.Connection;
import phex.query.DynamicQueryConstants;
import phex.statistic.MessageCountStatistic;
import phex.utils.*;

/**
 * <p>A Gnutella host, or servent together with operating statistics and IO.</p>
 *
 * <p>This class seems to be overloaded. Hosts are used in several different
 * distinct modes:
 * <ul>
 * <li>Incomming sTypeIncoming - client half of a Gnutella networm node pair.
 * This is the default state of a Host. A phex.ReadWorker will be assigned to
 * handle the connection.</li>
 * <li>Outgoing sTypeOutgoing - server half of a Gnutella network node pair. A
 * phex.Listener will be assigned to handle the connection, and this will
 * dellegate on to a phex.ReadWorker.</li>
 * <li>Download sTypeDownload - attempting to HTTP get data. A
 * phex.download.DownloadWorker will be assigned to handle the connection.</li>
 * <li>Push sTypePush - a Host to send data that has been ruited via a Push
 * request. A phex.PushWorker will be assigned to handle the connection.</li>
 * </ul>
 * </p>
 */
public class Host
{
    private static final Long ZERO_LONG = new Long(0);

    /**
     * The time after which a query routing table can be updated in milliseconds.
     */
    private long QUERY_ROUTING_UPDATE_TIME = 5 * 60 * 1000; // 5 minutes

    /**
     * The time after which a connection is stable.
     */
    private static final int STABLE_CONNECTION_TIME = 75 * 1000; // 75 seconds

    public static final int DISCONNECT_POLICY_THRESHOLD = 10000;

    public static final Short TYPE_OUTGOING = new Short((short) 1);

    public static final Short TYPE_INCOMING = new Short((short) 2);

    public static final Short TYPE_LOCAL = new Short((short) 3);

    /**
     * Normal connection type.
     */
    public static final byte CONNECTION_NORMAL = 0;

    /**
     * Connection type as me as a leaf and the host as a ultrapeer.
     */
    public static final byte CONNECTION_LEAF_UP = 1;

    /**
     * Connection type from ultrapeer to ultrapeer.
     */
    public static final byte CONNECTION_UP_UP = 2;

    /**
     * Connection type representing me as a ultrapeer and the host as a leaf.
     */
    public static final byte CONNECTION_UP_LEAF = 3;

    //public static final Short PROTOCOL_VER_0_4 = new Short( (short)0 );
    //public static final Short PROTOCOL_VER_0_6 = new Short( (short)1 );

    private NetworkHostsContainer hostsContainer;

    private DestAddress hostAddress;

    private Connection connection;

    private int status;

    private String lastStatusMsg = "";

    private long statusTime = 0;

    private Short type;

    private boolean mHasWorker;

    private int mReceivedCount;

    private int mSentCount;

    private int mDropCount;

    private long fileCount = -1;

    private long shareSize = -1;

    private String vendor;

    private boolean vendorChecked = false;

    /**
     * The maxTTL this connection accepts for dynamic queries.
     * It is provide through the handshake header X-Max-TTL and used
     * for the dynamic query proposal.
     * This header indicates that we should not send fresh queries to
     * this connection with TTLs higher than the X-Max-TTL. If we are
     * routing traffic from other Ultrapeers, the X-Max-TTL is irrelevant.
     * The X-Max-TTL MUST NOT exceed 4, as any TTL above 4 indicates a client
     * is allowing too much query traffic on the network. This header is
     * particularly useful for compatibility with future clients that may
     * choose to have higher degrees but that would prefer lower TTL traffic
     * from their neighbors. For example, if future clients connect to
     * 200 Ultrapeers, they could use the X-Max-TTL header to indicate to
     * today's clients that they will not accept TTLs above 2. A typical
     * initial value for X-Max-TTL is 3.
     */
    private byte maxTTL;
    
    /**
     * The max hops value to use for queries comming from a hops flow vendor
     * message.
     */
    private byte hopsFlowLimit;

    /**
     * The intra ultrapeer connection this connection holds.
     * It is provide through the handshake header X-Degree and used
     * for the dynamic query proposal.
     * The X-Degree header simply indicates the number of Ultrapeer
     * connections this nodes attempts to maintain. Clients supporting
     * the dynamic query proposal must have X-Degrees of at least 15, 
     * and higher values are preferable.
     */
    private int ultrapeerDegree;

    /**
     * Marks if a connection is stable. A connection is stable when a
     * host connection last over STABLE_CONNECTION_TIME seconds.
     */
    private boolean isConnectionStable;

    /**
     * Defines if the host supportes QRP. This is only importend for Ultrapeer
     * connections.
     */
    private boolean isQueryRoutingSupported;

    /**
     * Defines if the host supportes Ultrapeer QRP. This is only importend for
     * Ultrapeer connections.
     */
    private boolean isUPQueryRoutingSupported;

    /**
     * Defines if the host supports dynamic query.
     */
    private boolean isDynamicQuerySupported;

    /**
     * Marks the last time the local query routing table was sent to this
     * host. Only for leaf-ultrapeers connection a query routing table is send.
     * @see #isQRTUpdateRequired()
     */
    private long lastQRTableSentTime;

    /**
     * The QR table that was last sent to this host on lastQRTableSentTime. This
     * table is needed to send patch updates to the host.
     */
    private QueryRoutingTable lastSentQRTable;

    /**
     * The QR table that was last received from this host. This
     * table is needed to determine which querys to send to this host.
     */
    private QueryRoutingTable lastReceivedQRTable;

    /**
     * Defines the connection type we have with this host. Possible values are
     * CONNECTION_NORMAL
     * CONNECTION_LEAF_ULTRAPEER
     * PEER_LEAF
     */
    private byte connectionType;

    /**
     * A SACHRIFC message queue implementation.
     */
    private MessageQueue messageQueue;
    
    private boolean isVendorMessageSupported;
    
    private MessagesSupportedVMsg supportedVMsgs;
    
    /**
     * The PushProxy host address of this host. It is received from a
     * PushProxyAcknowledgement vendor message.
     */
    private DestAddress pushProxyAddress;;

    /**
     * Defines the protocol version
     */
    //private Short protocolVersion;

    /**
     * Create a new Host with a type of sTypeOutgoing.
     */
    public Host()
    {
        hostsContainer = HostManager.getInstance().getNetworkHostsContainer();

        connection = null;
        status = HostConstants.STATUS_HOST_NOT_CONNECTED;
        type = TYPE_OUTGOING;
        mHasWorker = false;
        isConnectionStable = false;
        connectionType = CONNECTION_NORMAL;
        isQueryRoutingSupported = false;
        isUPQueryRoutingSupported = false;
        isDynamicQuerySupported = false;
        isVendorMessageSupported = false;
        mReceivedCount = 0;
        mSentCount = 0;
        mDropCount = 0;
        maxTTL = DynamicQueryConstants.DEFAULT_MAX_TTL;
        hopsFlowLimit = -1;
    }

    /**
     * <p>Create a new Host for a HostAddress that will default to a type of
     * sTypeOutgoing.</p>
     *
     *
     * @param address  the HostAddress this Host will communicate with
     */
    public Host(DestAddress address)
    {
        this();
        hostAddress = address;
    }

    public Host(DestAddress address, Connection connection)
    {
        this();
        hostAddress = address;
        this.connection = connection;
    }

    public DestAddress getHostAddress()
    {
        return hostAddress;
    }

    public void setConnection(Connection connection)
    {
        this.connection = connection;
        mReceivedCount = 0;
        mSentCount = 0;
        mDropCount = 0;
    }

    /**
     * @return Returns the connection.
     */
    public Connection getConnection()
    {
        return connection;
    }
    
    public GnutellaInputStream getInputStream() throws IOException
    {
        if ( connection == null ) { throw new ConnectionClosedException(
            "Connection already closed"); }
        return connection.getInputStream();
    }

    public GnutellaOutputStream getOutputStream() throws IOException
    {
        if ( connection == null ) { throw new ConnectionClosedException(
            "Connection already closed"); }
        return connection.getOutputStream();
    }

    public void activateInputInflation() throws IOException
    {
        getInputStream().activateInputInflation();
    }

    public void activateOutputDeflation() throws IOException
    {
        getOutputStream().activateOutputDeflation();
    }

    public void setVendor(String aVendor)
    {
        vendor = aVendor;
    }

    public String getVendor()
    {
        return vendor;
    }

    public int getStatus()
    {
        return status;
    }
    
    public String getLastStatusMsg()
    {
        return lastStatusMsg;
    }

    public void setStatus(int status)
    {
        setStatus(status, null, System.currentTimeMillis());
    }

    public void setStatus(int status, long statusTime)
    {
        setStatus(status, null, statusTime);
    }

    public void setStatus(int status, String msg)
    {
        setStatus(status, msg, System.currentTimeMillis());
    }

    public void setStatus(int status, String msg, long statusTime)
    {
        if ( this.status == status && lastStatusMsg != null
            && lastStatusMsg.equals(msg) ) { return; }
        this.status = status;
        lastStatusMsg = msg;
        this.statusTime = statusTime;
        hostsContainer.fireNetworkHostChanged(this);
    }

    /**
     * Checks if a connection status is stable. A stable connection
     * is a connection to host that last over STABLE_CONNECTION_TIME seconds.
     * The current time is given out of performance reasons when
     * looping over all hosts.
     */
    public void checkForStableConnection(long currentTime)
    {
        if ( isConnectionStable ) { return; }

        // if we have connected status for at least STABLE_CONNECTION_TIME.
        if ( status == HostConstants.STATUS_HOST_CONNECTED
            && getConnectionUpTime(currentTime) > STABLE_CONNECTION_TIME )
        {
            isConnectionStable = true;
            hostsContainer.fireNetworkHostChanged(this);
        }
    }

    public boolean isConnectionStable()
    {
        return isConnectionStable;
    }

    /**
     * Returns the number of millis the connection is up.
     */
    public long getConnectionUpTime(long currentTime)
    {
        if ( status == HostConstants.STATUS_HOST_CONNECTED )
        {
            return currentTime - statusTime;
        }
        else
        {
            return 0;
        }
    }

    /**
     * Returns the number of millis the connection is up.
     */
    public Long getConnectionUpTimeObject(long currentTime)
    {
        if ( status == HostConstants.STATUS_HOST_CONNECTED )
        {
            return new Long(currentTime - statusTime);
        }
        else
        {
            return ZERO_LONG;
        }
    }

    public boolean isErrorStatusExpired(long currentTime)
    {
        if ( status == HostConstants.STATUS_HOST_ERROR || status == HostConstants.STATUS_HOST_DISCONNECTED )
        {
            if ( currentTime - statusTime > ServiceManager.sCfg.hostErrorDisplayTime ) { return true; }
        }
        return false;
    }

    public Short getType()
    {
        return type;
    }

    public void setType(Short aType)
    {
        this.type = aType;
    }

    public boolean isIncomming()
    {
        return type.equals(TYPE_INCOMING);
    }
    
    public void setVendorMessageSupported( boolean state )
    {
        this.isVendorMessageSupported = state;
    }
    
    public boolean isVendorMessageSupported()
    {
        return isVendorMessageSupported;
    }
    
    public void setSupportedVMsgs( MessagesSupportedVMsg supportedVMsgs )
    {
        this.supportedVMsgs = supportedVMsgs;
    }
    
    public boolean isTCPConnectBackSupported()
    {
        return supportedVMsgs != null && supportedVMsgs.isTCPConnectBackSupported();
    }
    
    public boolean isPushProxySupported()
    {
        return supportedVMsgs != null && supportedVMsgs.isPushProxySupported();
    }
    
    public boolean isHopsFlowSupported()
    {
        return supportedVMsgs != null && supportedVMsgs.isHopsFlowSupported();
    }
    
    /**
     * Returns the PushProxy host address of this host if received 
     * from a PushProxyAcknowledgement vendor message. Null otherwise.
     */
    public DestAddress getPushProxyAddress()
    {
        return pushProxyAddress;
    }
    
    /**
     * Sets the PushProxy host address of this host. It must be received 
     * from a PushProxyAcknowledgement vendor message.
     */
    public void setPushProxyAddress( DestAddress address )
    {
        pushProxyAddress = address;
    }

    /*
     public Short getProtocolVersion()
     {
     return protocolVersion;
     }

     public void setProtocolVersion( Short aProtocolVersion )
     {
     this.protocolVersion = aProtocolVersion;
     }
     */

    /*public String getTypeName()
     {
     switch(type)
     {
     case TYPE_OUTGOING:
     {
     String str = Localizer.getString( "Outgoing" );
     if ( isUltrapeer )
     {
     str += " " + Localizer.getString( "Ultrapeer" );
     }
     return str;
     }
     case TYPE_INCOMING:
     {
     String str = Localizer.getString( "Incoming" );
     if ( isUltrapeer )
     {
     str += " " + Localizer.getString( "Ultrapeer" );
     }
     return str;
     }
     default:
     // other types cant appear in the network table
     // and this is the only table displaying the type names.
     return Localizer.getString( "Error" ) + " " + type;
     }
     }*/

    public void incReceivedCount()
    {
        mReceivedCount++;
    }

    public int getReceivedCount()
    {
        return mReceivedCount;
    }

    public void incSentCount()
    {
        mSentCount++;
    }

    public int getSentCount()
    {
        return mSentCount;
    }

    public void incDropCount()
    {
        mDropCount++;
    }

    public int getDropCount()
    {
        return mDropCount;
    }

    public long getFileCount()
    {
        return fileCount;
    }

    public void setFileCount(long fileCount)
    {
        this.fileCount = fileCount;
    }

    /**
     * Returns total size in kBytes.
     */
    public long getTotalSize()
    {
        return shareSize;
    }

    public void setTotalFileSize(long shareSize)
    {
        this.shareSize = shareSize;
    }

    /**
     * Returns the maxTTL this connection accepts.
     * @return the maxTTL this connection accepts.
     * @see #maxTTL
     */
    public byte getMaxTTL()
    {
        return maxTTL;
    }

    /**
     * Sets the maxTTL this connection accepts.
     * @param maxTTL the new maxTTL.
     * @see #maxTTL
     */
    public void setMaxTTL(byte maxTTL)
    {
        this.maxTTL = maxTTL;
    }
    
    /**
     * Returns the max hops value to use for queries comming from a hops flow vendor
     * message, or -1 if not set.
     **/
    public byte getHopsFlowLimit()
    {
        return hopsFlowLimit;
    }
    
    /**
     * Sets the max hops value to use for queries comming from a hops flow vendor
     * message, or -1 to reset.
     * @param hopsFlowLimit
     */
    public void setHopsFlowLimit(byte hopsFlowLimit)
    {
        this.hopsFlowLimit = hopsFlowLimit;
    }
    
    /**
     * Returns the ultrapeer connection degree.
     * @return the ultrapeer connection degree.
     * @see #ultrapeerDegree
     */
    public int getUltrapeerDegree()
    {
        return ultrapeerDegree;
    }

    /**
     * Sets the ultrapeer connection degree of this connection.
     * @param degree the new ultrapeer connection degree.
     * @see #ultrapeerDegree
     */
    public void setUltrapeerDegree(int degree)
    {
        ultrapeerDegree = degree;
    }

    public boolean tooManyDropPackets()
    {
        // dont drop if this is a young connection
        if ( mReceivedCount < 50
            && getConnectionUpTime(System.currentTimeMillis()) < 1000 * 60 ) { return false; }
        return (mDropCount * 100 / (mReceivedCount + 1) > ServiceManager.sCfg.mDisconnectDropRatio);
    }

    public boolean dropPacketsInRed()
    {
        return (mDropCount * 100 / (mReceivedCount + 1)) > (ServiceManager.sCfg.mDisconnectDropRatio * 3 / 4);
    }

    public boolean isConnected()
    {
        return connection != null;
    }

    public void disconnect()
    {
        if ( connection != null )
        {
            if ( status != HostConstants.STATUS_HOST_ERROR )
            {
                setStatus(HostConstants.STATUS_HOST_DISCONNECTED);
            }
            connection.disconnect();
            connection = null;
        }

        if ( messageQueue != null )
        {
            // notify messageQueue to cause SendEngine to stop waiting and running..
            synchronized (messageQueue)
            {
                messageQueue.notify();
            }
        }
    }

    public synchronized boolean isAcquiredByWorker()
    {
        // Test and set, in one block of operation.
        if ( mHasWorker ) { return false; }
        mHasWorker = true;
        return true;
    }

    public synchronized void releaseFromWorker()
    {
        mHasWorker = false;
    }

    public int getSendQueueLength()
    {
        if ( messageQueue == null )
        {
            return 0;
        }
        else
        {
            return messageQueue.getQueuedMessageCount();
        }
    }

    public int getSendDropCount()
    {
        if ( messageQueue == null )
        {
            return 0;
        }
        else
        {
            return messageQueue.getDropCount();
        }
    }

    public boolean isSendQueueTooLong()
    {
        if ( messageQueue == null ) { return false; }
        return (messageQueue.getQueuedMessageCount() >= ServiceManager.sCfg.mNetMaxSendQueue - 1);
    }

    public boolean isSendQueueInRed()
    {
        if ( messageQueue == null ) { return false; }
        return (messageQueue.getQueuedMessageCount() >= ServiceManager.sCfg.mNetMaxSendQueue * 3 / 4);
    }

    public boolean isNoVendorDisconnectApplying()
    {
        if ( !ServiceManager.sCfg.mDisconnectApplyPolicy
            || !ServiceManager.sCfg.isNoVendorNodeDisconnected ) { return false; }

        // Already checked?  Short-circuit out (no need to recalculate len & delta-time)
        // Possible issue if user toggles the config setting while connected to
        // an unwanted host--it will not disconnect because it already passed the test
        if ( vendorChecked ) { return false; }
        // The vendor string might not be there immediately because of
        // handshaking, but will certainly be there when the status is HOST_CONNECTED.
        if ( status != HostConstants.STATUS_HOST_CONNECTED ) { return false; }

        String normalizedVendorString = this.vendor;
        if ( normalizedVendorString == null )
        {
            normalizedVendorString = "";
        }
        else
        {
            normalizedVendorString = normalizedVendorString.trim();
        }

        if ( normalizedVendorString.length() == 0 )
        {
            return true;
        }
        else
        {
            vendorChecked = true;
            return false;
        }
    }

    public boolean isFreeloader(long currentTime)
    {
        // never count a ultrapeer as freeloader...
        if ( isUltrapeer() ) { return false; }
        long timeDelta = getConnectionUpTime(currentTime);
        // We can only really tell after initial handshaing is complete, 10
        // seconds should be a good delay.
        if ( timeDelta >= DISCONNECT_POLICY_THRESHOLD )
        {
            if ( ServiceManager.sCfg.freeloaderFiles > 0
                && fileCount < ServiceManager.sCfg.freeloaderFiles ) { return true; }
            if ( ServiceManager.sCfg.freeloaderShareSize > 0
                && (shareSize / 1024) < ServiceManager.sCfg.freeloaderShareSize ) { return true; }
        }
        return false;
    }

    /**
     * Indicates that this is a ultrapeer and I am a leaf. The
     * connection type in this case is CONNECTION_LEAF_UP.
     * @return true if this is a ultrapeer and I am a leaf, false otherwise.
     */
    public boolean isLeafUltrapeerConnection()
    {
        return connectionType == CONNECTION_LEAF_UP;
    }

    /**
     * Indicates that this is a ultrapeer in general without paying attention to
     * my relationship to this ultrapeer. The connection type in this case can
     * be CONNECTION_LEAF_UP or CONNECTION_UP_UP.
     * @return true if this is a ultrapeer, false otherwise.
     */
    public boolean isUltrapeer()
    {
        return connectionType == CONNECTION_LEAF_UP
            || connectionType == CONNECTION_UP_UP;
    }

    /**
     * Indicates that this is a leaf and I am its ultrapeer. The
     * connection type in this case is CONNECTION_UP_LEAF.
     * @return true if this is a leaf and I am its ultrapeer, false otherwise.
     */
    public boolean isUltrapeerLeafConnection()
    {
        return connectionType == CONNECTION_UP_LEAF;
    }

    /**
     * Sets the connection type of the host. The connection can be of type:
     * CONNECTION_NORMAL
     * CONNECTION_LEAF_UP
     * CONNECTION_UP_UP
     * CONNECTION_UP_LEAF
     * @param connectionType the connection type of the host.
     */
    public void setConnectionType(byte connectionType)
    {
        this.connectionType = connectionType;
    }

    public String toString()
    {
        return "Host" + "[" + hostAddress.getHostName() + ":"
            + hostAddress.getPort() + "," + vendor + ",State=" + status + "]";
    }

    ////////////////////////START MessageQueue implementation///////////////////

    /**
     * Sends a message over the output stream but is not flushing the output
     * stream. This needs to be done by the caller.
     * @param message the message to send
     * @throws IOException when a send error occures
     */
    public void sendMessage(Message message) throws IOException
    {
        if (NLogger.isDebugEnabled(NLoggerNames.OUTGOING_MESSAGES) )
            NLogger.debug(NLoggerNames.OUTGOING_MESSAGES, 
                "Sending message: " + message + " - " + message.getHeader().getDebugString());
        
        GnutellaOutputStream outStream = getOutputStream();
        message.writeMessage( outStream );
        
        if (NLogger.isDebugEnabled(NLoggerNames.OUTGOING_MESSAGES) )
            NLogger.debug(NLoggerNames.OUTGOING_MESSAGES, 
                "Message send: " + message + " - " + message.getHeader().getDebugString());
        

        // keep track of sended message
        switch (message.getHeader().getPayload())
        {
        case MsgHeader.PING_PAYLOAD:
            MessageCountStatistic.pingMsgOutCounter.increment(1);
            break;
        case MsgHeader.PONG_PAYLOAD:
            MessageCountStatistic.pongMsgOutCounter.increment(1);
            break;
        case MsgHeader.PUSH_PAYLOAD:
            MessageCountStatistic.pushMsgOutCounter.increment(1);
            break;
        case MsgHeader.QUERY_PAYLOAD:
            MessageCountStatistic.queryMsgOutCounter.increment(1);
            break;

        case MsgHeader.QUERY_HIT_PAYLOAD:
            MessageCountStatistic.queryHitMsgOutCounter.increment(1);
            break;
        default:
            // no specific stat to count so count to total
            MessageCountStatistic.totalOutMsgCounter.increment(1);
        }
    }

    public void flushOutputStream() throws IOException
    {
        if ( isConnected() )
        {
            connection.flush();
            //Logger.logMessage( Logger.FINEST, Logger.NETWORK,
            //    "Messages flushed" );
        }
    }

    public void queueMessageToSend(Message message)
    {
        // before queuing a query check hops flow limit...
        if ( hopsFlowLimit > -1 &&
             message instanceof QueryMsg &&
             message.getHeader().getHopsTaken() >= hopsFlowLimit )
        {// dont send query!
            return;
        }
        
        NLogger.debug(NLoggerNames.OUTGOING_MESSAGES, 
            "Queuing message: " + message );
        initMessageQueue();
        incSentCount();
        synchronized (messageQueue)
        {
            messageQueue.addMessage(message);
            messageQueue.notify();
        }
    }

    private class SendEngine implements Runnable
    {
        public void run()
        {
            while (isConnected())
            {
                try
                {
                    waitForMessageQueueNotify();
                    messageQueue.sendQueuedMessages();
                }
                catch (ConnectionClosedException exp)
                {
                    return;
                }
                catch (IOException exp)
                {
                    setStatus(HostConstants.STATUS_HOST_ERROR, exp.getMessage());
                    hostsContainer.disconnectHost(Host.this);
                }
            }
        }

        private void waitForMessageQueueNotify()
            throws ConnectionClosedException
        {
            synchronized (messageQueue)
            {
                while (isConnected()
                    && messageQueue.getQueuedMessageCount() == 0)
                {
                    try
                    {
                        messageQueue.wait();
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
            }

            if ( !isConnected() ) { throw new ConnectionClosedException(
                "Connection already closed."); }
        }
    }

    /**
     * This method is here to make sure the message queue is only generated
     * when it is actually necessary. Generating it in the constructor
     * uses up a big amount of memory for every known Host. Together with the
     * message queue its SendEngine is initialized.
     */
    private void initMessageQueue()
    {
        if ( messageQueue != null ) { return; }
        // Create a queue with max-size, dropping the oldest msg when max reached.
        messageQueue = new MessageQueue(this);
        SendEngine engine = new SendEngine();
        ThreadPool.getInstance().addJob(engine,
            "SendEngine-" + Integer.toHexString(engine.hashCode()));
    }

    ////////////////////////END MessageQueue implementation/////////////////////

    ////////////////////////START QRP implementation////////////////////////////

    public boolean isQRTableUpdateRequired()
    {
        return System.currentTimeMillis() > lastQRTableSentTime
            + QUERY_ROUTING_UPDATE_TIME;
    }

    public QueryRoutingTable getLastSentRoutingTable()
    {
        return lastSentQRTable;
    }

    public void setLastSentRoutingTable(QueryRoutingTable routingTable)
    {
        lastSentQRTable = routingTable;
        lastQRTableSentTime = System.currentTimeMillis();
    }

    public QueryRoutingTable getLastReceivedRoutingTable()
    {
        return lastReceivedQRTable;
    }

    public void setLastReceivedRoutingTable(QueryRoutingTable routingTable)
    {
        lastReceivedQRTable = routingTable;
    }

    public boolean isQueryRoutingSupported()
    {
        return isQueryRoutingSupported;
    }

    public void setQueryRoutingSupported(boolean state)
    {
        isQueryRoutingSupported = state;
    }

    public boolean isUPQueryRoutingSupported()
    {
        return isUPQueryRoutingSupported;
    }

    public void setUPQueryRoutingSupported(boolean state)
    {
        isUPQueryRoutingSupported = state;
    }

    /**
     * Returns if the host supports dynamic query.
     * @return true if dynamic query is supported, false otherwise.
     */
    public boolean isDynamicQuerySupported()
    {
        return isDynamicQuerySupported;
    }

    /**
     * Sets if the hosts supports dynamic query.
     * @param state true if dynamic query is supported, false otherwise.
     */
    public void setDynamicQuerySupported(boolean state)
    {
        isDynamicQuerySupported = state;
    }

    //////////////////////////END QRP implementation////////////////////////////

    public static LocalHost LOCAL_HOST;
    static
    {
        LOCAL_HOST = new LocalHost();
    }

    public static class LocalHost extends Host
    {
        LocalHost()
        {
            super(NetworkManager.getInstance().getLocalAddress());
        }

        public boolean isConnected()
        {
            // return true to suite routing table..
            return true;
        }

        public Short getType()
        {
            return TYPE_LOCAL;
        }

    }

}