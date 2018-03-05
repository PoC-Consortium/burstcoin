package brs.db.h2;

import brs.TransactionDb;
import brs.db.BlockDb;
import brs.db.PeerDb;
import brs.db.store.Dbs;

import brs.db.sql.SqlBlockDb;
import brs.db.sql.SqlTransactionDb;
import brs.db.sql.SqlPeerDb;

/**
 * Db-Classes are necessary for the instanciation of some stores and have to be handled separately. In the original version these were static
 */
public class H2Dbs implements Dbs {

  private final BlockDb blockDb;
  private final TransactionDb transactionDb;
  private final PeerDb peerDb;


  public H2Dbs() {
    H2DbVersion.init();
    this.blockDb       = new SqlBlockDb();
    this.transactionDb = new SqlTransactionDb();
    this.peerDb        = new SqlPeerDb();
  }

  @Override
  public BlockDb getBlockDb() {
    return blockDb;
  }

  @Override
  public TransactionDb getTransactionDb() {
    return transactionDb;
  }

  @Override
  public PeerDb getPeerDb() {
    return peerDb;
  }

}
