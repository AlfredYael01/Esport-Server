package main;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;

import javax.imageio.ImageIO;

import data.Data;
import database.DatabaseAccess;
import database.Requete;
import database.Requete.typeRequete;
import database.Result;
import socket.Command;
import socket.CommandName;
import socket.Response;
import socket.ResponseObject;
import types.EcurieInfo;
import types.Entier;
import types.EquipeInfo;
import types.Image;
import types.InfoID;
import types.Infos;
import types.Jeu;
import types.JoueurInfo;
import types.Login;
import types.Permission;
import types.RegisterEquipe;
import types.Renomme;
import types.TournoiInfo;
import types.registerJoueur;

public class ListenClient implements Runnable{
	
	private ConnectionClient client;
	public Boolean run = true;
	public ListenClient(ConnectionClient client) {
		this.client = client;
	}
	
	@Override
	public void run() {
		ObjectInputStream in = client.getIn();
		while (run) {
			try {
				Object o = in.readObject();
				commande(o);
				
			} catch (ClassNotFoundException e) {
			} catch (IOException e) {
				System.out.println("fin d'un thread");
				mainThread.getInstance().closeClient(client);
				break;
			}
		}
	}
	
	public void commande(Object o) {
		Command c = null;
		
		if (o instanceof Command)
			c = (Command)o;
		System.out.println("Message recu : "+c.getName());
		if(client.getIsLogin()) {
			
			switch(c.getName()) {
				case LOGOUT : 
					logout();
					break;
				case AJOUTER_EQUIPE:
					if(client.getRole()!=Permission.ECURIE) {
						errorPermission();
						break;
					}
					RegisterEquipe equipe = (RegisterEquipe) c.getInfoByID(InfoID.Equipe);
					ajouterEquipe(equipe);
					break;
				case AJOUTER_TOURNOI:
					client.ajouterTournoi((TournoiInfo)c.getInfoByID(InfoID.Tournoi));
					break;
				case INSCRIPTION_TOURNOI:
					inscriptionTournoi(((Entier)c.getInfoByID(InfoID.Tournoi)).getEntier(), ((Entier)c.getInfoByID(InfoID.Joueur)).getEntier());
					break;
				case DESINSCRIPTION_TOURNOI:
					desinscriptionTournoi(((Entier)c.getInfoByID(InfoID.Tournoi)).getEntier(), ((Entier)c.getInfoByID(InfoID.Joueur)).getEntier(), ((Entier)c.getInfoByID(InfoID.Jeu)).getEntier());
					break;
				case VOIR_CALENDRIER:
					
					break;
				case VOIR_ECURIE:
					break;
				default:
			}
			
		} else {
			switch(c.getName()) {
				case LOGIN : 
					login(c);
					break;
				case VOIR_CALENDRIER:
					break;
				case VOIR_ECURIE:
					break;
				case INIT:
					
					Data d = mainThread.getInstance().getData();
					HashMap<InfoID,Infos> m = new HashMap<>();
					m.put(InfoID.all, d);
					ResponseObject r = new ResponseObject(Response.UPDATE_ALL, m, null);
					client.send(r);
					System.out.println("Send init");
					break;
				default:
					errorPermission();
			}
		}
	}
	
