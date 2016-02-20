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
 *  $Id: PhexSecurityManager.java,v 1.31 2005/11/03 16:33:48 gregork Exp $
 */
package phex.security;

import java.io.*;
import java.util.*;

import javax.xml.bind.JAXBException;

import phex.common.*;
import phex.common.address.AddressUtils;
import phex.common.address.DestAddress;
import phex.common.address.IpAddress;
import phex.common.file.FileManager;
import phex.common.file.ManagedFile;
import phex.common.file.ManagedFileException;
import phex.event.AsynchronousDispatcher;
import phex.event.SecurityRulesChangeListener;
import phex.event.UserMessageListener;
import phex.utils.*;
import phex.xml.*;

// TODO update list from http://methlabs.org/sync/
public class PhexSecurityManager implements Manager
{
    public static final byte ACCESS_GRANTED = 0x01;
    public static final byte ACCESS_DENIED = 0x02;
    public static final byte ACCESS_STRONGLY_DENIED = 0x03;

    private static PhexSecurityManager instance;

    private ArrayList ipAccessRuleList;
    private FastIpList fastIpList;
    

    public PhexSecurityManager()
    {
        ipAccessRuleList = new ArrayList();
        fastIpList = new FastIpList();
    }

    public int getIPAccessRuleCount()
    {
        synchronized( ipAccessRuleList )
        {
            return ipAccessRuleList.size();
        }
    }

    public IPAccessRule getIPAccessRule( int index )
    {
        synchronized( ipAccessRuleList )
        {
            if ( index < 0 || index >= ipAccessRuleList.size() )
            {
                return null;
            }
            return (IPAccessRule) ipAccessRuleList.get( index );
        }
    }

    public IPAccessRule[] getIPAccessRulesAt( int[] indices )
    {
        synchronized( ipAccessRuleList )
        {
            int length = indices.length;
            IPAccessRule[] rules = new IPAccessRule[ length ];
            int listSize = ipAccessRuleList.size();
            for ( int i = 0; i < length; i++ )
            {
                if ( indices[i] < 0 || indices[i] >= listSize )
                {
                    rules[i] = null;
                }
                else
                {
                    rules[i] = (IPAccessRule)ipAccessRuleList.get( indices[i] );
                }
            }
            return rules;
        }
    }


    public IPAccessRule createIPAccessRule( String description,
        boolean isDenyingRule, byte type, byte[] ip, byte[] compareIP,
        boolean isDisabled, ExpiryDate expiryDate, boolean isDeletedOnExpiry )
    {
        IPAccessRule rule = new IPAccessRule( description, isDenyingRule, type,
            ip, compareIP, false, false, isDisabled );
        rule.setExpiryDate( expiryDate );
        rule.setDeleteOnExpiry( isDeletedOnExpiry );

        int position;
        synchronized( ipAccessRuleList )
        {
            position = ipAccessRuleList.size();
            ipAccessRuleList.add( rule );
            fastIpList.add( rule );
        }
        fireSecurityRuleAdded( position );
        return rule;
    }

    public void removeSecurityRule( SecurityRule rule )
    {
        int idx;
        synchronized( ipAccessRuleList )
        {
            idx = ipAccessRuleList.indexOf( rule );
            if ( idx != -1 )
            {
                ipAccessRuleList.remove( idx );
                fastIpList.remove( (IPAccessRule)rule );
            }
        }
        fireSecurityRuleRemoved( idx );
    }

    public byte controlHostAddressAccess( DestAddress address )
    {
        IpAddress ipAddress = address.getIpAddress();
        if ( ipAddress == null )
        {// no ip address... security is not checking not ip based  addresses.
            return ACCESS_GRANTED;
        }
        byte[] hostIP = ipAddress.getHostIP();
        return controlHostIPAccess( hostIP );
    }

    public byte controlHostIPAccess( byte[] hostIP )
    {
        return fastIpList.controlHostIPAccess(hostIP);
    }

