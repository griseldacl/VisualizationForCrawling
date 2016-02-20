/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2005 Phex Development Group
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 *  --- CVS Information ---
 *  $Id: GWebCacheContainer.java,v 1.18 2005/11/03 16:33:47 gregork Exp $
 *  File Modified
 */

/**
 * PeerCrawl - Distributed P2P web crawler based on Gnutella Protocol
 * @version 2.0
 * 
 * Developed as part of Masters Project - Spring 2006
 * @author 	Vaibhav Padliya
 * 			College of Computing
 * 			Georgia Tech
 * 
 * @contributor Mudhakar Srivatsa
 * @contributor Mahesh Palekar
 */
package phex.gwebcache;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import phex.common.GeneralGnutellaNetwork;
import phex.common.ThreadPool;
import phex.common.address.AddressUtils;
import phex.common.address.DestAddress;
import phex.connection.NetworkManager;
import phex.connection.ProtocolNotSupportedException;
import phex.host.CaughtHostsContainer;
import phex.host.HostManager;
import phex.utils.*;

public class GWebCacheContainer {
	private static int MIN_G_WEB_CACHES_SIZE = 5;

	private static int MAX_G_WEB_CACHES_SIZE = 1000;

	private static List BLOCKED_WEB_CACHES;

	private static List PHEX_WEB_CACHES;
	static {
		// This is a list of Phex only GWebCaches. Requests from other hosts are
		// ignored and return with ERROR.
		String[] arr = {
		// "http://gc-phex02.draketo.de/gcache.php",
		// "http://phexgwc.kouk.de/gcache.php"
		};
		PHEX_WEB_CACHES = Collections.unmodifiableList(Arrays.asList(arr));

		String[] blockedArr = { "gavinroy.com" };
		BLOCKED_WEB_CACHES = Collections.unmodifiableList(Arrays
				.asList(blockedArr));
	}

	/**
	 * Contains GWebCache objects.
	 */
	private ArrayList allGWebCaches;

	/**
	 * Contains GWebCache objects.
	 */
	private ArrayList functionalGWebCaches;

	/**
	 * Contains Phex GWebCaches only. Locking is done through allGWebCaches.
	 */
	private ArrayList phexGWebCaches;

	/**
	 * Stores the URL.toExternalForm().toLowerCase(). The URL object itself is
	 * not stored since the hash function of the URL is way slow, it could lead
	 * to doing a IP lookup. The lower case convesion is done to keep us from
	 * adding multiple equal URLs of the same file, like they are often seen.
	 */
	private Set uniqueGWebCacheURLs;

	/**
	 * GWebCaches sorted by access order the last accesed GWebCaches first.
	 */
	private TreeSet sortedGWebCaches;

	private Random random;

	public GWebCacheContainer() {
		NLogger
				.debug(NLoggerNames.GWEBCACHE,
						"Initializing GWebCacheContainer");
		allGWebCaches = new ArrayList();
		phexGWebCaches = new ArrayList();
		functionalGWebCaches = new ArrayList();
		uniqueGWebCacheURLs = new HashSet();
		sortedGWebCaches = new TreeSet(new GWebCacheComparator());
		random = new Random();
		NLogger.debug(NLoggerNames.GWEBCACHE, "Initialized GWebCacheContainer");
	}

	/**
	 * 
	 */
	public void initializeGWebCacheContainer() {
		allGWebCaches.clear();
		phexGWebCaches.clear();
		functionalGWebCaches.clear();
		uniqueGWebCacheURLs.clear();
		sortedGWebCaches.clear();
		insertPhexGWebCaches();
		// to speed up returning from method... do loading in background
		Runnable runner = new Runnable() {
			public void run() {
				loadGWebCacheFromFile();
			}
		};
		ThreadPool.getInstance().addJob(runner, "LoadGWebCacheRunner");
	}

