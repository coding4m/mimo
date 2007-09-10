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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import edos.mimo.connection.Downloader;
import edos.mimo.connection.MirrorDownloadStatus;

public class DownloadCenterJob implements Job {
	private static Logger logger = Logger.getLogger(DownloadCenterJob.class);

	private ArrayList<Downloader> downloaders;
	private Hashtable<Downloader,MirrorDownloadStatus> status;
	
	/**
	 * Required public no-argument constructor
	 *
	 */
	public DownloadCenterJob() {
		
	}
	
	
	
	/**
	 * This method is called by Quartz at the time of
	 * firing up the job.
	 * 
	 * @param context
	 */
	public void execute(JobExecutionContext context) {
		
		logger.info("Job execution starting...");
		
		/*
		 * Set up
		 */
		JobDataMap dataMap = context.getJobDetail().getJobDataMap();
		downloaders = (ArrayList<Downloader>)dataMap.get("downloaders");
		status		= (Hashtable<Downloader,MirrorDownloadStatus>)dataMap.get("status");
		
		/*
		 * Starting all downloader threads
		 */
		Iterator<Downloader> it = downloaders.iterator();
		
		while(it.hasNext()) {
			Downloader downloader = it.next();
			MirrorDownloadStatus dls = status.get(downloader);

			if(dls.isActive()) {
				// last download is not completed!
				long minutesWorkingOnIt = dls.getWorkingTimeInMinutes();
				
				if(dls.getWorkingTimeInDays() > 1) {
					logger.error("Downloader " + downloader.getName()
							 + " has not terminated after " 
							 + dls.getWorkingTimeInDays() + " day(s)!");
					// TODO sending an email to an administrator would be appropriate
					
				}else{
				
					logger.warn("Downloader " + downloader.getName()
							+ " is not done yet!");
					logger.warn("Downloader " + downloader.getName()
							+ " is running for " + minutesWorkingOnIt
							+ " minutes");
					logger.warn("Skipping " + downloader.getName());
				}
				
			}else{
				// start new download
				status.get(downloader).downloadStarts();
				
				downloader.start();
				
				// before terminating the thread calls-back here with downloadComplete()
			}
		}
	}
}
