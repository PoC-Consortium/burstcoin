package brs.db.sql;

import brs.TransactionDb;
import brs.db.BlockDb;
import brs.db.PeerDb;
import brs.db.store.Dbs;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import brs.db.sql.SqlBlockDb;
import brs.db.sql.SqlTransactionDb;
import brs.db.sql.SqlPeerDb;

public class SqlDbs implements Dbs {

  private final BlockDb blockDb;
  private final TransactionDb transactionDb;
  private final PeerDb peerDb;


  public SqlDbs() {
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
