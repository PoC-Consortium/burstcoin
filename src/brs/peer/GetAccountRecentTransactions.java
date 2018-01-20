package brs.peer;

import brs.Account;
import brs.Burst;
import brs.Transaction;
import brs.db.BurstIterator;
import brs.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;

public class GetAccountRecentTransactions extends PeerServlet.PeerRequestHandler {
	
  static final GetAccountRecentTransactions instance = new GetAccountRecentTransactions();
	
  private GetAccountRecentTransactions() {}
	
  @Override
  JSONStreamAware processRequest(JSONObject request, Peer peer) {
		
    JSONObject response = new JSONObject();
		
    try {
      Long accountId = Convert.parseAccountId((String)request.get(ACCOUNT_PARAMETER));
      Account account = Account.getAccount(accountId);
      JSONArray transactions = new JSONArray();
      if(account != null) {
        BurstIterator<? extends Transaction> iterator = Burst.getBlockchain().getTransactions(account, 0, (byte)-1, (byte)0, 0, 0, 9);
        while(iterator.hasNext()) {
          Transaction transaction = iterator.next();
          transactions.add(brs.http.JSONData.transaction(transaction));
        }
      }
      response.put("transactions", transactions);
    }
    catch(Exception e) {
    }
		
    return response;
  }

}
