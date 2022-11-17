package database;

import java.sql.Blob;
import java.sql.Date;

public class Requete {

	public enum typeRequete{FONCTION, REQUETE, PROCEDURE, INSERT}
	
	
	private String requete;
	private typeRequete type;
	public Requete(String requete, typeRequete type) {
		this.requete = requete;
		this.type = type;
	}
	
	public static String Login(String user, String pass) {
		return "{? = call cmf4263a.loginapp('"+user+"', '"+pass+"')}";
	}
	
	public static String getUserByID(int id) {
		return "select nomjoueur, prenomjoueur, photojoueur, datenaissancejoueur, datecontratjoueur, fincontratjoueur, nomecurie, logoecurie, diminutifecurie, id_role, id_nationalite, id_equipe from cmf4263a.utilisateur where id_utilisateur = "+id;
	}
	
	/*
	public static String AjouterTournoi(Date dateLimiteInscription, String nom, Date dateTournoi,int Id_Tournois, String Renommée ) {
		return "INSERT INTO table VALUES (	"+ dateLimiteInscription +","+ nom +","+ dateTournoi +","+ Id_Tournois +","+ Renommée +");";
	*/
	public static String AjouterEquipe (int Id_Jeux, int Id_Ecurie ) {
		return "{? = call cmf4263a.insertEquipe("+Id_Jeux +","+ Id_Ecurie+")}";
	}	
	
	public static String InscriptionTournoi(int Id_Jeux,int Id_Tournois , int Id_Equipe) {
		return "{Call cmf4263a.InscriptionTournoi ("+ Id_Tournois +","+ Id_Jeux +","+Id_Equipe+")}";
	}
	
	public static String VoirInfosEcurie(int id) {
		return "Select NomEcurie, LogoEcurie, DiminutifEcurie from Utilisateurs WHERE	Id_Utilisateur 	=	"+ id;
	}
	
	public static String allEcurie() {
		return "Select Id_Utilisateur, NomEcurie, LogoEcurie, DiminutifEcurie from Utilisateurs where id_role=4";
	}
	
	public static String allJoueurByEquipe(int id) {
		return "select nomjoueur, prenomjoueur, photojoueur, datenaissancejoueur, datecontratjoueur, fincontratjoueur, u.id_equipe, id_nationalite from Utilisateur u, Equipe e where "+id+" = e.id_Utilisateur and e.id_equipe = u.id_equipe and id_role = 3";
	}
	
	public static String allEquipeByEcurie(int id) {
		return "select e.id_equipe, id_jeux, e.id_utilisateur from Equipe e where "+id+" = e.id_Utilisateur";
	}
	
	public static String ajouterEcurie(String  username, String password, String NomEcurie, String DiminutifEcurie, Blob LogoEcurie) {
		return "{? = CALL registerEcurie("+ username+","+ password +","+ NomEcurie +","+ LogoEcurie +","+ DiminutifEcurie +")}"	;

	}
	public static String AjouterJoueur(String username,  String password , String NomJoueur, String PrenomJoueur, Blob PhotoJoueur, Date DateNaissanceJoueur, Date DateContratJoueur, Date FinContratJoueur, int Id_Equipe, int Id_Nationalite) {
		return "{? = CALL registerJoueur("+ username + ","+ password+","+NomJoueur+ ","+PrenomJoueur+","+PhotoJoueur+","+ DateNaissanceJoueur + ","+ DateContratJoueur + "," + FinContratJoueur +","+Id_Equipe+","+Id_Nationalite+")}";
	}
	public static String VoirInfosEcurie4 (int IdEquipe) {

		return "Select  j.Nom as Nom_Joueur , Prenom, Photo, DateNaissance, Date_de_contrat, Fin_de_contrat, j.Nationalité as Nationalité_Joueur From Joueur j Where 	Joueur.Id_Equipe 	= 	IdEquipe;";

	}
	
	
	
	
	
	
	public String getRequete() {
		return requete;
	}
	
	public typeRequete getType() {
		return type;
	}
	
}