	private void ajouterEquipe(RegisterEquipe equipe) {
		Result res = null;
		try {
			Requete r = new Requete(Requete.AjouterEquipe(Jeu.jeuToInt(equipe.getJeu()), equipe.getIdEcurie()), typeRequete.FONCTION);
			res = DatabaseAccess.getInstance().getData(r);
			if (res.isError()) {
				error("Erreur dans la creation des equipes veuillez ressayyer plus tard");
			}
		} catch (InterruptedException | SQLException e) {
			e.printStackTrace();
			error("Erreur dans la creation des equipes veuillez ressayyer plus tard");
			return;
		}
		try {
			
			Requete temp = new Requete(Requete.VoirInfosEcurie(equipe.getIdEcurie()),typeRequete.REQUETE);
			Result tempRes = DatabaseAccess.getInstance().getData(temp);
			ResultSet rs = tempRes.getResultSet();
			rs.next();
			BufferedImage bf1 = ImageIO.read(rs.getBinaryStream("logoecurie"));
			Image im1 = new Image(bf1, "png");
			 
			EcurieInfo ecurie = new EcurieInfo(rs.getString("nomecurie"), im1, rs.getString("diminutifecurie"), equipe.getIdEcurie());
			
			EquipeInfo eq = new EquipeInfo(equipe.getJeu(), ecurie, null,res.getEntier());
			HashMap<Integer, JoueurInfo> joueurs = new HashMap<>();
			for (registerJoueur jou : equipe.getJoueurs()) {
				JoueurInfo joueur = jou.getJoueur();
				Requete reqJou = new Requete(Requete.AjouterJoueur(jou.getLogin().getUsername(), jou.getLogin().getPassword(), joueur.getNom(),joueur.getPrenom(), res.getEntier(), 1),typeRequete.INSERTJOUEUR);
				ByteArrayOutputStream os = new ByteArrayOutputStream();
	            ImageIO.write(joueur.getPhoto().getImage(), "png", os);
	            InputStream is = new ByteArrayInputStream(os.toByteArray());
				reqJou.setInputStream(is);
				reqJou.setDates(joueur.getDateNaissance(), joueur.getDateDebutContrat(), joueur.getDateFinContrat());
				Result resJou = DatabaseAccess.getInstance().getData(reqJou);
				if (resJou.isError()) {
					erreurAjoutEquipe(res.getEntier());
					error("Erreur dans la creation des equipes veuillez ressayyer plus tard");
					return;
				}
				joueur.setId(resJou.getEntier());
				joueurs.put(resJou.getEntier(), joueur);
			}
			eq.setJoueurs(joueurs);
			mainThread.getInstance().miseAJourData(InfoID.Equipe, eq);
				
		} catch (InterruptedException | SQLException e) {
			erreurAjoutEquipe(res.getEntier());
			error("Erreur dans l'ajout de cette equipe, veuillez ressayer plus tard");
		} catch (IOException e) {
			erreurAjoutEquipe(res.getEntier());
			error("Erreur dans l'ajout de cette equipe, veuillez ressayer plus tard");
		}
	}
	
