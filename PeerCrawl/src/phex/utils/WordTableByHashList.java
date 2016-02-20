package phex.utils;

import java.util.*;

/**
 * <p>A table that stores data items by sets of words.</p>
 *
 * <p>This is optimized for order-independant scanning of a new set of words
 * presented in a single string against a set of known string sets. It should
 * perform better than a linear search through the known string sets,
 * particularly for tables with many word sets. The idea is that you could
 * economicaly scan a WordTable many times with many different query strings
 * without it being prohibatively costly as the size of the table increases.</p>
 *
 * <p>The data items can be retrieved by any set of words that is a suber-set
 * of the items it was stored under originaly. That is, if you stored a->b, and
 * you then search with c, you will get back b iff c is a super set or is equal
 * to b.</p>
 * 
 * <p><em>node:</em> You must make sure that you synchronize on instances of
 * WordTable externaly.</p>
 */
public class WordTableByHashList
extends WordTable {
  private Map firstWordToListItem;
  
  private List gopher;
  
  /**
   * Create a new empty word table.
   */
  public WordTableByHashList() {
    firstWordToListItem = new HashMap();
    gopher = new ArrayList();
  }
  
  /**
   * <p>Associate a String with a data item.</p>
   *
   * <p>The data item will be associated with the set of words formed
   * from all alpha-numerical substrings of the String. Thus, "foo bar" will
   * resolve to the same key as "bar foo" or even to "foo-bar".</p>
   *
   * <p>It is perfectly legal to associate multiple items with the same key. The
   * result is that when a lookup is performed, a set containing all such items
   * will be returned.</p>
   *
   * @param keyString  a String representing the lookup key set
   * @param item  the Object to associate
   * @throws IllegalArgumentException if keyString resolves to an empty set of
   *         words
   */
  public void store(String keyString, Object item) {
    List list = new ArrayList();
    fillList(list, keyString);
    
    if(list.isEmpty()) {
      throw new IllegalArgumentException(
        "Key string contains no usefull sub strings"
      );
    }
    
    ListItem li = new ListItem(list, item);
    storeListItem(list.get(0), li);
  }
  
  public void fetch(String keyString, HitListener hitListener) {
    fillList(gopher, keyString);
    
    for(Iterator i = gopher.iterator(); i.hasNext(); ) {
      Object o = i.next();
      Set hits = (Set) firstWordToListItem.get(o);
      
      if(hits == null) {
        continue;
      }
      
      for(Iterator j = hits.iterator(); j.hasNext(); ) {
        ListItem li = (ListItem) j.next();
        
        if(listMatch(gopher, li.getList())) {
          hitListener.processHit(li.getItem());
        }
      }
    }
  }
  
  /**
   * <p>Utility method to automagicaly create book-keeping objects as needed.
   * </p>
   *
   * <p>There is scope to increase the performance of this by using a
   * small-memory implementation of Set, such as org.biojava.bio.utils.SmallSet
   * to store the hits.</p>
   *
   * @param item  the Object to key the list by - usualy the first item
   * @param li    the ListItem to add under this key
   */
  private void storeListItem(Object key, ListItem li) {
    Set hits = (Set) firstWordToListItem.get(key);
    
    if(hits == null) {
      firstWordToListItem.put(key, hits = new HashSet());
    }
    
    hits.add(li);
  }
  
  /**
   * <p>Compares two lists to check that superList contains every item in
   * subList.</p>
   *
   * <p>The two lists must both be sorted by hash code.</p>
   */
  private boolean listMatch(List superList, List subList) {
    if(subList.size() > superList.size()) {
      // can't have subset larger than superset!
      return false;
    }
    
    int i = 0;
    int j = 0;
    
    Object io = superList.get(i);
    Object jo = subList.get(j);
    
    int iHash = io.hashCode();
    int jHash = jo.hashCode();
    
    while(true) {
      if(
        iHash < jHash  ||
        ( (iHash == jHash) && (!io.equals(jo)) )
      ) {
        // skipping member of super set as it either has smaller hash code
        // or is a hash collision
        i++;
        if(i < superList.size()) {
          io = superList.get(i);
          iHash = io.hashCode();
        } else {
          // walked off the end looking for the last superset item
          return false;
        }
      } else if (iHash > jHash) {
        // skipped member of sub set - ergo it can't be a sub set
        return false;
      } else {
        // same object in each list - advance in both indecies

        // if we walk off the end of subList, we've found everything
        j++;
        if(j < subList.size()) {
          jo = subList.get(j);
          jHash = jo.hashCode();
        } else {
          return true;
        }

        // if we walk off the end of superList, then there are still subList
        // items left, so it can't be a super-subset pair
        i++;
        if(i < superList.size()) {
          io = superList.get(i);
          iHash = io.hashCode();
        } else {
          return false;
        }
      }
    }
  }
  
  /**
   * Small class for stooring a list and associated data item.
   */
  private static class ListItem {
    private final List list;
    private final Object item;
    
    public ListItem(List list, Object item) {
      this.list = list;
      this.item = item;
    }
    
    public List getList() {
      return list;
    }
    
    public Object getItem() {
      return item;
    }
    
    public int hashCode() {
      return item.hashCode();
    }
  }
}
