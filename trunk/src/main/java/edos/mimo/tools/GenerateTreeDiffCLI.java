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
import java.text.ParseException;
import java.util.GregorianCalendar;
import java.util.Scanner;

import org.apache.log4j.xml.DOMConfigurator;
import org.dom4j.DocumentException;

import edos.mimo.Config;
import edos.mimo.IMasterMirror;
import edos.mimo.ISecondaryMirror;
import edos.mimo.dom.DOMDiffGenerator;
import edos.mimo.dom.DOMMirrorDiff;
import edos.mimo.dom.ITreeMirrorStructure;
import edos.mimo.dom.TreeMirrorStructure;
import edos.mimo.dom.db.BDBXMLManager;
import edos.mimo.exception.BDBXMLException;
import edos.mimo.filesystem.DiffGeneratorFactory;
import edos.mimo.filesystem.IMirrorDiff;
import edos.mimo.statistics.MirrorDiffStatistics;

/**
 * This example works with the tree-based modelization
 * of the file system, with persistency as XML documents
 * stored within the Berkeley DB XML database.
 * 
 * HOW IT WORKS
 * ------------
 * First, 2 XML files are loaded from BDB XML, one
 * representing the master mirror structure, and the other
 * one for a secondary mirror structure. Both of these files
 * are the product of mirror-monitor browsing activity
 * occurred in the past (and stored).
 * 
 * Once these structures are in memory, the diff starts.
 * 
 * Finally, the diff is saved in the filesystem as a
 * XML file (ending with -diff.xml) in the application
 * home dir.
 * 
 * NOTE
 * ----
 * This example is similar to the one in package 
 * examples.filesystem. The only difference resides in where
 * the XML is read from (the filesystem or the Berkeley DB XML).
 *  
 * EXECUTION TIME
 * --------------
 * 
 *  
 * @author marc
 *
 */
public class GenerateTreeDiffCLI {
	//private static Logger logger = Logger.getLogger(GenerateTreeDiffExample.class);
	private static long startTimeInMillis = -1;
	private static IMasterMirror master = null;
	private static ISecondaryMirror mirror = null;
	
	public static void main(String[] args) {
		DOMConfigurator.configure("log4j-config-4testing.xml");
		
		/*
		 * USER INPUT
		 */
		Scanner sc = new Scanner(System.in);
		System.out.println("Diff generator for DOM implementation. Please enter the structure file names.");
		System.out.println("Source is file system(1) or the database(2) [1/2]: ");
		String choice = sc.next();
		
		System.out.println("Master file: ");
		String masterFile = sc.next();
		
		System.out.println("Mirror file: ");
		String mirrorFile = sc.next();
		
		if(choice.equals("1")) {
			loadFromFileSystem(masterFile, mirrorFile);
			
		}else if(choice.equals("2")) {
			loadFromDatabase(masterFile, mirrorFile);
			
		}else{
			System.err.println("Please choose 1 or 2.\nExited.");
			System.exit(1);
		}

		/*
		 * DIFF generation
		 * 
		 * Warning: SUN JDK 1.5.0_06-b05 crashes here
		 * Workaround: use IBM SDK (ibm-java2-x86_64-50)
		 */
		startTimeInMillis = new GregorianCalendar().getTimeInMillis();
		System.out.println("Generating the diff between " + masterFile + " and " + mirrorFile);
		DOMDiffGenerator gen;
		try {
			gen = (DOMDiffGenerator)DiffGeneratorFactory.getDiffGenerator(Config.TREE, master, mirror);
			
		} catch (ParseException e) {
			System.err.println("Error parsing dates in structures: " + e.getMessage());
			e.printStackTrace();
			return;
		}

		IMirrorDiff diff = gen.getDiff();
		double executionTime;
		
		if(diff instanceof DOMMirrorDiff	) {
			try{
				System.out.println("Saving to file system " + diff.getFileName());
				((DOMMirrorDiff)diff).save();
				
			}catch(IOException ioe) {
				System.err.println(ioe.getMessage());
			}
		}
		
		/*
		 * STATISTICS
		 */
		MirrorDiffStatistics stats = new MirrorDiffStatistics(diff);
		System.out.println(stats);
		
		executionTime = (new GregorianCalendar().getTimeInMillis()
							- startTimeInMillis) / 1000.0;
		System.out.println("Done in " + executionTime + " seconds");
		
	}

	private static void loadFromDatabase(String masterFile, String mirrorFile) {	
		BDBXMLManager manager;
		
		/*
		 * LOADING documents
		 */		
		try {
			manager = BDBXMLManager.getInstance();
			
			System.out.println("Loading " + masterFile);			
			ITreeMirrorStructure masterS = new TreeMirrorStructure(manager.getDocument(masterFile));
			master = (IMasterMirror)masterS.getMirror();
			
			System.out.println("Loading " + mirrorFile);		
			ITreeMirrorStructure mirrorS = new TreeMirrorStructure(manager.getDocument(mirrorFile));
			mirror = (ISecondaryMirror)mirrorS.getMirror();
			
			System.out.println("Loading is done!");
			
			
		}catch(DocumentException doce) {
			System.err.println("Unable to load mirror from file...");
			doce.printStackTrace();
			
		}catch(NullPointerException npe) {
			if(master == null || mirror == null) {
				System.err.println("Please ensure you have mirror data in the database first!");
				System.err.println("testmachine/ssh and testmachine/ftp are needed");
			}else
				npe.printStackTrace();

			System.exit(1);
			
		} catch (BDBXMLException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		
		}catch(ClassCastException e) {
			System.out.println("You have to use (master, mirror)");
			System.exit(1);
		}

		
	}

	private static void loadFromFileSystem(String masterFile, String mirrorFile) {		
		/*
		 * LOADING documents
		 */		
		try {
			System.out.println("Loading " + masterFile);	
			FileInputStream fin1 = new FileInputStream(new File(masterFile));
			ITreeMirrorStructure masterS = new TreeMirrorStructure(fin1);
			master = (IMasterMirror)masterS.getMirror();
			
			System.out.println("Loading " + mirrorFile);	
			FileInputStream fin2 = new FileInputStream(new File(mirrorFile));
			ITreeMirrorStructure mirrorS = new TreeMirrorStructure(fin2);
			mirror = (ISecondaryMirror)mirrorS.getMirror();
			
			System.out.println("Loading is done!");
			
			
		}catch(DocumentException doce) {
			System.err.println("Unable to load mirror from file...");
			doce.printStackTrace();
		}catch(NullPointerException npe) {
			if(master == null || mirror == null) {
				System.err.println("Please ensure you have mirror data in the database first!");
				System.err.println("testmachine/ssh and testmachine/ftp are needed");
			}else
				npe.printStackTrace();

			System.exit(1);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}catch(ClassCastException e) {
			System.out.println("You have to use (master, mirror)");
			System.exit(1);
		}
		
	}

	
	
}
