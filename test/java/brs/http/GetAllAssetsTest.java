package brs.http;

import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.ASSETS_RESPONSE;
import static brs.http.common.ResultFields.ASSET_RESPONSE;
import static brs.http.common.ResultFields.DECIMALS_RESPONSE;
import static brs.http.common.ResultFields.DESCRIPTION_RESPONSE;
import static brs.http.common.ResultFields.NAME_RESPONSE;
import static brs.http.common.ResultFields.NUMBER_OF_ACCOUNTS_RESPONSE;
import static brs.http.common.ResultFields.NUMBER_OF_TRADES_RESPONSE;
import static brs.http.common.ResultFields.NUMBER_OF_TRANSFERS_RESPONSE;
import static brs.http.common.ResultFields.QUANTITY_QNT_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Asset;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.db.BurstIterator;
import brs.services.AssetAccountService;
import brs.services.AssetService;
import brs.services.AssetTransferService;
import brs.services.TradeService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetAllAssetsTest extends AbstractUnitTest {

  private GetAllAssets t;

  private AssetService assetService;
  private AssetAccountService assetAccountService;
  private AssetTransferService assetTransferService;
  private TradeService tradeService;

  @Before
  public void setUp() {
    assetService = mock(AssetService.class);
    assetAccountService = mock(AssetAccountService.class);
    assetTransferService = mock(AssetTransferService.class);
    tradeService = mock(TradeService.class);

    t = new GetAllAssets(assetService, assetAccountService, assetTransferService, tradeService);
  }

  @Test
  public void processRequest() {
    final int firstIndex = 1;
    final int lastIndex = 2;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(FIRST_INDEX_PARAMETER, firstIndex),
        new MockParam(LAST_INDEX_PARAMETER, lastIndex)
    );

    final long mockAssetId = 1;

    final Asset mockAsset = mock(Asset.class);
    when(mockAsset.getId()).thenReturn(1L);
    when(mockAsset.getId()).thenReturn(mockAssetId);
    when(mockAsset.getName()).thenReturn("name");
    when(mockAsset.getDescription()).thenReturn("description");
    when(mockAsset.getDecimals()).thenReturn((byte) 1);
    when(mockAsset.getQuantityQNT()).thenReturn(2L);

    final BurstIterator<Asset> mockAssetIterator = mockBurstIterator(mockAsset);

    when(assetService.getAllAssets(eq(firstIndex), eq(lastIndex))).thenReturn(mockAssetIterator);
    when(assetAccountService.getAssetAccountsCount(eq(mockAssetId))).thenReturn(1);
    when(assetTransferService.getTransferCount(eq(mockAssetId))).thenReturn(2);
    when(tradeService.getTradeCount(eq(mockAssetId))).thenReturn(3);

    final JSONObject result = (JSONObject) t.processRequest(req);
    assertNotNull(result);

    final JSONArray assetsResult = (JSONArray) result.get(ASSETS_RESPONSE);
    assertNotNull(assetsResult);
    assertEquals(1, assetsResult.size());

    final JSONObject assetResult = (JSONObject) assetsResult.get(0);
    assertNotNull(assetResult);

    assertEquals(mockAsset.getName(), assetResult.get(NAME_RESPONSE));
    assertEquals(mockAsset.getDescription(), assetResult.get(DESCRIPTION_RESPONSE));
    assertEquals(mockAsset.getDecimals(), assetResult.get(DECIMALS_RESPONSE));
    assertEquals("" + mockAsset.getQuantityQNT(), assetResult.get(QUANTITY_QNT_RESPONSE));
    assertEquals("" + mockAsset.getId(), assetResult.get(ASSET_RESPONSE));
    assertEquals(1, assetResult.get(NUMBER_OF_ACCOUNTS_RESPONSE));
    assertEquals(2, assetResult.get(NUMBER_OF_TRANSFERS_RESPONSE));
    assertEquals(3, assetResult.get(NUMBER_OF_TRADES_RESPONSE));
  }

}
