package model.database;

import java.sql.ResultSet;

public class Result {

	private ResultSet r;
	private int integer;
	private boolean error;
	
	public Result(ResultSet r, int entier, boolean error) {
		this.r = r;
		integer = entier;
		this.error = error;
	}
	
	public void setError(boolean error) {
		this.error = error;
	}
	
	public boolean isError() {
		return error;
	}
	
	public int getInteger() {
		return integer;
	}
	
	public ResultSet getResultSet() {
		return r;
	}
	
	public void setInteger(int entier) {
		integer = entier;
	}
	
	public void setResultSet(ResultSet r) {
		this.r = r;
	}
	
	
	
}
