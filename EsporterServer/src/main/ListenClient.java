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
import java.util.Iterator;
import java.util.Timer;

import javax.imageio.ImageIO;

import model.data.Data;
import model.database.DatabaseAccess;
import model.database.Query;
import model.database.Result;
import model.database.Query.typeRequete;
import model.socket.Command;
import model.socket.CommandName;
import model.socket.Response;
import model.socket.ResponseObject;
import types.TypesStable;
import types.TypesInteger;
import types.TypesTeam;
import types.TypesImage;
import types.TypesID;
import types.Types;
import types.TypesGame;
import types.TypesPlayer;
import types.TypesLogin;
import types.TypesPermission;
import types.TypesRegisterTeam;
import types.TypesFame;
import types.TypesTournament;
import types.TypesRegisterPlayer;

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
				e.printStackTrace();
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
			case ADD_TEAM:
				if(client.getRole()!=TypesPermission.STABLE) {
					errorPermission();
					break;
				}
				TypesRegisterTeam equipe = (TypesRegisterTeam) c.getInfoByID(TypesID.TEAM);
				ajouterEquipe(equipe);
				break;
			case MODIFY_TEAM:
				if(client.getRole()!=TypesPermission.STABLE) {
					errorPermission();
					break;
				}
				TypesTeam team = (TypesTeam) c.getInfoByID(TypesID.TEAM);
				modifyTeam(team);
				break;
			case ADD_TOURNAMENT:
				client.ajouterTournoi((TypesTournament)c.getInfoByID(TypesID.TOURNAMENT));
				break;
			case REGISTER_TOURNAMENT:
				inscriptionTournoi(((TypesInteger)c.getInfoByID(TypesID.TOURNAMENT)).getInteger(), ((TypesInteger)c.getInfoByID(TypesID.PLAYER)).getInteger());
				break;
			case UNREGISTER_TOURNAMENT:
				desinscriptionTournoi(((TypesInteger)c.getInfoByID(TypesID.TOURNAMENT)).getInteger(), ((TypesInteger)c.getInfoByID(TypesID.PLAYER)).getInteger(), ((TypesInteger)c.getInfoByID(TypesID.GAME)).getInteger());
				break;
			case CALENDAR:

				break;
			case STABLE:
				break;
			case DELETE_TOURNAMENT:
				deleteTournament(((TypesInteger)c.getInfoByID(TypesID.TOURNAMENT)).getInteger());
				break;
			case MODIFY_TOURNAMENT:
				modifyTournament(((TypesTournament)c.getInfoByID(TypesID.TOURNAMENT)));
				break;
			default:
			}

		} else {
			switch(c.getName()) {
			case LOGIN : 
				login(c);
				break;
			case CALENDAR:
				break;
			case STABLE:
				break;
			case INIT:

				Data d = mainThread.getInstance().getData();
				HashMap<TypesID,Types> m = new HashMap<>();
				m.put(TypesID.ALL, d);
				ResponseObject r = new ResponseObject(Response.UPDATE_ALL, m, null);
				client.send(r);
				System.out.println("Send init");
				break;
			default:
				errorPermission();
			}
		}
	}
	
	
	private void modifyTournament(TypesTournament t) {
		mainThread.getInstance().miseAJourData(TypesID.TOURNAMENT, t);
	}

	private void ajouterEquipe(TypesRegisterTeam equipe) {
		Result res = null;
		try {
			Query r = new Query(Query.addTeam(TypesGame.gameToInt(equipe.getGame()), equipe.getIdStable()), typeRequete.FUNCTION);
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

			Query temp = new Query(Query.getStableInfo(equipe.getIdStable()),typeRequete.QUERY);
			Result tempRes = DatabaseAccess.getInstance().getData(temp);
			ResultSet rs = tempRes.getResultSet();
			rs.next();
			BufferedImage bf1 = ImageIO.read(rs.getBinaryStream("logoecurie"));
			TypesImage im1 = new TypesImage(bf1, "png");

			TypesStable ecurie = new TypesStable(rs.getString("nomecurie"), im1, rs.getString("diminutifecurie"), equipe.getIdStable());

			TypesTeam eq = new TypesTeam(equipe.getGame(), ecurie, null,res.getInteger());
			HashMap<Integer, TypesPlayer> joueurs = new HashMap<>();
			for (TypesRegisterPlayer jou : equipe.getPlayers()) {
				TypesPlayer joueur = jou.getPlayer();
				Query reqJou = new Query(Query.addPlayer(jou.getLogin().getUsername(), jou.getLogin().getPassword(), joueur.getName(),joueur.getFirstName(), res.getInteger(), 1),typeRequete.INSERTPLAYER);
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				ImageIO.write(joueur.getImage().getImage(), "png", os);
				InputStream is = new ByteArrayInputStream(os.toByteArray());
				reqJou.setInputStream(is);
				reqJou.setDates(joueur.getBirthDate(), joueur.getContractStartDate(), joueur.getContractEndDate());
				Result resJou = DatabaseAccess.getInstance().getData(reqJou);
				if (resJou.isError()) {
					erreurAjoutEquipe(res.getInteger());
					error("Erreur dans la creation des equipes veuillez ressayyer plus tard");
					return;
				}
				joueur.setId(resJou.getInteger());
				joueurs.put(resJou.getInteger(), joueur);
			}
			eq.setPlayers(joueurs);
			mainThread.getInstance().miseAJourData(TypesID.TEAM, eq);

		} catch (InterruptedException | SQLException e) {
			erreurAjoutEquipe(res.getInteger());
			error("Erreur dans l'ajout de cette equipe, veuillez ressayer plus tard");
		} catch (IOException e) {
			erreurAjoutEquipe(res.getInteger());
			error("Erreur dans l'ajout de cette equipe, veuillez ressayer plus tard");
		}
	}

	private void erreurAjoutEquipe(int idEquipe) {
		try {
			Query r = new Query(Query.removePlayerByTeam(idEquipe),typeRequete.QUERY);
			DatabaseAccess.getInstance().getData(r);

			r = new Query(Query.removeTeam(idEquipe),typeRequete.QUERY);
			DatabaseAccess.getInstance().getData(r);
		} catch (InterruptedException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	public void modifyTeam(TypesTeam team) {
		Iterator<TypesPlayer> ite = team.getPlayers().values().iterator();
		while(ite.hasNext()) {
			TypesPlayer player = ite.next();
			Query r = new Query(Query.modifyPlayer(player.getFirstName(), player.getName(), player.getId()),typeRequete.MODIFYPLAYER);
			r.setDates(player.getBirthDate(),player.getContractStartDate(),player.getContractEndDate());
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				ImageIO.write(player.getImage().getImage(), "png", os);
			} catch (IOException e) {
				e.printStackTrace();
			}
			InputStream is = new ByteArrayInputStream(os.toByteArray());
			r.setInputStream(is);
			try {
				Result res = DatabaseAccess.getInstance().getData(r);
				if (res.isError()) {
					System.out.println("Error on insert");
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		mainThread.getInstance().miseAJourData(TypesID.TEAM, team);

	}
	
	public void deleteTournament(int t) {
		Query req = new Query(Query.removeTournament(t),typeRequete.QUERY);
		try {
			Result r = DatabaseAccess.getInstance().getData(req);
			if (r.isError()) {
				error("Le tournoi ne peut pas etre supprim�");
				return;
			}
			mainThread.getInstance().deleteData(TypesID.TOURNAMENT, new TypesInteger(t));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private void inscriptionTournoi(int id_Tournoi, int id_Joueur) {

		try {
			//Recuperer equipe
			Query r = new Query(Query.getTeamByPlayer(id_Joueur),typeRequete.QUERY);
			Result res = DatabaseAccess.getInstance().getData(r);
			res.getResultSet().next();
			int id_equipe = res.getResultSet().getInt("id_equipe");


			TypesTournament tournoi;
			r = new Query(Query.getTournamentByID(id_Tournoi), typeRequete.QUERY);
			res = DatabaseAccess.getInstance().getData(r);
			ResultSet rs = res.getResultSet();
			rs.next();
			tournoi = new TypesTournament(rs.getTimestamp("datelimiteinscription"), rs.getString("nom"), TypesFame.intToRenommee(rs.getInt("Renommee")), TypesGame.intToGame(rs.getInt("id_jeux")), rs.getInt("id_tournois"));

			Query requete = new Query(Query.getTeamGame(id_equipe), typeRequete.QUERY);
			ResultSet resultset = DatabaseAccess.getInstance().getData(requete).getResultSet();
			resultset.next();

			TypesGame jeuEquipe= TypesGame.intToGame(resultset.getInt("id_jeux"));

			System.out.println("Jeu tournoi :"+tournoi.getGame()+", jeux equipe : "+jeuEquipe);
			if (tournoi.getGame() != jeuEquipe) {
				error("Vous ne pouvez pas vous inscrire, ce jeu n'est celui de votre equipe");
				return;
			}

			
		r = new Query(Query.registerTournament(TypesGame.gameToInt(tournoi.getGame()), id_Tournoi, id_equipe), typeRequete.PROCEDURE);
		res = DatabaseAccess.getInstance().getData(r);
		if (res.isError()) {
			error("Vous etes deja inscrit");
			return;
		}





		tournoi.registerTeam(id_equipe);
		if(tournoi.isFull()) {
			//on charge le tournoi
		}

		//Il manque le get Poule

		mainThread.getInstance().miseAJourData(TypesID.TOURNAMENT, tournoi);

	} catch (InterruptedException | SQLException e) {
		e.printStackTrace();
		error("Vous etes deja inscrit");
	}
}

private void desinscriptionTournoi(int id_Tournoi, int id_Joueur, int id_Jeu) {
	try {
		//Recuperer equipe
		Query r = new Query(Query.getTeamByPlayer(id_Joueur),typeRequete.QUERY);
		Result res = DatabaseAccess.getInstance().getData(r);
		res.getResultSet().next();
		int id_equipe = res.getResultSet().getInt("id_equipe");

		r = new Query(Query.unregisterTournament(id_Jeu, id_Tournoi, id_equipe), typeRequete.PROCEDURE);
		res = DatabaseAccess.getInstance().getData(r);
		if (res.isError()) {
			error("Vous n'êtes pas inscrit");
			return;
		}


		TypesTournament tournoi;
		r = new Query(Query.getTournamentByID(id_Tournoi), typeRequete.QUERY);
		res = DatabaseAccess.getInstance().getData(r);
		ResultSet rs = res.getResultSet();
		rs.next();
		tournoi = new TypesTournament(rs.getTimestamp("datelimiteinscription"), rs.getString("nom"), TypesFame.intToRenommee(rs.getInt("Renommee")), TypesGame.intToGame(rs.getInt("id_jeux")), rs.getInt("id_tournois"));



		//TournoiInfo tournoi = mainThread.getInstance().getData().getCalendrier().get(id_Tournoi).clone();
		tournoi.unregisterTeam(id_equipe);

		mainThread.getInstance().miseAJourData(TypesID.TOURNAMENT, tournoi);


	} catch (InterruptedException | SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

}

private void logout() {
	client.setIsLogin(false);
	client.setPermission(TypesPermission.VISITOR);
}

private void errorPermission() {
	ResponseObject r = new ResponseObject(Response.ERROR_PERMISSION, null, null);
	client.send(r);
}

private void error(String s) {
	ResponseObject r = new ResponseObject(Response.ERROR, null, s);
	client.send(r);
}

private void ErrorLogin() {
	ResponseObject r = new ResponseObject(Response.ERROR_LOGIN, null, null);
	client.send(r);
}

private void login(Command c) {
	TypesLogin l = (TypesLogin) c.getInfoByID(TypesID.LOGIN);
	int result = client.login(l.getUsername(), l.getPassword());
	if (result == -1) {
		ErrorLogin();
	} else {
		try {
			Result res = DatabaseAccess.getInstance().getData(new Query(Query.getUserByID(result), typeRequete.QUERY));
			if (res.isError()) {
				ErrorLogin();
			}
			ResultSet rs = res.getResultSet();
			rs.next();
			int perm = rs.getInt("id_role");
			client.setPermission(perm);
			HashMap<TypesID,Types> m = new HashMap<>();
			switch (perm) {
			case 1:
				m.put(TypesID.PERMISSION, TypesPermission.ORGANIZER);
				break;
			case 2:
				m.put(TypesID.PERMISSION, TypesPermission.REFEREE);
				break;
			case 3:
				m.put(TypesID.PERMISSION, TypesPermission.PLAYER);
				Result r = DatabaseAccess.getInstance().getData(new Query(Query.getTeamByPlayer(result), typeRequete.QUERY));
				ResultSet resultset = r.getResultSet();
				resultset.next();
				int idTeam = resultset.getInt("id_equipe");
				r = DatabaseAccess.getInstance().getData(new Query(Query.getStableByTeam(idTeam), typeRequete.QUERY));
				resultset = r.getResultSet();
				resultset.next();
				
				BufferedImage bf = ImageIO.read(rs.getBinaryStream("photojoueur"));
				TypesImage im = new TypesImage(bf, "png");
				m.put(TypesID.PLAYER, new TypesPlayer(result, rs.getString("nomjoueur"), rs.getString("prenomjoueur"),im, rs.getTimestamp("datenaissancejoueur"), rs.getTimestamp("datecontratjoueur"), rs.getTimestamp("fincontratJoueur"), rs.getInt("id_nationalite"), rs.getInt("id_equipe"), resultset.getInt("id_utilisateur")));
				break;
			case 4:
				m.put(TypesID.PERMISSION, TypesPermission.STABLE);
				BufferedImage bf1 = ImageIO.read(rs.getBinaryStream("logoecurie"));
				TypesImage im1 = new TypesImage(bf1, "png");
				m.put(TypesID.STABLE, new TypesStable(rs.getString("nomecurie"), im1, rs.getString("diminutifecurie"), result));
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
