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

import org.apache.log4j.Logger;

/**
 * Generates a delta from a mirror structure in order to 
 * save space in the database (storing a light delta 
 * instead of the full structure with a lot of redundant
 * data).
 * 
 * A delta is a simple XML document saying what has changed
 * since the last (full) mirror state. Deltas are not
 * cumulative. A delta refers always to the latest full
 * structure document (eg. mymirror_proto-12345.xml), and
 * never to other deltas.
 * 
 * @author marc
 *
 */
public class DOMDeltaGenerator {
	private static Logger logger = Logger.getLogger(DOMDeltaGenerator.class);
	
	private ITreeMirrorStructure latestFull;
	private ITreeMirrorStructure newOne;
	private DOMMirrorDelta delta;
	
	public DOMDeltaGenerator(ITreeMirrorStructure latestFull, ITreeMirrorStructure newOne) {
		this.latestFull = latestFull;
		this.newOne = newOne;	
		
		delta = new DOMMirrorDelta(latestFull, newOne);
	}
	
	public DOMMirrorDelta getDelta() {
		return delta;
	}

}
