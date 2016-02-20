package phex.utils;

import java.util.*;

/**
 * This implements a WordTable by stooring a suffix tree where all child
 * nodes in the tree are guaranteed to be less than the parent node, and
 * each node can have a set of hits associated with it.
 */
public class WordTableByTree
extends WordTable {
  private Node root;
  private List gopher;
  
  public WordTableByTree() {
    root = new Node();
    gopher = new ArrayList();
  }
  
  public void store(String keyString, Object item) {
    fillList(gopher, keyString);
    
    if(gopher.isEmpty()) {
      throw new IllegalArgumentException(
        "Key string contains no usefull sub strings"
      );
    }
    
    Node current = root;
    for(Iterator i = gopher.iterator(); i.hasNext(); ) {
      current = current.getChild(i.next(), true);
    }
    
    current.addItem(item);
  }
  
  public void fetch(String keyString, HitListener hitListener) {
    fillList(gopher, keyString);
    
    OUTER:
    for(int i = 0; i < gopher.size(); i++) {
      Node current = root;
      for(int j = i; j < gopher.size(); j++) {
        current = current.getChild(gopher.get(j), false);
        if(current == null) {
          continue OUTER;
        }
        
        Set hits = current.getItems();
        if(hits != null) {
          for(Iterator hi = hits.iterator(); hi.hasNext(); ) {
            hitListener.processHit(hi.next());
          }
        }
      }
    }
  }
  
  private static class Node {
    private Map children;
    private Set items;
    
    // use a small map impl if necisary
    public Node getChild(Object o, boolean create) {
      Node child = null;
      
      if(children == null) {
        if(create) {
          children = new HashMap();
          children.put(o, child = new Node());
        }
      } else {
        child = (Node) children.get(o);
        if(child == null) {
          children.put(o, child = new Node());
        }
      }
      
      return child;
    }
    
    public Set getItems() {
      return items;
    }
    
    public void addItem(Object item) {
      if(items == null) {
        items = new WordTable.DiffSet();
      }
      items.add(item);
    }
  }
  
  private void dumpTree(String leader, Node node) {
    if(node.children == null) {
      System.out.println(leader + node);
    }
    if(node.items != null) {
      System.out.println(leader + node.items);
    }
    if(node.children != null) {
      for(Iterator i = node.children.keySet().iterator(); i.hasNext(); ) {
        Object it = i.next();
        System.out.println(leader + "-> " + it);
        dumpTree(leader + "  ", (Node) node.children.get(it));
      }
    }
  }
}
