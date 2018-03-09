package brs.peer;

import brs.*;
import brs.common.Props;
import brs.services.AccountService;
import brs.services.PropertyService;
import brs.services.TimeService;
import brs.util.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.DoSFilter;
import org.eclipse.jetty.servlets.GzipFilter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import java.io.IOException;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;

public final class Peers {

  private static final Logger logger = LoggerFactory.getLogger(Peers.class);

  public enum Event {
    BLACKLIST, UNBLACKLIST, DEACTIVATE, REMOVE,
    DOWNLOADED_VOLUME, UPLOADED_VOLUME, WEIGHT,
    ADDED_ACTIVE_PEER, CHANGED_ACTIVE_PEER,
    NEW_PEER
  }

  static final int LOGGING_MASK_EXCEPTIONS = 1;
  static final int LOGGING_MASK_NON200_RESPONSES = 2;
  static final int LOGGING_MASK_200_RESPONSES = 4;
  static int communicationLoggingMask;

  static Set<String> wellKnownPeers;
  static Set<String> knownBlacklistedPeers;

  private static int connectWellKnownFirst;
  private static boolean connectWellKnownFinished;

  static Set<String> rebroadcastPeers;

  static int connectTimeout;
  static int readTimeout;
  static int blacklistingPeriod;
  static boolean getMorePeers;

  static final int DEFAULT_PEER_PORT = 8123;
  static final int TESTNET_PEER_PORT = 7123;
  private static String myPlatform;
  private static String myAddress;
  private static int myPeerServerPort;
  private static boolean shareMyAddress;
  private static int maxNumberOfConnectedPublicPeers;
  private static boolean enableHallmarkProtection;
  private static int pushThreshold;
  private static int pullThreshold;
  private static int sendToPeersLimit;
  private static boolean usePeersDb;
  private static boolean savePeers;
  private static int getMorePeersThreshold;
  private static String dumpPeersVersion;
  private static int lastSavedPeers;

  static JSONStreamAware myPeerInfoRequest;
  static JSONStreamAware myPeerInfoResponse;

  private static final Listeners<Peer,Event> listeners = new Listeners<>();

  private static final ConcurrentMap<String, PeerImpl> peers = new ConcurrentHashMap<>();
  private static final ConcurrentMap<String, String> announcedAddresses = new ConcurrentHashMap<>();

  static final Collection<PeerImpl> allPeers = Collections.unmodifiableCollection(peers.values());

  private static final ExecutorService sendToPeersService = Executors.newCachedThreadPool();
  private static final ExecutorService sendingService = Executors.newFixedThreadPool(10);

  private static TimeService timeService;

