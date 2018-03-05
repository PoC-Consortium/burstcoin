package brs.http;

import brs.Account;
import brs.Attachment;
import brs.Blockchain;
import brs.BurstException;
import brs.services.AccountService;
import brs.services.ParameterService;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.INCORRECT_PERIOD;
import static brs.http.JSONResponses.MISSING_PERIOD;
import static brs.http.common.Parameters.PERIOD_PARAMETER;
import static brs.http.common.Parameters.RECIPIENT_PARAMETER;
import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;

public final class LeaseBalance extends CreateTransaction {

  private final ParameterService parameterService;
  private final AccountService accountService;
  private final Blockchain blockchain;

  LeaseBalance(ParameterService parameterService, Blockchain blockchain, AccountService accountService, APITransactionManager apiTransactionManager) {
    super(new APITag[] {APITag.FORGING}, apiTransactionManager, PERIOD_PARAMETER, RECIPIENT_PARAMETER);
    this.parameterService = parameterService;
    this.accountService = accountService;
    this.blockchain = blockchain;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    String periodString = Convert.emptyToNull(req.getParameter(PERIOD_PARAMETER));
    if (periodString == null) {
      return MISSING_PERIOD;
    }
    short period;
    try {
      period = Short.parseShort(periodString);
      if (period < 1440) {
        return INCORRECT_PERIOD;
      }
    } catch (NumberFormatException e) {
      return INCORRECT_PERIOD;
    }

    Account account = parameterService.getSenderAccount(req);
    long recipient = ParameterParser.getRecipientId(req);
    Account recipientAccount = accountService.getAccount(recipient);
    if (recipientAccount == null || recipientAccount.getPublicKey() == null) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 8);
      response.put(ERROR_DESCRIPTION_RESPONSE, "recipient account does not have public key");
      return response;
    }
    Attachment attachment = new Attachment.AccountControlEffectiveBalanceLeasing(period, blockchain.getHeight());
    return createTransaction(req, account, recipient, 0, attachment);

  }

}
