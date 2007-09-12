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

import edos.distribution.mirror.IDiffGenerator;
import edos.mimo.Config;
import edos.mimo.IMasterMirror;
import edos.mimo.ISecondaryMirror;
import edos.mimo.dom.DOMDiffGenerator;

/**
 * Coping with many possible modelizations for the file system,
 * this factory returns the adapted DiffGenerator for the choosen
 * model.
 * 
 * @author marc
 *
 */
public class DiffGeneratorFactory {

	/**
	 * Generate a concrete implementation based on the 
	 * choosen implementation model
	 * @throws ParseException 
	 */
	public static IDiffGenerator getDiffGenerator(int type, IMasterMirror master, ISecondaryMirror mirror) throws ParseException {
		IDiffGenerator gen = null;
		
		if(type == Config.TREE) {
			gen =  new DOMDiffGenerator(master, mirror);
			
		}else if(type == Config.TABLE) {
			// add your own implementation here
			// eg. table-based adapted to RDBMS
		}
		
		return gen;
	}

}
