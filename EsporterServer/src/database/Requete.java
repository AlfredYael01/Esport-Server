package database;

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
	
	public String getRequete() {
		return requete;
	}
	
	public typeRequete getType() {
		return type;
	}
	
}
