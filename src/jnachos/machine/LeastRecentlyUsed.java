package jnachos.machine;

import java.util.HashMap;

class Node{
    int key;
    int value;
    Node pre;
    Node next;
 
    public Node(int key, int value){
        this.key = key;
        this.value = value;
    }
}

public  class LeastRecentlyUsed {
	public static int capacity;
    static HashMap<Integer, Node> map = new HashMap<Integer, Node>();
    static Node head=null;
    public static Node end=null;
 
    public LeastRecentlyUsed() {
       
    }
 
    public static int get(int key) {
        if(map.containsKey(key)){
            Node n = map.get(key);
            remove(n);
            setHead(n);
            return n.value;
        }
 
        return -1;
    }
 
    public static int remove(Node n){
        if(n.pre!=null){
            n.pre.next = n.next;
        }else{
            head = n.next;
        }
 
        if(n.next!=null){
            n.next.pre = n.pre;
        }else{
            end = n.pre;
        }
        return n.value;
 
    }
 
    public static void setHead(Node n){
        n.next = head;
        n.pre = null;
 
        if(head!=null)
            head.pre = n;
 
        head = n;
 
        if(end ==null)
            end = head;
    }
 
    public static void set(int key, int value) {
        if(map.containsKey(key)){
            Node old = map.get(key);
            old.value = value;
            remove(old);
            setHead(old);
        }else{
            Node created = new Node(key, value);
            if(map.size()>=capacity){
                map.remove(end.key);
               //create new node giving value = end.value
                remove(end);
               
                setHead(created);
 
            }else{
                setHead(created);
            }    
 
            map.put(key, created);
           
        }
    }

}
