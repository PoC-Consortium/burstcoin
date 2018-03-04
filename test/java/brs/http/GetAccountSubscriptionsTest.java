package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.SUBSCRIPTIONS_RESPONSE;
import static brs.http.common.ResultFields.AMOUNT_NQT_RESPONSE;
import static brs.http.common.ResultFields.FREQUENCY_RESPONSE;
import static brs.http.common.ResultFields.ID_RESPONSE;
import static brs.http.common.ResultFields.TIME_NEXT_RESPONSE;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.BurstException;
import brs.Subscription;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.db.BurstIterator;
import brs.services.ParameterService;
import brs.services.SubscriptionService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetAccountSubscriptionsTest extends AbstractUnitTest {

  private ParameterService parameterServiceMock;
  private SubscriptionService subscriptionServiceMock;

  private GetAccountSubscriptions t;

  @Before
  public void setUp() {
    parameterServiceMock = mock(ParameterService.class);
    subscriptionServiceMock = mock(SubscriptionService.class);

    t = new GetAccountSubscriptions(parameterServiceMock, subscriptionServiceMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final long userId = 123L;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
      new MockParam(ACCOUNT_PARAMETER, userId)
    );

    final Account account = mock(Account.class);
    when(account.getId()).thenReturn(userId);
    when(parameterServiceMock.getAccount(eq(req))).thenReturn(account);

    final Subscription subscription = mock(Subscription.class);
    when(subscription.getId()).thenReturn(1L);
    when(subscription.getAmountNQT()).thenReturn(2L);
    when(subscription.getFrequency()).thenReturn(3);
    when(subscription.getTimeNext()).thenReturn(4);

    final BurstIterator<Subscription> subscriptionIterator = this.mockBurstIterator(subscription);
    when(subscriptionServiceMock.getSubscriptionsByParticipant(eq(userId))).thenReturn(subscriptionIterator);

    final JSONObject result = (JSONObject) t.processRequest(req);
    assertNotNull(result);

    final JSONArray resultSubscriptions = (JSONArray) result.get(SUBSCRIPTIONS_RESPONSE);
    assertNotNull(resultSubscriptions);
    assertEquals(1, resultSubscriptions.size());

    final JSONObject resultSubscription = (JSONObject) resultSubscriptions.get(0);
    assertNotNull(resultSubscription);

    assertEquals("" + subscription.getId(), resultSubscription.get(ID_RESPONSE));
    assertEquals("" + subscription.getAmountNQT(), resultSubscription.get(AMOUNT_NQT_RESPONSE));
    assertEquals(subscription.getFrequency(), resultSubscription.get(FREQUENCY_RESPONSE));
    assertEquals(subscription.getTimeNext(), resultSubscription.get(TIME_NEXT_RESPONSE));
  }

}
