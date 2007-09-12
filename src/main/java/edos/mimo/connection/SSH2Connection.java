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
/*
 * Created on Aug 17, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edos.mimo.connection;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.ServerHostKeyVerifier;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import edos.distribution.mirror.IConnection;
import edos.distribution.mirror.SSH2Connection;
import edos.mimo.IPass;
import edos.mimo.Identity;
import edos.mimo.Shell;
import edos.mimo.exception.SSH2Exception;
import edos.mimo.filesystem.IFile;
import edos.mimo.filesystem.MirrorFileFactory;

/**
 * SSH2Connection. Model a SSH connection 
 * @author Marc and sample code from Christian Plattner, plattner@inf.ethz.ch
 * 
 */
/**
 * @author marc
 *
 */
public class SSH2Connection /* TODO extends NetworkConnection? */ implements ServerHostKeyVerifier, IConnection{
	private static Logger logger = Logger.getLogger(SSH2Connection.class);
	
	private String host;
	private String userLogin;
	private String password;
	//private String pem;			// deprecated since build 208 (ganymed)
	private File keyfile;
	private int port;
	private Connection sshc;
	private long acceptableLag;
	public static int INVALID_PORT = -1;
	public static String INVALID_HOST = "undefined host";
	public static int DEFAULT_PORT = 22;	// ssh is working on port 22 (generally)
	public static long DEFAULT_ACCEPTABLE_LAG = 3000;	// 3 seconds
	

	/*
	public SSH2Connection(Identity id) {
		this.userLogin = id.getLogin();
		this.password = id.getPassword();
		//this.pem = id.getPem();
		this.keyfile = id.getKeyfile();
		this.host = INVALID_HOST;
		this.port = INVALID_PORT;
		sshc = null;
		acceptableLag = DEFAULT_ACCEPTABLE_LAG;
	}
	
	public SSH2Connection(Identity id, Mirror mirror) throws IOException {
		this(id, mirror, DEFAULT_PORT, DEFAULT_ACCEPTABLE_LAG);
	}
	
	public SSH2Connection(Identity id, Mirror mirror, long lag) throws IOException {
		this(id, mirror, DEFAULT_PORT, lag);
	}

	public SSH2Connection(Identity id, Mirror mirror, int port) throws IOException {
		this(id, mirror, port, DEFAULT_ACCEPTABLE_LAG);
	}
	
	public SSH2Connection(Identity id, Mirror mirror, int port, long lag) throws IOException {
		this.host = mirror.getHost();
		this.port = port;
		this.acceptableLag = lag;
		this.userLogin = id.getLogin();
		this.password = id.getPassword();
		this.keyfile = id.getKeyfile();
		sshc = null;
		
		// now trying to establish connection, or throws exception
		connect(host, port);
	}
*/
	public SSH2Connection(Identity id, String host, int port) throws IOException {
		this(id, host, port, DEFAULT_ACCEPTABLE_LAG);
	}
	
	public SSH2Connection(Identity id, String host, int port, long lag) throws IOException {
		this.host = host;
		this.port = port;
		this.acceptableLag = lag;
		this.userLogin = id.getLogin();
		this.password = id.getPassword();
		this.keyfile = id.getKeyfile();
		sshc = null;
		
		// now trying to establish connection, or throws exception
		connect(host, port);
	}
	
	public SSH2Connection(IPass pass) throws IOException {
		this.host = pass.getAccess().getHost();
		this.port = pass.getAccess().getPort();
		this.acceptableLag = pass.getAccess().getAcceptableLag();
		this.userLogin = pass.getIdentity().getLogin();
		this.password  = pass.getIdentity().getPassword();
		this.keyfile   = pass.getIdentity().getKeyfile();
		sshc = null;
		
		connect(host, port);
	}
	
	public long getAcceptableLag() {
		return acceptableLag;
	}
	
	public void setAcceptableLag(long lag) {
		acceptableLag = lag;
	}

