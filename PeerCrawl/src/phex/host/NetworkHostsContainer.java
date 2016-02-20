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
 *  $Id: NetworkHostsContainer.java,v 1.54 2005/11/13 10:18:58 gregork Exp $
 */
package phex.host;

import java.io.IOException;
import java.util.*;


import phex.msg.*;
import phex.common.*;
import phex.common.address.DestAddress;
import phex.connection.*;
import phex.event.*;
import phex.udp.UdpConnectionManager;
import phex.utils.*;

/**
 * Responsible for holding all hosts of the current network neighbour hood.
 *
 * For performance there where two different implementation decision to be made:
 * 1) When accessing the lists, return the original internaly maintaind list.
 *    When updating the lists dont mutate the original list. Instead create a new
 *    list and copy the contents of the old list into the new list, then replace
 *    the new list.
 *    This would mean fast access (227ms) against slow updates (799ms) but we
 *    would reveal our internal data structure for mutation. Hidding this
 *    structure through the use of a unmodifiable List would give slightly slower
 *    access (414ms).
 * 2) When accessing the lists, return a array containing the elements of the
 *    internal list. When updating the lists mutate the original list.
 *    This would resut in slow access (430ms) against fast updates (146ms).
 *    This concept would not reveal the internal data structure for mutation.
 *
 *    Performance times are based on a 15 element ArrayList. Access was measured
 *    by iterating 10000000 times over the list. Updates are measured by performing
 *    10000000 add and remove operations.
 *
 * Since I would think not revealing the internal data structure for mutation is
 * a high design goal, the option would be to publish a unmodifiable List (1) or
 * a array (2). Since access times are very close for these two options, the update
 * times go in favour of concept 2. Therefore concept two is used in this container
 * implementation.
 */
final public class NetworkHostsContainer implements NetworkListener
{
    private HostManager hostMgr;

    /**
     * The complete neighbour hood. Contains all connected and not connected
     * hosts independent from its connection type.
     * This collection is mainly used for GUI representation.
     */
    private ArrayList networkHosts;

    /**
     * Contains a list of connected peer connections.
     */
    private ArrayList peerConnections;

    /**
     * Contains a list of connected ultrapeer connections.
     */
    private ArrayList ultrapeerConnections;

    /**
     * The number of connections that are leafUltrapeerConnections inside the
     * ultrapeerConnections list.
     */
    private int leafUltrapeerConnectionCount;

    /**
     * Contains a list of connected leaf connections, in case we act as there
     * Ultrapeer.
     */
    private ArrayList leafConnections;

    /**
     * All listeners interested in events.
     */
    private ArrayList listenerList = new ArrayList( 3 );
    
    /**
     * All Hosts with free Leaf slots 
     * these are added by parsing the UP ggep extension in MsgPongs 
     */
    private Collection freeLeafSlotSet;
    
    private final static int FREE_SLOT_SET_SIZE = 8;
    
    /**
     * All Hosts with free Ultrpeer slots
     * these are added by parsing the UP ggep extension in MsgPongs
     */
    private Collection freeUltrapeerSlotSet;
    
    public NetworkHostsContainer()
    {
        networkHosts = new ArrayList();
        peerConnections = new ArrayList();
        ultrapeerConnections = new ArrayList();
        leafConnections = new ArrayList();
        hostMgr = HostManager.getInstance();
        freeUltrapeerSlotSet = createFreeSlotContainer();
        freeLeafSlotSet = createFreeSlotContainer();
    }

    /**
     * Returns true if the local host is a shielded leaf node ( has a connection
     * to a ultrapeer).
     */
    public synchronized boolean isShieldedLeafNode()
    {
        return leafUltrapeerConnectionCount > 0;
    }

    public synchronized boolean hasLeafConnections()
    {
        // we are a ultrapeer if we have any leaf slots filled.
        return !leafConnections.isEmpty();
    }

    public synchronized boolean hasUltrapeerConnections()
    {
        return !ultrapeerConnections.isEmpty();
    }

