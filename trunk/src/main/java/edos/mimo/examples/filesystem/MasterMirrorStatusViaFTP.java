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
package edos.mimo.examples.filesystem;

import java.io.IOException;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;

import edos.distribution.mirror.MasterMirrorStatusViaFTP;
import edos.mimo.Access;
import edos.mimo.IMasterMirror;
import edos.mimo.Identity;
import edos.mimo.MirrorFactory;
import edos.mimo.dom.ITreeMirrorStructure;

/**
 * Sample session with a Master Mirror.
 * 
 * This applies to a FTP accessible mirror only.
 * 
 * @author marc
 *
 */
public class MasterMirrorStatusViaFTP {
	private static Logger logger = Logger.getLogger(MasterMirrorStatusViaFTP.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		IMasterMirror mirror = null;
		MirrorFactory mirrorFactory = MirrorFactory.getInstance();

		// instantiation : modify the host, protocol and local path to suit your environment
		Access myAccess = new Access("testmachine", "ftp", "/cooker/cooker");
		//Access myAccess = new Access("testmachine", "ftp", "/cooker/cooker/media"); // for a shorter branch (faster!)
		Identity id = new Identity("anonymous", "mirrormonitor@mandriva.com", null, "The mirror Monitor program"); 
		
		mirror = mirrorFactory.getMaster("my local FTP site", myAccess, id);
		
		
		// see this function to get a complete model of the mirror file system
		System.out.println("Acquiring a complete representation of " + mirror);
		


		/*
		 * connection and processing
		 */
		try{
			mirror.connect();
			
			mirror.setCheckinTime(GregorianCalendar.getInstance().getTimeInMillis());
			mirror.acquire();		// browse the physical mirror
			mirror.setCheckoutTime(GregorianCalendar.getInstance().getTimeInMillis());
			System.out.println("Download completed in "
					+ mirror.getDownloadDelayInMillis() / 1000.0 
					+ "seconds.");
			
			// data manipulation, reports
			ITreeMirrorStructure mirrorStructure = (ITreeMirrorStructure)mirror.getStructure();
		
			// uncomment the next line to get a (huge!) string on the console
			//System.out.println("XML representation: " + mirrorStructure);
			
			// persistence
			// (expected size ~2MB; expected time ~1min/MB to write the file to disk)
			//mirrorStructure.save();
			mirrorStructure.save(true);	// pretty print (skip lines which makes it easier to load in an editor)

			logger.info("Exited successfully");
			
		}catch(IOException ioe) {
			logger.error(ioe.getMessage());
			logger.error("Exited");
			
		}finally{
			if (mirror != null)
				mirror.disconnect();
		}
		
	}

}
