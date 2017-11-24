package brs.util;

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


public final class DownloadCacheImpl {
  public final int BLOCKCACHEMB = Burst.getIntProperty("brs.blockCacheMB") == 0 ? 40 : Burst.getIntProperty("brs.blockCacheMB");
	
  public static final Map<Long, Block> blockCache = new LinkedHashMap<Long, Block>();
  public static final Map<Long, Long> reverseCache = new LinkedHashMap<Long, Long>();
  public static final List<Long> unverified = new LinkedList<Long>();
  
  private final BlockchainImpl blockchain = BlockchainImpl.getInstance();
  private static final Logger logger = LoggerFactory.getLogger(DownloadCacheImpl.class);
  public static int blockCacheSize = 0;
  
 
  
  public int getChainHeight() {
    synchronized (this){
	  if(blockCache.size() > 0){ //we have a downloaded cache
		return  blockCache.get(blockCache.keySet().toArray()[blockCache.keySet().size()-1]).getHeight(); 
	  }else{ //we have no cache so fetch from chain
		return blockchain.getHeight();
	  }
	}
  }
  public boolean IsFull() {
    if(  blockCacheSize > BLOCKCACHEMB * 1024 * 1024) {
      return true;
    }
	return false;
  }
  public int getUnverifiedSize() {
    return unverified.size();
  }
  public long GetUnverifiedBlockId(int BlockId) {
    return unverified.get(0);
  }
  public void removeUnverified(long BlockId) {
    unverified.remove(BlockId);
  }
  public void VerifyCacheIntegrity() {
    synchronized (this){
      if (blockCache.size() >0) {
        long checkBlockId = getLastBlockId();
        while (checkBlockId != blockchain.getLastBlock().getId()) {
          if (blockCache.get(checkBlockId) == null) {
            clearblockCache();
            break;
          }
          checkBlockId = blockCache.get(checkBlockId).getPreviousBlockId();
        }
      }
    }
  }
  
  public void clearblockCache(){
      synchronized (this) { // cache may no longer correspond with current chain, so dump it
        blockCache.clear();
        reverseCache.clear();
        unverified.clear();
        blockCacheSize = 0;
     //   this.notify();
      }
    }
  
  
  public int getBlockHeight(long BlockId) {
	synchronized (this){
	  if (blockCache.containsKey(BlockId)) {
		return blockCache.get(BlockId).getHeight();
      }else if(blockchain.hasBlock(BlockId)) {
	    return blockchain.getBlock(BlockId).getHeight();
      }else {//this should not be needed will remove later when all checks out.
	   logger.warn("Cannot get blockheight. blockID: "+BlockId);
	   return 0;
      }
    }
  }
  public BlockImpl GetBlock(long BlockId) {
    if(blockCache.containsKey(BlockId)){
	  return (BlockImpl) blockCache.get(BlockId);
    }else if(blockchain.hasBlock(BlockId)){
      return blockchain.getBlock(BlockId);
    }else{
      return null;
    }
  }
  public BlockImpl GetBlock(Long BlockId) {
    if(blockCache.containsKey(BlockId)){
      return (BlockImpl) blockCache.get(BlockId);
    }else if(blockchain.hasBlock(BlockId)){
      return blockchain.getBlock(BlockId);
    }else{
      return null;
    }
  }
  public BlockImpl GetNextBlock(long prevBlockId) {
    if (!reverseCache.containsKey(prevBlockId)) {
      return null;
	}
    try {
      return (BlockImpl) blockCache.get(reverseCache.get(prevBlockId)); 
    } finally {}
  }