  public static void init(TimeService timeService, AccountService accountService, Blockchain blockchain, TransactionProcessor transactionProcessor,
      BlockchainProcessor blockchainProcessor, PropertyService propertyService, ThreadPool threadPool) {
    Peers.timeService = timeService;

    myPlatform = propertyService.getString(Props.P2P_MY_PLATFORM);
    if ( propertyService.getString(Props.P2P_MY_ADDRESS) != null
         && propertyService.getString(Props.P2P_MY_ADDRESS).trim().isEmpty()
         && Init.gateway != null ) {
      String externalIPAddress = null;
      try {
        externalIPAddress = Init.gateway.getExternalIPAddress();
      }
      catch (IOException|SAXException e) {
        logger.info("Can't get gateways IP adress");
      }
      myAddress = externalIPAddress;
    }
    else {
    }

    if (myAddress != null && myAddress.endsWith(":" + TESTNET_PEER_PORT) && !Constants.isTestnet) {
      throw new RuntimeException("Port " + TESTNET_PEER_PORT + " should only be used for testnet!!!");
    }
    myPeerServerPort = propertyService.getInt(Props.P2P_PORT);
    if (myPeerServerPort == TESTNET_PEER_PORT && !Constants.isTestnet) {
      throw new RuntimeException("Port " + TESTNET_PEER_PORT + " should only be used for testnet!!!");
    }
    shareMyAddress = propertyService.getBoolean(Props.P2P_SHARE_MY_ADDRESS) && ! Constants.isOffline;
    final String myHallmark = propertyService.getString(Props.P2P_MY_HALLMARK);
    if (myHallmark != null && ! myHallmark.isEmpty()) {
      try {
        Hallmark hallmark = Hallmark.parseHallmark(myHallmark);
        if (!hallmark.isValid() || myAddress == null) {
          throw new RuntimeException();
        }
        URI uri = new URI("http://" + myAddress.trim());
        String host = uri.getHost();
        if (!hallmark.getHost().equals(host)) {
          throw new RuntimeException();
        }
      }
      catch (RuntimeException | URISyntaxException e) {
        logger.info("Your hallmark is invalid: " + myHallmark + " for your address: " + myAddress);
        throw new RuntimeException(e.toString(), e);
      }
    }

    JSONObject json = new JSONObject();
    if (myAddress != null && ! myAddress.isEmpty()) {
      try {
        URI uri = new URI("http://" + myAddress.trim());
        String host = uri.getHost();
        int port = uri.getPort();
        if (!Constants.isTestnet) {
          if (port >= 0) {
            json.put("announcedAddress", myAddress);
          }
          else {
            json.put("announcedAddress", host + (myPeerServerPort != DEFAULT_PEER_PORT ? ":" + myPeerServerPort : ""));
          }
        }
        else {
          json.put("announcedAddress", host);
        }
      }
      catch (URISyntaxException e) {
        logger.info("Your announce address is invalid: " + myAddress);
        throw new RuntimeException(e.toString(), e);
      }
    }

    if (myHallmark != null && ! myHallmark.isEmpty()) {
      json.put("hallmark", myHallmark);
    }

    json.put("application",  Burst.APPLICATION);
    json.put("version",      Burst.VERSION);
    json.put("platform",     Peers.myPlatform);
    json.put("shareAddress", Peers.shareMyAddress);
    logger.debug("My peer info:\n" + json.toJSONString());
    myPeerInfoResponse = JSON.prepare(json);
    json.put("requestType", "getInfo");
    myPeerInfoRequest = JSON.prepareRequest(json);

    rebroadcastPeers = Collections.unmodifiableSet(new HashSet<>(propertyService.getStringList(Props.P2P_REBROADCAST_TO)));

    List<String> wellKnownPeersList = propertyService.getStringList(Constants.isTestnet ? Props.TEST_PEERS : Props.P2P_BOOTSTRAP_PEERS);

    for(String rePeer : rebroadcastPeers) {
      if(!wellKnownPeersList.contains(rePeer)) {
        wellKnownPeersList.add(rePeer);
      }
    }
    if (wellKnownPeersList.isEmpty() || Constants.isOffline) {
      wellKnownPeers = Collections.emptySet();
    } else {
      wellKnownPeers = Collections.unmodifiableSet(new HashSet<>(wellKnownPeersList));
    }

    connectWellKnownFirst = propertyService.getInt(Props.P2P_NUM_BOOTSTRAP_CONNECTIONS);
    connectWellKnownFinished = (connectWellKnownFirst == 0);

    List<String> knownBlacklistedPeersList = propertyService.getStringList(Props.P2P_BLACKLISTED_PEERS);
    if (knownBlacklistedPeersList.isEmpty()) {
      knownBlacklistedPeers = Collections.emptySet();
    } else {
      knownBlacklistedPeers = Collections.unmodifiableSet(new HashSet<>(knownBlacklistedPeersList));
    }

    maxNumberOfConnectedPublicPeers = propertyService.getInt(Props.P2P_MAX_CONNECTIONS);
    connectTimeout = propertyService.getInt(Props.P2P_TIMEOUT_CONNECT_MS);
    readTimeout = propertyService.getInt(Props.P2P_TIMEOUT_READ_MS);
    enableHallmarkProtection = propertyService.getBoolean(Props.P2P_HALLMARK_PROTECTION);
    pushThreshold = propertyService.getInt(Props.P2P_HALLMARK_PUSH);
    pullThreshold = propertyService.getInt(Props.P2P_HALLMARK_PULL);

    blacklistingPeriod = propertyService.getInt(Props.P2P_BLACKLISTING_TIME_MS);
    communicationLoggingMask = propertyService.getInt(Props.BRS_COMMUNICATION_LOGGING_MASK);
    sendToPeersLimit = propertyService.getInt(Props.BRS_SEND_TO_PEERS_LIMIT);
    usePeersDb       = propertyService.getBoolean(Props.P2P_USE_PEERS_DB) && ! Constants.isOffline;
    savePeers        = usePeersDb && propertyService.getBoolean(Props.P2P_SAVE_PEERS);
    getMorePeers     = propertyService.getBoolean(Props.P2P_GET_MORE_PEERS);
    getMorePeersThreshold = propertyService.getInt(Props.P2P_GET_MORE_PEERS_THRESHOLD);
    dumpPeersVersion = propertyService.getString(Props.DEV_DUMP_PEERS_VERSION);

    final List<Future<String>> unresolvedPeers = Collections.synchronizedList(new ArrayList<Future<String>>());

    threadPool.runBeforeStart(new Runnable() {

        private void loadPeers(Collection<String> addresses) {
          for (final String address : addresses) {
            Future<String> unresolvedAddress = sendToPeersService.submit(() -> {
              Peer peer = Peers.addPeer(address);
              return peer == null ? address : null;
            });
            unresolvedPeers.add(unresolvedAddress);
          }
        }

        @Override
        public void run() {
          if (! wellKnownPeers.isEmpty()) {
            loadPeers(wellKnownPeers);
          }
          if (usePeersDb) {
            logger.debug("Loading known peers from the database...");
            loadPeers(Burst.getDbs().getPeerDb().loadPeers());
          }
          lastSavedPeers= peers.size();
        }
      }, false);

    threadPool.runAfterStart(() -> {
      for (Future<String> unresolvedPeer : unresolvedPeers) {
        try {
          String badAddress = unresolvedPeer.get(5, TimeUnit.SECONDS);
          if (badAddress != null) {
            logger.debug("Failed to resolve peer address: " + badAddress);
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
          logger.debug("Failed to add peer", e);
        } catch (TimeoutException e) {
        }
      }
      logger.debug("Known peers: " + peers.size());
    });

    Init.init(timeService, accountService, blockchain, transactionProcessor, blockchainProcessor, propertyService, threadPool);

    if (! Constants.isOffline) {
      threadPool.scheduleThread("PeerConnecting", Peers.peerConnectingThread, 5);
      threadPool.scheduleThread("PeerUnBlacklisting", Peers.peerUnBlacklistingThread, 1);
      if (Peers.getMorePeers) {
        threadPool.scheduleThread("GetMorePeers", Peers.getMorePeersThread, 5);
      }
    }

    accountService.addListener(account -> {
      for (PeerImpl peer : Peers.peers.values()) {
        if (peer.getHallmark() != null && peer.getHallmark().getAccountId() == account.getId()) {
          Peers.listeners.notify(peer, Event.WEIGHT);
        }
      }
    }, Account.Event.BALANCE);
  }

  private static class Init {

    private static Server peerServer;
    private static GatewayDevice gateway;
    private static Integer port;

    static void init(TimeService timeService, AccountService accountService, Blockchain blockchain, TransactionProcessor transactionProcessor,
        BlockchainProcessor blockchainProcessor, PropertyService propertyService, ThreadPool threadPool) {
      if (Peers.shareMyAddress) {
        peerServer = new Server();
        ServerConnector connector = new ServerConnector(peerServer);
        port = Constants.isTestnet ? TESTNET_PEER_PORT : Peers.myPeerServerPort;
        connector.setPort(port);
        final String host = propertyService.getString(Props.P2P_LISTEN);
        connector.setHost(host);
        connector.setIdleTimeout(propertyService.getInt(Props.P2P_TIMEOUT_IDLE_MS));
        connector.setReuseAddress(true);
        peerServer.addConnector(connector);

        ServletHolder peerServletHolder = new ServletHolder(new PeerServlet(timeService, accountService, blockchain,
                                                                            transactionProcessor, blockchainProcessor));
        boolean isGzipEnabled = propertyService.getBoolean(Props.JETTY_P2P_GZIP_FILTER);
        peerServletHolder.setInitParameter("isGzipEnabled", Boolean.toString(isGzipEnabled));

        ServletHandler peerHandler = new ServletHandler();
        peerHandler.addServletWithMapping(peerServletHolder, "/*");

        if (isGzipEnabled) {
          FilterHolder gzipFilterHolder = peerHandler.addFilterWithMapping(GzipFilter.class, "/*", FilterMapping.DEFAULT);
          gzipFilterHolder.setInitParameter("methods",     propertyService.getString(Props.JETTY_P2P_GZIP_FILTER_METHODS));
          gzipFilterHolder.setInitParameter("bufferSize",  propertyService.getString(Props.JETTY_P2P_GZIP_FILTER_BUFFER_SIZE));
          gzipFilterHolder.setInitParameter("minGzipSize", propertyService.getString(Props.JETTY_P2P_GZIP_FILTER_MIN_GZIP_SIZE));
          gzipFilterHolder.setAsyncSupported(true);
        }

        if (propertyService.getBoolean("JETTY.P2P.DoSFilter")) {
          FilterHolder dosFilterHolder = peerHandler.addFilterWithMapping(DoSFilter.class, "/*", FilterMapping.DEFAULT);
          dosFilterHolder.setInitParameter("maxRequestsPerSec", propertyService.getString(Props.JETTY_P2P_DOS_FILTER_MAX_REQUESTS_PER_SEC));
          dosFilterHolder.setInitParameter("throttledRequests", propertyService.getString(Props.JETTY_P2P_DOS_FILTER_THROTTLED_REQUESTS));
          dosFilterHolder.setInitParameter("delayMs",           propertyService.getString(Props.JETTY_P2P_DOS_FILTER_DELAY_MS));
          dosFilterHolder.setInitParameter("maxWaitMs",         propertyService.getString(Props.JETTY_P2P_DOS_FILTER_MAX_WAIT_MS));
          dosFilterHolder.setInitParameter("maxRequestMs",      propertyService.getString(Props.JETTY_P2P_DOS_FILTER_MAX_REQUEST_MS));
          dosFilterHolder.setInitParameter("maxthrottleMs",     propertyService.getString(Props.JETTY_P2P_DOS_FILTER_THROTTLE_MS));
          dosFilterHolder.setInitParameter("maxIdleTrackerMs",  propertyService.getString(Props.JETTY_P2P_DOS_FILTER_MAX_IDLE_TRACKER_MS));
          dosFilterHolder.setInitParameter("trackSessions",     propertyService.getString(Props.JETTY_P2P_DOS_FILTER_TRACK_SESSIONS));
          dosFilterHolder.setInitParameter("insertHeaders",     propertyService.getString(Props.JETTY_P2P_DOS_FILTER_INSERT_HEADERS));
          dosFilterHolder.setInitParameter("remotePort",        propertyService.getString(Props.JETTY_P2P_DOS_FILTER_REMOTE_PORT));
          dosFilterHolder.setInitParameter("ipWhitelist",       propertyService.getString(Props.JETTY_P2P_DOS_FILTER_IP_WHITELIST));
          dosFilterHolder.setInitParameter("managedAttr",       propertyService.getString(Props.JETTY_P2P_DOS_FILTER_MANAGED_ATTR));
          dosFilterHolder.setAsyncSupported(true);
        }

        Runnable GwDiscover = () -> {
          GatewayDiscover gatewayDiscover = new GatewayDiscover();
          gatewayDiscover.setTimeout(2000);
          try {
            gatewayDiscover.discover();
          }
          catch (IOException|SAXException|ParserConfigurationException e) {
          }
          logger.trace("Looking for Gateway Devices");
          gateway = gatewayDiscover.getValidGateway();

          if (gateway != null) {
            gateway.setHttpReadTimeout(2000);
            try {
              InetAddress localAddress = gateway.getLocalAddress();
              String externalIPAddress = gateway.getExternalIPAddress();
              logger.info("Attempting to map {0}:{1} -> {2}:{3} on Gateway {0} ({1})",
                          externalIPAddress, port, localAddress, port, gateway.getModelName(), gateway.getModelDescription());

              if (!gateway.getSpecificPortMappingEntry(port, "TCP", new PortMappingEntry())) {
                logger.info("Port was already mapped. Aborting test.");
              }
              else {
                if (gateway.addPortMapping(port, port, localAddress.getHostAddress(), "TCP", "burstcoin")) {
                  logger.info("UPNP Mapping successful");
                }
              }
            }
            catch (IOException|SAXException e) {
              logger.error("Can't start UPNP", e);
            }
          }
        };
        new Thread(GwDiscover).start();

        peerServer.setStopAtShutdown(true);
        threadPool.runBeforeStart(new Runnable() {
            @Override
            public void run() {
              try {
                peerServer.start();
                logger.info("Started peer networking server at " + host + ":" + port);
              }
              catch (Exception e) {
                logger.error("Failed to start peer networking server", e);
                throw new RuntimeException(e.toString(), e);
              }
            }
          }, true);
      }
      else {
        peerServer = null;
        gateway    = null;
        port       = null;
        logger.info("shareMyAddress is disabled, will not start peer networking server");
      }
    }

    private Init() {}

  }

  private static final Runnable peerUnBlacklistingThread = () -> {

    try {
      try {

        long curTime = System.currentTimeMillis();
        for (PeerImpl peer : peers.values()) {
          peer.updateBlacklistedStatus(curTime);
        }

      } catch (Exception e) {
        logger.debug("Error un-blacklisting peer", e);
      }
    } catch (Throwable t) {
      logger.info("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
      System.exit(1);
    }

  };

  private static final Runnable peerConnectingThread = new Runnable() {
    @Override
    public void run() {
      try {
        int numConnectedPeers = getNumberOfConnectedPublicPeers();
        /*
         * aggressive connection with while loop.
         * if we have connected to our target amount we can exit loop.
         * if peers size is equal or below connected value we have nothing to connect to
         */
        while (numConnectedPeers < maxNumberOfConnectedPublicPeers && peers.size() > numConnectedPeers) {
          PeerImpl peer = (PeerImpl)getAnyPeer(ThreadLocalRandom.current().nextInt(2) == 0 ? Peer.State.NON_CONNECTED : Peer.State.DISCONNECTED, false);
          if (peer != null) {
            peer.connect(timeService.getEpochTime());
            /*
             * remove non connected peer. if peer is blacklisted, keep it to maintain blacklist time.
             * Peers should never be removed if total peers are below our target to prevent total erase of peers
             * if we loose Internet connection
             */
            if (peer.getState() != Peer.State.CONNECTED && !peer.isBlacklisted() && peers.size() > maxNumberOfConnectedPublicPeers) {
              removePeer(peer);
            }
            else {
              numConnectedPeers++;
            }
          }

          //Executor shutdown?
          if (Thread.currentThread().isInterrupted()) {
            return;
          }
        }

        int now = timeService.getEpochTime();
        for (PeerImpl peer : peers.values()) {
          if (peer.getState() == Peer.State.CONNECTED && now - peer.getLastUpdated() > 3600) {
            peer.connect(timeService.getEpochTime());
            if(peer.getState() != Peer.State.CONNECTED && !peer.isBlacklisted() && peers.size() > maxNumberOfConnectedPublicPeers) {
              removePeer(peer);
            }
          }
        }

        if(lastSavedPeers != peers.size()) {
          lastSavedPeers = peers.size();
          updateSavedPeers();
        }

      } catch (Exception e) {
        logger.debug("Error connecting to peer", e);
      }
  }
    private void updateSavedPeers() {
      Set<String> oldPeers = new HashSet<>(Burst.getDbs().getPeerDb().loadPeers());
      Set<String> currentPeers = new HashSet<>();
      for (Peer peer : Peers.peers.values()) {
        if (peer.getAnnouncedAddress() != null && ! peer.isBlacklisted() && ! peer.isWellKnown()) {
          currentPeers.add(peer.getAnnouncedAddress());
        }
      }
      Set<String> toDelete = new HashSet<>(oldPeers);
      toDelete.removeAll(currentPeers);
      try {
        Burst.getStores().beginTransaction();
        Burst.getDbs().getPeerDb().deletePeers(toDelete);
     //   logger.debug("Deleted " + toDelete.size() + " peers from the peers database");
        currentPeers.removeAll(oldPeers);
        Burst.getDbs().getPeerDb().addPeers(currentPeers);
     //   logger.debug("Added " + currentPeers.size() + " peers to the peers database");
        Burst.getStores().commitTransaction();
      } catch (Exception e) {
        Burst.getStores().rollbackTransaction();
        throw e;
      } finally {
        Burst.getStores().endTransaction();
      }
    }


  };

  private static final Runnable getMorePeersThread = new Runnable() {

      private final JSONStreamAware getPeersRequest;
      {
        JSONObject request = new JSONObject();
        request.put("requestType", "getPeers");
        getPeersRequest = JSON.prepareRequest(request);
      }

      private volatile boolean addedNewPeer;
      {
        Peers.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
              addedNewPeer = true;
            }
          }, Event.NEW_PEER);
      }

