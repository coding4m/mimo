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
package edos.mimo.dom.db;

import com.sleepycat.db.DatabaseException;
import com.sleepycat.db.Environment;
import com.sleepycat.db.EnvironmentConfig;
import com.sleepycat.dbxml.XmlContainer;
import com.sleepycat.dbxml.XmlContainerConfig;
import com.sleepycat.dbxml.XmlDocument;
import com.sleepycat.dbxml.XmlDocumentConfig;
import com.sleepycat.dbxml.XmlException;
import com.sleepycat.dbxml.XmlManager;
import com.sleepycat.dbxml.XmlManagerConfig;
import com.sleepycat.dbxml.XmlQueryContext;
import com.sleepycat.dbxml.XmlQueryExpression;
import com.sleepycat.dbxml.XmlResults;
import com.sleepycat.dbxml.XmlTransaction;
import com.sleepycat.dbxml.XmlUpdateContext;
import com.sleepycat.dbxml.XmlValue;

import edos.distribution.mirror.BDBXMLManager;
import edos.mimo.Config;
import edos.mimo.IMirror;
import edos.mimo.IMirrorDelta;
import edos.mimo.IMirrorStatus;
import edos.mimo.MonitorApplication;
import edos.mimo.MonitoredMirrors;
import edos.mimo.dom.DOMMirrorDiff;
import edos.mimo.dom.IDOMMirrorDelta;
import edos.mimo.dom.IDOMMirrorDiff;
import edos.mimo.dom.IRepository;
import edos.mimo.dom.ITreeMirrorStructure;
import edos.mimo.dom.TreeMirrorStructure;
import edos.mimo.exception.BDBXMLException;
import edos.mimo.exception.ConfigException;
import edos.mimo.filesystem.IMirrorDiff;
import edos.mimo.statistics.WorkflowStatistics;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dom4j.DocumentException;

/**
 * This class is a wrapper for the Berkeley DB XML
 * <code>XmlManager</code> class.
 * 
 * It follows the singleton pattern.
 * TODO make this class transaction aware (no singleton)
 * 
 * This instance is called to get a handle on the
 * XML documents (to be) stored in the database.
 * 
 * TODO (note): this singleton can have synchronized methods (instead of transactions)
 * 
 * TODO when Berkeley DB XML makes it available for the Java API, use XmlDocument::getContentAsDOM()
 * (currently works only with C++ API ; better than serializing to string to re-parse it!)
 * (note: dom4j is able to work with other DOMs 
 * @see http://www.dom4j.org/clover/org/dom4j/io/DOMWriter.html)
 * 
 * Lots of assistance online by George Feinberg
 * @see http://forums.oracle.com/forums/thread.jspa?forumID=274&threadID=430517
 * 
 * @author marc
 *
 */
public class BDBXMLManager {
	private static Logger logger = Logger.getLogger(BDBXMLManager.class);
	
	// DBXML debugging
	private static boolean BDBXMLDebug = false;
	
	private static BDBXMLManager instance = null;
	private static XmlManager manager = null;
	private static Config config = null;
	private static Hashtable<String, XmlContainer> containers = new Hashtable<String,XmlContainer>();

	private static final String MASTER_CONTAINER 	= "master.dbxml";
	private static final String MONITORED_CONTAINER = "monitored.dbxml";
	private static final String REPOSITORY_CONTAINER = "repository.dbxml";
	private static final String MIRRORS_CONTAINER = "mirrors.dbxml";
	private static final String STATISTICS_CONTAINER	= "statistics.dbxml";
	private static final String CONTAINER			= "container";	// name of the XQUERY container variable
	private static final String DOCUMENT 			= "doc";	// name of the XQUERY doc variable
	private static final String MIRRORID 			= "mirrorid";// name of the XQUERY mirrorid variable




	
	/*
	 * XQUERY
	 * Prepared queries.
	 * XQUERY can be used for XML database and relational databases.
	 * 
	 * These queries are documented under /doc/XQuery
	 */
	private XmlQueryContext defaultContext;
	
	private static String latestMasterStructQuery =
			"collection('" + MASTER_CONTAINER + "')/mirror[last()]";
	
	private static String latestFullStructInContainerQuery = 
		  "let $latestTimestamp := max(xs:dateTime(collection($container)/mirror/@checkoutTime))\n"
		+ "return collection($container)/mirror[@checkoutTime = $latestTimestamp]";

	private static String latestDeltaInContainerQuery = 
	 		  "let $latestTimestamp := max(collection($container)/delta/@timestamp)\n"
			+ "return collection($container)[delta/@timestamp = $latestTimestamp][1]";
	
	private static String latestDiffForMirrorInContainerQuery = 
			  "for $latestTimestamp in max(collection($container)/diff[@mirror = $mirrorid]/xs:dateTime(@timestamp))"
			+ "return collection($container)[diff/@mirror = $mirrorid and diff/@timestamp = $latestTimestamp]";
	
	private static String aGivenDocumentInContainerQuery = "doc(concat($container, '/', $doc))";
	private static String allDocumentsInContainerQuery = "collection($container)";
	
	private static String allDeltasFromLastFullStructQuery = 
			  "let $latestFullStructTimestamp := max(collection($container)/mirror/@checkoutTime)\n"
			+ "return collection($container)[delta/@timestamp > $latestFullStructTimestamp]";
	
	private static String mirrorTypeQuery = "collection($container)/mirror[@type = 'master']";
	
	// statistics
	private static String countFullStructsInContainerQuery = "count(collection($container)[mirror])";
	private static String countDeltasInContainerQuery = "count(collection($container)[delta])";
	private static String countAllDocsInContainerQuery = "count(collection($container))";

	
	private XmlQueryExpression latestMasterStructQueryExp = null;
	private XmlQueryExpression latestFullStructInContainerQueryExp = null;
	private XmlQueryExpression latestDeltaInContainerQueryExp = null;
	private XmlQueryExpression latestDiffForMirrorInContainerQueryExp = null;
	private XmlQueryExpression aGivenDocumentQueryExp = null;
	private XmlQueryExpression allDocumentsQueryExp = null;
	private XmlQueryExpression allDeltasFromLastFullStructQueryExp = null;
	private XmlQueryExpression countFullStructsInContainerQueryExp = null;
	private XmlQueryExpression countDeltasInContainerQueryExp = null;
	private XmlQueryExpression countAllDocsInContainerQueryExp = null;
	private XmlQueryExpression mirrorTypeQueryExp = null;
	
