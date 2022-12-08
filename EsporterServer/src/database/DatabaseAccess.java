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
import types.TypesStable;


public class DatabaseAccess {

	private static DatabaseAccess instance;
	private Connection conn;
	private QueueDatabase<Query> in;
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
						Entry<Integer, Query> entry = in.next();
						
						if (entry!=null) {
							int  id = entry.getKey();
							Query r = entry.getValue();
							Result rs=new Result(null, 0, false);
							switch (r.getType()) {
							case FUNCTION:
								try {
									CallableStatement cstmt = conn.prepareCall(r.getQuery());
									cstmt.registerOutParameter(1, Types.INTEGER);
									if (r.getDates()!=null) {
										for (int i=2;i<r.getDates().length+2;i++) {
											cstmt.setDate(i, r.getDates()[i-2]);
										}
									}
									cstmt.executeUpdate();
									int integer = cstmt.getInt(1);
									rs.setInteger(integer);
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
									cstmt = conn.prepareCall(r.getQuery());
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
							case QUERY:
								
								Statement st;
								try {
									st = conn.createStatement();
									rs.setResultSet(st.executeQuery(r.getQuery()));
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
									rs.setResultSet(st1.executeQuery(r.getQuery()));
								} catch (SQLException e1) {
									e1.printStackTrace();
									rs.setError(true);
								}
								break;
							case INSERTPLAYER:
								
								try {
									System.out.println(r.getQuery());
									CallableStatement insertPlayer = conn.prepareCall(r.getQuery());
									insertPlayer.registerOutParameter(1, Types.INTEGER);
									insertPlayer.setBinaryStream(2, r.getInputStream());
									int j = 3;
									if (r.getDates()!=null) {
										for (int i=0;i<r.getDates().length;i++) {
											insertPlayer.setDate(i+j, r.getDates()[i]);
										}
										j+=r.getDates().length;
									}
									insertPlayer.executeUpdate();
									int entier = insertPlayer.getInt(1);
									rs.setInteger(entier);
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
							
							}
							
						}
					}
				}
			}
		});
		t.setDaemon(true);
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
	
	public Result getData(Query query) throws InterruptedException {
		int id = in.put(query);
		Entry<Integer, Result> data;
		data = out.get(id);
		
		return data.getValue();
	}
	
	public Result insertData(Query query) throws InterruptedException {
		int id = in.put(query);
		Entry<Integer, Result> data;
		data = out.get(id);
		
		return data.getValue();
		
	}
	

	
	public Result login(Query query) throws InterruptedException {
		int id = in.put(query);
		Entry<Integer, Result> data;
		data = out.get(id);
		System.out.println(data.getValue().getInteger());
		return data.getValue();
	}
	
	public TypesStable getEcurie(Query query) throws InterruptedException {
		TypesStable ecurie = new TypesStable(null, null, null, 0);
		int id = in.put(query);
		Entry<Integer, Result> data;
		data = out.get(id);
		return ecurie;
	}
	
	public Thread getT() {
		return t;
	}
	
	
}
