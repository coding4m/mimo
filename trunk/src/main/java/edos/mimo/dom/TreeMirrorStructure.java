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
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import edos.distribution.mirror.ITreeMirrorStructure;
import edos.distribution.mirror.TreeMirrorStructure;
import edos.mimo.AbstractMirror;
import edos.mimo.Access;
import edos.mimo.AnalysisLevel;
import edos.mimo.Config;
import edos.mimo.Date;
import edos.mimo.IMasterMirror;
import edos.mimo.IMirror;
import edos.mimo.IMirrorDelta;
import edos.mimo.IPass;
import edos.mimo.Identity;
import edos.mimo.MirrorFactory;
import edos.mimo.connection.IConnection;
import edos.mimo.exception.ConfigException;
import edos.mimo.filesystem.IDirectory;
import edos.mimo.filesystem.IFile;
import edos.mimo.filesystem.MirrorDirectory;
import edos.mimo.filesystem.MirrorFile;

/**
 * The implementation of IMirrorStructure is built around a dom4j document.
 * The benefit is that everything is packed already to navigate the tree, 
 * print it, search it, etc. Hence we don't have to re-invent the wheel.
 * 
 * This class encapsulates a dom4j document.
 * 
 * The constructor needs a <code>IMirror</code> to refer to.
 * As soon as the constructor is called, it starts acquiring the structure
 * of this <code>IMirror</code>.
 *
 */
public class TreeMirrorStructure implements ITreeMirrorStructure {
	private static Logger logger = Logger.getLogger(TreeMirrorStructure.class);
	private Document doc;
	private Element  root;
	private IConnection conn;
	private AnalysisLevel analysis;
	private IMirror mirror;
	private long timestamp = 0; // creation date of this structure (long gives 16% gain in size over readable String)
	private String type = null; // master | mirror
	
	// mirror informations (from active connection pass)
	private String host = null;
	private String protocol = null;
	private String name = null;
	private String localPath = null;

	/**
	 * Constructore provided for testing only.
	 * It allows to create a MOCK object for testing.
	 * It does *NOT* fire up a mirror download and browse.
	 * @param doc
	 * @param root
	 * @param conn
	 * @param analysis
	 * @param mirror
	 */
	public TreeMirrorStructure(Document doc, Element root, IConnection conn,
			AnalysisLevel analysis, IMirror mirror) {
		this.doc = doc;
		this.root = root;
		this.conn = conn;
		this.analysis = analysis;
		this.mirror = mirror;
	}
	
	/**
	 * Constructs the structure with the mirror-configured analysis level.
	 * @param mirror
	 */
	public TreeMirrorStructure(IMirror mirror) throws IOException {
		this(mirror, mirror.getAnalysis());
	}

	/**
	 * Forces the construction of the structure based on some specific analysis
	 * level.
	 * @param mirror
	 * @param analysis
	 */
	public TreeMirrorStructure(IMirror mirror, AnalysisLevel analysis) throws IOException {
		this.mirror = mirror;
		IPass pass = mirror.getActivePass();
		host 		= pass.getAccess().getHost();
		protocol 	= pass.getAccess().getProtocol();
		name		= mirror.getName();
		localPath	= pass.getAccess().getLocalPath();
		
		this.analysis = analysis;
		if(mirror instanceof IMasterMirror)
			type = "master";
		else
			type = "mirror";
		
        doc= DocumentHelper.createDocument();
        root = doc.addElement( "mirror" );
        try {
			root.addAttribute(MIRROR, mirror.getMirrorID());
		
        } catch (ConfigException e1) {
        	logger.error("Unable to retrieve mirror ID for mirror " + mirror.getName());
        	// this should never happen at this point!
		}
        
		root.addAttribute(TYPE, type)
        		.addAttribute(NAME, name)
        		.addAttribute(HOST, host)
        		.addAttribute(PROTOCOL, protocol)
        		.addAttribute(PATH, localPath);
        conn = mirror.getConnection();
        
        // starting time
        mirror.setCheckinTime(new Date());
        
        String mirrorID;
		try {
			mirrorID = mirror.getMirrorID();
	        // recursively fill the document (tree) with the file structure
	        logger.info("Starting acquisition process for " + mirrorID);
	        acquire(root, pass.getAccess().getLocalPath());
	        logger.info("Completed acquisition process for " + mirrorID);
	        
		} catch (ConfigException e) {
			throw new IOException(e.getMessage());
		}

        // finish time
		mirror.setCheckoutTime(new Date());
        
		/*
        root.addAttribute(CHECKINTIME, Long.toString(mirror.getCheckinTime()))
        	.addAttribute(CHECKOUTTIME, Long.toString(mirror.getCheckoutTime()));
        */
        root.addAttribute(CHECKINTIME, mirror.getCheckinTimeAsISO8601())
    	.addAttribute(CHECKOUTTIME, mirror.getCheckoutTimeAsISO8601());
        
        // creation date of this structure
        timestamp = mirror.getCheckoutTime();
	}
	
