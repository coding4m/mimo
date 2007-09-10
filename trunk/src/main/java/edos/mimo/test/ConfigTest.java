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
import java.util.List;
import java.util.Iterator;

import edos.mimo.Access;
import edos.mimo.Config;
import edos.mimo.Identity;

public class ConfigTest extends TestCase {
	private Config config;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		config = new Config("src/edos/distribution/mirror/test/mirrormetrics-config.xml");
	}
	
	public void testDefaultUser() {
		List<Identity> users = config.getProbableAccounts();
		Iterator accounts = users.iterator();
		
		// here we test only 1 default user account
		Identity user = (Identity)accounts.next();
		
		assertEquals("anonymous", user.getLogin());
		assertEquals("Anonymous User", user.getRealName());
		assertEquals("edos-project@mandriva.com", user.getPassword());
	}
	
	/**
	 * Test the probableAccounts parsing, that is to say the ability to
	 * parse the set of default accounts.
	 */
	public void testMultipleDefaultUsers() {
		List<Identity> users = config.getProbableAccounts();
		Iterator accounts = users.iterator();
		
		// gets all the elements and nothing more
		assertEquals("Found one account more or one less", 3, users.size());
		
		
		// user 1
		assertTrue("There should be one account more", accounts.hasNext());
		Identity user = (Identity)accounts.next();
		
		assertEquals("anonymous", user.getLogin());
		assertEquals("Anonymous User", user.getRealName());
		assertEquals("edos-project@mandriva.com", user.getPassword());
		
		
		// user 2
		assertTrue("There should be one account more", accounts.hasNext());
		user = (Identity)accounts.next();
		
		assertEquals("Linus", user.getLogin());
		assertEquals("Linus Thornvald", user.getRealName());
		assertEquals("", user.getPassword());
		assertNotNull(user.getKeyfile());
		

		// user 3
		assertTrue("There should be one account more", accounts.hasNext());
		user = (Identity)accounts.next();
		
		assertEquals("anonymous", user.getLogin());
		assertEquals("Mirror Monitor", user.getRealName());
		assertEquals("", user.getPassword());
		assertNull(user.getKeyfile());
		
	}
	
	public void testMirrorListing() {
		List<Access> accessList = config.getMirrorAccessList();
		
		// naive test
		assertEquals("Error parsing the access list nodes", 1, accessList.size());
	}
	
	public void testStorageFormat() {
		int format = config.getStorageFormat();
		
		assertEquals("The value returned is not defined", true, 
				format == Config.TABLE || format == Config.TREE);
	}
}
