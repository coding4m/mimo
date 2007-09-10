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
import org.apache.log4j.Logger;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import edos.distribution.mirror.ConnectorJob;
import edos.mimo.LightMirrorContent;

public class ConnectorJob implements Job {
	private static Logger logger = Logger.getLogger(ConnectorJob.class);
	private LightMirrorContent content;

	/**
	 * Required public no-argument constructor
	 *
	 */
	public ConnectorJob() {
		super();
	}

	/**
	 * Method fired up by the job scheduler
	 */
	public void execute(JobExecutionContext context) throws JobExecutionException {
		/*
		 * SET UP
		 */
		JobDataMap dataMap = context.getJobDetail().getJobDataMap();
		content = (LightMirrorContent)dataMap.get("content");

		// logger.info("Starting connection at: " + content.getHost());
		
		/*
		 * JOB
		 */
		try {
			
			content.getFileList();
			content.storeFileList();
			// logger.info("Job for: " + content.getHost());
/*			

		} catch (IOException ioe) {
			logger.error(ioe.getMessage());
*/
		} catch (Exception e) {
			logger.error(e.getMessage());

		} finally {
			if (content != null)
				content.getTotalSize();//.disconnect();
		}
		
	}
}
