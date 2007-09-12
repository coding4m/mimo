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
package edos.mimo.dal;

import org.apache.log4j.Logger;

public class DatabaseXML implements IDatabase {
	private static DatabaseXML instance = null;
	private static Logger logger = Logger.getLogger(DatabaseXML.class); 
	
	private DatabaseXML() {
		
		try {
			init();
			
		}catch(Exception e) {
			logger.fatal("Unable to start  XML database");
			logger.fatal(e.getStackTrace());
		}
	}
	
	public static DatabaseXML getInstance() {
		if(instance == null)
			instance = new DatabaseXML();
		
		return instance;
	}
	
	/**
	 * Startup the XML database
	 *
	 */
	private void init() throws ClassNotFoundException,
		InstantiationException, IllegalAccessException {
	}
	
	/**
	 * Shutdown the database
	 * This method MUST be called before exiting!
	 */
	public static void shutdown() {
		if(instance == null)
			return; // do nothing if does not exist yet
		
	}
}
