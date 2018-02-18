package brs.util;

import brs.Burst;
import brs.services.PropertyService;
import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import brs.Block;
import brs.BlockImpl;
import brs.Constants;

public final class DownloadCacheImpl {
  public final int BLOCKCACHEMB;

  protected static final Map<Long, Block> blockCache = new LinkedHashMap<>();
  protected static final Map<Long, Long> reverseCache = new LinkedHashMap<>();
  protected static final List<Long> unverified = new LinkedList<>();

  private static final Logger logger = LoggerFactory.getLogger(DownloadCacheImpl.class);
  private static int blockCacheSize = 0;

  private Long LastBlockId = null;
  private int LastHeight = -1;
  BigInteger HigestCumulativeDifficulty = BigInteger.ZERO;

  private final StampedLock dcsl = new StampedLock();
  
  
  public DownloadCacheImpl() {
    PropertyService propertyService = Burst.getPropertyService();
    this.BLOCKCACHEMB = propertyService.getIntProperty("brs.blockCacheMB") == 0 ? 40 : propertyService.getIntProperty("brs.blockCacheMB");
  }

  public int getChainHeight() {
    long stamp = dcsl.tryOptimisticRead();
    int retVal = LastHeight;
    if (!dcsl.validate(stamp)) {
     
      stamp = dcsl.readLock();
      try {
        retVal = LastHeight;
      } finally {
         dcsl.unlockRead(stamp);
      }
   }
   if (retVal > -1) {
      return retVal;
    }
    return Burst.getBlockchain().getHeight();
  }
  
  public int getBlockCacheSize() {
    long stamp = dcsl.tryOptimisticRead();
    int retVal = blockCacheSize;
    if (!dcsl.validate(stamp)) {
     
      stamp = dcsl.readLock();
      try {
        retVal = blockCacheSize;
      } finally {
         dcsl.unlockRead(stamp);
      }
   }
    return retVal;
  }
 
  
  public boolean IsFull() {
    long stamp = dcsl.tryOptimisticRead();
    int retVal = blockCacheSize;
    if (!dcsl.validate(stamp)) {
     
      stamp = dcsl.readLock();
      try {
        retVal = blockCacheSize;
      } finally {
         dcsl.unlockRead(stamp);
      }
    }
    return retVal > BLOCKCACHEMB * 1024 * 1024;
  }

  public int getUnverifiedSize() {
    long stamp = dcsl.tryOptimisticRead();
    int retVal = unverified.size();
    if (!dcsl.validate(stamp)) {
     
      stamp = dcsl.readLock();
      try {
        retVal = unverified.size();
      } finally {
         dcsl.unlockRead(stamp);
      }
    }
    return retVal;
  }

  public BigInteger getCumulativeDifficulty() {
    long stamp = dcsl.tryOptimisticRead();
    Long lbID = LastBlockId;
    BigInteger retVal = HigestCumulativeDifficulty;
    
   
    if (!dcsl.validate(stamp)) {
     
      stamp = dcsl.readLock();
      try {
        lbID = LastBlockId;
        retVal = HigestCumulativeDifficulty;
      } finally {
         dcsl.unlockRead(stamp);
      }
    }
    if (lbID != null) {
      return retVal;
    }
    setLastVars();
    stamp = dcsl.tryOptimisticRead();
    retVal = HigestCumulativeDifficulty;
    if (!dcsl.validate(stamp)) {
      stamp = dcsl.readLock();
      try {
        retVal = HigestCumulativeDifficulty;
      } finally {
         dcsl.unlockRead(stamp);
      }
    }
    return retVal;
  }

  public long GetUnverifiedBlockIdFromPos(int pos) {
    long stamp = dcsl.tryOptimisticRead();
    long reVal = unverified.get(pos);
    
    if (!dcsl.validate(stamp)) {
     
      stamp = dcsl.readLock();
      try {
        reVal = unverified.get(pos);
      } finally {
         dcsl.unlockRead(stamp);
      }
    }
    return reVal;
  }
  public BlockImpl getFirstUnverifiedBlock() {
	 long stamp = dcsl.writeLock();
	 try {
		 long blockId = unverified.get(0);
		 BlockImpl block = (BlockImpl) blockCache.get(blockId);
		 unverified.remove(blockId);
		 return block;
	 } finally {
      dcsl.unlockWrite(stamp);
    }
  }

  public void removeUnverified(long BlockId) {
    long stamp = dcsl.writeLock();
    try {
      unverified.remove(BlockId);
    } finally {
      dcsl.unlockWrite(stamp);
    }
  }
  
  public void removeUnverifiedBatch(Collection<BlockImpl> blocks) {
    long stamp = dcsl.writeLock();
    try {
      for (BlockImpl block : blocks) {
        unverified.remove(block.getId());
      }
    } finally {
      dcsl.unlockWrite(stamp);
    }
  }

/*  public void VerifyCacheIntegrity() {
    if (blockCache.size() > 0) {
      long checkBlockId = getLastBlockId();
      while (checkBlockId != Burst.getBlockchain().getLastBlock().getId()) {
        if (blockCache.get(checkBlockId) == null) {
          ResetCache();
          break;
        }
        checkBlockId = blockCache.get(checkBlockId).getPreviousBlockId();
      }
    }
  }*/

