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

import java.util.Calendar;
import java.util.GregorianCalendar;

import edos.mimo.IMirror;

/**
 * This class stores information about the status
 * of a particular mirror download.
 * 
 * @author marc
 *
 */
public class MirrorDownloadStatus {
	private IMirror mirror;
	private boolean active = false;
	private Calendar startTime;
	private Calendar endTime;

	public MirrorDownloadStatus(IMirror m) {
		mirror = m;
	}

	public void downloadStarts() {
		active = true;
		startTime = new GregorianCalendar();
	}
	
	public void downloadEnds() {
		active = false;
		endTime = new GregorianCalendar();
	}
	
	public boolean isActive() {
		return active;
	}

	public Calendar getEndTime() {
		return endTime;
	}

	public IMirror getMirror() {
		return mirror;
	}

	public Calendar getStartTime() {
		return startTime;
	}

	/**
	 * Tells how much time the downloader has been busy
	 * working on this mirror
	 * 
	 * @return time in minutes
	 */
	public long getWorkingTimeInMinutes() {
		long milliseconds = GregorianCalendar.getInstance().getTimeInMillis()
								- startTime.getTimeInMillis();
		
		return milliseconds / 1000 / 60;
	}
	
	public float getWorkingTimeInDays() {
		return ((float)getWorkingTimeInMinutes()) / 60 / 24;
	}
	
}
