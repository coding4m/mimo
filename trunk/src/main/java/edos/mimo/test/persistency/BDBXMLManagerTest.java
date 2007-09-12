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
import java.util.Collection;

import org.apache.log4j.xml.DOMConfigurator;

import edos.mimo.Config;
import edos.mimo.IMirrorDelta;
import edos.mimo.dom.DOMMirrorDelta;
import edos.mimo.dom.ITreeMirrorStructure;
import edos.mimo.dom.TreeMirrorStructure;
import edos.mimo.dom.db.BDBXMLManager;

import junit.framework.TestCase;

public class BDBXMLManagerTest extends TestCase {

	private BDBXMLManager manager;
	private static String docName = "testmachine_ftp-1141113730566.xml";
	private static String deltaName = "testmachine_ftp-1141113730566-delta.xml";
	private static String path = "test-data/";
	private static String containerName = "testmachine_ftp.dbxml";
	
	protected void setUp() throws Exception {
		super.setUp();
		

		DOMConfigurator.configure("log4j-config-4testing.xml");
		Config.getInstance();
		manager = BDBXMLManager.getInstance();
		//BDBXMLManager.debug(true);
		manager.deleteDocument(docName);
		manager.deleteDocument(deltaName);
	}

	protected void tearDown() throws Exception {
		super.tearDown();

		// Database
		if(manager != null) manager.close();	// VERY IMPORTANT!
		
	}

	public void testAddStructureRemoveDocument() throws Exception {

		FileInputStream fin = new FileInputStream(new File(path + docName));
		ITreeMirrorStructure struct = new TreeMirrorStructure(fin);
		
		// store
		manager.save(struct);		
		
		// is it there?	
		//Collection docNames = manager.getDocuments(containerName);		
		Collection docNames = manager.getDocumentsInChronologicalOrder(containerName);
		assertTrue(docNames.contains(docName));
		
		// remove it
		manager.deleteDocument(docName);	
		
		// is it there?	
		//docNames = manager.getDocuments(containerName);
		docNames = manager.getDocumentsInChronologicalOrder(containerName);
		assertFalse(docNames.contains(docName));	
	}
	
	public void testAddRemoveDeltaDocument() throws Exception {

		FileInputStream fin = new FileInputStream(new File(path + deltaName));
		IMirrorDelta delta = new DOMMirrorDelta(deltaName, fin);
		
		// store
		manager.save(delta);
		

		// is it there?	
		//Collection docNames = manager.getDocuments(containerName);		
		Collection docNames = manager.getDocumentsInChronologicalOrder(containerName);
		assertTrue(docNames.contains(deltaName));
		
		// remove it
		manager.deleteDocument(deltaName);	
		
		// is it there?	
		//docNames = manager.getDocuments(containerName);
		docNames = manager.getDocumentsInChronologicalOrder(containerName);
		assertFalse(docNames.contains(deltaName));	
	}
}
