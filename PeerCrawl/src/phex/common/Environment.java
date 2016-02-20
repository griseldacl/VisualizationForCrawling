/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2005 Phex Development Group
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 *  --- CVS Information ---
 *  $Id: Environment.java,v 1.32 2005/10/03 00:18:22 gregork Exp $
 *  File Modified
 */

/**
 * PeerCrawl - Distributed P2P web crawler based on Gnutella Protocol
 * @version 2.0
 * 
 * Developed as part of Masters Project - Spring 2006
 * @author 	Vaibhav Padliya
 * 			College of Computing
 * 			Georgia Tech
 * 
 * @contributor Mudhakar Srivatsa
 * @contributor Mahesh Palekar
 */
package phex.common;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import phex.event.UserMessageListener;
import phex.utils.*;


public class Environment
{
	
	public static final String statsDir = "stats";
	
    /**
     * A list of all managers to initialize on startup.
     */
    private static final Class[] MANAGER_CLASSES =
    {
        phex.download.swarming.SwarmingManager.class,
        phex.connection.NetworkManager.class,
        phex.udp.UdpConnectionManager.class,
        phex.upload.UploadManager.class,
        phex.share.ShareManager.class,
        phex.query.QueryManager.class,
        phex.chat.ChatManager.class,
        phex.host.HostManager.class,
        phex.security.PhexSecurityManager.class,
        phex.msg.MsgManager.class,
        phex.statistic.StatisticsManager.class,
        phex.common.bandwidth.BandwidthManager.class,
        phex.gwebcache.GWebCacheManager.class,
        phex.common.Ip2CountryManager.class,
        phex.udp.hostcache.UdpHostCacheManager.class
    };

    private static Environment environment;

    private Properties properties;
    private File configurationRoot;
    
    private UserMessageListener userMessageListener;

    /**
     * The TimerService is a single thread that will handle multiple TimerTask.
     * Therefore each task has to make sure it is not performing a long blocking
     * operation.
     */
    private Timer timerService;
    private boolean isWindowsOS;
    private boolean isWin2000orXpOS;
    private boolean isMacOSX;
    private String javaVersion;

    private Environment()
    {
        try
        {
            properties = new Properties();
            properties.load( Environment.class.getResourceAsStream(
                "/phex/resources/version.properties" ) );
        }
        catch ( IOException exp )
        {
            NLogger.error( NLoggerNames.STARTUP, exp, exp );
            throw new RuntimeException();
        }
        initializeOS();
        initializeJavaVersion();
        timerService = new Timer( true );
    }

    public static Environment getInstance()
    {
        if ( environment == null )
        {
            environment = new Environment();
        }
        return environment;
    }

    public void initializeManagers()
    {
        ArrayList managerList = new ArrayList( MANAGER_CLASSES.length );

        Method method;
        Manager manager;
        for ( int i = 0; i < MANAGER_CLASSES.length; i++ )
        {
            long start = System.currentTimeMillis();
            try
            {
                Logger.logMessage( Logger.FINER, Logger.GLOBAL,
                    "Initializing " + MANAGER_CLASSES[i].getName() );
                method = MANAGER_CLASSES[i].getMethod("getInstance", new Class[0]);
                manager = (Manager)method.invoke(null, new Object[0]);
                boolean succ = manager.initialize();
                if ( !succ )
                {
                    NLogger.error( NLoggerNames.GLOBAL,
                        "Failed to initialize " + manager.getClass().getName() );
                    throw new RuntimeException( "Failed to initialize " +
                        manager.getClass().getName() );
                }
                managerList.add( manager );
                
                long end = System.currentTimeMillis();
                Logger.logMessage( Logger.INFO, Logger.PERFORMANCE,
                    "Initialization time: " + (end-start) + " - " +
                    manager.getClass().getName());
            }
            catch ( InvocationTargetException exp )
            {
                NLogger.error( NLoggerNames.GLOBAL, exp, exp );
            }
            catch ( NoSuchMethodException exp )
            {
                NLogger.error( NLoggerNames.GLOBAL, exp, exp );
            }
            catch ( IllegalAccessException exp )
            {
                NLogger.error( NLoggerNames.GLOBAL, exp, exp );
            }
        }

        int size = managerList.size();
        for ( int i = 0; i < size; i++ )
        {
            long start = System.currentTimeMillis();
            manager = (Manager)managerList.get( i );
            Logger.logMessage( Logger.FINER, Logger.GLOBAL,
                "On post initialization " + manager.getClass().getName() );
            boolean succ = manager.onPostInitialization();
            if ( !succ )
            {
                NLogger.error( NLoggerNames.GLOBAL, "Failed to initialize " +
                    manager.getClass().getName() );
                throw new RuntimeException( "Failed to initialize " +
                    manager.getClass().getName() );
            }
            long end = System.currentTimeMillis();
            Logger.logMessage( Logger.INFO, Logger.PERFORMANCE,
                "Post-Initialization time: " + (end-start) + " - " +
                manager.getClass().getName() );
        }
    }
    
