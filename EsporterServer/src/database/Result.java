package database;

import java.sql.ResultSet;

public class Result {

	private ResultSet r;
	private int Entier;
	
	public Result(ResultSet r, int entier) {
		this.r = r;
		Entier = entier;
	}
	
	public int getEntier() {
		return Entier;
	}
	
	public ResultSet getResultSet() {
		return r;
	}
	
	public void setEntier(int entier) {
		Entier = entier;
	}
	
	public void setResultSet(ResultSet r) {
		this.r = r;
	}
	
	
	
}