	/**
	 * Singleton access method
	 * @return an instance of me
	 */
	public static BDBXMLManager getInstance() throws BDBXMLException {
		if(instance == null)
			instance = new BDBXMLManager();
		
		return instance;
	}

	/**
	 * Singleton access method
	 * 
	 * Sets the debug flag for BDB XML
	 * and reopens a manager with the new config
	 * @param BDBXMLDebug (true of false)
	 * @return an instance of me
	 */
	public static BDBXMLManager getInstance(boolean debug) throws BDBXMLException {
		if(BDBXMLDebug != debug) {
			BDBXMLDebug = debug;
			
			// must shutdown and restart with new initialization options
			if(instance != null)
				instance.close();	
			
			instance = new BDBXMLManager();
			
		}else{

			// no change in debug option, can proceed with the same config, create new one if needed
			if(instance == null)
				instance = new BDBXMLManager();
		}
		
		return instance;
	}
	
	/** 
	 * Singleton's private constructor
	 *
	 */
	private BDBXMLManager() throws BDBXMLException {
		logger.info("Instantiating a BDBXMLManager");
		
		/*
		 * Main configuration
		 */
		Environment myEnv = null;
		File envHome = new File(Config.getBDBXMLEnvHome());
		XmlTransaction txn = null;
		
		try {
			// BDBXMLManager needs to know about Config (to get delta limit config for example)
			// Master mirror configuration 
			if(!Config.isLoaded()) {
				logger.fatal("Configuration is not loaded yet. The BDB XML database requires it to start!");
				throw new BDBXMLException("Config has not been loaded. The database can't be started");
			}
			config = Config.getInstance();
			
			// BDB XML Configuration
		    EnvironmentConfig envConf = new EnvironmentConfig();
		    envConf.setAllowCreate(true);       // If the environment does not
		                                        // exits, create it.
		    envConf.setInitializeCache(true);   // Turn on the shared memory
		                                        // region.
		    envConf.setCacheSize(7*1024*1024);	// TODO is 7MB big enough?
		    envConf.setInitializeLocking(true); // Turn on the locking subsystem.
		    envConf.setInitializeLogging(true); // Turn on the logging subsystem.
		    envConf.setTransactional(true);     // Turn on the transactional
		                                        // subsystem.
		    
		    myEnv = new Environment(envHome, envConf);
		    XmlManagerConfig managerConfig = new XmlManagerConfig();
		    //managerConfig.setAllowExternalAccess(true);	// necessary to find dtd files @see http://www.sleepycat.com/xmldocs/java/com/sleepycat/dbxml/XmlManagerConfig.html#setAllowExternalAccess(boolean)
		    managerConfig.setAdoptEnvironment(true);	// manager will take care of closing myEnv
		    manager = new XmlManager(myEnv, managerConfig);
		    
		    // DEBUG settings
		    if(BDBXMLDebug) {
			    XmlManager.setLogLevel(XmlManager.LEVEL_ALL, true);
			    XmlManager.setLogCategory(XmlManager.CATEGORY_ALL, true);
		    }

			logger.info("BDBXMLManager created and configured");
			
			/*
			 * OPTIMIZATION
			 * According to BDB XML documentation opening a container is expensive
			 * while maintaining the container open only consumes a small memory
			 * footprint. Let's open all containers as they will be queried periodically.
			 * (At least the master.dbxml container should be open as it is queried by
			 * everyone).
			 * 
			 * Open all known containers (ie. already existing).
			 * 
			 * TRANSACTIONS
			 * Here, the containers are also set up to handle transactions. A nice
			 * side-effect is that BDBXML will automatically transactionally protect
			 * all write operations to these containers (as long as they are not closed).
			 */
			List existingContainerNames = getContainers(); // opens already existing containers
			ListIterator it = existingContainerNames.listIterator();
			while(it.hasNext()) {
				String name = (String)it.next();	
				openContainer(name);
			}
			
			logger.info("All containers open and ready for transaction");
			
			
			/*
			 * PREPARED QUERIES
			 * Creating queries is expensive, hence preparing queries is recommanded.
			 */
			
			// this default context is not thread-safe
			// and it should not be used at query execution time
			// Here it tells the query compiler about the presence of variables...
			defaultContext = manager.createQueryContext();
			defaultContext.setReturnType(XmlQueryContext.DeadValues);
			defaultContext.setEvaluationType(XmlQueryContext.Eager);
			defaultContext.setVariableValue(CONTAINER, new XmlValue("undefined"));
			defaultContext.setVariableValue(DOCUMENT, new XmlValue("undefined"));
			defaultContext.setVariableValue(MIRRORID, new XmlValue("undefined"));
			
			// next query needs an open master.dbxml container
			if(manager.existsContainer(MASTER_CONTAINER) == 0) {
				XmlContainerConfig containerConf = new XmlContainerConfig();
				containerConf.setTransactional(true);
				containerConf.setAllowCreate(true);
				
				txn = manager.createTransaction();
				manager.createContainer(txn, MASTER_CONTAINER);
				txn.commit();
				if(txn != null) txn.delete();
			}
			
			txn = manager.createTransaction();
			latestMasterStructQueryExp = manager.prepare(txn,
						latestMasterStructQuery, defaultContext);
			txn.commit();
			if(txn != null) txn.delete();

			txn = manager.createTransaction();
			latestFullStructInContainerQueryExp = manager.prepare(txn,
						latestFullStructInContainerQuery, defaultContext);
			txn.commit();
			if(txn != null) txn.delete();

			txn = manager.createTransaction();
			latestDeltaInContainerQueryExp = manager.prepare(txn,
						latestDeltaInContainerQuery, defaultContext);
			txn.commit();
			if(txn != null) txn.delete();

			txn = manager.createTransaction();
			latestDiffForMirrorInContainerQueryExp = manager.prepare(txn,
						latestDiffForMirrorInContainerQuery, defaultContext);
			txn.commit();
			if(txn != null) txn.delete();

			txn = manager.createTransaction();
			aGivenDocumentQueryExp = manager.prepare(txn, aGivenDocumentInContainerQuery, defaultContext);
			txn.commit();
			if(txn != null) txn.delete();
			
			txn = manager.createTransaction();
			allDocumentsQueryExp = manager.prepare(txn, allDocumentsInContainerQuery, defaultContext);
			txn.commit();
			if(txn != null) txn.delete();
			
			txn = manager.createTransaction();
			allDeltasFromLastFullStructQueryExp = manager.prepare(txn,
						allDeltasFromLastFullStructQuery, defaultContext);
			txn.commit();
			if(txn != null) txn.delete();

			txn = manager.createTransaction();
			countFullStructsInContainerQueryExp = manager.prepare(txn,
						countFullStructsInContainerQuery, defaultContext);
			txn.commit();
			if(txn != null) txn.delete();

			txn = manager.createTransaction();
			countDeltasInContainerQueryExp = manager.prepare(txn,
						countDeltasInContainerQuery, defaultContext);
			txn.commit();
			if(txn != null) txn.delete();

			txn = manager.createTransaction();
			countAllDocsInContainerQueryExp = manager.prepare(txn,
						countAllDocsInContainerQuery, defaultContext);
			txn.commit();
			if(txn != null) txn.delete();

			txn = manager.createTransaction();
			mirrorTypeQueryExp = manager.prepare(txn, 
						mirrorTypeQuery, defaultContext);
			txn.commit();
			if(txn != null) txn.delete();
			
			logger.info("Prepared queries... done.");

			
			
			logger.info("Done. A BDBXMLManager is available for all threads.");
			
		} catch (DatabaseException de) {
			logger.fatal("BDB XML error: " + de.getMessage());
			logger.fatal(de.getStackTrace());
			if(txn != null)
				try {
					txn.abort();
				} catch (XmlException e) {
					logger.error(e.getMessage());
				}
			close();
			throw new BDBXMLException(de.getMessage());
			
		} catch (FileNotFoundException fnfe) {
			logger.fatal("BDB XML error: " + fnfe.getMessage());
			logger.fatal(fnfe.getStackTrace());
			close();
			throw new BDBXMLException(fnfe.getMessage());
			
		}catch(ConfigException cfe) {
			logger.fatal("Configuration exception caught while initializing database. "
					+ "This should not happen (the default configuration file should be valid)! "
					+ "Please also make sure the config file is parsed before entering here.");
			close();
			throw new BDBXMLException(cfe.getMessage());
			
		}finally{
			if(txn != null)
				txn.delete();
		}

	}
		
