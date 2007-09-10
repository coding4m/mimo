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

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edos.mimo.connection.ConnectionFactory;
import edos.mimo.connection.IConnection;
import edos.mimo.connection.IConnectionFactory;
import edos.mimo.dom.ITreeMirrorStructure;
import edos.mimo.dom.TreeMirrorStructure;
import edos.mimo.exception.ConfigException;
import edos.mimo.filesystem.IFile;

public class AbstractMirror implements IMirror {
	private static Logger logger = Logger.getLogger(AbstractMirror.class);
	private List<IPass> passes;
	private String name;	// a default name would be computed from the first Access (see also getName())
	//private List<Identity> defaultIds;
	//private List<Access> accessList;
	protected IConnection conn;
	private IPass pass;					// keeps the pass used in the current connection
	private AnalysisLevel analysis;
	private boolean online = false;
	private String cronExpression = "0 0 2 * * ? *";	//default is everyday at 2am
	protected IMirrorStructure structure; 
	protected int storageFormat = Config.TREE; // type of structure
	//protected long checkinTime = -1;
	//protected long checkoutTime;
	protected Date checkinTime = new Date();
	protected Date checkoutTime;
	protected String mirrorID = null;
	private String latestErrorMessage = "No connection yet";
	private Date lastVisit = null;
	private boolean scheduled = false;

	
	
	public AbstractMirror(String name, List<IPass> passes) {
		this(name, passes, null, null);
	}
	
	public AbstractMirror(List<IPass> passes) {
		this(passes.get(0).getAccess().getHost(), passes);
	}
	
	/*
	public AbstractMirror(String name, List<Access> accessList, List<Identity> defaultIds ) {
		this.name = name;
		this.accessList = accessList;
		this.defaultIds = defaultIds;
	}
	
	public AbstractMirror(List<Access> accessList, List<Identity> defaultIds ) {
		this(accessList.get(0).toString(), accessList, defaultIds);
	}
	*/
	
	public AbstractMirror(String name, List<IPass> passes, AnalysisLevel analysis, String cronExpression) {
		if(name == null)
			name = "unnamed mirror";
		this.name = name;
		this.passes = passes;
		conn = null;
		pass = null;
		if(analysis == null)
			analysis = new AnalysisLevel();
		this.analysis = analysis;
		if(cronExpression != null)
			this.cronExpression = cronExpression;		
	}

	public AbstractMirror(String name, List<IPass> passes,
			AnalysisLevel analysis, String cronExpression, int storageFormat) {
		if(name == null)
			name = "unnamed mirror";
		this.name = name;
		this.passes = passes;
		conn = null;
		pass = null;
		if(analysis == null)
			analysis = new AnalysisLevel();
		this.analysis = analysis;
		if(cronExpression != null)
			this.cronExpression = cronExpression;
		this.storageFormat = storageFormat;
	}
	
	
	public Iterator<Access> accessIterator() {
		return getAccessList().iterator();
	}

	public List<Access> getAccessList() {
		Iterator<IPass> passes = this.passes.iterator();
		LinkedList<Access> accesses = new LinkedList<Access>();
		int accessCount = 0;
		
		while(passes.hasNext()) {
			accesses.add(passes.next().getAccess());
			accessCount++;
		}
		
		return accesses;
	}

	
	/**
	 * Counts the number of <code>Access</code>es defined for this host.
	 * @return
	 */
	public int accessCount() {
		return this.passes.size();
	}

	public Access getMainAccess() {
		// the main access is the first access in the list
		return passes.get(0).getAccess();
	}

	public boolean hasMultipleAccess() {
		return (accessCount()>1)?true:false;
	}
	
	public Iterator<IPass> passIterator() {
		return passes.iterator();
	}
	
	public void addPass(IPass pass) {
		passes.add(pass);
	}
	
	public void removePass(IPass pass) {
		passes.remove(pass);
	}

/*	
	public Iterator<Access> accessIterator() {
		return accessList.iterator();
	}
	
	public Access getMainAccess() {
		return accessList.get(0);
	}
	
	public boolean hasMultipleAccess() {
		return (accessList.size() > 1)?true:false;
	}
*/
	/**
	 * Try to connect using the known passes one by one until a valid
	 * pass is found. If no valid pass exist then an exception is thrown
	 * with all the accumulated exceptions obtained at each connection
	 * attempt.
	 */
	public void connect() throws IOException { 
		//logger.setLevel(Level.DEBUG);
		logger.debug("connection attempt " + name + "...");
		StringBuffer message = new StringBuffer();
		disconnect();	//	close any existing connection
		
		// if previously connected with a working access, try this one first
		if(pass != null) {
			try{
				logger.debug("Trying to connect with " + pass);
				connectWith(pass);
				logger.debug("Successful connection with " + pass);
				
			}catch(IOException ioe){
				logger.info("tried unsuccessfully pass " + pass);
				// fails silently
			}
			return;
		}
		
		// try all available passes in order
		Iterator<IPass> p = passes.iterator();
		while(p.hasNext()) {
			IPass pass = p.next();
			if(pass == null)
				continue;
			
			try {
				logger.debug("Trying to connect with " + pass);
				connectWith(pass);
				logger.debug("Successful connection with " + pass);
				setActivePass(pass);
				online = true;
				//latestErrorMessage = null;	// no problem
				return;	// if success
			
			}catch(IOException ioe) {
				logger.debug("Error connecting " + name + " " + ioe.getMessage());
				message.append("Connection with ").append(pass.getAccess());
				message.append(" with ").append(pass.getIdentity()).append("\n");
				message.append(ioe.getMessage());
				message.append("\n\n");
			}
		}
		latestErrorMessage = message.toString();
		online = false;
		throw new IOException("Unable to connect in any way:\n\t\t" + message.toString());
	}