	private void erreurAjoutEquipe(int idEquipe) {
		try {
			Requete r = new Requete(Requete.removeJoueurByEquipe(idEquipe),typeRequete.REQUETE);
			Result res = DatabaseAccess.getInstance().getData(r);
			
			r = new Requete(Requete.removeEquipe(idEquipe),typeRequete.REQUETE);
			res = DatabaseAccess.getInstance().getData(r);
		} catch (InterruptedException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	private void inscriptionTournoi(int id_Tournoi, int id_Joueur) {
		
		try {
			//Recuperer equipe
			Requete r = new Requete(Requete.getEquipeByJoueur(id_Joueur),typeRequete.REQUETE);
			Result res = DatabaseAccess.getInstance().getData(r);
			res.getResultSet().next();
			int id_equipe = res.getResultSet().getInt("id_equipe");
			
			
			TournoiInfo tournoi;
			r = new Requete(Requete.getTournoiByID(id_Tournoi), typeRequete.REQUETE);
			res = DatabaseAccess.getInstance().getData(r);
			ResultSet rs = res.getResultSet();
			rs.next();
			tournoi = new TournoiInfo(rs.getDate("datelimiteinscription"), rs.getString("nom"), Renomme.intToRenommee(rs.getInt("Renommee")), Jeu.intToJeu(rs.getInt("id_jeux")), rs.getInt("id_tournois"));
			
			Requete requete = new Requete(Requete.getJeuxEquipe(id_equipe), typeRequete.REQUETE);
			ResultSet resultset = DatabaseAccess.getInstance().getData(requete).getResultSet();
			resultset.next();
			
			Jeu jeuEquipe= Jeu.intToJeu(resultset.getInt("id_jeux"));
			
			System.out.println("Jeu tournoi :"+tournoi.getJeux()+", jeux equipe : "+jeuEquipe);
			if (tournoi.getJeux() != jeuEquipe) {
				error("Vous ne pouvez pas vous inscrire, ce jeu n'est celui de votre equipe");
				return;
			}
			
			
			
			r = new Requete(Requete.InscriptionTournoi(Jeu.jeuToInt(tournoi.getJeux()), id_Tournoi, id_equipe), typeRequete.PROCEDURE);
			res = DatabaseAccess.getInstance().getData(r);
			if (res.isError()) {
				error("Vous etes deja inscrit");
				return;
			}
			
			
			
			
			
			r = new Requete(Requete.getInscris(id_Tournoi), typeRequete.REQUETE);
			res = DatabaseAccess.getInstance().getData(r);
			rs = res.getResultSet();
			ArrayList<Integer> inscrits = new ArrayList<>();
			while (rs.next()) {
				inscrits.add(rs.getInt("id_equipe"));
			}
			tournoi.setInscris(inscrits);
			
			//Il manque le get Poule
			
			mainThread.getInstance().miseAJourData(InfoID.Tournoi, tournoi);
			
		} catch (InterruptedException | SQLException e) {
			e.printStackTrace();
			error("Vous etes deja inscrit");
		}
	}
	
	private void desinscriptionTournoi(int id_Tournoi, int id_Joueur, int id_Jeu) {
		try {
			//Recuperer equipe
			Requete r = new Requete(Requete.getEquipeByJoueur(id_Joueur),typeRequete.REQUETE);
			Result res = DatabaseAccess.getInstance().getData(r);
			res.getResultSet().next();
			int id_equipe = res.getResultSet().getInt("id_equipe");
			
			r = new Requete(Requete.desinscriptionTournoi(id_Jeu, id_Tournoi, id_equipe), typeRequete.PROCEDURE);
			res = DatabaseAccess.getInstance().getData(r);
			if (res.isError()) {
				error("Vous n'êtes pas inscrit");
				return;
			}
			
			
		} catch (InterruptedException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void logout() {
		client.setIsLogin(false);
		client.setPermission(Permission.VISITEUR);
	}
	
	private void errorPermission() {
		ResponseObject r = new ResponseObject(Response.ERROR_PERMISSION, null, null);
		client.send(r);
	}
	
	private void error(String s) {
		ResponseObject r = new ResponseObject(Response.Error, null, s);
		client.send(r);
	}
	
	private void ErrorLogin() {
		ResponseObject r = new ResponseObject(Response.ERROR_LOGIN, null, null);
		client.send(r);
	}
	
	private void login(Command c) {
		Login l = (Login) c.getInfoByID(InfoID.login);
		int result = client.login(l.getUsername(), l.getPassword());
		if (result == -1) {
			ErrorLogin();
		} else {
			try {
				Result res = DatabaseAccess.getInstance().getData(new Requete(Requete.getUserByID(result), typeRequete.REQUETE));
				if (res.isError()) {
					ErrorLogin();
				}
				ResultSet rs = res.getResultSet();
				rs.next();
				int perm = rs.getInt("id_role");
				client.setPermission(perm);
				HashMap<InfoID,Infos> m = new HashMap<>();
				switch (perm) {
				case 1:
					m.put(InfoID.Permission, Permission.ORGANISATEUR);
					break;
				case 2:
					m.put(InfoID.Permission, Permission.ARBITRE);
					break;
				case 3:
					m.put(InfoID.Permission, Permission.JOUEUR);
					Result r = DatabaseAccess.getInstance().getData(new Requete(Requete.getEquipeByJoueur(result), typeRequete.REQUETE));
					ResultSet resultset = r.getResultSet();
					resultset.next();
					BufferedImage bf = ImageIO.read(rs.getBinaryStream("photojoueur"));
					Image im = new Image(bf, "png");
					m.put(InfoID.Joueur, new JoueurInfo(result, rs.getString("nomjoueur"), rs.getString("prenomjoueur"),im, rs.getDate("datenaissancejoueur"), rs.getDate("datecontratjoueur"), rs.getDate("fincontratJoueur"), rs.getInt("id_nationalite"), rs.getInt("id_equipe"), resultset.getInt("id_equipe")));
					break;
				case 4:
					m.put(InfoID.Permission, Permission.ECURIE);
					BufferedImage bf1 = ImageIO.read(rs.getBinaryStream("logoecurie"));
					Image im1 = new Image(bf1, "png");
					m.put(InfoID.Ecurie, new EcurieInfo(rs.getString("nomecurie"), im1, rs.getString("diminutifecurie"), result));
					break;
				
				}
				ResponseObject r = new ResponseObject(Response.LOGIN, m, null);
				client.send(r);
				client.setIsLogin(true);
			} catch (Exception e) {
				e.printStackTrace();
				ErrorLogin();
			}

			
			
			
		}
	}

}
