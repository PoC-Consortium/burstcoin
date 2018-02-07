package brs.db.sql;

import brs.Alias;
import brs.Burst;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;
import brs.db.store.AliasStore;
import java.util.ArrayList;
import java.util.List;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.jooq.DSLContext;
import org.jooq.SortField;

import static brs.schema.Tables.ALIAS;
import static brs.schema.Tables.ALIAS_OFFER;

public class SqlAliasStore implements AliasStore {

  private static final DbKey.LongKeyFactory<Alias.Offer> offerDbKeyFactory = new DbKey.LongKeyFactory<Alias.Offer>("id") {
      @Override
      public BurstKey newKey(Alias.Offer offer) {
        return offer.dbKey;
      }
    };

  @Override
  public BurstKey.LongKeyFactory<Alias.Offer> getOfferDbKeyFactory() {
    return offerDbKeyFactory;
  }

  private static final BurstKey.LongKeyFactory<Alias> aliasDbKeyFactory = new DbKey.LongKeyFactory<Alias>("id") {

      @Override
      public BurstKey newKey(Alias alias) {
        return alias.dbKey;
      }
    };

  @Override
  public BurstKey.LongKeyFactory<Alias> getAliasDbKeyFactory() {
    return aliasDbKeyFactory;
  }

  @Override
  public VersionedEntityTable<Alias> getAliasTable() {
    return aliasTable;
  }

  private class SqlOffer extends Alias.Offer {
    private SqlOffer(ResultSet rs) throws SQLException {
      super(rs.getLong("id"), rs.getLong("price"), rs.getLong("buyer_id"), offerDbKeyFactory.newKey(rs.getLong("id")));
    }
  }

  protected void saveOffer(Alias.Offer offer) throws SQLException {
    try (DSLContext ctx = Db.getDSLContext()) {
      ctx.insertInto(
        ALIAS_OFFER,
        ALIAS_OFFER.ID, ALIAS_OFFER.PRICE, ALIAS_OFFER.BUYER_ID, ALIAS_OFFER.HEIGHT
      ).values(
        offer.getId(), offer.getPriceNQT(), ( offer.getBuyerId() == 0 ? null : offer.getBuyerId() ), Burst.getBlockchain().getHeight()
      ).execute();
    }
  }

  private final VersionedEntityTable<Alias.Offer> offerTable = new VersionedEntitySqlTable<Alias.Offer>("alias_offer", ALIAS_OFFER, offerDbKeyFactory) {
      @Override
      protected Alias.Offer load(DSLContext ctx, ResultSet rs) throws SQLException {
        return new SqlOffer(rs);
      }

      @Override
      protected void save(DSLContext ctx, Alias.Offer offer) throws SQLException {
        saveOffer(offer);
      }
    };

  @Override
  public VersionedEntityTable<Alias.Offer> getOfferTable() {
    return offerTable;
  }

  private class SqlAlias extends Alias {
    private SqlAlias(ResultSet rs) throws SQLException {
      super(
            rs.getLong("id"),
            rs.getLong("account_id"),
            rs.getString("alias_name"),
            rs.getString("alias_uri"),
            rs.getInt("timestamp"),
            aliasDbKeyFactory.newKey(rs.getLong("id"))
            );
    }
  }

  protected void saveAlias(DSLContext ctx, Alias alias) {
    ctx.insertInto(ALIAS).
      set(ALIAS.ID, alias.getId()).
      set(ALIAS.ACCOUNT_ID, alias.getAccountId()).
      set(ALIAS.ALIAS_NAME, alias.getAliasName()).
      set(ALIAS.ALIAS_NAME_LOWER, alias.getAliasName().toLowerCase()).
      set(ALIAS.ALIAS_URI, alias.getAliasURI()).
      set(ALIAS.TIMESTAMP, alias.getTimestamp()).
      set(ALIAS.HEIGHT, Burst.getBlockchain().getHeight()).execute();
  }

  private final VersionedEntityTable<Alias> aliasTable = new VersionedEntitySqlTable<Alias>("alias", brs.schema.Tables.ALIAS, aliasDbKeyFactory) {
      @Override
      protected Alias load(DSLContext ctx, ResultSet rs) throws SQLException {
        return new SqlAlias(rs);
      }

      @Override
      protected void save(DSLContext ctx, Alias alias) throws SQLException {
        saveAlias(ctx, alias);
      }

      @Override
      protected List<SortField> defaultSort() {
        List<SortField> sort = new ArrayList<>();
        sort.add(tableClass.field("alias_name_lower", String.class).asc());
        return sort;
      }
    };

  @Override
  public BurstIterator<Alias> getAliasesByOwner(long accountId, int from, int to) {
    return aliasTable.getManyBy(brs.schema.Tables.ALIAS.ACCOUNT_ID.eq(accountId), from, to);
  }

  @Override
  public Alias getAlias(String aliasName) {
    return aliasTable.getBy(brs.schema.Tables.ALIAS.ALIAS_NAME_LOWER.eq(aliasName.toLowerCase()));
  }

}
