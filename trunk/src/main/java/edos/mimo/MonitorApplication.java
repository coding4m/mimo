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

import java.io.File;
import java.text.ParseException;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;

import edos.mimo.connection.DownloadCenter;
import edos.mimo.dom.ITreeMirrorStructure;
import edos.mimo.dom.db.BDBXMLManager;
import edos.mimo.exception.BDBXMLException;
import edos.mimo.exception.ConfigException;
import edos.mimo.job.DOMDeltaGeneratorJob;
import edos.mimo.job.DOMDiffGeneratorJob;
import edos.mimo.job.DownloaderJob;
import edos.mimo.statistics.ApplicationListener;

/**
 * This is the entry-point to the Monitor program.
 *
 */
public class MonitorApplication {
	private static Logger logger;
	private static BDBXMLManager manager = null;
	private static Config config = null;
	private static Scheduler sched = null;		// Quartz scheduler
	
	private static DownloadCenter dcenter = DownloadCenter.getInstance();
	
	public static final int SUCCESS 	= 0;
	public static final int ABORT 		= 1;
	public static final String MIRROR		= "mirror";
	public static final String DOWNLOADER = "Downloader";
	public static final String DIFF_GENERATOR = "Diff-Generator";
	public static final String DELTA_GENERATOR = "Delta-Generator";
	
	
	/**
	 * Start the Mirror Monitoring Application
	 * 
	 * Looks at the first parameter and if it exists, take it as the path
	 * to an alternative configuration file.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		IMasterMirror master = null; 
		List<IMirror> mirrorList = null;

		/*
		 * PARSING THE CONFIGURATION FILE
		 */
		try {
			// can take an alternative config file path as parameter 1
			if(args.length < 1)
				config = Config.getInstance();
			else
				config = Config.getInstance(new File(args[0]));
			
			// mirror information
			master = config.getMasterMirror();
			mirrorList = config.getSecondaryMirrors();
			
			// Logging
			if(config.getLoggingSystem() != Config.LOG4J) {
				System.err.println("Not a valid logging system: " + config.getLoggingSystem()
						+ " Only log4j is supported.");
				exit(ABORT);
			}
			DOMConfigurator.configure(config.getLoggingSystemConfigFile());
			logger = Logger.getLogger(MonitorApplication.class);
			
			logger.info("Configuration loaded using " + config.getSourceFile());
			logger.info("Logging module loaded using " + config.getLoggingSystemConfigFile());
			logger.info("Monitoring ready to start.");
			
		} catch (ConfigException e) {
			/*
			logger.fatal("Blocking error in the configuration file:");
			logger.fatal(e.getStackTrace());
			logger.fatal("Exiting");
			*/
			System.err.println("Blocking error in the configuration file:");
			e.printStackTrace();
			exit(ABORT);
		}

		/*
		 * INITIALIZING THE DATABASE
		 */
		boolean debug = false;
		try {
			manager = BDBXMLManager.getInstance(debug);
			
		} catch (BDBXMLException e) {
			logger.fatal(e.getStackTrace());
			exit(ABORT);
			
		}
		
		
		/*
		 * INITIALIZE MASTER STRUCTURE
		 * Before looking after mirrors, the master structure must be
		 * synchronized with the master mirror.
		 */
		try {
			logger.info("Initializing internal master structure");
			
			// connection and processing
			master.connect();
			
			master.setCheckinTime(GregorianCalendar.getInstance().getTimeInMillis() );
			master.acquire();
			master.setCheckoutTime(GregorianCalendar.getInstance().getTimeInMillis());

			// save the structure in the database
			ITreeMirrorStructure struct = (ITreeMirrorStructure)master.getStructure(); 			
			manager.save(struct);
			
			logger.info("Completed download: " + master.getName() 
					+ " ("+ master.getActivePass().getAccess() + ")"
					+ " in " + (master.getDownloadDelayInMillis() / 1000.0)
					+ " milliseconds");
			
		}catch (Exception e) {
			logger.fatal("Unable to initialize the master structure. The program can't start.");
			logger.fatal(e.getMessage());
			if (master != null)
				master.disconnect();
			exit(ABORT);

		} 
		
		
		/*
		 * SET UP ALL THE JOBS WITH THE SCHEDULER
		 * The master needs to be resynchronized in the regular basis,
		 * and the mirrors as well.
		 * 
		 * The Download Job handles the rest by starting new jobs
		 * in order to produce diffs and deltas. These jobs are one-time
		 * use, they can be seen as simple threads.
		 */
		SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
		IMirror mirror = null;	// temp variable for the loop
		
