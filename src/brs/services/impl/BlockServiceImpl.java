package brs.services.impl;

import brs.Account;
import brs.Block;
import brs.Blockchain;
import brs.BlockchainProcessor;
import brs.Constants;
import brs.Generator;
import brs.Genesis;
import brs.Transaction;
import brs.crypto.Crypto;

import brs.services.AccountService;
import brs.services.BlockService;
import brs.services.TransactionService;
import brs.util.Convert;
import brs.util.DownloadCacheImpl;
import java.math.BigInteger;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockServiceImpl implements BlockService {

  private final AccountService accountService;
  private final TransactionService transactionService;
  private final Blockchain blockchain;
  private DownloadCacheImpl downloadCache;
  private Generator generator;

  private static final Logger logger = LoggerFactory.getLogger(BlockServiceImpl.class);

  public BlockServiceImpl(AccountService accountService, TransactionService transactionService, Blockchain blockchain, DownloadCacheImpl downloadCache, Generator generator) {
    this.accountService = accountService;
    this.transactionService = transactionService;
    this.blockchain = blockchain;
    this.downloadCache = downloadCache;
    this.generator = generator;
  }

  @Override
  public boolean verifyBlockSignature(Block block) throws BlockchainProcessor.BlockOutOfOrderException {
    try {
      Block previousBlock = blockchain.getBlock(block.getPreviousBlockId());
      if (previousBlock == null) {
        throw new BlockchainProcessor.BlockOutOfOrderException(
            "Can't verify signature because previous block is missing");
      }

      byte[] data = block.getBytes();
      byte[] data2 = new byte[data.length - 64];
      System.arraycopy(data, 0, data2, 0, data2.length);

      byte[] publicKey;
      Account genAccount = accountService.getAccount(block.getGeneratorPublicKey());
      Account.RewardRecipientAssignment rewardAssignment;
      rewardAssignment = genAccount == null ? null : accountService.getRewardRecipientAssignment(genAccount);
      if (genAccount == null || rewardAssignment == null || previousBlock.getHeight()
          + 1 < Constants.BURST_REWARD_RECIPIENT_ASSIGNMENT_START_BLOCK) {
        publicKey = block.getGeneratorPublicKey();
      } else {
        if (previousBlock.getHeight() + 1 >= rewardAssignment.getFromHeight()) {
          publicKey = accountService.getAccount(rewardAssignment.getRecipientId()).getPublicKey();
        } else {
          publicKey = accountService.getAccount(rewardAssignment.getPrevRecipientId()).getPublicKey();
        }
      }

      return Crypto.verify(block.getBlockSignature(), data2, publicKey, block.getVersion() >= 3);

    } catch (RuntimeException e) {

      logger.info("Error verifying block signature", e);
      return false;

    }

  }

  @Override
  public boolean verifyGenerationSignature(final Block block) throws BlockchainProcessor.BlockNotAcceptedException {
    try {
      Block previousBlock = blockchain.getBlock(block.getPreviousBlockId());

      if (previousBlock == null) {
        throw new BlockchainProcessor.BlockOutOfOrderException(
            "Can't verify generation signature because previous block is missing");
      }

      byte[] correctGenerationSignature = generator.calculateGenerationSignature(
          previousBlock.getGenerationSignature(), previousBlock.getGeneratorId());
      if (!Arrays.equals(block.getGenerationSignature(), correctGenerationSignature)) {
        return false;
      }
      int elapsedTime = block.getTimestamp() - previousBlock.getTimestamp();
      BigInteger pTime = block.getPocTime().divide(BigInteger.valueOf(previousBlock.getBaseTarget()));
      return BigInteger.valueOf(elapsedTime).compareTo(pTime) > 0;
    } catch (RuntimeException e) {
      logger.info("Error verifying block generation signature", e);
      return false;
    }
  }

  @Override
  public void preVerify(Block block) throws BlockchainProcessor.BlockNotAcceptedException {
    preVerify(block, null);
  }

  @Override
  public void preVerify(Block block, byte[] scoopData) throws BlockchainProcessor.BlockNotAcceptedException {
    // Just in case its already verified
    if (block.isVerified()) {
      return;
    }

    try {
      // Pre-verify poc:
      if (scoopData == null) {
        block.setPocTime(generator.calculateHit(block.getGeneratorId(), block.getNonce(), block.getGenerationSignature(), getScoopNum(block), block.getHeight()));
      } else {
        block.setPocTime(generator.calculateHit(block.getGeneratorId(), block.getNonce(), block.getGenerationSignature(), scoopData));
      }
    } catch (RuntimeException e) {
      logger.info("Error pre-verifying block generation signature", e);
      return;
    }

    for (Transaction transaction : block.getTransactions()) {
      if (!transaction.verifySignature()) {
        logger.info("Bad transaction signature during block pre-verification for tx: {} at block height: {}",
            Convert.toUnsignedLong(transaction.getId()), block.getHeight());
        throw new BlockchainProcessor.TransactionNotAcceptedException("Invalid signature for tx: "
            + Convert.toUnsignedLong(transaction.getId()) + "at block height: " + block.getHeight(),
            transaction);
      }
    }

  }

  @Override
  public void apply(Block block) {
    Account generatorAccount = accountService.getOrAddAccount(block.getGeneratorId());
    generatorAccount.apply(block.getGeneratorPublicKey(), block.getHeight());
    if (block.getHeight() < Constants.BURST_REWARD_RECIPIENT_ASSIGNMENT_START_BLOCK) {
      accountService.addToBalanceAndUnconfirmedBalanceNQT(generatorAccount, block.getTotalFeeNQT() + getBlockReward(block));
      accountService.addToForgedBalanceNQT(generatorAccount, block.getTotalFeeNQT() + getBlockReward(block));
    } else {
      Account rewardAccount;
      Account.RewardRecipientAssignment rewardAssignment = accountService.getRewardRecipientAssignment(generatorAccount);
      if (rewardAssignment == null) {
        rewardAccount = generatorAccount;
      } else if (block.getHeight() >= rewardAssignment.getFromHeight()) {
        rewardAccount = accountService.getAccount(rewardAssignment.getRecipientId());
      } else {
        rewardAccount = accountService.getAccount(rewardAssignment.getPrevRecipientId());
      }
      accountService.addToBalanceAndUnconfirmedBalanceNQT(rewardAccount, block.getTotalFeeNQT() + getBlockReward(block));
      accountService.addToForgedBalanceNQT(rewardAccount, block.getTotalFeeNQT() + getBlockReward(block));
    }

    for(Transaction transaction : block.getTransactions()) {
      transactionService.apply(transaction);
    }
  }

  @Override
  public long getBlockReward(Block block) {
    if (block.getHeight() == 0 || block.getHeight() >= 1944000) {
      return 0;
    }
    int month = block.getHeight() / 10800;
    return BigInteger.valueOf(10000).multiply(BigInteger.valueOf(95).pow(month))
        .divide(BigInteger.valueOf(100).pow(month)).longValue() * Constants.ONE_BURST;
  }

  @Override
  public void setPrevious(Block block, Block previousBlock) {
    if (previousBlock != null) {
      if (previousBlock.getId() != block.getPreviousBlockId()) {
        // shouldn't happen as previous id is already verified, but just in case
        throw new IllegalStateException("Previous block id doesn't match");
      }
      block.setHeight(previousBlock.getHeight() + 1);
      if(block.getBaseTarget() == Constants.INITIAL_BASE_TARGET ) {
        this.calculateBaseTarget(block, previousBlock);
      }
    } else {
      block.setHeight(0);
    }
    block.getTransactions().forEach(transaction -> transaction.setBlock(block));
  }

  @Override
  public void calculateBaseTarget(Block block, Block previousBlock) {
    if (block.getId() == Genesis.GENESIS_BLOCK_ID && block.getPreviousBlockId() == 0) {
      block.setBaseTarget(Constants.INITIAL_BASE_TARGET);
      block.setCumulativeDifficulty(BigInteger.ZERO);
    } else if (block.getHeight() < 4) {
      block.setBaseTarget(Constants.INITIAL_BASE_TARGET);
      block.setCumulativeDifficulty(previousBlock.getCumulativeDifficulty().add(Convert.two64.divide(BigInteger.valueOf(Constants.INITIAL_BASE_TARGET))));
    } else if (block.getHeight() < Constants.BURST_DIFF_ADJUST_CHANGE_BLOCK) {
      Block itBlock = previousBlock;
      BigInteger avgBaseTarget = BigInteger.valueOf(itBlock.getBaseTarget());
      do {
        itBlock = downloadCache.getBlock(itBlock.getPreviousBlockId());
        avgBaseTarget = avgBaseTarget.add(BigInteger.valueOf(itBlock.getBaseTarget()));
      } while (itBlock.getHeight() > block.getHeight() - 4);
      avgBaseTarget = avgBaseTarget.divide(BigInteger.valueOf(4));
      long difTime = (long) block.getTimestamp() - itBlock.getTimestamp();

      long curBaseTarget = avgBaseTarget.longValue();
      long newBaseTarget = BigInteger.valueOf(curBaseTarget).multiply(BigInteger.valueOf(difTime))
          .divide(BigInteger.valueOf(240L * 4)).longValue();
      if (newBaseTarget < 0 || newBaseTarget > Constants.MAX_BASE_TARGET) {
        newBaseTarget = Constants.MAX_BASE_TARGET;
      }
      if (newBaseTarget < (curBaseTarget * 9 / 10)) {
        newBaseTarget = curBaseTarget * 9 / 10;
      }
      if (newBaseTarget == 0) {
        newBaseTarget = 1;
      }
      long twofoldCurBaseTarget = curBaseTarget * 11 / 10;
      if (twofoldCurBaseTarget < 0) {
        twofoldCurBaseTarget = Constants.MAX_BASE_TARGET;
      }
      if (newBaseTarget > twofoldCurBaseTarget) {
        newBaseTarget = twofoldCurBaseTarget;
      }
      block.setBaseTarget(newBaseTarget);
      block.setCumulativeDifficulty(previousBlock.getCumulativeDifficulty().add(Convert.two64.divide(BigInteger.valueOf(newBaseTarget))));
    } else {
      Block itBlock = previousBlock;
      BigInteger avgBaseTarget = BigInteger.valueOf(itBlock.getBaseTarget());
      int blockCounter = 1;
      do {
        itBlock = downloadCache.getBlock(itBlock.getPreviousBlockId());
        blockCounter++;
        avgBaseTarget = (avgBaseTarget.multiply(BigInteger.valueOf(blockCounter))
            .add(BigInteger.valueOf(itBlock.getBaseTarget())))
            .divide(BigInteger.valueOf(blockCounter + 1L));
      } while (blockCounter < 24);
      long difTime = (long) block.getTimestamp() - itBlock.getTimestamp();
      long targetTimespan = 24L * 4 * 60;

      if (difTime < targetTimespan / 2) {
        difTime = targetTimespan / 2;
      }

      if (difTime > targetTimespan * 2) {
        difTime = targetTimespan * 2;
      }

      long curBaseTarget = previousBlock.getBaseTarget();
      long newBaseTarget = avgBaseTarget.multiply(BigInteger.valueOf(difTime))
          .divide(BigInteger.valueOf(targetTimespan)).longValue();

      if (newBaseTarget < 0 || newBaseTarget > Constants.MAX_BASE_TARGET) {
        newBaseTarget = Constants.MAX_BASE_TARGET;
      }

      if (newBaseTarget == 0) {
        newBaseTarget = 1;
      }

      if (newBaseTarget < curBaseTarget * 8 / 10) {
        newBaseTarget = curBaseTarget * 8 / 10;
      }

      if (newBaseTarget > curBaseTarget * 12 / 10) {
        newBaseTarget = curBaseTarget * 12 / 10;
      }

      block.setBaseTarget(newBaseTarget);
      block.setCumulativeDifficulty(previousBlock.getCumulativeDifficulty().add(Convert.two64.divide(BigInteger.valueOf(newBaseTarget))));
    }
  }

  @Override
  public int getScoopNum(Block block) {
    return generator.calculateScoop(block.getGenerationSignature(), block.getHeight());
  }
}