	/**
	 * Load the structure from file or database, as an InputStream.
	 * 
	 * @param InputStream is
	 * @throws IOException
	 */
	public TreeMirrorStructure(InputStream is) throws DocumentException {
	    SAXReader xmlReader = new SAXReader();
	    this.doc = xmlReader.read(is);	
	    
	   setup();
	}


	/**
	 * Private constructor which may only be called from within.
	 * It serves when loading from file.
	 * Then all fields can be filled one by one, but we don't want
	 * to let anybody (else) do that.
	 
	private TreeMirrorStructure() {
		// create an empty object
	}
*/
	
	/**
	 * Load the structure from file or database, as a String.
	 * 
	 * This constructor can be called from the database manager
	 * to re-instantiate a Mirror structure object from a XML string
	 * 
	 * @param InputStream is
	 * @throws IOException
	 */
	public TreeMirrorStructure(String s) throws DocumentException {
		this(new ByteArrayInputStream(s.getBytes()));
	    setup();
	}
	
	/**
	 * Continue loading by filling all this attribute values
	 * from the XML document content.
	 *
	 */
	private void setup() {

	    root = doc.getRootElement();

		String type = root.attributeValue(TYPE);
		name = root.attributeValue(NAME);
		host = root.attributeValue(HOST);
		protocol = root.attributeValue(PROTOCOL);
		localPath = root.attributeValue(PATH);
		
		Access access = new Access(host, protocol, localPath);
		// TODO implement Identity registry , BD where relations Identity-Access persist
		Identity id = new Identity("fake name", "fake password", null);
		
		conn = null;
		analysis = new AnalysisLevel();	//default
		
		MirrorFactory factory = MirrorFactory.getInstance();
	    mirror = (type.equals(AbstractMirror.MASTER))?
	    		factory.getMaster(name, access, id):factory.getSecondary(name, access, id);
	    
	    mirror.setStructure(this);

		// Timestamp the structure as the time its acquisition was completed
		try{
			Date checkoutTime = new Date(root.attributeValue(CHECKOUTTIME), Date.ISO8601);
			timestamp = checkoutTime.getTimeInMillis();

		    mirror.setCheckinTime(new Date(root.attributeValue(CHECKINTIME), Date.ISO8601));
		    mirror.setCheckoutTime(checkoutTime);
		    
		}catch(ParseException pe) {
			logger.error("Unable to parse timestamp " + root.attributeValue(CHECKOUTTIME) 
					+ " or " + root.attributeValue(CHECKINTIME) +  " - " + pe.getMessage());
			timestamp = -1;	// error
		}
		
	    // setting active pass is mandatory, all visited mirrors have one ; here only one access, it has to be this one!
	    mirror.setActivePass(mirror.getPasses().get(0));
	}
	
	/**
	 * Private constructor which may only be called from within.
	 * It serves when loading from file.
	 * Then all fields can be filled one by one, but we don't want
	 * to let anybody (else) do that.
	 
	private TreeMirrorStructure() {
		// create an empty object
	}
*/
	public List<IDirectory> getTopDirectories() {
		// root.elements(MirrorStructure.DIRECTORY) returns List<Element>
		Iterator dirs = root.elementIterator(MirrorFile.DIRECTORY);
		List<IDirectory> list = new LinkedList<IDirectory>();
		
		while(dirs.hasNext())
			list.add(new MirrorDirectory((Element)dirs.next()));
		
		return list;
	}

