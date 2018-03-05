package brs.http;

import static brs.http.common.Parameters.RECIPIENT_PARAMETER;
import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;

import brs.Account;
import brs.Attachment;
import brs.Blockchain;
import brs.BurstException;
import brs.services.AccountService;
import brs.services.ParameterService;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SetRewardRecipient extends CreateTransaction {

  private final ParameterService parameterService;
  private final Blockchain blockchain;
  private AccountService accountService;

  public SetRewardRecipient(ParameterService parameterService, Blockchain blockchain, AccountService accountService, APITransactionManager apiTransactionManager) {
    super(new APITag[] {APITag.ACCOUNTS, APITag.MINING, APITag.CREATE_TRANSACTION}, apiTransactionManager, RECIPIENT_PARAMETER);
    this.parameterService = parameterService;
    this.blockchain = blockchain;
    this.accountService = accountService;
  }
	
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    final Account account = parameterService.getSenderAccount(req);
    Long recipient = ParameterParser.getRecipientId(req);
    Account recipientAccount = accountService.getAccount(recipient);
    if (recipientAccount == null || recipientAccount.getPublicKey() == null) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 8);
      response.put(ERROR_DESCRIPTION_RESPONSE, "recipient account does not have public key");
      return response;
    }
    Attachment attachment = new Attachment.BurstMiningRewardRecipientAssignment(blockchain.getHeight());
    return createTransaction(req, account, recipient, 0, attachment);
  }

}
