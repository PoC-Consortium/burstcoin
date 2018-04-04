package brs.http;

import static brs.http.common.ResultFields.ASSET_BALANCES_RESPONSE;
import static brs.http.common.ResultFields.ASSET_RESPONSE;
import static brs.http.common.ResultFields.BALANCE_QNT_RESPONSE;
import static brs.http.common.ResultFields.DESCRIPTION_RESPONSE;
import static brs.http.common.ResultFields.NAME_RESPONSE;
import static brs.http.common.ResultFields.PUBLIC_KEY_RESPONSE;
import static brs.http.common.ResultFields.UNCONFIRMED_ASSET_BALANCES_RESPONSE;
import static brs.http.common.ResultFields.UNCONFIRMED_BALANCE_QNT_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

import brs.Account;
import brs.Account.AccountAsset;
import brs.BurstException;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.db.BurstIterator;
import brs.services.AccountService;
import brs.services.ParameterService;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetAccountTest extends AbstractUnitTest {

  private GetAccount t;

  private ParameterService parameterServiceMock;
  private AccountService accountServiceMock;

  @Before
  public void setUp() {
    parameterServiceMock = mock(ParameterService.class);
    accountServiceMock = mock(AccountService.class);

    t = new GetAccount(parameterServiceMock, accountServiceMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final long mockAccountId = 123L;
    final String mockAccountName = "accountName";
    final String mockAccountDescription = "accountDescription";

    final long mockAssetId = 321L;
    final long balanceNQT = 23L;
    final long mockUnconfirmedQuantityNQT = 12L;

    final HttpServletRequest req = QuickMocker.httpServletRequest();

    final Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn(mockAccountId);
    when(mockAccount.getPublicKey()).thenReturn(new byte[]{(byte) 1});
    when(mockAccount.getName()).thenReturn(mockAccountName);
    when(mockAccount.getDescription()).thenReturn(mockAccountDescription);

    when(parameterServiceMock.getAccount(eq(req))).thenReturn(mockAccount);

    final AccountAsset mockAccountAsset = mock(AccountAsset.class);
    when(mockAccountAsset.getAssetId()).thenReturn(mockAssetId);
    when(mockAccountAsset.getUnconfirmedQuantityQNT()).thenReturn(mockUnconfirmedQuantityNQT);
    when(mockAccountAsset.getQuantityQNT()).thenReturn(balanceNQT);
    BurstIterator<AccountAsset> mockAssetOverview = mockBurstIterator(Arrays.asList(mockAccountAsset));
    when(accountServiceMock.getAssets(eq(mockAccountId), eq(0), eq(-1))).thenReturn(mockAssetOverview);

    final JSONObject response = (JSONObject) t.processRequest(req);
    assertEquals("01", response.get(PUBLIC_KEY_RESPONSE));
    assertEquals(mockAccountName, response.get(NAME_RESPONSE));
    assertEquals(mockAccountDescription, response.get(DESCRIPTION_RESPONSE));

    final JSONArray confirmedBalanceResponses = (JSONArray) response.get(ASSET_BALANCES_RESPONSE);
    assertNotNull(confirmedBalanceResponses);
    assertEquals(1, confirmedBalanceResponses.size());
    final JSONObject balanceResponse = (JSONObject) confirmedBalanceResponses.get(0);
    assertEquals("" + mockAssetId, balanceResponse.get(ASSET_RESPONSE));
    assertEquals("" + balanceNQT, balanceResponse.get(BALANCE_QNT_RESPONSE));

    final JSONArray unconfirmedBalanceResponses = (JSONArray) response.get(UNCONFIRMED_ASSET_BALANCES_RESPONSE);
    assertNotNull(unconfirmedBalanceResponses);
    assertEquals(1, unconfirmedBalanceResponses.size());
    final JSONObject unconfirmedBalanceResponse = (JSONObject) unconfirmedBalanceResponses.get(0);
    assertEquals("" + mockAssetId, unconfirmedBalanceResponse.get(ASSET_RESPONSE));
    assertEquals("" + mockUnconfirmedQuantityNQT, unconfirmedBalanceResponse.get(UNCONFIRMED_BALANCE_QNT_RESPONSE));
  }
}
