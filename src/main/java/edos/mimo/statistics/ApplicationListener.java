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
import java.util.GregorianCalendar;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerListener;

import edos.distribution.mirror.ApplicationListener;
import edos.distribution.mirror.WorkflowStatistics;
import edos.mimo.MonitorApplication;
import edos.mimo.dom.db.BDBXMLManager;
import edos.mimo.exception.BDBXMLException;
import edos.mimo.exception.ConfigException;

/**
 * This class takes statistics on the mirror monitoring process itself.
 * 
 * For example, how much time does it take to download a particular
 * mirror information? to generate diff and delta?
 * 
 * TODO improve persistence mechanism
 * For now, the complete statistics are stored only when the full job
 * has been completed. Hence, if the program crashes all information
 * about outgoing processes is lost. This can be fixes by using 
 * XQUERY to update the proper XML document within the database
 * (create a DAO for the statistics class or other approach)
 * 
 * @author marc
 *
 */
public class ApplicationListener implements TriggerListener {
	private static Logger logger = Logger.getLogger(ApplicationListener.class);
	
	private static final String NAME = "Application Listener"; 
	private static ApplicationListener instance = null;
	private Hashtable<String,WorkflowStatistics> workflows;
	private BDBXMLManager manager;
	
	/**
	 * Private constructor for a singleton
	 * @throws ConfigException 
	 *
	 */
	private ApplicationListener() throws ConfigException {
		workflows = new Hashtable<String,WorkflowStatistics>();
		
		try {
			manager = BDBXMLManager.getInstance();
			
		} catch (BDBXMLException e) {
			logger.fatal("Unable to get an instance of the database manager - " + e.getMessage());
			throw new ConfigException("Unable to get an instance of the database manager - "  + e.getMessage());
		}
	}

	/**
	 * Singleton type access
	 * @return
	 * @throws ConfigException 
	 */
	public static ApplicationListener getInstance() throws ConfigException {
		if(instance == null)
			instance = new ApplicationListener();
		return instance;
	}
	
	public String getName() {
		return NAME;
	}

	/**
	 * Called when some job has been fired.
	 * 
	 * If a Download job is fired, they create a new object to trace
	 * the execution time of the whole process. Then tell the database
	 * to store it. It will be updated next (until a new download job
	 * is fired).
	 */
	public void triggerFired(Trigger arg0, JobExecutionContext arg1) {
		String jobGroup = arg0.getJobGroup();
		String mirrorID	= arg0.getJobName();

		logger.info("Firing " + jobGroup + " for " + mirrorID);
		
		long timestamp = new GregorianCalendar().getTimeInMillis();
		
		WorkflowStatistics jobStat = null;
		
		if(jobGroup.equals(MonitorApplication.DOWNLOADER)) {
			jobStat = new WorkflowStatistics(mirrorID, timestamp);
			jobStat.setDownloadStartTime(timestamp);
		
		}else if(jobGroup.equals(MonitorApplication.DIFF_GENERATOR)){
			jobStat = workflows.get(mirrorID);
			jobStat.setDiffStartTime(timestamp);
		
		}else if(jobGroup.equals(MonitorApplication.DELTA_GENERATOR)){
			jobStat = workflows.get(mirrorID);
			jobStat.setDeltaStartTime(timestamp);
		}
				
		workflows.put(mirrorID, jobStat);
	}

	/**
	 * No reason to veto a job execution
	 */
	public boolean vetoJobExecution(Trigger arg0, JobExecutionContext arg1) {
		return false;
	}

	public void triggerMisfired(Trigger arg0) {
		String jobGroup = arg0.getJobGroup();
		String mirrorID	= arg0.getJobName();
		Calendar now = new GregorianCalendar();
		logger.error(jobGroup + " has been misfired for " + mirrorID + " " + now); 	

		WorkflowStatistics jobStat = workflows.get(mirrorID);
		jobStat.addMisfire(jobGroup, now);
	}

	/**
	 * Called by Quartz when a Quartz job completes
	 */
	public void triggerComplete(Trigger arg0, JobExecutionContext arg1, int arg2) {
		String jobGroup = arg0.getJobGroup();
		String mirrorID	= arg0.getJobName();

		logger.info(jobGroup + " job completed for " + mirrorID);
		
		WorkflowStatistics jobStat = workflows.get(mirrorID);
		long timestamp = new GregorianCalendar().getTimeInMillis();
		
		if(jobGroup.equals(MonitorApplication.DOWNLOADER))
			jobStat.setDownloadEndTime(timestamp);
		
		else if(jobGroup.equals(MonitorApplication.DIFF_GENERATOR))
			jobStat.setDiffEndTime(timestamp);
		
		else if(jobGroup.equals(MonitorApplication.DELTA_GENERATOR))
			jobStat.setDeltaEndTime(timestamp);
		
		// persist/update this to the database
		// TODO create a xquery to update the field instead of add/remove
		try {
			manager.save(jobStat);
			
		} catch (BDBXMLException e) {
			logger.error("Unable to update " + jobStat.getDocumentID() 
					+ " to the database");
			logger.error(e.getMessage());
		}
	}

}
