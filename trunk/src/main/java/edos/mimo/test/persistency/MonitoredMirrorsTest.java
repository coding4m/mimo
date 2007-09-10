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
package edos.mimo.test.persistency;

import java.util.List;


import edos.mimo.Config;
import edos.mimo.IMirror;
import edos.mimo.MonitoredMirrors;
import junit.framework.TestCase;

public class MonitoredMirrorsTest extends TestCase {
	Config config;
	List<IMirror> mirrorList = null;
	MonitoredMirrors monitoredMirrors = null;
	
	protected void setUp() throws Exception {
		config  = Config.getInstance();
		
		super.setUp();
	}

	protected void tearDown() throws Exception {
		config = null;
		
		super.tearDown();
	}
	
	public void testInstantiateMonitoredMirrors() {
		assertNotNull(MonitoredMirrors.getInstance());
	}
	
	public void testAddMirror() throws Exception {
		mirrorList = config.getSecondaryMirrors();
		IMirror m = mirrorList.get(0);
		assertNotNull(m);
		m.connect();
		m.disconnect();
		
		monitoredMirrors = MonitoredMirrors.getInstance();
		monitoredMirrors.add(m);
		assertNotNull(monitoredMirrors.toString());
	}
	
	
}
