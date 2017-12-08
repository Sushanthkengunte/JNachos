package jnachos.filesystem;




import jnachos.kern.Debug;

import jnachos.machine.JavaSys;

public class InodeMap {
	
//	private static  HashMap<Integer,InodeMapValue> imap=new HashMap<Integer,InodeMapValue>();
//
//	public static InodeMapValue getImapValue(Integer fileDescriptor) {
//		if(!imap.containsKey(fileDescriptor))
//			return null;
//		
//		return imap.get(fileDescriptor);
//	}
//
//	public static void updateImap(Integer fileDescriptor, InodeMapValue address) {
//		if(imap.containsKey(fileDescriptor))
//			imap.put(fileDescriptor, address);
//	}
	/**
	 * for simplicity, we assume file names are <= 9 characters long
	 */
	public static final int FILENAMEMAXLEN = 9;

	/**
	 * The following class defines a "directory entry", representing a file in
	 * the directory. Each entry gives the name of the file, and where the
	 * file's header is to be found on disk.
	 *
	 * Internal data structures kept public so that Directory operations can
	 * access them directly.
	 */
	class InodeMapValue {
		/** Is this directory entry in use? */
		public boolean mInUse;

		/**
		 * Location on disk to find the FileHeader for this file.
		 */
		public int mSector;

		/**
		 * Text name for file, with +1 for the trailing '\0'
		 */
		public char[] mName;

		/** DirectoryEntry constructor. */
		InodeMapValue() {
		}

	}

	/**
	 * Computes how large a directory entry is.
	 * 
	 * @return The size of a directory entry
	 */
	public static int sizeOfInodeMapValue() {
		return FILENAMEMAXLEN * 2 + 1 + 4;
	}

	/** Number of directory entries. */
	private int mTableSize;

	/**
	 * Table of pairs: <file name, file header location>
	 */
	private InodeMapValue[] mTable;

	/**
	 * Initialize a directory; initially, the directory is completely empty. If
	 * the disk is being formatted, an empty directory is all we need, but
	 * otherwise, we need to call FetchFrom in order to initialize it from disk.
	 *
	 * @param pSize
	 *            the number of entries in the directory.
	 */

	public InodeMap(int pSize) {
		// Create teh table array
		mTable = new InodeMapValue[pSize];

		// Save the table size
		mTableSize = pSize;

		// Create the directory taable
		for (int i = 0; i < mTableSize; i++) {
			mTable[i] = new InodeMapValue();
			mTable[i].mInUse = false;
		}
	}

	/**
	 * De-allocate directory data structure.
	 */
	public void delete() {
		// delete table;
	}

