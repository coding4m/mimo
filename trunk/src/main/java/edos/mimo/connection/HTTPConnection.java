/* 
 * Copyright 2004-2007 EDOS consortium http://www.edos-project.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package edos.mimo.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.LinkedList;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.log4j.Logger;

import edos.distribution.mirror.HTTPConnection;
import edos.distribution.mirror.IConnection;
import edos.mimo.Access;
import edos.mimo.filesystem.IFile;
import edos.mimo.filesystem.MirrorFile;

public class HTTPConnection implements IConnection {
	private static Logger logger = Logger.getLogger(HTTPConnection.class);
	private URL urlAddress;
	private BufferedReader httpStream;

	public HTTPConnection() {
		this.urlAddress = null;
		this.httpStream = null;
	}
	
	public HTTPConnection(String host, String path) {
		try {
			this.urlAddress = new URL("http", host, path);
		} catch (MalformedURLException e) {
			logger.fatal(e.getMessage());
		}
		this.httpStream = null;
	}

	public HTTPConnection(Access access) {
		this(access.getHost(), access.getLocalPath());
	}

	public void connect() throws IOException {
		httpStream = new BufferedReader(new InputStreamReader(urlAddress.openStream()));
	}
	
	public List<String> ls() throws IOException {
		return null;
	}
	
	public List<IFile> lsl() throws IOException {
		LinkedList<IFile> fileList = new LinkedList<IFile>();
		
		String line = httpStream.readLine(); 
		while (null != line) {
			int i = line.indexOf("<a href=\"");
			if ((i >= 0) && (line.indexOf(".rpm") >= 0)) {
				String fileName = line.substring(i+9, line.indexOf("\">", i));
				IFile file = new MirrorFile(fileName);
				fileList.add(file);
			}
			line = httpStream.readLine();
		}
		
		return fileList;
	}
	
	public List<IFile> lsFrom(String relPath) throws IOException {
		return null;
	}
	
	public List<IFile> lsDirFrom(String relPath) throws IOException {
		return null;
	}
	
	public void close() {
		try {
			if (null != httpStream) httpStream.close(); 
		} catch (IOException e) {
			logger.fatal(e.getMessage());
		}
		if (null != urlAddress) urlAddress = null; 
	}
	
	public boolean isValidPath(String path) { 
		try{
			connect();
			return true;
			
		}catch(IOException ioe) {
			return false;
		}
	}
}
