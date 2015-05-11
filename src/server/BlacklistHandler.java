/*
 * FileHandler.java
 * May 10, 2015
 *
 * Simple Web Server (SWS) for EE407/507 and CS455/555
 * 
 * Copyright (C) 2011 Chandan Raj Rupakheti, Clarkson University
 * 
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either 
 * version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/lgpl.html>.
 * 
 * Contact Us:
 * Chandan Raj Rupakheti (rupakhcr@clarkson.edu)
 * Department of Electrical and Computer Engineering
 * Clarkson University
 * Potsdam
 * NY 13699-5722
 * http://clarkson.edu/~rupakhcr
 */
 
package server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TimerTask;

/**
 * 
 * @author Chandan R. Rupakheti (rupakhcr@clarkson.edu)
 */
public class BlacklistHandler extends TimerTask {

	private HashMap<InetAddress, Integer> requests = new HashMap<InetAddress, Integer>();
	private HashMap<InetAddress, Integer> responses = new HashMap<InetAddress, Integer>();
	
	private ArrayList<InetAddress> blockedUsers = new ArrayList<InetAddress>();
	
	public void setBlockedUsers(ArrayList<InetAddress> blocked){
		this.blockedUsers = blocked;
	}
	
	public ArrayList<InetAddress> getBlockedUsers(){
		return this.blockedUsers;
	}
	
	
	public void pushDataResponse(String data){
		try {
			InetAddress address = InetAddress.getByName(data);
			Integer count = responses.get(address);
			
			if (count == null){
				count = 0;
			}
			count++;
			responses.put(address, count);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void pushDataRequest(String data){
		try {
			InetAddress address = InetAddress.getByName(data);
			Integer count = requests.get(address);
			
			if (count == null){
				count = 0;
			}
			count++;
			requests.put(address, count);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void resetFiles(){
		responses.clear();
		requests.clear();
	}
	
	public void run(){
		Set<InetAddress> requestSet = requests.keySet();
		for (InetAddress request : requestSet) {
			if (requests.get(request) > 100){
				try (PrintWriter out = new PrintWriter(new BufferedWriter(
						new FileWriter("blacklist.txt", true)))) {
					out.println(request);
					this.blockedUsers.add(request);
				} catch (IOException e) {
					System.err.println("IOException: " + e.getMessage());
				}
			}
		}
		
	}
}

