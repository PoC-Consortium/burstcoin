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

import brs.Account;
import brs.Asset;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.db.BurstIterator;
import brs.services.AssetAccountService;
import brs.services.AssetService;
import brs.services.AssetTransferService;
import brs.services.ParameterService;
import brs.services.TradeService;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetAssetsByIssuerTest extends AbstractUnitTest {

  private GetAssetsByIssuer t;

  private ParameterService mockParameterService;
  private AssetService mockAssetService;
  private AssetAccountService mockAssetAccountService;
  private AssetTransferService mockAssetTransferService;
  private TradeService mockTradeService;

  @Before
  public void setUp() {
    mockParameterService = mock(ParameterService.class);
    mockAssetService = mock(AssetService.class);
    mockAssetAccountService = mock(AssetAccountService.class);
    mockAssetTransferService = mock(AssetTransferService.class);
    mockTradeService = mock(TradeService.class);

    t = new GetAssetsByIssuer(mockParameterService, mockAssetService, mockTradeService, mockAssetTransferService, mockAssetAccountService);
  }

  @Test
  public void processRequest() throws ParameterException {
    final int firstIndex = 1;
    final int lastIndex = 2;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(FIRST_INDEX_PARAMETER, firstIndex),
        new MockParam(LAST_INDEX_PARAMETER, lastIndex)
    );


    final Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn(1l);
    when(mockParameterService.getAccounts(eq(req))).thenReturn(Arrays.asList(mockAccount));

    final long mockAssetId = 1;

    final Asset mockAsset = mock(Asset.class);
    when(mockAsset.getId()).thenReturn(1L);
    when(mockAsset.getId()).thenReturn(mockAssetId);
    when(mockAsset.getName()).thenReturn("name");
    when(mockAsset.getDescription()).thenReturn("description");
    when(mockAsset.getDecimals()).thenReturn((byte) 1);
    when(mockAsset.getQuantityQNT()).thenReturn(2L);

    final BurstIterator<Asset> mockAssetIterator = mockBurstIterator(mockAsset);

    when(mockAssetService.getAssetsIssuedBy(eq(mockAccount.getId()), eq(firstIndex), eq(lastIndex))).thenReturn(mockAssetIterator);
    when(mockAssetAccountService.getAssetAccountsCount(eq(mockAssetId))).thenReturn(1);
    when(mockAssetTransferService.getTransferCount(eq(mockAssetId))).thenReturn(2);
    when(mockTradeService.getTradeCount(eq(mockAssetId))).thenReturn(3);

    final JSONObject result = (JSONObject) t.processRequest(req);
    assertNotNull(result);

    final JSONArray assetsForAccountsResult = (JSONArray) result.get(ASSETS_RESPONSE);
    assertNotNull(assetsForAccountsResult);
    assertEquals(1, assetsForAccountsResult.size());

    final JSONArray assetsForAccount1Result = (JSONArray) assetsForAccountsResult.get(0);
    assertNotNull(assetsForAccount1Result);
    assertEquals(1, assetsForAccount1Result.size());

    final JSONObject assetResult = (JSONObject) assetsForAccount1Result.get(0);
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
