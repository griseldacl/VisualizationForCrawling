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
 * Class PCHTMLDocument : Document of type HTML
 * 
 * @author Suiyang
 */

public class PCHTMLDocument extends Document implements Serializable
{
	// Data of HTML file
	private StringBuffer dataBuffer;
	
	
	/**
	 * Constructor
	 * @param url
	 * @param buffer
	 */
	public PCHTMLDocument(UrlWithDepth url, StringBuffer buffer) 
	{
		super( url);
		this.dataBuffer = buffer;
	}
	
    /**
     * Getter
     * @return
     */
	public StringBuffer getDataBuffer() {
		return dataBuffer;
	}
	
} // End of Class PCHTMLDocument