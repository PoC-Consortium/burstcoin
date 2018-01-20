package brs.user;

import brs.Account;
import brs.Block;
import brs.Burst;
import brs.Transaction;
import brs.db.BurstIterator;
import brs.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

import static brs.Constants.*;
import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;
import static brs.http.common.ResultFields.ACCOUNT_RESPONSE;
import static brs.http.common.ResultFields.BALANCE_NQT_RESPONSE;
import static brs.user.JSONResponses.LOCK_ACCOUNT;

public final class UnlockAccount extends UserServlet.UserRequestHandler {

  static final UnlockAccount instance = new UnlockAccount();

  private UnlockAccount() {}

  private static final Comparator<JSONObject> myTransactionsComparator = new Comparator<JSONObject>() {
      @Override
      public int compare(JSONObject o1, JSONObject o2) {
        int t1 = ((Number)o1.get("timestamp")).intValue();
        int t2 = ((Number)o2.get("timestamp")).intValue();
        if (t1 < t2) {
          return 1;
        }
        if (t1 > t2) {
          return -1;
        }
        String id1 = (String)o1.get("id");
        String id2 = (String)o2.get("id");
        return id2.compareTo(id1);
      }
    };

  @Override
  JSONStreamAware processRequest(HttpServletRequest req, User user) throws IOException {
    String secretPhrase = req.getParameter(SECRET_PHRASE_PARAMETER);
    // lock all other instances of this account being unlocked
    for (User u : Users.getAllUsers()) {
      if (secretPhrase.equals(u.getSecretPhrase())) {
        u.lockAccount();
        if (! u.isInactive()) {
          u.enqueue(LOCK_ACCOUNT);
        }
      }
    }

    long accountId = user.unlockAccount(secretPhrase);

    JSONObject response = new JSONObject();
    response.put(RESPONSE, "unlockAccount");
    response.put(ACCOUNT_RESPONSE, Convert.toUnsignedLong(accountId));

    if (secretPhrase.length() < 30) {

      response.put("secretPhraseStrength", 1);

    } else {

      response.put("secretPhraseStrength", 5);

    }

    Account account = Account.getAccount(accountId);
    if (account == null) {

      response.put(BALANCE_NQT_RESPONSE, 0);

    } else {

      response.put(BALANCE_NQT_RESPONSE, account.getUnconfirmedBalanceNQT());

      JSONArray myTransactions = new JSONArray();
      byte[] accountPublicKey = account.getPublicKey();
      try (BurstIterator<? extends Transaction> transactions = Burst.getTransactionProcessor().getAllUnconfirmedTransactions()) {
        while (transactions.hasNext()) {
          Transaction transaction = transactions.next();
          JSONObject myTransaction = new JSONObject();
          myTransaction.put("index", Users.getIndex(transaction));
          myTransaction.put("transactionTimestamp", transaction.getTimestamp());
          myTransaction.put("deadline", transaction.getDeadline());

          if (Arrays.equals(transaction.getSenderPublicKey(), accountPublicKey)) {
            myTransaction.put("account", Convert.toUnsignedLong(transaction.getRecipientId()));
            myTransaction.put("sentAmountNQT", transaction.getAmountNQT());
            if (accountId == transaction.getRecipientId()) {
              myTransaction.put("receivedAmountNQT", transaction.getAmountNQT());
            }

          } else if (accountId == transaction.getRecipientId()) {
            myTransaction.put("account", Convert.toUnsignedLong(transaction.getSenderId()));
            myTransaction.put("receivedAmountNQT", transaction.getAmountNQT());

          }

          myTransaction.put("feeNQT", transaction.getFeeNQT());
          myTransaction.put("numberOfConfirmations", -1);
          myTransaction.put("id", transaction.getStringId());
          myTransactions.add(myTransaction);
        }
      }

      SortedSet<JSONObject> myTransactionsSet = new TreeSet<>(myTransactionsComparator);

      int blockchainHeight = Burst.getBlockchain().getLastBlock().getHeight();
      try (BurstIterator<? extends Block> blockIterator = Burst.getBlockchain().getBlocks(account, 0)) {
        while (blockIterator.hasNext()) {
          Block block = blockIterator.next();
          if (block.getTotalFeeNQT() > 0) {
            JSONObject myTransaction = new JSONObject();
            myTransaction.put("index", "block" + Users.getIndex(block));
            myTransaction.put("blockTimestamp", block.getTimestamp());
            myTransaction.put("block", block.getStringId());
            myTransaction.put("earnedAmountNQT", block.getTotalFeeNQT());
            myTransaction.put("numberOfConfirmations", blockchainHeight - block.getHeight());
            myTransaction.put("id", "-");
            myTransaction.put("timestamp", block.getTimestamp());
            myTransactionsSet.add(myTransaction);
          }
        }
      }

      try (BurstIterator<? extends Transaction> transactionIterator = Burst.getBlockchain().getTransactions(account, (byte) -1, (byte) -1, 0)) {
        while (transactionIterator.hasNext()) {
          Transaction transaction = transactionIterator.next();
          JSONObject myTransaction = new JSONObject();
          myTransaction.put("index", Users.getIndex(transaction));
          myTransaction.put("blockTimestamp", transaction.getBlockTimestamp());
          myTransaction.put("transactionTimestamp", transaction.getTimestamp());
          if (transaction.getSenderId() == accountId) {
            myTransaction.put("account", Convert.toUnsignedLong(transaction.getRecipientId()));
            myTransaction.put("sentAmountNQT", transaction.getAmountNQT());
            if (accountId == transaction.getRecipientId()) {
              myTransaction.put("receivedAmountNQT", transaction.getAmountNQT());
            }
          } else if (transaction.getRecipientId() == accountId) {
            myTransaction.put("account", Convert.toUnsignedLong(transaction.getSenderId()));
            myTransaction.put("receivedAmountNQT", transaction.getAmountNQT());

          }
          myTransaction.put("feeNQT", transaction.getFeeNQT());
          myTransaction.put("numberOfConfirmations", blockchainHeight - transaction.getHeight());
          myTransaction.put("id", transaction.getStringId());
          myTransaction.put("timestamp", transaction.getTimestamp());
          myTransactionsSet.add(myTransaction);
        }
      }

      Iterator<JSONObject> iterator = myTransactionsSet.iterator();
      while (myTransactions.size() < 1000 && iterator.hasNext()) {
        myTransactions.add(iterator.next());
      }

      if (myTransactions.size() > 0) {
        JSONObject response2 = new JSONObject();
        response2.put(RESPONSE, "processNewData");
        response2.put("addedMyTransactions", myTransactions);
        user.enqueue(response2);
      }
    }
    return response;
  }

  @Override
  boolean requirePost() {
    return true;
  }

}
