package brs.http;

import static brs.TransactionType.ColoredCoins.ASK_ORDER_CANCELLATION;
import static brs.http.JSONResponses.UNKNOWN_ORDER;
import static brs.http.common.Parameters.ORDER_PARAMETER;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

import brs.Account;
import brs.Attachment;
import brs.Blockchain;
import brs.BurstException;
import brs.Order.Ask;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.services.OrderService;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;

public class CancelAskOrderTest extends AbstractTransactionTest {

  private CancelAskOrder t;

  private ParameterService parameterServiceMock;
  private Blockchain blockchainMock;
  private OrderService orderServiceMock;
  private APITransactionManager apiTransactionManagerMock;

  @Before
  public void setUp() {
    parameterServiceMock = mock(ParameterService.class);
    blockchainMock = mock(Blockchain.class);
    orderServiceMock = mock(OrderService.class);
    apiTransactionManagerMock = mock(APITransactionManager.class);

    t = new CancelAskOrder(parameterServiceMock, blockchainMock, orderServiceMock, apiTransactionManagerMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final long orderId = 5;
    final long sellerId = 6;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ORDER_PARAMETER, orderId)
    );

    final Account sellerAccount = mock(Account.class);
    when(sellerAccount.getId()).thenReturn(sellerId);

    final Ask order = mock(Ask.class);
    when(order.getAccountId()).thenReturn(sellerId);

    when(orderServiceMock.getAskOrder(eq(orderId))).thenReturn(order);
    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(sellerAccount);

    final Attachment.ColoredCoinsAskOrderCancellation attachment = (brs.Attachment.ColoredCoinsAskOrderCancellation) attachmentCreatedTransaction(() -> t.processRequest(req),
        apiTransactionManagerMock);
    assertNotNull(attachment);

    assertEquals(ASK_ORDER_CANCELLATION, attachment.getTransactionType());
    assertEquals(orderId, attachment.getOrderId());
  }

  @Test
  public void processRequest_orderDataNotFound() throws BurstException {
    int orderId = 5;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ORDER_PARAMETER, orderId)
    );

    when(orderServiceMock.getAskOrder(eq(orderId))).thenReturn(null);

    assertEquals(UNKNOWN_ORDER, t.processRequest(req));
  }

  @Test
  public void processRequest_orderOtherAccount() throws BurstException {
    final long orderId = 5;
    final long accountId = 6;
    final long otherAccountId = 7;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ORDER_PARAMETER, orderId)
    );

    final Account sellerAccount = mock(Account.class);
    when(sellerAccount.getId()).thenReturn(accountId);

    final Ask order = mock(Ask.class);
    when(order.getAccountId()).thenReturn(otherAccountId);

    when(orderServiceMock.getAskOrder(eq(orderId))).thenReturn(order);
    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(sellerAccount);

    assertEquals(UNKNOWN_ORDER, t.processRequest(req));
  }
}