    /**
     * Used to chek if we have anymore peer slots. This method can be used by
     * peers, leafs and ultrapeers.
     * @return
     */
    public boolean hasPeerSlotsAvailable()
    {
        // Note: That we don't response on pings when the slots are full this
        // results in not getting that many incomming requests.
        if ( hostMgr.isUltrapeer() )
        {
            return peerConnections.size() < ServiceManager.sCfg.up2peerConnections;
        }
        else if ( isShieldedLeafNode() )
        {
            return peerConnections.size() < ServiceManager.sCfg.leaf2peerConnections;
        }
        else
        {
            return peerConnections.size() < ServiceManager.sCfg.peerConnections;
        }
    }

    /**
     * Used to chek if we have anymore ultrapeer slots. Usually this method
     * should only be used used as a Ultrapeer.
     * @return
     */
    public boolean hasUltrapeerSlotsAvailable()
    {
        // Note: That we don't response on pings when the slots are full this
        // results in not getting that many incomming requests.
        return ultrapeerConnections.size() < ServiceManager.sCfg.up2upConnections;
    }
    
    /**
     * Returns the number of open slots for leaf nodes. Usually this method
     * should only be used used as a Ultrapeer.
     * @return the number of open slots for leaf nodes.
     */
    public int getOpenUltrapeerSlotsCount()
    {
        return ServiceManager.sCfg.up2upConnections - ultrapeerConnections.size();
    }


    /**
     * Used to check if we would provide a Ultrapeer that will become a possible
     * leaf through leaf guidance a slot. This is only the case if we have not
     * already a ultrapeer too much and if we have a leaf slot available.
     * @return
     */
    public boolean hasLeafSlotForUltrapeerAvailable()
    {
        return hasLeafSlotsAvailable() &&
            // alow one more up2up connection to accept this possibly leaf guidanced
            // ultrapeer
            ultrapeerConnections.size() < ServiceManager.sCfg.up2upConnections + 1;
    }

    /**
     * Used to chek if we have anymore leaf slots. Usually this method
     * is only used used as a Ultrapeer.
     * @return
     */
    public boolean hasLeafSlotsAvailable()
    {
        // Note: That we don't response on pings when the slots are full this
        // results in not getting that many incomming requests.
        return leafConnections.size() < ServiceManager.sCfg.up2leafConnections;
    }
    
    /**
     * Returns the number of open slots for leaf nodes.
     * @return the number of open slots for leaf nodes.
     */
    public int getOpenLeafSlotsCount()
    {
        if ( hostMgr.isUltrapeer() )
        {
            return ServiceManager.sCfg.up2leafConnections - leafConnections.size();
        }
        return 0;
    }

    /**
     * Returns all available connected ultrapeers.
     * @return all available connected ultrapeers.
     */
    public synchronized Host[] getUltrapeerConnections()
    {
        Host[] hosts = new Host[ ultrapeerConnections.size() ];
        ultrapeerConnections.toArray( hosts );
        return hosts;
    }

    /**
     * Returns all available connected leafs.
     * @return all available connected leafs.
     */
    public synchronized Host[] getLeafConnections()
    {
        Host[] hosts = new Host[ leafConnections.size() ];
        leafConnections.toArray( hosts );
        return hosts;
    }

    /**
     * Returns all available connected peers.
     * @return all available connected peers.
     */
    public synchronized Host[] getPeerConnections()
    {
        Host[] hosts = new Host[ peerConnections.size() ];
        peerConnections.toArray( hosts );
        return hosts;
    }

    public synchronized int getTotalConnectionCount()
    {
        return ultrapeerConnections.size() +
            leafConnections.size() +
            peerConnections.size();
    }
    
    public int getLeafConnectionCount()
    {
        return leafConnections.size();
    }

    public synchronized int getUltrapeerConnectionCount()
    {
        return ultrapeerConnections.size();
    }
    
    /**
     * Returns a array of push proxy addresses or null if 
     * this is not a shielded leaf node.
     * @return a array of push proxy addresses or null.
     */
    public DestAddress[] getPushProxies() 
    {
        if ( isShieldedLeafNode() )
        {
            Iterator iterator = ultrapeerConnections.iterator();
            HashSet pushProxies = new HashSet();
            while ( iterator.hasNext() )
            {
                if ( pushProxies.size() == 4 )
                {
                    break;
                }
                Host host = (Host)iterator.next();
                DestAddress pushProxyAddress = host.getPushProxyAddress();
                if ( pushProxyAddress != null )
                {
                    pushProxies.add( pushProxyAddress );
                }
            }
            DestAddress[] addresses = new DestAddress[ pushProxies.size() ];
            pushProxies.toArray( addresses );
            return addresses;
        }
        return null;
    }

