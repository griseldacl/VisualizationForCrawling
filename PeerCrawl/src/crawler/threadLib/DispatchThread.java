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

import java.util.Vector;

import phex.query.Search;
import crawler.PeerCrawlFrame;
import crawler.util.UrlWithDepth;


/**
 * Class DispatchThread : Used to broadcast urls to others peers in the network
 * 
 * @author Tushar
 */
public class DispatchThread extends Thread 
{
	// Data Structure that batches the broadcasts
	private Vector<UrlWithDepth> Dispatch;

	
	/**
	 * Constructor
	 * @param toBeDispatched  list of urls to be dispatched
	 */
	public DispatchThread(Vector toBeDispatched) 
	{
		this.Dispatch = toBeDispatched;
		this.start();
	}

	
	/**
	 * Thread to broadcast urls
	 */
	public void run() 
	{
		Search search = new Search(null);
		String sendurl = "";

		try 
		{
			while (!(Dispatch.isEmpty())) 
			{
				sendurl = (String) Dispatch.elementAt(0).getUrl();
				Dispatch.removeElementAt(0);
				search.setSearchString(sendurl);
				search.startSearching();
				PeerCrawlFrame.tarea5.append("Broadcast : " + sendurl + "\n");
			}
		} catch (Exception ex) { 
			System.out.println("Exception while dispacthing data to peers");
			ex.printStackTrace();
		}
	} // End of Function
	
} // End of Class DispatchThread 
