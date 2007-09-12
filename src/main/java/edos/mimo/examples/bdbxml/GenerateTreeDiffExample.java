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
package edos.mimo.examples.bdbxml;

import java.io.IOException;
import java.text.ParseException;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.dom4j.DocumentException;

import edos.distribution.mirror.GenerateTreeDiffExample;
import edos.mimo.Config;
import edos.mimo.IMasterMirror;
import edos.mimo.ISecondaryMirror;
import edos.mimo.MonitorApplication;
import edos.mimo.dom.DOMDiffGenerator;
import edos.mimo.dom.DOMMirrorDiff;
import edos.mimo.dom.ITreeMirrorStructure;
import edos.mimo.dom.db.BDBXMLManager;
import edos.mimo.exception.BDBXMLException;
import edos.mimo.exception.ConfigException;
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
 * This File is in examples.bdbxml only because it queries the
 * database. Generating a diff has nothing to do with the 
 * database, it only involves mirror structures (one master
 * and one mirror).
 *  
 * @author marc
 *
 */
public class GenerateTreeDiffExample {
	private static Logger logger = Logger.getLogger(GenerateTreeDiffExample.class);
	private static long startTimeInMillis = -1;
	
	private static final String MASTER_ID = "testmachine_ssh";
	private static final String MIRROR_ID = "testmachine_ftp";
	
	
	public static void main(String[] args) {
		IMasterMirror master = null;
		ISecondaryMirror mirror = null;

		// initializing the logging system
		DOMConfigurator.configure("log4j-config-4testing.xml");
		
		// initializing the database
		BDBXMLManager manager = null;
		try {
			Config.getInstance();	// load default configuration from file
			manager = BDBXMLManager.getInstance();
			
		} catch (BDBXMLException e) {
			logger.fatal(e.getStackTrace());
			MonitorApplication.exit(MonitorApplication.ABORT);
			
		} catch (ConfigException e) {
			logger.fatal("Unable to parse the default configuration file!");
			logger.fatal(e.getMessage());
			logger.fatal(e.getStackTrace());
			MonitorApplication.exit(MonitorApplication.ABORT);
		}
		
		
		startTimeInMillis = new GregorianCalendar().getTimeInMillis();
		
		/*
		 * LOADING documents
		 */
		System.out.println("Step 1. loading required structures for testmachine_ssh and testmachine_ftp");
		
		try {
			System.out.println("Loading master..");			
			ITreeMirrorStructure masterS = manager.getLatestStructure(MASTER_ID);
			master = (IMasterMirror)masterS.getMirror();
			
			System.out.println("Loading mirror..");		
			ITreeMirrorStructure mirrorS = manager.getLatestStructure(MIRROR_ID);
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


			MonitorApplication.exit(MonitorApplication.ABORT);
		}

		/*
		 * DIFF generation
		 * 
		 * Warning: SUN JDK 1.5.0_06-b05 crashes here
		 * Workaround: use IBM SDK (ibm-java2-x86_64-50)
		 */
		System.out.println("Step 2. Generate the diff between the 2 (master and mirror respectively)");
		DOMDiffGenerator gen;
		try {
			gen = (DOMDiffGenerator)DiffGeneratorFactory.getDiffGenerator(Config.TREE, master, mirror);
		
		} catch (ParseException e) {
			System.err.println("Error parsing dates in structures; " + e.getMessage());
			e.printStackTrace();
			return;
		}

		IMirrorDiff diff = gen.getDiff();
		double executionTime;
		
		if(diff instanceof DOMMirrorDiff	) {
			try{
				System.out.println("Saving " + diff.getFileName());
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


		/*
		 * CLOSING DATABASE
		 * Very important!
		 */
		manager.close();
		logger.info("Database shutdown. Task complete.");
	}
}
