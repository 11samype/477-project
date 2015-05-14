/*
 * ConnectionHandler.java
 * Oct 7, 2012
 *
 * Simple Web Server (SWS) for CSSE 477
 * 
 * Copyright (C) 2012 Chandan Raj Rupakheti
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
 */
 
package server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.Timer;
import java.util.TimerTask;

import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseFactory;
import protocol.Protocol;
import protocol.ProtocolException;

/**
 * This class is responsible for handling a incoming request
 * by creating a {@link HttpRequest} object and sending the appropriate
 * response be creating a {@link HttpResponse} object. It implements
 * {@link Runnable} to be used in multi-threaded environment.
 * 
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class ConnectionHandler implements Runnable {
	private Server server;
	private Socket socket;
	private PluginHandler pluginHandler;
//	private String requestLog;
//	private String responseLog;
	private Timer timer;
	private BlacklistHandler fileHandler;
	
	public ConnectionHandler(Server server, Socket socket, BlacklistHandler fileHandler) {
		this.server = server;
		this.socket = socket;

		this.timer = new Timer();
		this.timer.schedule(new TimeOut(socket), 1000);
		
//		responseLog = "responselog.txt";
//		requestLog = "requestlog.txt";
//		
		pluginHandler = new PluginHandler();
		this.fileHandler = fileHandler;

	}
	
	/**
	 * @return the socket
	 */
	public Socket getSocket() {
		return socket;
	}


	/**
	 * The entry point for connection handler. It first parses
	 * incoming request and creates a {@link HttpRequest} object,
	 * then it creates an appropriate {@link HttpResponse} object
	 * and sends the response back to the client (web browser).
	 */
	public void run() {
		// Get the start time
		long start = System.currentTimeMillis();
		
		InputStream inStream = null;
		OutputStream outStream = null;
		
		try {
			inStream = this.socket.getInputStream();
			outStream = this.socket.getOutputStream();
		}
		catch(Exception e) {
			// Cannot do anything if we have exception reading input or output stream
			// May be have text to log this for further analysis?
			e.printStackTrace();
			
			// Increment number of connections by 1
			server.incrementConnections(1);
			// Get the end time
			long end = System.currentTimeMillis();
			this.server.incrementServiceTime(end-start);
			return;
		}
		
		// At this point we have the input and output stream of the socket
		// Now lets create a HttpRequest object
		HttpRequest request = null;
		HttpResponse response = null;
		try {
			request = HttpRequest.read(inStream);
			
//			System.out.println(request);
		}
		catch(ProtocolException pe) {
			// We have some sort of protocol exception. Get its status code and create response
			// We know only two kind of exception is possible inside fromInputStream
			// Protocol.BAD_REQUEST_CODE and Protocol.NOT_SUPPORTED_CODE
			int status = pe.getStatus();
			if(status == Protocol.BAD_REQUEST_CODE) {
				response = HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
			}
			// DONE: Handle version not supported code as well
			if(status == Protocol.NOT_SUPPORTED_CODE) {
				response = HttpResponseFactory.create505NotSupported(Protocol.CLOSE);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			// For any other error, we will create bad request response as well
			response = HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
		}
		this.timer.cancel();
		this.timer.purge();
		
//		writeLog(requestLog, "-");
//		System.out.println("Req Push");
		fileHandler.pushDataRequest(socket.getInetAddress().toString().substring(1));
		
		
		if(response != null) {
			// Means there was an error, now write the response object to the socket
			try {
				
				response.write(outStream);
//				System.out.println(response);
			}
			catch(Exception e){
				// We will ignore this exception
				e.printStackTrace();
			}
//			writeLog(responseLog, "-");
//			System.out.println("Res Push");
			fileHandler.pushDataResponse(socket.getInetAddress().toString().substring(1));

			// Increment number of connections by 1
			server.incrementConnections(1);
			// Get the end time
			long end = System.currentTimeMillis();
			this.server.incrementServiceTime(end-start);
			return;
		}
		
		// We reached here means no error so far, so lets process further
		try {
			// Fill in the code to create a response for version mismatch.
			// You may want to use constants such as Protocol.VERSION, Protocol.NOT_SUPPORTED_CODE, and more.
			// You can check if the version matches as follows
			if(!request.getVersion().equalsIgnoreCase(Protocol.VERSION)) {
				// Here you checked that the "Protocol.VERSION" string is not equal to the  
				// "request.version" string ignoring the case of the letters in both strings
				// DONE: Fill in the rest of the code here
				response = HttpResponseFactory.create505NotSupported(Protocol.CLOSE);
			}
			else {
				Timer timer = new Timer();
				timer.schedule(new TimeOut(socket), 1000);
				response = pluginHandler.handle(request, timer, outStream, server.getRootDirectory());
			}

		}
		catch(Exception e) {
			e.printStackTrace();
		}
		

		// DONE: So far response could be null for protocol version mismatch.
		// So this is a temporary patch for that problem and should be removed
		// after a response object is created for protocol version mismatch.
		if(response == null) {
			response = HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
		}
		
		try{
			// Write response and we are all done so close the socket
			response.write(outStream);
//			System.out.println(response);
			socket.close();
		}
		catch(Exception e){
			// We will ignore this exception
			e.printStackTrace();
		} 
		
//		writeLog(responseLog, "-");
//		System.out.println("Res Push");
		fileHandler.pushDataResponse(socket.getInetAddress().toString().substring(1));
		// Increment number of connections by 1
		server.incrementConnections(1);
		// Get the end time
		long end = System.currentTimeMillis();
		this.server.incrementServiceTime(end-start);
	}
	
	private void writeLog(String logFile, String toAppend) {
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)))) {
			out.println(toAppend);
		} catch (IOException e) {
			System.err.println("IOException: " + e.getMessage());
		}
	}
}
