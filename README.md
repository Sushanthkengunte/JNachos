"# JNachos" 

Implementation:

•	Building on a fundamental operating system to expand its capabilities by implementing kernel mechanisms and high-level policies and incorporating a Log-Structure File System to reduce the seek time for small files.

•	My Responsibilities:

o	Kernel mechanisms such as

  	 fork 
  
  	exec 
  
  	exit 
  
  
o	Memory Virtualization 

  •	A swap space was created which imported all pages of the current process.
  
  •	Least Recently Used algorithm was followed to evict a page from RAM when a page fault was encountered ensuring the availability of       the required page in RAM.
  
  
o	Log Structure File System:

  •	Implementing FileSystem interface with functions:

    1.	Create
    
    2.	Open
    
  •	Implementing OpenFile interface with functions:
  
    1.	Read
    
    2.	Write
    
    
    
  •	Models used to build the file system:
  
  o	InodeMap
  
  o	InodeMapValue
  
  o	BitMap (SectorMap, SegmentMap) 
  
  o	InodeNormal
  
  o	LogOpenFile
  
  o	SegmentSummary
  
  o	SegmentSummaryValue
  
  o	LogStructureFileSystem
  
  
  •	Relationships

    o	InodeMap has InodeMapValue
    
    o	SegmentSummary has SegmentSummaryValue
    
    o	LogStructureFileSystem has InodeMap, BitMap, SegmentSummary.



Model:


1)	InodeMapValue:

      InUse: Boolean variable
      
      sectorNumberOfFile
      
      FileName
      
      
2)	InodeMap:

      Array of InodeMapValue
      
     fetchFromTheFile(OpenFile)
     
     writeBackIntoTheFile(OpenFile)
     
     findTheFileWithName()
     
     addFileIntoDirectory()
     
     
     
3)	BitMap:

     numClearNits
     
     array of Boolean values 
     
     clear()
     
     writeback(OpenFile)
     
     fetchFrom(OpenFile)
     
     mark(int)
     
     getNextWhichIsFree()
     
     inverse()
     
     
     
4)	InodeNormal:

     numBytes
     
     numSectors
     
     array of direct pointers (mDataSectors)
     
     filename
     
     allocate(SectorMap,fileSize,SegmentMap)
     
     fetchFrom(int sector)
     
     writeback(int sector)
     
     
5)	SegmentSummaryValue:

      fileNames
      
      
6)	Segment Summary:

     //last 8 sectors of each segment tracking which sector is used by which file.
     
    Array of SegmentSummaryValue
    
    
7)	LogOpenFile : Implementation of OpenFile

    fileHeader
    
    mSeekPosition
    
    writeIntoDisk
    
    ReadFromDisk
    
      
8)	LogStructureFileSystem

    findFreeSector()
    
    getCleanThreeSegments()
    
    cleanSegments(int[])
    
    writebackIntoSegmentFile()
    
    writebackIntoSectorFile()
    
    fillOutSegmentDS()
    
    fillOutSectorDS()
    
    Create()
    
    Open()
