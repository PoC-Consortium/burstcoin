package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.ASSET_PARAMETER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.INCLUDE_ASSET_INFO_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.TRADES_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Asset;
import brs.BurstException;
import brs.Trade;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.db.BurstIterator;
import brs.services.AssetService;
import brs.services.ParameterService;
import brs.services.TradeService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetTradesTest extends AbstractUnitTest {

  private GetTrades t;

  private ParameterService mockParameterService;
  private AssetService mockAssetService;
  private TradeService mockTradeService;

  @Before
  public void setUp() {
    mockParameterService = mock(ParameterService.class);
    mockAssetService = mock(AssetService.class);
    mockTradeService = mock(TradeService.class);

    t = new GetTrades(mockParameterService, mockAssetService, mockTradeService);
  }

  @Test
  public void processRequest_withAssetId() throws BurstException {
    final long assetId = 123L;
    final int firstIndex = 0;
    final int lastIndex = 1;
    final boolean includeAssetInfo = true;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ASSET_PARAMETER, assetId),
        new MockParam(FIRST_INDEX_PARAMETER, firstIndex),
        new MockParam(LAST_INDEX_PARAMETER, lastIndex),
        new MockParam(INCLUDE_ASSET_INFO_PARAMETER, includeAssetInfo)
    );

    final Asset mockAsset = mock(Asset.class);
    when(mockAsset.getId()).thenReturn(assetId);

    final Trade mockTrade = mock(Trade.class);
    final BurstIterator<Trade> mockTradesIterator = mockBurstIterator(mockTrade);

    when(mockParameterService.getAsset(eq(req))).thenReturn(mockAsset);
    when(mockAssetService.getTrades(eq(assetId), eq(firstIndex), eq(lastIndex))).thenReturn(mockTradesIterator);

    final JSONObject result = (JSONObject) t.processRequest(req);
    assertNotNull(result);

    final JSONArray trades = (JSONArray) result.get(TRADES_RESPONSE);
    assertNotNull(trades);
    assertEquals(1, trades.size());

    final JSONObject tradeResult = (JSONObject) trades.get(0);
    assertNotNull(tradeResult);
  }

  @Test
  public void processRequest_withAccountId() throws BurstException {
    final long accountId = 321L;
    final int firstIndex = 0;
    final int lastIndex = 1;
    final boolean includeAssetInfo = true;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ACCOUNT_PARAMETER, accountId),
        new MockParam(FIRST_INDEX_PARAMETER, firstIndex),
        new MockParam(LAST_INDEX_PARAMETER, lastIndex),
        new MockParam(INCLUDE_ASSET_INFO_PARAMETER, includeAssetInfo)
    );

    final Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn(accountId);

    final Trade mockTrade = mock(Trade.class);
    final BurstIterator<Trade> mockTradesIterator = mockBurstIterator(mockTrade);

    when(mockParameterService.getAccount(eq(req))).thenReturn(mockAccount);
    when(mockTradeService.getAccountTrades(eq(accountId), eq(firstIndex), eq(lastIndex))).thenReturn(mockTradesIterator);

    final JSONObject result = (JSONObject) t.processRequest(req);
    assertNotNull(result);

    final JSONArray trades = (JSONArray) result.get(TRADES_RESPONSE);
    assertNotNull(trades);
    assertEquals(1, trades.size());

    final JSONObject tradeResult = (JSONObject) trades.get(0);
    assertNotNull(tradeResult);
  }

  @Test
  public void processRequest_withAssetIdAndAccountId() throws BurstException {
    final long assetId = 123L;
    final long accountId = 321L;
    final int firstIndex = 0;
    final int lastIndex = 1;
    final boolean includeAssetInfo = true;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ASSET_PARAMETER, assetId),
        new MockParam(ACCOUNT_PARAMETER, accountId),
        new MockParam(FIRST_INDEX_PARAMETER, firstIndex),
        new MockParam(LAST_INDEX_PARAMETER, lastIndex),
        new MockParam(INCLUDE_ASSET_INFO_PARAMETER, includeAssetInfo)
    );

    final Asset mockAsset = mock(Asset.class);
    when(mockAsset.getId()).thenReturn(assetId);

    final Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn(accountId);

    final Trade mockTrade = mock(Trade.class);
    final BurstIterator<Trade> mockTradesIterator = mockBurstIterator(mockTrade);

    when(mockParameterService.getAsset(eq(req))).thenReturn(mockAsset);
    when(mockParameterService.getAccount(eq(req))).thenReturn(mockAccount);
    when(mockTradeService.getAccountAssetTrades(eq(accountId), eq(assetId), eq(firstIndex), eq(lastIndex))).thenReturn(mockTradesIterator);

    final JSONObject result = (JSONObject) t.processRequest(req);
    assertNotNull(result);

    final JSONArray trades = (JSONArray) result.get(TRADES_RESPONSE);
    assertNotNull(trades);
    assertEquals(1, trades.size());

    final JSONObject tradeResult = (JSONObject) trades.get(0);
    assertNotNull(tradeResult);
  }

  @Test
  public void startDbTransaction() {
    assertTrue(t.startDbTransaction());
  }

}
