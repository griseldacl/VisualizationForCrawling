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
 *  $Id: SWDownloadInfo.java,v 1.16 2005/10/03 00:18:25 gregork Exp $
 */
package phex.download.swarming;

import phex.common.format.TimeFormatUtils;
import phex.utils.Localizer;

public final class SWDownloadInfo implements SWDownloadConstants
{
    // dont allow to create instances
    private SWDownloadInfo()
    {
    }

    /**
     * Returns a localized string for the given status of a download file.
     */
    public static String getDownloadFileStatusString( int status )
    {
        switch( status )
        {
            case STATUS_FILE_WAITING:
                return Localizer.getString( STATUS_FILE_WAITING_KEY );
            case STATUS_FILE_DOWNLOADING:
                return Localizer.getString( STATUS_FILE_DOWNLOADING_KEY );
            case STATUS_FILE_COMPLETED:
            case STATUS_FILE_COMPLETED_MOVED:
                return Localizer.getString( STATUS_FILE_COMPLETED_KEY );
            case STATUS_FILE_STOPPED:
                return Localizer.getString( STATUS_FILE_STOPPED_KEY );
            case STATUS_FILE_QUEUED:
                return Localizer.getString( STATUS_FILE_QUEUED_KEY );
            default:
                Object[] arguments = new Object[1];
                arguments[0] = new Integer( status );
                return Localizer.getFormatedString( STATUS_UNRECOGNIZED_KEY,
                    arguments );
        }
    }

    /**
     * Returns a localized string for the given status of a download candidate.
     * @param statusTimeout the time in seconds until the status times out
     */
    public static String getDownloadCandidateStatusString(
        SWDownloadCandidate candidate )
    {
        Object[] arguments;
        int status = candidate.getStatus();
        switch( status )
        {
            case STATUS_CANDIDATE_IGNORED:
                return Localizer.getString( STATUS_CANDIDATE_IGNORED_KEY )
                    + " (" + Localizer.getString( candidate.getStatusReason() ) + ").";
            case STATUS_CANDIDATE_BAD:
                arguments = new Object[2];
                arguments[0] = new Long( candidate.getStatusTimeLeft() / 1000 );
                arguments[1] = TimeFormatUtils.convertSecondsToTime(
                    (int)(candidate.getStatusTimeLeft() / 1000 ) );
                return Localizer.getFormatedString( STATUS_CANDIDATE_BAD_KEY,
                    arguments );
            case STATUS_CANDIDATE_WAITING:
                return Localizer.getString( STATUS_CANDIDATE_WAITING_KEY );
            case STATUS_CANDIDATE_BUSY:
                arguments = new Object[2];
                arguments[0] = new Long( candidate.getStatusTimeLeft() / 1000 );
                arguments[1] = TimeFormatUtils.convertSecondsToTime(
                    (int)(candidate.getStatusTimeLeft() / 1000 ) );
                return Localizer.getFormatedString( STATUS_CANDIDATE_BUSY_KEY,
                    arguments );
            case STATUS_CANDIDATE_CONNECTING:
                arguments = new Object[1];
                arguments[0] = new Long( candidate.getStatusTimeLeft() / 1000 );
                return Localizer.getFormatedString( STATUS_CANDIDATE_CONNECTING_KEY,
                    arguments );
            case STATUS_CANDIDATE_RANGE_UNAVAILABLE:
                arguments = new Object[1];
                arguments[0] = TimeFormatUtils.convertSecondsToTime(
                    (int)(candidate.getStatusTimeLeft() / 1000) );
                return Localizer.getFormatedString( STATUS_CANDIDATE_RANGE_UNAVAILABLE_KEY,
                    arguments );
            case STATUS_CANDIDATE_REMOTLY_QUEUED:
                arguments = new Object[3];
                arguments[0] = candidate.getXQueueParameters().getPosition();
                arguments[1] = TimeFormatUtils.convertSecondsToTime(
                    (int)(candidate.getStatusTimeLeft() / 1000 ) );
                return Localizer.getFormatedString( STATUS_CANDIDATE_REMOTLY_QUEUED_KEY,
                    arguments );
            case STATUS_CANDIDATE_CONNECTION_FAILED:
                arguments = new Object[3];
                arguments[0] = new Integer( candidate.getFailedConnectionTries() );
                arguments[1] = new Long( candidate.getStatusTimeLeft() / 1000 );
                arguments[2] = TimeFormatUtils.convertSecondsToTime(
                    (int)(candidate.getStatusTimeLeft() / 1000 ) );
                return Localizer.getFormatedString( STATUS_CANDIDATE_CONNECTION_FAILED_KEY,
                    arguments );
            case STATUS_CANDIDATE_PUSH_REQUEST:
                arguments = new Object[1];
                arguments[0] = new Long( candidate.getStatusTimeLeft() / 1000 );
                return Localizer.getFormatedString( STATUS_CANDIDATE_PUSH_REQUEST_KEY,
                    arguments );
            case STATUS_CANDIDATE_REQUESTING:
                return Localizer.getFormatedString( STATUS_CANDIDATE_REQUESTING_KEY,
                    null );
            case STATUS_CANDIDATE_DOWNLOADING:
                return Localizer.getFormatedString( STATUS_CANDIDATE_DOWNLOADING_KEY,
                    null );
            default:
                arguments = new Object[1];
                arguments[0] = new Integer( status );
                return Localizer.getFormatedString( STATUS_UNRECOGNIZED_KEY,
                    arguments );
        }
    }
}