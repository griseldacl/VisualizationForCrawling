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
 *  $Id: PhexLogger.java,v 1.9 2005/10/03 00:18:29 gregork Exp $
 */
package phex.utils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;

import phex.common.Environment;


/**
 * 
 */
public class PhexLogger implements Log
{
    public static final LogLevel DEBUG = new LogLevel( (short)0 );

    public static final LogLevel INFO = new LogLevel( (short)1 );

    public static final LogLevel WARN = new LogLevel( (short)2 );

    public static final LogLevel ERROR = new LogLevel( (short)3 );
    
    private static String[] verboseLevelName =
    {
        "Debug", "Info", "Warn", "Error"
    };
    
    /**
     * The current verbose level. Default is ERROR.
     */
    private static short logLevelValue = ERROR.value;
    
    /**
     * This flag indicates if the log output is also written to the console.
     */
    private static boolean logToConsole = false;
    
    private static File logFile;
    private static File errorLogFile;

    private static PrintWriter logWriter;

    private static DateFormat dateFormat;
    private static Date date;
    private static long maxLogFileLength;
    
    private String loggerName;

    /**
     * 
     */
    public PhexLogger()
    {
    }
    
    /**
     * 
     */
    public PhexLogger(String name)
    {
        this.loggerName = name;
    }
    
    /**
     * @see org.apache.commons.logging.Log#isDebugEnabled()
     */
    public boolean isDebugEnabled()
    {
        return isLevelLogged( DEBUG );
    }

    /**
     * @see org.apache.commons.logging.Log#isErrorEnabled()
     */
    public boolean isErrorEnabled()
    {
        return isLevelLogged( ERROR );
    }

    /**
     * @see org.apache.commons.logging.Log#isFatalEnabled()
     */
    public boolean isFatalEnabled()
    {
        return isLevelLogged( ERROR );
    }

    /**
     * @see org.apache.commons.logging.Log#isInfoEnabled()
     */
    public boolean isInfoEnabled()
    {
        return isLevelLogged( INFO );
    }

    /* (non-Javadoc)
     * @see org.apache.commons.logging.Log#isTraceEnabled()
     */
    public boolean isTraceEnabled()
    {
        return isLevelLogged( DEBUG );
    }

    /**
     * @see org.apache.commons.logging.Log#isWarnEnabled()
     */
    public boolean isWarnEnabled()
    {
        return isLevelLogged( WARN );
    }

    /**
     * @see org.apache.commons.logging.Log#trace(java.lang.Object)
     */
    public void trace(Object message)
    {
        debug( message );
    }

    /**
     * @see org.apache.commons.logging.Log#trace(java.lang.Object, java.lang.Throwable)
     */
    public void trace(Object message, Throwable t)
    {
        debug(message, t);
    }

    /* (non-Javadoc)
     * @see org.apache.commons.logging.Log#debug(java.lang.Object)
     */
    public void debug( Object message )
    {
        if ( isLevelLogged(DEBUG) )
        {
            writeLogMessage( DEBUG, message, null );
        }
    }

    /**
     * @see org.apache.commons.logging.Log#debug(java.lang.Object, java.lang.Throwable)
     */
    public void debug(Object message, Throwable t)
    {
        if ( isLevelLogged(DEBUG) )
        {
            writeLogMessage( DEBUG, message, t );
        }
    }

    /**
     * @see org.apache.commons.logging.Log#info(java.lang.Object)
     */
    public void info(Object message)
    {
        if ( isLevelLogged(INFO) )
        {
            writeLogMessage( INFO, message, null );
        }
    }

    /**
     * @see org.apache.commons.logging.Log#info(java.lang.Object, java.lang.Throwable)
     */
    public void info(Object message, Throwable t)
    {
        if ( isLevelLogged(INFO) )
        {
            writeLogMessage( INFO, message, t );
        }
    }

    /**
     * @see org.apache.commons.logging.Log#warn(java.lang.Object)
     */
    public void warn(Object message)
    {
        if ( isLevelLogged(WARN) )
        {
            writeLogMessage( WARN, message, null );
        }
    }

    /**
     * @see org.apache.commons.logging.Log#warn(java.lang.Object, java.lang.Throwable)
     */
    public void warn(Object message, Throwable t)
    {
        if ( isLevelLogged(WARN) )
        {
            writeLogMessage( WARN, message, t );
        }
    }

    /**
     * @see org.apache.commons.logging.Log#error(java.lang.Object)
     */
    public void error(Object message)
    {
        if ( isLevelLogged(ERROR) )
        {
            writeLogMessage( ERROR, message, null );
        }
    }

    /**
     * @see org.apache.commons.logging.Log#error(java.lang.Object, java.lang.Throwable)
     */
    public void error(Object message, Throwable t)
    {
        if ( isLevelLogged(ERROR) )
        {
            writeLogMessage( ERROR, message, t );
        }
    }

    /**
     * @see org.apache.commons.logging.Log#fatal(java.lang.Object)
     */
    public void fatal(Object message)
    {
        error(message);
    }

    /**
     * @see org.apache.commons.logging.Log#fatal(java.lang.Object, java.lang.Throwable)
     */
    public void fatal(Object message, Throwable t)
    {
        error( message, t );
    }
    
