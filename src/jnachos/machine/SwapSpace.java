package jnachos.machine;


import java.util.Arrays;
import java.util.HashMap;

import jnachos.filesystem.BitMap;
import jnachos.filesystem.JavaFileSystem;
import jnachos.filesystem.JavaOpenFile;
import jnachos.filesystem.OpenFile;
import jnachos.kern.Debug;
import jnachos.kern.JNachos;
import jnachos.userbin.NoffHeader;



public class SwapSpace extends JavaOpenFile{
	
	//provides the translation from vpn to the lseek position in the file
	private HashMap<Integer,TranslationEntry> swapTable=new HashMap<Integer,TranslationEntry>();
	//number of physical pages
	
	public HashMap<Integer, TranslationEntry> getSwapTable() {
		return swapTable;
	}
	private int fileDescriptor;
	
	public String swapFileName;
	private static final int PageSize = 128;
	
	private static  int NumberPage;
	
	private int lseek = 0;
	//private JavaOpenFile fileAccess;
	
	
	
/***
 * Constructor,create a file and save its fileDescriptor,for mapping save the seek position of each page frame as seek address 
 */
	public SwapSpace(int numPages) {
		// TODO Auto-generated constructor stub
		//super(creatingSwapFile(numPages));
		super();
		int fd = creatingSwapFile(numPages);
		setmFile(fd);
		setCurrentOffset(0);
		//fileAccess = new JavaOpenFile();	
		NumberPage = numPages;
		
	}
	private int creatingSwapFile(int numPages){
		
		int size = numPages*PageSize;		
		swapFileName = "Process"+JNachos.getCurrentProcess().getProcessID();
		fileDescriptor = JNachos.mFileSystem.create_FD(swapFileName, size);		
		return fileDescriptor;
	}

	
	public  void putAllPagesinFile(NoffHeader noffH,OpenFile excutable) {
		
		//mPageTable = new TranslationEntry[NumberPage];
		for (int i = 0; i < NumberPage; i++) {
			TranslationEntry eachEntry = new TranslationEntry();
			eachEntry.virtualPage = i;
			
			eachEntry.valid = true;
			eachEntry.use = false;
			eachEntry.dirty = false;

			// if the code segment was entirely on
			eachEntry.readOnly = false;
			// a separate page, we could set its
			// pages to be read-only

			/* Zero out all of main memory
			Arrays.fill(Machine.mMainMemory, mPageTable[i].physicalPage * Machine.PageSize,
					(mPageTable[i].physicalPage + 1) * Machine.PageSize, (byte) 0);*/

			// Copy the code segment into memory
			if ((i * Machine.PageSize) < (noffH.code.size + noffH.initData.size)) {
				Debug.print('a',
						"Initializing code segment, at " + noffH.code.virtualAddr + ", size " + noffH.code.size);

				// Create a temporary buffer to copy the code
				byte[] bytes = new byte[Machine.PageSize];

				// read the code into the buffer
				//readAt(bytes, Machine.PageSize, noffH.code.inFileAddr + i * Machine.PageSize);
				excutable.readAt(bytes, Machine.PageSize, noffH.code.inFileAddr + i * Machine.PageSize);

				// Copy the buffer into the main memory
				//System.arraycopy(bytes, 0, Machine.mMainMemory, mPageTable[i].physicalPage * Machine.PageSize,
						//Machine.PageSize);
				
				JavaOpenFile fileName = (JavaOpenFile) JNachos.mFileSystem.open(swapFileName);
				assert(fileName!=null);
				
				int offset = fileName.writeAt(bytes, Machine.PageSize,lseek);//, lseek
				//incrementCurrent(offset);
				eachEntry.physicalPage = lseek;
				lseek += offset;
				swapTable.put(i, eachEntry);
				
			}else{
				eachEntry.physicalPage = lseek;
				swapTable.put(i, eachEntry);
				
			}
		}
		
	}
	
	public void pageRead(){
		JavaOpenFile fileName = (JavaOpenFile) JNachos.mFileSystem.open(swapFileName);
		byte[] bytes = new byte[Machine.PageSize];
		int  amountRead = 0;
		while ((amountRead = fileName.read(bytes, Machine.PageSize))>0) {
			String s = new String(bytes);
			System.out.println(s);
			
		}
	}
	
	

}
