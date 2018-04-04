package brs.db.sql;

import brs.*;
import brs.db.BlockDb;
import brs.db.BurstIterator;
import brs.db.store.BlockchainStore;
import brs.schema.tables.records.BlockRecord;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Condition;
import org.jooq.SelectConditionStep;

import static brs.schema.Tables.BLOCK;
import static brs.schema.Tables.TRANSACTION;

public class SqlBlockchainStore implements BlockchainStore {

  private final TransactionDb transactionDb = Burst.getDbs().getTransactionDb();
  private final BlockDb blockDb = Burst.getDbs().getBlockDb();

  @Override
  public BurstIterator<Block> getBlocks(int from, int to) {
    try ( DSLContext ctx = Db.getDSLContext() ) {
      int blockchainHeight = Burst.getBlockchain().getHeight();
      return
        getBlocks(
          ctx,
          ctx.selectFrom(BLOCK).where(
            BLOCK.HEIGHT.between(blockchainHeight - Math.max(from, 0)).and(to > 0 ? blockchainHeight - to : 0)
          ).orderBy(BLOCK.HEIGHT.desc()).fetchResultSet()
        );
    }
    catch ( Exception e ) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  @Override
  public BurstIterator<Block> getBlocks(Account account, int timestamp, int from, int to) {
    try ( DSLContext ctx = Db.getDSLContext() ) {
      int blockchainHeight      = Burst.getBlockchain().getHeight();
      SelectConditionStep query = ctx.selectFrom(BLOCK).where(BLOCK.GENERATOR_ID.eq(account.getId()));
      if ( timestamp > 0 ) {
        query = query.and(BLOCK.TIMESTAMP.ge(timestamp));
      }
      // DbUtils.limitsClause(from, to)))
      return
        getBlocks(
          ctx,
          query.orderBy(BLOCK.HEIGHT.desc()).fetchResultSet()
        );
    }
    catch ( Exception e ) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  @Override
  public BurstIterator<Block> getBlocks(DSLContext ctx, ResultSet rs) {
    return new DbIterator<>(ctx, rs, blockDb::loadBlock);
  }

  @Override
  public List<Long> getBlockIdsAfter(long blockId, int limit) {
    if (limit > 1440) {
      throw new IllegalArgumentException("Can't get more than 1440 blocks at a time");
    }

    try ( DSLContext ctx = Db.getDSLContext() ) {
      return
        ctx.selectFrom(BLOCK).where(
          BLOCK.HEIGHT.gt( ctx.select(BLOCK.HEIGHT).from(BLOCK).where(BLOCK.ID.eq(blockId) ) )
        ).orderBy(BLOCK.HEIGHT.asc()).limit(limit).fetch(BLOCK.ID, Long.class);
    }
    catch ( Exception e ) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  @Override
  public List<Block> getBlocksAfter(long blockId, int limit) {
    if (limit > 1440) {
      throw new IllegalArgumentException("Can't get more than 1440 blocks at a time");
    }

      try ( DSLContext ctx = Db.getDSLContext() ) {
        List<Block> blocksAfter = new ArrayList<Block>();
        try (Cursor<BlockRecord> cursor = ctx.selectFrom(BLOCK).where(BLOCK.HEIGHT.gt( ctx.select(BLOCK.HEIGHT).from(BLOCK).where(BLOCK.ID.eq(blockId)))).orderBy(BLOCK.HEIGHT.asc()).limit(limit).fetchLazy()) {
          while (cursor.hasNext()) {
            blocksAfter.add(blockDb.loadBlock(cursor.fetchNext()));
          }
        }
        return blocksAfter;
    }
    catch ( Exception e ) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  @Override
  public int getTransactionCount() {
    DSLContext ctx = Db.getDSLContext();
    return ctx.selectCount().from(TRANSACTION).fetchOne(0, int.class);
  }

  @Override
  public BurstIterator<Transaction> getAllTransactions() {
    DSLContext ctx = Db.getDSLContext();
    return getTransactions(
      ctx,
      ctx.selectFrom(TRANSACTION).orderBy(TRANSACTION.DB_ID.asc()).fetchResultSet()
    );
  }


  @Override
  public BurstIterator<Transaction> getTransactions(Account account, int numberOfConfirmations, byte type, byte subtype,
                                                        int blockTimestamp, int from, int to) {
    int height = numberOfConfirmations > 0 ? Burst.getBlockchain().getHeight() - numberOfConfirmations : Integer.MAX_VALUE;
    if (height < 0) {
      throw new IllegalArgumentException("Number of confirmations required " + numberOfConfirmations
                                         + " exceeds current blockchain height " + Burst.getBlockchain().getHeight());
    }
    DSLContext ctx = Db.getDSLContext();
    ArrayList<Condition> conditions = new ArrayList<>();
    if (blockTimestamp > 0) {
      conditions.add(TRANSACTION.BLOCK_TIMESTAMP.ge(blockTimestamp));
    }
    if (type >= 0) {
      conditions.add(TRANSACTION.TYPE.eq(type));
      if (subtype >= 0) {
        conditions.add(TRANSACTION.SUBTYPE.eq(subtype));
      }
    }
    if (height < Integer.MAX_VALUE) {
      conditions.add(TRANSACTION.HEIGHT.le(height));
    }
    return getTransactions(
      ctx,
      ctx.selectFrom(TRANSACTION).where(conditions).and(
        TRANSACTION.RECIPIENT_ID.eq(account.getId()).and(
          TRANSACTION.SENDER_ID.ne(account.getId())
        )
      )
      .unionAll(
        ctx.selectFrom(TRANSACTION).where(conditions).and(
          TRANSACTION.SENDER_ID.eq(account.getId())
        )
      )
      .orderBy(TRANSACTION.BLOCK_TIMESTAMP.desc(), TRANSACTION.ID.desc())
      .limit(from, to - from + 1)
      .fetchResultSet()
    );
  }

  @Override
  public BurstIterator<Transaction> getTransactions(DSLContext ctx, ResultSet rs) {
    return new DbIterator<>(ctx, rs, transactionDb::loadTransaction);
  }

  @Override
  public boolean addBlock(Block block) {
    DSLContext ctx = Db.getDSLContext();
    blockDb.saveBlock(ctx, block);
    return true;
  }

  public void scan(int height)
  {
  }
}
