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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.GregorianCalendar;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.dom4j.DocumentException;

import edos.distribution.mirror.StoreDeltaDocument;
import edos.mimo.Config;
import edos.mimo.IMirrorDelta;
import edos.mimo.MonitorApplication;
import edos.mimo.dom.DOMMirrorDelta;
import edos.mimo.dom.db.BDBXMLManager;
import edos.mimo.exception.BDBXMLException;
import edos.mimo.exception.ConfigException;


/**
 * This example shows how to store a delta document 
 * from the file system to the XML database.
 * 
 * @author marc
 *
 */
public class StoreDeltaDocument {
	private static Logger logger = Logger.getLogger(StoreDeltaDocument.class);

	
	public static void main(String[] args) {
		IMirrorDelta delta = null;

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
		
		
		/*
		 * USER INPUT: document name
		 */
		System.out.println("Please enter the name of the delta document you want to store to the database: ");
		Scanner sc = new Scanner(System.in);
		String docName = sc.next();
		
		
		/*
		 * COPYING TO THE DATABASE
		 */
		long startTimeInMillis = new GregorianCalendar().getTimeInMillis();
	
		try {
			FileInputStream fin = new FileInputStream(new File(docName));
			String filenamePathStripped = docName.substring(docName.lastIndexOf("/")+1);
			delta = new DOMMirrorDelta(filenamePathStripped, fin);
			
			// store
			manager.save(delta);
			
		}catch(FileNotFoundException fnfe) {
			System.err.println("Unable to find document: " + docName);
			System.err.println("Please check the path and try again.");

			/*
			 * CLOSING DATABASE
			 * Very important!
			 */
			manager.close();
			logger.info("Database shutdown. Task complete.");
			System.exit(1);
			
		}catch(DocumentException doce) {
			System.err.println("An error happened while parsing the document " + docName);
			System.err.println("Is is a mirror structure? Mission aborted. Please try again");
			doce.printStackTrace();

			/*
			 * CLOSING DATABASE
			 * Very important!
			 */
			manager.close();
			logger.info("Database shutdown. Task complete.");
			System.exit(1);
			
		}catch(BDBXMLException bdbe) {
			logger.fatal("Unable to store " + delta.getFilename());
		}

		/*
		 * Print Status and Execution time
		 */
		double executionTime = (new GregorianCalendar().getTimeInMillis()
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