    static
    {
        String logFileName = System.getProperty( 
            "phex.utils.logger.logFile", "phex.log" );
        setLogFile( Environment.getInstance().getPhexConfigFile(logFileName) );

        String errorLogFileName = System.getProperty( 
            "phex.utils.logger.errorLogFile", "phex.error.log" );
        setErrorLogFile( Environment.getInstance().getPhexConfigFile(
            errorLogFileName ) );
        
        String logToConsoleStr = System.getProperty( 
            "phex.utils.logger.console", "false" );
        setLogToConsole( "true".equalsIgnoreCase(logToConsoleStr) );
        
        String verboseLevel = System.getProperty( 
            "phex.utils.logger.level", "3" );
        try
        {
            setVerboseLevel( Short.parseShort(verboseLevel) );
        }
        catch ( NumberFormatException exp )
        {}

        String maxFileSize = System.getProperty( 
            "phex.utils.logger.maxFileSize", "524288" );
        try
        {
            setMaxLogFileLength( Long.parseLong(maxFileSize) );
        }
        catch ( NumberFormatException exp )
        {}
    }
    
    private static boolean isLevelLogged( LogLevel aLogLevel )
    {
        if ( logLevelValue > aLogLevel.value )
        {
            return false;
        }
        return true;
    }
    
    private void writeLogMessage( LogLevel aVerboseLevel, Object message,
        Throwable throwable )
    {
        String msgText = message != null ? message.toString() : "";
        StringBuffer buffer = new StringBuffer( 40 + msgText.length() );
        buffer.append( getTimeString() );
        buffer.append( " ");
        buffer.append( getLogLevelName( aVerboseLevel ) );
        buffer.append( "/" );
        buffer.append( loggerName );
        buffer.append( ":: " );
        buffer.append( message );
        if ( throwable != null )
        {
            String stackTrace = getStackTrace( throwable );
            buffer.append( " - Exception: " );
            buffer.append( stackTrace );
        }

        if ( logToConsole )
        {
            if ( aVerboseLevel.value >= ERROR.value )
            {
                System.err.println( buffer.toString() );
            }
            else
            {
                System.out.println( buffer.toString() );
            }
        }

        initLogFileWriter();
        synchronized( logWriter )
        {
            logWriter.println( buffer.toString() );
    
            if ( logFile != null && logFile.length() > maxLogFileLength )
            {
                if ( logWriter != null )
                {
                    logWriter.close();
                }
                logWriter = null;
                File tmpFile = new File( logFile.getAbsolutePath() + ".1" );
                tmpFile.delete();
                logFile.renameTo( tmpFile );
            }
            
            // special case.. log errors to error log file...
            if ( aVerboseLevel.value >= ERROR.value )
            {
                try
                {
                    PrintWriter errorLogWriter = new PrintWriter( new BufferedWriter(
                        new FileWriter( errorLogFile.getPath(), true ) ), true );
                    errorLogWriter.println( buffer.toString() );
                    errorLogWriter.close();
                    if ( errorLogFile != null && errorLogFile.length() > maxLogFileLength )
                    {
                        File tmpFile = new File( errorLogFile.getAbsolutePath() + ".1" );
                        tmpFile.delete();
                        errorLogFile.renameTo( tmpFile );
                    }
                }
                catch ( IOException exp )
                {
                    exp.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Returns the current Date in a String-representation.<BR>
     * Remarks:<BR>
     */
    private static String getTimeString()
    {
        if ( dateFormat == null )
        {
            SimpleDateFormat sDateFormat = new SimpleDateFormat(
                "yyMMdd HH:mm:ss,SSSS" );
            dateFormat = sDateFormat;
        }

        if (date == null)
        {
            date = new Date( System.currentTimeMillis() );
        }
        else
        {
            date.setTime( System.currentTimeMillis() );
        }

        return dateFormat.format( date );
    }
    
    private static String getLogLevelName( LogLevel logLevel )
    {
        if ( logLevel.value >= 0 && logLevel.value < verboseLevelName.length )
        {
            return verboseLevelName[ logLevel.value ];
        }
        else
        {
            return "Unknwon (" + logLevel.value + ')';
        }
    }
    
    public static void setLogToConsole( boolean state )
    {
        logToConsole = state;
    }

    public static void setMaxLogFileLength( long byteLength )
    {
        maxLogFileLength = byteLength;
    }

    public static void setVerboseLevel( short newLogLevelValue )
    {
        if ( newLogLevelValue >= DEBUG.value && newLogLevelValue <= ERROR.value )
        {
            logLevelValue = newLogLevelValue;
        }
    }
    
    public static void setLogFile( File file )
    {
        logFile = file;
        logWriter = null;
    }
    
    public static void setErrorLogFile( File file )
    {
        errorLogFile = file;
    }

    
    private static synchronized void initLogFileWriter()
    {
        if ( logWriter != null )
        {
            return;
        }
        if ( logFile != null )
        {
            logFile.getParentFile().mkdirs();
            try
            {
                logWriter = new PrintWriter( new BufferedWriter(
                    new FileWriter( logFile.getPath(), true ) ), true );
            }
            catch ( IOException exp )
            {
                exp.printStackTrace();
            }
        }
        else
        {// redirect to console
            logToConsole = false;
            logWriter = new PrintWriter( System.out, true );
        }
        
        if ( errorLogFile != null )
        {
            errorLogFile.getParentFile().mkdirs();
        }
    }
    
    /**
     * Gets the stack trace of an exception as string.
     * @param       aThrowable  the Throwable
     * @return      the stack trace of the exception
     */
    private static String getStackTrace( Throwable aThrowable )
    {
        CharArrayWriter buffer = new CharArrayWriter();
        PrintWriter printWriter = new PrintWriter( buffer );

        // recursively print nested exceptions to a String
        while (aThrowable != null)
        {
            aThrowable.printStackTrace(printWriter);

            if (aThrowable instanceof InvocationTargetException)
            {
                aThrowable = ((InvocationTargetException) aThrowable)
                    .getTargetException();
            }
            else
            {
                aThrowable = null;
            }
        }
        return buffer.toString();
    }
    
    private static final class LogLevel
    {
        public final short value;

        public LogLevel( short aLevel )
        {
            value = aLevel;
        }
    }
}
