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
 *  $Id: NLogger.java,v 1.12 2005/11/03 00:59:14 gregork Exp $
 */
package phex.utils;

import java.io.*;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.LogFactory;

import phex.common.Environment;

/**
 * Proxy class for new logging.
 */
public class NLogger
{
    /**
     * Debug log level field to use for configurable logging calls.
     * Preferred method is still to use direct log method calls.
     */
    public static final short LOG_LEVEL_DEBUG = 1;
    
    /**
     * Info log level field to use for configurable logging calls.
     * Preferred method is still to use direct log method calls.
     */
    public static final short LOG_LEVEL_INFO = 2;
    
    /**
     * Debug log level field to use for configurable logging calls.
     * Preferred method is still to use direct log method calls.
     */
    public static final short LOG_LEVEL_WARN = 3;
    
    /**
     * Debug log level field to use for configurable logging calls.
     * Preferred method is still to use direct log method calls.
     */
    public static final short LOG_LEVEL_ERROR = 4;
    
    private static LogFactory factory;
    
    static
    {
        Properties sysProps = System.getProperties();
        // add loaded properties to system properties.
        sysProps.put( "org.apache.commons.logging.Log", "phex.utils.PhexLogger" );
        factory = LogFactory.getFactory( );
        
        // load logging properties
        Properties loggingProperties = new Properties();
        InputStream resIs = null;
        try
        {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            resIs = cl.getResourceAsStream( "phex/resources/logging.properties" );
            if ( resIs != null )
            {
                loggingProperties.load( resIs );
            }
        }
        catch ( Throwable th )
        {
            th.printStackTrace();
        }
        finally
        {
            IOUtil.closeQuietly(resIs);
        }
        InputStream fileIs = null;
        try
        {
            File file = Environment.getInstance().getPhexConfigFile( "logging.properties" );
            if ( file.exists() )
            {
                fileIs = new BufferedInputStream( new FileInputStream(file) );
                loggingProperties.load( fileIs );
            }
        }
        catch ( Throwable th )
        {
            th.printStackTrace();
        }
        finally
        {
            IOUtil.closeQuietly(fileIs);
        }
        
        sysProps = System.getProperties();
        // add loaded properties to system properties.
        sysProps.putAll( loggingProperties );
        LogFactory.releaseAll();
        factory = LogFactory.getFactory();
    }

    /**
     * Returns a log instance.
     * @param clazz
     * @return a log instance.
     */
    public static Log getLogInstance( String name )
    {
        try
        {
            return factory.getInstance( name );
        }
        catch ( LogConfigurationException exp )
        {
            Properties sysProps = System.getProperties();
            // add loaded properties to system properties.
            sysProps.put( "org.apache.commons.logging.Log", "phex.utils.PhexLogger" );
            LogFactory.releaseAll();
            factory = LogFactory.getFactory( );
            return factory.getInstance( name );
        }
    }
    
    /**
     * Returns a log instance.
     * @param clazz
     * @return a log instance.
     */
    public static Log getLogInstance( Class clazz )
    {
        try
        {
            return factory.getInstance( clazz );
        }
        catch ( LogConfigurationException exp )
        {
            Properties sysProps = System.getProperties();
            // add loaded properties to system properties.
            sysProps.put( "org.apache.commons.logging.Log", "phex.utils.PhexLogger" );
            LogFactory.releaseAll();
            factory = LogFactory.getFactory( );
            return factory.getInstance( clazz );
        }
    }

    /**
     * @see org.apache.commons.logging.Log#isDebugEnabled()
     */
    public static boolean isDebugEnabled( String name )
    {
        return getLogInstance( name ).isDebugEnabled();
    }
    
    /**
     * @see org.apache.commons.logging.Log#isInfoEnabled()
     */
    public static boolean isInfoEnabled( String name )
    {
        return getLogInstance( name ).isInfoEnabled();
    }
    
    /**
     * @see org.apache.commons.logging.Log#isWarnEnabled()
     */
    public static boolean isWarnEnabled( String name )
    {
        return getLogInstance( name ).isWarnEnabled();
    }
    
    /**
     * @see org.apache.commons.logging.Log#isWarnEnabled()
     */
    public static boolean isWarnEnabled( Class clazz )
    {
        return getLogInstance( clazz ).isWarnEnabled();
    }
    
