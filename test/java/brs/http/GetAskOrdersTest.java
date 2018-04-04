package brs.http;

import static brs.http.common.Parameters.ASSET_PARAMETER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.ASK_ORDERS_RESPONSE;
import static brs.http.common.ResultFields.ASSET_RESPONSE;
import static brs.http.common.ResultFields.HEIGHT_RESPONSE;
import static brs.http.common.ResultFields.ORDER_RESPONSE;
import static brs.http.common.ResultFields.PRICE_NQT_RESPONSE;
import static brs.http.common.ResultFields.QUANTITY_QNT_RESPONSE;
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

public class GetAskOrdersTest extends AbstractUnitTest {

  private ParameterService parameterServiceMock;
  private OrderService orderServiceMock;

  private GetAskOrders t;

  @Before
  public void setUp() {
    parameterServiceMock = mock(ParameterService.class);
    orderServiceMock = mock(OrderService.class);

    t = new GetAskOrders(parameterServiceMock, orderServiceMock);
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
    when(askOrder1.getId()).thenReturn(3L);
    when(askOrder1.getAssetId()).thenReturn(assetIndex);
    when(askOrder1.getQuantityQNT()).thenReturn(56L);
    when(askOrder1.getPriceNQT()).thenReturn(45L);
    when(askOrder1.getHeight()).thenReturn(32);

    final Ask askOrder2 = mock(Ask.class);
    when(askOrder1.getId()).thenReturn(4L);

    final BurstIterator<Ask> askIterator = this.mockBurstIterator(askOrder1, askOrder2);

    when(orderServiceMock.getSortedAskOrders(eq(assetIndex), eq(firstIndex), eq(lastIndex))).thenReturn(askIterator);

    final JSONObject result = (JSONObject) t.processRequest(req);
    assertNotNull(result);

    final JSONArray orders = (JSONArray) result.get(ASK_ORDERS_RESPONSE);
    assertNotNull(orders);

    assertEquals(2, orders.size());

    final JSONObject askOrder1Result = (JSONObject) orders.get(0);

    assertEquals("" + askOrder1.getId(), askOrder1Result.get(ORDER_RESPONSE));
    assertEquals("" + askOrder1.getAssetId(), askOrder1Result.get(ASSET_RESPONSE));
    assertEquals("" + askOrder1.getQuantityQNT(), askOrder1Result.get(QUANTITY_QNT_RESPONSE));
    assertEquals("" + askOrder1.getPriceNQT(), askOrder1Result.get(PRICE_NQT_RESPONSE));
    assertEquals(askOrder1.getHeight(), askOrder1Result.get(HEIGHT_RESPONSE));
  }
}
