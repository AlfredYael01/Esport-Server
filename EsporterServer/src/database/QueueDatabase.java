package database;

import java.util.HashMap;
import java.util.Map;

public class QueueDatabase<T> {

	private Map<Integer, T> queue;
	private int max;
	private int actual;
	private int nbElement;
	private DatabaseAccess db;
	
	public QueueDatabase () {
		queue = new HashMap<>();
		max =0;
		actual=0;
		nbElement =0;
	}
	
	public int put(T s) {
		if (nbElement==0) {
			db.getT().notify();
		}
		max++;
		nbElement++;
		queue.put(max, s);
		return max;
	}
	
	public void put(T s, int id) {
		queue.put(id, s);
	}
	
	public T suivant() {
		T t = get(actual+1);
		if (t!=null)
			actual++;
		return t;
	}
	
	public T get(int i) {
		if (queue.containsKey(i)) {
			T t = queue.get(i);
			queue.remove(i);
			nbElement--;
			return t;
			
		}
		return null;
	}
	
	public void remove(int i) {
		
	}
	
	public int getNbElement() {
		return nbElement;
	}
}
