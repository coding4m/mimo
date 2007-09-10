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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import edos.distribution.mirror.DOMMirrorDelta;
import edos.distribution.mirror.IDOMMirrorDelta;
import edos.distribution.mirror.ITreeMirrorStructure;
import edos.distribution.mirror.TreeMirrorStructure;
import edos.mimo.Access;
import edos.mimo.Date;
import edos.mimo.IMasterMirror;
import edos.mimo.IMirror;
import edos.mimo.IPass;
import edos.mimo.Identity;
import edos.mimo.MirrorFactory;
import edos.mimo.exception.ConfigException;
import edos.mimo.filesystem.IFile;

/**
* A delta is a simple XML document saying what has changed
* since the last (full) mirror state. Deltas are not 
* cumulative. A delta refers always to the latest full
* structure document (eg. mymirror_proto-12345.xml), and
* never to other deltas.
* 
* Deltas are currently implemented as Dom4j XML documents
* which are simply <code>IDOMMirrorDiff</code>s with a 
* rewritten root element ("delta" instead of "diff"). Hence,
* the cost of generating a delta is close to the cost of 
* generating a diff, plus a small overhead.
* 
* 
* TODO a XSL stylesheet could do the same thing nicely (and more efficiently?)
* 
* @author marc
*
*/
public class DOMMirrorDelta implements IDOMMirrorDelta {
	private static Logger logger = Logger.getLogger(DOMMirrorDelta.class);
	
	private int deltaSize = 0;	// how many changes in this delta
	private Document oldestDoc;
	private Document latestDoc;
	private Document deltaDoc;
	private Element  root;
	
	private IMirror mirror;
	
	private String documentID;	// the name of the XML document (without extension)
	private String fileName;	// the name of the XML document (with extension)
	

	/**
	 * Initalizes all the fields and builds an empty document (size null)
	 * @param latestFull
	 * @param newOne
	 */
	public DOMMirrorDelta(ITreeMirrorStructure latestFull, ITreeMirrorStructure newOne) {
		mirror = newOne.getMirror();
		IPass pass = mirror.getActivePass();
		
        oldestDoc = latestFull.getDocument();
        latestDoc = newOne.getDocument();
		
		documentID = newOne.getDocumentID() + "-delta";	// mirrorID + timestamp + delta
		fileName = documentID + ".xml";

        deltaDoc= DocumentHelper.createDocument();
        //deltaDoc.addProcessingInstruction("xml-stylesheet", "href=\"../xsl/delta.xsl\" type=\"text/xml\"");
        
        String type = (newOne instanceof IMasterMirror)?MASTER:MIRROR;
        
        
        root = deltaDoc.addElement( "delta" )
			.addAttribute(MIRROR, latestFull.getMirrorID())
			.addAttribute(TYPE, type)
			.addAttribute(NAME, mirror.getName())
			.addAttribute(HOST, pass.getAccess().getHost())
			.addAttribute(PROTOCOL, pass.getAccess().getProtocol())
			.addAttribute(PATH, pass.getAccess().getLocalPath())
	    	//.addAttribute(TIMESTAMP, Long.toString(new GregorianCalendar().getTimeInMillis()));
			.addAttribute(TIMESTAMP, new Date().asISO8601());

       
		/*
		 * COMPUTE DELTA FROM 
		 */
		logger.info("Starting delta generation ("
				+ latestFull.getFilename()
				+ "->"
				+ newOne.getFilename()
				+ ")");
		
		// traverse the base tree to visit elements one by one
		treeWalkTheReferenceDocument(oldestDoc);
		treeWalkSearchingForNewFiles(latestDoc);
		
		logger.info("Delta completed: " + fileName);
		
		// TODO listeners? / Quartz
	}

	/**
	 * Load the delta from file or database, as an InputStream.
	 * 
	 * 
	 * @param InputStream is
	 * @throws IOException
	 */
	public DOMMirrorDelta(String filename, FileInputStream fin) throws DocumentException {
	    SAXReader xmlReader = new SAXReader();
	    this.deltaDoc = xmlReader.read(fin);	
	    
	   setup(filename);
	}

