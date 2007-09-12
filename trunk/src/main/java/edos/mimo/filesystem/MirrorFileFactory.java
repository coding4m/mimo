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
package edos.mimo.filesystem;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.Logger;
import org.dom4j.Element;

import edos.distribution.mirror.IFile;
import edos.distribution.mirror.MirrorDirectory;
import edos.distribution.mirror.MirrorFile;
import edos.distribution.mirror.MirrorFileFactory;
import edos.mimo.Date;
import edos.mimo.Shell;
import edos.mimo.exception.SSH2Exception;

public class MirrorFileFactory {
	private static Logger logger = Logger.getLogger(MirrorFileFactory.class);

	/**
	 * Parses a FTPFile object from the Apache Commons Net library.
	 * @param f
	 * @return
	 */
	public static IFile create(String path, FTPFile f) {
		IFile file;
		
		if(f.isDirectory())
			file = new MirrorDirectory(f.getName(), new Date(f.getTimestamp()), f.getSize(), path);
		else
			file = new MirrorFile(f.getName(), new Date(f.getTimestamp()), f.getSize(), path);
		
		return file;
	}

	/**
	 * Creates the file by individually setting up every attribute.
	 * @param name
	 * @param date
	 * @param size
	 * @param dir
	 * @return
	 */
	public static IFile create(String name, Date date, long size, String path, boolean dir) {
		IFile file;
		
		if(dir)
			file = new MirrorDirectory(name, date, size, path);
		else
			file = new MirrorFile(name, date, size, path);
		
		return file;
	}

	/**
	 * Parses a dom4j Element representing a file of a directory.
	 * @throws ParseException 
	 */
	public static IFile create(Element node, String path) throws ParseException {
		
		String name = node.attributeValue(IFile.NAME);
		String sDate = node.attributeValue(IFile.DATE);
		Date date;
		if(sDate == null) {
			logger.error("node without a date! " + name + " (" + node + ") ; using 'now'");
			date = new Date();
			
		}else{
			date = new Date(sDate);
		}
		
		long   size = Long.parseLong(node.attributeValue(IFile.SIZE));
		boolean dir = (node.getName().equals(MirrorFile.DIRECTORY))?true:false;
		
		return create(name, date, size, path, dir);
	}

	/**
	 * Parses a string received via SSH from a ls-l or another command
	 * @param ssh2RawListing
	 * @return IFile a file
	 */
	public static IFile create(String path, String ssh2RawListing, String command) 
										throws SSH2Exception {
		
		if(command.contains(Shell.COMMAND_LS_F)) {
			// the line contains a file name eventually followed by a '/' character
			// in lieu of a directory
			if(ssh2RawListing.endsWith("/")) {
				IFile file = new MirrorDirectory(ssh2RawListing.substring(0, ssh2RawListing.length()-1),
						MirrorFile.UNKNOWN_DATE, MirrorFile.UNKNOWN_SIZE, MirrorFile.UNKNOWN_PATH);
				return file;
			}
			
			// a regular file -not a directory
			IFile file = new MirrorFile(ssh2RawListing,
					MirrorFile.UNKNOWN_DATE, MirrorFile.UNKNOWN_SIZE, path);	
			return file;	
			
		}else if(command.contains(Shell.COMMAND_LS_L)){
			// this should not happen because the commands filters the header
			if(ssh2RawListing.startsWith("total"))
				throw new SSH2Exception("The ls -l output contains extra information");


			String[] result1 = ssh2RawListing.split("\\s");
			ArrayList<String> result = new ArrayList<String>(result1.length);
			// trimming the empty strings from the array
			for(int i=0; i<result1.length; i++) 
				if(!result1[i].equals(""))
					result.add(result1[i]);
			
			/*
			 * Date format
			 * dr-xr-xr-x  2 root root   8192 2005-09-26 13:08:01.000000000 -0400 Applications/
			 */
			String[] yearMonthDayPart  = result.get(5).split("-");
			int year = Integer.parseInt(yearMonthDayPart[0]);
			int month = Integer.parseInt(yearMonthDayPart[1]) - 1;
			int day = Integer.parseInt(yearMonthDayPart[2]);
			
			String[] hmsPart = result.get(6).split(":");
			int hour   = Integer.parseInt(hmsPart[0]);
			int minute = Integer.parseInt(hmsPart[1]);
			int second = (int)Double.parseDouble(hmsPart[2]);
			
			long size = Long.parseLong(result.get(4));
			String name = result.get(8);
			
		    Calendar cal = GregorianCalendar.getInstance();
		    cal.set(Calendar.DATE, day);
		    cal.set(Calendar.MONTH, month);
		    cal.set(Calendar.YEAR, year);
		    cal.set(Calendar.HOUR_OF_DAY, hour);
		    cal.set(Calendar.MINUTE, minute);
		    cal.set(Calendar.SECOND, second);
		    cal.set(Calendar.MILLISECOND, 0); // superfluous here
		    
		    
			if(ssh2RawListing.startsWith("d")) {
				if(name.endsWith("/"))
					name = name.substring(0, name.length() -1);
				IFile file = new MirrorDirectory(name, new Date(cal), size, path );
				return file;
			}
			
			IFile file = new MirrorFile(name, new Date(cal), size, path);
			return file;
			
		}else
			throw new SSH2Exception("Command " + command
					+ " undefined. Unable to parse content: " + ssh2RawListing);				
	}
}