      @Override
      public void run() {

        try {
          try {
            /* We do not want more peers if above Threshold but we need enough to
             * connect to selected number of peers
             */
            if(peers.size() >= getMorePeersThreshold && peers.size() > maxNumberOfConnectedPublicPeers) {
              return;
            }

            Peer peer = getAnyPeer(Peer.State.CONNECTED, true);
            if (peer == null) {
              return;
            }
            JSONObject response = peer.send(getPeersRequest);
            if (response == null) {
              return;
            }
            JSONArray peers = (JSONArray)response.get("peers");
            Set<String> addedAddresses = new HashSet<>();
            if (peers != null) {
              for (Object announcedAddress : peers) {
                if (addPeer((String) announcedAddress) != null) {
                  addedAddresses.add((String) announcedAddress);
                }
              }
              if (savePeers && addedNewPeer) {
                addedNewPeer = false;
              }
            }

            JSONArray myPeers = new JSONArray();
            for (Peer myPeer : Peers.getAllPeers()) {
              if (! myPeer.isBlacklisted() && myPeer.getAnnouncedAddress() != null
                  && myPeer.getState() == Peer.State.CONNECTED && myPeer.shareAddress()
                  && ! addedAddresses.contains(myPeer.getAnnouncedAddress())
                  && ! myPeer.getAnnouncedAddress().equals(peer.getAnnouncedAddress())) {
                myPeers.add(myPeer.getAnnouncedAddress());
              }
            }
            //executor shutdown?
            if (Thread.currentThread().isInterrupted()) {
              return;
            }

            if (myPeers.size() > 0) {
              JSONObject request = new JSONObject();
              request.put("requestType", "addPeers");
              request.put("peers", myPeers);
              peer.send(JSON.prepareRequest(request));
            }

          } catch (Exception e) {
            logger.debug("Error requesting peers from a peer", e);
          }
        } catch (Throwable t) {
          logger.info("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
          System.exit(1);
        }

      }

    };

