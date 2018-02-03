package brs.user;

import brs.*;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static brs.Constants.*;
import static brs.http.common.Parameters.*;
import static brs.user.JSONResponses.NOTIFY_OF_ACCEPTED_TRANSACTION;

public final class SendMoney extends UserServlet.UserRequestHandler {

  static final String INCORRECT_TRANSACTION_RESPONSE = "notifyOfIncorrectTransaction";
  static final String INCORRECT_FIELD_MESSAGE = "One of the fields is filled incorrectly!";
  static final String WRONG_SECRET_MESSAGE = "Wrong secret phrase!";
  static final String AMOUNT_MIN_MESSAGE = "\"Amount\" must be greater than 0!";
  static final String INVALID_FEE_MESSAGE = "\"Fee\" must be at least 1 BURST!";
  static final String INVALID_DEADLINE_MESSAGE = "\"Deadline\" must be greater or equal to 1 minute and less than 24 hours!";
  static final String INSUFFICIENT_FUNDS_MESSAGE = "Not enough funds!";

  static final SendMoney instance = new SendMoney();

  private SendMoney() {}

  private JSONObject errorResponse(String message, String recipient, String amount, String fee, String deadline) {
    JSONObject response = new JSONObject();
    response.put(RESPONSE, INCORRECT_TRANSACTION_RESPONSE);
    response.put(MESSAGE_PARAMETER, message);
    response.put(RECIPIENT_PARAMETER, recipient);
    response.put(AMOUNT_BURST_PARAMETER, amount);
    response.put(FEE_BURST_PARAMETER, fee);
    response.put(DEADLINE_PARAMETER, deadline);

    return response;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req, User user) throws BurstException.ValidationException {
    if (user.getSecretPhrase() == null) {
      return null;
    }

    String recipientValue = req.getParameter(RECIPIENT_PARAMETER);
    String amountValue = req.getParameter(AMOUNT_BURST_PARAMETER);
    String feeValue = req.getParameter(FEE_BURST_PARAMETER);
    String deadlineValue = req.getParameter(DEADLINE_PARAMETER);
    String secretPhrase = req.getParameter(SECRET_PHRASE_PARAMETER);

    long recipient;
    long amountNQT;
    long feeNQT;
    short deadline;

    try {

      recipient = Convert.parseUnsignedLong(recipientValue);
      if (recipient == 0) throw new IllegalArgumentException("invalid recipient");
      amountNQT = Convert.parseBURST(amountValue.trim());
      feeNQT = Convert.parseBURST(feeValue.trim());
      deadline = (short)(Double.parseDouble(deadlineValue) * 60);

    } catch (RuntimeException e) {

      return errorResponse(INCORRECT_FIELD_MESSAGE, recipientValue, amountValue, feeValue, deadlineValue);

    }

    if (! user.getSecretPhrase().equals(secretPhrase)) {

      return errorResponse(WRONG_SECRET_MESSAGE, recipientValue, amountValue, feeValue, deadlineValue);

    } else if (amountNQT <= 0 || amountNQT > Constants.MAX_BALANCE_NQT) {

      return errorResponse(AMOUNT_MIN_MESSAGE, recipientValue, amountValue, feeValue, deadlineValue);

    } else if (feeNQT < Constants.ONE_BURST || feeNQT > Constants.MAX_BALANCE_NQT) {

      return errorResponse(INVALID_FEE_MESSAGE, recipientValue, amountValue, feeValue, deadlineValue);

    } else if (deadline < 1 || deadline > 1440) {

      return errorResponse(INVALID_DEADLINE_MESSAGE, recipientValue, amountValue, feeValue, deadlineValue);
    }

    Account account = Account.getAccount(user.getPublicKey());
    if (account == null || Convert.safeAdd(amountNQT, feeNQT) > account.getUnconfirmedBalanceNQT()) {

      return errorResponse(INSUFFICIENT_FUNDS_MESSAGE, recipientValue, amountValue, feeValue, deadlineValue);

    } else {

      final Transaction transaction = Burst.getTransactionProcessor().newTransactionBuilder(user.getPublicKey(),
                                                                                            amountNQT, feeNQT, deadline, Attachment.ORDINARY_PAYMENT).recipientId(recipient).build();
      transaction.validate();
      transaction.sign(user.getSecretPhrase());

      Burst.getTransactionProcessor().broadcast(transaction);

      return NOTIFY_OF_ACCEPTED_TRANSACTION;

    }
  }

  @Override
  boolean requirePost() {
    return true;
  }

}
