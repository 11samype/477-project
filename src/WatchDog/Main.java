package WatchDog;

import java.io.IOException;
import java.util.Timer;


public class Main {

	public static void main(String[] args) {
		
		Process pr = null;
		pr = startServer();
		if (pr == null) {
			System.exit(0);
		}
		
		Timer time = new Timer();
		WatchFile watchFile = new WatchFile("status.txt", pr);
		
		time.scheduleAtFixedRate(watchFile, 0, 5000);
		

	}
	
	static Process startServer() {
		Runtime rt = Runtime.getRuntime();
		Process pr = null;
		try {
			pr = rt.exec("java -jar WebServer.jar");
			
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return pr;
	}

}