  public void ResetCache() {
    long stamp = dcsl.writeLock();
    try {
      blockCache.clear();
      reverseCache.clear();
      unverified.clear();
      blockCacheSize = 0;
    } finally {
     dcsl.unlockWrite(stamp);
    }
    setLastVars();
  }
  public int getBlockHeight(long BlockId) {
    long stamp = dcsl.tryOptimisticRead();
    int retVal;
    
    if (blockCache.containsKey(BlockId)) {
      retVal = blockCache.get(BlockId).getHeight();
      if (dcsl.validate(stamp)) {
        return retVal;
      }
    }
    if (!dcsl.validate(stamp)) {
     
      stamp = dcsl.readLock();
      try {
        if (blockCache.containsKey(BlockId)) {
          return blockCache.get(BlockId).getHeight();
        }
      } finally {
         dcsl.unlockRead(stamp);
      }
    }
    return Burst.getBlockchain().getBlock(BlockId).getHeight();
  }

  public BlockImpl GetBlock(long BlockId) {
    long stamp = dcsl.tryOptimisticRead();
    BlockImpl retVal = GetBlockInt(BlockId);
    if (!dcsl.validate(stamp)) {
      stamp = dcsl.readLock();
      try {
        retVal = GetBlockInt(BlockId);
      } finally {
         dcsl.unlockRead(stamp);
      }
    }
    if(retVal != null) {
      return retVal;
    }
    if (Burst.getBlockchain().hasBlock(BlockId)) {
      return Burst.getBlockchain().getBlock(BlockId);
    }
    return null;
  }
  private BlockImpl GetBlockInt(long BlockId) {
    if (blockCache.containsKey(BlockId)) {
      return (BlockImpl) blockCache.get(BlockId);
    }
    return null;
  }
    
/*  public BlockImpl GetBlock(Long BlockId) {
    if (blockCache.containsKey(BlockId)) {
      return (BlockImpl) blockCache.get(BlockId);
    }
    if (Burst.getBlockchain().hasBlock(BlockId)) {
      return Burst.getBlockchain().getBlock(BlockId);
    }

    return null;
  }
*/
  
  public BlockImpl GetNextBlock(long prevBlockId) {
    long stamp = dcsl.tryOptimisticRead();
    BlockImpl retVal = GetNextBlockInt(prevBlockId);
    if (!dcsl.validate(stamp)) {
     
      stamp = dcsl.readLock();
      try {
        retVal = GetNextBlockInt(prevBlockId);
      } finally {
        dcsl.unlockRead(stamp);
      }
    }
    return retVal;
  }
  private BlockImpl GetNextBlockInt(long prevBlockId) {
    if (reverseCache.containsKey(prevBlockId)) {
      return (BlockImpl) blockCache.get(reverseCache.get(prevBlockId));
    }
    return null;
  }

 /* public void WaitForMapToBlockChain() {
    synchronized (this) {
      while (!reverseCache.containsKey(Burst.getBlockchain().getLastBlock().getId())) {
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
*/
  
  public boolean HasBlock(long BlockId) {
    long stamp = dcsl.tryOptimisticRead();
    boolean retVal =  blockCache.containsKey(BlockId);
    if (!dcsl.validate(stamp)) {
     
      stamp = dcsl.readLock();
      try {
        retVal =  blockCache.containsKey(BlockId);
      } finally {
        dcsl.unlockRead(stamp);
      }
    }
    if (retVal) {
      return true;
    }
    return Burst.getBlockchain().hasBlock(BlockId);
  }

  public boolean CanBeFork(long oldBlockId) {
    int curHeight = getChainHeight();
    BlockImpl block = null;
    long stamp = dcsl.tryOptimisticRead();
    block = GetBlockInt(oldBlockId);
    if (!dcsl.validate(stamp)) {
     
      stamp = dcsl.readLock();
      try {
        block = GetBlockInt(oldBlockId);
      } finally {
         dcsl.unlockRead(stamp);
      }
    }
    if (block == null && Burst.getBlockchain().hasBlock(oldBlockId)) {
      block = Burst.getBlockchain().getBlock(oldBlockId);
    }
    if (block == null) {
      return false;
    }
    return (curHeight - block.getHeight()) <= Constants.MAX_ROLLBACK;
  }

