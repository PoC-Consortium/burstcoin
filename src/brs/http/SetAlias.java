package brs.http;

import brs.*;
import brs.services.AliasService;
import brs.services.ParameterService;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.*;
import static brs.http.common.Parameters.ALIAS_NAME_PARAMETER;
import static brs.http.common.Parameters.ALIAS_URI_PARAMETER;
import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;

public final class SetAlias extends CreateTransaction {

  private final ParameterService parameterService;
  private final Blockchain blockchain;
  private final AliasService aliasService;

  public SetAlias(ParameterService parameterService, Blockchain blockchain, AliasService aliasService, APITransactionManager apiTransactionManager) {
    super(new APITag[] {APITag.ALIASES, APITag.CREATE_TRANSACTION}, apiTransactionManager, ALIAS_NAME_PARAMETER, ALIAS_URI_PARAMETER);
    this.parameterService = parameterService;
    this.blockchain = blockchain;
    this.aliasService = aliasService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    String aliasName = Convert.emptyToNull(req.getParameter(ALIAS_NAME_PARAMETER));
    String aliasURI = Convert.nullToEmpty(req.getParameter(ALIAS_URI_PARAMETER));

    if (aliasName == null) {
      return MISSING_ALIAS_NAME;
    }

    aliasName = aliasName.trim();
    if (aliasName.isEmpty() || aliasName.length() > Constants.MAX_ALIAS_LENGTH) {
      return INCORRECT_ALIAS_LENGTH;
    }

    String normalizedAlias = aliasName.toLowerCase();
    for (int i = 0; i < normalizedAlias.length(); i++) {
      if (Constants.ALPHABET.indexOf(normalizedAlias.charAt(i)) < 0) {
        return INCORRECT_ALIAS_NAME;
      }
    }

    aliasURI = aliasURI.trim();
    if (aliasURI.length() > Constants.MAX_ALIAS_URI_LENGTH) {
      return INCORRECT_URI_LENGTH;
    }

    Account account = parameterService.getSenderAccount(req);

    Alias alias = aliasService.getAlias(normalizedAlias);
    if (alias != null && alias.getAccountId() != account.getId()) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 8);
      response.put(ERROR_DESCRIPTION_RESPONSE, "\"" + aliasName + "\" is already used");
      return response;
    }

    Attachment attachment = new Attachment.MessagingAliasAssignment(aliasName, aliasURI, blockchain.getHeight());
    return createTransaction(req, account, attachment);

  }

}
