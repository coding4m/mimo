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
import java.io.IOException;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.dom4j.Element;

import edos.mimo.exception.ConfigException;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class Config {
	// class started before the logging system
	//private static Logger logger = Logger.getLogger(Config.class);
	
	// singleton clas
	private static Config instance = null;
	
	private File configFile;	// keep track of which config file has been used	
	private Document doc;
	private ArrayList<Identity> probableAccounts;	// default user account and alternatives
	private AnalysisLevel analysis;
	private static String HOME_DIR;
	public static String DEFAULT_CONFIG_FILE = "mirrormetrics-config.xml";
	private MirrorFactory mirrorFactory;
	private int intervalHours = DEFAULT_INTERVAL_HOURS;	// download frequency in hours (each intervalHours a new download starts)
	private int deltaLimit    = DEFAULT_DELTA_LIMIT;	// tells how many changes are necessary to store a full struct instead 
	private int loggingSystem = DEFAULT_LOGGING_SYSTEM;
	private String loggingSystemConfigFile = DEFAULT_LOG4J_CONFIG_FILE;
	private String defaultCronExpression = NO_CRON_EXPRESSION;	// default

	public static int DEFAULT_INTERVAL_HOURS  	= 24;
	private static int DEFAULT_DELTA_LIMIT 		= 100;
	public static int LOG4J						= 0;
	private static int DEFAULT_LOGGING_SYSTEM	= LOG4J;
	private static String DEFAULT_LOG4J_CONFIG_FILE = "log4j-config.xml";
	private static String NO_CRON_EXPRESSION	= null;
	
	// options to control the data storage format
	public static final int TREE = 1;		// custom tree representation
	public static final int TABLE = 2;		// custom table representation
	//public static final int BOTH = 3; not implemented
	
	/*
	 * JDBC specific config
	 * TO BE relocated later
	 * @author radu
	 */
	public static String DEFAULT_JDBC_HOST = "localhost";
	private static String jdbcHost = DEFAULT_JDBC_HOST;
	public static String DEFAULT_JDBC_USER = "root";
	private static String jdbcUserName = DEFAULT_JDBC_USER;
	private static String jdbcUserPasswd = "";

	private static int storageFormat = TREE; // default
	
	// Berkeley DB XML related constants
	//public static String BDDXML_HOME = "bdbxml_containers"; not used
	private static String BDBXML_ENV_HOME = "bdbxml_containers";
	private static String bdbxmlEnvHome = BDBXML_ENV_HOME;
	public static String LATEST_DIFFS_CONTAINER = "latest-diffs.dbxml";


	private static String basePath = "./";	// root directory for this application, can be modified via setter
	private static String basePathXMLStorage = "./";// directory for storage, can be modified via setter
	
	
	private static Vector<String> monitoredDirs = new Vector<String>();

	
	// used in the web interface but available here
	public static final String MONITORED = "monitored.xml";
	public static final String MONITORED_DIRS = "monitoredDirs";
	public static final String MIRRORS = "mirrors";
	public static final String MASTER  = "master";
	
	//private static String adminPassword = "xyz123";

	
	/**
	 * Tells if the config file has been read and parsed
	 * or not.
	 * 
	 * @return a boolean config existence status 
	 */
	public static boolean isLoaded() {
		return instance != null;
	}

	/**
	 * Singleton 
	 * @return instance
	 */
	public static Config getInstance() throws ConfigException {
		if(instance == null)
			instance = new Config();
		return instance;
	}

	/**
	 * Singleton 
	 * @return instance
	 */
	public static Config getInstance(File file) throws ConfigException {
		if(instance == null)
			instance = new Config(file);
		return instance;
	}
	
	/**
	 * 
	 */
	private Config() throws ConfigException {
		this(DEFAULT_CONFIG_FILE);
	}

	/**
	 * @param file
	 */
	private Config(File file) throws ConfigException {
//		logger.info("Initiating configuration parsing based on " + file);						
		instance = this;
		
		configFile = file;
		
		probableAccounts = new ArrayList<Identity>();
		mirrorFactory = MirrorFactory.getInstance();
		
		/*
		 * Getting application install path
		 * Where are we located?
		 */
		try {
			File currentDir = new File (".");
			HOME_DIR = currentDir.getCanonicalPath();
			
		}catch(IOException ioe) {
//			logger.fatal("Unable to get current directory path");
			throw new ConfigException("Unable to get current directory path");
		}
		
		/*
		 * Parse application config file
		 */
		try {
			parseWithSAX(file);

			parseProbableAccounts();
			parseAnalysis();
			parseStorage();
			parseDelta();
			parseSchedule();
			parseLogging();
			parseBasePath();
			parseMonitoredDirs();
			//parseAdminPassword();
			
			//logger.info(("Configuration loaded successfully"));

		} catch (DocumentException doce) {
			throw new ConfigException("Unable to load the configuration file ("
					+ file.getAbsolutePath() + ")");
		}
	}

	/**
	 * Get an admin password
	 * Basic authentication mechanism
	 * Used to shutdown the system and other admin-type of actions
	 * 	 
	private void parseAdminPassword() {
		Element node = (Element)doc.selectSingleNode("//admin/password");
		
		if(node == null) return; // keep default
		adminPassword = node.getText();
	}
	 */
	
	/**
	 * Gets the base path, ie the directory from which all
	 * files are stored.
	 */
	private void parseMonitoredDirs() throws ConfigException {
		Element node = (Element)doc.selectSingleNode("//monitor");
		
		if(node == null) return; // keep default (empty vector)
		Iterator dirNodesIterator = node.selectNodes("dir/@path").iterator();
		while(dirNodesIterator.hasNext()) {
			Attribute pathAttribute = ((Attribute)dirNodesIterator.next());
			monitoredDirs.add(pathAttribute.getText());
		}
	}
	
	/**
	 * Gets the base path, ie the directory from which all
	 * files are stored.
	 */
	private void parseBasePath() throws ConfigException {
		Element node = (Element)doc.selectSingleNode("//basepath");
		
		if(node == null) return; // keep default
		
		String path = node.attributeValue("dir");
		if(path != null) basePath = path;
	}

	/**
	 * Gets the part of the config file where the logging system is set.
	 * This makes the Monitor config file the one-place to define
	 * everything.
	 */
	private void parseLogging() throws ConfigException {
		Element node = (Element)doc.selectSingleNode("//logging");
		
		if(node == null) 
			return; // keep default return value as defined above
		
		// There is only one logging system implemented
		String logType = node.attributeValue("type");
		if(logType != null && !logType.equals("log4j"))
			throw new ConfigException("Logging system must be log4j!");
		loggingSystem = LOG4J;
		
		String configFile = node.attributeValue("config");
		if(configFile != null)
			loggingSystemConfigFile = configFile;
		// else keep the default as defined above
	}

	/**
	 * Gets the part of the config file specifying how many changes
	 * are necessary for a full structure to be stored instead of 
	 * a delta.
	 */
	private void parseDelta() {
		Element node = (Element)doc.selectSingleNode("//delta");
		
		if(node == null) 
			return; // keep default return value as defined above
		
		String limit = node.attributeValue("limit");
		if(limit != null)
			deltaLimit = Integer.parseInt(limit);
		// else keep the default as defined above
	}

	/**
	 * Gets the part of the config file specifying the time delay 
	 * between 2 download jobs.
	 * 
	 * For example, it will tell a job Jn will start every N hours.
	 * 
	 * Please refer to the sample configuration file for information
	 * about the rules.
	 *
	 */
	private void parseSchedule() {
		// the attributes are defined with a default value above
		
		// parsing cron expression
		Element cronNode = (Element)doc.selectSingleNode("//schedule/cron");
		
		if(cronNode != null) {
			String expression = cronNode.attributeValue("expression");
			if(expression != null)
				defaultCronExpression = expression;
			
			return; // done here (cron takes precedence)
		}
		
		// parsing frequency information if needed
		Element frequencyNode = (Element)doc.selectSingleNode("//schedule/frequency");
		
		if(frequencyNode != null) {
			String sHours = frequencyNode.attributeValue("hours");
			if(sHours != null)
				intervalHours = Integer.parseInt(sHours);
			// else keep the default as defined above
			
			// building the cron expression for the program if user had defined an interval
			defaultCronExpression = "0 0 0/" + intervalHours + " * * ?";
		}				
	}

	/**
	 * Gets the part of the config file specifying the storage type.
	 * 
	 * Please refer to the config file included documentation.
	 * 
	 * Modified by radu: added JDBC configs
	 *
	 */
	private void parseStorage() {
		Element node = (Element)doc.selectSingleNode("//storage");
		
		if(node == null) 
			return; // keep default return value as defined above

		if(node.attributeValue("type").equals("tree"))
			storageFormat = TREE;
		else if(node.attributeValue("type").equals("table")) {
			storageFormat = TABLE;
			Element jdbcUser = (Element)node.selectSingleNode("./jdbcUser");			
			if (null != jdbcUser) {
				jdbcHost = jdbcUser.attributeValue("host", DEFAULT_JDBC_HOST);
				jdbcUserName = jdbcUser.attributeValue("login", DEFAULT_JDBC_USER);
				jdbcUserPasswd = jdbcUser.attributeValue("passwd", "");
			}
			// else keep the default as defined above
		}
		// else keep the default as defined above
	}

	/**
	 * Gets the part of the config file determining what will be downloaded
	 * (if all informations or just a subset).
	 * 
	 * Please refer to the config file included documentation.
	 *
	 */
	private void parseAnalysis() {
		Element node = (Element)doc.selectSingleNode("//analysis");
		String depth = AnalysisLevel.DEFAULT_DEPTH;
		String verbosity = AnalysisLevel.DEFAULT_VERBOSITY;
		
		// default analysis level
		if(node == null) {
			analysis = new AnalysisLevel();
			return;
		}
		
		
		if(node.attributeValue("depth").equals(AnalysisLevel.COMPLETE) 
				|| node.attributeValue("depth").equals(AnalysisLevel.SKELETON)) 
			depth = node.attributeValue("depth");
		
		if(node.attributeValue("verbosity").equals(AnalysisLevel.QUIET)
				|| node.attributeValue("verbosity").equals(AnalysisLevel.VERBOSE))
			verbosity = node.attributeValue("verbosity");
		
		analysis = new AnalysisLevel(depth, verbosity);
	}

	/**
	 * This methods parse the configuration file for defaults ids (or accounts)
	 * and it initialize a private attribute.
	 * @throws ConfigException
	 */
	private void parseProbableAccounts() throws ConfigException {
		// retrieve the default user (account data) and all its default alternatives
		List userNodes = doc.selectNodes("/mirrormetrics/user");
		Iterator userNodesIterator = userNodes.iterator();
		while(userNodesIterator.hasNext()) {
			Identity id = probeIdentity((Element)userNodesIterator.next());			
			probableAccounts.add(id);	// add this default user
		}
	}

	public IMasterMirror getMasterMirror() throws ConfigException {
		ArrayList<Access> accessList = new ArrayList<Access>();
		Identity specificUserAccount = null;
		ArrayList<Identity> ids = new ArrayList<Identity>();

		// retrieve master mirror data
		Element masterMirrorNode = (Element) doc
				.selectSingleNode("//mastermirror");
		String mirrorName = masterMirrorNode.attributeValue("name");
		String host = masterMirrorNode.attributeValue("host");

		// probing for a specific user account for this mirror
		Element user = (Element)masterMirrorNode.selectSingleNode("./user");
		if(user != null) {
			specificUserAccount = probeIdentity(user);
			ids.add(specificUserAccount);	// main id (at the first spot)
		}
		ids.addAll(probableAccounts);	// default ids
		
		// looping thru access nodes (child nodes of mastermirror)
		Iterator accessNodes = masterMirrorNode.selectNodes("access").iterator();
		while (accessNodes.hasNext()) {
			Element e = (Element) accessNodes.next();
			Access access = new Access(host, e.attributeValue("protocol"), e
					.attributeValue("path"));
			accessList.add(access);
		}

		// getting cron expression (if exists it is overriding the default one)
		Element cronElt = (Element)masterMirrorNode.selectSingleNode("cron");
		String cronExpression = defaultCronExpression;

		if(cronElt != null) {
			String expression = cronElt.attributeValue("expression");
			if(expression != null)
				cronExpression = expression;
		}
		
		IMasterMirror m = mirrorFactory.getMaster(mirrorName, accessList, ids, analysis, cronExpression);

		return m;
	}
	
	/**
	 * @return
	 * @throws ConfigException
	 */
	public List<IMirror> getSecondaryMirrors() throws ConfigException {
		ArrayList<IMirror> mirrors = new ArrayList<IMirror>();
		Identity specificUserAccount = null;
		ArrayList<Identity> ids = new ArrayList<Identity>();
		
		Iterator nodes = doc.selectNodes("//secondarymirror").iterator();
		while(nodes.hasNext()) {
			Element mirror = (Element)nodes.next();
			String mirrorName 	= mirror.attributeValue("name");
			String host			= mirror.attributeValue("host");
			ArrayList<Access> accessList = new ArrayList<Access>();

			// probing for a specific user account for this mirror
			Element user = (Element)mirror.selectSingleNode("./user");
			if(user != null) {
				specificUserAccount = probeIdentity(user);
				ids.add(specificUserAccount);	// main id (at the first spot)
			}
			ids.addAll(probableAccounts);	// default ids
			
			// Get all available access for this mirror
			Iterator accessIterator = mirror.selectNodes("./access").iterator();
			while (accessIterator.hasNext()) {
				Element e = (Element) accessIterator.next();
				Access access = new Access(host, e.attributeValue("protocol"), 
												 e.attributeValue("path"));
				accessList.add(access);
			}

			// getting cron expression (if exists it is overriding the default one)
			Element cronElt = (Element)mirror.selectSingleNode("cron");
			String cronExpression = defaultCronExpression;

			if(cronElt != null) {
				String expression = cronElt.attributeValue("expression");
				if(expression != null)
					cronExpression = expression;
			}
			
			// create the new secondary mirror and add it to the list
			ISecondaryMirror m = mirrorFactory.getSecondary(mirrorName, accessList, ids, cronExpression);
			mirrors.add(m);
		}
		return mirrors;
	}
	
	/**
	 * @deprecated
	 * @return
	 * @throws ConfigException
	
	public MonitoredMirrors getMonitoredMirrors() throws ConfigException {
		MonitoredMirrors.getInstance().add((List<Node>)doc.selectNodes("//secondarymirror"));
		return MonitoredMirrors.getInstance();
	}
	*/

	/**
	 * This method tries to parse a XML node 'user' to get all the information
	 * corresponding to an account and if it works it creates a new Identity object.
	 * @param userNode
	 * @return
	 * @throws ConfigException
	 */
	private Identity probeIdentity(Element userNode) throws ConfigException {
		String login, passwd, keyfile, realName;

		/*
		 * Getting basic information
		 */
		login = userNode.attributeValue("login");
		if(login == null)
			throw new ConfigException("The login value is missing");
		
		// now the real name is optional (and it can be null)
		realName = userNode.attributeValue("realname");
		
		
		/* 
		 * Probing/getting a dsakey node
		 * (It should not be hard to add support for alternative keys to be tried out,
		 * but for now only the first key entered will be considered. More keys can 
		 * always be added specifically mirror by mirror.) 
		 */
		keyfile = null;
		passwd  = null;
		Element dsakey = (Element)userNode.selectSingleNode("dsakey");
		if(dsakey != null) {
			// get values
			passwd  = dsakey.attributeValue("passwd");
			keyfile = dsakey.attributeValue("path");
			
			// no passwd attribute is interpreted as an empty password
			if(passwd == null)
				passwd = "";
			
			String userHome = System.getProperty("user.home");	// get unix ~
			if(keyfile.contains("~"))
				keyfile = keyfile.replace("~", userHome);
			
			// probe the existence of the dsa key file or die here
			File testKeyFile = new File(keyfile);
			if (!testKeyFile.canRead())
				throw new ConfigException("Unable to open " + keyfile
						+ " for reading");
			
		}
		
		/*
		 * Probing/getting a passwd node
		 * but only if no dsa key has been found
		 */
		if(keyfile == null) {
			Element passwdNode = (Element)userNode.selectSingleNode("passwd");
			if(passwdNode != null)
				passwd = passwdNode.getText();
			else
				// no authentication: interpret that as an empty passwd
				passwd = "";
		}	
		
		/* Note about the Dom4j API
		 * alternative way passwd =
		 * appUser.selectSingleNode("./dsakey/@passwd").getText();
		 * pathToDSAKey =
		 * appUser.selectSingleNode("./dsakey/@path").getText();
		 */
		
		return new Identity(login, passwd, keyfile, realName);

	}
	
	/**
	 * @param filename
	 */
	public Config(String filename) throws ConfigException {
		this(new File(filename));
	}

	/**
	 * Loads a document from a file.
	 * 
	 * @param aFile
	 *            the data source
	 * @throw a org.dom4j.DocumentExcepiton occurs on parsing failure.
	 */
	private void parseWithSAX(File aFile) throws DocumentException {
		SAXReader xmlReader = new SAXReader();
		this.doc = xmlReader.read(aFile);
	}

	/**
	 * Returns the list of default accounts to be used to connect to the mirrors.
	 * @return
	 */
	public ArrayList<Identity> getProbableAccounts() {
		return probableAccounts;
	}
	
	/**
	 * If more than one account has been defined as the default, common, account
	 * for all of the mirrors then this methods tells how many they are.
	 * Note: each mirror can have its own specific account which will override
	 * the default accounts.
	 * @return
	 */
	public int getNumberOfDefaultAccountsDefined() {
		return probableAccounts.size();
	}
	
	/**
	 * This methods looks in the configuration file for a <listing> node.
	 * This defines a url to access a list of mirror urls.
	 * @return
	 */
	public ArrayList<Access> getMirrorAccessList() {
		ArrayList<Access> accessList = new ArrayList<Access>();
		
		Iterator nodes = doc.selectNodes("/mirrormetrics/listing").iterator();
		while(nodes.hasNext()) {
			Element listing = (Element)nodes.next();
			String host			= listing.attributeValue("host");
			
			// looping thru access nodes (child nodes of listing)
			Iterator accessIterator = listing.elementIterator();
			while (accessIterator.hasNext()) {
				Element e 	= (Element)accessIterator.next();
				Access access = new Access(host, e.attributeValue("protocol"), e
						.attributeValue("path"));
		
				accessList.add(access);
			}			
		}		
		
		return accessList;
	}

	/**
	 * The Analysis level defines the level of detail the
	 * mirror is examined
	 * @return the analysis level defined
	 */
	public AnalysisLevel getAnalysis() {
		return analysis;
	}
	
	/**
	 * The storage format relates to the persistency options:
	 *  - XML database
	 *  - RDBMS
	 *  - other?
	 * @return the defined storage format
	 */
	public int getStorageFormat() {
		return storageFormat;
	}
	
	/**
	 * @author radu
	 * @return the JDBC host
	 */
	public String getJdbcHost() {
		return new String(jdbcHost);
	}
	
	/**
	 * @author radu
	 * @return the JDBC user name
	 */
	public String getJdbcUserName() {
		return new String(jdbcUserName);
	}

	/**
	 * @author radu
	 * @return the JDBC user password
	 */
	public String getJdbcUserPasswd() {
		return new String(jdbcUserPasswd);
	}
	
	/**
	 * @return the installation direction of this application
	 */
	public static String getHomeDir() {
		return HOME_DIR;
	}

	public int getIntervalHours() {
		return intervalHours;
	}
	
	public int getDeltaLimit() {
		return deltaLimit;
	}

	public String getLoggingSystemConfigFile() {
		return loggingSystemConfigFile;
	}

	public int getLoggingSystem() {
		return loggingSystem;
	}

	public File getSourceFile() {
		return configFile;
	}

	public static String getBasePath() {
		return basePath;
	}
	
	public static void setBasePath(String s) {
		basePath = s;
	}

	public static String getBasePathXMLStorage() {
		return basePathXMLStorage;
	}

	public static void setBasePathXMLStorage(String basePathXMLStorage) {
		Config.basePathXMLStorage = basePathXMLStorage;
	}
	
	public static String getBDBXMLEnvHome() {
		return bdbxmlEnvHome;
	}

	public static void setBDBXMLEnvHome(String bdbxmlEnvHome) {
		Config.bdbxmlEnvHome = bdbxmlEnvHome;
	}

	public static Iterator<String> getMonitoredDirectories() {
		return monitoredDirs.iterator();
	}
/*
	public static String getAdminPassword() {
		return adminPassword;
	}
*/
}
