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

/**
 * This class provides a complete set to successfully login to a server.
 */
public class Pass implements IPass {
	private Access access;
	private Identity id;
	private boolean active;

	public Pass(Access a, Identity i) {
		access = a;
		id = i;
	}
	
	public Access getAccess() {
		return access;
	}

	public Identity getIdentity() {
		return id;
	}
	
	/**
	 * 
	 * @return true if this object is currently used by an active connection
	 */
	public boolean isActive()  {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	
	/**
	 * Human readable information about this pass
	 */
	public String toString() {
		return access.toString() + " " + id.toString();
	}
}
