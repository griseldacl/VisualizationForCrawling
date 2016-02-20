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


import javax.swing.*;
import java.awt.*;


/**
 * Class PeerCrawlFrame: GUI used to display crawler details during runtime
 * 
 * @author Tushar
 */
public class PeerCrawlFrame 
{
	// GUI Elements
    private JScrollPane jsp1,jsp2,jsp3,jsp4,jsp5;
    private JPanel tab1,tab2,tab3,tab4,tab5;
    private JTabbedPane tpane;
    public static JTextArea tarea1,tarea2,tarea3,tarea4,tarea5;
    JFrame peerCrawlFrame = new JFrame("Domain Specific Peer Crawler Statistics");
	
    
    /**
     * Constructor
     */
	public PeerCrawlFrame()
	{
		tpane = new JTabbedPane();
	  	peerCrawlFrame.setContentPane(tpane);
	  	
	  	CreateTab1();
	  	CreateTab2();
	  	CreateTab3();
	  	CreateTab4();
	  	CreateTab5();
	  	
	  	peerCrawlFrame.setBounds(200,200,500,500);
	  	peerCrawlFrame.setVisible(true);
	  	
	  	peerCrawlFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	  	
	  	Peer.getInstance();
	}
	
	
	/**
	 * Function to create "Network" Tab.
	 * Displays URLs of peers currently in the network
	 */
	private void CreateTab1()
	{
		tab1 = new JPanel(new BorderLayout());
		tpane.addTab("Network" , tab1);
		tarea1 = new JTextArea(80,100);
	    tarea1.setBackground(Color.white);
	    tarea1.setWrapStyleWord(false);
	    tab1.add(tarea1);
	    jsp1 = new JScrollPane(tarea1);
	    jsp1.getHorizontalScrollBar().setBackground(Color.lightGray);
	    jsp1.getVerticalScrollBar().setBackground(Color.lightGray);
	    tab1.add(jsp1);
	} // End of Function CreateTab1()
	
	
	/**
	 * Function to create "Crawling" Tab.
	 * Displays URLs crawled by the crawler.
	 */
	private void CreateTab2()
	{
		tab2 = new JPanel(new BorderLayout());
		tpane.addTab("Crawling" , tab2);
		tarea2 = new JTextArea(80,100);
		tarea2.setBackground(Color.white);
	    tarea2.setWrapStyleWord(false);
	    tarea2.setEditable(true);
        tab2.add(tarea2);
        jsp2 = new JScrollPane(tarea2);
        jsp2.getHorizontalScrollBar().setBackground(Color.lightGray);
        jsp2.getVerticalScrollBar().setBackground(Color.lightGray);
        tab2.add(jsp2);
	} // End of Function CreateTab2()
	
	
	/**
	 * Function to create "Per Second Data" Tab.
	 * Displays per second statistics of crawler.
	 */
	private void CreateTab3()
	{
		tab3 = new JPanel(new BorderLayout());
		tpane.addTab("Per Second Data" , tab3);
		tarea3 = new JTextArea(80,100);
		tarea3.setBackground(Color.white);
	    tarea3.setWrapStyleWord(false);
        tab3.add(tarea3);
        jsp3 = new JScrollPane(tarea3);
        jsp3.getHorizontalScrollBar().setBackground(Color.lightGray);
        jsp3.getVerticalScrollBar().setBackground(Color.lightGray);
        tab3.add(jsp3);
	} // End of Function CreateTab3()
	
	
	/**
	 * Function to create "Per Minute Data" Tab.
	 * Displays per minute statistics of crawler.
	 */
	private void CreateTab4()
	{
		tab4 = new JPanel(new BorderLayout());
		tpane.addTab("Per Minute Data" , tab4);
		tarea4 = new JTextArea(80,100);
		tarea4.setBackground(Color.white);
	    tarea4.setWrapStyleWord(false);
        tab4.add(tarea4);
        jsp4 = new JScrollPane(tarea4);
        jsp4.getHorizontalScrollBar().setBackground(Color.lightGray);
        jsp4.getVerticalScrollBar().setBackground(Color.lightGray);
        tab4.add(jsp4);
	} // End of Function CreateTab4()
	
	
	/**
	 * Function to create "URL passing" Tab.
	 * Displays urls broadcasted to and caught from peers of a crawler.
	 */
	private void CreateTab5()
	{
		tab5 = new JPanel(new BorderLayout());
		tpane.addTab("URL passing" , tab5);
		tarea5 = new JTextArea(80,100);
		tarea5.setBackground(Color.white);
	    tarea5.setWrapStyleWord(false);
        tab5.add(tarea5);
        jsp5 = new JScrollPane(tarea5);
        jsp5.getHorizontalScrollBar().setBackground(Color.lightGray);
        jsp5.getVerticalScrollBar().setBackground(Color.lightGray);
        tab5.add(jsp5);
	} // End of Function CreateTab5()
	
	
} // End of Class PeerCrawlFrame 