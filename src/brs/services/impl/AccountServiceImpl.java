package brs.services.impl;

import brs.Account;
import brs.Account.AccountAsset;
import brs.Account.Event;
import brs.Account.RewardRecipientAssignment;
import brs.AssetTransfer;
import brs.Burst;
import brs.Constants;
import brs.crypto.Crypto;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.BurstKey.LinkKeyFactory;
import brs.db.BurstKey.LongKeyFactory;
import brs.db.VersionedBatchEntityTable;
import brs.db.VersionedEntityTable;
import brs.db.store.AccountStore;
import brs.db.store.AssetTransferStore;
import brs.services.AccountService;
import brs.util.Convert;
import brs.util.Listener;
import brs.util.Listeners;
import java.util.Arrays;

public class AccountServiceImpl implements AccountService {

  private final AccountStore accountStore;
  private final VersionedBatchEntityTable<Account> accountTable;
  private final LongKeyFactory<Account> accountBurstKeyFactory;
  private final VersionedEntityTable<AccountAsset> accountAssetTable;
  private final LinkKeyFactory<AccountAsset> accountAssetKeyFactory;
  private final VersionedEntityTable<RewardRecipientAssignment> rewardRecipientAssignmentTable;
  private final LongKeyFactory<RewardRecipientAssignment> rewardRecipientAssignmentKeyFactory;

  private final AssetTransferStore assetTransferStore;

  private final Listeners<Account, Event> listeners = new Listeners<>();
  private final Listeners<AccountAsset, Event> assetListeners = new Listeners<>();

  public AccountServiceImpl(AccountStore accountStore, AssetTransferStore assetTransferStore) {
    this.accountStore = accountStore;
    this.accountTable = accountStore.getAccountTable();
    this.accountBurstKeyFactory = accountStore.getAccountKeyFactory();
    this.assetTransferStore = assetTransferStore;
    this.accountAssetTable = accountStore.getAccountAssetTable();
    this.accountAssetKeyFactory = accountStore.getAccountAssetKeyFactory();
    this.rewardRecipientAssignmentTable = accountStore.getRewardRecipientAssignmentTable();
    this.rewardRecipientAssignmentKeyFactory = accountStore.getRewardRecipientAssignmentKeyFactory();
  }

  @Override
  public boolean addListener(Listener<Account> listener, Event eventType) {
    return listeners.addListener(listener, eventType);
  }

  @Override
  public boolean addAssetListener(Listener<AccountAsset> listener, Event eventType) {
    return assetListeners.addListener(listener, eventType);
  }

  @Override
  public Account getAccount(long id) {
    return id == 0 ? null : accountTable.get(accountBurstKeyFactory.newKey(id));
  }

  @Override
  public Account getAccount(long id, int height) {
    return id == 0 ? null : accountTable.get(accountBurstKeyFactory.newKey(id), height);
  }

  @Override
  public Account getAccount(byte[] publicKey) {
    final Account account = accountTable.get(accountBurstKeyFactory.newKey(getId(publicKey)));

    if (account == null) {
      return null;
    }

    if (account.getPublicKey() == null || Arrays.equals(account.getPublicKey(), publicKey)) {
      return account;
    }

    throw new RuntimeException("DUPLICATE KEY for account " + Convert.toUnsignedLong(account.getId())
        + " existing key " + Convert.toHexString(account.getPublicKey()) + " new key " + Convert.toHexString(publicKey));
  }

  @Override
  public BurstIterator<AssetTransfer> getAssetTransfers(long accountId, int from, int to) {
    return assetTransferStore.getAccountAssetTransfers(accountId, from, to);
  }

  @Override
  public BurstIterator<AccountAsset> getAssets(long accountId, int from, int to) {
    return accountStore.getAssets(from, to, accountId);
  }

  @Override
  public BurstIterator<RewardRecipientAssignment> getAccountsWithRewardRecipient(Long recipientId) {
    return accountStore.getAccountsWithRewardRecipient(recipientId);
  }

