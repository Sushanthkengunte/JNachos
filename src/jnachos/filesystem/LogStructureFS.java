package jnachos.filesystem;

import java.util.HashMap;

import jnachos.kern.Debug;
import jnachos.kern.JNachos;
import jnachos.machine.Disk;

public class LogStructureFS implements FileSystem {
	
	//To-Do:divide the sectors to form a segment, let the segment size be 10kb i.e aggregate
		//some sectors collectively forming 10 kb for a segment 1024X10/128 = 80, number of blocks in a segment 1024X10/512 = 20 blocks
		//Decide which segment maintains the checkpoint region 
	//Each block is divided into the page size = sector size 512/128 = 4 pages
	
	/** The sector where checkpoint is saved */
	public static final int checkpointSegment = 0;
	
	/** Each segment of size 10kb */
	public static final int SegmentSize = 8192;
	//public static final int SegmentSize = 10240;
	//each block of a file is in one sector
	public static final int BlockSize = 128;
	//20, 19
	/** the subtraction by one is to incorporate a segment summary block*/
	public static final int NumberOfBlocks = (SegmentSize/BlockSize);
	//12.8
	public static final int NumberOfSegments = (Disk.NumSectors * Disk.SectorSize)/SegmentSize;
	//80
	public static final int NumberOfSectorsInASegment = SegmentSize/Disk.SectorSize;
	//4
	public static final int NumberOfSectorsInABlock = NumberOfSectorsInASegment/(NumberOfBlocks+1);
	
	

	// Sectors containing the file headers for the bitmap of free sectors,
	// and the directory of files. These file headers are placed in well-known
	// sectors, so that they can be located on boot-up.
	/** The sector where the bitmap is stored. */
	public static final int FreeMapSector = 1;
	/** The sector where the top level sector is stored. */
	public static final int InodeMapSector = 0;
	public static final int freeSegments = 2;
	
	
	// Initial file sizes for the bitmap and directory; until the file system
	// supports extensible files, the directory size sets the maximum number
	// of files that can be loaded onto the disk.
	public static final int FreeMapFileSize = (Disk.NumSectors);
	public static final int NumDirEntries = 10;
	public static final int InodeMapFileSize = ((9 * 2 + 1 + 4) * NumDirEntries) + 4;
	
	public static final int segmentSummaryFileSize = NumberOfSectorsInASegment*9*2+ 4; 
	
	public static final String DirectoryName = "directory";
	public static final String FreeSectorMapName = "freeSector";
	public static final String FreeSegmentMapName = "segmentMap";
	
	
	// Actual file for inodeMap,FreeSectorFile and FreeSegmentFile
	private   LogOpenFile mInodeMap;
	private  LogOpenFile mFreeMap;
	private  LogOpenFile mFreeSegmentMap;

	//should this be written in the disk as well?
	//private static BitMap segmentManager = new BitMap(NumberOfSegments);	
	
	public static SegmentSummary mSummary = new SegmentSummary();
	//To check for the sector number
	public static int count = 0;
	public static BitMap SectorMapDS = new BitMap(Disk.NumSectors);
	public static BitMap SegmentMapDS = new BitMap(NumberOfSegments);
	public static int LastModifiedSegment = 0;
	public static int CurrentSegment = 0;
	public static int lastSegmentCleaned = -1;
	
	class LiveBlockInformation{
		int segment;
		String fileName;
		int datablock;
		
		
		public LiveBlockInformation() {
			// TODO Auto-generated constructor stub
		}
		
	}
	
	
	public LogStructureFS(){
		
	}
	
