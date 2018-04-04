package brs.db.h2;

import brs.db.sql.Db;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import brs.db.sql.SqlBlockDb;

final class H2DbVersion {

  private static final Logger logger = LoggerFactory.getLogger(H2DbVersion.class);
  private static int initialDbVersion = 0;

  static void init() {
    try (Connection con = Db.beginTransaction(); Statement stmt = con.createStatement()) {
      int nextUpdate = 1;
      try ( ResultSet rs = stmt.executeQuery("SELECT next_update FROM version") ) {
        if (! rs.next() || ! rs.isLast()) {
          throw new RuntimeException("Invalid version table");
        }
        nextUpdate       = rs.getInt("next_update");
        initialDbVersion = nextUpdate - 1;
        // wallets with DB release 175 had a broken trim function, which deleted way too much data
        if ( initialDbVersion >= 163 && initialDbVersion <= 175 ) {
          throw new RuntimeException("Your database looks inconsistent. Please drop and recreate your database.");
        }
        logger.info("Database update may take a while if needed, current db version " + (nextUpdate - 1) + "...");
      } catch (SQLException e) {
        logger.info("Initializing an empty database");
        stmt.executeUpdate("CREATE TABLE version (next_update INT NOT NULL)");
        stmt.executeUpdate("INSERT INTO version VALUES (1)");
        Db.commitTransaction();
      }
      update(nextUpdate);
    } catch (RuntimeException|SQLException e) {
      Db.rollbackTransaction();
      throw new RuntimeException(e.toString(), e);
    } finally {
      Db.endTransaction();
    }

  }

