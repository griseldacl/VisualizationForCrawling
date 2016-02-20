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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Class RobotsExclusion : Used to check for robots.txt file that excludes
 * 							some subdomains a crawler can scan. 
 * 
 * @author Suiyang
 * @contributor Tushar
 */

public class RobotsExclusion 
{	
	// Data structure which keeps a mapping from the Domain string to 
	// the DomainWithDepth Class containing the excluded urls.
	public static Map<String,DomainWithDepth> Robots;
	
	
	/**
	 * Function that parses the robots.txt file and stores the excluded subdomains 
	 * 
	 * @param reader
	 * @param userAgents
	 * @param disallows
	 * @return true if the parse didnt throw errors
	 * @throws IOException
	 */
	public static boolean parseRobotsTxt( BufferedReader reader, LinkedList<String> userAgents, 
			Map<String,List<String>> disallows ) throws IOException 
	{
        boolean hasErrors = false;
        String read;
        List<String> current = null;
        String catchAll = null;
        while (reader != null) 
        {
            do 
            {
                read = reader.readLine();
                // Skip comments & blanks
            } 
            while ((read != null) && ((read = read.trim()).startsWith("#") ||
                read.length() == 0));
            
            if (read == null) 
            {
                reader.close();
                break;
            }
            else 
            {
                int commentIndex = read.indexOf("#");
                if (commentIndex > -1) {
                    // Strip trailing comment
                    read = read.substring(0, commentIndex);
                }
                read = read.trim();
                if (read.matches("(?i)^User-agent:.*")) 
                {
                    String ua = read.substring(11).trim().toLowerCase();
                    if (current == null || current.size() != 0) 
                    {
                        // only create new rules-list if necessary
                        // otherwise share with previous user-agent
                        current = new ArrayList<String>();
                    }
                    
                    if (ua.equals("*")) 
                    {
                        ua = "";
                        catchAll = ua;
                    } 
                    else 
                        userAgents.addLast(ua);
                    
                    disallows.put(ua, current);
                    continue;
                }
                
                if (read.matches("(?i)Disallow:.*")) 
                {
                    if (current == null) 
                    {
                        // buggy robots.txt
                        hasErrors = true;
                        continue;
                    }
                    
                    String path = read.substring(9).trim();
                    current.add(path);
                    continue;
                }
                // unknown line; do nothing for now
            }
        }

        if (catchAll != null) 
            userAgents.addLast(catchAll);
        
        return hasErrors;
    } // End of Function parseRobotsTxt()
	
	
	/**
	 * Check of the url is excluded by robots.txt for that domain
	 * 
	 * @param url
	 * @return true if excluded, false if allowed
	 */
    public static boolean disallows(UrlWithDepth url)
    {
        // In the common case with policy=Classic, the useragent is remembered from uri 
    	// to uri on the same server
    	
    	String domainString = url.getDomain();
    	String restString;
    	
    	if(domainString.length() < url.getUrl().length())
    	{
    		restString = url.getUrl().substring(domainString.length(), 
    												url.getUrl().length() - 1); 
    	}
    	else
    		return false;
    	
    	DomainWithDepth tempDomain = RobotsExclusion.Robots.get(domainString);
    	if(tempDomain == null)
    	{
    		tempDomain = new DomainWithDepth(domainString, url.getDepth());
    		RobotsExclusion.Robots.put(domainString, tempDomain);	
    	}
    	
    	LinkedList userAgents = tempDomain.getUserAgents();
    	Map disallows = tempDomain.getDisallows();

        boolean examined = false;
        String ua = null;

        // Go thru list of all user agents we might act as
        
        for (Iterator uas = userAgents.iterator(); uas.hasNext(); ) 
        {
            ua = (String) uas.next();
            Iterator dis = ((List) disallows.get(ua)).iterator();

            // Check if the current user agent is allowed to crawl
            while(dis.hasNext() && examined == false) 
            {
                String disallowedPath = (String) dis.next();
                if(disallowedPath.length() == 0)
                {
                    // blanket allow
                    examined = true;
                    break;
                }

                String p = restString;
                if (p != null && p.startsWith(disallowedPath) )
                {
                    // the user agent tested isn't allowed to get this uri
                    return true;
                }
            }
        }

        // Are we supposed to masquerade as the user agent to which restrictions
        return false;        
    } // End of Function disallows()
    
} // End of class RobotsExclusion
