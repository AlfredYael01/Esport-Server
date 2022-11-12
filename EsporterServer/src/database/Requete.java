package database;

public class Requete {

	public enum typeRequete{FONCTION, REQUETE}
	
	
	private String requete;
	private typeRequete type;
	private Requete(String requete, typeRequete type) {
		this.requete = requete;
		this.type = type;
	}
	
	public static String Login(String user, String pass) {
		return null;
	}
	
	public String getRequete() {
		return requete;
	}
	
	public typeRequete getType() {
		return type;
	}
	
}
