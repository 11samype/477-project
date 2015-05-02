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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.StandardWatchEventKinds;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import protocol.HttpRequest;
import protocol.HttpResponse404NotFound;
import protocol.HttpResponseFactory;
import protocol.HttpResponseSender;
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
	public PluginHandler() throws IOException {
		
		WatchService watcher = FileSystems.getDefault().newWatchService();
		Path dir = Paths.get("plugins");
		dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE,
				StandardWatchEventKinds.ENTRY_DELETE,
				StandardWatchEventKinds.ENTRY_MODIFY);
		while (true) {

			WatchKey key;

			try {

				key = watcher.take();

			} catch (InterruptedException ex) {
				return;
			}

			for (WatchEvent<?> event : key.pollEvents()) {

				WatchEvent.Kind<?> kind = event.kind();

				WatchEvent<Path> ev = (WatchEvent<Path>) event;
				Path filename = ev.context();

				System.out.println(kind.name() + ": " + filename);

				if (kind == StandardWatchEventKinds.OVERFLOW) {
					continue;
				} else if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
					System.out.println("created");
				
					if (isDirectory(filename) && checkPlugin(filename)) {
						plugins.add(filename.toString());
						writeOverallConfig();
					}
					
				} else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
					System.out.println("deleted");
					plugins.remove(filename);
					writeOverallConfig();
				} else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
					System.out.println("modified");
					
					if (isDirectory(filename) && !checkPlugin(filename)){
						plugins.remove(filename.toString());
						writeOverallConfig();
					}
				}
				
				

			}

			boolean valid = key.reset();
			if (!valid) {
				break;
			}

		}
	}

	void handle(HttpRequest request, OutputStream outStream, String rootDirectory) {

		String uri = request.getUri();

		String uriParts[] = uri.split("/");

		String pluginName = uriParts[0];

		if (!plugins.contains(pluginName)) {
			HttpResponseSender.sendResponse(
					HttpResponseFactory.create404NotFound(Protocol.CLOSE),
					null, outStream);
		}

		File pluginConfigFile = new File("plugins/" + pluginName
				+ "/config.txt");

		try (BufferedReader br = new BufferedReader(new FileReader(
				pluginConfigFile))) {
			String line;
			while ((line = br.readLine()) != null) {

				String mapping[] = uri.split(" ");

				if (request.getMethod().equals(mapping[0])) {
					if (("/" + uriParts[1]).equals(mapping[1])) {
						
						File pluginJar = new File("plugins/" + pluginName + "/" + pluginName);
						
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
						
						if (classList.contains(mapping[2])) {
							int index = classList.indexOf(mapping[2]);
							Class<?> pluginClass = classList.get(index);
							
							IRequestHandler plugin = (IRequestHandler) pluginClass.newInstance();
							plugin.doRequest(request, rootDirectory);
							return;
						}
					}
				}

				HttpResponseSender.sendResponse(
						HttpResponseFactory.create404NotFound(Protocol.CLOSE),
						null, outStream);

			}
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	private void readOverallConfig() {
		plugins.clear();
		File configFile = new File("plugins/config.txt");

		try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				plugins.add(line);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

	}
	
	private void writeOverallConfig() throws IOException {
		File configFile = new File("plugins/config.txt");
		
		FileOutputStream fos = new FileOutputStream(configFile);
		
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
		
		for (int i = 0; i < plugins.size(); i++) {
			bw.write(plugins.get(i));
			bw.newLine();
		}
		
		bw.close();
	}

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

	private boolean checkPlugin(Path filename){
		
		boolean hasConfig = new File(filename.toString() + "/config.txt").exists();
		System.out.println(filename.toString() + "/config.txt");
		boolean hasPlugin = new File(filename.toString() + "/" + filename.toString() + ".jar").exists();
		System.out.println(filename.toString() + "/" + filename.toString() + ".jar");
		
		if (hasConfig && hasPlugin) {
			return true;
		}
		return false;
	}
	
	private boolean isDirectory(Path filename) {

		boolean isDirectory = filename.toFile().isDirectory();
		isDirectory = new File(filename.toString()).isDirectory();
		System.out.println("Directory: " + isDirectory);
		return isDirectory;
	}
}