    /**
     * Adds a connected host to the connected host list. But only if its already
     * in the network host list.
     * @param host the host to add to the connected host list.
     */
    public synchronized void addConnectedHost( Host host )
    {
        // make sure host is still in network and not already removed
        if ( !networkHosts.contains( host ) )
        {// host is already removed by user action...
            disconnectHost( host );
            return;
        }

        if ( host.isUltrapeer() )
        {
            ultrapeerConnections.add( host );
            if ( host.isLeafUltrapeerConnection() )
            {
                leafUltrapeerConnectionCount++;
            }
        }
        else if ( host.isUltrapeerLeafConnection() )
        {
            leafConnections.add( host );
        }
        else
        {
            peerConnections.add( host );
        }

        /* We keep the connections it will be usefull to have one...
        // if the host is a ultrapeer we are a shielded leaf...
        // therefor drop all none ultrapeer connections!
        if ( host.isUltrapeer() )
        {
            dropAllNonUltrapeers();
        }*/
        //dump();
    }

    public synchronized void disconnectHost( Host host )
    {
        if ( host == null )
        {
            return;
        }

        if ( host.isUltrapeer() )
        {
            boolean isRemoved = ultrapeerConnections.remove( host );
            if ( isRemoved && host.isLeafUltrapeerConnection() )
            {
                leafUltrapeerConnectionCount--;
            }
        }
        else if ( host.isUltrapeerLeafConnection() )
        {
            leafConnections.remove( host );
        }
        else
        {
            peerConnections.remove( host );
        }

        // first disconnect to make sure that no disconnected host are added
        // to the routing.
        host.disconnect();
        // then clean routings
        MsgManager.getInstance().removeHost( host );

        fireNetworkHostChanged( host );

        //dump();
        // This is only for testing!!!
        /*if ( connectedHosts.contains( host ) )
        {
            // go crazy
            throw new RuntimeException ("notrem");
        }*/
    }

    /**
     * Checks for hosts that have a connection timeout...
     * Checks if a connected host is able to keep up...
     * if not it will be removed...
     */
    public synchronized void periodicallyCheckHosts()
    {
        Host host;
        int status;
        long currentTime = System.currentTimeMillis();

        Host[] badHosts = new Host[ networkHosts.size() ];
        int badHostsPos = 0;
        //boolean isShieldedLeafNode = isShieldedLeafNode();

        Iterator iterator = networkHosts.iterator();
        while( iterator.hasNext() )
        {
            host = (Host) iterator.next();

            status = host.getStatus();

            if ( status == HostConstants.STATUS_HOST_CONNECTED )
            {
                host.checkForStableConnection( currentTime );

                if ( ServiceManager.sCfg.mDisconnectApplyPolicy )
                {
                    String policyInfraction = null;
                    if ( host.tooManyDropPackets() )
                    {
                        policyInfraction = Localizer.getString( "TooManyDroppedPackets" );
                    }
                    else if ( host.isSendQueueTooLong() )
                    {
                        policyInfraction = Localizer.getString( "SendQueueTooLong" );
                    }
                    else if ( host.isNoVendorDisconnectApplying() )
                    {
                        policyInfraction = Localizer.getString( "NoVendorString" );
                    }
                    else if ( host.isFreeloader( currentTime ) )
                    {
                        policyInfraction = Localizer.getString( "FreeloaderNotSharing" );
                    }
                    if ( policyInfraction != null )
                    {
                        //Logger.logMessage( Logger.FINE, "log.core.msg",
                        //    "Applying disconnect policy to host: " + host +
                        //    " drops: " + host.tooManyDropPackets() +
                        //    " queue: " + host.sendQueueTooLong() );
                        host.setStatus( HostConstants.STATUS_HOST_ERROR, policyInfraction, currentTime );
                        disconnectHost( host );
                    }
                }
            }
            if ( ServiceManager.sCfg.mAutoCleanup )
            {
                // first collect...
                if ( status != HostConstants.STATUS_HOST_CONNECTED &&
                     status != HostConstants.STATUS_HOST_CONNECTING &&
                     status != HostConstants.STATUS_HOST_ACCEPTING )
                {
                    if ( host.isErrorStatusExpired( currentTime ) )
                    {
                        //Logger.logMessage( Logger.DEBUG, "log.core.msg",
                        //    "Cleaning up network host: " + host + " Status: " + status );
                        badHosts[ badHostsPos ] = host;
                        badHostsPos ++;
                        continue;
                    }
                }

                /* we dont drop non-ultrapeers in leaf mode anymore

                // actually this should never be a problem... but to make sure...
                if ( status == Host.STATUS_HOST_CONNECTED &&
                     isShieldedLeafNode && !host.isUltrapeer() )
                {
                    Logger.logMessage( Logger.FINER, Logger.NETWORK,
                        "Dropping none Ultrapeer: " + host );
                    badHosts[ badHostsPos ] = host;
                    badHostsPos ++;
                    continue;
                }*/
            }
        }
        // kill all bad hosts...
        if ( badHostsPos > 0 )
        {
            removeNetworkHosts( badHosts );
        }
    }

