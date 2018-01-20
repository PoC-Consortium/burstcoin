package brs.util;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import brs.Block;
import brs.BlockImpl;
import brs.BlockchainImpl;
import brs.Burst;
import brs.Constants;

public final class DownloadCacheImpl {
  public final int BLOCKCACHEMB =
      Burst.getIntProperty("brs.blockCacheMB") == 0 ? 40 : Burst.getIntProperty("brs.blockCacheMB");

  protected static final Map<Long, Block> blockCache = new LinkedHashMap<>();
  protected static final Map<Long, Long> reverseCache = new LinkedHashMap<>();
  protected static final List<Long> unverified = new LinkedList<>();

  private final BlockchainImpl blockchain = BlockchainImpl.getInstance();
  private static final Logger logger = LoggerFactory.getLogger(DownloadCacheImpl.class);
  private static int blockCacheSize = 0;

  private Long LastBlockId = null;
  private int LastHeight = -1;
  BigInteger HigestCumulativeDifficulty = BigInteger.ZERO;

  public int getChainHeight() {
    if (LastHeight > -1) {
      return LastHeight;
    }
    return blockchain.getHeight();
  }

  public int getBlockCacheSize() {
    return blockCacheSize;
  }

  public boolean IsFull() {
    if (blockCacheSize > BLOCKCACHEMB * 1024 * 1024) {
      return true;
    }
    return false;
  }

  public int getUnverifiedSize() {
    return unverified.size();
  }

  public BigInteger getCumulativeDifficulty() {
    if (LastBlockId == null) {
      setLastVars();
    }
    return HigestCumulativeDifficulty;
  }

  public long GetUnverifiedBlockId(int BlockId) {
    return unverified.get(0);
  }

  public void removeUnverified(long BlockId) {
    unverified.remove(BlockId);
  }

  public void VerifyCacheIntegrity() {
    if (blockCache.size() > 0) {
      long checkBlockId = getLastBlockId();
      while (checkBlockId != blockchain.getLastBlock().getId()) {
        if (blockCache.get(checkBlockId) == null) {
          ResetCache();
          break;
        }
        checkBlockId = blockCache.get(checkBlockId).getPreviousBlockId();
      }
    }
  }

  public void ResetCache() {
    blockCache.clear();
    reverseCache.clear();
    unverified.clear();
    blockCacheSize = 0;
    setLastVars();
  }

  public int getBlockHeight(long BlockId) {
    if (blockCache.containsKey(BlockId)) {
      return blockCache.get(BlockId).getHeight();
    }
    if (blockchain.hasBlock(BlockId)) {
      return blockchain.getBlock(BlockId).getHeight();
    }

    // this should not be needed will remove later when all checks out.
    logger.warn("Cannot get blockheight. blockID: " + BlockId);
    return 0;
  }

  public BlockImpl GetBlock(long BlockId) {
    if (blockCache.containsKey(BlockId)) {
      return (BlockImpl) blockCache.get(BlockId);
    }
    if (blockchain.hasBlock(BlockId)) {
      return blockchain.getBlock(BlockId);
    }

    return null;
  }

  public BlockImpl GetBlock(Long BlockId) {
    if (blockCache.containsKey(BlockId)) {
      return (BlockImpl) blockCache.get(BlockId);
    }
    if (blockchain.hasBlock(BlockId)) {
      return blockchain.getBlock(BlockId);
    }

    return null;
  }

  public BlockImpl GetNextBlock(long prevBlockId) {
    if (!reverseCache.containsKey(prevBlockId)) {
      return null;
    }
    try {
      return (BlockImpl) blockCache.get(reverseCache.get(prevBlockId));
    } finally {
    }
  }