	/**
	 * Attempt to connect to this mirror using a specific pass.
	 */
	public void connectWith(IPass pass) throws IOException {
		//logger.setLevel(Level.DEBUG);
		
		IConnectionFactory conFactory = ConnectionFactory.getInstance();
		
		//if(!passes.contains(pass))
		//	passes.add(pass);
		
		if(pass == null)
			throw new IOException("Unable to connect: pass is null");
		
		logger.debug("mirror " + name + " looking for connection");
		conn = conFactory.getConnection(pass);
		conn.connect();
		
		pass.setActive(true);
		online = true;
		this.pass = pass;		// keep the good pass at hand
		lastVisit = checkinTime;			
	}

	/**
	 * Close the connection.
	 */
	public void disconnect() {
		//logger.debug("disconnecting " + name + "...");
		
		if(conn != null)
			conn.close();
		if(pass!= null)
			pass.setActive(false);
		conn = null;
	}
	
	/**
	 * This will fire up the acquire process for the mirror.
	 * The mirror-monitor will crawl and get all files and/or
	 * directories. 
	 * The analysis level  is not given here, it is then the 
	 * default defined within <code>AnalysisLevel</code> unless
	 * the mirror has been set up with a specific analysis level
	 * after or during its creation.
	 * @see edos.mimo.IMirror#acquire()
	 */
	public void acquire() throws IOException  {
		structure = new TreeMirrorStructure(this);
	}

	/**
	 * Forces the acquisition of the structure at a specific level of completness.
	 * @param analysis
	 * @throws IOException 
	 */
	public void acquire(AnalysisLevel analysis) throws IOException {
		structure = new TreeMirrorStructure(this, analysis);
	}
	
	/**
	 * Force the acquisition of a partial structure: only directories.
	 * @throws IOException 
	 */
	public void acquireSkeleton() throws IOException {
		acquire(new AnalysisLevel(AnalysisLevel.SKELETON, AnalysisLevel.VERBOSE));
	}

	/**
	 * Force the acquisition of the complete structure
	 * @throws IOException 
	 */
	public void acquireStructure() throws IOException {
		acquire(new AnalysisLevel(AnalysisLevel.COMPLETE,AnalysisLevel.VERBOSE));
	}

	public IMirrorStructure getStructure() {
		return structure;
	}

	public boolean isOffLine() {
		return !online;
	}

	public boolean isOnLine() {
		return online;
	}

	public List<String> ls() throws IOException {
		return conn.ls();
	}

	public List<IFile> lsl()  throws IOException {
		return conn.lsl();
	}
	
	public String toString() { 
		StringBuffer sb = new StringBuffer();
		sb.append(this.name).append(": ").append(this.getMainAccess());
		if(accessCount() > 1)
			sb.append(" (other access url exist)");
		return sb.toString();
	}

	/**
	 * Returns the user-defined name for this mirror.
	 * This name is probably not unique and it can be obscure (not 
	 * describing much about where is this mirror).
	 * 
	 * The prefered action is to call this.getStructure().getMirrorID()
	 * once a structure exists.
	 * 
	 * In fact, because a mirror can have multiple access (host, protocol, 
	 * etc, combinations) we only know for sure what its prefered access is
	 * after a download. Then we can use it as a unique ID for it.
	 */
	public String getName() {
		return name;
	}

	public IPass getActivePass() {
		return pass;
	}
	
	public IConnection getConnection() {
		return conn;
	}

	public AnalysisLevel getAnalysis() {
		return analysis;
	}

	public String getFilename() {
		if(structure instanceof ITreeMirrorStructure)
			return ((ITreeMirrorStructure)structure).getFilename();
		else 
			return "nofile";
	}

	/**
	 * Tells what subsystem was choosen for persistency
	 * (eg. XML or RDBMS)
	 * @return
	 */
	public int getStorageFormat() {
		return storageFormat;
	}

