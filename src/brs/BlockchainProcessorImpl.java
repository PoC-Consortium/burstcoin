package brs;

import static brs.Constants.FEE_QUANT;
import static brs.Constants.ONE_BURST;
import static brs.fluxcapacitor.FeatureToggle.PRE_DYMAXION;
import static brs.fluxcapacitor.FeatureToggle.SLOT_FEE_ENFORCING;
import static brs.fluxcapacitor.FeatureToggle.SLOT_FEE_ENFORCING_OPTION_2;

import brs.props.Props;
import brs.db.cache.DBCacheManagerImpl;
import brs.db.store.BlockchainStore;
import brs.db.store.DerivedTableManager;
import brs.db.store.Stores;
import brs.fluxcapacitor.FeatureToggle;
import brs.fluxcapacitor.FluxInt;
import brs.services.BlockService;
import brs.services.EscrowService;
import brs.props.PropertyService;
import brs.services.SubscriptionService;
import brs.services.TimeService;
import brs.services.TransactionService;
import brs.statistics.StatisticsManagerImpl;
import brs.services.AccountService;
import brs.transactionduplicates.TransactionDuplicatesCheckerImpl;
import brs.unconfirmedtransactions.UnconfirmedTransactionStore;
import brs.util.ThreadPool;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import brs.at.AT_Block;
import brs.at.AT_Controller;
import brs.at.AT_Exception;
import brs.crypto.Crypto;
import brs.db.BlockDb;
import brs.db.DerivedTable;
import brs.peer.Peer;
import brs.peer.Peers;
import brs.util.Convert;
import brs.util.DownloadCacheImpl;
import brs.util.JSON;
import brs.util.Listener;
import brs.util.Listeners;
import brs.db.sql.Db;
import org.jooq.DSLContext;

public final class BlockchainProcessorImpl implements BlockchainProcessor {

  private final Logger logger = LoggerFactory.getLogger(BlockchainProcessorImpl.class);
  private final Stores stores;
  private BlockchainImpl blockchain;
  private BlockService blockService;
  private AccountService accountService;
  private final SubscriptionService subscriptionService;
  private final EscrowService escrowService;
  private final TimeService timeService;
  private final TransactionService transactionService;
  private TransactionProcessorImpl transactionProcessor;
  private EconomicClustering economicClustering;
  private BlockchainStore blockchainStore;
  private BlockDb blockDb;
  private TransactionDb transactionDb;
  private DownloadCacheImpl downloadCache;
  private DerivedTableManager derivedTableManager;
  private final StatisticsManagerImpl statisticsManager;
  private Generator generator;
  private final DBCacheManagerImpl dbCacheManager;

  public static final int MAX_TIMESTAMP_DIFFERENCE = 15;
  private boolean oclVerify;
  private int oclUnverifiedQueue;

  private final Semaphore gpuUsage = new Semaphore(2);

  private boolean trimDerivedTables;
  private volatile int lastTrimHeight;

  private final Listeners<Block, Event> blockListeners = new Listeners<>();
  private volatile Peer lastBlockchainFeeder;
  private volatile int lastBlockchainFeederHeight;
  private volatile boolean getMoreBlocks = true;

  private volatile boolean isScanning;
  private boolean forceScan;
  private boolean validateAtScan;

  private boolean forgeFatBlocks;

  private Integer ttsd;

  private final Runnable debugInfoThread = () -> {
    logger.info("Unverified blocks: " + downloadCache.getUnverifiedSize());
    logger.info("Blocks in cache: " + downloadCache.size());
    logger.info("Bytes in cache: " + downloadCache.getBlockCacheSize());
  };

  public final void setOclVerify(Boolean b) {
    oclVerify = b;
  }

  public final Boolean getOclVerify() {
    return oclVerify;
  }

  public BlockchainProcessorImpl(ThreadPool threadPool, BlockService blockService, TransactionProcessorImpl transactionProcessor, BlockchainImpl blockchain,
      PropertyService propertyService,
      SubscriptionService subscriptionService, TimeService timeService, DerivedTableManager derivedTableManager,
      BlockDb blockDb, TransactionDb transactionDb, EconomicClustering economicClustering, BlockchainStore blockchainStore, Stores stores, EscrowService escrowService,
      TransactionService transactionService, DownloadCacheImpl downloadCache, Generator generator, StatisticsManagerImpl statisticsManager, DBCacheManagerImpl dbCacheManager,
      AccountService accountService) {
    this.blockService = blockService;
    this.transactionProcessor = transactionProcessor;
    this.timeService = timeService;
    this.derivedTableManager = derivedTableManager;
    this.blockDb = blockDb;
    this.transactionDb = transactionDb;
    this.blockchain = blockchain;
    this.subscriptionService = subscriptionService;
    this.blockchainStore = blockchainStore;
    this.stores = stores;
    this.downloadCache = downloadCache;
    this.generator = generator;
    this.economicClustering = economicClustering;
    this.escrowService = escrowService;
    this.transactionService = transactionService;
    this.statisticsManager = statisticsManager;
    this.dbCacheManager = dbCacheManager;
    this.accountService = accountService;

    oclVerify = propertyService.getBoolean(Props.GPU_ACCELERATION); // use GPU acceleration ?
    oclUnverifiedQueue = propertyService.getInt(Props.GPU_UNVERIFIED_QUEUE);

    trimDerivedTables = propertyService.getBoolean(Props.DB_TRIM_DERIVED_TABLES);

    forceScan = propertyService.getBoolean(Props.DEV_FORCE_SCAN);
    validateAtScan = propertyService.getBoolean(Props.DEV_FORCE_VALIDATE);
    forgeFatBlocks = "fat".equals(propertyService.getString(Props.BRS_FORGING_STRATEGY));

    if(forgeFatBlocks) {
      ttsd = 400;
    }

    blockListeners.addListener(block -> {
      if (block.getHeight() % 5000 == 0) {
        logger.info("processed block " + block.getHeight());
      }
    }, Event.BLOCK_SCANNED);

    blockListeners.addListener(block -> {
      if (block.getHeight() % 5000 == 0) {
        logger.info("processed block " + block.getHeight());
        // Db.analyzeTables(); no-op
      }
    }, Event.BLOCK_PUSHED);

    blockListeners.addListener(block -> {
      transactionProcessor.revalidateUnconfirmedTransactions();
    }, Event.BLOCK_PUSHED);

    if (trimDerivedTables) {
      blockListeners.addListener(block -> {
        if (block.getHeight() % 1440 == 0) {
          lastTrimHeight = Math.max(block.getHeight() - Constants.MAX_ROLLBACK, 0);
          if (lastTrimHeight > 0) {
            this.derivedTableManager.getDerivedTables().forEach(table -> table.trim(lastTrimHeight));
          }
        }
      }, Event.AFTER_BLOCK_APPLY);
    }
    // No-op
    // blockListeners.addListener(new Listener<Block>() {
    // @Override
    // public void notify(Block block) {
    // Db.analyzeTables();
    // }
    // }, Event.RESCAN_END);

    threadPool.runBeforeStart(() -> {
      addGenesisBlock();
      if (forceScan) {
        scan(0);
      }
    }, false);

    threadPool.scheduleThread("GetMoreBlocks", getMoreBlocksThread, 2);
    threadPool.scheduleThread("ImportBlocks", blockImporterThread, 10);
    if (propertyService.getBoolean(Props.GPU_ACCELERATION)) {
      logger.debug("Starting preverifier thread in Open CL mode.");
      threadPool.scheduleThread("VerifyPoc", pocVerificationThread, 9);
    } else {
      logger.debug("Starting preverifier thread in CPU mode.");
      threadPool.scheduleThreadCores("VerifyPoc", pocVerificationThread, 9);
    }

    // ThreadPool.scheduleThread("Info", debugInfoThread, 5);
  }

