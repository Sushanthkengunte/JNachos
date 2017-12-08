package jnachos.filesystem;



import jnachos.kern.Debug;
import jnachos.kern.JNachos;
import jnachos.machine.Disk;
import jnachos.machine.JavaSys;

public class SegmentSummary {
	
	//public static  String[] summary = new String[LogStructureFS.NumberOfSectorsInASegment-1];
	//should we include this??
	private static int segmentNumber = 0;
	
	public static final int FILENAMEMAXLEN = 9;
	public static final int NumberOFSectorsInSegmentSummary = LogStructureFS.NumberOfSectorsInASegment-8;
	
	class SegmentSummaryValue {
		/** Is this directory entry in use? */
//		public boolean mInUse;
//
//		/**
//		 * Location on disk to find the FileHeader for this file.
//		 */
//		public int mSector;
//
//		/**
//		 * Text name for file, with +1 for the trailing '\0'
//		 */
		public char[] mName;

		/** DirectoryEntry constructor. */
		SegmentSummaryValue() {
		}

	}
	
	/**
	 * Computes how large a directory entry is.
	 * 
	 * @return The size of a directory entry
	 */
	public static int sizeOfSegmentSummaryValue() {
		return FILENAMEMAXLEN * 2 ;
	}

	/** Number of directory entries. */
	//private int mSegmentSummarySize;

	/**
	 * Table of pairs: <file name, file header location>
	 */
	private SegmentSummaryValue[] segmentSummaryTable;
	
	public SegmentSummary() {
		// Create teh table array
		segmentSummaryTable = new SegmentSummaryValue[NumberOFSectorsInSegmentSummary];

		// Save the table size
		//mSegmentSummarySize = NumberOFSectorsInSegmentSummary;

		// Create the directory taable
		for (int i = 0; i < NumberOFSectorsInSegmentSummary; i++) {
			segmentSummaryTable[i] = new SegmentSummaryValue();
			//segmentSummaryTable[i].mInUse = false;
		}
	}

//	public void updateSegmentNumber(int num){
//		segmentNumber = num;
//	}
//	
//	
//	public static void writeSegmentSummary(int sector,String pFileName){
//		//int segmentNumber = sector % LogStructureFS.NumberOfSegments;
//		int endOfPreviousSegment = LogStructureFS.NumberOfSectorsInASegment*(segmentNumber-1);
//		int sectorOffset = sector - endOfPreviousSegment;
//		summary[sectorOffset] = pFileName;
//	}
	//public void fetchFrom(OpenFile pFile) 
	public void fetchFrom(int sector) {
		


		int size = NumberOFSectorsInSegmentSummary * sizeOfSegmentSummaryValue()+4;
		byte[] buffer = new byte[size];
		//pFile.readAt(buffer, size, sector);
		int divideSize = size/8;
		for(int i=0;i<8;i++){
			
			byte[] sector1Buffer = new byte[divideSize];
			JNachos.mSynchDisk.readSector(sector+i, sector1Buffer);
			System.arraycopy(sector1Buffer, 0, buffer, i*divideSize, divideSize);
				
		}
		
		
		segmentNumber = JavaSys.bytesToInt(buffer, 0).intValue();
		// mTable = new DirectoryEntry[mTableSize];
		for (int i = 0; i < NumberOFSectorsInSegmentSummary; i++) {

			
			segmentSummaryTable[i] = new SegmentSummaryValue();
			//segmentSummaryTable[i].mInUse = (buffer[i * sizeOfSegmentSummaryValue() + 4] == (byte) 1);
			//if (segmentSummaryTable[i].mInUse) {
				//segmentSummaryTable[i].mSector = JavaSys.bytesToInt(buffer, i * sizeOfSegmentSummaryValue() + 5).intValue();
				byte[] bName = new byte[FILENAMEMAXLEN * 2];
				System.arraycopy(buffer, i * sizeOfSegmentSummaryValue()+4, bName, 0, bName.length);
				segmentSummaryTable[i].mName = new String(bName).trim().toCharArray();
				// System.out.println("FILE: " + mTable[i].mName.t);
			//}
			
			
			
				
//				byte[] bName = new byte[FILENAMEMAXLEN * 2];
//				System.arraycopy(buffer, i + 4, bName, 0, bName.length);
//				summary[i] = new String(bName).trim();
				// System.out.println("FILE: " + mTable[i].mName.t);
			
			/*
			 * String sName = new String(mTable[i].mName); byte bName =
			 * sName.getBytes(); byte nameBuffer = new byte[FILENAMEMAXLEN * 2];
			 * System.arraycopy(bName,0, nameBuffer,0,
			 * Math.min(bName.length,nameBuffer.length));
			 * System.arraycopy(nameBuffer, 0, buffer, i *
			 * sizeOfDirectoryEntry() + 5,nameBuffer.length );
			 */
		}

	}

	
	//public static void writeIntoTheDisk(OpenFile pFile)
	public  void writeIntoTheDisk(int sector){
//		

		int size = NumberOFSectorsInSegmentSummary * sizeOfSegmentSummaryValue()+4;
		byte[] buffer = new byte[size];
		JavaSys.intToBytes(segmentNumber, buffer, 0);
		int divideSize = size/8;
		
		for (int i = 0; i < NumberOFSectorsInSegmentSummary; i++) {
			
			//buffer[i * sizeOfSegmentSummaryValue() + 4] = (byte) (segmentSummaryTable[i].mInUse ? 1 : 0);
			if (segmentSummaryTable[i].mName!=null) {
				//JavaSys.intToBytes(segmentSummaryTable[i].mSector, buffer, i * sizeOfSegmentSummaryValue() + 5);
			
				String sName = new String(segmentSummaryTable[i].mName);
				for (int j = sName.length(); j < FILENAMEMAXLEN; j++) {
					sName += " ";
				}
				byte[] bName = sName.getBytes();
				byte[] nameBuffer = new byte[FILENAMEMAXLEN * 2];
				System.arraycopy(bName, 0, nameBuffer, 0, Math.min(bName.length, nameBuffer.length));
				System.arraycopy(nameBuffer, 0, buffer, i * sizeOfSegmentSummaryValue()+4, nameBuffer.length);
			}
			
		}
		for(int i=0;i<8;i++){
			
			byte[] sector1Buffer = new byte[divideSize];
			JNachos.mSynchDisk.writeSector(sector+i, sector1Buffer);
			System.arraycopy(buffer, i*divideSize, sector1Buffer, 0, divideSize);
			JNachos.mSynchDisk.writeSector(sector+i, sector1Buffer);
			
		}
		
		//pFile.writeAt(buffer, buffer.length, sector);
		
		
		
	}
	public int findIndex(String pName) {

		// Search through the directory table
		for (int i = 0; i < NumberOFSectorsInSegmentSummary; i++) {
			// if the entry is found
			if (pName.equals(new String(segmentSummaryTable[i].mName))) {
				return i;
			}
		}

		// name not in directory
		return -1;
	}
	
