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
 * Class PCPDFDocument : Document of type PDF
 * 
 * @author Suiyang
 */
public class PCPDFDocument extends Document  implements Serializable
{
	// Data of PDF file
	private byte[] byteBuffer ;
	
	
	/**
	 * Constructor
	 * @param url
	 * @param inBuffer
	 */
	public PCPDFDocument(UrlWithDepth url, byte[] inBuffer) 
	{
		super( url);
		byteBuffer = inBuffer;	
	}
	
	/**
	 * Getter
	 * @return
	 */
	public byte[] getByteBuffer() {
		return byteBuffer;
	}
	
} // End of Class PCPDFDocument