	public void LogStructureFSConstructor(boolean pFormat) {
		// super(pFormat);
		Debug.print('f', "Initializing the file system.");

		if (pFormat) {
			//Bit map for sectors
			BitMap freeMap = new BitMap(Disk.NumSectors);
			//directoryStructure which will hold inodeMap values which inturn saves the inodes file
			InodeMap directory = new InodeMap(NumDirEntries);
			//Add segments BitMap
			BitMap segmentManager = new BitMap(NumberOfSegments);
			
			//sectors bitmap, freeMAp
			InodeNormal fileHdr = new InodeNormal(FreeSectorMapName);
			//inodeMap 
			InodeNormal dirHdr = new InodeNormal(DirectoryName);
			//segments BitMap,segmentManager
			InodeNormal segmentFree = new InodeNormal(FreeSegmentMapName);
			

			Debug.print('f', "Formatting the file system.");

			// First, allocate space for FileHeaders for the directory and
			// bitmap
			// (make sure no one else grabs these!)
			//Each time we get a sector we have to check if it fills the segment i.e if the sector is the last but one sector in the segment
			//markTheSector(freeMap,FreeMapSector,segmentManager);
			freeMap.mark(FreeMapSector);
			SectorMapDS.mark(FreeMapSector);
			//markTheSector(freeMap,InodeMapSector,segmentManager);
			freeMap.mark(InodeMapSector);
			SectorMapDS.mark(InodeMapSector);
			//Allocate for segment bit
			//markTheSector(freeMap,freeSegments,segmentManager);
			freeMap.mark(freeSegments);
			SectorMapDS.mark(freeSegments);
			//markTheSector(freeMap,freeSegments,segmentManager);

			// Second, allocate space for the data blocks containing the
			// contents
			// of the directory and bitmap files. There better be enough space!
			//8Inode alli naavu freeMapdu contents yellidhe antha save madtha idhivi
			if (!fileHdr.allocate(freeMap, FreeMapFileSize,segmentManager)) {
				assert (false);
			}
			//2
			if (!dirHdr.allocate(freeMap, InodeMapFileSize,segmentManager)) {
				assert (false);
			}
			//1
			if (!segmentFree.allocate(freeMap, NumberOfSegments,segmentManager)) {
				assert (false);
			}
			

			// Flush the bitmap and directory FileHeaders back to disk
			// We need to do this before we can "Open" the file, since open
			// reads the file header off of disk (and currently the disk has
			// garbage
			// on it!).
			Debug.print('f', "Writing headers back to disk.");
			//placing three inodes in first three sectors
			fileHdr.writeBack(FreeMapSector);
			JNachos.mFileSystem.mSummary.writeInSegmentSummary(FreeMapSector, "freeSector_H", 0);
			dirHdr.writeBack(InodeMapSector);
			JNachos.mFileSystem.mSummary.writeInSegmentSummary(InodeMapSector, "directory_H", 0);
			segmentFree.writeBack(freeSegments);
			JNachos.mFileSystem.mSummary.writeInSegmentSummary(freeSegments, "freeSegments_H", 0);
			
			
			//Saved the Inode into the file
			
			
			// OK to open the bitmap and directory files now
			// The file system operations assume these two files are left open
			// while Nachos is running.
			// creating file instances of the first three sectors and extracting the inode information into the files
			
			mFreeMap = new LogOpenFile(FreeMapSector,FreeSectorMapName);
			mInodeMap = new LogOpenFile(InodeMapSector,DirectoryName);
			mFreeSegmentMap = new LogOpenFile(freeSegments,FreeSegmentMapName);

			// Once we have the files "open", we can write the initial version
			// of each file back to disk. The directory at this point is
			// completely
			// empty; but the bitmap has been changed to reflect the fact that
			// sectors on the disk have been allocated for the file headers and
			// to hold the file data for the directory and bitmap.
			//writes back to the allocated place
			Debug.print('f', "Writing bitmap and directory back to disk.\n");
			//fillOutDS();
			freeMap.writeBack(mFreeMap); 
			//writeBackTheFile();
			System.out.println(SectorMapDS);
			directory.writeBack(mInodeMap); 
			segmentManager.writeBack(mFreeSegmentMap);
			
			writeBackTheFile();
			writeBackTheSegmentFile();
			for(int i=0;i<8;i++){
				SectorMapDS.mark(SegmentSummary.NumberOFSectorsInSegmentSummary+1+i);
			}
			JNachos.mFileSystem.mSummary.writeIntoTheDisk(SegmentSummary.NumberOFSectorsInSegmentSummary);

			if (Debug.isEnabled('f')) {
				freeMap.print();
				directory.print();
				freeMap.delete();
				directory.delete();
				fileHdr.delete();
				dirHdr.delete();
			}
		} else {
			// if we are not formatting the disk, just open the files
			// representing
			// the bitmap and directory; these are left open while Nachos is
			// running
			//JNachos.mFileSystem.mSummary.fetchFrom(sector);
			mFreeMap = new LogOpenFile(FreeMapSector,FreeSectorMapName);
			mInodeMap = new LogOpenFile(InodeMapSector,DirectoryName);
			mFreeSegmentMap = new LogOpenFile(freeSegments,FreeSegmentMapName);
			BitMap segmentManager = new BitMap(NumberOfSegments);
			segmentManager.fetchFrom(mFreeSegmentMap);
			fillOutDS();
			//System.out.println(SectorMapDS);
			
			fillOutSegmentDS();
			//System.out.println(SegmentMapDS);
			
			
			int segmentNumber = segmentManager.checkNextWhichIsFree();
			if(segmentNumber == 0){
				LastModifiedSegment = 0;
				CurrentSegment = 0;
				JNachos.mFileSystem.mSummary.fetchFrom(SegmentSummary.NumberOFSectorsInSegmentSummary);
			}else {
				LastModifiedSegment = segmentNumber;
				//CurrentSegment = segmentNumber;
				int correctSector = (SegmentSummary.NumberOFSectorsInSegmentSummary*(LastModifiedSegment+1))+LastModifiedSegment*8;
				//int temp =SegmentSummary.NumberOFSectorsInSegmentSummary*(LastModifiedSegment+1);
				//(NumberOfSectorsInASegment- 9)*(NextSegment+1)
				JNachos.mFileSystem.mSummary.fetchFrom(correctSector);
			}
			//System.out.println("Current segment"+LastModifiedSegment);
			writeBackTheSegmentFile();
			writeBackTheFile();
			JNachos.mFileSystem.mSummary.printContentsOFSegment();	
			
		}
		
	}
	public void writeBackTheSegmentFile(){
		SegmentMapDS.writeBack(mFreeSegmentMap);
	}
	public void fillOutSegmentDS(){
		
	SegmentMapDS.fetchFrom(mFreeSegmentMap);
	int numOfClear = SegmentMapDS.getNumberOfClear();
	SegmentMapDS.setNumClear(numOfClear);
	
		
	}
	public void fillOutDS(){
		
		SectorMapDS.fetchFrom(mFreeMap);
		
	}
	public void writeBackTheFile(){
		SectorMapDS.writeBack(mFreeMap);
	}
	public  void markTheSector(BitMap sectorMap,int sectorNumber,BitMap segmentManager){
		int segmentNumber = sectorNumber / NumberOfSectorsInASegment;
		int sectorOffset = sectorNumber - ((segmentNumber)*NumberOfSectorsInASegment);
		//int endOfSegment = NumberOfSectorsInASegment*segmentNumber;
		if(sectorOffset == NumberOfSectorsInASegment-2){
			segmentManager.mark(segmentNumber);
			
			//mSummary.updateSegmentNumber(segmentNumber+1);
			//TODO run cleaner 
			
		}
		//TODO  update the segment summary
		//Marks the sector map
		sectorMap.mark(sectorNumber);
		
		
		
	}
	public void getAndCleanThreeSegments(){
		boolean[] mInverse = new boolean[NumberOfSegments];
		mInverse = SegmentMapDS.getInverseOfBitMap();
		int[] segmentsToClean = new int[3];
		if(lastSegmentCleaned == -1 || lastSegmentCleaned==NumberOfSegments-1){
			segmentsToClean[0] = 0;
			segmentsToClean[1] = 1;
			segmentsToClean[2] = 2;
			
		}else{
			for(int i = lastSegmentCleaned+1;i<NumberOfSegments;i++){
				if(!mInverse[i]){
					if(i==NumberOfSegments-1){
						segmentsToClean[0] = i;
						segmentsToClean[1] = 0;
						segmentsToClean[2] = 1;
					}else if(i == NumberOfSegments - 2){
						segmentsToClean[0] = i;
						segmentsToClean[1] = i+1;
						segmentsToClean[2] = 0;
					}else{
						segmentsToClean[0] = i;
						segmentsToClean[1] = i+1;
						segmentsToClean[2] = i+2;
					}
					
				}
				break;
			}
			
		}
		lastSegmentCleaned = segmentsToClean[2];
		cleanThreeSegments(segmentsToClean);
		
		
		
	}
	
