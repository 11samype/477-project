/*
 * POSTRequestHandler.java
 * Apr 25, 2015
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
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponse200OK;
import protocol.HttpResponseFactory;
import protocol.Protocol;

/**
 * 
 * @author Chandan R. Rupakheti (rupakhcr@clarkson.edu)
 */
public class POSTRequestHandler implements IRequestHandler {

	/* (non-Javadoc)
	 * @see server.IRequestHandler#interpretRequest(protocol.HttpRequest, server.Server)
	 */
	@Override
	public HttpResponse interpretRequest(HttpRequest request, Server server) {
		
		StringBuffer buffer = new StringBuffer();
		buffer.append(request.getBody());
		String body = buffer.toString();
		
		String[] segments = body.split("\""); // index 3 should house name
		
		String fileText = body.substring(body.indexOf("text/plain") + 14, body.lastIndexOf("------WebKitFormBoundary") - 2);
		
//		System.out.println(fileText);
		
		String uri = request.getUri();
		String rootDirectory = server.getRootDirectory();
		File file;
		
//		String location = rootDirectory + uri;
		String location = rootDirectory + "/" + segments[3];
//		System.out.println(location);
		file = new File(location);
		
		FileWriter fw;
		try {
//			System.out.println(file.getAbsolutePath());
			fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
//			bw.write(request.getBody());
			bw.write(fileText);
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		
		return HttpResponseFactory.create200OK(null, Protocol.CLOSE);
	}

}
