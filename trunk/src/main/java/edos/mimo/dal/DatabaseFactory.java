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

public abstract class DatabaseFactory {
	public static final int XML_DATABASE = 1;
	public static final int RELATIONAL_DATABASE = 2;	// JDBC connection to MySQL database
	public static final int FILESYSTEM_DATABASE = 3;	// for testing only

	public static IDatabase getDatabase(int type) {
		IDatabase db = null;
		
		if(type == XML_DATABASE) {
			db = DatabaseXML.getInstance();
			
		}else if(type == RELATIONAL_DATABASE) {
			db = DatabaseJDBC.getInstance();
			
		}else if(type == FILESYSTEM_DATABASE) {
			// TODO complete filesystem implementation (if needed)
			
		}
		
		return db;
	}
}
