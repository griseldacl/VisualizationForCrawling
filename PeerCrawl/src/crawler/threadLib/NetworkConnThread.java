/**
 * PeerCrawl - Distributed P2P web crawler based on Gnutella Protocol
 * @version 5.0
 * 
 * Developed as part of Masters Project - Spring 2006
 * @author 	Vaibhav Padliya
 * 			College of Computing
 * 			Georgia Tech
 * 
 * @contributor Mudhakar Srivatsa
 * @contributor Mahesh Palekar
 * @contributor Tushar Bansal
 * @contributor Suiyang Li
 */


package crawler.threadLib;

import java.net.InetAddress;

import crawler.PeerCrawlFrame;
import crawler.util.Const;
import crawler.util.StatsFile;
import phex.host.Host;
import phex.host.HostManager;
import phex.host.NetworkHostsContainer;

/**
 * Class NetworkConnThread : Used to track peers joining/leaving network
 * 
 * @author Tushar
 */
public class NetworkConnThread extends Thread 
{
	private NetworkHostsContainer networkHostsContainer;

	// Bookkeeping info
	private StatsFile network;

	/**
	 * Constructor
	 */
	public NetworkConnThread() 
	{
		try 
		{
			networkHostsContainer = HostManager.getInstance().getNetworkHostsContainer();
			network = new StatsFile(Const.NETWORK_STATS_FILE);
			network.writeData("CurrentTimeMillis, CONNECTED TO PEERS - IP_ADDRESS : PORT_NUMBER");
		} catch (Exception e) {
			System.out.println("Exception while writing to network stats file");
			e.printStackTrace();
		}

		this.setName("Network Connection Thread");
		this.setPriority(Thread.NORM_PRIORITY - 1);
		this.start();
	}

	
	/**
	 * Thread for connecting to peers
	 */
	public void run() 
	{
		System.out.println(this.getName() + " Started");
		
		Host[] peers = null;
		String stats = "";
		int size = 0;
		String peerAddr = "";
		boolean modified = false;

		while (true) 
		{
			try {
				peers = networkHostsContainer.getUltrapeerConnections();
				if(peers.length != size)
				{
					PeerCrawlFrame.tarea1.setText("");
					modified = true;
				}
				
				for (int i = 0; i < peers.length; i++) 
				{
					InetAddress selfAddr = InetAddress.getLocalHost();
					peerAddr = peers[i].getHostAddress().getFullHostName();
					if( peerAddr.equals(selfAddr.getHostAddress() + ":8000") )
							continue;
					
					stats = System.currentTimeMillis() + "," + peerAddr;
					network.writeData(stats);
					
					if(modified)
						PeerCrawlFrame.tarea1.append("Connected to Peer[" + 
							peerAddr + "]\n");
				}
				size = peers.length;
				modified = false;
				
				Thread.sleep(Const.NETCONN_SLEEP_TIME);
			} catch (Exception e) {
				System.out.println("Exception while connecting to other peers");
				e.printStackTrace();
				continue;
			}
		}
	} // End of Function
	
} // End of Class NetworkConnThread
