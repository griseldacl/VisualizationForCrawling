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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.commons.lang.SystemUtils;

import crawler.util.Const;
import phex.common.Environment;
import phex.utils.*;

/** 
 * Main Class: Program Execution begins here. 
 * 				Throws up GUI to take input parameters
 * 
 * @author Vaibhav
 * @contributor Tushar
 */
public class PeerCrawl implements ActionListener, ItemListener
{
	// GUI Elements
	private JButton start = new JButton("Start Crawler");
    private JCheckBox rootNode = new JCheckBox("Root Node", true);
    private JCheckBox focusedCrawl = new JCheckBox("Focused Crawl",true);
    private JCheckBox caching = new JCheckBox("Cache Documents", true);
    private JCheckBox crawlPdf = new JCheckBox("Crawl Pdf", false);
    private JTextArea seedList = new JTextArea("www.gatech.edu",5,15);
    private JTextField hopCount = new JTextField("0",20);
    private JTextField ipAddr = new JTextField(10);
    JFrame inputFrame = new JFrame("Input Parameters");
    
    /**
     * Constructor
     */
	public PeerCrawl()
	{
		// Initialize bounds of GUI on screen
		inputFrame.setBounds(200,200,450,300);
		
		// Add listeners to GUI elements
		start.addActionListener(this);
		rootNode.addItemListener(this);
		ipAddr.setEditable(false);
		focusedCrawl.addItemListener(this);
		
		// GUI Layout
		GridBagLayout g1 = new GridBagLayout();
		GridBagConstraints c1 = new GridBagConstraints(); 
		JPanel panel = new JPanel();
		panel.setLayout(g1);

		buildConstraints(c1,0,0,1,1,30,15);
		c1.fill=GridBagConstraints.NONE;
		c1.anchor=GridBagConstraints.WEST;
		g1.setConstraints(rootNode,c1);
		panel.add(rootNode);
		
		JLabel ipAddrL = new JLabel("Root IP ",JLabel.LEFT);
		buildConstraints(c1,1,0,1,1,20,0);
		c1.fill=GridBagConstraints.NONE;
		c1.anchor=GridBagConstraints.EAST;
		g1.setConstraints(ipAddrL,c1);
		panel.add(ipAddrL);
		
		buildConstraints(c1,2,0,1,1,50,0);
		c1.fill=GridBagConstraints.HORIZONTAL;
		c1.anchor=GridBagConstraints.WEST;
		g1.setConstraints(ipAddr,c1);
		panel.add(ipAddr);
		
		c1.fill=GridBagConstraints.NONE;
		c1.anchor=GridBagConstraints.WEST;
		buildConstraints(c1,0,1,1,1,50,15);
		g1.setConstraints(focusedCrawl,c1);
		panel.add(focusedCrawl);
		
		c1.fill=GridBagConstraints.NONE;
		c1.anchor=GridBagConstraints.CENTER;
		buildConstraints(c1,1,1,1,1,0,0);
		g1.setConstraints(caching,c1);
		panel.add(caching);
		
		c1.fill=GridBagConstraints.NONE;
		c1.anchor=GridBagConstraints.CENTER;
		buildConstraints(c1,2,1,1,1,0,0);
		g1.setConstraints(crawlPdf,c1);
		panel.add(crawlPdf);
		
		JLabel seedListL = new JLabel("Seed List ",JLabel.LEFT);
		buildConstraints(c1,0,2,1,1,0,40);
		c1.fill=GridBagConstraints.NONE;
		c1.anchor=GridBagConstraints.CENTER;
		g1.setConstraints(seedListL,c1);
		panel.add(seedListL);
		
		c1.fill=GridBagConstraints.BOTH;
		c1.anchor=GridBagConstraints.CENTER;
		buildConstraints(c1,1,2,1,1,0,0);
		g1.setConstraints(seedList,c1);
		panel.add(seedList);
		
		JLabel hopCountL = new JLabel("Hop Count",JLabel.LEFT);
		buildConstraints(c1,0,3,1,1,0,15);
		c1.fill=GridBagConstraints.NONE;
		c1.anchor=GridBagConstraints.CENTER;
		g1.setConstraints(hopCountL,c1);
		panel.add(hopCountL);
		
		c1.fill=GridBagConstraints.HORIZONTAL;
		c1.anchor=GridBagConstraints.CENTER;
		buildConstraints(c1,1,3,1,1,0,0);
		g1.setConstraints(hopCount,c1);
		panel.add(hopCount);
		
		c1.fill=GridBagConstraints.NONE;
		c1.anchor=GridBagConstraints.CENTER;
		buildConstraints(c1,0,4,3,1,0,0);
		g1.setConstraints(start,c1);
		panel.add(start);	
		
		inputFrame.setContentPane(panel);
		inputFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		inputFrame.setVisible(true);
	} // End of Constructor
	
	
	/**
	 * Function for setting up GUI parameters
	 */
	void buildConstraints(GridBagConstraints gbc,int gx,int gy,int gw,int gh,int wx,int wy)
	{
		gbc.gridx=gx;
		gbc.gridy=gy;
		gbc.gridwidth=gw;
		gbc.gridheight=gh;
		gbc.weightx=wx;
		gbc.weighty=wy;
	}
	
