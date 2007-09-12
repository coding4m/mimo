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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.DocumentException;


public interface IMirrorFactory {

	//public IMasterMirror getMaster(String name, List<IPass> passes);
	//public IMasterMirror getMaster(String name, List<Access> accessList, List<Identity> defaultIds);
	public IMasterMirror getMaster(String name, Access a, Identity id);
	public IMasterMirror getMaster(String mirrorName, ArrayList<Access> accessList, ArrayList<Identity> ids,
			AnalysisLevel analysis, String cronExpression);
	
	
	public ISecondaryMirror getSecondary(String name, List<IPass>passes);
	//public ISecondaryMirror getSecondary(String name, List<Access> accessList, List<Identity> defaultIds);
	public ISecondaryMirror getSecondary(String name, IPass p);
	public ISecondaryMirror getSecondary(String name, Access a, Identity id);
	
	public IMirror getMirror(File file) throws DocumentException;
}
