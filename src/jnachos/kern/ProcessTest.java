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
		
		
		
		if(pArg == "fork"){
			
			JNachos.getCurrentProcess().restoreUserState();
			JNachos.getCurrentProcess().getSpace().restoreState();
			
			Machine.run();
		
			
		}
			String fileName = ((String) pArg).toString();
			JNachos.startProcess(fileName);
			
		
		
			
		
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
			
			NachosProcess p = new NachosProcess("forked process" + j);
			
			p.fork(this, twoFilePath[j]);
		}
		
	}
	
}
