package protocal;
import java.util.*;
import java.io.*;
import java.nio.ByteBuffer;

/**
 * The structure of Bit Field. We use bit map to represent the data piece.
 * 
 * Need test.
 * */

public class BitField implements Serializable {

	static boolean debug = true;
	
	public byte[] bitMap;
	// how many bytes we need
	public int byteLength;
	// number of pieces
	public int numPiece;

	public BitField() {}
	
	public BitField(int fileSize, int pieceSize) {
		numPiece = (int) Math.ceil((double) fileSize / pieceSize);
		byteLength = (int) Math.ceil(numPiece / 8.0);
		bitMap = new byte[byteLength];
	}

	public BitField(byte[] bytes) {
		bitMap = bytes;
	}

	public void setAllOne() {
		for (int i = 0; i < bitMap.length; i++)
			bitMap[i] |= 255;
	}

	public void setOne(int pieceIndex) {
		int byteIndex = pieceIndex / 8;
		int leftShift = pieceIndex - byteIndex * 8;
		bitMap[byteIndex] |= 1 << leftShift;
	}

	public int getBit(int pieceIndex) {
		int byteIndex = pieceIndex / 8;
		int leftShift = pieceIndex - byteIndex * 8;
		return (bitMap[byteIndex] & (1 << leftShift)) > 0 ? 1 : 0;
	}

	public boolean isAllOne() {
		for (int i = 0; i < numPiece; i++) {
			if ((bitMap[i / 8] & (1 << (i % 8))) == 0)
				return false;
		}
		return true;
	}

	/**
	 * To exchange the BitField, we need serialize and deserialize functions
	 * */
	public static byte[] serializeBF(BitField bf) {
		
//		System.out.println("SerialzaBF:");
//		System.out.println(bf.byteLength);
//		System.out.println(bf.bitMap.length);
		
		int bfLength = 4 + 4 + bf.bitMap.length;
		byte[] bfArray = new byte[bfLength];
		ByteBuffer byteBuf = ByteBuffer.wrap(bfArray);
		byteBuf.put(Tool.toByteArray(bf.byteLength));
		byteBuf.put(Tool.toByteArray(bf.numPiece));
		byteBuf.put(bf.bitMap);
		
		return byteBuf.array();
	}

	public static BitField deserializeBF(byte bytes[]) {
//		System.out.println("Deserialze BF:");
//		for(int i = 0 ; i< bytes.length; i++)
//			System.out.print(bytes[i]+" ");
		System.out.println();
		BitField bf = new BitField();
		bf.byteLength = Tool.toInteger(Tool.getSubArray(bytes, 0, 4));
		bf.numPiece = Tool.toInteger(Tool.getSubArray(bytes, 4, 8));
		bf.bitMap = Tool.getSubArray(bytes, 8, bytes.length);
//		System.out.println(bf.byteLength);
//		System.out.println(bf.bitMap.length);
		return bf;
	}
	
	//for testing
	private void printInfo() {
		System.out.print("Number of Pieces "+this.numPiece+", ");
		System.out.println("Number of Bytes "+this.byteLength);
		System.out.print("Bit Map: ");
		for(int i = 0; i < this.byteLength; i++)
			System.out.print(String.format("%8s", Integer.toBinaryString(bitMap[i]&0xFF)).replace(" ", "0"));
		System.out.println();//System.out.println();
	}
	
	public static void main(String args[]) {
		BitField bf = new BitField(9,1);
//		bf.setAllOne();
//		bf.printInfo();
		bf = bf.deserializeBF(bf.serializeBF(bf));
		for(int i = 0; i < bf.numPiece; i++) {
			System.out.println("index of piece:"+i);
			bf.setOne(i);
			bf.printInfo();
			System.out.print("Get bits: ");
			for(int j = 0; j < bf.numPiece; j++)
				System.out.print(bf.getBit(j));
			System.out.println();
			System.out.println();
		}
	}
}
