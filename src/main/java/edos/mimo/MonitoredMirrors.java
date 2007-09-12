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
package edos.mimo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import edos.mimo.exception.ConfigException;


/**
 * Singleton class containing the list of mirrors registered
 * to the system and if their monitoring is active or not.
 * 
 * This list does not include the master mirror, only the
 * secondary mirrors.
 * 
 * @author marc
 *
 */
public class MonitoredMirrors {
	private static Logger logger = Logger.getLogger(MonitoredMirrors.class);
	
	private static final MonitoredMirrors instance = new MonitoredMirrors();
	private Hashtable<String,IMirror> mirrors = new Hashtable<String, IMirror>();
	private Document doc = DocumentHelper.createDocument();
	private Element  root = doc.addElement("mirrors").addAttribute("timestamp", new Date().asISO8601());

	private File file = new File("mirrors.xml");
	
	public static MonitoredMirrors getInstance() {
		return instance;
	}
	
	private MonitoredMirrors() {
		// nobody can call me! Only through getInstance()..
	}
	
	/**
	 * Build the Mirror List by taking a list of dom4j nodes 
	 * and copying them directly into the document (after 
	 * changing the name of the elements for "mirror").
	 * 
	 * Advantage: mirrorlist has the same format as the mirror
	 * metrics configuration file
	 * 
	 * @param list
	 
	public void add(List<Node> list) {
		Iterator it = list.iterator();
		while(it.hasNext()) {
			Object elt = it.next();
			if(elt instanceof Element) {
				Element mirrorElt = ((Element)elt).createCopy("mirror");
				root.add(mirrorElt);
				
			}else{
				logger.error("need a list of dom4j nodes!");
				return;
			}
		}
	}
	*/
	
	/**
	 * Add a mirror to the list. But reject any non-tested mirror.
	 * 
	 * @param mirror
	 * @throws ConfigException 
	 */
	public void add(IMirror mirror) throws ConfigException {
		// will throw ConfigException if it has no ID
		mirror.getMirrorID();
		//logger.info("adding mirror : " + mirror.getMirrorID());	// does not accept mirrors without an ID, aka not tested
		
		// should not happen
		if(mirror.getActivePass() == null) {
			logger.error("mirror discarded! It is forbidden to add a mirror with no pass, which has not being visited before");
			return;
		}
		
		try {
			if(root.selectSingleNode("//mirror[@mirrorid = '" + mirror.getMirrorID() + "']") != null) {
				logger.error("This mirror is already in the list");
				return;
			}
		} catch (ConfigException e) {
			logger.error(e.getMessage());
			return;
		}
		
		logger.info("adding mirror " + mirror.getMirrorID());
		root.addAttribute("timestamp", new Date().asISO8601());
		Element mirrorElt = root.addElement("mirror")
								.addAttribute("name", mirror.getName())
								.addAttribute("mirrorid", mirror.getMirrorID())
								.addAttribute("host", mirror.getActivePass().getAccess().getHost());
		
		Access a = mirror.getActivePass().getAccess();
		mirrorElt.addElement("access")
					//.addAttribute("host", a.getHost())
					.addAttribute("path", a.getLocalPath())
					.addAttribute("protocol", a.getProtocol())
					.addAttribute("port", new Integer(a.getPort()).toString())
					.addAttribute("maxlag", new Integer(a.getAcceptableLag()).toString());
		
		IPass p = mirror.getActivePass();
		Element userElt = mirrorElt.addElement("user")
					.addAttribute("login", p.getIdentity().getLogin())
					.addAttribute("realname", p.getIdentity().getRealName());
		userElt.addElement("passwd")
				.addText(p.getIdentity().getPassword());
		if(p.getIdentity().getKeyfile() != null)
					userElt.addElement("dsakey")
					.addText(p.getIdentity().getKeyfile().getAbsolutePath());
		
		// saves the reference in the hashtable
		mirrors.put(mirror.getMirrorID(), mirror);
		//logger.info("mirror " + mirror + " is added to the monitored list");
	}
	
	/**
	 * Remove a mirror from the list
	 * 
	 * @param mirror
	 */
	public void remove(IMirror mirror) {
		Element mirrorElt;
		List l;
		try {
			mirrorElt = (Element)doc.selectSingleNode("//mirror[@mirrorid = '" + mirror.getMirrorID() + "']");
			
			if(mirrorElt == null)	{ // trying to get all mirrors by this name then
				l = doc.selectNodes("//mirror[@name = '" + mirror.getName() + "']");
				Iterator it = l.iterator();
				while(it.hasNext())
					doc.remove((Element)it.next());
			}else{

				root.addAttribute("timestamp", new Date().asISO8601());
				root.remove(mirrorElt);
				mirrors.remove(mirror.getMirrorID());
				logger.debug("removed " + mirror);
			}
			
		} catch (ConfigException e) {
			logger.error(e.getMessage());
		} 
	}
	
	public void remove(String mirrorID) {
		Element mirrorElt = (Element)doc.selectSingleNode("//mirror[@mirrorid = '" + mirrorID + "']");
		if(mirrorElt == null) {
			logger.error("no such mirror with id=" + mirrorID);
			logger.error(this.toString());
		
		}else{
			root.addAttribute("timestamp", new Date().asISO8601());
			root.remove(mirrorElt); 
			mirrors.remove(mirrorID);
			logger.debug("removed " + mirrorID);
		}
	}
	
	/**
	 * Remove an access from mirror definition
	 * @param mirror
	 * @param acess
	 */
	public void remove(IMirror mirror, Access access) {
		try {
			Element accessElt = (Element)doc.selectSingleNode("//access[../@mirrorid = '" + mirror.getMirrorID() 
					+ "' and @protocol='" + access.getProtocol()
					+ "' and @path='" + access.getLocalPath()
					+ "']");
			if(accessElt != null) {
				doc.remove(accessElt);
				root.addAttribute("timestamp", new Date().asISO8601());
			}
			
		} catch (ConfigException e) {
			logger.error(e.getMessage());
		}		
	}
	
	public Iterator<String> getMirrorIDs() {
		return mirrors.keySet().iterator();
	}
	
	public IMirror get(String mirrorID) {
		return mirrors.get(mirrorID);
	}
	
	/**
	 * Show mirror list as an XML string
	 * 
	 * @return String
	 */
	public String toString() {
		return doc.asXML();
	}

	/**
	 * @param file
	 */
	public void setSnapshotFile(File file) {
		this.file  = file;
	}

	public void save() {
		logger.info("saving to " + file.getAbsolutePath());
		
		try {
			PrintWriter out = new PrintWriter(file);
			out.write(instance.toString());
			out.close();
			
		} catch (FileNotFoundException e) {
			logger.error(e);	// should not happen
		}
	}

	public String getDocumentID() {
		return "mirrors-" + new Date().getTimeInMillis();	// reputed unique among structures
	}

	public String getFilePath() {
		return Config.getBasePathXMLStorage() + getFilename();
	}

	public String getFilename() {
		return getDocumentID() + ".xml";
	}

	
	
}
