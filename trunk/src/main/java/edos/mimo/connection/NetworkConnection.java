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

import java.util.List;
import java.io.IOException;

import edos.distribution.mirror.IConnection;
import edos.mimo.Access;
import edos.mimo.Identity;
import edos.mimo.filesystem.IFile;


/**
 * NetworkConnection
 * This class models objects tied to a particular connection by host.
 * It can be made re-usable by calling connect/close alternatively.
 * But it cannot be used to connect to another host.
 * @author marc
 *
 */
public abstract class NetworkConnection implements IConnection {
	protected String host;
	protected int port;
	protected String userLogin;
	protected String password;
	protected String localPath;
	public static int INVALID_PORT = -1;
	public static String INVALID_HOST = "undefined host";
	public static int DEFAULT_PORT_SSH  = 22;	// ssh is working on port 22 (generally)
	public static int DEFAULT_PORT_FTP  = 21;	// idem
	public static int DEFAULT_PORT_HTTP = 80;	// idem
	public static String PROTOCOL_FTP = "ftp";
	public static String PROTOCOL_HTTP = "http";
	public static String PROTOCOL_SSH  = "ssh";

	
	/**
	 * NetworkConnection
	 * Abstract class carrying the minimum to deal with a mirror.
	 * This constructor accepts a Mirror class which hols different access
	 * methods. It's up to the connection to find out one that works and to
	 * use it successfully.
	 * If this method fails after trying all access alternatives, then it
	 * throws a IOException.
	 * @param id
	 * @param mirror
	 */
	public NetworkConnection(Identity id, Access access){
		this.host = access.getHost();
		this.port = access.getPort();
		this.localPath = access.getLocalPath();

		this.userLogin = (id!=null)?id.getLogin():"anonymous";
		this.password = (id!=null)?id.getPassword():"mirror-monitor@edos-project.org";
	}
	
	/**
	 * Network Connection
	 * Useful constructor for protocols not using authentication
	 * (eg. http)
	 * 
	 * @param access
	 */
	public NetworkConnection(Access access) {
		this(new Identity(), access);	// default/fake id
	}

	/**
	 * ls
	 * Performs a naive ls -F and returns the result as a List.
	 * @return
	 * @throws IOException
	 */
	public abstract List<String>      ls() throws IOException;
	public abstract List<IFile>      lsl() throws IOException;
	public abstract List<IFile>      lsFrom(String relPath) throws IOException;
	public abstract List<IFile> lsDirFrom(String relPath) throws IOException;
	
	/**
	 * connect
	 * This method tries to establish the connection to the server.
	 * Throws an exception if it fails.
	 */
	public abstract void connect() throws IOException;
	
	/**
	 * close
	 * This method close the connection if it exists.
	 */
	public abstract void close();
}
