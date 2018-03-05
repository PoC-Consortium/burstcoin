package brs.db.sql;

import brs.*;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;
import brs.db.store.DerivedTableManager;
import brs.db.store.EscrowStore;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Field;

import static brs.schema.Tables.ESCROW;
import static brs.schema.Tables.ESCROW_DECISION;

public class SqlEscrowStore implements EscrowStore {
  private final BurstKey.LongKeyFactory<Escrow> escrowDbKeyFactory = new DbKey.LongKeyFactory<Escrow>("id") {
      @Override
      public BurstKey newKey(Escrow escrow) {
        return escrow.dbKey;
      }
    };

  private final VersionedEntityTable<Escrow> escrowTable;
  private final DbKey.LinkKeyFactory<Escrow.Decision> decisionDbKeyFactory =
      new DbKey.LinkKeyFactory<Escrow.Decision>("escrow_id", "account_id") {
        @Override
        public BurstKey newKey(Escrow.Decision decision) {
          return decision.dbKey;
        }
      };
  private final VersionedEntityTable<Escrow.Decision> decisionTable;
  private final List<Transaction> resultTransactions = new ArrayList<>();


  public SqlEscrowStore(DerivedTableManager derivedTableManager) {
    escrowTable = new VersionedEntitySqlTable<Escrow>("escrow", brs.schema.Tables.ESCROW, escrowDbKeyFactory, derivedTableManager) {
      @Override
      protected Escrow load(DSLContext ctx, ResultSet rs) throws SQLException {
        return new SqlEscrow(rs);
      }

      @Override
      protected void save(DSLContext ctx, Escrow escrow) throws SQLException {
        saveEscrow(ctx, escrow);
      }
    };

    decisionTable = new VersionedEntitySqlTable<Escrow.Decision>("escrow_decision", brs.schema.Tables.ESCROW_DECISION, decisionDbKeyFactory, derivedTableManager) {
      @Override
      protected Escrow.Decision load(DSLContext ctx, ResultSet rs) throws SQLException {
        return new SqlDecision(rs);
      }

      @Override
      protected void save(DSLContext ctx, Escrow.Decision decision) throws SQLException {
        saveDecision(ctx, decision);
      }
    };
  }



  protected void saveDecision(DSLContext ctx, Escrow.Decision decision) throws SQLException {
    brs.schema.tables.records.EscrowDecisionRecord decisionRecord = ctx.newRecord(ESCROW_DECISION);
    decisionRecord.setEscrowId(decision.escrowId);
    decisionRecord.setAccountId(decision.accountId);
    decisionRecord.setDecision(((int) Escrow.decisionToByte(decision.getDecision())));
    decisionRecord.setHeight(Burst.getBlockchain().getHeight());
    decisionRecord.setLatest(true);
    DbUtils.mergeInto(
      ctx, decisionRecord, ESCROW_DECISION,
      ( new Field[] { decisionRecord.field("escrow_id"), decisionRecord.field("account_id"), decisionRecord.field("height") } )
    );
  }

  @Override
  public BurstKey.LongKeyFactory<Escrow> getEscrowDbKeyFactory() {
    return escrowDbKeyFactory;
  }

  @Override
  public VersionedEntityTable<Escrow> getEscrowTable() {
    return escrowTable;
  }

  @Override
  public DbKey.LinkKeyFactory<Escrow.Decision> getDecisionDbKeyFactory() {
    return decisionDbKeyFactory;
  }

  @Override
  public VersionedEntityTable<Escrow.Decision> getDecisionTable() {
    return decisionTable;
  }

  @Override
  public Collection<Escrow> getEscrowTransactionsByParticipant(Long accountId) {
    List<Escrow> filtered = new ArrayList<>();
    BurstIterator<Escrow.Decision> it = decisionTable.getManyBy(ESCROW_DECISION.ACCOUNT_ID.eq(accountId), 0, -1);
    while (it.hasNext()) {
      Escrow.Decision decision = it.next();
      Escrow escrow = escrowTable.get(escrowDbKeyFactory.newKey(decision.escrowId));
      if (escrow != null) {
        filtered.add(escrow);
      }
    }
    return filtered;
  }



  @Override
  public List<Transaction> getResultTransactions() {
    return resultTransactions;
  }

  protected void saveEscrow(DSLContext ctx, Escrow escrow) throws SQLException {
    brs.schema.tables.records.EscrowRecord escrowRecord = ctx.newRecord(ESCROW);
    escrowRecord.setId(escrow.id);
    escrowRecord.setSenderId(escrow.senderId);
    escrowRecord.setRecipientId(escrow.recipientId);
    escrowRecord.setAmount(escrow.amountNQT);
    escrowRecord.setRequiredSigners(escrow.requiredSigners);
    escrowRecord.setDeadline(escrow.deadline);
    escrowRecord.setDeadlineAction(((int) escrow.decisionToByte(escrow.deadlineAction)));
    escrowRecord.setHeight(Burst.getBlockchain().getHeight());
    escrowRecord.setLatest(true);
    DbUtils.mergeInto(
      ctx, escrowRecord, ESCROW,
      ( new Field[] { escrowRecord.field("id"), escrowRecord.field("height") } )
    );
  }

  private class SqlDecision extends Escrow.Decision {
    private SqlDecision(ResultSet rs) throws SQLException {
      super(decisionDbKeyFactory.newKey(rs.getLong("escrow_id"), rs.getLong("account_id")), rs.getLong("escrow_id"), rs.getLong("account_id"),
            Escrow.byteToDecision((byte) rs.getInt("decision")));
    }
  }

  private class SqlEscrow extends Escrow {
    private SqlEscrow(ResultSet rs) throws SQLException {
      super(
            rs.getLong("id"),
            rs.getLong("sender_id"),
            rs.getLong("recipient_id"),
            escrowDbKeyFactory.newKey(rs.getLong("id")),
            rs.getLong("amount"),
            rs.getInt("required_signers"),
            rs.getInt("deadline"),
            byteToDecision((byte) rs.getInt("deadline_action"))
            );
    }
  }

  @Override
  public 	BurstIterator<Escrow.Decision> getDecisions(Long id)
  {
    return  decisionTable.getManyBy(ESCROW_DECISION.ESCROW_ID.eq(id), 0, -1);
  }

}