	/**
	 * Let the mirror know what subsystem was choosen
	 * for its persistency (eg. XML or RDBMS)
	 * @param storageFormat
	 */
	public void setStorageFormat(int storageFormat) {
		this.storageFormat = storageFormat;
	}

	/**
	 * Register the time the download starts
	 * @param timestamp long
	 */
	public void setCheckinTime(long timestamp) {
		checkinTime = new Date(timestamp);
	}

	/**
	 * Register the time the download starts
	 * @param timestamp Date
	 */
	public void setCheckinTime(Date timestamp) {
		checkinTime = timestamp;
	}

	/**
	 * Register the time the download ends
	 * @param timestamp long
	 */
	public void setCheckoutTime(long timestamp) {
		checkoutTime = new Date(timestamp);
	}

	/**
	 * Register the time the download ends
	 * @param timestamp Date
	 */
	public void setCheckoutTime(Date timestamp) {
		checkoutTime = timestamp;
	}

	/**
	 * @return Calendar the time the download starts
	 */
	public long getCheckinTime() {
		return checkinTime.getTimeInMillis();
	}

	/**
	 * @return Calendar the time the download ends
	 */
	public long getCheckoutTime() {
		return checkoutTime.getTimeInMillis();
	}
	
	/**
	 * @return time the download starts in ISO8601 format
	 */
	public String getCheckinTimeAsISO8601() {
		return checkinTime.asISO8601();
	}
	
	/**
	 * @return time the download ends in ISO8601 format
	 */
	public String getCheckoutTimeAsISO8601() {
		return checkoutTime.asISO8601();
	}
	
	/**
	 * Returns how much time has elapsed between the start
	 * of the download and the end of it, when getting
	 * the latest version of this mirror structure.
	 * @return int time in milliseconds
	 */
	public long getDownloadDelayInMillis() {
		return checkoutTime.getTimeInMillis() - checkinTime.getTimeInMillis();
	}

	/**
	 * This method is used only for testing.
	 * For regular use, @see acquire()
	 * @param structure the tree structure
	 * TODO make generic
	 */
	public void setStructure(IMirrorStructure structure) {
		this.structure = structure;
	}

	/** 
	 * TODO implement storage for pass
	 * @param pass
	 */
	public void setActivePass(IPass pass) {
		this.pass = pass;
		pass.setActive(true);
	}

	/**
	 * Returns a unique identifier for this mirror
	 */
	public String getMirrorID() throws ConfigException {
		if(mirrorID  == null) {
			if(getActivePass() == null)
				throw new ConfigException("Mirror needs to connect at least once to get an ID");
			
			Access a = getActivePass().getAccess();
			mirrorID = a.getHost() + "_" + a.getProtocol();
		}
		return mirrorID;	// reputed unique for a mirror
	}
	
	/**
	 * Sets the mirror ID
	 * 
	 * This mirror is useful when the mirror information is read
	 * from a file (testing and delta instantiation from file/database).
	 * @param id
	 */
	public void setMirrorID(String id) {
		mirrorID = id;
	}

	public String getCronExpression() {
		return cronExpression;
	}

	public void setCronExpression(String cronExp) {
		cronExpression = cronExp;
	}

	/**
	 * Tells if a mirror is workable or not.
	 * 
	 * A workable mirror is one that the mirror monitor is able
	 * to connect to at this point in time.
	 * 
	 * A side-effect (positive) is that a <code>IPass</code> is
	 * defined and named "active pass" for the rest of the session.
	 * 
	 * This method catch all exceptions and send them to the logger.
	 * It only returns true/false.
	 * 
	 * @return true if a connection can be established
	 */
	public boolean isWorkable() {
		logger.setLevel(Level.DEBUG);
		logger.debug("isWorkable()");
		
		try{
			connect();
			disconnect();
			return true;
			
		}catch(Exception e) {
			String id = (mirrorID == null)?name:mirrorID;
			logger.error("not workable (" + id + "): " + e.getMessage());
			return false;
		}
	}

	public List<IPass> getPasses() {
		return passes;
	}

	public boolean canConnect() {
		logger.setLevel(Level.DEBUG);
		logger.debug("canConnect()");
		
		boolean canDo = false;
		
		try {
			connect();
			if(online)
				canDo = true;
			disconnect();
			
		} catch (IOException e) {
			// don't care
		}
		
		return canDo;
	}

	public String getLastVisit() {
		return (lastVisit == null)?"never":lastVisit.asISO8601();
	}

	public void setErrorMessage(String message) {
		latestErrorMessage = message;
	}
	
	public String getErrorMessage() {
		return latestErrorMessage;
	}

	public boolean isScheduled() {
		return scheduled;
	}
	
	public void setScheduled(boolean s) {
		scheduled = s;
	}
}
