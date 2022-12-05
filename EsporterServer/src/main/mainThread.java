package main;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.imageio.ImageIO;

import data.Data;
import database.DatabaseAccess;
import database.Requete;
import database.Requete.typeRequete;
import socket.Response;
import socket.ResponseObject;
import types.EcurieInfo;
import types.EquipeInfo;
import types.Image;
import types.InfoID;
import types.Infos;
import types.Jeu;
import types.JoueurInfo;
import types.Renomme;
import types.Titre;
import types.TournoiInfo;

public class mainThread {

	private static mainThread instance;
	private boolean running=true;
	private static volatile ArrayList<ConnectionClient> tabClient =  new ArrayList<>();
	private static volatile int nbClient=0;
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
				Socket s = server.accept();
				ConnectionClient c = new ConnectionClient(s);
				ajouterClient(c);

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
			//e.printStackTrace();
		} catch (SQLException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e2) {
			// TODO Auto-generated catch block
			//e2.printStackTrace();
		}
	}

	
	public static mainThread getInstance() {
		if (instance==null)
			instance = new mainThread();
		return instance;
	}
	
	public void ajouterClient(ConnectionClient c) {
		synchronized (tabClient) {
			tabClient.add(c);
			nbClient++;
		}
	}
	
	public synchronized Data getData() {
		return data;
	}
	
	public void initializeApp() throws InterruptedException, SQLException, IOException {
		//ECURIE
		Requete r = new Requete(Requete.allEcurie(), typeRequete.REQUETE);
		ResultSet rs = db.getData(r).getResultSet();
		HashMap<Integer, EcurieInfo> ecuries = new HashMap<>();
		while(rs.next()) {
			BufferedImage bf1 = ImageIO.read(rs.getBinaryStream("logoecurie"));
			Image im1 = new Image(bf1, "png");
			ecuries.put(rs.getInt("id_utilisateur"), new EcurieInfo(rs.getString("nomEcurie"), im1, rs.getString("DiminutifEcurie"), rs.getInt("id_utilisateur")));
		}
		data.setEcuries(ecuries);
		//EQUIPE
		EquipeInfo equipe;
		JoueurInfo joueur;
		ResultSet resultJoueur;
		Requete requeteGetJoueur;
		HashMap<Integer,JoueurInfo> joueurs;
		ArrayList<Titre> palmares;
		for (EcurieInfo ec : data.getListEcurie()) {
			r = new Requete(Requete.allEquipeByEcurie(ec.getId()), typeRequete.REQUETE);
			rs = db.getData(r).getResultSet();
			while(rs.next()) {
				//Joueur
				joueurs = new HashMap<>();
				requeteGetJoueur = new Requete(Requete.allJoueurByEquipe(rs.getInt("Id_Equipe")), typeRequete.REQUETE);
				resultJoueur = db.getData(requeteGetJoueur).getResultSet();
				while(resultJoueur.next()) {
					BufferedImage bf1 = ImageIO.read(resultJoueur.getBinaryStream("photojoueur"));
					Image im1 = new Image(bf1, "png");
					joueurs.put(resultJoueur.getInt("Id_Utilisateur"), new JoueurInfo(resultJoueur.getInt("Id_Utilisateur"), resultJoueur.getString("nomjoueur"), resultJoueur.getString("prenomjoueur"), im1, resultJoueur.getDate("datenaissancejoueur"), resultJoueur.getDate("datecontratjoueur"), resultJoueur.getDate("fincontratJoueur"), -1, rs.getInt("Id_Equipe"), ec.getId()));
				}
				equipe = new EquipeInfo(Jeu.intToJeu(rs.getInt("Id_Jeux")), ec , joueurs, rs.getInt("Id_Equipe"));
				ec.ajouterEquipe(equipe);
			}
			
			r = new Requete(Requete.getTitreBuEcurie(ec.getId()), typeRequete.REQUETE);
			rs = db.getData(r).getResultSet();
			palmares = new ArrayList<>();
			while(rs.next()) {
				//Titre
				palmares.add(new Titre(rs.getString("libelle"),rs.getDate("dateobtention")));
				
			}
			ec.setPalmares(palmares);
			
			
		}
		
		
		/*On va maintenant initialiser la partie tournoi*/
		//Tournoi
		r = new Requete(Requete.getCalendrier(), typeRequete.REQUETE);
		rs = db.getData(r).getResultSet();
		HashMap<Integer, TournoiInfo> calendrier = new HashMap<>();
		TournoiInfo tournoi;
		Requete req;
		ResultSet res;
		ArrayList<Integer> inscrits;
		while(rs.next()) {
			tournoi = new TournoiInfo(rs.getDate("datelimiteinscription"), rs.getString("nom"), Renomme.intToRenommee(rs.getInt("Renommee")), Jeu.intToJeu(rs.getInt("id_jeux")), rs.getInt("id_tournois"));
			req = new Requete(Requete.getInscris(tournoi.getId()), typeRequete.REQUETE);
			res = DatabaseAccess.getInstance().getData(req).getResultSet();
			inscrits = new ArrayList<>();
			while (res.next()) {
				inscrits.add(res.getInt("id_equipe"));
			}
			tournoi.setInscris(inscrits);
			calendrier.put(rs.getInt("id_tournois"), tournoi);
		}
		this.data.setCalendrier(calendrier);
			//Poule
				//Rencontre
		//Classement
	}
	
	
	public synchronized void miseAJourData(InfoID info, Infos data) {
		System.out.println("MISE A JOUR DES DATA");
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
			System.out.println();
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
		synchronized (tabClient) {
			System.out.println("Send all "+response.getName());
			for (ConnectionClient con : tabClient) {
				System.out.println("send");
				con.send(response);
			}
		}
	}
	
	public void closeClient(ConnectionClient c) {
		synchronized (tabClient) {
			tabClient.remove(c);
			nbClient--;
		}
	}
}
