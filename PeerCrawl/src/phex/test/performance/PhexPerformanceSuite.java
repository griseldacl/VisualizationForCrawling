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
 *  $Id: PhexPerformanceSuite.java,v 1.5 2005/10/03 00:18:29 gregork Exp $
 */
package phex.test.performance;

import java.io.File;

import junit.framework.*;
import phex.common.*;
import phex.utils.Localizer;
import phex.utils.Logger;


public class PhexPerformanceSuite extends TestSuite
{

    public PhexPerformanceSuite(String s)
    {
        super(s);
    }

    protected void setUp()
        throws Exception
    {
        StringBuffer path = new StringBuffer(20);
        path.append( System.getProperty("user.home") );
        path.append( File.separator );

        //phex config files are hidden on all UNIX systems (also MacOSX. Since
        //there are many UNIX like operation systems with Java support out there,
        //we can not recognize the OS through it's name. Thus we check if the
        //root of the filesystem starts with "/" since only UNIX uses such
        //filesystem conventions
        if ( File.separatorChar == '/' )
        {
            path.append ('.');
        }
        path.append ("phex");
        path.append( File.separator );
        path.append( "testSuite" );
        Localizer.initialize();
        Environment.getInstance().setPhexConfigRoot( new File( path.toString() ) );
        //Environment.getInstance().initializeManagers();

        ServiceManager.sCfg.loggerVerboseLevel = 0;
        ServiceManager.sCfg.logType = 62;
        Logger.setLogType( (short)62 );
        Logger.setVerboseLevel( (short)0 );
    }

    public static Test suite()
        throws Exception
    {
        PhexPerformanceSuite suite = new PhexPerformanceSuite("PhexPerformanceSuite");
        suite.setUp();
        //suite.addTestSuite( TestSHA1.class );
        //suite.addTestSuite( TestFileCopy.class );
        //suite.addTestSuite( TestByteBuffer.class );
        return suite;
    }
}
