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

import edos.mimo.Date;

public interface IFile {

	public String getName();
	public Date getDate();
	public long getSize();
	
	// publish elementary information
	public String toString();
	public String getPath();	// the path of this file from the mirror root
	

	// attributes
	public static final String NAME = "name";
	public static final String DATE = "date";
	public static final String SIZE = "size";

	// it can happen (for example by getting the results of a ls command)
	// that not all the information is available for a IFile.
	public static final Date UNKNOWN_DATE = null;
	public static final long UNKNOWN_SIZE = -1;
	public static final String UNKNOWN_PATH = "/path_unknown/";

	// element names
	public static final String DIRECTORY = "dir";
	public static final String FILE = "file";
	public static final int DEFAULT_DIRECTORY_SIZE = 4096;
}