	// TODO move this code to a IPublisher and call it from here
	
	/**
	 * Save the <code>IMirrorStructure</code> to a file named after the mirror name:
	 * <mirror name>-complete-filesystem-structure.xml
	 * 
	 * @throws IOException
	 */
	public void save() throws IOException {
		// TODO for production disable XML pretty print
		save(true);	// "pretty print" by default: it is much more efficient when loading in editor
					// probably less efficient for production
	}
	
	/**
	 * Save the <code>IMirrorStructure</code> to a file named after the mirror name:
	 * <mirror name>-complete-filesystem-structure.xml
	 *
	 * There is an option to get a "pretty print" which is obtained by an automated
	 * insertion of blank spaces at the cost of processing power.
	 * (A multi-line document is however easier to open within an editor!)
	 * 
	 * @throws IOException
	 * @param mirrorName String the name of the mirror
	 */
	public void save(boolean pretty) throws IOException {
		PrintWriter outG = new PrintWriter( getFilePath() );
		if(pretty)
			serializetoPrettyPrintXML(outG);
		else
			serializetoXML(outG); // output to file
	}
	
	/**
	 * Print the XML representation of the <code>IMirrorStructure</code>
	 * to the <code>OutputStream</code>.
	 * @param out
	 * @throws Exception
	 
	private void serializetoXML(OutputStream out) throws Exception {
		XMLWriter writer = new XMLWriter(out);
		writer.write(this.doc);
		writer.flush();
	}
	*/
	
	/**
	 * Print the XML representation of the <code>IMirrorStructure</code>
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
	 
	private void serializetoPrettyPrintXML(OutputStream out) throws IOException {
		OutputFormat outformat = OutputFormat.createPrettyPrint();
		XMLWriter writer = new XMLWriter(out, outformat);
		writer.write(this.doc);
		writer.flush();
	}
	*/
	
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
	 * This method is generic. It is called to get either a tree of the directory
	 * structure from the mirror (aka skeleton) or a "complete" structure, including
	 * the files. Also a verbose switch can be set to have information about the
	 * timestamp and size of the files.
	 * @see AnalysisLevel
	 * Populate the subtree from its elemRoot with the structure found on the mirror
	 * at its path. 
	 * The internal representation of the mirror structure is a dom4j XML document.
	 * @param element
	 * @param path
	 */
	private void acquire(Element root, String path) throws IOException {
		List<IFile> files = conn.lsFrom(path);		// can throw exception
		
		if(files == null)
			throw new IOException("Connection (" + conn + ") could not return a list of files");
		
		// filtering files from directories ; adding them to the structure
		ListIterator<IFile> iterator = files.listIterator();
		while(iterator.hasNext()) {
			IFile file = iterator.next();
			
			// debug::prints everything on the console
			//System.err.println(file);
			
			if(file instanceof IDirectory) {
				// add the new directory to the structure
				Element newDir = root.addElement(DIRECTORY)
					.addAttribute(MirrorFile.NAME, file.getName());
				logger.debug("acquiring dir: " + file.getName());
				
				if(analysis.getVerbosity().equals(AnalysisLevel.VERBOSE))
					newDir.addAttribute(MirrorFile.DATE, file.getDate().asISO8601())
					.addAttribute(MirrorFile.SIZE, Long.valueOf(file.getSize()).toString());
				
				// recursive call
				if(path.endsWith("/"))
					acquire(newDir, path + file.getName());
				else
					acquire(newDir, path + "/" + file.getName());
				
			}else
				if(analysis.getDepth().equals(AnalysisLevel.COMPLETE)) {
					// add also the new file to the structure
					Element newFile = root.addElement(FILE);
					logger.debug("acquiring file: " + file.getName());
					if(analysis.getVerbosity().equals(AnalysisLevel.VERBOSE))
						newFile.addAttribute(NAME, file.getName())
						.addAttribute(MirrorFile.DATE, file.getDate().asISO8601())
						.addAttribute(SIZE, Long.valueOf(file.getSize()).toString());
				}
		}
	}