	/**
	 * Open a container and set it up for transactional support
	 * 
	 * Secondary effect: BDBXML will automatically transactionally protect
	 * all write operations to these containers (as long as they are not closed).
	 * 
	 * Index setup is performed at this stage.
	 * 
	 * TODO merge with createContainer() below to eliminate useless duplication
	 * 
	 * @param name
	 * @throws XmlException
	 */
	private XmlContainer openContainer(String name) {
		XmlContainerConfig containerConf = new XmlContainerConfig();
		containerConf.setTransactional(true);
		containerConf.setAllowCreate(true);
		XmlContainer container = null;
		XmlTransaction txn = null;
		
		if(name.equals(Config.LATEST_DIFFS_CONTAINER))
			containerConf.setNodeContainer(false); // wholedoc container (retrieve all docs usually)
		
		try{
			txn = manager.createTransaction();
			container = manager.openContainer(txn , name, containerConf);
			XmlUpdateContext uc = manager.createUpdateContext();
			container.addIndex(txn, "", "mirror",       "node-attribute-equality-string", uc);
			container.addIndex(txn, "", "timestamp",    "unique-node-attribute-equality-dateTime", uc);
			container.addIndex(txn, "", "checkoutTime", "unique-node-attribute-equality-dateTime", uc);
			container.addIndex(txn, "", "type", 			"node-attribute-equality-string", uc);
			// TODO add more index here
			txn.commit();
			
			logger.info("Container " + name + " is now open (node container: " 
					+ containerConf.getNodeContainer()
					+ " ; index nodes: " + containerConf.getIndexNodes() + ")");
			
		}catch(XmlException e) {
			if(txn !=null)
				try {
					txn.abort();
				}catch(XmlException e2) {
					logger.error(e2.getMessage());
				}
		}finally{
			if(txn != null) 
				txn.delete();
		}
		
		// this hashtable keeps track of the open containers
		containers.put(name, container);
		
		return container;
	}
	

	/**
	 * Gets the container handle, and if necessary creates it before
	 * 
	 * Secondary effect: BDBXML will automatically transactionally protect
	 * all write operations to these containers (as long as they are not closed).
	 * 
	 * TODO is there some duplication with method openContainer() ?
	 * 
	 * @param name
	 * @throws XmlException 
	 */
	private XmlContainer initContainer(String name) {
		XmlContainer container = (XmlContainer)containers.get(name);
		
		if(container == null) {
			XmlContainerConfig containerConf = new XmlContainerConfig();
			containerConf.setTransactional(true);
			containerConf.setAllowCreate(true);
			
			if(name.equals(Config.LATEST_DIFFS_CONTAINER))
				containerConf.setNodeContainer(false); // wholedoc container (retrieve all docs usually)
			
			XmlTransaction txn = null;
			try {
				txn = manager.createTransaction();
				container = manager.createContainer(txn, name, containerConf);
				XmlUpdateContext uc = manager.createUpdateContext();
				container.addIndex(txn, "", "mirror",       "node-attribute-equality-string", uc);
				container.addIndex(txn, "", "timestamp",    "unique-node-attribute-equality-dateTime", uc);
				container.addIndex(txn, "", "checkoutTime", "unique-node-attribute-equality-dateTime", uc);
				container.addIndex(txn, "", "type", 		"node-attribute-equality-string", uc);
				// TODO add more index here
				txn.commit();
				containers.put(name, container);
				
				logger.info("created container: " + name + " (node container: " 
						+ containerConf.getNodeContainer()
						+ " ; index nodes: " + containerConf.getIndexNodes() + ")");
				
			}catch(XmlException e) {
				logger.error("Unable to create container " + name 
						+ "\n" + e.getMessage());
				if(txn !=null)
					try {
						txn.abort();
					}catch(XmlException e2) {
						logger.error(e2.getMessage());
					}
			}finally{
				if(txn != null) 
					txn.delete();
				txn = null;
			}
		}
		
		return container;
	}
	

