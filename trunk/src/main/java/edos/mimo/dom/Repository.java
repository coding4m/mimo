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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import edos.distribution.mirror.IRepository;
import edos.distribution.mirror.Repository;
import edos.mimo.Config;
import edos.mimo.IMasterMirror;
import edos.mimo.IMirror;
import edos.mimo.IMirrorDelta;
import edos.mimo.IPass;
import edos.mimo.connection.IConnection;
import edos.mimo.exception.ConfigException;
import edos.mimo.exception.ConnectionException;
import edos.mimo.filesystem.IDirectory;
import edos.mimo.filesystem.IFile;

public class Repository implements IRepository {
	private static final String MIRROR_ID = "mirrorid";
	private static final String REPOSITORY_PATH = "path";
	private static Logger logger = Logger.getLogger(Repository.class);
	private Document doc;
	private Element  root;
	private IConnection conn;
	private IMirror mirror;
	private long timestamp = 0; // creation date of this structure (long gives 16% gain in size over readable String)
	
	
	/**
	 * Forces the construction of the structure based on some specific analysis
	 * level.
	 * @param mirror
	 * @param analysis
	 */
	public Repository(IMirror mirror, String repositoryPath) throws IOException {
		this.mirror = mirror;
		IPass pass = mirror.getActivePass();
		String mirrorID;
		try {
			mirrorID = mirror.getMirrorID();
		
		} catch (ConfigException e1) {
			logger.error(e1.getMessage());
			logger.error(e1.getStackTrace());
			throw new IOException("Unable to retrieve mirror ID for mirror " + mirror);
		}
		
		doc= DocumentHelper.createDocument();
        root = doc.addElement( "repository" )
				.addAttribute(MIRROR_ID, mirrorID)
        		.addAttribute(REPOSITORY_PATH, repositoryPath);
        mirror.connect();
        conn = mirror.getConnection();
        
        if(!conn.isValidPath(pass.getAccess().getLocalPath() + repositoryPath)) {
        	mirror.disconnect();
        	throw new ConnectionException("invalid URL " + pass.getAccess().getLocalPath() + repositoryPath);
        }
        
        // starting time
        mirror.setCheckinTime(GregorianCalendar.getInstance().getTimeInMillis());
        
		try {
			mirrorID = mirror.getMirrorID();
	        // recursively fill the document (tree) with the file structure
	        logger.info("Starting acquisition process for repository " + mirrorID + repositoryPath);
	        acquire(root, pass.getAccess().getLocalPath() + repositoryPath);
	        logger.info("Completed acquisition process for repository " + mirrorID + repositoryPath);
	        
		} catch (ConfigException e) {
			throw new IOException(e.getMessage());
			
		}finally {
			if(mirror != null)
				mirror.disconnect();
			
		}

        // finish time
		mirror.setCheckoutTime(GregorianCalendar.getInstance().getTimeInMillis());
        
        root.addAttribute(CHECKINTIME, Long.toString(mirror.getCheckinTime()))
        	.addAttribute(CHECKOUTTIME, Long.toString(mirror.getCheckoutTime()));
        
        // creation date of this structure
        timestamp = mirror.getCheckoutTime();
        
	}
	
	
	private void acquire(Element root, String path) throws IOException {
		logger.setLevel(Level.DEBUG);
		logger.debug("acquiring with path=" + path);
		logger.debug("connection: " + conn);
		
		List<IFile> files = conn.lsFrom(path);
		
		if(files == null)
			throw new IOException("Connection (" + conn + ") could not return a list of files");
		
		// filtering files from directories ; adding them to the structure
		ListIterator<IFile> iterator = files.listIterator();
		while(iterator.hasNext()) {
			IFile file = iterator.next();
			
			if(!(file instanceof IDirectory)) {
				Element newFile = root.addElement(FILE);
				logger.debug("acquiring file: " + file.getName());
				newFile.addAttribute(NAME, file.getName())
					.addAttribute(DATE, Long.toString(file.getDate().getTimeInMillis()))
					.addAttribute(SIZE, Long.valueOf(file.getSize()).toString());
			}
			
		}
	}

	public Document getDocument() {
		return doc;
	}

	public String getDocumentID() {
		return "repository-" + timestamp;	// reputed unique among structures
	}

	public String getFilePath() {
		return Config.getBasePathXMLStorage() + getFilename();
	}

	public String getFilename() {
		return getDocumentID() + ".xml";
	}

	public IMirror getMirror() {
		return mirror;
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

	public long getTimeStamp() {
		return timestamp;
	}

	public List<IDirectory> getTopDirectories() {
		return null;
	}
	
	public void patch(IMirrorDelta delta) {
		logger.info("method not implemented");
	}

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


	public boolean isMaster() {
		return (mirror instanceof IMasterMirror);
	}

}
