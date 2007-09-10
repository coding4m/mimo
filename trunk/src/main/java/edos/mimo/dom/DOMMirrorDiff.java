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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import edos.distribution.mirror.DOMMirrorDiff;
import edos.distribution.mirror.IDOMMirrorDiff;
import edos.distribution.mirror.ITreeMirrorStructure;
import edos.mimo.Date;
import edos.mimo.IMasterMirror;
import edos.mimo.IPass;
import edos.mimo.ISecondaryMirror;
import edos.mimo.exception.ConfigException;
import edos.mimo.filesystem.IFileDiff;

public class DOMMirrorDiff implements IDOMMirrorDiff {
	private static Logger logger = Logger.getLogger(DOMMirrorDiff.class);
	
	// private attributes
	private Document doc;
	private Element  root;
	
	private String diffDocumentID;	// the name of the XML document (without extension)
	private String fileName;		// the name of the XML document (with extension)
	private String mirrorID;
	
	private Hashtable<String,String> monitoredDirs; // filter for the diff (exclude everything but these paths)
	
	public DOMMirrorDiff(IMasterMirror master, ISecondaryMirror mirror) {
		this(master, mirror, null);	// null means catch all files (no filter)
	}
	
	public DOMMirrorDiff(IMasterMirror master, ISecondaryMirror mirror, Hashtable<String,String> monitoredDirs) {
		this.monitoredDirs = monitoredDirs;
		diffDocumentID = ((ITreeMirrorStructure)mirror.getStructure()).getDocumentID() + "-diff";
		fileName = diffDocumentID + ".xml";
		IPass pass = mirror.getActivePass();

        doc= DocumentHelper.createDocument();
        doc.addProcessingInstruction("xml-stylesheet", "href=\"../xsl/diff.xsl\" type=\"text/xml\"");
        
        root = doc.addElement( "diff" );
        
        try {			
        	mirrorID =  mirror.getMirrorID();
			root.addAttribute(MIRROR,mirrorID);
				
		} catch (ConfigException e) {
			logger.info("Unable to retrieve mirrorID for " + mirror.getName());
			// this should not happen!
		}

		root.addAttribute(NAME, mirror.getName())
				.addAttribute(HOST, pass.getAccess().getHost())
				.addAttribute(PROTOCOL, pass.getAccess().getProtocol())
				.addAttribute(PATH, pass.getAccess().getLocalPath())
				.addAttribute(TIMESTAMP, new Date().asISO8601())
				.addAttribute(MASTER_TIMESTAMP, master.getCheckoutTimeAsISO8601());
	
	}


	/**
	 * Called from BDBXMLManager to re-instantiate an object 
	 * from the database storage
	 * 
	 * @param content
	 * @throws DocumentException
	 */
	public DOMMirrorDiff(String content) throws DocumentException {
		this(new ByteArrayInputStream(content.getBytes()));		
	}

	public DOMMirrorDiff(ByteArrayInputStream stream) throws DocumentException {
	    SAXReader xmlReader = new SAXReader();
	    this.doc = xmlReader.read(stream);	
	    
	    setup();
	}

	private void setup() {
	    root = doc.getRootElement();
	    long timestamp = -1;
	    try {
	    	timestamp = new Date(root.attributeValue(TIMESTAMP)).getTimeInMillis();
	    	
	    }catch(Exception e) {
	    	logger.error("Unable to read timetamp back from diff document " 
	    			+ root.attributeValue(TIMESTAMP));
	    	// ignore
	    }
		diffDocumentID = root.attributeValue(HOST)
							+ "_"  + root.attributeValue(PROTOCOL)
							+ timestamp + "-diff";
		fileName = diffDocumentID + ".xml";
		mirrorID = root.attributeValue(MIRROR);
		
		monitoredDirs = null ; // don't care

	}

	/**
	 * Add the right node depending on the diff situation
	 * 
	 * If there is no list of monitored directories, it will listen to all diffs.
	 * If there is a list of monitored directories it will only listens to the diffs
	 * registered for these directories and nothing more.
	 */
	public void addDiff(IFileDiff diff) {
		logger.setLevel(Level.DEBUG);
		
		String filePath = diff.getPath();										// this file
		String dirPath = filePath.substring(0, filePath.lastIndexOf('/'));		// its directory (where it sits)
		if(monitoredDirs != null && monitoredDirs.get(dirPath) == null) {
			logger.debug("discarding " + diff.getPath() + " because it is not in the monitored path list");
			return;	
		}
			
		switch(diff.getStatus()) {
			case IFileDiff.FILE_MISSING:
				root.addElement(DOMMirrorDiff.MISSING)
					.addAttribute(DOMMirrorDiff.PATH, diff.getPath());
				break;
				
			case IFileDiff.FILE_CORRUPTED:
				root.addElement(DOMMirrorDiff.CORRUPTED)
				.addAttribute(DOMMirrorDiff.PATH, diff.getPath());
				break;
				
			case IFileDiff.FILE_OLDER:
				root.addElement(DOMMirrorDiff.OLDER)
				.addAttribute(DOMMirrorDiff.PATH, diff.getPath())
				.addAttribute(DOMMirrorDiff.EXPECTED_TIMESTAMP, diff.getExpectedDate())
				.addAttribute(DOMMirrorDiff.EFFECTIVE_TIMESTAMP, diff.getEffectiveDate());
				break;
				
			case IFileDiff.FILE_NEWER:
				root.addElement(DOMMirrorDiff.NEWER)
				.addAttribute(DOMMirrorDiff.PATH, diff.getPath())
				.addAttribute(DOMMirrorDiff.EXPECTED_TIMESTAMP, diff.getExpectedDate())
				.addAttribute(DOMMirrorDiff.EFFECTIVE_TIMESTAMP, diff.getEffectiveDate());
				break;
				
			case IFileDiff.FILE_SUPERFLUOUS:
				root.addElement(DOMMirrorDiff.SUPERFLUOUS)
				.addAttribute(DOMMirrorDiff.PATH, diff.getPath());
				break;
				
			case IFileDiff.FILE_WRONG_TYPE:
				root.addElement(DOMMirrorDiff.WRONG_TYPE)
				.addAttribute(DOMMirrorDiff.PATH, diff.getPath());	
				break;		
		}
		
	}


