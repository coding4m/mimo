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
package edos.mimo.dom;

import org.dom4j.Document;

import edos.mimo.IMirror;
import edos.mimo.IMirrorDelta;
import edos.mimo.IMirrorStructure;

/**
 * Specific Mirror structure interface which is counting on
 * a DOM-based tree implementation.
 * 
 * @author marc
 *
 */
public interface ITreeMirrorStructure extends IMirrorStructure {
	public static final String XPATH		= IMirrorDelta.XPATH;

	public String getFilename();		// unique filename for file system storage
	public String getFilePath();		// filename prefixed by the path defined in Config
	public String getMirrorID();		// unique name for the mirror
	public String getDocumentID();		// unique document name (including timestamp)
	public long getTimeStamp();			// creation time in millis (for uniqueness)

	public IMirror  getMirror();		// get a mirror containing all info
	public Document getDocument();	// returns the structure as a dom4j document
	public boolean isMaster();		// returns true if represents the master structure
	
	// implemented as constructors
	// TODO load(File file) as a constructor
	//public static ITreeMirrorStructure load(File file) throws DocumentException;
	//public ITreeMirrorStructure load(InputStream is) throws DocumentException;
	
	public void patch(IMirrorDelta delta);	// apply the changes specified in delta
}
