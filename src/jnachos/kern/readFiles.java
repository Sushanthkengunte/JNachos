package jnachos.kern;

import jnachos.filesystem.OpenFile;
import jnachos.filesystem.SegmentSummary;
import jnachos.machine.JavaSys;

public class readFiles implements VoidFunctionPtr {
	public void call(Object pArg) {
		int[] testing = new int[500];
		byte[] buffer = new byte[500*4];

		
//		for(int i=0;i<500;i++){
//			JavaSys.intToBytes(i, buffer, i*4);
//		}
		JNachos.mFileSystem.create("explain", buffer.length+1);
		//JNachos.mFileSystem.print();
		//JNachos.mFileSystem.remove("testing2");
		//JNachos.mFileSystem.create("testing2", buffer.length+1);
		OpenFile t = JNachos.mFileSystem.open("explain");
		//t.readAt(buffer, buffer.length,0);
		for(int i=0;i<500;i++){
		testing[i] = JavaSys.bytesToInt(buffer, i*4);
			
		}
		
//		JNachos.mFileSystem.create("test21", 40);
//		OpenFile t1 = JNachos.mFileSystem.open("test21");
		int[] testing1 = new int[500];
		byte[] buffer1 = new byte[500*4];
		for(int i=0;i<500;i++){
			testing1[i] = i;
			
		}		
		for(int i=0;i<500;i++){
			JavaSys.intToBytes(i, buffer1, i*4);
		}
		JNachos.mFileSystem.fillOutDS();
		for(int k=0;k<13;k++){
			//System.out.println("numberoftimes"+k);
			t.writeAt(buffer1, buffer1.length,0);
			
			JNachos.mFileSystem.writeBackTheFile();
			
			t.readAt(buffer, buffer1.length, 0);
			int[] xof = new int[500];
			for(int i=0;i<500;i++){
				//System.out.println("explain");
				xof[i] = JavaSys.bytesToInt(buffer, i*4);
				//System.out.println(xof[i]);
			}
			
		}
		
		
		
		//System.out.println(JNachos.mFileSystem.mSummary);
		 //JNachos.mFileSystem.mSummary.fetchFrom(56);
		
	}

	public readFiles() {
		
		NachosProcess p = new NachosProcess("forked process");
		
		p.fork(this, null);
	}
}
