package peer;

import protocal.BitField;
import protocal.Message;
import protocal.Tool;

import java.io.*;
import java.util.*;

public class ReceiverThread implements Runnable{
	
	private boolean debug = true;
	private static final int UNCHOKED = 0;
	private static final int CHOKED = 1;
	private static final int UNKNOWN = 2;
	
	int id;
	PeerConfig config;
	Peer peer;
	BitField bitField;
	volatile  boolean canTerminate = false;
	volatile  boolean canClose = false;
	InputStream in;
	//we need a sender to response the message
	SenderThread sender;
	//the received message
	Message revMessage;	
	//byte buffer for the file
	byte[] data;
	//multiple threads maintain one set of vectors
	Vector<Integer> consumerVec;
	Vector<Integer> providerVec;
	Vector<Integer> strangerVec;
	int curStatus;
	FileIO fileIO;
	
	public ReceiverThread(PeerConfig config, SenderThread sender) {
		this.id = config.id;
		this.config = config;
		this.peer = config.peer;
		bitField = config.bitField;
		this.consumerVec = config.consumerVec;
		this.providerVec = config.providerVec;
		this.strangerVec = config.strangerVec;
		this.curStatus = UNKNOWN;
		this.fileIO = config.fileIO;
		try {
			in = config.peer.socket.getInputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Fail to get InputStream from: "+peer.peerID);
			e.printStackTrace();
		}
		this.sender = sender;
		data = new byte[config.config.fileSize];
	}
	
	/**
	 * The receiver is basically reading messages 
	 * and take some reactions based on the received message 
	 * */
	public void run() {
		// TODO Auto-generated method stub	
		try {
			while(!this.canTerminate() && ! this.canTerminate) {
				revMessage = Tool.readMessage(in);
				this.takeReactions();
			}
			PeerProcess.NUM_THREADS--;	
			
			while(!canClose) 
				Thread.sleep(50);
			in.close();
			peer.socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Errors when closing the sockets: "+peer.peerID);
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sender.terminate();
		System.out.println("Terminate Receiver Thread: "+id+" To "+peer.peerID);
		if(fileIO.fout == null)
			return;
		//write the file to the disk before stop
		System.out.println("Writing the file to the disk");
		try {
			fileIO.writeToDisk();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Fail to write to the disk.");
			e.printStackTrace();
		}
		
	}
	
	public void start() {
		System.out.println("Starting sender thread:"+id+" To "+peer.peerID);
		new Thread(this).start();
	}
 	
	public void terminate() {
		this.canTerminate = true;
	}
	
	public void close() {
		this.canClose = true;
	}
	
	private boolean canTerminate() {
		return peer.bitField.isAllOne() && this.bitField.isAllOne(); 
	}

	private void takeReactions() {
		switch (revMessage.type){
		case Message.CHOKE: 
			beChoked();
			break;
		case Message.UNCHOKE:
			beUnchoked();
			break;
		case Message.REQUEST:
			sendPiece();
			break;
		case Message.PIECE:
			receivePiece();
			break;
		case Message.INSTERESTED:
			addConsumer();
			PeerProcess.NUM_RECEIVED_MESSAGES++;
			break;
		case Message.NOT_INSTERESTED:
			deleteConsumer();
			PeerProcess.NUM_RECEIVED_MESSAGES++;
			break;
		case Message.HAVE:
			updatePeerBitfield();
			break;
		case Message.BITFIELD:
			responseBitField();
			break;
		}
	} 
	
	private void beChoked() {
		//TODO
		if(debug)
			System.out.println(id+":Receive Choked");
		this.curStatus = CHOKED;
	}
	
	private void beUnchoked() {
		//TODO
		if(debug)
			System.out.println(id+":Receieve Unchoked");	
		//change current state
		this.curStatus = UNCHOKED;	
		if(!bitField.isAllOne())
			// send request
			this.sendRequest();
	}
	
	private void sendPiece() {
	
		sender.sendMessage(Message.PIECE, revMessage.pieceIndex);
		peer.bitField.setOne(revMessage.pieceIndex);
		if(debug) {
			System.out.println(id+":Send piece "+revMessage.pieceIndex+" to "+peer.peerID);
			//TODO
			System.out.print("Peer BitMap: ");
			for(int i = 0; i < peer.bitField.bitMap.length; i++)
				System.out.print(peer.bitField.bitMap[i]);
			System.out.println();
		}
		if(this.canTerminate())
			this.canTerminate = true;
	}
	
	private void receivePiece() {
		//TODO
		fileIO.writeToBuffer(revMessage.payload, config.config.pieceSize);
		if(this.curStatus == UNCHOKED && this.bitField.isAllOne() == false)
			this.sendRequest();
		
		if(debug) {
			System.out.println(id+":Received piece "+revMessage.pieceIndex+" from "+peer.peerID);
			System.out.print("BitMap: ");
			for(int i = 0; i < bitField.bitMap.length; i++)
				System.out.print(bitField.bitMap[i]);
			System.out.println();
		}
		
		if(bitField.isAllOne())
			System.out.println(id+" Download Complete.");
	}
	
	/**
	 * Randomly selected the piece index and the sender thread send request.
	 * */
	private void sendRequest() {
		// randomly select a piece
		int rand = 1, index = rand;
		Random r = new Random();
		rand = r.nextInt(bitField.numPiece) + 1;
		while(rand > 0) {
			index = (index + 1) % bitField.numPiece;
			if(bitField.getBit(index) == 0) 
				rand--;
		}
		sender.sendMessage(Message.REQUEST, index);
	}
	
	private void addConsumer() {
		if(debug)
			System.out.println("Add a consumer :"+this.peer.peerID);
		//TODO
		int pid = this.peer.peerID;
		if(this.consumerVec.contains(pid) == false)
			this.consumerVec.add(pid);
		if(this.providerVec.contains(pid))
			this.providerVec.remove(new Integer(pid));
		if(this.strangerVec.contains(pid))
			this.strangerVec.remove(new Integer(pid));
	}
	
	private void deleteConsumer() {
		if(debug)
			System.out.println("Delete a consumer :"+this.peer.peerID);
		int pid = this.peer.peerID;
		if(this.consumerVec.contains(pid))
			this.consumerVec.remove(new Integer(pid));
		if(this.providerVec.contains(pid) == false && this.strangerVec.contains(pid) == false)
			this.strangerVec.add(new Integer(pid));
	}
	
	/**
	 * If the peer have already down load the whole file, remove him from the consumer vector.
	 * */
	private void updatePeerBitfield() {
		if(debug)
			System.out.println("Update the bitField.");
		peer.bitField.setOne(revMessage.pieceIndex);
		if(peer.bitField.isAllOne()) 
			deleteConsumer();
	}
	
	private void responseBitField() {
		BitField peerBF = revMessage.bitField;
		for(int i = 0; i < peerBF.numPiece; i++) {
			if(bitField.getBit(i) == 0 && peerBF.getBit(i) == 1) {
				sender.sendMessage(Message.INSTERESTED);
				return;
			}
		}
		sender.sendMessage(Message.NOT_INSTERESTED);
	}
}

