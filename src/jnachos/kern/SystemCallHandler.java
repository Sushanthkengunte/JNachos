/**
 * Copyright (c) 1992-1993 The Regents of the University of California.
 * All rights reserved.  See copyright.h for copyright notice and limitation 
 * of liability and disclaimer of warranty provisions.
 *  
 *  Created by Patrick McSweeney on 12/5/08.
 */
package jnachos.kern;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Mac;

import jnachos.filesystem.JavaFileSystem;
import jnachos.filesystem.OpenFile;
import jnachos.machine.*;

/** The class handles System calls made from user programs. */
public class SystemCallHandler {
	/** The System call index for halting. */
	public static final int SC_Halt = 0;

	/** The System call index for exiting a program. */
	public static final int SC_Exit = 1;

	/** The System call index for executing program. */
	public static final int SC_Exec = 2;

	/** The System call index for joining with a process. */
	public static final int SC_Join = 3;

	/** The System call index for creating a file. */
	public static final int SC_Create = 4;

	/** The System call index for opening a file. */
	public static final int SC_Open = 5;

	/** The System call index for reading a file. */
	public static final int SC_Read = 6;

	/** The System call index for writting a file. */
	public static final int SC_Write = 7;

	/** The System call index for closing a file. */
	public static final int SC_Close = 8;

	/** The System call index for forking a forking a new process. */
	public static final int SC_Fork = 9;

	/** The System call index for yielding a program. */
	public static final int SC_Yield = 10;

	/**
	 * Entry point into the Nachos kernel. Called when a user program is
	 * executing, and either does a syscall, or generates an addressing or
	 * arithmetic exception.
	 * 
	 * For system calls, the following is the calling convention:
	 * 
	 * system call code -- r2 arg1 -- r4 arg2 -- r5 arg3 -- r6 arg4 -- r7
	 * 
	 * The result of the system call, if any, must be put back into r2.
	 * 
	 * And don't forget to increment the pc before returning. (Or else you'll
	 * loop making the same system call forever!
	 * 
	 * @pWhich is the kind of exception. The list of possible exceptions are in
	 *         Machine.java
	 **/
	public static void handleSystemCall(int pWhichSysCall) {

		System.out.println("Process ID : "+JNachos.getCurrentProcess().getProcessID()+" System call : " + pWhichSysCall +"\n");
		Debug.print('a', "!!!!" + Machine.read1 + "," + Machine.read2 + "," + Machine.read4 + "," + Machine.write1 + ","
				+ Machine.write2 + "," + Machine.write4);

		// Increment the Program counter when a system call happens 
		Machine.mRegisters[Machine.PCReg] =  Machine.mRegisters[Machine.PCReg] + 4;
		
		switch (pWhichSysCall) {
		// If halt is received shut down
		case SC_Halt:
			Debug.print('a', "Shutdown, initiated by user program.");
			System.out.println("Arguments Passed are "+Machine.mRegisters[2]+ " "+Machine.mRegisters[4]+" "+Machine.mRegisters[5]+" "+Machine.mRegisters[6]+" "+Machine.mRegisters[7] );
			Interrupt.halt();
			break;

		case SC_Exit:
			// Read in any arguments from the 4th register
			int arg = Machine.readRegister(4);

			System.out
					.println("Current Process " + JNachos.getCurrentProcess().getName() + " exiting with code " + arg);
			
			//JNachos.getCurrentProcess().finish();
			
			NachosProcess processWaiting = JNachos.getCurrentProcess().getWaitingProcess();
			
			if( processWaiting != null){
				
				NachosProcess processSleeping = Machine.hmForAllProcess.get(processWaiting.getProcessID());
				
				JNachos.getCurrentProcess().setWaitingProcess(null);				
				processSleeping.setSpecificRegister(2, arg);
				
				
				Scheduler.readyToRun(processSleeping);
				 
				JNachos.getCurrentProcess().finish();
				
				
				
			}else{
				JNachos.getCurrentProcess().finish();
			}
			
			
			
			break;
		case SC_Fork:
			Debug.print('a', "Create new process.");
			// Forking new process
			int returnValue = JNachos.getCurrentProcess().fork_System_Call();
			Machine.writeRegister(2, returnValue);
			
			Machine.hmForAllProcess.put(JNachos.getCurrentProcess().getProcessID(), JNachos.getCurrentProcess());
			break;
			
			
		case SC_Exec:
			Debug.print('a', "Execute the program");
			
			//System.out.println("Arguments Passed are "+Machine.mRegisters[2]+ " "+Machine.mRegisters[4]+" "+Machine.mRegisters[5]+" "+Machine.mRegisters[6]+" "+Machine.mRegisters[7] );
 		
			//pointer to the starting of the file name
 			int offset = Machine.readRegister(4);
 			
 			ArrayList<Character> sample = new ArrayList<Character>();
 			while(Machine.mMainMemory[offset] != '\0'){
 				
 				sample.add((char)Machine.mMainMemory[offset]);
 				
 				offset = offset + 1;
 			}
 			
 			String fileName = new String();
 			for(char each : sample){
 				fileName = fileName+each;
 			}
 			System.out.println("Executing file : " + fileName);
 			//Opens the files copies address space and executes
 			JNachos.startExecuting(fileName);
 		
			System.out.println("Execute ");
			break;

		case SC_Join:
		
			
			int processToJoin = Machine.readRegister(4);
			if(JNachos.getCurrentProcess().getProcessID() == processToJoin || !Machine.hmForAllProcess.containsKey(processToJoin)){
				break;
			}else
				{
					NachosProcess processToBeJoined = Machine.hmForAllProcess.get(processToJoin);
					
					JNachos.getCurrentProcess().saveUserState();//take out this
					processToBeJoined.setWaitingProcess(JNachos.getCurrentProcess());
					JNachos.getCurrentProcess().sleep();
					
				}
			// Finish the invoking process
			break;
			
		
		default:
			Interrupt.halt();
			break;
		}
	}
}
