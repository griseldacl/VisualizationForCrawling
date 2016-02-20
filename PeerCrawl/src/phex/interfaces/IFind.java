/*
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

