package phex.utils;

import java.util.*;

public abstract class WordTable {
  static final Comparator HASH_COMPARATOR = new Comparator() {
    public int compare(Object a, Object b) {
      return a.hashCode() - b.hashCode();
    }
  };
  
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
  public abstract void store(String keyString, Object item);
  
  /**
   * <p>Return a set of items that were stored under strings that are composed
   * from words that are <em>all</em> found in keyString.</p>
   *
   * @param keyString  the lookup String
   * @param hitListener  the HitListener instance to inform of matches
   */
  public abstract void fetch(String keyString, HitListener hitListener);
  
  /**
   * <p>Break data up into words, and populate list with these sorted by natural
   * ordering.</p>
   *
   * <p>This will split on white space. It will substitute whitespace for all
   * non-alpha-numeric characters. Ideally this would be done
   * by pulling out all matches to the regex ([\d\w]*), but we only get that
   * sort of fun in java 1.4 unless we pull in a library.</p>
   *
   * <p>While this is running, it would be a <em>bad thing</em> to modify list.
   * You should assume that WordTable takes ownership of list untill the
   * method returns.</p>
   *
   * @param list  the List to populate
   * @param data  the string to tokenize
   */
  protected void fillList(List list, String data) {
    list.clear();
    
    // scan for non letter digit chars
    // if we find any, edit the string in a buffer and copy back into data
    for(int i = 0; i < data.length(); i++) {
      if(!Character.isLetterOrDigit(data.charAt(i))) {
        StringBuffer sb = new StringBuffer(data);
        
        sb.setCharAt(i, ' ');
        for(int j = i+1; j < sb.length(); j++) {
          if(!Character.isLetterOrDigit(sb.charAt(j))) {
            sb.setCharAt(j, ' ');
          }
        }
        
        data = sb.toString();
        break;
      }
    }
    
    // populate list with the remaining tokens
    StringTokenizer sTok = new StringTokenizer(data);
    while(sTok.hasMoreTokens()) {
      String token = sTok.nextToken();
      list.add(token);
    }
    
    // sort the result
    Collections.sort(list, WordTable.HASH_COMPARATOR);
  }
  
  public static interface HitListener {
    public void processHit(Object item);
  }

  /**
   * <p>Set where we know that each item added will be unique.</p>
   */
  static class DiffSet
  extends AbstractSet {
    private List items;
    
    public DiffSet() {
      items = new ArrayList();
    }
    
    public DiffSet(int capacity) {
      items = new ArrayList(capacity);
    }
    
    public int size() {
      return items.size();
    }
    
    public Iterator iterator() {
      return items.iterator();
    }
    
    public boolean add(Object o) {
      return items.add(o);
    }
    
    public boolean addAll(Collection c) {
      return items.addAll(c);
    }
  }
}
