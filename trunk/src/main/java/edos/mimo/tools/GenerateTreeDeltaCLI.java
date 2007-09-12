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
package edos.mimo.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.Scanner;

import org.apache.log4j.xml.DOMConfigurator;
import org.dom4j.DocumentException;

import edos.mimo.dom.DOMMirrorDelta;
import edos.mimo.dom.ITreeMirrorStructure;
import edos.mimo.dom.TreeMirrorStructure;
import edos.mimo.dom.db.BDBXMLManager;
import edos.mimo.exception.BDBXMLException;
import edos.mimo.statistics.MirrorDeltaStatistics;


public class GenerateTreeDeltaCLI {
	//private static Logger logger = Logger.getLogger(GenerateTreeDeltaCLI.class);
	private static long startTimeInMillis = -1;
	private static ITreeMirrorStructure referenceStruct;
	private static ITreeMirrorStructure newStruct;
	
	// constants: test data
	private static final String DEFAULT_REFERENCE_FILE 	= "test-data/testmachine_ftp-1141113730566.xml";
	private static final String DEFAULT_NEW_FILE		= "test-data/testmachine_ftp-1141114200000.xml";

	public static void main(String[] args) {
		DOMConfigurator.configure("log4j-config-4testing.xml");
		
		/*
		 * USER INPUT
		 */
		Scanner sc = new Scanner(System.in);
		System.out.println("Delta generator for DOM implementation. Please enter the structure file names.");
		System.out.println("Source is file system(1) or the database(2) [default=1]: ");
		String choice = sc.nextLine();
		if(choice.equals(""))
			choice = "1";
		
		System.out.println("Reference file [default=" + DEFAULT_REFERENCE_FILE + "]: ");
		String referenceFile = sc.nextLine();
		if(referenceFile.equals(""))
			referenceFile = DEFAULT_REFERENCE_FILE;
		
		System.out.println("New file (to be compared)[default=" + DEFAULT_NEW_FILE + "]: ");
		String newFile = sc.nextLine();
		if(newFile.equals(""))
			newFile = DEFAULT_NEW_FILE;
		
		if(choice.equals("1")) {
			loadFromFileSystem(referenceFile, newFile);
			
		}else if(choice.equals("2")) {
			loadFromDatabase(referenceFile, newFile);
			
		}else{
			System.err.println("Please choose 1 or 2.\nExited.");
			System.exit(1);
		}


		/*
		 * DELTA GENERATION
		 */
		System.out.println("Step 2. Generate the delta");
		startTimeInMillis = new GregorianCalendar().getTimeInMillis();
		DOMMirrorDelta delta = new DOMMirrorDelta(referenceStruct, newStruct);
		try {
			delta.save();
			
		}catch(IOException ioe) {
			System.err.println("Unable to save " + delta.getFileName());
		}

		double executionTime = (new GregorianCalendar().getTimeInMillis()
					- startTimeInMillis) / 1000.0;
		System.out.println("Delta done in " + executionTime + " seconds");
		
		/*
		 * STATISTICS
		 */
		MirrorDeltaStatistics stats = new MirrorDeltaStatistics(delta);
		System.out.println(stats);
	}

	private static void loadFromDatabase(String refFile, String newFile) {	
		BDBXMLManager manager;
		
		/*
		 * LOADING documents
		 */		
		try {
			manager = BDBXMLManager.getInstance();
			
			System.out.println("Loading " + refFile);			
			referenceStruct = new TreeMirrorStructure(manager.getDocument(refFile));
			
			System.out.println("Loading " + newFile);		
			newStruct = new TreeMirrorStructure(manager.getDocument(newFile));
			
			System.out.println("Loading is done!");
			
			
		}catch(DocumentException doce) {
			System.err.println("Unable to load mirror from file...");
			doce.printStackTrace();
			
		}catch (BDBXMLException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		
		}
	}

	private static void loadFromFileSystem(String refFile, String newFile) {		
		/*
		 * LOADING documents
		 */		
		try {
			System.out.println("Loading " + refFile);	
			FileInputStream fin1 = new FileInputStream(new File(refFile));
			referenceStruct = new TreeMirrorStructure(fin1);
			
			System.out.println("Loading " + newFile);	
			FileInputStream fin2 = new FileInputStream(new File(newFile));
			newStruct = new TreeMirrorStructure(fin2);
			
			System.out.println("Loading is done!");
			
			
		}catch(DocumentException doce) {
			System.err.println("Unable to load mirror from file...");
			doce.printStackTrace();
		}catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	
}
