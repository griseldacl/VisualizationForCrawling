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
 *  Created on 12.12.2004
 *  --- CVS Information ---
 *  $Id: FileSystemTreeModel.java,v 1.7 2005/10/03 00:18:27 gregork Exp $
 */
package phex.gui.tabs.library;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import phex.event.ShareChangeListener;
import phex.share.ShareManager;
import phex.utils.DirectoryOnlyFileFilter;
import sun.awt.shell.ShellFolder;

/**
 * @author gregor
 */
public class FileSystemTreeModel implements TreeModel
{
    /** Listeners. */
    protected EventListenerList listenerList = new EventListenerList();
    
    private FileFilter dirFilter = new DirectoryOnlyFileFilter();

    private File[] fsRoots;
    private Object root = new String("ROOT");
    private HashMap fsTree;
    
    
    FileSystemView fsv;
    public FileSystemTreeModel()
    {
        fsTree = new HashMap();
        updateFileSystem();
        FileSystemChangeListener listener = new FileSystemChangeListener();
        ShareManager.getInstance().getSharedFilesService().addSharedFilesChangeListener(listener);
    }
    /**
     * 
     */
    public void updateFileSystem()
    {
        fsv = FileSystemView.getFileSystemView();
        fsRoots = fsv.getRoots();
        fsTree.clear();
    }
//        File[] roots = File.listRoots();
//        if ( SystemUtils.IS_OS_WINDOWS )
//        {
//            ArrayList rootList = new ArrayList();
//            for ( int i = 0; i < roots.length; i++ )
//            {// we never like to have A:\ or B:\ on windows here...
//                String absolutePath = roots[i].getAbsolutePath();
//                if ( !(absolutePath.startsWith("A:") ||
//                       absolutePath.startsWith("B:") ) 
//                     && checkFsAccess( roots[i] ) )
//                {
//                    rootList.add(roots[i]);
//                }
//            }
//            fsRoots = new File[rootList.size()];
//            rootList.toArray(fsRoots); 
//        }
//        else
//        {
//            fsRoots = roots;
//        }
//        fireTreeStructureChanged();
//    }
    
//    private boolean checkFsAccess( File file )
//    {
//        return true;
////        try
////        {
////            return file.canRead();
////        }
////        catch (SecurityException x)
////        {
////            return false;
////        }
//    }

    /**
     * @see javax.swing.tree.TreeModel#getRoot()
     */
    public Object getRoot()
    {
        return root;
    }

    /**
     * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
     */
    public Object getChild(Object parent, int index)
    {
System.out.println("getChild " + parent + " " + index );
        if (parent.equals(root))
        {
            return fsRoots[index];
        }
        FolderInfo info = getFolderInfo( (File)parent );
        return info.getChilds()[index];
    }

    /**
     * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
     */
    public int getChildCount( Object parent )
    {
        System.out.println( "getChildCount " + parent );
        if ( parent.equals( root ) )
        {
            return fsRoots.length;
        }
        
        FolderInfo info = getFolderInfo( (File)parent );
        return info.getChilds().length;

        //        File[] files = ((File) parent).listFiles(dirFilter);
        //        if (files == null)
        //        {
        //            return 0;
        //        }
        //        return files.length;
    }

    /**
     * @see javax.swing.tree.TreeModel#isLeaf(java.lang.Object)
     */
    public boolean isLeaf(Object node)
    {
        return false;
    }

    /**
     * @see javax.swing.tree.TreeModel#valueForPathChanged(javax.swing.tree.TreePath, java.lang.Object)
     */
    public void valueForPathChanged(TreePath path, Object newValue)
    {
System.out.println("valueForPathChanged");
        // TODO Auto-generated method stub
    }

    /**
     * @see javax.swing.tree.TreeModel#getIndexOfChild(java.lang.Object, java.lang.Object)
     */
    public int getIndexOfChild(Object parent, Object child)
    {
System.out.println("getIndexOfChild");
        // TODO Auto-generated method stub
        return 0;
    }

    //
    //  Events
    //

    /**
     * Adds a listener for the TreeModelEvent posted after the tree changes.
     *
     * @see     #removeTreeModelListener
     * @param   l       the listener to add
     */
    public void addTreeModelListener(TreeModelListener l)
    {
        listenerList.add(TreeModelListener.class, l);
    }

    /**
     * Removes a listener previously added with <B>addTreeModelListener()</B>.
     *
     * @see     #addTreeModelListener
     * @param   l       the listener to remove
     */
    public void removeTreeModelListener(TreeModelListener l)
    {
        listenerList.remove(TreeModelListener.class, l);
    }
    
    /**
     * 
     */
    private void fireTreeStructureChanged()
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==TreeModelListener.class) {
                // Lazily create the event:
                if (e == null)
                    e = new TreeModelEvent( this, (TreePath)null );
                ((TreeModelListener)listeners[i+1]).treeStructureChanged(e);
            }
        }
    }
    
    public class FileSystemChangeListener implements ShareChangeListener
    {
        /**
         * @see phex.event.ShareChangeListener#sharedDirectoriesChanged()
         */
        public void sharedDirectoriesChanged()
        {
            fireTreeStructureChanged();
        }
    }
    
    private FolderInfo getFolderInfo( File file )
    {
        FolderInfo fInfo = (FolderInfo) fsTree.get(file);
        if ( fInfo != null && fInfo.timestamp < System.currentTimeMillis() - 1000 * 30 )
        {
            return fInfo;
        }


        fInfo = new FolderInfo();
        fInfo.file = file;
        fInfo.timestamp = System.currentTimeMillis();
        fsTree.put( file, fInfo );
        return fInfo;
    }
    
    private class FolderInfo
    {
        private File file;
        private File[] childs;
        long timestamp;
        
        public File[] getChilds()
        {
            System.out.println( "---" );
            System.out.println( ((ShellFolder)file).getPath());
            System.out.println( ((ShellFolder)file).isFileSystem());
            System.out.println( ShellFolder.isComputerNode(file));
            System.out.println( ShellFolder.isFileSystemRoot(file));
            System.out.println( "---" );
            
            if ( childs != null )
            {
                return childs;
            }
            
            File[] files = fsv.getFiles( file, false );
            ArrayList folderList = new ArrayList();
            for ( int i = 0; i < files.length; i++ )
            {
                if ( fsv.isTraversable(files[i]).booleanValue() )
                {
                    folderList.add( files[i] );
                }
            }
            childs = (File[])folderList.toArray( new File[folderList.size()]);
            return childs;
        }
    }
}