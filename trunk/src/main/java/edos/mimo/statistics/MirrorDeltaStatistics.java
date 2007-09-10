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

import edos.distribution.mirror.IMirrorDeltaStatistics;
import edos.distribution.mirror.MirrorDeltaStatistics;
import edos.mimo.IMirrorDelta;

public class MirrorDeltaStatistics implements IMirrorDeltaStatistics {
	private static Logger logger = Logger.getLogger(MirrorDeltaStatistics.class);
	
	private IMirrorDelta delta;
	
	public MirrorDeltaStatistics(IMirrorDelta delta) {
		this.delta = delta;
	}
	
	public int deletedFiles() {
		return delta.countDeletedFiles();
	}

	public int newFiles() {
		return delta.countNewFiles();
	}

	public int updatedFiles() {
		return delta.countUpdatedFiles();
	}
	/**
	 * Fancy String for console reading
	 * @return big string (multi-line)
	 */
	public String toString() {
		ArrayList<StringBuffer> list = new ArrayList<StringBuffer>();
		list.add(new StringBuffer("| Statistics on ").append(delta.getDocumentID()));
		list.add(new StringBuffer("|").append(format(newFiles())).append(" new files"));
		list.add(new StringBuffer("|").append(format(deletedFiles())).append(" deleted files"));
		list.add(new StringBuffer("|").append(format(updatedFiles())).append(" updated files"));
				
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
