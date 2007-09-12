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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

import edos.distribution.mirror.HTTPConnection3;
import edos.distribution.mirror.IConnection;
import edos.distribution.mirror.NetworkConnection;
import edos.mimo.Access;
import edos.mimo.Date;
import edos.mimo.Identity;
import edos.mimo.filesystem.IFile;
import edos.mimo.filesystem.MirrorDirectory;
import edos.mimo.filesystem.MirrorFileFactory;


/**
 * This alternative to HTTPConnection is based on the
 * Apache commons HTTP3.0 client (under Apache License)
 * 
 * From sample code at:
 * 	http://svn.apache.org/viewcvs.cgi/jakarta/commons/proper/httpclient/trunk/src/examples/TrivialApp.java?view=markup
 * 
 * Apache License
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * @author marc
 *
 */
public class HTTPConnection3 extends NetworkConnection implements IConnection {
	private static Logger logger = Logger.getLogger(HTTPConnection3.class);
	
	private static final String BASE_DIR = "";
	
	private HttpClient httpClient = null;
	
	
	public HTTPConnection3(Access access) {
		super(access);
		
		setup();
	}
	
	public HTTPConnection3(Identity id, Access access) {
		super(id, access);
		
		setup();
	}
	
	private void setup() {
		httpClient = new HttpClient();    
		        
		//establish a connection within 5 seconds
		httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
		
	}
	
	
	/**
	 * Gets the url relative to the access url.
	 * 
	 * Example
	 * access is http://www.mandriva.com/sales
	 * relativePart is linux
	 * result is  http://www.mandriva.com/sales/linux
	 * 
	 * @param relativePart
	 * @return
	 */
	private String getUrlFor(String relativePart) {
		if(relativePart.equals("."))
			relativePart = "";
		
		StringBuffer sb = new StringBuffer( "http://")
						.append(this.host)
						.append(relativePart)
						.append("/");
		
		return sb.toString();
	}

	@Override
	public List<String> ls() throws IOException {
		List<String> fileNames = new ArrayList<String>();
		
		List<IFile> files = lsFrom(BASE_DIR);
		Iterator<IFile> it = files.iterator();
		while(it.hasNext()) 
			fileNames.add(it.next().getName());
		
		return fileNames;
	}

	@Override
	public List<IFile> lsl() throws IOException {
		return lsFrom(BASE_DIR);
	}

	@Override
	public List<IFile> lsFrom(String relPath) throws IOException {
        HttpMethod method = null;
        List<IFile> files = new ArrayList<IFile>();
        BufferedReader in = null;		// needs to be read entirely then closed!
        

        // for HTTP directories end with / but not for the mirror monitor (multi-protocol)
		String path;
		if(relPath.endsWith("/"))
			path = relPath.substring(0, relPath.length()-1);
		else 
			path = relPath;
		
		// takes the mirrormonitor (with no /) and can add one if necessary
        String url = getUrlFor(path);

        logger.info("---> lsFrom " + path + "(url=" + url + ")");
        
        //create a method object
        method = new GetMethod(url);
        method.setFollowRedirects(true);
        
        try{     
        	// Execute the method.
            int statusCode = httpClient.executeMethod(method);
            
            if (statusCode != HttpStatus.SC_OK) 
            	throw new IOException("HTTP Method failed: " + method.getStatusLine());
            
            // best method due to large page size, it is more efficient        
            in = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
            
            String line = in.readLine();
            while(line != null) {
            	files.addAll(parseLine(line, path));	// add the file(s) found on this line
            	line = in.readLine();
            }
            
            
        } catch (HttpException he) {
        	logger.error(he.getStackTrace());
        	throw new IOException("Http error connecting to '" + url + "'" 
        			+ he.getMessage());   
        	
        }finally{
        	in.close();
        	method.releaseConnection();
        }
        
		return files;
	}

	/**
	 * Sends a get command for a specific url with a relative path
	 * (under the limit of the defined access path)
	 * 
	 * @param relPath
	 * @return the raw HTTP response body as a <code>String</code>
	 * @throws IOException
	 */
	public String executeGet(String relPath) throws IOException {
        StringBuffer sbContent = new StringBuffer();
        HttpMethod method = null;
        String url = getUrlFor(relPath);
        BufferedReader in = null;		// needs to be read entirely then closed!

        //create a method object
        method = new GetMethod(url);
        method.setFollowRedirects(true);
    
        method.getParams();
    
        try{     
        	// Execute the method.
            int statusCode = httpClient.executeMethod(method);
            
            if (statusCode != HttpStatus.SC_OK) 
            	throw new IOException("HTTP Method failed: " + method.getStatusLine());
            
            // best method due to large page size, it is more efficient        
            in = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
            
            String line = in.readLine();
            while(line != null) {
            	sbContent.append(line).append("\n");
            	line = in.readLine();
            }
            
        } catch (HttpException he) {
        	logger.error(he.getStackTrace());
        	throw new IOException("Http error connecting to '" + url + "'" 
        			+ he.getMessage());   
        	
        }finally{
        	if(in!= null)
        		in.close();
        	if(method != null)
        		method.releaseConnection();
        }
        
        return sbContent.toString();
	}

