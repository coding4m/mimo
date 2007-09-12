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

import java.io.IOException;

import edos.distribution.mirror.IFileDiff;
import edos.mimo.AbstractMirror;

public interface IMirrorDiff {

	// node names or column names (constants)
	public static final String MISSING 		= "missing";
	public static final String CORRUPTED	= "corrupted";
	public static final String OLDER		= "older";
	public static final String NEWER		= "newer";
	public static final String SUPERFLUOUS	= "superfluous";
	public static final String WRONG_TYPE	= "wrong-type";
	
	// local constants
	public static final String MIRROR		= AbstractMirror.MIRROR;
	public static final String NAME			= AbstractMirror.NAME;
	public static final String HOST			= AbstractMirror.HOST;
	public static final String PROTOCOL		= AbstractMirror.PROTOCOL;
	public static final String PATH			= AbstractMirror.PATH;
	public static final String TIMESTAMP 	= "timestamp";
	public static final String MASTER_TIMESTAMP = "master-timestamp";
	public static final String EXPECTED_TIMESTAMP  = "expected";
	public static final String EFFECTIVE_TIMESTAMP = "effective";
	
	
	/*
	 * NOT TO BE IMPLEMENTED for the moment
	 * (interesting only for Java processing
	 *  of the output/statistics
	 *  & tabular storage implementation)
	public List<IFile> getMissingFiles();
	public List<IFileDiff> getFileDiffs();
	*/
	
	// statistics
	public int countMissingFiles();
	public int newerFiles();
	public int olderFiles();
	public int corruptedFiles();
	public int superfluousFiles();
	public int wrongTypeFiles();
	
	
	public void addDiff(IFileDiff diff);
	
	public String getFileName();
	
	// persistency
	public void save() throws IOException;	
	public void save(String relativePathToDirectory) throws IOException;
	public String getDocumentID();	// returns a unique ID for the latest diff file (ie not timestamped)
	public String toString();			// returns a string representation of the data
	public String getMirrorID();

	
}
