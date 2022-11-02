package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Vector;

public class mainThread {

	private static mainThread instance;
	private boolean running=true;
	private Vector<ConnectionClient> tabClient=new Vector<>();
	private int nbClient=0;
	
	private mainThread() {
		try {
		
			ServerSocket server = new ServerSocket(80);
			System.out.println("Serv démarré");
			while(running) {
				System.out.println("En attente d'une connexion");
				new ConnectionClient(server.accept());
				System.out.println("Nouvelle connexion accepté");
			}
			for (ConnectionClient c : tabClient) {
				try {
					c.getSocket().close();
					c.getThread().join();
			
				} catch (Exception e) {}
			}
			server.close();
		} catch (IOException e) {}
	}

	
	public static mainThread getInstance() {
		if (instance==null)
			instance = new mainThread();
		return instance;
	}
	
	public void ajouterClient(ConnectionClient c) {
		tabClient.add(c);
		nbClient++;
	}
	
	public void closeClient(ConnectionClient c) {
		tabClient.remove(c);
		nbClient--;
	}
}
