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
 *  $Id: Localizer.java,v 1.18 2005/10/03 00:18:29 gregork Exp $
 */
package phex.utils;

import java.io.*;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.util.*;

import phex.common.ServiceManager;

/**
 * This class is intended to provide localized strings.
 * 
 * <b>How to store localized ressource bundles</b>
 * Phex will look for resource boundles in the classpath that includes the
 * directory $PHEX/lang.<br>It will look for a file called 'language.list'
 * This file should contain a list of translated locales, one per row, in the format
 * language_COUNTRY e.g. de_DE for german Germany. Also its possible to only provide
 * a single language without country definition e.g. de as a definition for all
 * german speeking countries.<br>
 * Translation files for each local should be named e.g. Lang_de_DE.properties or
 * Lang_de.properties<br>
 * <br>
 * <b>Lookup strategy</b>
 * On startup Phex will try to use the locale defined in its configuration file. If
 * nothing is configured it will use the standard platform locale. With the defined
 * locale e.g. de_DE the $PHEX/lang directory and afterwards the classpath
 * phex.resources is searched for a file called Lang_de_DE.properties, then for
 * a file Lang_de.properties and then for a file Lang.properties. All found files
 * are chained for language key lookup in a ResourceBundle.
 * 
 * To display all available locales in the options menu, Phex will use the file
 * $PHEX/lang/language.list and the internal resource
 * phex/resources/language.list for available locale definitions.
 */
public class Localizer
{
    private static Map langKeyMap;
    private static Locale usedLocale;
    private static List availableLocales;
    private static DecimalFormatSymbols decimalFormatSymbols;
    
    public static void initialize()
    {
        setUsedLocale( Locale.US );
        
        String localeStr = ServiceManager.sCfg.usedLocale;
        Locale locale;
        if ( localeStr == null || localeStr.length() == 0 ||
            ( localeStr.length() != 2 && localeStr.length() != 5 && localeStr.length() != 8) )            
        {// default to en_US
            locale = Locale.US;
        }
        else
        {
	        String lang = localeStr.substring( 0, 2 );
	        String country = "";
	        if ( localeStr.length() >= 5 )
	        {
	            country = localeStr.substring( 3, 5 );
	        }
            String variant = "";
            if ( localeStr.length() == 8 )
            {
                variant = localeStr.substring( 6, 8 );
            }
	        locale = new Locale( lang, country, variant );
        }
        setUsedLocale( locale );
    }

    public static void setUsedLocale(Locale locale)
    {
        usedLocale = locale;
        buildResourceBundle( locale );
        decimalFormatSymbols = new DecimalFormatSymbols( usedLocale );
    }
    
    public static Locale getUsedLocale()
    {
        return usedLocale;
    }
    
    public static DecimalFormatSymbols getDecimalFormatSymbols()
    {
        return decimalFormatSymbols;
    }

    private static void buildResourceBundle(Locale locale)
    {
        // we need to build up the resource bundles backwards to chain correctly.
        ArrayList fileList = new ArrayList();
        StringBuffer buffer = new StringBuffer( "Lang" );
        fileList.add( buffer.toString() );
        String language = locale.getLanguage();
        if ( language.length() > 0 )
        {
            buffer.append( '_' );
            buffer.append( language );
            fileList.add( buffer.toString() );
            String country = locale.getCountry();
            if ( country.length() > 0 )
            {
                buffer.append( '_' );
                buffer.append( country );
                fileList.add( buffer.toString() );
                String variant = locale.getVariant();
                if ( variant.length() > 0 )
                {
                    buffer.append( '_' );
                    buffer.append( variant );
                    fileList.add( buffer.toString() );
                }
            }
        }
        langKeyMap = new HashMap();
        HashMap tmpMap = new HashMap();
        String resourceName;
        int size = fileList.size();
        for (int i = 0; i < size; i++)
        {
            // 1) phex.resources classpath
            resourceName = "/phex/resources/" + (String) fileList.get( i )
                + ".properties";
            tmpMap = loadProperties( resourceName );
            if ( tmpMap != null )
            {
                langKeyMap.putAll( tmpMap );
                Logger.logMessage( Logger.FINEST, Logger.GLOBAL, (String)null,
                    new String[]{"Loaded language map: " + resourceName + "."});
            }
            // 2) $PHEX/lang
            resourceName = "/" + (String) fileList.get( i ) + ".properties";
            tmpMap = loadProperties( resourceName );
            if ( tmpMap != null )
            {
                langKeyMap.putAll( tmpMap );
                Logger.logMessage( Logger.FINEST, Logger.GLOBAL, (String)null,
                    new String[]{"Loaded language map: " + resourceName + "."});
            }
        }
    }

