package database;

import java.sql.Connection;
import java.sql.ResultSet;

import data.Data;

public class DatabaseAccess {

	private DatabaseAccess instance;
	private Connection connection;
	private QueueDatabase<Requete> in;
	private QueueDatabase<ResultSet> out;
	private Thread t;
	
	private DatabaseAccess() {
		in = new QueueDatabase<>();
		out = new QueueDatabase<>();
		connection = null; //A CHANGER
		
		t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				while(true) {
					if (in.getNbElement()==0)
						try {
							t.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					Requete r = in.suivant();
					if (r!=null) {
						//Execute la requete r
						
						
						
						ResultSet rs=null;
						out.put(rs);
					}
				}
			}
		});
		
	}
	
	public DatabaseAccess getInstance() {
		if (instance==null)
			instance = new DatabaseAccess();
		return instance;
	}
	
	public ResultSet getData(Requete requete) {
		int id = in.put(requete);
		ResultSet r;
		r = out.get(id);
		while (r == null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			r = out.get(id);
		}
		return r;
	}
	
	public Thread getT() {
		return t;
	}
	
	
}
