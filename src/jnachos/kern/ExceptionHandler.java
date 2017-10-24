/**
 * Copyright (c) 1992-1993 The Regents of the University of California.
 * All rights reserved.  See copyright.h for copyright notice and limitation 
 * of liability and disclaimer of warranty provisions.
 *
 *  Created by Patrick McSweeney on 12/13/08.
 *
 */
package jnachos.kern;

import java.util.Arrays;

import javax.crypto.Mac;

import jnachos.filesystem.JavaOpenFile;
import jnachos.machine.*;

/**
 * The ExceptionHanlder class handles all exceptions raised by the simulated
 * machine. This class is abstract and should not be instantiated.
 */
public abstract class ExceptionHandler {

	/**
	 * This class does all of the work for handling exceptions raised by the
	 * simulated machine. This is the only funciton in this class.
	 *
	 * @param pException
	 *            The type of exception that was raised.
	 * @see ExceptionType.java
	 */
	public static void handleException(ExceptionType pException) {
		switch (pException) {
		// If this type was a system call
		case SyscallException:
			// Get what type of system call was made
			int type = Machine.readRegister(2);
			// Invoke the System call handler
			SystemCallHandler.handleSystemCall(type);
			break;
		case PageFaultException:
			System.out.println("Page fault happened");
			System.out.println("With process ID = " + JNachos.getCurrentProcess().getProcessID());
			int pid = JNachos.getCurrentProcess().getProcessID();
			SwapSpace temp = Machine.swapSpaceMap.get(pid);
			int physicalPage = AddrSpace.mFreeMap.find();
			int arg = Machine.readRegister(39);
			int vpn = (int) arg/Machine.PageSize;
			//System.out.println(temp.getSwapTable().get(vpn).physicalPage);
			
			
			
			if(physicalPage!=-1){
				//if there is a free page in physical memory
				JNachos.getCurrentProcess().getSpace().updateTable(physicalPage, vpn);
				
				int swapSpaceSeekValue = temp.getSwapTable().get(vpn).physicalPage;
				String fileName = temp.swapFileName;
				JavaOpenFile openedFile = (JavaOpenFile) JNachos.mFileSystem.open(fileName);
				byte[] bytes = new byte[Machine.PageSize];
				openedFile.readAt(bytes, Machine.PageSize,swapSpaceSeekValue);
				Arrays.fill(Machine.mMainMemory, physicalPage * Machine.PageSize,
						(physicalPage + 1) * Machine.PageSize, (byte) 0);
				System.arraycopy(bytes, 0, Machine.mMainMemory, physicalPage * Machine.PageSize,
						Machine.PageSize);
				
				Machine.invertedTable.put(physicalPage, JNachos.getCurrentProcess().getProcessID());
				
				///
				FirstInFirstOut.SetFifoCounter(physicalPage);
				///
				
				for(int k : Machine.invertedTable.keySet()){
					System.out.println(k + "  " + Machine.invertedTable.get(k));
				}
				
			}else{
				System.out.println("No more space");
				
				////FIFO implementation 
				
				int physicalToSwap = FirstInFirstOut.noSpace();
				int pidToSwap = Machine.invertedTable.get(physicalToSwap);
				
				NachosProcess tempProcess = Machine.hmForAllProcess.get(pidToSwap);
				tempProcess.getSpace().removePage(physicalToSwap);
				
				physicalPage = physicalToSwap;
				
				JNachos.getCurrentProcess().getSpace().updateTable(physicalPage, vpn);
				
				int swapSpaceSeekValue = temp.getSwapTable().get(vpn).physicalPage;
				String fileName = temp.swapFileName;
				JavaOpenFile openedFile = (JavaOpenFile) JNachos.mFileSystem.open(fileName);
				byte[] bytes = new byte[Machine.PageSize];
				
				openedFile.readAt(bytes, Machine.PageSize,swapSpaceSeekValue);
				Arrays.fill(Machine.mMainMemory, physicalPage * Machine.PageSize,
						(physicalPage + 1) * Machine.PageSize, (byte) 0);
				System.arraycopy(bytes, 0, Machine.mMainMemory, physicalPage * Machine.PageSize,
						Machine.PageSize);
				
				Machine.invertedTable.put(physicalPage, JNachos.getCurrentProcess().getProcessID());
				
				///
				FirstInFirstOut.SetFifoCounter(physicalPage);
				///
				
				
				////
				
				
				//if there is no free page in physical memory,implement page replacement algorithm
			}
			/*for(int k: temp.getSwapTable().keySet())
			{
				System.out.println(k);
				System.out.println(temp.getSwapTable().get(k).physicalPage);
			}*/
				
			
			//temp.pageRead();
			
			
			break;

		// All other exceptions shut down for now
		default:
			System.exit(0);
		}
	}
}