    private static HashMap loadProperties(String name)
    {
        InputStream stream = Localizer.class.getResourceAsStream( name );
        if ( stream == null ) { return null; }
        // make sure it is buffered
        stream = new BufferedInputStream( stream );
        Properties props = new Properties();
        try
        {
            props.load( stream );
            return new HashMap( props );
        }
        catch (IOException exp)
        {
        }
        finally
        {
            IOUtil.closeQuietly( stream );
        }
        return null;
    }

    /**
     * To display all available locales in the options menu, Phex will use the file
     * $PHEX/lang/translations.list and the internal resource
     * phex/resources/translations.list for available locale definitions.
     */
    public static List getAvailableLocales()
    {
        if ( availableLocales != null ) { return availableLocales; }
        availableLocales = new ArrayList();
        List list = loadLocalList( "/language.list" );
        availableLocales.addAll( list );
        list = loadLocalList( "/phex/resources/language.list" );
        availableLocales.addAll( list );
        return availableLocales;
    }

    private static List loadLocalList(String name)
    {
        InputStream stream = Localizer.class.getResourceAsStream( name );
        if ( stream == null ) { return Collections.EMPTY_LIST; }
        // make sure it is buffered
        try
        {
            BufferedReader reader = new BufferedReader( new InputStreamReader(
                stream, "ISO-8859-1" ) );
            ArrayList list = new ArrayList();
            String line;
            Locale locale;
            while (true)
            {
                line = reader.readLine();
                if ( line == null )
                {
                    break;
                }
                line = line.trim();
                if ( line.startsWith( "#" ) 
                  || ( line.length() != 2 && line.length() != 5 && line.length() != 8 ) )
                {
                    continue;
                }
                String lang = line.substring( 0, 2 );
                String country = "";
                if ( line.length() >= 5 )
                {
                    country = line.substring( 3, 5 );
                }
                String variant = "";
                if ( line.length() == 8 )
                {
                    variant = line.substring( 6, 8 );
                }
                locale = new Locale( lang, country, variant );
                list.add( locale );
            }
            return list;
        }
        catch (IOException exp)
        {
            NLogger.error( NLoggerNames.LOCALIZATION, exp, exp);
        }
        finally
        {
            IOUtil.closeQuietly( stream );
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Returns the actual language text out of the resource boundle.
     * If the key is not defined it returns the key itself and prints an
     * error message on system.err.
     */
    public static String getString(String key)
    {
        String value = (String)langKeyMap.get( key );
        if ( value == null )
        {
            NLogger.error(NLoggerNames.LOCALIZATION, "Missing language key: " + key );
            value = key;
        }
        return value;
    }

    /**
     * Returns the first character of the actual language text out of the
     * resource boundle. The method can be usefull for getting mnemonics.
     * If the key is not defined it returns the first char of the key itself and
     * prints an error message on system.err.
     */
    public static char getChar(String key)
    {
        String str = getString( key );
        return str.charAt( 0 );
    }

    /**
     * Returns the actual language text out of the resource boundle and formats
     * it accordingly with the given Object array.
     * If the key is not defined it returns the key itself and print an
     * error message on system.err.
     */
    public static String getFormatedString(String key, Object[] obj)
    {
        String value = null;
        
        String lookupValue = (String)langKeyMap.get( key );
        if ( lookupValue != null )
        {
            value = MessageFormat.format( lookupValue, obj );
        }
        else
        {
            Logger.logMessage( Logger.INFO, Logger.GLOBAL, (String)null,
                new String[]{"Missing language key: " + key} );
            value = key;
        }
        return value;
    }
}