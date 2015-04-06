package test;
import java.io.IOException;
import java.net.*;
import java.net.UnknownHostException;

import peer.*;

public class Host1 {
	public static void main(String args[]) throws IOException {
	
//		Socket theSocket = new Socket("127.0.0.1", 8080);
//	    System.out.println("Connected to " + theSocket.getInetAddress() + " on port "
//	        + theSocket.getPort() + " from port " + theSocket.getLocalPort() + " of "
//	        + theSocket.getLocalAddress());
		new PeerProcess(1001);
		System.out.println("Host1 is running");
//		PeerProcess p2 = new PeerProcess(1002);
//		System.out.println("Peer 2 is running");
		
	}
}