  @Override
  public BurstIterator<Account> getAllAccounts(int from, int to) {
    return accountTable.getAll(from, to);
  }

  @Override
  public Account getOrAddAccount(long id) {
    Account account = accountTable.get(accountBurstKeyFactory.newKey(id));
    if (account == null) {
      account = new Account(id);
      accountTable.insert(account);
    }
    return account;
  }

  public static long getId(byte[] publicKey) {
    byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
    return Convert.fullHashToId(publicKeyHash);
  }

  @Override
  public void flushAccountTable() {
    accountTable.finish();
  }

  @Override
  public int getCount() {
    return accountTable.getCount();
  }

  @Override
  public void addToForgedBalanceNQT(Account account, long amountNQT) {
    if (amountNQT == 0) {
      return;
    }
    account.setForgedBalanceNQT(Convert.safeAdd(account.getForgedBalanceNQT(), amountNQT));
    accountTable.insert(account);
  }

  @Override
  public void setAccountInfo(Account account, String name, String description) {
    account.setName(Convert.emptyToNull(name.trim()));
    account.setDescription(Convert.emptyToNull(description.trim()));
    accountTable.insert(account);
  }

  @Override
  public void addToAssetBalanceQNT(Account account, long assetId, long quantityQNT) {
    if (quantityQNT == 0) {
      return;
    }
    AccountAsset accountAsset;

    BurstKey newKey = accountAssetKeyFactory.newKey(account.getId(), assetId);
    accountAsset = accountAssetTable.get(newKey);
    long assetBalance = accountAsset == null ? 0 : accountAsset.getQuantityQNT();
    assetBalance = Convert.safeAdd(assetBalance, quantityQNT);
    if (accountAsset == null) {
      accountAsset = new AccountAsset(newKey, account.getId(), assetId, assetBalance, 0);
    } else {
      accountAsset.setQuantityQNT(assetBalance);
    }
    saveAccountAsset(accountAsset);
    listeners.notify(account, Event.ASSET_BALANCE);
    assetListeners.notify(accountAsset, Event.ASSET_BALANCE);
  }

  @Override
  public void addToUnconfirmedAssetBalanceQNT(Account account, long assetId, long quantityQNT) {
    if (quantityQNT == 0) {
      return;
    }
    AccountAsset accountAsset;
    BurstKey newKey = accountAssetKeyFactory.newKey(account.getId(), assetId);
    accountAsset = accountAssetTable.get(newKey);
    long unconfirmedAssetBalance = accountAsset == null ? 0 : accountAsset.getUnconfirmedQuantityQNT();
    unconfirmedAssetBalance = Convert.safeAdd(unconfirmedAssetBalance, quantityQNT);
    if (accountAsset == null) {
      accountAsset = new AccountAsset(newKey, account.getId(), assetId, 0, unconfirmedAssetBalance);
    } else {
      accountAsset.setUnconfirmedQuantityQNT(unconfirmedAssetBalance);
    }
    saveAccountAsset(accountAsset);
    listeners.notify(account, Event.UNCONFIRMED_ASSET_BALANCE);
    assetListeners.notify(accountAsset, Event.UNCONFIRMED_ASSET_BALANCE);
  }

  @Override
  public void addToAssetAndUnconfirmedAssetBalanceQNT(Account account, long assetId, long quantityQNT) {
    if (quantityQNT == 0) {
      return;
    }
    AccountAsset accountAsset;
    BurstKey newKey = accountAssetKeyFactory.newKey(account.getId(), assetId);
    accountAsset = accountAssetTable.get(newKey);
    long assetBalance = accountAsset == null ? 0 : accountAsset.getQuantityQNT();
    assetBalance = Convert.safeAdd(assetBalance, quantityQNT);
    long unconfirmedAssetBalance = accountAsset == null ? 0 : accountAsset.getUnconfirmedQuantityQNT();
    unconfirmedAssetBalance = Convert.safeAdd(unconfirmedAssetBalance, quantityQNT);
    if (accountAsset == null) {
      accountAsset = new AccountAsset(newKey, account.getId(), assetId, assetBalance, unconfirmedAssetBalance);
    } else {
      accountAsset.setQuantityQNT(assetBalance);
      accountAsset.setUnconfirmedQuantityQNT(unconfirmedAssetBalance);
    }
    saveAccountAsset(accountAsset);
    listeners.notify(account, Event.ASSET_BALANCE);
    listeners.notify(account, Event.UNCONFIRMED_ASSET_BALANCE);
    assetListeners.notify(accountAsset, Event.ASSET_BALANCE);
    assetListeners.notify(accountAsset, Event.UNCONFIRMED_ASSET_BALANCE);
  }

