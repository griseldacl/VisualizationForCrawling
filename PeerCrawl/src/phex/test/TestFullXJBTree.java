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
 *  $Id: TestFullXJBTree.java,v 1.6 2005/10/03 00:18:29 gregork Exp $
 */
package phex.test;

import java.io.File;

import junit.framework.TestCase;
import phex.common.file.FileManager;
import phex.common.file.ManagedFile;
import phex.xml.ObjectFactory;
import phex.xml.XJBAlternateLocation;
import phex.xml.XJBFavoriteHost;
import phex.xml.XJBFavoritesList;
import phex.xml.XJBGUISettings;
import phex.xml.XJBGUITab;
import phex.xml.XJBGUITable;
import phex.xml.XJBGUITableColumn;
import phex.xml.XJBGUITableColumnList;
import phex.xml.XJBGUITableList;
import phex.xml.XJBIPAccessRule;
import phex.xml.XJBMediaType;
import phex.xml.XJBPhex;
import phex.xml.XJBSWDownloadCandidate;
import phex.xml.XJBSWDownloadFile;
import phex.xml.XJBSWDownloadList;
import phex.xml.XJBSWDownloadSegment;
import phex.xml.XJBSearchFilter;
import phex.xml.XJBSearchFilters;
import phex.xml.XJBSecurity;
import phex.xml.XJBSharedFile;
import phex.xml.XJBSharedLibrary;
import phex.xml.XJBUpdateRequest;
import phex.xml.XJBUpdateResponse;
import phex.xml.XMLBuilder;


public class TestFullXJBTree extends TestCase
{

    public TestFullXJBTree(String s)
    {
        super(s);
    }

    protected void setUp()
    {
    }

    protected void tearDown()
    {
    }

