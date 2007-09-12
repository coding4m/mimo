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
import java.util.List;

import edos.distribution.mirror.ITableMirrorStructure;
import edos.mimo.AnalysisLevel;
import edos.mimo.IMirror;
import edos.mimo.IMirrorDelta;
import edos.mimo.filesystem.IDirectory;

public class TableMirrorStructure implements ITableMirrorStructure {

	private AnalysisLevel analysis;
	private IMirror mirror;
	
	// constants used to generate the file names
	private static final String FILENAME_ENDING = "-structure.xml";
	private static final String FILENAME_VERBOSE = "-complete";
	private static final String FILENAME_COMPLETE = "-filesystem";
	private static final String FILENAME_PARTIAL = "-directory";

	public TableMirrorStructure(IMirror mirror) {
	}

	public List<IDirectory> getTopDirectories() {
		return null;
	}

	public void save() throws IOException {

	}

	public void save(boolean pretty) throws IOException {

	}

	public void patch(IMirrorDelta delta) {
		
	}

	/**
	 * For file-dump
	 * @return descriptive for a file dump in the file system
	 */
	public String getFilename() {
		StringBuffer name = new StringBuffer(mirror.getName());
		
		if(analysis.getVerbosity().equals(AnalysisLevel.VERBOSE))
			name.append(FILENAME_VERBOSE);
		
		if(analysis.getDepth().equals(AnalysisLevel.COMPLETE))
			name.append(FILENAME_COMPLETE);
		else
			name.append(FILENAME_PARTIAL);
		
		name.append(FILENAME_ENDING);
		return name.toString();
	}

}
