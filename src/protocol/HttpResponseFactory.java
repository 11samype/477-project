/*
 * HttpResponseFactory.java
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
 
package protocol;

import java.io.File;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;

/**
 * This is a factory to produce various kind of HTTP responses.
 * 
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class HttpResponseFactory {
	/**
	 * Convenience method for adding general header to the supplied response object.
	 * 
	 * @param response The {@link HttpResponse} object whose header needs to be filled in.
	 * @param connection Supported values are {@link Protocol#OPEN} and {@link Protocol#CLOSE}.
	 */
	private static void fillGeneralHeader(HttpResponse response, String connection) {
		// Lets add Connection header
		response.put(Protocol.CONNECTION, connection);

		// Lets add current date
		Date date = Calendar.getInstance().getTime();
		response.put(Protocol.DATE, date.toString());
		
		// Lets add server info
		response.put(Protocol.Server, Protocol.getServerInfo());

		// Lets add extra header with provider info
		response.put(Protocol.PROVIDER, Protocol.AUTHOR);
	}
	
	private static void appendFile(HttpResponse response, File file) {
		response.addFile(file);
		
		// Lets add last modified date for the file
		long timeSinceEpoch = file.lastModified();
		Date modifiedTime = new Date(timeSinceEpoch);
		response.put(Protocol.LAST_MODIFIED, modifiedTime.toString());
		
		// Lets get content length in bytes
		long length = file.length();
		response.put(Protocol.CONTENT_LENGTH, length + "");
		
		// Lets get MIME type for the file
		FileNameMap fileNameMap = URLConnection.getFileNameMap();
		String mime = fileNameMap.getContentTypeFor(file.getName());
		// The fileNameMap cannot find mime type for all of the documents, e.g. doc, odt, etc.
		// So we will not add this field if we cannot figure out what a mime type is for the file.
		// Let browser do this job by itself.
		if(mime != null) { 
			response.put(Protocol.CONTENT_TYPE, mime);
		}
	}
	
	/**
	 * Creates a {@link HttpResponse} object for sending the supplied file with supplied connection
	 * parameter.
	 * 
	 * @param file The {@link File} to be sent.
	 * @param connection Supported values are {@link Protocol#OPEN} and {@link Protocol#CLOSE}.
	 * @return A {@link HttpResponse} object represent 200 status.
	 */
	public static HttpResponse create200OK(File file, String connection) {
		HttpResponse response = new HttpResponse200OK();
		
		// Lets fill up header fields with more information
		fillGeneralHeader(response, connection);
		
		if (file != null) {
			appendFile(response, file);
		}
		
		return response;
	}
	
	/**
	 * Creates a {@link HttpResponse} object for sending bad request response.
	 * 
	 * @param connection Supported values are {@link Protocol#OPEN} and {@link Protocol#CLOSE}.
	 * @return A {@link HttpResponse} object represent 400 status.
	 */
	public static HttpResponse create400BadRequest(String connection) {
		HttpResponse response = new HttpResponse400BadRequest();
		
		// Lets fill up header fields with more information
		fillGeneralHeader(response, connection);
		
		return response;
	}
	
	/**
	 * Creates a {@link HttpResponse} object for sending not found response.
	 * 
	 * @param connection Supported values are {@link Protocol#OPEN} and {@link Protocol#CLOSE}.
	 * @return A {@link HttpResponse} object represent 404 status.
	 */
	public static HttpResponse create404NotFound(String connection) {
		HttpResponse response = new HttpResponse404NotFound();
		
		// Lets fill up the header fields with more information
		fillGeneralHeader(response, connection);
		
		return response;	
	}
	
	/**
	 * Creates a {@link HttpResponse} object for sending version not supported response.
	 * 
	 * @param connection Supported values are {@link Protocol#OPEN} and {@link Protocol#CLOSE}.
	 * @return A {@link HttpResponse} object represent 505 status.
	 */
	public static HttpResponse create505NotSupported(String connection) {
		HttpResponse response = new HttpResponse505NotSupported();
		
		// Lets fill up the header fields with more information
		fillGeneralHeader(response, connection);
		
		return null;
	}
	
	/**
	 * Creates a {@link HttpResponse} object for sending file not modified response.
	 * 
	 * @param connection Supported values are {@link Protocol#OPEN} and {@link Protocol#CLOSE}.
	 * @return A {@link HttpResponse} object represent 304 status.
	 */
	public static HttpResponse create304NotModified(String connection) {
		HttpResponse response = new HttpResponse304NotModified();
		
		fillGeneralHeader(response, connection);
		
		return null;
	}
	
	/**
	 * Creates a {@link HttpResponse} object for sending file not modified response.
	 * 
	 * @param connection Supported values are {@link Protocol#OPEN} and {@link Protocol#CLOSE}.
	 * @return A {@link HttpResponse} object represent 304 status.
	 */
	public static HttpResponse createCustom(String connection, int code, String text) {
		HttpResponse response = new CustomHttpResponse(code, text);
		
		fillGeneralHeader(response, connection);
		
		return null;
	}
}
