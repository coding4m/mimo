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
package edos.mimo.statistics;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import edos.mimo.filesystem.IMirrorDiff;

public class MirrorDiffStatistics implements IMirrorDiffStatistics {
	private static Logger logger = Logger.getLogger(MirrorDiffStatistics.class);
	
	private IMirrorDiff diff;
	
	public MirrorDiffStatistics(IMirrorDiff diff) {
		this.diff = diff;
	}
	
	public int missingFiles() {
		return diff.countMissingFiles();
	}

	public int newerFiles() {
		return diff.newerFiles();
	}

	public int olderFiles() {
		return diff.olderFiles();
	}

	public int corruptedFiles() {
		return diff.corruptedFiles();
	}

	public int superfluousFiles() {
		return diff.superfluousFiles();
	}

	public int wrongTypeFiles() {
		return diff.wrongTypeFiles();
	}

	public int allProblematicFiles() {
		return missingFiles() 
				+ newerFiles()
				+ olderFiles()
				+ corruptedFiles()
				+ superfluousFiles()
				+ wrongTypeFiles();
	}

	/**
	 * Fancy String for console reading
	 * @return big string (multi-line)
	 */
	public String toString() {
		ArrayList<StringBuffer> list = new ArrayList<StringBuffer>();
		list.add(new StringBuffer("| Statistics on ").append(diff.getDocumentID()));
		list.add(new StringBuffer("|").append(format(missingFiles())).append(" missing files"));
		list.add(new StringBuffer("|").append(format(newerFiles())).append(" newer files"));
		list.add(new StringBuffer("|").append(format(olderFiles())).append(" older files"));
		list.add(new StringBuffer("|").append(format(corruptedFiles())).append(" corrupted files"));
		list.add(new StringBuffer("|").append(format(superfluousFiles())).append(" superfluous files (not in the master)"));
		list.add(new StringBuffer("|").append(format(wrongTypeFiles())).append(" files of the wrong type"));
				
		int max = list.get(0).length();
		for(int i=1; i<list.size(); i++)
			if(list.get(i).length() > max)
				max = list.get(i).length();
		int width = max + 2;

		StringBuffer hr = new StringBuffer(horizontalLine(width)).append("\n");
		StringBuffer sb = new StringBuffer(hr)
				.append(list.get(0)).append(" |\n")
				.append(hr)
				.append(list.get(1)).append(spaceFill(width - list.get(1).length() - 1)).append("|\n")
				.append(list.get(2)).append(spaceFill(width - list.get(2).length() - 1)).append("|\n")
				.append(list.get(3)).append(spaceFill(width - list.get(3).length() - 1)).append("|\n")
				.append(list.get(4)).append(spaceFill(width - list.get(4).length() - 1)).append("|\n")
				.append(list.get(5)).append(spaceFill(width - list.get(5).length() - 1)).append("|\n")
				.append(hr);
		
		return sb.toString();
	}
	
	private String format(int number) {
		return String.format("%7d", number);
	}
	
	private String horizontalLine(int size) {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<size; i++)
			sb.append("-");
		return sb.toString();
	}
	
	private String spaceFill(int size) {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<size; i++)
			sb.append(" ");
		return sb.toString();		
	}
}
