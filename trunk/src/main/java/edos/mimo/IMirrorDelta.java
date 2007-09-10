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
package edos.mimo;

import java.util.List;

/**
 * This interface represents the changes between
 * 2 versions of the same mirror.
 * 
 * It is to be expected that changes relatively 
 * few compared to the huge number of files
 * typically on a mirror. Hence, the "delta" is
 * the set of changes which occured from the 
 * latest complete snapshot of a mirror.
 * 
 * Storing a delta is much much less expensive
 * than storing a complete snapshot space-wise.
 * 
 * @author marc
 *
 */

public interface IMirrorDelta {	
	// local constants
	public static final String TYPE			= IMirror.TYPE;
	public static final String NAME			= AbstractMirror.NAME;
	public static final String HOST			= AbstractMirror.HOST;
	public static final String PROTOCOL		= AbstractMirror.PROTOCOL;
	public static final String PATH			= AbstractMirror.PATH;
	public static final String TIMESTAMP 	= "timestamp";
	public static final String EXPECTED_TIMESTAMP  = "expected";
	public static final String EFFECTIVE_TIMESTAMP = "effective";
	
	// constants
	public static final String MASTER	= IMirror.MASTER;
	public static final String MIRROR	= IMirror.MIRROR;
	public static final String XPATH	= "xpath";
	public static final String ADD		= "add";
	public static final String DEL		= "del";
	public static final String UPDATE	= "update";

	public int getSize();			// how many changes this delta contains
	public String getDocumentID();	// unique name for this delta
	public String getMirrorID();	// what mirror is this delta for
	public String getFilename();	// a unique file name based on the mirror ID
	
	public String toString();				// get document via String
	

	// statistics
	public int countDeletedFiles();
	public int countNewFiles();
	public int countUpdatedFiles();
	
	// data
	public List getDeletedFiles();
	public List getNewFiles();
	public List getUpdatedFiles();
}
