package brs.db.store;

import brs.Transaction;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.sql.EntitySqlTable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface TransactionProcessorStore {
  // WATCH: BUSINESS-LOGIC
  void processLater(Collection<Transaction> transactions);

  BurstKey.LongKeyFactory<Transaction> getUnconfirmedTransactionDbKeyFactory();

  Set<Transaction> getLostTransactions();

  Map<Long, Integer> getLostTransactionHeights();

  EntitySqlTable<Transaction> getUnconfirmedTransactionTable();

  BurstIterator<Transaction> getExpiredTransactions();

  int deleteTransaction (Transaction transaction);

  boolean hasTransaction(long transactionId);
}
