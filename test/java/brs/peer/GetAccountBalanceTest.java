package brs.peer;

import static brs.common.TestConstants.TEST_ACCOUNT_ID;
import static brs.common.TestConstants.TEST_ACCOUNT_NUMERIC_ID_PARSED;
import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.ResultFields.BALANCE_NQT_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Account.class)
public class GetAccountBalanceTest {

  private GetAccountBalance t;

  @Before
  public void setUp() {
    t = GetAccountBalance.instance;
  }

  @Test
  public void processRequest() {
    PowerMockito.mockStatic(Account.class);

    final JSONObject req = new JSONObject();
    req.put(ACCOUNT_PARAMETER, TEST_ACCOUNT_ID);
    final Peer peer = mock(Peer.class);

    long mockBalanceNQT = 5;
    Account mockAccount = mock(Account.class);
    when(mockAccount.getBalanceNQT()).thenReturn(mockBalanceNQT);

    when(Account.getAccount(eq(TEST_ACCOUNT_NUMERIC_ID_PARSED))).thenReturn(mockAccount);

    final JSONObject result = (JSONObject) t.processRequest(req, peer);

    assertEquals("" + mockBalanceNQT, result.get(BALANCE_NQT_RESPONSE));
  }

  @Test
  public void processRequest_notExistingAccount() {
    PowerMockito.mockStatic(Account.class);

    final JSONObject req = new JSONObject();
    req.put(ACCOUNT_PARAMETER, TEST_ACCOUNT_ID);
    final Peer peer = mock(Peer.class);

    when(Account.getAccount(eq(TEST_ACCOUNT_NUMERIC_ID_PARSED))).thenReturn(null);

    final JSONObject result = (JSONObject) t.processRequest(req, peer);

    assertEquals("0", result.get(BALANCE_NQT_RESPONSE));
  }
}
