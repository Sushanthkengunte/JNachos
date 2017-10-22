/**
 * 
 */
package jnachos.kern.sync;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import jnachos.kern.JNachos;
import jnachos.kern.NachosProcess;
import jnachos.kern.VoidFunctionPtr;
import jnachos.kern.sync.Water.HAtom;
import jnachos.kern.sync.Water.OAtom;

/**
 * @author sushanth
 *
 */
public class peroxide {
	/** Semaphore H */
	static Semaphore H = new Semaphore("SemH", 0);

	/**	*/
	static Semaphore O = new Semaphore("SemO", 0);
	static Semaphore hydrogenFinished = new Semaphore("FinishedHydrogen", 0);

	/**	*/
	static Semaphore wait = new Semaphore("wait", 0);
	
	static Semaphore waitO = new Semaphore("wait", 0);

	/**	*/
	static Semaphore mutex = new Semaphore("MUTEX", 1);

	/**	*/
	static Semaphore mutex1 = new Semaphore("MUTEX1", 1);

	/** */
	static Semaphore mutex2 = new Semaphore("MUTEX2", 1);
	/**	*/
	static long countH = 0;
	
	static long countO = 0;

	/**	*/
	static int Hcount, Ocount, nH, nO;

	/**	*/
	class HAtom implements VoidFunctionPtr {
		int mID;

		/**
		 *
		 */
		public HAtom(int id) {
			mID = id;
		}

		/**
		 * oAtom will call oReady. When this atom is used, do continuous
		 * "Yielding" - preserving resource
		 */
		public void call(Object pDummy) {
			mutex.P();
			if (countH % 2 == 0) // first H atom
			{
				countH++; // increment counter for the first H
				mutex.V(); // Critical section ended
				H.P(); // Waiting for the second H atom
			} else // second H atom
			{
				countH++; // increment count for next first H
				mutex.V(); // Critical section ended
				H.V(); // wake up the first H atom
				//O.V(); // wake up O atom
				hydrogenFinished.V();
			}

			wait.P(); // wait for water message done

			System.out.println("H atom #" + mID + " used in making water.");
		}
	}

	/**	*/
	class OAtom implements VoidFunctionPtr {
		int mID;

		/**
		 * oAtom will call oReady. When this atom is used, do continuous
		 * "Yielding" - preserving resource
		 */
		public OAtom(int id) {
			mID = id;
		}

		/**
		 * oAtom will call oReady. When this atom is used, do continuous
		 * "Yielding" - preserving resource
		 */
		public void call(Object pDummy) {
			
			
			
			mutex1.P();
			if (countO % 2 == 0) // first H atom
			{
				countO++; // increment counter for the first O
				mutex1.V(); // Critical section ended
				O.P(); // Waiting for the second O atom
			} else // second O atom
			{
				hydrogenFinished.P();
				countO++; // increment count for next first O
				mutex1.V(); // Critical section ended
				O.V(); // wake up the first O atom
				//H.V(); // wake up O atom
				makePeroxide();
				wait.V(); // wake up H atoms and they will return to
				wait.V();
				
				mutex2.P();
				Hcount = Hcount - 2;
				Ocount = Ocount - 2;
				System.out.println("Numbers Left: H Atoms: " + Hcount + ", O Atoms: " + Ocount);
				System.out.println("Numbers Used: H Atoms: " + (nH - Hcount) + ", O Atoms: " + (nO - Ocount));
				
				mutex2.V();
				
			}
			//waitO.P();
			System.out.println("O atom #" + mID + " used in making peroxide.");

		}
	}

	/**
	 * oAtom will call oReady. When this atom is used, do continuous "Yielding"
	 * - preserving resource
	 */
	public static void makePeroxide() {
		System.out.println("** peroxide made!! **");
	}

	/**
	 * oAtom will call oReady. When this atom is used, do continuous "Yielding"
	 * - preserving resource
	 */
	public peroxide() {
		runPeroxide();
	}

	/**
	 *
	 */
	public void runPeroxide() {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Number of H atoms ? ");
			nH = (new Integer(reader.readLine())).intValue();
			System.out.println("Number of O atoms ? ");
			nO = (new Integer(reader.readLine())).intValue();
		} catch (Exception e) {
			e.printStackTrace();
		}

		Hcount = nH;
		Ocount = nO;

		for (int i = 0; i < nH; i++) {
			HAtom atom = new HAtom(i);
			(new NachosProcess(new String("hAtom" + i))).fork(atom, null);
		}

		for (int j = 0; j < nO; j++) {
			OAtom atom = new OAtom(j);
			(new NachosProcess(new String("oAtom" + j))).fork(atom, null);
		}
	}

}
