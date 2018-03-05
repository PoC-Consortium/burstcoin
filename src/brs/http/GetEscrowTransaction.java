package brs.http;

import brs.Escrow;
import brs.services.EscrowService;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.common.Parameters.ESCROW_PARAMETER;
import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;

public final class GetEscrowTransaction extends APIServlet.APIRequestHandler {
	
  private final EscrowService escrowService;
	
  GetEscrowTransaction(EscrowService escrowService) {
    super(new APITag[] {APITag.ACCOUNTS}, ESCROW_PARAMETER);
    this.escrowService = escrowService;
  }
	
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {
    Long escrowId;
    try {
      escrowId = Convert.parseUnsignedLong(Convert.emptyToNull(req.getParameter(ESCROW_PARAMETER)));
    } catch(Exception e) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 3);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid or not specified escrow");
      return response;
    }
		
    Escrow escrow = escrowService.getEscrowTransaction(escrowId);
    if(escrow == null) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 5);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Escrow transaction not found");
      return response;
    }
		
    return JSONData.escrowTransaction(escrow);
  }
}
