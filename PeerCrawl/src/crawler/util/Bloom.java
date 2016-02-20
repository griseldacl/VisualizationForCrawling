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
import java.security.MessageDigest;
import java.util.BitSet;

/**
 *  Class Bloom : Maintains Bloom filters for duplication prevention
 *  
 *  @author Vaibhav
 */

public class Bloom implements Serializable
{
	// Bitset that keeps value of bloom filter
	BitSet bitSet = null;
	
	// Bloom filter size
	int size = Const.BLOOM_FILTER_SIZE;

	// Hash functions used
	String[] hashFunctions = {"MD5", "SHA-1"};

 	
 	/**
 	 * Constructor
 	 */
	public Bloom()
	{
		bitSet = new BitSet(size);
	}

	/**
	 * Function that tests the filtering of key
	 * @param key
	 * @return
	 */
	public boolean test(String key)
	{
		for(int i = 0; i < hashFunctions.length; i++)
		{
			int loc = hash(key, hashFunctions[i]);
			if(!bitSet.get(loc)) return false;
		}
		return true;
	}

	/**
	 * Overrides native method
	 */
	public String toString ()
	{
		return "Size = " + size + "\n" + bitSet;
	}
	
	
	/**
	 * Function to add key to filter 
	 * @param key
	 */
	public void set(String key)
	{
		for(int i = 0; i < hashFunctions.length; i++)
		{
			int loc = hash(key, hashFunctions[i]);
			bitSet.set(loc);
		}
	}

	
	/**
	 * Function that calculates the hash of key using hashFunction
	 * @param key
	 * @param hashFunction
	 * @return Hash value
	 */
	public int hash(String key, String hashFunction) 
	{	  
		byte[] sha = null;
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance(hashFunction);
		} catch (Exception e) { System.out.println("Unknown hash function " + hashFunction); }

		sha = md.digest(key.getBytes());

		String bits = new String("");
		for(int i = 0; i < sha.length; i++)
		{
			int b = (new Byte(sha[i])).intValue() & 0xff;
			String binStr = Integer.toBinaryString(b);
			byte[] reqdStr = new byte[8];
			for(int j = 0; j < binStr.length(); j++)
				reqdStr[8 - binStr.length() + j] = (byte) binStr.charAt(j);
			for(int k = 0; k < 8 - binStr.length(); k++)
				reqdStr[k] = (byte) '0';
			bits = bits + (new String (reqdStr));
		}

		int reqdSize = Integer.toBinaryString(Const.BLOOM_FILTER_SIZE).length() - 1;
		String hashStr = bits.substring(0, reqdSize);
		return Integer.parseInt(hashStr, 2);
	}

	
	/**
	 * Function to test functionality of the filter
	 * @param args
	 */
	public static void test(String[] args) 
	{
		Bloom b = new Bloom();
		for(int i = 0; i < 10; i++)
			b.set(i + "13adfef");

		for(int i = 5; i < 15; i++)
			System.out.println(b.test(i + "13adfef"));
	}
	
} // End of Class Bloom