	public String getFilename() {
		/* old fashion: for a filesystem implementation (in TableMirrorStructure?
		StringBuffer name = new StringBuffer(mirror.getName());
		
		if(analysis.getVerbosity().equals(AnalysisLevel.VERBOSE))
			name.append(FILENAME_VERBOSE);
		
		if(analysis.getDepth().equals(AnalysisLevel.COMPLETE))
			name.append(FILENAME_COMPLETE);
		else
			name.append(FILENAME_PARTIAL);
		
		name.append(FILENAME_ENDING);
		return name.toString();
		*/
		return getDocumentID() + ".xml";
	}

	public String getFilePath() {
		//return Config.getBasePath() + getFilename();
		return Config.getBasePathXMLStorage() + getFilename();
	}
	
	
	public Document getDocument() {
		return doc;
	}

	/**
	 * This method takes a xpath and returns a valid file system
	 * path for it.
	 * 
	 * xpath is of the form: /mirror/dir[@name = 'x']/(...)/file[@name = 'y']
	 * 
	 * Note: only element xpath are valid (eg. /mirror/text() is not valid)
	 * 
	 * @return String path of the file
	 */
	public static String computePath(String xpath) {
		String[] parts = xpath.split("/");
		StringBuffer sb = new StringBuffer("/");
		
		// debug
		//System.err.println("Computing path for XPATH=" + xpath);
		
		if(!parts[1].equals("mirror"))
			return null;
		
		for(int i = 2; i<parts.length; i++) {
			String[] parts2 = parts[i].split("\\[");
			String type = parts2[0];
			
			String filename = parts2[1].split("'")[1];
			sb.append(filename);
			
			if(type.equals("dir")) {				
				sb.append("/");
			}else
				break;
		}
		
		return sb.toString();
	}
	
	/**
	 * This method takes a xpath and returns a valid file system
	 * path for it.
	 * 
	 * path is of the form: /dir1/dir2/file
	 * or : /dir1/dir2/dir3/
	 * 
	 * @return String path of the file
	 */
	public static String computeXPath(String path) {
		StringBuffer sb = new StringBuffer("/mirror");	// root directory
		String[] parts = path.split("/");
		
		for(int i = 1; i<parts.length - 1; i++) {
			sb.append("/dir[@name = '").append(parts[i]).append("']");
		}
		
		// file or directory (if ending with /)
		if(path.endsWith("/")) 
			sb.append("/dir[@name = '").append(parts[parts.length-1]).append("']");
		else
			sb.append("/file[@name = '").append(parts[parts.length-1]).append("']");
		
		return sb.toString();
	}

	/**
	 * Transform a non-indexed XPATH (which would give many results) into
	 * a custom-indexed XPATH which will reference in a unique way
	 * the file.
	 * 
	 * eg.
	 *   /mirror/dir/file
	 * should become
	 *   /mirror/dir[@name = 'dir1Name']]/file[@name = 'filename']
	 * 
	 * @param String xpath String non-indexed
	 * @param Element elt the node we want to find the path of
	 */
	public static String enrichXPATH(String xpath, Element elementReferencedByXPATH) {
		Element cur = elementReferencedByXPATH;		// current element pointing now to elt
		
		/*
		 * process the later part, if it is a file (non-directory)
		 */
		if(xpath.contains("file")) {
			// it has to be the last part of the expression, just add the index after it
			xpath += "[@name='" + ((Element)elementReferencedByXPATH)
										.selectSingleNode("@name").getStringValue() + "']";
			//isDir = false;
			cur = elementReferencedByXPATH.getParent();	// moving up one element
		}
		
		/*
		 * process the remaining "dir" parts of the xpath expression
		 * from the last one to the first one, going up from the node 
		 * to the root.
		 * Adding index from XML data for each one of them.
		 */
		int lastIdx = xpath.lastIndexOf("/dir");	// last index of "dir" in xpath
		if(lastIdx > -1) {
			// there exist at least one occurrence of "dir" within the xpath expression
			
			// part before the last occurrence of "/dir"
			String curPart = xpath;
			
			do{
				curPart = curPart.substring(0, lastIdx);

				String rest = xpath.substring(lastIdx + 4);	// remaining part is all good, keep it
				
				String enrichedDirPart = "/dir[@name='"
					+ ((Element)cur).attributeValue("name") + "']";
				xpath = curPart + enrichedDirPart  + rest;

				// loop management
				cur = cur.getParent();	// moving up one element
				lastIdx = curPart.lastIndexOf("/dir");
				
			}while(curPart.contains("/dir"));
			
		}
		
		return xpath;
	}

