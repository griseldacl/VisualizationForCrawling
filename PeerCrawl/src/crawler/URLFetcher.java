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


package crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Vector;

import crawler.threadLib.StatsThread;
import crawler.util.Const;
import crawler.util.Document;
import crawler.util.PCHTMLDocument;
import crawler.util.PCPDFDocument;
import crawler.util.UrlWithDepth;


/**
 * Class URLFetcher: Used to get url documents from web server
 * 						Uses Http Connection
 * 						Can be optimized by storing DNS mappings
 * 
 * @author Tushar
 * @contributor Suiyang
 */

public class URLFetcher 
{
	// Data structure for caching DNS Lookups
	public static HashMap<String,String> dnsLookup = new HashMap<String,String>();
	
	//	HTTP Responses
	public static int httpResponses[] = new int[10];
	 
	//	Returned mime types
	public static int mimeTypes[] = new int[9];
	
	// Size of total content
	public static int totalContent = 0;
	
	
	/**
	 * Function to get a document from web server
	 * 
	 * @param url to be fetched
	 * @return Document object corresponding to the url 
	 */
	public static Document fetchHttp(final UrlWithDepth url)
	{
		Document doc = null;
		HttpURLConnection uc = null;
		InputStream inputStream = null;
		String tempURL = "";
		
		//	HTTP Responses
		final int localHttpResponses[] = new int[10];
		 
		//	Returned mime types
		final int localMimeTypes[] = new int[9];
		int local_totalContent = 0;
		
		tempURL = url.getUrl();	
		
		try
		{	
			System.setProperty("sun.net.client.defaultConnectTimeout", 
					Integer.toString(Const.SOCKET_TIMEOUT) );
			System.setProperty("sun.net.client.defaultReadTimeout", 
					Integer.toString(Const.SOCKET_TIMEOUT) );
			
			// Setting HTTP Connection to URL
			uc = (HttpURLConnection)new URL(tempURL).openConnection();
			
			// Parsing response code
			switch(uc.getResponseCode()) 
			{
				case 200: localHttpResponses[0]++; break;
				case 404: localHttpResponses[1]++; break;
				case 302: localHttpResponses[2]++; break;
				case 301: localHttpResponses[3]++; break;
				case 403: localHttpResponses[4]++; break;
				case 401: localHttpResponses[5]++; break;
				case 500: localHttpResponses[6]++; break;
				case 406: localHttpResponses[7]++; break;
				case 400: localHttpResponses[8]++; break;
				default: localHttpResponses[9]++;
			}
			inputStream = uc.getInputStream();
		
			// If document is html
			if (uc.getContentType().toLowerCase().startsWith("text/html"))
			{
				localMimeTypes[0]++;
				if(uc.getContentLength() > 0)
					local_totalContent += uc.getContentLength();

				StringBuffer fileContent = URLFetcher.getFileContent(inputStream);
				

				if( fileContent != null)
				{
					fileContent.insert(0, "URL: " + "\"" + url.getUrl() + "\"" + "\n");
					doc = new PCHTMLDocument(url, fileContent);
					doc.setToBeCached( Peer.isInDomain(url) );
				}
				
				uc.disconnect();
				inputStream.close();
			}
	
			else if (uc.getContentType().toLowerCase().startsWith("image/gif")) localMimeTypes[1]++;
			else if (uc.getContentType().toLowerCase().startsWith("image/jpeg")) localMimeTypes[2]++;
			else if (uc.getContentType().toLowerCase().startsWith("text/plain")) localMimeTypes[3]++;
			else if (uc.getContentType().toLowerCase().startsWith("application/pdf"))
			{
				// if document is pdf
				
				localMimeTypes[4]++;
				if(uc.getContentLength() > 0)
					local_totalContent += uc.getContentLength();

				final byte[] fileContent = URLFetcher.getFileContentInByte(inputStream, local_totalContent);
				uc.disconnect();
				inputStream.close();

				if( fileContent != null)
				{
					doc = new PCPDFDocument(url, fileContent);
					doc.setToBeCached( Peer.isInDomain(url) );
				}	
			}
			else if (uc.getContentType().toLowerCase().startsWith("audio/x-pn-realaudio")) localMimeTypes[5]++;
			else if (uc.getContentType().toLowerCase().startsWith("application/zip")) localMimeTypes[6]++;
			else if (uc.getContentType().toLowerCase().startsWith("application/postscript")) localMimeTypes[7]++;
			else localMimeTypes[8]++;	
			inputStream.close();	
		}
		catch(final Exception e)
		{
			System.out.println("Error in fetching url : " + tempURL);
			uc.disconnect();
		}
		
		// Update the statistics
		synchronized(StatsThread.mutex)
		{
			URLFetcher.totalContent += local_totalContent;

			// HTTP Responses
			for(int j=0; j < URLFetcher.httpResponses.length; j++)
				URLFetcher.httpResponses[j] += localHttpResponses[j];

			// Returned mime types
			for(int j=0; j < URLFetcher.mimeTypes.length; j++)
				URLFetcher.mimeTypes[j] += localMimeTypes[j];
		}
		
		return doc;
	} // End of Function fetchHttp()
	
	
	/**
	 * Function to get file content for html pages
	 * 
	 * @param inputstream
	 * @return data from the stream
	 */
	private static StringBuffer getFileContent(final InputStream inputStream)
	{
		StringBuffer fileContent = null;
		try 
		{
			final BufferedReader bufreader = new BufferedReader(
					new InputStreamReader(inputStream));
			
			int cnt = 0;
			boolean getout = true;
			String line = "";

			fileContent = new StringBuffer();
			while ( (line = bufreader.readLine()) != null ) 
			{
				fileContent.append(line);
				if ( getout && (cnt <= Const.MAX_NON_HTML_LINES) ) 
				{
					if (line.toLowerCase().indexOf("<html")!=-1) 
						getout = false; 	
				} 
				else 
				{
					if (getout) 
						return null;
				}
				cnt++;
			}
		} catch (final IOException e) { 
			System.out.println("\nSocket Timeout!"); 
		}

		return fileContent;
	} // End of Function getFileContent()
	
	
	/**
	 * Function to get file content for pdf pages
	 * 
	 * @param inputstream
	 * @param length of file
	 * @return data from the stream
	 */
	private static byte[] getFileContentInByte(final InputStream inputStream, int length)
	{
		byte[] document =   new byte[0];
		Vector<Byte> temp = new Vector();
		try{
			int current;
			int count = 1;
			
			while(( current = ( inputStream.read() ) ) != -1)
			{
				temp.add(new Byte((byte)current));
				count++;
			}
			
			document = new byte[temp.size()];
			for(int i = 0; i < temp.size(); i++)
			{
				document[i] = ((Byte)temp.get(i)).byteValue();
			}
		}catch (final IOException e) { 
			System.out.println("\nPDF file Socket Timeout!");
		}
		
		return document;
	} // End of Function getFileContentInByte()
	
	
	/**
	 * Function to get IP address from local dns mapping cache
	 * 
	 * @param url
	 * @return IP Address
	 */
	public static String getIPAddr(final UrlWithDepth url)
	{
		String domain = url.getDomain();
		String subdomains = url.getUrl().substring(domain.length() + 1 );
		
		String IPAddr = domain;
		if( !URLFetcher.dnsLookup.containsKey(domain) )
		{	
			InetAddress inetAddr = null;
			try {
				inetAddr = InetAddress.getByName(domain);
			} catch (final UnknownHostException e) {
				e.printStackTrace();
			}
			if( inetAddr != null)
			{
				IPAddr = inetAddr.getHostAddress();
				URLFetcher.dnsLookup.put(domain,IPAddr);
			}
		}
		else
			IPAddr = URLFetcher.dnsLookup.get(domain);
		
		return ("http://" + IPAddr + subdomains);
	} // End of Function getIPAddr()
	
} // End of Class URLFetcher
