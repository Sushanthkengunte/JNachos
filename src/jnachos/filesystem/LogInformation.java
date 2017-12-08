package jnachos.filesystem;

public class LogInformation {
	
	public static int recentSegmentUsed = -1;
	private static int nextFreeSegment = 0;
	// set to first when first written and changed when there is a clean up
	public static int nextFilledSegment = -1;
	// -1 because one segment will have checkpoint region
	public static int totalNumberSegments = LogStructureFS.NumberOfSegments -1;
	
	
	public static int numOfSegments(){
		
		
		
		
		
		
		return 0;
	}
	
	public static int numberOfFreeSegments(){
		
		
		
		if(recentSegmentUsed>nextFreeSegment && nextFilledSegment < recentSegmentUsed){
			//when last part and the first part of the disk is empty
			//last part plus first part is free
			return (totalNumberSegments - recentSegmentUsed)+(nextFilledSegment - 0);
			
			
			
			
		}else if(recentSegmentUsed<nextFreeSegment && nextFilledSegment == 0){
			//when its filled completely from the start
			//last part is free
			return totalNumberSegments - nextFreeSegment;
			
			
			
			
		}else if(recentSegmentUsed<nextFreeSegment && nextFilledSegment>nextFreeSegment){
			//when the middle portion is free
			return nextFilledSegment - nextFreeSegment;
			
			
		}else if(recentSegmentUsed>nextFreeSegment && nextFilledSegment > 0 && nextFreeSegment == 0){
			// when first part is free
			return nextFilledSegment;
			
			
			
		}
		return -1;
		

	}
	public static int[] getNextFreeSegment(int numberOfSegments){
		//Check for if the segment is free is not coded
		int[] temp = new int[numberOfSegments];
		
		if(numberOfSegments>numberOfFreeSegments()){
			for(int i =0; i<numberOfSegments;i++)
				temp[i] = -1;
			
		}		
		else{
			
			for(int i=0;i<numberOfSegments;i++){
				temp[i] = nextFreeSegment;
				nextFreeSegment +=1;
				if(nextFreeSegment >= LogStructureFS.NumberOfSegments)
					nextFreeSegment = 0;
			}
		}
		return temp;
	}
	public static int getNextFreeBlock(){
		return nextFreeSegment*(LogStructureFS.NumberOfBlocks+1);
	}
	public static int[] getNumberOfFreeSectors(int numberOfSectors){
		int[] temp = new int[numberOfSectors];
		int numberOfSeg = numberOfSectors/LogStructureFS.NumberOfSectorsInABlock;
		
		
		
		
		
		return temp;
	}


}
