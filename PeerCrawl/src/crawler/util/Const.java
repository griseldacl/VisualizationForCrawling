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

package crawler.util;

/**
 * Class Const : Contains crawler configuration parameters
 * 
 * @author Vaibhav
 * @contributor Tushar
 */

public class Const
{
	// General or focused crawl
	public static boolean focusedCrawl;
	
	// Crawl at root node starts from this URL
	public static String currentURL;
	
    // The default number of fetch and process threads to be started
	public final static int MAX_THREADS = 10; 
	
	// Max crawl threads that a system can handle
	public final static int MAX_CRAWL_THREADS = 25;
	
	// The length of hash (in bits)
	public final static int m = 128;                       
	
	// Number of junk lines allowed before <html tag appears in HTTP Response
    public final static int MAX_NON_HTML_LINES=50;                  
    
    // Depth
    public final static int MAX_DEPTH = 25;  
	
    // Bloom Filter Capacity
	public final static int BLOOM_FILTER_SIZE = 4096 * 8;
	
	// In root node
	public static boolean initialNode = false;
	
	// If caching on secondary storage is enabled
	public static boolean cacheDocs = true;
	
	// If PDF type documents need to be crawled
	public static boolean crawlPDF = false;
	
	// LOG files
	public final static String NETWORK_STATS_FILE= "network.csv";
	public final static String CRAWL_URLS_STATS_FILE= "urls.csv";
	public final static String PerSecond_STATS_FILE= "second_data.csv";
	public final static String PerMinute_STATS_FILE= "minute_data.csv";
	public final static String HTTP_STATS_FILE= "http_data.csv";
	
	
	// Bounds on various queues
	public final static int CRAWL_MAXJOB_LIMIT = 10000;
	public final static int DISPATCH_THRESHOLD = 20;

	// Filename for backing up statistics
	public final static String CRAWL_JOB_PATH = "crawlJobsSaved";
	public final static String FETCH_BUFFER_PATH = "fetchBufferSaved";
	public final static String URL_INFO_PATH = "URLInfoSaved";
	public final static String ROBOTS_PATH = "robotsSaved";
	
	
    // Timeout for getting pages
    public final static int SOCKET_TIMEOUT = 4*1000;  
    
    // Time to Backup data structures
	public final static int BACKUP_TIME = 1000*60*5;	
	
	// Time to sleep between two runs of statistics collection
	public final static int STATS_SLEEP_TIME = 1000;
	
	// Time for checking peers
	public final static int NETCONN_SLEEP_TIME = 5000;
	
	// Maximum number of hops allowed for entering back into domain
	public static int MAX_HOP_COUNT = 0;
	
	// Maximum number of inactive minutes before we kill the crawler 
	public static int MAX_INACTIVE_MIN = 10;
	
} // End of Class Const