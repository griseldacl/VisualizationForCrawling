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

public interface UploadConstants
{

    /*----------------------------------------------------------------
     * Range Constants
     */

    /**
     * Indicates that the range is available
     */
    public static final short RANGE_AVAILABLE = 0;

    /**
     * Indicates that the range is not available
     */
    public static final short RANGE_NOT_AVAILABLE = 1;

    /**
     * Indicates that the range is available
     */
    public static final short RANGE_NOT_SATISFIABLE = 2;


    /*----------------------------------------------------------------
     * Status Constants - Be aware to only add new status to the end of the integer
     *     list. The status is stored as value in the XML output. DON'T assign
     *     a different status to a already used value!!
     */

    /**
     * The status of a upload indicating that a upload is initializing.
     */
    public static final short STATUS_INITIALIZING = 0;

    /**
     * The status of a upload indicating that a upload is queued.
     */
    public static final short STATUS_QUEUED = 1;

    /**
     * The status of a upload indicating that a upload is in progress.
     */
    public static final short STATUS_UPLOADING = 2;

    /**
     * The status of a upload indicating that a upload is completed.
     */
    public static final short STATUS_COMPLETED = 3;

    /**
     * The status of a upload indicating that a upload is aborted.
     */
    public static final short STATUS_ABORTED = 4;


    /*----------------------------------------------------------------
     *Status Key Constants used to store localized status values
     */

    /**
     * The status key for the localized status string indicating that a download
     * file is downloading.
     */
    public static final String STATUS_INITIALIZING_KEY =
        "Initializing";

    /**
     * The status key for the localized status string indicating that a download
     * file is downloading.
     */
    public static final String STATUS_QUEUED_KEY =
        "UploadQueued";

    /**
     * The status key for the localized status string indicating that a download
     * file is downloading.
     */
    public static final String STATUS_UPLOADING_KEY =
        "Uploading";

    /**
     * The status key for the localized status string indicating that a download
     * file is downloading.
     */
    public static final String STATUS_COMPLETED_KEY =
        "Completed";

    /**
     * The status key for the localized status string indicating that a download
     * file is downloading.
     */
    public static final String STATUS_ABORTED_KEY =
        "Aborted";

    /**
     * The status key for the localized status string indicating that a download
     * file has an unrecognized status.
     */
    public static final String STATUS_UNRECOGNIZED_KEY = "UnrecognizedStatus";
}