	/**
	 * Connects to a random GWebCache to fetch more hosts. This should be
	 * triggered on startup and when there are not enough hosts in the catcher.
	 * The queryed hosts are directly put into the CaughtHostsContainer with
	 * high priority.
	 */
	public boolean queryMoreHosts(boolean preferPhex) {
		int retrys = 0;
		boolean succ = false;
		do {
			retrys++;
			GWebCacheConnection connection = getRandomGWebCacheConnection(preferPhex);
			// continue if no connection...
			if (connection == null) {
				continue;
			}
			DestAddress[] hosts = connection.sendHostFileRequest();
			// continue if cache is bad or data is null...
			if (!verifyGWebCache(connection) || hosts == null) {
				continue;
			}
			// everything looks good add data..
			CaughtHostsContainer container = HostManager.getInstance()
					.getCaughtHostsContainer();
			
			for (int i = 0; i < hosts.length; i++) {
				// gwebcache should only return Ultrapeers therefore we have
				// high priority.
				container.addCaughtHost(hosts[i],
						CaughtHostsContainer.HIGH_PRIORITY);
			}
			succ = true;
		}
		// do this max 5 times or until we where successful
		while (!succ && retrys < 5);
		return succ;
	}

	/**
	 * Updates the remote GWebCache. By the specification clients should only
	 * send updates if they accept incoming connections - i.e. clients behind
	 * firewalls should not send updates. Also, if supported by clients, only
	 * Ultrapeers/Supernodes should send updates. After a client has been up for
	 * an hour, it should begin sending an Update request periodically - every
	 * 60 minutes. It sends its own IP address and port in the "ip" parameter
	 * and a the URL of a random cache in the "url" parameter. Clients should
	 * only submit the URLs of caches that they know are functional!
	 */
	public boolean updateRemoteGWebCache(DestAddress myHostAddress,
			boolean preferPhex) {
		String fullHostName = null;
		if (myHostAddress != null) {
			fullHostName = myHostAddress.getFullHostName();
		}

		int retrys = 0;
		boolean succ = false;
		do {
			retrys++;
			GWebCacheConnection connection = getRandomGWebCacheConnection(preferPhex);
			// continue if no connection...
			if (connection == null) {
				continue;
			}
			GWebCache gWebCache = getGWebCacheForUpdate(connection
					.getGWebCache());
			assert !gWebCache.isPhexCache()
					&& gWebCache.equals(connection.getGWebCache()) : "isPhexCache: "
					+ gWebCache.isPhexCache()
					+ ",equals "
					+ gWebCache.getUrl()
					+ " - " + connection.getGWebCache().getUrl();

			String urlString = null;
			if (gWebCache != null) {
				urlString = gWebCache.getUrl().toExternalForm();
			}

			if (fullHostName == null && urlString == null) {
				// no data to update... try again to determine random GWebCache
				// in loop
				continue;
			}

			succ = connection.updateRequest(fullHostName, urlString);
			// continue if cache is bad or not successful...
			if (!verifyGWebCache(connection) || !succ) {
				continue;
			}
		}
		// do this max 5 times or until we where successful
		while (!succ && retrys < 5);
		return succ;
	}

	/**
	 * Connects to a random GWebCache to fetch more GWebCaches.
	 */
	public boolean queryMoreGWebCaches(boolean preferPhex) {
		int retrys = 0;
		boolean succ = false;
		do {
			retrys++;
			GWebCacheConnection connection = getRandomGWebCacheConnection(preferPhex);
			// continue if no connection...
			if (connection == null) {
				continue;
			}
			/** *************************************** */
			// URL[] urls = connection.sendURLFileRequest();
			URL[] urls = null;
			/** ***************************************** */
			// continue if cache is bad or data is null...
			if (!verifyGWebCache(connection) || urls == null) {
				continue;
			}
			// everything looks good add data..
			for (int i = 0; i < urls.length; i++) {
				try {
					GWebCache gWebCache = new GWebCache(urls[i]);
					if (isCacheAccessAllowed(gWebCache)) {
						insertGWebCache(gWebCache);
					}
				} catch (IOException exp) {// invalid GWebCache... ignore
					NLogger.debug(NLoggerNames.GWEBCACHE, exp);
				}
			}
			succ = true;
		}
		// do this max 5 times or until we where successful
		while (!succ && retrys < 5);
		return succ;
	}

	public int getGWebCacheCount() {
		return allGWebCaches.size();
	}

