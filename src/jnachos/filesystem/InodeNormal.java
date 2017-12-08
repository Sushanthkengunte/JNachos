package jnachos.filesystem;

import java.nio.CharBuffer;

import jnachos.kern.JNachos;
import jnachos.machine.Disk;
import jnachos.machine.JavaSys;

public class InodeNormal {
	
	/** The number of direct pointers */
	public static final int NumDirect = ((Disk.SectorSize - 2 * 4) / 4);

	/** The maximum size of a file in JNachos */
	public static final int MaxFileSize = (NumDirect * Disk.SectorSize);
	
	public static final int FILENAMEMAXLEN = 9;

	/** Number of bytes in the file. */
	private int mNumBytes;

	/** Number of data sectors in the file. */
	private int mNumSectors;

	/** Disk sector numbers for each data block in the file. */
	private int[] mDataSectors;
	
	/** Filename for the inode*/
	public char[] inodeName;

	/**
	 * Default constructor.
	 */
	public InodeNormal(String fileName) {
		mDataSectors = new int[NumDirect];
		inodeName = fileName.toCharArray() ;
	}

	/**
	 * 
	 */
	public void delete() {

	}
	
//	public static int findAFreeSector(BitMap sectorMap,BitMap segmentManager){
//		int sectorNumber = sectorMap.find();
//		int segmentNumber = sectorNumber / LogStructureFS.NumberOfSectorsInASegment;
//		int sectorOffset = sectorNumber - ((segmentNumber)*LogStructureFS.NumberOfSectorsInASegment);
//		//int endOfSegment = NumberOfSectorsInASegment*segmentNumber;
//		if(sectorOffset == LogStructureFS.NumberOfSectorsInASegment-2){
//			segmentManager.mark(segmentNumber);
//			//mSummary.updateSegmentNumber(segmentNumber+1);
//			//TODO run cleaner 
//			
//			
//		}
//		//TODO  update the segment summary
//		//sectorMap.mark(sectorNumber);
//		return sectorNumber;
//	}

	/**
	 * Initialize a fresh file header for a newly created file. Allocate data
	 * blocks for the file out of the map of free disk blocks.
	 * 
	 *
	 * @param pFreeMap
	 *            the bit map of free disk sectors.
	 * @param pFileSize
	 *            the bit map of free disk sectors.
	 * @return true if successful, false if there are not enough free blocks to
	 *         accomodate the new file.
	 */
	public boolean allocate(BitMap pFreeMap,int pFileSize,BitMap segmentManager) {
		mNumBytes = pFileSize;
		mNumSectors = (int) Math.ceil((double) pFileSize / (double) Disk.SectorSize);

		//int mNumberOfFreeSectors = LogInformation.numberOfFreeSegments() * LogStructureFS.NumberOfSectorsInASegment;
		//int mNumberOfFreeSectors = pFreeMap.numClear() * LogStructureFS.NumberOfSectorsInASegment;
		// not enough space
		if (pFreeMap.numClear() < mNumSectors) {
			return false;
		}
		
		// Find enough sectors
//		for (int i = 0; i < mNumSectors; i++) {
//			mDataSectors[i] = findAFreeSector(pFreeMap,segmentManager);
//			
//		}
		return true;
	}

	/**
	 * De-allocate all the space allocated for data blocks for this file.
	 *
	 * @param pFreeMap
	 *            the bit map of free disk sectors.
	 * @throws Assertion
	 *             Error if the
	 */

	public void deallocate(BitMap pFreeMap) {
		for (int i = 0; i < mNumSectors; i++) {
			// ought to be marked!
			assert (pFreeMap.test(mDataSectors[i]));
			pFreeMap.clear(mDataSectors[i]);
		}
	}

	/**
	 * Fetch contents of file header from disk.
	 *
	 * @param sector
	 *            the disk sector containing the file header
	 */
	public void fetchFrom(int sector) {
		byte[] buffer = new byte[Disk.SectorSize];
		JNachos.mSynchDisk.readSector(sector, buffer);

		mNumBytes = JavaSys.bytesToInt(buffer, 0);
		mNumSectors = JavaSys.bytesToInt(buffer, 4);
		//inodeName = JavaSys.bytesToInt(buffer, 8);
		byte[] bName = new byte[FILENAMEMAXLEN * 2];
		System.arraycopy(buffer, 8, bName, 0, bName.length);
		inodeName = new String(bName).trim().toCharArray();
		for (int i = 0; i < mNumSectors; i++) {
			int startValue = 8+ (FILENAMEMAXLEN*2) + i * 4;
			mDataSectors[i] = JavaSys.bytesToInt(buffer, startValue);
		}

	}

	/**
	 * Write the modified contents of the file header back to disk.
	 *
	 * @param sector
	 *            the disk sector to contain the file header.
	 */
	public void writeBack(int sector) {

		byte[] buffer = new byte[Disk.SectorSize];

		
		JavaSys.intToBytes(mNumBytes, buffer, 0);
		JavaSys.intToBytes(mNumSectors, buffer, 4);
		//JavaSys.intToBytes(inodeName, buffer, 8);
		byte[] temp =  new String(inodeName).getBytes();
		System.arraycopy(temp, 0, buffer, 8, temp.length);
		
		for (int i = 0; i < mNumSectors; i++) {
			int startValue = 8+ (FILENAMEMAXLEN*2) + i * 4;
			JavaSys.intToBytes(mDataSectors[i], buffer, startValue);
		}

		JNachos.mSynchDisk.writeSector(sector, buffer);
		//JNachos.mFileSystem
		JNachos.mFileSystem.changeInodeMapAndWriteItBack(inodeName, sector);
	}

	public int isThisSectorUsed(int sector){
		for(int i=0;i<mDataSectors.length;i++ ){
			if(sector == mDataSectors[i])
				return i;
		}
		
		
		return -1;
	}
	
	/**
	 * Converts virtual address to sector number. This is essentially a
	 * translation from a virtual address (the offset in the file) to a physical
	 * address (the sector where the data at the offset is stored).
	 *
	 * @param offset
	 *            the location within the file of the byte in question
	 * @return The disk sector which is storing a particular byte within the
	 *         file.
	 */
	public int byteToSector(int offset) {
		return (mDataSectors[offset / Disk.SectorSize]);
	}

	/**
	 * Return the number of bytes in the file.
	 */
	public int fileLength() {
		return mNumBytes;
	}
	
	public void modifyTheInBlockInformation(int sectorNumberToModify,int value){
		mDataSectors[sectorNumberToModify] = value;
	}

	/**
	 * Print the contents of the file header, and the contents of all the data
	 * blocks pointed to by the file header.
	 */
	public void print() {
		int i, j, k;
		byte[] data = new byte[Disk.SectorSize];

		System.out.println("FileHeader contents.  File size: " + mNumBytes + "File Name"+inodeName+" File blocks:");

		for (i = 0; i < mNumSectors; i++) {
			System.out.println(mDataSectors[i] + " ");
		}

		System.out.println("File contents:");

		for (i = k = 0; i < mNumSectors; i++) {
			JNachos.mSynchDisk.readSector(mDataSectors[i], data);
			// for (j = 0; (j < Disk.SectorSize) && (k < mNumBytes); j++, k++)
			// {
			System.out.println(new String(data));
			/*
			 * if ('\040' <= data[j] && data[j] <= '\176') // isprint(data[j])
			 * System.out.println(new String(data[j])); else
			 * System.out.println(new String(data[j]));
			 */
			// }
		}
		// delete data;
	}

}
