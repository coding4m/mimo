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

import edos.distribution.mirror.GenerateDeltaExample;
import edos.mimo.Config;
import edos.mimo.MonitorApplication;
import edos.mimo.dom.db.BDBXMLManager;
import edos.mimo.exception.BDBXMLException;
import edos.mimo.exception.ConfigException;

/**
 * Gives the possibility to run any XQUERY within the database.
 * 
 * Note:
 * oXygenXML Editor allows to do that (not free software, but 
 * excellent one)
 * 
 * @author marc
 *
 */
public class RunXQUERY {
	private static Logger logger = Logger.getLogger(GenerateDeltaExample.class);
	
	// Eclipse crashes when trying to print big strings on a view, this defines a limit
	private static final int MAX_SIZE = 2000;
	
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
		 * GET THE XQUERY from the user
		 */
		String xquery = "";
		String line;
		Scanner sc = new Scanner(System.in);
		System.out.println("Enter your XQUERY (blank line to escape): ");
		do{
			line = sc.nextLine(); 
			xquery += line + "\n";
		}while(!line.equals(""));
		
		System.out.println("Querying: \n" + xquery)	;
		
		/*
		 * RUN THE QUERY
		 */
		long startTimeInMillis = new GregorianCalendar().getTimeInMillis();
		
		System.out.println("Result:");
		String result;
		try {
			result = manager.query(xquery);

			if(result.length() > MAX_SIZE) {
				File file = new File("xquery-output-" + startTimeInMillis + ".xml");
				PrintWriter out;
				try {
					out = new PrintWriter(file);
					out.print(result);
					out.close();
					System.out.println("The result as been piped to " + file.getName() + "\n");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}finally{
					manager.close();
				}
			}else
				System.out.println(result);
			
		} catch (BDBXMLException e1) {
			logger.fatal(e1.getMessage());
			logger.fatal(e1.getStackTrace());
		}

		
		/*
		 * STATISTICS
		 */
		
		double executionTime = (new GregorianCalendar().getTimeInMillis()
					- startTimeInMillis) / 1000.0;
		System.out.println("Delta done in " + executionTime + " seconds");
		
		
		/*
		 * CLOSING DATABASE
		 * Very important!
		 */
		manager.close();
		logger.info("Database shutdown. Task complete.");
		
	}

}
