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
 *  $Id: UploadZone.java,v 1.3 2005/10/03 00:18:26 gregork Exp $
 */
package phex.gui.statusbar;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import phex.common.Cfg;
import phex.common.bandwidth.BandwidthController;
import phex.common.bandwidth.BandwidthManager;
import phex.common.format.NumberFormatUtils;
import phex.gui.common.GUIRegistry;
import phex.statistic.StatisticProvider;
import phex.statistic.StatisticProviderConstants;
import phex.statistic.StatisticsManager;
import phex.upload.UploadManager;
import phex.utils.Localizer;

public class UploadZone extends JPanel
{
    private UploadManager uploadMgr;
    private StatisticsManager statsMgr;
    private BandwidthManager bwMgr;
    private JLabel uploadLabel;

    private JLabel bwLabel;

    public UploadZone()
    {
        super(  );
        SpringLayout layout = new SpringLayout();
        setLayout( layout );
        uploadMgr = UploadManager.getInstance();
        statsMgr = StatisticsManager.getInstance();
        bwMgr = BandwidthManager.getInstance();
        
        uploadLabel = new JLabel();
        uploadLabel.setIcon( GUIRegistry.getInstance().getIconFactory()
            .getIcon( "UploadSmall" ) );
        add( uploadLabel );
        
        bwLabel = new JLabel();
        add( bwLabel );
        
        updateZone();
        
        layout.putConstraint(SpringLayout.NORTH, uploadLabel, 3, SpringLayout.NORTH, this );
        layout.putConstraint(SpringLayout.WEST, uploadLabel, 5, SpringLayout.WEST, this );
        
        
        layout.putConstraint(SpringLayout.WEST, bwLabel, 5, SpringLayout.EAST, uploadLabel );
        layout.putConstraint(SpringLayout.NORTH, bwLabel, 3, SpringLayout.NORTH, this );
        
        layout.putConstraint(SpringLayout.EAST, this, 5, SpringLayout.EAST, bwLabel );
        //layout.putConstraint(SpringLayout.SOUTH, this, 2, SpringLayout.SOUTH, bwLabel );
    }

    public void updateZone()
    {
        StatisticProvider uploadCountProvider = statsMgr.getStatisticProvider(
            StatisticProviderConstants.SESSION_UPLOAD_COUNT_PROVIDER );
        Object[] args = new Object[]
        { 
            new Integer( uploadMgr.getUploadingCount() ),
            new Integer( uploadMgr.getUploadQueueSize() ),
            uploadCountProvider.getValue()
        };
        String text = Localizer.getFormatedString( "StatusBar_Uploads", args );
        uploadLabel.setText( text );
        uploadLabel.setToolTipText( Localizer.getString( "StatusBar_TTTUploads" ) );
        
        
        BandwidthController bwController = bwMgr.getUploadBandwidthController();
        long transferRate = bwController.getShortTransferAvg().getAverage();
        long throttlingRate = bwController.getThrottlingRate();
        String transferRateStr = NumberFormatUtils.formatSignificantByteSize(transferRate);
        String throttlingRateStr;
        if ( throttlingRate == Cfg.UNLIMITED_BANDWIDTH )
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
