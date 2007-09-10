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

import edos.mimo.dom.DOMMirrorDelta;
import edos.mimo.dom.ITreeMirrorStructure;
import edos.mimo.dom.TreeMirrorStructure;

import junit.framework.TestCase;

public class DOMMirrorDeltaTest extends TestCase {
	private static ITreeMirrorStructure referenceStruct;
	private static ITreeMirrorStructure newStruct;
	
	// constants: test data
	private static final String DEFAULT_REFERENCE_FILE 	= "test-data/testmachine_ftp-1141113730566.xml";
	private static final String DEFAULT_NEW_FILE		= "test-data/testmachine_ftp-1141114200000.xml";

	protected void setUp() throws Exception {
		super.setUp();
		
		/*
		 * LOADING documents
		 */		
		FileInputStream fin;
		
		fin = new FileInputStream(new File(DEFAULT_REFERENCE_FILE));
		referenceStruct = new TreeMirrorStructure(fin);
		
		fin = new FileInputStream(new File(DEFAULT_NEW_FILE));
		newStruct = new TreeMirrorStructure(fin);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testDeltaGeneration() throws Exception {
		DOMMirrorDelta delta = new DOMMirrorDelta(referenceStruct, newStruct);
		
		assertEquals(delta.countNewFiles(), 1);
		assertEquals(delta.countDeletedFiles(), 1);
		assertEquals(delta.countUpdatedFiles(), 3);
	}

	public void testDeltaPatching() throws Exception {
		DOMMirrorDelta delta = new DOMMirrorDelta(referenceStruct, newStruct);
		delta.save(); // debug
		
		// patching the reference struct we should update it into a new struct	
		FileInputStream fin = new FileInputStream(new File(DEFAULT_REFERENCE_FILE));
		ITreeMirrorStructure struct = new TreeMirrorStructure(fin);
		struct.patch(delta);
		
		// the delta between struct and newStruct should  be empty!
		DOMMirrorDelta verificationDelta = new DOMMirrorDelta(struct, newStruct);
		verificationDelta.save(); // debug
		
		assertEquals(0, verificationDelta.countDeletedFiles());
		assertEquals(0, verificationDelta.countNewFiles());
		assertEquals(0, verificationDelta.countUpdatedFiles());
		
	}

}
