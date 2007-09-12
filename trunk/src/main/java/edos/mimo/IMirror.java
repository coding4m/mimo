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
import java.util.List;

import edos.mimo.connection.IConnection;
import edos.mimo.exception.ConfigException;
import edos.mimo.filesystem.IFile;

public interface IMirror {

	// main information/fields
	public static final String TYPE = "type";
	public static final String NAME = "name";
	public static final String HOST = "host";
	public static final String PROTOCOL = "protocol";
	public static final String PATH = "path";

	// mirror types
	public static final String MASTER = "master";
	public static final String MIRROR = "mirror";
	public static final String CHECKINTIME = "checkinTime";
	public static final String CHECKOUTTIME = "checkoutTime";

	/*
	 * METHODS TO INTERFACE WITH THE MIRROR (SERVER)
	 */
	public Iterator<Access> accessIterator();
	public List<Access> getAccessList();
	public Access getMainAccess();
	public boolean hasMultipleAccess();
	
	/*
	public Iterator<IPass> passIterator();
	public void addPass(IPass pass); 
	public void removePass(IPass pass);
	*/
	public IPass getActivePass();
	public Iterator<IPass> passIterator();
	public void setActivePass(IPass pass);
	public List<IPass> getPasses();
	
	public void connect() throws IOException;
	public void connectWith(IPass p) throws IOException;
	
	public void disconnect();
	
	public boolean isWorkable();	// true if connection is working
	
	
	/*
	 * METHODS RELATED TO DOWNLOADING AND MIRROR STATUS
	 */
	public void acquire() throws IOException;
	public void acquire(AnalysisLevel a) throws IOException;
	//public void acquireDirectoryStructure();
	public void acquireSkeleton() throws IOException;
	//public void acquireFileSystemStructure();
	public void acquireStructure() throws IOException;
	
	public IMirrorStructure getStructure();
	
	public boolean isOffLine();
	public boolean isOnLine();
	
	public String getMirrorID() throws ConfigException;	// unique identifier
	
	
	/*
	 * Monitoring the time it takes to download 
	 * the mirror structure
	 */
	public void setCheckinTime(long timestamp);
	public void setCheckinTime(Date timestamp);
	public void setCheckoutTime(long timestamp);
	public void setCheckoutTime(Date date);
	public long getCheckinTime();
	public long getCheckoutTime();
	public String getCheckinTimeAsISO8601();
	public String getCheckoutTimeAsISO8601();
	public long getDownloadDelayInMillis();
	public String getLastVisit();			// date	in ISO8601 format or "never"
	
	// scheduling
	public String getCronExpression();
	public void setCronExpression(String cronExp);
	
	/*
	 * METHODS RELATED WITH MANUAL NAVIGATION (and testing)
	 */
	public List<String> ls() throws IOException;
	public List<IFile>  lsl() throws IOException;
	
	/*
	 * VARIA
	 */
	public String toString();				// a computed name of the mirror
	public IConnection getConnection();		// an active connection (or not)
	public String getName();				// mirror name (not unique; the prefered way is struct.getMirrorID())
	public AnalysisLevel getAnalysis();		// to which level of details the analysis is conducted
	public String getFilename();			// used for filesystem storage (should be unique)
	
	/*
	 * For TESTING and INSTANTIATION from file/database 
	 * (eg. pulling a structure or a delta back for comparison purposes)
	 */
	public void setStructure(IMirrorStructure s);
	public void setMirrorID(String string);
	
	
	public boolean canConnect();
	/**
	 * Latest connection error message
	 * @param message
	 */
	public void setErrorMessage(String message);
	
	/**
	 * Returns latest connection error message
	 * @return
	 */
	public String getErrorMessage();
	
	/**
	 * Tells if a job is scheduled for this mirror
	 * @return
	 */
	public boolean isScheduled();
	public void setScheduled(boolean b);
}
