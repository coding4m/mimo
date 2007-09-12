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

import java.io.IOException;

import org.apache.log4j.xml.DOMConfigurator;

import edos.distribution.mirror.HTTPConnection3Test;
import edos.mimo.Access;
import edos.mimo.connection.HTTPConnection3;
import edos.mimo.connection.NetworkConnection;

import junit.framework.TestCase;

public class HTTPConnection3Test extends TestCase {
	private Access access1;
	
	private static final String HOST = "gulus.usherbrooke.ca";
	private static final String PATH = "/pub/distro/Mandrakelinux/devel/cooker/cooker/media/contrib/";

	public static void main(String[] args) {
		junit.textui.TestRunner.run(HTTPConnection3Test.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
		access1 = new Access(HOST, NetworkConnection.PROTOCOL_HTTP, PATH);

		// initializing the logging system
		DOMConfigurator.configure("log4j-config-4testing.xml");
	}
	
	protected void tearDown() throws Exception {
	}
	
	/*
	 * Test method for 'org.edos_project.mirror.connection.HTTPConnection3.ls()'
	 */
	public void testLs() throws IOException {

		HTTPConnection3 conn = new HTTPConnection3(access1);
		
		assertNotNull(conn.ls());
		
		conn.close();

	}

	/*
	 * Test method for 'org.edos_project.mirror.connection.HTTPConnection3.lsl()'
	 */
	public void testLsl() throws IOException {

		HTTPConnection3 conn = new HTTPConnection3(access1);
		
		assertNotNull(conn.lsl());
		
		conn.close();
	}

	/*
	 * Test method for 'org.edos_project.mirror.connection.HTTPConnection3.lsFrom(String)'
	 */
	public void testLsFrom() throws IOException {


		HTTPConnection3 conn = new HTTPConnection3(access1);
		
		assertNotNull(conn.lsFrom("."));
		
		conn.close();
	}

	/*
	 * Test method for 'org.edos_project.mirror.connection.HTTPConnection3.lsDirFrom(String)'
	 */
	public void testLsDirFrom() throws IOException {

		HTTPConnection3 conn = new HTTPConnection3(access1);
		
		assertNotNull(conn.lsDirFrom("."));
		
		conn.close();
	}

	/*
	 * Test method for 'org.edos_project.mirror.connection.HTTPConnection3.executeGet(String)'
	 */
	public void testExecuteGet() throws IOException {
		HTTPConnection3 conn = new HTTPConnection3(access1);
		
		assertNotNull(conn);
		
		//System.out.println(conn.executeGet(""));
	}

}
