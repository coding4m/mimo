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
package edos.mimo.dom;

import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import edos.distribution.mirror.DOMDiffGenerator;
import edos.distribution.mirror.DOMMirrorDiff;
import edos.distribution.mirror.ITreeMirrorStructure;
import edos.distribution.mirror.TreeMirrorStructure;
import edos.mimo.Date;
import edos.mimo.IMasterMirror;
import edos.mimo.ISecondaryMirror;
import edos.mimo.filesystem.FileDiff;
import edos.mimo.filesystem.IDiffGenerator;
import edos.mimo.filesystem.IDiffGeneratorListener;
import edos.mimo.filesystem.IFileDiff;
import edos.mimo.filesystem.IMirrorDiff;
import edos.mimo.filesystem.MirrorFile;
import edos.mimo.filesystem.MirrorFileFactory;

/**
 * DiffGenerator takes 2 mirrors, compares them
 * and computes a diff between them.
 * 
 * The first mirror passed to the constructor is
 * viewed as the reference (eg. the master).
 * 
 * Note that differences between mirrors (content)
 * can be made for different purposes such as:
 *  . check synchronization of a mirror with the master
 *  . check one mirror history (with oneself past state)
 * 
 * This class only cares about mirror content, for
 * example the DOM tree representing the content
 * of the mirror (ie. a subset of ITreeMirrorStructure).
 * @author marc
 *
 */
public class DOMDiffGenerator implements IDiffGenerator {
	private static Logger logger = Logger.getLogger(DOMDiffGenerator.class);

	private ITreeMirrorStructure mirrorStruct;
	private IMirrorDiff diff;
	private List<IDiffGeneratorListener> listeners;
	
	// the mirror structures are available as dom4j documents
	private Document masterDoc;
	private Document mirrorDoc;
	

	
	public DOMDiffGenerator(IMasterMirror master, ISecondaryMirror mirror) throws ParseException {
		/*
		 * SET UP
		 */
		mirrorStruct = (ITreeMirrorStructure)mirror.getStructure();
		listeners = new LinkedList<IDiffGeneratorListener>();
		
		masterDoc = ((ITreeMirrorStructure)master.getStructure()).getDocument();
		mirrorDoc = ((ITreeMirrorStructure)mirror.getStructure()).getDocument();
		
		
		diff = new DOMMirrorDiff(master, mirror);
		
		/*
		 * COMPUTE DIFF
		 */
		logger.info("Starting diff generation for " + mirrorStruct.getMirrorID());
		
		// traverse the master tree to visit elements one by one
		treeWalkMaster(masterDoc);
		treeWalkMirror(mirrorDoc);
		
		logger.info("Diff completed for " + mirrorStruct.getFilename());
		
		// notify all listeners that we are done
		notifyAllListeners();
		logger.info("listeners were notified");
	}

	/**
	 * Get method returning the <code>IMirrorDiff</code> to the listeners.
	 */
	public void notifyAllListeners() {
		Iterator<IDiffGeneratorListener> it = listeners.iterator();
		while (it.hasNext()) {
			IDiffGeneratorListener listener = it.next();
			if (listener == null)
				continue;

			listener.diffCompleted(diff);
		}
	}
	
	/**
	 * Set up the listener(s)
	 * @param listener
	 */
	public void addListener(IDiffGeneratorListener listener) {
		if(listener == null)
			return;
		listeners.add(listener);
	}
	
	/**
	 * Traverse all nodes of the master tree
	 * to find old and missing files
	 * @param document
	 * @throws ParseException 
	 */
	public void treeWalkMaster(Document document) throws ParseException {
		treeWalkMaster(document.getRootElement());
	}

