package brs.http;

import brs.Blockchain;
import brs.BlockchainProcessor;
import brs.Constants;
import brs.EconomicClustering;
import brs.Generator;
import brs.TransactionProcessor;
import brs.common.Props;
import brs.services.*;
import brs.util.Subnet;
import brs.util.ThreadPool;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.servlet.*;
import org.eclipse.jetty.servlets.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class API {

  static Set<Subnet> allowedBotHosts;
  static boolean enableDebugAPI;
  private static final Logger logger = LoggerFactory.getLogger(API.class);
  private static final int TESTNET_API_PORT = 6876;
  private static Server apiServer;

  public API(TransactionProcessor transactionProcessor,
             Blockchain blockchain, BlockchainProcessor blockchainProcessor, ParameterService parameterService,
             AccountService accountService, AliasService aliasService, OrderService orderService,
             AssetService assetService, AssetTransferService assetTransferService,
             TradeService tradeService, EscrowService escrowService, DGSGoodsStoreService digitalGoodsStoreService,
             AssetAccountService assetAccountService, SubscriptionService subscriptionService, ATService atService,
             TimeService timeService, EconomicClustering economicClustering, PropertyService propertyService,
             ThreadPool threadPool, TransactionService transactionService, BlockService blockService,
             Generator generator, APITransactionManager apiTransactionManager) {

    enableDebugAPI = propertyService.getBoolean(Props.API_DEBUG);
    List<String> allowedBotHostsList = propertyService.getStringList(Props.API_ALLOWED);
    if (!allowedBotHostsList.contains("*")) {
      // Temp hashset to store allowed subnets
      Set<Subnet> allowedSubnets = new HashSet<>();

      for (String allowedHost : allowedBotHostsList) {
        try {
          allowedSubnets.add(Subnet.createInstance(allowedHost));
        }
        catch (UnknownHostException e) {
          logger.error("Error adding allowed host/subnet '" + allowedHost + "'", e);
        }
      }
      allowedBotHosts = Collections.unmodifiableSet(allowedSubnets);
    }
    else {
      allowedBotHosts = null;
    }

    boolean enableAPIServer = propertyService.getBoolean(Props.API_SERVER);
    if (enableAPIServer) {
      final String host = propertyService.getString(Props.API_LISTEN);
      final int    port = Constants.isTestnet ? TESTNET_API_PORT : propertyService.getInt(Props.API_PORT);
      apiServer = new Server();
      ServerConnector connector;

      boolean enableSSL = propertyService.getBoolean(Props.API_SSL);
      if (enableSSL) {
        logger.info("Using SSL (https) for the API server");
        HttpConfiguration https_config = new HttpConfiguration();
        https_config.setSecureScheme("https");
        https_config.setSecurePort(port);
        https_config.addCustomizer(new SecureRequestCustomizer());
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(propertyService.getString(Props.API_SSL_KEY_STORE_PATH));
        sslContextFactory.setKeyStorePassword(propertyService.getString(Props.API_SSL_KEY_STORE_PASSWORD));
        sslContextFactory.setExcludeCipherSuites("SSL_RSA_WITH_DES_CBC_SHA",
                                                 "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                                                 "SSL_DHE_DSS_WITH_DES_CBC_SHA",
                                                 "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                                                 "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                                                 "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
                                                 "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");
        sslContextFactory.setExcludeProtocols("SSLv3");
        connector = new ServerConnector(apiServer, new SslConnectionFactory(sslContextFactory, "http/1.1"),
                                        new HttpConnectionFactory(https_config));
      }
      else {
        connector = new ServerConnector(apiServer);
      }

      connector.setHost(host);
      connector.setPort(port);
      connector.setIdleTimeout(propertyService.getInt(Props.API_SERVER_IDLE_TIMEOUT));
      // defaultProtocol
      // stopTimeout
      // acceptQueueSize
      connector.setReuseAddress(true);
      // soLingerTime
      apiServer.addConnector(connector);

      HandlerList apiHandlers = new HandlerList();

      ServletContextHandler apiHandler = new ServletContextHandler();
      String apiResourceBase = propertyService.getString(Props.API_UI_DIR);
      if (apiResourceBase != null) {
        ServletHolder defaultServletHolder = new ServletHolder(new DefaultServlet());
        defaultServletHolder.setInitParameter("dirAllowed", "false");
        defaultServletHolder.setInitParameter("resourceBase", apiResourceBase);
        defaultServletHolder.setInitParameter("welcomeServlets", "true");
        defaultServletHolder.setInitParameter("redirectWelcome", "true");
        defaultServletHolder.setInitParameter("gzip", "true");
        apiHandler.addServlet(defaultServletHolder, "/*");
        apiHandler.setWelcomeFiles(new String[]{"index.html"});
      }

      String javadocResourceBase = propertyService.getString(Props.API_DOC_DIR);
      if (javadocResourceBase != null) {
        ContextHandler contextHandler  = new ContextHandler("/doc");
        ResourceHandler docFileHandler = new ResourceHandler();
        docFileHandler.setDirectoriesListed(false);
        docFileHandler.setWelcomeFiles(new String[]{"index.html"});
        docFileHandler.setResourceBase(javadocResourceBase);
        contextHandler.setHandler(docFileHandler);
        apiHandlers.addHandler(contextHandler);
      }

      ServletHolder peerServletHolder = new ServletHolder(new APIServlet(transactionProcessor, blockchain, blockchainProcessor, parameterService,
                                                                         accountService, aliasService, orderService, assetService, assetTransferService,
                                                                         tradeService, escrowService, digitalGoodsStoreService, assetAccountService,
                                                                         subscriptionService, atService, timeService, economicClustering, transactionService, blockService, generator, propertyService, apiTransactionManager));
      apiHandler.addServlet(peerServletHolder, "/burst");

      if (propertyService.getBoolean("JETTY.API.GzipFilter")) {
        FilterHolder gzipFilterHolder = apiHandler.addFilter(GzipFilter.class, "/burst", null);
        gzipFilterHolder.setInitParameter("methods",     propertyService.getString(Props.JETTY_API_GZIP_FILTER_METHODS));
        gzipFilterHolder.setInitParameter("bufferSize",  propertyService.getString(Props.JETTY_API_GZIP_FILTER_BUFFER_SIZE));
        gzipFilterHolder.setInitParameter("minGzipSize", propertyService.getString(Props.JETTY_API_GZIP_FILTER_MIN_GZIP_SIZE));
        gzipFilterHolder.setAsyncSupported(true);
      }
      
      if (propertyService.getBoolean("JETTY.API.DoSFilter")) {
        FilterHolder dosFilterHolder = apiHandler.addFilter(DoSFilter.class, "/burst", null);
        dosFilterHolder.setInitParameter("maxRequestsPerSec", propertyService.getString(Props.JETTY_API_DOS_FILTER_MAX_REQUEST_PER_SEC));
        dosFilterHolder.setInitParameter("throttledRequests", propertyService.getString(Props.JETTY_API_DOS_FILTER_THROTTLED_REQUESTS));
        dosFilterHolder.setInitParameter("delayMs",           propertyService.getString(Props.JETTY_API_DOS_FILTER_DELAY_MS));
        dosFilterHolder.setInitParameter("maxWaitMs",         propertyService.getString(Props.JETTY_API_DOS_FILTER_MAX_WAIT_MS));
        dosFilterHolder.setInitParameter("maxRequestMs",      propertyService.getString(Props.JETTY_API_DOS_FILTER_MAX_REQUEST_MS));
        dosFilterHolder.setInitParameter("maxthrottleMs",     propertyService.getString(Props.JETTY_API_DOS_FILTER_THROTTLE_MS));
        dosFilterHolder.setInitParameter("maxIdleTrackerMs",  propertyService.getString(Props.JETTY_API_DOS_FILTER_MAX_IDLE_TRACKER_MS));
        dosFilterHolder.setInitParameter("trackSessions",     propertyService.getString(Props.JETTY_API_DOS_FILTER_TRACK_SESSIONS));
        dosFilterHolder.setInitParameter("insertHeaders",     propertyService.getString(Props.JETTY_API_DOS_FILTER_INSERT_HEADERS));
        dosFilterHolder.setInitParameter("remotePort",        propertyService.getString(Props.JETTY_API_DOS_FILTER_REMOTE_PORT));
        dosFilterHolder.setInitParameter("ipWhitelist",       propertyService.getString(Props.JETTY_API_DOS_FILTER_IP_WHITELIST));
        dosFilterHolder.setInitParameter("managedAttr",       propertyService.getString(Props.JETTY_API_DOS_FILTER_MANAGED_ATTR));
        dosFilterHolder.setAsyncSupported(true);
      }

      apiHandler.addServlet(APITestServlet.class, "/test");

      if (propertyService.getBoolean(Props.API_CROSS_ORIGIN_FILTER)) {
        FilterHolder filterHolder = apiHandler.addFilter(CrossOriginFilter.class, "/*", null);
        filterHolder.setInitParameter("allowedHeaders", "*");
        filterHolder.setAsyncSupported(true);
      }

      apiHandlers.addHandler(apiHandler);
      apiHandlers.addHandler(new DefaultHandler());

      apiServer.setHandler(apiHandlers);
      apiServer.setStopAtShutdown(true);

      threadPool.runBeforeStart(new Runnable() {
          @Override
          public void run() {
            try {
              apiServer.start();
              logger.info("Started API server at " + host + ":" + port);
            } catch (Exception e) {
              logger.error("Failed to start API server", e);
              throw new RuntimeException(e.toString(), e);
            }

          }
        }, true);

    } else {
      apiServer = null;
      logger.info("API server not enabled");
    }

  }

  public void shutdown() {
    if (apiServer != null) {
      try {
        apiServer.stop();
      } catch (Exception e) {
        logger.info("Failed to stop API server", e);
      }
    }
  }

}