	public void cleanThreeSegments(int[] segments){
		HashMap<Integer, LiveBlockInformation> liveBlocks = new HashMap<Integer, LiveBlockInformation>();
		InodeMap directory;
		directory = new InodeMap(NumDirEntries);
		directory.fetchFrom(mInodeMap);
		System.out.println("Cleaning Segment from"+segments[0]+segments[1]+segments[2]);
		
		for(int i : segments){
			
			int start = -1;
			int end = -1;
			if(i==0){
				start = 3;
				end =55;
			}				
			else{
				start = (i)*NumberOfSectorsInASegment;
				end = start+55;
			}
			
			//change it to segmentSummarynumber of segment -1
			int segmentSummarySector = end+1;
			
			SegmentSummary summaryOfTheSegment = new SegmentSummary();
			summaryOfTheSegment.fetchFrom(segmentSummarySector);
			for(int q=0;q<56;q++){
				int sectorOffset = -1;
				if((i==0&&q==0)||(i==0&&q==1)||(i==0&&q==2))
					continue;
				else if(i==0)
					sectorOffset = q;
				else
					sectorOffset = start+q;
				String fileName = summaryOfTheSegment.getFileNameWhichUsesTheSector(q);
				//header files
				if(fileName.endsWith("_H")){
					
					int fileSectorNumber = directory.find(fileName);
					if (fileSectorNumber != -1) {
						if(q==fileSectorNumber){
							
							LiveBlockInformation tempOFInformation = new LiveBlockInformation();
							tempOFInformation.fileName = fileName;
							tempOFInformation.datablock = -1;
							tempOFInformation.segment = i;
							liveBlocks.put(sectorOffset, tempOFInformation);
						}
					}
					
				}else{
					
					
					
					int fileSectorNumber = directory.find(fileName);
					if(fileName.equals(DirectoryName)){
						fileSectorNumber = InodeMapSector;
					}else if(fileName.equals(FreeSectorMapName)){
						fileSectorNumber = FreeMapSector;
					}else if(fileName.equals(FreeSegmentMapName)){
						fileSectorNumber = freeSegments;
					}
					if (fileSectorNumber != -1) {
						// file is in directory					
						InodeNormal hdr = new InodeNormal(fileName);
						
						hdr.fetchFrom(fileSectorNumber);
						int temp = hdr.isThisSectorUsed(sectorOffset);
						
						if(temp!=-1){
							LiveBlockInformation tempOFInformation = new LiveBlockInformation();
							tempOFInformation.fileName = fileName;
							tempOFInformation.datablock = temp;
							tempOFInformation.segment = i;
							liveBlocks.put(sectorOffset, tempOFInformation);
							
						}
											
						
						
					} 
					
					
				}
				
	
				
				
				
				
				
			}
		
		}
		//create a buffer of the length of the live blocks
		int size = liveBlocks.size() * 128;
		byte[] buffer = new byte[size];
		int count = 0;
		for(int key:liveBlocks.keySet()){
			LiveBlockInformation tempOFInformation = liveBlocks.get(key);
			byte[] smallbuffer = new byte[128];
			JNachos.mSynchDisk.readSector(key, smallbuffer);
			System.arraycopy(smallbuffer, 0, buffer, 128*count, smallbuffer.length);
			int sector = findAFreeSector(tempOFInformation.fileName);
			JNachos.mSynchDisk.writeSector(sector, smallbuffer);
		}
		
		for(int i : segments){
			
			int start = -1;
			int end = -1;
			if(i==0){
				start = 3;
				end =55;
			}				
			else{
				start = (i)*NumberOfSectorsInASegment;
				end = start+55;
			}
			SegmentMapDS.clear(i);
			for(int j=start;j<end+8;j++){
				SectorMapDS.clear(j);
			}
		}
		
		
		//read everything from the live blocks
		//write it
		//clean the bits both in sector and segment
		//write back the correct sector and segments
		
	}
	public  int findAFreeSector(String fileName){
//		BitMap sectorMap = getSectorMap();		
//		BitMap segmentManager = getSegmentMap();
		BitMap sectorMap = SectorMapDS;
		int sectorNumber = SectorMapDS.find();
		//sectorMap.writeBack(mFreeMap); 
		int endOfSegment = -1;
		int sectorOffset = -1;
		//int segmentNumber = sectorNumber / (NumberOfSectorsInASegment);
		int segmentNumber = sectorNumber / (NumberOfSectorsInASegment);
		//fillOutSegmentDS();
		//int segmentNumber = SegmentMapDS.checkNextWhichIsFree();
		if(segmentNumber == 0){
			sectorOffset = sectorNumber;
			endOfSegment = sectorNumber;
//			sectorOffset = sectorNumber - ((segmentNumber)*NumberOfSectorsInASegment);
		}else
		{
			sectorOffset = sectorNumber - ((segmentNumber)*(NumberOfSectorsInASegment));
			endOfSegment = sectorOffset-9;
			
		}
		
		//int endOfSegment = NumberOfSectorsInASegment*segmentNumber;
		if(sectorOffset == NumberOfSectorsInASegment-9){
			//segmentManager.mark(segmentNumber);
			fillOutSegmentDS();
			//System.out.println(SegmentMapDS);
			SegmentMapDS.mark(segmentNumber);
			mSummary.writeInSegmentSummary(sectorOffset, fileName, segmentNumber);
			for(int i=0;i<8;i++){
				SectorMapDS.mark(sectorNumber+1+i);
			}
			JNachos.mFileSystem.mSummary.setNextSegmentNumber(SegmentMapDS.checkNextWhichIsFree());
			mSummary.writeIntoTheDisk(sectorNumber+1);
			
			//LastModifiedSegment = segmentNumber;
			
			
			JNachos.mFileSystem.mSummary.clear();		
			int NextSegment = SegmentMapDS.checkNextWhichIsFree();
			CurrentSegment = NextSegment;
			
			//(NumberOFSectorsInSegmentSummary*(segmentNumber+1))+NextSegment*8
			int SectorNumberNext = ((SegmentSummary.NumberOFSectorsInSegmentSummary*(NextSegment+1))+NextSegment*8) ;
			for(int i=0;i<8;i++){
				SectorMapDS.mark(SectorNumberNext+i);
			}
			mSummary.writeIntoTheDisk(SectorNumberNext);
		
			
			//segmentManager.writeBack(mFreeSegmentMap);
			//get the next Sector and mark it as its segment summary inode sector
			//clear the segment summary
			//mSummary.updateSegmentNumber(segmentNumber+1);
			//TODO run cleaner 
			if(SegmentMapDS.numClear()<=2){
				getAndCleanThreeSegments();
			}
			writeBackTheSegmentFile();
			writeBackTheFile();
			
		}else{
			mSummary.writeInSegmentSummary(sectorOffset, fileName, segmentNumber);
		}
		//TODO  update the segment summary
		//write it in the segment summary
		
		return sectorNumber;
	}

	

