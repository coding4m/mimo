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
package edos.mimo.filesystem;

import java.io.File;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;

/**
 * Spike: good to keep track of XML files on the filesystem?
 * 
 * @deprecated
 * 
 * @author marc
 *
 */
public class XmlCatalog {
	private static Logger logger = Logger.getLogger(XmlCatalog.class);
	
	private static XmlCatalog instance;
	private static Hashtable<String,Vector<String>> diffs; // diffs by mirrorid, and by chronological order
	private static String pathPrefix;
	
	private XmlCatalog(String pathPrefix) {
		if(pathPrefix.endsWith("/"))
			this.pathPrefix = pathPrefix;
		else
			this.pathPrefix = pathPrefix + "/";
	}
	
	/**
	 * Constructs a catalog to locate XML files of interest
	 * @param pathPrefix the location where the XML files are stored
	 * @return
	 */
	public static XmlCatalog getInstance(String pathPrefix) {
		if(instance == null)
			instance = new XmlCatalog(pathPrefix);
		
		return instance;
	}

	
	public void addDiff(String mirrorID, String filename) {
		Vector<String> diffFiles = diffs.get(mirrorID);
		
		diffFiles.add(filename);
	}
	
	public File getLatestDiffFile(String mirrorID) {
		Vector<String> diffFiles = diffs.get(mirrorID);
		return new File(pathPrefix + diffFiles.lastElement());
	}
}
