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
package phex.query;

import phex.common.address.DestAddress;
import phex.connection.NetworkManager;
import phex.msg.GUID;
import phex.msg.QueryResponseMsg;
import phex.utils.VendorCodes;

/**
 * This class holds the available informations of a QueryHit for a host.
 * TODO currently it only contains the QHD informations. But it should be
 * extended so that not every RemoteFile for one host contains the informations
 * of the host.
 */
public class QueryHitHost implements QueryConstants
{
    /**
     * The unique identifier of the host.
     */
    private GUID hostGUID;

    /**
     * The speed of the host in Kbits/s.
     */
    private Long hostSpeedObj;

    /**
     * Defines if a push transfer is needed or not or unknown.
     */
    private short pushNeededFlag;

    /**
     * Defines if a server is busy currently or unknown.
     */
    private short serverBusyFlag;

    /**
     * Defines if a the server has already uploaded a file.
     */
    private short hasUploadedFlag;

    /**
     * Defines if the upload speed of a server is measured.
     */
    private short uploadSpeedFlag;

    /**
     * The vendor name.
     */
    private String vendor;

    /**
     * <p>States wether the servent supportes chat connections.</p>
     */
    private boolean isChatSupported;
	
	/**
     * <p>States wether the servent supportes thex specification.</p>
     */
	private boolean isThexSupported;
    
    /**
     * <p>States wether the servent supportes browse hosts connections.</p>
     */
    private boolean isBrowseHostSupported;

    /**
     * The rating of the host. It is determined from the QHD informations.
     */
    private Short hostRating;

    /**
     * The host address of the query hit host.
     */
    private DestAddress hostAddress;
    
    private DestAddress[] pushProxyAddresses;

    public QueryHitHost( GUID aHostGUID, DestAddress address, long aHostSpeed )
    {
        hostGUID = aHostGUID;
        hostAddress = address;
        hostSpeedObj =  new Long( aHostSpeed );
        setQHDFlags( QHD_UNKNOWN_FLAG, QHD_UNKNOWN_FLAG, QHD_UNKNOWN_FLAG,
            QHD_UNKNOWN_FLAG );
        hostRating = new Short( (short)-1 );
    }
    
    /**
     * Sets the vendor, chat, browse and push proxy field of the 
     * QueryHitHost from the query response message.
     * @param msg
     */
    public void setQueryResponseFields( QueryResponseMsg msg )
    {
        setVendorCode( msg.getVendorCode() );
        setChatSupported( msg.isChatSupported() );
        setBrowseHostSupported( msg.isBrowseHostSupported() );
        setPushProxyAddresses( msg.getPushProxyAddresses() );
    }

    /**
     * @return Returns the pushProxyAddresses.
     */
    public DestAddress[] getPushProxyAddresses()
    {
        return pushProxyAddresses;
    }
    
    /**
     * @param pushProxyAddresses The pushProxyAddresses to set.
     */
    public void setPushProxyAddresses(DestAddress[] pushProxyAddresses)
    {
        this.pushProxyAddresses = pushProxyAddresses;
    }
    
    /**
     * Sets the vendor code and stores it translated into the vendor name.
     */
    public void setVendorCode( String aVendorCode )
    {
        if ( aVendorCode != null )
        {
            vendor = VendorCodes.getVendorName( aVendorCode );
        }
    }

    /**
     * Returns the vendor name.
     */
    public String getVendor()
    {
        return vendor;
    }

    /**
     * <p>Sets wether the servent supportes a chat connections.</p>
     */
    public void setChatSupported( boolean state )
    {
        isChatSupported = state;
    }

    /**
     * <p>States wether the servent supportes chat connections.</p>
     *
     * @return true if a servent supports chat connections false otherwise.
     */
    public boolean isChatSupported( )
    {
        return isChatSupported;
    }

	/**
	 * <p>Sets wether the servent supportes thex specification.</p>
	 */
	public void setThexSupported( boolean state )
	{
	    isThexSupported = state;
	}

	/**
	 * <p>States wether the servent supportes thex specification.</p>
     *
     * @return true if a servent supports thex specification false otherwise.
     */
    public boolean isThexSupported( )
	{
        return isThexSupported;
	}
    
    /**
     * <p>Sets wether the servent supportes a browse host connections.</p>
     */
    public void setBrowseHostSupported( boolean state )
    {
        isBrowseHostSupported = state;
    }

    /**
     * <p>States wether the servent supportes browse host connections.</p>
     *
     * @return true if a servent supports browse hosts connections false otherwise.
     */
    public boolean isBrowseHostSupported( )
    {
        return isBrowseHostSupported;
    }

    /**
     * Returns the GUID of the host.
     */
    public GUID getHostGUID()
    {
        return hostGUID;
    }

