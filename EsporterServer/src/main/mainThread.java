package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Vector;

import data.Data;
import database.DatabaseAccess;
import database.Requete;
import database.Requete.typeRequete;
import socket.Response;
import socket.ResponseObject;
import types.EcurieInfo;
import types.EquipeInfo;
import types.InfoID;
import types.Infos;
import types.Jeu;
import types.JoueurInfo;

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
	
	public void initializeApp() throws InterruptedException, SQLException {
		Requete r = new Requete(Requete.allEcurie(), typeRequete.REQUETE);
		ResultSet rs = db.getData(r).getResultSet();
		HashMap<Integer, EcurieInfo> ecuries = new HashMap<>();
		while(rs.next()) {
			ecuries.put(rs.getInt("id_utilisateur"), new EcurieInfo(rs.getString("nomEcurie"), rs.getBlob("logoEcurie"), rs.getString("DiminutifEcurie"), rs.getInt("id_utilisateur")));
		}
		Data.setEcuries(ecuries);
		EquipeInfo equipe;
		JoueurInfo joueur;
		for (EcurieInfo ec : Data.getListEcurie()) {
			r = new Requete(Requete.allEquipeByEcurie(ec.getId()), typeRequete.REQUETE);
			rs = db.getData(r).getResultSet();
			while(rs.next()) {
				equipe = new EquipeInfo(Jeu.intToJeu(rs.getInt("Id_Jeux")), ec , null, rs.getInt("Id_Equipe"));
				ec.ajouterEquipe(equipe);
			}
		}
		//Ecurie
			//Equipe
				//Joueur
		//Tournoi
			//Poule
				//Rencontre
		//Classement
	}
	
	public void miseAJourData(InfoID info, Infos data) {
		ResponseObject r;
		HashMap<InfoID, Infos> m = new HashMap<>();
		m.put(info, data);
		switch (info) {
		case Joueur:;
			r = new ResponseObject(Response.UPDATE_JOUEUR, m, null);
			sendAll(r);
			break;
		case Tournoi:
			r = new ResponseObject(Response.UPDATE_TOURNOI, m, null);
			sendAll(r);
			break;
		case Equipe:
			r = new ResponseObject(Response.UPDATE_EQUIPE, m, null);
			sendAll(r);
			break;
		} 
		
	}
	
	public void sendAll(ResponseObject response) {
		for (ConnectionClient con : tabClient) {
			con.send(response);
		}
	}
	
	public void closeClient(ConnectionClient c) {
		tabClient.remove(c);
		nbClient--;
	}
}