    /**
     * This method is called after the complete application including GUI completed
     * its startup process.
     */
    public void startupCompletedNotify()
    {
        Method method;
        Manager manager = null;
        for ( int i = 0; i < MANAGER_CLASSES.length; i++ )
        {
            long start = System.currentTimeMillis();
            try
            {
                Logger.logMessage( Logger.FINER, Logger.GLOBAL,
                    "StartupCompletedNotify " + MANAGER_CLASSES[i].getName() );
                method = MANAGER_CLASSES[i].getMethod("getInstance", new Class[0]);
                manager = (Manager)method.invoke(null, new Object[0]);
                manager.startupCompletedNotify();
            }
            catch ( InvocationTargetException exp )
            {
                NLogger.error( NLoggerNames.GLOBAL, exp, exp );
            }
            catch ( NoSuchMethodException exp )
            {
                NLogger.error( NLoggerNames.GLOBAL, exp, exp );
            }
            catch ( IllegalAccessException exp )
            {
                NLogger.error( NLoggerNames.GLOBAL, exp, exp );
            }
            long end = System.currentTimeMillis();
            Logger.logMessage( Logger.INFO, Logger.PERFORMANCE,
                "StartupCompletedNotify time: " + (end-start) + " - " +
                manager.getClass().getName() );

        }
    }    

    public void shutdownManagers( )
    {
        Method method;
        Manager manager;
        for ( int i = 0; i < MANAGER_CLASSES.length; i++ )
        {
            try
            {
                Logger.logMessage( Logger.FINER, Logger.GLOBAL,
                    "Shutdown " + MANAGER_CLASSES[i].getName() );
                method = MANAGER_CLASSES[i].getMethod("getInstance", new Class[0]);
                manager = (Manager)method.invoke(null, new Object[0]);
                manager.shutdown();
            }
            catch ( InvocationTargetException exp )
            {
                NLogger.error( NLoggerNames.GLOBAL, exp, exp  );
            }
            catch ( NoSuchMethodException exp )
            {
                NLogger.error( NLoggerNames.GLOBAL, exp, exp  );
            }
            catch ( IllegalAccessException exp )
            {
                NLogger.error( NLoggerNames.GLOBAL, exp, exp  );
            }
            catch ( Exception exp )
            {
                NLogger.error( NLoggerNames.GLOBAL, exp, exp  );
            }
        }
    }

    /**
     * Sets the directory into which Phex adds its configuration files. When
     * configRoot is null the directory is set to:<br>
     * {user.home}/phex on windows systems and<br>
     * {user.home}/.phex on unix and mac systems.
     * @param configRoot the directory into which Phex adds its configuration files
     *        or null.
     */
    public void setPhexConfigRoot( File configRoot )
        throws IOException
    {
        if ( configRoot == null )
        {
            StringBuffer path = new StringBuffer(20);
            //path.append( System.getProperty("user.home") );
            //path.append( File.separator );

            //phex config files are hidden on all UNIX systems (also MacOSX. Since
            //there are many UNIX like operation systems with Java support out there,
            //we can not recognize the OS through it's name. Thus we check if the
            //root of the filesystem starts with "/" since only UNIX uses such
            //filesystem conventions
            if ( File.separatorChar == '/' )
            {
                path.append ('.');
            }
            path.append (statsDir);
            configRoot = new File( path.toString() );
        }
        if ( !configRoot.isDirectory() )
        {
            boolean succ = configRoot.mkdirs();
            if ( !succ )
            {
                throw new IOException( "Cant create directory: " + configRoot.getAbsolutePath() );
            }
        }
        configurationRoot = configRoot;
    }

