package peer;

import java.io.IOException;
import protocal.*;
import java.net.*;

import protocal.Tool;

public class Peer {

	//indicate the Peer status
	static final int UNCHOKED = 0;
	static final int CHOKED = 1;
	static final int UNKNOWN = 2;
	
	public int peerID;
	public int port;
	public String addr;
	public Socket socket;
	public boolean hasFile;
	public BitField bitField;
	public int status;
	
	Peer(int id, String addr, int port, boolean hasFile){
		this.peerID = id;
		this.addr = addr;
		this.port = port;
		this.hasFile = hasFile;
		this.status = UNKNOWN;
	}
	
	public void connect(){
		try {
			socket = new Socket(addr, port);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Fail to create the socket for peerID: "+ this.peerID);
			e.printStackTrace();
		}
	}
	
	
}
