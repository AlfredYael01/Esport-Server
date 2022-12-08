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

import database.DatabaseAccess;
import database.Query;
import database.Query.typeRequete;
import model.data.Data;
import model.socket.Response;
import model.socket.ResponseObject;
import types.TypesStable;
import types.TypesTeam;
import types.TypesImage;
import types.TypesID;
import types.Types;
import types.TypesGame;
import types.TypesPlayer;
import types.TypesFame;
import types.TypesTitle;
import types.TypesTournament;

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
		Query r = new Query(Query.allStables(), typeRequete.QUERY);
		ResultSet rs = db.getData(r).getResultSet();
		HashMap<Integer, TypesStable> ecuries = new HashMap<>();
		while(rs.next()) {
			BufferedImage bf1 = ImageIO.read(rs.getBinaryStream("logoecurie"));
			TypesImage im1 = new TypesImage(bf1, "png");
			ecuries.put(rs.getInt("id_utilisateur"), new TypesStable(rs.getString("nomEcurie"), im1, rs.getString("DiminutifEcurie"), rs.getInt("id_utilisateur")));
		}
		data.setStables(ecuries);
		//EQUIPE
		TypesTeam equipe;
		TypesPlayer joueur;
		ResultSet resultJoueur;
		Query requeteGetJoueur;
		HashMap<Integer,TypesPlayer> joueurs;
		ArrayList<TypesTitle> palmares;
		for (TypesStable ec : data.listStables()) {
			r = new Query(Query.allTeamByStables(ec.getId()), typeRequete.QUERY);
			rs = db.getData(r).getResultSet();
			while(rs.next()) {
				//Joueur
				joueurs = new HashMap<>();
				requeteGetJoueur = new Query(Query.allPlayerByTeam(rs.getInt("Id_Equipe")), typeRequete.QUERY);
				resultJoueur = db.getData(requeteGetJoueur).getResultSet();
				while(resultJoueur.next()) {
					BufferedImage bf1 = ImageIO.read(resultJoueur.getBinaryStream("photojoueur"));
					TypesImage im1 = new TypesImage(bf1, "png");
					joueurs.put(resultJoueur.getInt("Id_Utilisateur"), new TypesPlayer(resultJoueur.getInt("Id_Utilisateur"), resultJoueur.getString("nomjoueur"), resultJoueur.getString("prenomjoueur"), im1, resultJoueur.getDate("datenaissancejoueur"), resultJoueur.getDate("datecontratjoueur"), resultJoueur.getDate("fincontratJoueur"), -1, rs.getInt("Id_Equipe"), ec.getId()));
				}
				equipe = new TypesTeam(TypesGame.intToGame(rs.getInt("Id_Jeux")), ec , joueurs, rs.getInt("Id_Equipe"));
				ec.addTeam(equipe);
			}
			
			r = new Query(Query.getTitleByStable(ec.getId()), typeRequete.QUERY);
			rs = db.getData(r).getResultSet();
			palmares = new ArrayList<>();
			while(rs.next()) {
				//Titre
				palmares.add(new TypesTitle(rs.getString("libelle"),rs.getDate("dateobtention")));
				
			}
			ec.setTitles(palmares);
			
			
		}
		
		
		/*On va maintenant initialiser la partie tournoi*/
		//Tournoi
		r = new Query(Query.getCalendar(), typeRequete.QUERY);
		rs = db.getData(r).getResultSet();
		HashMap<Integer, TypesTournament> calendrier = new HashMap<>();
		TypesTournament tournoi;
		Query req;
		ResultSet res;
		ArrayList<Integer> inscrits;
		while(rs.next()) {
			tournoi = new TypesTournament(rs.getDate("datelimiteinscription"), rs.getString("nom"), TypesFame.intToRenommee(rs.getInt("Renommee")), TypesGame.intToGame(rs.getInt("id_jeux")), rs.getInt("id_tournois"));
			req = new Query(Query.getRegistered(tournoi.getId()), typeRequete.QUERY);
			res = DatabaseAccess.getInstance().getData(req).getResultSet();
			inscrits = new ArrayList<>();
			while (res.next()) {
				inscrits.add(res.getInt("id_equipe"));
			}
			tournoi.setRegistered(inscrits);
			calendrier.put(rs.getInt("id_tournois"), tournoi);
		}
		this.data.setCalendar(calendrier);
			//Poule
				//Rencontre
		//Classement
	}
	
	
	public synchronized void miseAJourData(TypesID info, Types data) {
		System.out.println("MISE A JOUR DES DATA");
		ResponseObject r;
		HashMap<TypesID, Types> m = new HashMap<>();
		m.put(info, data);
		switch (info) {
		case PLAYER:
			TypesPlayer joueur = (TypesPlayer)data;
			this.data.getStables().get(joueur.getIdStable()).getTeams().get(joueur.getIdTeam()).modifyPlayer(joueur);
			r = new ResponseObject(Response.UPDATE_JOUEUR, m, null);
			sendAll(r);
			break;
		case TOURNAMENT:
			TypesTournament tournoi = (TypesTournament)data;
			this.data.getCalendar().put(tournoi.getId(), tournoi);
			r = new ResponseObject(Response.UPDATE_TOURNOI, m, null);
			System.out.println();
			sendAll(r);
			break;
		case TEAM:
			TypesTeam equipe = (TypesTeam)data;
			this.data.getStables().get(equipe.getStable().getId()).getTeams().put(equipe.getId(), equipe);
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