	private GWebCache getGWebCacheForUpdate(GWebCache ignore) {
		GWebCache gWebCache = null;
		int count = functionalGWebCaches.size();
		if (count == 0) {
			return null;
		} else if (count == 1) {
			gWebCache = (GWebCache) functionalGWebCaches.get(0);
			if (gWebCache.equals(ignore)) {
				return null;
			} else {
				assert (!gWebCache.isPhexCache());
				return gWebCache;
			}
		}

		int tries = 0;
		do {
			int randomIndex = random.nextInt(count - 1);
			gWebCache = (GWebCache) functionalGWebCaches.get(randomIndex);
			if (!gWebCache.equals(ignore)) {
				assert (!gWebCache.isPhexCache());
				return gWebCache;
			}
			tries++;
		} while (tries < 10);
		// no valid cache found...
		return null;
	}

	private GWebCache getRandomGWebCache(boolean preferPhex) {
		ensureMinGWebCaches();
		synchronized (allGWebCaches) {
			boolean usePhexCache = preferPhex && random.nextInt(8) == 0;
			if (usePhexCache && phexGWebCaches.size() > 0) {
				int randomIndex = random.nextInt(phexGWebCaches.size());
				return (GWebCache) phexGWebCaches.get(randomIndex);
			}

			int count = allGWebCaches.size();
			if (count == 0) {
				return null;
			}

			long now = System.currentTimeMillis();
			GWebCache cache = (GWebCache) sortedGWebCaches.first();
			if (now > cache.getEarliestReConnectTime()) {
				return cache;
			} else {
				return null;
			}
		}
	}

	/**
	 * Single trys to get a random and pinged GWebCacheConnection or null if
	 * connection could not be obtained.
	 */
	private GWebCacheConnection getRandomGWebCacheConnection(boolean preferPhex) {
		GWebCacheConnection connection = null;
		GWebCache gWebCache = null;
		try {
			gWebCache = getRandomGWebCache(preferPhex);
			// ususally this url is always != null but in case there is no
			// standard
			// GWebCache file or a different default gnutella network is choosed
			// the
			// url might be null
			if (gWebCache == null) {
				return null;
			}

			connection = new GWebCacheConnection(gWebCache);
			// we stop pinging GWebCache... this is not necessary since we
			// find out anyway if cache is working on first contact.
			// if ( !connection.sendPingRequest() )
			// {
			// removeGWebCache( url, false );
			// return null;
			// }
		} catch (ProtocolNotSupportedException exp) {// not valid url.. throw
														// away...
			removeGWebCache(gWebCache, true);
			return null;
		}

		return connection;
	}

	private boolean verifyGWebCache(GWebCacheConnection connection) {
		GWebCache gWebCache = connection.getGWebCache();
		// resort cache.
		sortedGWebCaches.remove(gWebCache);
		gWebCache.countConnectionAttempt(connection.isCacheBad());
		sortedGWebCaches.add(gWebCache);

		// cache is working add it to functional list if not phex cache.
		if (!connection.isCacheBad() && !gWebCache.isPhexCache()
				&& !functionalGWebCaches.contains(gWebCache)) {
			functionalGWebCaches.add(gWebCache);
		}
		saveGWebCacheToFile();

		return true;
	}

	/**
	 * Returns the CaughtHost that can be parsed from the line, or null if
	 * parsing failed for some reason.
	 * 
	 * @param line
	 * @return
	 * @throws MalformedURLException
	 */
	private GWebCache parseGWebCacheFromLine(String line) throws IOException {
		// tokenize line
		// line format can be:
		// URL or:
		// URL lastRequestTime failedInRowCount
		StringTokenizer tokenizer = new StringTokenizer(line, " ");
		int tokenCount = tokenizer.countTokens();

		String urlStr;
		long lastRequestTime;
		int failedInRowCount;
		if (tokenCount == 1) {
			urlStr = line;
			lastRequestTime = -1;
			failedInRowCount = -1;
		} else if (tokenCount == 3) {
			urlStr = tokenizer.nextToken();
			try {
				lastRequestTime = Long.parseLong(tokenizer.nextToken());
			} catch (NumberFormatException exp) {
				lastRequestTime = -1;
			}
			try {
				failedInRowCount = Integer.parseInt(tokenizer.nextToken());
			} catch (NumberFormatException exp) {
				failedInRowCount = -1;
			}
		} else {// Unknown format
			NLogger.warn(NLoggerNames.GWEBCACHE,
					"Unknown HostCache line format: " + line);
			return null;
		}

		NormalizableURL helpUrl = new NormalizableURL(urlStr);
		helpUrl.normalize();
		URL url = new URL(helpUrl.toExternalForm());

		GWebCache cache = new GWebCache(url);
		if (lastRequestTime > 0) {
			cache.setLastRequestTime(lastRequestTime);
		}
		if (failedInRowCount > 0) {
			cache.setFailedInRowCount(failedInRowCount);
		}

		return cache;
	}