	/**
	 * (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) 
	{
		if(e.getSource() == start)
		{
			Const.initialNode = rootNode.isSelected();
			Const.focusedCrawl = focusedCrawl.isSelected();
			Const.cacheDocs = caching.isSelected();
			Const.crawlPDF = crawlPdf.isSelected();
			
			int hopCountInt = Integer.parseInt(hopCount.getText()); 
			Const.MAX_HOP_COUNT = hopCountInt;
			
			String seedUrl = seedList.getText();
			if( Const.initialNode == true && (seedUrl == null || seedUrl.equals("")) )
				System.exit(1);
			
			File statsDir = new File(Environment.statsDir);
			if( !statsDir.isDirectory())
				statsDir.mkdirs();
			
			String IPAddress;
			File hostFile = new File(Environment.statsDir + "/PeerCrawl.hosts");
			RandomAccessFile raf = null;
			
			try {
				raf = new RandomAccessFile(hostFile,"rw");
			} catch (FileNotFoundException e2) {}
		
			if(!Const.initialNode)
			{
				IPAddress = ipAddr.getText();
				if(IPAddress.equals(""))
					System.exit(1);
				
				try {
					raf.seek(0);
					raf.writeBytes(IPAddress + ":8000,");
				} catch (IOException e1) {}
			}
			
							
			if(seedUrl.indexOf("http://") == -1)
				seedUrl = "http://" + seedUrl;
			Const.currentURL = URLExtractor.cleanURL(seedUrl);
			System.out.println(Const.currentURL);

			
            Localizer.initialize();
            Environment.getInstance().initializeManagers();                
            Environment.getInstance().startupCompletedNotify();
            
			inputFrame.setVisible(false);
			inputFrame.dispose();
			new PeerCrawlFrame();
		}
	}

	/**
	 * (non-Javadoc)
	 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent e) {
		if(e.getSource() == rootNode)
		{
			if(rootNode.isSelected())
				ipAddr.setEditable(false);
			else
				ipAddr.setEditable(true);
		}
		
		if(e.getSource() == focusedCrawl)
		{
			if(focusedCrawl.isSelected())
				hopCount.setEditable(true);
			else
				hopCount.setEditable(false);
		}
	}

	/**
	 * Main function
	 */
    public static void main( String args[] )
    {   
        PeerCrawl.validateJavaVersion();

       	new PeerCrawl();
        while (true)
        {
        	try {
            		Thread.yield();
            	} catch (Exception e) { e.printStackTrace(); }
        }
    } // End Main

 
    /**
     * Validates Java version. Atleast 1.4 required.
     */
    private static void validateJavaVersion()
    {
        if ( SystemUtils.isJavaVersionAtLeast( 1.4f ) )
            return;
        
        JFrame frame = new JFrame( "Wrong Java Version" );
        frame.setSize( new Dimension( 0, 0 ) );
        frame.setVisible(true);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension winSize = frame.getSize();
        Rectangle rect = new Rectangle(
            (screenSize.width - winSize.width) / 2,
            (screenSize.height - winSize.height) / 2,
            winSize.width, winSize.height );
        frame.setBounds(rect);
        JOptionPane.showMessageDialog( frame,
            "Please use a newer Java VM.\n" +
            "Phex requires at least Java 1.4.0. You are using Java " + SystemUtils.JAVA_VERSION + "\n" +
        	"To get the latest Java release go to http://java.com.",
            "Wrong Java Version", JOptionPane.WARNING_MESSAGE );
        System.exit( 1 );
    } // End of Function validateJavaVersion
    
} // End of Class PeerCrawl
