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
package edos.mimo.examples;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import edos.mimo.Config;
import edos.mimo.IMasterMirror;
import edos.mimo.IMirror;
import edos.mimo.Identity;
import edos.mimo.exception.ConfigException;

/**
 * This class illustrates how to use <code>Config</code>, the class which
 * loads the XML configuration file.
 * 
 * @author marc
 * 
 */
public class ConfigurationReader {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = null;

		try {
			// read the default configuration file (a File or a String can also be passed)
			config = Config.getInstance();
			
			// get a set of default user accounts (which may be used against any mirror..)
			ArrayList<Identity> accounts = config.getProbableAccounts();
			
			Iterator a = accounts.iterator();
			while(a.hasNext()) 
				System.out.println("Default account: " + a.next());
			System.out.println();
			
			
			// get the master mirror with eventually a specific user account to connect to it
			IMasterMirror master = config.getMasterMirror();
			System.out.println("Master mirror: " + master + "\n");
			
			
			
			// get a list of the secondary mirrors to probe
			List<IMirror> mirrors = config.getSecondaryMirrors();
			
			Iterator m = mirrors.iterator();
			while(m.hasNext()) {
				System.out.println("Secondary mirror: " + m.next());				
			}

			/*
			 * Storage format
			 * option 1: customized DOM tree
			 * option 2: SQL tables or object database
			 */
			if(config.getStorageFormat() == Config.TREE)	
				System.out.println("The data will be stored in a DOM tree");
			else if(config.getStorageFormat() == Config.TABLE)
				System.out.println("The date will be stored in a database");
			
			
			/*
			 * Reaching this point means that all operations were successful
			 */
			System.out.println("Exited successfully.");
			
		} catch (ConfigException cfge) {
			System.out.println(cfge.getMessage());

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}

}