    public synchronized Host getNetworkHostAt( int index )
    {
        if ( index < 0 || index >= networkHosts.size() )
        {
            return null;
        }
        return (Host) networkHosts.get( index );
    }

    public synchronized Host[] getNetworkHostsAt( int[] indices )
    {
        int length = indices.length;
        Host[] hosts = new Host[ length ];
        for ( int i = 0; i < length; i++ )
        {
            hosts[i] = (Host)networkHosts.get( indices[i] );
        }
        return hosts;
    }
    
    public synchronized Host getNetworkHost( DestAddress address )
    {
        for ( Iterator i = networkHosts.iterator(); i.hasNext(); )
        {
            Host networkHost = (Host) i.next();
            DestAddress networkAddress = networkHost.getHostAddress();
            if ( networkAddress.equals( address ) )
            {
                return networkHost;
            }            
        }
        //not found
        return null;
    }

    /**
     * Returns the count of the complete neighbour hood, containing all 
     * connected and not connected hosts independent from its connection type.
     */
    public synchronized int getNetworkHostCount()
    {
        return networkHosts.size();
    }

    /**
     * Returns the count of the networks hosts with the given status.
     */
    public synchronized int getNetworkHostCount( int status )
    {
        int count = 0;
        Iterator iterator = networkHosts.iterator();
        while( iterator.hasNext() )
        {
            Host host = (Host)iterator.next();
            if ( host.getStatus() == status )
            {
                count ++;
            }
        }
        return count;
    }
    
    /**
     * Adds a host to the network host list.
     * @param host the host to add to the network host list.
     */
    public synchronized void addNetworkHost( Host host )
    {
        int position = networkHosts.size();
        networkHosts.add( host );
        fireNetworkHostAdded( position );
        //dump();
        
        try
        {
            // send udp ping as soon as we have recognized host
            UdpConnectionManager.getInstance().sendUdpPing( 
                    host.getHostAddress() );
        }
        catch ( IOException e )
        {
            NLogger.warn( NLoggerNames.UDP_OUTGOING_MESSAGES, " could " +
            		" not send udp ping to : " + host, e );
        }
    }
    
    public synchronized boolean isConnectedToHost( DestAddress address )
    {
        // Check for duplicate.
        for (int i = 0; i < networkHosts.size(); i++)
        {
            Host host = (Host)networkHosts.get( i );
            if ( host.getHostAddress().equals( address ) )
            {// already connected
                return true;
            }
        }
        return false;
    }

    public synchronized void createOutgoingConnectionToHost( DestAddress address )
    {
        OutgoingConnectionDispatcher dispatcher =
            new OutgoingConnectionDispatcher( );
        dispatcher.setHostAddressToConnect( address );
        ThreadPool.getInstance().addJob( dispatcher,
            "OutgoingConnectionDispatcher-" + Integer.toHexString(hashCode()) );
    }
    
