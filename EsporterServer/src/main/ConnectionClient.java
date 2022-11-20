	package main;

import java.awt.Window.Type;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;

import database.DatabaseAccess;
import database.Requete;
import database.Requete.typeRequete;
import database.Result;
import socket.ResponseObject;
import types.InfoID;
import types.Permission;
import types.TournoiInfo;

public class ConnectionClient {

	
	private Thread thread;
	private mainThread main = mainThread.getInstance();
	private Socket s;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private boolean isLogin = false;
	private Permission role = Permission.VISITEUR;
	
	public ConnectionClient(Socket s) {
		this.s = s;
		try {
			out = new ObjectOutputStream(s.getOutputStream());
			in = new ObjectInputStream(s.getInputStream());			
		} catch (IOException e) {
			
		}
		thread = new Thread(new ListenClient(this));
		thread.start();
	}
	
	public int login(String username, String password) {
		try {
			Result r = DatabaseAccess.getInstance().login(new Requete(Requete.Login(username, password), typeRequete.FONCTION));
			if (r.isError()) {
				System.out.println("Error");
				return -1;
			}
			System.out.println(r.getEntier());
			return r.getEntier();
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
	
	public int ajouterTournoi(TournoiInfo t) {
		Requete req = new Requete(Requete.ajouterTournoi(t.getJeux().ordinal(), t.getDateInscription(), t.getNom(), t.getRenomme().ordinal()), typeRequete.INSERT);
		try {
			Result r = DatabaseAccess.getInstance().insertData(req);
			int id = r.getEntier();
			t.setId(id);
			main.miseAJourData(InfoID.Tournoi, t);
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
		case 1:role = Permission.ORGANISATEUR;break;
		case 2:role = Permission.ARBITRE;break;
		case 3:role = Permission.JOUEUR;break;
		case 4:role = Permission.ECURIE;break;
		}
	}
	
	public Permission getRole() {
		return role;
	}
	
	public void setPermission(Permission perm) {
		role = perm;
	}
	
	public void send(ResponseObject o) {
		try {
			out.writeObject(o);
		} catch (IOException e) {
			
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
