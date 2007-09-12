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

/**
 * Represents a difference (mismatch) between 2 files, one
 * on the master mirror and one on the secondary mirror,
 * when they were supposed to be identical.
 * 
 * This interface contains the minimal set of services
 * we should expect from a file diff among the implementations.
 * 
 * @author marc
 *
 */
public interface IFileDiff {
	// constants
	public static final int FILE_OK				= 0;
	public static final int FILE_MISSING 		= 1;
	public static final int FILE_CORRUPTED 		= 2;	// size is not matching
	public static final int FILE_OLDER 			= 3;
	public static final int FILE_NEWER 			= 4;	// very unlikely
	public static final int FILE_SUPERFLUOUS 	= 5;	// file is not on the master mirror
	public static final int FILE_WRONG_TYPE 	= 6;	// should not happen
	
	public static final String MESSAGE_FILE_OK 			= "The files are matching";
	public static final String MESSAGE_FILE_MISSING		= "The file is missing";
	public static final String MESSAGE_FILE_CORRUPTED 	= "The file looks corrupted (size is different)";
	public static final String MESSAGE_FILE_OLDER 		= "The file is older than the master's version";
	public static final String MESSAGE_FILE_NEWER 		= "The file is newer than the master's version(!)";
	public static final String MESSAGE_FILE_SUPERFLUOUS = "This extra-file does not exist on the master mirror";
	public static final String MESSAGE_FILE_WRONG_TYPE 	= "The file type (directory/regular file) is wrong!";
	
	
	public int getStatus();		// return diff (error) status as above
	public String toString();	// return error message as above
	public String getPath();	// filesystem path of this file
	public String getExpectedDate();	// the date of the master file
	public String getEffectiveDate();	// date of the compared file
	public long getExpectedTimeInMillis();
	public long getEffectiveTimeInMillis();
}
