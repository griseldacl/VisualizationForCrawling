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
 */
package phex.utils;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import phex.common.ServiceManager;


/**
 * This class is supposed to try to integrate a final logging framework for Phex
 * after many different trys this should be the standard!
 *
 * Currently all logging is static but this could be extended to have a
 * identifier for each logger instance. But this seems not to be necessary now.
 */
public final class Logger
{
    /**
     * Verbose Level: FINEST indicates a highly detailed tracing message
     */
    public static final LogLevel FINEST = new LogLevel( (short)0 );

    /**
     * Verbose Level: FINER indicates a fairly detailed tracing message.
     */
    public static final LogLevel FINER = new LogLevel( (short)1 );

    /**
     * Verbose Level: FINE is a message level providing tracing information.
     */
    public static final LogLevel FINE = new LogLevel( (short)2 );

    /**
     * Verbose Level: CONFIG is a message level for static configuration messages.
     */
    public static final LogLevel CONFIG = new LogLevel( (short)3 );

    /**
     * Verbose Level: INFO is a message level for informational messages.
     */
    public static final LogLevel INFO = new LogLevel( (short)4 );

    /**
     * Verbose Level: WARNING is a message level indicating a potential problem.
     */
    public static final LogLevel WARNING = new LogLevel( (short)5 );

    /**
     * Verbose Level: SEVERE is a message level indicating a serious failure.
     */
    public static final LogLevel SEVERE = new LogLevel( (short)6 );

    /**
     * Log type global message. For messages that have global influence like
     * errors or application notifications. These messages are always logged
     * and cant be turned off.
     */
    public static final short GLOBAL = 0xFF;

    /**
     * Log type download message.
     */
    public static final short DOWNLOAD = 0x02;

    /**
     * Log type upload message.
     */
    public static final short UPLOAD = 0x04;

    /**
     * Log type search message.
     */
    public static final short SEARCH = 0x08;

    /**
     * Log type network message.
     */
    public static final short NETWORK = 0x10;

    /**
     * Log type gui message.
     */
    public static final short GUI = 0x20;
    
    /**
     * Log type performance message.
     */
    public static final short PERFORMANCE = 0x40;
    
    /**
     * Log type for download network and IO related messages.
     */
    public static final short DOWNLOAD_NET = 0x80;


    /**
     * The current verbose level. Default is ERROR.
     */
    private static short logLevelValue = SEVERE.value;

    /**
     * The current log type.
     */
    private static short logType = 0x01;
    
    private static Map logResourceMap;

    static
    {
        logResourceMap = new HashMap();
        logResourceMap.put( "log.core.msg", "{0}" );
        logResourceMap.put( "log.core.msg2", "{0} {1}" );
        logResourceMap.put( "log.core.exp", "Exception occured: {0}" );
        logResourceMap.put( "log.core.expmsg", "{1} - Exception: {0}" );

        setVerboseLevel( ServiceManager.sCfg.loggerVerboseLevel );
        setLogType( ServiceManager.sCfg.logType );
    }

    private Logger()
    {
    }

    /**
     * @deprecated
     */
    public static void logWarning( Throwable throwable )
    {
        if ( logLevelValue > WARNING.value || (GLOBAL & logType) == 0)
        {
            return;
        }
        String stackTrace = getStackTrace( throwable );
        Object[] arguments =
        {
            stackTrace
        };
        logMessage( WARNING, GLOBAL, "log.core.exp", arguments );
    }

    /**
     * @deprecated
     */
    public static void logWarning( short aLogType, String message )
    {
        if ( logLevelValue > WARNING.value || (aLogType & logType) == 0)
        {
            return;
        }
        Object[] arguments =
        {
            message
        };
        logMessage( WARNING, aLogType, "log.core.msg", arguments );
    }
    