  public static void shutdown(ThreadPool threadPool) {
    if (Init.peerServer != null) {
      try {
        Init.peerServer.stop();
      } catch (Exception e) {
        logger.info("Failed to stop peer server", e);
      }
    }
    if ( Init.gateway != null ) {
      try {
        Init.gateway.deletePortMapping(Init.port, "TCP");
      }
      catch ( Exception e) {
        logger.info("Failed to remove UPNP rule from gateway", e);
      }
    }
    if (dumpPeersVersion != null) {
      StringBuilder buf = new StringBuilder();
      for (Map.Entry<String,String> entry : announcedAddresses.entrySet()) {
        Peer peer = peers.get(entry.getValue());
        if (peer != null && peer.getState() == Peer.State.CONNECTED && peer.shareAddress() && !peer.isBlacklisted()
            && peer.getVersion() != null && peer.getVersion().startsWith(dumpPeersVersion)) {
          buf.append("('").append(entry.getKey()).append("'), ");
        }
      }
      logger.info(buf.toString());
    }

    threadPool.shutdownExecutor(sendToPeersService);
  }

  public static boolean addListener(Listener<Peer> listener, Event eventType) {
    return Peers.listeners.addListener(listener, eventType);
  }

  public static boolean removeListener(Listener<Peer> listener, Event eventType) {
    return Peers.listeners.removeListener(listener, eventType);
  }

