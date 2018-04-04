package brs.util;

import brs.Block;
import brs.Blockchain;
import brs.common.Props;
import brs.services.PropertyService;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import brs.Constants;

public final class DownloadCacheImpl {
  private final int blockCacheMB;

  protected final Map<Long, Block> blockCache = new LinkedHashMap<>();
  protected final List<Block> forkCache = new ArrayList<>();
  protected final Map<Long, Long> reverseCache = new LinkedHashMap<>();
  protected final List<Long> unverified = new LinkedList<>();

  private final Logger logger = LoggerFactory.getLogger(DownloadCacheImpl.class);

  private final Blockchain blockchain;

  private int blockCacheSize = 0;

  private Long lastBlockId = null;
  private int lastHeight = -1;
  private BigInteger highestCumulativeDifficulty = BigInteger.ZERO;

  private final StampedLock dcsl = new StampedLock();
  
  private boolean lockedCache = false;
  
  
  public DownloadCacheImpl(PropertyService propertyService, Blockchain blockchain) {
    this.blockCacheMB = propertyService.getInt(Props.BRS_BLOCK_CACHE_MB, 40);
    this.blockchain = blockchain;
  }

  public int getChainHeight() {
    long stamp = dcsl.tryOptimisticRead();
    int retVal = lastHeight;
    if (!dcsl.validate(stamp)) {
     
      stamp = dcsl.readLock();
      try {
        retVal = lastHeight;
      } finally {
         dcsl.unlockRead(stamp);
      }
   }
   if (retVal > -1) {
      return retVal;
    }
    return blockchain.getHeight();
  }
  public void lockCache() {
    long stamp = dcsl.writeLock();
	try {
	  lockedCache = true;
	} finally {
	  dcsl.unlockWrite(stamp);
	}
	setLastVars();
  }
  public void unlockCache() {
    long stamp = dcsl.tryOptimisticRead();
	boolean retVal = lockedCache;
	if (!dcsl.validate(stamp)) {
	  stamp = dcsl.readLock();
	  try {
	    retVal = lockedCache;
	  } finally {
	    dcsl.unlockRead(stamp);
	  }
	}
	
	if(retVal == true) {
	  stamp = dcsl.writeLock();
	  try {
	    lockedCache = false;
	  } finally {
		dcsl.unlockWrite(stamp);
      }	
	}
  }
  private boolean getLockState() {
    long stamp = dcsl.tryOptimisticRead();
	boolean retVal = lockedCache;
	if (!dcsl.validate(stamp)) {
	  stamp = dcsl.readLock();
	  try {
	    retVal = lockedCache;
      } finally {
		dcsl.unlockRead(stamp);
      }
    }
	return retVal;
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
 
  
  public boolean isFull() {
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
    return retVal > blockCacheMB * 1024 * 1024;
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
    Long lbID = lastBlockId;
    BigInteger retVal = highestCumulativeDifficulty;
    
   
    if (!dcsl.validate(stamp)) {
     
      stamp = dcsl.readLock();
      try {
        lbID = lastBlockId;
        retVal = highestCumulativeDifficulty;
      } finally {
         dcsl.unlockRead(stamp);
      }
    }
    if (lbID != null) {
      return retVal;
    }
    setLastVars();
    stamp = dcsl.tryOptimisticRead();
    retVal = highestCumulativeDifficulty;
    if (!dcsl.validate(stamp)) {
      stamp = dcsl.readLock();
      try {
        retVal = highestCumulativeDifficulty;
      } finally {
         dcsl.unlockRead(stamp);
      }
    }
    return retVal;
  }

  public long getUnverifiedBlockIdFromPos(int pos) {
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
  public Block getFirstUnverifiedBlock() {
	 long stamp = dcsl.writeLock();
	 try {
		 long blockId = unverified.get(0);
		 Block block = blockCache.get(blockId);
		 unverified.remove(blockId);
		 return block;
	 } finally {
      dcsl.unlockWrite(stamp);
    }
  }

  public void removeUnverified(long blockId) {
    long stamp = dcsl.writeLock();
    try {
      unverified.remove(blockId);
    } finally {
      dcsl.unlockWrite(stamp);
    }
  }
  
  public void removeUnverifiedBatch(Collection<Block> blocks) {
    long stamp = dcsl.writeLock();
    try {
      for (Block block : blocks) {
        unverified.remove(block.getId());
      }
    } finally {
      dcsl.unlockWrite(stamp);
    }
  }

  public void resetCache() {
    long stamp = dcsl.writeLock();
    try {
      blockCache.clear();
      reverseCache.clear();
      unverified.clear();
      blockCacheSize = 0;
      lockedCache = true;
    } finally {
     dcsl.unlockWrite(stamp);
    }
    setLastVars();
  }

  public Block getBlock(long BlockId) {
	//search the forkCache if we have a forkList
    if(!forkCache.isEmpty()) {
      for (Block block : forkCache) {
        if(block.getId() == BlockId) {
        	return block;
        }
      }
    }
    long stamp = dcsl.tryOptimisticRead();
    Block retVal = getBlockInt(BlockId);
    if (!dcsl.validate(stamp)) {
      stamp = dcsl.readLock();
      try {
        retVal = getBlockInt(BlockId);
      } finally {
         dcsl.unlockRead(stamp);
      }
    }
    if(retVal != null) {
      return retVal;
    }
    if (blockchain.hasBlock(BlockId)) {
      return blockchain.getBlock(BlockId);
    }
    return null;
  }
  private Block getBlockInt(long BlockId) {
    if (blockCache.containsKey(BlockId)) {
      return blockCache.get(BlockId);
    }
    return null;
  }

  public Block getNextBlock(long prevBlockId) {
    long stamp = dcsl.tryOptimisticRead();
    Block retVal = getNextBlockInt(prevBlockId);
    if (!dcsl.validate(stamp)) {
     
      stamp = dcsl.readLock();
      try {
        retVal = getNextBlockInt(prevBlockId);
      } finally {
        dcsl.unlockRead(stamp);
      }
    }
    return retVal;
  }

  private Block getNextBlockInt(long prevBlockId) {
    if (reverseCache.containsKey(prevBlockId)) {
      return blockCache.get(reverseCache.get(prevBlockId));
    }
    return null;
  }

  public boolean hasBlock(long BlockId) {
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
    return blockchain.hasBlock(BlockId);
  }

  public boolean canBeFork(long oldBlockId) {
    int curHeight = getChainHeight();
    long stamp = dcsl.tryOptimisticRead();
    Block block = getBlockInt(oldBlockId);
    if (!dcsl.validate(stamp)) {
     
      stamp = dcsl.readLock();
      try {
        block = getBlockInt(oldBlockId);
      } finally {
         dcsl.unlockRead(stamp);
      }
    }
    if (block == null && blockchain.hasBlock(oldBlockId)) {
      block = blockchain.getBlock(oldBlockId);
    }
    if (block == null) {
      return false;
    }
    return (curHeight - block.getHeight()) <= Constants.MAX_ROLLBACK;
  }

  public boolean addBlock(Block block) {
    if(!getLockState()) {
	  long stamp = dcsl.writeLock();
      try {
        blockCache.put(block.getId(), block);
        reverseCache.put(block.getPreviousBlockId(), block.getId());
        unverified.add(block.getId());
        blockCacheSize += block.getByteLength();
        lastBlockId = block.getId();
        lastHeight = block.getHeight();
        highestCumulativeDifficulty = block.getCumulativeDifficulty();
        return true;
      } finally {
        dcsl.unlockWrite(stamp);
      }
    }
    return false;
  }
  public void addForkBlock(Block block) {
    forkCache.add(block);
  }
  public void resetForkBlocks() {
    forkCache.clear();  
  }
  public List<Block> getForkList(){
    return forkCache;
  }

  public boolean removeBlock(Block block) {
    long stamp = dcsl.tryOptimisticRead();
    boolean chkVal = blockCache.containsKey(block.getId());
    long lastId = lastBlockId;
    
    if (!dcsl.validate(stamp)) {
      stamp = dcsl.readLock();
      try {
        chkVal = blockCache.containsKey(block.getId());
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
    Block blockImpl = getBlock(blockId);
    return (blockImpl == null || blockImpl.getHeight() < Constants.POC2_START_BLOCK) ? 1 : 2;
  }
  
  public long getLastBlockId() {
    Long lId = getLastCacheId();
    if (lId != null) {
      return lId;
    }
    return blockchain.getLastBlock().getId();
  }
  private Long getLastCacheId() {
    long stamp = dcsl.tryOptimisticRead();
    Long lId = lastBlockId;
    if (!dcsl.validate(stamp)) {
     
      stamp = dcsl.readLock();
      try {
        lId = lastBlockId;
      } finally {
        dcsl.unlockRead(stamp);
      }
    }
    return lId;
  }
    
  public Block getLastBlock() {
    Long iLd = getLastCacheId();
    if (iLd != null) {
      long stamp = dcsl.tryOptimisticRead();
      Block retBlock = blockCache.get(iLd);
      if (!dcsl.validate(stamp)) {
        stamp = dcsl.readLock();
        try {
          retBlock = blockCache.get(iLd);
        } finally {
          dcsl.unlockRead(stamp);
        }
      }
      return retBlock;
    }
    return blockchain.getLastBlock();
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
      if (! blockCache.isEmpty()) {
        lastBlockId = blockCache.get(blockCache.keySet().toArray()[blockCache.keySet().size() - 1]).getId();
        lastHeight = blockCache.get(lastBlockId).getHeight();
        highestCumulativeDifficulty = blockCache.get(lastBlockId).getCumulativeDifficulty();
      } else {
        lastBlockId = blockchain.getLastBlock().getId();
        lastHeight = blockchain.getHeight();
        highestCumulativeDifficulty = blockchain.getLastBlock().getCumulativeDifficulty();
      }
    } finally {
      dcsl.unlockWrite(stamp);
    }
  }
}
