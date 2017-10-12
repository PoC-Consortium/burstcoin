package nxt;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.github.gquintana.metrics.util.SqlObjectNameFactory;
import nxt.db.firebird.FirebirdDbs;
import nxt.db.firebird.FirebirdStores;
import nxt.db.h2.H2Dbs;
import nxt.db.h2.H2Stores;
import nxt.db.mariadb.MariadbDbs;
import nxt.db.mariadb.MariadbStores;
import nxt.db.sql.Db;
import nxt.db.store.Dbs;
import nxt.db.store.Stores;
import nxt.http.API;
import nxt.peer.Peers;
import nxt.user.Users;
import nxt.util.LoggerConfigurator;
import nxt.util.ThreadPool;
import nxt.util.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public final class Nxt {

    public static final String VERSION = "1.3.5cg";
    public static final String APPLICATION = "NRS";
    public static final MetricRegistry metrics = new MetricRegistry();
    private static final Logger logger = LoggerFactory.getLogger(Nxt.class);
    private static final Properties defaultProperties = new Properties();
    private static final Properties properties = new Properties(defaultProperties);
    private static volatile Time time = new Time.EpochTime();
    private static Stores stores;
    private static Dbs dbs;
    private static Generator generator = new GeneratorImpl();

    static {
        System.out.println("Initializing Burst server version " + Nxt.VERSION);
        try (InputStream is = ClassLoader.getSystemResourceAsStream("nxt-default.properties")) {
            if (is != null) {
                Nxt.defaultProperties.load(is);
            } else {
                String configFile = System.getProperty("nxt-default.properties");
                if (configFile != null) {
                    try (InputStream fis = new FileInputStream(configFile)) {
                        Nxt.defaultProperties.load(fis);
                    } catch (IOException e) {
                        throw new RuntimeException("Error loading nxt-default.properties from " + configFile);
                    }
                } else {
                    throw new RuntimeException("nxt-default.properties not in classpath and system property nxt-default.properties not defined either");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading nxt-default.properties", e);
        }
    }

    static {
        try (InputStream is = ClassLoader.getSystemResourceAsStream("nxt.properties")) {
            if (is != null) {
                Nxt.properties.load(is);
            } // ignore if missing
        } catch (IOException e) {
            throw new RuntimeException("Error loading nxt.properties", e);
        }
    }

    private Nxt() {
    } // never

    public static int getIntProperty(String name) {
        try {
            int result = Integer.parseInt(properties.getProperty(name));
            logger.info(name + " = \"" + result + "\"");
            return result;
        } catch (NumberFormatException e) {
            logger.info(name + " not defined, assuming 0");
            return 0;
        }
    }

    public static int getIntProperty(String name,int defaultValue) {
        try {
            int result = Integer.parseInt(properties.getProperty(name));
            logger.info(name + " = \"" + result + "\"");
            return result;
        } catch (NumberFormatException e) {
            logger.info(name + " not defined, assuming ",defaultValue);
            return defaultValue;
        }
    }

    public static String getStringProperty(String name) {
        return getStringProperty(name, null);
    }

    public static String getStringProperty(String name, String defaultValue) {
        String value = properties.getProperty(name);
        if (value != null && !"".equals(value)) {
            if (name != null && name.toLowerCase().contains("password"))
                logger.info(name + " = \"" + value.replaceAll(".", "*") + "\"");
                    else
                logger.info(name + " = \"" + value + "\"");
            return value;
        } else {
            logger.info(name + " not defined");
            return defaultValue;
        }
    }

    public static List<String> getStringListProperty(String name) {
        String value = getStringProperty(name);
        if (value == null || value.length() == 0) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String s : value.split(";")) {
            s = s.trim();
            if (s.length() > 0) {
                result.add(s);
            }
        }
        return result;
    }

    public static Boolean getBooleanProperty(String name) {
        String value = properties.getProperty(name);
        if (Boolean.TRUE.toString().equals(value)) {
            logger.info(name + " = \"true\"");
            return true;
        } else if (Boolean.FALSE.toString().equals(value)) {
            logger.info(name + " = \"false\"");
            return false;
        }
        logger.info(name + " not defined, assuming false");
        return false;
    }

    public static Boolean getBooleanProperty(String name, boolean assume) {
        String value = properties.getProperty(name);
        if (Boolean.TRUE.toString().equals(value)) {
            logger.info(name + " = \"true\"");
            return true;
        } else if (Boolean.FALSE.toString().equals(value)) {
            logger.info(name + " = \"false\"");
            return false;
        }
        logger.info(name + " not defined, assuming " + assume);
        return assume;
    }

    public static Blockchain getBlockchain() {
        return BlockchainImpl.getInstance();
    }

    public static BlockchainProcessor getBlockchainProcessor() {
        return BlockchainProcessorImpl.getInstance();
    }

    public static TransactionProcessor getTransactionProcessor() {
        return TransactionProcessorImpl.getInstance();
    }

    public static Generator getGenerator() {
        return generator;
    }

    public static void setGenerator(Generator newGenerator) {
        generator = newGenerator;
    }

    public static int getEpochTime() {
        return time.getTime();
    }

    static void setTime(Time time) {
        Nxt.time = time;
    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                Nxt.shutdown();
            }
        }));
        init();
    }

    public static void init(Properties customProperties) {
        properties.putAll(customProperties);
        init();
    }

    public static void init() {
        Init.init();
    }

    public static void shutdown() {
        logger.info("Shutting down...");
        API.shutdown();
        Users.shutdown();
        Peers.shutdown();
        ThreadPool.shutdown();
        Db.shutdown();
        if (BlockchainProcessorImpl.oclVerify) {
            OCLPoC.destroy();
        }
        logger.info("Burst server " + VERSION + " stopped.");
        LoggerConfigurator.shutdown();
    }

    public static Stores getStores() {
        return stores;
    }

    public static Dbs getDbs() {
        return dbs;
    }

    private static void initGraphiteReporter()
    {
        logger.info("Initializing graphite Reporter");
        final Graphite graphite = new Graphite(new InetSocketAddress(Nxt.getStringProperty("burst.graphiteHost"), Nxt.getIntProperty("burst.graphitePort")));
        final GraphiteReporter reporter = GraphiteReporter.forRegistry(metrics)
                .prefixedWith("burstWallet")
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(graphite);
        reporter.start(1, TimeUnit.MINUTES);
    }

    private static final String HAWTIO_WAR = "hawtio/hawtio-default-1.5.4.war";

    private static void enableHawtIo() throws Exception {
        if (!Files.exists(Paths.get(HAWTIO_WAR)))
        {
            logger.error("Unable to start hawtio - please make sure to use the right distribution or " +
                    "manually create "+HAWTIO_WAR);
        }
        io.hawt.embedded.Main main = new io.hawt.embedded.Main();
        System.setProperty("hawtio.config.dir", "hawtio/base");
        System.setProperty("hawtio.authenticationEnabled", "true");
        System.setProperty("hawtio.authenticationContainerDiscoveryClasses", "");
        System.setProperty("hawtio.realm", "hawtio");
        System.setProperty("java.security.auth.login.config", "hawtio/login.conf");


        main.setWar(HAWTIO_WAR);
        main.setPort(Nxt.getIntProperty("burst.hawtIoPort", 8124));
        main.run();

    }


    private static class Init {

        static {
            try {
                final JmxReporter reporter = JmxReporter.forRegistry(metrics).createsObjectNamesWith(new SqlObjectNameFactory()).build();
                reporter.start();

                long startTime = System.currentTimeMillis();

                LoggerConfigurator.init();

                String dbUrl;
                if (Constants.isTestnet) {
                    dbUrl = Nxt.getStringProperty("nxt.testDbUrl");
                } else {
                    dbUrl = Nxt.getStringProperty("nxt.dbUrl");
                }

                Db.init();
                switch (Db.getDatabaseType()) {
                    case MARIADB:
                        logger.info("Using mariadb Backend");
                        dbs = new MariadbDbs();
                        stores = new MariadbStores();
                        break;
                    case FIREBIRD:
                        logger.info("Using Firebird Backend");
                        dbs = new FirebirdDbs();
                        stores = new FirebirdStores();
                        break;
                    case H2:
                        logger.info("Using h2 Backend");
                        dbs = new H2Dbs();
                        stores = new H2Stores();
                        break;
                    default:
                        throw new RuntimeException("Error initializing wallet: Unknown database type");
                }
                TransactionProcessorImpl.getInstance();
                BlockchainProcessorImpl.getInstance();

                if (Nxt.getBooleanProperty("burst.enableGraphiteMetrics", false))
                {
                    initGraphiteReporter();
                }

                if (Nxt.getBooleanProperty("burst.enableHawtIo", false))
                {
                    enableHawtIo();
                }

                Account.init();
                Alias.init();
                Asset.init();
                DigitalGoodsStore.init();
                Hub.init();
                Order.init();
                Poll.init();
                Trade.init();
                AssetTransfer.init();
                Vote.init();
                AT.init();
                Peers.init();
                getGenerator().init();
                API.init();
                Users.init();
                DebugTrace.init();
                int timeMultiplier = (Constants.isTestnet && Constants.isOffline) ? Math.max(Nxt.getIntProperty("nxt.timeMultiplier"), 1) : 1;
                ThreadPool.start(timeMultiplier);
                if (timeMultiplier > 1) {
                    setTime(new Time.FasterTime(Math.max(getEpochTime(), Nxt.getBlockchain().getLastBlock().getTimestamp()), timeMultiplier));
                    logger.info("TIME WILL FLOW " + timeMultiplier + " TIMES FASTER!");
                }

                long currentTime = System.currentTimeMillis();
                logger.info("Initialization took " + (currentTime - startTime) / 1000 + " seconds");
                logger.info("Burst server " + VERSION + " started successfully.");
                if (Constants.isTestnet) {
                    logger.info("RUNNING ON TESTNET - DO NOT USE REAL ACCOUNTS!");
                }
                if (Nxt.getBooleanProperty("burst.mockMining")) {
                    setGenerator(new GeneratorImpl.MockGeneratorImpl());
                }
                if (BlockchainProcessorImpl.oclVerify) {
                    try {
                        OCLPoC.init();
                    } catch (OCLPoC.OCLCheckerException e) {
                        logger.error("Error initializing OpenCL, disabling ocl verify: " + e.getMessage());
                        BlockchainProcessorImpl.oclVerify = false;
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                System.exit(1);
            }
        }

        private Init() {
        } // never

        private static void init() {
        }

    }
}
