package types;

public enum Renomme {
LOCAL, NATIONAL, INTERNATIONAL;
	
	
	public static Renomme intToRenommee(int i) {
		switch(i) {
		case 1:return LOCAL;
		case 2:return NATIONAL;
		case 3:return INTERNATIONAL;
		}
		return null;

	}
	
	public static int FameToInt(Renomme r) throws IllegalArgumentException{
		return r.ordinal()+1;
	}

}