  @Override
  public void addToBalanceNQT(Account account, long amountNQT) {
    if (amountNQT == 0) {
      return;
    }
    account.setBalanceNQT(Convert.safeAdd(account.getBalanceNQT(), amountNQT));
    account.checkBalance();
    accountTable.insert(account);
    listeners.notify(account, Event.BALANCE);
  }

  @Override
  public void addToUnconfirmedBalanceNQT(Account account, long amountNQT) {
    if (amountNQT == 0) {
      return;
    }
    account.setUnconfirmedBalanceNQT(Convert.safeAdd(account.getUnconfirmedBalanceNQT(), amountNQT));
    account.checkBalance();
    accountTable.insert(account);
    listeners.notify(account, Event.UNCONFIRMED_BALANCE);
  }

  @Override
  public void addToBalanceAndUnconfirmedBalanceNQT(Account account, long amountNQT) {
    if (amountNQT == 0) {
      return;
    }
    account.setBalanceNQT(Convert.safeAdd(account.getBalanceNQT(), amountNQT));
    account.setUnconfirmedBalanceNQT(Convert.safeAdd(account.getUnconfirmedBalanceNQT(), amountNQT));
    account.checkBalance();
    accountTable.insert(account);
    listeners.notify(account, Event.BALANCE);
    listeners.notify(account, Event.UNCONFIRMED_BALANCE);
  }

  @Override
  public RewardRecipientAssignment getRewardRecipientAssignment(Account account) {
    return getRewardRecipientAssignment(account.getId());
  }

  private RewardRecipientAssignment getRewardRecipientAssignment(Long id) {
    return rewardRecipientAssignmentTable.get(rewardRecipientAssignmentKeyFactory.newKey(id));
  }

  @Override
  public void setRewardRecipientAssignment(Account account, Long recipient) {
    int currentHeight = Burst.getBlockchain().getLastBlock().getHeight();
    RewardRecipientAssignment assignment = getRewardRecipientAssignment(account.getId());
    if (assignment == null) {
      BurstKey burstKey = rewardRecipientAssignmentKeyFactory.newKey(account.getId());
      assignment = new RewardRecipientAssignment(account.getId(), account.getId(), recipient, (int) (currentHeight + Constants.BURST_REWARD_RECIPIENT_ASSIGNMENT_WAIT_TIME), burstKey);
    } else {
      assignment.setRecipient(recipient, (int) (currentHeight + Constants.BURST_REWARD_RECIPIENT_ASSIGNMENT_WAIT_TIME));
    }
    rewardRecipientAssignmentTable.insert(assignment);
  }

  @Override
  public long getUnconfirmedAssetBalanceQNT(Account account, long assetId) {
    BurstKey newKey = Burst.getStores().getAccountStore().getAccountAssetKeyFactory().newKey(account.getId(), assetId);
    AccountAsset accountAsset = accountAssetTable.get(newKey);
    return accountAsset == null ? 0 : accountAsset.getUnconfirmedQuantityQNT();
  }


  private void saveAccountAsset(AccountAsset accountAsset) {
    accountAsset.checkBalance();
    if (accountAsset.getQuantityQNT() > 0 || accountAsset.getUnconfirmedQuantityQNT() > 0) {
      accountAssetTable.insert(accountAsset);
    } else {
      accountAssetTable.delete(accountAsset);
    }
  }
}