	/**
	 * Free the resource and cleanly shutdown the database.
	 * 
	 * This step is necessary. However, an implicit close
	 * is performed when the last handle to this object
	 * is closed.
	 *
	 */
	public void close() {

	    try {
	    	/*
	    	 * STEP 1 - close all containers
	    	 */
	    	Collection conts = containers.values();
	    	Iterator it = conts.iterator();
	    	while(it.hasNext()) {
	    		XmlContainer container = (XmlContainer)it.next();
	    		if(container != null) {
	    			String contName = container.getName();
	    			//container.close();
	    			container.delete();
	    			logger.info("Container " + contName + " closed");
	    		}
	    	}
	    	
	    	/*
	    	 * STEP 2 - delete prepared queries
	    	 */
	    	if(latestMasterStructQueryExp != null)
	    		latestMasterStructQueryExp.delete();
	    	
	    	if(latestFullStructInContainerQueryExp != null)
	    		latestFullStructInContainerQueryExp.delete();
	    	
	    	if(latestDeltaInContainerQueryExp != null)
	    		latestDeltaInContainerQueryExp.delete();
	    	
	    	if(aGivenDocumentQueryExp != null)
	    		aGivenDocumentQueryExp.delete();
	    	
	    	if(allDocumentsQueryExp != null)
	    		allDocumentsQueryExp.delete();
	    	
	    	if(allDeltasFromLastFullStructQueryExp != null)
	    		allDeltasFromLastFullStructQueryExp.delete();
	    	
	    	if(countFullStructsInContainerQueryExp != null)
	    		countFullStructsInContainerQueryExp.delete();
	    	
	    	if(countDeltasInContainerQueryExp != null)
	    		countDeltasInContainerQueryExp.delete();
	    	
	    	if(countAllDocsInContainerQueryExp != null)
	    		countAllDocsInContainerQueryExp.delete();
	    	
	    	if(mirrorTypeQueryExp != null)
	    		mirrorTypeQueryExp.delete();
	    		
	    	
	    	/*
	    	 * STEP 3 - close the manager and the environment
	    	 */
	        if (manager != null) {
	            //manager.close();
	        	manager.delete();
	            manager = null;
	            // environment has been closed by manager
	        }
	        
	    	logger.info("Closed successfullly");
	    	
	    } catch (XmlException ce) {
	    	logger.fatal("Unable to close properly");
	    	logger.fatal(ce.getStackTrace());
	    
	    }finally{
	    	instance = null;	// total cleanup
	    	containers = new Hashtable<String,XmlContainer>();
	    	config = null;
	    }
	}
	
	/**
	 * Store the mirror structure as XML in the database.
	 * 
	 * @param struct to be stored
	 * @return success of failure
	 * @throws BDBXMLException
	 */
	public void save(ITreeMirrorStructure struct) throws BDBXMLException {
		String containerName 	= struct.getMirrorID() + ".dbxml";
		String docName			= struct.getDocumentID() + ".xml";
		String content			= struct.toString();		
		
		storeContent(containerName, docName, content);
	}

	/**
	 * Store the new version of the mirror structure.
	 * 
	 * This method contains optimization logic to save
	 * on space. Instead of storing redundant data all 
	 * the time, it stores only the changes (delta structure)
	 * while they are in small number (as defined in Config).
	 * 
	 * @param status
	 * @throws BDBXMLException
	 */
	public void save(IMirrorStatus status) throws BDBXMLException {
		IMirrorDelta delta = null;
		ITreeMirrorStructure struct = null;
		
		if(status.getMirrorStructure() instanceof ITreeMirrorStructure)
			struct = (ITreeMirrorStructure)status.getMirrorStructure();
		else
			throw new BDBXMLException("Error: BDBXML saves only ITreeMirrorStructures " +
					"and nothing else");

		if(status.getDelta() instanceof IDOMMirrorDelta)
			delta = (IDOMMirrorDelta)status.getDelta();
		else
			throw new BDBXMLException("Error: BDBXML saves only IDOMMirrorDelta " +
					"and nothing else");
		
		// config can't be null, @see BDBXMLManager()
		if(delta.getSize() > config.getDeltaLimit()) {
			save(struct);
			logger.info("Storing full structure: " + struct.getFilename());
			
		}else{
			save(delta);
			logger.info("Storing delta (" + delta.getSize() + " changes): " + delta.getFilename());
		}
	}
	
	/**
	 * Store the delta mirror structure in the database
	 * 
	 * Should be private for the application, the entry point
	 * is through save(IMirrorStatus) but kept public for
	 * unit testing.
	 *  
	 * @see save(IMirrorStatus) 
	 * @throws BDBXMLException
	 * @param delta
	 */
	public void save(IMirrorDelta delta) throws BDBXMLException {
		logger.info("Saving " + delta.getDocumentID());
		
		String containerName 	= delta.getMirrorID() + ".dbxml";
		String docName			= delta.getDocumentID() + ".xml";
		String content			= delta.toString();		
		
		storeContent(containerName, docName, content);
	}
	
