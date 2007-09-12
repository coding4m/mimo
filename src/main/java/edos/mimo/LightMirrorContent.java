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

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import edos.mimo.connection.FTPConnection;
import edos.mimo.connection.HTTPConnection;
import edos.mimo.connection.IConnection;
import edos.mimo.filesystem.IFile;

public class LightMirrorContent {
	private static Logger logger = Logger.getLogger(LightMirrorContent.class);

	// Content's data
	private String Mirror_ID;
	private String Content;
	private String Protocol;
	private String Host;
	private String Path;

	// Content's file list
	private List<IFile> fileList;
	private long totalSize;
	
	// Content's connection
	private IConnection conn;
	private Access acc;
	private int isUp;
	
	// JDBC pointers
	private Connection jdbcConnection;
	private Calendar timeStamp;
	
	
	public LightMirrorContent() {
		this.Mirror_ID = null;
		this.Content = null;
		this.Protocol = null;
		this.Host = null;
		this.Path = null;
		this.fileList = null;
		this.totalSize = 0;
		this.isUp = 0;
	}
	
	
	public LightMirrorContent(String Mirror_ID, String Content, String Protocol, String Host, String Path) {
		this.Mirror_ID = new String(Mirror_ID);
		this.Content = new String(Content);
		this.Protocol = new String(Protocol);
		this.Host = new String(Host);
		this.Path = new String(Path);
		this.acc = new Access(Host, Protocol, Path);

//		IConnectionFactory conFactory = ConnectionFactory.getInstance();
		if (Protocol.equals("ftp")) conn = new FTPConnection(acc);
		else if (Protocol.equals("http")) conn = new HTTPConnection(Host, Path);
		else logger.info("Protocol not supported yet for mirror " + Mirror_ID);
		this.totalSize = 0;
		this.isUp = 0;
	}
	
	
	public void setJdbcConnection(Connection conn) {
		jdbcConnection = conn;
	}
	
	
	public void setTimeStamp(Calendar runTime) {
		timeStamp = runTime;
	}
	
	
	public List<IFile> getFileList() {
		if (null == fileList) fileList = retrieveFileList();
		return fileList;
	}
	
	
	public long getTotalSize() {
		if (0 != totalSize) return totalSize;
		if (null == fileList) return 0;
		int size = 0;
		Iterator<IFile> it = fileList.iterator();
		while (it.hasNext()) size += it.next().getSize();
		totalSize = size;
		return totalSize;
	}
	
	
	private List<IFile> retrieveFileList() {
		List<IFile> flst = null;
		isUp = 0;
		try {
			logger.info("Connect to mirror " + Mirror_ID + " (" + Host + ")...");
			conn.connect();
			isUp = 1;
			logger.info("... succesfully connected");			
			flst = conn.lsl();
//			Iterator<IFile> it = fls.iterator();
//			while (it.hasNext()) {
//				logger.info(it.next().toString());
//			}
			logger.info("Total: " + flst.size() + " files");			
			conn.close();
			logger.info("Connection closed on mirror " + Mirror_ID + " (" + Host + ")");
			
		} catch (IOException e) {
			logger.fatal(e.getMessage());
		}		
		return flst; 
	}
	
	
	private int executeStatement(Connection sqlConnection, String sqlStatement) {
		int result = 0;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = sqlConnection.createStatement();
			if (stmt.execute(sqlStatement)) {
				rs = stmt.getResultSet();
				logger.info(rs.getString(0));
			}
		} catch (SQLException e) {
			logger.fatal("SQLException: " + e.getMessage());
			logger.fatal("SQLState: " + e.getSQLState());
			logger.fatal("VendorError: " + e.getErrorCode());
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException sqlEx) {
					logger.fatal("SQLException: " + sqlEx.getMessage());
				}
				rs = null;
			}
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException sqlEx) {
					logger.fatal("SQLException: " + sqlEx.getMessage());
				}
				stmt = null;
			}
		}
		
		return result;
	}
	
	
	public int storeFileList() {
		// Prepare the date and time stamps 
		Formatter timeFormatter = new Formatter();
		timeFormatter.format("%tF",	timeStamp.getTime());
		String dateStampString = timeFormatter.toString();
		timeFormatter = new Formatter();
		timeFormatter.format("%tT",	timeStamp.getTime());
		String timeStampString = timeFormatter.toString();

		// Insert the file values into "LocalState" table
		if (null != fileList) {
			Iterator<IFile> it = fileList.iterator();
			while (it.hasNext()) {
				IFile currentFile = it.next();
				StringBuffer sqlStatement = new StringBuffer("insert into LocalState values (\"");			
				sqlStatement.append(Mirror_ID);
				sqlStatement.append("\", \"");
				sqlStatement.append(dateStampString);
				sqlStatement.append("\", \"");
				sqlStatement.append(timeStampString);
				sqlStatement.append("\", \"");
				sqlStatement.append(currentFile.getName());
				sqlStatement.append("\", \"");
				sqlStatement.append(currentFile.getSize());
				sqlStatement.append("\", \"");	
				// TODO now that date is available in standard datetime format (ISO8601), update DB schema 
				if (null != currentFile.getDate()) sqlStatement.append(currentFile.getDate().getTimeInMillis());
				else sqlStatement.append("null");
				sqlStatement.append("\" )");
				//	logger.info(sqlStatement);
				long i = executeStatement(jdbcConnection, sqlStatement.toString());
			}// while Iterator
		}
		// Insert the content summary into "Availability" table
		StringBuffer sqlStatement = new StringBuffer("insert into Availability values (\"");			
		sqlStatement.append(Mirror_ID);
		sqlStatement.append("\", \"");
		sqlStatement.append(dateStampString);
		sqlStatement.append("\", \"");
		sqlStatement.append(timeStampString);
		sqlStatement.append("\", \"");
		sqlStatement.append(isUp);
		sqlStatement.append("\", \"");
		sqlStatement.append(fileList.size());
		sqlStatement.append("\", \"");
		sqlStatement.append(getTotalSize());

		// Add here "No_Missing_Packages" !
		sqlStatement.append("\" , \"0\")");
//		logger.info(sqlStatement);
		long i = executeStatement(jdbcConnection, sqlStatement.toString());
		
		return 1;
	}
	
	public String getMirror_ID() {
		return Mirror_ID;
	}

	public String getContent() {
		return Content;
	}

	public String getProtocol() {
		return Protocol;
	}
	
	public String getHost() {
		return Host;
	}

	public String getPath() {
		return Path;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("[Mirror_ID]:");
		sb.append(Mirror_ID);		
		sb.append("::[Content]:" + Content);
		sb.append("::[Protocol]:" + Protocol);
		sb.append("::/" + Host + "/" + Path);
		return sb.toString();
	}
}
