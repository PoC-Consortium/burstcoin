package brs.http;

import brs.*;
import brs.services.ParameterService;
import brs.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.*;
import static brs.http.common.Parameters.ALIAS_NAME_PARAMETER;
import static brs.http.common.Parameters.ALIAS_PARAMETER;
import static brs.http.common.Parameters.PRICE_NQT_PARAMETER;
import static brs.http.common.Parameters.RECIPIENT_PARAMETER;

public final class SellAlias extends CreateTransaction {

  private final ParameterService parameterService;
  private final Blockchain blockchain;

  SellAlias(ParameterService parameterService, Blockchain blockchain, APITransactionManager apiTransactionManager) {
    super(new APITag[] {APITag.ALIASES, APITag.CREATE_TRANSACTION}, apiTransactionManager, ALIAS_PARAMETER, ALIAS_NAME_PARAMETER, RECIPIENT_PARAMETER, PRICE_NQT_PARAMETER);
    this.parameterService = parameterService;
    this.blockchain = blockchain;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    Alias alias = parameterService.getAlias(req);
    Account owner = parameterService.getSenderAccount(req);

    String priceValueNQT = Convert.emptyToNull(req.getParameter(PRICE_NQT_PARAMETER));
    if (priceValueNQT == null) {
      return MISSING_PRICE;
    }
    long priceNQT;
    try {
      priceNQT = Long.parseLong(priceValueNQT);
    } catch (RuntimeException e) {
      return INCORRECT_PRICE;
    }
    if (priceNQT < 0 || priceNQT > Constants.MAX_BALANCE_NQT) {
      throw new ParameterException(INCORRECT_PRICE);
    }

    String recipientValue = Convert.emptyToNull(req.getParameter(RECIPIENT_PARAMETER));
    long recipientId = 0;
    if (recipientValue != null) {
      try {
        recipientId = Convert.parseAccountId(recipientValue);
      } catch (RuntimeException e) {
        return INCORRECT_RECIPIENT;
      }
      if (recipientId == 0) {
        return INCORRECT_RECIPIENT;
      }
    }

    if (alias.getAccountId() != owner.getId()) {
      return INCORRECT_ALIAS_OWNER;
    }

    Attachment attachment = new Attachment.MessagingAliasSell(alias.getAliasName(), priceNQT, blockchain.getHeight());
    return createTransaction(req, owner, recipientId, 0, attachment);
  }
}
