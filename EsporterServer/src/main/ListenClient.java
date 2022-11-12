package main;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Timer;

import database.DatabaseAccess;
import database.Requete;
import database.Requete.typeRequete;
import database.Result;
import socket.Command;
import socket.CommandName;
import socket.Response;
import socket.ResponseObject;
import types.EcurieInfo;
import types.InfoID;
import types.Infos;
import types.JoueurInfo;
import types.Login;
import types.Permission;

public class ListenClient implements Runnable{
	
	private ConnectionClient client;
	
	public ListenClient(ConnectionClient client) {
		this.client = client;
	}
	
	@Override
	public void run() {
		ObjectInputStream in = client.getIn();
		while (true) {
			try {
				Object o = in.readObject();
				System.out.println(o.toString());
				commande(o);
			} catch (ClassNotFoundException e) {
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
					break;
				case AJOUTER_EQUIPE:
					break;
				case AJOUTER_TOURNOI:
					break;
				case INSCRIPTION_TOURNOI:
					break;
				case VOIR_CALENDRIER:
					
					break;
				case VOIR_ECURIE:
					break;
				default:
			}
			
		} else {
			switch(c.getName()) {
				case LOGIN : 
					login(c);
					break;
				case VOIR_CALENDRIER:
					break;
				case VOIR_ECURIE:
					break;
				default:
					
			}
		}
	}
	
	
	private void login(Command c) {
		Login l = (Login) c.getInfoByID(InfoID.login);
		int result = client.login(l.getUsername(), l.getPassword());
		System.out.println(result);
		if (result == -1) {
			ResponseObject r = new ResponseObject(Response.ERROR_LOGIN, null, null);
			send(r);
		} else {
			try {
				Result res = DatabaseAccess.getInstance().getData(new Requete(Requete.getUserByID(result), typeRequete.REQUETE));
				if (res.isError()) {
					System.out.println("Erreur :"+res.isError());
					ResponseObject r = new ResponseObject(Response.ERROR_LOGIN, null, null);
					send(r);
				}
				ResultSet rs = res.getResultSet();
				int perm = rs.getInt("id_role");
				client.setPermission(perm);
				HashMap<InfoID,Infos> m = new HashMap<>();
				switch (perm) {
				case 1:
					m.put(InfoID.Permission, Permission.ORGANISATEUR);
					break;
				case 2:
					m.put(InfoID.Permission, Permission.ARBITRE);
					break;
				case 3:
					m.put(InfoID.Permission, Permission.JOUEUR);
					
					break;
				case 4:
					m.put(InfoID.Permission, Permission.ECURIE);
					m.put(InfoID.Ecurie, new EcurieInfo(rs.getString("nomecurie"), rs.getBlob("logoecurie"), rs.getString("diminutifecurie"), result));
					break;
				
				}
				ResponseObject r = new ResponseObject(Response.LOGIN, m, null);
				System.out.println(r);
				send(r);
				client.setIsLogin(true);
			} catch (Exception e) {
				e.printStackTrace();
				ResponseObject r = new ResponseObject(Response.ERROR_LOGIN, null, null);
				send(r);
			}

			
			
			
		}
	}
	
	public void send(ResponseObject o) {
		ObjectOutputStream out = client.getOut();
		try {
			out.writeObject(o);
		} catch (IOException e) {
			
		}
	}

}
