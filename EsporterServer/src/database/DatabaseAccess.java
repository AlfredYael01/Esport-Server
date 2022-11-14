package database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Map.Entry;

import main.mainThread;


public class DatabaseAccess {

	private static DatabaseAccess instance;
	private Connection conn;
	private QueueDatabase<Requete> in;
	private QueueDatabase<Result> out;
	private Thread t;
	
	private DatabaseAccess() throws SQLException {
		in = new QueueDatabase<>(this);
		out = new QueueDatabase<>(this);
		connexion();
		DatabaseAccess database = this;	
		t = new Thread(new Runnable() {
		
			@Override
			public void run() {
				while(true) {
					synchronized (database) {

						if (in.getNbElement()==0)
							try {
								database.wait();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						Entry<Integer, Requete> entree = in.suivant();
						
						if (entree!=null) {
							int  id = entree.getKey();
							Requete r = entree.getValue();
							Result rs=new Result(null, 0, false);
							switch (r.getType()) {
							case FONCTION:
								try {
									CallableStatement cstmt = conn.prepareCall(r.getRequete());
									cstmt.registerOutParameter(1, Types.INTEGER);
									cstmt.executeUpdate();
									int entier = cstmt.getInt(1);
									rs.setEntier(entier);
									System.out.println("Entier db : "+entier);
								} catch (SQLException e1) {
									e1.printStackTrace();
									rs.setError(true);
								}
								try {
									out.put(rs, id);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								
								break;
							case PROCEDURE:
								CallableStatement cstmt;
								try {
									cstmt = conn.prepareCall(r.getRequete());
									cstmt.executeUpdate();
								} catch (SQLException e2) {
									e2.printStackTrace();
									rs.setError(true);
								}
								try {
									out.put(rs, id);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								
								break;
							case REQUETE:
								
								Statement st;
								try {
									st = conn.createStatement();
									rs.setResultSet(st.executeQuery(r.getRequete()));
								} catch (SQLException e1) {
									e1.printStackTrace();
									rs.setError(true);
								}
								try {
									out.put(rs, id);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								break;
							case INSERT:
								Statement st1;
								try {
									st1 = conn.createStatement();
									rs.setResultSet(st1.executeQuery(r.getRequete()));
								} catch (SQLException e1) {
									e1.printStackTrace();
									rs.setError(true);
								}
								break;

							
							}
							
						}
					}
				}
			}
		});
		t.start();
		
	}
	
	
	private void connexion() throws SQLException {
        String login = "MRC4302A";
        String passw = "$iutinfo";
        String connectString = "jdbc:oracle:thin:@telline.univ-tlse3.fr:1521:etupre";
    
        DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
        
        conn = DriverManager.getConnection(connectString, login, passw);
        System.out.println("Connexion OK");

    }
	
	public static DatabaseAccess getInstance() throws SQLException {
		if (instance==null)
			instance = new DatabaseAccess();
		return instance;
	}
	
	public Result getData(Requete requete) throws InterruptedException {
		int id = in.put(requete);
		Entry<Integer, Result> data;
		data = out.get(id);
		
		return data.getValue();
	}
	
	public void insertData(Requete requete) throws InterruptedException {
		int id = in.put(requete);
		
		
	}
	
	public void insertTournoi(Requete requete) throws InterruptedException {
		insertData(requete);
		mainThread.getInstance().sendAll(null);
	}
	
	public Result login(Requete requete) throws InterruptedException {
		int id = in.put(requete);
		Entry<Integer, Result> data;
		data = out.get(id);
		System.out.println(data.getValue().getEntier());
		return data.getValue();
	}
	
	public Thread getT() {
		return t;
	}
	
	
}
