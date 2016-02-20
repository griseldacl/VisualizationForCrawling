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

import java.security.*;
import java.math.BigInteger;


/**
 * Class VirtualID : Creates and contains a MD5 id for peers.
 * 
 * @author Vaibhav
 */

public class VirtualID 
{
	//Byte Array containing the vid value
	private byte[] vid;   
	private BigInteger lowerBound;
	private BigInteger upperBound;

	/**
	 * Constructors
	 */
	public VirtualID() throws Exception 
	{
		vid = new byte[Const.m / 8];
	}
	
	/**
	 * Constructor for urls 
	 */
	public VirtualID(String s) throws Exception
	{
		vid = new byte[Const.m / 8];
		MessageDigest md = MessageDigest.getInstance("MD5");
		vid = md.digest(s.getBytes());		 
		 
		lowerBound = BigInteger.ZERO;
		upperBound = BigInteger.ZERO;
	}
	 
	/**
	 * Constructor for node 
	 */
	public VirtualID(String s, int offset) throws Exception
	{
		vid = new byte[Const.m / 8];
		MessageDigest md = MessageDigest.getInstance("MD5");
		vid = md.digest(s.getBytes());		
		ComputeCrawlRange(offset);
	}
	 
	
	/**
	 * Function to compute the crawl range for the peer
	 * 
	 * @param offset  
	 */
	public void ComputeCrawlRange(int offset)
	{
		// Compute b=(2^(128-offset)) 
		BigInteger b = BigInteger.valueOf(2);
		b = b.pow(128-offset);
			
		// Compute (2^(128-offset) -1)
		BigInteger maxvalue = BigInteger.valueOf(2);
		maxvalue = maxvalue.pow(Const.m);
		maxvalue = maxvalue.subtract(BigInteger.ONE);
			
		String shost = this.toString();
		BigInteger bhost = new BigInteger(shost,16);
	
		// Compute upper bound: hash(hostip)+b 
		upperBound = bhost.add(b);
		upperBound = upperBound.min(maxvalue);
		
		// Compute lower bound: hash(hostip)-b
		lowerBound = bhost.subtract(b);
		lowerBound = lowerBound.max(BigInteger.ZERO);
	} // End of Function ComputeCrawlRange()
	
	
	/**
	 * Overiding function to convert Virtual ID to a String
	 * 
	 * @return String value
	 */
	public String toString() 
	{
		String s = new String();
		for(int i = 0; i < Const.m / 8; i++)
		{
			int num = (new Byte(vid[i])).hashCode() & 0xff;
			if(num < 0x10) s += new String("0");
			s += Integer.toHexString(num);
		}
		return s;
	} // End of Function toString()
	 
	 
	/**
	 * Function that checks if this host is responsible for crawling  
	 * the passed url based on the url domain (urlid).
	 * 
	 * @param urlid
	 * @return true if url should be crawled by the peer
	 */
	 public boolean shouldCrawl(VirtualID urlid)
	 {
		 String surl = urlid.toString();
		 BigInteger burl = new BigInteger(surl,16);

		 if(lowerBound.compareTo(burl) < 0 && upperBound.compareTo(burl) > 0)
			 return true;

		 return false;
	 } // End of Function shouldCrawl()
	 

	 /**
	  * Function to get Virtual ID as bytes
	  * 
	  * @return Virtual ID as bytes
	  */
	 public byte[] getVID() 
	 {
		 return vid;
	 }

	 /**
	  * Function to Lower Bound
	  * 
	  * @return Lower Bound
	  */
	 public BigInteger getLowerBound() 
	 {
		 return lowerBound;
	 }

	 
	 /**
	  * Function to Upper Bound
	  * 
	  * @return Upper Bound
	  */
	 public BigInteger getUpperBound() 
	 {
		 return upperBound;
	 }
	 
} // End of Class Virtual ID