	@Override
	public boolean create(String pFileName, int pInitialSize) {
		// TODO Auto-generated method stub
		
		InodeMap directory;
		BitMap freeMap;
		BitMap segmentManager;
		InodeNormal hdr;
		int sector;
		boolean success;

		Debug.print('f', "Creating file " + pFileName + ", size: " + pInitialSize);
		//SectorMapDS.fetchFrom(mFreeMap);
		//SegmentMapDS.fetchFrom(mFreeSegmentMap);
		fillOutDS();

		//fetchFromThe fileThatIsAlreadyOpen
		directory = new InodeMap(NumDirEntries);
		directory.fetchFrom(mInodeMap);

		if (directory.find(pFileName) != -1) {
			// file is already in directory
			success = false;
		} else {
			//doubt this would be Disk.NumberOfSectors
			freeMap = new BitMap(Disk.NumSectors);
			freeMap.fetchFrom(mFreeMap);
			
			segmentManager = new BitMap(NumberOfSegments);
			segmentManager.fetchFrom(mFreeSegmentMap);

			
			// find a sector to hold the file header
			//sector = freeMap.find();
			//fillOutDS();
			sector = findAFreeSector(pFileName+"_H");

			if (sector < 0) {
				success = false; // no free block for file header
			} else if (!directory.add(pFileName, sector)) {
				success = false; // no space in directory
			} else {
				hdr = new InodeNormal(pFileName);
				if (!hdr.allocate(freeMap, pInitialSize,segmentManager)) {
					success = false; // no space on disk for data
				} else {
					success = true;
					// everthing worked, flush all changes back to disk
					hdr.writeBack(sector);
					directory.writeBack(mInodeMap);//redundant
					//freeMap.writeBack(mFreeMap);//redundant
					
					writeBackTheFile();
					Debug.print('f', "File created succesffully " + sector + "\t" + mInodeMap);
				}
				hdr.delete();
			}
			freeMap.delete();
		}
		directory.delete();
		//SectorMapDS.writeBack(mFreeMap);
		//SegmentMapDS.writeBack(mFreeSegmentMap);
		JNachos.mFileSystem.mSummary.writeItself();
		return success;
		
	}
	public  BitMap getSectorMap(){
		
		BitMap freeMap = new BitMap(Disk.NumSectors);
		freeMap.fetchFrom(mFreeMap);
		return freeMap;
	}
	public  BitMap getSegmentMap(){
		
		BitMap segmentManager = new BitMap(NumberOfSegments);
		segmentManager.fetchFrom(mFreeSegmentMap);
		return segmentManager;
	}
	
