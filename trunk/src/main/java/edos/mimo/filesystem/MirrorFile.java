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

import org.apache.log4j.Logger;
import org.dom4j.Element;

import edos.distribution.mirror.IFile;
import edos.distribution.mirror.MirrorFile;
import edos.mimo.Date;

/**
 * Implementation built around a dom4j Element.
 * 
 */
public class MirrorFile implements IFile {
	private static Logger logger = Logger.getLogger(MirrorFile.class);
	
	private String name;
	private Date date;
	private long size;
	private String path;
	
	public MirrorFile(String name, Date date, long size, String path) {
		this.name = name;
		this.size = size;
		this.date = date;
		this.path = path;
	}

	public MirrorFile(String name) {		
		this.name = name;	// Fixed by Radu
		date = UNKNOWN_DATE;
		size = UNKNOWN_SIZE;
		path = UNKNOWN_PATH;
	}

	public MirrorFile(Element e) {
		// TODO implement path
		name = e.attributeValue(MirrorFile.NAME);		
		try{
			date = new Date(e.attributeValue(MirrorFile.DATE));
		}catch(ParseException pe) {
			logger.warn("Unable to parse date; using 'now' instead - " + pe.getMessage());
			date = new Date();
		}
		size = Integer.parseInt(e.attributeValue(MirrorFile.SIZE));
	}

	public String getName() {
		return name;
	}

	public Date getDate() {
		return date;
	}

	public long getSize() {
		return size;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		// TODO fix the time zone issue: gets one hour less than time posted on
		// the ftp site
		sb.append(name).append(" [").append(date.asISO8601())
				/* .append(date.getTimeZone()) */.append(" ] - ").append(size)
				.append(" bytes");
		return sb.toString();
	}

	public String getPath() {
		return path;
	}
}
