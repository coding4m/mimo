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

public class AnalysisLevel {

	private String depth;
	private String verbosity;
	
	// depth settings
	public static final String SKELETON = "skeleton";
	public static final String COMPLETE = "complete";
	
	// verbosity settings
	public static final String QUIET	= "quiet";
	public static final String VERBOSE	= "verbose";
	
	
	public static final String DEFAULT_VERBOSITY 	= VERBOSE;
	public static final String DEFAULT_DEPTH		= COMPLETE;
	
	/**
	 * Defines the default settings.
	 *
	 */
	public AnalysisLevel() {
		depth = DEFAULT_DEPTH;
		verbosity = DEFAULT_VERBOSITY;
	}
	
	public AnalysisLevel(String depth, String verbosity) {
		this.depth = depth;
		this.verbosity = verbosity;
	}
	
	public String getDepth() {
		return depth;
	}
	
	public String getVerbosity() {
		return verbosity;
	}
}
