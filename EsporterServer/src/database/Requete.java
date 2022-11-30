package database;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.Date;

import types.registerJoueur;

public class Requete {

	public enum typeRequete{FONCTION, REQUETE, PROCEDURE, INSERT, INSERTJOUEUR}
	
	
	private String requete;
	private typeRequete type;
	private InputStream inputStream;
	private Date[] dates = null;
	private int[] integers = null;
	public Requete(String requete, typeRequete type) {
		this.requete = requete;
		this.type = type;
	}
	
	public int[] getIntegers() {
		return integers;
	}
	
	public void setIntegers(int... integers) {
		this.integers = integers;
	}
	
	
	public void setDates(Date... dates) {
		this.dates = dates;
	}
	
	public Date[] getDates() {
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
	
	public static String getCalendrier() {
		return "select id_jeux, id_tournois, DateLimiteInscription, nom, renommee from cmf4263a.Tournoi";
	}
	
	public static String getInscris(int id_tournoi) {
		return "select id_equipe from cmf4263a.Participer where id_tournois = "+id_tournoi;
	}
	
	
	
	public static String getTournoiByID(int id) {
		return "select id_jeux, id_tournois, DateLimiteInscription, nom, renommee from cmf4263a.Tournoi where id_tournois = "+id;
	}
	
	
	public static String getEquipeByJoueur(int id) {
		return "select id_equipe from cmf4263a.Utilisateur where Utilisateur.Id_Utilisateur = "+id;
	}
	
	public static String getJeuxEquipe(int id) {
		return "select id_jeux from cmf4263a.Equipe where id_equipe= "+id;
	}
	
	/*
	public static String AjouterTournoi(Date dateLimiteInscription, String nom, Date dateTournoi,int Id_Tournois, String Renomm�e ) {
		return "INSERT INTO table VALUES (	"+ dateLimiteInscription +","+ nom +","+ dateTournoi +","+ Id_Tournois +","+ Renomm�e +");";
	*/
	public static String AjouterEquipe (int Id_Jeux, int Id_Ecurie ) {
		return "{? = call cmf4263a.insertEquipe("+Id_Jeux +","+ Id_Ecurie+")}";
	}	
	
	public static String getJeux (int id_tournoi) {
		return "select id_jeux from cmf4263a.Tournoi where id_tournois ="+id_tournoi;
	}
	
	public static String ajouterTournoi (int id_jeux, Date datelimite, String nom, int renommee) {
		return String.format("{? = call cmf4263a.inserttournoi(%d,"+datelimite+",%s,%d)}", id_jeux, nom, renommee);
	}
	
	public static String InscriptionTournoi(int Id_Jeux,int Id_Tournois , int Id_Equipe) {
		return "{call cmf4263a.INSCRIPTIONTOURNOI ("+ Id_Tournois +","+ Id_Jeux +","+Id_Equipe+")}";
	}
	
	public static String desinscriptionTournoi(int Id_Jeux, int Id_Tournoi, int Id_Equipe) {
		return "{call cmf4263a.DESINSCRIPTIONTOURNOI ("+ Id_Tournoi +","+ Id_Jeux +","+Id_Equipe+")}";
	}
	
	public static String getTitreBuEcurie(int id) {
		return "select libelle, dateobtention from cmf4263a.Titre, cmf4263a.Gagner where Titre.id_titre = Gagner.id_Titre and Gagner.id_Utilisateur = "+id;
	}
	
	public static String VoirInfosEcurie(int id) {
		return "Select NomEcurie, LogoEcurie, DiminutifEcurie from cmf4263a.Utilisateur WHERE	Id_Utilisateur 	=	"+ id;
	}
	
	public static String allEcurie() {
		return "Select Id_Utilisateur, NomEcurie, LogoEcurie, DiminutifEcurie from cmf4263a.Utilisateur where id_role=4";
	}
	
	public static String allJoueurByEquipe(int id) {
		return "select nomjoueur, prenomjoueur, photojoueur, datenaissancejoueur, datecontratjoueur, fincontratjoueur, u.id_equipe, id_nationalite from cmf4263a.Utilisateur u, cmf4263a.Equipe e where "+id+" = e.id_Utilisateur and e.id_equipe = u.id_equipe and id_role = 3";
	}
	
	public static String allEquipeByEcurie(int id) {
		return "select e.id_equipe, id_jeux, e.id_utilisateur from cmf4263a.Equipe e where "+id+" = e.id_Utilisateur";
	}
	
	public static String ajouterEcurie(String  username, String password, String NomEcurie, String DiminutifEcurie, String LogoEcurie) {
		return "{? = call cmf4263a.registerEcurie("+ username+","+ password +","+ NomEcurie +","+ LogoEcurie +","+ DiminutifEcurie +")}"	;

	}
	public static String AjouterJoueur(String username,  String password , String NomJoueur, String PrenomJoueur, int Id_Equipe, int Id_Nationalite) {
		return "{? = call cmf4263a.registerJoueur('"+ username + "','"+ password+"','"+NomJoueur+ "','"+PrenomJoueur+"',?,?,?,?,"+Id_Nationalite+","+Id_Equipe+")}";
	}
	
	public static String removeJoueurByEquipe(int id) {
		return "delete from cmf4263a.Utilisateur where id_equipe = "+id;
	}
	
	public static String removeEquipe(int id) {
		return "delete from cmf4263a.Equipe where id_equipe = "+id;
	}
	/*
	public static String VoirInfosEcurie4 (int IdEquipe) {

		return "Select  j.Nom as Nom_Joueur , Prenom, Photo, DateNaissance, Date_de_contrat, Fin_de_contrat, j.Nationalit� as Nationalit�_Joueur From Joueur j Where 	Joueur.Id_Equipe 	= 	IdEquipe;";

	}*/
	
	
	
	
	
	
	public String getRequete() {
		return requete;
	}
	
	public typeRequete getType() {
		return type;
	}
	
}
