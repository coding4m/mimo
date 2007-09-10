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
package edos.mimo.examples;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import edos.mimo.Access;
import edos.mimo.ISecondaryMirror;
import edos.mimo.Identity;
import edos.mimo.MirrorFactory;
import edos.mimo.filesystem.IFile;


/**
 * This class demonstrates the use of ls() and lsl(), 2 methods implementing
 * the functionalities of respectively ls -F and ls -l at the bash shell.
 * 
 * These functions are very low-level and they are not intended for the 
 * programmer to use them. Try instead a high level API from IMirror.
 *
 */
public class SimpleFTPCommands {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ISecondaryMirror free_fr = null;
		MirrorFactory mirrorFactory = MirrorFactory.getInstance();
		
		try {	
			Access free_fr_access  = new Access("ftp.free.fr", "ftp", "/mirrors/ftp.mandrake-linux.com/Mandrakelinux/devel/cooker");
			Identity id = new Identity("anonymous", "mirrormonitor@mandriva.com", null, "The mirror Monitor program"); 
			
			free_fr = mirrorFactory.getSecondary("Mandriva main FTP site", free_fr_access, id);
			
			free_fr.connect();
			
			// execute the ls command to list the directories and other files
			System.out.println("--------------------------------");
			System.out.println("Basic list of files:");
			List<String> fileNames = free_fr.ls();
			Iterator free_fr_Iterator = fileNames.iterator();
			while(free_fr_Iterator.hasNext()) {
				System.out.println((String)free_fr_Iterator.next());
			}
			
			// execute a more verbose ls command version to get extended information
			//	such as time stamps, etc.
			System.out.println("--------------------------------");
			System.out.println("Complete file information:");
			List<IFile> files = free_fr.lsl();
			free_fr_Iterator = files.iterator();
			while(free_fr_Iterator.hasNext()) {
				System.out.println(free_fr_Iterator.next().toString());
			}
			
		}catch(IOException ioe) {
			System.out.println(ioe.getMessage());
			
		}catch(Exception e) {
			System.out.println(e.getMessage());
			
		}finally{
			if(free_fr != null)
				free_fr.disconnect();			
		}		

	}

}
