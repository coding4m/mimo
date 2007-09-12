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
package edos.mimo.job;

import java.io.IOException;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

import edos.distribution.mirror.DOMDeltaGeneratorJob;
import edos.distribution.mirror.DOMDiffGeneratorJob;
import edos.distribution.mirror.DownloaderJob;
import edos.mimo.IMirror;
import edos.mimo.ISecondaryMirror;
import edos.mimo.MonitorApplication;
import edos.mimo.dom.ITreeMirrorStructure;

/**
 * Downloads the data from the mirror
 * and fires up the next process in the pipeline.
 * 
 * The next jobs (dealing with delta and diff generation)
 * are more CPU intensive while the download is obviously
 * more IO intensive -and depending on external network 
 * conditions. Therefore it may make sense to separate
 * these jobs to be run in different threads to give the
 * scheduler a chance to perform optimization.
 * 
 * @author marc
 *
 */
public class DownloaderJob implements Job {
	private static Logger logger = Logger.getLogger(DownloaderJob.class);
	
	private IMirror mirror;					// holds a reference to the current mirror
	private ITreeMirrorStructure mirrorStructure;	// holds a reference to the internal representation of this mirror

	private JobDetail diffJobDetail;
	private JobDetail deltaJobDetail;

	/**
	 * Required public no-argument constructor
	 *
	 */
	public DownloaderJob() {
		super();
	}

	/**
	 * Method fired up by the job scheduler
	 */
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		logger.info("Job execution starting...");
		
		/*
		 * SET UP
		 */
		JobDataMap dataMap = context.getJobDetail().getJobDataMap();
		mirror = (IMirror)dataMap.get(MonitorApplication.MIRROR);
		//deltaJobDetail = (JobDetail)dataMap.get(MonitorApplication.DELTA_GENERATOR);
		//diffJobDetail = (JobDetail)dataMap.get(MonitorApplication.DIFF_GENERATOR);	// null for master

		logger.info("\tstarting download: " + mirror.getName());
		
		/*
		 * JOB
		 */
		try {
			// connection and processing
			mirror.connect();
			
			mirror.setCheckinTime(GregorianCalendar.getInstance().getTimeInMillis() );
			mirror.acquire();
			mirror.setCheckoutTime(GregorianCalendar.getInstance().getTimeInMillis());

			// data manipulation, reports using a custom tree structure
			mirrorStructure = (ITreeMirrorStructure)mirror.getStructure(); 
			
			//BDBXMLManager manager = BDBXMLManager.getInstance();
			//manager.save(mirrorStructure);
			
			logger.info("Completed download: " + mirror.getName() 
					+ " ("+ mirror.getActivePass().getAccess() + ")"
					+ " in " + (mirror.getDownloadDelayInMillis() / 1000.0)
					+ " milliseconds");
				
			/*
			 * NEXT JOBS IN THE PIPELINE
			 *  	- Delta is for everybody
			 * 		- Diff is only for mirrors (!= master)
			 */
			fireDeltaJob();
			
			if(mirror instanceof ISecondaryMirror)
				fireDiffJob();
			
			// if everything is fine ; nothing to report
			mirror.setErrorMessage(null);
			
		} catch (IOException ioe) {
			mirror.setErrorMessage(ioe.getMessage());
			logger.error(ioe.getMessage());

		} catch (Exception e) {
			mirror.setErrorMessage(e.getMessage());
			logger.error(e.getMessage());

		} finally {
			if (mirror != null)
				mirror.disconnect();
		}
		
	}
	
	private void fireDeltaJob() {

		try{
			/*
			 * FIRE UP NEXT JOBS in the pipeline (for mirrors only ie != master)
			 * ------------------
			 * From here 2 actions must be taken:
			 * 		1. diff generation then outputed in a publicly accessible directory
			 * 		2. storage of the structure (including delta generation and DB access)
			 * 
			 * Each action is taken care of by a specific job (ie Thread).
			 */
			SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
			Scheduler sched = schedFact.getScheduler();
			
			String mirrorID = mirrorStructure.getMirrorID();
						
			// DELTA GENERATION & STORAGE
			
			JobDetail deltaJobDetail  =  new JobDetail("DeltaGenerator for " + mirrorID,
										MonitorApplication.DELTA_GENERATOR, // default group
										DOMDeltaGeneratorJob.class); // the delta job
			deltaJobDetail.getJobDataMap().put("structure", mirrorStructure);
			
			Trigger deltaTrigger = new SimpleTrigger("Delta trigger for " + mirrorID, null);
			sched.scheduleJob(deltaJobDetail, deltaTrigger);
			logger.info("Fired DeltaGeneratorJob for " + mirrorID);
		
		}catch(SchedulerException se) {
			logger.error(se.getMessage());
		}
		
	}
	
	private void fireDiffJob() {

		try{
			/*
			 * FIRE UP NEXT JOBS in the pipeline (for mirrors only ie != master)
			 * ------------------
			 * From here 2 actions must be taken:
			 * 		1. diff generation then outputed in a publicly accessible directory
			 * 		2. storage of the structure (including delta generation and DB access)
			 * 
			 * Each action is taken care of by a specific job (ie Thread).
			 */
			SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
			Scheduler sched = schedFact.getScheduler();
			
			String mirrorID = mirrorStructure.getMirrorID();
			
			// I) DIFF GENERATION
			
			JobDetail diffJobDetail = new JobDetail("DiffGenerator for " + mirrorID,
										MonitorApplication.DIFF_GENERATOR, // default group
										DOMDiffGeneratorJob.class); // the diff job
										
			diffJobDetail.getJobDataMap().put("mirror", mirror);
			Trigger diffTrigger = new SimpleTrigger("Diff trigger for " + mirrorID, null);
			sched.scheduleJob(diffJobDetail, diffTrigger);
			logger.info("Fired DiffGeneratorJob for " + mirrorID);
					
		}catch(SchedulerException se) {
			logger.error(se.getMessage());
		}
	}
}
