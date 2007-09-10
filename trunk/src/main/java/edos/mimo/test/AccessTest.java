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

import edos.mimo.Access;

import junit.framework.TestCase;


public class AccessTest extends TestCase {

	private Access access1;
	//private Access access2;
	
	protected void setUp() throws Exception {
		super.setUp();
		
		access1 = new Access("ftp.mandriva.com", "ftp", "/test");
		//access2 = new Access("kenobi.mandriva.com", "ssh", "/test", 22);
	}

	public void testCreate() {
		Access access = new Access("x", "y", "z");
		assertNotNull(access);
	}
	
	public void testExist() {
		assertNotNull(access1);
	}
	
	public void testGet() {
		String host = access1.getHost();
		String protocol = access1.getProtocol();
		String path = access1.getLocalPath();
		int port = access1.getPort();
		
		assertEquals(host, "ftp.mandriva.com");
		assertEquals(protocol, "ftp");
		assertEquals(path, "/test");
		assertEquals(port, 21);		
	}
}
