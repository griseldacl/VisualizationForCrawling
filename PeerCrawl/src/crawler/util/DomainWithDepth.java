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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Class DomainWithDepth : Used to store robots.txt excluded domains
 * 
 * @author Suiyang
 * @contributor Tushar
 */

public class DomainWithDepth extends UrlWithDepth
{
	// Different agents(crawlers) that a domain can disallow specific subdomains 
	private LinkedList<String> userAgents;
	
	// domains that it disallows
	private Map<String,List<String>> disallows;
	
	
	/**
	 * Constructor
	 * 
	 * @param url
	 * @param depth
	 */
	public DomainWithDepth(String url, int depth)
	{
		super( url, depth );	
		userAgents = new LinkedList<String>();
		disallows = new HashMap<String, List<String>>();
		initRobotTextInfo();
	}
	
	
	/**
	 * Function to check for robots.txt file
	 */
	private void initRobotTextInfo()
	{
		BufferedReader reader = null;
		InputStream inputStream = null;
		HttpURLConnection uc = null;
		try{
			String tempURL = ( getUrl().endsWith("/") )? getUrl() + "robots.txt"
														: getUrl() + "/robots.txt";
			URL robotURL = new URL(tempURL);
			System.setProperty("sun.net.client.defaultConnectTimeout", ""+Const.SOCKET_TIMEOUT);
			System.setProperty("sun.net.client.defaultReadTimeout", ""+Const.SOCKET_TIMEOUT);
			uc = (HttpURLConnection)robotURL.openConnection();
			inputStream = uc.getInputStream();
			reader = new BufferedReader(new InputStreamReader(inputStream));
			
			if (RobotsExclusion.parseRobotsTxt(reader, userAgents, disallows) )
				System.out.println("Error in Parsing Robots file");
			else
				System.out.println("Intialized Robots Text");
			
		}catch(Exception e){
			System.out.println("No Robots text exists for Domain: " + 
					getUrl() + " or url can not be opened");
		}
	} // End of Function initRobotTextInfo()
	
	
	/* Getter and setter */
	
	public LinkedList getUserAgents(){
		return userAgents;
	}
	
	public Map getDisallows(){
		return disallows;
	}
	
} // End of Class DomainWithDepth
