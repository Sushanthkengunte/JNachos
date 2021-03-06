package jnachos.filesystem;

import jnachos.kern.Debug;
import jnachos.kern.JNachos;
import jnachos.machine.Disk;

public class LogOpenFile implements OpenFile {
	/** The header for the file. */
	private InodeNormal mHdr;

	/** The position within the file. */
	private int mSeekPosition;

	/**
	 * Open a Nachos file for reading and writing. Bring the file header into
	 * memory while the file is open.
	 *
	 * @param sector
	 *            the location on disk of the file header for this file
	 */
	public LogOpenFile(int sector,String fileName) {
		mHdr = new InodeNormal(fileName);
		mHdr.fetchFrom(sector);
		mSeekPosition = 0;
	}

	/**
	 * Close a Nachos file, de-allocating any in-memory data structures.
	 */
	public void delete() {
		mHdr.delete();
	}

	/**
	 * Change the current location within the open file -- the point at which
	 * the next Read or Write will start from.
	 *
	 * @param position
	 *            the location within the file for the next Read/Write.
	 */
	public void seek(int position) {
		mSeekPosition = position;
	}

	/**
	 * Read/write a portion of a file, starting from mSeekPosition. Return the
	 * number of bytes actually written or read, and as a side effect, increment
	 * the current position within the file.
	 *
	 * Implemented using the more primitive ReadAt/WriteAt.
	 *
	 * @param into
	 *            the buffer to contain the data to be read from disk
	 * @param from
	 *            the buffer containing the data to be written to disk
	 * @param numBytes
	 *            the number of bytes to transfer
	 */
	public int read(byte[] into, int numBytes) {
		int result = readAt(into, numBytes, mSeekPosition);
		mSeekPosition += result;
		return result;
	}

	public int write(byte[] into, int numBytes) {
		int result = writeAt(into, numBytes, mSeekPosition);
		mSeekPosition += result;
		return result;
	}

	/**
	 * Read/write a portion of a file, starting at "position". Return the number
	 * of bytes actually written or read, but has no side effects (except that
	 * Write modifies the file, of course).
	 * 
	 * There is no guarantee the request starts or ends on an even disk sector
	 * boundary; however the disk only knows how to read/write a whole disk
	 * sector at a time. Thus:
	 * 
	 * For ReadAt: We read in all of the full or partial sectors that are part
	 * of the request, but we only copy the part we are interested in. For
	 * WriteAt: We must first read in any sectors that will be partially
	 * written, so that we don't overwrite the unmodified portion. We then copy
	 * in the data that will be modified, and write back all the full or partial
	 * sectors that are part of the request.
	 * 
	 * @param into
	 *            the buffer to contain the data to be read from disk
	 * @param from
	 *            the buffer containing the data to be written to disk
	 * @param numBytes
	 *            the number of bytes to transfer
	 * @param position
	 *            the offset within the file of the first byte to be
	 *            read/written
	 *            ----------------------------------------------------------------------
	 */
	public int readAt(byte[] into, int numBytes, int position) {
		int fileLength = mHdr.fileLength();
		int i, firstSector, lastSector, numSectors;
		byte[][] buf;
		

		if ((numBytes <= 0) || (position >= fileLength)) {
			return 0; // check request
		}

		if ((position + numBytes) > fileLength) {
			numBytes = fileLength - position;
		}

		Debug.print('f', "Reading " + numBytes + " bytes at " + position + ", from file of length " + fileLength);

		firstSector = (int) Math.floor((double) position / (double) Disk.SectorSize);
		lastSector = (int) Math.floor(((double) position + numBytes - 1) / (double) Disk.SectorSize);

		numSectors = 1 + lastSector - firstSector;

		// read in all the full and partial sectors that we need
		buf = new byte[numSectors][Disk.SectorSize];

		int skip = position - (firstSector * Disk.SectorSize);
		int offset = 0;
		int bytesRead = 0;
		for (i = firstSector; i <= lastSector; i++) {
			int sectorNumber = mHdr.byteToSector(i * Disk.SectorSize);
			JNachos.mSynchDisk.readSector(mHdr.byteToSector(i * Disk.SectorSize), buf[(i - firstSector)]);

			// Copy the data into the buffer
			if (i == firstSector) {
				System.arraycopy(buf[(i - firstSector)], skip, into, offset,
						Math.min(into.length, Disk.SectorSize - skip));

				offset += Math.min(into.length, Disk.SectorSize - skip);
			} else if (i == lastSector) {

				// System.out.println(into.length +"\t" + numBytes + "\t" +( i *
				// Disk.SectorSize) + "\t" + (Disk.SectorSize - ((lastSector +
				// 1) * Disk.SectorSize) - (position + numBytes)));
				// System.out.println(Disk.SectorSize + "\t" + position + "\t" +
				// lastSector);
				System.arraycopy(buf[(i - firstSector)], 0, into, offset,
						Disk.SectorSize - (((lastSector + 1) * Disk.SectorSize) - (position + numBytes)));
			} else {
				System.arraycopy(buf[(i - firstSector)], 0, into, offset, Disk.SectorSize);
				offset += Disk.SectorSize;
			}
		}

		return numBytes;
	}

