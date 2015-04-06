package peer;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;

import protocal.*;

public class PeerProcess {

	private static boolean debug = true;
	
	private final String ComConfig = "Common.cfg";
	private final String PeerInfo = "PeerInfo.cfg";
	
	int id;
	int numPeers;
	// all the other peers
	ArrayList<Peer> peerList;
	// the common configuration
	CommonConfig config;
	// BitField for current process
	BitField bitField;
	// indicate the type of the peer: stranger, provider, consumer
	int[] peerTypeMap;

	//thread list to track all the threads
	//each host should have two threads, a sender and a receiver
	ArrayList<SenderThread> senderList;
	ArrayList<ReceiverThread> receiverList;
	//Map the peer id to the arrayList index
	private HashMap<Integer, Integer> idMap;
	Vector<Integer> consumerVec;
	Vector<Integer> providerVec;
	Vector<Integer> strangerVec;
	byte[] data;
	FileIO fileIO;
	FileOutputStream fout;
	String dataFilePath;
	
	//shared variable for threads synchronization
	static int NUM_RECEIVED_MESSAGES = 0;
	static int NUM_THREADS = 0;
	static boolean DONE = false;

	public PeerProcess(int id) throws IOException {
		this.id = id;
		peerList = new ArrayList<Peer>();
		config = new CommonConfig();
		numPeers = 0;
		senderList = new ArrayList<SenderThread>();
		receiverList = new ArrayList<ReceiverThread>();
		idMap = new HashMap<Integer,Integer>();
		consumerVec = new Vector<Integer>();
		providerVec = new Vector<Integer>();
		strangerVec = new Vector<Integer>();
		dataFilePath = "Peer"+id+"/data.dat";
		mappingID();
		init(id);
		startP2Pprocess();
	}

