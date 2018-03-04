package brs.http;

import static brs.TransactionType.AdvancedPayment.ESCROW_SIGN;
import static brs.http.common.Parameters.DECISION_PARAMETER;
import static brs.http.common.Parameters.ESCROW_PARAMETER;
import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

import brs.Account;
import brs.Attachment;
import brs.Blockchain;
import brs.BurstException;
import brs.Escrow;
import brs.Escrow.DecisionType;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.services.EscrowService;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class EscrowSignTest extends AbstractTransactionTest {

  private ParameterService parameterServiceMock;
  private Blockchain blockchainMock;
  private EscrowService escrowServiceMock;
  private APITransactionManager apiTransactionManagerMock;

  private EscrowSign t;

  @Before
  public void setUp() {
    parameterServiceMock = mock(ParameterService.class);
    blockchainMock = mock(Blockchain.class);
    escrowServiceMock = mock(EscrowService.class);
    apiTransactionManagerMock = mock(APITransactionManager.class);

    t = new EscrowSign(parameterServiceMock, blockchainMock, escrowServiceMock, apiTransactionManagerMock);
  }

  @Test
  public void processRequest_positiveAsEscrowSender() throws BurstException {
    final long escrowId = 5;
    final long senderId = 6;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ESCROW_PARAMETER, escrowId),
        new MockParam(DECISION_PARAMETER, "release")
    );

    final Escrow escrow = mock(Escrow.class);
    when(escrow.getSenderId()).thenReturn(senderId);
    when(escrow.getRecipientId()).thenReturn(2L);

    final Account sender = mock(Account.class);
    when(sender.getId()).thenReturn(senderId);

    when(escrowServiceMock.getEscrowTransaction(eq(escrowId))).thenReturn(escrow);
    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(sender);

    final Attachment.AdvancedPaymentEscrowSign attachment = (brs.Attachment.AdvancedPaymentEscrowSign) attachmentCreatedTransaction(() -> t.processRequest(req),
        apiTransactionManagerMock);
    assertNotNull(attachment);

    assertEquals(ESCROW_SIGN, attachment.getTransactionType());
    assertEquals(DecisionType.RELEASE, attachment.getDecision());
  }

  @Test
  public void processRequest_positiveAsEscrowRecipient() throws BurstException {
    final long escrowId = 5;
    final long senderId = 6;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ESCROW_PARAMETER, escrowId),
        new MockParam(DECISION_PARAMETER, "refund")
    );

    final Escrow escrow = mock(Escrow.class);
    when(escrow.getSenderId()).thenReturn(1L);
    when(escrow.getRecipientId()).thenReturn(senderId);

    final Account sender = mock(Account.class);
    when(sender.getId()).thenReturn(senderId);

    when(escrowServiceMock.getEscrowTransaction(eq(escrowId))).thenReturn(escrow);
    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(sender);

    final Attachment.AdvancedPaymentEscrowSign attachment = (brs.Attachment.AdvancedPaymentEscrowSign) attachmentCreatedTransaction(() -> t.processRequest(req),
        apiTransactionManagerMock);
    assertNotNull(attachment);

    assertEquals(ESCROW_SIGN, attachment.getTransactionType());
    assertEquals(DecisionType.REFUND, attachment.getDecision());
  }

  @Test
  public void processRequest_positiveAsEscrowSigner() throws BurstException {
    final long escrowId = 5;
    final long senderId = 6;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ESCROW_PARAMETER, escrowId),
        new MockParam(DECISION_PARAMETER, "refund")
    );

    final Escrow escrow = mock(Escrow.class);
    when(escrow.getRecipientId()).thenReturn(1L);
    when(escrow.getSenderId()).thenReturn(2L);

    final Account sender = mock(Account.class);
    when(sender.getId()).thenReturn(senderId);

    when(escrowServiceMock.isIdSigner(eq(senderId), eq(escrow))).thenReturn(true);

    when(escrowServiceMock.getEscrowTransaction(eq(escrowId))).thenReturn(escrow);
    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(sender);

    final Attachment.AdvancedPaymentEscrowSign attachment = (brs.Attachment.AdvancedPaymentEscrowSign) attachmentCreatedTransaction(() -> t.processRequest(req),
        apiTransactionManagerMock);
    assertNotNull(attachment);

    assertEquals(ESCROW_SIGN, attachment.getTransactionType());
    assertEquals(DecisionType.REFUND, attachment.getDecision());
  }

  @Test
  public void processRequest_invalidEscrowId() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ESCROW_PARAMETER, "NotANumber")
    );

    final JSONObject result = (JSONObject) t.processRequest(req);

    assertEquals(3, result.get(ERROR_CODE_RESPONSE));
  }

  @Test
  public void processRequest_escrowNotFound() throws BurstException {
    final long escrowId = 5;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ESCROW_PARAMETER, escrowId)
    );

    when(escrowServiceMock.getEscrowTransaction(eq(escrowId))).thenReturn(null);

    final JSONObject result = (JSONObject) t.processRequest(req);

    assertEquals(5, result.get(ERROR_CODE_RESPONSE));
  }

  @Test
  public void processRequest_invalidDecisionType() throws BurstException {
    final long escrowId = 5;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ESCROW_PARAMETER, escrowId),
        new MockParam(DECISION_PARAMETER, "notADecisionValue")
    );

    final Escrow escrow = mock(Escrow.class);

    when(escrowServiceMock.getEscrowTransaction(eq(escrowId))).thenReturn(escrow);

    final JSONObject result = (JSONObject) t.processRequest(req);

    assertEquals(5, result.get(ERROR_CODE_RESPONSE));
  }

  @Test
  public void processRequest_invalidSender() throws BurstException {
    final long escrowId = 5;
    final long senderId = 6;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ESCROW_PARAMETER, escrowId),
        new MockParam(DECISION_PARAMETER, "refund")
    );

    final Escrow escrow = mock(Escrow.class);
    when(escrow.getSenderId()).thenReturn(1L);
    when(escrow.getRecipientId()).thenReturn(2L);

    when(escrowServiceMock.isIdSigner(eq(senderId), eq(escrow))).thenReturn(false);

    final Account sender = mock(Account.class);
    when(sender.getId()).thenReturn(senderId);

    when(escrowServiceMock.getEscrowTransaction(eq(escrowId))).thenReturn(escrow);
    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(sender);

    final JSONObject result = (JSONObject) t.processRequest(req);

    assertEquals(5, result.get(ERROR_CODE_RESPONSE));
  }

  @Test
  public void processRequest_senderCanOnlyRelease() throws BurstException {
    final long escrowId = 5;
    final long senderId = 6;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ESCROW_PARAMETER, escrowId),
        new MockParam(DECISION_PARAMETER, "refund")
    );

    final Escrow escrow = mock(Escrow.class);
    when(escrow.getSenderId()).thenReturn(senderId);

    final Account sender = mock(Account.class);
    when(sender.getId()).thenReturn(senderId);

    when(escrowServiceMock.getEscrowTransaction(eq(escrowId))).thenReturn(escrow);
    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(sender);

    final JSONObject result = (JSONObject) t.processRequest(req);

    assertEquals(4, result.get(ERROR_CODE_RESPONSE));
  }

  @Test
  public void processRequest_recipientCanOnlyRefund() throws BurstException {
    final long escrowId = 5;
    final long senderId = 6;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ESCROW_PARAMETER, escrowId),
        new MockParam(DECISION_PARAMETER, "release")
    );

    final Escrow escrow = mock(Escrow.class);
    when(escrow.getRecipientId()).thenReturn(senderId);

    final Account sender = mock(Account.class);
    when(sender.getId()).thenReturn(senderId);

    when(escrowServiceMock.getEscrowTransaction(eq(escrowId))).thenReturn(escrow);
    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(sender);

    final JSONObject result = (JSONObject) t.processRequest(req);

    assertEquals(4, result.get(ERROR_CODE_RESPONSE));
  }
}