	/**
	 * This method is called by DOMMirrorDelta(String filename, FileInputStream fin)
	 * to instantiate delta documents from file.
	 * 
	 * ONLY FOR TESTING!! 
	 * (for production use: public DOMMirrorDelta(ITreeMirrorStructure latestFull, ITreeMirrorStructure newOne))
	 * 
	 * @see DOMMirrorDelta(String filename, FileInputStream fin)
	 * 
	 * @param filename
	 * @throws DocumentException 
	 */
	private void setup(String filename) throws DocumentException {
		root = deltaDoc.getRootElement();

		String type = root.attributeValue(TYPE);
		String name = root.attributeValue(NAME);
		String host = root.attributeValue(HOST);
		String protocol = root.attributeValue(PROTOCOL);
		String localPath = root.attributeValue(PATH);

		try{
			Access access = new Access(host, protocol, localPath);
			// TODO implement Identity registry , BD where relations Identity-Access persist
			Identity id = new Identity("fake name", "fake password", null);
			
			MirrorFactory factory = MirrorFactory.getInstance();
		    mirror = (type.equals(IMirror.MASTER))?
		    		factory.getMaster(name, access, id):factory.getSecondary(name, access, id);
		    		
		    mirror.setMirrorID(filename.split("-")[0]);
	    		
		}catch(NullPointerException ne) {
			// reminder: this method is meant only for testing
			logger.fatal("Unable to load delta from file " + filename);
			throw new DocumentException("Unable to load delta from file " + filename);
		}
	    
	    // not recoverable from file
        //oldestDoc = latestFull.getDocument();
        //latestDoc = newOne.getDocument();
		
	    this.fileName = filename;
		documentID = filename.split("\\.")[0]; // remove the ".xml" part
		fileName = documentID + ".xml";
	}

	/**
	 * Traverse all nodes of the master tree
	 * @param document
	 */
	private void treeWalkTheReferenceDocument(Document document) {
		treeWalkTheReferenceDocument(document.getRootElement());
	}

	/**
	 * Traverse all child nodes of element
	 * @param element
	 */
	private void treeWalkTheReferenceDocument(Element element) {
		for (int i = 0, length = element.nodeCount(); i < length; i++) {
			Node baseNode = element.node(i);
			String xpath = baseNode.getPath();	// non-indexed XPATH
			
			if (baseNode instanceof Element) {
				Element baseElt = (Element)baseNode;
				
				// completing the XPATH here with our own indexing by file name
				// (which are unique in a given directory)
				xpath = TreeMirrorStructure.enrichXPATH(xpath, baseElt);
								
				// looking for the corresponding element in the latest doc
				Element elt = (Element)latestDoc.selectSingleNode(xpath);
				logger.debug(baseElt);
				String  name		= baseElt.attributeValue(IFile.NAME);
				
				/*
				 * FILE REMOVED
				 */
				if(elt == null) {
					root.addElement(DEL)
						.addAttribute(XPATH, xpath);
					deltaSize++;
					
					logger.info("[" + mirror.getFilename() + "] removed: " + name );
					
				}else{
					
					// get file information
					String baseTimeStamp = baseElt.attributeValue(IFile.DATE);
					String eltTimeStramp = elt.attributeValue(IFile.DATE);
					int  baseSize	= Integer.parseInt(baseElt.attributeValue(IFile.SIZE));
					int  eltSize	= Integer.parseInt(elt.attributeValue(IFile.SIZE));
					Element newElt = null;
					
					// is there a change?
					if(!baseTimeStamp.equals(eltTimeStramp)
							|| baseSize != eltSize) {
						newElt = root.addElement(UPDATE)
									 .addAttribute(XPATH, xpath);
						deltaSize++;
					
						/*
						 * TIMESTAMP change
						 */
						if(!baseTimeStamp.equals(eltTimeStramp)) {
							newElt.addAttribute(IFile.DATE, eltTimeStramp);
							logger.info("[" + mirror.getFilename() + "] update: " + name +
									" : timestamp is now " + eltTimeStramp + " (before=" + baseTimeStamp + ")");
						}
		
						/*
						 * FILE SIZE mismath
						 */
						if(baseSize != eltSize) {
							newElt.addAttribute(IFile.SIZE, Integer.toString(eltSize));
							logger.info("[" + mirror.getFilename() + "] update: " + name +
									" : size is now " + eltSize + " (before=" + eltSize + ")");
						}
					}
					/*
					 * TRAVERSE next
					 */
					treeWalkTheReferenceDocument((Element) baseNode);
				}
			}
		}
	}

	/**
	 * Compares the oldest structure with the newest one to find
	 * new files
	 * @param doc
	 */
	public void treeWalkSearchingForNewFiles(Document doc) {
		treeWalkSearchingForNewFiles(doc.getRootElement());
	}
	
