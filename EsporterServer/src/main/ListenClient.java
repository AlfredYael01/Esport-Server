package main;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Timer;

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
		if (result == -1) {
			ResponseObject r = new ResponseObject(Response.ERROR_LOGIN, null, null);
			send(r);
		} else {
			client.setPermission(result);
			HashMap<InfoID,Infos> m = new HashMap<>();
			m.put(InfoID.Permission, Permission.ECURIE);
			m.put(InfoID.Ecurie, new EcurieInfo("Vitality", null, "VIT", "Francaise", 0));
			
			ResponseObject r = new ResponseObject(Response.LOGIN, m, null);
			System.out.println(r);
			send(r);
			client.setIsLogin(true);
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
