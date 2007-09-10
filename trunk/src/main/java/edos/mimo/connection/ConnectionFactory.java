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

import edos.distribution.mirror.ConnectionFactory;
import edos.distribution.mirror.FTPConnection;
import edos.distribution.mirror.HTTPConnection3;
import edos.distribution.mirror.IConnection;
import edos.distribution.mirror.IConnectionFactory;
import edos.distribution.mirror.NetworkConnection;
import edos.distribution.mirror.SSH2Connection;
import edos.mimo.Access;
import edos.mimo.IPass;
import edos.mimo.Identity;


public class ConnectionFactory implements IConnectionFactory {
	private static ConnectionFactory instance;

	private ConnectionFactory() {
		
	}
	
	public static IConnectionFactory getInstance() {
		if(instance == null)
			instance = new ConnectionFactory();
		return instance;
	}
	
	public IConnection getConnection(IPass pass) throws IOException {
		Access access = pass.getAccess();
		Identity id   = pass.getIdentity();
		IConnection conn;

		if(access.getProtocol().equals(NetworkConnection.PROTOCOL_FTP)) {
			conn = new FTPConnection(pass);
			
		}else if(access.getProtocol().equals(NetworkConnection.PROTOCOL_HTTP)) {
			conn = new HTTPConnection3(access);
			
		}else if(access.getProtocol().equals(NetworkConnection.PROTOCOL_SSH)) {
			conn = new SSH2Connection(id, access.getHost(), access.getPort());
			
		}else{
			throw new IOException("Protocol unsupported: " + access.getProtocol());
		}		
		
		return conn;
	}

}
