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
import types.Renomme;
import types.TournoiInfo;

public class mainThread {

	private static mainThread instance;
	private boolean running=true;
	private Vector<ConnectionClient> tabClient=new Vector<>();
	private int nbClient=0;
	private DatabaseAccess db;
	private Data data;
	
	private mainThread() {
		try {
			db = DatabaseAccess.getInstance();
			data = new Data();
			initializeApp();
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
		} catch (InterruptedException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
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
	
	public synchronized Data getData() {
		return data;
	}
	
	public void initializeApp() throws InterruptedException, SQLException {
		//ECURIE
		Requete r = new Requete(Requete.allEcurie(), typeRequete.REQUETE);
		ResultSet rs = db.getData(r).getResultSet();
		HashMap<Integer, EcurieInfo> ecuries = new HashMap<>();
		while(rs.next()) {
			ecuries.put(rs.getInt("id_utilisateur"), new EcurieInfo(rs.getString("nomEcurie"), rs.getBlob("logoEcurie"), rs.getString("DiminutifEcurie"), rs.getInt("id_utilisateur")));
		}
		data.setEcuries(ecuries);
		//EQUIPE
		EquipeInfo equipe;
		JoueurInfo joueur;
		ResultSet resultJoueur;
		Requete requeteGetJoueur;
		HashMap<Integer,JoueurInfo> joueurs;
		for (EcurieInfo ec : data.getListEcurie()) {
			r = new Requete(Requete.allEquipeByEcurie(ec.getId()), typeRequete.REQUETE);
			rs = db.getData(r).getResultSet();
			while(rs.next()) {
				//Joueur
				joueurs = new HashMap<>();
				requeteGetJoueur = new Requete(Requete.allJoueurByEquipe(rs.getInt("Id_Equipe")), typeRequete.REQUETE);
				resultJoueur = db.getData(requeteGetJoueur).getResultSet();
				while(resultJoueur.next()) {
					joueurs.put(resultJoueur.getInt("Id_Utilisateur"), new JoueurInfo(resultJoueur.getInt("Id_Utilisateur"), rs.getString("nomjoueur"), rs.getString("prenomjoueur"), rs.getBlob("photojoueur"), rs.getDate("datenaissancejoueur"), rs.getDate("datecontratjoueur"), rs.getDate("fincontratJoueur"), rs.getInt("id_nationalite"), rs.getInt("id_equipe"), ec.getId()));
				}
				equipe = new EquipeInfo(Jeu.intToJeu(rs.getInt("Id_Jeux")), ec , joueurs, rs.getInt("Id_Equipe"));
				ec.ajouterEquipe(equipe);
			}
		}
		
		
		/*On va maintenant initialiser la partie tournoi*/
		//Tournoi
		r = new Requete(Requete.getCalendrier(), typeRequete.REQUETE);
		rs = db.getData(r).getResultSet();
		HashMap<Integer, TournoiInfo> calendrier = new HashMap<>();
		while(rs.next()) {
			calendrier.put(rs.getInt("id_tournois"), new TournoiInfo(rs.getDate("datelimiteinscription"), rs.getString("nom"), Renomme.intToRenommee(rs.getInt("Renommee")), Jeu.intToJeu(rs.getInt("id_jeux")), rs.getInt("id_tournois")));
		}
		this.data.setCalendrier(calendrier);
			//Poule
				//Rencontre
		//Classement
	}
	
	
	public synchronized void miseAJourData(InfoID info, Infos data) {
		ResponseObject r;
		HashMap<InfoID, Infos> m = new HashMap<>();
		m.put(info, data);
		switch (info) {
		case Joueur:
			JoueurInfo joueur = (JoueurInfo)data;
			this.data.getEcuries().get(joueur.getId_ecurie()).getEquipes().get(joueur.getId_equipe()).modifierJoueur(joueur);
			r = new ResponseObject(Response.UPDATE_JOUEUR, m, null);
			sendAll(r);
			break;
		case Tournoi:
			TournoiInfo tournoi = (TournoiInfo)data;
			this.data.getCalendrier().put(tournoi.getId(), tournoi);
			r = new ResponseObject(Response.UPDATE_TOURNOI, m, null);
			sendAll(r);
			break;
		case Equipe:
			EquipeInfo equipe = (EquipeInfo)data;
			this.data.getEcuries().get(equipe.getEcurie().getId()).getEquipes().put(equipe.getId(), equipe);
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
