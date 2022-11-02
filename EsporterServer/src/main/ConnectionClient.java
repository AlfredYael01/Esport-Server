package main;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import types.Permission;

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
		if (username.equals("test") && password.equals("mdpTest"))
			return 3;
		return -1;
	}
	
	
	public void setPermission(int perm) {
		switch(perm) {
		case 1:role = Permission.ORGANISATEUR;break;
		case 2:role = Permission.ARBITRE;break;
		case 3:role = Permission.JOUEUR;break;
		case 4:role = Permission.ECURIE;break;
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
