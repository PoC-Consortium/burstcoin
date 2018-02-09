package brs;

import brs.AT.HandleATBlockTransactionsListener;
import brs.blockchainlistener.DevNullListener;
import brs.common.Props;
import brs.db.BlockDb;
import brs.db.BurstKey;
import brs.db.EntityTable;
import brs.db.sql.Db;
import brs.db.store.BlockchainStore;
import brs.db.store.Dbs;
import brs.db.store.Stores;
import brs.http.API;
import brs.peer.Peers;
import brs.services.ATService;
import brs.services.AccountService;
import brs.services.AliasService;
import brs.services.AssetAccountService;
import brs.services.AssetService;
import brs.services.AssetTransferService;
import brs.services.DGSGoodsStoreService;
import brs.services.EscrowService;
import brs.services.OrderService;
import brs.services.ParameterService;
import brs.services.PropertyService;
import brs.services.SubscriptionService;
import brs.services.TimeService;
import brs.services.TradeService;
import brs.services.impl.ATServiceImpl;
import brs.services.impl.AccountServiceImpl;
import brs.services.impl.AliasServiceImpl;
import brs.services.impl.AssetAccountServiceImpl;
import brs.services.impl.AssetServiceImpl;
import brs.services.impl.AssetTransferServiceImpl;
import brs.services.impl.DGSGoodsStoreServiceImpl;
import brs.services.impl.EscrowServiceImpl;
import brs.services.impl.OrderServiceImpl;
import brs.services.impl.ParameterServiceImpl;
import brs.services.impl.PropertyServiceImpl;
import brs.services.impl.SubscriptionServiceImpl;
import brs.services.impl.TimeServiceImpl;
import brs.services.impl.TradeServiceImpl;
import brs.user.Users;
import brs.util.LoggerConfigurator;
import brs.util.ThreadPool;
import brs.util.Time;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Burst {

  public static final String VERSION = "1.9.1";
  public static final String APPLICATION = "BRS";

  private static final String DEFAULT_PROPERTIES_NAME = "brs-default.properties";

  private static final Logger logger = LoggerFactory.getLogger(Burst.class);
  private static Properties properties;
  private static volatile Time time = new Time.EpochTime();
  private static Stores stores;
  private static Dbs dbs;
  private static Generator generator;

  private static ThreadPool threadPool;

  private static BlockchainImpl blockchain;
  private static BlockchainProcessorImpl blockchainProcessor;
  private static TransactionProcessorImpl transactionProcessor;

  private static PropertyService propertyService;
  private static EconomicClustering economicClustering;
  // Temporary until BlockchainProcessorImpl is refactored
  private static boolean readPropertiesSuccessfully = false;

  private static API api;
  private static Users users;

  private static PropertyService loadProperties() {
    final Properties defaultProperties = new Properties();

    logger.info("Initializing Burst Reference Software (BRS) version {}", VERSION);
    try (InputStream is = ClassLoader.getSystemResourceAsStream(DEFAULT_PROPERTIES_NAME)) {
      if (is != null) {
        defaultProperties.load(is);
      } else {
        String configFile = System.getProperty(DEFAULT_PROPERTIES_NAME);

        if (configFile != null) {
          try (InputStream fis = new FileInputStream(configFile)) {
            defaultProperties.load(fis);
          } catch (IOException e) {
            throw new RuntimeException("Error loading " + DEFAULT_PROPERTIES_NAME + " from " + configFile);
          }
        } else {
          throw new RuntimeException(DEFAULT_PROPERTIES_NAME + " not in classpath and system property " + DEFAULT_PROPERTIES_NAME + " not defined either");
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Error loading " + DEFAULT_PROPERTIES_NAME, e);
    }

    try (InputStream is = ClassLoader.getSystemResourceAsStream("brs.properties")) {
      properties = new Properties(defaultProperties);
      if (is != null) { // parse if brs.properties was loaded
        properties.load(is);
      }
    } catch (IOException e) {
      throw new RuntimeException("Error loading brs.properties", e);
    }

    readPropertiesSuccessfully = true;
    return new PropertyServiceImpl(properties);
  }

  private Burst() {
  } // never

  public static BlockchainImpl getBlockchain() {
    return blockchain;
  }

  public static BlockchainProcessor getBlockchainProcessor() {
    return blockchainProcessor;
  }

  public static TransactionProcessorImpl getTransactionProcessor() {
    return transactionProcessor;
  }

  public static Stores getStores() {
    return stores;
  }

  public static Dbs getDbs() {
    return dbs;
  }

  public static Generator getGenerator() {
    return generator;
  }

  public static int getEpochTime() {
    return time.getTime();
  }

  static void setTime(Time t) {
    time = t;
  }

  public static void main(String[] args) {
    Runtime.getRuntime().addShutdownHook(new Thread(Burst::shutdown));
    init();
  }

  public static void init(Properties customProperties) {
    properties.putAll(customProperties);
    init();
  }

  public static void init() {
    try {
      long startTime = System.currentTimeMillis();

      propertyService = loadProperties();

      threadPool = new ThreadPool();

      LoggerConfigurator.init();

      Db.init(propertyService);
      dbs = Db.getDbsByDatabaseType();

      blockchainProcessor = BlockchainProcessorImpl.getInstance();

      stores = new Stores();

      final TransactionDb transactionDb = Burst.getDbs().getTransactionDb();
      final BlockDb blockDb =  Burst.getDbs().getBlockDb();
      final BlockchainStore blockchainStore = Burst.getStores().getBlockchainStore();
      blockchain = new BlockchainImpl(transactionDb, blockDb, blockchainStore);

      economicClustering = new EconomicClustering(blockchain);

      final BurstKey.LongKeyFactory<TransactionImpl> unconfirmedTransactionDbKeyFactory =
          Burst.getStores().getTransactionProcessorStore().getUnconfirmedTransactionDbKeyFactory();


      final EntityTable<TransactionImpl> unconfirmedTransactionTable =
          Burst.getStores().getTransactionProcessorStore().getUnconfirmedTransactionTable();

      final TimeService timeService = new TimeServiceImpl();

      final AccountService accountService = new AccountServiceImpl(stores.getAccountStore(), stores.getAssetTransferStore());
      transactionProcessor = new TransactionProcessorImpl(unconfirmedTransactionDbKeyFactory, unconfirmedTransactionTable, propertyService, economicClustering, blockchain, stores, timeService, dbs, accountService, threadPool);

      final ATService atService = new ATServiceImpl(Burst.getStores().getAtStore());
      final SubscriptionService subscriptionService = new SubscriptionServiceImpl(Burst.getStores().getSubscriptionStore());
      final DGSGoodsStoreService digitalGoodsStoreService = new DGSGoodsStoreServiceImpl(blockchain, Burst.getStores().getDigitalGoodsStoreStore(), accountService);
      final EscrowService escrowService = new EscrowServiceImpl(Burst.getStores().getEscrowStore());
      final TradeService tradeService = new TradeServiceImpl(Burst.getStores().getTradeStore());
      final AssetAccountService assetAccountService = new AssetAccountServiceImpl(stores.getAccountStore());
      final AssetTransferService assetTransferService = new AssetTransferServiceImpl(stores.getAssetTransferStore());
      final AliasService aliasService = new AliasServiceImpl(stores.getAliasStore());
      final AssetService assetService = new AssetServiceImpl(assetAccountService, tradeService, stores.getAssetStore(), assetTransferService);
      final ParameterService parameterService = new ParameterServiceImpl(accountService, aliasService, assetService,
          digitalGoodsStoreService, blockchain, blockchainProcessor, getTransactionProcessor(), atService);
      final OrderService orderService = new OrderServiceImpl(stores.getOrderStore(), accountService, tradeService);

      addBlockchainListeners(blockchainProcessor, accountService, digitalGoodsStoreService, blockchain, Burst.getDbs().getTransactionDb());

      Peers.init(timeService, accountService, blockchain, transactionProcessor, blockchainProcessor, propertyService);

      // TODO this really should be better...
      TransactionType.init(blockchain, accountService, digitalGoodsStoreService, aliasService, assetService, orderService, assetTransferService);

      api = new API(transactionProcessor, blockchain, blockchainProcessor, parameterService,
          accountService, aliasService, orderService, assetService, assetTransferService,
          tradeService, escrowService, digitalGoodsStoreService, assetAccountService,
          subscriptionService, atService, timeService, economicClustering, propertyService, threadPool);

      users = new Users(propertyService);
      DebugTrace.init(propertyService, blockchainProcessor, tradeService, orderService, digitalGoodsStoreService);

      if (propertyService.getBooleanProperty("brs.mockMining")) {
        generator = new GeneratorImpl.MockGeneratorImpl(blockchainProcessor);
      } else {
        generator = new GeneratorImpl(blockchainProcessor, blockchain, timeService, threadPool);
      }

      int timeMultiplier = (Constants.isTestnet && Constants.isOffline) ? Math.max(propertyService.getIntProperty(Props.TIME_MULTIPLIER), 1) : 1;

      threadPool.start(timeMultiplier);
      if (timeMultiplier > 1) {
        setTime(new Time.FasterTime(Math.max(getEpochTime(), getBlockchain().getLastBlock().getTimestamp()), timeMultiplier));
        logger.info("TIME WILL FLOW " + timeMultiplier + " TIMES FASTER!");
      }

      long currentTime = System.currentTimeMillis();
      logger.info("Initialization took " + (currentTime - startTime) + " ms");
      logger.info("BRS " + VERSION + " started successfully.");

      if (Constants.isTestnet) {
        logger.info("RUNNING ON TESTNET - DO NOT USE REAL ACCOUNTS!");
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      System.exit(1);
    }
  }

  private static void addBlockchainListeners(BlockchainProcessor blockchainProcessor, AccountService accountService, DGSGoodsStoreService goodsService, Blockchain blockchain,
      TransactionDb transactionDb) {
    final HandleATBlockTransactionsListener handleATBlockTransactionListener = new HandleATBlockTransactionsListener(accountService, blockchain, transactionDb);
    final DevNullListener devNullListener = new DevNullListener(accountService, goodsService);

    blockchainProcessor.addListener(handleATBlockTransactionListener, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    blockchainProcessor.addListener(devNullListener, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
  }

  public static void shutdown() {
    logger.info("Shutting down...");
    api.shutdown();
    users.shutdown();
    Peers.shutdown();
    threadPool.shutdown();
    Db.shutdown();
    if (readPropertiesSuccessfully && BlockchainProcessorImpl.getOclVerify()) {
      OCLPoC.destroy();
    }
    logger.info("BRS " + VERSION + " stopped.");
    LoggerConfigurator.shutdown();
  }

  public static Boolean getBooleanProperty(String name, boolean assume) {
    return propertyService.getBooleanProperty(name, assume);
  }

  public static Boolean getBooleanProperty(String name) {
    return propertyService.getBooleanProperty(name);
  }

  public static int getIntProperty(String name, int defaultValue) {
    return propertyService.getIntProperty(name, defaultValue);
  }

  public static int getIntProperty(String name) {
    return propertyService.getIntProperty(name);
  }

  public static PropertyService getPropertyService() {
    return propertyService;
  }

  public static EconomicClustering getEconomicClustering() {
    return economicClustering;
  }

  public static ThreadPool getThreadPool() {
    return threadPool;
  }
}
