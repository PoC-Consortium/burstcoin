package brs.http;

import brs.Order.Ask;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.db.BurstIterator;
import brs.services.OrderService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetAllOpenAskOrdersTest extends AbstractUnitTest {

  private GetAllOpenAskOrders t;

  private OrderService mockOrderService;

  @Before
  public void setUp() {
    mockOrderService = mock(OrderService.class);

    t = new GetAllOpenAskOrders(mockOrderService);
  }

  @Test
  public void processRequest() {
    final Ask mockAskOrder = mock(Ask.class);
    when(mockAskOrder.getId()).thenReturn(1L);
    when(mockAskOrder.getAssetId()).thenReturn(2L);
    when(mockAskOrder.getQuantityQNT()).thenReturn(3L);
    when(mockAskOrder.getPriceNQT()).thenReturn(4L);
    when(mockAskOrder.getHeight()).thenReturn(5);

    final int firstIndex = 1;
    final int lastIndex = 2;

    final BurstIterator<Ask> mockIterator = mockBurstIterator(mockAskOrder);
    when(mockOrderService.getAllAskOrders(eq(firstIndex), eq(lastIndex)))
        .thenReturn(mockIterator);

    final JSONObject result = (JSONObject) t.processRequest(QuickMocker.httpServletRequest(
        new MockParam(FIRST_INDEX_PARAMETER, "" + firstIndex),
        new MockParam(LAST_INDEX_PARAMETER, "" + lastIndex)
    ));

    assertNotNull(result);
    final JSONArray openOrdersResult = (JSONArray) result.get(OPEN_ORDERS_RESPONSE);

    assertNotNull(openOrdersResult);
    assertEquals(1, openOrdersResult.size());

    final JSONObject openOrderResult = (JSONObject) openOrdersResult.get(0);
    assertEquals("" + mockAskOrder.getId(), openOrderResult.get(ORDER_RESPONSE));
    assertEquals("" + mockAskOrder.getAssetId(), openOrderResult.get(ASSET_RESPONSE));
    assertEquals("" + mockAskOrder.getQuantityQNT(), openOrderResult.get(QUANTITY_QNT_RESPONSE));
    assertEquals("" + mockAskOrder.getPriceNQT(), openOrderResult.get(PRICE_NQT_RESPONSE));
    assertEquals(mockAskOrder.getHeight(), openOrderResult.get(HEIGHT_RESPONSE));
  }
}
