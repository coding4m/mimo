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
package edos.mimo.test.persistency;

import java.io.File;
import java.io.FileInputStream;
import java.text.ParseException;

import edos.mimo.Config;
import edos.mimo.IMasterMirror;
import edos.mimo.ISecondaryMirror;
import edos.mimo.dom.DOMDiffGenerator;
import edos.mimo.dom.TreeMirrorStructure;
import edos.mimo.filesystem.DiffGeneratorFactory;
import edos.mimo.filesystem.IMirrorDiff;

import junit.framework.TestCase;

public class DOMMirrorDiffTest extends TestCase {

	private IMasterMirror master = null;
	private ISecondaryMirror mirror = null;
	private static String masterFile = "test-data/testmachine_ssh-1141114107946.xml";
	private static String mirrorFile = "test-data/testmachine_ftp-1141113730566.xml";
	//private static String diffFile	 = "testmachine_ftp-1141113730566-diff.xml";	// expected result
	
	public DOMMirrorDiffTest(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {
		super.setUp();
	
		FileInputStream fin1 = new FileInputStream(new File(masterFile));
		master = (IMasterMirror)new TreeMirrorStructure(fin1).getMirror();
		
		
		FileInputStream fin2 = new FileInputStream(new File(mirrorFile));
		mirror = (ISecondaryMirror)new TreeMirrorStructure(fin2).getMirror();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testDiffGeneration() throws ParseException {
		DOMDiffGenerator gen = (DOMDiffGenerator)DiffGeneratorFactory.getDiffGenerator(Config.TREE, master, mirror);
		IMirrorDiff diff = gen.getDiff();
		
		assertNotNull(diff);
		assertEquals(diff.countMissingFiles(), 1);
		assertEquals(diff.newerFiles(), 1);
		assertEquals(diff.olderFiles(), 1);
		assertEquals(diff.corruptedFiles(), 2);
		assertEquals(diff.superfluousFiles(), 1);
		
	}
	
}