	/**
	 * Inserts a GWebCache...
	 */
	private void insertGWebCache(GWebCache gWebCache) {
		synchronized (allGWebCaches) {
			if (allGWebCaches.size() >= MAX_G_WEB_CACHES_SIZE) {
				NLogger.error(NLoggerNames.GWEBCACHE,
						"Limit of 1000 GWebCaches reached.");
				removeGWebCache((GWebCache) sortedGWebCaches.last(), true);
				return;
			}

			// The URL object itself is not stored
			// since the hash function of the URL is way slow, it could lead to
			// doing a IP lookup.
			String uniqueString = gWebCache.getHostDomain();
			if (!uniqueGWebCacheURLs.contains(uniqueString)) {
				allGWebCaches.add(gWebCache);
				sortedGWebCaches.add(gWebCache);
				uniqueGWebCacheURLs.add(uniqueString);

				if (gWebCache.isPhexCache()) {
					phexGWebCaches.add(gWebCache);
				}
			}
		}
	}

	/**
	 * Removes a GWebCache..
	 */
	private void removeGWebCache(GWebCache gWebCache, boolean force) {
		synchronized (allGWebCaches) {
			// maintain a min number of GWebCaches even if bad.
			if (allGWebCaches.size() > MIN_G_WEB_CACHES_SIZE || force) {
				allGWebCaches.remove(gWebCache);
				functionalGWebCaches.remove(gWebCache);
				sortedGWebCaches.remove(gWebCache);

				String uniqueString = gWebCache.getHostDomain();
				uniqueGWebCacheURLs.remove(uniqueString);

				if (gWebCache.isPhexCache()) {
					phexGWebCaches.remove(gWebCache);
				}

				// save file...
				saveGWebCacheToFile();
			}
		}
	}

	/**
	 * Inserts a GWebCache...
	 */
	private void insertGWebCacheFromLine(String gWebCacheLine) {
		// verify URL
		try {
			GWebCache gWebCache = parseGWebCacheFromLine(gWebCacheLine);
			if (gWebCache != null && isCacheAccessAllowed(gWebCache)) {
				insertGWebCache(gWebCache);
			}
		} catch (IOException exp) {
			NLogger.debug(NLoggerNames.GWEBCACHE, exp);
		}
	}

	/**
	 * Tries to ensure that there is a minimum number of GWebCaches available.
	 * This is done by loading GWebCaches from a districuted GWebCache default
	 * file and 1 hard coded emergency GWebCache. If we are not on the General
	 * Gnutella Network there is no way to ensure a minimum set of GWebCaches
	 * and this call returns without actions.
	 * 
	 */
	private void ensureMinGWebCaches() {
		if (allGWebCaches.size() >= 10) {
			return;
		}
		NetworkManager networkMgr = NetworkManager.getInstance();
//		if (!(networkMgr.getGnutellaNetwork() instanceof GeneralGnutellaNetwork)) {// not
//																					// on
//																					// general
//																					// gnutella
//																					// network...
//																					// cant
//																					// use
//																					// default
//																					// list
//			return;
//		}

		NLogger.debug(NLoggerNames.GWEBCACHE, "Load default GWebCache file.");
		InputStream inStream = ClassLoader
				.getSystemResourceAsStream("phex/resources/PeerCrawl_gwebcache.cfg");
		if (inStream != null) {
			InputStreamReader reader = new InputStreamReader(inStream);
			try {
				loadGWebCacheFromReader(reader);
				saveGWebCacheToFile();
			} catch (IOException exp) {
				NLogger.warn(NLoggerNames.GWEBCACHE, exp, exp);
			}
		} else {
			NLogger.warn(NLoggerNames.GWEBCACHE,
					"Default GWebCache file not found.");
		}
		if (allGWebCaches.size() < 1) {// emergency case which should never
										// happen since the gwebcache.cfg
			// should contain enough caches.
			// insertGWebCache( "http://gwebcache.bearshare.net/gcache.php" );
			insertPhexGWebCaches();
			saveGWebCacheToFile();
		}
	}