	public int find(String pName) {
		int i = findIndex(pName);
		Debug.print('f', "FileName : " + pName + "  Found at : " + i);

		if (i != -1) {
			return	i;
		}
		return -1;
	}
	public void clear(){
		
		segmentSummaryTable = new SegmentSummaryValue[NumberOFSectorsInSegmentSummary];

		// Save the table size
		//mSegmentSummarySize = NumberOFSectorsInSegmentSummary;

		// Create the directory taable
		for (int i = 0; i < NumberOFSectorsInSegmentSummary; i++) {
			segmentSummaryTable[i] = new SegmentSummaryValue();
			//segmentSummaryTable[i].mInUse = false;
		}
		
	}
	public void writeItself(){
		int temp=0;
		//System.out.println(segmentNumber);
		if(segmentNumber == 0){
			temp = NumberOFSectorsInSegmentSummary;
		}else{
			temp = ((NumberOFSectorsInSegmentSummary*(segmentNumber+1))+segmentNumber*8);
		}
		 
		writeIntoTheDisk(temp);
		
	}
	public void setNextSegmentNumber(int value){
		segmentNumber = value;
	}
	public void writeInSegmentSummary(int sectorOffset, String pfileName,int segmentNumber){
		
		//if(!segmentSummaryTable[sectorOffset].mInUse){
			
			segmentSummaryTable[sectorOffset].mName = pfileName.toCharArray();
			//segmentSummaryTable[sectorOffset].mInUse = true;
			//segmentSummaryTable[sectorOffset].mSector = segmentNumber-1*LogStructureFS.NumberOfSectorsInASegment + sectorOffset;
			
		//}
		
		
	}
	public String getFileNameWhichUsesTheSector(int value){
		
		
		String fileName = new String(segmentSummaryTable[value].mName);		
		return fileName;
		
	}
	public void printContentsOFSegment(){
		//System.out.println("Contents of Segment Summary");
		//System.out.println("Next segment"+ segmentNumber);
		for (int i = 0; i < NumberOFSectorsInSegmentSummary; i++) {
			if (segmentSummaryTable[i].mName!=null) {
				//JavaSys.intToBytes(segmentSummaryTable[i].mSector, buffer, i * sizeOfSegmentSummaryValue() + 5);
			
				String sName = new String(segmentSummaryTable[i].mName);
				for (int j = sName.length(); j < FILENAMEMAXLEN; j++) {
					sName += " ";
				}
				//System.out.println("Sector"+i+"is used by the file"+sName);
			
		}
		}
	}
	

}
