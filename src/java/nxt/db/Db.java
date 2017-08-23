package nxt.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import nxt.Constants;
import nxt.Nxt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

// import org.h2.jdbcx.JdbcConnectionPool;

public final class Db {

    private static final Logger logger = LoggerFactory.getLogger(Db.class);

    // private static final JdbcConnectionPool cp;
    private static final HikariDataSource cp;
    private static volatile int maxActiveConnections;

    private static final ThreadLocal<DbConnection> localConnection = new ThreadLocal<>();
    private static final ThreadLocal<Map<String,Map<DbKey,Object>>> transactionCaches = new ThreadLocal<>();
    private static final ThreadLocal<Map<String,Map<DbKey,Object>>> transactionBatches = new ThreadLocal<>();

    private static final class DbConnection extends FilteredConnection {

        private DbConnection(Connection con) {
            super(con);
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            throw new UnsupportedOperationException("Use Db.beginTransaction() to start a new transaction");
        }

        @Override
        public void commit() throws SQLException {
            if (localConnection.get() == null) {
                super.commit();
            } else if (! this.equals(localConnection.get())) {
                throw new IllegalStateException("Previous connection not committed");
            } else {
                throw new UnsupportedOperationException("Use Db.commitTransaction() to commit the transaction");
            }
        }

        private void doCommit() throws SQLException {
            super.commit();
        }

        @Override
        public void rollback() throws SQLException {
            if (localConnection.get() == null) {
                super.rollback();
            } else if (! this.equals(localConnection.get())) {
                throw new IllegalStateException("Previous connection not committed");
            } else {
                throw new UnsupportedOperationException("Use Db.rollbackTransaction() to rollback the transaction");
            }
        }

        private void doRollback() throws SQLException {
            super.rollback();
        }

        @Override
        public void close() throws SQLException {
            if (localConnection.get() == null) {
                super.close();
            } else if (! this.equals(localConnection.get())) {
                throw new IllegalStateException("Previous connection not committed");
            }
        }

    }

    public static void init() {}

    static {
        String dbUrl;
        String dbUsername;
        String dbPassword;
        if ( Constants.isTestnet ) {
            dbUrl = Nxt.getStringProperty("nxt.testDbUrl");
            dbUsername = Nxt.getStringProperty("nxt.testDbUsername");
            dbPassword = Nxt.getStringProperty("nxt.testDbPassword");
        }
        else {
            dbUrl = Nxt.getStringProperty("nxt.dbUrl");
            dbUsername = Nxt.getStringProperty("nxt.dbUsername");
            dbPassword = Nxt.getStringProperty("nxt.dbPassword");
        }

        logger.debug("Database jdbc url set to: " + dbUrl);
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            if ( dbUsername != null )
                config.setUsername(dbUsername);
            if ( dbPassword != null )
                config.setPassword(dbPassword);

            config.setMaximumPoolSize(10);
            config.setAutoCommit(true);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("characterEncoding","utf8mb4");
            config.addDataSourceProperty("useUnicode","true");
            config.setConnectionInitSql("SET NAMES utf8mb4;");

            cp = new HikariDataSource(config);
        } catch (Exception e) {
             throw new RuntimeException(e.toString(), e);
        }
    }

    public static void analyzeTables() {
        /*        try (Connection con = cp.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute("ANALYZE SAMPLE_SIZE 0");
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
            }*/
    }

    public static void shutdown() {
        try {
            Connection con = cp.getConnection();
            //Statement stmt = con.createStatement();
            // stmt.execute("SHUTDOWN COMPACT");
            logger.info("Database shutdown completed");
        } catch (SQLException e) {
            logger.info(e.toString(), e);
        }
    }

    private static Connection getPooledConnection() throws SQLException {
        Connection con = cp.getConnection();
        /*
        int activeConnections = cp.getActiveConnections();
        if (activeConnections > maxActiveConnections) {
            maxActiveConnections = activeConnections;
            logger.debug("Database connection pool current size: " + activeConnections);
        }
        */
        return con;
    }

    public static Connection getConnection() throws SQLException {
        Connection con = localConnection.get();
        if (con != null) {
            return con;
        }
        con = getPooledConnection();
        //con.setAutoCommit(true);
        return new DbConnection(con);
    }

    static Map<DbKey,Object> getCache(String tableName) {
        if (!isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        Map<DbKey,Object> cacheMap = transactionCaches.get().get(tableName);
        if (cacheMap == null) {
            cacheMap = new HashMap<>();
            transactionCaches.get().put(tableName, cacheMap);
        }
        return cacheMap;
    }

    static Map<DbKey,Object> getBatch(String tableName) {
        if(!isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        Map<DbKey,Object> batchMap = transactionBatches.get().get(tableName);
        if(batchMap == null) {
            batchMap = new HashMap<>();
            transactionBatches.get().put(tableName, batchMap);
        }
        return batchMap;
    }

    public static boolean isInTransaction() {
        return localConnection.get() != null;
    }

    public static Connection beginTransaction() {
        if (localConnection.get() != null) {
            throw new IllegalStateException("Transaction already in progress");
        }
        try {
            Connection con = getPooledConnection();
            //con.setAutoCommit(false);
            con = new DbConnection(con);
            localConnection.set((DbConnection)con);
            transactionCaches.set(new HashMap<String, Map<DbKey, Object>>());
            transactionBatches.set(new HashMap<String, Map<DbKey, Object>>());
            return con;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static void commitTransaction() {
        DbConnection con = localConnection.get();
        if (con == null) {
            throw new IllegalStateException("Not in transaction");
        }
        try {
            con.doCommit();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static void rollbackTransaction() {
        DbConnection con = localConnection.get();
        if (con == null) {
            throw new IllegalStateException("Not in transaction");
        }
        try {
            con.doRollback();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        transactionCaches.get().clear();
        transactionBatches.get().clear();
    }

    public static void endTransaction() {
        Connection con = localConnection.get();
        if (con == null) {
            throw new IllegalStateException("Not in transaction");
        }
        localConnection.set(null);
        transactionCaches.get().clear();
        transactionCaches.set(null);
        transactionBatches.get().clear();
        transactionBatches.set(null);
        DbUtils.close(con);
    }

    private Db() {} // never

}
