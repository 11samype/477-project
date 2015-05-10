package WatchDog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.TimerTask;



public class WatchFile extends TimerTask {
	
	File watchFile;
	Process webServerProcess;
	
	public WatchFile(String fileName, Process webServerProcess) {
		watchFile = new File(fileName);
		this.webServerProcess = webServerProcess;
	}
	
	@Override
	public void run() {
		
		Date lastModified = new Date(watchFile.lastModified());
		Date current = new Date();
		
		long lastModifiedMillis = lastModified.getTime();
		long currentMillis = current.getTime();
		
		long diff = currentMillis - lastModifiedMillis;
		
		if (diff > 6000) {
			
//			writeLog("watchLog.txt", "DOWN: " + diff);
			
			webServerProcess.destroy();
			
			Runtime rt = Runtime.getRuntime();
			try {
				webServerProcess = rt.exec("java -jar WebServer.jar");
			} 
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} else {
//			writeLog("watchLog.txt", "UP " + diff);
		}
		
		
	}
	
	private void writeLog(String logFile, String toAppend) {
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)))) {
			out.println(toAppend);
		} catch (IOException e) {
			System.err.println("IOException: " + e.getMessage());
		}
	}

}