  private static void apply(String sql) {
    try (Connection con = Db.getConnection(); Statement stmt = con.createStatement()) {
      try {
        if (sql != null) {
          logger.debug("Will apply sql:\n" + sql);
          stmt.executeUpdate(sql);
        }
        stmt.executeUpdate("UPDATE version SET next_update = next_update + 1");
        Db.commitTransaction();
      } catch (SQLException e) {
        Db.rollbackTransaction();
        throw e;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Database error executing " + sql, e);
    }
  }

  private static void update(int nextUpdate) {
    switch (nextUpdate) {
      case 1:
        apply("CREATE TABLE IF NOT EXISTS block (db_id IDENTITY, id BIGINT NOT NULL, version INT NOT NULL, "
              + "timestamp INT NOT NULL, previous_block_id BIGINT, "
              + "FOREIGN KEY (previous_block_id) REFERENCES block (id) ON DELETE CASCADE, total_amount INT NOT NULL, "
              + "total_fee INT NOT NULL, payload_length INT NOT NULL, generator_public_key BINARY(32) NOT NULL, "
              + "previous_block_hash BINARY(32), cumulative_difficulty VARBINARY NOT NULL, base_target BIGINT NOT NULL, "
              + "next_block_id BIGINT, FOREIGN KEY (next_block_id) REFERENCES block (id) ON DELETE SET NULL, "
              + "index INT NOT NULL, height INT NOT NULL, generation_signature BINARY(64) NOT NULL, "
              + "block_signature BINARY(64) NOT NULL, payload_hash BINARY(32) NOT NULL, generator_account_id BIGINT NOT NULL, nonce BIGINT NOT NULL)");
      case 2:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS block_id_idx ON block (id)");
      case 3:
        apply("CREATE TABLE IF NOT EXISTS transaction (db_id IDENTITY, id BIGINT NOT NULL, "
              + "deadline SMALLINT NOT NULL, sender_public_key BINARY(32) NOT NULL, recipient_id BIGINT NOT NULL, "
              + "amount INT NOT NULL, fee INT NOT NULL, referenced_transaction_id BIGINT, index INT NOT NULL, "
              + "height INT NOT NULL, block_id BIGINT NOT NULL, FOREIGN KEY (block_id) REFERENCES block (id) ON DELETE CASCADE, "
              + "signature BINARY(64) NOT NULL, timestamp INT NOT NULL, type TINYINT NOT NULL, subtype TINYINT NOT NULL, "
              + "sender_account_id BIGINT NOT NULL, attachment OTHER)");
      case 4:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS transaction_id_idx ON transaction (id)");
      case 5:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS block_height_idx ON block (height)");
      case 6:
        apply("CREATE INDEX IF NOT EXISTS transaction_timestamp_idx ON transaction (timestamp)");
      case 7:
        apply("CREATE INDEX IF NOT EXISTS block_generator_account_id_idx ON block (generator_account_id)");
      case 8:
        apply("CREATE INDEX IF NOT EXISTS transaction_sender_account_id_idx ON transaction (sender_account_id)");
      case 9:
        apply("CREATE INDEX IF NOT EXISTS transaction_recipient_id_idx ON transaction (recipient_id)");
      case 10:
        apply("ALTER TABLE block ALTER COLUMN generator_account_id RENAME TO generator_id");
      case 11:
        apply("ALTER TABLE transaction ALTER COLUMN sender_account_id RENAME TO sender_id");
      case 12:
        apply("ALTER INDEX block_generator_account_id_idx RENAME TO block_generator_id_idx");
      case 13:
        apply("ALTER INDEX transaction_sender_account_id_idx RENAME TO transaction_sender_id_idx");
      case 14:
        apply("ALTER TABLE block DROP COLUMN IF EXISTS index");
      case 15:
        apply("ALTER TABLE transaction DROP COLUMN IF EXISTS index");
      case 16:
        apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS block_timestamp INT");
      case 17:
        apply(null);
      case 18:
        apply("ALTER TABLE transaction ALTER COLUMN block_timestamp SET NOT NULL");
      case 19:
        apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS hash BINARY(32)");
      case 20:
        apply(null);
      case 21:
        apply(null);
      case 22:
        apply("CREATE INDEX IF NOT EXISTS transaction_hash_idx ON transaction (hash)");
      case 23:
        apply(null);
      case 24:
        apply("ALTER TABLE block ALTER COLUMN total_amount BIGINT");
      case 25:
        apply("ALTER TABLE block ALTER COLUMN total_fee BIGINT");
      case 26:
        apply("ALTER TABLE transaction ALTER COLUMN amount BIGINT");
      case 27:
        apply("ALTER TABLE transaction ALTER COLUMN fee BIGINT");
      case 28:
        apply(null);
      case 29:
        apply(null);
      case 30:
        apply(null);
      case 31:
        apply(null);
      case 32:
        apply(null);
      case 33:
        apply(null);
      case 34:
        apply(null);
      case 35:
        apply(null);
      case 36:
        apply("CREATE TABLE IF NOT EXISTS peer (address VARCHAR PRIMARY KEY)");
      case 37:
        /*if (!Constants.isTestnet) {
          apply("INSERT INTO peer (address) VALUES " +
          "('85.25.198.120'), ('home.kaerner.net'), ('162.243.145.83'), ('ct.flipflop.mooo.com'), ('87.138.143.21'), " +
          "('dtodorov.asuscomm.com'), ('188.35.156.10'), ('88.163.78.131'), ('nxt01.now.im'), ('198.211.127.34'), " +
          "('nxtx.ru'), ('54.194.212.248'), ('brs.scryptmh.eu'), ('rigel1.ddns.net'), ('91.120.22.146'), " +
          "('beor.homeip.net'), ('nacho.damnserver.com'), ('93.103.20.35'), ('nxt1.achnodes.com'), ('128.199.228.211'), " +
          "('87.139.122.157'), ('212.18.225.173'), ('81.64.77.101'), ('node6.mynxtcoin.org'), ('185.61.148.216'), " +
          "('24.149.8.238'), ('86.14.63.4'), ('192.3.158.120'), ('46.173.9.98'), ('90.153.126.17'), " +
          "('node5.mynxtcoin.org'), ('108.61.57.76'), ('162.243.213.190'), ('node7.mynxtcoin.org'), ('91.69.121.229'), " +
          "('89.250.240.56'), ('77.179.46.202'), ('209.222.2.110'), ('176.9.29.76'), ('cyborg.thican.net'), " +
          "('nxt1107.no-ip.biz'), ('199.231.211.23'), ('66.30.204.105'), ('103.22.181.239'), ('silvanoip.dhcp.biz'), " +
          "('108.170.40.6'), ('nxt1.lgc.pw'), ('106.186.127.189'), ('191.238.101.73'), ('210.188.36.5'), " +
          "('188.226.197.131'), ('84.242.91.139'), ('144.76.17.83'), ('sluni.szn.dk'), ('37.139.6.166'), " +
          "('node4.mynxtcoin.org'), ('178.15.99.67'), ('67.212.71.171'), ('84.128.162.237'), ('148.251.139.82'), " +
          "('107.170.164.129'), ('nxtpi.zapto.org'), ('2.225.88.10'), ('85.214.222.82'), ('113.106.85.172'), " +
          "('199.193.252.103'), ('46.109.165.170'), ('37.59.47.155'), ('87.139.122.48'), ('178.194.110.193'), " +
          "('brs.sx'), ('89.250.240.60'), ('23.102.0.45'), ('54.83.4.11'), ('81.2.216.179'), ('46.237.8.30'), " +
          "('woll-e.net'), ('77.88.208.12'), ('54.77.63.53'), ('212.232.49.28'), ('54.88.54.58'), ('80.153.101.190'), " +
          "('128.199.160.141'), ('46.4.212.230'), ('190.10.9.166'), ('37.187.21.28'), ('109.230.224.65'), ('67.212.71.173'), " +
          "('107.170.3.62'), ('nxt2.achnodes.com'), ('167.206.61.3'), ('211.149.213.86'), ('96.251.124.95'), " +
          "('node0.forgebrs.com'), ('178.32.221.58'), ('108.170.40.4'), ('188.226.206.41'), ('217.26.24.27'), " +
          "('188.226.245.226'), ('84.133.75.209'), ('dorcsforge.cloudapp.net'), ('89.250.243.200'), ('brs.hofhom.nl'), " +
          "('89.179.31.237'), ('phalanx149.ddns.net'), ('188.226.179.119'), ('humanoide.thican.net'), " +
          "('enricoip.no-ip.biz:65074'), ('nxtforgersr.ddns.net'), ('91.121.150.75'), ('brs.alkeron.com'), ('85.84.67.234'), " +
          "('213.46.57.77'), ('178.93.103.130'), ('83.60.90.135'), ('81.169.150.141'), ('brs.cybermailing.com'), " +
          "('185.21.192.9'), ('54.213.222.141'), ('107.170.75.92'), ('vh44.ddns.net:7873'), ('108.170.40.5'), " +
          "('miasik.no-ip.org'), ('54.68.61.191'), ('96.240.128.221')");
          } else {
          apply("INSERT INTO peer (address) VALUES " +
          "('brs.scryptmh.eu'), ('54.186.98.117'), ('178.150.207.53'), ('192.241.223.132'), ('node9.mynxtcoin.org'), " +
          "('node10.mynxtcoin.org'), ('node3.mynxtcoin.org'), ('109.87.169.253'), ('nxtnet.fr'), ('50.112.241.97'), " +
          "('2.84.142.149'), ('bug.airdns.org'), ('83.212.103.14'), ('62.210.131.30'), ('104.131.254.22'), " +
          "('46.28.111.249'), ('94.79.54.205')");
          }*/
        apply(null);
      case 38:
        apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS full_hash BINARY(32)");
      case 39:
        apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS referenced_transaction_full_hash BINARY(32)");
      case 40:
        apply(null);
      case 41:
        apply("ALTER TABLE transaction ALTER COLUMN full_hash SET NOT NULL");
      case 42:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS transaction_full_hash_idx ON transaction (full_hash)");
      case 43:
        apply(null);
      case 44:
        apply(null);
      case 45:
        apply(null);
      case 46:
        apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS attachment_bytes VARBINARY");
      case 47:
        apply(null);
      case 48:
        apply("ALTER TABLE transaction DROP COLUMN attachment");
      case 49:
        apply(null);
      case 50:
        apply("ALTER TABLE transaction DROP COLUMN referenced_transaction_id");
      case 51:
        apply("ALTER TABLE transaction DROP COLUMN hash");
      case 52:
        apply(null);
      case 53:
        apply("DROP INDEX transaction_recipient_id_idx");
      case 54:
        apply("ALTER TABLE transaction ALTER COLUMN recipient_id SET NULL");
      case 55:
        new SqlBlockDb().deleteAll(true);
        apply(null);
      case 56:
        apply("CREATE INDEX IF NOT EXISTS transaction_recipient_id_idx ON transaction (recipient_id)");
      case 57:
        apply(null);
      case 58:
        apply(null);
      case 59:
        apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS version TINYINT");
      case 60:
        apply("UPDATE transaction SET version = 0");
      case 61:
        apply("ALTER TABLE transaction ALTER COLUMN version SET NOT NULL");
      case 62:
        apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS has_message BOOLEAN NOT NULL DEFAULT FALSE");
      case 63:
        apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS has_encrypted_message BOOLEAN NOT NULL DEFAULT FALSE");
      case 64:
        apply("UPDATE transaction SET has_message = TRUE WHERE type = 1 AND subtype = 0");
      case 65:
        apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS has_public_key_announcement BOOLEAN NOT NULL DEFAULT FALSE");
      case 66:
        apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS ec_block_height INT DEFAULT NULL");
      case 67:
        apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS ec_block_id BIGINT DEFAULT NULL");
      case 68:
        apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS has_encrypttoself_message BOOLEAN NOT NULL DEFAULT FALSE");
      case 69:
        apply("CREATE INDEX IF NOT EXISTS transaction_block_timestamp_idx ON transaction (block_timestamp DESC)");
      case 70:
        apply("DROP INDEX transaction_timestamp_idx");
      case 71:
        apply("CREATE TABLE IF NOT EXISTS alias (db_id IDENTITY, id BIGINT NOT NULL, "
              + "account_id BIGINT NOT NULL, alias_name VARCHAR NOT NULL, "
              + "alias_name_lower VARCHAR AS LOWER (alias_name) NOT NULL, "
              + "alias_uri VARCHAR NOT NULL, timestamp INT NOT NULL, "
              + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
      case 72:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS alias_id_height_idx ON alias (id, height DESC)");
      case 73:
        apply("CREATE INDEX IF NOT EXISTS alias_account_id_idx ON alias (account_id, height DESC)");
      case 74:
        apply("CREATE INDEX IF NOT EXISTS alias_name_lower_idx ON alias (alias_name_lower)");
      case 75:
        apply("CREATE TABLE IF NOT EXISTS alias_offer (db_id IDENTITY, id BIGINT NOT NULL, "
              + "price BIGINT NOT NULL, buyer_id BIGINT, "
              + "height INT NOT NULL, latest BOOLEAN DEFAULT TRUE NOT NULL)");
      case 76:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS alias_offer_id_height_idx ON alias_offer (id, height DESC)");
      case 77:
        apply("CREATE TABLE IF NOT EXISTS asset (db_id IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
              + "name VARCHAR NOT NULL, description VARCHAR, quantity BIGINT NOT NULL, decimals TINYINT NOT NULL, "
              + "height INT NOT NULL)");
      case 78:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS asset_id_idx ON asset (id)");
      case 79:
        apply("CREATE INDEX IF NOT EXISTS asset_account_id_idx ON asset (account_id)");
      case 80:
        apply("CREATE TABLE IF NOT EXISTS trade (db_id IDENTITY, asset_id BIGINT NOT NULL, block_id BIGINT NOT NULL, "
              + "ask_order_id BIGINT NOT NULL, bid_order_id BIGINT NOT NULL, ask_order_height INT NOT NULL, "
              + "bid_order_height INT NOT NULL, seller_id BIGINT NOT NULL, buyer_id BIGINT NOT NULL, "
              + "quantity BIGINT NOT NULL, price BIGINT NOT NULL, timestamp INT NOT NULL, height INT NOT NULL)");
      case 81:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS trade_ask_bid_idx ON trade (ask_order_id, bid_order_id)");
      case 82:
        apply("CREATE INDEX IF NOT EXISTS trade_asset_id_idx ON trade (asset_id, height DESC)");
      case 83:
        apply("CREATE INDEX IF NOT EXISTS trade_seller_id_idx ON trade (seller_id, height DESC)");
      case 84:
        apply("CREATE INDEX IF NOT EXISTS trade_buyer_id_idx ON trade (buyer_id, height DESC)");
      case 85:
        apply("CREATE TABLE IF NOT EXISTS ask_order (db_id IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
              + "asset_id BIGINT NOT NULL, price BIGINT NOT NULL, "
              + "quantity BIGINT NOT NULL, creation_height INT NOT NULL, height INT NOT NULL, "
              + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
      case 86:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS ask_order_id_height_idx ON ask_order (id, height DESC)");
      case 87:
        apply("CREATE INDEX IF NOT EXISTS ask_order_account_id_idx ON ask_order (account_id, height DESC)");
      case 88:
        apply("CREATE INDEX IF NOT EXISTS ask_order_asset_id_price_idx ON ask_order (asset_id, price)");
      case 89:
        apply("CREATE TABLE IF NOT EXISTS bid_order (db_id IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
              + "asset_id BIGINT NOT NULL, price BIGINT NOT NULL, "
              + "quantity BIGINT NOT NULL, creation_height INT NOT NULL, height INT NOT NULL, "
              + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
      case 90:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS bid_order_id_height_idx ON bid_order (id, height DESC)");
      case 91:
        apply("CREATE INDEX IF NOT EXISTS bid_order_account_id_idx ON bid_order (account_id, height DESC)");
      case 92:
        apply("CREATE INDEX IF NOT EXISTS bid_order_asset_id_price_idx ON bid_order (asset_id, price DESC)");
      case 93:
        apply("CREATE TABLE IF NOT EXISTS goods (db_id IDENTITY, id BIGINT NOT NULL, seller_id BIGINT NOT NULL, "
              + "name VARCHAR NOT NULL, description VARCHAR, "
              + "tags VARCHAR, timestamp INT NOT NULL, quantity INT NOT NULL, price BIGINT NOT NULL, "
              + "delisted BOOLEAN NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
      case 94:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS goods_id_height_idx ON goods (id, height DESC)");
      case 95:
        apply("CREATE INDEX IF NOT EXISTS goods_seller_id_name_idx ON goods (seller_id, name)");
      case 96:
        apply("CREATE INDEX IF NOT EXISTS goods_timestamp_idx ON goods (timestamp DESC, height DESC)");
      case 97:
        apply("CREATE TABLE IF NOT EXISTS purchase (db_id IDENTITY, id BIGINT NOT NULL, buyer_id BIGINT NOT NULL, "
              + "goods_id BIGINT NOT NULL, "
              + "seller_id BIGINT NOT NULL, quantity INT NOT NULL, "
              + "price BIGINT NOT NULL, deadline INT NOT NULL, note VARBINARY, nonce BINARY(32), "
              + "timestamp INT NOT NULL, pending BOOLEAN NOT NULL, goods VARBINARY, goods_nonce BINARY(32), "
              + "refund_note VARBINARY, refund_nonce BINARY(32), has_feedback_notes BOOLEAN NOT NULL DEFAULT FALSE, "
              + "has_public_feedbacks BOOLEAN NOT NULL DEFAULT FALSE, discount BIGINT NOT NULL, refund BIGINT NOT NULL, "
              + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
      case 98:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS purchase_id_height_idx ON purchase (id, height DESC)");
      case 99:
        apply("CREATE INDEX IF NOT EXISTS purchase_buyer_id_height_idx ON purchase (buyer_id, height DESC)");
      case 100:
        apply("CREATE INDEX IF NOT EXISTS purchase_seller_id_height_idx ON purchase (seller_id, height DESC)");
      case 101:
        apply("CREATE INDEX IF NOT EXISTS purchase_deadline_idx ON purchase (deadline DESC, height DESC)");
      case 102:
        apply("CREATE TABLE IF NOT EXISTS account (db_id IDENTITY, id BIGINT NOT NULL, creation_height INT NOT NULL, "
              + "public_key BINARY(32), key_height INT, balance BIGINT NOT NULL, unconfirmed_balance BIGINT NOT NULL, "
              + "forged_balance BIGINT NOT NULL, name VARCHAR, description VARCHAR, current_leasing_height_from INT, "
              + "current_leasing_height_to INT, current_lessee_id BIGINT NULL, next_leasing_height_from INT, "
              + "next_leasing_height_to INT, next_lessee_id BIGINT NULL, height INT NOT NULL, "
              + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
      case 103:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS account_id_height_idx ON account (id, height DESC)");
      case 104:
        //apply("CREATE INDEX IF NOT EXISTS account_current_lessee_id_leasing_height_idx ON account (current_lessee_id, "
        //        + "current_leasing_height_to DESC)");
        apply(null);
      case 105:
        apply("CREATE TABLE IF NOT EXISTS account_asset (db_id IDENTITY, account_id BIGINT NOT NULL, "
              + "asset_id BIGINT NOT NULL, quantity BIGINT NOT NULL, unconfirmed_quantity BIGINT NOT NULL, height INT NOT NULL, "
              + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
      case 106:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS account_asset_id_height_idx ON account_asset (account_id, asset_id, height DESC)");
      case 107:
        //apply("CREATE TABLE IF NOT EXISTS account_guaranteed_balance (db_id IDENTITY, account_id BIGINT NOT NULL, "
        //        + "additions BIGINT NOT NULL, height INT NOT NULL)");
        apply(null);
      case 108:
        //apply("CREATE UNIQUE INDEX IF NOT EXISTS account_guaranteed_balance_id_height_idx ON account_guaranteed_balance "
        //        + "(account_id, height DESC)");
        apply(null);
      case 109:
        apply("CREATE TABLE IF NOT EXISTS purchase_feedback (db_id IDENTITY, id BIGINT NOT NULL, feedback_data VARBINARY NOT NULL, "
              + "feedback_nonce BINARY(32) NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
      case 110:
        apply("CREATE INDEX IF NOT EXISTS purchase_feedback_id_height_idx ON purchase_feedback (id, height DESC)");
      case 111:
        apply("CREATE TABLE IF NOT EXISTS purchase_public_feedback (db_id IDENTITY, id BIGINT NOT NULL, public_feedback "
              + "VARCHAR NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
      case 112:
        apply("CREATE INDEX IF NOT EXISTS purchase_public_feedback_id_height_idx ON purchase_public_feedback (id, height DESC)");
      case 113:
        apply("CREATE TABLE IF NOT EXISTS unconfirmed_transaction (db_id IDENTITY, id BIGINT NOT NULL, expiration INT NOT NULL, "
              + "transaction_height INT NOT NULL, fee_per_byte BIGINT NOT NULL, timestamp INT NOT NULL, "
              + "transaction_bytes VARBINARY NOT NULL, height INT NOT NULL)");
      case 114:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS unconfirmed_transaction_id_idx ON unconfirmed_transaction (id)");
      case 115:
        apply("CREATE INDEX IF NOT EXISTS unconfirmed_transaction_height_fee_timestamp_idx ON unconfirmed_transaction "
              + "(transaction_height ASC, fee_per_byte DESC, timestamp ASC)");
      case 116:
        apply("CREATE TABLE IF NOT EXISTS asset_transfer (db_id IDENTITY, id BIGINT NOT NULL, asset_id BIGINT NOT NULL, "
              + "sender_id BIGINT NOT NULL, recipient_id BIGINT NOT NULL, quantity BIGINT NOT NULL, timestamp INT NOT NULL, "
              + "height INT NOT NULL)");
      case 117:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS asset_transfer_id_idx ON asset_transfer (id)");
      case 118:
        apply("CREATE INDEX IF NOT EXISTS asset_transfer_asset_id_idx ON asset_transfer (asset_id, height DESC)");
      case 119:
        apply("CREATE INDEX IF NOT EXISTS asset_transfer_sender_id_idx ON asset_transfer (sender_id, height DESC)");
      case 120:
        apply("CREATE INDEX IF NOT EXISTS asset_transfer_recipient_id_idx ON asset_transfer (recipient_id, height DESC)");
      case 121:
        logger.warn("If this was an update and not a fresh start this might not work correctly - please delete your db and start over. If you just created a fresh db everything is fine!");
        apply(null);
      case 122:
        apply("CREATE INDEX IF NOT EXISTS account_asset_quantity_idx ON account_asset (quantity DESC)");
      case 123:
        apply("CREATE INDEX IF NOT EXISTS purchase_timestamp_idx ON purchase (timestamp DESC, id)");
      case 124:
        apply("CREATE INDEX IF NOT EXISTS ask_order_creation_idx ON ask_order (creation_height DESC)");
      case 125:
        apply("CREATE INDEX IF NOT EXISTS bid_order_creation_idx ON bid_order (creation_height DESC)");
      case 126:
        apply("CREATE TABLE IF NOT EXISTS reward_recip_assign (db_id IDENTITY, account_id BIGINT NOT NULL, "
              + "prev_recip_id BIGINT NOT NULL, recip_id BIGINT NOT NULL, from_height INT NOT NULL, "
              + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
      case 127:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS reward_recip_assign_account_id_height_idx ON reward_recip_assign (account_id, height DESC)");
      case 128:
        apply("CREATE INDEX IF NOT EXISTS reward_recip_assign_recip_id_height_idx ON reward_recip_assign (recip_id, height DESC)");
      case 129:
        apply("CREATE TABLE IF NOT EXISTS escrow (db_id IDENTITY, id BIGINT NOT NULL, sender_id BIGINT NOT NULL, recipient_id BIGINT NOT NULL, "
              + "amount BIGINT NOT NULL, required_signers INT, deadline INT NOT NULL, deadline_action INT NOT NULL, "
              + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
      case 130:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS escrow_id_height_idx ON escrow (id, height DESC)");
      case 131:
        apply("CREATE INDEX IF NOT EXISTS escrow_sender_id_height_idx ON escrow (sender_id, height DESC)");
      case 132:
        apply("CREATE INDEX IF NOT EXISTS escrow_recipient_id_height_idx ON escrow (recipient_id, height DESC)");
      case 133:
        apply("CREATE INDEX IF NOT EXISTS escrow_deadline_height_idx ON escrow (deadline, height DESC)");
      case 134:
        apply("CREATE TABLE IF NOT EXISTS escrow_decision (db_id IDENTITY, escrow_id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
              + "decision INT NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
      case 135:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS escrow_decision_escrow_id_account_id_height_idx ON escrow_decision (escrow_id, account_id, height DESC)");
      case 136:
        apply("CREATE INDEX IF NOT EXISTS escrow_decision_escrow_id_height_idx ON escrow_decision (escrow_id, height DESC)");
      case 137:
        apply("CREATE INDEX IF NOT EXISTS escrow_decision_account_id_height_idx ON escrow_decision (account_id, height DESC)");
      case 138:
        apply("ALTER TABLE transaction ALTER COLUMN signature SET NULL");
      case 139:
        apply("CREATE TABLE IF NOT EXISTS subscription (db_id IDENTITY, id BIGINT NOT NULL, sender_id BIGINT NOT NULL, recipient_id BIGINT NOT NULL, "
              + "amount BIGINT NOT NULL, frequency INT NOT NULL, time_next INT NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
      case 140:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS subscription_id_height_idx ON subscription (id, height DESC)");
      case 141:
        apply("CREATE INDEX IF NOT EXISTS subscription_sender_id_height_idx ON subscription (sender_id, height DESC)");
      case 142:
        apply("CREATE INDEX IF NOT EXISTS subscription_recipient_id_height_idx ON subscription (recipient_id, height DESC)");
      case 143:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS block_timestamp_idx ON block (timestamp DESC)");
      case 144:
        apply("CREATE TABLE IF NOT EXISTS at (db_id IDENTITY, id BIGINT NOT NULL, creator_id BIGINT NOT NULL, name VARCHAR, description VARCHAR, "
              + "version SMALLINT NOT NULL, csize INT NOT NULL, dsize INT NOT NULL, c_user_stack_bytes INT NOT NULL, c_call_stack_bytes INT NOT NULL, "
              + "creation_height INT NOT NULL, ap_code BINARY NOT NULL, "
              + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
      case 145:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS at_id_height_idx ON at (id, height DESC)");
      case 146:
        apply("CREATE INDEX IF NOT EXISTS at_creator_id_height_idx ON at (creator_id, height DESC)");
      case 147:
        apply("CREATE TABLE IF NOT EXISTS at_state (db_id IDENTITY, at_id BIGINT NOT NULL, state BINARY NOT NULL, prev_height INT NOT NULL, "
              + "next_height INT NOT NULL, sleep_between INT NOT NULL, "
              + "prev_balance BIGINT NOT NULL, freeze_when_same_balance BOOLEAN NOT NULL, min_activate_amount BIGINT NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
      case 148:
        apply("CREATE UNIQUE INDEX IF NOT EXISTS at_state_at_id_height_idx ON at_state (at_id, height DESC)");
      case 149:
        apply("CREATE INDEX IF NOT EXISTS at_state_id_next_height_height_idx ON at_state (at_id, next_height, height DESC)");
      case 150:
        apply("ALTER TABLE block ADD COLUMN IF NOT EXISTS ats BINARY");
      case 151:
        apply("CREATE INDEX IF NOT EXISTS account_id_balance_height_idx ON account (id, balance, height DESC)");
      case 152:
        apply("CREATE INDEX IF NOT EXISTS transaction_recipient_id_amount_height_idx ON transaction (recipient_id, amount, height)");
      case 153:
        apply(null);
      case 154:
        apply("DROP INDEX IF EXISTS account_guaranteed_balance_id_height_idx");
      case 155:
        apply("DROP TABLE IF EXISTS account_guaranteed_balance");
      case 156:
        apply("DROP INDEX IF EXISTS account_current_lessee_id_leasing_height_idx");
      case 157:
        apply("ALTER TABLE account DROP COLUMN IF EXISTS current_leasing_height_from");
      case 158:
        apply("ALTER TABLE account DROP COLUMN IF EXISTS current_leasing_height_to");
      case 159:
        apply("ALTER TABLE account DROP COLUMN IF EXISTS current_lessee_id");
      case 160:
        apply("ALTER TABLE account DROP COLUMN IF EXISTS next_leasing_height_from");
      case 161:
        apply("ALTER TABLE account DROP COLUMN IF EXISTS next_leasing_height_to");
      case 162:
        apply("ALTER TABLE account DROP COLUMN IF EXISTS next_lessee_id");
      case 163:
        apply("ALTER TABLE transaction ALTER COLUMN referenced_transaction_full_hash RENAME TO referenced_transaction_fullhash");
      case 173:
        apply("UPDATE \"version\" SET \"next_update\" = '173';");
      case 174:
        apply("ALTER TABLE alias ALTER COLUMN alias_name_LOWER RENAME TO alias_name_lower");
      case 175:
        apply("CREATE INDEX IF NOT EXISTS account_id_latest_idx ON account(id, latest)");
      case 176:
        apply("ALTER TABLE alias ALTER COLUMN alias_name_lower VARCHAR NOT NULL");
      case 177:
        return;
      default:
        throw new RuntimeException("Database inconsistent with code, probably trying to run older code on newer database");
    }
  }
  private H2DbVersion() {} //never
}
