package brs.common;

public class Props {

  public static final String TIME_MULTIPLIER = "brs.timeMultiplier";

  public static final String GPU_ACCELERATION = "GPU.Acceleration";
  public static final String GPU_UNVERIFIED_QUEUE = "GPU.UnverifiedQueue";
  public static final String GPU_HASHES_PER_BATCH = "GPU.HashesPerBatch";
  public static final String GPU_MEM_PERCENT = "GPU.MemPercent";
  public static final String GPU_AUTODETECT = "GPU.AutoDetect";
  public static final String GPU_PLATFORM_IDX = "GPU.PlatformIdx";
  public static final String GPU_DEVICE_IDX = "GPU.DeviceIdx";

  public static final String BRS_TRIM_DERIVED_TABLES = "brs.trimDerivedTables";

  public static final String BRS_FORCE_SCAN = "brs.forceScan";
  public static final String BRS_FORCE_VALIDATE = "brs.forceValidate";

  public static final String BRS_MOCK_MINING = "brs.mockMining";

  public static final String TEST_NET = "TEST.Net";
  public static final String BRS_IS_OFFLINE = "brs.isOffline";

  public static final String BRS_DEBUG_TRACE_QUOTE = "brs.debugTraceQuote";
  public static final String BRS_DEBUG_TRACE_SEPARATOR = "brs.debugTraceSeparator";
  public static final String BRS_DEBUG_LOG_CONFIRMED = "brs.debugLogUnconfirmed";
  public static final String BRS_DEBUG_TRACE_ACCOUNTS = "brs.debugTraceAccounts";
  public static final String BRS_DEBUG_TRACE_LOG = "brs.debugTraceLog";

  public static final String BRS_ENABLE_TRANSACTION_REBROADCASTING = "brs.enableTransactionRebroadcasting";
  public static final String BRS_TEST_UNCONFIRMED_TRANSACTIONS = "brs.testUnconfirmedTransactions";

  public static final String BRS_REBROADCAST_AFTER = "brs.rebroadcastAfter";
  public static final String REBROADCAST_EVERY = "brs.rebroadcastEvery";
  public static final String DB_H2_DEFRAG_ON_SHUTDOWN = "Db.H2.DefragOnShutdown";

  public static final String BRS_TEST_DB_URL = "brs.testDbUrl";
  public static final String BRS_TEST_DB_USERNAME = "brs.testDbUsername";
  public static final String BRS_TEST_DB_SECRET = "brs.testDbPassword";
  public static final String BRS_DB_URL = "brs.dbUrl";
  public static final String BRS_DB_USERNAME = "brs.dbUsername";
  public static final String BRS_DB_SECRET = "brs.dbPassword";
  public static final String BRS_DB_MAXIMUM_POOL_SIZE = "brs.dbMaximumPoolSize";
  public static final String BRS_DB_DEFAULT_LOCK_TIMEOUT = "brs.dbDefaultLockTimeout";

  public static final String BRS_ALLOWED_BOT_HOSTS = "brs.allowedBotHosts";

  public static final String BRS_BLOCK_CACHE_MB = "brs.blockCacheMB";

  public static final String BRS_MAX_ROLLBACK = "brs.maxRollback";

  public static final String API_DEBUG = "API.Debug";

  public static final String API_SSL = "API.SSL";
  public static final String API_SERVER = "API.Server";
  public static final String API_SERVER_PORT = "API.ServerPort";
  public static final String API_SERVER_HOST = "API.ServerHost";
  public static final String API_DOC_DIR = "API.Doc_Dir";
  public static final String API_UI_DIR = "API.UI_Dir";
  public static final String API_CROSS_ORIGIN_FILTER = "API.CrossOriginFilter";
  public static final String API_SSL_KEY_STORE_PATH = "API.SSL_keyStorePath";
  public static final String API_SSL_KEY_STORE_SECRET = "API.SSL_keyStorePassword";
  public static final String API_SERVER_IDLE_TIMEOUT = "API.ServerIdleTimeout";
  public static final String API_SERVER_ENFORCE_POST = "API.ServerEnforcePOST";

  public static final String JETTY_API_GZIP_FILTER_METHODS = "JETTY.API.GZIPFilter.methods";
  public static final String JETTY_API_GZIP_FILTER_BUFFER_SIZE = "JETTY.API.GZIPFilter.bufferSize";
  public static final String JETTY_API_GZIP_FILTER_MIN_GZIP_SIZE = "JETTY.API.GZIPFilter.minGzipSize";

  public static final String JETTY_API_DOS_FILTER_MAX_REQUEST_PER_SEC = "JETTY.API.DoSFilter.maxRequestsPerSec";
  public static final String JETTY_API_DOS_FILTER_THROTTLED_REQUESTS = "JETTY.API.DoSFilter.throttledRequests";
  public static final String JETTY_API_DOS_FILTER_DELAY_MS = "JETTY.API.DoSFilter.delayMs";
  public static final String JETTY_API_DOS_FILTER_MAX_WAIT_MS = "JETTY.API.DoSFilter.maxWaitMs";
  public static final String JETTY_API_DOS_FILTER_MAX_REQUEST_MS = "JETTY.API.DoSFilter.maxRequestMs";
  public static final String JETTY_API_DOS_FILTER_THROTTLE_MS = "JETTY.API.DoSFilter.throttleMs";
  public static final String JETTY_API_DOS_FILTER_MAX_IDLE_TRACKER_MS = "JETTY.API.DoSFilter.maxIdleTrackerMs";
  public static final String JETTY_API_DOS_FILTER_TRACK_SESSIONS = "JETTY.API.DoSFilter.trackSessions";
  public static final String JETTY_API_DOS_FILTER_INSERT_HEADERS = "JETTY.API.DoSFilter.insertHeaders";
  public static final String JETTY_API_DOS_FILTER_REMOTE_PORT = "JETTY.API.DoSFilter.remotePort";
  public static final String JETTY_API_DOS_FILTER_IP_WHITELIST = "JETTY.API.DoSFilter.ipWhitelist";
  public static final String JETTY_API_DOS_FILTER_MANAGED_ATTR = "JETTY.API.DoSFilter.managedAttr";

