package brs.http;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Attachment;
import brs.BurstException;
import brs.common.AbstractUnitTest;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;
import org.mockito.ArgumentCaptor;

public abstract class AbstractTransactionTest extends AbstractUnitTest {

  @FunctionalInterface
  public interface TransactionCreationFunction<R> {
    R apply() throws BurstException;
  }

  protected Attachment attachmentCreatedTransaction(TransactionCreationFunction r, APITransactionManager apiTransactionManagerMock) throws BurstException {
    final ArgumentCaptor<Attachment> ac = ArgumentCaptor.forClass(Attachment.class);

    when(apiTransactionManagerMock.createTransaction(any(HttpServletRequest.class), nullable(Account.class), nullable(Long.class), anyLong(), ac.capture(), anyLong())).thenReturn(mock(JSONStreamAware.class));

    r.apply();

    return ac.getValue();
  }

}
