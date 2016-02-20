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
 *  $Id: NLoggerNames.java,v 1.30 2005/10/03 00:18:29 gregork Exp $
 */
package phex.utils;

/**
 *
 */
public interface NLoggerNames
{
    public static final String LOCALIZATION = "LOCALIZATION";
    public static final String PUSH = "PUSH";
    public static final String MAGMA = "MAGMA";
    public static final String RSS = "RSS";
    public static final String NATIV_MACOSX = "NATIV_MacOsX";
    public static final String STARTUP = "STARTUP";
    public static final String LOOPBACK = "LOOPBACK";
    public static final String GLOBAL = "GLOBAL";
    public static final String BANDWIDTH = "BANDWIDTH";
    public static final String LIBRARY_SCANNER = "LIBRARY_SCANNER";
    public static final String GWEBCACHE = "GWEBCACHE";
    public static final String UDP_HOST_CACHE = "UDP_HOST_CACHE";
    public static final String UDP_CONNECTION = "UDP_CONNECTION";
    public static final String UDP_INCOMING_MESSAGES = "UDP_INCOMING_MESSAGES";
    public static final String UDP_OUTGOING_MESSAGES = "UDP_OUTGOING_MESSAGES";
    public static final String Network = "Network";
    public static final String ManagedFile = "ManagedFile";
    
    public static final String Download = "Download";
    public static final String Download_Engine = "Download.Engine";
    public static final String Download_Manager = "Download.Manager";
    public static final String Download_Candidate = "Download.Candidate";
    public static final String Download_Segment = "Download.Segment";
    public static final String Download_Segment_Allocate = "Download.Segment.Allocate";
    public static final String Download_Segment_Dump = "Download.Segment.Dump";
    public static final String Download_Segment_MergSortSplit = "Download.Segment.MergeSortSplit";
    public static final String Download_File = "Download_File";
    public static final String Download_File_RangePriority = "Download.File.RangePriority";
    public static final String Download_DownloadSet = "Download.DownloadSet";
    public static final String Download_Worker = "Download.Worker";
    public static final String Download_Candidate_Allocate = "Download.Candidate.Allocate";
    
    public static final String UPLOAD = "UPLOAD";
    
    public static final String ONLINE_OBSERVER = "ONLINE_OBSERVER";
    
    public static final String OUTGOING_MESSAGES = "OUTGOING_MESSAGES";
    public static final String MESSAGE_ENCODE_DECODE = "MESSAGE_ENCODE_DECODE";
    public static final String SERVER = "SERVER";
    public static final String IN_CONNECTION = "INCOMING_CONNECTION";
    public static final String OUT_CONNECTION = "OUT_CONNECTION";
    public static final String USER_INTERFACE = "USER_INTERFACE";
    
    public static final String IncomingMessages = "IncomingMessages";
    public static final String IncomingMessages_Dropped = "IncomingMessages.Dropped";
    
    public static final String Thex_Calculation = "Thex.Calculation";
    
    public static final String Security = "Security";
    
    public static final String Query_DynamicQueryWorker = "Query.DynamicQueryWorker";
    
    public static final String UpdateCheck_Request = "UpdateCheck.Request";
    public static final String UpdateCheck_Response = "UpdateCheck.Response";
    
    public static final String Favorites = "Favorites";
    public static final String Sharing = "Sharing";
}