  public static final String JETTY_P2P_GZIP_FILTER = "JETTY.P2P.GZIPFilter";
  public static final String JETTY_P2P_GZIP_FILTER_METHODS = "JETTY.P2P.GZIPFilter.methods";
  public static final String JETTY_P2P_GZIP_FILTER_BUFFER_SIZE = "JETTY.P2P.GZIPFilter.bufferSize";
  public static final String JETTY_P2P_GZIP_FILTER_MIN_GZIP_SIZE = "JETTY.P2P.GZIPFilter.minGzipSize";

  public static final String JETTY_P2P_DOS_FILTER_MAX_REQUESTS_PER_SEC = "JETTY.P2P.DoSFilter.maxRequestsPerSec";
  public static final String JETTY_P2P_DOS_FILTER_THROTTLED_REQUESTS = "JETTY.P2P.DoSFilter.throttledRequests";
  public static final String JETTY_P2P_DOS_FILTER_DELAY_MS = "JETTY.P2P.DoSFilter.delayMs";
  public static final String JETTY_P2P_DOS_FILTER_MAX_WAIT_MS = "JETTY.P2P.DoSFilter.maxWaitMs";
  public static final String JETTY_P2P_DOS_FILTER_MAX_REQUEST_MS = "JETTY.P2P.DoSFilter.maxRequestMs";
  public static final String JETTY_P2P_DOS_FILTER_THROTTLE_MS = "JETTY.P2P.DoSFilter.throttleMs";
  public static final String JETTY_P2P_DOS_FILTER_MAX_IDLE_TRACKER_MS = "JETTY.P2P.DoSFilter.maxIdleTrackerMs";
  public static final String JETTY_P2P_DOS_FILTER_TRACK_SESSIONS = "JETTY.P2P.DoSFilter.trackSessions";
  public static final String JETTY_P2P_DOS_FILTER_INSERT_HEADERS = "JETTY.P2P.DoSFilter.insertHeaders";
  public static final String JETTY_P2P_DOS_FILTER_REMOTE_PORT = "JETTY.P2P.DoSFilter.remotePort";
  public static final String JETTY_P2P_DOS_FILTER_IP_WHITELIST = "JETTY.P2P.DoSFilter.ipWhitelist";
  public static final String JETTY_P2P_DOS_FILTER_MANAGED_ATTR = "JETTY.P2P.DoSFilter.managedAttr";

  public static final String P2P_MY_PLATFORM = "P2P.myPlatform";
  public static final String P2P_MY_ADDRESS = "P2P.myAddress";
  public static final String P2P_PORT = "P2P.Port";
  public static final String P2P_SHARE_MY_ADDRESS = "P2P.shareMyAddress";
  public static final String P2P_MY_HALLMARK = "P2P.myHallmark";
  public static final String P2P_REBROADCAST_TO = "P2P.rebroadcastTo";
  public static final String P2P_BOOTSTRAP_PEERS = "P2P.BootstrapPeers";
  public static final String TEST_PEERS = "TEST.Peers";
  public static final String P2P_NUM_BOOTSTRAP_CONNECTIONS = "P2P.NumBootstrapConnections";
  public static final String P2P_BLACKLISTED_PEERS = "P2P.BlacklistedPeers";
  public static final String P2P_MAX_CONNECTIONS = "P2P.MaxConnections";
  public static final String P2P_TIMEOUT_CONNECT_MS = "P2P.TimeoutConnect_ms";
  public static final String P2P_TIMEOUT_READ_MS = "P2P.TimeoutRead_ms";
  public static final String P2P_HALLMARK_PROTECTION = "P2P.HallmarkProtection";
  public static final String P2P_HALLMARK_PUSH = "P2P.HallmarkPush";
  public static final String P2P_HALLMARK_PULL = "P2P.HallmarkPull";
  public static final String P2P_BLACKLISTING_TIME_MS = "P2P.BlacklistingTime_ms";
  public static final String BRS_COMMUNICATION_LOGGING_MASK = "brs.communicationLoggingMask";
  public static final String BRS_SEND_TO_PEERS_LIMIT = "brs.sendToPeersLimit";
  public static final String BRS_USE_PEERS_DB = "brs.usePeersDb";
  public static final String BRS_SAVE_PEERS = "brs.savePeers";
  public static final String BRS_GET_MORE_PEERS = "brs.getMorePeers";
  public static final String BRS_GET_MORE_PEERS_THRESHOLD = "brs.getMorePeersThreshold";
  public static final String BRS_DUMP_PEERS_VERSION = "brs.dumpPeersVersion";
  public static final String P2P_LISTEN = "P2P.Listen";
  public static final String P2P_TIMEOUT_IDLE_MS = "P2P.TimeoutIdle_ms";



  public static final String CPU_NUM_CORES = "CPU.NumCores";


  private Props() { //no need to construct
  }
}