  static void notifyListeners(Peer peer, Event eventType) {
    Peers.listeners.notify(peer, eventType);
  }

  public static Collection<? extends Peer> getAllPeers() {
    return allPeers;
  }

  public static Collection<? extends Peer> getActivePeers() {
    List<PeerImpl> activePeers = new ArrayList<>();
    for (PeerImpl peer : peers.values()) {
      if (peer.getState() != Peer.State.NON_CONNECTED) {
        activePeers.add(peer);
      }
    }
    return activePeers;
  }

  public static Collection<? extends Peer> getPeers(Peer.State state) {
    List<PeerImpl> peerList = new ArrayList<>();
    for (PeerImpl peer : peers.values()) {
      if (peer.getState() == state) {
        peerList.add(peer);
      }
    }
    return peerList;
  }

  public static Peer getPeer(String peerAddress) {
    return peers.get(peerAddress);
  }

  public static Peer addPeer(String announcedAddress) {
    if (announcedAddress == null) {
      return null;
    }
    announcedAddress = announcedAddress.trim();
    Peer peer;
    if ((peer = peers.get(announcedAddress)) != null) {
      return peer;
    }
    String address;
    if ((address = announcedAddresses.get(announcedAddress)) != null && (peer = peers.get(address)) != null) {
      return peer;
    }
    try {
      URI uri = new URI("http://" + announcedAddress);
      String host = uri.getHost();
      if ((peer = peers.get(host)) != null) {
        return peer;
      }
      InetAddress inetAddress = InetAddress.getByName(host);
      return addPeer(inetAddress.getHostAddress(), announcedAddress);
    } catch (URISyntaxException | UnknownHostException e) {
      //logger.debug("Invalid peer address: " + announcedAddress + ", " + e.toString());
      return null;
    }
  }

