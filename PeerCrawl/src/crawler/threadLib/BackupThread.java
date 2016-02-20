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
 */

package crawler.threadLib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import crawler.util.Const;


/**
 * Class BackupThread : Used to backup data structures
 * 
 * @author Tushar
 */
public class BackupThread extends Thread 
{
	// Directory where backup data is stored
	public static final String backupDir = "saved"; 
	
	/**
	 * Constructor
	 */
	public BackupThread() 
	{
		this.setName("Backup Thread");
		this.setPriority(Thread.NORM_PRIORITY - 1);
		this.start();
	}

	/**
	 * Thread to backup
	 */
	public void run() 
	{
		System.out.println(this.getName() + " Started");		
		File backupDir = new File(BackupThread.backupDir);
		
		if (!backupDir.isDirectory())
			backupDir.mkdir();
		
		while (true) {
			try {
				Thread.sleep(Const.BACKUP_TIME);

				// write the crawlJobs
				FileOutputStream out = new FileOutputStream(
						BackupThread.backupDir + "/" + Const.CRAWL_JOB_PATH);
				ObjectOutputStream s = new ObjectOutputStream(out);
				synchronized (FetchThread.crawlJobs) {
					s.writeObject(FetchThread.crawlJobs);
				}
				s.flush();
				out.close();

				
				// write fetchBuffer
				out = new FileOutputStream(BackupThread.backupDir + "/" + 
						Const.FETCH_BUFFER_PATH);
				s = new ObjectOutputStream(out);
				synchronized (ProcessThread.fetchBuffer) {
					s.writeObject(ProcessThread.fetchBuffer);
				}
				s.flush();
				out.close();
				
				
				// write the URLInfo
				out = new FileOutputStream(BackupThread.backupDir + "/" + 
						Const.URL_INFO_PATH);
				s = new ObjectOutputStream(out);
				synchronized (ProcessThread.URLInfo) {
					s.writeObject(ProcessThread.URLInfo);
				}
				s.flush();
				out.close();
				
			} catch (Exception e) {
				System.out.println("Exception while backing up data structures");
				e.printStackTrace();
			}
		} 
	} // End of Function
	
} // End of Class BackupThread
