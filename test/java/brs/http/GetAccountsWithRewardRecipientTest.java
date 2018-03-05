package brs.http;

import static brs.http.common.Parameters.ACCOUNTS_RESPONSE;
import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Account.RewardRecipientAssignment;
import brs.BurstException;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.db.BurstIterator;
import brs.services.AccountService;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetAccountsWithRewardRecipientTest extends AbstractUnitTest {

  private ParameterService parameterService;
  private AccountService accountService;

  private GetAccountsWithRewardRecipient t;

  @Before
  public void setUp() {
    parameterService = mock(ParameterService.class);
    accountService = mock(AccountService.class);

    t = new GetAccountsWithRewardRecipient(parameterService, accountService);
  }

  @Test
  public void processRequest() throws BurstException {
    final long targetAccountId = 4L;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
      new MockParam(ACCOUNT_PARAMETER, targetAccountId)
    );

    final Account targetAccount = mock(Account.class);
    when(targetAccount.getId()).thenReturn(targetAccountId);

    when(parameterService.getAccount(eq(req))).thenReturn(targetAccount);

    final RewardRecipientAssignment assignment = mock(RewardRecipientAssignment.class);
    when(assignment.getAccountId()).thenReturn(targetAccountId);

    final BurstIterator assignmentIterator = mockBurstIterator(assignment);

    when(accountService.getAccountsWithRewardRecipient(eq(targetAccountId))).thenReturn(assignmentIterator);

    final JSONObject resultOverview = (JSONObject) t.processRequest(req);
    assertNotNull(resultOverview);

    final JSONArray resultList = (JSONArray) resultOverview.get(ACCOUNTS_RESPONSE);
    assertNotNull(resultList);
    assertEquals(2, resultList.size());
  }

  @Test
  public void processRequest_withRewardRecipientAssignmentKnown() throws BurstException {
    final long targetAccountId = 4L;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ACCOUNT_PARAMETER, targetAccountId)
    );

    final Account targetAccount = mock(Account.class);
    when(targetAccount.getId()).thenReturn(targetAccountId);

    when(parameterService.getAccount(eq(req))).thenReturn(targetAccount);

    final RewardRecipientAssignment assignment = mock(RewardRecipientAssignment.class);
    when(assignment.getAccountId()).thenReturn(targetAccountId);

    final BurstIterator assignmentIterator = mockBurstIterator(assignment);

    when(accountService.getAccountsWithRewardRecipient(eq(targetAccountId))).thenReturn(assignmentIterator);
    when(accountService.getRewardRecipientAssignment(eq(targetAccount))).thenReturn(assignment);

    final JSONObject resultOverview = (JSONObject) t.processRequest(req);
    assertNotNull(resultOverview);

    final JSONArray resultList = (JSONArray) resultOverview.get(ACCOUNTS_RESPONSE);
    assertNotNull(resultList);
    assertEquals(1, resultList.size());
  }
}
