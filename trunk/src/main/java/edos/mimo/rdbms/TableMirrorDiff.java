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
package edos.mimo.rdbms; 

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edos.mimo.ISecondaryMirror;
import edos.mimo.filesystem.IFile;
import edos.mimo.filesystem.IFileDiff;
import edos.mimo.filesystem.IMirrorDiff;

/**
 * Basic implementation of <code>IMirrorDiff</code>
 * for a table-based model
 * 
 * @author marc
 *
 */
public class TableMirrorDiff  implements IMirrorDiff {

	private ArrayList<IFile> missingFiles;
	private ArrayList<IFileDiff> diffs;
	private String mirrorFileName;
	
	public TableMirrorDiff(ISecondaryMirror mirror) {
		missingFiles = new ArrayList<IFile>();
		diffs = new ArrayList<IFileDiff>();
		mirrorFileName = mirror.getFilename();
	}

	
	public List<IFile> getMissingFiles() {
		return missingFiles;
	}

	public int countMissingFiles() {
		return missingFiles.size();
	}

	public void addDiff(IFileDiff diff) {
		diffs.add(diff);
	}


	public List<IFileDiff> getFileDiffs() {
		return diffs;
	}


	public String getFileName() {
		return mirrorFileName;
	}


	public void save() throws IOException {
		
	}


	public String getDocumentID() {
		return null;
	}


	public int newerFiles() {
		return 0;
	}


	public int olderFiles() {
		return 0;
	}


	public int corruptedFiles() {
		return 0;
	}


	public int superfluousFiles() {
		return 0;
	}


	public int wrongTypeFiles() {
		return 0;
	}


	public void save(String relativePathToDirectory) throws IOException {
		
	}


	public String getMirrorID() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
