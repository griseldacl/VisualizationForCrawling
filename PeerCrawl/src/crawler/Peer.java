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

package crawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import crawler.threadLib.BackupThread;
import crawler.threadLib.CachingThread;
import crawler.threadLib.FetchThread;
import crawler.threadLib.NetworkConnThread;
import crawler.threadLib.ProcessThread;
import crawler.threadLib.StatsThread;
import crawler.util.Bloom;
import crawler.util.Const;
import crawler.util.Document;
import crawler.util.DomainWithDepth;
import crawler.util.RobotsExclusion;
import crawler.util.StatsFile;
import crawler.util.UrlWithDepth;
import crawler.util.VirtualID;

/** 
 * Class Peer: Responsible for firing up different threads
 * 				to do the crawler tasks
 * 
 * @author Vaibhav
 * @contributor Tushar
 */

public class Peer {

	private static Peer instance;
	public static VirtualID myVirtualID;			// The hash of the peer's IP address
	private static String userEnteredUrl = ""; 

	
	/** 
	 * Getting a singleton instance 
	 * 
	 * @return Peer instance
	 */
	public static Peer getInstance()
	{
		if ( Peer.instance == null )
		{
			System.out.println("Starting new instance of crawler.");
			Peer.instance = new Peer(Const.initialNode);
		}
	    
		return Peer.instance;
	}
	

	
	/** 
	 * Constructor: Fires off different threads for various jobs
	 * 
	 * @param whether root node
	 */
	public Peer( final boolean rootNode )
	{
		// Getting the domain from seed url
		if( Const.focusedCrawl )
		{
			userEnteredUrl = Const.currentURL;
			
			if(Const.currentURL.startsWith("http://www."))
				userEnteredUrl = Const.currentURL.substring(11, Const.currentURL.length());
			else if(Const.currentURL.startsWith("www."))
				userEnteredUrl = Const.currentURL.substring(4, Const.currentURL.length());
		}
		
		// Getting the virtual identifier used by Gnutella
		try {
			myVirtualID = new VirtualID(InetAddress.getLocalHost().getHostAddress(),0);
		} catch (UnknownHostException e) {
			System.out.println("Unknown host exception while getting local id");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("Exception while getting self Virtual Id");
			e.printStackTrace();
		}   
		  
		
		// Initialize data structures 
	  	FetchThread.crawlJobs = new Vector<UrlWithDepth>(1000,500);   
	  	ProcessThread.URLInfo = new Hashtable<String, Bloom>();    
	  	RobotsExclusion.Robots = new HashMap<String, DomainWithDepth>();
	  	ProcessThread.fetchBuffer = new Vector<Document>(1000,200);
	  	
	  	
	  	// Initializating Statistics files 			   
	  	try {
			FetchThread.urls = new StatsFile(Const.CRAWL_URLS_STATS_FILE);
		} catch (Exception e) {
			System.out.println("Exception while creating Statsfile");
			e.printStackTrace();
		}
		
		
	  	FetchThread.urls.writeData("CurrentTimeMillis, URLS CRAWLED");
			   
	  	// Initial node in the network starts the crawl using seed url
		if (rootNode) 
		{
			UrlWithDepth url = new UrlWithDepth(Const.currentURL, 0);
			FetchThread.crawlJobs.addElement(url);
			StatsThread.totalAdded++;
			
			// Check for Robots Exclusion URLs
			RobotsExclusion.Robots.put(Const.currentURL, 
										new DomainWithDepth(Const.currentURL, 0) );

			// Initialize bloom filters for the root node 
			final Bloom bloomFilter = new Bloom();
			bloomFilter.set(Const.currentURL);
			ProcessThread.URLInfo.put(url.getDomain(), bloomFilter);
		}

		
		// Load checkpointed data structures from secondary storage
		loadCheckpointedData();
		
		// Thread that generates statistics
		new StatsThread();
		
		// Thread that keeps track of network connections for peers
		new NetworkConnThread();

		
		/* Threads are started in an interleaved manner ! 
		 * We do not want all threads of the same kind acting together!
		 * Better to sparse them out.
		 */			
		
		// Usually more than one thread of these kind is required
		for ( int i = 0; i < Const.MAX_THREADS; i++) 
		{
			// Thread that gets url pages from the web server 
			new FetchThread();
			
			// Thread that scans a page for urls
			new ProcessThread();
		}

		// Thread for periodically backing up data structures
		new BackupThread();
		
		// Threads for saving data onto disk
		if(Const.cacheDocs)
		{
			new CachingThread();
			new CachingThread();
		} 		
	}	// End of Constructor
	
	
	/**
	 * Function to check for focused crawling domain.
	 * 
	 * @param url to be checked
	 * @return true if url within domain or if not a focused crawl task 
	 */
	public static boolean isInDomain(UrlWithDepth url)
	{
		return !Const.focusedCrawl || 
					((url.getUrl().indexOf("http://" + userEnteredUrl) > 0) ||
							(url.getUrl().indexOf("." + userEnteredUrl) > 0));
	} // End of function isInDomain()
		
	
	public void loadCheckpointedData()	
	{
		try{
			if( (new File(Const.CRAWL_JOB_PATH)).exists() && 
					(new File(Const.FETCH_BUFFER_PATH)).exists() && 
					(new File(Const.URL_INFO_PATH)).exists() )
			{
				//loading crawlJobs
				FileInputStream in = new FileInputStream(Const.CRAWL_JOB_PATH);
				ObjectInputStream s = new ObjectInputStream(in);
				FetchThread.crawlJobs = (Vector)s.readObject();
				s.close();
				in.close();
				
				//loading fetchBuffer
				in = new FileInputStream(Const.FETCH_BUFFER_PATH);
				s = new ObjectInputStream(in);
				ProcessThread.fetchBuffer = (Vector)s.readObject();
				s.close();
				in.close();
						
				//loading URLInfo
				in = new FileInputStream(Const.URL_INFO_PATH);
				s = new ObjectInputStream(in);
				ProcessThread.URLInfo = (Hashtable)s.readObject();
				s.close();
				in.close();
			
			}
		}catch(final Exception e)
			{ 
				FetchThread.crawlJobs = new Vector<UrlWithDepth>();    
				ProcessThread.URLInfo = new Hashtable<String, Bloom>();    
				ProcessThread.fetchBuffer = new Vector<Document>();
				RobotsExclusion.Robots = new Hashtable<String, DomainWithDepth>();
	
				System.out.println("Exception while loading checkpointed data");
				e.printStackTrace();
			}		
	} // End of function loadCheckpointedData()
	
	
	/** 
	 * Function to dynamically recompute the crawl range for this peer 
	 * depending on number of hosts on its horizon
	 * 
	 * @param total no of peers in network
	 */ 
	public void recomputeCrawlRange(final int totalHostCount) 
	{
		int offset=0;
	 	double d;
	 	double mean;
	 	double floor;
	 	
	 	System.out.println("Recomupting Crawl Range.");
		d = Math.log(totalHostCount)/Math.log(2);

		floor = Math.floor(d);
		mean = Math.pow(2,floor)+Math.pow(2,floor-1);
		
		offset = (totalHostCount <= mean)? (new Double(floor)).intValue()
											:(new Double(Math.ceil(d))).intValue();
			
	 	myVirtualID.ComputeCrawlRange(offset);
	} // End of function recomputeCrawlRange()
		
	 
	/**
	 * Function that catches queries from other peers
	 * 
	 * @param url that is cought from other peers
	 * @return true if url within allocated IP-Address space of peer
	 */
	public boolean processQuery(final String urlString) 
	{
		try {
			PeerCrawlFrame.tarea5.append("Caught URL: "+urlString+"...");
			UrlWithDepth url = new UrlWithDepth(urlString, 0);
			
			final VirtualID vdomain = new VirtualID(url.getDomain());
			// Incase this peer is responsible for that URL
			if(myVirtualID.shouldCrawl(vdomain) && !RobotsExclusion.disallows(url))
			{
				PeerCrawlFrame.tarea5.append("Added\n");
				synchronized(FetchThread.crawlJobs){
					ProcessThread.updateCrawlJobsAndURLInfo(url);
				}
				return true;
			}
		} catch (final Exception e) {
			e.printStackTrace(); 
		}
		PeerCrawlFrame.tarea5.append("Dropped\n");
		return false;
	} // End of function processQuery()

	
} // End of Class Peer