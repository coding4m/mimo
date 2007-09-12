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
/**
 * General schema:
 * 1. Config
 * 2. JdbcConnection
 * 3. Retrieve mirror contents (on threads)
 * 4. Store contents (on threads)
 */

package edos.mimo;

// The Basics
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Calendar;
import java.io.File;
import java.io.IOException;

// For nice logging: Log4j
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

// Quartz Scheduler
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;

// JDBC Connection
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Mirror Monitor
//import org.edos_project.mirror.connection.DownloadCenter;
import edos.distribution.mirror.DatabaseFactory;
import edos.distribution.mirror.DatabaseJDBC;
import edos.mimo.dal.*;
import edos.mimo.exception.ConfigException;
import edos.mimo.job.ConnectorJob;





/**
 * This is the entry-point to the CONNECTOR module of the Monitor program
 * -- under testing: JDBC connection and MySQL storage --
 *  
 * @author radu
 *
 */

public class Connector {
	private static Logger logger;
	private static Config config;
	private static Connection localConnection;
	private static LinkedList<LightMirrorContent> localMirrorList;
	private static Scheduler sched;
	
	public static final int SUCCESS 	= 0;
	public static final int ABORT 		= 1;
	
	// TODO: refactorize query execution
	public static LinkedList<LightMirrorContent> loadMirrors(Connection conn) {
		LinkedList<LightMirrorContent> lightMirrorList = new LinkedList<LightMirrorContent>();
        /* 
         * Get the Mirrors list from "Mirror" table  
         */
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
//            rs = stmt.executeQuery("SHOW TABLES");

            // or alternatively, if you don't know ahead of time that
            // the query will be a SELECT...
            if (stmt.execute("select Mirror_ID, Content, Protocol, Host, Path from Mirror")) {
                rs = stmt.getResultSet();
            }

            while (rs.next()) {
            	LightMirrorContent lmc = new LightMirrorContent(rs.getString("Mirror_ID"),
            													rs.getString("Content"),
            													rs.getString("Protocol"),
            													rs.getString("Host"),
            													rs.getString("Path"));
            	// logger.info(lmc.toString());
            	lightMirrorList.add(lmc);
            }
        } catch (SQLException e) {
            logger.fatal("SQLException: " + e.getMessage());
            logger.fatal("SQLState: " + e.getSQLState());
            logger.fatal("VendorError: " + e.getErrorCode());
        } finally {
            // it is a good idea to release
            // resources in a finally{} block
            // in reverse-order of their creation
            // if they are no-longer needed

            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException sqlEx) { // ignore }
                }

                rs = null;
            }

            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException sqlEx) { // ignore }
                }
                stmt = null;
            }
        }
		return lightMirrorList;
	}


	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		/*
		 * PARSING THE CONFIGURATION FILE
		 */
		try {
			// can take an alternative config file path as parameter 1
			if(args.length < 1)
				config = Config.getInstance();
			else
				config = Config.getInstance(new File(args[0]));
			
			// Logging
			if(config.getLoggingSystem() != Config.LOG4J) {
				System.err.println("Not a valid logging system: " + config.getLoggingSystem()
						+ " Only log4j is supported.");
				System.exit(ABORT);
			}
			DOMConfigurator.configure(config.getLoggingSystemConfigFile());
			logger = Logger.getLogger(Connector.class);			
			logger.info("Configuration loaded.");
			
		} catch (ConfigException e) {
			System.err.println("Blocking error in the configuration file:");
			e.printStackTrace();
			System.exit(ABORT);
		}

		
		DatabaseJDBC db = (DatabaseJDBC)DatabaseFactory.getDatabase(2);
		localConnection = db.getConnection(config.getJdbcHost(), "MIRRORDB", config.getJdbcUserName(), config.getJdbcUserPasswd());		

		localMirrorList = loadMirrors(localConnection);
		Calendar timeStamp = Calendar.getInstance();
		timeStamp.set(Calendar.SECOND, 0);
		
		// test1
/*
		LightMirrorContent test1 = localMirrorList.getFirst();		
		test1.getFileList();
		test1.storeFileList(localConnection, timeStamp);
*/		

		
		
		
		
		/*
		 * SET UP ALL THE JOBS WITH THE SCHEDULER
		 */
		
		int intervalHours = Config.DEFAULT_INTERVAL_HOURS;	// determines the delay between 2 jobs

		
		
		SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
		
		try {
			sched = schedFact.getScheduler();
			
			Iterator<LightMirrorContent> it  = localMirrorList.iterator();
			while(it.hasNext()) {
				LightMirrorContent test2 = it.next();
				
				// logger.info(test2.getHost());
				test2.setJdbcConnection(localConnection);
				test2.setTimeStamp(timeStamp);
				
				JobDetail jobDetail = new JobDetail("Download Job for " + test2,
											null, // default group
											ConnectorJob.class); // the job
				jobDetail.getJobDataMap().put("content", test2);
				
				Trigger trigger = TriggerUtils.makeMinutelyTrigger(intervalHours);
				//Trigger trigger = TriggerUtils.makeHourlyTrigger(intervalHours);
				trigger.setStartTime(new java.util.Date());
				trigger.setName("Download Job for " + test2);
				sched.scheduleJob(jobDetail, trigger);
				
				
				// logger.info("Scheduled job for " + test2);
			}
			
			sched.start();
			logger.info("Scheduler started");
			
		} catch (SchedulerException e) {
			logger.fatal("Unable to get a Quartz scheduler instance");
			logger.fatal(e.getMessage());
			logger.fatal(e.getStackTrace());
			logger.fatal("Exiting");
			System.exit(ABORT);
		}
//*/
		logger.info("Terminated");
	}
}