	/**
	 * Store the mirror diff as XML in the database.
	 *  
	 * @param struct to be stored
	 * @return success of failure
	 * @throws BDBXMLException
	 */
	public void save(IMirrorDiff diff) throws BDBXMLException {
		String containerName 	= Config.LATEST_DIFFS_CONTAINER;
		String docName			= diff.getDocumentID() + ".xml";
		
		logger.setLevel(Level.DEBUG);
		logger.debug("saving diff in " + containerName + " as " + docName);
		
		String content = diff.toString();
		
		// removing old one first
		removeLatestDiffForMirror(diff.getMirrorID());
		storeContent(containerName, docName, content);
	}

	
	/**
	 * Saves a new <code>WorkflowStatistics</code> document
	 * @param jobStat
	 * @throws BDBXMLException 
	 */
	public void save(WorkflowStatistics jobStat) throws BDBXMLException {
		String containerName 	= STATISTICS_CONTAINER;
		String docName			= jobStat.getDocumentID() + ".xml";
		
		String content = jobStat.toString();
		storeContent(containerName, docName, content);
	}
	
	/**
	 * Saves a XML string containing information about monitored
	 * directory.
	 */
	public void saveMonitored(String docName, String xmlString)  throws BDBXMLException {
		storeContent(MONITORED_CONTAINER, docName, xmlString);
	}
	

	public void saveRepository(IRepository rep) throws BDBXMLException{
		String docName = rep.getFilename();
		String xmlString = rep.toString();
		storeContent(REPOSITORY_CONTAINER, docName, xmlString);		
	}
	
	public void saveMonitoredMirrorsList(MonitoredMirrors mirrors) throws BDBXMLException{
		String docName = mirrors.getFilename();
		String xmlString = mirrors.toString();
		storeContent(MIRRORS_CONTAINER, docName, xmlString);		
	}
	
	/**
	 * Temporary method using add/remove strategy to update a document
	 * This is to be replaced by a XQUERY targeted to the node(s) to 
	 * be replaced (for efficiency).
	 * @deprecated
	 * @param jobStat
	 * @throws BDBXMLException
	 */
	public void update(WorkflowStatistics jobStat) throws BDBXMLException {
		String containerName 	= STATISTICS_CONTAINER;
		String docName			= jobStat.getDocumentID() + ".xml";
		
		String content = jobStat.toString();
		deleteDocument(docName);
		storeContent(containerName, docName, content);
	}
	
	
	/**
	 * Private method to handle storing a document as a specific
	 * file within a specific container.
	 * 
	 * @param container name
	 * @param document name
	 * @param content as a <code>String</code>
	 * @return true or false
	 * @throws BDBXMLException
	 */
	private void storeContent(String containerName, String docName, String content)
														throws BDBXMLException {

		XmlContainer container = null;
		XmlUpdateContext uc = null;
		XmlTransaction txn = null;
		
		try {
			// containers are all assumed open
			String cName = (containerName==null)?convertToContainer(docName):containerName;
			container = initContainer(cName);
			uc = manager.createUpdateContext();
			
			txn = manager.createTransaction();
			container.putDocument(txn, docName, content, uc, null);
			txn.commit();
			
			if(containerName == null)
				logger.warn("container name " + containerName + " is invalid, using " + cName +" instead");
			logger.info("Added doc " + docName + " to container " + cName);
			
		} catch (XmlException e) {
			logger.error("BDB XML error in storeContent(): " + e.getMessage());
			logger.error(e.getStackTrace());
			if(txn != null)
				try {
					txn.abort();
				} catch (XmlException e1) {
					logger.error(e1.getMessage());
				}
			throw new BDBXMLException("BDB XML error: " + e.getMessage());
			
		}finally{
			if(uc != null)
				uc.delete();
			if(txn != null) 
				txn.delete();
		}
	}
	
	/**
	 * Remove latest diff for a given mirror which ID is passed as a parameter
	 * 
	 * The container for diffs is know. A query is run to find the document, 
	 * and if it finds one, it will be the latest diff with this mirrorid (
	 * other diffs can exist but it takes the last one). Finally, the document
	 * is removed from the database.
	 * 
	 * This method is used to remove un-necessary documents from the database.
	 * 
	 * @param mirrorID
	 */
	public void removeLatestDiffForMirror(String mirrorID) {
		XmlContainer container = null;
		XmlUpdateContext uc = null;
		XmlTransaction txn = null;
		XmlResults res = null;
		XmlDocument doc = null;
		
		try {
			XmlValue value = getLatestDiffValueForMirror(mirrorID);
			if(value == null) {
				logger.error("Unable to retrieve latest diff for mirror " + mirrorID
						+ " in container "+ Config.LATEST_DIFFS_CONTAINER
						+ " (was there a document to be retrieved?)");
				return;
			}
			
			logger.info("Found diff document for " + mirrorID + " ; deleting");
			doc = value.asDocument();
			logger.debug("Deleting doc with name='" + doc.getName() + "'");
			
			uc = manager.createUpdateContext();
			txn = manager.createTransaction();
			//container.deleteDocument(txn, doc, uc);
			container.deleteDocument(txn, doc.getName(), uc);
			txn.commit();
			
			/*
			// containers are all assumed open
			container = initContainer(Config.LATEST_DIFFS_CONTAINER);
			XmlQueryContext context = manager.createQueryContext();	// not thread-safe
			//context.setReturnType(XmlQueryContext.DeadValues);
			//context.setEvaluationType(XmlQueryContext.Lazy);
			//context.setReturnType(XmlQueryContext.LiveValues);
			context.setVariableValue(CONTAINER, new XmlValue(Config.LATEST_DIFFS_CONTAINER));
			context.setVariableValue(MIRRORID, new XmlValue(mirrorID));
			uc = manager.createUpdateContext();
			
			txn = manager.createTransaction();
			res = latestDiffForMirrorInContainerQueryExp.execute(txn, context);
			
			XmlValue value = res.next();
			while(value != null) {
				logger.info("node name = " + value.getNodeName());
				int l = (value.asString().length() > 300)?300:value.asString().length();
				logger.info("got a value : " + value.asString().substring(0, l));
				logger.info("before doc=" + doc);
				doc = value.asDocument();
				logger.info("got doc=" + doc);
				logger.info("doc name=" + doc.getName());
				logger.info("doc content=" + doc.getContentAsString());
				String fullName = new String(doc.getName());

				// bug workaround (bdbxml v.2.2.13 would return a doc with no name set)
				doc.setContent("<tmp/>");
				container.updateDocument(txn, doc, uc);
		        
				container.deleteDocument(txn, doc, uc);
				logger.info("deleting " + fullName + " from database (as old diff)");
				value = res.next();
			}
			
			txn.commit();
			*/
			
		}catch (XmlException e) {
			logger.warn("This is a known bug for DBDXML v.2.2.13 - ");
			logger.error("Unable to retrieve latest diff for mirror " + mirrorID
					+ " in container "+ Config.LATEST_DIFFS_CONTAINER
					+ " (was there a document to be retrieved?)");
			logger.error(e.getMessage());
			if(txn !=null)
				try {
					txn.abort();
				}catch(XmlException e2) {
					logger.error(e2.getMessage());
				}
				
		}catch(NullPointerException npe) {
			logger.error("NULL: Something unexpected is happening when calling BDBXML container.deleteDocument()");
		
		}finally {
				if(txn != null) 
					txn.delete();
				if(res != null)
					res.delete();
				if(doc!= null)
					doc.delete();
		}

		
	}
	
