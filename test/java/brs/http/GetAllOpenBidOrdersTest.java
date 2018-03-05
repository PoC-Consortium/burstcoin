package brs.http;

import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.ASSET_RESPONSE;
import static brs.http.common.ResultFields.HEIGHT_RESPONSE;
import static brs.http.common.ResultFields.OPEN_ORDERS_RESPONSE;
import static brs.http.common.ResultFields.ORDER_RESPONSE;
import static brs.http.common.ResultFields.PRICE_NQT_RESPONSE;
import static brs.http.common.ResultFields.QUANTITY_NQT_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Order.Ask;
import brs.Order.Bid;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.db.BurstIterator;
import brs.services.OrderService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetAllOpenBidOrdersTest extends AbstractUnitTest {

  private GetAllOpenBidOrders t;

  private OrderService mockOrderService;

  @Before
  public void setUp() {
    mockOrderService = mock(OrderService.class);

    t = new GetAllOpenBidOrders(mockOrderService);
  }

  @Test
  public void processRequest() {
    final Bid mockBidOrder = mock(Bid.class);
    when(mockBidOrder.getId()).thenReturn(1L);
    when(mockBidOrder.getAssetId()).thenReturn(2L);
    when(mockBidOrder.getQuantityQNT()).thenReturn(3L);
    when(mockBidOrder.getPriceNQT()).thenReturn(4L);
    when(mockBidOrder.getHeight()).thenReturn(5);

    final int firstIndex = 1;
    final int lastIndex = 2;

    final BurstIterator<Bid> mockIterator = mockBurstIterator(mockBidOrder);
    when(mockOrderService.getAllBidOrders(eq(firstIndex), eq(lastIndex)))
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
    assertEquals("" + mockBidOrder.getId(), openOrderResult.get(ORDER_RESPONSE));
    assertEquals("" + mockBidOrder.getAssetId(), openOrderResult.get(ASSET_RESPONSE));
    assertEquals("" + mockBidOrder.getQuantityQNT(), openOrderResult.get(QUANTITY_NQT_RESPONSE));
    assertEquals("" + mockBidOrder.getPriceNQT(), openOrderResult.get(PRICE_NQT_RESPONSE));
    assertEquals(mockBidOrder.getHeight(), openOrderResult.get(HEIGHT_RESPONSE));
  }
}