	/**
	 * Get base directory (as defined in internal Access value)
	 * 
	 * @return the raw HTTP response body as a <code>String</code>
	 * @throws IOException 
	 */
	public String executeGet() throws IOException {
		return executeGet(".");
	}
	
	@Override
	public List<IFile> lsDirFrom(String relPath) throws IOException {
		List<IFile> dirs = new ArrayList<IFile>();
		List<IFile> fileList = lsFrom(relPath);
		Iterator<IFile> it = fileList.iterator();
		
		while(it.hasNext()) {
			IFile file = it.next();
			
			if(file instanceof MirrorDirectory) 
				dirs.add(file);
		}
		
		return dirs;		
		
	}

	@Override
	public void connect() throws IOException {
		// main fire an Exception ; exactly what we are looking for
		executeGet();
	}

	@Override
	public void close() {
		// nothing todo HTTP is a stateless protocol
	}
	
	/**
	 * Parse a line from a HTTP response body and extract a file
	 * or many files (if stumbling across a one-line html body)
	 * 
	 * The idea is to extract the file name from within the link
	 * since there is a necessary bijection link/filenames.
	 * 
	 * Limitation: for now we assume one line per file
	 * TODO check for multiple files per line first, then break the line around <a> tag
	 * 
	 * @author marc
	 * @param line
	 * @return
	 */
	private List<IFile> parseLine(String line, String curPath) {
		IFile file = null;
		List<IFile> files = new ArrayList<IFile>();
		int curIndex = -1;	// marking last matching index
		
		while((curIndex = line.indexOf("<a href=\"", curIndex+1)) != -1) {
			String fileName = line.substring(curIndex+9, line.indexOf("\">", curIndex));
			
			// type of file: regular or directory?
			boolean isDirectory = false;
			String patternFileLine = ".*[0-9]+[kM]\\s*";	// contains a size at the end of line
			if(fileName.endsWith("/") && !line.contains("Parent Directory"))	// discard title line
				isDirectory = true;
			
			else if(!line.matches(patternFileLine))
				continue;	// to be a file, there needs to be a size indicated
			
			// parsing date and time
			StringBuffer sbDate = new StringBuffer();
			int i = 0;
			
			String afterFileName = line.substring(line.indexOf("</a>", curIndex)+4).trim();
			String[] parts = afterFileName.split("\\p{javaWhitespace}");
			
			/*  testing
			for(i=0; i<parts.length; i++) {
				if(parts[i].contains("-") || parts[i].contains(":")) { // dates' parts
					sbDate.append(parts[i]).append(" ");
				}
			}
			*/
			
			// date
			while(parts[i].equals("")) i++;
			sbDate.append(parts[i++]).append(" ");
			
			// time
			while(parts[i].equals("")) i++;
			sbDate.append(parts[i++]).append(" ");
			
			Date date;
			try {
				date = new Date(sbDate.toString(), Date.SHORT);
			} catch (ParseException e) {
				logger.error("Unable to parse date " + sbDate.toString() + " ; using 'now' - " + e.getMessage());
				date = new Date();
			}
			
			// size ; the string contains either 'k' or 'M'
			// and also the directories have no reported size, say linux default is 4096
			float size;
			if(isDirectory) 
				size = IFile.DEFAULT_DIRECTORY_SIZE;
			else {
				while(parts[i].equals("")) i++;
				if(parts[i].contains("k"))
					size = Float.parseFloat(parts[i].substring(0, parts[i].indexOf('k'))) * 1024;
				else if(parts[i].contains("M"))
					size = Float.parseFloat(parts[i].substring(0, parts[i].indexOf('M'))) * 1024 * 1024;
				else
					size = Float.parseFloat(parts[i]);
			}
			
			String path = curPath;
			file  = MirrorFileFactory.create(fileName, date, (long)size, path, isDirectory);
			files.add(file);
			
			logger.info("added file: " + file);
	}
		
		return files;
	}

	public boolean isValidPath(String path) { 
        String url = getUrlFor(path);
		HttpMethod method = new GetMethod(url);
        method.setFollowRedirects(true);
        
        try{     
        	// Execute the method.
            int statusCode = httpClient.executeMethod(method);
            
            if (statusCode != HttpStatus.SC_OK) 
            	return false;
            
        }catch(IOException ioe) {
        	// don't care
        	return false;
        }
        
        return true;
	}

}
