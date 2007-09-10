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

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;

public class MirrorDiffPublisher extends AbstractPublisher implements IPublisher {
	private IMirrorDiff mirrorDiff;
	
	public MirrorDiffPublisher(IMirrorDiff diff) {
		this.mirrorDiff = diff;
		super.doc = createDom4jDoc();
		super.filename = diff.getFileName();
	}
	
	@Override
	public void run() {
		try {
			save();
			generateHTMLReport();
			
		}catch(IOException ioe) {
			// call back, log and react
		}
	}

	protected Document createDom4jDoc() {
		/* TODO not necessary for DOM storage implementation
		Document doc = DocumentFactory.getInstance().createDocument();
		Element root = doc.addElement("mirrordiff");
		List<IFileDiff> diffs = mirrorDiff.getFileDiffs();
		Iterator<IFileDiff> it = diffs.iterator();
		while(it.hasNext()) {
			IFileDiff diff = it.next();
			Element delta = root.addElement("delta");
		}
		*/
		return null;
	}

	public void generateHTMLReport() throws IOException {
		// TODO implement HTML report generation
		throw new IOException("Method not implemented");
	}


}