	private void insertPhexGWebCaches() {
		NetworkManager networkMgr = NetworkManager.getInstance();
		if (!(networkMgr.getGnutellaNetwork() instanceof GeneralGnutellaNetwork)) {// not
																					// on
																					// general
																					// gnutella
																					// network...
																					// cant
																					// use
																					// default
																					// list
			return;
		}
		URL url;

		Iterator iterator = PHEX_WEB_CACHES.iterator();
		while (iterator.hasNext()) {
			try {
				String phexCachesURL = (String) iterator.next();
				url = new URL(phexCachesURL);
				GWebCache cache = new GWebCache(url, true);
				insertGWebCache(cache);
			} catch (IOException e) {
				NLogger.error(NLoggerNames.GWEBCACHE, e, e);
			}
		}
	}

	private boolean isCacheAccessAllowed(GWebCache gWebCache) {
		// check access by IP
		// Looking up host ip turns out to be a very slow solution...
		// byte[] hostIP = gWebCache.getHostIp();
		// byte access =
		// PhexSecurityManager.getInstance().controlHostIPAccess(hostIP);
		// if ( access != PhexSecurityManager.ACCESS_GRANTED )
		// {
		// // ignore GWebCache
		// NLogger.debug( NLoggerNames.GWEBCACHE, "GWebCache IP blocked." );
		// return false;
		// }

		// check if this is a IP only gWebCache.. IP only GWebCache are mostly
		// spam distributed by gavinroy.com
		URL cacheURL = gWebCache.getUrl();
		String hostName = cacheURL.getHost();
		if (AddressUtils.isIPHostName(hostName)
				&& StringUtils.isEmpty(cacheURL.getPath())) {
			return false;
		}

		// check access by host name
		Iterator iterator = BLOCKED_WEB_CACHES.iterator();
		while (iterator.hasNext()) {
			String blocked = (String) iterator.next();
			if (hostName.indexOf(blocked) != -1) {
				// ignore GWebCache
				NLogger
						.debug(NLoggerNames.GWEBCACHE,
								"GWebCache host blocked.");
				return false;
			}
		}

		return true;
	}

	public boolean isPhexGWebCache(String url) {
		return PHEX_WEB_CACHES.indexOf(url) != -1;
	}

	private void loadGWebCacheFromFile() {
		try {
			NetworkManager networkMgr = NetworkManager.getInstance();
			File file = networkMgr.getGnutellaNetwork().getGWebCacheFile();
			if (!file.exists()) {
				return;
			}
			loadGWebCacheFromReader(new FileReader(file));
		} catch (IOException exp) {
			NLogger.error(NLoggerNames.GWEBCACHE, exp, exp);
		} finally {
			ensureMinGWebCaches();
		}
	}

	/**
	 * Reads the contents of the reader and closes the reader when done.
	 * 
	 * @param reader
	 *            the reader to read from.
	 * @throws IOException
	 *             thrown when there are io errors.
	 */
	private void loadGWebCacheFromReader(Reader reader) throws IOException {
		BufferedReader br = new BufferedReader(reader);
		String line;
		synchronized (allGWebCaches) {
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}
				insertGWebCacheFromLine(line);
			}
		}
		br.close();
	}

	private void saveGWebCacheToFile() {
		NLogger.debug(NLoggerNames.GWEBCACHE, "Saving GWebCaches.");
		try {
			NetworkManager networkMgr = NetworkManager.getInstance();
			File file = networkMgr.getGnutellaNetwork().getGWebCacheFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));

			synchronized (allGWebCaches) {
				Iterator iterator = allGWebCaches.iterator();
				while (iterator.hasNext()) {
					GWebCache gWebCache = (GWebCache) iterator.next();
					if (gWebCache.isPhexCache()) {
						continue;
					}
					// line format can be:
					// URL or:
					// URL lastRequestTime failedInRowCount
					writer.write(gWebCache.getUrl().toExternalForm() + " "
							+ gWebCache.getLastRequestTime() + " "
							+ gWebCache.getFailedInRowCount());
					writer.newLine();
				}
			}
			writer.close();
		} catch (IOException exp) {
			NLogger.error(NLoggerNames.GWEBCACHE, exp, exp);
		}
	}
}