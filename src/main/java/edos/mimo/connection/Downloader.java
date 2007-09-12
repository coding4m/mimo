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
package edos.mimo.connection;

import java.io.IOException;
import java.text.ParseException;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import edos.distribution.mirror.Downloader;
import edos.distribution.mirror.IDownloaderListener;
import edos.mimo.IMirror;
import edos.mimo.dom.ITreeMirrorStructure;
import edos.mimo.dom.db.BDBXMLManager;


/**
 * This class represents a single thread connecting to a given mirror
 * for synchronization (status).
 * 
 * @deprecated use DownloaderJob instead (Quartz scheduled)
 * 
 * @author marc
 *
 */
public class Downloader extends Thread {
	private static Logger logger = Logger.getLogger(Downloader.class);
	
	private IMirror mirror;					// holds a reference to the current mirror
	private ITreeMirrorStructure mirrorStructure;	// holds a reference to the internal representation of this mirror
	private boolean saved = false;
	List<IDownloaderListener> listeners;
	
	public Downloader(IMirror mirror) {
		super("Downloader for " + mirror);

		this.mirror = mirror;
		listeners = new LinkedList<IDownloaderListener>();
	}
	
	public void addListener(IDownloaderListener listener) {
		if(listener == null)
			return;
		listeners.add(listener);
	}
	
	/**
	 * Broadcast the status "download complete" to all registered
	 * listeners.
	 *
	 */
	private void notifyListeners() {
		Iterator<IDownloaderListener> it = listeners.iterator();
		while(it.hasNext()) {
			try {
				it.next().downloadComplete(this);
			} catch (ParseException e) {
				logger.fatal("date parsing has failed : " + e.getMessage());
			}
		}
	}
	
	/**
	 * The downloader connects to the mirror, crawls in
	 * and returns a custom structure IMirrorStructure
	 * with is a custom tree built on top of the common
	 * filesystem classes.
	 * 
	 * @see edos.mimo.dom.ITreeMirrorStructure
	 * 
	 * Override Thread.run()
	 */
	public void run() {
		logger.info("starting download: " + mirror.getName() 
				+ " ("+ mirror.getActivePass().getAccess() + ")");
		
		try {
			// connection and processing
			mirror.connect();
			
			mirror.setCheckinTime(GregorianCalendar.getInstance().getTimeInMillis() );
			mirror.acquire();
			mirror.setCheckoutTime(GregorianCalendar.getInstance().getTimeInMillis());

			// data manipulation, reports using a custom tree structure
			mirrorStructure = (ITreeMirrorStructure)mirror.getStructure(); 
			
			// uncomment the next line to get a (huge!) string on the console (it crashes Eclipse)
			//System.out.println("XML representation: " + mirrorStructure.asXML());
			
			// persistence to filesystem ; uncomment this to save as file.xml
			// (expected size ~2MB; expected time ~1min/MB to write the file to disk)
			//mirrorStructure.save();
			//saved = true;
			
			// TODO try saving from here (transactional)
			BDBXMLManager manager = BDBXMLManager.getInstance();
			manager.save(mirrorStructure);
			saved = true;
			
			logger.info("Completed download: " + mirror.getName() 
					+ " ("+ mirror.getActivePass().getAccess() + ")"
					+ " in " + (mirror.getDownloadDelayInMillis() / 1000.0)
					+ " milliseconds");
			
			// fire the next stage
			notifyListeners();
			logger.info("listeners were notified");
			
		} catch (IOException ioe) {
			logger.error(ioe.getMessage());

		} catch (Exception e) {
			logger.error(e.getMessage());

		} finally {
			if (mirror != null)
				mirror.disconnect();
		}
	}
	
	public ITreeMirrorStructure getStructure() {
		return mirrorStructure;
	}
	
	public boolean isSaved() {
		return saved;
	}
	
	public IMirror getMirror() {
		return mirror;
	}
}