	public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey)
	{
		// TODO check server key (SSH connection)
		logger.info("TODO: Checking key from " + hostname + ":" + port);

		/* Should keep a database of already collected keys and always compare */

		return true;
	}

	public void connect() throws IOException {
		connect(host, port);
	}
	
	public void connect(String host, int port) throws IOException {
		this.host = host;
		this.port = port;
		
		// close any previous connection
		if(sshc != null)
			sshc.close();
		
		// initiate new connection
		sshc = new Connection(host, port);
		
		/* Connect and register callback */
		sshc.connect(this);

		/* Authenticate */
		boolean res = false;
		boolean keyfileExists = false;
		try{
			if(keyfile != null)
				keyfileExists = keyfile.exists();
		}catch(Exception e) {
			logger.debug("Error while trying to access " + keyfile);
			logger.debug(e.getMessage());
			logger.debug(e.getStackTrace());
		}
		
		if(keyfileExists) {
			logger.debug("Trying Authentication with public key from file: " + keyfile);
			res =  sshc.authenticateWithPublicKey(userLogin, keyfile, password);
		}else{
			logger.info("You did not provide a key file while this protocol is ssh, is that on purpose?");
		}
		
		if(res == false) {
			logger.debug("Trying Authentication with password ***");
			res = sshc.authenticateWithPassword(userLogin, password);
		}

		if (res == false)
			throw new SSH2Exception("Authentication failed");
	}
	
	public void close() {
		sshc.close();
	}
	
	/**
	 * take a ls command as a string and returns lines of output
	 * The command can be a concatenation of commands, but before
	 * any command is called a cd $path is done. Hence, path must 
	 * be set to an appropriate directory (eventually '.').
	 * eg. cd $path && ls -F
	 * 
	 * Most of the time this methods is called with a path different
	 * than '.'.
	 * 
	 * This method is private and it is to be called from within this class.
	 * The idea is to avoid code duplication here.
	 * @param command
	 * @return
	 * @throws IOException
	 */
	private List<IFile> execls(String path, String command) throws IOException {
		LinkedList<IFile>  files		= new LinkedList<IFile>();

		// get a session
		Session session = sshc.openSession();

		if(path == null)
			path = ".";
		session.execCommand("cd " + path + " && " + command);

		// StreamGobbler consumes the incoming data and avoids blocking.. (faq)
		InputStream stdout = new StreamGobbler(session.getStdout());
		//InputStream stderr = new StreamGobbler(session.getStderr());
		
		// buffering
		BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(stdout));
		//BufferedReader stderrReader = new BufferedReader(new InputStreamReader(stderr));
		
		while (true) {
			String rawListing = stdoutReader.readLine();
			if (rawListing == null) break;
			
			logger.debug("\t\t" + rawListing);
			
			IFile file = MirrorFileFactory.create(path, rawListing, command);
			files.add(file);
		}

		/*
		while (true) {
			String line = stderrReader.readLine();
			if (line == null)
				break;
			System.out.println(line);
		}
		*/
		
		session.close();
		return files;
	}
	

	/**
	 * This method works in a similar fashion with execls()
	 * but instead of grabbing full information it grabs only the files
	 * names and returns them as String.
	 * It can be handy for testing.
	 * execls() can still be used to get the IFiles interface (and the
	 * information included as if we are dealing with a directory or not)
	 * but the IFiles do not contain full information (only their name).
	 * It is not recommanded to use this method but for testing only.
	 */
	public List<String> ls() throws IOException {
		LinkedList<String> filenames	= new LinkedList<String>();

		// get a session
		Session session = sshc.openSession();

		session.execCommand(Shell.COMMAND_LS_F);

		// StreamGobbler consumes the incoming data and avoids blocking.. (faq)
		InputStream stdout = new StreamGobbler(session.getStdout());
		//InputStream stderr = new StreamGobbler(session.getStderr());
		
		// buffering
		BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(stdout));
		//BufferedReader stderrReader = new BufferedReader(new InputStreamReader(stderr));
		
		while (true) {
			String filename = stdoutReader.readLine();
			if (filename == null) break;
			filenames.add(filename);
		}

		/*
		while (true) {
			String line = stderrReader.readLine();
			if (line == null)
				break;
			System.out.println(line);
		}
		*/
		
		session.close();
		return filenames;
	}
	
	public List<IFile> lsFrom(String path) throws IOException {
		//String command = Shell.COMMAND_LS_F;
		String command =  Shell.COMMAND_LS_L;
		logger.debug("\t-> cd " + path + " && " + command);
		return execls(path, command);
	}
	
	/**
	 * Gives the output of ls -F | grep '/'
	 * The result is the list of directories present in the current path (parameter).
	 * This methods saves bandwith by making the server filtering all files first.
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public List<IFile> lsDirFrom(String path) throws IOException {
		String command = Shell.COMMAND_LS_F + " | grep '/'";
		logger.debug("\t-> cd " + path + " && " + command);
		return execls(path, command);
	}


	public List<IFile> lsl() throws IOException {
		logger.debug("\t-> " + Shell.COMMAND_LS_L);
		String path = ".";
		return execls(path, Shell.COMMAND_LS_L);
	}

	public boolean isValidPath(String path) {
		try{
			lsFrom(path);
			return true;
		}catch(IOException ioe) {
			return false;
		}
	}
}

