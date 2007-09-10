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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import edos.mimo.connection.IConnection;
import edos.mimo.dom.TreeMirrorStructure;


/**
 * Singleton class helping to build all kinds of mirror objects.
 *
 */
public class MirrorFactory implements IMirrorFactory {
	private static MirrorFactory instance;
	
	private MirrorFactory() {
	}
	
	public static MirrorFactory getInstance() {
		if(instance == null)
			instance = new MirrorFactory();
		return instance;
	}
	
	/*
	 * MASTER MIRROR (non-Javadoc)
	 * @see org.edos_project.mirror.IMirrorFactory#getMaster(java.lang.String, java.util.List)
	 */
	/*
	public IMasterMirror getMaster(String name, List<IPass> passes) {
		IMasterMirror m = new MasterMirror(name, passes);
		return m;
	}

	public IMasterMirror getMaster(String name, List<Access> accessList, List<Identity> defaultIds) {
		List<IPass> passes = createPasses(accessList, defaultIds);
		
		IMasterMirror m = new MasterMirror(name, passes);
		return m;
	}
	*/
	
	public IMasterMirror getMaster(String name, Access a, Identity id) {
		List<IPass> passes = new LinkedList<IPass>();
		passes.add(new Pass(a, id));
		return new MasterMirror(name, passes);
	}

	public IMasterMirror getMaster(String mirrorName, ArrayList<Access> accessList, ArrayList<Identity> ids,
			AnalysisLevel analysis, String cronExpression) {
		List<IPass> passes = createPasses(accessList, ids);
		
		IMasterMirror m = new MasterMirror(mirrorName, passes, analysis, cronExpression);
		return m;
	}

	public IMasterMirror getMaster(String mirrorName, ArrayList<Access> accessList, ArrayList<Identity> ids,
			AnalysisLevel analysis, String cronExpression, int storageFormat) {
		List<IPass> passes = createPasses(accessList, ids);
		
		IMasterMirror m = new MasterMirror(mirrorName, passes, analysis, cronExpression, storageFormat);
		return m;
	}
	
	
	/*
	 * SECONDARY MIRROR (non-Javadoc)
	 * @see org.edos_project.mirror.IMirrorFactory#getSecondary(java.lang.String, java.util.List)
	 */
	public ISecondaryMirror getSecondary(String name, List<IPass> passes) {
		ISecondaryMirror m = new SecondaryMirror(name, passes);
		return m;
	}

	public ISecondaryMirror getSecondary(String name, List<Access> accessList,
			List<Identity> defaultIds, String cronExpression) {
		List<IPass> passes = createPasses(accessList, defaultIds);
		
		ISecondaryMirror m = new SecondaryMirror(name, passes, cronExpression);
		return m;
	}
	
	public ISecondaryMirror getSecondary(String name, List<IPass> passes, String cronExpression) {
		ISecondaryMirror m = new SecondaryMirror(name, passes, cronExpression);
		return m;
	}
	
	/**
	 * A shorthand when only one access+identity is used (main situation).
	 * @param name mirror name
	 * @param p IPass
	 * @return secondary mirror
	 */
	public ISecondaryMirror getSecondary(String name, IPass p) {
		List<IPass> passes = new LinkedList<IPass>();
		passes.add(p);
		
		return new SecondaryMirror(name, passes);
	}
	
	/**
	 * A shorthand when only one access+identity is used (main situation).
	 * @param name mirror name
	 * @param a Access
	 * @param id Identity
	 * @return secondary mirror
	 */
	public ISecondaryMirror getSecondary(String name, Access a, Identity id) {
		
		List<IPass> passes = new LinkedList<IPass>();
		passes.add(new Pass(a, id));
		
		return new SecondaryMirror(name, passes);
	}
	
	/*
	 * PRIVATE METHODS
	 */
	private static List<IPass> createPasses(List<Access> accessList, List<Identity> defaultIds) {
		LinkedList<IPass> passes = new LinkedList<IPass>();
		Iterator<Access> accesses = accessList.iterator();
		Iterator<Identity> ids	  = defaultIds.iterator();
		
		// building IPasses ready for use
		while(accesses.hasNext()) {
			Access access = accesses.next();	// first access is main access
			
			while(ids.hasNext()) {
				Identity id = ids.next();		// first id is main id
				IPass pass = new Pass(access, id);
				passes.add(pass);
			}
		}
		return passes;
	}

	/**
	 * Parses a Dom4j element to produce a mirror.
	 * Called by TreeMirrorStructure while loading 
	 * from file.
	 * 
	 * TODO move this code to TreeMirrorStructure
	 * within a constructor (such as the one taking
	 * an InputStream)
	 */
	public IMirror getMirror(File file) throws DocumentException {
		SAXReader xmlReader = new SAXReader();
	    Document doc = xmlReader.read(file);
	    Element root = doc.getRootElement();

		String type = root.attributeValue(AbstractMirror.TYPE);
		String name = root.attributeValue(AbstractMirror.NAME);
		String host = root.attributeValue(AbstractMirror.HOST);
		String protocol = root.attributeValue(AbstractMirror.PROTOCOL);
		String path = root.attributeValue(AbstractMirror.PATH);

		Access access = new Access(host, protocol, path);
		// TODO implement Identity registry , BD where relations Identity-Access persist
		Identity id = new Identity("fake name", "fake password", null);
		
		IConnection conn = null;
		AnalysisLevel analysis = new AnalysisLevel();	//default
		
	    IMirror mirror = (type.equals(AbstractMirror.MASTER))?
	    		getMaster(name, access, id):getSecondary(name, access, id);
	    
	    mirror.setStructure(new TreeMirrorStructure(doc, root, conn,
	    		analysis, mirror ));
	    
	    return mirror;
	}
}