	/**
	 * Helper function for removeLatestDiffForMirror(String mirrorID)
	 * and getLatestDiffForMirror(String mirrorID)
	 * @param mirrorID
	 * @return
	 */
	private XmlValue getLatestDiffValueForMirror(String mirrorID) {
		XmlTransaction txn = null;
		XmlResults res = null;
		XmlValue value = null;
		
		try {
			// containers are all assumed open
			initContainer(Config.LATEST_DIFFS_CONTAINER);
			XmlQueryContext context = manager.createQueryContext();	// not thread-safe
			context.setReturnType(XmlQueryContext.LiveValues);	// mandatory to be able to call doc.getName()
			context.setEvaluationType(XmlQueryContext.Eager);
			context.setVariableValue(CONTAINER, new XmlValue(Config.LATEST_DIFFS_CONTAINER));
			context.setVariableValue(MIRRORID, new XmlValue(mirrorID));
			
			txn = manager.createTransaction();
			res = latestDiffForMirrorInContainerQueryExp.execute(txn, context);			
			value = res.next();
			
			txn.commit();
			
		}catch (XmlException e) {
			logger.error("Unable to retrieve latest diff for mirror " + mirrorID
					+ " in container "+ Config.LATEST_DIFFS_CONTAINER
					+ " (was there a document to be retrieved?)");
			logger.error(e.getMessage());
			if(txn !=null)
				try {
					txn.abort();
				}catch(XmlException e2) {
					logger.error(e2.getMessage());
				}
				
		}finally {
				if(txn != null) 
					txn.delete();
				if(res != null)
					res.delete();
		}		
		return value;
	}

	/**
	 * Get the list of available containers
	 * within the database.
	 * 
	 * @return List<String> container list
	 */
	public List<String> getContainers() {
		ArrayList<String> containers = new ArrayList<String>();
		
		File repos = new File(Config.getBDBXMLEnvHome() /*"bdbxml_containers" */);
		File[] file = repos.listFiles();
		for(int i=0; i < file.length; i++) 
			if(file[i].getName().endsWith(".dbxml"))
				containers.add(file[i].getName());
		
		return containers;
	}
	
	/**
	 * Get the list of documents in a given container.
	 * 
	 * @param container
	 * @return
	 */
	public List<String> getDocuments(String container) {
		ArrayList<String> docs = new ArrayList<String>();
		XmlContainer cont = null;
		XmlResults res = null;
		XmlDocument doc = null;
		XmlTransaction txn = null;
		XmlDocumentConfig docConf = null;
		
		try {

			// all containers are open with the database
			cont = initContainer(container);
			
			txn = manager.createTransaction();
			docConf = new XmlDocumentConfig();
			res = cont.getAllDocuments(txn, docConf);
			txn.commit();
			
			doc = manager.createDocument();
			while(res.next(doc)) {
				docs.add(doc.getName());
			}
			
		}catch (XmlException e) {
			logger.error("Unable to retrieve all documents from " + container);
			logger.error(e.getStackTrace());
			if(txn !=null)
				try {
					txn.abort();
				}catch(XmlException e2) {
					logger.error(e2.getMessage());
				}
		}finally {
				if(doc != null)
					doc.delete();
				if(res != null)
					res.delete();
				if(txn != null) 
					txn.delete();
		}
		
		return docs;
	}

	
	/**
	 * Get the list of documents in a given container
	 * sorted in the chronological order.
	 * 
	 * @param container
	 * @return the sorted list of documents
	 */
	public Collection<String> getDocumentsInChronologicalOrder(String container) {
		TreeMap<Long,String> docs = new TreeMap<Long,String>();
		XmlResults res = null;
		XmlDocument doc = null;
		XmlTransaction txn = null;
		
		try {

			initContainer(container);	// initialization
			
			XmlQueryContext context = manager.createQueryContext();	// not thread-safe
			context.setReturnType(XmlQueryContext.DeadValues);
			context.setEvaluationType(XmlQueryContext.Eager);
			context.setVariableValue(CONTAINER, new XmlValue(container));
			
			txn = manager.createTransaction();
			res = allDocumentsQueryExp.execute(txn, context);
			
			XmlValue value = res.next();
			while(value != null) {
				doc = value.asDocument();
				String fullName = new String(doc.getName());
				String sLong = null;
				try{
					sLong = extractTimeStampFromDocName(fullName);
					
				}catch(BDBXMLException bdbe) {
					logger.error("Parsing error: " + bdbe.getMessage());
					continue;	// skip this one
				}
				Long timeStamp = new Long(sLong);
				docs.put(timeStamp, fullName);
				value = res.next();
			}
			
			txn.commit();
			
		}catch (XmlException e) {
			logger.error("Unable to retrieve all documents from " + container);
			logger.error(e.getStackTrace());
			if(txn !=null)
				try {
					txn.abort();
				}catch(XmlException e2) {
					logger.error(e2.getMessage());
				}
				
		}finally {
				if(txn != null) 
					txn.delete();
				if(res != null)
					res.delete();
				if(doc!= null)
					doc.delete();
		}
		
		return docs.values();
	}
	
	
	/**
	 * Fetches a document by name in the database.
	 * 
	 * This method is to be preferred over getDocumentAsInputStream()
	 * when a String is enough (faster).
	 * 
	 * @param document name
	 * @return document as a String
	 * @throws BDBXMLException
	 */
	public String getDocument(String docName) throws BDBXMLException {
		String container = convertToContainer(docName);
		String content = null;
		XmlDocument doc = null;
		
		XmlResults res = null;
		XmlTransaction txn = null;
		
		try {

			initContainer(container);
			
			XmlQueryContext context = manager.createQueryContext();	// not thread-safe
			context.setReturnType(XmlQueryContext.DeadValues);
			context.setEvaluationType(XmlQueryContext.Eager);
			context.setVariableValue(CONTAINER, new XmlValue(container));
			context.setVariableValue(DOCUMENT, new XmlValue(docName));
			
			txn = manager.createTransaction();
			res = aGivenDocumentQueryExp.execute(txn, context);
			
			XmlValue value = res.next();
			if(value != null)
				doc = value.asDocument();
			
			if(doc != null) 
				content = doc.getContentAsString();
			txn.commit();
			
		}catch (XmlException e) {
			logger.error("Unable to retrieve " + docName);
			logger.error(e.getMessage());
			logger.error(e.getStackTrace());
			if(txn !=null)
				try {
					txn.abort();
				}catch(XmlException e2) {
					logger.error(e2.getMessage());
				}
			
		}finally {
			if(res != null)
				res.delete();
			if(doc != null)
				doc.delete();
			if(txn != null) 
				txn.delete();
		}		

		if(content == null) {
			String message = "Unable to find " + docName + " in the database";
			logger.error(message);
			throw new BDBXMLException(message);
		}
		return content;
	}

	
	/**
	 * Remove a complete document from the database.
	 * 
	 * @param document name as a <code>String</code>
	 * TODO add BDBXMLException throw
	 */
	public void deleteDocument(String docName) {
		String container = convertToContainer(docName);
		XmlContainer cont = null;
		XmlUpdateContext uc = null;
		XmlTransaction txn = null;

		try {

			// all containers are open with the database (init time)
			cont = initContainer(container);
			
			uc = manager.createUpdateContext();

			txn = manager.createTransaction();
			cont.deleteDocument(txn, docName, uc);
			txn.commit();
			
		}catch (XmlException e) {
			logger.error("(deleteDocument: Unable to retrieve " + docName);
			logger.error(e.getStackTrace());
			if(txn !=null)
				try {
					txn.abort();
				}catch(XmlException e2) {
					logger.error("deleteDocument: Unable to abort transaction: " 
							+ e2.getMessage());
				}
		}finally {
			if(uc != null)
				uc.delete();
			if(txn != null) 
				txn.delete();
		}		
	}
	
