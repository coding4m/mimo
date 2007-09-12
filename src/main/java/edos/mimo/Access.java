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
package edos.mimo;

/**
 * @author marc
 *
 */
public class Access implements Comparable<Access> {
	private String host;			// host part of the url, eg. www.mandriva.com
	private String protocol; 		// either http, rsync, ftp for now
	private String localPath;		// eg. to perform 'cd localPath'
	private int port;				// port number (depends on protocol)
	private int maxLag = DEFAULT_MAX_LAG;		// 3 seconds as a default
	public static int DEFAULT_SSH_PORT = 22;
	public static int DEFAULT_FTP_PORT = 21;
	public static int DEFAULT_HTTP_PORT = 80;
	public static int DEFAULT_MAX_LAG   = 3000; 
	public static String PROTOCOL_SSH = "ssh2";
	public static String PROTOCOL_FTP = "ftp";
	public static String PROTOCOL_HTTP = "http";
	
	public Access(String host, String protocol, String path) {
		this.host = host;
		this.protocol = protocol;
		this.localPath = path;
		if(protocol.equals("ssh"))
			this.port = DEFAULT_SSH_PORT;
		else if(protocol.equals("ftp"))
			this.port = DEFAULT_FTP_PORT;
		else if(protocol.equals("http"))
			this.port = DEFAULT_HTTP_PORT;
	}
	
	public Access(String host, String protocol, String path, int port) {
		this.host = host;
		this.protocol = protocol;
		this.localPath = path;
		this.port = port;
	}
	
	/**
	 * @return local path
	 */
	public String getLocalPath() {
		return localPath;
	}
	
	/**
	 * @param localPath
	 */
	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}
	
	/**
	 * @return protocol
	 */
	public String getProtocol() {
		return protocol;
	}
	
	/**
	 * @param protocol
	 */
	public void setProtocol(String supportedProtocol) {
		this.protocol = supportedProtocol;
	}

	/**
	 * @return port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * How much lag can be waited for as a maximum.
	 * @return maxLag after which the connection is to time out
	 */
	public int getAcceptableLag() {
		return maxLag;
	}

	/**
	 * Sets the amount of time a lag is acceptable before 
	 * considering the connection dead.
	 * @param maxLag after which the connection is to time out
	 */
	public void setMaxLag(int maxLag) {
		this.maxLag = maxLag;
	}
	
	/**
	 * @param port
	 */
	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}
	
	public String toString() {
		return protocol + "://" + host + localPath;
	}

	/**
	 * Required method to implement the Comparable interface.
	 * The <code>Access</code>es can be sorted and checked against
	 * duplication.
	 * @param a
	 * @return
	 */
	public int compareTo(Access a) {
		if(this.host.equals(a.getHost()) 
				&& this.localPath.equals(a.getLocalPath() )
				&& this.protocol.equals(a.getProtocol()))
			return 0;	// equal!
		else 
			return this.host.compareTo(a.getHost());
	}
	
	/**
	 * This class has a compareTo() method which is consistent with
	 * equals().
	 * @param a
	 * @return
	 */
	public boolean equals(Access a) {
		return (compareTo(a) == 0)?true:false;
	}
}
