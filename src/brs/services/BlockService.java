package brs.services;

import brs.Block;
import brs.BlockchainProcessor;
import brs.BlockchainProcessor.BlockNotAcceptedException;
import brs.BlockchainProcessor.BlockOutOfOrderException;

public interface BlockService {

  void preVerify(Block block) throws BlockchainProcessor.BlockNotAcceptedException;

  void preVerify(Block block, byte[] scoopData) throws BlockchainProcessor.BlockNotAcceptedException;

  long getBlockReward(Block block);

  void calculateBaseTarget(Block block, Block lastBlock);

  void setPrevious(Block block, Block previousBlock);

  boolean verifyGenerationSignature(Block block) throws BlockNotAcceptedException;

  boolean verifyBlockSignature(Block block) throws BlockOutOfOrderException;

  void apply(Block block);

  int getScoopNum(Block block);
}
