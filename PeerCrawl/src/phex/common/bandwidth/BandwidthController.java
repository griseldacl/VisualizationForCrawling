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
 *  $Id: BandwidthController.java,v 1.18 2005/10/08 17:59:48 gregork Exp $
 */
package phex.common.bandwidth;

import phex.utils.NLogger;
import phex.utils.NLoggerNames;

/**
 * A class that units a clamping bandwidth throttle with memory and a simple
 * current/avg. bandwith tracker.
 * <p>
 * Bandwidth generally does not exceed set value within a one second period.
 * Excess bandwidth is partially available for the
 * next period, exceeded bandwidth is fully unavailble for the next period.
 */
public class BandwidthController
{
    private static final int WINDOWS_PER_SECONDS = 5;

    private static final int MILLIS_PER_WINDOW = 1000 / WINDOWS_PER_SECONDS;
    
    /**
     * The number of bytes each window has.
     */
    private int bytesPerWindow;

    /**
     * The number of bytes left in the current window
     */
    private int bytesRemaining;

    /**
     * The timestamp of the start of the current window.
     */
    private long lastWindowTime;

    /**
     * The maximal rate in bytes per second.
     */
    private long throttlingRate;

    /**
     * The name of this BandwidthController.
     */
    private final String controllerName;

    /**
     * To measure bandwidth in different levels BandwidthControllers can be
     * chained together so that a throttling would have to pass all controllers
     * and tracking can be done in higher levels too.
     */
    private BandwidthController nextContollerInChain = null;
    
    private TransferAverage shortTransferAvg;
    private TransferAverage longTransferAvg;

    /**
     * Create a new bandwidth controller through acquireController()
     * @param controllerName the name of this BandwidthController.
     * @param throttlingRate the used throttling rate in bytes per second.
     */
    private BandwidthController(String controllerName, long throttlingRate)
    {
        this.controllerName = controllerName + " "
            + Integer.toHexString(hashCode());
        setThrottlingRate(throttlingRate);
        // init the bytes remaining on start to ensure correct stats on start.
        bytesRemaining = bytesPerWindow;
    }
    
    public synchronized void activateShortTransferAvg( int refreshRate, int period )
    {
        shortTransferAvg = new TransferAverage( refreshRate, period );
    }
    
    public synchronized void activateLongTransferAvg( int refreshRate, int period )
    {
        longTransferAvg = new TransferAverage( refreshRate, period );
    }
    
    public TransferAverage getShortTransferAvg()
    {
        return shortTransferAvg;
    }
    
    public TransferAverage getLongTransferAvg()
    {
        return longTransferAvg;
    }

    /**
     * Specify another controller to be automatically always called after this one.
     *
     * @return the old BandwidthController it was linked with.
     */
    public synchronized BandwidthController linkControllerIntoChain(
        BandwidthController toLink)
    {
        BandwidthController temp = nextContollerInChain;
        nextContollerInChain = toLink;
        return temp;
    }

    /**
     * Call to set the desired throttling rate.
     */
    public synchronized void setThrottlingRate(long bytesPerSecond)
    {
        throttlingRate = bytesPerSecond;
        bytesPerWindow = (int) ((double) throttlingRate / (double) WINDOWS_PER_SECONDS);
        if ( NLogger.isDebugEnabled( NLoggerNames.BANDWIDTH ) )
            NLogger.debug(NLoggerNames.BANDWIDTH, 
                "["+controllerName + "] Set throttling rate to " + bytesPerSecond + "bps (" + bytesPerWindow + " per window)");
        bytesRemaining = Math.min( bytesRemaining, bytesPerWindow );
    }
    
    /**
     * Returns the throttling rate in bytes per seconds
     * @return
     */
    public long getThrottlingRate()
    {
        return throttlingRate;
    }
    
    /**
     * Returns the max number of bytes available through this bandwidth controller
     * and its parents.
     * @return
     */
    public synchronized int getAvailableByteCount( boolean blockTillAvailable )
    {
        updateWindow( blockTillAvailable );
        int bytesAllowed = bytesRemaining;
        // If there is another controller we are chained to, call it.
        if( nextContollerInChain != null )
        {
            bytesAllowed = Math.min( bytesAllowed,
                nextContollerInChain.getAvailableByteCount( blockTillAvailable ) );
        }
        if ( NLogger.isDebugEnabled( NLoggerNames.BANDWIDTH ) )
            NLogger.debug(NLoggerNames.BANDWIDTH, 
                "["+controllerName + "] Available byte count " + bytesAllowed 
                + "bps - Remaining: " + bytesRemaining + ".");
        return bytesAllowed;
    }
    
