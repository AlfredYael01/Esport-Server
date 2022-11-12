package database;

import java.sql.ResultSet;

public class Result {

	private ResultSet r;
	private int Entier;
	private boolean error;
	
	public Result(ResultSet r, int entier, boolean error) {
		this.r = r;
		Entier = entier;
		this.error = error;
	}
	
	public void setError(boolean error) {
		this.error = error;
	}
	
	public boolean isError() {
		return error;
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
