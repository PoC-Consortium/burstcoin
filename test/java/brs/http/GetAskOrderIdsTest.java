package brs.http;

import static brs.http.common.Parameters.ASSET_PARAMETER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.ASK_ORDER_IDS_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Asset;
import brs.BurstException;
import brs.Order.Ask;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.db.BurstIterator;
import brs.services.OrderService;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetAskOrderIdsTest extends AbstractUnitTest {

  private ParameterService parameterServiceMock;
  private OrderService orderServiceMock;

  private GetAskOrderIds t;

  @Before
  public void setUp() {
    parameterServiceMock = mock(ParameterService.class);
    orderServiceMock = mock(OrderService.class);

    t = new GetAskOrderIds(parameterServiceMock, orderServiceMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final long assetIndex = 5;
    final int firstIndex = 1;
    final int lastIndex = 3;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
      new MockParam(ASSET_PARAMETER, assetIndex),
      new MockParam(FIRST_INDEX_PARAMETER, firstIndex),
      new MockParam(LAST_INDEX_PARAMETER, lastIndex)
    );

    final Asset asset = mock(Asset.class);
    when(asset.getId()).thenReturn(assetIndex);

    when(parameterServiceMock.getAsset(eq(req))).thenReturn(asset);

    final Ask askOrder1 = mock(Ask.class);
    when(askOrder1.getId()).thenReturn(5L);
    final Ask askOrder2 = mock(Ask.class);
    when(askOrder1.getId()).thenReturn(6L);

    final BurstIterator<Ask> askIterator = this.mockBurstIterator(askOrder1, askOrder2);

    when(orderServiceMock.getSortedAskOrders(eq(assetIndex), eq(firstIndex), eq(lastIndex))).thenReturn(askIterator);

    final JSONObject result = (JSONObject) t.processRequest(req);
    assertNotNull(result);

    final JSONArray ids = (JSONArray) result.get(ASK_ORDER_IDS_RESPONSE);
    assertNotNull(ids);

    assertEquals(2, ids.size());
  }
}
