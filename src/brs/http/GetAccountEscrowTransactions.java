package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.ESCROWS_RESPONSE;

import brs.Account;
import brs.BurstException;
import brs.Escrow;
import brs.services.EscrowService;
import brs.services.ParameterService;
import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetAccountEscrowTransactions extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;

  private final EscrowService escrowService;

  GetAccountEscrowTransactions(ParameterService parameterService, EscrowService escrowService) {
    super(new APITag[]{APITag.ACCOUNTS}, ACCOUNT_PARAMETER);
    this.parameterService = parameterService;
    this.escrowService = escrowService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    final Account account = parameterService.getAccount(req);

    Collection<Escrow> accountEscrows = escrowService.getEscrowTransactionsByParticipant(account.getId());

    JSONObject response = new JSONObject();

    JSONArray escrows = new JSONArray();

    for (Escrow escrow : accountEscrows) {
      escrows.add(JSONData.escrowTransaction(escrow));
    }

    response.put(ESCROWS_RESPONSE, escrows);
    return response;
  }
}
