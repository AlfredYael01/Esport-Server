package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.SQLException;
import java.util.Vector;

import database.DatabaseAccess;

public class mainThread {

	private static mainThread instance;
	private boolean running=true;
	private Vector<ConnectionClient> tabClient=new Vector<>();
	private int nbClient=0;
	private DatabaseAccess db;
	
	private mainThread() {
		try {
			db = DatabaseAccess.getInstance();
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
			
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			try {
				server.close();
			} catch (IOException e) {
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
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
