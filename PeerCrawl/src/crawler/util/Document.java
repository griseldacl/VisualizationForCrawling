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
 * Class Document : Represents a document obtained from web server
 * @author Tushar
 */

public class Document 
{
	// URL of document
	private UrlWithDepth url;
	
	// If processed than can be cached
	private boolean processed;
	
	// If is not in domain then doesnt need to be cached 
	// Used in focused crawls
	private boolean toBeCached;
	
	
	/**
	 * Constructor
	 * @param url
	 */
	public Document(UrlWithDepth url)
	{
		this.url = url;
		this.processed = false;
	}

	/* Getters and Setters*/
	
	public boolean isProcessed() {
		return processed;
	}

	public UrlWithDepth getUrl() {
		return url;
	}


	public void setProcessed(boolean processed) {
		this.processed = processed;
	}

	public boolean isToBeCached() {
		return toBeCached;
	}

	public void setToBeCached(boolean toBeCached) {
		this.toBeCached = toBeCached;
	}
	
} // End of Class Document