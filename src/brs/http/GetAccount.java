package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.ResultFields.ACCOUNT_RESPONSE;
import static brs.http.common.ResultFields.ASSET_BALANCES_RESPONSE;
import static brs.http.common.ResultFields.ASSET_RESPONSE;
import static brs.http.common.ResultFields.BALANCE_QNT_RESPONSE;
import static brs.http.common.ResultFields.DESCRIPTION_RESPONSE;
import static brs.http.common.ResultFields.NAME_RESPONSE;
import static brs.http.common.ResultFields.PUBLIC_KEY_RESPONSE;
import static brs.http.common.ResultFields.UNCONFIRMED_ASSET_BALANCES_RESPONSE;
import static brs.http.common.ResultFields.UNCONFIRMED_BALANCE_QNT_RESPONSE;

import brs.Account;
import brs.BurstException;
import brs.db.BurstIterator;
import brs.services.AccountService;
import brs.services.ParameterService;
import brs.util.Convert;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetAccount extends APIServlet.APIRequestHandler {

  private ParameterService parameterService;
  private AccountService accountService;

  GetAccount(ParameterService parameterService, AccountService accountService) {
    super(new APITag[] {APITag.ACCOUNTS}, ACCOUNT_PARAMETER);
    this.parameterService = parameterService;
    this.accountService = accountService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    Account account = parameterService.getAccount(req);

    JSONObject response = JSONData.accountBalance(account);
    JSONData.putAccount(response, ACCOUNT_RESPONSE, account.getId());

    if (account.getPublicKey() != null) {
      response.put(PUBLIC_KEY_RESPONSE, Convert.toHexString(account.getPublicKey()));
    }
    if (account.getName() != null) {
      response.put(NAME_RESPONSE, account.getName());
    }
    if (account.getDescription() != null) {
      response.put(DESCRIPTION_RESPONSE, account.getDescription());
    }

    try (BurstIterator<Account.AccountAsset> accountAssets = accountService.getAssets(account.getId(), 0, -1)) {
      JSONArray assetBalances = new JSONArray();
      JSONArray unconfirmedAssetBalances = new JSONArray();
      while (accountAssets.hasNext()) {
        Account.AccountAsset accountAsset = accountAssets.next();
        JSONObject assetBalance = new JSONObject();
        assetBalance.put(ASSET_RESPONSE, Convert.toUnsignedLong(accountAsset.getAssetId()));
        assetBalance.put(BALANCE_QNT_RESPONSE, String.valueOf(accountAsset.getQuantityQNT()));
        assetBalances.add(assetBalance);
        JSONObject unconfirmedAssetBalance = new JSONObject();
        unconfirmedAssetBalance.put(ASSET_RESPONSE, Convert.toUnsignedLong(accountAsset.getAssetId()));
        unconfirmedAssetBalance.put(UNCONFIRMED_BALANCE_QNT_RESPONSE, String.valueOf(accountAsset.getUnconfirmedQuantityQNT()));
        unconfirmedAssetBalances.add(unconfirmedAssetBalance);
      }

      if (! assetBalances.isEmpty()) {
        response.put(ASSET_BALANCES_RESPONSE, assetBalances);
      }
      if (! unconfirmedAssetBalances.isEmpty()) {
        response.put(UNCONFIRMED_ASSET_BALANCES_RESPONSE, unconfirmedAssetBalances);
      }
    }

    return response;
  }

}
