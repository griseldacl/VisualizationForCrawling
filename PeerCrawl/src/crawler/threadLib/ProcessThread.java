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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import crawler.Peer;
import crawler.URLExtractor;
import crawler.util.Bloom;
import crawler.util.Const;
import crawler.util.Document;
import crawler.util.RobotsExclusion;
import crawler.util.UrlWithDepth;
import crawler.util.VirtualID;

/**
 * Class ProcessThread : Used to parse urls in a fetched page
 * 							Maintains bloom filter for checking duplicate urls
 * 							Forwards urls for broadcasting to Dispactch Thread
 * @author Tushar
 */

public class ProcessThread extends Thread 
{
	// Data structure that maintains documents fetched from web server in memory
	public static Vector<Document> fetchBuffer;

	// Hashtable for URL duplication checking
	public static Hashtable<String, Bloom> URLInfo;

	// No of process threads (usually same as the no of fetch threads)
	public static int processThreadCount = 0;

	/**
	 * Constructor
	 */
	public ProcessThread() 
	{
		this.setName("Process Thread " + ProcessThread.processThreadCount++);
		this.setPriority(Thread.NORM_PRIORITY + 1);
		this.start();
	}

	
	/**
	 * Thread that does the processing of documents
	 */
	public void run() 
	{
		System.out.println(this.getName() + " Started");

		final Vector<UrlWithDepth> Dispatch = new Vector<UrlWithDepth>();
		Document doc = null;

		while (true) 
		{
			try {
				Thread.sleep(100);
				
				// check for non-processed urls in fetchBuffer
				synchronized (ProcessThread.fetchBuffer) 
				{
					for (int i = 0; i < ProcessThread.fetchBuffer.size(); i++)
						if (!ProcessThread.fetchBuffer.get(i).isProcessed()) 
						{
							ProcessThread.fetchBuffer.get(i).setProcessed(true);
						
							// if we dont need to cache then free memory occupied by doc
							doc = Const.cacheDocs? ProcessThread.fetchBuffer.get(i)
												: ProcessThread.fetchBuffer.remove(i);
							break;
						}
				}

				if (doc == null)
					continue;

				// Using URLExtractor to parse urls from file
				final List<UrlWithDepth> urlList = URLExtractor.extractURLs(doc);
				
				if(urlList == null || urlList.size() <= 0)
					continue;
				
				for (final Iterator itr = urlList.iterator(); itr.hasNext();) 
				{
					final UrlWithDepth generatedURL = (UrlWithDepth) itr.next();
					final VirtualID vdomain = new VirtualID(generatedURL.getDomain());

					// Check for robots exclusion urls
					if (RobotsExclusion.disallows(generatedURL)) 
					{
						System.out.println("Url " + generatedURL.getUrl()
								+ " Excluded due to Robots Text");
						continue;
					}

					// If url is not within domain in focused crawl then
					// increment hop count
					if( !Peer.isInDomain(generatedURL) )
						generatedURL.setHopCount(doc.getUrl().getHopCount() + 1);
					
					// use if you need to find inlinks in focused crawl
					/*	else if( !Peer.isInDomain(doc.getUrl()) )
						System.out.println("Inlink URL from " + doc.getUrl().getUrl() + 
								" to " + generatedURL.getUrl() );
					 */					
					
					// If the local peer is responsible for that generated
					if (Peer.myVirtualID.shouldCrawl(vdomain))
						ProcessThread.updateCrawlJobsAndURLInfo(generatedURL);
					else // Dispatch to Peers
					{
						Thread.sleep(100);
						Dispatch.addElement(generatedURL);
						//PeerCrawlFrame.tarea5.append("broadcast : " + generatedURL.getUrl() + "\n");
						if (Dispatch.size() > Const.DISPATCH_THRESHOLD) 
						{
							final Vector<UrlWithDepth> tempVector = 
								(Vector<UrlWithDepth>) Dispatch.clone();
							new DispatchThread(tempVector);
							Dispatch.clear();
						}
					}
				} // End of loop for Extracting Various URLs from a buffered page

				doc = null;

			} catch (final Exception e) {
				e.printStackTrace();
			}
		} // While true

	}

	
	/**
	 * Function that checks if the urls is with depth-bounds. Also checks if the url 
	 * is not off more than allowed the domain to be crawled in focused crawl
	 * @param url
	 * @return
	 */
	private static boolean isToBeCrawled(final UrlWithDepth url)
	{
		return ( (url.getHopCount() <= Const.MAX_HOP_COUNT) 
					&& (url.getDepth() <= Const.MAX_DEPTH));
	}

	
	/**
	 * Adding newly found urls from parsing back to the crawl jobs
	 * @param url
	 */
	public static void updateCrawlJobsAndURLInfo(final UrlWithDepth url) 
	{
		if (!ProcessThread.isToBeCrawled(url))
			return;

		// Check if the no of crawl jobs are out of limits
		if (FetchThread.checkAddCrawlJob()) 
		{
			final String domain = url.getDomain();
			
			// If first url in domain
			if (ProcessThread.URLInfo.get(domain) == null) 
			{
				final Bloom bloomFilter = new Bloom();
				bloomFilter.set(url.getUrl());
				ProcessThread.URLInfo.put(domain, bloomFilter);
				FetchThread.addCrawlJob(url);
			} 
			else // Check for bloom filter for that domain 
			{
				final Bloom bloomFilter = ProcessThread.URLInfo.get(domain);
				if (!bloomFilter.test(url.getUrl())) 
				{
					bloomFilter.set(url.getUrl());
					FetchThread.addCrawlJob(url);
				} 
				else // duplicate url
					StatsThread.totalDuplicated++;
			}
		} 
		else	// too many crawl jobs
			StatsThread.totalDropped++;
	} // End of function updateCrawlJobsAndURLInfo()
	
	/**
	 * Function to add document for processing and caching
	 * @param doc
	 */
	public static void addFetchBuffer(Document doc) 
	{
		synchronized (StatsThread.mutex) {
			StatsThread.crawledURLs++;
		}
		
		try {
			synchronized (ProcessThread.fetchBuffer) {
				ProcessThread.fetchBuffer.add(doc);
			}
		} catch (final Exception e) {}
	}
	
} // End of Class ProcessThread