  public void WaitForMapToBlockChain() {
	while (!reverseCache.containsKey(blockchain.getLastBlock().getId())) {
      try {
     //   logger.debug("PoC Verification waiting for. "+blockchain.getLastBlock().getId());
      //  printDebug();
	    this.wait(2000);
	  } catch (InterruptedException ignore) {}
    }
  }
  public boolean HasBlock(long BlockId) {
    if(blockCache.containsKey(BlockId)) {
	  return true;
    }else if(blockchain.hasBlock(BlockId)) {
      return true;
	}else {
      return false;  
    }
  }
  public boolean IsConflictingBlock(BlockImpl block) {
	
	/*
	 * We want to make sure that there is no block at the position we want to place input block.
	 * We should not call this function without having checked that block is not in chain.
     *
	 */
	
		Long ChainBlockId = null;
		//check reverse cache and get input block id from it
        ChainBlockId = reverseCache.get(block.getPreviousBlockId());
		
	    if(ChainBlockId == null) { 
	    	int Height;
	    	Height =  blockchain.getBlock(block.getPreviousBlockId()).getHeight();
	    	ChainBlockId = blockchain.getBlockIdAtHeight(Height +1);	  
	    }
	    if(ChainBlockId != null) {
			if(ChainBlockId == block.getId()) {
				return false;
			}
		}
    
	return true;
  }
  public boolean CanBeFork(long oldBlockId){
	  int curHeight = getChainHeight();
	  BlockImpl block = null;
	  if(blockCache.containsKey(oldBlockId)){
		  block = (BlockImpl) blockCache.get(oldBlockId);
	  }else if(blockchain.hasBlock(oldBlockId)){
		  block = blockchain.getBlock(oldBlockId);
	  }
	  if(block == null){
		  return false;
	  }
	  if((curHeight - block.getHeight()) >720){
		  return false;
	  }
	  return true;
  }
  
  
  public void AddBlock(BlockImpl block) {
	synchronized (this){
	  blockCache.put(block.getId(), block);
      reverseCache.put(block.getPreviousBlockId(), block.getId());
      unverified.add(block.getId());
      blockCacheSize += block.getByteLength();
	}
  }
  
  public void SetCacheBackTo(long BadBlockId) {
	  /* Starting from lowest poing and erase all up to lastblock */
	  if(blockCache.containsKey(BadBlockId)) { //we have something to remove
        BlockImpl badBlock;
        long id;
        badBlock = (BlockImpl)blockCache.get(BadBlockId);
        reverseCache.remove(badBlock.getPreviousBlockId());
        blockCacheSize -= badBlock.getByteLength();
        blockCache.remove(BadBlockId);
        while(reverseCache.containsKey(BadBlockId)) {
          id = reverseCache.get(BadBlockId);
          reverseCache.remove(BadBlockId);
          blockCacheSize -= ((BlockImpl)blockCache.get(id)).getByteLength();
          blockCache.remove(id);
          BadBlockId = id;
        }
	  }

  }
  public boolean RemoveBlock(BlockImpl block) {
   synchronized (this){
	if (blockCache.containsKey(block.getId())) { // make sure it wasn't already removed(ex failed preValidate) to avoid double subtracting from blockCacheSize
      reverseCache.remove(block.getPreviousBlockId());
      blockCache.remove(block.getId());
      blockCacheSize -= block.getByteLength();
      return true;
    }else {
      return false;
    }
  }
  }
  public BlockImpl getLastBlock() {
    synchronized (this){
	  if (blockCache.size() >0){
		return  GetBlock(blockCache.get(blockCache.keySet().toArray()[blockCache.keySet().size()-1]).getId()); 
      }else{
		return blockchain.getLastBlock();
	  }
	}	    
  }
  public long getLastBlockId() {
    synchronized (this){
	  if (blockCache.size() >0){
	    return  blockCache.get(blockCache.keySet().toArray()[blockCache.keySet().size()-1]).getId(); 
	  }else{
	    return blockchain.getLastBlock().getId();
	  }
    }	  
  }
  public int size() {
	  return blockCache.size();
  }

  public void printDebug(){
	 if(reverseCache.size()>0){ 
	  logger.debug("BlockCache First block key:"+blockCache.keySet().toArray()[0]);
	  logger.debug("revCache First block key:"+reverseCache.keySet().toArray()[0]);
	  logger.debug("revCache First block Val:"+reverseCache.get(reverseCache.keySet().toArray()[0]));
	  logger.debug("BlockCache size:"+blockCache.size());
	  logger.debug("revCache size:"+reverseCache.size());
	 }else{
	  logger.debug("BlockCache size:"+blockCache.size());
  	  logger.debug("revCache size:"+reverseCache.size());
	 }
	  
  }
  
}