  static PeerImpl addPeer(final String address, final String announcedAddress) {

    //re-add the [] to ipv6 addresses lost in getHostAddress() above
    String clean_address = address;
    if (clean_address.split(":").length > 2) {
      clean_address = "[" + clean_address + "]";
    }
    PeerImpl peer;
    if ((peer = peers.get(clean_address)) != null) {
      return peer;
    }
    String peerAddress = normalizeHostAndPort(clean_address);
    if (peerAddress == null) {
      return null;
    }
    if ((peer = peers.get(peerAddress)) != null) {
      return peer;
    }

    String announcedPeerAddress = address.equals(announcedAddress) ? peerAddress : normalizeHostAndPort(announcedAddress);

    if (Peers.myAddress != null && ! Peers.myAddress.isEmpty() && Peers.myAddress.equalsIgnoreCase(announcedPeerAddress)) {
      return null;
    }

    peer = new PeerImpl(peerAddress, announcedPeerAddress);
    if (Constants.isTestnet && peer.getPort() > 0 && peer.getPort() != TESTNET_PEER_PORT) {
      logger.debug("Peer " + peerAddress + " on testnet is not using port " + TESTNET_PEER_PORT + ", ignoring");
      return null;
    }
    peers.put(peerAddress, peer);
    if (announcedAddress != null) {
      updateAddress(peer);
    }
    listeners.notify(peer, Event.NEW_PEER);
    return peer;
  }

