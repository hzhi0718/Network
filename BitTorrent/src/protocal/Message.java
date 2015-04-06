package protocal;

import java.io.IOException;
import java.net.Socket;

/**
 * This class define the messages, including the handshake message and actual message.
 * @author zhi huang
 * */
public class Message {
	
	public static final int HANDSHAKE = -1;
	public static final int CHOKE = 0;
	public static final int UNCHOKE = 1;
	public static final int INSTERESTED = 2;
	public static final int NOT_INSTERESTED = 3;
	public static final int HAVE = 4;
	public static final int BITFIELD = 5;
	public static final int REQUEST = 6;
	public static final int PIECE = 7;
	
	public boolean isActualMessage = true;
	
	public int type;
	public int pid;
	public int length;
	public int pieceIndex;
	public byte[] payload;
	public BitField bitField;
	
	
	/**
	 * Send HandShakeMessage to particular socket
	 * */
	public static void sendHandshakeMessage(int id, Socket socket) throws IOException {
		byte[] handshakeMessage = Tool.generateHandshakeMessge(id);
		socket.getOutputStream().write(handshakeMessage);
		socket.getOutputStream().flush();
	}
	
	public static Message receiveHandShakeMessage(Socket socket) throws IOException{
		Message message = null;
		message = Tool.parseMessage(Tool.readBytes(socket.getInputStream()));
		return message;
	}
	
	public static void sendBitFieldMessage(Socket socket, BitField bf) throws IOException{
		//serialize the BitField object before sending
		byte[] bitFieldMessage = Tool.generateActualMessage(BITFIELD, BitField.serializeBF(bf));
		socket.getOutputStream().write(bitFieldMessage);
		socket.getOutputStream().flush();
	}
	
	public static Message receiveBitFieldMessage(Socket socket) throws IOException{
		Message message = null;
		message = Tool.parseMessage(Tool.readBytes(socket.getInputStream()));
		return message;
	}
	
	public static void sendActualMessage(Socket socket, int type) throws IOException{
		byte[] actualMessage = Tool.generateActualMessage(type);
		socket.getOutputStream().write(actualMessage);
		socket.getOutputStream().flush();
	}
	
	public static void sendActualMessage(Socket socket, int type, byte[] payload) throws IOException{
		byte[] actualMessage = Tool.generateActualMessage(type, payload);
		socket.getOutputStream().write(actualMessage);
		socket.getOutputStream().flush();
	}
}