	/**
	 * Save the <code>IMirrorDiff</code> to a file named after the mirror name:
	 * <mirror name>-diff.xml
	 * 
	 * Do NOT use this for production, but save(false) or save(path, false)
	 * @see save(String relativePathToDir, boolean prettyPrint)
	 * @throws IOException
	 */
	public void save() throws IOException {
		String defaultDir = "";
		boolean pretty = true;	// "pretty print" by default: it is much more efficient when loading in editor
		// for production do not use XML pretty print
		save(defaultDir, pretty);	
	}


	/**
	 * Save the <code>IMirrorDiff</code> to a file named after the mirror name:
	 * <mirror name>-diff.xml in the specified directory
	 * 
	 * Do NOT use this for production, but save(path, false)
	 * @see save(String relativePathToDir, boolean prettyPrint)
	 * @throws IOException
	 */
	public void save(String relativePathToDirectory) throws IOException {
		save(relativePathToDirectory, true);
	}
	
	/**
	 * Save the <code>IMirrorDiff</code> to a file named after the mirror name:
	 * <mirror name>-diff.xml
	 *
	 * There is an option to get a "pretty print" which is obtained by an automated
	 * insertion of blank spaces at the cost of processing power.
	 * (A multi-line document is however easier to open within an editor!)
	 * 
	 * For production: disabling pretty print is recommanded
	 * 
	 * @throws IOException
	 * @param mirrorName String the name of the mirror
	 */
	public void save(boolean pretty) throws IOException {
		String defaultDir = "";
		save(defaultDir, pretty);
	}
	
	/**
	 * Save the <code>IMirrorDiff</code> to a file named after the mirror name:
	 * <mirror name>-diff.xml
	 *
	 * There is an option to get a "pretty print" which is obtained by an automated
	 * insertion of blank spaces at the cost of processing power.
	 * (A multi-line document is however easier to open within an editor!)
	 * 
	 * For production: disabling pretty print is recommanded
	 * 
	 * @throws IOException
	 * @param mirrorName String the name of the mirror
	 */
	public void save(String relativePathToDir, boolean pretty) throws IOException {
		PrintWriter outG = new PrintWriter( relativePathToDir + getFileName() );
		if(pretty)
			serializetoPrettyPrintXML(outG);
		else
			serializetoXML(outG); // output to file
	}
	
	/**
	 * Print the XML representation of the <code>IMirrorDiff</code>
	 * to the <code>OutputStream</code>.
	 * @param out
	 * @throws Exception
	 */
	private void serializetoXML(Writer out) throws IOException {
		XMLWriter writer = new XMLWriter(out);
		writer.write(this.doc);
		writer.flush();
	}
	
	/**
	 * Print the XML representation of the <code>IMirrorStructure</code>
	 * to the <code>OutputStream</code>. Uses pretty print format (end of line, etc.)
	 * @param out
	 * @throws Exception
	 */
	private void serializetoPrettyPrintXML(Writer out) throws IOException {
		OutputFormat outformat = OutputFormat.createPrettyPrint();
		XMLWriter writer = new XMLWriter(out, outformat);
		writer.write(this.doc);
		writer.flush();
	}

	/**
	 * Returns an XML String.
	 */
	public String toString() {
		return doc.asXML();
	}


	/**
	 * Returns a unique name (for a given timestamp)
	 */
	public String getDocumentID() {
		return diffDocumentID;
	}
	
	/**
	 * Returns a unique file name
	 * @return
	 */
	public String getFileName() {
		return fileName;
	}
	
	/**
	 * Gets the diff as a Dom4j document
	 * @return the dom4j document
	 */
	public Document getDom4jDocument() {
		return doc;
	}


	/** 
	 * Counts the number of missing files
	 * 
	 * @return the number of missing files
	 */
	public int countMissingFiles() {
		List missingFiles = root.selectNodes("//" + MISSING);
		
		return missingFiles.size();
	}


	public int newerFiles() {
		return root.selectNodes("//" + NEWER).size();
	}


	public int olderFiles() {
		return root.selectNodes("//" + OLDER).size();
	}


	public int corruptedFiles() {
		return root.selectNodes("//" + CORRUPTED).size();
	}


	public int superfluousFiles() {
		return root.selectNodes("//" + SUPERFLUOUS).size();
	}


	public int wrongTypeFiles() {
		return root.selectNodes("//" + WRONG_TYPE).size();
	}

	public String getMirrorID() {
		return mirrorID;
	}

}
