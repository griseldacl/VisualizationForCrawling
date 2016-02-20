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
 *  $Id: BandwidthManager.java,v 1.11 2005/10/03 00:18:22 gregork Exp $
 */
package phex.common.bandwidth;

import phex.common.*;

/**
 * A Manager class that manages all bandwidth controllers
 * and allows fast access to the controllers.
 */
public class BandwidthManager implements Manager
{
    private static BandwidthManager instance;   

    private BandwidthController phexBandwidthController;
    private BandwidthController networkBandwidthController;
    private BandwidthController downloadBandwidthController;
    private BandwidthController uploadBandwidthController;

    public void setDownloadBandwidth(int newDownloadBwInBytes)
    {
        ServiceManager.sCfg.mDownloadMaxBandwidth = newDownloadBwInBytes;
        downloadBandwidthController.setThrottlingRate(newDownloadBwInBytes);
    }

    public void setNetworkBandwidth(int newNetworkBwInBytes)
    {
        ServiceManager.sCfg.mNetMaxRate = newNetworkBwInBytes;
        networkBandwidthController.setThrottlingRate(newNetworkBwInBytes);
    }
    
    public void setPhexTotalBandwidth(int newPhexBwInBytes)
    {
        ServiceManager.sCfg.maxTotalBandwidth = newPhexBwInBytes;
        phexBandwidthController.setThrottlingRate(newPhexBwInBytes);
    }

    public void setUploadBandwidth(int newUploadBwInBytes)
    {
        ServiceManager.sCfg.mUploadMaxBandwidth = newUploadBwInBytes;
        uploadBandwidthController.setThrottlingRate(newUploadBwInBytes);
    }

    public BandwidthController getPhexBandwidthController()
    {
        return phexBandwidthController;
    }

    public BandwidthController getNetworkBandwidthController()
    {
        return networkBandwidthController;
    }

    public BandwidthController getDownloadBandwidthController()
    {
        return downloadBandwidthController;
    }

    public BandwidthController getUploadBandwidthController()
    {
        return uploadBandwidthController;
    }
    
    //////////////////////// Start Manager interface ///////////////////////////

    public static BandwidthManager getInstance()
    {
        if (instance == null)
        {
            instance = new BandwidthManager();
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
        phexBandwidthController =
            BandwidthController.acquireBandwidthController("PhexThrottle", 
            ServiceManager.sCfg.maxTotalBandwidth);
        phexBandwidthController.activateShortTransferAvg(1000, 5);
        phexBandwidthController.activateLongTransferAvg(2000, 90);
            
        networkBandwidthController =
            BandwidthController.acquireBandwidthController("NetworkThrottle",
            ServiceManager.sCfg.mNetMaxRate );
        networkBandwidthController.activateShortTransferAvg(1000, 5);
        networkBandwidthController.activateLongTransferAvg(2000, 90);
        networkBandwidthController.linkControllerIntoChain(
            phexBandwidthController);

        downloadBandwidthController =
            BandwidthController.acquireBandwidthController("DownloadThrottle",
            ServiceManager.sCfg.mDownloadMaxBandwidth );
        downloadBandwidthController.activateShortTransferAvg(1000, 5);
        downloadBandwidthController.activateLongTransferAvg(2000, 90);
        downloadBandwidthController.linkControllerIntoChain(
            phexBandwidthController);

        uploadBandwidthController =
            BandwidthController.acquireBandwidthController("UploadThrottle", 
            ServiceManager.sCfg.mUploadMaxBandwidth );
        uploadBandwidthController.activateShortTransferAvg(1000, 5);
        uploadBandwidthController.activateLongTransferAvg(2000, 90);
        uploadBandwidthController.linkControllerIntoChain(
            phexBandwidthController);
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
//        BandwidthUseChangeHandler listener = new BandwidthUseChangeHandler();
//        UploadManager.getInstance().addUploadFilesChangeListener( listener );
//        SwarmingManager.getInstance().addDownloadFilesChangeListener( listener );
//        HostManager.getInstance().getNetworkHostsContainer().addNetworkHostsChangeListener(
//            listener );
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
        BandwidthController.releaseController( downloadBandwidthController );
        BandwidthController.releaseController( uploadBandwidthController );
        BandwidthController.releaseController( networkBandwidthController );
        BandwidthController.releaseController( phexBandwidthController );
    }
    //////////////////////// End Manager interface ///////////////////////////
    
    //////////////////////// Start Listeners /////////////////////////////////
    
//    private class BandwidthUseChangeHandler implements UploadFilesChangeListener,
//        DownloadFilesChangeListener, NetworkHostsChangeListener
//    {
//        private final UploadManager uploadMgr = UploadManager.getInstance();
//        private final SwarmingManager swarmingMgr = SwarmingManager.getInstance();
//        private final HostManager hostMgr = HostManager.getInstance();
//        
//        /**
//         * @see phex.event.UploadFilesChangeListener#uploadFileChanged(int)
//         */
//        public void uploadFileChanged(int position)
//        {
//            int count = uploadMgr.getUploadingCount();
//            if ( count == 0 )
//            {// stop controller since its not used anyway..
//             // the controller is automatically restarted once it is used again.
//                uploadBandwidthController.stopController();
//            }
//        }
//
//        /**
//         * @see phex.event.DownloadFilesChangeListener#downloadFileChanged(int)
//         */
//        public void downloadFileChanged(int position)
//        {
//            int count = swarmingMgr.getDownloadFileCount(
//                SWDownloadConstants.STATUS_FILE_DOWNLOADING );
//            if ( count == 0 )
//            {// stop controller since its not used anyway..
//             // the controller is automatically restarted once it is used again.
//                downloadBandwidthController.stopController();
//            }
//        }
//        
//        /**
//         * @see phex.event.NetworkHostsChangeListener#networkHostChanged(int)
//         */
//        public void networkHostChanged(int position)
//        {
//            int count = hostMgr.getNetworkHostsContainer().getNetworkHostCount();
//            if ( count == 0 )
//            {// stop controller since its not used anyway..
//             // the controller is automatically restarted once it is used again.
//                networkBandwidthController.stopController();
//            }
//        }
//
//        public void downloadFileAdded(int position)
//        {}
//        public void downloadFileRemoved(int position)
//        {}
//        public void uploadFileAdded(int position)
//        {}
//        public void uploadQueueChanged()
//        {}
//        public void uploadFileRemoved(int position)
//        {}
//        public void networkHostAdded(int position)
//        {}
//        public void networkHostRemoved(int position)
//        {}
//    }
    
    /////////////////////////// End Listeners //////////////////////////////
}
