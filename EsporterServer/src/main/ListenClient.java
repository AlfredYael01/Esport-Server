package main;

public class ListenClient implements Runnable{
	
	private ConnectionClient client;
	
	public ListenClient(ConnectionClient client) {
		this.client = client;
	}
	
	@Override
	public void run() {
		
	}

}
