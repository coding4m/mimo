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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import edos.distribution.mirror.ListDocuments;
import edos.mimo.Config;
import edos.mimo.MonitorApplication;
import edos.mimo.dom.db.BDBXMLManager;
import edos.mimo.exception.BDBXMLException;
import edos.mimo.exception.ConfigException;


/**
 * In this example, we show how to list the documents
 * stored in the database.
 * 
 * @author marc
 *
 */
public class ListDocuments {
	private static Logger logger = Logger.getLogger(ListDocuments.class);

	public static void main(String args[])	 {

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
		 * CONTAINERS LIST
		 */
		System.out.println("List of containers (by mirror access):");
		System.out.println("--------------------------------------");
		List<String> containers = manager.getContainers();
		ListIterator<String> listIt = containers.listIterator();
		while(listIt.hasNext()) {
			System.out.println("\t- " + listIt.next());
		}
		System.out.println("");
		
		/*
		 * DOCUMENT LIST
		 */
		listIt = containers.listIterator();
		while(listIt.hasNext()) {
			String container = listIt.next();
			System.out.println("--------------------------------------------------");
			System.out.println("Documents in " + container );
			
			/* ------------ Start options --------------- */
			// OPTION 1: Unsorted list of documents
			//List<String> docs = manager.getDocuments(container);
			//ListIterator<String> docsIt = docs.listIterator();
			
			// OPTION 2: sorted collection documents in chronological order
			Collection<String> docs = manager.getDocumentsInChronologicalOrder(container);
			if(docs.size() < 1) {
				System.out.println("\t<empty>\n");
				continue;
			}
			Iterator<String> docsIt = docs.iterator();
			/* ------------ Ed options --------------- */
			
			while(docsIt.hasNext()) {
				String xmlDoc = docsIt.next();
				String type = manager.getMirrorType(container, xmlDoc);
				System.out.println("\t- " + xmlDoc + "\t("  + type + ")");				
			}

			System.out.println("");
			
		}
		

		/*
		 * CLOSING DATABASE
		 * Very important!
		 */
		manager.close();
		logger.info("Database shutdown. Task complete.");
	}

}
