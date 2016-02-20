/* *  PHEX - The pure-java Gnutella-servent. *  Copyright (C) 2001 - 2005 Phex Development Group * *  This program is free software; you can redistribute it and/or modify *  it under the terms of the GNU General Public License as published by *  the Free Software Foundation; either version 2 of the License, or *  (at your option) any later version. * *  This program is distributed in the hope that it will be useful, *  but WITHOUT ANY WARRANTY; without even the implied warranty of *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the *  GNU General Public License for more details. * *  You should have received a copy of the GNU General Public License *  along with this program; if not, write to the Free Software *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA *  *  --- CVS Information --- *  $Id: IFind.java,v 1.4 2005/10/03 00:18:28 gregork Exp $ */
/*
 * TODO: Figure out exactly what the contract of this interface is. It doesn't
 * really make much sense, it specifies no behavior and the results to be searched
 * are not passed as parameters, so they're assumed. This should probably be
 * implemented by an abstract class, guaranteeing that all descendents have the
 * correct internals to deal with the interface.
 */

package phex.interfaces;

/**
 * Indicates that the implementing class has the ability to search through an
 * internal set of results.
 */
public interface IFind
{
	/**
	 * Searches through an internal set of results.
	 *
	 * @param bMatchCase <code>true</code> if the search is case-sensitive
	 * @param bFindDown <code>true</code> if the search should go in sequential
	 *	order
	 * @param searchText the text to search for
	 */
	public void findInResult(boolean bMatchCase, boolean bFindDown, String searchText);
}


