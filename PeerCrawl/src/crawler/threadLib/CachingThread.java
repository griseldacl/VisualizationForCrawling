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

package crawler.threadLib;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;

import crawler.util.Document;
import crawler.util.PCHTMLDocument;
import crawler.util.PCPDFDocument;
import crawler.util.UrlWithDepth;


/**
 * Class CachingThread : Saves documents to disk after they have been processed
 * 							Uses the fetchBuffer data structure
 * @author Tushar
 */

public class CachingThread extends Thread 
{
	// Directory that gets created on the fly
	public static final String cachingDir = "cached";

	// No fo Caching threads (usually 2)
	public static int cachingThreadCount = 0;

	
	/**
	 * Constructor
	 */
	public CachingThread() 
	{
		this.setName("Caching Thread " + cachingThreadCount++);
		this.start();
	}

	
	/**
	 * Thread that does the caching
	 */
	public void run() 
	{
		System.out.println(this.getName() + " Started");
		
		File cachedDir = new File(CachingThread.cachingDir);
		
		if (!cachedDir.isDirectory())
			cachedDir.mkdir();
		
		while (true) 
		{
			String fileName = "";
			Document cachedFileDoc = null;
			try {
				Thread.sleep(100);

				// Extract a document that has been processed and is ready to be cached
				synchronized (ProcessThread.fetchBuffer)
				{
					for (int i = 0; i < ProcessThread.fetchBuffer.size(); i++) 
					{
						if (ProcessThread.fetchBuffer.get(i).isProcessed() &&
								ProcessThread.fetchBuffer.get(i).isToBeCached()) 
						{
							cachedFileDoc = ProcessThread.fetchBuffer.remove(i);
							break;
						}
					}
				}

				// If we get a document
				if (cachedFileDoc != null)
				{
					final UrlWithDepth url = cachedFileDoc.getUrl();
					
					String tempStr = url.getUrl().substring(url.getUrl().lastIndexOf("/") + 1);
					if (tempStr.indexOf(".") == -1)
						tempStr = "index.html";
										
					tempStr = CachingThread.validateFileName(tempStr);
					String dir = createDir(url);
					
					// If the file is html file
					if(cachedFileDoc instanceof PCHTMLDocument)
					{
						fileName = dir + "/" + tempStr;
						final File webFile = new File(fileName);
						if (webFile.createNewFile()) 
						{
							final BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(webFile.getPath()));
							bufferedWriter.write(((PCHTMLDocument)cachedFileDoc).getDataBuffer().toString());								
							bufferedWriter.close();
						}	
					}
					// If the file is pdf file
					else if (cachedFileDoc instanceof PCPDFDocument)
					{						
						fileName = dir + "/" + tempStr;
						final File webFile = new File(fileName);
						if (webFile.createNewFile()) 
						{
							FileOutputStream stream = new FileOutputStream(fileName);
							stream.write(((PCPDFDocument)cachedFileDoc).getByteBuffer());
							stream.close();
						}						
					}
									
					// Updating statistics
					synchronized (StatsThread.mutex) {
						StatsThread.totalCached++;
					}
				}
				
				cachedFileDoc = null;
			} catch (final Exception e) {
				System.out.println("Error with caching " + fileName);
			}
		}
	}
	
	
	/**
	 * Creating directory to structure the caching on disk
	 * @param url
	 * @return relative path of directories created
	 */
	private static String createDir(final UrlWithDepth url)
	{
		String urlString = url.getUrl();
		String domain = url.getDomain();
			
		urlString = urlString.substring(domain.lastIndexOf("/") + 1, 
											urlString.lastIndexOf("/"));
		
		String dir = cachingDir + "/" + urlString; 
		//System.out.println("dir : " + dir);
		File domainDir = new File(dir);
		
		if (!domainDir.isDirectory())
			domainDir.mkdirs();
		
		return dir;
	}

	
	/**
	 * Function to replace unwanted characters in filename by % for windows
	 * @param filename
	 * @return modified filename
	 */
	private static String validateFileName(final String filename) 
	{
		String tempStr = filename;
		tempStr = tempStr.replace('"', '%');
		tempStr = tempStr.replace('?', '%');
		tempStr = tempStr.replace('\\', '%');
		tempStr = tempStr.replace(':', '%');
		tempStr = tempStr.replace('*', '%');
		tempStr = tempStr.replace('|', '%');
		tempStr = tempStr.replace('<', '%');
		tempStr = tempStr.replace('>', '%');

		return tempStr;
	}
}