	/**
	 * Gets the latest and freshest master structure.
	 * 
	 * @return the latest master structure
	 * @throws DocumentException
	 */
	public ITreeMirrorStructure getLatestMasterStructure() 
												throws DocumentException {
		ITreeMirrorStructure struct = null;
		XmlResults res = null;
		XmlTransaction txn = null;
		
		try {
			initContainer(MASTER_CONTAINER); // initializes it if necessary
			
			XmlQueryContext context = manager.createQueryContext();	// not thread-safe
			context.setReturnType(XmlQueryContext.DeadValues);
			context.setEvaluationType(XmlQueryContext.Eager);
			
			txn = manager.createTransaction();
			res = latestMasterStructQueryExp.execute(txn, context);
			txn.commit();
			
			String content = null;
			XmlValue value = res.next();
			if(value != null)
				content = value.asString();
			
			struct = new TreeMirrorStructure(content);
			
		}catch (XmlException e) {
			logger.fatal("Unable to retrieve latest structure document for master mirror");
			logger.fatal(e.getStackTrace());
			if(res != null)
				res.delete();
			if(txn !=null)
				try {
					txn.abort();
				}catch(XmlException e2) {
					logger.error(e2.getMessage());
				}
			MonitorApplication.exit(MonitorApplication.ABORT);
			
		}finally {
			if(res != null)
				res.delete();
			if(txn != null) 
				txn.delete();
		}
		
		return struct;
	}
	
	/**
	 * Load from the database the latest representation of a mirror
	 * defined as mirrorID (host+protocol).
	 * 
	 * All representations of the same mirror are stored in the
	 * same container. And all these docs have a unique timestamp.
	 * This method uses the above properties to load the structure.
	 * 
	 * @param mirrorID
	 * @return the mirror structure
	 * 
	 * @throws DocumentException
	 */
	public ITreeMirrorStructure getLatestStructure(String mirrorID)
												throws DocumentException {
		//logger.setLevel(Level.DEBUG);
		ITreeMirrorStructure struct = null;
		String container = convertMirrorIDToContainer(mirrorID);
		XmlResults res = null;
		XmlTransaction txn = null;
		
		try {

			initContainer(container);	// initization
			
			XmlQueryContext context = manager.createQueryContext();	// not thread-safe
			context.setReturnType(XmlQueryContext.DeadValues);
			context.setEvaluationType(XmlQueryContext.Eager);
			context.setVariableValue(CONTAINER, new XmlValue(container));
			
			txn = manager.createTransaction();
			logger.debug("latestFullStructInContainerQueryExp=" + latestFullStructInContainerQueryExp);
			res = latestFullStructInContainerQueryExp.execute(txn, context);
			txn.commit();
			if(txn != null) {
				txn.delete();
				txn = null;
			}

			String content = null;
			XmlValue value = res.next();
			if(value != null)
				content = value.asString();
			
			struct = new TreeMirrorStructure(content);
			
		}catch (XmlException e) {
			logger.error("Unable to retrieve latest structure document for " + mirrorID);
			logger.error("\t" + e.getMessage());
			if(txn !=null)
				try {
					txn.abort();
				}catch(XmlException e2) {
					logger.error(e2.getMessage());
				}
			
		}finally {
			if(res != null)
				res.delete();
			if(txn != null) 
				txn.delete();
		}
		
		return struct;
	}

