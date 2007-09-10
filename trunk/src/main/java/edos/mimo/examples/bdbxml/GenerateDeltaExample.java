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
import java.util.Collection;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.dom4j.DocumentException;

import edos.distribution.mirror.GenerateDeltaExample;
import edos.mimo.Config;
import edos.mimo.MonitorApplication;
import edos.mimo.dom.DOMMirrorDelta;
import edos.mimo.dom.ITreeMirrorStructure;
import edos.mimo.dom.TreeMirrorStructure;
import edos.mimo.dom.db.BDBXMLManager;
import edos.mimo.exception.BDBXMLException;
import edos.mimo.exception.ConfigException;
import edos.mimo.statistics.MirrorDeltaStatistics;

/**
 * A delta is a difference between 2 historical versions
 * of the same mirror (structure).
 * 
 * This example shows the processing required to generate
 * a delta and what it actually looks like.
 * 
 * @author marc
 *
 */
public class GenerateDeltaExample {
	private static Logger logger = Logger.getLogger(GenerateDeltaExample.class);
	
	
	public static void main(String[] args) {
		ITreeMirrorStructure oldest  = null , latest = null;

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
		
		
		long startTimeInMillis = new GregorianCalendar().getTimeInMillis();
		
		/*
		 * LOADING documents
		 */
		System.out.println("Step 1. loading 2 historical versions of the same document from the database");
		
		try {
			// retrieve all documents from the test data collection
			Collection<String> docs = manager.getDocumentsInChronologicalOrder("testmachine_ftp.dbxml");
			
			// take the latest 2
			Object[] docsArray = docs.toArray();
			int size = docsArray.length;
			if(size < 2) {
				System.err.println("Please make sure you have at least 2 documents "
						+"in container testmachine_ftp.dbxml for this test.");
				System.err.println("Aborted");
				System.exit(1);
			}
			String latestDoc = (String)docsArray[size-1];
			String olderDoc  = (String)docsArray[size-2];
			
			// now retrieve the documents one by one
			
			System.out.println("Loading oldest one..");	
			oldest = new TreeMirrorStructure(manager.getDocument(olderDoc));
				

			System.out.println("Loading newest one..");
			latest = new TreeMirrorStructure(manager.getDocument(latestDoc));
		
			double executionTime = (new GregorianCalendar().getTimeInMillis()
					- startTimeInMillis) / 1000.0;
			System.out.println("Loading done in " + executionTime + " seconds");
			
		}catch(DocumentException doce) {
			System.err.println("Unable to load mirror from file...");
			logger.fatal(doce.getStackTrace());
			//doce.printStackTrace();
			
		} catch (BDBXMLException e) {
			System.err.println(e.getMessage());
			logger.fatal(e.getStackTrace());
			//e.printStackTrace();
		}
		
		/*
		 * DELTA GENERATION
		 */
		System.out.println("Step 2. Generate the delta");
		startTimeInMillis = new GregorianCalendar().getTimeInMillis();
		DOMMirrorDelta delta = new DOMMirrorDelta(oldest, latest);
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

		/*
		 * CLOSING DATABASE
		 * Very important!
		 */
		manager.close();
		logger.info("Database shutdown. Task complete.");
	}
}
