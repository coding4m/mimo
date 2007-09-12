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

import edos.mimo.dom.TreeMirrorStructure;

import junit.framework.TestCase;

public class ConvertPathToFromXPath extends TestCase {
	private TreeMirrorStructure struct;

	protected void setUp() throws Exception {
		super.setUp();
		
		// fake structure ; only good for testing here
		struct = new TreeMirrorStructure(null, null, null, null, null);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		
		struct = null;
	}
	
	/**
	 * 2 situations arise:
	 *  - path ending with / means directory
	 *  - otherwise the path is a file's (non-directory)
	 *
	 */
	public void testXpathConversion() {
		assertEquals("Error computing PATH",
				"/dir1/dir2/file1",
				struct.computePath("/mirror/dir[@name = 'dir1']/dir[@name = 'dir2']/file[@name = 'file1']"));

		assertEquals("Error computing PATH",
				"/dir1/dir2/dir3/",
				struct.computePath("/mirror/dir[@name = 'dir1']/dir[@name = 'dir2']/dir[@name = 'dir3']"));
	}

	/**
	 * 2 situations arise:
	 *  - path ending with / means directory
	 *  - otherwise the path is a file's (non-directory)
	 *
	 */
	public void testPathConversion() {
		assertEquals("Error computing XPATH",
				"/mirror/dir[@name = 'dir1']/dir[@name = 'dir2']/file[@name = 'file1']",
				struct.computeXPath("/dir1/dir2/file1"));
		
		assertEquals("Error computing XPATH",
				"/mirror/dir[@name = 'dir1']/dir[@name = 'dir2']/dir[@name = 'dir3']",
				struct.computeXPath("/dir1/dir2/dir3/"));
	}
}