    public void testXJBSaveToFile()
    {
        try
        {
            ObjectFactory objFactory = new ObjectFactory();
            
            XJBPhex xjbPhex = objFactory.createPhexElement();
            XJBFavoritesList favList = objFactory.createXJBFavoritesList();
            XJBFavoriteHost favHost = objFactory.createXJBFavoriteHost();
            favHost.setHostName("t‰st");
            favHost.setIp(new byte[] {1,1,1,1});
            favHost.setPort(1234);
            favList.getFavoritesList().add(favHost);
            xjbPhex.setFavoritesList( favList );
            
            XJBGUISettings guiSet = objFactory.createXJBGUISettings();
            guiSet.setLookAndFeelClass("teﬂt");
            guiSet.setToolbarVisible(true);
            guiSet.setLogBandwidthSliderUsed(true);
            guiSet.setWindowHeight(123);
            guiSet.setWindowPosX(123);
            guiSet.setWindowPosY(123);
            guiSet.setWindowWidth(123);
            
            XJBGUITableList guiTableList = objFactory.createXJBGUITableList();
            guiTableList.setShowHorizontalLines(false);
            guiTableList.setShowVerticalLines(false);
            XJBGUITable table = objFactory.createXJBGUITable();
            table.setTableIdentifier("test");
            XJBGUITableColumnList colList = objFactory.createXJBGUITableColumnList();
            XJBGUITableColumn col = objFactory.createXJBGUITableColumn();
            col.setColumnID(123);
            col.setVisible(false);
            col.setVisibleIndex(123);
            col.setWidth(123);
            colList.getTableColumnList().add(col);
            table.setTableColumnList(colList);
            guiTableList.getTableList().add(table);
            guiSet.setTableList(guiTableList);
            XJBGUITab tab = objFactory.createXJBGUITab();
            tab.setTabID(123);
            tab.setVisible(false);
            guiSet.getTabList().add(tab);
            xjbPhex.setGuiSettings( guiSet );
            
            xjbPhex.setPhexVersion("1.2.3");
            
            XJBSearchFilters filters = objFactory.createXJBSearchFilters();
            XJBSearchFilter filter = objFactory.createXJBSearchFilter();
            filter.setLastTimeUsed(1234567l);
            filter.setMinFileSize(1234567l);
            filter.setMinHostRating((short)123);
            filter.setMinHostSpeed(123);
            filter.setName("test");
            filter.setRefineText("test");
            XJBMediaType media = XJBMediaType.PROGRAM;
            filter.setMediaType(media);
            filters.getSearchFilterList().add(filter);
            xjbPhex.setSearchFilters(filters);
            
            XJBSecurity security = objFactory.createXJBSecurity();
            XJBIPAccessRule iprule = objFactory.createXJBIPAccessRule();
            iprule.setAddressType((byte)1);
            iprule.setCompareIP(new byte[] {1,1,1,1});
            iprule.setDeletedOnExpiry(false);
            iprule.setDenyingRule(true);
            iprule.setDescription("test");
            iprule.setDisabled(false);
            iprule.setExpiryDate(1234567l);
            iprule.setIp(new byte[] {1,1,1,1});
            iprule.setSystemRule(true);
            iprule.setTriggerCount(123);
            security.getIpAccessRuleList().add(iprule);
            xjbPhex.setSecurity(security);
            
            XJBSharedLibrary library = objFactory.createXJBSharedLibrary();
            XJBSharedFile sharedFile = objFactory.createXJBSharedFile();
            sharedFile.setFileName("test");
            sharedFile.setHitCount(1);
            sharedFile.setLastModified(1234567l);
            sharedFile.setLastSeen(123);
            sharedFile.setSHA1("test");
            sharedFile.setUploadCount(123);
            XJBAlternateLocation altLoc = objFactory.createXJBAlternateLocation();
            altLoc.setHostAddress("test");
            altLoc.setURN("test");
            sharedFile.getAltLoc().add( altLoc );
            library.getSharedFileList().add( sharedFile );
            xjbPhex.setSharedLibrary(library);
            
            XJBSWDownloadList downList = objFactory.createXJBSWDownloadList();
            XJBSWDownloadFile downFile = objFactory.createXJBSWDownloadFile();
            downFile.setCreatedTime(1234567l);
            downFile.setFileSize(1234567l);
            downFile.setFileURN("test");
            downFile.setLocalFileName("test");
            downFile.setModifiedTime(1234567l);
            downFile.setSearchTerm("test");
            downFile.setStatus((short)123);
            XJBSWDownloadCandidate downCandidate = objFactory.createXJBSWDownloadCandidate();
            downCandidate.setChatSupported(false);
            downCandidate.setConnectionFailedRepetition(123);
            downCandidate.setFileIndex(1234567l);
            downCandidate.setFileName("test");
            downCandidate.setGUID("test");
            downCandidate.setPushNeeded(true);
            downCandidate.setRemoteHost("test");
            downCandidate.setThexSupported(false);
            downCandidate.setVendor("test");
            downFile.getCandidateList().add(downCandidate);
            XJBSWDownloadSegment segment = objFactory.createXJBSWDownloadSegment();
            segment.setIncompleteFileName("test");
            segment.setLength(1234567l);
            segment.setSegmentNumber(123);
            segment.setStartPosition(1234567l);
            downFile.getSegmentList().add(segment);
            downList.getSWDownloadFileList().add(downFile);
            xjbPhex.setSWDownloadList(downList);
            
            XJBUpdateRequest request = objFactory.createXJBUpdateRequest();
            request.setAvgUptime(1234567l);
            request.setCurrentVersion("test");
            request.setDownloadCount(123);
            request.setHostid("test");
            request.setJavaVersion("test");
            request.setLafUsed("test");
            request.setLastCheckVersion("test");
            request.setLastInfoId(123);
            request.setOperatingSystem("test");
            request.setSharedFiles(123);
            request.setSharedSize(123);
            request.setShowBetaInfo(false);
            request.setUploadCount(123);
            xjbPhex.setUpdateRequest(request);
            
            XJBUpdateResponse response = objFactory.createXJBUpdateResponse();
            XJBUpdateResponse.InfoType info = objFactory.createXJBUpdateResponseInfoType();
            info.setHeader("test");
            info.setId("test");
            info.setText("test");
            response.getInfoList().add(info);
            XJBUpdateResponse.VersionType version = objFactory.createXJBUpdateResponseVersionType();
            version.setBeta(false);
            version.setId("test");
            version.setText("test");
            response.getVersionList().add(version);
            xjbPhex.setUpdateResponse(response);
            
            
            File aFile1 = new File( "c:\\temp\\tempXJB.xml" );
            ManagedFile managedFile = FileManager.getInstance().getReadWriteManagedFile( aFile1 );
            XMLBuilder.saveToFile( managedFile, xjbPhex );
        }
        catch(Exception e)
        {
            System.err.println("Exception thrown:  "+e);
        }
    }

    public void testLoadXJBPhexFromFile()
    {
        long startMem = Runtime.getRuntime().totalMemory();
        long start = System.currentTimeMillis();
       File aFile1 = new File( "c:\\temp\\tempXJB.xml" );
       try
       {
           ManagedFile managedFile = FileManager.getInstance().getReadWriteManagedFile( aFile1 );
           XJBPhex xjbphexRet = XMLBuilder.loadXJBPhexFromFile( managedFile );
       }
       catch(Exception e) {
           System.err.println("Exception thrown:  "+e);
       }
       long end = System.currentTimeMillis();
        long endMem = Runtime.getRuntime().totalMemory();
        System.out.println( "parsexjb: " + (end-start) + " " + ( endMem - startMem) );
   }
}