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
package edos.mimo.dal;

import org.apache.log4j.Logger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseJDBC implements IDatabase {	
	private static Logger logger = Logger.getLogger(DatabaseJDBC.class); 
	private static DatabaseJDBC instance = null;
	private Connection conn = null;

	public DatabaseJDBC () {
		try {
			init();
		}
		catch (Exception e) {
			logger.fatal("Unable to start MySQL database");
			logger.fatal(e.getStackTrace());
		}
	}
		

	public static DatabaseJDBC getInstance() {
		if (null == instance)
			instance = new DatabaseJDBC();
		return instance;
	}
	
	
	private void init() throws ClassNotFoundException,
	InstantiationException, IllegalAccessException {
        try {        	
        	/* Comments from MySQL Connector/J driver documentation:
        	 * 
             * The newInstance() call is a work around for some
             * broken Java implementations
             */
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception ex) {
        	logger.fatal("Unable to load JDBC driver");
            logger.fatal(ex.getStackTrace());
            // handle the error
        }
	}

	
	public Connection getConnection(String hostName, String databaseName, String userName, String userPasswd) {		
        try {		
        	conn = DriverManager.getConnection("jdbc:mysql://"+hostName+"/"+databaseName, userName, userPasswd);
//      	conn = DriverManager.getConnection("jdbc:mysql://localhost/MIRRORDB?user=radu&password=");
        } catch (SQLException ex) {
            // handle any errors
            logger.fatal("SQLException: " + ex.getMessage());
            logger.fatal("SQLState: " + ex.getSQLState());
            logger.fatal("VendorError: " + ex.getErrorCode());
            return null;
        }
        
        if (null == conn) System.out.println("null");
        
		return conn;
	}
	
	
	public void shutdown() {
		if (null == instance) return;
        try {
        	conn.close();
        } catch (SQLException ex) {
        	logger.fatal(ex.getStackTrace());
        }
	}
}