	/**
	 * Retrieve the latest diff for a given mirror (by mirror ID)
	 * 
	 * @param mirrorID
	 * @return
	 * @throws BDBXMLException 
	 * @throws DocumentException 
	 */
	public IDOMMirrorDiff getLatestDiffForMirror(String mirrorID) throws DocumentException {
		IDOMMirrorDiff diff = null;

		String content = null;
		
		try {
			XmlValue value = getLatestDiffValueForMirror(mirrorID);
			
			if(value != null)
				content = value.asString();
			else
				throw new DocumentException("Unable to retrieve Diff document");
			
			diff = new DOMMirrorDiff(content); // can throw DocumentException
						
		}catch (XmlException e) {
			logger.error("Unable to retrieve latest diff for " + mirrorID);
			logger.error("\t: " + e.getMessage());
		}
		
		return diff;
	}
	
	/**
	 * Tells how many documents exist in the container
	 * 
	 * @param container
	 * @return number of documents in this container
	 */
	public int getDocumentCount(String mirrorID) {
		XmlResults res = null;
		String container = convertMirrorIDToContainer(mirrorID);
		int count = -1;
		XmlTransaction txn = null;
		
		try {
			initContainer(container);
			
			XmlQueryContext context = manager.createQueryContext();	// not thread-safe
			context.setReturnType(XmlQueryContext.LiveValues);
			context.setEvaluationType(XmlQueryContext.Lazy);
			context.setVariableValue(CONTAINER, new XmlValue(container));

			txn = manager.createTransaction();
			res = countAllDocsInContainerQueryExp.execute(txn, context);
			
			
			XmlValue value = res.next();
			if(value != null)
				count = (int)value.asNumber();
			
			txn.commit();
			
		}catch(XmlException e) {
			logger.error("Unable to determine the number of documents in " + container);
			logger.error(e.getStackTrace());
			if(txn !=null)
				try {
					txn.abort();
				}catch(XmlException e2) {
					logger.error(e2.getMessage());
				}
			
		}finally {
			if(res != null)
				res.delete();
			if(txn != null) 
				txn.delete();
		}
		
		return count;
	}
	
	
	/**
	 * Tells if a structure belongs to a master or to a mirror by looking at
	 * the XML content
	 * 
	 * @param container
	 * @param docName
	 * @return IMirror.MASTER or IMirror.MIRROR, master or mirror (as a <code>String</code>)
	 */
	public String getMirrorType(String container, String docName) {
		String type = "master mirror?";
		XmlResults res = null;
		XmlTransaction txn = null;
		
		try {
			initContainer(container);	// initialization
			
			XmlQueryContext context = manager.createQueryContext();	// not thread-safe
			context.setReturnType(XmlQueryContext.DeadValues);
			context.setEvaluationType(XmlQueryContext.Eager);
			context.setVariableValue(CONTAINER, new XmlValue(container));
			context.setVariableValue(DOCUMENT, new XmlValue(docName));
			
			txn = manager.createTransaction();
			res = mirrorTypeQueryExp.execute(txn, context);
			txn.commit();
			
			if(res != null && res.size() > 0)
				type = IMirror.MASTER; 
			else
				type = IMirror.MIRROR;

		}catch(XmlException e) {
			logger.error("Unable to read the mirror type (master/mirror) for " + docName);
			logger.error(e.getStackTrace());
			if(txn !=null)
				try {
					txn.abort();
				}catch(XmlException e2) {
					logger.error(e2.getMessage());
				}
			
		}finally {
			if(res != null)
				res.delete();
			if(txn != null) 
				txn.delete();
		}
		
		return type;
	}
	
	
	/**
	 * Runs the given XQUERY in the database
	 * 
	 * @param xquery
	 * @return the result of the query
	 */
	public String query(String xquery) throws BDBXMLException {
		XmlResults res = null;
		String result = null;
		XmlTransaction txn = null;
		
		try {
			
			// Get a query context
			XmlQueryContext context = manager.createQueryContext();
			context.setEvaluationType(XmlQueryContext.Eager);
			context.setReturnType(XmlQueryContext.LiveValues);

			txn = manager.createTransaction();
			res = manager.query(txn, xquery, context);
			txn.commit();
			
			XmlValue value = res.next();
			result = value.asString();
			
		}catch(XmlException e) {
			if(txn !=null)
				try {
					txn.abort();
				}catch(XmlException e2) {
					logger.error(e2.getMessage());
				}
			throw new BDBXMLException("Unable to run XQUERY=" + xquery, e);
			
		}finally {
				if(res != null)
					res.delete();
				if(txn != null) 
					txn.delete();
		}
		
		return result;
	}

	
	
	 
	/**
	 * Build the container name from the host and protocol of a
	 * given mirror.
	 * 
	 * @param host
	 * @param protocol
	 * @return
	 */
	private static String convertMirrorIDToContainer(String mirrorID) {
		return mirrorID + ".dbxml";
	}
	
	/**
	 * Computes the container name from the document name.
	 * 
	 * @param document name
	 * @return the container name
	 */
	private static String convertToContainer(String docName) {
		return docName.split("-")[0] + ".dbxml";
	}
	
	/**
	 * Extract the timestamp from the document name
	 * 
	 * Examples
	 * 		testmachine_ftp-1141113730566-delta.xml
	 * 		testmachine_ftp-1141113730566.xml
	 * 		testmachine_ftp-1141113730566-diff.xml
	 * 
	 * @param docName
	 * @return the time stamp contained in document name
	 */
	private static String extractTimeStampFromDocName(String docName) throws BDBXMLException {
		if(!docName.contains("-"))
			throw new BDBXMLException("Illegal filename: " + docName);	// problem, this does not look like a good file name!
		
		String lastPart = docName.split("-")[1];	// after the mirror ID
		
		if(lastPart.split("-").length > 0) // diff or delta
			return lastPart.split("-")[0].split("\\.")[0];
		else
			return docName.split("-")[1].split("\\.")[0];
	}

}