  public void WaitForMapToBlockChain() {
    synchronized (this) {
      while (!reverseCache.containsKey(blockchain.getLastBlock().getId())) {
        try {
          printDebug();
          this.wait(2000);
        } catch (InterruptedException ignore) {
          logger.trace("Interrupted: ", ignore);
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  public boolean HasBlock(long BlockId) {
    if (blockCache.containsKey(BlockId)) {
      return true;
    }
    if (blockchain.hasBlock(BlockId)) {
      return true;
    }

    return false;
  }

  public boolean CanBeFork(long oldBlockId) {
    int curHeight = getChainHeight();
    BlockImpl block = null;
    if (blockCache.containsKey(oldBlockId)) {
      block = (BlockImpl) blockCache.get(oldBlockId);
    } else if (blockchain.hasBlock(oldBlockId)) {
      block = blockchain.getBlock(oldBlockId);
    }
    if (block == null) {
      return false;
    }
    if ((curHeight - block.getHeight()) > Constants.MAX_ROLLBACK) {
      return false;
    }

    return true;
  }

  public void AddBlock(BlockImpl block) {
    blockCache.put(block.getId(), block);
    reverseCache.put(block.getPreviousBlockId(), block.getId());
    unverified.add(block.getId());
    blockCacheSize += block.getByteLength();
    LastBlockId = block.getId();
    LastHeight = block.getHeight();
    HigestCumulativeDifficulty = block.getCumulativeDifficulty();
  }

  public void SetCacheBackTo(long BadBlockId) {
    // Starting from lowest point and erase all up to last block.
    if (blockCache.containsKey(BadBlockId)) {
      BlockImpl badBlock;
      long id;
      badBlock = (BlockImpl) blockCache.get(BadBlockId);
      reverseCache.remove(badBlock.getPreviousBlockId());
      blockCacheSize -= badBlock.getByteLength();
      blockCache.remove(BadBlockId);
      while (reverseCache.containsKey(BadBlockId)) {
        id = reverseCache.get(BadBlockId);
        reverseCache.remove(BadBlockId);
        blockCacheSize -= ((BlockImpl) blockCache.get(id)).getByteLength();
        blockCache.remove(id);
        BadBlockId = id;
      }
      setLastVars();
    }
  }

  public boolean RemoveBlock(BlockImpl block) {
    if (blockCache.containsKey(block.getId())) { // make sure there is something to remove
      reverseCache.remove(block.getPreviousBlockId());
      blockCache.remove(block.getId());
      blockCacheSize -= block.getByteLength();
      if (block.getId() == LastBlockId) {
        setLastVars();
      }
      return true;
    }

    return false;
  }

  public int getPoCVersion(long blockId) {
    return (GetBlock(blockId).getHeight() >= Constants.POC2_START_BLOCK) ? 2 : 1;
  }

  public BlockImpl getLastBlock() {
    if (LastBlockId != null) {
      return (BlockImpl) blockCache.get(LastBlockId);
    }
    return blockchain.getLastBlock();
  }

  public long getLastBlockId() {
    if (LastBlockId != null) {
      return LastBlockId;
    }
    return blockchain.getLastBlock().getId();
  }

  public int size() {
    return blockCache.size();
  }

  public void printDebug() {
    if (reverseCache.size() > 0) {
      logger.debug("BlockCache First block key:" + blockCache.keySet().toArray()[0]);
      logger.debug("revCache First block key:" + reverseCache.keySet().toArray()[0]);
      logger.debug(
          "revCache First block Val:" + reverseCache.get(reverseCache.keySet().toArray()[0]));
      logger.debug("BlockCache size:" + blockCache.size());
      logger.debug("revCache size:" + reverseCache.size());
    } else {
      logger.debug("BlockCache size:" + blockCache.size());
      logger.debug("revCache size:" + reverseCache.size());
    }
  }

  private void setLastVars() {
    if (blockCache.size() > 0) {
      LastBlockId =
          blockCache.get(blockCache.keySet().toArray()[blockCache.keySet().size() - 1]).getId();
      LastHeight = blockCache.get(LastBlockId).getHeight();
      HigestCumulativeDifficulty = blockCache.get(LastBlockId).getCumulativeDifficulty();
    } else {
      LastBlockId = blockchain.getLastBlock().getId();
      LastHeight = blockchain.getHeight();
      HigestCumulativeDifficulty = blockchain.getLastBlock().getCumulativeDifficulty();
    }
  }
}
