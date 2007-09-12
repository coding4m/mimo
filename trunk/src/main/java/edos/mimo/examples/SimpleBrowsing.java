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

import edos.mimo.Access;
import edos.mimo.ISecondaryMirror;
import edos.mimo.Identity;
import edos.mimo.MirrorFactory;
import edos.mimo.dom.ITreeMirrorStructure;


/**
 * This class is called simple browsing but it offers 2 features: - recursive
 * navigation of the file system to model this as a directory tree - same as
 * above but filling the tree with the non-directory files
 * 
 * In this example the "Access" chosen is via FTP, but it should work the same
 * way just by changing the "Access" instantiation with another protocol such as
 * SSH2 (and later on other ones).
 * 
 * @author marc
 * 
 */
public class SimpleBrowsing {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ISecondaryMirror mirror = null;
		MirrorFactory mirrorFactory = MirrorFactory.getInstance();
		
		// instantiation
		//Access myAccess = new Access("ganimedes", "ftp", "/cooker");
		Access myAccess = new Access("ftp.free.fr", "ftp",
		// "/mirrors/ftp.mandrake-linux.com/Mandrakelinux/devel/cooker");
		 "/mirrors/ftp.mandrake-linux.com/Mandrakelinux/devel/cooker/cooker/media");

		Identity id = new Identity("anonymous", "mirrormonitor@mandriva.com", null, "The mirror Monitor program"); 
		
		mirror = mirrorFactory.getSecondary("Mandriva main FTP site", myAccess, id);
		
		// see this function to get a tree of directories
		System.out.println("Acquiring a directory skeleton of " + mirror);
		getDirectoryStructure(mirror);

		// see this function to get a complete model of the mirror file system
		System.out.println("Acquiring a complete representation of " + mirror);
		getCompleteFileSystemStructure(mirror);

		System.out.println("Exited successfully");

		if (mirror != null)
			mirror.disconnect();

	}

	private static void getDirectoryStructure(ISecondaryMirror mirror) {
		try {
			// connection and work
			mirror.connect();
			mirror.acquireSkeleton();

			// data manipulation, reports
			ITreeMirrorStructure structure = (ITreeMirrorStructure)mirror.getStructure();
			structure.save();

		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());

		} catch (Exception e) {
			System.out.println(e.getMessage());

		} finally {
			if (mirror != null)
				mirror.disconnect();
		}
	}

	private static void getCompleteFileSystemStructure(ISecondaryMirror mirror) {
		try {
			// connection and work
			mirror.connect();
			mirror.acquireStructure();

			// data manipulation, reports
			ITreeMirrorStructure structure = (ITreeMirrorStructure)mirror.getStructure();
			
			System.out.println("XML representation: ");
			System.out.println(structure);
			
			structure.save(); // output to file (~1MB)

		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());

		} catch (Exception e) {
			System.out.println(e.getMessage());

		} finally {
			if (mirror != null)
				mirror.disconnect();
		}
	}
}
