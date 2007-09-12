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

import java.io.File;

public class Identity {
	private String login;
	private String password;
	private String realName;
	private String keyfile;	// file containing the private key
	
	private static final String DEFAULT_LOGIN = "anonymous";
	private static final String DEFAULT_PASSWORD = "mirrormonitor@mandriva.com";
	private static final String DEFAULT_REAL_NAME = "Mirror Monitor";
	private static final String DEFAULT_KEY_FILE = "none";
	
	/**
	 * Builds a fake/empty identity
	 * Useful with some protocols which do not require it.
	 */
	public Identity() {
		this(DEFAULT_LOGIN, DEFAULT_PASSWORD, DEFAULT_KEY_FILE, DEFAULT_REAL_NAME);
	}
	
	public Identity(String name, String password, String keyfile) {
		this(name, password, keyfile, DEFAULT_REAL_NAME);
	}
	
	/**
	 * Identity creates the account information for a connection, including
	 * authentication data.
	 * If the keyfile contains null, a password-based authentication is
	 * assumed. 
	 * Otherwise, the keyfile must be a DSA key file and the password will
	 * help to decrypt it.
	 * @param name
	 * @param password
	 * @param keyfile
	 * @param realName
	 */
	public Identity(String name, String password, String keyfile, String realName) {
		this.login = name;
		
		if(password == null)
			password = DEFAULT_PASSWORD;
		else
			this.password = password;
		
		this.keyfile = keyfile;
		
		if(realName != null)
			this.realName = realName;
		else
			this.realName = DEFAULT_REAL_NAME;
	}
	
	public String getLogin() {
		return login;
	}
	
	public String getPassword() {
		return password;
	}
	
	public String getRealName() {
		return realName;
	}
	
	public File getKeyfile() {
		return (keyfile!=null)?new File(keyfile):null;
	}
	
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(login).append(" with passwd '").append(password)
				.append("' and keyfile ").append(keyfile)
				.append(" (").append(realName).append(")");
		return sb.toString();
	}
	
	
	/**
	 * This method will return true in case the authentication method uses DSA key.
	 * It returns false otherwise, for a password-based authentication process.
	 * @return
	 */
	public boolean useDSA() {
		return keyfile != null;
	}

	/**
	 * Use this in case you need a generic Identity.
	 * The configuration file may contain a user-defined generic identity
	 * which should get the preference of this one.
	 * @return
	 */
	public static Identity getDefault() {
		return new Identity(DEFAULT_LOGIN, DEFAULT_PASSWORD, null, null);
	}

}
