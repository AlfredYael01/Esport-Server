package database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Map.Entry;


public class DatabaseAccess {

	private DatabaseAccess instance;
	private Connection conn;
	private QueueDatabase<Requete> in;
	private QueueDatabase<Result> out;
	private Thread t;
	
	private DatabaseAccess() {
		in = new QueueDatabase<>();
		out = new QueueDatabase<>();
		connexion();
		
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
					Entry<Integer, Requete> entree = in.suivant();
					
					if (entree!=null) {
						int  id = entree.getKey();
						Requete r = entree.getValue();
						Result rs=new Result(null, 0);
						switch (r.getType()) {
						case FONCTION:
							int role;
							try {
								CallableStatement cstmt = conn.prepareCall(r.getRequete());
								cstmt.registerOutParameter(1, Types.INTEGER);
								cstmt.executeUpdate();
								role = cstmt.getInt(1);
							} catch (SQLException e1) {
								
								e1.printStackTrace();
								role = -1;
							}
							rs.setEntier(role);
							break;
						case REQUETE:
							
							Statement st;
							try {
								st = conn.createStatement();
								rs.setResultSet(st.executeQuery(r.getRequete()));
							} catch (SQLException e1) {
								
								e1.printStackTrace();
							}
								
							
							
							
							rs.setResultSet(null);
							break;
						
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
		});
		
	}
	
	
	private void connexion() {
        String login = "MRC4302A";
        String passw = "$iutinfo";
        String connectString = "jdbc:oracle:thin:@localhost:1521:xe";
    
        try {
            DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
        } catch (SQLException e){
            e.printStackTrace();
        }
        
        try {
        	conn = DriverManager.getConnection(connectString, login, passw);
            System.out.println("Connexion OK");
            
            
        } catch(Exception ee){
            System.out.println("Erreur connexion");
        }

    }
	
	public DatabaseAccess getInstance() {
		if (instance==null)
			instance = new DatabaseAccess();
		return instance;
	}
	
	public ResultSet getData(Requete requete) throws InterruptedException {
		int id = in.put(requete);
		Entry<Integer, Result> data;
		data = out.get(id);
		
		return data.getValue().getResultSet();
	}
	
	public int login(Requete requete) throws InterruptedException {
		int id = in.put(requete);
		Entry<Integer, Result> data;
		data = out.get(id);
		
		return data.getValue().getEntier();
	}
	
	public Thread getT() {
		return t;
	}
	
	
}
