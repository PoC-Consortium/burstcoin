package brs.common;

public class Props {

  // DEV options
  public static final String DEV_OFFLINE = "DEV.Offline";
  public static final String DEV_TESTNET = "DEV.TestNet";
  
  public static final String DEV_TIMEWARP    = "DEV.TimeWarp";
  public static final String DEV_MOCK_MINING = "DEV.mockMining";

  public static final String DEV_DB_URL      = "DEV.DB.Url";
  public static final String DEV_DB_USERNAME = "DEV.DB.Username";
  public static final String DEV_DB_PASSWORD = "DEV.DB.Password";

  public static final String DEV_DUMP_PEERS_VERSION = "DEV.dumpPeersVersion";

  public static final String DEV_FORCE_VALIDATE = "DEV.forceValidate";
  public static final String DEV_FORCE_SCAN     = "DEV.forceScan";

  public static final String BRS_DEBUG_TRACE_QUOTE = "brs.debugTraceQuote";
  public static final String BRS_DEBUG_TRACE_SEPARATOR = "brs.debugTraceSeparator";
  public static final String BRS_DEBUG_LOG_CONFIRMED = "brs.debugLogUnconfirmed";
  public static final String BRS_DEBUG_TRACE_ACCOUNTS = "brs.debugTraceAccounts";

  public static final String BRS_DEBUG_TRACE_LOG = "brs.debugTraceLog";
  public static final String BRS_COMMUNICATION_LOGGING_MASK = "brs.communicationLoggingMask";


  
  // GPU options 
  public static final String GPU_ACCELERATION     = "GPU.Acceleration";
  public static final String GPU_AUTODETECT       = "GPU.AutoDetect";
  public static final String GPU_PLATFORM_IDX     = "GPU.PlatformIdx";
  public static final String GPU_DEVICE_IDX       = "GPU.DeviceIdx";
  public static final String GPU_UNVERIFIED_QUEUE = "GPU.UnverifiedQueue";
  public static final String GPU_HASHES_PER_BATCH = "GPU.HashesPerBatch";
  public static final String GPU_MEM_PERCENT      = "GPU.MemPercent";

  // CPU options
  public static final String CPU_NUM_CORES = "CPU.NumCores";


  // DB options
  public static final String DB_URL          = "DB.Url";
  public static final String DB_USERNAME     = "DB.Username";
  public static final String DB_PASSWORD     = "DB.Password";
  public static final String DB_CONNECTIONS  = "DB.Connections";  
  public static final String DB_LOCK_TIMEOUT = "DB.LockTimeout";

  public static final String DB_TRIM_DERIVED_TABLES = "DB.trimDerivedTables";
  public static final String DB_MAX_ROLLBACK        = "DB.maxRollback";

  public static final String BRS_TEST_UNCONFIRMED_TRANSACTIONS = "brs.testUnconfirmedTransactions";

  public static final String DB_H2_DEFRAG_ON_SHUTDOWN = "Db.H2.DefragOnShutdown";


  public static final String BRS_BLOCK_CACHE_MB = "brs.blockCacheMB";

  // P2P options
  public static final String P2P_REBROADCAST_AFTER = "P2P.rebroadcastTxAfter";
  public static final String P2P_REBROADCAST_EVERY = "P2P.rebroadcastTxEvery";

  public static final String P2P_MY_PLATFORM = "P2P.myPlatform";
  public static final String P2P_MY_ADDRESS  = "P2P.myAddress";
  public static final String P2P_LISTEN      = "P2P.Listen";
  public static final String P2P_PORT        = "P2P.Port";
  public static final String P2P_SHARE_MY_ADDRESS = "P2P.shareMyAddress";
  public static final String P2P_MY_HALLMARK = "P2P.myHallmark";
  public static final String P2P_ENABLE_TX_REBROADCAST = "P2P.enableTxRebroadcast";
  public static final String P2P_REBROADCAST_TO  = "P2P.rebroadcastTo";
  public static final String P2P_BOOTSTRAP_PEERS = "P2P.BootstrapPeers";

  public static final String P2P_NUM_BOOTSTRAP_CONNECTIONS = "P2P.NumBootstrapConnections";
  public static final String P2P_BLACKLISTED_PEERS = "P2P.BlacklistedPeers";
  public static final String P2P_MAX_CONNECTIONS = "P2P.MaxConnections";
  public static final String P2P_TIMEOUT_CONNECT_MS = "P2P.TimeoutConnect_ms";
  public static final String P2P_TIMEOUT_READ_MS = "P2P.TimeoutRead_ms";
  public static final String P2P_HALLMARK_PROTECTION = "P2P.HallmarkProtection";
  public static final String P2P_HALLMARK_PUSH = "P2P.HallmarkPush";
  public static final String P2P_HALLMARK_PULL = "P2P.HallmarkPull";
  public static final String P2P_BLACKLISTING_TIME_MS = "P2P.BlacklistingTime_ms";

  public static final String P2P_TIMEOUT_IDLE_MS = "P2P.TimeoutIdle_ms";

  public static final String P2P_USE_PEERS_DB        = "P2P.usePeersDb";
  public static final String P2P_SAVE_PEERS          = "P2P.savePeers";
  public static final String P2P_GET_MORE_PEERS      = "P2P.getMorePeers";
  public static final String P2P_GET_MORE_PEERS_THRESHOLD = "P2P.getMorePeersThreshold";



  // API options
  public static final String API_DEBUG   = "API.Debug";
  public static final String API_SSL     = "API.SSL";
  public static final String API_SERVER  = "API.Server";
  public static final String API_ALLOWED = "API.allowed";

  public static final String API_LISTEN  = "API.Listen";
  public static final String API_PORT    = "API.Port";

  public static final String API_UI_DIR  = "API.UI_Dir";
  public static final String API_CROSS_ORIGIN_FILTER = "API.CrossOriginFilter";
  public static final String API_SSL_KEY_STORE_PATH     = "API.SSL_keyStorePath";
  public static final String API_SSL_KEY_STORE_PASSWORD = "API.SSL_keyStorePassword";
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

  public static final String JETTY_P2P_GZIP_FILTER               = "JETTY.P2P.GZIPFilter";
  public static final String JETTY_P2P_GZIP_FILTER_METHODS       = "JETTY.P2P.GZIPFilter.methods";
  public static final String JETTY_P2P_GZIP_FILTER_BUFFER_SIZE   = "JETTY.P2P.GZIPFilter.bufferSize";
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

  public static final String TEST_PEERS = "TEST.Peers";

  public static final String BRS_SEND_TO_PEERS_LIMIT = "brs.sendToPeersLimit";

  private Props() { //no need to construct
  }
}
