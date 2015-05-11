/*
 * PluginHandler.java
 * May 2, 2015
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Timer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseFactory;
import protocol.Protocol;

/**
 * 
 * @author Chandan R. Rupakheti (rupakhcr@clarkson.edu)
 */
public class PluginHandler {

	ArrayList<String> plugins;

	/**
	 * @throws IOException
	 * 
	 */
	public PluginHandler() {
		
		
	}
	/**
	 * 
	 * Interprets Request and routes to proper plugin request handlers
	 * 
	 * @param request
	 * @param outStream
	 * @param rootDirectory
	 * @return
	 */
	HttpResponse handle(HttpRequest request, Timer timer, OutputStream outStream, String rootDirectory) {

		// get URI, break into parts
		String uri = request.getUri();
		String uriParts[] = uri.split("/");
		String pluginName = uriParts[1];
		
		// check if files exist
		File pluginFile = new File("plugins/" + pluginName);
		
		if (!(pluginFile.exists() && pluginFile.isDirectory())) {
			return HttpResponseFactory.create404NotFound(Protocol.CLOSE);
		}

		File pluginConfigFile = new File("plugins/" + pluginName
				+ "/config.txt");
		// go line by line in config to see if request can be filled
		try (BufferedReader br = new BufferedReader(new FileReader(
				pluginConfigFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				String mapping[] = line.split(" ");
				
				if (request.getMethod().equals(mapping[0])) {
					if (("/" + uriParts[2]).equals(mapping[1])) {

						File pluginJar = new File("plugins/" + pluginName + "/" + pluginName + ".jar");
						
						URL[] urls = new URL[] { pluginJar.toURI().toURL() };
						URLClassLoader loader = new URLClassLoader(urls);

						
						Class<?> iface = null;
						
						switch (request.getMethod()) {
						case Protocol.POST:
							iface = IPOSTRequestHandler.class;
							break;
						case Protocol.PUT:
							iface = IPUTRequestHandler.class;
							break;
						case Protocol.DELETE:
							iface = IDELETERequestHandler.class;
							break;
						default:
							iface = IGETRequestHandler.class;
							break;
						}
						
						List<Class<?>> classList = findImplementingClassesInJarFile(pluginJar, iface, loader);
						
						ArrayList<String> classListStrings = new ArrayList<String>();
						
						for (Class<?> class1 : classList) {
							classListStrings.add(pluginName + "." + class1.toString().split(" ")[1]);
						}
						
						if (classListStrings.contains(mapping[2])) {
							int index = classListStrings.indexOf(mapping[2]);
							Class<?> pluginClass = classList.get(index);
							
							IRequestHandler plugin = (IRequestHandler) pluginClass.newInstance();
							
							rootDirectory = "plugins/" + pluginName;
							
							String objectPath = "";
							
							for (int i = 3; i < uriParts.length; i++) {
								
								objectPath += "/" + uriParts[i];
							}
							loader.close();
							// proper request handler found in plugin, routing request to be handled
							
							HttpResponse response = plugin.doRequest(request, rootDirectory, objectPath);
							timer.cancel();
							timer.purge();
							return response;
							
						}
					}
				}
				
			}
		} catch (Exception e) {
			System.out.println(e);
		}
		return HttpResponseFactory.create404NotFound(Protocol.CLOSE);

	}
	
	/**
	 * Searches for classes that implement a specific interface in a jar file
	 * @param file
	 * @param iface
	 * @param loader
	 * @return
	 * @throws Exception
	 */
	public static List<Class<?>> findImplementingClassesInJarFile(File file,
			Class<?> iface, ClassLoader loader) throws Exception {
		List<Class<?>> implementingClasses = new ArrayList<Class<?>>();
		// scan the jar file for all included classes
		for (String classFile : scanJarFileForClasses(file)) {
			Class<?> clazz;
			try {
				// now try to load the class
				if (loader == null)
					clazz = Class.forName(classFile);
				else
					clazz = Class.forName(classFile, true, loader);

				// and check if the class implements the provided interface
				if (iface.isAssignableFrom(clazz) && !clazz.equals(iface)) {
					implementingClasses.add(clazz);

				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return implementingClasses;
	}

	/**
	 * Finds all classes within a jar file
	 * @param file
	 * @return
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public static List<String> scanJarFileForClasses(File file)
			throws IOException, IllegalArgumentException {
		if (file == null || !file.exists()) {
			throw new IllegalArgumentException(
					"Invalid jar-file to scan provided");
		}
		if (file.getName().endsWith(".jar")) {
			List<String> foundClasses = new ArrayList<String>();
			try (JarFile jarFile = new JarFile(file)) {
				Enumeration<JarEntry> entries = jarFile.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					if (entry.getName().endsWith(".class")) {
						String name = entry.getName();
						name = name.substring(0, name.lastIndexOf(".class"));
						if (name.indexOf("/") != -1)
							name = name.replaceAll("/", ".");
						if (name.indexOf("\\") != -1)
							name = name.replaceAll("\\", ".");

						foundClasses.add(name);
					}
				}
			}
			return foundClasses;
		}
		throw new IllegalArgumentException("No jar-file provided");
	}
}


