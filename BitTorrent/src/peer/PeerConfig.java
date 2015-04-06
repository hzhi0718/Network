package peer;
import protocal.*;
import java.util.*;

public class PeerConfig {

	CommonConfig config;
	int id;
	Peer peer;
	ArrayList<Peer> peerList;
	int[] peerTypeMap;
	BitField bitField;
	Vector<Integer> consumerVec;
	Vector<Integer> providerVec;
	Vector<Integer> strangerVec;
	byte[] data;
	FileIO fileIO;
	
}
