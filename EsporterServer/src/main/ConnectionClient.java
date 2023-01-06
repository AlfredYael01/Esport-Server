	package main;

import java.awt.Window.Type;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;

import model.database.DatabaseAccess;
import model.database.Query;
import model.database.Result;
import model.database.Query.typeRequete;
import model.socket.ResponseObject;
import types.TypesFame;
import types.TypesGame;
import types.TypesID;
import types.TypesPermission;
import types.TypesTournament;

public class ConnectionClient {

	
	private Thread thread;
	private Socket s;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private boolean isLogin = false;
	private TypesPermission role = TypesPermission.VISITOR;
	
	public ConnectionClient(Socket s) {
		this.s = s;
		try {
			out = new ObjectOutputStream(s.getOutputStream());
			in = new ObjectInputStream(s.getInputStream());			
		} catch (IOException e) {
			
		}
		thread = new Thread(new ListenClient(this));
		thread.setDaemon(true);
		thread.start();
	}
	
	public int login(String username, String password) {
		try {
			Result r = DatabaseAccess.getInstance().login(new Query(Query.Login(username, password), typeRequete.FUNCTION));
			if (r.isError()) {
				System.out.println("Error");
				return -1;
			}
			System.out.println(r.getInteger());
			return r.getInteger();
		} catch (SQLException s) {
			s.printStackTrace();
			return -1;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return -1;
		}
		/*
		if (username.equals("test") && password.equals("mdpTest"))
			return 4;
		return -1;*/
	}
	
	public int ajouterTournoi(TypesTournament t) {
		Query req = new Query(Query.addTournament(TypesGame.gameToInt(t.getGame()), t.getName(), TypesFame.FameToInt(t.getFame())), typeRequete.FUNCTION);
		req.setDates(t.getRegisterDate());

		try {
			Result r = DatabaseAccess.getInstance().insertData(req);
			int id = r.getInteger();
			t.setId(id);
			mainThread.getInstance().miseAJourData(TypesID.TOURNAMENT, t);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		return 1;
	}
	
	
	public void setPermission(int perm) {
		switch(perm) {
		case 1:role = TypesPermission.ORGANIZER;break;
		case 2:role = TypesPermission.REFEREE;break;
		case 3:role = TypesPermission.PLAYER;break;
		case 4:role = TypesPermission.STABLE;break;
		}
	}
	
	public TypesPermission getRole() {
		return role;
	}
	
	public void setPermission(TypesPermission perm) {
		role = perm;
	}
	
	public void send(ResponseObject o) {
		try {
			out.writeObject(o);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public int logout() {
		return -1;
	}
	
	public Socket getSocket() {
		return s;
	}
	
	public Thread getThread() {
		return thread;
	}
	
	public ObjectInputStream getIn() {
		return in;
	}
	
	public ObjectOutputStream getOut() {
		return out;
	}
	
	public boolean getIsLogin() {
		return isLogin;
	}
	
	public void setIsLogin(boolean b) {
		isLogin = b;
	}
	
}
