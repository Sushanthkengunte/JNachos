/**
 * Copyright (c) 1992-1993 The Regents of the University of California.
 * All rights reserved.  See copyright.h for copyright notice and limitation 
 * of liability and disclaimer of warranty provisions.
 *  
 *  Created by Patrick McSweeney on 12/5/08.
 */
package jnachos.kern;

import jnachos.machine.Machine;

/**
 * Set up a ping-pong between two threads, by forking a thread to 'call'.
 **/
public class ProcessTest implements VoidFunctionPtr {
	/**
	 * The body for processes to run
	 * 
	 * @param pArg
	 *            is an Integer with an id.
	 */
	public void call(Object pArg) {
		// which is the processes local id
		
		//System.out.println(JNachos.getCurrentProcess().getProcessID());
		if(pArg == "fork"){
			NachosProcess tmp = JNachos.getCurrentProcess();
			new AddrSpace(JNachos.getCurrentProcess().getSpace());
			JNachos.getCurrentProcess().restoreUserState();
			//System.out.println("Join process with ID "+Machine.mRegisters[4]);
			Machine.run();
		
			
		}/*else if(pArg == "parentRun"){
			new AddrSpace(JNachos.getCurrentProcess().getSpace());
			JNachos.getCurrentProcess().restoreUserState();
			int temp = Machine.readRegister(4);
			Machine.run();
		}
		else{*/
			String fileName = ((String) pArg).toString();
			JNachos.startProcess(fileName);
			
		//}
		
			
		
	}
	
	public ProcessTest(){
		
	}
	/**
	 * ThreadTest Set up a ping-pong between two threads, by forking a thread to
	 * call SimpleThread, and then calling SimpleThread ourselves.
	 **/
	public ProcessTest(String fileNames) {
		
		Debug.print('t', "Entering SimpleTest");
		String[] twoFilePath = fileNames.split(",");
		for(int j = 0;j<twoFilePath.length;j++){
			//System.out.println(twoFilePath[j]+twoFilePath.length);
			NachosProcess p = new NachosProcess("forked process" + j);
			
			p.fork(this, twoFilePath[j]);
		}
		
	}
	/*public int putInNewThread(){
		int childProcessID;
		NachosProcess childProcess = new NachosProcess("Creating child process");
		childProcessID = childProcess.getProcessID();
		childProcess.setSpace(JNachos.getCurrentProcess().getSpace());
		//Machine.mRegisters[Machine.PCReg] =  Machine.mRegisters[Machine.PCReg] + 4;
		Machine.increaseTheProgramCounter();
		
		
		//Machine.mRegisters[Machine.NextPCReg] =  Machine.mRegisters[Machine.PCReg] + 4;
		//Machine.mRegisters[2] = 0;
		childProcess.saveUserState();
		childProcess.setSpecificRegister(2, 0);
		
		//childProcess.se
		childProcess.fork(this, "fork");
		return childProcessID;
	}
	public void runParent(NachosProcess parent){
		parent.runTheResumedProcess(this, "parentRun");
		
	}*/
}