	public void changeInodeMapAndWriteItBack(char[] fileName, int newSector){
		//should check using name
		String newName = new String(fileName);
		if(!(newName.equalsIgnoreCase(DirectoryName) || newName.equalsIgnoreCase(FreeSectorMapName)  || newName.equalsIgnoreCase(FreeSegmentMapName))){
			InodeMap directory = new InodeMap(NumDirEntries);
			directory.fetchFrom(mInodeMap);
			directory.update(newName, newSector);
			directory.writeBack(mInodeMap);	
		}
		
		
		
		
	}

	@Override
	public int create_FD(String pName, int initialSize) {
		// TODO Auto-generated method stub
		fillOutDS();
		create(pName, initialSize);
		int sector;
		InodeMap directory = new InodeMap(NumDirEntries);
		LogOpenFile openFile = null;
		sector = directory.find(pName);
		if (sector >= 0) {
			// name was found in directory
			openFile = new LogOpenFile(sector,pName);
		}
		writeBackTheFile();
		return sector;
	}

	public  void putContentsIntoFile(String pFileName,byte[] contentsOFTheFile){
		InodeMap directory = new InodeMap(NumDirEntries);
		LogOpenFile openFile = null;
		int sector;
		fillOutDS();
		Debug.print('f', "Opening file " + pFileName);
		directory.fetchFrom(mInodeMap);

		sector = directory.find(pFileName);

		if (sector >= 0) {
			// name was found in directory
			openFile = new LogOpenFile(sector,pFileName);
		}
		
		openFile.writeAt(contentsOFTheFile, contentsOFTheFile.length, 0);
		writeBackTheFile();
		
	}
	@Override
	public OpenFile open(String pFileName) {
		// TODO Auto-generated method stub
		InodeMap directory = new InodeMap(NumDirEntries);
		LogOpenFile openFile = null;
		int sector;
		
		Debug.print('f', "Opening file " + pFileName);
		directory.fetchFrom(mInodeMap);

		sector = directory.find(pFileName);

		if (sector >= 0) {
			// name was found in directory
			openFile = new LogOpenFile(sector,pFileName);
		}

		directory.delete();

		// return null if not found
		return openFile;
		
	}

