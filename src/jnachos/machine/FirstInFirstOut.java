package jnachos.machine;

public class FirstInFirstOut {
	
	private static int currentPhysicalPage = 0;
	
	public static void SetFifoCounter(int phy) {
		currentPhysicalPage = phy;
	}
	
	public static int noSpace() {
		
		currentPhysicalPage++;
		int phyPageToSwap = currentPhysicalPage % Machine.NumPhysPages;
		
		return phyPageToSwap;
	}

}
