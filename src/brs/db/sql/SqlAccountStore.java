package brs.db.sql;

import brs.Account;
import brs.Burst;
import brs.db.BurstIterator;
import brs.db.cache.DBCacheManagerImpl;
import brs.db.VersionedBatchEntityTable;
import brs.db.VersionedEntityTable;
import brs.db.store.AccountStore;
import brs.db.store.DerivedTableManager;
import brs.schema.tables.records.AccountRecord;
import brs.util.Convert;
import java.util.stream.Collectors;
import org.jooq.BatchBindStep;
import org.jooq.Cursor;
import org.jooq.Query;
import org.slf4j.LoggerFactory;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import static brs.schema.Tables.*;
import org.jooq.DSLContext;
import org.jooq.SortField;
import org.jooq.Field;
import org.jooq.Condition;

public class SqlAccountStore implements AccountStore {

  protected static final DbKey.LongKeyFactory<Account> accountDbKeyFactory = new DbKey.LongKeyFactory<Account>("id") {
      @Override
      public DbKey newKey(Account account) {
        return (DbKey) account.nxtKey;
      }
    };
  protected static final DbKey.LongKeyFactory<Account.RewardRecipientAssignment> rewardRecipientAssignmentDbKeyFactory
    = new DbKey.LongKeyFactory<Account.RewardRecipientAssignment>("account_id") {
        @Override
        public DbKey newKey(Account.RewardRecipientAssignment assignment) {
          return (DbKey) assignment.burstKey;
        }
      };
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SqlAccountStore.class);
  protected static final DbKey.LinkKeyFactory<Account.AccountAsset> accountAssetDbKeyFactory
    = new DbKey.LinkKeyFactory<Account.AccountAsset>("account_id", "asset_id") {
        @Override
        public DbKey newKey(Account.AccountAsset accountAsset) {
          return (DbKey) accountAsset.burstKey;
        }
    };

  public SqlAccountStore(DerivedTableManager derivedTableManager, DBCacheManagerImpl dbCacheManager) {
    rewardRecipientAssignmentTable = new VersionedEntitySqlTable<Account.RewardRecipientAssignment>("reward_recip_assign", brs.schema.Tables.REWARD_RECIP_ASSIGN, rewardRecipientAssignmentDbKeyFactory, derivedTableManager) {

      @Override
      protected Account.RewardRecipientAssignment load(DSLContext ctx, ResultSet rs) throws SQLException {
        return new SqlRewardRecipientAssignment(rs);
      }

      @Override
      protected void save(DSLContext ctx, Account.RewardRecipientAssignment assignment) throws SQLException {
        brs.schema.tables.records.RewardRecipAssignRecord rewardRecord = ctx.newRecord(brs.schema.Tables.REWARD_RECIP_ASSIGN);
        rewardRecord.setAccountId(assignment.accountId);
        rewardRecord.setPrevRecipId(assignment.getPrevRecipientId());
        rewardRecord.setRecipId(assignment.getRecipientId());
        rewardRecord.setFromHeight(assignment.getFromHeight());
        rewardRecord.setHeight(Burst.getBlockchain().getHeight());
        rewardRecord.setLatest(true);
        DbUtils.mergeInto(
            ctx, rewardRecord, brs.schema.Tables.REWARD_RECIP_ASSIGN,
            ( new Field[] { rewardRecord.field("account_id"), rewardRecord.field("height") } )
        );
      }
    };

    accountAssetTable = new VersionedEntitySqlTable<Account.AccountAsset>("account_asset", brs.schema.Tables.ACCOUNT_ASSET, accountAssetDbKeyFactory, derivedTableManager) {

      @Override
      protected Account.AccountAsset load(DSLContext ctx, ResultSet rs) throws SQLException {
        return new SQLAccountAsset(rs);
      }

      @Override
      protected void save(DSLContext ctx, Account.AccountAsset accountAsset) throws SQLException {
        brs.schema.tables.records.AccountAssetRecord assetRecord = ctx.newRecord(brs.schema.Tables.ACCOUNT_ASSET);
        assetRecord.setAccountId(accountAsset.accountId);
        assetRecord.setAssetId(accountAsset.assetId);
        assetRecord.setQuantity(accountAsset.getQuantityQNT());
        assetRecord.setUnconfirmedQuantity(accountAsset.getUnconfirmedQuantityQNT());
        assetRecord.setHeight(Burst.getBlockchain().getHeight());
        assetRecord.setLatest(true);
        DbUtils.mergeInto(
            ctx, assetRecord, brs.schema.Tables.ACCOUNT_ASSET,
            ( new Field[] { assetRecord.field("account_id"), assetRecord.field("asset_id"), assetRecord.field("height") } )
        );
      }

      @Override
      protected List<SortField> defaultSort() {
        List<SortField> sort = new ArrayList<>();
        sort.add(tableClass.field("quantity", Long.class).desc());
        sort.add(tableClass.field("account_id", Long.class).asc());
        sort.add(tableClass.field("asset_id", Long.class).asc());
        return sort;
      }

    };

    accountTable = new VersionedBatchEntitySqlTable<Account>("account", brs.schema.Tables.ACCOUNT, accountDbKeyFactory, derivedTableManager, dbCacheManager) {
      @Override
      protected Account load(DSLContext ctx, ResultSet rs) throws SQLException {
        return new SqlAccount(rs);
      }

      @Override
      protected void bulkInsert(DSLContext ctx, ArrayList<Account> accounts) {
        if ( ctx.fetchExists(ctx.selectOne().from(ACCOUNT).where(ACCOUNT.HEIGHT.eq(Burst.getBlockchain().getHeight())).limit(1)) ) {
          ArrayList<Query> accountQueries = new ArrayList<Query>();
          for ( Account account: accounts ) {
            accountQueries.add(
              ctx.mergeInto(ACCOUNT, ACCOUNT.ID, ACCOUNT.HEIGHT, ACCOUNT.CREATION_HEIGHT, ACCOUNT.PUBLIC_KEY, ACCOUNT.KEY_HEIGHT, ACCOUNT.BALANCE,
                  ACCOUNT.UNCONFIRMED_BALANCE, ACCOUNT.FORGED_BALANCE, ACCOUNT.NAME, ACCOUNT.DESCRIPTION, ACCOUNT.LATEST)
                  .key(ACCOUNT.ID, ACCOUNT.HEIGHT).values(account.getId(), Burst.getBlockchain().getHeight(), account.getCreationHeight(), account.getPublicKey(), account.getKeyHeight(),
                  account.getBalanceNQT(), account.getUnconfirmedBalanceNQT(), account.getForgedBalanceNQT(), account.getName(), account.getDescription(), true)
            );
          }
          ctx.batch(accountQueries).execute();
        }
        else {
          BatchBindStep insertBatch = ctx.batch(
              ctx.insertInto(ACCOUNT, ACCOUNT.ID, ACCOUNT.HEIGHT, ACCOUNT.CREATION_HEIGHT, ACCOUNT.PUBLIC_KEY, ACCOUNT.KEY_HEIGHT, ACCOUNT.BALANCE,
                  ACCOUNT.UNCONFIRMED_BALANCE, ACCOUNT.FORGED_BALANCE, ACCOUNT.NAME, ACCOUNT.DESCRIPTION, ACCOUNT.LATEST)
                  .values((Long) null, null, null, null, null, null, null, null, null, null, null));
          for (Account account : accounts) {
            DbKey dbKey = (DbKey) accountDbKeyFactory.newKey(account.getId());
            if (!getCache().containsKey(dbKey)) {
              getCache().put(dbKey, account);
            }
            insertBatch.bind(account.getId(), Burst.getBlockchain().getHeight(), account.getCreationHeight(), account.getPublicKey(), account.getKeyHeight(),
                account.getBalanceNQT(), account.getUnconfirmedBalanceNQT(), account.getForgedBalanceNQT(), account.getName(), account.getDescription(), true);
          }
          insertBatch.execute();
        }
      }

      @Override
      public void fillCache(ArrayList<Long> ids) {
        try ( DSLContext ctx = Db.getDSLContext() ) {
          Cursor<AccountRecord> cursor = null;
          try {
            cursor = ctx.selectFrom(brs.schema.Tables.ACCOUNT).where(
                brs.schema.Tables.ACCOUNT.LATEST.isTrue()
            ).and(
                brs.schema.Tables.ACCOUNT.ID
                    .in(ids.stream().distinct().collect(Collectors.toList()))
            ).fetchLazy();

            while (cursor.hasNext()) {
              AccountRecord account = cursor.fetchNext();
              try {
                DbKey dbKey = (DbKey)accountDbKeyFactory.newKey(account.getId());
                getCache().put(dbKey, new SqlAccount(account.intoResultSet()));
              }
              catch ( SQLException e ) {
                // ignore
              }
            }
          }
          finally {
            if (cursor != null) {
              cursor.close();
            }
          }
        }
      }
    };
  }

  private static Condition getAccountsWithRewardRecipientClause(final long id, final int height) {
    return REWARD_RECIP_ASSIGN.RECIP_ID.eq(id).and(REWARD_RECIP_ASSIGN.FROM_HEIGHT.le(height));
  }

  private final VersionedEntityTable<Account.AccountAsset> accountAssetTable;

  VersionedEntityTable<Account.RewardRecipientAssignment> rewardRecipientAssignmentTable;

  VersionedBatchEntityTable<Account> accountTable;

  @Override
  public VersionedBatchEntityTable<Account> getAccountTable() {
    return accountTable;
  }

  @Override
  public VersionedEntityTable<Account.RewardRecipientAssignment> getRewardRecipientAssignmentTable() {
    return rewardRecipientAssignmentTable;
  }

  @Override
  public DbKey.LongKeyFactory<Account.RewardRecipientAssignment> getRewardRecipientAssignmentKeyFactory() {
    return rewardRecipientAssignmentDbKeyFactory;
  }

  @Override
  public DbKey.LinkKeyFactory<Account.AccountAsset> getAccountAssetKeyFactory() {
    return accountAssetDbKeyFactory;
  }

  @Override
  public VersionedEntityTable<Account.AccountAsset> getAccountAssetTable() {
    return accountAssetTable;
  }

  @Override
  public int getAssetAccountsCount(long assetId) {
    DSLContext ctx = Db.getDSLContext();
    return ctx.selectCount().from(ACCOUNT_ASSET).where(ACCOUNT_ASSET.ASSET_ID.eq(assetId)).and(ACCOUNT_ASSET.LATEST.isTrue()).fetchOne(0, int.class);
  }

  @Override
  public DbKey.LongKeyFactory<Account> getAccountKeyFactory() {
    return accountDbKeyFactory;
  }

  @Override
  public BurstIterator<Account.RewardRecipientAssignment> getAccountsWithRewardRecipient(Long recipientId) {
    return getRewardRecipientAssignmentTable().getManyBy(getAccountsWithRewardRecipientClause(recipientId, Burst.getBlockchain().getHeight() + 1), 0, -1);
  }

  @Override
  public BurstIterator<Account.AccountAsset> getAssets(int from, int to, Long id) {
    return getAccountAssetTable().getManyBy(ACCOUNT_ASSET.ACCOUNT_ID.eq(id), from, to);
  }

  @Override
  public BurstIterator<Account.AccountAsset> getAssetAccounts(long assetId, int from, int to) {
    List<SortField> sort = new ArrayList<>();
    sort.add(ACCOUNT_ASSET.field("quantity", Long.class).desc());
    sort.add(ACCOUNT_ASSET.field("account_id", Long.class).asc());
    return getAccountAssetTable().getManyBy(ACCOUNT_ASSET.ASSET_ID.eq(assetId), from, to, sort);
  }

  @Override
  public BurstIterator<Account.AccountAsset> getAssetAccounts(long assetId, int height, int from, int to) {
    if (height < 0) {
      return getAssetAccounts(assetId, from, to);
    }

    List<SortField> sort = new ArrayList<>();
    sort.add(ACCOUNT_ASSET.field("quantity", Long.class).desc());
    sort.add(ACCOUNT_ASSET.field("account_id", Long.class).asc());
    return getAccountAssetTable().getManyBy(ACCOUNT_ASSET.ASSET_ID.eq(assetId), height, from, to, sort);
  }

  @Override
  public boolean setOrVerify(Account acc, byte[] key, int height) {
    if (acc.getPublicKey() == null) {
      if (Db.isInTransaction()) {
        acc.setPublicKey(key);
        acc.setKeyHeight(-1);
        getAccountTable().insert(acc);
      }
      return true;
    } else if (Arrays.equals(acc.getPublicKey(), key)) {
      return true;
    } else if (acc.getKeyHeight() == -1) {
      logger.info("DUPLICATE KEY!!!");
      logger.info("Account key for " + Convert.toUnsignedLong(acc.id) + " was already set to a different one at the same height "
                  + ", current height is " + height + ", rejecting new key");
      return false;
    } else if (acc.getKeyHeight() >= height) {
      logger.info("DUPLICATE KEY!!!");
      if (Db.isInTransaction()) {
        logger.info("Changing key for account " + Convert.toUnsignedLong(acc.id) + " at height " + height
                    + ", was previously set to a different one at height " + acc.getKeyHeight());
        acc.setPublicKey(key);
        acc.setKeyHeight(height);
        getAccountTable().insert(acc);
      }
      return true;
    }
    logger.info("DUPLICATE KEY!!!");
    logger.info("Invalid key for account " + Convert.toUnsignedLong(acc.id) + " at height " + height
                + ", was already set to a different one at height " + acc.getKeyHeight());
    return false;
  }

  protected static class SQLAccountAsset extends Account.AccountAsset {
    public SQLAccountAsset(ResultSet rs) throws SQLException {
      super(rs.getLong("account_id"),
            rs.getLong("asset_id"),
            rs.getLong("quantity"),
            rs.getLong("unconfirmed_quantity"),
            accountAssetDbKeyFactory.newKey(rs.getLong("account_id"), rs.getLong("asset_id"))
            );
    }
  }

  protected class SqlAccount extends Account {
    SqlAccount(Long id) {
      super(id);
    }

    public SqlAccount(ResultSet rs) throws SQLException {
      super(rs.getLong("id"), accountDbKeyFactory.newKey(rs.getLong("id")),
            rs.getInt("creation_height"));
      this.setPublicKey(rs.getBytes("public_key"));
      this.setKeyHeight(rs.getInt("key_height"));
      this.balanceNQT = rs.getLong("balance");
      this.unconfirmedBalanceNQT = rs.getLong("unconfirmed_balance");
      this.forgedBalanceNQT = rs.getLong("forged_balance");
      this.name = rs.getString("name");
      this.description = rs.getString("description");
    }
  }

  protected class SqlRewardRecipientAssignment extends Account.RewardRecipientAssignment {
    public SqlRewardRecipientAssignment(ResultSet rs) throws SQLException {
      super(
            rs.getLong("account_id"),
            rs.getLong("prev_recip_id"),
            rs.getLong("recip_id"),
            (int) rs.getLong("from_height"),
            rewardRecipientAssignmentDbKeyFactory.newKey(rs.getLong("account_id"))
            );
    }
  }


}
