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
package edos.mimo.test;

import junit.framework.TestCase;
import java.io.IOException;
import java.util.List;

import edos.mimo.Access;
import edos.mimo.Identity;
import edos.mimo.connection.FTPConnection;


public class FTPConnectionTest extends TestCase {
	private Identity id;
	private Access   access;
	private FTPConnection ftpc1;
	
	protected void setUp() throws Exception {
		super.setUp();
		id = new Identity("anonymous", "edos-project@mandriva.com", "nokeyfile");
		access = new Access("ftp.mandriva.com", "ftp", "/");
		ftpc1 = new FTPConnection(id, access);
	}
	
	protected void tearDown() throws Exception {
		ftpc1.close();
	}
	
	public void testConnect() {
		try {			
			// let's just hope that we have ftp access to mandriva from here...
			ftpc1.connect();
			
		}catch(IOException ioe) {
			assertTrue("Unable to connect to " + access + " with id: " + id, false);
		}
	}
	
	public void testLs() {
		try {
			ftpc1.connect();
			List<String> fileList = ftpc1.ls();
			
			fileList.clear();	// empty list, and remove warning from java...
		}catch(IOException ioe) {
			assertTrue("Unable to execute ls() " + ioe, false);
		}
	}
}
