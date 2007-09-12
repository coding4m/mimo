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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;

import edos.mimo.dom.DOMMirrorDelta;
import edos.mimo.dom.ITreeMirrorStructure;
import edos.mimo.dom.TreeMirrorStructure;
import junit.framework.TestCase;

public class testDOMMirrorDelta extends TestCase {
	
	private ITreeMirrorStructure first, second, third;
	//private String deltaNoChange, deltaAdded2dirs, deltaRemoved2Dirs;
	private DOMMirrorDelta deltaNoChange, deltaAdded2dirs, deltaRemoved2Dirs;
	
	@Override
	protected void setUp() throws Exception {
		/*
		 * Structures (import fake from file)
		 */
		FileInputStream fin1 = new FileInputStream(
					new File("test-data/delta/master-1162071113939.xml"));
		first = new TreeMirrorStructure(fin1);	

		FileInputStream fin2 = new FileInputStream(
					new File("test-data/delta/master-with-added-dirs.xml"));
		second = new TreeMirrorStructure(fin2);	

		FileInputStream fin3 = new FileInputStream(
					new File("test-data/delta/master-with-removed-dirs.xml"));
		third = new TreeMirrorStructure(fin3);	
		
		/*
		 * Delta results (import expected results from file)
		 */
		// a string approach can work
		//deltaNoChange = loadStringFromFile("test-data/delta/master-1162071136241-delta.xml");
		deltaNoChange = new DOMMirrorDelta("master-1162071136241-delta.xml",
							new FileInputStream("test-data/delta/master-1162071136241-delta.xml"));
		
		
		deltaAdded2dirs = new DOMMirrorDelta("master-1162071375775-delta.xml",
				new FileInputStream("test-data/delta/master-1162071375775-delta.xml"));
		
		deltaRemoved2Dirs = new DOMMirrorDelta("master-1162071495883-delta.xml",
				new FileInputStream("test-data/delta/master-1162071495883-delta.xml"));
		
		
	}
	
	private String loadStringFromFile(String filename) throws IOException {
		StringBuffer sb = new StringBuffer();
		BufferedReader in = new BufferedReader(new FileReader(filename));
		String line;
		while((line = in.readLine()) != null)
			sb.append(line).append("\n");
		return sb.toString();
	}

	private String compact(String s) {
		return s.replaceAll( "[\n ]", "");
	}
	
	@Override
	protected void tearDown() throws Exception {
	}

	public void testDeltaGenerationNoChange() {
		DOMMirrorDelta delta = new DOMMirrorDelta(first, first);
		assertNotSame(deltaNoChange.toString(), delta.toString()); // date only should differ
		assertEquals(deltaNoChange.toString().replace(deltaNoChange.getTimestamp(), delta.getTimestamp()), delta.toString());
	}
	
	public void testDeltaGenerationAdd2Dirs() {
		DOMMirrorDelta delta = new DOMMirrorDelta(first, second);
		assertNotSame(deltaNoChange.toString(), delta.toString()); 
		assertNotSame(deltaNoChange.toString().replace(deltaNoChange.getTimestamp(), delta.getTimestamp()), delta.toString());

		assertEquals(compact(deltaAdded2dirs.toString().replace(deltaAdded2dirs.getTimestamp(), delta.getTimestamp())), 
				compact(delta.toString()));
	}
	
	public void testDeltaGenerationRemoved2Dirs() {
		DOMMirrorDelta delta = new DOMMirrorDelta(second, third);

		assertEquals(compact(deltaRemoved2Dirs.toString().replace(deltaRemoved2Dirs.getTimestamp(), delta.getTimestamp())), 
				compact(delta.toString()));
	}
	
}
