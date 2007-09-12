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

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edos.distribution.mirror.FTPConnection;
import edos.distribution.mirror.IConnection;
import edos.distribution.mirror.NetworkConnection;
import edos.mimo.Access;
import edos.mimo.IPass;
import edos.mimo.Identity;
import edos.mimo.filesystem.IFile;
import edos.mimo.filesystem.MirrorFileFactory;

public class FTPConnection extends NetworkConnection implements IConnection{
	private static Logger logger = Logger.getLogger(FTPConnection.class);
	
	private FTPClient ftpc;
		
	/**
	 * A FTP connection does not necessarily needs a login and password.
	 * This constructor sets the default to <code>anonymous</code>.
	 */
	public FTPConnection(Access a) {
		this(new Identity("anonymous", "edos-project@mandriva.com", "nokeyfile"), a);
	}
	
	/**
	 * Constructor
	 * Calls base class constructor.
	 * @param id
	 */
	public FTPConnection(Identity id, Access access) {
		super(id, access);		
		ftpc = new FTPClient();
	}
	
	public FTPConnection(IPass pass) {
		super(pass.getIdentity(), pass.getAccess());
		ftpc = new FTPClient();
	}
	
	/**
	 * This method overrides the base class' one. It is a very naive ls
	 * command which only returns the file names (as <code>String</code>s)
	 * and nothing else.
	 */
	public List<String> ls() throws IOException {
		// is the connection still active?
		checkConnection();	// potentially throws an IOException
		
		// perform the command
		//String fileNames[] = ftpc.listNames();
		FTPFile[] files = ftpc.listFiles();
		LinkedList<String> list = new LinkedList<String>();
		
		for(int i = 0; i < files.length; i++) {
			String name = files[i].getName();
			list.add(name);
		}
		
		return list;		
	}
	
	/**
	 * Checks to see if the connection is still active.
	 * @throws IOException
	 */
	private void checkConnection() throws IOException {
		logger.debug("checking FTP connection" + " - " + ftpc);
		
		//logger.setLevel(Level.DEBUG);
		logger.debug("ftpc: " + ftpc);
		logger.debug("ftpc passive host: " + ftpc.getPassiveHost());
		
		// is the connection still active?
		try{
			logger.debug("Checking connection with " + ftpc.getPassiveHost());
			ftpc.sendNoOp();
			logger.debug("Connection with " + host + " is alive!");
			
		}catch(FTPConnectionClosedException clse) {
			// try to reconnect once
			logger.info("FTP connection is closed, trying to reconnect once");
			close();
			connect();
			// or throws another exception
		}
	}
	
	/**
	 * Equivalent to ls -l
	 * <p>
	 * Gives a long description of the files present in the current directory.
	 * </p> 
	 */
	public List<IFile> lsl() throws IOException {
		// is the connection still active?
		checkConnection();	// potentially throws an IOException
		
		// perform the command
		FTPFile[] files = ftpc.listFiles();
		LinkedList<IFile> list = new LinkedList<IFile>();
		
		for(int i = 0; i < files.length; i++) {
			IFile file = MirrorFileFactory.create("./", files[i]);
			list.add(file);
		}
		
		return list;		
	}

	/**
	 * Performs a 'list files' command and returns only the directories.
	 * @param relative path (from the working directory) where to perform the list
	 * @return a list of <code>Directory</code>s
	 * @throws IOException
	 */
	public List<IFile> lsDirFrom(String relPath) throws IOException {
		// is the connection still active?
		checkConnection();	// potentially throws an IOException
		
		// perform the command
		FTPFile[] fileList = ftpc.listFiles(relPath);
		LinkedList<IFile> dirs = new LinkedList<IFile>();
		
		for(int i = 0; i < fileList.length; ++i) {
			// add only the directories
			if(fileList[i].isDirectory()) {
				IFile file = MirrorFileFactory.create(relPath, fileList[i]);
				dirs.add(file);
			}
		}
		
		return dirs;		
	}

	public List<IFile>  lsFrom(String relPath) throws IOException {
		// is the connection still active?
		checkConnection();	// potentially throws an IOException
		
		// move to proper working directory
		String currentDir = ftpc.printWorkingDirectory();
		if(!ftpc.changeWorkingDirectory(relPath))
			throw new IOException("Unable to change to directory " + relPath);
		
		FTPFile[] files = ftpc.listFiles();
		LinkedList<IFile> list = new LinkedList<IFile>();
		
		for(int i = 0; i < files.length; i++) {
			IFile file = MirrorFileFactory.create(relPath, files[i]);
			list.add(file);
		}
		
		ftpc.changeWorkingDirectory(currentDir);
		return (List<IFile>)list;		
	}
	
	/**
	 * connect
	 * This method tries to establish the connection to the server.
	 * Throws an exception if it fails.
	 */
	public void connect() throws IOException {
		//logger.setLevel(Level.DEBUG);
		
		int reply;		// will contain the reply code sent by the server
		logger.debug("Opening connection with " + host);
		
		// host check
		if(host.equals(INVALID_HOST))
			throw new IOException("Invalid host! Please set the host.");
		
		// connection
		ftpc.connect(host);
		reply = ftpc.getReplyCode();
		if(!FTPReply.isPositiveCompletion(reply)) {
			ftpc.disconnect();
			throw new IOException("Connection refused: " + ftpc.getReplyCode());
		}
		
		// login
		if(!ftpc.login(userLogin, password)) {
			ftpc.disconnect();
			throw new IOException("Unable to login as " + userLogin
					+ " with password '" + password +  "'");
		}
		
		// passive mode (pass the firewall)
		ftpc.enterLocalPassiveMode();	
		
		// go to Mandriva mirror space
		ftpc.changeWorkingDirectory(localPath);
		
		logger.info("FTP connection established with " + host + " - " + ftpc);
	}

	/**
	 * close
	 * This method close the connection if it exists.
	 */
	public void close() {
		try {
			if(ftpc != null && ftpc.isConnected()) {
				ftpc.logout();
				ftpc.disconnect();
			}
			
		}catch(IOException ioe) {
			logger.error("ftp complaint while closing: " + ioe.getMessage());
			// do nothing
		}catch(NullPointerException npe) {
			
		}
		logger.info("FTP connection closed with " + host + " - " + ftpc);
	}

	public boolean isValidPath(String path){
		try {
			// is the connection still active?
			checkConnection();	// potentially throws an IOException
			
			// move to proper working directory
			String currentDir = ftpc.printWorkingDirectory();
			
			if(ftpc.changeWorkingDirectory(path)) {
				ftpc.changeWorkingDirectory(currentDir);
				return true;
			}
			
		}catch(IOException ioe) {
			// don't care
		}
		return false;
	}


}