		try {
			sched = schedFact.getScheduler();
			
			// schedule master mirror
			schedule(master);
			
			// schedule mirrors
			Iterator<IMirror> it  = mirrorList.iterator();
			while(it.hasNext()) {
				IMirror m = it.next();
				if(m.isWorkable())
					schedule(m);				
			}
			
			try {
				sched.addTriggerListener(ApplicationListener.getInstance());
			} catch (ConfigException e) {
				logger.fatal(e.getMessage());
				MonitorApplication.exit(ABORT);
			}
			sched.start();
			logger.info("Scheduler started");
			
		} catch (SchedulerException e) {
			logger.fatal("Unable to get a Quartz scheduler instance");
			logger.fatal(e.getMessage());
			logger.fatal(e.getStackTrace());
			logger.fatal("Exiting");
			exit(ABORT);
			
		} catch (ParseException pe) {
			logger.fatal("Unable to parse the cron expression " + mirror.getCronExpression()
					+ " for mirror " + mirror);
			logger.fatal(pe.getMessage());
			logger.fatal(pe.getStackTrace());
			logger.fatal("Exiting");
			exit(ABORT);
		}
		

		/*
		 * BASIC INTERACTIVITY
		 * For now we let the user shutdown cleanly the program
		 */
		System.out.println("\t\t ----------------");
		System.out.println("\t\t| Mirror Monitor |");
		System.out.println("\t\t ----------------");
		System.out.println("");
		System.out.println("This rustic interface can be extended to pause/add jobs.");
		System.out.println("");
		String help = "(commands available: \\q to quit and \\h to get help)";
		System.out.println(help);
		String input = "";
		Scanner sc = new Scanner(System.in);
		boolean running = true;
		do{
			System.out.println("MMMonitor> ");
			input = sc.nextLine();
			
			if(input.contains("quit") || input.contains("\\q"))
				running =false;
			
			if(input.contains("help") || input.contains("\\h")) 
				System.out.print(help);
			
		}while(running);
		
		// shutdown
		exit(SUCCESS);
		
	}

	/**
	 * Registers a downloader job with the scheduler
	 * 
	 * @param mirror
	 * @throws SchedulerException
	 * @throws ParseException
	 */
	private static 	void schedule(IMirror mirror) throws SchedulerException, ParseException {

        String mirrorID = "none";
		try {
			mirrorID = mirror.getMirrorID();
	        
		} catch (ConfigException e) {
			logger.error("At this point the mirror must have a mirrorID which it has not: " + e.getMessage());
		}
		
		String jobNameAKAMirrorID = mirrorID;
		String downloaderGroupName = DOWNLOADER;
		String diffGeneratorGroupName = DIFF_GENERATOR;
		String deltaGeneratorGroupName = DELTA_GENERATOR;
		
		/*
		 * DELTA job
		 */
		JobDetail deltaJobDetail = new JobDetail(jobNameAKAMirrorID,
								deltaGeneratorGroupName, // default group
								DOMDeltaGeneratorJob.class); // the diff job
		
		/*
		 * DIFF job
		 */
		JobDetail diffJobDetail = new JobDetail(jobNameAKAMirrorID,
								diffGeneratorGroupName, // default group
								DOMDiffGeneratorJob.class); // the diff job
		
		/*
		 * DOWNLOADER job
		 * This job is the point of entry in the workflow.
		 * It needs to know about the 2 jobs which are next (see above).
		 */
		JobDetail jobDetail = new JobDetail(jobNameAKAMirrorID, downloaderGroupName,
									DownloaderJob.class); // the job
		
		jobDetail.getJobDataMap().put(MIRROR, mirror);
		jobDetail.getJobDataMap().put(DELTA_GENERATOR, deltaJobDetail);
		jobDetail.getJobDataMap().put(DIFF_GENERATOR, diffJobDetail);
		
		CronTrigger trigger = new CronTrigger("CronTrigger for " + mirror,
									downloaderGroupName, jobNameAKAMirrorID, downloaderGroupName,
									mirror.getCronExpression() );
		// TODO remove below!
		//Trigger trigger = TriggerUtils.makeMinutelyTrigger(intervalHours);
		//Trigger trigger = TriggerUtils.makeHourlyTrigger(intervalHours);
		trigger.setStartTime(new java.util.Date());
		trigger.setName("Download Job for " + mirror);
		sched.scheduleJob(jobDetail, trigger);
		
		logger.info("Scheduled job for " + mirror 
				+ " with cron expression '" + mirror.getCronExpression() + "'");
		
	}
	
	/**
	 * Clean shutdown of the application, including all threads
	 * and the database which needs to be cleanly shutdown to 
	 * avoid losing integrity.
	 * 
	 * @param status
	 */
	public static void exit(int status) {
		if(status != SUCCESS)
			logger.error("This application was terminated abnormally.");
		
		// TODO deprecated
		if(dcenter != null) dcenter.close();
		
		// Quartz scheduler
		boolean waitForJobsToComplete = true;
		try {
			if(sched != null)
				sched.shutdown(waitForJobsToComplete);
		} catch (SchedulerException e) {
			logger.fatal("Quartz Scheduler shutdowns with an exception");
			logger.fatal(e.getStackTrace());
		}

		// Database
		if(manager != null) manager.close();	// VERY IMPORTANT!
		
		logger.fatal("All systems have cleanly shutdown.");
		
		System.exit(status);
	}
}
