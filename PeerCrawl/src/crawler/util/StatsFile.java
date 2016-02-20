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

import java.io.*;

import phex.common.Environment;
import phex.utils.FileUtils;


/**
 * Class StatsFile : Used to maintain crawler statistics on secondary storage
 * 
 * @author Vaibhav
 */

public class StatsFile 
{	
	private File dataFile;
	private FileWriter fw;
	private PrintWriter pw;
	
	/**
	 * Constructor
	 * 
	 * @param fileName
	 * @throws Exception
	 */
	public StatsFile(String fileName) throws Exception
	{
		dataFile = getStatisticsFile(fileName);
		fw=new FileWriter(dataFile);
		pw=new PrintWriter(fw,true);
	}
	
	
	/**
	 * Getter for dataFile
	 * @return
	 */
	public File getDataFile(){
		return dataFile;
	}
	
	
	/**
	 * Function to get statistics file
	 * 
	 * @param fileName
	 * @return
	 */
	private File getStatisticsFile(String fileName) 
	{
		fileName = FileUtils.convertToLocalSystemFilename(fileName);
		return Environment.getInstance().getPhexConfigFile( fileName );
	}

	
	/**
	 * Function to write data onto a file
	 * @param c
	 */
	public void writeData(String c) 
	{
		try {
			pw.println(c);
		} catch (Exception e) { e.printStackTrace(); }
	}

	
	/**
	 * Function to refresh contents of file
	 * 
	 * @param fileName
	 */
	public void refresh(String fileName) 
	{
		try 
		{
			pw.close();
			fw.close();
			fw=new FileWriter(dataFile,false);
			pw=new PrintWriter(fw,true);
		} 
		catch (Exception e) { e.printStackTrace(); }
	}
	
	
	/**
	 * Override the finalize method
	 */
	protected void finalize() throws Exception
	{
		pw.close();
		fw.close();
		dataFile.delete();
	}
	
} // End of Class StatsFile