	@Override
	public boolean remove(String pFileName) {
		// TODO Auto-generated method stub
		InodeMap directory;
		BitMap freeMap;
		BitMap segmentManager;
		InodeNormal fileHdr;
		int sector;

		directory = new InodeMap(NumDirEntries);
		//directory.fetchFrom(mDirectoryFile);mInodeMap
		directory.fetchFrom(mInodeMap);
		sector = directory.find(pFileName);

		if (sector == -1) {
			directory.delete();
			return false; // file not found
		}
		fileHdr = new InodeNormal(pFileName);
		fileHdr.fetchFrom(sector);

		freeMap = new BitMap(NumberOfSegments);
		freeMap.fetchFrom(mFreeMap);
		
		segmentManager = new BitMap(NumberOfSegments);
		segmentManager.fetchFrom(mFreeSegmentMap);
		//TODO look at deallocate and clear
		//fileHdr.deallocate(freeMap); // remove data blocks
		//freeMap.clear(sector); // remove header block
		directory.remove(pFileName);

		
		freeMap.writeBack(mFreeMap);
		directory.writeBack(mInodeMap);
		fileHdr.delete();
		directory.delete();
		freeMap.delete();
		return true;
		
	
	}

	@Override
	public void list() {
		// TODO Auto-generated method stub
		
		InodeMap directory = new InodeMap(NumDirEntries);
		//directory.fetchFrom(mDirectoryFile);mInodeMap
		directory.list();
		directory.delete();
		
		
	}