	public long getTimeStamp() {
		return timestamp;
	}

	/*
	public String getMirrorID() {
		return host + "_" + protocol;	// reputed unique for a mirror
	}
	*/
	
	public String getMirrorID() {
        String mirrorID = "none";
		try {
			mirrorID = mirror.getMirrorID();
	        
		} catch (ConfigException e) {
			logger.error("At this point the mirror must have a mirrorID which it has not: " + e.getMessage());
		}
		return mirrorID;
	}
	
	public String getDocumentID() {
		return getMirrorID() + "-" + timestamp;	// reputed unique among structures
	}

	public IMirror getMirror() {
		return mirror;
	}

	/**
	 * Modify this mirror structure to put it up to data
	 * based on the delta provided as a parameter
	 */
	public void patch(IMirrorDelta delta) {
		
		/*
		 * ADDING new files
		 */
		List addedFiles = delta.getNewFiles();
		ListIterator itAdd = addedFiles.listIterator();
		while(itAdd.hasNext()) {
			Element elt = (Element)itAdd.next();
			String xpath = elt.attributeValue(XPATH);
			String date	= elt.attributeValue(DATE);
			String size	= elt.attributeValue(SIZE);
			
			// count on the fact the above directories are already setup
			// otherwise, introduce a new attribute depth in delta to proceed
			// by directory depth (in case dom4j does change node order)
			String dirXPATH = xpath.substring(0, xpath.lastIndexOf("/"));
			
			String targetPart = xpath.substring(xpath.lastIndexOf("/")+1);
			String type = targetPart.split("\\[")[0];
			String name = targetPart.split("'")[1];
			
			Element dir = (Element)root.selectSingleNode(dirXPATH);
			dir.addElement(type)
				.addAttribute(NAME, name)
				.addAttribute(DATE, date)
				.addAttribute(SIZE, size);
		}
		
		/*
		 * DELETING old files
		 */
		List deletedFiles = delta.getDeletedFiles();
		ListIterator itDel = deletedFiles.listIterator();
		while(itDel.hasNext()) {
			Element elt = (Element)itDel.next();
			String xpath = elt.attributeValue(XPATH);
			
			Node node = root.selectSingleNode(xpath);
			node.detach();
			node = null;
		}
		
		/*
		 * UPDATING some files
		 */
		List updatedFiles = delta.getUpdatedFiles();
		ListIterator itUp = updatedFiles.listIterator();
		while(itUp.hasNext()) {
			Element elt = (Element)itUp.next();
			String xpath = elt.attributeValue(XPATH);
			
			// get the referenced element
			Element cur = (Element)root.selectSingleNode(xpath);
			
			// update date if requested
			Attribute attr = elt.attribute(DATE);
			if(attr != null) {
				Attribute a = cur.attribute(DATE);
				a.setValue(attr.getValue());
			}		

			// update size if requested
			attr = elt.attribute(SIZE);
			if(attr != null) 
				cur.attribute(SIZE).setValue(attr.getValue());
		}
		
	}

	/**
	 * Returns true if this structure represents a master structure
	 * (ie taken from the master mirror)
	 * @return boolean true if master
	 */
	public boolean isMaster() {
		return type.equals("master");
	}
}
