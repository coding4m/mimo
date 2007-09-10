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

import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.Scanner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import edos.distribution.mirror.CLIBrowsing;
import edos.mimo.Access;
import edos.mimo.ISecondaryMirror;
import edos.mimo.Identity;
import edos.mimo.MirrorFactory;
import edos.mimo.dom.ITreeMirrorStructure;


/**
 * This class is called simple browsing but it offers 2 features: - recursive
 * navigation of the file system to model this as a directory tree - same as
 * above but filling the tree with the non-directory files
 * 
 * In this example the "Access" chosen is via FTP, but it should work the same
 * way just by changing the "Access" instantiation with another protocol such as
 * SSH2 (and later on other ones).
 * 
 * @author marc
 * 
 */
public class CLIBrowsing {
	static Logger logger = Logger.getLogger(CLIBrowsing.class);
	
	/*
	private static final String DEFAULT_HOST = "testmachine";
	private static final String DEFAULT_PATH = "/cooker";
	private static final String DEFAULT_PROTOCOL = "ssh";
	private static final String DEFAULT_USERNAME = "mmonitor";
	private static final String DEFAULT_KEYFILE  = "mmonitor@mandriva.com_dsa";
	private static final String DEFAULT_PASSWD = "Ican'ttellyou";
	private static final String DEFAULT_REALNAME = "Kenobi";
	*/
	private static final String DEFAULT_HOST = "ftp.proxad.fr";
	private static final String DEFAULT_PATH = "/pub/Distributions_Linux/MandrivaLinux/devel/2007.0/i586";
	private static final String DEFAULT_PROTOCOL = "ftp";
	private static final String DEFAULT_USERNAME = "anonymous";
	private static final String DEFAULT_KEYFILE  = "mmonitor@mandriva.com_dsa";
	private static final String DEFAULT_PASSWD = "mmonitor@edos-project.org";
	private static final String DEFAULT_REALNAME = "ftp.proxad.fr";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ISecondaryMirror mirror = null;
		MirrorFactory mirrorFactory = MirrorFactory.getInstance();
		String host, path, protocol;			// for Access definition
		String name, passwd, keyFile, realName;	// for Identity definition
		name = passwd = keyFile = realName = null;
		
		BasicConfigurator.configure();	// level DEBUG by default
		
		
		/*
		 * Asking the user for a mirror to browse
		 */
		Scanner sc = new Scanner(System.in);
		System.out.println("Please enter your configuration");
		System.out.println("\thost [" + DEFAULT_HOST + "]: ");
		host = sc.nextLine();
		if(host.equals(""))
			host = DEFAULT_HOST;
		
		System.out.println("\tpath [" + DEFAULT_PATH + "]: ");
		path = sc.nextLine();
		if(path.equals(""))
			path = DEFAULT_PATH;
		
		System.out.println("\tprotocol [" + DEFAULT_PROTOCOL + "]: ");
		protocol = sc.nextLine();
		if(protocol.equals(""))
			protocol = DEFAULT_PROTOCOL;
		
		// instantiation
		//Access myAccess = new Access("ganimedes", "ftp", "/cooker");
		Access myAccess = new Access(host, protocol, path);
		
		/*
		 * Asking the user for credentials
		 */
		if(protocol.equals("ftp") || protocol.equals("ssh")) {
			System.out.println("username for " + host + " [" + DEFAULT_USERNAME + "]:");
			name = sc.nextLine();
			
			System.out.println("password for " + host + " [" + DEFAULT_PASSWD + "]:");
			passwd = sc.nextLine();

			if(protocol.equals("ssh")) {
				System.out.println("key file path for " + host + " [" + DEFAULT_KEYFILE + "]:");
				keyFile = sc.nextLine();
			}
		}
			
		
		System.out.println("and your full name is [" + DEFAULT_REALNAME + "]:");
		realName = sc.nextLine();
		if(realName.equals(""))	realName = DEFAULT_REALNAME;
		

		if(name == null || name.equals(""))		name = DEFAULT_USERNAME;
		if(passwd == null || passwd.equals(""))	passwd = DEFAULT_PASSWD;
		if(keyFile == null || keyFile.equals(""))	keyFile = DEFAULT_KEYFILE;
		Identity id = new Identity(name, passwd, keyFile, realName); 
		
		
		/*
		 * Starting downloading/browse info
		 */
		mirror = mirrorFactory.getSecondary("User-defined site", myAccess, id);
		
		// see this function to get a tree of directories
		System.out.println("Acquiring a directory skeleton of " + mirror);
		getDirectoryStructure(mirror);

		/* @deprecated
		// see this function to get a complete model of the mirror file system
		System.out.println("Acquiring a complete representation of " + mirror);
		getCompleteFileSystemStructure(mirror);
		*/

		System.out.println("Exited successfully");

		if (mirror != null)
			mirror.disconnect();

	}

	private static void getDirectoryStructure(ISecondaryMirror mirror) {
		try {
			// connection and work
			mirror.connect();
			mirror.setCheckinTime(GregorianCalendar.getInstance().getTimeInMillis());
			mirror.acquireSkeleton();
			mirror.setCheckoutTime(GregorianCalendar.getInstance().getTimeInMillis());
			System.out.println("Download completed in "
					+ mirror.getDownloadDelayInMillis() / 1000.0 
					+ "seconds.");

			// data manipulation, reports
			ITreeMirrorStructure structure = (ITreeMirrorStructure)mirror.getStructure();
			structure.save();
			
			System.out.println("-> directory structure saved as " + structure.getFilename());
			System.out.println("");

		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());

		} catch (Exception e) {
			System.out.println(e.getMessage());

		} finally {
			if (mirror != null)
				mirror.disconnect();
		}
	}

	/* @deprecated
	private static void getCompleteFileSystemStructure(ISecondaryMirror mirror) {
		try {
			// connection and work
			mirror.connect();
			mirror.setCheckinTime(GregorianCalendar.getInstance().getTimeInMillis());
			mirror.acquireStructure();
			mirror.setCheckoutTime(GregorianCalendar.getInstance().getTimeInMillis());
			System.out.println("Download completed in "
					+ mirror.getDownloadDelayInMillis() / 1000.0 
					+ "seconds.");

			// data manipulation, reports
			ITreeMirrorStructure structure = (ITreeMirrorStructure)mirror.getStructure();
			
			//System.out.println("XML representation: ");
			//System.out.println(structure); // String to big for X server!
			
			structure.save(); // output to file (~1MB)
			
			System.out.println("-> complete filesystem structure saved as " + structure.getFilename());
			System.out.println("");
			
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());

		} catch (Exception e) {
			System.out.println(e.getMessage());

		} finally {
			if (mirror != null)
				mirror.disconnect();
		}
	}
	*/
}
