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
import java.text.ParseException;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import edos.distribution.mirror.DOMDiffGeneratorJob;
import edos.mimo.Config;
import edos.mimo.IMasterMirror;
import edos.mimo.ISecondaryMirror;
import edos.mimo.dom.DOMDiffGenerator;
import edos.mimo.dom.db.BDBXMLManager;
import edos.mimo.exception.BDBXMLException;
import edos.mimo.filesystem.IMirrorDiff;

public class DOMDiffGeneratorJob implements Job {
	private static Logger logger = Logger.getLogger(DOMDiffGeneratorJob.class);

	
	/**
	 * Required public no-argument constructor
	 */
	public DOMDiffGeneratorJob() {
		
	}

	/**
	 * Method fired up by the job scheduler
	 */
	public void execute(JobExecutionContext context) throws JobExecutionException {
		IMasterMirror master = null;
		ISecondaryMirror mirror;
		BDBXMLManager manager = null;
		
		logger.info("Job execution starting...");
		
		/*
		 * SET UP
		 */
		JobDataMap dataMap = context.getJobDetail().getJobDataMap();
		mirror = (ISecondaryMirror)dataMap.get("mirror");		

		try {
			manager = BDBXMLManager.getInstance();
			master = (IMasterMirror) manager.getLatestMasterStructure().getMirror();
			
			//logger.info(manager.getLatestMasterStructure().toString());
			
		} catch (BDBXMLException e) {
			logger.fatal(e.getMessage());
			throw new JobExecutionException("BDBXMLException: " + e);
			
		} catch (DocumentException e) {
			logger.fatal(e.getMessage());
			throw new JobExecutionException("DocumentException: " + e);
			
		} catch (Exception e) {
			logger.fatal(e.getMessage());
			throw new JobExecutionException("Exception: " + e);
		}

		
		
		/*
		 * JOB
		 * Generate the diff (changes between this mirror version and the
		 * latest mirror structure available)
		 */
		long startTimeInMillis = new GregorianCalendar().getTimeInMillis();
		
		DOMDiffGenerator diffGenerator;
		try {
			diffGenerator = new DOMDiffGenerator(master, mirror);
		
		} catch (ParseException e) {
			logger.fatal("date parsing has failed ; " + e.getMessage());
			throw new JobExecutionException(e);
		}
		
		IMirrorDiff diff = diffGenerator.getDiff();
		
		double executionTime = (new GregorianCalendar().getTimeInMillis()
				- startTimeInMillis) / 1000.0;
		logger.info("Diff done in " + executionTime + " seconds");
		
		/*
		 * OUTPUT
		 * The purpose of this diff is to be available publicly.
		 * A file is saved on the filesystem, which can be exposed
		 * via web server.
		 */
		//String dir = "web/diff/";	// relative path to the output directory
		/* not necessary, the document is in the DB
		try {
			diff.save(Config.getBasePathXMLStorage());
			logger.info("Diff saved in " + Config.getBasePathXMLStorage() + ": " + diff.getFileName());
			
		}catch(IOException ioe) {
			logger.error("Unable to save diff in " + Config.getBasePathXMLStorage() + ": " + diff.getFileName());
			throw new JobExecutionException(ioe);
			
		}
		*/
		
		/*
		 * STORING to DB
		 * Stored to be available readily through XQuery (eg. to build total diff)
		 */
		try {
			manager.save(diff);
			logger.info("Saved diff: " + diff.getFileName());
			
		}catch(BDBXMLException bdbe) {
			logger.error("Unable to store " + diff.getFileName());
		}
		
		
	}
}