  static PeerImpl removePeer(PeerImpl peer) {
    if (peer.getAnnouncedAddress() != null) {
      announcedAddresses.remove(peer.getAnnouncedAddress());
    }
    return peers.remove(peer.getPeerAddress());
  }

  static void updateAddress(PeerImpl peer) {
    String oldAddress = announcedAddresses.put(peer.getAnnouncedAddress(), peer.getPeerAddress());
    if (oldAddress != null && !peer.getPeerAddress().equals(oldAddress)) {
      //logger.debug("Peer " + peer.getAnnouncedAddress() + " has changed address from " + oldAddress
      //        + " to " + peer.getPeerAddress());
      Peer oldPeer = peers.remove(oldAddress);
      if (oldPeer != null) {
        Peers.notifyListeners(oldPeer, Peers.Event.REMOVE);
      }
    }
  }

  public static void sendToSomePeers(Block block) {
    JSONObject request = block.getJSONObject();
    request.put("requestType", "processBlock");
    sendToSomePeers(request);
  }

  public static void sendToSomePeers(List<Transaction> transactions) {
    JSONObject request = new JSONObject();
    JSONArray transactionsData = new JSONArray();
    for (Transaction transaction : transactions) {
      transactionsData.add(transaction.getJSONObject());
    }
    request.put("requestType", "processTransactions");
    request.put("transactions", transactionsData);
    sendToSomePeers(request);
  }