    /**
     * @deprecated
     */
    public static void logWarning( short aLogType, Object firstArgument, Object secondArgument )
    {
        if ( logLevelValue > WARNING.value || (aLogType & logType) == 0)
        {
            return;
        }
        Object[] arguments =
        {
            firstArgument,
            secondArgument
        };
        logMessage( WARNING, aLogType, "log.core.msg2", arguments );
    }

    
    public static void logMessage( LogLevel aVerboseLevel, short aLogType,
        Object firstArgument )
    {
        if ( logLevelValue > aVerboseLevel.value || (aLogType & logType) == 0)
        {
            return;
        }
        Object[] arguments =
        {
            firstArgument
        };
        logMessage( aVerboseLevel, aLogType, "log.core.msg", arguments );
    }

    
    public static void logMessage( LogLevel aVerboseLevel, short aLogType,
        Throwable aThrowable )
    {
        if ( logLevelValue > aVerboseLevel.value || (aLogType & logType) == 0)
        {
            return;
        }
        String stackTrace = getStackTrace( aThrowable );
        Object[] arguments =
        {
            stackTrace
        };
        logMessage( aVerboseLevel, aLogType, "log.core.exp", arguments );
    }
    
    
    public static void logMessage( LogLevel aVerboseLevel, short aLogType,
        Object firstArgument, Object secondArgument )
    {
        if ( logLevelValue > aVerboseLevel.value || (aLogType & logType) == 0)
        {
            return;
        }
        Object[] arguments =
        {
            firstArgument,
            secondArgument
        };
        logMessage( aVerboseLevel, aLogType, "log.core.msg2", arguments );
    }

    
    public static void logMessage( LogLevel aVerboseLevel, short aLogType,
        Throwable aThrowable, Object secondArgument )
    {
        if ( logLevelValue > aVerboseLevel.value || (aLogType & logType) == 0)
        {
            return;
        }
        String stackTrace = getStackTrace( aThrowable );
        Object[] arguments =
        {
            stackTrace,
            secondArgument
        };
        logMessage( aVerboseLevel, aLogType, "log.core.expmsg", arguments );
    }

    
    public static void logMessage( LogLevel aVerboseLevel, short aLogType,
        String aResource, Object[] arguments )
    {
        if ( logLevelValue > aVerboseLevel.value || (aLogType & logType) == 0)
        {
            return;
        }
        String formatedString;
        if ( aResource != null )
        {
            String lookupValue = (String)logResourceMap.get( aResource );
            if ( lookupValue != null )
            {
                formatedString = MessageFormat.format( lookupValue, arguments );
            }
            else
            {
                formatedString = lookupValue;
            }
        }
        else
        {
            formatedString = StringUtils.join( arguments, ";" );
        }
        
        String logTypeName = getLogTypeName(aLogType);
        formatedString = "(" + logTypeName + ")! " + formatedString;
        switch (aVerboseLevel.value)
        {
        case 0:
        case 1:
        case 2:
            NLogger.debug(logTypeName, formatedString );
            break;
        case 3:
        case 4:
            NLogger.info(logTypeName, formatedString );
            break;
        case 5:
            NLogger.warn(logTypeName, formatedString );
            break;
        case 6:
            NLogger.error(logTypeName, formatedString );
            break;
        }
    }

    
    public static void setVerboseLevel( short newLogLevelValue )
    {
        if ( newLogLevelValue >= FINEST.value && newLogLevelValue <= SEVERE.value )
        {
            logLevelValue = newLogLevelValue;
        }
    }

    public static int getVerboseLevel( )
    {
        return logLevelValue;
    }

    public static void setLogType( boolean download, boolean upload,
        boolean search, boolean network, boolean gui )
    {
        logType = (short)( 0x01 | (download?DOWNLOAD:0x00) | (upload?UPLOAD:0x00)
            | (search?SEARCH:0x00) | (network?NETWORK:0x00) | (gui?GUI:0x00) );
    }

    public static void setLogType( short aLogType )
    {
        if ( aLogType >= 0x01 && aLogType <= 0xFF )
        {
            logType = aLogType;
        }
    }

    public static boolean isTypeLogged( short aLogType )
    {
        return (aLogType & logType) > 0;
    }

    public static boolean isLevelLogged( LogLevel aLogLevel )
    {
        if ( logLevelValue > aLogLevel.value )
        {
            return false;
        }
        return true;
    }
    
    public static boolean isLevelTypeLogged( LogLevel aLogLevel,
        short aLogType )
    {
        if ( logLevelValue > aLogLevel.value )
        {
            return false;
        }
        return (aLogType & logType) > 0;
    }


    private static String getLogTypeName( short logType )
    {
        switch (logType)
        {
            case DOWNLOAD:
                return "Download";
            case UPLOAD:
                return "Upload";
            case SEARCH:
                return "Search";
            case NETWORK:
                return "Network";
            case GUI:
                return "GUI";
            case PERFORMANCE:
                return "Performance";
            case DOWNLOAD_NET:
                return "Download Net";
            case GLOBAL:
                return "Global";
            default:
                return "Unknwon (" + logType + ')';
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


    public static final class LogLevel
    {
        public final short value;

        public LogLevel( short aLevel )
        {
            value = aLevel;
        }
    }
}