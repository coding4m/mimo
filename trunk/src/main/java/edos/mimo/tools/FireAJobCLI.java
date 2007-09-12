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
package edos.mimo.tools;

import java.util.Scanner;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

import edos.distribution.mirror.FireAJobCLI;
import edos.mimo.Access;
import edos.mimo.Config;
import edos.mimo.ISecondaryMirror;
import edos.mimo.Identity;
import edos.mimo.MirrorFactory;
import edos.mimo.exception.ConfigException;
import edos.mimo.job.DownloaderJob;

/**
 * Does the full process for a Job: download the mirror structure,
 * computes the diffs, the delta and stores to the database.
 * 
 * REQUIRES
 * 	- working BDB XML database
 *  - mirrormetrics-config.xml with a master mirror definition
 * 	- this master mirror data available in the database (unless this 
 * 		job targets a master mirror, in which case it will be added
 * 		at the end of this job)
 * 
 * @author marc
 *
 */
public class FireAJobCLI {
	private static Logger logger = Logger.getLogger(FireAJobCLI.class);
	
	// default mirror to analyse
	private static final String DEFAULT_HOST = "testmachine";
	private static final String DEFAULT_PATH = "/cooker/cooker";
	private static final String DEFAULT_PROTOCOL = "ftp";
	private static final String DEFAULT_USERNAME = "anonymous";
	private static final String DEFAULT_KEYFILE  = "none";
	private static final String DEFAULT_PASSWD = "mirrormonitor@edos-project.org";
	private static final String DEFAULT_REALNAME = "Local Test Site";

	/**
	 * MAIN
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		DOMConfigurator.configure("log4j-config-4testing.xml");
		Scheduler sched;			// Quartz scheduler
		ISecondaryMirror mirror = null;
		String host, path, protocol, name, passwd, keyFile, realName;
		
		/*
		 * STATIC SET UP
		 */
		MirrorFactory mirrorFactory = MirrorFactory.getInstance();
		try {
			Config.getInstance();
			
		} catch (ConfigException e1) {
			logger.fatal(e1.getMessage());
			System.exit(1);
		}
		
		/*
		 * USER INPUT
		 * What is the targeted mirror for this Job
		 */
		Scanner sc = new Scanner(System.in);
		System.out.println("\nSingle Mirror Monitoring: download, diff, delta, XML storage and publishing.");

		System.out.println("\thost [" + DEFAULT_HOST + "]: ");
		host = sc.nextLine();
		if(host.equals(""))
			host = DEFAULT_HOST;
		
		System.out.println("\tpath [" + DEFAULT_PATH + "]: ");
		path = sc.nextLine();
		if(path.equals(""))
			path = DEFAULT_PATH;
		
		System.out.println("\tprotocol [" + DEFAULT_PROTOCOL + "]: ");
		protocol = sc.nextLine();
		if(protocol.equals(""))
			protocol = DEFAULT_PROTOCOL;
		
		// instantiation
		//Access myAccess = new Access("ganimedes", "ftp", "/cooker");
		Access myAccess = new Access(host, protocol, path);
		
		/*
		 * Asking the user for credentials
		 */
		System.out.println("username for " + host + " [" + DEFAULT_USERNAME + "]:");
		name = sc.nextLine();
		if(name.equals(""))
			name = DEFAULT_USERNAME;
		
		System.out.println("password for " + host + " [" + DEFAULT_PASSWD + "]:");
		passwd = sc.nextLine();
		if(passwd.equals(""))
			passwd = DEFAULT_PASSWD;
		
		System.out.println("key file path for " + host + " [" + DEFAULT_KEYFILE + "]:");
		keyFile = sc.nextLine();
		if(keyFile.equals(""))
			keyFile = DEFAULT_KEYFILE;
		
		System.out.println("and your full name is [" + DEFAULT_REALNAME + "]:");
		realName = sc.nextLine();
		if(realName.equals(""))
			realName = DEFAULT_REALNAME;
		

		Identity id = new Identity(name, passwd, keyFile, realName); 
		
		
		/*
		 * Starting downloading/browse info
		 */
		mirror = mirrorFactory.getSecondary("User-defined site", myAccess, id);
		
		
		/*
		 * Setting up the Quartz scheduler
		 * for the big job : Downloader Job,
		 * starting this job will cascade into the start of the other jobs
		 * in the pipeline (each one calling the next one)
		 */
		SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
		
		try {
			sched = schedFact.getScheduler();
			JobDetail jobDetail = new JobDetail("Fire download job for " + mirror,
										null, // default group
										DownloaderJob.class); // the job
			jobDetail.getJobDataMap().put("mirror", mirror);
			Trigger trigger = new SimpleTrigger("Execute once now", null); // fire once and now!
			sched.scheduleJob(jobDetail, trigger);
			sched.start();
			
		} catch (SchedulerException e) {
			logger.fatal("Unable to get a Quartz scheduler instance");
			logger.fatal("Exiting");
			System.exit(1);
		}
		
	}
}
