/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `account`
--

DROP TABLE IF EXISTS `account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `account` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `creation_height` int(11) NOT NULL,
  `public_key` varbinary(32) DEFAULT NULL,
  `key_height` int(11) DEFAULT NULL,
  `balance` bigint(20) NOT NULL,
  `unconfirmed_balance` bigint(20) NOT NULL,
  `forged_balance` bigint(20) NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `account_id_height_idx` (`id`,`height`),
  KEY `account_id_balance_height_idx` (`id`,`balance`,`height`),
  KEY `account_id_latest_idx` (`id`,`latest`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `account_asset`
--

DROP TABLE IF EXISTS `account_asset`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `account_asset` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) NOT NULL,
  `asset_id` bigint(20) NOT NULL,
  `quantity` bigint(20) NOT NULL,
  `unconfirmed_quantity` bigint(20) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `account_asset_id_height_idx` (`account_id`,`asset_id`,`height`),
  KEY `account_asset_quantity_idx` (`quantity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `alias`
--

DROP TABLE IF EXISTS `alias`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `alias` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `alias_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `alias_name_lower` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `alias_uri` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `timestamp` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `alias_id_height_idx` (`id`,`height`),
  KEY `alias_account_id_idx` (`account_id`,`height`),
  KEY `alias_name_lower_idx` (`alias_name_lower`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `alias_offer`
--

DROP TABLE IF EXISTS `alias_offer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `alias_offer` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `price` bigint(20) NOT NULL,
  `buyer_id` bigint(20) DEFAULT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `alias_offer_id_height_idx` (`id`,`height`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ask_order`
--

DROP TABLE IF EXISTS `ask_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ask_order` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `asset_id` bigint(20) NOT NULL,
  `price` bigint(20) NOT NULL,
  `quantity` bigint(20) NOT NULL,
  `creation_height` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `ask_order_id_height_idx` (`id`,`height`),
  KEY `ask_order_account_id_idx` (`account_id`,`height`),
  KEY `ask_order_asset_id_price_idx` (`asset_id`,`price`),
  KEY `ask_order_creation_idx` (`creation_height`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `asset`
--

DROP TABLE IF EXISTS `asset`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `asset` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `name` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `quantity` bigint(20) NOT NULL,
  `decimals` tinyint(4) NOT NULL,
  `height` int(11) NOT NULL,
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `asset_id_idx` (`id`),
  KEY `asset_account_id_idx` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `asset_transfer`
--

DROP TABLE IF EXISTS `asset_transfer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `asset_transfer` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `asset_id` bigint(20) NOT NULL,
  `sender_id` bigint(20) NOT NULL,
  `recipient_id` bigint(20) NOT NULL,
  `quantity` bigint(20) NOT NULL,
  `timestamp` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `asset_transfer_id_idx` (`id`),
  KEY `asset_transfer_asset_id_idx` (`asset_id`,`height`),
  KEY `asset_transfer_sender_id_idx` (`sender_id`,`height`),
  KEY `asset_transfer_recipient_id_idx` (`recipient_id`,`height`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `at`
--

DROP TABLE IF EXISTS `at`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `at` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `creator_id` bigint(20) NOT NULL,
  `name` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `version` smallint(6) NOT NULL,
  `csize` int(11) NOT NULL,
  `dsize` int(11) NOT NULL,
  `c_user_stack_bytes` int(11) NOT NULL,
  `c_call_stack_bytes` int(11) NOT NULL,
  `creation_height` int(11) NOT NULL,
  `ap_code` blob NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `at_id_height_idx` (`id`,`height`),
  KEY `at_creator_id_height_idx` (`creator_id`,`height`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `at_state`
--

DROP TABLE IF EXISTS `at_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `at_state` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `at_id` bigint(20) NOT NULL,
  `state` blob NOT NULL,
  `prev_height` int(11) NOT NULL,
  `next_height` int(11) NOT NULL,
  `sleep_between` int(11) NOT NULL,
  `prev_balance` bigint(20) NOT NULL,
  `freeze_when_same_balance` tinyint(1) NOT NULL,
  `min_activate_amount` bigint(20) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `at_state_at_id_height_idx` (`at_id`,`height`),
  KEY `at_state_id_next_height_height_idx` (`at_id`,`next_height`,`height`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bid_order`
--

DROP TABLE IF EXISTS `bid_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bid_order` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `asset_id` bigint(20) NOT NULL,
  `price` bigint(20) NOT NULL,
  `quantity` bigint(20) NOT NULL,
  `creation_height` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `bid_order_id_height_idx` (`id`,`height`),
  KEY `bid_order_account_id_idx` (`account_id`,`height`),
  KEY `bid_order_asset_id_price_idx` (`asset_id`,`price`),
  KEY `bid_order_creation_idx` (`creation_height`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `block`
--

DROP TABLE IF EXISTS `block`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `block` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `timestamp` int(11) NOT NULL,
  `previous_block_id` bigint(20) DEFAULT NULL,
  `total_amount` bigint(20) NOT NULL,
  `total_fee` bigint(20) NOT NULL,
  `payload_length` int(11) NOT NULL,
  `generator_public_key` varbinary(32) NOT NULL,
  `previous_block_hash` varbinary(32) DEFAULT NULL,
  `cumulative_difficulty` blob NOT NULL,
  `base_target` bigint(20) NOT NULL,
  `next_block_id` bigint(20) DEFAULT NULL,
  `height` int(11) NOT NULL,
  `generation_signature` varbinary(64) NOT NULL,
  `block_signature` varbinary(64) NOT NULL,
  `payload_hash` varbinary(32) NOT NULL,
  `generator_id` bigint(20) NOT NULL,
  `nonce` bigint(20) NOT NULL,
  `ats` blob,
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `block_id_idx` (`id`),
  UNIQUE KEY `block_height_idx` (`height`),
  UNIQUE KEY `block_timestamp_idx` (`timestamp`),
  KEY `block_generator_id_idx` (`generator_id`),
  KEY `constraint_3c5` (`next_block_id`),
  KEY `constraint_3c` (`previous_block_id`),
  CONSTRAINT `constraint_3c` FOREIGN KEY (`previous_block_id`) REFERENCES `block` (`id`) ON DELETE CASCADE,
  CONSTRAINT `constraint_3c5` FOREIGN KEY (`next_block_id`) REFERENCES `block` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `escrow`
--

DROP TABLE IF EXISTS `escrow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `escrow` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `sender_id` bigint(20) NOT NULL,
  `recipient_id` bigint(20) NOT NULL,
  `amount` bigint(20) NOT NULL,
  `required_signers` int(11) DEFAULT NULL,
  `deadline` int(11) NOT NULL,
  `deadline_action` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `escrow_id_height_idx` (`id`,`height`),
  KEY `escrow_sender_id_height_idx` (`sender_id`,`height`),
  KEY `escrow_recipient_id_height_idx` (`recipient_id`,`height`),
  KEY `escrow_deadline_height_idx` (`deadline`,`height`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `escrow_decision`
--

DROP TABLE IF EXISTS `escrow_decision`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `escrow_decision` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `escrow_id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `decision` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `escrow_decision_escrow_id_account_id_height_idx` (`escrow_id`,`account_id`,`height`),
  KEY `escrow_decision_escrow_id_height_idx` (`escrow_id`,`height`),
  KEY `escrow_decision_account_id_height_idx` (`account_id`,`height`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `goods`
--

DROP TABLE IF EXISTS `goods`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `goods` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `seller_id` bigint(20) NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `tags` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `timestamp` int(11) NOT NULL,
  `quantity` int(11) NOT NULL,
  `price` bigint(20) NOT NULL,
  `delisted` tinyint(1) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `goods_id_height_idx` (`id`,`height`),
  KEY `goods_seller_id_name_idx` (`seller_id`,`name`),
  KEY `goods_timestamp_idx` (`timestamp`,`height`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `peer`
--

DROP TABLE IF EXISTS `peer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `peer` (
  `address` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `purchase`
--

DROP TABLE IF EXISTS `purchase`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `purchase` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `buyer_id` bigint(20) NOT NULL,
  `goods_id` bigint(20) NOT NULL,
  `seller_id` bigint(20) NOT NULL,
  `quantity` int(11) NOT NULL,
  `price` bigint(20) NOT NULL,
  `deadline` int(11) NOT NULL,
  `note` blob,
  `nonce` varbinary(32) DEFAULT NULL,
  `timestamp` int(11) NOT NULL,
  `pending` tinyint(1) NOT NULL,
  `goods` blob,
  `goods_nonce` varbinary(32) DEFAULT NULL,
  `refund_note` blob,
  `refund_nonce` varbinary(32) DEFAULT NULL,
  `has_feedback_notes` tinyint(1) NOT NULL DEFAULT '0',
  `has_public_feedbacks` tinyint(1) NOT NULL DEFAULT '0',
  `discount` bigint(20) NOT NULL,
  `refund` bigint(20) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `purchase_id_height_idx` (`id`,`height`),
  KEY `purchase_buyer_id_height_idx` (`buyer_id`,`height`),
  KEY `purchase_seller_id_height_idx` (`seller_id`,`height`),
  KEY `purchase_deadline_idx` (`deadline`,`height`),
  KEY `purchase_timestamp_idx` (`timestamp`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `purchase_feedback`
--

DROP TABLE IF EXISTS `purchase_feedback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `purchase_feedback` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `feedback_data` blob NOT NULL,
  `feedback_nonce` varbinary(32) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`db_id`),
  KEY `purchase_feedback_id_height_idx` (`id`,`height`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `purchase_public_feedback`
--

DROP TABLE IF EXISTS `purchase_public_feedback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `purchase_public_feedback` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `public_feedback` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`db_id`),
  KEY `purchase_public_feedback_id_height_idx` (`id`,`height`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reward_recip_assign`
--

DROP TABLE IF EXISTS `reward_recip_assign`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `reward_recip_assign` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) NOT NULL,
  `prev_recip_id` bigint(20) NOT NULL,
  `recip_id` bigint(20) NOT NULL,
  `from_height` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `reward_recip_assign_account_id_height_idx` (`account_id`,`height`),
  KEY `reward_recip_assign_recip_id_height_idx` (`recip_id`,`height`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `subscription`
--

DROP TABLE IF EXISTS `subscription`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `subscription` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `sender_id` bigint(20) NOT NULL,
  `recipient_id` bigint(20) NOT NULL,
  `amount` bigint(20) NOT NULL,
  `frequency` int(11) NOT NULL,
  `time_next` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `subscription_id_height_idx` (`id`,`height`),
  KEY `subscription_sender_id_height_idx` (`sender_id`,`height`),
  KEY `subscription_recipient_id_height_idx` (`recipient_id`,`height`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `trade`
--

DROP TABLE IF EXISTS `trade`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `trade` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `asset_id` bigint(20) NOT NULL,
  `block_id` bigint(20) NOT NULL,
  `ask_order_id` bigint(20) NOT NULL,
  `bid_order_id` bigint(20) NOT NULL,
  `ask_order_height` int(11) NOT NULL,
  `bid_order_height` int(11) NOT NULL,
  `seller_id` bigint(20) NOT NULL,
  `buyer_id` bigint(20) NOT NULL,
  `quantity` bigint(20) NOT NULL,
  `price` bigint(20) NOT NULL,
  `timestamp` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `trade_ask_bid_idx` (`ask_order_id`,`bid_order_id`),
  KEY `trade_asset_id_idx` (`asset_id`,`height`),
  KEY `trade_seller_id_idx` (`seller_id`,`height`),
  KEY `trade_buyer_id_idx` (`buyer_id`,`height`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `transaction`
--

DROP TABLE IF EXISTS `transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `transaction` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `deadline` smallint(6) NOT NULL,
  `sender_public_key` varbinary(32) NOT NULL,
  `recipient_id` bigint(20) DEFAULT NULL,
  `amount` bigint(20) NOT NULL,
  `fee` bigint(20) NOT NULL,
  `height` int(11) NOT NULL,
  `block_id` bigint(20) NOT NULL,
  `signature` varbinary(64) DEFAULT NULL,
  `timestamp` int(11) NOT NULL,
  `type` tinyint(4) NOT NULL,
  `subtype` tinyint(4) NOT NULL,
  `sender_id` bigint(20) NOT NULL,
  `block_timestamp` int(11) NOT NULL,
  `full_hash` varbinary(32) NOT NULL,
  `referenced_transaction_fullhash` varbinary(32) DEFAULT NULL,
  `attachment_bytes` blob,
  `version` tinyint(4) NOT NULL,
  `has_message` tinyint(1) NOT NULL DEFAULT '0',
  `has_encrypted_message` tinyint(1) NOT NULL DEFAULT '0',
  `has_public_key_announcement` tinyint(1) NOT NULL DEFAULT '0',
  `ec_block_height` int(11) DEFAULT NULL,
  `ec_block_id` bigint(20) DEFAULT NULL,
  `has_encrypttoself_message` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `transaction_id_idx` (`id`),
  UNIQUE KEY `transaction_full_hash_idx` (`full_hash`),
  KEY `transaction_block_timestamp_idx` (`block_timestamp`),
  KEY `transaction_sender_id_idx` (`sender_id`),
  KEY `transaction_recipient_id_idx` (`recipient_id`),
  KEY `transaction_recipient_id_amount_height_idx` (`recipient_id`,`amount`,`height`),
  KEY `constraint_ff` (`block_id`),
  CONSTRAINT `constraint_ff` FOREIGN KEY (`block_id`) REFERENCES `block` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `unconfirmed_transaction`
--

DROP TABLE IF EXISTS `unconfirmed_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `unconfirmed_transaction` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `expiration` int(11) NOT NULL,
  `transaction_height` int(11) NOT NULL,
  `fee_per_byte` bigint(20) NOT NULL,
  `timestamp` int(11) NOT NULL,
  `transaction_bytes` blob NOT NULL,
  `height` int(11) NOT NULL,
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `unconfirmed_transaction_id_idx` (`id`),
  KEY `unconfirmed_transaction_height_fee_timestamp_idx` (`transaction_height`,`fee_per_byte`,`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `version`
--

DROP TABLE IF EXISTS `version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `version` (
  `next_update` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `version` (`next_update`) VALUES (178);

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
