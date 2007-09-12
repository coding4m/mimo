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
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.GregorianCalendar;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import edos.distribution.mirror.ExtractDocument;
import edos.mimo.Config;
import edos.mimo.MonitorApplication;
import edos.mimo.dom.db.BDBXMLManager;
import edos.mimo.exception.BDBXMLException;
import edos.mimo.exception.ConfigException;

/**
 * This example shows how a document is extracted from the database
 * and save in the file system as a regular xml file.
 * 
 * 
 * @author marc
 *
 */
public class ExtractDocument {
	private static Logger logger = Logger.getLogger(ExtractDocument.class);

	
	public static void main(String[] args) {

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
		System.out.println("Please enter the name of the document you want to extract from the database: ");
		Scanner sc = new Scanner(System.in);
		String docName = sc.next();
		
		/*
		 * COPYING FROM THE DATABASE
		 */
		long startTimeInMillis = new GregorianCalendar().getTimeInMillis();
		
		/* slower than fetching a String
		InputStream is = (InputStream)manager.getDocumentAsInputStream(docName);
		if(is == null) {
			System.out.println("Unable to find " + docName + " inside the database");
			System.exit(1);
		}
		
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		String line = null;
		*/
		
		PrintWriter out = null;
		
		try {
			out = new PrintWriter(new File(docName));
			
			/* slower than fetching a String
			while((line = in.readLine()) != null) {
				out.print(line);
			}
			*/
			out.print(manager.getDocument(docName));
			
		}catch(FileNotFoundException fnfe) {
			// very unlikely!
			fnfe.printStackTrace();
			
		/*
		}catch(IOException ioe) { // only when requesting an InputStream
			System.err.println("An error happened while copying the document " + docName 
					+ " from the database to the file system");
		*/
		} catch (BDBXMLException e) {
			System.err.println(e.getMessage());
			//e.printStackTrace();
		}finally{
			out.close();
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