    public synchronized void createOutConnectionToNextHosts( int count )
    {
        for ( int i = 0; i < count; i ++ )
        {
            OutgoingConnectionDispatcher dispatcher =
                new OutgoingConnectionDispatcher( );
            ThreadPool.getInstance().addJob( dispatcher,
                "OutgoingConnectionDispatcher-" + Integer.toHexString(hashCode()) );
        }
    }

    public synchronized void addIncomingHost( Host host )
    {
        // a incoming host is new for the network and is connected
        addNetworkHost( host );
        addConnectedHost( host );
        //dump();
    }

    public synchronized void removeAllNetworkHosts()
    {
        Host host;
        while ( networkHosts.size() > 0 )
        {
            host = (Host) networkHosts.get( 0 );
            internalRemoveNetworkHost( host );
        }
    }

    public synchronized void removeNetworkHosts( Host[] hosts )
    {
        Host host;
        int length = hosts.length;
        for ( int i = 0; i < length; i++ )
        {
            host = hosts[ i ];
            internalRemoveNetworkHost( host );
        }
    }

    /**
     * This is the only way a Host gets diconnected right!
     * The Host disconnect method is only used to clean up the connection.
     */
    public synchronized void removeNetworkHost( Host host )
    {
        internalRemoveNetworkHost( host );
    }

    /*
    // Not used anymore but might be used again later...
    public synchronized void dropAllNonUltrapeers()
    {
        Logger.logMessage( Logger.FINE, Logger.NETWORK, "Dropping all non-Ultrapeers" );
        Host host;
        for ( int i = connectedHosts.size() - 1; i >= 0; i-- )
        {
            host = (Host) connectedHosts.get( i );
            if ( !host.isUltrapeer() )
            {
                Logger.logMessage( Logger.FINER, Logger.NETWORK,
                    "Dropping non-Ultrapeer: " + host );
                internalRemoveNetworkHost( host );
            }
        }
    }*/

    /**
     * Disconnects from host.
     */
    private synchronized void internalRemoveNetworkHost( Host host )
    {
        if ( host == null )
        {
            return;
        }
        disconnectHost( host );
        int position = networkHosts.indexOf( host );
        if ( position >= 0 )
        {
            networkHosts.remove( position );
            fireNetworkHostRemoved( position );
        }
        //dump();
        // This is only for testing!!!
        /*if ( connectedHosts.contains( host ) )
        {
            // go crazy
            throw new RuntimeException ("notrem");
        }*/
    }

    ///////////////////// START event handling methods ////////////////////////
    public void addNetworkHostsChangeListener( NetworkHostsChangeListener listener )
    {
        synchronized ( listenerList )
        {
            listenerList.add( listener );
        }
    }

    public void removeNetworkHostsChangeListener( NetworkHostsChangeListener listener )
    {
        synchronized ( listenerList )
        {
            listenerList.remove( listener );
        }
    }


