package test;

import java.io.IOException;
import java.net.ServerSocket;

import peer.PeerProcess;

public class Host2 {
	public static void main(String args[]) throws IOException {
//		ServerSocket server = new ServerSocket(8080);
//		server.accept();
		new PeerProcess(1002);
		System.out.println("Host2 is running");
//		PeerProcess p2 = new PeerProcess(1002);
//		System.out.println("Peer 2 is running");
		
	}
}
