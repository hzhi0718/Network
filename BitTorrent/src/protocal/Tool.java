package protocal;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * This class provides some functions to process the data.
 * @author zhi huang 
 * */
public class Tool {
	
	public static String HandShakeHead = "P2PFILESHARINGPROJ";
	
	public static byte[] generateHandshakeMessge(int pid){
		byte[] message = new byte[32];
		byte[] head;
		byte[] id;
		try {
			head = HandShakeHead.getBytes("US-ASCII");
			int i = 0;
			for(i = 0; i < 18; i++)
				message[i] = head[i];
			for(; i < 28; i++)
				message[i] = (byte)'0';
			id = toByteArray(pid);
			for(;i < 32; i++)
				message[i] = id[i - 28];
			
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			System.out.println("There is an error in creating the HandShake message.");
			e.printStackTrace();
		}
		return message;
	}
	
	/**
	 * generate the message without payload
	 * */
	public static byte[] generateActualMessage(int type) {
		//the length of payload is 0, the length of length field is excluded
		int length = 1;
		byte[] bytes = new byte[length + 4];
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		byte[] lenBytes = toByteArray(length);
		byte[] typeBytes = new byte[1];
		typeBytes[0] = (byte)type;
		byteBuffer.put(lenBytes);
		byteBuffer.put(typeBytes);
		return byteBuffer.array();
	}
	
	/**
	 * generate the message with payload
	 * */
	public static byte[] generateActualMessage(int type, byte[] payload) {
		int length = 1 + payload.length;
		byte[] bytes = new byte[length + 4];
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		byte[] lenBytes = toByteArray(length);
		byte[] typeBytes = new byte[1];
		typeBytes[0] = (byte)type;
		byteBuffer.put(lenBytes);
		byteBuffer.put(typeBytes);
		byteBuffer.put(payload);
		return byteBuffer.array();
	}
	
	/**
	 * convert the integer to 4-byte array
	 * Note:The high-end is on the right-hand side. 
	 * */
	public static byte[] toByteArray(int num) {
		byte[] bytes = new byte[4];
		for(int i = 0; i < 4; i++) 
			bytes[i] = (byte)(num >>> (i * 8));
		return bytes;
	}
	
	/**
	 * convert the 4-byte array to integer
	 * */
	public static int toInteger(byte[] bytes){
		return bytes[3] << 24 
				| (bytes[2] & 0xFF) << 16 
				| (bytes[1] & 0xFF) << 8 
				| (bytes[0] & 0xFF);
	}
	
	public static Message parseMessage(byte[] bytes) {
		Message message = new Message();
		String head = new String(getSubArray(bytes, 0, 18));
		message.isActualMessage = (head.equals(HandShakeHead) == false);
		//if the message is handshake message, we have to parse the peer id
		if(message.isActualMessage == false) {
			byte[]idBytes = new byte[4];
			for(int i = 32 - 4; i < 32; i++)
				idBytes[i - 28] = bytes[i];
			message.pid = toInteger(idBytes);
		}
		//else we have to parse the actual message
		else {
			message.length = toInteger(getSubArray(bytes, 0, 4));
			message.type = (byte)getSubArray(bytes,4,5)[0];
			//different types of message with different types of pay load
			if(message.type == Message.BITFIELD) {
				message.bitField = BitField.deserializeBF(getSubArray(bytes, 5, bytes.length));
			} else if(message.type == Message.PIECE) {
				message.pieceIndex = toInteger(getSubArray(bytes, 5, 9));
				message.payload = getSubArray(bytes, 5, bytes.length);
			} else if(message.type == Message.HAVE
					|| message.type == Message.REQUEST) {
				message.pieceIndex = toInteger(getSubArray(bytes, 5, 9));
			}
		}
		return message;
	}
	
	/**
	 * get a consecutive sub-array from large byte array;
	 * the first index is included, the last index is excluded.
	 * */
	public static byte[] getSubArray(byte[] a, int low, int high) {
		int length = high - low;
		byte[] sub = new byte[length];
		for(int i = 0; i < length && i < a.length; i++) 
			sub[i] = a[i + low];
		return sub;
	}
	
	/**
	 * High level interface to read data from socket inputstream and parse byte[] to message
	 * */
	public static Message readMessage(InputStream in) throws IOException {
		return parseMessage(readBytes(in));
	}
	
	/**
	 * Handle the reading process of handshake message and actual message 
	 * */
	public static byte[] readBytes (InputStream in)throws IOException{
		byte[] firstFive = new byte[5];
		ByteBuffer data = ByteBuffer.wrap(firstFive);
		//determine whether it is a handshake message or actual message
		//by reading first 5-bytes, if the fifth byte is 'i' then it is
		//a handshake message
		firstFive = readTCPinputstream(in, 5);
		//the fifth byte is 'I'
		if((int)firstFive[4] == (int)"I".getBytes()[0]){
			//it is a handshake message
			byte[] rest = new byte[32 - 5];
			rest = readTCPinputstream(in, 27);
			data = ByteBuffer.wrap(new byte[32]);
			data.put(firstFive);
			data.put(rest);
		}
		// it is a actual message
		else {
			int length = toInteger(getSubArray(firstFive,0,4));
			int type = (int)firstFive[4];
			data = ByteBuffer.wrap(new byte[length + 4]);
			data.put(firstFive);
			if(length > 1) {
				byte[] rest = new byte[length - 1];
				//if the message contains payloads
				rest = readTCPinputstream(in, length - 1);
				data.put(rest);
			}
		}
		return data.array();
	}
	
	/**
	 * To read the data from a TCP stream,
	 * we may have to read the data for multiple times.
	 * @throws IOException 
	 * */
	public static byte[] readTCPinputstream(InputStream in, int length) throws IOException{
		ByteBuffer byteBuf = ByteBuffer.wrap(new byte[length]);
		byte[] input = new byte[length];
		int curLength = 0, inputLength;
			
		while(curLength < length) {
			//each time we may only get part of the data
			inputLength = in.read(input);
			/*
			 * This loop is crucial.
			 * Because when inputStream fail to read data from the socket,
			 * the function will return -1 which could cause overflow.
			 * **/
			if(inputLength <= 0)
				continue;
			curLength += inputLength;
			//append the data to the output buffer
			byteBuf.put(input, 0, inputLength);
			
		}
		return byteBuf.array();
	}
	
	//extract the ip address using regex
	public static String getIPaddress(Socket socket) {
		String ipAddr = socket.getRemoteSocketAddress().toString();
		Pattern p = Pattern.compile("/(.+):");
		Matcher m = p.matcher(ipAddr);
		m.find();
		return m.group(1);	
	}
	
	public static String getIPaddress(String ipAddr) {

		Pattern p = Pattern.compile("/(.+):");
		Matcher m = p.matcher(ipAddr);
		m.find();
		return m.group(1);	
	}
	
	/**
	 * for testing
	 * */
	static public void main(String args[]) {
		int pid = 1001;
		Message m = parseMessage(generateHandshakeMessge(pid));
		System.out.println(m.type);
		System.out.println(m.pid);
		System.out.println(getIPaddress("/127.0.0.1:62659"));
		
	}
}