	/**
	 * Traverse all child nodes of element
	 * @param element
	 * @throws ParseException 
	 */
	public void treeWalkMaster(Element element) throws ParseException {
		for (int i = 0, size = element.nodeCount(); i < size; i++) {
			Node masterNode = element.node(i);
			String xpath = masterNode.getPath();	// non-indexed XPATH
			String path = null;
			
			if (masterNode instanceof Element) {
				Element masterElt = (Element)masterNode;
				
				// completing the XPATH here with our own indexing by file name
				// (which are unique in a given directory)
				xpath = TreeMirrorStructure.enrichXPATH(xpath, masterElt);
				
				// getting the actual path in the mirror filesystem
				path = TreeMirrorStructure.computePath(xpath);	
				logger.debug("checking master:" + path);
				
				// looking for the corresponding element in the mirror
				Element mirrorElt = (Element)mirrorDoc.selectSingleNode(xpath);
				
				/*
				 * MISSING FILE?
				 */
				if(mirrorElt == null) {
					IFileDiff fileDiff = new FileDiff(MirrorFileFactory.create(masterElt, path),
												FileDiff.FILE_MISSING);
					diff.addDiff(fileDiff);
					
				}else if(!masterElt.getName().equals(mirrorElt.getName())) {
					/*
					 * WRONG TYPE
					 * This should not happen but it can
					 */
					
					IFileDiff fileDiff = new FileDiff(MirrorFileFactory.create(mirrorElt, path), 
												FileDiff.FILE_WRONG_TYPE);
					diff.addDiff(fileDiff);
					
				}else{
					
					/*
					 * TIMESTAMP mismatch
					 */
					Date masterDate = new Date(masterElt.attributeValue(MirrorFile.DATE));
					Date mirrorDate = new Date(mirrorElt.attributeValue(MirrorFile.DATE));
					
					if(masterDate.greaterThan(mirrorDate) ) {
						IFileDiff fileDiff = new FileDiff(
								MirrorFileFactory.create(masterElt, path),
								MirrorFileFactory.create(mirrorElt, path), 
								FileDiff.FILE_OLDER);
						
						diff.addDiff(fileDiff);
						
					}else if(mirrorDate.greaterThan(masterDate)) {
						IFileDiff fileDiff = new FileDiff(
								MirrorFileFactory.create(masterElt, path),
								MirrorFileFactory.create(mirrorElt, path), 
								FileDiff.FILE_NEWER);
						
						diff.addDiff(fileDiff);
						
					}
					
					/*
					 * FILE SIZE mismath
					 */
					long masterSize = Long.parseLong(masterElt.attributeValue(MirrorFile.SIZE));
					long mirrorSize = Long.parseLong(mirrorElt.attributeValue(MirrorFile.SIZE));
					
					if(masterSize != mirrorSize){
						IFileDiff fileDiff = new FileDiff(MirrorFileFactory.create(mirrorElt, path), 
													FileDiff.FILE_CORRUPTED);
						diff.addDiff(fileDiff);
					}
				}
				/*
				 * TRAVERSE next
				 */
				treeWalkMaster((Element) masterNode);
			}
		}
	}
	
	/**
	 * Traverse all nodes of the mirror tree
	 * to find new files
	 * @param document
	 * @throws ParseException 
	 */
	public void treeWalkMirror(Document document) throws ParseException {
		treeWalkMirror(document.getRootElement());
	}

	/**
	 * Traverse all child nodes of element
	 * @param element
	 * @throws ParseException 
	 */
	public void treeWalkMirror(Element element) throws ParseException {
		for (int i = 0, size = element.nodeCount(); i < size; i++) {
			Node mirrorNode = element.node(i);
			String xpath = mirrorNode.getPath();	// non-indexed XPATH
			String path = null;
			
			if (mirrorNode instanceof Element) {
				Element mirrorElt = (Element)mirrorNode;

				// completing the XPATH here with our own indexing by file name
				// (which are unique in a given directory)
				xpath = TreeMirrorStructure.enrichXPATH(xpath, mirrorElt);
				
				// getting the actual path in the mirror filesystem
				path = TreeMirrorStructure.computePath(xpath);		
				logger.debug("checking mirror:" + path);							
				
				// looking for the corresponding element in the mirror
				Element masterElt = (Element)masterDoc.selectSingleNode(xpath);
				
				/*
				 * EXTRA FILES?
				 */
				if(masterElt == null) {
					IFileDiff fileDiff = new FileDiff(MirrorFileFactory.create(mirrorElt, path),
												FileDiff.FILE_SUPERFLUOUS);
					diff.addDiff(fileDiff);
					
				}
				
				/*
				 * TRAVERSE next
				 */
				treeWalkMirror((Element) mirrorNode);
			}
		}
	}

	public IMirrorDiff getDiff() {
		return diff;
	}

}
