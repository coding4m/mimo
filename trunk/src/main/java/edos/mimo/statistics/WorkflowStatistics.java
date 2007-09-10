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
package edos.mimo.statistics;

import java.util.Calendar;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import edos.mimo.MonitorApplication;
import edos.mimo.dom.db.IXmlDocument;

/**
 * A workflow is a complete job set for a given mirror, 
 * starting with the download of the current status, the
 * comparison with the master copy and the delta generation
 * for storage.
 * 
 * This class retains statistics about this process. It 
 * will allow tuning of the mirror monitoring for a particular
 * platform.
 * 
 * This class can return XML which can then be stored in
 * a database.
 * 
 * @author marc
 *
 */
public class WorkflowStatistics implements IXmlDocument {
	// primary key for this class
	private String mirrorID;		// mirror it relates to
	private long timestamp;		// time the workflow has initiated

	// private attributes
	private Document doc;
	private Element  root;
	
	private long downloadStartTime;
	private long downloadEndTime;
	private long diffStartTime;
	private long diffEndTime;
	private long deltaStartTime;
	private long deltaEndTime;
	
	private Vector<Misfire> misfires;	// registers the misfires occurred
	

	private static final String TIMESTAMP 	= "timestamp";
	private static final String MIRROR		= "mirror";
	private static final String START_TIME	= "start";
	private static final String END_TIME		= "end";
	private static final String MISFIRE		= "misfire";
	private static final String JOB			= "job";
	
	
	/**
	 * Inner class used in the <code>Vector</code>
	 * @author marc
	 *
	 */
	private class Misfire {
		private String job;
		Calendar timestamp;
		
		Misfire(String job, Calendar timestamp) {
			this.job = job;
			this.timestamp = timestamp;
		}

		public String getJob() {
			return job;
		}

		public void setJob(String job) {
			this.job = job;
		}

		public Calendar getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(Calendar timestamp) {
			this.timestamp = timestamp;
		}
	}

	public WorkflowStatistics(String mirrorID, long timestamp) {
		this.mirrorID = mirrorID;
		this.timestamp = timestamp;
		
		misfires = new Vector<Misfire>();
		
		
        doc= DocumentHelper.createDocument();
        doc.addProcessingInstruction("xml-stylesheet", "href=\"../xsl/workflow.xsl\" type=\"text/xml\"");
        
        root = doc.addElement( "workflow" )
        		.addAttribute(MIRROR, mirrorID)
        		.addAttribute(TIMESTAMP, new Long(timestamp).toString());		
		
	}



	public long getDeltaEndTime() {
		return deltaEndTime;
	}



	public void setDeltaEndTime(long deltaEndTime) {
		this.deltaEndTime = deltaEndTime;
		
		root.addElement(MonitorApplication.DELTA_GENERATOR)
			.addAttribute(END_TIME, new Long(deltaEndTime).toString());
	}



	public long getDeltaStartTime() {
		return deltaStartTime;
	}



	public void setDeltaStartTime(long deltaStartTime) {
		this.deltaStartTime = deltaStartTime;
		
		root.addElement(MonitorApplication.DELTA_GENERATOR)
			.addAttribute(START_TIME, new Long(deltaStartTime).toString());
	}



	public long getDiffEndTime() {
		return diffEndTime;
	}



	public void setDiffEndTime(long diffEndTime) {
		this.diffEndTime = diffEndTime;
		
		root.addElement(MonitorApplication.DIFF_GENERATOR)
			.addAttribute(END_TIME, new Long(diffEndTime).toString());
	}



	public long getDiffStartTime() {
		return diffStartTime;
	}



	public void setDiffStartTime(long diffStartTime) {
		this.diffStartTime = diffStartTime;
		
		root.addElement(MonitorApplication.DIFF_GENERATOR)
			.addAttribute(START_TIME, new Long(diffStartTime).toString());
	}



	public long getDownloadEndTime() {
		return downloadEndTime;
	}



	public void setDownloadEndTime(long downloadEndTime) {
		this.downloadEndTime = downloadEndTime;
		
		root.addElement(MonitorApplication.DOWNLOADER)
			.addAttribute(END_TIME, new Long(downloadEndTime).toString());
	}



	public long getDownloadStartTime() {
		return downloadStartTime;
	}



	public void setDownloadStartTime(long downloadStartTime) {
		this.downloadStartTime = downloadStartTime;
		
		root.addElement(MonitorApplication.DOWNLOADER)
			.addAttribute(START_TIME, new Long(downloadStartTime).toString());
	}



	public void addMisfire(String job, Calendar now) {
		misfires.add(new Misfire(job, now));
		
		root.addElement(MISFIRE)
			.addAttribute(JOB, job)
			.addAttribute(TIMESTAMP, now.getTime().toString());
	}


	/**
	 * Returns the content of this workflow statistics as an XML document
	 * @return content as a <code>String</code>
	 */
	public String toString() {
		return doc.asXML();
	}
	
	
	/**
	 * Returns a unique ID for this document
	 * @return a unique ID as a <code>String</code>
	 */
	public String getDocumentID() {
		return mirrorID + "-" + timestamp;
	}
}
