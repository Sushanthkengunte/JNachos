package jnachos.kern;

import jnachos.filesystem.OpenFile;
import jnachos.machine.JavaSys;

public class createFileInLogFS implements VoidFunctionPtr{
	public void call(Object pArg) {
		
		int[] testing = new int[500];
		byte[] buffer = new byte[500*4];
		for(int i=0;i<500;i++){
			testing[i] = i;
			
		}
		
		for(int i=0;i<500;i++){
			JavaSys.intToBytes(i, buffer, i*4);
		}
		JNachos.mFileSystem.create("test1", buffer.length+1);
		OpenFile t = JNachos.mFileSystem.open("test1");
		t.writeAt(buffer, buffer.length,0);
		
//		JNachos.mFileSystem.create("test21", 40);
//		OpenFile t1 = JNachos.mFileSystem.open("test21");
//		int[] testing1 = new int[500];
		byte[] buffer1 = new byte[500*4];
//		for(int i=0;i<500;i++){
//			testing1[i] = i;
//			
//		}
//		
		t.readAt(buffer, buffer1.length, 0);
		int[] xof = new int[500];
		for(int i=0;i<500;i++){
			System.out.println("explain");
			xof[i] = JavaSys.bytesToInt(buffer, i*4);
			System.out.println(xof[i]);
		}
	}
	
		public createFileInLogFS() {
		
			NachosProcess p = new NachosProcess("forked process");
			
			p.fork(this, null);
		}
}
