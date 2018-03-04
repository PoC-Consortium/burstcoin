package brs.services;

import brs.Account;
import brs.Account.AccountAsset;
import brs.Account.Event;
import brs.Account.RewardRecipientAssignment;
import brs.AssetTransfer;
import brs.db.BurstIterator;
import brs.util.Listener;

public interface AccountService {

  boolean addListener(Listener<Account> listener, Event eventType);

  boolean addAssetListener(Listener<AccountAsset> listener, Event eventType);

  Account getAccount(long id);

  Account getAccount(long id, int height);

  Account getAccount(byte[] publicKey);

  BurstIterator<AssetTransfer> getAssetTransfers(long accountId, int from, int to);

  BurstIterator<AccountAsset> getAssets(long accountId, int from, int to);

  BurstIterator<RewardRecipientAssignment> getAccountsWithRewardRecipient(Long recipientId);

  BurstIterator<Account> getAllAccounts(int from, int to);

  Account getOrAddAccount(long id);

  void flushAccountTable();

  int getCount();

  void addToForgedBalanceNQT(Account account, long amountNQT);

  void setAccountInfo(Account account, String name, String description);

  void addToAssetBalanceQNT(Account account, long assetId, long quantityQNT);

  void addToUnconfirmedAssetBalanceQNT(Account account, long assetId, long quantityQNT);

  void addToAssetAndUnconfirmedAssetBalanceQNT(Account account, long assetId, long quantityQNT);

  void addToBalanceNQT(Account account, long amountNQT);

  void addToUnconfirmedBalanceNQT(Account account, long amountNQT);

  void addToBalanceAndUnconfirmedBalanceNQT(Account account, long amountNQT);

  RewardRecipientAssignment getRewardRecipientAssignment(Account account);

  void setRewardRecipientAssignment(Account account, Long recipient);

  long getUnconfirmedAssetBalanceQNT(Account account, long assetId);
}
