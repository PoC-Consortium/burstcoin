package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.ESCROWS_RESPONSE;
import static brs.http.common.ResultFields.AMOUNT_NQT_RESPONSE;
import static brs.http.common.ResultFields.DEADLINE_ACTION_RESPONSE;
import static brs.http.common.ResultFields.DEADLINE_RESPONSE;
import static brs.http.common.ResultFields.DECISION_RESPONSE;
import static brs.http.common.ResultFields.ID_RESPONSE;
import static brs.http.common.ResultFields.ID_RS_RESPONSE;
import static brs.http.common.ResultFields.RECIPIENT_RESPONSE;
import static brs.http.common.ResultFields.RECIPIENT_RS_RESPONSE;
import static brs.http.common.ResultFields.REQUIRED_SIGNERS_RESPONSE;
import static brs.http.common.ResultFields.SENDER_RESPONSE;
import static brs.http.common.ResultFields.SENDER_RS_RESPONSE;
import static brs.http.common.ResultFields.SIGNERS_RESPONSE;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.BurstException;
import brs.Escrow;
import brs.Escrow.Decision;
import brs.Escrow.DecisionType;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.db.BurstIterator;
import brs.services.EscrowService;
import brs.services.ParameterService;
import brs.util.Convert;
import java.util.Arrays;
import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetAccountEscrowTransactionsTest extends AbstractUnitTest {

  private ParameterService parameterServiceMock;
  private EscrowService escrowServiceMock;

  private GetAccountEscrowTransactions t;

  @Before
  public void setUp() {
    parameterServiceMock = mock(ParameterService.class);
    escrowServiceMock = mock(EscrowService.class);

    t = new GetAccountEscrowTransactions(parameterServiceMock, escrowServiceMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final long accountId = 5;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
      new MockParam(ACCOUNT_PARAMETER, accountId)
    );

    final Account account = mock(Account.class);
    when(account.getId()).thenReturn(accountId);
    when(parameterServiceMock.getAccount(eq(req))).thenReturn(account);

    final Escrow escrow = mock(Escrow.class);
    when(escrow.getId()).thenReturn(1L);
    when(escrow.getSenderId()).thenReturn(2L);
    when(escrow.getRecipientId()).thenReturn(3L);
    when(escrow.getAmountNQT()).thenReturn(4L);
    when(escrow.getRequiredSigners()).thenReturn(5);
    when(escrow.getDeadlineAction()).thenReturn(DecisionType.UNDECIDED);

    final Decision decision = mock(Decision.class);
    when(decision.getAccountId()).thenReturn(3L);
    when(decision.getDecision()).thenReturn(DecisionType.UNDECIDED);

    final Decision skippedDecision = mock(Decision.class);
    when(skippedDecision.getAccountId()).thenReturn(5L);

    final Decision otherSkippedDecision = mock(Decision.class);
    when(otherSkippedDecision.getAccountId()).thenReturn(6L);

    when(escrow.getRecipientId()).thenReturn(5L);
    when(escrow.getSenderId()).thenReturn(6L);

    final BurstIterator<Decision> decisionsIterator = mockBurstIterator(decision, skippedDecision, otherSkippedDecision);
    when(escrow.getDecisions()).thenReturn(decisionsIterator);

    final Collection<Escrow> escrowCollection = Arrays.asList(escrow);
    when(escrowServiceMock.getEscrowTransactionsByParticipant(eq(accountId))).thenReturn(escrowCollection);

    final JSONObject resultOverview = (JSONObject) t.processRequest(req);
    assertNotNull(resultOverview);

    final JSONArray resultList = (JSONArray) resultOverview.get(ESCROWS_RESPONSE);
    assertNotNull(resultList);
    assertEquals(1, resultList.size());

    final JSONObject result = (JSONObject) resultList.get(0);
    assertNotNull(result);

    assertEquals("" + escrow.getId(), result.get(ID_RESPONSE));
    assertEquals("" + escrow.getSenderId(), result.get(SENDER_RESPONSE));
    assertEquals("BURST-2228-2222-BMNG-22222", result.get(SENDER_RS_RESPONSE));
    assertEquals("" + escrow.getRecipientId(), result.get(RECIPIENT_RESPONSE));
    assertEquals("BURST-2227-2222-ZAYB-22222", result.get(RECIPIENT_RS_RESPONSE));
    assertEquals("" + escrow.getAmountNQT(), result.get(AMOUNT_NQT_RESPONSE));
    assertEquals(escrow.getRequiredSigners(), result.get(REQUIRED_SIGNERS_RESPONSE));
    assertEquals(escrow.getDeadline(), result.get(DEADLINE_RESPONSE));
    assertEquals("undecided", result.get(DEADLINE_ACTION_RESPONSE));

    final JSONArray signersResult = (JSONArray) result.get(SIGNERS_RESPONSE);
    assertEquals(1, signersResult.size());

    final JSONObject signer = (JSONObject) signersResult.get(0);
    assertEquals("" + decision.getAccountId(), signer.get(ID_RESPONSE));
    assertEquals("BURST-2225-2222-QVC9-22222", signer.get(ID_RS_RESPONSE));
    assertEquals("undecided", signer.get(DECISION_RESPONSE));
  }
}
