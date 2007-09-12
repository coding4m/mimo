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
package edos.mimo.examples.filesystem;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.GregorianCalendar;

import org.dom4j.DocumentException;

import edos.distribution.mirror.GenerateTreeDiffExample;
import edos.mimo.Config;
import edos.mimo.IMasterMirror;
import edos.mimo.IMirror;
import edos.mimo.ISecondaryMirror;
import edos.mimo.MirrorFactory;
import edos.mimo.dom.DOMDiffGenerator;
import edos.mimo.dom.DOMMirrorDiff;
import edos.mimo.filesystem.DiffGeneratorFactory;
import edos.mimo.filesystem.IDiffGeneratorListener;
import edos.mimo.filesystem.IMirrorDiff;

/**
 * This example works with the tree-based modelization
 * of the file system, with persistency as XML files
 * stored within the host file system (not with a database 
 * manager).
 * 
 * HOW IT WORKS
 * ------------
 * First, 2 XML files are loaded from the file system, one
 * representing the master mirror structure, and the other
 * one for a secondary mirror structure. Both of these files
 * are the product of mirror-monitor browsing activity
 * occurred in the past (and stored).
 * 
 * Once these structures are in memory, the diff starts.
 * 
 * Finally, the diff is saved in the filesystem as another
 * XML file (ending with -diff.xml).
 *  
 * EXECUTION TIME
 * --------------
 * Comparing 2 i586 cooker ("cooker") branches takes 10 sec
 * on my machine.
 *  
 * @author marc
 *
 */
public class GenerateTreeDiffExample implements IDiffGeneratorListener {
	static final String MASTER_XML = "my local FTP site-complete-filesystem-structure.xml";
	static final String MIRROR_XML = "User-defined site-complete-filesystem-structure.xml";
	private long startTimeInMillis = -1;
	
	GenerateTreeDiffExample() {
		startTimeInMillis = new GregorianCalendar().getTimeInMillis();
	}
	
	public static void main(String[] args) {
		IMasterMirror master = null;
		ISecondaryMirror mirror = null;
		GenerateTreeDiffExample me = new GenerateTreeDiffExample();
		
		try {
			System.out.println("Loading master..");
			master = (IMasterMirror)load(MASTER_XML);
	
			System.out.println("Loading mirror..");
			mirror = (ISecondaryMirror)load(MIRROR_XML);
			
		}catch(DocumentException doce) {
			System.err.println("Unable to load mirror from file...");
			doce.printStackTrace();
		}
		
		DOMDiffGenerator gen;
		try {
			gen = (DOMDiffGenerator)DiffGeneratorFactory.getDiffGenerator(Config.TREE, master, mirror);
		} catch (ParseException e) {
			System.err.println("Error parsing dates in structure ; " + e.getMessage());
			e.printStackTrace();
			return;
		}
		gen.addListener(me);	// will call me.diffCompleted() when finished
		
	}

	private static IMirror load(String filename) throws DocumentException {
		IMirror mirror = null;

		MirrorFactory factory = MirrorFactory.getInstance();
		mirror = factory.getMirror(new File(filename));
		
		return mirror;
	}
	
	/**
	 * This method is called by a <code>IDiffGenerator</code> when
	 * its job is done, a IMirrorDiff is available.
	 * 
	 * This method also belongs to <code>DownloadCenter</code> which
	 * is also responsible for dealing (storing) diffs.
	 */
	public void diffCompleted(IMirrorDiff diff) {
		double executionTime;
		
		if(diff instanceof DOMMirrorDiff	) {
			try{
				System.out.println("Saving " + diff.getFileName());
				((DOMMirrorDiff)diff).save();
				
				executionTime = (new GregorianCalendar().getTimeInMillis()
									- startTimeInMillis) / 1000.0;
				System.out.println("Done in " + executionTime + " seconds");
				
			}catch(IOException ioe) {
				System.err.println(ioe.getMessage());
			}
		}
	}

}