  private static void sendToSomePeers(final JSONObject request) {

    sendingService.submit(() -> {
      final JSONStreamAware jsonRequest = JSON.prepareRequest(request);

      int successful = 0;
      List<Future<JSONObject>> expectedResponses = new ArrayList<>();
      for (final Peer peer : peers.values()) {

        if (Peers.enableHallmarkProtection && peer.getWeight() < Peers.pushThreshold) {
          continue;
        }

        if (!peer.isBlacklisted() && peer.getState() == Peer.State.CONNECTED && peer.getAnnouncedAddress() != null) {
          Future<JSONObject> futureResponse = sendToPeersService.submit(() -> peer.send(jsonRequest));
          expectedResponses.add(futureResponse);
        }
        if (expectedResponses.size() >= Peers.sendToPeersLimit - successful) {
          for (Future<JSONObject> future : expectedResponses) {
            try {
              JSONObject response = future.get();
              if (response != null && response.get("error") == null) {
                successful += 1;
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
              logger.debug("Error in sendToSomePeers", e);
            }

          }
          expectedResponses.clear();
        }
        if (successful >= Peers.sendToPeersLimit) {
          return;
        }
      }
    });
  }

  public static void rebroadcastTransactions(List<Transaction> transactions) {
    StringBuilder info = new StringBuilder("Rebroadcasting transactions: ");
    for(Transaction tx : transactions) {
      info.append(Convert.toUnsignedLong(tx.getId())).append(" ");
    }
    info.append("\n to peers ");
    for(Peer peer : peers.values()) {
      if(peer.isRebroadcastTarget()) {
        info.append(peer.getPeerAddress()).append(" ");
      }
    }
    logger.debug(info.toString());

    JSONObject request = new JSONObject();
    JSONArray transactionsData = new JSONArray();
    for (Transaction transaction : transactions) {
      transactionsData.add(transaction.getJSONObject());
    }
    request.put("requestType", "processTransactions");
    request.put("transactions", transactionsData);

    final JSONObject requestFinal = request;

    sendingService.submit(() -> {
      final JSONStreamAware jsonRequest = JSON.prepareRequest(requestFinal);

      for (final Peer peer : peers.values()) {
        if(peer.isRebroadcastTarget()) {
          sendToPeersService.submit(() -> peer.send(jsonRequest));
        }
      }
    });

    sendToSomePeers(request); // send to some normal peers too
  }


  public static Peer getAnyPeer(Peer.State state, boolean applyPullThreshold) {

    if(!connectWellKnownFinished) {
      int wellKnownConnected = 0;
      for(Peer peer : peers.values()) {
        if(peer.isWellKnown() && peer.getState() == Peer.State.CONNECTED) {
          wellKnownConnected++;
        }
      }
      if (wellKnownConnected >= connectWellKnownFirst) {
        connectWellKnownFinished = true;
        logger.info("Finished connecting to " + connectWellKnownFirst + " well known peers.");
        logger.info("You can open your Burst Wallet in your favorite browser with: http://127.0.0.1:8125 or http://localhost:8125");
      }
    }

    List<Peer> selectedPeers = new ArrayList<>();
    for (Peer peer : peers.values()) {
      if (! peer.isBlacklisted() && peer.getState() == state && peer.shareAddress()
          && (!applyPullThreshold || ! Peers.enableHallmarkProtection || peer.getWeight() >= Peers.pullThreshold)
          && (connectWellKnownFinished || peer.getState() == Peer.State.CONNECTED || peer.isWellKnown())) {
        selectedPeers.add(peer);
      }
    }

    if (selectedPeers.size() > 0) {
      if (! Peers.enableHallmarkProtection) {
        return selectedPeers.get(ThreadLocalRandom.current().nextInt(selectedPeers.size()));
      }

      long totalWeight = 0;
      for (Peer peer : selectedPeers) {
        long weight = peer.getWeight();
        if (weight == 0) {
          weight = 1;
        }
        totalWeight += weight;
      }

      long hit = ThreadLocalRandom.current().nextLong(totalWeight);
      for (Peer peer : selectedPeers) {
        long weight = peer.getWeight();
        if (weight == 0) {
          weight = 1;
        }
        if ((hit -= weight) < 0) {
          return peer;
        }
      }
    }
    return null;
  }

  static String normalizeHostAndPort(String address) {
    try {
      if (address == null) {
        return null;
      }
      URI uri = new URI("http://" + address.trim());
      String host = uri.getHost();
      if (host == null || host.equals("") ) {
        return null;
      }
      InetAddress inetAddress = InetAddress.getByName(host);
      if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress() ||
          inetAddress.isLinkLocalAddress()) {
        return null;
      }
      int port = uri.getPort();
      return port == -1 ? host : host + ':' + port;
    } catch (URISyntaxException |UnknownHostException e) {
      return null;
    }
  }

  private static int getNumberOfConnectedPublicPeers() {
    int numberOfConnectedPeers = 0;
    for (Peer peer : peers.values()) {
      // If hallmark enabled below  if line will return 0.
      // if (peer.getState() == Peer.State.CONNECTED) && peer.getAnnouncedAddress() != null
      //     && (! Peers.enableHallmarkProtection || peer.getWeight() > 0)) {
      if (peer.getState() == Peer.State.CONNECTED) {
        numberOfConnectedPeers++;
      }
    }
    return numberOfConnectedPeers;
  }

  private Peers() {} // never

}
