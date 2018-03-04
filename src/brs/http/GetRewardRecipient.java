package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.ResultFields.REWARD_RECIPIENT_RESPONSE;

import brs.Account;
import brs.Blockchain;
import brs.BurstException;
import brs.services.AccountService;
import brs.services.ParameterService;
import brs.util.Convert;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetRewardRecipient extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;
  private final Blockchain blockchain;
  private final AccountService accountService;

  GetRewardRecipient(ParameterService parameterService, Blockchain blockchain, AccountService accountService) {
    super(new APITag[]{APITag.ACCOUNTS, APITag.MINING, APITag.INFO}, ACCOUNT_PARAMETER);
    this.parameterService = parameterService;
    this.blockchain = blockchain;
    this.accountService = accountService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    JSONObject response = new JSONObject();

    final Account account = parameterService.getAccount(req);
    Account.RewardRecipientAssignment assignment = accountService.getRewardRecipientAssignment(account);
    long height = blockchain.getLastBlock().getHeight();
    if (assignment == null) {
      response.put(REWARD_RECIPIENT_RESPONSE, Convert.toUnsignedLong(account.getId()));
    } else if (assignment.getFromHeight() > height + 1) {
      response.put(REWARD_RECIPIENT_RESPONSE, Convert.toUnsignedLong(assignment.getPrevRecipientId()));
    } else {
      response.put(REWARD_RECIPIENT_RESPONSE, Convert.toUnsignedLong(assignment.getRecipientId()));
    }

    return response;
  }

}