    /**
     * @param guid
     */
    public void setHostGUID(GUID guid)
    {
        hostGUID = guid;
    }

    /**
     * Return the host speed as int value.
     *
     * @return the host speed
     */
    public int getHostSpeed()
    {
        return hostSpeedObj.intValue();
    }
    
    /**
     * @param i
     */
    public void setHostSpeed(long speed)
    {
        hostSpeedObj = new Long( speed );
        calculateHostRating();
    }


    /**
     * Return the host speed in Kbits as an Long instance.
     *
     * @return the remote host speed as an Integer
     */
    public Long getHostSpeedObject()
    {
        return hostSpeedObj;
    }

    /**
     * Returns the host address.
     */
    public DestAddress getHostAddress()
    {
        return hostAddress;
    }

    /**
     * Stores the QHD flags
     */
    public void setQHDFlags( short aPushNeededFlag, short aServerBusyFlag,
        short aHasUploadedFlag, short aUploadSpeedFlag )
    {
        pushNeededFlag = aPushNeededFlag;
        serverBusyFlag = aServerBusyFlag;
        hasUploadedFlag = aHasUploadedFlag;
        uploadSpeedFlag = aUploadSpeedFlag;
    }

    /**
     * Returns if a push is needed for the host. If the flag is set to unknown
     * then false is returned. The downloader should try a push after failed
     * normal connection.
     */
    public boolean isPushNeeded()
    {
        return pushNeededFlag == QHD_TRUE_FLAG;
    }

    /**
     * Returns the from the QHD calculated rating.
     */
    public short getHostRating()
    {
        if ( hostRating.shortValue() == -1 )
        {
            calculateHostRating();
        }
        return hostRating.shortValue();
    }

    public Short getHostRatingObject()
    {
        if ( hostRating.shortValue() == -1 )
        {
            calculateHostRating();
        }
        return hostRating;
    }

    /**
     * pushNeeded | serverBusy | I'm firewalled | useHasUploaded | Rating
     *         -1 |         -1 |                |              y |      6
     *         -1 |          0 |                |              y |      5
     *          0 |       0/-1 |             -1 |              y |      4
     *          1 |         -1 |             -1 |              y |      3
     *          0 |       0/-1 |              1 |              y |      3
     *          1 |          0 |             -1 |              y |      2
     *            |          1 |                |              n |      1
     *          1 |            |              1 |              n |      0
     *
     * If useHasUploaded is set to y in this chart then depending on the flag
     * the rating is raised.
     * hasUploaded == QHD_TRUE_FLAG -> Raiting + 2
     * hasUploaded == QHD_UNKNOWN_FLAG -> Raiting + 1
     * hasUploaded == QHD_FALSE_FLAG -> Raiting + 0
     */
    private void calculateHostRating()
    {
        // TODO use isPushRecommended information to rate.
        // A push is recommended if the pushNeededFlag is set or the host has a
        // private host address and we are in a lan.


        // remote host and local host is firewalled... download not possible
        NetworkManager networkMgr = NetworkManager.getInstance();
        if (pushNeededFlag == QHD_TRUE_FLAG
            && !networkMgr.hasConnectedIncoming())
        {
            hostRating = new Short( (short)0 );
            return;
        }
        // servent is busy
        if ( serverBusyFlag == QHD_TRUE_FLAG )
        {
            hostRating = new Short( (short)1 );
            return;
        }

        // complex ratings influenced by hasUploaded
        short tmpHostRating;
        // remote is firewalled i'm not firewalled
        if ( pushNeededFlag == QHD_TRUE_FLAG )
        {
            if ( serverBusyFlag == QHD_FALSE_FLAG )
            {
                tmpHostRating = 3;
            }
            else // serverBusyFlag == QHD_UNKNOWN_FLAG
            {
                tmpHostRating = 2;
            }
        }
        else if ( pushNeededFlag == QHD_FALSE_FLAG )
        {
            if ( serverBusyFlag == QHD_FALSE_FLAG )
            {
                tmpHostRating = 6;
            }
            else // serverBusyFlag == QHD_UNKNOWN_FLAG
            {
                tmpHostRating = 5;
            }
        }
        else // pushNeededFlag == QHD_UNKNOWN_FLAG
        {// serverBusyFlag != QHD_TRUE_FLAG
            if ( !networkMgr.hasConnectedIncoming() )
            {// I'm firewalled
                tmpHostRating = 3;
            }
            else
            {// I'm not firewalled
                tmpHostRating = 4;
            }
        }

        if ( hasUploadedFlag == QHD_TRUE_FLAG )
        {
            tmpHostRating += 2;
        }
        else if ( hasUploadedFlag == QHD_UNKNOWN_FLAG )
        {
            tmpHostRating += 1;
        }
        hostRating = new Short( tmpHostRating );
    }
}