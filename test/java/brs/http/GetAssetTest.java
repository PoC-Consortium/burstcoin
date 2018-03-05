package brs.http;

import static brs.http.common.Parameters.ASSET_PARAMETER;
import static brs.http.common.ResultFields.ASSET_RESPONSE;
import static brs.http.common.ResultFields.DECIMALS_RESPONSE;
import static brs.http.common.ResultFields.DESCRIPTION_RESPONSE;
import static brs.http.common.ResultFields.NAME_RESPONSE;
import static brs.http.common.ResultFields.NUMBER_OF_ACCOUNTS_RESPONSE;
import static brs.http.common.ResultFields.NUMBER_OF_TRADES_RESPONSE;
import static brs.http.common.ResultFields.NUMBER_OF_TRANSFERS_RESPONSE;
import static brs.http.common.ResultFields.QUANTITY_NQT_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Asset;
import brs.BurstException;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.services.AssetAccountService;
import brs.services.AssetTransferService;
import brs.services.ParameterService;
import brs.services.TradeService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetAssetTest extends AbstractUnitTest {

  private ParameterService parameterServiceMock;
  private AssetAccountService assetAccountServiceMock;
  private AssetTransferService assetTransferServiceMock;
  private TradeService tradeServiceMock;

  private GetAsset t;

  @Before
  public void setUp() {
    parameterServiceMock = mock(ParameterService.class);
    assetAccountServiceMock = mock(AssetAccountService.class);
    assetTransferServiceMock = mock(AssetTransferService.class);
    tradeServiceMock = mock(TradeService.class);

    t = new GetAsset(parameterServiceMock, assetAccountServiceMock, assetTransferServiceMock, tradeServiceMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final long assetId = 4;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ASSET_PARAMETER, assetId)
    );

    final Asset asset = mock(Asset.class);
    when(asset.getId()).thenReturn(assetId);
    when(asset.getName()).thenReturn("assetName");
    when(asset.getDescription()).thenReturn("assetDescription");
    when(asset.getDecimals()).thenReturn(new Byte("3"));

    when(parameterServiceMock.getAsset(eq(req))).thenReturn(asset);

    int tradeCount = 1;
    int transferCount = 2;
    int assetAccountsCount = 3;

    when(tradeServiceMock.getTradeCount(eq(assetId))).thenReturn(tradeCount);
    when(assetTransferServiceMock.getTransferCount(eq(assetId))).thenReturn(transferCount);
    when(assetAccountServiceMock.getAssetAccountsCount(eq(assetId))).thenReturn(assetAccountsCount);

    final JSONObject result = (JSONObject) t.processRequest(req);

    assertNotNull(result);
    assertEquals(asset.getName(), result.get(NAME_RESPONSE));
    assertEquals(asset.getDescription(), result.get(DESCRIPTION_RESPONSE));
    assertEquals(asset.getDecimals(), result.get(DECIMALS_RESPONSE));
    assertEquals("" + asset.getQuantityQNT(), result.get(QUANTITY_NQT_RESPONSE));
    assertEquals("" + asset.getId(), result.get(ASSET_RESPONSE));
    assertEquals(tradeCount, result.get(NUMBER_OF_TRADES_RESPONSE));
    assertEquals(transferCount, result.get(NUMBER_OF_TRANSFERS_RESPONSE));
    assertEquals(assetAccountsCount, result.get(NUMBER_OF_ACCOUNTS_RESPONSE));
  }
}