  private final Runnable pocVerificationThread = () -> {
    boolean verifyWithOcl;
    int queueThreshold = oclVerify ? oclUnverifiedQueue : 0;

    while (!Thread.interrupted() && ThreadPool.running.get() ) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
      int unVerified = downloadCache.getUnverifiedSize();
      if (unVerified > queueThreshold) { //Is there anything to verify
        if (unVerified >= oclUnverifiedQueue && oclVerify) { //should we use Ocl?
          verifyWithOcl = true;
          if (!gpuUsage.tryAcquire()) { //is Ocl ready ?
            logger.debug("already max locked");
            verifyWithOcl = false;
          }
        }else {
          verifyWithOcl = false;
        }
        if(verifyWithOcl) {
          int poCVersion = 1;
          int pos = 0;
          List<Block> blocks = new LinkedList<>();
          poCVersion = downloadCache.getPoCVersion(downloadCache.getUnverifiedBlockIdFromPos(0));
          while (!Thread.interrupted() && ThreadPool.running.get()
              && (downloadCache.getUnverifiedSize() - 1) > pos
              && blocks.size() < OCLPoC.getMaxItems()) {
            long blockId = downloadCache.getUnverifiedBlockIdFromPos(pos);
            if (downloadCache.getPoCVersion(blockId) != poCVersion) {
              break;
            }
            blocks.add(downloadCache.getBlock(blockId));
            pos+=1;
          }
          try {
            OCLPoC.validatePoC(blocks, poCVersion, blockService);
            downloadCache.removeUnverifiedBatch(blocks);
          } catch (OCLPoC.PreValidateFailException e) {
            logger.info(e.toString(), e);
            blacklistClean(e.getBlock(), e, "found invalid pull/push data during processing the pocVerification");
          } catch (OCLPoC.OCLCheckerException e) {
            logger.info("Open CL error. slow verify will occur for the next "+oclUnverifiedQueue+" Blocks", e);
          } catch (Exception e) {
            logger.info("Unspecified Open CL error: ", e);
          } finally {
            gpuUsage.release();
          }
        }else { //verify using java
          try {
            blockService.preVerify(downloadCache.getFirstUnverifiedBlock());
          } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
          } catch (BlockchainProcessor.BlockNotAcceptedException e) {
            logger.error("Block failed to preverify: ", e);
          }
        }
      }
    }
  };

  private final Runnable blockImporterThread = () -> {
    while (!Thread.interrupted() && ThreadPool.running.get() && downloadCache.size() > 0) {
      try {
        Block lastBlock = blockchain.getLastBlock();
        Long lastId = lastBlock.getId();
        Block currentBlock = downloadCache.getNextBlock(lastId); /* this should fetch first block in cache */
        if (currentBlock == null || currentBlock.getHeight() != (lastBlock.getHeight() + 1)) {
          logger.debug("cache is reset due to orphaned block(s). CacheSize: " + downloadCache.size());
          downloadCache.resetCache(); //resetting cache because we have blocks that cannot be processed.
          break;
        }
        try {
          if (!currentBlock.isVerified()) {
            downloadCache.removeUnverified(currentBlock.getId());
            blockService.preVerify(currentBlock);
            logger.debug("block was not preverified");
          }
          lastId = currentBlock.getId();
          pushBlock(currentBlock); //pushblock removes the block from cache.
        } catch ( InterruptedException e ) {
          Thread.currentThread().interrupt();
        } catch (BlockNotAcceptedException e) {
          logger.error("Block not accepted", e);
          blacklistClean(currentBlock, e,
              "found invalid pull/push data during importing the block");
          break;
        }
      } catch (Throwable exception) {
        logger.error("Uncaught exception in blockImporterThread", exception);
      }
    }
  };

  private void blacklistClean(Block block, Exception e, String description) {
    logger.debug("Blacklisting peer and cleaning cache queue");
    if (block == null) {
      return;
    }
    Peer peer = block.getPeer();
    if (peer != null) {
      peer.blacklist(e, description);
    }
    downloadCache.resetCache();
    logger.debug("Blacklisted peer and cleaned queue");
  }

  private final Runnable getMoreBlocksThread = new Runnable() {
    private final JSONStreamAware getCumulativeDifficultyRequest;

    {
      JSONObject request = new JSONObject();
      request.put("requestType", "getCumulativeDifficulty");
      getCumulativeDifficultyRequest = JSON.prepareRequest(request);
    }

    private boolean peerHasMore;

    @Override
    public void run() {
      while (!Thread.currentThread().isInterrupted() && ThreadPool.running.get()) {
        try {
          try {
            if (!getMoreBlocks) {
              return;
            }
            //unlocking cache for writing.
            //This must be done before we query where to add blocks.
            //We sync the cache in event of popoff
            synchronized (downloadCache){
              downloadCache.unlockCache();
            }
            
            
            if (downloadCache.isFull()) {
              return;
            }
            peerHasMore = true;
            Peer peer = Peers.getAnyPeer(Peer.State.CONNECTED);
            if (peer == null) {
              logger.debug("No peer connected.");
              return;
            }
            JSONObject response = peer.send(getCumulativeDifficultyRequest);
            if (response == null) {
              // logger.debug("Peer Response is null");
              return;
            }
            if (response.get("blockchainHeight") != null) {
              lastBlockchainFeeder = peer;
              lastBlockchainFeederHeight = ((Long) response.get("blockchainHeight")).intValue();
            } else {
              logger.debug("Peer has no chainheight");
              return;
            }

            /* Cache now contains Cumulative Difficulty */

            BigInteger curCumulativeDifficulty = downloadCache.getCumulativeDifficulty();
            String peerCumulativeDifficulty = (String) response.get("cumulativeDifficulty");
            if (peerCumulativeDifficulty == null) {
              logger.debug("Peer CumulativeDifficulty is null");
              return;
            }
            BigInteger betterCumulativeDifficulty = new BigInteger(peerCumulativeDifficulty);
            if (betterCumulativeDifficulty.compareTo(curCumulativeDifficulty) < 0) {
              // logger.debug("Peer has lower chain or is on bad fork.");
              return;
            }
            if (betterCumulativeDifficulty.equals(curCumulativeDifficulty)) {
              // logger.debug("We are on same height.");
              return;
            }

            long commonBlockId = Genesis.GENESIS_BLOCK_ID;
            long cacheLastBlockId = downloadCache.getLastBlockId();

            // Now we will find the highest common block between ourself and our peer
            if (cacheLastBlockId != Genesis.GENESIS_BLOCK_ID) {
              commonBlockId = getCommonMilestoneBlockId(peer);
              if (commonBlockId == 0 || !peerHasMore) {
                logger.debug("We could not get a common milestone block from peer.");
                return;
              }
            }

            /*
             * if we did not get the last block in chain as common block we will be downloading a
             * fork. however if it is to far off we cannot process it anyway. canBeFork will check
             * where in chain this common block is fitting and return true if it is worth to
             * continue.
             */

            boolean saveInCache = true;
            if (commonBlockId != cacheLastBlockId) {
              if (downloadCache.canBeFork(commonBlockId)) {
                // the fork is not that old. Lets see if we can get more precise.
                commonBlockId = getCommonBlockId(peer, commonBlockId);
                if (commonBlockId == 0 || !peerHasMore) {
                  logger.debug("Trying to get a more precise common block resulted in an error.");
                  return;
                }
                saveInCache = false;
                downloadCache.resetForkBlocks();
              } else {
                logger.warn("Our peer want to feed us a fork that is more than "
                    + Constants.MAX_ROLLBACK + " blocks old.");
                return;
              }
            }

         //   List<Block> forkBlocks = new ArrayList<>();
            JSONArray nextBlocks = getNextBlocks(peer, commonBlockId);
            if (nextBlocks == null || nextBlocks.isEmpty()) {
              logger.debug("Peer did not feed us any blocks");
              return;
            }

            // download blocks from peer
            Block lastBlock = downloadCache.getBlock(commonBlockId);
            if(lastBlock == null) {
              logger.info("Error: lastBlock is null");
              return;
            }
            // loop blocks and make sure they fit in chain
                       
            Block block;
            JSONObject blockData;
            List<Block> blocks = new ArrayList<>();
              
            for (Object o : nextBlocks) {
              int height = lastBlock.getHeight() + 1;
              blockData = (JSONObject) o;
              try {
                block = Block.parseBlock(blockData, height);
                if (block == null) {
                  logger.debug("Unable to process downloaded blocks.");
                  return;
                }
                // Make sure it maps back to chain
                if (lastBlock.getId() != block.getPreviousBlockId()) {
                  logger.debug("Discarding downloaded data. Last downloaded blocks is rubbish");
                  logger.debug("DB blockID: " + lastBlock.getId() + " DB blockheight:"
                      + lastBlock.getHeight() + " Downloaded previd:"
                      + block.getPreviousBlockId());
                  return;
                }
                // set height and cumulative difficulty to block
                block.setHeight(height);
                block.setPeer(peer);
                block.setByteLength(blockData.toString().length());
                blockService.calculateBaseTarget(block, lastBlock);
                if (saveInCache) {
                  if (downloadCache.getLastBlockId() == block.getPreviousBlockId()) { //still maps back? we might have got announced/forged blocks
                    if(! downloadCache.addBlock(block)) {
                      //we stop the loop since cahce has been locked
                      return;
                    }
                    logger.debug("Added from download: Id: " +block.getId()+" Height: "+block.getHeight());
                  }
                } else {
                  downloadCache.addForkBlock(block);
                }
                lastBlock = block;
              } catch (BlockOutOfOrderException e) {
                logger.info(e.toString() + " - autoflushing cache to get rid of it", e);
                downloadCache.resetCache();
                return;
              } catch (RuntimeException | BurstException.ValidationException e) {
                logger.info("Failed to parse block: {}" + e.toString(), e);
                logger.info("Failed to parse block trace: " + e.getStackTrace());
                peer.blacklist(e, "pulled invalid data using getCumulativeDifficulty");
                return;
              } catch (Exception e) {
                logger.warn("Unhandled exception {}" + e.toString(), e);
                logger.warn("Unhandled exception trace: " + e.getStackTrace());
              }
              //executor shutdown?
              if (Thread.currentThread().isInterrupted())
                return;
            } // end block loop

            logger.trace("Unverified blocks: " + downloadCache.getUnverifiedSize());
            logger.trace("Blocks in cache: {}", downloadCache.size());
            logger.trace("Bytes in cache: " + downloadCache.getBlockCacheSize());
            if (!saveInCache) {
              /*
               * Since we cannot rely on peers reported cumulative difficulty we do
               * a final check to see that the CumulativeDifficulty actually is bigger
               * before we do a popOff and switch chain.
               */
              if(lastBlock.getCumulativeDifficulty().compareTo(curCumulativeDifficulty) < 0) {
                peer.blacklist("peer claimed to have bigger cumulative difficulty but in reality it did not.");
                downloadCache.resetForkBlocks();
                break;
              }
              processFork(peer, downloadCache.getForkList(), commonBlockId);
            }

          } catch (BurstException.StopException e) {
            logger.info("Blockchain download stopped: " + e.getMessage());
          } catch (Exception e) {
            logger.info("Error in blockchain download thread", e);
          } // end second try
        } catch (Throwable t) {
          logger.info("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
          System.exit(1);
        } // end first try
      } // end while
    }

    private long getCommonMilestoneBlockId(Peer peer) throws InterruptedException {

      String lastMilestoneBlockId = null;

      while (!Thread.currentThread().isInterrupted() && ThreadPool.running.get()) {
        JSONObject milestoneBlockIdsRequest = new JSONObject();
        milestoneBlockIdsRequest.put("requestType", "getMilestoneBlockIds");
        if (lastMilestoneBlockId == null) {
          milestoneBlockIdsRequest.put("lastBlockId",
              Convert.toUnsignedLong(downloadCache.getLastBlockId()));
        } else {
          milestoneBlockIdsRequest.put("lastMilestoneBlockId", lastMilestoneBlockId);
        }

        JSONObject response = peer.send(JSON.prepareRequest(milestoneBlockIdsRequest));
        if (response == null) {
          logger.debug("Got null response in getCommonMilestoneBlockId");
          return 0;
        }
        JSONArray milestoneBlockIds = (JSONArray) response.get("milestoneBlockIds");
        if (milestoneBlockIds == null) {
          logger.debug("MilestoneArray is null");
          return 0;
        }
        if (milestoneBlockIds.isEmpty()) {
          return Genesis.GENESIS_BLOCK_ID;
        }
        // prevent overloading with blockIds
        if (milestoneBlockIds.size() > 20) {
          peer.blacklist("obsolete or rogue peer sends too many milestoneBlockIds");
          return 0;
        }
        if (Boolean.TRUE.equals(response.get("last"))) {
          peerHasMore = false;
        }

        for (Object milestoneBlockId : milestoneBlockIds) {
          long blockId = Convert.parseUnsignedLong((String) milestoneBlockId);

          if (downloadCache.hasBlock(blockId)) {
            if (lastMilestoneBlockId == null && milestoneBlockIds.size() > 1) {
              peerHasMore = false;
              logger.debug("Peer dont have more (cache)");
            }
            return blockId;
          }
          lastMilestoneBlockId = (String) milestoneBlockId;
        }
      }
      throw new InterruptedException("interrupted");
    }

    private long getCommonBlockId(Peer peer, long commonBlockId) throws InterruptedException {

      while (!Thread.currentThread().isInterrupted() && ThreadPool.running.get()) {
        JSONObject request = new JSONObject();
        request.put("requestType", "getNextBlockIds");
        request.put("blockId", Convert.toUnsignedLong(commonBlockId));
        JSONObject response = peer.send(JSON.prepareRequest(request));
        if (response == null) {
          return 0;
        }
        JSONArray nextBlockIds = (JSONArray) response.get("nextBlockIds");
        if (nextBlockIds == null || nextBlockIds.isEmpty()) {
          return 0;
        }
        // prevent overloading with blockIds
        if (nextBlockIds.size() > 1440) {
          peer.blacklist("obsolete or rogue peer sends too many nextBlocks");
          return 0;
        }

        for (Object nextBlockId : nextBlockIds) {
          long blockId = Convert.parseUnsignedLong((String) nextBlockId);
          if (!downloadCache.hasBlock(blockId)) {
            return commonBlockId;
          }
          commonBlockId = blockId;
        }
      }

      throw new InterruptedException("interrupted");
    }

    private JSONArray getNextBlocks(Peer peer, long curBlockId) {

      JSONObject request = new JSONObject();
      request.put("requestType", "getNextBlocks");
      request.put("blockId", Convert.toUnsignedLong(curBlockId));
      logger.debug("Getting next Blocks after " + curBlockId + " from " + peer.getPeerAddress());
      JSONObject response = peer.send(JSON.prepareRequest(request));
      if (response == null) {
        return null;
      }

      JSONArray nextBlocks = (JSONArray) response.get("nextBlocks");
      if (nextBlocks == null) {
        return null;
      }
      // prevent overloading with blocks
      if (nextBlocks.size() > 1440) {
        peer.blacklist("obsolete or rogue peer sends too many nextBlocks");
        return null;
      }
      logger.debug("Got " + nextBlocks.size() + " Blocks after " + curBlockId + " from "
          + peer.getPeerAddress());
      return nextBlocks;

    }

  private void processFork(Peer peer, final List<Block> forkBlocks, long forkBlockId) {
    logger.warn("A fork is detected. Waiting for cache to be processed.");
    downloadCache.lockCache(); //dont let anything add to cache!
    while (!Thread.currentThread().isInterrupted() && ThreadPool.running.get()) {
      if (downloadCache.size() == 0) {
        break;
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
    synchronized (downloadCache) {
      synchronized (transactionProcessor.getUnconfirmedTransactionsSyncObj()) {
        logger.warn("Cache is now processed. Starting to process fork.");
        Block forkBlock = blockchain.getBlock(forkBlockId);

        // we read the current cumulative difficulty
        BigInteger curCumulativeDifficulty = blockchain.getLastBlock().getCumulativeDifficulty();

        // We remove blocks from chain back to where we start our fork
        // and save it in a list if we need to restore
        List<Block> myPoppedOffBlocks = popOffTo(forkBlock);

        // now we check that our chain is popped off.
        // If all seems ok is we try to push fork.
        int pushedForkBlocks = 0;
        if (blockchain.getLastBlock().getId() == forkBlockId) {
          for (Block block : forkBlocks) {
            if (blockchain.getLastBlock().getId() == block.getPreviousBlockId()) {
              try {
                blockService.preVerify(block);
                pushBlock(block);
                pushedForkBlocks += 1;
              } catch ( InterruptedException e ) {
                Thread.currentThread().interrupt();
              } catch (BlockNotAcceptedException e) {
                peer.blacklist(e, "during processing a fork");
                break;
              }
            }
          }
        }

        /*
         * we check if we succeeded to push any block. if we did we check against cumulative
         * difficulty If it is lower we blacklist peer and set chain to be processed later.
         */
        if (pushedForkBlocks > 0 && blockchain.getLastBlock().getCumulativeDifficulty()
            .compareTo(curCumulativeDifficulty) < 0) {
          logger.warn("Fork was bad and Pop off was caused by peer " + peer.getPeerAddress() + ", blacklisting");
          peer.blacklist("got a bad fork");
          List<Block> peerPoppedOffBlocks = popOffTo(forkBlock);
          pushedForkBlocks = 0;
          peerPoppedOffBlocks.forEach(block -> transactionProcessor.processLater(block.getTransactions()));
        }

        // if we did not push any blocks we try to restore chain.
        if (pushedForkBlocks == 0) {
          for (int i = myPoppedOffBlocks.size() - 1; i >= 0; i--) {
            Block block = myPoppedOffBlocks.remove(i);
            try {
              blockService.preVerify(block);
              pushBlock(block);
            } catch ( InterruptedException e ) {
              Thread.currentThread().interrupt();
            } catch (BlockNotAcceptedException e) {
              logger.warn("Popped off block no longer acceptable: " + block.getJSONObject().toJSONString(), e);
              break;
            }
          }
        } else {
          myPoppedOffBlocks.forEach(block -> transactionProcessor.processLater(block.getTransactions()));
          logger.warn("Successfully switched to better chain.");
        }
        logger.warn("Forkprocessing complete.");
        downloadCache.resetForkBlocks();
        downloadCache.resetCache(); // Reset and set cached vars to chaindata.
      }
    }
  }
};


  @Override
  public boolean addListener(Listener<Block> listener, BlockchainProcessor.Event eventType) {
    return blockListeners.addListener(listener, eventType);
  }

  @Override
  public boolean removeListener(Listener<Block> listener, Event eventType) {
    return blockListeners.removeListener(listener, eventType);
  }

  @Override
  public Peer getLastBlockchainFeeder() {
    return lastBlockchainFeeder;
  }

  @Override
  public int getLastBlockchainFeederHeight() {
    return lastBlockchainFeederHeight;
  }

  @Override
  public boolean isScanning() {
    return isScanning;
  }

  @Override
  public int getMinRollbackHeight() {
    return trimDerivedTables ? (lastTrimHeight > 0 ? lastTrimHeight
        : Math.max(blockchain.getHeight() - Constants.MAX_ROLLBACK, 0)) : 0;
  }

  @Override
  public void processPeerBlock(JSONObject request, Peer peer) throws BurstException {
    Block newBlock = Block.parseBlock(request, blockchain.getHeight());
    if (newBlock == null) {
      logger.debug("Peer {} has announced an unprocessable block.", peer.getPeerAddress());
      return;
    }
    /*
     * This process takes care of the blocks that is announced by peers We do not want to be
     * feeded forks.
     */
    Block chainblock = downloadCache.getLastBlock();
    if (chainblock.getId() == newBlock.getPreviousBlockId()) {
      newBlock.setHeight(chainblock.getHeight() + 1);
      newBlock.setByteLength(newBlock.toString().length());
      blockService.calculateBaseTarget(newBlock, chainblock);
      downloadCache.addBlock(newBlock);
      logger.debug("Peer {} added block from Announce: Id: {} Height: {}", peer.getPeerAddress(), newBlock.getId(), newBlock.getHeight());
    } else {
      logger.debug("Peer {} sent us block: {} which is not the follow-up block for {}", peer.getPeerAddress(), newBlock.getPreviousBlockId(), chainblock.getId());
    }
  }

  @Override
  public List<Block> popOffTo(int height) {
    return popOffTo(blockchain.getBlockAtHeight(height));
  }

  @Override
  public void fullReset() {
    // blockDb.deleteBlock(Genesis.GENESIS_BLOCK_ID); // fails with stack overflow in H2
    blockDb.deleteAll(false);
    dbCacheManager.flushCache();
    downloadCache.resetCache();
    addGenesisBlock();
    scan(0);
  }

  @Override
  public void forceScanAtStart() {
    forceScan = true;
  }

  @Override
  public void validateAtNextScan() {
    validateAtScan = true;
  }

  @Override
  public Integer getWalletTTSD() {
    return this.ttsd;
  }

  void setGetMoreBlocks(boolean getMoreBlocks) {
    this.getMoreBlocks = getMoreBlocks;
  }

  private void addBlock(Block block) {
    if (blockchainStore.addBlock(block)) {
      blockchain.setLastBlock(block);
    }
  }

  private void addGenesisBlock() {
    if (blockDb.hasBlock(Genesis.GENESIS_BLOCK_ID)) {
      logger.info("Genesis block already in database");
      Block lastBlock = blockDb.findLastBlock();
      blockchain.setLastBlock(lastBlock);
      logger.info("Last block height: " + lastBlock.getHeight());
      return;
    }
    logger.info("Genesis block not in database, starting from scratch");
    try {
      List<Transaction> transactions = new ArrayList<>();
      MessageDigest digest = Crypto.sha256();
      transactions.forEach(transaction -> digest.update(transaction.getBytes()));
      ByteBuffer bf = ByteBuffer.allocate(0);
      bf.order(ByteOrder.LITTLE_ENDIAN);
      byte[] byteATs = bf.array();
      Block genesisBlock = new Block(-1, 0, 0, 0, 0, transactions.size() * 128,
          digest.digest(), Genesis.getCreatorPublicKey(), new byte[32],
          Genesis.getGenesisBlockSignature(), null, transactions, 0, byteATs, -1);
      blockService.setPrevious(genesisBlock, null);
      addBlock(genesisBlock);
    } catch (BurstException.ValidationException e) {
      logger.info(e.getMessage());
      throw new RuntimeException(e.toString(), e);
    }
  }

  private void pushBlock(final Block block) throws BlockNotAcceptedException {
    if(ttsd != null) {
      ttsd--;

      if (ttsd < 0) {
        logger.warn("Tick tocks, been crafting too many fat blocks");
        Burst.shutdown(false);
        System.exit(0);
      }
    }

    synchronized (transactionProcessor.getUnconfirmedTransactionsSyncObj()) {
      stores.beginTransaction();
      int curTime = timeService.getEpochTime();

      Block previousLastBlock = null;
      try {

        previousLastBlock = blockchain.getLastBlock();

        if (previousLastBlock.getId() != block.getPreviousBlockId()) {
          throw new BlockOutOfOrderException(
              "Previous block id doesn't match for block " + block.getHeight()
                  + ((previousLastBlock.getHeight() + 1) == block.getHeight() ? "" : " invalid previous height " + previousLastBlock.getHeight())
          );
        }

        if (block.getVersion() != getBlockVersion()) {
          throw new BlockNotAcceptedException("Invalid version " + block.getVersion() + " for block " + block.getHeight());
        }

        if (block.getVersion() != 1
            && !Arrays.equals(Crypto.sha256().digest(previousLastBlock.getBytes()),
            block.getPreviousBlockHash())) {
          throw new BlockNotAcceptedException("Previous block hash doesn't match for block " + block.getHeight());
        }
        if (block.getTimestamp() > curTime + MAX_TIMESTAMP_DIFFERENCE
            || block.getTimestamp() <= previousLastBlock.getTimestamp()) {
          throw new BlockOutOfOrderException("Invalid timestamp: " + block.getTimestamp()
              + " current time is " + curTime
              + ", previous block timestamp is " + previousLastBlock.getTimestamp());
        }
        if (block.getId() == 0L || blockDb.hasBlock(block.getId())) {
          throw new BlockNotAcceptedException("Duplicate block or invalid id for block " + block.getHeight());
        }
        if (!blockService.verifyGenerationSignature(block)) {
          throw new BlockNotAcceptedException("Generation signature verification failed for block " + block.getHeight());
        }
        if (!blockService.verifyBlockSignature(block)) {
          throw new BlockNotAcceptedException("Block signature verification failed for block " + block.getHeight());
        }

        final TransactionDuplicatesCheckerImpl transactionDuplicatesChecker = new TransactionDuplicatesCheckerImpl();
        long calculatedTotalAmount = 0;
        long calculatedTotalFee = 0;
        MessageDigest digest = Crypto.sha256();

        ArrayList<Long> accountIds = new ArrayList<Long>();
        try (DSLContext ctx = Db.getDSLContext()) {
          block.getTransactions().forEach(t -> {
            if (t.getRecipientId() != 0L)
              accountIds.add(t.getRecipientId());
            if (t.getSenderId() != 0L)
              accountIds.add(t.getSenderId());
          });
          if (!accountIds.isEmpty()) {
            stores.getAccountStore().getAccountTable().fillCache(accountIds);
          }
        }

        long[] feeArray = new long[block.getTransactions().size()];
        long slotIdx = 0;

        for (Transaction transaction : block.getTransactions()) {
          if (transaction.getTimestamp() > curTime + MAX_TIMESTAMP_DIFFERENCE) {
            throw new BlockOutOfOrderException("Invalid transaction timestamp: "
                + transaction.getTimestamp() + ", current time is " + curTime);
          }
          if (transaction.getTimestamp() > block.getTimestamp() + MAX_TIMESTAMP_DIFFERENCE
              || transaction.getExpiration() < block.getTimestamp()) {
            throw new TransactionNotAcceptedException("Invalid transaction timestamp "
                + transaction.getTimestamp() + " for transaction " + transaction.getStringId()
                + ", current time is " + curTime + ", block timestamp is " + block.getTimestamp(),
                transaction);
          }
          if (transactionDb.hasTransaction(transaction.getId())) {
            throw new TransactionNotAcceptedException(
                "Transaction " + transaction.getStringId() + " is already in the blockchain",
                transaction);
          }
          if (transaction.getReferencedTransactionFullHash() != null) {
            if ((previousLastBlock.getHeight() < Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK
                && !transactionDb.hasTransaction(
                Convert.fullHashToId(transaction.getReferencedTransactionFullHash())))
                || (previousLastBlock
                .getHeight() >= Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK
                && !hasAllReferencedTransactions(transaction, transaction.getTimestamp(), 0))) {
              throw new TransactionNotAcceptedException("Missing or invalid referenced transaction "
                  + transaction.getReferencedTransactionFullHash() + " for transaction "
                  + transaction.getStringId(), transaction);
            }
          }
          if (transaction.getVersion() != transactionProcessor.getTransactionVersion(previousLastBlock.getHeight())) {
            throw new TransactionNotAcceptedException("Invalid transaction version "
                + transaction.getVersion() + " at height " + previousLastBlock.getHeight(),
                transaction);
          }

          if (!transactionService.verifyPublicKey(transaction)) {
            throw new TransactionNotAcceptedException("Wrong public key in transaction "
                + transaction.getStringId() + " at height " + previousLastBlock.getHeight(),
                transaction);
          }
          if (Burst.getFluxCapacitor().isActive(FeatureToggle.AUTOMATED_TRANSACTION_BLOCK)) {
            if (!economicClustering.verifyFork(transaction)) {
              logger.debug("Block " + block.getStringId() + " height "
                  + (previousLastBlock.getHeight() + 1)
                  + " contains transaction that was generated on a fork: "
                  + transaction.getStringId() + " ecBlockHeight " + transaction.getECBlockHeight()
                  + " ecBlockId " + Convert.toUnsignedLong(transaction.getECBlockId()));
              throw new TransactionNotAcceptedException("Transaction belongs to a different fork",
                  transaction);
            }
          }
          if (transaction.getId() == 0L) {
            throw new TransactionNotAcceptedException("Invalid transaction id", transaction);
          }

          if (transactionDuplicatesChecker.hasAnyDuplicate(transaction)) {
            throw new TransactionNotAcceptedException("Transaction is a duplicate: " + transaction.getStringId(), transaction);
          }

          try {
            transactionService.validate(transaction);
          } catch (BurstException.ValidationException e) {
            throw new TransactionNotAcceptedException(e.getMessage(), transaction);
          }

          calculatedTotalAmount += transaction.getAmountNQT();
          calculatedTotalFee += transaction.getFeeNQT();

          feeArray[slotIdx] = transaction.getFeeNQT();
          slotIdx += 1;

          digest.update(transaction.getBytes());
        }

        if (calculatedTotalAmount > block.getTotalAmountNQT()
            || calculatedTotalFee > block.getTotalFeeNQT()) {
          throw new BlockNotAcceptedException("Total amount or fee don't match transaction totals for block " + block.getHeight());
        }

        if (Burst.getFluxCapacitor().isActive(FeatureToggle.SLOT_FEE_ENFORCING_OPTION_2)) {
            Arrays.sort(feeArray);
            for (int i = 0; i < feeArray.length; i++) {
                if (feeArray[i] >= Constants.FEE_QUANT * (i + 1)) {
                    throw new BlockNotAcceptedException("Transaction fee is not enough to be included in this block " + block.getHeight());
                }
            }
        }

        if (Burst.getFluxCapacitor().isActive(FeatureToggle.SLOT_FEE_ENFORCING)
            && Constants.FEE_QUANT * ((block.getTransactions().size() / 2) * (1 + block.getTransactions().size())) > block.getTotalFeeNQT())) {
            throw new BlockNotAcceptedException("Total fees are not enough to match the number of transactions for block " + block.getHeight());
        }        

        if (!Arrays.equals(digest.digest(), block.getPayloadHash())) {
          throw new BlockNotAcceptedException("Payload hash doesn't match for block " + block.getHeight());
        }

        long remainingAmount =
            Convert.safeSubtract(block.getTotalAmountNQT(), calculatedTotalAmount);
        long remainingFee = Convert.safeSubtract(block.getTotalFeeNQT(), calculatedTotalFee);

        blockService.setPrevious(block, previousLastBlock);
        blockListeners.notify(block, Event.BEFORE_BLOCK_ACCEPT);
        transactionProcessor.removeForgedTransactions(block.getTransactions());
        transactionProcessor.requeueAllUnconfirmedTransactions();
        accountService.flushAccountTable();
        addBlock(block);
        downloadCache.removeBlock(block); //We make sure downloadCache do not have this block anymore.
        accept(block, remainingAmount, remainingFee);
        derivedTableManager.getDerivedTables().forEach(DerivedTable::finish);
        stores.commitTransaction();
      } catch (BlockNotAcceptedException | ArithmeticException e) {
        stores.rollbackTransaction();
        blockchain.setLastBlock(previousLastBlock);
        downloadCache.resetCache();
        throw e;
      } finally {
        stores.endTransaction();
      }
      logger.debug("Successfully pushed " + block.getId() + " (height " + block.getHeight() + ")");
      statisticsManager.blockAdded();
      blockListeners.notify(block, Event.BLOCK_PUSHED);
      if (block.getTimestamp() >= timeService.getEpochTime() - MAX_TIMESTAMP_DIFFERENCE) {
        Peers.sendToSomePeers(block);
      }
    }
  }

  private void accept(Block block, Long remainingAmount, Long remainingFee)
      throws BlockNotAcceptedException {
    subscriptionService.clearRemovals();
    for (Transaction transaction : block.getTransactions()) {
      if (!transactionService.applyUnconfirmed(transaction)) {
        throw new TransactionNotAcceptedException(
            "Double spending transaction: " + transaction.getStringId(), transaction);
      }
    }
    
    long calculatedRemainingAmount = 0;
    long calculatedRemainingFee = 0;
    // ATs
    AT_Block atBlock;
    AT.clearPendingFees();
    AT.clearPendingTransactions();
    try {
      atBlock = AT_Controller.validateATs(block.getBlockATs(), blockchain.getHeight());
    } catch (NoSuchAlgorithmException e) {
      // should never reach that point
      throw new BlockNotAcceptedException("md5 does not exist for block " + block.getHeight());
    } catch (AT_Exception e) {
      throw new BlockNotAcceptedException("ats are not matching at block height " + blockchain.getHeight() + " (" + e + ")");
    }
    calculatedRemainingAmount += atBlock.getTotalAmount();
    calculatedRemainingFee += atBlock.getTotalFees();
    // ATs
    if (subscriptionService.isEnabled()) {
      calculatedRemainingFee += subscriptionService.applyUnconfirmed(block.getTimestamp());
    }
    if (remainingAmount != null && remainingAmount != calculatedRemainingAmount) {
      throw new BlockNotAcceptedException("Calculated remaining amount doesn't add up for block " + block.getHeight());
    }
    if (remainingFee != null && remainingFee != calculatedRemainingFee) {
      throw new BlockNotAcceptedException("Calculated remaining fee doesn't add up for block " + block.getHeight());
    }
    blockListeners.notify(block, Event.BEFORE_BLOCK_APPLY);
    blockService.apply(block);
    subscriptionService.applyConfirmed(block, blockchain.getHeight());
    if (escrowService.isEnabled()) {
      escrowService.updateOnBlock(block, blockchain.getHeight());
    }
    blockListeners.notify(block, Event.AFTER_BLOCK_APPLY);
    if (! block.getTransactions().isEmpty()) {
      transactionProcessor.notifyListeners(block.getTransactions(),
          TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
    }
  }

  private List<Block> popOffTo(Block commonBlock) {
  
    if (commonBlock.getHeight() < getMinRollbackHeight()) {
        throw new IllegalArgumentException("Rollback to height " + commonBlock.getHeight()
            + " not suppported, " + "current height " + blockchain.getHeight());
    }
    if (!blockchain.hasBlock(commonBlock.getId())) {
      logger.debug("Block " + commonBlock.getStringId() + " not found in blockchain, nothing to pop off");
      return Collections.emptyList();
    }
    List<Block> poppedOffBlocks = new ArrayList<>();
    synchronized (downloadCache) {
      synchronized (transactionProcessor.getUnconfirmedTransactionsSyncObj()) {
        //Burst.getTransactionProcessor().clearUnconfirmedTransactions();
        try {
          stores.beginTransaction();
          Block block = blockchain.getLastBlock();
          logger.debug("Rollback from " + block.getHeight() + " to " + commonBlock.getHeight());
          while (block.getId() != commonBlock.getId() && block.getId() != Genesis.GENESIS_BLOCK_ID) {
            poppedOffBlocks.add(block);
            block = popLastBlock();
          }
          derivedTableManager.getDerivedTables().forEach(table -> table.rollback(commonBlock.getHeight()));
          dbCacheManager.flushCache();
          stores.commitTransaction();
          downloadCache.resetCache();
        } catch (RuntimeException e) {
          stores.rollbackTransaction();
          logger.debug("Error popping off to " + commonBlock.getHeight(), e);
          throw e;
        } finally {
          stores.endTransaction();
        }
      }
    }
    return poppedOffBlocks;
  }

  private Block popLastBlock() {
    Block block = blockchain.getLastBlock();
    if (block.getId() == Genesis.GENESIS_BLOCK_ID) {
      throw new RuntimeException("Cannot pop off genesis block");
    }
    Block previousBlock = blockDb.findBlock(block.getPreviousBlockId());
    blockchain.setLastBlock(block, previousBlock);
    block.getTransactions().forEach(Transaction::unsetBlock);
    blockDb.deleteBlocksFrom(block.getId());
    blockListeners.notify(block, Event.BLOCK_POPPED);
    return previousBlock;
  }

  int getBlockVersion() {
    return 3;
  }

  @Override
  public void generateBlock(String secretPhrase, byte[] publicKey, Long nonce) throws BlockNotAcceptedException {
    synchronized (downloadCache) {
      downloadCache.lockCache(); //stop all incoming blocks.
      UnconfirmedTransactionStore unconfirmedTransactionStore = stores.getUnconfirmedTransactionStore();
      SortedSet<Transaction> orderedBlockTransactions = new TreeSet<>();

      int blockSize   = Burst.getFluxCapacitor().getInt(FluxInt.MAX_NUMBER_TRANSACTIONS);
      int payloadSize = Burst.getFluxCapacitor().getInt(FluxInt.MAX_PAYLOAD_LENGTH);

      long totalAmountNQT = 0;
      long totalFeeNQT = 0;

      final Block previousBlock = blockchain.getLastBlock();
      final int blockTimestamp = timeService.getEpochTime();

      // this is just an validation. which collects all valid transactions, which fit into the block
      // finally all stuff is reverted so nothing is written to the db
      // the block itself with all transactions we found is pushed using pushBlock which calls
      // accept (so it's going the same way like a received/synced block)
      try {
        stores.beginTransaction();

        final TransactionDuplicatesCheckerImpl transactionDuplicatesChecker = new TransactionDuplicatesCheckerImpl();

        List<Transaction> unconfirmedTransactionsOrderedByFee = unconfirmedTransactionStore.getAll().stream().filter(
            transaction ->
              transaction.getVersion() == transactionProcessor.getTransactionVersion(previousBlock.getHeight())
                  && transaction.getExpiration() >= blockTimestamp
                  && transaction.getTimestamp()  <= blockTimestamp + MAX_TIMESTAMP_DIFFERENCE
                  && (
                      ! Burst.getFluxCapacitor().isActive(FeatureToggle.AUTOMATED_TRANSACTION_BLOCK)
                          || economicClustering.verifyFork(transaction)
                  )
        ).collect(Collectors.toList());
        unconfirmedTransactionsOrderedByFee.sort((o2, o1) -> ((Long) o1.getFeeNQT()).compareTo(o2.getFeeNQT()));

        COLLECT_TRANSACTIONS: for (Transaction transaction : unconfirmedTransactionsOrderedByFee) {
          boolean transactionHasBeenHandled = false;
          while ( ! transactionHasBeenHandled ) {
            if ( blockSize <= 0 || payloadSize <= 0 ) {
              break COLLECT_TRANSACTIONS;
            }
            else if ( transaction.getSize() > payloadSize ) {
              continue COLLECT_TRANSACTIONS;
            }

            Long slotFee = Burst.getFluxCapacitor().isActive(PRE_DYMAXION) ? (forgeFatBlocks ? 1 : blockSize) * FEE_QUANT : ONE_BURST;
            if (transaction.getFeeNQT() >= slotFee) {
              // transaction can only be handled if all referenced ones exist
              if (hasAllReferencedTransactions(transaction, transaction.getTimestamp(), 0)) {
                // handle non- duplicates and transactions which can be applied
                if (! transactionDuplicatesChecker.hasAnyDuplicate(transaction) && ! transactionDb.hasTransaction(transaction.getId()) && transactionService.applyUnconfirmed(transaction)) {
                  try {
                    transactionService.validate(transaction);
                    payloadSize -= transaction.getSize();
                    blockSize--;

                    totalAmountNQT += transaction.getAmountNQT();
                    totalFeeNQT += transaction.getFeeNQT();

                    orderedBlockTransactions.add(transaction);
                  } catch (BurstException.NotCurrentlyValidException e) {
                    transactionService.undoUnconfirmed(transaction);
                  } catch (BurstException.ValidationException e) {
                    unconfirmedTransactionStore.remove(transaction);
                    transactionService.undoUnconfirmed(transaction);
                  }
                }
                else {
                  // drop duplicates and those transactions which can not be applied
                  unconfirmedTransactionStore.remove(transaction);
                }
              }
              // handled by a real handling or by discarding the transaction
              transactionHasBeenHandled = true;
            }
            else {
              blockSize--;
            }
          }
        }

        if (subscriptionService.isEnabled()) {
          subscriptionService.clearRemovals();
          totalFeeNQT += subscriptionService.calculateFees(blockTimestamp);
        }
      }
      catch (Exception e) {
        stores.rollbackTransaction();
        throw e;
      }
      finally {
        stores.rollbackTransaction();
        stores.endTransaction();
      }

      // final byte[] publicKey = Crypto.getPublicKey(secretPhrase);

      // ATs for block
      AT.clearPendingFees();
      AT.clearPendingTransactions();
      AT_Block atBlock = AT_Controller.getCurrentBlockATs(payloadSize, previousBlock.getHeight() + 1);
      byte[] byteATs = atBlock.getBytesForBlock();

      // digesting AT Bytes
      if (byteATs != null) {
        payloadSize    -= byteATs.length;
        totalFeeNQT    += atBlock.getTotalFees();
        totalAmountNQT += atBlock.getTotalAmount();
      }

      // ATs for block

      MessageDigest digest = Crypto.sha256();
      orderedBlockTransactions.forEach(transaction -> digest.update(transaction.getBytes()));
      byte[] payloadHash = digest.digest();
      byte[] generationSignature = generator.calculateGenerationSignature(
          previousBlock.getGenerationSignature(), previousBlock.getGeneratorId());
      Block block;
      byte[] previousBlockHash = Crypto.sha256().digest(previousBlock.getBytes());
      try {
        block = new Block(getBlockVersion(), blockTimestamp,
            previousBlock.getId(), totalAmountNQT, totalFeeNQT, Burst.getFluxCapacitor().getInt(FluxInt.MAX_PAYLOAD_LENGTH) - payloadSize, payloadHash, publicKey,
            generationSignature, null, previousBlockHash, new ArrayList<>(orderedBlockTransactions), nonce,
            byteATs, previousBlock.getHeight());

      } catch (BurstException.ValidationException e) {
        // shouldn't happen because all transactions are already validated
        logger.info("Error generating block", e);
        return;
      }
      block.sign(secretPhrase);
      blockService.setPrevious(block, previousBlock);
      try {
        blockService.preVerify(block);
        pushBlock(block);
        blockListeners.notify(block, Event.BLOCK_GENERATED);
        logger.debug("Account " + Convert.toUnsignedLong(block.getGeneratorId()) + " generated block "
            + block.getStringId() + " at height " + block.getHeight());
        downloadCache.resetCache();
      } catch ( InterruptedException e ) {
        Thread.currentThread().interrupt();
      } catch (TransactionNotAcceptedException e) {
        logger.debug("Generate block failed: " + e.getMessage());
        Transaction transaction = e.getTransaction();
        logger.debug("Removing invalid transaction: " + transaction.getStringId());
        unconfirmedTransactionStore.remove(transaction);
        throw e;
      } catch (BlockNotAcceptedException e) {
        logger.debug("Generate block failed: " + e.getMessage());
        throw e;
      }
    } //end synchronized cache
  }

  private boolean hasAllReferencedTransactions(Transaction transaction, int timestamp, int count) {
    if (transaction.getReferencedTransactionFullHash() == null) {
      return timestamp - transaction.getTimestamp() < 60 * 1440 * 60 && count < 10;
    }
    transaction =
        transactionDb.findTransactionByFullHash(transaction.getReferencedTransactionFullHash());
    if (!subscriptionService.isEnabled()) {
      if (transaction != null && transaction.getSignature() == null) {
        transaction = null;
      }
    }
    return transaction != null && hasAllReferencedTransactions(transaction, timestamp, count + 1);
  }

  @Override
  public void scan(int height) {
    throw new UnsupportedOperationException(
        "scan is disabled for the moment - please use the pop off feature");
  }

}