    private void loadHostileHostList( Map systemRuleMap )
    {
        try
        {
            NLogger.debug( NLoggerNames.Security,
                "Load hostile hosts file." );
            long start = System.currentTimeMillis();
            InputStream inStream = ClassLoader.getSystemResourceAsStream(
                "phex/resources/hostilehosts.cfg" );
            BufferedReader br;
            if ( inStream != null )
            {
                br = new BufferedReader( new InputStreamReader( inStream ) );
            }
            else
            {
                NLogger.debug( NLoggerNames.Security,
                    "Hostile hosts file not found." );
                return;
            }

            String line;
            IPAccessRule rule;
            while ( (line = br.readLine()) != null)
            {
                if ( StringUtils.isEmpty(line) || line.startsWith("#") )
                {
                    continue;
                }
                int slashIdx = line.indexOf( '/' );
                byte[] ip;
                byte[] mask;
                byte type;
                if ( slashIdx == -1 )
                {// single ip...
                    ip = AddressUtils.parseIP( line );
                    mask = null;
                    type = IPAccessRule.SINGLE_ADDRESS;
                }
                else
                {
                    String ipStr = line.substring( 0, slashIdx ).trim();
                    String maskStr = line.substring( slashIdx + 1 ).trim();
                    ip = AddressUtils.parseIP( ipStr );
                    mask = AddressUtils.parseIP( maskStr );
                    type = IPAccessRule.NETWORK_MASK;
                }
                rule = new IPAccessRule( "System rule.", true, type, ip, mask, true, true, false );
                
                // adjust hit count..
                XJBSecurityRule xjbRule = findSystemXJBRule( systemRuleMap, ip,
                    mask );
                if ( xjbRule != null )
                {
                    rule.setTriggerCount( xjbRule.getTriggerCount() );
                }
                ipAccessRuleList.add( rule );
                fastIpList.add(rule);
                //if ( ipAccessRuleList.size()%10000==0)
                //{
                //    long end = System.currentTimeMillis();
                //    NLogger.debug( NLoggerNames.Security,
                //        "Part: " + ((double)(end-start)/(double)ipAccessRuleList.size()) + " " + ipAccessRuleList.size() );
                //}
            }
            br.close();
            long end = System.currentTimeMillis();
            NLogger.debug( NLoggerNames.Security,
                "Loaded hostile hosts file: " + (end-start) );
        }
        catch ( IOException exp )
        {
            NLogger.warn( NLoggerNames.Security, exp, exp );
        }
    }

    private XJBSecurityRule findSystemXJBRule( Map systemRuleMap, byte[] ip, 
        byte[] mask )
    {
        StringBuffer keyBuf = new StringBuffer( AddressUtils.ip2string(ip) );
        if ( mask != null )
        {
            keyBuf.append( "-" ).append( AddressUtils.ip2string(mask) );
        }
        XJBIPAccessRule xjbRule = (XJBIPAccessRule)systemRuleMap.get( keyBuf.toString() );
        if ( xjbRule == null || !xjbRule.isSystemRule())
        {
            return null;
        }
        return xjbRule;
    }

    private void loadSecurityRuleList()
    {
        NLogger.debug( NLoggerNames.Security,
            "Loading security rule list..." );
        File securityFile = Environment.getInstance().getPhexConfigFile(
            EnvironmentConstants.XML_SECURITY_FILE_NAME );
        XJBPhex phex;
        ObjectFactory objFactory = new ObjectFactory();
        try
        {
            if ( securityFile.exists() )
            {
                FileManager fileMgr = FileManager.getInstance();
                ManagedFile managedFile = fileMgr.getReadWriteManagedFile( securityFile );
                phex = XMLBuilder.loadXJBPhexFromFile( managedFile );
            }
            else
            {
                phex = objFactory.createXJBPhex();
            }            
            XJBSecurity xjbSecurity = phex.getSecurity();
            if ( xjbSecurity == null )
            {
                NLogger.debug( NLoggerNames.Security,
                    "No security definition found." );
                xjbSecurity = objFactory.createXJBSecurity();
            }
            List ruleList = xjbSecurity.getIpAccessRuleList();
            synchronized( ipAccessRuleList )
            {
                Iterator iterator = ruleList.iterator();
                XJBIPAccessRule xjbRule;
                IPAccessRule rule;
                Map systemRuleMap = new HashMap();
                while( iterator.hasNext() )
                {
                    xjbRule = (XJBIPAccessRule)iterator.next();
                    if ( !xjbRule.isSystemRule() )
                    {
                        rule = new IPAccessRule( xjbRule );
                        ipAccessRuleList.add( rule );
                        fastIpList.add(rule);
                    }
                    else
                    {
                        StringBuffer keyBuf = new StringBuffer( AddressUtils.ip2string(xjbRule.getIp()) );
                        if ( xjbRule.getCompareIP() != null )
                        {
                            keyBuf.append( "-" ).append( AddressUtils.ip2string(xjbRule.getCompareIP()) );
                        }
                        systemRuleMap.put(keyBuf.toString(), xjbRule);
                    }
                }
                loadHostileHostList( systemRuleMap );
                
                // optimize ipAccessRuleList
                ipAccessRuleList.trimToSize();
            }
        }
        catch ( JAXBException exp )
        {
            Throwable linkedException = exp.getLinkedException();
            if ( linkedException != null )
            {
                NLogger.error( NLoggerNames.Security, linkedException, linkedException );
            }
            NLogger.error( NLoggerNames.Security, exp, exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.SecuritySettingsLoadFailed, 
                new String[]{ exp.toString() } );
            return;
        }
        catch ( ManagedFileException exp )
        {
            NLogger.error( NLoggerNames.Security, exp, exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.SecuritySettingsLoadFailed, 
                new String[]{ exp.toString() } );
            return;
        }
    }

