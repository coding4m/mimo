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

import java.io.PrintWriter;

import edos.mimo.Access;
import edos.mimo.MirrorAccessList;

import junit.framework.TestCase;


public class MirrorAccessListTest extends TestCase {
	private MirrorAccessList mirrors;
	private Access access;

	protected void setUp() throws Exception {
		super.setUp();
		
		//url = "http://www1.mandrivalinux.com/mirrorsfull.list";
		access = new Access("www1.mandrivalinux.com", "http", "/mirrorsfull.list");
	}

	/*
	 * Test method for 'net.iedos.metrics.mirroring.MirrorAccessList.MirrorAccessList(String)'
	 */
	public void testMirrorAccessListFromURL() {
		mirrors = new MirrorAccessList(access);
		mirrors.print(new PrintWriter(System.out));
		
		assertNotNull("Unable to download the payload", mirrors.getRawList());
		assertNotNull("Unable to parse the mirror list from " + access, 
							mirrors.get());
	}
}