    private void fireNetworkHostChanged( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                NetworkHostsChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (NetworkHostsChangeListener)listeners[ i ];
                    listener.networkHostChanged( position );
                }
            }
        });
    }

    private void fireNetworkHostAdded( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                NetworkHostsChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (NetworkHostsChangeListener)listeners[ i ];
                    listener.networkHostAdded( position );
                }
            }
        });
    }

    private void fireNetworkHostRemoved( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                try
                {
                    Object[] listeners = listenerList.toArray();
                    NetworkHostsChangeListener listener;
                    // Process the listeners last to first, notifying
                    // those that are interested in this event
                    for ( int i = listeners.length - 1; i >= 0; i-- )
                    {
                        listener = (NetworkHostsChangeListener)listeners[ i ];
                        listener.networkHostRemoved( position );
                    }
                }
                catch ( Throwable th )
                {// catch all
                    NLogger.error( NLoggerNames.GLOBAL, th, th );
                }
            }
        });
    }

    public void fireNetworkHostChanged( Host host )
    {
        int position = networkHosts.indexOf( host );
        if ( position >= 0 )
        {
            fireNetworkHostChanged( position );
        }
    }

    // BEGIN NetworkListener handler methods
    public void connectedToNetwork()
    {
    }

    public void disconnectedFromNetwork()
    {
        // Disconnect all hosts
        removeAllNetworkHosts();
    }

    public void networkIPChanged(DestAddress hostAddress)
    {
        // not implemented
    }

    /**
     * adds to the container listing the hosts that have free 
     * ultrapeer slots.This is done in a synchronized fashion.
     * the maximum no of hosts that are allowed in this container 
     * is FREE_SLOT_SIZE.
     * if the container is full then new host is added into the container
     * and the host with the least uptime is removed from the container
     * @param aHost
     */
    public void addToFreeUltrapeerSlotSet( Host aHost )
    {
        synchronized ( freeUltrapeerSlotSet )
        {
            freeUltrapeerSlotSet.add( aHost );
            if( freeUltrapeerSlotSet.size() > FREE_SLOT_SET_SIZE )
            {
                TreeSet treeSetView = (TreeSet) freeUltrapeerSlotSet;
                Host lastHost = (Host) treeSetView.last();
                freeUltrapeerSlotSet.remove( lastHost );
            }
        }                
    }
    
    /**
     * adds to the container listing the hosts that have free 
     * leaf slots.This is done in a synchronized fashion.
     * the maximum no of hosts that are allowed in this container 
     * is FREE_SLOT_SIZE.
     * if the container is full then new host is added into the container
     * and the host with the least uptime is removed from the container
     * @param aHost
     */
    public void addToFreeLeafSlotSet( Host aHost )
    {
        synchronized ( freeLeafSlotSet )
        {
            freeLeafSlotSet.add( aHost );
            if( freeLeafSlotSet.size() > FREE_SLOT_SET_SIZE )
            {
                TreeSet treeSetView = (TreeSet) freeLeafSlotSet;
                Host lastHost = (Host) treeSetView.last();
                freeLeafSlotSet.remove( lastHost );
            }
        }
    }
    
    public Collection getFreeUltrapeerSlotHosts( )
    {
        synchronized ( freeUltrapeerSlotSet )
        {
            ArrayList freeHosts = new ArrayList( freeUltrapeerSlotSet );
            freeHosts.trimToSize();
            return freeHosts;
        }
    }
    
    public Collection getFreeLeafSlotHosts( )
    {
        synchronized ( freeLeafSlotSet )
        {
            ArrayList freeHosts = new ArrayList( freeLeafSlotSet );
            freeHosts.trimToSize();
            return freeHosts;
        }
    }
    
    private Collection createFreeSlotContainer()
    {
        Collection freeSlotSet = new TreeSet( new SlotHostComparator() );
        return freeSlotSet;
    }

    /**
     * used in the creation of tree sets for the freeSlots containers
     * orders the hosts in decreasing order of uptime
     * 
     * @author Madhu
     */
    private class SlotHostComparator implements Comparator
    {
        // orders in decreasing order of uptime 
        public int compare( Object o1, Object o2 )
        {
            Host hostA = (Host) o1;
            Host hostB = (Host) o2;
            
            DestAddress addrA = hostA.getHostAddress();
            DestAddress addrB = hostB.getHostAddress();
            
            //if same address
            if( addrA.equals( addrB ) )
            {
                return 0;
            }
            
            long currentTime = System.currentTimeMillis();
            
            //now not the same adress, so order by uptime
            if( hostA.getConnectionUpTime( currentTime )
                    > hostB.getConnectionUpTime( currentTime ) )
            {
                return -1;
            }
            else if ( hostA.getConnectionUpTime( currentTime )
                    < hostB.getConnectionUpTime( currentTime ) )
            {
                return 1;
            }
            
            // now their up time is equal so return -1
            // can be 1 also........ just done this way to 
            // to satisfy the sgn constraint
            return -1;
        }        
    }
    // END NetworkListener handler methods

    ///////////////////// END event handling methods ////////////////////////


    /////////////////////////// debug ///////////////////////////////////////
    /*private synchronized void dump()
    {
        System.out.println( "-------------- network -----------------" );
        Iterator iterator = networkHosts.iterator();
        while ( iterator.hasNext() )
        {
            System.out.println( iterator.next() );
        }
        System.out.println( "-------------- connected ---------------" );
        iterator = connectedHosts.iterator();
        while ( iterator.hasNext() )
        {
            System.out.println( iterator.next() );
        }
        System.out.println( "----------------------------------------" );
    }*/
}