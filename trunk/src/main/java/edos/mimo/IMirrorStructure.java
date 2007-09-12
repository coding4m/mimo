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

import java.io.IOException;
import java.util.List;

import edos.mimo.filesystem.IDirectory;
import edos.mimo.filesystem.IFile;

/**
 * Generic interface to describe a mirror structure.
 * More specific implementation exist under filesystem
 * and dal.
 * 
 * @author marc
 *
 */
public interface IMirrorStructure {	
	// local constants
	public static final String MIRROR 		= IMirror.MIRROR;
	public static final String TYPE			= IMirror.TYPE;
	public static final String NAME			= IMirror.NAME;
	public static final String HOST			= IMirror.HOST;
	public static final String PROTOCOL		= IMirror.PROTOCOL;
	public static final String PATH			= IMirror.PATH;
	public static final String TIMESTAMP 	= "timestamp";
	public static final String CHECKINTIME	= IMirror.CHECKINTIME;
	public static final String CHECKOUTTIME	= IMirror.CHECKOUTTIME;
	public static final String SIZE			= IFile.SIZE;
	public static final String DATE			= IFile.DATE;
	public static final String DIRECTORY	= IFile.DIRECTORY;
	public static final String FILE			= IFile.FILE;

	public List<IDirectory> getTopDirectories();
	
	public void save() throws IOException;
	public void save(boolean pretty) throws IOException;
	
	public String toString(); // dangerously long String (for Eclipse UI)

	// patching with a delta
	public void patch(IMirrorDelta delta);
}
