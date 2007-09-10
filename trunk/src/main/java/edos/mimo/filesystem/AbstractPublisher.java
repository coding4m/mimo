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
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

// TODO update the commments (methods originally in MirrorStructure(!))
@Deprecated
// not used
public abstract class AbstractPublisher extends Thread implements IPublisher {
	protected String filename = null;
	protected Document doc;
	

	public String getFilename() {
		return this.getFilename();
	}
	
	protected abstract Document createDom4jDoc();
	
	
	/**
	 * Save the <code>IMirrorStructure</code> to a file named after the mirror name:
	 * <mirror name>-complete-filesystem-structure.xml
	 * 
	 * @throws IOException
	 */
	public void save() throws IOException {
		// TODO for production disable XML pretty print
		save(true);	// "pretty print" by default: it is much more efficient when loading in editor
					// probably less efficient for production
	}
	
	/**
	 * Save the <code>IMirrorStructure</code> to a file named after the mirror name:
	 * <mirror name>-complete-filesystem-structure.xml
	 *
	 * There is an option to get a "pretty print" which is obtained by an automated
	 * insertion of blank spaces at the cost of processing power.
	 * (A multi-line document is however easier to open within an editor!)
	 * 
	 * @throws IOException
	 * @param mirrorName String the name of the mirror
	 */
	public void save(boolean pretty) throws IOException {
		PrintWriter outG = new PrintWriter( getFilename() );
		if(pretty)
			serializetoPrettyPrintXML(outG);
		else
			serializetoXML(outG); // output to file
	}
	
	/**
	 * Print the XML representation of the <code>IMirrorStructure</code>
	 * to the <code>OutputStream</code>.
	 * @param out
	 * @throws Exception
	 */
	private void serializetoXML(OutputStream out) throws Exception {
		XMLWriter writer = new XMLWriter(out);
		writer.write(this.doc);
		writer.flush();
	}
	
	/**
	 * Print the XML representation of the <code>IMirrorStructure</code>
	 * to the <code>OutputStream</code>.
	 * @param out
	 * @throws Exception
	 */
	private void serializetoXML(Writer out) throws IOException {
		XMLWriter writer = new XMLWriter(out);
		writer.write(this.doc);
		writer.flush();
	}
	
	/**
	 * Print the XML representation of the <code>IMirrorStructure</code>
	 * to the <code>OutputStream</code>. Uses pretty print format (end of line, etc.)
	 * @param out
	 * @throws Exception
	 */
	private void serializetoPrettyPrintXML(OutputStream out) throws IOException {
		OutputFormat outformat = OutputFormat.createPrettyPrint();
		XMLWriter writer = new XMLWriter(out, outformat);
		writer.write(this.doc);
		writer.flush();
	}
	
	/**
	 * Print the XML representation of the <code>IMirrorStructure</code>
	 * to the <code>OutputStream</code>. Uses pretty print format (end of line, etc.)
	 * @param out
	 * @throws Exception
	 */
	private void serializetoPrettyPrintXML(Writer out) throws IOException {
		OutputFormat outformat = OutputFormat.createPrettyPrint();
		XMLWriter writer = new XMLWriter(out, outformat);
		writer.write(this.doc);
		writer.flush();
	}

	/**
	 * Returns an XML String.
	 */
	public String toString() {
		return doc.asXML();
	}

}
