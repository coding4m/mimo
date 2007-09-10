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

import java.text.ParseException;

import org.apache.log4j.Logger;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;

import edos.distribution.mirror.DownloaderJob;
import edos.distribution.mirror.Scheduler;
import edos.distribution.mirror.org;
import edos.mimo.IMirror;
import edos.mimo.exception.ConfigException;
import edos.mimo.statistics.ApplicationListener;

/**
 * Singleton class in charge of the scheduling
 * 
 * @author marc
 *
 */
public class Scheduler {
	private static Logger logger = Logger.getLogger(Scheduler.class);

	public static final String MIRROR		= "mirror";
	public static final String DOWNLOADER = "Downloader";
	public static final String DIFF_GENERATOR = "Diff-Generator";
	public static final String DELTA_GENERATOR = "Delta-Generator";
	
	private static Scheduler instance;
	private static org.quartz.Scheduler quartzScheduler;
	
	
	private Scheduler() throws SchedulerException, ConfigException {
		SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
		quartzScheduler = schedFact.getScheduler();
		

		quartzScheduler.addTriggerListener(ApplicationListener.getInstance());
		quartzScheduler.start();
		
	}
	
	
	public static Scheduler getInstance() throws SchedulerException, ConfigException {
		if(instance == null) 
			instance = new Scheduler();
		
		return instance;
	}
	

	/**
	 * Registers a downloader job with the scheduler
	 * 
	 * @param mirror
	 * @throws SchedulerException
	 * @throws ParseException
	 */
	public void schedule(IMirror mirror) throws SchedulerException, ParseException {

        String mirrorID = "none";
		try {
			mirrorID = mirror.getMirrorID();
	        
		} catch (ConfigException e) {
			logger.error("At this point the mirror must have a mirrorID which it has not: " + e.getMessage());
		}
		
		String jobNameAKAMirrorID = mirrorID;
		String downloaderGroupName = DOWNLOADER;
		
		/*
		 * DOWNLOADER job
		 * This job is the point of entry in the workflow.
		 * It needs to know about the 2 jobs which are next (see above).
		 */
		JobDetail jobDetail = new JobDetail(jobNameAKAMirrorID, downloaderGroupName,
									DownloaderJob.class); // the job

		jobDetail.getJobDataMap().put(MIRROR, mirror);
		
		CronTrigger trigger = new CronTrigger("CronTrigger for " + mirror,
									downloaderGroupName, jobNameAKAMirrorID, downloaderGroupName,
									mirror.getCronExpression() );

		trigger.setStartTime(new java.util.Date());
		trigger.setName("Download Job for " + mirror);
		quartzScheduler.scheduleJob(jobDetail, trigger);
		mirror.setScheduled(true);
		
		logger.info("\t->Scheduled job for " + mirror 
				+ " with cron expression '" + mirror.getCronExpression() + "'");
		
	}


	private void pauseJob(String id, String downloader) throws SchedulerException {
		quartzScheduler.pauseJob(id, downloader);
	}

	public void pauseJob(IMirror mirror) throws SchedulerException, ConfigException {
		String id = mirror.getMirrorID();
		pauseJob(id, DOWNLOADER);
		logger.info("paused job for " + id);
	}

	private void resumeJob(String id, String downloader) throws SchedulerException {
		quartzScheduler.resumeJob(id, downloader);
	}
	
	public void resumeJob(IMirror mirror) throws SchedulerException, ConfigException {
		String id = mirror.getMirrorID();
		resumeJob(id, DOWNLOADER);
		logger.info("Resume job for " + id);
	}


	public void shutdown(boolean waitForJobsToComplete) throws SchedulerException {
		quartzScheduler.shutdown(waitForJobsToComplete);
	}
}