	/**
	 * Read the contents of the directory from disk.
	 *
	 * @param file
	 *            the file containing the directory contents
	 */
	public void fetchFrom(OpenFile pFile) {

		int size = LogStructureFS.NumDirEntries * sizeOfInodeMapValue();
		byte[] buffer = new byte[size];
		pFile.readAt(buffer, size, 0);

		mTableSize = JavaSys.bytesToInt(buffer, 0).intValue();
		// mTable = new DirectoryEntry[mTableSize];
		for (int i = 0; i < mTableSize; i++) {

			mTable[i] = new InodeMapValue();
			mTable[i].mInUse = (buffer[i * sizeOfInodeMapValue() + 4] == (byte) 1);
			if (mTable[i].mInUse) {
				mTable[i].mSector = JavaSys.bytesToInt(buffer, i * sizeOfInodeMapValue() + 5).intValue();
				byte[] bName = new byte[FILENAMEMAXLEN * 2];
				System.arraycopy(buffer, i * sizeOfInodeMapValue() + 9, bName, 0, bName.length);
				mTable[i].mName = new String(bName).trim().toCharArray();
				// System.out.println("FILE: " + mTable[i].mName.t);
			}
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

	/**
	 * Write any modifications to the directory back to disk
	 *
	 * @param file
	 *            The file to contain the new directory contents.
	 */
	public void writeBack(OpenFile pFile) {
		int size = NachosFileSystem.NumDirEntries * sizeOfInodeMapValue() + 4;
		byte[] buffer = new byte[size];

		JavaSys.intToBytes(mTableSize, buffer, 0);

		// (void) file->ReadAt((char *)table, tableSize *
		// sizeof(DirectoryEntry), 0);
		for (int i = 0; i < mTableSize; i++) {
			buffer[i * sizeOfInodeMapValue() + 4] = (byte) (mTable[i].mInUse ? 1 : 0);
			if (mTable[i].mInUse) {
				JavaSys.intToBytes(mTable[i].mSector, buffer, i * sizeOfInodeMapValue() + 5);
				String sName = new String(mTable[i].mName);
				for (int j = sName.length(); j < FILENAMEMAXLEN; j++) {
					sName += " ";
				}
				byte[] bName = sName.getBytes();
				byte[] nameBuffer = new byte[FILENAMEMAXLEN * 2];
				System.arraycopy(bName, 0, nameBuffer, 0, Math.min(bName.length, nameBuffer.length));
				System.arraycopy(nameBuffer, 0, buffer, i * sizeOfInodeMapValue() + 9, nameBuffer.length);
			}
		}

		pFile.writeAt(buffer, buffer.length, 0);

	}

	
	/**
	 * Look up file name in directory, and return its location in the table of
	 * directory entries.
	 *
	 * @param pName
	 *            the file name to look up
	 * @return -1 if the name isn't in the directory, the location in the table
	 *         of directory entries where the file is saved.
	 */
	public int findIndex(String pName) {

		// Search through the directory table
		for (int i = 0; i < mTableSize; i++) {
			// if the entry is found
			if (mTable[i].mInUse && pName.equals(new String(mTable[i].mName))) {
				return i;
			}
		}

		// name not in directory
		return -1;
	}

	/**
	 * Look up file name in directory, and return the disk sector number where
	 * the file's header is stored. Return -1 if the name isn't in the
	 * directory.
	 *
	 * @param pName
	 *            the file name to look up
	 * @return the disk sector number where the file's header is stored. -1 if
	 *         the name isn't in the directory.
	 */
	public int find(String pName) {
		int i = findIndex(pName);
		Debug.print('f', "FileName : " + pName + "  Found at : " + i);

		if (i != -1) {
			return mTable[i].mSector;
		}
		return -1;
	}
	
	public void update(String pName,int sector){
		int fileIndex = findIndex(pName);
		if(fileIndex == -1){
			add(pName, sector);
		}else{
			mTable[fileIndex].mSector = sector;	
		}
		
		
	}

	/**
	 * Add a file into the directory. Return TRUE if successful; return FALSE if
	 * the file name is already in the directory, or if the directory is
	 * completely full, and has no more space for additional file names.
	 *
	 * @param PName
	 *            the name of the file being added.
	 * @param pNewSector
	 *            the disk sector containing the added file's header.
	 * @return true if successful, false otherwise.
	 */
	public boolean add(String pName, int pNewSector) {
		if (pName.length() > 9) {
			assert (false);
		}

		if (findIndex(pName) != -1) {
			return false;
		}

		// Iterate through the table
		for (int i = 0; i < mTableSize; i++) {
			// If the entry is not inuse
			if (!mTable[i].mInUse) {
				// Mark it as in use
				mTable[i].mInUse = true;

				// strncpy(table[i].name, name, FileNameMaxLen);
				mTable[i].mName = pName.toCharArray();
				mTable[i].mSector = pNewSector;
				return true;
			}
		}
		return false; // no space. Fix when we have extensible files.
	}

	/**
	 * Remove a file name from the directory.
	 *
	 * @param pName
	 *            the file name to be removed
	 * @return true if successful, false if the file is not in the directory.
	 */
	public boolean remove(String pName) {
		int i = findIndex(pName);

		// name not in directory
		if (i == -1) {
			return false;
		}

		mTable[i].mInUse = false;
		return true;
	}

	/**
	 * List all the file names in the directory.
	 */
	public void list() {
		System.out.println("Printing");

		for (int i = 0; i < mTableSize; i++) {
			if (mTable[i].mInUse) {
				System.out.println(new String(mTable[i].mName));
			}
		}

	}

	/**
	 * List all the file names in the directory, their FileHeader locations, and
	 * the contents of each file. For debugging.
	 */
	public void print() {
		Inode hdr = new Inode();

		System.out.println("Directory contents:\n");

		for (int i = 0; i < mTableSize; i++) {
			if (mTable[i].mInUse) {
				
				System.out.println("Name: " + new String(mTable[i].mName) + ", Sector: " + mTable[i].mSector);
				hdr.fetchFrom(mTable[i].mSector);
				hdr.print();
			}
		}
		// delete hdr;
	}

}
