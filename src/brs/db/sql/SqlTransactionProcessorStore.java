package brs.db.sql;

import brs.*;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.store.DerivedTableManager;
import brs.db.store.TransactionProcessorStore;
import brs.services.TimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static brs.schema.Tables.UNCONFIRMED_TRANSACTION;
import org.jooq.DSLContext;
import org.jooq.SortField;

public class SqlTransactionProcessorStore implements TransactionProcessorStore {

  private static final Logger logger = LoggerFactory.getLogger(SqlTransactionProcessorStore.class);
  private final TimeService timeService;

  protected final DbKey.LongKeyFactory<Transaction> unconfirmedTransactionDbKeyFactory = new DbKey.LongKeyFactory<Transaction>("id") {

      @Override
      public BurstKey newKey(Transaction transaction) {
        return transaction.getDbKey();
      }

    };
  private final Set<Transaction> lostTransactions = new HashSet<>();
  private final Map<Long, Integer> lostTransactionHeights = new HashMap<>();

  private final EntitySqlTable<Transaction> unconfirmedTransactionTable;

  @Override
  public boolean hasTransaction(long transactionId) {
    DSLContext ctx = Db.getDSLContext();
    return ctx.fetchExists(ctx.selectFrom(UNCONFIRMED_TRANSACTION).where(UNCONFIRMED_TRANSACTION.ID.eq(transactionId)));
  }

  public SqlTransactionProcessorStore(DerivedTableManager derivedTableManager, TimeService timeService) {
    this.timeService = timeService;

    unconfirmedTransactionTable = new EntitySqlTable<Transaction>("unconfirmed_transaction", brs.schema.Tables.UNCONFIRMED_TRANSACTION, unconfirmedTransactionDbKeyFactory, derivedTableManager) {

      @Override
      protected Transaction load(DSLContext ctx, ResultSet rs) throws SQLException {
        byte[] transactionBytes = rs.getBytes("transaction_bytes");
        try {
          Transaction transaction = Transaction.parseTransaction(transactionBytes);
          transaction.setHeight(rs.getInt("transaction_height"));
          return transaction;
        } catch (BurstException.ValidationException e) {
          throw new RuntimeException(e.toString(), e);
        }
      }

      @Override
      protected void save(DSLContext ctx, Transaction transaction) {
        ctx.insertInto(
            UNCONFIRMED_TRANSACTION,
            UNCONFIRMED_TRANSACTION.ID, UNCONFIRMED_TRANSACTION.TRANSACTION_HEIGHT, UNCONFIRMED_TRANSACTION.FEE_PER_BYTE,
            UNCONFIRMED_TRANSACTION.TIMESTAMP, UNCONFIRMED_TRANSACTION.EXPIRATION, UNCONFIRMED_TRANSACTION.TRANSACTION_BYTES,
            UNCONFIRMED_TRANSACTION.HEIGHT
        ).values(
            transaction.getId(), transaction.getHeight(), transaction.getFeeNQT() / transaction.getSize(),
            transaction.getTimestamp(), transaction.getExpiration(), transaction.getBytes(),
            Burst.getBlockchain().getHeight()
        ).execute();
      }

      @Override
      public void rollback(int height) {
        List<Transaction> transactions = new ArrayList<>();
        try (DSLContext ctx = Db.getDSLContext()) {
          try (ResultSet rs = ctx.selectFrom(UNCONFIRMED_TRANSACTION).where(UNCONFIRMED_TRANSACTION.HEIGHT.gt(height)).fetchResultSet()) {
            while (rs.next()) {
              transactions.add(load(ctx, rs));
            }
          }
        } catch (SQLException e) {
          throw new RuntimeException(e.toString(), e);
        }
        super.rollback(height);
        // WATCH: BUSINESS-LOGIC
        processLater(transactions);
      }

      @Override
      protected List<SortField> defaultSort() {
        List<SortField> sort = new ArrayList<>();
        sort.add(tableClass.field("transaction_height", Integer.class).asc());
        sort.add(tableClass.field("fee_per_byte", Long.class).desc());
        sort.add(tableClass.field("id", Integer.class).asc());
        return sort;
      }
    };
  }


  // WATCH: BUSINESS-LOGIC
  @Override
  public void processLater(Collection<Transaction> transactions) {
    synchronized (Burst.getBlockchain()) {
      lostTransactions.addAll(transactions);
    }
  }

  @Override
  public DbKey.LongKeyFactory<Transaction> getUnconfirmedTransactionDbKeyFactory() {
    return unconfirmedTransactionDbKeyFactory;
  }

  @Override
  public Set<Transaction> getLostTransactions() {
    return lostTransactions;
  }

  @Override
  public Map<Long, Integer> getLostTransactionHeights() {
    return lostTransactionHeights;
  }

  @Override
  public EntitySqlTable<Transaction> getUnconfirmedTransactionTable() {
    return unconfirmedTransactionTable;
  }

  @Override
  public BurstIterator<Transaction> getExpiredTransactions() {
    DSLContext ctx = Db.getDSLContext();
    return unconfirmedTransactionTable.getManyBy(
      ctx,
      ctx.selectFrom(UNCONFIRMED_TRANSACTION).where(UNCONFIRMED_TRANSACTION.EXPIRATION.lt(timeService.getEpochTime())).getQuery(),
      true
    );
  }

  @Override
  public int deleteTransaction(Transaction transaction) {
    DSLContext ctx = Db.getDSLContext();
    return ctx.delete(UNCONFIRMED_TRANSACTION).where(UNCONFIRMED_TRANSACTION.ID.eq(transaction.getId())).execute();
  }
}
