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

import java.util.Map;
import java.util.TreeMap;

import org.dom4j.Element;

import edos.distribution.mirror.IDirectory;
import edos.distribution.mirror.IFile;
import edos.distribution.mirror.MirrorFile;
import edos.mimo.Date;

public class MirrorDirectory extends MirrorFile implements IDirectory {
	private TreeMap<String,IFile> files; // contained files sorted by names
	
	public MirrorDirectory(String name, Date date, long size, String path) {
		super(name, date, size, path);
		this.files = new TreeMap<String,IFile>();
	}
	
	public MirrorDirectory(String name, Date date, long size, String path, Map<String,IFile> files) {
		this(name, date, size, path);
		this.files.putAll(files);
	}

	public MirrorDirectory(Element e) {
		super(e);
		this.files = new TreeMap<String,IFile>();
	}

	public Map<String,IFile> getFiles() {
		return files;
	}

}
