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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import edos.distribution.mirror.DOMDeltaGeneratorJob;
import edos.mimo.Config;
import edos.mimo.IMirrorStatus;
import edos.mimo.MirrorStatus;
import edos.mimo.MonitorApplication;
import edos.mimo.dom.DOMMirrorDelta;
import edos.mimo.dom.ITreeMirrorStructure;
import edos.mimo.dom.db.BDBXMLManager;
import edos.mimo.exception.BDBXMLException;

public class DOMDeltaGeneratorJob  implements Job {
	private static Logger logger = Logger.getLogger(DOMDeltaGeneratorJob.class);

	
	/**
	 * Required public no-argument constructor
	 */
	public DOMDeltaGeneratorJob() {
		
	}

	/**
	 * Method fired up by the job scheduler
	 */
	public void execute(JobExecutionContext context) throws JobExecutionException {
		//logger.setLevel(Level.DEBUG);
		
		ITreeMirrorStructure oldest = null, latest;
		BDBXMLManager manager = null;
		
		logger.info("Job execution starting...");
		
		/*
		 * SET UP
		 */
		JobDataMap dataMap = context.getJobDetail().getJobDataMap();
		latest = (ITreeMirrorStructure)dataMap.get("structure");		

		try {
			manager = BDBXMLManager.getInstance();
			
			logger.debug("mirror id=" + latest.getMirrorID());
			logger.debug("delta document count=" + manager.getDocumentCount(latest.getMirrorID()));
			// No delta is necessary for the first download! (no data to compare to)
			if(manager.getDocumentCount(latest.getMirrorID()) < 1) {
				logger.info("First download: skipping delta generation and saving " 
												+ latest.getMirrorID() + "...");
				manager.save(latest);
				return;
			}

			logger.debug("BDBXML manager=" + manager);
			// the previous version of this mirror structure is the latest from the database
			if(latest.isMaster())
				oldest = (ITreeMirrorStructure)manager.getLatestMasterStructure();
			else
				oldest = (ITreeMirrorStructure)manager.getLatestStructure(latest.getMirrorID());
			
			if(oldest.getDocumentID().equals(latest.getDocumentID())) {
				logger.fatal("Tentative to generate a delta with itself!");
				throw new Exception("Aborted.");	// terminate
			}
			
		} catch (BDBXMLException e) {
			logger.fatal("BDBXML exception: " + e.getMessage());
			throw new JobExecutionException(e);
			
		} catch (DocumentException e) {
			logger.fatal("Document Exception: " + e.getMessage());
			throw new JobExecutionException(e);
			
		} catch (Exception e) {
			logger.fatal("Exception: " + e.getMessage());
			throw new JobExecutionException(e);
		}

		
		
		/*
		 * JOB
		 * Generate the delta
		 * A delta is a difference between 2 historical versions
		 * of the same mirror (structure).
		 */
		long startTimeInMillis = new GregorianCalendar().getTimeInMillis();
		
		DOMMirrorDelta delta = new DOMMirrorDelta(oldest, latest);

		double executionTime = (new GregorianCalendar().getTimeInMillis()
					- startTimeInMillis) / 1000.0;
		logger.info("Delta done in " + executionTime + " seconds");
		
		
		/*
		 * STORING to DB
		 */
		try {
			IMirrorStatus status = new MirrorStatus(latest, delta);
			manager.save(status);
			
		}catch(BDBXMLException bdbe) {
			logger.error("Unable to store neither full structure not delta ("
					+ delta.getFileName() + ")");
		}
		
		
		/*
		 * STORING to File system
		 */ 
		try {
			delta.save(Config.getBasePathXMLStorage());
			logger.info("Delta saved in "+ Config.getBasePathXMLStorage() + ": " + delta.getFileName());
			
		}catch(IOException ioe) {
			logger.error("Unable to save delta in "+ Config.getBasePathXMLStorage() + ": " + delta.getFileName());
			throw new JobExecutionException(ioe);
		}
		
		
		logger.info("Exited successfully");
	}

}