  public void AddBlock(BlockImpl block) {
    long stamp = dcsl.writeLock();
    try {
      blockCache.put(block.getId(), block);
      reverseCache.put(block.getPreviousBlockId(), block.getId());
      unverified.add(block.getId());
      blockCacheSize += block.getByteLength();
      LastBlockId = block.getId();
      LastHeight = block.getHeight();
      HigestCumulativeDifficulty = block.getCumulativeDifficulty();
    } finally {
      dcsl.unlockWrite(stamp);
    }
  }
  public void AddBlockBatch(List<BlockImpl> blocks) {
    long stamp = dcsl.writeLock();
    try {
      while(blocks.size() > 0) {
        BlockImpl block = blocks.get(0);
        blockCache.put(block.getId(), block);
        reverseCache.put(block.getPreviousBlockId(), block.getId());
        unverified.add(block.getId());
        blockCacheSize += block.getByteLength();
        LastBlockId = block.getId();
        LastHeight = block.getHeight();
        HigestCumulativeDifficulty = block.getCumulativeDifficulty();
        blocks.remove(0);
      }
    } finally {
      dcsl.unlockWrite(stamp);
    }
  }

 /* public void SetCacheBackTo(long BadBlockId) {
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
  }*/

  public boolean RemoveBlock(BlockImpl block) {
    long stamp = dcsl.tryOptimisticRead();
    boolean chkVal = blockCache.containsKey(block.getId());
    long lastId = LastBlockId;
    
    if (!dcsl.validate(stamp)) {
      stamp = dcsl.readLock();
      try {
        chkVal = blockCache.containsKey(block.getId());
        lastId = LastBlockId;
      } finally {
         dcsl.unlockRead(stamp);
      }
    }
    
    if (chkVal) { // make sure there is something to remove
      stamp = dcsl.writeLock();
      try {
    	unverified.remove(block.getId());
        reverseCache.remove(block.getPreviousBlockId());
        blockCache.remove(block.getId());
        blockCacheSize -= block.getByteLength();
      } finally {
        dcsl.unlockWrite(stamp);
      }
      if (block.getId() == lastId) {
        setLastVars();
      }
      return true;
    }
    return false;
  }

  public int getPoCVersion(long blockId) {
    BlockImpl blockImpl = GetBlock(blockId);
    return (blockImpl == null || blockImpl.getHeight() < Constants.POC2_START_BLOCK) ? 1 : 2;
  }
  
  public long getLastBlockId() {
    Long lId = getLastCacheId();
    if (lId != null) {
      return lId;
    }
    return Burst.getBlockchain().getLastBlock().getId();
  }
  private Long getLastCacheId() {
    long stamp = dcsl.tryOptimisticRead();
    Long lId = LastBlockId;
    if (!dcsl.validate(stamp)) {
     
      stamp = dcsl.readLock();
      try {
        lId = LastBlockId;
      } finally {
        dcsl.unlockRead(stamp);
      }
    }
    return lId;
  }
    
  public BlockImpl getLastBlock() {
    Long iLd = getLastCacheId();
    if (iLd != null) {
      long stamp = dcsl.tryOptimisticRead();
      BlockImpl retBlock = (BlockImpl) blockCache.get(iLd);
      if (!dcsl.validate(stamp)) {
        stamp = dcsl.readLock();
        try {
          retBlock = (BlockImpl) blockCache.get(iLd);
        } finally {
          dcsl.unlockRead(stamp);
        }
      }
      return retBlock;
    }
    return Burst.getBlockchain().getLastBlock();
  }

  public int size() {
    long stamp = dcsl.tryOptimisticRead();
    int size = blockCache.size();
    if (!dcsl.validate(stamp)) {
     
      stamp = dcsl.readLock();
      try {
        size = blockCache.size();
      } finally {
        dcsl.unlockRead(stamp);
      }
    }
    return size;
  }

  public void printDebug() {
/*    if (reverseCache.size() > 0) {
      logger.debug("BlockCache First block key:" + blockCache.keySet().toArray()[0]);
      logger.debug("revCache First block key:" + reverseCache.keySet().toArray()[0]);
      logger.debug("revCache First block Val:" + reverseCache.get(reverseCache.keySet().toArray()[0]));
      logger.debug("BlockCache size:" + blockCache.size());
      logger.debug("revCache size:" + reverseCache.size());
    } else {
      logger.debug("BlockCache size:" + blockCache.size());
      logger.debug("Unverified size:" + unverified.size());
      logger.debug("Verified size:" + (blockCache.size() - unverified.size()));
    }*/
    logger.info("BlockCache size:" + blockCache.size());
    logger.info("Unverified size:" + unverified.size());
    logger.info("Verified size:" + (blockCache.size() - unverified.size()));
    
  }

  private void setLastVars() {
    long stamp = dcsl.writeLock();
    try {
      if (blockCache.size() > 0) {
        LastBlockId =
            blockCache.get(blockCache.keySet().toArray()[blockCache.keySet().size() - 1]).getId();
        LastHeight = blockCache.get(LastBlockId).getHeight();
        HigestCumulativeDifficulty = blockCache.get(LastBlockId).getCumulativeDifficulty();
      } else {
        LastBlockId = Burst.getBlockchain().getLastBlock().getId();
        LastHeight = Burst.getBlockchain().getHeight();
        HigestCumulativeDifficulty = Burst.getBlockchain().getLastBlock().getCumulativeDifficulty();
      }
    } finally {
      dcsl.unlockWrite(stamp);
    }
  }
}
