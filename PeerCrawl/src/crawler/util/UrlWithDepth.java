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

import java.io.Serializable;


/**
 * Class UrlWithDepth : Stores additional information associated with a URL
 * 
 * @author Suiyang
 * @contributor Tushar
 */

public class UrlWithDepth implements Serializable
{
	// URL
	private String url;
	
	// Depth from seed
	private int depth;
	
	// Domain to which URL belongs
	private String domain;
	
	// If focused crawl, then no of hops outside the crawl domain
	private int hopCount;
	
	
	/**
	 * Constructor
	 * @param url
	 * @param depth
	 * @param hopCount
	 */
	public UrlWithDepth(String url, int depth, int hopCount)
	{
		this.depth = depth;
		this.url = url;
		this.hopCount = hopCount;
		
		int index = url.indexOf("/", 8);
		this.domain = (index > 0)? url.substring(0, index): url;
	}
	
	
	/**
	 * Contructor 2
	 * 
	 * @param url
	 * @param depth
	 */
	public UrlWithDepth(String url, int depth)
	{
		this(url, depth, 0);
	}

	
	/* Getters and setters */
	
	public String getDomain() {
		return domain;
	}

	public String getUrl() {
		return url;
	}

	public int getDepth() {
		return depth;
	}

	public int getHopCount() {
		return hopCount;
	}

	public void setHopCount(int hopCount) {
		this.hopCount = hopCount;
	}		
	
} // End of Class UrlWithDepth
