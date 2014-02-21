package th.in.whs.ku.bus.api;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class MemoryManager {
	
	private static double totalMemory = -1;
	
	// https://stackoverflow.com/questions/12551547/is-there-a-way-to-get-total-device-rami-need-it-for-an-optimization
	public static double getTotalMemory() {
		
		if(totalMemory != -1){
			return totalMemory;
		}

		String fileName = "/proc/meminfo";
		try {
			FileReader localFileReader = new FileReader(fileName);
			BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);
			String memInfo = localBufferedReader.readLine(); // should be MemTotal
			String[] memCols = memInfo.split("\\s+");
			//total Memory
			double memory = Double.parseDouble(memCols[1]) / 1024.0;   
			localBufferedReader.close();
			totalMemory = memory;
			return memory;
		} catch (IOException e) {       
			return -1;
		}
	}  
}
