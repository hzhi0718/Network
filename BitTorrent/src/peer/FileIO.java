package peer;

import java.io.FileNotFoundException;
import java.io.IOException;

import protocal.*;

import java.io.*;


/**
 * This class provides synchronized writing methods.
 * */
public class FileIO {
	FileOutputStream fout;
	byte[] data;
	BitField bitField;
	
	public FileIO(byte[] data, FileOutputStream fout, BitField bitField) {
	
		this.data = data;
		this.fout = fout;
		this.bitField = bitField;
	}
	
	public void loadToBuffer(String filePath) {
		try {
			FileInputStream fin = new FileInputStream(filePath);
			fin.read(data);
			fin.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Fail to load the data into the buffer.");
//			e.printStackTrace();
		}
		
	}
	
	/**
	 * Read specific piece of data from the data buffer.
	 * */
	public byte[] readFromBuffer(int pieceIndex, int pieceSize) {
		byte[] dataPiece = new byte[pieceSize];
		for(int i = 0; i < pieceSize; i++) {
			dataPiece[i] = data[pieceIndex * pieceSize + i];
		}
		return dataPiece;
	}
	/**
	 * We assume that we have large enough memory to store all the data.
	 * */
	public synchronized void writeToBuffer(byte[] payload, int pieceSize) {
		//first four bytes is the index
		int pieceOffset = Tool.toInteger(Tool.getSubArray(payload, 0, 4));
		//the rest bytes are real data
		writeToBuffer(Tool.getSubArray(payload, 4, payload.length), pieceOffset, pieceSize);
	}
	
	public synchronized void writeToBuffer(byte[] bytes, int pieceOffset, int pieceSize) {
		//convert the piece offset to byte offset
		int offset = pieceOffset * pieceSize;
		for(int i = 0; i < bytes.length; i++) {
			data[i + offset] = bytes[i];
		}
		//update the bitField
		this.bitField.setOne(pieceOffset);
	}
	
	public synchronized void writeToDisk() throws IOException {
		fout.write(data);
		fout.close();
	}
}