	/**
	 * Read the configuration files and initialize the sockets
	 * 
	 * @param int id, the id of current peer process
	 * @author zhi huang
	 * */
	private void init(int id) {
		// read the common configuration
		Scanner scanner = null;
		try {
			scanner = new Scanner(new FileInputStream(ComConfig));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		scanner.next();// escape the name
		config.numPerferredNeighbor = scanner.nextInt();// read the data
		scanner.next();
		config.unchokingInter = scanner.nextInt();
		scanner.next();
		config.optUnchokingInter = scanner.nextInt();
		scanner.next();
		config.dataFileName = scanner.next();
		scanner.next();
		config.fileSize = scanner.nextInt();
		scanner.next();
		config.pieceSize = scanner.nextInt();
		data = new byte[config.fileSize];
		// testing
//		config.printConfig();
		scanner.close();
		
		try {
			// read the information of the peers in the network
			// connect the servers already set up
			// act as a client first
			scanner = new Scanner(new FileInputStream(PeerInfo));
			while (scanner.hasNext()) {
				int pid = scanner.nextInt();
				if (pid == id)
					break;
				String addr = scanner.next();
				int port = scanner.nextInt();
				boolean hasFile = scanner.nextInt() == 1 ? true : false;
				Peer p = new Peer(pid, addr, port, hasFile);
				peerList.add(p);
			}

			// then act as a server, waiting for other clients to connect
			scanner.next();// escape the field addr
			int port = scanner.nextInt();
			ServerSocket welcomeSocket = new ServerSocket(port);
			// escape the field hasFile
			boolean hasFile = scanner.nextInt() == 1 ? true : false;
			bitField = new BitField(config.fileSize, config.pieceSize);
			if (hasFile) {
				fileIO = new FileIO(data, null, bitField);
				bitField.setAllOne();
				fileIO.loadToBuffer(dataFilePath);
			}
			else {
				fout = new FileOutputStream(dataFilePath);
				fileIO = new FileIO(data, fout, bitField);
			}
		
			//exchange message with previously set up server
			for(Peer p:peerList) {
				p.connect();
				Message handshakeResponse;
				Message bitFieldResponse;
				//exchange handshake message
				do {
					//after tcp connection set up, we can exchange handshake message
					Message.sendHandshakeMessage(id, p.socket);
					//receive the handshake response and parse it
					handshakeResponse = Message.receiveHandShakeMessage(p.socket);
				}//check the received message
				while(handshakeResponse != null && handshakeResponse.pid != p.peerID);
				
				//exchange the BitField message
				Message.sendBitFieldMessage(p.socket, bitField);
				bitFieldResponse = Message.receiveBitFieldMessage(p.socket);
				p.bitField = bitFieldResponse.bitField;
			}
			
			if(debug)
				System.out.println("Already connect the servers.");
			int numClient = 0;
			// we need to know how many clients left to connect
			while (scanner.hasNext()) {
				int pid = scanner.nextInt();
				peerList.add(new Peer(pid, scanner.next(),
						scanner.nextInt(), scanner.nextInt() == 1 ? true : false));
				numClient++;
			}
			
			if(debug) 
				System.out.println("Remaining clients: "+numClient);
			// stop server process when all the peers are connected
			while (numClient-- > 0) {
				Socket client = welcomeSocket.accept();
				//exchange messages
				Message handshakeResponse;
				Message bitFieldResponse;
				handshakeResponse = Message.receiveHandShakeMessage(client);
				Message.sendHandshakeMessage(id, client);
				bitFieldResponse = Message.receiveBitFieldMessage(client);
				Message.sendBitFieldMessage(client, bitField);
				
				String ipAddr = Tool.getIPaddress(client);
				int index = idMap.get(handshakeResponse.pid);
				peerList.get(index).socket = client;
				peerList.get(index).bitField = bitFieldResponse.bitField;
			}
			
			welcomeSocket.close();	
			scanner.close();
			peerTypeMap = new int[peerList.size()];
			PeerProcess.NUM_THREADS = peerList.size();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Fail to open the peer info configuration file.");
			e.printStackTrace();
		}
		System.out.println(id+": Initialzation complete.");
	}

	/**
	 * mapping the peer id to the list index ID
	 * */
	private void mappingID() {
		Scanner scanner = null;
		try {
			scanner = new Scanner(new FileInputStream(PeerInfo));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		int i = 0;
		while(scanner.hasNext()) {
			int id = scanner.nextInt();
			if(id != this.id)
				idMap.put(id, i++);
			scanner.next();	scanner.nextInt();scanner.nextInt();
		}
		scanner.close();
	}
	
	/**
	 * Determine whether the process can terminate. If all the peers and itself
	 * download the data completely, then the process can terminate.
	 * */
	private boolean canTerminate() {
		// no other peers are interested in the data && itself has all the data.
		return PeerProcess.NUM_THREADS == 0;
	}

	/**
	 * Start threads for each peer.
	 * */
	private void startP2Pprocess() {

		// create configuration for each thread
		for (Peer p : peerList) {
			PeerConfig peerConfig = new PeerConfig();
			peerConfig.peer = p;
			peerConfig.bitField = this.bitField;
			peerConfig.peerTypeMap = this.peerTypeMap;
			peerConfig.config = this.config;
			peerConfig.id = this.id;
			peerConfig.consumerVec = this.consumerVec;
			peerConfig.providerVec = this.providerVec;
			peerConfig.strangerVec = this.strangerVec;
			peerConfig.data = this.data;
			peerConfig.fileIO = this.fileIO;
					
			SenderThread sender = new SenderThread(peerConfig);
			ReceiverThread recevier = new ReceiverThread(peerConfig, sender);
			senderList.add(sender);
			receiverList.add(recevier);
		}

		//start all the threads
		this.startThreads();
		this.updateVectors();
		while (!canTerminate()) {
			/**
			 * We may need a timer thread here.
			 * */
			try {
				// select some consumers and unchoke them.
				Vector<Integer> selectedConsumerList = this.reselectPeer();
				if(selectedConsumerList == null)
					break;
				if(debug && selectedConsumerList.size() > 0) 
					System.out.println("Selected List: "+selectedConsumerList);
				
				for(Integer pid : selectedConsumerList) 
					senderList.get(idMap.get(pid)).sendMessage(Message.UNCHOKE);

				Thread.sleep(config.unchokingInter);
//				break;
//				for(Integer pid : selectedConsumerList) 
//					senderList.get(idMap.get(pid)).sendMessage(Message.CHOKE);
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		this.closeThreads();
		System.out.println("Peer:"+id+" is going offline");
	}
	
	/**
	 * reselect the unchoke target based on the downloading speed.
	 * */
	private Vector<Integer> reselectPeer() {
		this.updateVectors();
		if(canTerminate())
			return null;
		Vector<Integer> selectedConsumerList = new Vector<Integer>();
		selectedConsumerList = this.consumerVec;
		return selectedConsumerList;
	}
	
	/**
	 * randomly select an unchoke target.(Optimistically unchoke)
	 * */
	private int randomSelect() {
		int randIndex = 1;
		Random r = new Random();
		randIndex = r.nextInt(consumerVec.size());
		return randIndex;
	}
	/**
	 * Use a thread pool to manage all the threads.
	 * */
	private void startThreads() {
		 ExecutorService cachedThreadPool = Executors.newCachedThreadPool();  
		 for(SenderThread st: senderList)
			 cachedThreadPool.execute(st);
		 for(ReceiverThread st: receiverList)
			 cachedThreadPool.execute(st);
		 cachedThreadPool.shutdown();
	}
	
	private void closeThreads() {
		for(ReceiverThread r:receiverList) {
			r.close();
		}
	}
	
	/**
	 * update the consumer and producer vectors by sending bitfield to other peers
	 * */
	private void updateVectors() {
		PeerProcess.NUM_RECEIVED_MESSAGES = 0;
		this.consumerVec.clear();
		
		for(SenderThread s: senderList)
			s.sendMessage(Message.BITFIELD);
		
		while(PeerProcess.NUM_RECEIVED_MESSAGES < PeerProcess.NUM_THREADS) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
	}
	
}
