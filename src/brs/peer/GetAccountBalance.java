package brs.peer;

import brs.Account;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.ResultFields.BALANCE_NQT_RESPONSE;

public class GetAccountBalance extends PeerServlet.PeerRequestHandler {

  static final GetAccountBalance instance = new GetAccountBalance();

  private GetAccountBalance() {
  }

  @Override
  JSONStreamAware processRequest(JSONObject request, Peer peer) {

    JSONObject response = new JSONObject();

    try {
      Long accountId = Convert.parseAccountId((String) request.get(ACCOUNT_PARAMETER));
      Account account = Account.getAccount(accountId);
      if (account != null) {
        response.put(BALANCE_NQT_RESPONSE, Convert.toUnsignedLong(account.getBalanceNQT()));
      } else {
        response.put(BALANCE_NQT_RESPONSE, "0");
      }
    } catch (Exception e) {
    }

    return response;
  }
}
