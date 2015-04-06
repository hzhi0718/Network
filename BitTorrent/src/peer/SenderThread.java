package peer;

import protocal.*;

import java.util.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.io.*;

/**
 * We create socket thread for each peer.
 * These thread is responsible to receive and response the messages.
 * @author zhi huang 
 * */
public class SenderThread implements Runnable{

	static boolean debug = true;
	
	int id;
	PeerConfig config;
	Peer peer;
	BitField bitField;
	volatile boolean canTerminate = false;
	OutputStream out;
	int type;
	byte[] payload;
	
	public SenderThread(PeerConfig config) {
		this.id = config.id;
		this.config = config;
		this.peer = config.peer;
		bitField = config.bitField;
		type = -1;
		try {
			out = peer.socket.getOutputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Fail to get OutputStream: "+peer.peerID);
			e.printStackTrace();
		}
	}
	
	public void run() {
		// TODO Auto-generated method stub	
		try {
			while(!canTerminate) {
				//read bytes and parse them
				while(type == -1 && !canTerminate) Thread.sleep(10);
				if(type == Message.INSTERESTED 
						|| type == Message.NOT_INSTERESTED
						|| type == Message.CHOKE
						|| type == Message.UNCHOKE) {
					//send the message without payload
					Message.sendActualMessage(peer.socket, type);	
				}
				
				else if(type == Message.REQUEST
					 	|| type == Message.PIECE) {
					Message.sendActualMessage(peer.socket, type, payload);
				}
				
				else if(type == Message.BITFIELD) 
					Message.sendBitFieldMessage(peer.socket, bitField);
				
				type = -1;
				
			}
			out.close();
			peer.socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Terminate Sender Thread: "+id+" To "+peer.peerID);
	}
	
	public void start() {
		System.out.println("Starting sender thread:"+id+" To "+peer.peerID);
		new Thread(this).start();
	}
	
	public void terminate() {
		canTerminate = true;;
	} 
	
	public void sendMessage(int type){
		this.type = type;
	}
	
	public void sendMessage(int type, int index) {
		this.type = type;
		if(type == Message.REQUEST)
			payload = Tool.toByteArray(index);
		//The payload includes the index and data piece
		else if(type == Message.PIECE) {
			ByteBuffer buf = ByteBuffer.allocate(config.config.pieceSize + 4);
			buf.put(Tool.toByteArray(index));
			buf.put(config.fileIO.readFromBuffer(index, config.config.pieceSize));
			payload = buf.array();
		} else
			System.out.println("Type Error");
	}
	
}
