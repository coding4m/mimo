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
 * This class is generic and it could be used by diverse
 * model implementations.
 * 
 * @author marc
 *
 */
public class FileDiff implements IFileDiff {
	//private static Logger logger = Logger.getLogger(FileDiff.class);
	
	private int status;
	private IFile file;
	private IFile masterFile;
	

	/**
	 * Basic constructor for a file.
	 * It indicates the problem associated with it.
	 * 
	 * @param file
	 * @param status
	 */
	public FileDiff(IFile file, int status) {
		this.file = file;
		this.status = status;
		masterFile = null;
	}
	
	/**
	 * More detailled constructor including details
	 * about the original file and its mirrored copy.
	 * 
	 * Useful when comparing timestamps.
	 * 
	 * @param file from the master mirror
	 * @param mirrored copy of the same file
	 * @param status describing the discrepancy between the above two
	 */
	public FileDiff(IFile masterFile, IFile mirrorFile, int status) {
		this.file = mirrorFile;
		this.masterFile = masterFile;
		this.status = status;
	}
	
	public IFile getFile() {
		return file;
	}
	
	public int getStatus() {
		return status;
	}
	
	public String toString() {
		switch(status) {
			case FILE_OK:
				return MESSAGE_FILE_OK;
			case FILE_MISSING:
				return MESSAGE_FILE_MISSING;
			case FILE_CORRUPTED:
				return MESSAGE_FILE_CORRUPTED;
			case FILE_OLDER:
				return MESSAGE_FILE_OLDER;
			case FILE_NEWER:
				return MESSAGE_FILE_NEWER;
			default:
				throw new NullPointerException("Unexpected status");
		}
	}

	public String getPath() {
		return file.getPath();
	}

	public long getExpectedTimeInMillis() {
		return (masterFile == null)? 0 :  masterFile.getDate().getTimeInMillis();
	}

	public long getEffectiveTimeInMillis() {
		return file.getDate().getTimeInMillis();
	}

	public String getExpectedDate() {
		return (masterFile == null)? "unknown"
				:  masterFile.getDate().asISO8601();
	}

	public String getEffectiveDate() {
		return file.getDate().asISO8601();
	}
}
