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
package edos.mimo.connection;

import java.io.IOException;
import java.util.List;

import edos.mimo.filesystem.IFile;

public interface IConnection {

	public void connect() throws IOException;
	public void close();
	
	public List<String> ls() throws IOException;
	public List<IFile> lsl() throws IOException;
	
	public List<IFile> lsFrom(String relPath) throws IOException;
	public List<IFile> lsDirFrom(String relPath) throws IOException;
	
	public boolean isValidPath(String path);
}
