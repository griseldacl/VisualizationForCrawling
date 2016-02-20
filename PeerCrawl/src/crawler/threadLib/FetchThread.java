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

import crawler.PeerCrawlFrame;
import crawler.URLFetcher;
import crawler.util.Const;
import crawler.util.Document;
import crawler.util.StatsFile;
import crawler.util.UrlWithDepth;

/**
 * Class FetchThread : Gets document from web
 * @author Tushar
 */

public class FetchThread extends Thread 
{
	// Data Structure that stores document urls to be fetched
	public static Vector<UrlWithDepth> crawlJobs;
	
	// Statistics file containing the urls crawled by this peer
	public static StatsFile urls; 

	// No of fetch threads (usually same as process threads = Const.MAX_THREADS )
	public static int fetchThreadCount = 0;

	
	/**
	 * Constructor
	 */
	public FetchThread() 
	{
		this.setName("Fetch Thread " + fetchThreadCount++);
		this.setPriority(Thread.NORM_PRIORITY + 1);
		this.start();
	}

	
	/**
	 * Thread for fetching urls
	 */
	public void run()
	{
		UrlWithDepth url = null;
		String urlDataBuffer = null;
		Vector<UrlWithDepth> domURLs = null;
		Document doc = null;

		System.out.println(this.getName() + " Started");
		while (true) 
		{
			try {
				Thread.sleep(100);
				
				// Get the entire batch of crawljobs
				synchronized (crawlJobs) 
				{
					domURLs = (Vector) crawlJobs.clone();
					crawlJobs.clear();
				}
				
				if((domURLs == null) || (domURLs.size() <= 0))	
					continue;
				
				for (int i = 0; i < domURLs.size(); i++) 
				{
					url = domURLs.get(i);

					// Update stats
					synchronized (StatsThread.mutex) {
						StatsThread.seenURLs++;
					}

					try {
						// Use URLFetcher to get urls
						doc = URLFetcher.fetchHttp(url);
						
						// If successfully got url
						if (doc != null) 
						{
							ProcessThread.addFetchBuffer(doc);
							
							// Update Stats file
							urlDataBuffer = System.currentTimeMillis() + "," + url.getUrl();
							urls.writeData(urlDataBuffer);
							PeerCrawlFrame.tarea2.append("Getting url..." + url.getUrl() + "\n");	
						} 
						
						// Use code if socket timeout on urls
						/* else 
						{
							synchronized (CrawlJobs) {
								CrawlJobs.add(url);
							}
						}*/
						} catch (final Exception e) {
					}
				}
			} catch (final Exception e1) {
				e1.printStackTrace();
				continue;
			}
		}
	} // End of thread

	
	/**
	 * Function to add more jobs for crawling
	 * @return true  if number of crawl jobs has not exceeded limits 
	 */
	public static boolean checkAddCrawlJob() 
	{
		boolean flag = true;
		int size = 0;

		synchronized (FetchThread.crawlJobs) {
			size = FetchThread.crawlJobs.size();
		}

		if (size >= Const.CRAWL_MAXJOB_LIMIT) 
		{
			flag = false;
		
			// If max limit has been reached then we start a new fetch thread 
			// 	till the max no of threads the system can handle
			if( FetchThread.fetchThreadCount < Const.MAX_CRAWL_THREADS )
			{
				System.out.println("Starting new Fetch Thread");
				new FetchThread();
			}
		}

		return flag;
	}

	/**
	 * Function to add jobs for crawling
	 * @param url
	 */
	public static void addCrawlJob(UrlWithDepth url) 
	{
		synchronized (StatsThread.mutex) {
			StatsThread.totalAdded++;
		}
		
		try {
			synchronized (FetchThread.crawlJobs) {
					FetchThread.crawlJobs.addElement(url);
				}
		} catch (final Exception e) {
		}
	}
	
} // End of Class FetchThread
