package WatchDog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.TimerTask;

public class WatchFile extends TimerTask {

	File watchFile;
	Process webServerProcess;
	HashMap<InetAddress, Integer> requesters;

	public WatchFile(String fileName, Process webServerProcess) {
		watchFile = new File(fileName);
		this.webServerProcess = webServerProcess;

		this.requesters = new HashMap<InetAddress, Integer>();
	}

	@Override
	public void run() {

		Date lastModified = new Date(watchFile.lastModified());
		Date current = new Date();

		long lastModifiedMillis = lastModified.getTime();
		long currentMillis = current.getTime();

		long diff = currentMillis - lastModifiedMillis;

		if (diff > 6000) {

			// writeLog("watchLog.txt", "DOWN: " + diff);

			webServerProcess.destroy();

			blockAttacker();

			Runtime rt = Runtime.getRuntime();
			try {
				webServerProcess = rt.exec("java -jar WebServer.jar");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			// writeLog("watchLog.txt", "UP " + diff);
		}

	}

	/**
	 * 
	 */
	private void blockAttacker() {

		requesters.clear();
		File blockedFile = new File("requestlog.txt");

		// go line by line in config to see if request can be filled
		try (BufferedReader br = new BufferedReader(new FileReader(blockedFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				InetAddress requester = InetAddress
						.getByName(line.substring(1));

				Integer requests = requesters.get(requester);
				
				if (requests == null) {
					requests = 0;
				}
				requests++;
				
				requesters.put(requester, requests);
				
			}
			
			Set<InetAddress> addressSet = requesters.keySet();
			for (InetAddress address : addressSet) {
				if (requesters.get(address) > 100) {
					
				}
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	private void writeLog(String logFile, String toAppend) {
		try (PrintWriter out = new PrintWriter(new BufferedWriter(
				new FileWriter(logFile, true)))) {
			out.println(toAppend);
		} catch (IOException e) {
			System.err.println("IOException: " + e.getMessage());
		}
	}

}
