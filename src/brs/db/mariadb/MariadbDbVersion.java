package brs.db.mariadb;

import brs.db.sql.Db;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class MariadbDbVersion {

  private static final Logger logger = LoggerFactory.getLogger(MariadbDbVersion.class);
  private static final String CHARSET = "utf8mb4";
  private static int initialDbVersion = 0;

  static void init() {
    try (Connection con = Db.beginTransaction(); Statement stmt = con.createStatement()) {
      // try to check for a valid charset, cause there are so many tools around, which
      // use the latin1 default charsets :-(
      try ( ResultSet rs = stmt.executeQuery("SHOW VARIABLES LIKE \"character_set_database\"") ) {
        if (rs.next()) {
          String dbCharset = rs.getString(2);
          if ( ! dbCharset.equalsIgnoreCase(CHARSET) ) {
            throw new RuntimeException("Invalid database charset >" + dbCharset + "< - needs to be >" + CHARSET + "<");
          }
        }
      }
      int nextUpdate = 1;
      try ( ResultSet rs = stmt.executeQuery("SELECT next_update FROM version") ) {
        if (! rs.next() || ! rs.isLast()) {
          throw new RuntimeException("Invalid version table");
        }
        nextUpdate       = rs.getInt("next_update");
        initialDbVersion = nextUpdate - 1;
        // wallets with DB release 175 had a broken trim function, which deleted way too much data
        if ( initialDbVersion >= 170 && initialDbVersion <= 175 ) {
          throw new RuntimeException("Your database looks inconsistent. Please drop and recreate your database.");
        }
        logger.info("Database update may take a while if needed, current db version " + (nextUpdate - 1) + "...");
      } catch (SQLException e) {
        logger.info("Initializing an empty database");
        stmt.executeUpdate("CREATE TABLE version (next_update INT NOT NULL) ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        stmt.executeUpdate("INSERT INTO version VALUES (1)");
        Db.commitTransaction();
      }
      update(nextUpdate, con.getCatalog());
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
      } catch (Exception e) {
        Db.rollbackTransaction();
        throw e;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Database error executing " + sql, e);
    }
  }

  private static void update(int nextUpdate, String catalog) {
    switch (nextUpdate) {
      case 1:
        apply("CREATE TABLE alias("
              + "    db_id BIGINT AUTO_INCREMENT,"
              + "    id BIGINT NOT NULL,"
              + "    account_id BIGINT NOT NULL,"
              + "    alias_name VARCHAR(100) NOT NULL,"
              + "    alias_name_LOWER VARCHAR(100) NOT NULL,"
              + "    alias_uri TEXT NOT NULL,"
              + "    timestamp INT NOT NULL,"
              + "    height INT NOT NULL,"
              + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
              + "    PRIMARY KEY (db_id)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 2:
        apply("UPDATE version set next_update = '2';");
      case 3:
        apply("UPDATE version set next_update = '3';");
      case 4:
        apply("CREATE UNIQUE INDEX alias_id_height_idx ON alias(id, height DESC);");
      case 5:
        apply("CREATE INDEX alias_account_id_idx ON alias(account_id, height DESC);");
      case 6:
        apply("CREATE INDEX alias_name_lower_idx ON alias(alias_name_lower);");
      case 7:
        apply("CREATE TABLE account("
              + "    db_id BIGINT AUTO_INCREMENT,"
              + "    id BIGINT NOT NULL,"
              + "    creation_HEIGHT INT NOT NULL,"
              + "    public_key VARBINARY(32),"
              + "    key_height INT,"
              + "    balance BIGINT NOT NULL,"
              + "    unconfirmed_balance BIGINT NOT NULL,"
              + "    forged_balance BIGINT NOT NULL,"
              + "    name VARCHAR(100),"
              + "    description TEXT,"
              + "    height INT NOT NULL,"
              + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
              + "    PRIMARY KEY (db_id)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 8:
        apply("CREATE UNIQUE INDEX account_id_height_idx ON account(id, height DESC);");
      case 9:
        apply("CREATE INDEX account_id_balance_height_idx ON account(id, balance, height DESC);");
      case 10:
        apply("CREATE TABLE alias_offer("
              + "    db_id BIGINT AUTO_INCREMENT,"
              + "    id BIGINT NOT NULL,"
              + "    price BIGINT NOT NULL,"
              + "    buyer_ID BIGINT,"
              + "    height INT NOT NULL,"
              + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
              + "    PRIMARY KEY (db_id)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 11:
        apply("CREATE UNIQUE INDEX alias_offer_id_height_idx ON alias_offer(id, height DESC);");
      case 12:
        apply("CREATE TABLE peer("
              + "    address VARCHAR(100) NOT NULL,"
              + "    primary KEY (ADDRESS)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 13:
        apply("CREATE TABLE transaction("
              + "    db_id BIGINT AUTO_INCREMENT,"
              + "    id BIGINT NOT NULL,"
              + "    deadline SMALLINT NOT NULL,"
              + "    sender_public_key VARBINARY(32) NOT NULL,"
              + "    recipient_id BIGINT,"
              + "    amount BIGINT NOT NULL,"
              + "    fee BIGINT NOT NULL,"
              + "    height INT NOT NULL,"
              + "    block_id BIGINT NOT NULL,"
              + "    signature VARBINARY(64),"
              + "    timestamp INT NOT NULL,"
              + "    type TINYINT NOT NULL,"
              + "    subtype TINYINT NOT NULL,"
              + "    sender_id BIGINT NOT NULL,"
              + "    block_timestamp INT NOT NULL,"
              + "    full_hash VARBINARY(32) NOT NULL,"
              + "    referenced_transaction_full_hash VARBINARY(32),"
              + "    attachment_bytes BLOB,"
              + "    version TINYINT NOT NULL,"
              + "    has_message BOOLEAN DEFAULT FALSE NOT NULL,"
              + "    has_encrypted_message BOOLEAN DEFAULT FALSE NOT NULL,"
              + "    has_public_key_announcement BOOLEAN DEFAULT FALSE NOT NULL,"
              + "    ec_block_height INT DEFAULT NULL,"
              + "    ec_block_id BIGINT DEFAULT NULL,"
              + "    has_encrypttoself_message BOOLEAN DEFAULT FALSE NOT NULL,"
              + "    PRIMARY KEY (db_id)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 14:
        apply("CREATE INDEX transaction_block_timestamp_idx ON transaction(block_timestamp DESC);");
      case 15:
        apply("CREATE UNIQUE INDEX transaction_id_idx ON transaction(id);");
      case 16:
        apply("CREATE INDEX transaction_sender_id_idx ON transaction(sender_ID);");
      case 17:
        apply("CREATE UNIQUE INDEX transaction_full_hash_idx ON transaction(full_hash);");
      case 18:
        apply("CREATE INDEX transaction_recipient_id_idx ON transaction(recipient_id);");
      case 19:
        apply("CREATE INDEX transaction_recipient_id_amount_height_idx ON transaction(recipient_id, amount, height);");
      case 20:
        apply("CREATE TABLE asset("
              + "    db_id BIGINT AUTO_INCREMENT,"
              + "    id BIGINT NOT NULL,"
              + "    account_id BIGINT NOT NULL,"
              + "    name VARCHAR(10) NOT NULL,"
              + "    description TEXT,"
              + "    quantity BIGINT NOT NULL,"
              + "    decimals TINYINT NOT NULL,"
              + "    height INT NOT NULL,"
              + "    PRIMARY KEY (db_id)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 21:
        apply("CREATE UNIQUE INDEX asset_id_idx ON asset(id);");
      case 22:
        apply("CREATE INDEX asset_account_id_idx ON asset(account_id);");
      case 23:
        apply("CREATE TABLE trade("
              + "    db_id BIGINT AUTO_INCREMENT,"
              + "    asset_id BIGINT NOT NULL,"
              + "    block_id BIGINT NOT NULL,"
              + "    ask_order_id BIGINT NOT NULL,"
              + "    bid_order_id BIGINT NOT NULL,"
              + "    ask_order_height INT NOT NULL,"
              + "    bid_order_height INT NOT NULL,"
              + "    seller_id BIGINT NOT NULL,"
              + "    buyer_id BIGINT NOT NULL,"
              + "    quantity BIGINT NOT NULL,"
              + "    price BIGINT NOT NULL,"
              + "    timestamp INT NOT NULL,"
              + "    height INT NOT NULL,"
              + "    PRIMARY KEY (db_id)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 24:
        apply("CREATE UNIQUE INDEX trade_ask_bid_idx ON trade(ask_order_id, bid_order_id);");
      case 25:
        apply("CREATE INDEX trade_asset_id_idx ON trade(asset_id, height DESC);");
      case 26:
        apply("CREATE INDEX trade_seller_id_idx ON trade(seller_id, height DESC);");
      case 27:
        apply("CREATE INDEX trade_buyer_id_idx ON trade(buyer_id, height DESC);");
      case 28:
        apply("CREATE TABLE ask_order("
              + "    db_id BIGINT AUTO_INCREMENT,"
              + "    id bigint NOT NULL,"
              + "    account_id BIGINT NOT NULL,"
              + "    asset_id BIGINT NOT NULL,"
              + "    price BIGINT NOT NULL,"
              + "    quantity BIGINT NOT NULL,"
              + "    creation_height INT NOT NULL,"
              + "    height INT NOT NULL,"
              + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
              + "    PRIMARY KEY (db_id)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 29:
        apply("CREATE UNIQUE INDEX ask_order_id_height_idx ON ask_order(id, height DESC);");
      case 30:
        apply("CREATE INDEX ask_order_account_id_idx ON ask_order(account_id, height DESC);");
      case 31:
        apply("CREATE INDEX ask_order_asset_id_price_idx ON ask_order(asset_id, price);");
      case 32:
        apply("CREATE INDEX ask_order_creation_idx ON ask_order(creation_height DESC);");
      case 33:
        apply("CREATE TABLE bid_order("
              + "    db_id BIGINT AUTO_INCREMENT,"
              + "    id BIGINT NOT NULL,"
              + "    account_id BIGINT NOT NULL,"
              + "    asset_id BIGINT NOT NULL,"
              + "    price BIGINT NOT NULL,"
              + "    quantity BIGINT NOT NULL,"
              + "    creation_height INT NOT NULL,"
              + "    height INT NOT NULL,"
              + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
              + "    PRIMARY KEY (db_id)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 34:
        apply("CREATE UNIQUE INDEX bid_order_id_height_idx ON bid_order(id, height DESC);");
      case 35:
        apply("CREATE INDEX bid_order_account_id_idx ON bid_order(account_id, height DESC);");
      case 36:
        apply("CREATE INDEX bid_order_asset_id_price_idx ON bid_order(asset_id, price DESC);");
      case 37:
        apply("CREATE INDEX bid_order_creation_idx ON bid_order(creation_height DESC);");
      case 38:
        apply("CREATE TABLE goods("
              + "    db_id BIGINT AUTO_INCREMENT,"
              + "    ID BIGINT NOT NULL,"
              + "    seller_id BIGINT NOT NULL,"
              + "    name VARCHAR(100) NOT NULL,"
              + "    description TEXT,"
              + "    tags VARCHAR(100),"
              + "    timestamp INT NOT NULL,"
              + "    quantity INT NOT NULL,"
              + "    price BIGINT NOT NULL,"
              + "    delisted BOOLEAN NOT NULL,"
              + "    height INT NOT NULL,"
              + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
              + "    PRIMARY KEY (db_id)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 39:
        apply("CREATE UNIQUE INDEX goods_id_height_idx ON goods(id, height DESC);");
      case 40:
        apply("CREATE INDEX goods_seller_id_name_idx ON goods(seller_id, NAME);");
      case 41:
        apply("CREATE index goods_timestamp_idx ON goods(timestamp DESC, height DESC);");
      case 42:
        apply("CREATE TABLE purchase("
              + "    db_id BIGINT AUTO_INCREMENT,"
              + "    id BIGINT NOT NULL,"
              + "    buyer_id BIGINT NOT NULL,"
              + "    goods_id BIGINT NOT NULL,"
              + "    seller_id BIGINT NOT NULL,"
              + "    quantity INT NOT NULL,"
              + "    price BIGINT NOT NULL,"
              + "    deadline INT NOT NULL,"
              + "    note BLOB,"
              + "    nonce VARBINARY(32),"
              + "    timestamp INT NOT NULL,"
              + "    pending BOOLEAN NOT NULL,"
              + "    goods BLOB,"
              + "    goods_nonce VARBINARY(32),"
              + "    refund_note BLOB,"
              + "    refund_nonce VARBINARY(32),"
              + "    has_feedback_notes BOOLEAN DEFAULT FALSE NOT NULL,"
              + "    has_public_feedbacks BOOLEAN DEFAULT FALSE NOT NULL,"
              + "    discount BIGINT NOT NULL,"
              + "    refund BIGINT NOT NULL,"
              + "    height INT NOT NULL,"
              + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
              + "    PRIMARY KEY (db_id)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 43:
        apply("CREATE UNIQUE INDEX purchase_id_height_idx ON purchase(id, height DESC);");
      case 44:
        apply("CREATE INDEX purchase_buyer_id_height_idx ON purchase(buyer_id, height DESC);");
      case 45:
        apply("CREATE INDEX purchase_seller_id_height_idx ON purchase(seller_id, height DESC);");
      case 46:
        apply("CREATE INDEX purchase_deadline_idx ON purchase(deadline DESC, height DESC);");
      case 47:
        apply("CREATE INDEX purchase_timestamp_idx ON purchase(timestamp DESC, id);");
      case 48:
        apply("CREATE TABLE account_asset("
              + "    db_id BIGINT AUTO_INCREMENT,"
              + "    account_id BIGINT NOT NULL,"
              + "    asset_id BIGINT NOT NULL,"
              + "    quantity BIGINT NOT NULL,"
              + "    unconfirmed_quantity BIGINT NOT NULL,"
              + "    height INT NOT NULL,"
              + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
              + "    PRIMARY KEY (db_id)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 49:
        apply("CREATE UNIQUE INDEX account_asset_id_height_idx ON account_asset(account_id, asset_id, height DESC);");
      case 50:
        apply("CREATE INDEX account_asset_quantity_idx ON account_asset(quantity DESC);");
      case 51:
        apply("CREATE TABLE purchase_feedback("
              + "    db_id BIGINT AUTO_INCREMENT,"
              + "    id BIGINT NOT NULL,"
              + "    feedback_data BLOB NOT NULL,"
              + "    feedback_nonce VARBINARY(32) NOT NULL,"
              + "    height INT NOT NULL,"
              + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
              + "    PRIMARY KEY (db_id)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 52:
        apply("CREATE INDEX purchase_feedback_id_height_idx ON purchase_feedback(id, height DESC);");
      case 53:
        apply("CREATE TABLE purchase_public_feedback("
              + "    db_id BIGINT AUTO_INCREMENT,"
              + "    id BIGINT NOT NULL,"
              + "    public_feedback TEXT NOT NULL,"
              + "    height INT NOT NULL,"
              + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
              + "    PRIMARY KEY (db_id)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 54:
        apply("CREATE INDEX purchase_public_feedback_id_height_idx ON purchase_public_feedback(id, height DESC);");
      case 55:
        apply("CREATE TABLE unconfirmed_transaction("
              + "    db_id BIGINT AUTO_INCREMENT,"
              + "    id BIGINT NOT NULL,"
              + "    expiration INT NOT NULL,"
              + "    transaction_height INT NOT NULL,"
              + "    fee_per_byte BIGINT NOT NULL,"
              + "    timestamp INT NOT NULL,"
              + "    transaction_bytes BLOB NOT NULL,"
              + "    height INT NOT NULL,"
              + "    PRIMARY KEY (db_id)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 56:
        apply("CREATE UNIQUE INDEX unconfirmed_transaction_id_idx ON unconfirmed_transaction(id);");
      case 57:
        apply("CREATE INDEX unconfirmed_transaction_height_fee_timestamp_idx ON unconfirmed_transaction(transaction_height, fee_per_byte DESC, timestamp);");
      case 58:
        apply("CREATE TABLE asset_transfer("
              + "    db_id BIGINT AUTO_INCREMENT,"
              + "    id BIGINT NOT NULL,"
              + "    asset_id BIGINT NOT NULL,"
              + "    sender_id BIGINT NOT NULL,"
              + "    recipient_id BIGINT NOT NULL,"
              + "    quantity BIGINT NOT NULL,"
              + "    timestamp INT NOT NULL,"
              + "    height INT NOT NULL,"
              + "    PRIMARY KEY (db_id)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 59:
        apply("CREATE UNIQUE INDEX asset_transfer_id_idx ON asset_transfer(id);");
      case 60:
        apply("CREATE INDEX asset_transfer_asset_id_idx ON asset_transfer(asset_id, height DESC);");
      case 61:
        apply("CREATE INDEX asset_transfer_sender_id_idx ON asset_transfer(sender_id, height DESC);");
      case 62:
        apply("CREATE INDEX asset_transfer_recipient_id_idx ON asset_transfer(recipient_id, height DESC);");
      case 63:
        apply("CREATE TABLE reward_recip_assign("
              + "    db_id BIGINT AUTO_INCREMENT,"
              + "    account_id BIGINT NOT NULL,"
              + "    prev_recip_id BIGINT NOT NULL,"
              + "    recip_id BIGINT NOT NULL,"
              + "    from_height INT NOT NULL,"
              + "    height INT NOT NULL,"
              + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
              + "    PRIMARY KEY (db_id)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 64:
        apply("CREATE UNIQUE INDEX reward_recip_assign_account_id_height_idx ON reward_recip_assign(account_id, height DESC);");
      case 65:
        apply("CREATE INDEX reward_recip_assign_recip_id_height_idx ON reward_recip_assign(recip_id, height DESC);");
      case 66:
        apply("CREATE TABLE escrow("
              + "    db_id BIGINT AUTO_INCREMENT,"
              + "    id BIGINT NOT NULL,"
              + "    sender_id BIGINT NOT NULL,"
              + "    recipient_id BIGINT NOT NULL,"
              + "    amount BIGINT NOT NULL,"
              + "    required_signers INT,"
              + "    deadline INT NOT NULL,"
              + "    deadline_action INT NOT NULL,"
              + "    height INT NOT NULL,"
              + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
              + "    PRIMARY KEY (db_id)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 67:
        apply("CREATE UNIQUE INDEX escrow_id_height_idx ON escrow(id, height DESC);");
      case 68:
        apply("CREATE INDEX escrow_sender_id_height_idx ON escrow(sender_id, height DESC);");
      case 69:
        apply("CREATE INDEX escrow_recipient_id_height_idx ON escrow(recipient_id, height DESC);");
      case 70:
        apply("CREATE INDEX escrow_deadline_height_idx ON escrow(deadline, height DESC);");
      case 71:
        apply("CREATE TABLE escrow_decision("
              + "    db_id BIGINT AUTO_INCREMENT,"
              + "    escrow_id BIGINT NOT NULL,"
              + "    account_id BIGINT NOT NULL,"
              + "    decision int NOT NULL,"
              + "    height INT NOT NULL,"
              + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
              + "    PRIMARY KEY (db_id)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 72:
        apply("CREATE UNIQUE INDEX escrow_decision_escrow_id_account_id_height_idx ON escrow_decision(escrow_id, account_id, height DESC);");
      case 73:
        apply("CREATE INDEX escrow_decision_escrow_id_height_idx ON escrow_decision(escrow_id, height DESC);");
      case 74:
        apply("CREATE INDEX escrow_decision_account_id_height_idx ON escrow_decision(account_id, height DESC);");
      case 75:
        apply("CREATE TABLE subscription("
              + "    db_id BIGINT AUTO_INCREMENT,"
              + "    id BIGINT NOT NULL,"
              + "    sender_id BIGINT NOT NULL,"
              + "    recipient_id BIGINT NOT NULL,"
              + "    amount BIGINT NOT NULL,"
              + "    frequency INT NOT NULL,"
              + "    time_next INT NOT NULL,"
              + "    height INT NOT NULL,"
              + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
              + "    PRIMARY KEY (db_id)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 76:
        apply("CREATE UNIQUE INDEX subscription_id_height_idx ON subscription(id, height DESC);");
      case 77:
        apply("CREATE INDEX subscription_sender_id_height_idx ON subscription(sender_id, height DESC);");
      case 78:
        apply("CREATE INDEX subscription_recipient_id_height_idx ON subscription(recipient_id, height DESC);");
      case 79:
        apply("CREATE TABLE at("
              + "    db_id BIGINT AUTO_INCREMENT,"
              + "    id BIGINT NOT NULL,"
              + "    creator_id BIGINT NOT NULL,"
              + "    name VARCHAR(30),"
              + "    description TEXT,"
              + "    version SMALLINT NOT NULL,"
              + "    csize INT NOT NULL,"
              + "    dsize INT NOT NULL,"
              + "    c_user_stack_bytes INT NOT NULL,"
              + "    c_call_stack_bytes INT NOT NULL,"
              + "    creation_height INT NOT NULL,"
              + "    ap_code BLOB NOT NULL,"
              + "    height INT NOT NULL,"
              + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
              + "    PRIMARY KEY (db_id)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 80:
        apply("CREATE UNIQUE INDEX at_id_height_idx ON at(id, height DESC);");
      case 81:
        apply("CREATE INDEX at_creator_id_height_idx ON at(creator_id, height DESC);");
      case 82:
        apply("CREATE TABLE at_state("
              + "    db_id BIGINT AUTO_INCREMENT,"
              + "    at_id BIGINT NOT NULL,"
              + "    state BLOB NOT NULL,"
              + "    prev_height INT NOT NULL,"
              + "    next_height INT NOT NULL,"
              + "    sleep_between INT NOT NULL,"
              + "    prev_balance BIGINT NOT NULL,"
              + "    freeze_when_same_balance BOOLEAN NOT NULL,"
              + "    min_activate_amount BIGINT NOT NULL,"
              + "    height INT NOT NULL,"
              + "    latest BOOLEAN DEFAULT TRUE NOT NULL,"
              + "    PRIMARY KEY (db_id)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 83:
        apply("CREATE UNIQUE INDEX at_state_at_id_height_idx ON at_state(at_id, height DESC);");
      case 84:
        apply("CREATE INDEX at_state_id_next_height_height_idx ON at_state(at_id, next_height, height DESC);");
      case 85:
        apply("CREATE TABLE block("
              + "    db_id BIGINT AUTO_INCREMENT,"
              + "    id BIGINT NOT NULL,"
              + "    version INT NOT NULL,"
              + "    timestamp INT NOT NULL,"
              + "    previous_block_id BIGINT,"
              + "    total_amount BIGINT NOT NULL,"
              + "    total_fee BIGINT NOT NULL,"
              + "    payload_length INT NOT NULL,"
              + "    generator_public_key VARBINARY(32) NOT NULL,"
              + "    previous_block_hash VARBINARY(32),"
              + "    cumulative_difficulty BLOB NOT NULL,"
              + "    base_target BIGINT NOT NULL,"
              + "    next_block_id BIGINT,"
              + "    height INT NOT NULL,"
              + "    generation_signature VARBINARY(64) NOT NULL,"
              + "    block_signature VARBINARY(64) NOT NULL,"
              + "    payload_hash VARBINARY(32) NOT NULL,"
              + "    generator_id BIGINT NOT NULL,"
              + "    nonce BIGINT NOT NULL,"
              + "    ats BLOB,"
              + "    PRIMARY KEY (db_id)"
              + ") ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
      case 86:
        apply("CREATE UNIQUE INDEX block_id_idx ON block(id);");
      case 87:
        apply("CREATE UNIQUE INDEX block_height_idx ON block(height);");
      case 88:
        apply("CREATE INDEX block_generator_id_idx ON block(generator_id);");
      case 89:
        apply("CREATE UNIQUE INDEX block_timestamp_idx ON block(timestamp DESC);");
      case 90:
        apply("ALTER TABLE transaction ADD CONSTRAINT constraint_ff FOREIGN KEY(block_id) REFERENCES block(id) ON DELETE CASCADE;");
      case 91:
        apply("ALTER TABLE block ADD CONSTRAINT constraint_3c5 FOREIGN KEY(next_block_id) REFERENCES block(id) ON DELETE SET NULL;");
      case 92:
        apply("ALTER TABLE block ADD CONSTRAINT constraint_3c FOREIGN KEY(previous_block_id) REFERENCES block(id) ON DELETE CASCADE;");
      case 93:
        apply("UPDATE version set next_update = '162';");
      case 163:
        apply("ALTER TABLE alias ALTER COLUMN alias_name_LOWER SET DEFAULT '';");
      case 164:
        apply("ALTER DATABASE "+catalog+" CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
      case 165:
        apply("ALTER TABLE alias CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
      case 166:
        apply("ALTER TABLE account CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
      case 167:
        apply("ALTER TABLE asset CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
      case 168:
        apply("ALTER TABLE goods CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
      case 169:
        apply("ALTER TABLE at CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
      case 170:
        apply("ALTER TABLE goods CHANGE COLUMN ID id BIGINT NOT NULL;");
      case 171:
        apply("ALTER TABLE account CHANGE COLUMN creation_HEIGHT creation_height INT NOT NULL;");
      case 172:
        apply("ALTER TABLE alias_offer CHANGE COLUMN buyer_ID buyer_id BIGINT;");
      case 173:
        apply("ALTER TABLE transaction CHANGE COLUMN referenced_transaction_full_hash referenced_transaction_fullhash VARBINARY(32);");
      case 174:
        apply("ALTER TABLE alias CHANGE COLUMN alias_name_LOWER alias_name_lower VARCHAR(100) NOT NULL;");
      case 175:
        apply("CREATE INDEX account_id_latest_idx ON account(id, latest);");
      case 176:
        // doing index things works only with super privileges; to avoid weird exceptions for
        // those who never had the index, we check for the prior version of the db
        apply( initialDbVersion == 0 ? "UPDATE version set next_update = '176';" : "DROP TRIGGER IF EXISTS lower_alias_name_insert;");
      case 177:
        apply( initialDbVersion == 0 ? "UPDATE version set next_update = '177';" : "DROP TRIGGER IF EXISTS lower_alias_name_update;");
      case 178:
        return;
      default:
        throw new RuntimeException("Database inconsistent with code, probably trying to run older code on newer database");

    }
  }

  private MariadbDbVersion() {} //never
}
