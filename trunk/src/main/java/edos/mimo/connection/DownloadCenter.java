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
package edos.mimo.connection;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Iterator;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;

import edos.distribution.mirror.DownloadCenter;
import edos.distribution.mirror.Downloader;
import edos.distribution.mirror.IDownloaderListener;
import edos.distribution.mirror.MirrorDownloadStatus;
import edos.mimo.Access;
import edos.mimo.Config;
import edos.mimo.IMasterMirror;
import edos.mimo.IMirror;
import edos.mimo.ISecondaryMirror;
import edos.mimo.dom.DOMDiffGenerator;
import edos.mimo.dom.DOMMirrorDiff;
import edos.mimo.dom.db.BDBXMLManager;
import edos.mimo.exception.BDBXMLException;
import edos.mimo.filesystem.DiffGeneratorFactory;
import edos.mimo.filesystem.IDiffGeneratorListener;
import edos.mimo.filesystem.IMirrorDiff;
import edos.mimo.job.DownloadCenterJob;


public class DownloadCenter implements IDownloaderListener, IDiffGeneratorListener {
	private static Logger logger = Logger.getLogger(DownloadCenter.class);
	
	private static DownloadCenter me;
	private ArrayList<Downloader> downloaders;
	private TreeSet<Access> accessSet;
	private IMasterMirror master;
	private Hashtable<Downloader,MirrorDownloadStatus> status;
	private Scheduler sched;										// Quartz scheduler
	private int intervalHours;	// will make the trigger fire the job after each intervalHours
	
	private DownloadCenter() {
		downloaders = new ArrayList<Downloader>();
		accessSet = new TreeSet<Access>();
		master = null;
		status = new Hashtable<Downloader,MirrorDownloadStatus>();
	}
	
	public static DownloadCenter getInstance() {
		if(me == null)
			me = new DownloadCenter();
		return me;
	}	
	
	public void setMaster(IMasterMirror master) throws IOException {
		this.master = master;
		
		if(master == null)
			throw new NullPointerException("The master object cannot be null!");
		
		// the master needs to be an unique and identified mirror
		Iterator<Access> accesses = master.accessIterator();
		while(accesses.hasNext()) {
			Access a = accesses.next();
			if(accessSet.contains(a))
				throw new IOException("The master mirror must be uniquely defined: "
						+ a + " is redundant");
		}
		
		// master also needs to be browsed/synchronized
		addDownloader(master);
	}
	
	public void addDownloader(IMirror mirror) {
		if(mirror == null)
			return;
		
		// if an equivalent access is already registered then the mirror 
		// is also already registered 
		Iterator<Access> accesses = mirror.accessIterator();
		while(accesses.hasNext()) 
			if(accessSet.contains(accesses.next()))
				return;	// ignore this mirror which is already being taken care of
		
		// mirror is not registered already, let's register it now
		accessSet.addAll(mirror.getAccessList());
		Downloader dl = new Downloader(mirror);
		dl.addListener(this);
		downloaders.add(dl);
		status.put(dl, new MirrorDownloadStatus(mirror));
	}
	
	
	public void addDownloaders(List<IMirror> mirrors) {		
		Iterator<IMirror> it = mirrors.iterator();
		
		while(it.hasNext()) 
			addDownloader(it.next());
	}	

	/**
	 * Entry point to this singleton.
	 * 
	 * This method set up the Quartz scheduler and lets it
	 * handle the rest. Quartz will call execute() at the
	 * right time.
	 * 
	 * TODO (optional) a cron-job-like scheduler can be set up for each mirror
	 * 
	 * @see execute()
	 *
	 */
	public void start() {	
		
		// fail fast if no master to base our comparisons to
		if(master == null) {
			logger.fatal("A master mirror needs to be set!");
			logger.fatal("Exiting!");
			System.exit(1);
		}
		

		/*
		 * Setting up the Quartz scheduler
		 * for the big job (to start all downloader threads at once)
		 */
		SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
		try {
			sched = schedFact.getScheduler();
			JobDetail jobDetail = new JobDetail("Fire all downloader threads at once",
										null, // default group
										DownloadCenterJob.class); // the job
			jobDetail.getJobDataMap().put("downloaders", downloaders);
			jobDetail.getJobDataMap().put("status", status);
			Trigger trigger = TriggerUtils.makeHourlyTrigger(intervalHours); // fire every intervalHours hour
			sched.scheduleJob(jobDetail, trigger);
			
			// the quartz schedular will fire up the big job
			// which will start all the downloader threads at the same time
			
		} catch (SchedulerException e) {
			logger.fatal("Unable to get a Quartz scheduler instance");
			logger.fatal("Exiting");
			System.exit(1);
		}
	}
	
	public int getDownloadersCount() {
		return downloaders.size();
	}

	/**
	 * This method is called by a downloader when its job is done.
	 * @throws ParseException 
	 */
	public void downloadComplete(Downloader downloader) throws ParseException {
		// keeping track of the threads' status
		status.get(downloader).downloadEnds();
		
		IMirror mirror = downloader.getMirror();
		
		if(mirror instanceof ISecondaryMirror) {	// ie. not for master 
			// Runs with the DOM-based implementation
			
			DOMDiffGenerator comparator = (DOMDiffGenerator)DiffGeneratorFactory.getDiffGenerator(Config.TREE, master, (ISecondaryMirror)mirror);
			

			// the comparator thread will call back listener's diffCompleted()			
		}		
	}

	/**
	 * This method is called by a <code>DiffGenerator</code> when
	 * its job is done, a IMirrorDiff is available.
	 * 
	 * Naive implementation of a locking mechanism, only
	 * one DownloadCenter, hence only one write at a time.
	 * Note: BDB XML is transaction capable
	 */
	public void diffCompleted(IMirrorDiff diff) {
		if(diff instanceof DOMMirrorDiff	) {			
			/*
			 * File system save: uncomment next lines
			 *
			try{
				((DOMMirrorDiff)diff).save();
				
			}catch(IOException ioe) {
				
			}
			*/
			
			BDBXMLManager manager = null;
			
			try {
				manager = BDBXMLManager.getInstance();
				manager.save(diff);
				logger.info("Saved diff: " + diff.getFileName());
				
			}catch(BDBXMLException e) {
				logger.info("Unable to store " + diff.getFileName());
				logger.info(e.getMessage());
			}
		}
	}

	public TreeSet<Access> getAccessSet() {
		return accessSet;
	}

	public int getIntervalHours() {
		return intervalHours;
	}

	public void setIntervalHours(int intervalHours) {
		this.intervalHours = intervalHours;
	}

	public IMasterMirror getMasterMirror() {
		return master;
	}

	public void close() {
		Iterator<Downloader> it = downloaders.iterator();
		
		while(it.hasNext()) {
			Downloader downloader = it.next();
			// TODO something to terminate cleanly the downloader/job
			logger.fatal("not implemented yet!");
		}
	}
}
