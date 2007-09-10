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

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

/**
 * This class is a wrapper around a source of mirror access' such as
 * http://www1.mandrivalinux.com/mirrorsfull.list and it provides a 
 * single place to put the retrieval and parsing code for many possible
 * alternate sources.
 * 
 * @author marc
 *
 */
public class MirrorAccessList {
	private static Logger logger = Logger.getLogger(MirrorAccessList.class);
	
	private LinkedList<Access> accesses;
	private HttpClient client;
	private String response = null;

	
	public MirrorAccessList(Access access) {
		
		// TODO add support for other protocols
		if(access.getProtocol().equals("http")) {
			String url = access.toString();
			
			// connect and download the list from http://www1.mandrivalinux.com/mirrorsfull.list
			client = new HttpClient();
	        client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
	        GetMethod get = new GetMethod(url);

	        try{
	            client.executeMethod(get);
	            response = get.getResponseBodyAsString();

	            if (response != null) 
	            	parse(response);
	            
	        }catch (Exception ex) {
	            ex.printStackTrace();
	            
	        }finally {
	            get.releaseConnection();
	        }
		}
	}
	
	/**
	 * Parsing a multi-line string where each line looks like this:
	 * communityi586:ftp://ftp.gwdg.de/pub/linux/mandrakelinux/devel/community/i586/media/main
	 * 
	 * a family type, a colon, then a url
	 * 
	 * @param response
	 */
	private void parse(String response) {
		String[] lines = response.split("\n");
		if(lines.length == 0)
			return;	// just one line, something is wrong...
		
		accesses = new LinkedList<Access>();
		
		for(int i = 0; i < lines.length ; i++) {
			String[] tokens = lines[i].split(":", 3);
			
			if(tokens.length < 2) {
				// lines are not in the expected format (see below)
				accesses = null;
				return;
			}
			
			// tokens[0] contains some mirror family type, eg. updatesx86_64 or cookeri586
			// TODO add suport for these family types
			
			// tokens[1] contains the protocol
			String protocol = tokens[1];

			// tokens[2] contains the rest
			String rest = tokens[2].substring(2);	// cutting the leading '//'
			
			// now, a rsync url may contain other colons within, while ftp and http do not
			// 2 possibilities:
			//		i) a '::' exists after the host
			//		ii) a '/' exists instead therefore we can treat it as a 'standard url' such as ftp, or http
			String[] temp;
			if(protocol.equals("rsync") && rest.contains("::")) {
				temp = rest.split("::");	// cutting from '::'
				
			}else{
				temp = rest.split("/", 2);		// cutting from the leading / after the host part
			}
			
			String host = temp[0];
			String path = "/" + temp[1];
			
			logger.info(host + "  " + path + "\n");
			
			// instantiating the mirror
			Access access = new Access(host, protocol, path);
			accesses.add(access);
			
		}
		
	}
	
	public List<Access> get() {
		return accesses;
	}
	
	/**
	 * The access list as obtained from the server
	 * @return the access list as-is if received or null - String
	 */
	public String getRawList() {
		return response;
	}
	
	/**
	 * Print the access list as-is
	 * @param out - PrintWriter
	 */
	public void print(PrintWriter out) {
		out.print(response);
	}
}