	/**
	 * Compares the oldest structure with the newest one to find
	 * new files
	 * @param doc
	 */	
	private void treeWalkSearchingForNewFiles(Element element) {

		for (int i = 0, length = element.nodeCount(); i < length; i++) {
			Node newNode = element.node(i);
			String xpath = newNode.getPath();	// non-indexed XPATH
			
			if (newNode instanceof Element) {
				Element newElt = (Element)newNode;
				
				// completing the XPATH here with our own indexing by file name
				// (which are unique in a given directory)
				xpath = TreeMirrorStructure.enrichXPATH(xpath, newElt);

				// looking for the corresponding element in the oldest doc
				Element elt = (Element)oldestDoc.selectSingleNode(xpath);
				
				
				if(elt == null) {
					String name = newElt.attributeValue(IFile.NAME);
					String date = newElt.attributeValue(IFile.DATE);
					String fileSize = newElt.attributeValue(IFile.SIZE);								
					
					root.addElement(ADD)
						.addAttribute(XPATH, xpath)
						.addAttribute(IFile.NAME, name)
						.addAttribute(IFile.DATE, date)
						.addAttribute(IFile.SIZE, fileSize);

					deltaSize++;
					logger.info("[" + mirror.getFilename() + "] new: " + name );
				}
				
				treeWalkSearchingForNewFiles((Element)newNode);
			}
			
		}
	}

	/**
	 * Returns how many changes are stored in this delta
	 * @return the number of changes
	 */
	public int getSize() {
		return deltaSize;
	}
	
	/**
	 * Returns the delta content as an XML document
	 * @return a big <code>String</code> containing the complete document
	 */
	public String toString() {
		return deltaDoc.asXML();
	}

	/**
	 * Saves as a file in the file system
	 * 
	 * @throws IOException
	 */
	public void save() throws IOException {
		// for production disable XML pretty print
		save(true);	// "pretty print" by default: it is much more efficient when loading in editor
					// probably less efficient for production
	}
	
	/**
	 * Saves as a file in the file system
	 * 
	 * @throws IOException
	 */
	public void save(boolean pretty) throws IOException {
		PrintWriter outG = new PrintWriter( getFileName() );
		if(pretty)
			serializetoPrettyPrintXML(outG);
		else
			serializetoXML(outG); // output to file
	}
	
	/**
	 * Outputs to a given <code>java.io.Writer</code>
	 * @param out
	 * @throws IOException
	 */
	private void serializetoXML(Writer out) throws IOException {
		XMLWriter writer = new XMLWriter(out);
		writer.write(this.deltaDoc);
		writer.flush();
	}

	/**
	 * Outputs to a given <code>java.io.Writer</code>
	 * Does "pretty print".
	 * 
	 * @param out
	 * @throws IOException
	 */
	private void serializetoPrettyPrintXML(Writer out) throws IOException {
		OutputFormat outformat = OutputFormat.createPrettyPrint();
		XMLWriter writer = new XMLWriter(out, outformat);
		writer.write(this.deltaDoc);
		writer.flush();
	}

	/**
	 * Returns the unique delta document ID
	 * @return the unique delta document ID
	 */
	public String getDocumentID() {
		return documentID;
	}

	public String getMirrorID() {
        String mirrorID = "none";
		try {
			mirrorID = mirror.getMirrorID();
	        
		} catch (ConfigException e) {
			logger.error("At this point the mirror must have a mirrorID which it has not: " + e.getMessage());
		}
		return mirrorID;
	}
	
	/**
	 * Returns the file name
	 * @return the unique file name for this delta
	 */
	public String getFileName() {
		return fileName;
	}

	public int countDeletedFiles() {
		return root.selectNodes("//" + DEL).size();
	}


	public int countNewFiles() {
		return root.selectNodes("//" + ADD).size();
	}


	public int countUpdatedFiles() {
		return root.selectNodes("//" + UPDATE).size();
	}

	public String getFilename() {
		return fileName;
	}

	/**
	 * Returns a list of Dom4j nodes.
	 * TODO a layer of separation between the internal representation (dom4j) and the others...
	 */
	public List getDeletedFiles() {
		return root.selectNodes("//" + DEL);
	}

	/**
	 * Returns a list of Dom4j nodes.
	 * TODO a layer of separation between the internal representation (dom4j) and the others...
	 */
	public List getNewFiles() {
		return root.selectNodes("//" + ADD);
	}

	/**
	 * Returns a list of Dom4j nodes.
	 * TODO a layer of separation between the internal representation (dom4j) and the others...
	 */
	public List getUpdatedFiles() {
		return root.selectNodes("//" + UPDATE);
	}

	public void save(String relativePathToDirectory) throws IOException {
		save(relativePathToDirectory, true);
	}

	public void save(String relativePathToDir, boolean pretty) throws IOException {
		PrintWriter outG = new PrintWriter( relativePathToDir + getFileName() );
		if(pretty)
			serializetoPrettyPrintXML(outG);
		else
			serializetoXML(outG); // output to file
	}
	
	public String getTimestamp() {
		return root.selectSingleNode("/delta/@timestamp").getText();
	}
}