    private void saveSecurityRuleList()
    {
        NLogger.debug( NLoggerNames.Security,
            "Saving security rule list..." );

        try
        {
            ObjectFactory objFactory = new ObjectFactory();
            XJBPhex phex = objFactory.createPhexElement();

            XJBSecurity security = objFactory.createXJBSecurity();
            phex.setSecurity( security );
            phex.setPhexVersion( VersionUtils.getFullProgramVersion() );

            List ruleList = security.getIpAccessRuleList();
            synchronized( ipAccessRuleList )
            {
                Iterator iterator = ipAccessRuleList.iterator();
                IPAccessRule rule;
                while ( iterator.hasNext() )
                {
                    rule = ( IPAccessRule )iterator.next();
                    if ( !rule.isSystemRule() && rule.isDeletedOnExpiry() && 
                         ( rule.getExpiryDate().isExpiringEndOfSession() ||
                           rule.getExpiryDate().isExpired() ) )
                    {// skip session expiry rules that get deleted on expiry...
                     // except if they are system rules
                        continue;
                    }

                    if ( rule.isSystemRule() && rule.getTriggerCountObject().intValue() == 0)
                    {// we dont care about system rules with no trigger count.
                        continue;
                    }
                    
                    XJBSecurityRule xjbRule = rule.createXJBSecurityRule();
                    ruleList.add( xjbRule );
                }
            }

            File securityFile = Environment.getInstance().getPhexConfigFile(
                EnvironmentConstants.XML_SECURITY_FILE_NAME );
            ManagedFile managedFile = FileManager.getInstance().getReadWriteManagedFile( securityFile );
            XMLBuilder.saveToFile( managedFile, phex );
        }
        catch ( JAXBException exp )
        {
            // TODO during close this message is never displayed since application
            // will exit too fast. A solution to delay exit process in case 
            // SlideInWindows are open needs to be found.
            NLogger.error( NLoggerNames.Security, exp, exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.SecuritySettingsSaveFailed, 
                new String[]{ exp.toString() } );
        }
        catch ( ManagedFileException exp )
        {
            // TODO during close this message is never displayed since application
            // will exit too fast. A solution to delay exit process in case 
            // SlideInWindows are open needs to be found.
            NLogger.error( NLoggerNames.Security, exp, exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.SecuritySettingsSaveFailed, 
                new String[]{ exp.toString() } );
        }
    }

    //////////////////////// Start Manager interface ///////////////////////////

    public static PhexSecurityManager getInstance()
    {
        if ( instance == null )
        {
            instance = new PhexSecurityManager();
        }
        return instance;
    }

    /**
     * This method is called in order to initialize the manager. This method
     * includes all tasks that must be done to intialize all the several manager.
     * Like instantiating the singleton instance of the manager. Inside
     * this method you can't rely on the availability of other managers.
     * @return true is initialization was successful, false otherwise.
     */
    public boolean initialize()
    {
        return true;
    }

    /**
     * This method is called in order to perform post initialization of the
     * manager. This method includes all tasks that must be done after initializing
     * all the several managers. Inside this method you can rely on the
     * availability of other managers.
     * @return true is initialization was successful, false otherwise.
     */
    public boolean onPostInitialization()
    {
        loadSecurityRuleList();
        return true;
    }
    
    /**
     * This method is called after the complete application including GUI completed
     * its startup process. This notification must be used to activate runtime
     * processes that needs to be performed once the application has successfully
     * completed startup.
     */
    public void startupCompletedNotify()
    {
    }

    /**
     * This method is called in order to cleanly shutdown the manager. It
     * should contain all cleanup operations to ensure a nice shutdown of Phex.
     */
    public void shutdown()
    {
        saveSecurityRuleList();
    }
    //////////////////////// End Manager interface ///////////////////////////

    ///////////////////// START event handling methods /////////////////////////

    /**
     * All listeners interested in events.
     */
    private ArrayList listenerList = new ArrayList( 2 );

    public void addUploadFilesChangeListener( SecurityRulesChangeListener listener )
    {
        listenerList.add( listener );
    }

    public void removeUploadFilesChangeListener( SecurityRulesChangeListener listener )
    {
        listenerList.remove( listener );
    }

    private void fireSecurityRuleChanged( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                SecurityRulesChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (SecurityRulesChangeListener)listeners[ i ];
                    listener.securityRuleChanged( position );
                }
            }
        });
    }

    private void fireSecurityRuleAdded( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                SecurityRulesChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (SecurityRulesChangeListener)listeners[ i ];
                    listener.securityRuleAdded( position );
                }
            }
        });
    }

    private void fireSecurityRuleRemoved( final int position )
    {
        // invoke update in event dispatcher
        AsynchronousDispatcher.invokeLater(
        new Runnable()
        {
            public void run()
            {
                Object[] listeners = listenerList.toArray();
                SecurityRulesChangeListener listener;
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length - 1; i >= 0; i-- )
                {
                    listener = (SecurityRulesChangeListener)listeners[ i ];
                    listener.securityRuleRemoved( position );
                }
            }
        });
    }

    public void fireSecurityRuleChanged( SecurityRule rule )
    {
        synchronized( ipAccessRuleList )
        {
            int position = ipAccessRuleList.indexOf( rule );
            if ( position >= 0 )
            {
                fireSecurityRuleChanged( position );
            }
        }
    }
    ///////////////////// END event handling methods ////////////////////////
}