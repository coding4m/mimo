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
package edos.mimo.test;


import java.text.ParseException;

import edos.mimo.Date;
import junit.framework.TestCase;

public class DateTest extends TestCase {
	
	public void testDateCreation() throws InterruptedException {
		Date d1 = new Date();
		
		Thread.sleep(2000);
		
		Date d2 = new Date();
		
		assertNotSame(d1.asISO8601(), d2.asISO8601());
		assertNotSame(d1, d2);
		assertNotSame(d1.toString(), d2.toString());
		assertFalse(d1.toString().equals(d2.toString()));
		assertFalse(d1.asISO8601().equals(d2.asISO8601()));
	}

	public void testDateParsing() throws ParseException {
		Date d1 = new Date("2006-10-28T03:08:27.978", Date.ISO8601);
		
		assertEquals(1162019307978l, d1.getTimeInMillis());
	}
}
