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
					logout();
					break;
				case AJOUTER_EQUIPE:
					if(client.getRole()!=Permission.ECURIE) {
						errorPermission();
						break;
					}
					
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
					errorPermission();
			}
		}
	}
	
	private void ajouterEquipe() {
		
	}
	
	
	private void logout() {
		client.setIsLogin(false);
		client.setPermission(Permission.VISITEUR);
	}
	
	private void errorPermission() {
		ResponseObject r = new ResponseObject(Response.ERROR_PERMISSION, null, null);
		client.send(r);
	}
	
	private void ErrorLogin() {
		ResponseObject r = new ResponseObject(Response.ERROR_LOGIN, null, null);
		client.send(r);
	}
	
	private void login(Command c) {
		Login l = (Login) c.getInfoByID(InfoID.login);
		int result = client.login(l.getUsername(), l.getPassword());
		if (result == -1) {
			ErrorLogin();
		} else {
			try {
				Result res = DatabaseAccess.getInstance().getData(new Requete(Requete.getUserByID(result), typeRequete.REQUETE));
				if (res.isError()) {
					ErrorLogin();
				}
				ResultSet rs = res.getResultSet();
				rs.next();
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
					m.put(InfoID.Joueur, new JoueurInfo(result, rs.getString("nomjoueur"), rs.getString("prenomjoueur"), rs.getBlob("photojoueur"), rs.getDate("datenaissancejoueur"), rs.getDate("datecontratjoueur"), rs.getDate("fincontratJoueur"), rs.getInt("id_nationalite"), rs.getInt("id_equipe"), -1));
					break;
				case 4:
					m.put(InfoID.Permission, Permission.ECURIE);
					m.put(InfoID.Ecurie, new EcurieInfo(rs.getString("nomecurie"), rs.getBlob("logoecurie"), rs.getString("diminutifecurie"), result));
					break;
				
				}
				ResponseObject r = new ResponseObject(Response.LOGIN, m, null);
				client.send(r);
				client.setIsLogin(true);
			} catch (Exception e) {
				e.printStackTrace();
				ErrorLogin();
			}

			
			
			
		}
	}

}
