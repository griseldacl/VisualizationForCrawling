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

import java.text.DecimalFormat;

import crawler.PeerCrawlFrame;
import crawler.URLFetcher;
import crawler.util.Const;
import crawler.util.StatsFile;

/**
 * Class StatsThread : Gather and store statistics for every peer
 * @author Tushar
 */

public class StatsThread extends Thread 
{
	// Mutex for statistics collection in fetch thread
	public static final Integer mutex = new Integer(10);

	// Contains per sond statistics
	private StatsFile second_data;

	// Contains per minute statistics
	private StatsFile minute_data;

	// Contains http connection information
	private StatsFile http_data; 
	
    // Total number of URLs cached
	public static int totalCached = 0;

	// Total number of URLs crawled
	public static int crawledURLs = 0; 

	// Total number of URLs encountered/seen
	public static int seenURLs = 0; 

	// Total number of URLs added to the crawl list
	public static int totalAdded = 0; 

	// Total number of URLs dropped ==> not crawled by this peer
	public static int totalDropped = 0; 

	// Total number of URLs seen more than once by this peer
	public static int totalDuplicated = 0; 

	// Number of robots.txt for every url
	public static int numberRobots = 0;

	public StatsThread() {
		try {
			second_data = new StatsFile(Const.PerSecond_STATS_FILE);
			minute_data = new StatsFile(Const.PerMinute_STATS_FILE);
			http_data = new StatsFile(Const.HTTP_STATS_FILE);

			second_data.writeData("CurrentTimeMillis, URLsCrawled/sec, UrlsAdded/Sec, "
							+ "SeenURLs/sec, SeenURLS/AddedURLs, QueueRatio, Content/sec, "
							+ "totalURLsDropped, totalURLsDuplicated");

			minute_data.writeData("CurrentTimeMillis, URLsCrawled/min, UrlsAdded/min, "
							+ "SeenURLs/min, TotalCached, CrawlJobQueueSize, FetchBufferSize, "
							+ "CrawledURLs/AddedURLs/min, QueueGrowth");

			http_data.writeData("CurrentTimeMillis, TotalContentSize, TotalURLsDropped, "
							+ "TotalURLsDuplicated, numberOfRobots, 200,404,302,301,403,401,500,406,400,"
							+ "other, text/html, image/gif, image/jpeg, text/plain,"
							+ "applications/pdf, audio/x-pn-realaudio, applications/zip,"
							+ "applications/postscript, otherM");
		} catch (final Exception e) {
			e.printStackTrace();
		}

		this.setName("Statistics Thread");
		this.start();
	}

	
	/**
	 * Thread for collecting statistics
	 */
	public void run() {
		System.out.println(this.getName() + " Started");

		int counter = 0;
		
		// Used when crawler is inactive for a long amount of time
		int deadCounter = 0;

		int urlsCrawledStart;
		int urlsCrawledEnd = 0;
		int urlsCrawledPM = 0; // urls crawled per minute
		int urlsCrawledPS = 0; // urls crawled per second

		int urlsSeenStart;
		int urlsSeenEnd = 0;
		int urlsSeenPM = 0; // urls seen (removed from crawl queue) per minute
		int urlsSeenPS = 0; // urls seen (removed from crawl queue) per second

		int urlsAddedStart;
		int urlsAddedEnd = 0;
		int urlsAddedPM = 0; // urls added to crawl queue per minute
		int urlsAddedPS = 0; // urls added to crawl queue per second

		int totalContentStart;
		int totalContentEnd = 0;
		int totalContentPM = 0; // total content downloaded per minute
		int totalContentPS = 0; // total content downloaded per second

		int totalDropped = 0;
		int totalDuplicated = 0;
		int numberRobots = 0;
		int httpResponses[];
		int mimeTypes[];
		int sizeof_crawljob;
		int totalCached;
		int sizeof_fetchbuffer;
		float queueRatio = 0;
		float queueGrowth = 0;
		float crawledPerAdded = 0;
		float crawledPerAddedPM = 0;

		final DecimalFormat df = new DecimalFormat("0.##");

		String secondDataBuffer;
		String minuteDataBuffer;
		String httpDataBuffer;

		while (true) 
		{
			try {
				urlsCrawledStart = urlsCrawledEnd;
				urlsSeenStart = urlsSeenEnd;
				urlsAddedStart = urlsAddedEnd;
				totalContentStart = totalContentEnd;

				Thread.sleep(Const.STATS_SLEEP_TIME);
				synchronized (StatsThread.mutex) {
					urlsCrawledEnd = StatsThread.crawledURLs;
					urlsSeenEnd = StatsThread.seenURLs;
					urlsAddedEnd = StatsThread.totalAdded;
					totalDropped = StatsThread.totalDropped;
					totalDuplicated = StatsThread.totalDuplicated;
					numberRobots = StatsThread.numberRobots;
					totalContentEnd = URLFetcher.totalContent;
					httpResponses = URLFetcher.httpResponses;
					mimeTypes = URLFetcher.mimeTypes;
				}

				urlsCrawledPS = urlsCrawledEnd - urlsCrawledStart;
				urlsCrawledPM += urlsCrawledPS;

				urlsSeenPS = urlsSeenEnd - urlsSeenStart;
				urlsSeenPM += urlsSeenPS;

				urlsAddedPS = urlsAddedEnd - urlsAddedStart;
				urlsAddedPM += urlsAddedPS;

				totalContentPS = totalContentEnd - totalContentStart;
				totalContentPM += totalContentPS;

				if (urlsAddedPS != 0) {
					queueRatio = (float) urlsSeenPS / (float) urlsAddedPS; // outgoing/incoming
					crawledPerAdded = (float) urlsCrawledPS / (float) urlsAddedPS; // crawled/incoming
				} 
				else 
				{
					queueRatio = -1;
					crawledPerAdded = -1;
				}

				secondDataBuffer = System.currentTimeMillis() + ","
						+ urlsCrawledPS + "," + urlsAddedPS + "," + urlsSeenPS
						+ "," + df.format(crawledPerAdded) + ","
						+ df.format(queueRatio) + "," + totalContentPS + ","
						+ totalDropped + "," + totalDuplicated;

				second_data.writeData(secondDataBuffer);

				PeerCrawlFrame.tarea3.append("CurrentTime: " + System.currentTimeMillis() + "\t");
				PeerCrawlFrame.tarea3.append("URLsCrawled/sec: " + urlsCrawledPS + "\t");
				PeerCrawlFrame.tarea3.append("URLsAdded/sec: " + urlsAddedPS + "\t");
				PeerCrawlFrame.tarea3.append("URLsRemoved/sec: " + urlsSeenPS + "\t");
				PeerCrawlFrame.tarea3.append("URLsCrawled/URLsAdded: " + df.format(crawledPerAdded) + "\t");
				PeerCrawlFrame.tarea3.append("QueueRatio: " + df.format(queueRatio) + "\t");
				PeerCrawlFrame.tarea3.append("TotalContent/sec: " + totalContentPS + "\t");
				PeerCrawlFrame.tarea3.append("TotalDropped: " + totalDropped + "\t");
				PeerCrawlFrame.tarea3.append("TotalDuplicated: " + totalDuplicated + "\n");

				counter++;

				if (counter % 60 == 0) 
				{
					if (urlsAddedPM != 0) 
					{
						queueGrowth = 100*((float) urlsAddedPM - (float) urlsSeenPM)/ urlsAddedPM;
						crawledPerAddedPM = (float) urlsCrawledPM/(float) urlsAddedPM;
					} 
					else 
					{
						deadCounter++;
						queueGrowth = -1;
						crawledPerAddedPM = -1;
					}
					
					if(deadCounter > Const.MAX_INACTIVE_MIN)
					{
						System.out.println("Crawler Inactive for " + 
								Const.MAX_INACTIVE_MIN + " minutes!!");
						System.exit(1);
					}

					synchronized (StatsThread.mutex) {
						totalCached = StatsThread.totalCached;
					}

					synchronized (FetchThread.crawlJobs) {
						sizeof_crawljob = FetchThread.crawlJobs.size();
					}

					synchronized (ProcessThread.fetchBuffer) {
						sizeof_fetchbuffer = ProcessThread.fetchBuffer.size();
					}

					minuteDataBuffer = System.currentTimeMillis() + ","
							+ urlsCrawledPM + "," + urlsAddedPM + ","
							+ urlsSeenPM + "," + StatsThread.totalCached + ","
							+ sizeof_crawljob + "," + sizeof_fetchbuffer + ","
							+ df.format(crawledPerAddedPM) + ","
							+ df.format(queueGrowth);

					minute_data.writeData(minuteDataBuffer);

					PeerCrawlFrame.tarea4.append("CurrentTime: " + System.currentTimeMillis() + "\t");
					PeerCrawlFrame.tarea4.append("URLsCrawled/min: " + urlsCrawledPM + "\t");
					PeerCrawlFrame.tarea4.append("URLsAdded/min: " + urlsAddedPM + "\t");
					PeerCrawlFrame.tarea4.append("URLsRemoved/min: " + urlsSeenPM + "\t");
					PeerCrawlFrame.tarea4.append("TotalCached: " + totalCached + "\t");
					PeerCrawlFrame.tarea4.append("CrawlQueueSize: " + sizeof_crawljob + "\t");
					PeerCrawlFrame.tarea4.append("FetchBufferSize: " + sizeof_fetchbuffer + "\t");
					PeerCrawlFrame.tarea4.append("URLsCrawled/URLsAdded: " + df.format(crawledPerAddedPM) + "\t");
					PeerCrawlFrame.tarea4.append("CrawlQueueGrowth: " + df.format(queueGrowth) + " \n");

					urlsCrawledPM = 0;
					urlsAddedPM = 0;
					urlsSeenPM = 0;
				}

				httpDataBuffer = System.currentTimeMillis() + ","
						+ totalContentEnd + "," + totalDropped + ","
						+ totalDuplicated + "," + numberRobots + ",";

				for (int i = 0; i < httpResponses.length; i++)
					httpDataBuffer += httpResponses[i] + ",";

				// Returned mime types
				for (int i = 0; i < mimeTypes.length; i++)
					httpDataBuffer += mimeTypes[i] + ",";

				http_data.writeData(httpDataBuffer);

			} catch (final Exception ex) { /* ex.printStackTrace(); */
				continue;
			}
		}
	} // End of thread
	
} // End of Class StatsThread
