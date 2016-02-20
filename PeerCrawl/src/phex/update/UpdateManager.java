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
 *  $Id: UpdateManager.java,v 1.10 2005/10/03 00:18:29 gregork Exp $
 */
package phex.update;

import phex.common.*;
import phex.event.UpdateNotificationListener;

/**
 * This new update manager class introduces the new more complex Phex update
 * check and information transmit model.
 * To identify the same Phex installation over time the persistent host GUID is
 * transmited as a identifier with the request.
 * Phex informations are transmitted in an XML structure, the update information
 * response is also returned in an XML structure.<p>
 * Phex update request format:<br>
 * <pre>
 * &lt;phex&gt;
 *   &lt;update-request&gt;
 *     &lt;hostid&gt;468EE01D8B2F87D5FF9A57679B3DD200&lt;/hostid&gt;
 *     &lt;current-version&gt;0.9.0.50&lt;/current-version&gt;
 *     &lt;skin-laf-installed&gt;false&lt;/current-version&gt;
 *     &lt;operating-system&gt;Windows 2000&lt;/operating-system&gt;
 *     &lt;java-version&gt;1.3&lt;/operating-system&gt;
 *     &lt;avg-uptime&gt;2888649&lt;/avg-uptime&gt;
 *     &lt;download-count&gt;10&lt;/download-count&gt;
 *     &lt;upload-count&gt;10&lt;/upload-count&gt;
 *     &lt;shared-files&gt;100&lt;/shared-files&gt;
 *     &lt;shared-size&gt;100000&lt;/shared-size&gt; 
 *     &lt;last-check-version&gt;0.8.0&lt;/last-check-version&gt;
 *     &lt;last-info-id&gt;1&lt;/last-info-id&gt;
 *     &lt;show-beta-info&gt;true&lt;/show-beta-info&gt;
 *   &lt;/update-request&gt;
 * &lt;/phex&gt;
 * </pre>
 * Phex update response format:<br>
 * <pre>
 * &lt;phex&gt;
 *   &lt;update-response&gt;
 *     &lt;version id="0.9.5.60" isBeta="true"&gt;
 *       &lt;text&gt;Changes bla bla&lt;/text&gt;
 *     &lt;/version&gt;
 *     &lt;info id="2"&gt;
 *       &lt;header&gt;A importent info.&lt;/header&gt;
 *       &lt;text&gt;Please support Phex.&lt;/text&gt;
 *     &lt;/info&gt;
 *   &lt;update-response&gt;
 * &lt;/phex&gt;
 * </pre>
 */
public class UpdateManager implements Manager
{
    private static final long ONE_WEEK_MILLIS = 1000 * 60 * 1440 * 7;
    private static UpdateManager instance;
    
    public UpdateManager()
    {
    }
    
    public static UpdateManager getInstance()
    {
        if ( instance == null )
        {
            instance = new UpdateManager();
        }
        return instance;
    }
    
    /**
     * @see phex.common.Manager#initialize()
     */
    public boolean initialize()
    {
        return false;
    }

    /**
     * @see phex.common.Manager#onPostInitialization()
     */
    public boolean onPostInitialization()
    {
        return false;
    }

    /**
     * @see phex.common.Manager#startupCompletedNotify()
     */
    public void startupCompletedNotify()
    {
    }

    /**
     * @see phex.common.Manager#shutdown()
     */
    public void shutdown(){}

    /**
     * Trigger a automated background update check. This is the standard Phex
     * check done every week to display the update dialog or just collect Phex
     * statistics.
     */
    public void triggerAutoBackgroundCheck(
        final UpdateNotificationListener updateListener )
    {
        if ( ServiceManager.sCfg.lastUpdateCheckTime >
            System.currentTimeMillis() - ONE_WEEK_MILLIS )
        {
            return;
        }
        Runnable updateCheckTrigger = new Runnable()
        {
            public void run()
            {
                // before we set of the update check we will wait 1 minute...
                // this will help manager to initialize (file scan) and reduce
                // update checks on short Phex session lifetimes...
                try
                {
                    Thread.sleep( 60 * 1000 );
                }
                catch (InterruptedException e)
                {
                }
                UpdateCheckRunner runner = new UpdateCheckRunner( updateListener );
                Thread thread = new Thread( ThreadTracking.rootThreadGroup,
                    runner, "UpdateCheckRunner" );
                thread.setPriority( Thread.MIN_PRIORITY );
                thread.setDaemon( true );
                thread.start();
            }
        };
        ThreadPool.getInstance().addJob(updateCheckTrigger, "UpdateCheckTrigger" );
    }    
}