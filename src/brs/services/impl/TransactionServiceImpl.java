package brs.services.impl;

import brs.Account;
import brs.Appendix;
import brs.Blockchain;
import brs.Burst;
import brs.BurstException;
import brs.Constants;
import brs.Transaction;

import brs.services.AccountService;
import brs.services.TransactionService;

public class TransactionServiceImpl implements TransactionService {

  private final AccountService accountService;
  private final Blockchain blockchain;

  public TransactionServiceImpl(AccountService accountService, Blockchain blockchain) {
    this.accountService = accountService;
    this.blockchain = blockchain;
  }

  @Override
  public boolean verifyPublicKey(Transaction transaction) {
    synchronized (Burst.getTransactionProcessor().getUnconfirmedTransactionsSyncObj()) {
    Account account = accountService.getAccount(transaction.getSenderId());
    if (account == null) {
      return false;
    }
    if (transaction.getSignature() == null) {
      return false;
    }
    return account.setOrVerify(transaction.getSenderPublicKey(), transaction.getHeight());
    }
  }

  @Override
  public void validate(Transaction transaction) throws BurstException.ValidationException {
	  synchronized (Burst.getTransactionProcessor().getUnconfirmedTransactionsSyncObj()) {
    for (Appendix.AbstractAppendix appendage : transaction.getAppendages()) {
      appendage.validate(transaction);
    }
    long minimumFeeNQT = transaction.getType().minimumFeeNQT(blockchain.getHeight(), transaction.getAppendagesSize());
    if (transaction.getFeeNQT() < minimumFeeNQT) {
      throw new BurstException.NotCurrentlyValidException(String.format("Transaction fee %d less than minimum fee %d at height %d",
          transaction.getFeeNQT(), minimumFeeNQT, blockchain.getHeight()));
    }
    if (blockchain.getHeight() >= Constants.PUBLIC_KEY_ANNOUNCEMENT_BLOCK) {
      if (transaction.getType().hasRecipient() && transaction.getRecipientId()!= 0) {
        Account recipientAccount = accountService.getAccount(transaction.getRecipientId());
        if ((recipientAccount == null || recipientAccount.getPublicKey() == null) && transaction.getPublicKeyAnnouncement() == null) {
          throw new BurstException.NotCurrentlyValidException("Recipient account does not have a public key, must attach a public key announcement");
        }
      }
    }
	  }
  }

  @Override
  public boolean applyUnconfirmed(Transaction transaction) {
	  synchronized (Burst.getTransactionProcessor().getUnconfirmedTransactionsSyncObj()) {
    Account senderAccount = accountService.getAccount(transaction.getSenderId());
    return senderAccount != null && transaction.getType().applyUnconfirmed(transaction, senderAccount);
	  }
  }

  @Override
  public void apply(Transaction transaction) {
	  synchronized (Burst.getTransactionProcessor().getUnconfirmedTransactionsSyncObj()) {
    Account senderAccount = accountService.getAccount(transaction.getSenderId());
    senderAccount.apply(transaction.getSenderPublicKey(), transaction.getHeight());
    Account recipientAccount = accountService.getOrAddAccount(transaction.getRecipientId());
    for (Appendix.AbstractAppendix appendage : transaction.getAppendages()) {
      appendage.apply(transaction, senderAccount, recipientAccount);
    }
	  }
  }

  @Override
  public void undoUnconfirmed(Transaction transaction) {
	  synchronized (Burst.getTransactionProcessor().getUnconfirmedTransactionsSyncObj()) {
    final Account senderAccount = accountService.getAccount(transaction.getSenderId());
    transaction.getType().undoUnconfirmed(transaction, senderAccount);
	  }
  }

}
