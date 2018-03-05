package brs.http;

import static brs.http.common.Parameters.AMOUNT_NQT_PARAMETER;
import static brs.http.common.Parameters.RECIPIENT_PARAMETER;

import brs.Account;
import brs.BurstException;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;

public final class SendMoney extends CreateTransaction {

  private final ParameterService parameterService;

  SendMoney(ParameterService parameterService, APITransactionManager apiTransactionManager) {
    super(new APITag[]{APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, apiTransactionManager, RECIPIENT_PARAMETER, AMOUNT_NQT_PARAMETER);
    this.parameterService = parameterService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    long recipient = ParameterParser.getRecipientId(req);
    long amountNQT = ParameterParser.getAmountNQT(req);
    Account account = parameterService.getSenderAccount(req);
    return createTransaction(req, account, recipient, amountNQT);
  }

}