    /**
     * Returns the File representing the complete path to the configuration file
     * with the given configFileName.
     * @param configFileName the name of the config file to determine the complete
     *        path for.
     * @return the File representing the complete path to the configuration file
     *         with the given configFileName.
     */
    public File getPhexConfigFile( String configFileName )
    {
        if ( configurationRoot == null )
        {
            try
            {
                setPhexConfigRoot( null );
            }
            catch ( IOException exp )
            {// TODO test if logging here can cause trouble when the Logger cant
             // initialize the file name.
             //Logger.logError( exp );
            }
        }
        return new File( configurationRoot, configFileName );
    }

    public String getProperty( String name )
    {
        return properties.getProperty( name );
    }

    public String getProperty( String name, String defaultValue )
    {
        return properties.getProperty( name, defaultValue );
    }

    /**
     * Returns the Phex Vendor string containing the Phex version.
     * @return the Phex Vendor string containing the Phex version.
     */
    public static String getPhexVendor()
    {
        return "Phex " + Cfg.PRIVATE_BUILD_ID + VersionUtils.getFullProgramVersion();
    }

    /**
     * Schedules the specified task for repeated fixed-delay execution,
     * beginning after the specified delay. Subsequent executions take place at
     * approximately regular intervals separated by the specified period.
     *
     * The TimerService is a single thread that will handle multiple TimerTask.
     * Therefore each task has to make sure it is not performing a long blocking
     * operation.
     *
     * @param task The task to be scheduled.
     * @param delay The delay in milliseconds before task is to be executed.
     * @param period The time in milliseconds between successive task executions.
     */
    public void scheduleTimerTask(TimerTask task, long delay, long period )
    {
        timerService.schedule( task, delay, period );
    }
    
    /**
     * Schedules the specified task for execution after the specified delay.
     * 
     * The TimerService is a single thread that will handle multiple TimerTask.
     * Therefore each task has to make sure it is not performing a long blocking
     * operation.
     *
     * @param task The task to be scheduled.
     * @param delay The delay in milliseconds before task is to be executed.
     */
    public void scheduleTimerTask(TimerTask task, long delay )
    {
        timerService.schedule( task, delay );
    }

    /**
     * Returns true if the system is a windows os, false otherwise.
     * @return true if the system is a windows os, false otherwise.
     */
    public boolean isWindowsOS()
    {
        return isWindowsOS;
    }

    /**
     * Returns true if the system is a MacOSX, false otherwise.
     * @return true if the system is a MacOSX, false otherwise.
     */
    public boolean isMacOSX()
    {
        return isMacOSX;
    }

    /**
     * Returns true if the system is a ultrapeer os, false otherwise.
     * @return true if the system is a ultrapeer os, false otherwise.
     */
    public boolean isUltrapeerOS()
    {
        // accept all none windows systems (MacOSX, Unix...) or Windows 2000 or XP.
        return !isWindowsOS || isWin2000orXpOS;
    }

    /**
     * Determines what os this system is using.
     */
    private void initializeOS()
    {
        // get the operating system
        String os = System.getProperty("os.name").toLowerCase();
        if( os.startsWith("mac os") )
        {
            isMacOSX = os.endsWith("x");
            return;
        }
        else if( os.indexOf( "windows" ) != -1 )
        {
            isWindowsOS = true;
            if ( os.indexOf( "windows 2000" ) != -1 ||
                os.indexOf( "windows xp" ) != -1 )
            {
                isWin2000orXpOS = true;
            }
            return;
        }
    }
    
    private void initializeJavaVersion()
    {
        javaVersion = System.getProperty( "java.version" );
    }
        
    /**
     * Checks if the user runs a Java 1.4 or later VM.
     */
    public boolean isJava14orLater()
    {
        return !javaVersion.startsWith("1.3") && !javaVersion.startsWith("1.2") 
            && !javaVersion.startsWith("1.1") && !javaVersion.startsWith("1.0"); 
    }
    
    public void setUserMessageListener( UserMessageListener listener )
    {
        userMessageListener = listener;
    }
    
    public void fireDisplayUserMessage( String userMessageId )
    {
        // if initialized
        if ( userMessageListener != null )
        {
            userMessageListener.displayUserMessage( userMessageId, null );
        }
    }
    
    public void fireDisplayUserMessage( String userMessageId, String[] args )
    {
        // if initialized
        if ( userMessageListener != null )
        {
            userMessageListener.displayUserMessage( userMessageId, args );
        }
    }
}