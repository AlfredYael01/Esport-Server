package model.database;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.Date;
import java.sql.Timestamp;

import types.TypesRegisterPlayer;

public class Query {

	public enum typeRequete{FUNCTION, QUERY, PROCEDURE, INSERT, INSERTPLAYER, MODIFYPLAYER}
	
	
	private String query;
	private typeRequete type;
	private InputStream inputStream;
	private Timestamp[] dates = null;
	private int[] integers = null;
	public Query(String requete, typeRequete type) {
		this.query = requete;
		this.type = type;
	}
	
	public int[] getIntegers() {
		return integers;
	}
	
	public void setIntegers(int... integers) {
		this.integers = integers;
	}
	
	
	public void setDates(Timestamp... dates) {
		this.dates = dates;
	}
	
	public Timestamp[] getDates() {
		return dates;
	}
	
	public static String Login(String user, String pass) {
		return "{? = call cmf4263a.loginapp('"+user+"', '"+pass+"')}";
	}
	
	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}
	
	public InputStream getInputStream() {
		return inputStream;
	}
	
	public static String getUserByID(int id) {
		return "select nomjoueur, prenomjoueur, photojoueur, datenaissancejoueur, datecontratjoueur, fincontratjoueur, nomecurie, logoecurie, diminutifecurie, id_role, id_nationalite, id_equipe from cmf4263a.utilisateur where id_utilisateur = "+id;
	}
	
	public static String getCalendar() {
		return "select id_jeux, id_tournois, DateLimiteInscription, nom, renommee from cmf4263a.Tournoi";
	}
	
	public static String getRegistered(int id_tournoi) {
		return "select id_equipe from cmf4263a.Participer where id_tournois = "+id_tournoi;
	}
	
	
	
	public static String getTournamentByID(int id) {
		return "select id_jeux, id_tournois, DateLimiteInscription, nom, renommee from cmf4263a.Tournoi where id_tournois = "+id;
	}
	
	
	public static String getTeamByPlayer(int id) {
		return "select id_equipe from cmf4263a.Utilisateur where Utilisateur.Id_Utilisateur = "+id;
	}
	
	public static String getStableByTeam(int id) {
		return "Select equipe.id_utilisateur from cmf4263a.Equipe where Equipe.id_equipe = "+id; 
	}
	
	public static String getTeamGame(int id) {
		return "select id_jeux from cmf4263a.Equipe where id_equipe= "+id;
	}
	
	/*
	public static String AjouterTournoi(Date dateLimiteInscription, String nom, Date dateTournoi,int Id_Tournois, String Renomm�e ) {
		return "INSERT INTO table VALUES (	"+ dateLimiteInscription +","+ nom +","+ dateTournoi +","+ Id_Tournois +","+ Renomm�e +");";
	*/
	public static String addTeam (int Id_Jeux, int Id_Ecurie ) {
		return "{? = call cmf4263a.insertEquipe("+Id_Jeux +","+ Id_Ecurie+")}";
	}	
	
	public static String getGame (int id_tournoi) {
		return "select id_jeux from cmf4263a.Tournoi where id_tournois ="+id_tournoi;
	}
	
	public static String addTournament (int id_jeux, String nom, int renommee) {
		return String.format("{? = call cmf4263a.inserttournoi(%d,?,'%s',%d)}", id_jeux, nom, renommee);
	}
	
	public static String registerTournament(int Id_Jeux,int Id_Tournois , int Id_Equipe) {
		return "{call cmf4263a.INSCRIPTIONTOURNOI ("+ Id_Tournois +","+ Id_Jeux +","+Id_Equipe+")}";
	}
	
	public static String unregisterTournament(int Id_Jeux, int Id_Tournoi, int Id_Equipe) {
		return "{call cmf4263a.DESINSCRIPTIONTOURNOI ("+ Id_Tournoi +","+ Id_Jeux +","+Id_Equipe+")}";
	}
	
	public static String getTitleByStable(int id) {
		return "select libelle, dateobtention from cmf4263a.Titre, cmf4263a.Gagner where Titre.id_titre = Gagner.id_Titre and Gagner.id_Utilisateur = "+id;
	}
	
	public static String getStableInfo(int id) {
		return "Select NomEcurie, LogoEcurie, DiminutifEcurie from cmf4263a.Utilisateur WHERE	Id_Utilisateur 	=	"+ id;
	}
	
	public static String allStables() {
		return "Select Id_Utilisateur, NomEcurie, LogoEcurie, DiminutifEcurie from cmf4263a.Utilisateur where id_role=4";
	}
	
	public static String allPlayerByTeam(int id) {
		return "select id_utilisateur, nomjoueur, prenomjoueur, photojoueur, datenaissancejoueur, datecontratjoueur, fincontratjoueur from cmf4263a.Utilisateur where "+id+" = id_equipe and id_role = 3";
	}
	
	public static String allTeamByStables(int id) {
		return "select e.id_equipe, id_jeux, e.id_utilisateur from cmf4263a.Equipe e where "+id+" = e.id_Utilisateur";
	}
	
	public static String addStable(String  username, String password, String NomEcurie, String DiminutifEcurie, String LogoEcurie) {
		return "{? = call cmf4263a.registerEcurie("+ username+","+ password +","+ NomEcurie +","+ LogoEcurie +","+ DiminutifEcurie +")}"	;

	}
	public static String addPlayer(String username,  String password , String NomJoueur, String PrenomJoueur, int Id_Equipe, int Id_Nationalite) {
		return "{? = call cmf4263a.registerJoueur('"+ username + "','"+ password+"','"+NomJoueur+ "','"+PrenomJoueur+"',?,?,?,?,"+Id_Nationalite+","+Id_Equipe+")}";
	}
	
	public static String removePlayerByTeam(int id) {
		return "delete from cmf4263a.Utilisateur where id_equipe = "+id;
	}
	
	public static String removeTeam(int id) {
		return "delete from cmf4263a.Equipe where id_equipe = "+id;
	}
	
	public static String modifyPlayer(String prenom, String nom, int id_Utilisateur) {
		return "{call cmf4263a.modifierjoueur('"+nom+"','"+prenom+"',?,?,?,?,"+id_Utilisateur+")}";
	}
	public static String removeTournament(int Id_Tournoi) {
		return "delete from cmf4263a.Tournoi where id_tournois = "+Id_Tournoi; 
	}
	
	
	public static String getPool(int tournementId, int gameId) {
		return "select * from cmf4263a.Poule where id_tournois = "+tournementId+" and id_jeux = "+gameId+" order by id_poule";
	}
	
	public static String getEquipeParPool(int pool, int tournament, int game) {
		return String.format("select affilier.id_equipe, affilier.id_poule, point, equipe.id_utilisateur  from cmf4263a.affilier, cmf4263a.equipe where equipe.id_equipe = affilier.id_equipe and id_poule = %d and affilier.id_tournois = %d and affilier.id_jeux = %d", pool, tournament, game);
	}
	
	/*
	public static String VoirInfosEcurie4 (int IdEquipe) {

		return "Select  j.Nom as Nom_Joueur , Prenom, Photo, DateNaissance, Date_de_contrat, Fin_de_contrat, j.Nationalit� as Nationalit�_Joueur From Joueur j Where 	Joueur.Id_Equipe 	= 	IdEquipe;";

	}*/
	
	
	public static String getMatchs(int idTournament, int idPool, int idJeux) {
		return String.format("select id_equipeA, id_equipeB, nombrePointEquipe1, nombrePointEquipe2, Gagnant, DateMatch,id_rencontre from cmf4263a.rencontre where id_tournois = %d and id_poule = %d and id_jeux = %d",idTournament, idPool, idJeux);
	}
	
	public static String getAllRanking() {
		return ("select id_classement, id_jeux from cmf4263a.classement");
	}
	
	public static String getRankingByUserByGame(int id_user, int id_classement) {
		return String.format("select nombrepoint from cmf4263a.appartenir where id_utilisateur = %d and id_classement = %d", id_user, id_classement);
	}
	
	public static String setScoreA(int idWinner, int idTournament, int idPool, int idEquipeA, int idEquipeB, int idJeux) {
		return String.format("update cmf4263a.rencontre set gagnant = %d, nombrepointequipe1 = 1, nombrepointequipe2=0 where id_tournois = %d and id_jeux = %d and id_poule = %d and id_equipeA = %d and id_equipeB = %d", idWinner, idTournament, idJeux, idPool, idEquipeA, idEquipeB);
	}
	
	public static String setScoreB(int idWinner, int idTournament, int idPool, int idEquipeA, int idEquipeB, int idJeux) {
		return String.format("update cmf4263a.rencontre set gagnant = %d, nombrepointequipe1 = 0, nombrepointequipe2=1 where id_tournois = %d and id_jeux = %d and id_poule = %d and id_equipeA = %d and id_equipeB = %d", idWinner, idTournament, idJeux, idPool, idEquipeA, idEquipeB);
	}
	
	public static String remplissagePoule(int idPoule, int idTournoi, int Idjeux, int idWinner, int idA, int idB) {
		return String.format("{ call cmf4263a.remplissagePoule(%d,%d,%d,%d,%d,%d)}", idPoule,idTournoi,Idjeux,idWinner,idA,idB);
	}
	
	public static String rencontrepouleFinale(int idTournoi, int idJeux) {
		return String.format("{ call cmf4263a.rencontrepoule(%d,%d)}",idJeux, idTournoi);
	}
	
	public static String setScore(int idUser, int idJeux, int score) {
		return String.format("{ call cmf4263a.setscore(%d,%d,%d)}",score, idJeux, idUser);
	}
	
	
	
	
	
	public String getQuery() {
		return query;
	}
	
	public typeRequete getType() {
		return type;
	}
	
}