	public int writeAt(byte[] from, int numBytes, int position) {
		int fileLength = mHdr.fileLength();
		int i, firstSector, lastSector, numSectors;
		boolean firstAligned, lastAligned;
		byte[][] buf;
		
		String fileName = new String(mHdr.inodeName);

		if ((numBytes <= 0) || (position >= fileLength)) {
			return 0; // check request
		}

		if ((position + numBytes) > fileLength) {
			numBytes = fileLength - position;
		}

		Debug.print('f', "Writing " + numBytes + " bytes at " + position + ", from file of length " + fileLength);

		// ceil
		firstSector = (int) Math.floor(((double) position) / Disk.SectorSize);
		lastSector = (int) Math.floor(((double) position + numBytes - 1) / Disk.SectorSize);

		numSectors = 1 + lastSector - firstSector;

		buf = new byte[numSectors][Disk.SectorSize];

		firstAligned = (position == (firstSector * Disk.SectorSize));
		lastAligned = ((position + numBytes) == ((lastSector + 1) * Disk.SectorSize));

		int firstSkip = position - (firstSector * Disk.SectorSize);
		int lastSkip = ((lastSector + 1) * Disk.SectorSize) - (position + numBytes);

		// read in first and last sector, if they are to be partially modified
		if (!firstAligned) {
			readAt(buf[0], Disk.SectorSize, firstSector * Disk.SectorSize);
		}
		if (!lastAligned && ((firstSector != lastSector) || firstAligned)) {
			readAt(buf[(lastSector - firstSector)], Disk.SectorSize, lastSector * Disk.SectorSize);
		}

		// copy in the bytes we want to change
		// bcopy(from, &buf[position - (firstSector * SectorSize)], numBytes);
		// System.arraycopy(from,0,buf,position - (firstSector *
		// Disk.SectorSize), numBytes);
		// ? Need quick array copy

		// System.out.println("FS: " + firstSkip);
		int offset = 0;
		//copyBitmap From mfreeMap file into sectorMapDS
		//JNachos.mFileSystem.fillOutDS();
		//System.out.println(JNachos.mFileSystem.SectorMapDS);
		
		// write modified sectors back
		for (i = firstSector; i <= lastSector; i++) {

			if (i == firstSector) {
				int bytes = (i + 1) * Disk.SectorSize - position;
				
//				byte[] zeroOutPreviousData = new byte[Disk.SectorSize];
//				
//				System.arraycopy(from, offset, zeroOutPreviousData, 0,Disk.SectorSize);
				
				System.arraycopy(from, offset, buf[0], position - firstSector * Disk.SectorSize,Math.min(bytes, numBytes));
				
//				System.arraycopy(from, offset, buf[0], position - firstSector * Disk.SectorSize,
//						Math.min(bytes, numBytes));
				offset += bytes;
			} else if (i == lastSector) {
				// System.out.println("LS: " + lastSkip + "loc :" + position);
				System.arraycopy(from, offset, buf[lastSector - firstSector], 0,
						position + numBytes - lastSector * Disk.SectorSize);
			} else {
				System.arraycopy(from, offset, buf[i - firstSector], 0, Disk.SectorSize);
				offset += Disk.SectorSize;
			}
			
			//MajorChange got a free sector and set it in the inode
			//if(!(fileName.equalsIgnoreCase(LogStructureFS.DirectoryName) || fileName.equalsIgnoreCase(LogStructureFS.FreeSectorMapName)  || fileName.equalsIgnoreCase(LogStructureFS.FreeSegmentMapName))){
				
			//String fileNameFor = String.valueOf(mHdr.inodeName).toString();
			
			int newSector = JNachos.mFileSystem.findAFreeSector(fileName);
				
			mHdr.modifyTheInBlockInformation(i, newSector);
			
			//}
			
//			int sectorNumber = mHdr.byteToSector(i * Disk.SectorSize);
			int sectorNumber = newSector;
//			byte[] zeroOutPreviousData = new byte[Disk.SectorSize];
//			JNachos.mSynchDisk.writeSector(sectorNumber,zeroOutPreviousData);
			
			byte[] data = buf[(i - firstSector)];
		
			
			JNachos.mSynchDisk.writeSector(sectorNumber,data );//
			
			
			
			//mHdr.allocate(pFreeMap, pFileSize, segmentManager)
		}
		//writeBack into mFreeMap File
		//JNachos.mFileSystem.writeBackTheFile();
		JNachos.mFileSystem.mSummary.writeItself();
		if(!(fileName.equalsIgnoreCase(LogStructureFS.DirectoryName) || fileName.equalsIgnoreCase(LogStructureFS.FreeSectorMapName)  || fileName.equalsIgnoreCase(LogStructureFS.FreeSegmentMapName))){
			int newInodeSector = JNachos.mFileSystem.findAFreeSector(fileName+"_H");
			mHdr.writeBack(newInodeSector);
		}else if(fileName.equalsIgnoreCase(LogStructureFS.DirectoryName)){
			mHdr.writeBack(LogStructureFS.InodeMapSector);
		}else if(fileName.equalsIgnoreCase(LogStructureFS.FreeSectorMapName)){
			mHdr.writeBack(LogStructureFS.FreeMapSector);
		}else if (fileName.equalsIgnoreCase(LogStructureFS.FreeSegmentMapName)){
			mHdr.writeBack(LogStructureFS.freeSegments);
		}

		return numBytes;
	}
	public int writeAtFirst(byte[] from, int numBytes, int position) {
		int fileLength = mHdr.fileLength();
		int i, firstSector, lastSector, numSectors;
		boolean firstAligned, lastAligned;
		byte[][] buf;
		
		String fileName = new String(mHdr.inodeName);

		if ((numBytes <= 0) || (position >= fileLength)) {
			return 0; // check request
		}

		if ((position + numBytes) > fileLength) {
			numBytes = fileLength - position;
		}

		Debug.print('f', "Writing " + numBytes + " bytes at " + position + ", from file of length " + fileLength);

		// ceil
		firstSector = (int) Math.floor(((double) position) / Disk.SectorSize);
		lastSector = (int) Math.floor(((double) position + numBytes - 1) / Disk.SectorSize);

		numSectors = 1 + lastSector - firstSector;

		buf = new byte[numSectors][Disk.SectorSize];

		firstAligned = (position == (firstSector * Disk.SectorSize));
		lastAligned = ((position + numBytes) == ((lastSector + 1) * Disk.SectorSize));

		int firstSkip = position - (firstSector * Disk.SectorSize);
		int lastSkip = ((lastSector + 1) * Disk.SectorSize) - (position + numBytes);

		// read in first and last sector, if they are to be partially modified
		if (!firstAligned) {
			readAt(buf[0], Disk.SectorSize, firstSector * Disk.SectorSize);
		}
		if (!lastAligned && ((firstSector != lastSector) || firstAligned)) {
			readAt(buf[(lastSector - firstSector)], Disk.SectorSize, lastSector * Disk.SectorSize);
		}

		// copy in the bytes we want to change
		// bcopy(from, &buf[position - (firstSector * SectorSize)], numBytes);
		// System.arraycopy(from,0,buf,position - (firstSector *
		// Disk.SectorSize), numBytes);
		// ? Need quick array copy

		// System.out.println("FS: " + firstSkip);
		int offset = 0;
		// write modified sectors back
		for (i = firstSector; i <= lastSector; i++) {

			if (i == firstSector) {
				int bytes = (i + 1) * Disk.SectorSize - position;

				System.arraycopy(from, offset, buf[0], position - firstSector * Disk.SectorSize,
						Math.min(bytes, numBytes));
				offset += bytes;
			} else if (i == lastSector) {
				// System.out.println("LS: " + lastSkip + "loc :" + position);
				System.arraycopy(from, offset, buf[lastSector - firstSector], 0,
						position + numBytes - lastSector * Disk.SectorSize);
			} else {
				System.arraycopy(from, offset, buf[i - firstSector], 0, Disk.SectorSize);
				offset += Disk.SectorSize;
			}
			
			
			int sectorNumber = mHdr.byteToSector(i * Disk.SectorSize);
			byte[] data = buf[(i - firstSector)];
		
			
			JNachos.mSynchDisk.writeSector(sectorNumber,data );//
			
			//mHdr.allocate(pFreeMap, pFileSize, segmentManager)
		}
		if(!(fileName.equalsIgnoreCase(LogStructureFS.DirectoryName) || fileName.equalsIgnoreCase(LogStructureFS.FreeSectorMapName)  || fileName.equalsIgnoreCase(LogStructureFS.FreeSegmentMapName))){
			int newInodeSector = JNachos.mFileSystem.findAFreeSector(mHdr.inodeName.toString()+"_H");
			mHdr.writeBack(newInodeSector);
			
		}
		//JNachos.mFileSystem.mSummary.writeItself();

		return numBytes;
	}

	/**
	 * Closes the file
	 */
	public void closeFile() {

	}

	/**
	 * Return the number of bytes in the file.
	 */
	public int length() {
		return mHdr.fileLength();
	}
}
