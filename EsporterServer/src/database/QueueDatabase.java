package database;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class QueueDatabase<T> {

	private LinkedBlockingQueue<Entry<Integer, T>> queue;
	private int max;
	private int actual;
	private int nbElement;
	private static volatile int ID;
	private DatabaseAccess db;
	private final static int CAPACITY = 50;
	
	public QueueDatabase (DatabaseAccess db) {
		queue = new LinkedBlockingQueue<>(CAPACITY);
		max =0;
		actual=0;
		nbElement =0;
		this.db = db;
		
	}
	
	public int put(T s) throws InterruptedException {
		if (queue.size()==0) {
			synchronized (db) {
				db.notify();
			}
		}
		queue.put(new SimpleEntry<>(++ID,s));
		return ID;
	}
	
	public void put(T s, int id) throws InterruptedException {
		queue.put(new SimpleEntry<>(id,s));
	}
	
	public Entry<Integer, T> suivant(){
		try {
			return queue.take();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public Entry<Integer, T> get(int id) throws InterruptedException{
		Entry<Integer, T> t;
		t = queue.peek();
		while(true) {
			if (t!=null) {
				if (t.getKey()==id)
					return queue.take();
				Thread.sleep(100);
			}
			t = queue.peek();
		}
	}
	
	public void remove(int i) {
		
	}
	
	public int getNbElement() {
		return queue.size();
	}
}
