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

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import edos.distribution.mirror.MirrorStatusViaHTTP;
import edos.mimo.Access;
import edos.mimo.Config;
import edos.mimo.ISecondaryMirror;
import edos.mimo.Identity;
import edos.mimo.MirrorFactory;
import edos.mimo.MonitorApplication;
import edos.mimo.dom.ITreeMirrorStructure;
import edos.mimo.dom.db.BDBXMLManager;
import edos.mimo.exception.BDBXMLException;
import edos.mimo.exception.ConfigException;

/**
 * Example showing how a mirror structure can be acquiring
 * through a HTTP connection
 * 
 * @author marc
 *
 */
public class MirrorStatusViaHTTP {
	private static Logger logger = Logger.getLogger(MirrorStatusViaHTTP.class);

	/**
	 * @param args
	 */
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
		
		ISecondaryMirror mirror = null;
		MirrorFactory mirrorFactory = MirrorFactory.getInstance();

		// instantiation : modify the host, protocol and local path to suit your environment
		Access myAccess = new Access("gulus.usherbrooke.ca", "http", "/pub/distro/Mandrakelinux/devel/cooker/cooker/media/contrib/");
		Identity id = new Identity("anonymous", "mirrormonitor@mandriva.com", null, "The mirror Monitor program"); 
		
		//to get a master (say for testing) uncomment the next line
		// mirror = mirrorFactory.getMaster("my local FTP site", myAccess, id);
		mirror 	= mirrorFactory.getSecondary("Sample HTTP site", myAccess, id);
		
		// see this function to get a complete model of the mirror file system
		System.out.println("Acquiring a complete representation of " + mirror);
		


		/*
		 * connection and processing
		 */
		try{
			mirror.connect();
			
			
			mirror.acquire();		// browse the physical mirror
			System.out.println("Download completed in "
					+ mirror.getDownloadDelayInMillis() / 1000.0 
					+ "seconds.");
			
			// data manipulation, reports
			ITreeMirrorStructure mirrorStructure = (ITreeMirrorStructure)mirror.getStructure();
		
			// uncomment the next line to get a (huge!) string on the console
			//System.out.println("XML representation: " + mirrorStructure);
			
			// persistence
			manager.save(mirrorStructure);

			System.out.println("Exited successfully");
			
		}catch(IOException ioe) {
			logger.fatal(ioe.getMessage());
			
		}finally{
			if (mirror != null)
				mirror.disconnect();
			
			/*
			 * CLOSING DATABASE
			 * Very important!
			 */
			manager.close();
			logger.info("Database shutdown. Task complete.");
		}


	}


}
