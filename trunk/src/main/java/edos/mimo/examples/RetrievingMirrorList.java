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
package edos.mimo.examples;

import java.util.ArrayList;
import java.util.List;

import edos.mimo.Access;
import edos.mimo.Config;
import edos.mimo.MirrorAccessList;
import edos.mimo.exception.ConfigException;


public class RetrievingMirrorList {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = null;
		MirrorAccessList mirrorList = null;
		List<Access> mirrors = null;

		try {
			// read the default configuration file (a File or a String can also be passed)
			config = Config.getInstance();
			
			// list of target urls where to retrieve a mirror list
			ArrayList<Access> targets = config.getMirrorAccessList();
			
			// try the targets one by one until finding a working one
			for(int i = 0; i<targets.size(); i++) {
				
				Access a = targets.get(i);
				mirrorList = new MirrorAccessList(a);
				mirrors = mirrorList.get();
				
				if(mirrors != null)
					break;		// one copy retrieved is enough
			}
			
			if(mirrors != null) {
				System.out.println(mirrorList.getRawList());
				System.out.println("Successfully acquired and instantiated the list of mirrors");
			}
			
		} catch (ConfigException cfge) {
			System.out.println(cfge.getMessage());

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}


	}

}