	@Override
	public void print() {
		// TODO Auto-generated method stub
		InodeNormal bitHdr = new InodeNormal(FreeSectorMapName);
		InodeNormal dirHdr = new InodeNormal(DirectoryName);
		InodeNormal segmentHdr = new InodeNormal(FreeSegmentMapName);
		BitMap freeMap = new BitMap(NumberOfSegments);
		InodeMap directory = new InodeMap(NumDirEntries);
		
		BitMap segmentManager = new BitMap(NumberOfSegments);
		

		System.out.println("Bit map file header:\n");

		bitHdr.fetchFrom(FreeMapSector);
		bitHdr.print();

		System.out.println("Directory file header:\n");
		dirHdr.fetchFrom(InodeMapSector);
		dirHdr.print();
		
		System.out.println("Segment map file header:\n");
		segmentHdr.fetchFrom(freeSegments);
		segmentHdr.print();

		freeMap.fetchFrom(mFreeMap);
		freeMap.print();

		directory.fetchFrom(mInodeMap);
		directory.print();
		
		segmentManager.fetchFrom(mFreeSegmentMap);
		segmentManager.print();

		bitHdr.delete();
		dirHdr.delete();
		freeMap.delete();
		directory.delete();
		
	}
	
	
	
	public static int getFirstFreeSector(int segment){
		return segment*NumberOfSectorsInASegment;
	}

}
