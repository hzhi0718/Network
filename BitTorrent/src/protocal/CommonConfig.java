package protocal;

/**
 * The common configuration.
 * @author zhi huang
 * */

public class CommonConfig {
	public  int numPerferredNeighbor;
	public  int unchokingInter;
	public  int optUnchokingInter;
	public  String dataFileName;
	public  int fileSize;
	public  int pieceSize;
	
	//for debugging purpose
	public void printConfig(){
		System.out.println("Num of Neighbor: "+this.numPerferredNeighbor);
		System.out.println("unchokingInter: "+this.unchokingInter);
		System.out.println("optUnchokingInter: "+this.optUnchokingInter);
		System.out.println("dataFileName: "+this.dataFileName);
		System.out.println("fileSize: "+this.fileSize);
		System.out.println("pieceSize: "+this.pieceSize);
	}
}
