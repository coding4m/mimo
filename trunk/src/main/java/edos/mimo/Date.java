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

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Locale;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Date {
	private static Logger logger = Logger.getLogger(edos.mimo.Date.class);
	
	private Calendar cal;
	//private ISO8601DateFormat dateFormatter = new ISO8601DateFormat();	does not respect the standard
	private SimpleDateFormat  dateFormatter = new SimpleDateFormat(ISO8601_PATTERN, Locale.FRANCE);
	
	// types of String representation existing out there
	public static final int LONG 		= 0;
	public static final int SHORT 		= 1;
	public static final int ISO8601 	= 2;
	
	public static final String ISO8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.S";
	
	// for conversion between Date and Calendar
	private static Hashtable<String, Integer> monthsHashTable = new Hashtable<String, Integer>();
	static {
		monthsHashTable.put("Jan", new Integer(Calendar.JANUARY));
		monthsHashTable.put("Feb", new Integer(Calendar.FEBRUARY));
		monthsHashTable.put("Mar", new Integer(Calendar.MARCH));
		monthsHashTable.put("Apr", new Integer(Calendar.APRIL));
		monthsHashTable.put("May", new Integer(Calendar.MAY));
		monthsHashTable.put("Jun", new Integer(Calendar.JUNE));
		monthsHashTable.put("Jul", new Integer(Calendar.JULY));
		monthsHashTable.put("Aug", new Integer(Calendar.AUGUST));
		monthsHashTable.put("Sep", new Integer(Calendar.SEPTEMBER));
		monthsHashTable.put("Oct", new Integer(Calendar.OCTOBER));
		monthsHashTable.put("Nov", new Integer(Calendar.NOVEMBER));
		monthsHashTable.put("Dec", new Integer(Calendar.DECEMBER));
	}

	/**
	 * Creates a date representing this moment (now)
	 *
	 */
	public Date() {
		cal = GregorianCalendar.getInstance();
	}
	
	/**
	 * Creates the date with the same value as the given <code>Calendar</code>
	 * 
	 * @param a calendar
	 */
	public Date(Calendar cal) {
		this.cal = cal;
	}
	
	/**
	 * Creates the date from a number of milliseconds since Jan 1, 1970
	 * @param timestamp
	 */
	public Date (long timestamp) {
		cal = new GregorianCalendar();
		cal.setTimeInMillis(timestamp);
	}
	
	/**
	 * Creates a date from a string representation taking into account
	 * the possibility of diverse types
	 * 
	 * @param sDate
	 * @param type
	 * @throws ParseException 
	 */
	public Date(String sDate, int type) throws ParseException {
		switch(type) {
		case LONG: 		parseLongDate(sDate); break;
		case ISO8601: 	parseISO8601Date(sDate); break;
		default:		parseShortDate(sDate); break;
		}
	}
	
	/**
	 * Creates a date from a String value in the same format as the one
	 * produced by java.util.Date.toString()
	 * 
	 * @param the data as a <code>String</code>
	 * @throws ParseException 
	 */
	public Date(String sDate) throws ParseException {
		// TODO nice to have: code to auto-distinguish which type of format it is
		parseISO8601Date(sDate);	// default is now ISO8601
	}
	
	private void parseISO8601Date(String sDate) {
		//logger.setLevel(Level.DEBUG);
		logger.debug("parsing " + sDate);
		
		cal = new GregorianCalendar();
		cal.setTimeInMillis(dateFormatter.parse(sDate,new ParsePosition(0)).getTime());		
	}
	
	/**
	 * Creates a date from a String value such as:
	 * 	03-Oct-2005 10:09
	 * 
	 * @param sDate
	 * @throws ParseException 
	 */
	private void parseShortDate(String sDate) throws ParseException {	
		try {
			String[] parts = sDate.split("\\s");	// split around white space
			String datePart = parts[0];
			String timePart = parts[1];
				
			String[] dmyParts = datePart.split("-");
			int year = Integer.parseInt(dmyParts[2]);
			int month = monthsHashTable.get(dmyParts[1]) ;
			int dayOfMonth = Integer.parseInt(dmyParts[0]);
			
			String[] timeParts = timePart.split(":");				
			int hourOfDay = Integer.parseInt(timeParts[0]);
			int minute = Integer.parseInt(timeParts[1]);
			cal = new GregorianCalendar(year, month, dayOfMonth, hourOfDay, minute);
		
		}catch(Exception e) {
			throw new ParseException(e.getMessage(), 0);
		}
	}
	
	
	/**
	 * Creates a date from a String value in the same format as the one
	 * produced by java.util.Date.toString()
	 * 
	 * ex: Mon Sep 26 13:08:01 EDT 2005
	 * 
	 * @param the data as a <code>String</code>
	 * @throws ParseException 
	 */	
	private void parseLongDate(String sDate) throws ParseException {
		/*
		 * a date looks like this (as in Date.toString()):
		 *  Mon Sep 26 13:08:01 EDT 2005
		 * 
		 * From Javadoc of java.util.Date
		 * ------------------------------
		 * Converts this Date object to a String of the form: dow mon dd
		 * hh:mm:ss zzz yyyy where: dow is the day of the week (Sun, Mon, Tue,
		 * Wed, Thu, Fri, Sat). mon is the month (Jan, Feb, Mar, Apr, May, Jun,
		 * Jul, Aug, Sep, Oct, Nov, Dec). dd is the day of the month (01 through
		 * 31), as two decimal digits. hh is the hour of the day (00 through
		 * 23), as two decimal digits. mm is the minute within the hour (00
		 * through 59), as two decimal digits. ss is the second within the
		 * minute (00 through 61, as two decimal digits. zzz is the time zone
		 * (and may reflect daylight saving time). Standard time zone
		 * abbreviations include those recognized by the method parse. If time
		 * zone information is not available, then zzz is empty - that is, it
		 * consists of no characters at all. yyyy is the year, as four decimal
		 * digits.
		 */
		try {
			String[] datePart = sDate.split("\\s");
			cal = GregorianCalendar.getInstance();
			cal.set(Calendar.YEAR, Integer.parseInt(datePart[5]));
			cal.set(Calendar.MONTH, monthsHashTable.get(datePart[1]));
			cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(datePart[2]));
			
			String[] timePart = datePart[3].split(":");
			cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timePart[0]));
			cal.set(Calendar.MINUTE, Integer.parseInt(timePart[1]));
			cal.set(Calendar.SECOND, Integer.parseInt(timePart[2]));
			cal.set(Calendar.MILLISECOND, 0); // superfluous here
		}catch(Exception e) {
			throw new ParseException(e.getMessage(), 0);
		}
		
	}
	
	public String toString() {
		return cal.getTime().toString();
	}
	
	public Calendar getCalendar() {
		return cal;
	}
	
	public long getTimeInMillis() {
		return cal.getTimeInMillis();
	}
	
	public boolean equals(Date d) {
		return cal.equals(d);
	}
	
	public boolean greaterThan(Date d) {
		return (cal.getTimeInMillis()>d.getCalendar().getTimeInMillis())?true:false;
	}
	
	public String asISO8601() {
		//StringBuffer sb = new StringBuffer();
		//return dateFormatter.format(new java.util.Date(cal.getTimeInMillis()), sb, new FieldPosition(java.text.DateFormat.DATE_FIELD)).toString();
		return dateFormatter.format(new java.util.Date(cal.getTimeInMillis()));
	}
}
