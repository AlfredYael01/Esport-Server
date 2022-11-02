package main;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ConnectionClient {

	
	private Thread thread;
	private mainThread main = mainThread.getInstance();
	private Socket s;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	
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
		return -1;
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
	
}