    /**
     * Returns the max number of bytes available through this bandwidth controller
     * and its parents.
     * @return
     */
    public synchronized int getAvailableByteCount( int maxToSend, 
        boolean blockTillAvailable )
    {
        return Math.min( maxToSend, getAvailableByteCount(blockTillAvailable) );
    }
    
    /**
     * Marks bytes as used.
     * @param byteCount
     */
    public synchronized void markBytesUsed( int byteCount )
    {
        updateWindow( false );
        bytesRemaining -= byteCount;
        if  ( bytesRemaining < 0 )
        {
            updateWindow( true );
        }
        
        if ( NLogger.isDebugEnabled( NLoggerNames.BANDWIDTH ) )
            NLogger.debug(NLoggerNames.BANDWIDTH, 
                "["+controllerName + "] Mark bytes used " + byteCount + " - remaining: " + bytesRemaining + ".");
        
        if ( shortTransferAvg != null )
        {
            shortTransferAvg.addValue( byteCount );
        }
        if ( longTransferAvg != null )
        {
            longTransferAvg.addValue( byteCount );
        }
        // If there is another controller we are chained to, call it.
        if( nextContollerInChain != null )
        {
            nextContollerInChain.markBytesUsed( byteCount );
        }
    }
    
    private void updateWindow( boolean blockTillAvailable )
    {
        boolean wasInterrupted = false;
        long elapsedWindowMillis;
        long now;
        while ( true )
        {
            now = System.currentTimeMillis();
            elapsedWindowMillis = now - lastWindowTime;
            if (elapsedWindowMillis >= MILLIS_PER_WINDOW )
            {
                // last window used up too many bytes... 
                if ( bytesRemaining < 0 )
                {
                    bytesRemaining += bytesPerWindow;
                }
                else
                {
                    bytesRemaining = bytesPerWindow; 
                }                
                lastWindowTime = now;
                if ( NLogger.isDebugEnabled( NLoggerNames.BANDWIDTH ) )
                    NLogger.debug(NLoggerNames.BANDWIDTH, 
                        "["+controllerName + "] Update new Window " + bytesPerWindow 
                        + " - Remaining: " + bytesRemaining + ".");
            }
            if ( !blockTillAvailable || bytesRemaining > 0 )
            {
                break;
            }
            try
            {
                Thread.sleep( Math.max( 
                    MILLIS_PER_WINDOW - elapsedWindowMillis, 0 ) );
            }
            catch (InterruptedException e)
            {
                wasInterrupted = true;
                break;
            }
        }
        if ( wasInterrupted )
        {//reset interrupted
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Returns the name of this BandwidthController.
     * @return the name.
     */
    public String getName()
    {
        return controllerName;
    }

    /**
     * Returns a debug string of this BandwidthController.
     * @return a debug string.
     */
    public String toDebugString()
    {
        return "ThrottleController[Name:" + controllerName + 
            ",bytesPerWindow:" + bytesPerWindow + ",bytesRemaining:" + bytesRemaining 
            //+ ",Rate:" + getValue() + ",Avg:" + getAverageValue()
            ;
    }

    /**
     * Returns a BandwidthController object which can be used as a bandwidth
     * tracker and throttle.
     * @param controllerName the name of BandwidthController to create.
     * @param throttlingRate the used throttling rate in bytes per second.
     */
    public static BandwidthController acquireBandwidthController(
        String controllerName, long throttlingRate)
    {
        return new BandwidthController(controllerName, throttlingRate);
    }

    /**
     * Release any resources associated with the controller. Once this is called,
     * the controller should no longer be used and should be dereferenced.
     * Must be called only once for each controller.
     * Be sure that any linked throttles that are to be disposed of are also
     * disposed of before calling this method.
     */
    public static void releaseController(BandwidthController controller)
    {
        controller.nextContollerInChain = null;
    }
}