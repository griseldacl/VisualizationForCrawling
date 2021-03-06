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
 *  $Id: FWTab.java,v 1.16 2005/10/03 00:18:26 gregork Exp $
 */
package phex.gui.tabs;

import java.util.HashMap;
import java.util.Iterator;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.xml.bind.JAXBException;

import phex.gui.actions.FWAction;
import phex.gui.actions.FWToggleAction;
import phex.gui.actions.ToggleTabViewAction;
import phex.xml.ObjectFactory;
import phex.xml.XJBGUISettings;
import phex.xml.XJBGUITab;



/**
 * Base class for all tabs.
 */
public class FWTab extends JPanel
{
    /**
     * Contains the actions of this tab together with a retrieval key.
     */
    private HashMap tabActionMap;

    /**
     * The name of the tab in the tabbed pane.
     */
    private String name;

    /**
     * The icon of the tab in the tabbed pane.
     */
    private Icon icon;

    /**
     * The tool tip text of the tab in the tabbed pane.
     */
    private String toolTip;

    /**
     * The mnemonic of the tab.
     */
    private char mnemonic;

    /**
     * The accelerator of the tab.
     */
    private KeyStroke accelerator;

    /**
     * The position of the tab in the tabbed pane.
     */
    private int index;

    /**
     * The unique id of the tab.
     */
    private int tabID;

    private ToggleTabViewAction toggleTabViewAction;

    public FWTab( int aTabID, String aName, Icon aIcon,
        String aToolTip, char mnemonic, KeyStroke accelerator, int aIndex)
    {
        tabID = aTabID;
        name = aName;
        icon = aIcon;
        toolTip = aToolTip;
        index = aIndex;
        this.mnemonic = mnemonic;
        this.accelerator = accelerator;
        tabActionMap = new HashMap();
    }

    public String getName()
    {
        return name;
    }

    public Icon getIcon()
    {
        return icon;
    }

    public char getMnemonic()
    {
        return mnemonic;
    }

    public KeyStroke getAccelerator()
    {
        return accelerator;
    }

    public String getToolTip()
    {
        return toolTip;
    }

    public int getIndex()
    {
        return index;
    }
    
    /**
     * Indicates if this tab is visible by default, when there is no known 
     * visible setting from the user.
     * @return true if visible by default false otherwise.
     */
    public boolean isVisibleByDefault()
    {
        return true;
    }
    
    /**
     * This provides a trade-off between the tab hiding and the tab destroy.
     * The <code>start()</code> (<code>stop()</code>) method 
     * creates (destroys) some objects
     * and starts (stops) some subscribes.
     * This allows deeper memory and cpu saves than the <code>setVisible()</code> method.
     * This is not designed to be called often.
     * @see <code>#stop()</code>
     */
    /*public void start()
    {
    }*/
    
    /**
     * This provides a trade-off between the tab hiding and the tab destroy.
     * @see <code>#start()</code>
     */
    /*public void stop()
    {
    }*/


    /**
     * Method is called when the tab will be selected in the tabbed pane. Can be
     * overloaded to do some action.
     */
    public void tabSelectedNotify()
    {
    }

    public FWToggleAction getToggleTabViewAction()
    {
        if ( toggleTabViewAction == null )
        {
            toggleTabViewAction = new ToggleTabViewAction( this );
        }
        return toggleTabViewAction;
    }

    public void appendXJBGUISettings( XJBGUISettings xjbGUISettings )
        throws JAXBException
    {
        ObjectFactory objFactory = new ObjectFactory();
        XJBGUITab xjbTab = objFactory.createXJBGUITab();
        xjbTab.setTabID( tabID );
        // only store visible state if not default value.
        boolean visibleState = getParent() != null;
        if ( visibleState != isVisibleByDefault() )
        {
            xjbTab.setVisible( getParent() != null );
        }
        xjbGUISettings.getTabList().add( xjbTab );
    }

    public void addTabAction( FWAction action )
    {
        tabActionMap.put( action, action );
    }

    public void addTabAction( String key, FWAction action )
    {
        tabActionMap.put( key, action );
    }

    public FWAction getTabAction( String key )
    {
        return (FWAction)tabActionMap.get( key );
    }

    public void refreshTabActions()
    {
        Iterator iterator = tabActionMap.values().iterator();
        while ( iterator.hasNext() )
        {
            FWAction action = (FWAction)iterator.next();
            action.refreshActionState();
        }
    }
}