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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

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
import types.TypesPool;
import types.TypesRanking;
import types.TypesLogin;
import types.TypesMatch;
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
				//register new Stable
				registerStable(c);
				break;
			case DELETE_TOURNAMENT:
				deleteTournament(((TypesInteger)c.getInfoByID(TypesID.TOURNAMENT)).getInteger());
				break;
			case MODIFY_TOURNAMENT:
				modifyTournament(((TypesTournament)c.getInfoByID(TypesID.TOURNAMENT)));
				break;
			case SCORE:
				changeScore(c);
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
				registerStable(c);
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
	
	
	private void registerStable(Command c) {
		TypesStable s = (TypesStable) c.getInfoByID(TypesID.STABLE);
		TypesLogin l = (TypesLogin) c.getInfoByID(TypesID.LOGIN);
		
		Query q = new Query(Query.addStable(l.getUsername(), l.getPassword(), s.getName(), s.getNickname()),typeRequete.INSERTPLAYER);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			ImageIO.write(s.getLogo().getImage(), "png", os);
		} catch (IOException e) {
			e.printStackTrace();
		}
		InputStream is = new ByteArrayInputStream(os.toByteArray());
		q.setInputStream(is);
		
		try {
			Result r = DatabaseAccess.getInstance().getData(q);
			if(r.isError()) {
				error("Impossible de s'inscrire");
				return;
			}
			
			int id = r.getInteger();
			s.setId(id);
			mainThread.getInstance().getData().getStables().put(id, s);
			
			HashMap<TypesID, Types> m = new HashMap<>();
			m.put(TypesID.STABLE, s);
			ResponseObject res = new ResponseObject(Response.UPDATE_STABLE,m,null);
			mainThread.getInstance().sendAll(res);
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	private void changeScore(Command c) {
		int idTournoi = ((TypesInteger)c.getInfoByID(TypesID.TOURNAMENT)).getInteger();
		int Pool = ((TypesInteger)c.getInfoByID(TypesID.POOL)).getInteger();
		TypesMatch match = ((TypesMatch)c.getInfoByID(TypesID.MATCH));
		TypesGame jeu = mainThread.getInstance().getData().getCalendar().get(match.getIdTournament()).getGame();
		Query q;
		System.out.println("Gagnant = "+match.getWinner());
		if(match.getWinner() == match.getTeam1()) {
			q = new Query(Query.setScoreA(match.getWinner(), idTournoi, Pool, match.getTeam1(), match.getTeam2(), TypesGame.gameToInt(jeu)), typeRequete.QUERY);
		}else {
			q = new Query(Query.setScoreB(match.getWinner(), idTournoi, Pool, match.getTeam1(), match.getTeam2(), TypesGame.gameToInt(jeu)), typeRequete.QUERY);
		}
		try {
			DatabaseAccess.getInstance().getData(q);
			DatabaseAccess.getInstance().getData(new Query(Query.remplissagePoule(Pool, idTournoi, TypesGame.gameToInt(jeu), match.getWinner(), match.getTeam1(), match.getTeam2()),typeRequete.PROCEDURE));
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		TypesPool pool = mainThread.getInstance().getData().getCalendar().get(idTournoi).getPool().get(Pool-1);
		for(TypesMatch m : pool.getMatchs()) {
			if(m.equals(match)) {
				m.setPoint(match.getTeam1Score(), match.getTeam2Score());
				break;
			}
		}
		
		
		
		TypesTournament t = mainThread.getInstance().getData().getCalendar().get(match.getIdTournament());
		ArrayList<TypesPool> listP = t.getPool();
		
		for(TypesPool p : listP) {
			if(p.getId() == match.getIdPool()) {
				p.getPoint().put(mainThread.getInstance().getData().getTeams().get(match.getWinner()), pool.getPoint().get(mainThread.getInstance().getData().getTeams().get(match.getWinner()+1)));
			}
		}
		
		try {
			listP = mainThread.getInstance().getPool(t, TypesGame.gameToInt(t.getGame()));
			mainThread.getInstance().getData().getCalendar().get(t.getId()).setPool(listP);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		TypesPool p = listP.get(4);
		if(p.getPoint().size()!=4 && !p.poolFinished()) {
			
			try {
				DatabaseAccess.getInstance().getData(new Query(Query.rencontrepouleFinale(idTournoi, Pool),typeRequete.PROCEDURE));
				listP = mainThread.getInstance().getPool(t, TypesGame.gameToInt(t.getGame()));
				mainThread.getInstance().getData().getCalendar().get(t.getId()).setPool(listP);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	
		
		/*HashMap<TypesID, Types> m = new HashMap<>();
		m.put(TypesID.MATCH, match);
		m.put(TypesID.TOURNAMENT, new TypesInteger(idTournoi));
		m.put(TypesID.POOL, new TypesInteger(Pool));
		
		ResponseObject r = new ResponseObject(Response.UP,m,null);*/
		
		HashMap<TypesID, Types> m = new HashMap<>();
		m.put(TypesID.TOURNAMENT, mainThread.getInstance().getData().getCalendar().get(t.getId()));
		ResponseObject r = new ResponseObject(Response.UPDATE_TOURNAMENT,m,null);
		
		
		mainThread.getInstance().sendAll(r);
		
		TypesTournament tournois = mainThread.getInstance().getData().getCalendar().get(t.getId());
		if (tournois.getPool().get(4).poolFinished()) {
			int multiplicateur = 1;
			int point = 100;

			//100 60 30 10
			switch(tournois.getFame()) {
			case INTERNATIONAL:
				multiplicateur = 3;
				break;
			case NATIONAL:
				multiplicateur = 2;
				break;
			case LOCAL:
				multiplicateur = 1;
				break;
			}
			
			Comparator<TypesTeam> comp = new Comparator<TypesTeam>() {
				
				@Override
				public int compare(TypesTeam o1, TypesTeam o2) {
					if(tournois.getPool().get(4).getPoint().get(o1).compareTo(tournois.getPool().get(4).getPoint().get(o2))==0 ) {
						return o1.getId() - (o2.getId());
					}
					return tournois.getPool().get(4).getPoint().get(o1).compareTo(tournois.getPool().get(4).getPoint().get(o2));
				}
			};
			
			List<TypesTeam> listeTeamSort = new ArrayList<>(tournois.getPool().get(4).getPoint().keySet());
			Collections.sort(listeTeamSort, comp.reversed());
			int passage = 1;
			for(TypesTeam team : listeTeamSort) {
				switch(passage) {
				case 1:
					point = 100*multiplicateur;
					break;
				case 2:
					point = 60*multiplicateur;
					break;
				case 3:
					point = 30*multiplicateur;
					break;
				case 4:
					point = 10*multiplicateur;
					break;
				}
				passage++;
				try {
					DatabaseAccess.getInstance().getData(new Query(Query.setScore(team.getStable().getId(),TypesGame.gameToInt(jeu) , point),typeRequete.PROCEDURE));
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			for(TypesRanking ranking : mainThread.getInstance().getData().getRanking().values()) {
				if(ranking.getGame()==jeu) {
					mainThread.getInstance().getData().getRanking().get(ranking.getId()).setStables(mainThread.getInstance().getCalendar(ranking.getId()));
					HashMap<TypesID, Types> m2 = new HashMap<>();
					m2.put(TypesID.RANKING, mainThread.getInstance().getData().getRanking().get(ranking.getId()));
					ResponseObject r2 = new ResponseObject(Response.UPDATE_RANKING,m2,null);
					mainThread.getInstance().sendAll(r2);
					break;
				}
			}
			
			
		}
		
		
	}
	
	
	private void modifyTournament(TypesTournament t) {
		mainThread.getInstance().getData().getCalendar().put(t.getId(), t);
		HashMap<TypesID, Types> m = new HashMap<>();
		m.put(TypesID.TOURNAMENT, t);
		mainThread.getInstance().miseAJourData(m);
	}

	private void ajouterEquipe(TypesRegisterTeam equipe) {
		Result res = null;
		try {
			Query r = new Query(Query.addTeam(TypesGame.gameToInt(equipe.getGame()), equipe.getIdStable()), typeRequete.FUNCTION);
			res = DatabaseAccess.getInstance().getData(r);
			if (res.isError()) {
				error("Erreur dans la creation des equipes veuillez ressayer plus tard");
			}
		} catch (InterruptedException | SQLException e) {
			e.printStackTrace();
			error("Erreur dans la creation des equipes veuillez ressayer plus tard");
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
			rs.close();
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
					error("Erreur dans la creation des equipes veuillez ressayer plus tard");
					return;
				}
				joueur.setId(resJou.getInteger());
				joueurs.put(resJou.getInteger(), joueur);
			}
			eq.setPlayers(joueurs);
			mainThread.getInstance().getData().getStables().get(eq.getStable().getId()).getTeams().put(eq.getId(), eq);
			HashMap<TypesID, Types> m = new HashMap<>();
			m.put(TypesID.TEAM, eq);
			mainThread.getInstance().miseAJourData(m);

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
		mainThread.getInstance().getData().getStables().get(team.getStable().getId()).getTeams().put(team.getId(), team);
		HashMap<TypesID, Types> m = new HashMap<>();
		m.put(TypesID.TEAM, team);
		mainThread.getInstance().miseAJourData(m);

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
			res.getResultSet().close();

			TypesTournament tournoi;
			r = new Query(Query.getTournamentByID(id_Tournoi), typeRequete.QUERY);
			res = DatabaseAccess.getInstance().getData(r);
			ResultSet rs = res.getResultSet();
			rs.next();
			tournoi = mainThread.getInstance().getData().getCalendar().get(id_Tournoi);
			rs.close();
			Query requete = new Query(Query.getTeamGame(id_equipe), typeRequete.QUERY);
			ResultSet resultset = DatabaseAccess.getInstance().getData(requete).getResultSet();
			resultset.next();

			TypesGame jeuEquipe= TypesGame.intToGame(resultset.getInt("id_jeux"));
			resultset.close();

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
			System.out.println("Tournoi full, chargement de la poule");
			TimeUnit.SECONDS.sleep(5);
			tournoi.setPool(mainThread.getInstance().getPool(tournoi, TypesGame.gameToInt(tournoi.getGame())));
		}

		//Il manque le get Poule

		mainThread.getInstance().getData().getCalendar().put(tournoi.getId(), tournoi);
		HashMap<TypesID, Types> m = new HashMap<>();
		m.put(TypesID.TOURNAMENT, tournoi);
		mainThread.getInstance().miseAJourData(m);

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
		res.getResultSet().close();
		
		if(mainThread.getInstance().getData().getCalendar().get(id_Tournoi).isFull()) {
			error("Le tournoi a deja commencé vous ne pouvez plus vous desinscrire");
			
		}

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
		rs.close();


		//TournoiInfo tournoi = mainThread.getInstance().getData().getCalendrier().get(id_Tournoi).clone();
		tournoi.unregisterTeam(id_equipe);

		mainThread.getInstance().getData().getCalendar().put(tournoi.getId(), tournoi);
		HashMap<TypesID, Types> m = new HashMap<>();
		m.put(TypesID.TOURNAMENT, tournoi);
		mainThread.getInstance().miseAJourData(m);


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
				m.put(TypesID.PLAYER, new TypesPlayer(result, rs.getString("nomjoueur"), rs.getString("prenomjoueur"),im, rs.getTimestamp("datenaissancejoueur"), rs.getTimestamp("datecontratjoueur"), rs.getTimestamp("fincontratJoueur"), rs.getInt("id_nationalite"), rs.getInt("id_equipe"), resultset.getInt("id_utilisateur"), rs.getString("username")));
				break;
			case 4:
				m.put(TypesID.PERMISSION, TypesPermission.STABLE);
				BufferedImage bf1 = ImageIO.read(rs.getBinaryStream("logoecurie"));
				TypesImage im1 = new TypesImage(bf1, "png");
				m.put(TypesID.STABLE, new TypesStable(rs.getString("nomecurie"), im1, rs.getString("diminutifecurie"), result));
				break;

			}
			rs.close();
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
