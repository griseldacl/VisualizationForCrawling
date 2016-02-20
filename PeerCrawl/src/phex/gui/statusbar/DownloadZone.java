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
 *  Created on 11.08.2005
 *  --- CVS Information ---
 *  $Id: DownloadZone.java,v 1.3 2005/10/03 00:18:26 gregork Exp $
 */
package phex.gui.statusbar;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import phex.common.Cfg;
import phex.common.bandwidth.BandwidthController;
import phex.common.bandwidth.BandwidthManager;
import phex.common.format.NumberFormatUtils;
import phex.download.swarming.SWDownloadConstants;
import phex.download.swarming.SwarmingManager;
import phex.gui.common.GUIRegistry;
import phex.statistic.StatisticsManager;
import phex.utils.Localizer;

public class DownloadZone extends JPanel
{
    private SwarmingManager swarmingMgr;
    private StatisticsManager statsMgr;
    private BandwidthManager bwMgr;
    
    private JLabel downloadLabel;
    private JLabel bwLabel;

    public DownloadZone()
    {
        super(  );
        SpringLayout layout = new SpringLayout();
        setLayout( layout );
        swarmingMgr = SwarmingManager.getInstance();
        statsMgr = StatisticsManager.getInstance();
        bwMgr = BandwidthManager.getInstance();
        
        downloadLabel = new JLabel();
        downloadLabel.setIcon( GUIRegistry.getInstance().getIconFactory()
            .getIcon( "DownloadSmall" ) );
        add( downloadLabel );
        
        bwLabel = new JLabel();
        add( bwLabel );
        
        updateZone();
        
        layout.putConstraint(SpringLayout.WEST, downloadLabel, 5, SpringLayout.WEST, this );
        layout.putConstraint(SpringLayout.NORTH, downloadLabel, 3, SpringLayout.NORTH, this );
        
        layout.putConstraint(SpringLayout.WEST, bwLabel, 5, SpringLayout.EAST, downloadLabel );
        layout.putConstraint(SpringLayout.NORTH, bwLabel, 3, SpringLayout.NORTH, this );
        
        layout.putConstraint(SpringLayout.EAST, this, 5, SpringLayout.EAST, bwLabel );
        //layout.putConstraint(SpringLayout.SOUTH, this, 2, SpringLayout.SOUTH, downloadLabel );
    }

    public void updateZone()
    {
        Object[] args = new Object[]
        {
            new Integer( swarmingMgr.getDownloadFileCount(
                SWDownloadConstants.STATUS_FILE_DOWNLOADING ) ),
            new Integer( swarmingMgr.getDownloadFileCount() ),
            new Integer( swarmingMgr.getDownloadFileCount(
                SWDownloadConstants.STATUS_FILE_COMPLETED_MOVED ) )
        };
        String text = Localizer.getFormatedString( "StatusBar_Downloads", args );
        downloadLabel.setText( text );
        downloadLabel.setToolTipText( Localizer.getString( "StatusBar_TTTDownloads" ) );
        
        BandwidthController bwController = bwMgr.getDownloadBandwidthController();
        long transferRate = bwController.getShortTransferAvg().getAverage();
        long throttlingRate = bwController.getThrottlingRate();
        String transferRateStr = NumberFormatUtils.formatSignificantByteSize(transferRate);
        String throttlingRateStr;
        if ( throttlingRate >= Cfg.UNLIMITED_BANDWIDTH )
        {
            throttlingRateStr = Localizer.getDecimalFormatSymbols().getInfinity();
        }
        else
        {
            throttlingRateStr = NumberFormatUtils.formatSignificantByteSize(throttlingRate);
        }
        bwLabel.setText( transferRateStr + Localizer.getString("PerSec") + " ("
            + throttlingRateStr + ")");
        
        validate();
    }
    
}