    /**
     * @see org.apache.commons.logging.Log#isErrorEnabled()
     */
    public static boolean isErrorEnabled( String name )
    {
        return getLogInstance( name ).isErrorEnabled();
    }
    
    /**
     * Configurable isEnabled call, should only be used in rare cases.
     * Preferred is the direct call.
     */
    public static boolean isEnabled( short logLevel, String name )
    {
        switch (logLevel)
        {
        case LOG_LEVEL_DEBUG:
            return isDebugEnabled(name);
        case LOG_LEVEL_INFO:
            return isInfoEnabled(name);
        case LOG_LEVEL_WARN:
            return isWarnEnabled(name);
        case LOG_LEVEL_ERROR:
            return isErrorEnabled(name);
        default:
            throw new IllegalArgumentException( "Unknown log level: " + logLevel );
        }
    }
    
    /**
     * @see org.apache.commons.logging.Log#debug(java.lang.Object)
     */
    public static void debug( String name, Object message )
    {
        getLogInstance( name ).debug( message );
    }
    
    /**
     * @see org.apache.commons.logging.Log#debug(java.lang.Object)
     */
    public static void debug( Class clazz, Object message )
    {
        getLogInstance( clazz ).debug( message );
    }

    /**
     * @see org.apache.commons.logging.Log#debug(java.lang.Object, java.lang.Throwable)
     */
    public static void debug(String name, Object message, Throwable t)
    {
        getLogInstance( name ).debug( message, t );
    }
    
    /**
     * @see org.apache.commons.logging.Log#info(java.lang.Object)
     */
    public static void info( String name, Object message )
    {
        getLogInstance( name ).info( message );
    }

    /**
     * @see org.apache.commons.logging.Log#info(java.lang.Object, java.lang.Throwable)
     */
    public static void info(String name, Object message, Throwable t)
    {
        getLogInstance( name ).info( message, t );
    }
    
    /**
     * @see org.apache.commons.logging.Log#warn(java.lang.Object)
     */
    public static void warn( String name, Object message )
    {
        getLogInstance( name ).warn( message );
    }

    /**
     * @see org.apache.commons.logging.Log#warn(java.lang.Object, java.lang.Throwable)
     */
    public static void warn(String name, Object message, Throwable t)
    {
        getLogInstance( name ).warn( message, t );
    }
    
    /**
     * @see org.apache.commons.logging.Log#warn(java.lang.Object, java.lang.Throwable)
     */
    public static void warn(Class clazz, Object message, Throwable t)
    {
        getLogInstance( clazz ).warn( message, t );
    }
    
    /**
     * @see org.apache.commons.logging.Log#warn(java.lang.Object, java.lang.Throwable)
     */
    public static void warn(Class clazz, Object message )
    {
        getLogInstance( clazz ).warn( message );
    }

    
    /**
     * @see org.apache.commons.logging.Log#error(java.lang.Object)
     */
    public static void error( String name, Object message )
    {
        getLogInstance( name ).error( message );
    }

    /**
     * @see org.apache.commons.logging.Log#error(java.lang.Object, java.lang.Throwable)
     */
    public static void error(String name, Object message, Throwable t)
    {
        getLogInstance( name ).error( message, t );
    }
    
    /**
     * @see org.apache.commons.logging.Log#error(java.lang.Object, java.lang.Throwable)
     */
    public static void error(Class clazz, Object message, Throwable t)
    {
        getLogInstance( clazz ).error( message, t );
    }
    
    /**
     * Configurable log call, should only be used in rare cases.
     * Preferred is the direct call.
     */
    public static void log( short logLevel, String name, Object message, Throwable t)
    {
        switch (logLevel)
        {
        case LOG_LEVEL_DEBUG:
            debug( name, message, t );
            break;
        case LOG_LEVEL_INFO:
            info( name, message, t );
            break;
        case LOG_LEVEL_WARN:
            warn( name, message, t );
            break;
        case LOG_LEVEL_ERROR:
            error( name, message, t );
            break;
        default:
            throw new IllegalArgumentException( "Unknown log level: " + logLevel );
        }
    }
}
