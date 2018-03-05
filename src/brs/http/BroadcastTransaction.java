package brs.http;

import static brs.http.common.Parameters.TRANSACTION_BYTES_PARAMETER;
import static brs.http.common.Parameters.TRANSACTION_JSON_PARAMETER;
import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;
import static brs.http.common.ResultFields.ERROR_RESPONSE;
import static brs.http.common.ResultFields.FULL_HASH_RESPONSE;
import static brs.http.common.ResultFields.TRANSACTION_RESPONSE;

import brs.BurstException;
import brs.Transaction;
import brs.TransactionProcessor;
import brs.services.ParameterService;
import brs.services.TransactionService;
import brs.util.Convert;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class BroadcastTransaction extends APIServlet.APIRequestHandler {

  private static final Logger logger = Logger.getLogger(BroadcastTransaction.class.getSimpleName());

  private final TransactionProcessor transactionProcessor;
  private final ParameterService parameterService;
  private final TransactionService transactionService;

  public BroadcastTransaction(TransactionProcessor transactionProcessor, ParameterService parameterService, TransactionService transactionService) {
    super(new APITag[]{APITag.TRANSACTIONS}, TRANSACTION_BYTES_PARAMETER, TRANSACTION_JSON_PARAMETER);

    this.transactionProcessor = transactionProcessor;
    this.parameterService = parameterService;
    this.transactionService = transactionService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    String transactionBytes = Convert.emptyToNull(req.getParameter(TRANSACTION_BYTES_PARAMETER));
    String transactionJSON = Convert.emptyToNull(req.getParameter(TRANSACTION_JSON_PARAMETER));
    Transaction transaction = parameterService.parseTransaction(transactionBytes, transactionJSON);
    JSONObject response = new JSONObject();
    try {
      transactionService.validate(transaction);
      transactionProcessor.broadcast(transaction);
      response.put(TRANSACTION_RESPONSE, transaction.getStringId());
      response.put(FULL_HASH_RESPONSE, transaction.getFullHash());
    } catch (BurstException.ValidationException | RuntimeException e) {
      logger.log(Level.INFO, e.getMessage(), e);
      response.put(ERROR_CODE_RESPONSE, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Incorrect transaction: " + e.toString());
      response.put(ERROR_RESPONSE, e.getMessage());
    }
    return response;

  }

  @Override
  boolean requirePost() {
    return true;
  }

}
