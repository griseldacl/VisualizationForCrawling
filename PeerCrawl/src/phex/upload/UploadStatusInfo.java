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
package phex.upload;

import phex.utils.*;

public final class UploadStatusInfo implements UploadConstants
{
    // dont allow to create instances
    private UploadStatusInfo()
    {
    }

    /**
     * Returns a localized string for the given status of a download file.
     *
     * @param status the status to get the string representation for.
     * @return the status string representation.
     */
    public static String getUploadStatusString( short status )
    {
        switch( status )
        {
            case STATUS_INITIALIZING:
                return Localizer.getString( STATUS_INITIALIZING_KEY );
            case STATUS_QUEUED:
                return Localizer.getString( STATUS_QUEUED_KEY );
            case STATUS_COMPLETED:
                return Localizer.getString( STATUS_COMPLETED_KEY );
            case STATUS_UPLOADING:
                return Localizer.getString( STATUS_UPLOADING_KEY );
            case STATUS_ABORTED:
                return Localizer.getString( STATUS_ABORTED_KEY );
            default:
                Object[] arguments = new Object[1];
                arguments[0] = new Integer( status );
                return Localizer.getFormatedString( STATUS_UNRECOGNIZED_KEY,
                    arguments );
        }
    }
}