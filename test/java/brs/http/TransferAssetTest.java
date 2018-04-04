package brs.http;

import static brs.TransactionType.ColoredCoins.ASSET_TRANSFER;
import static brs.http.JSONResponses.NOT_ENOUGH_ASSETS;
import static brs.http.common.Parameters.ASSET_PARAMETER;
import static brs.http.common.Parameters.QUANTITY_QNT_PARAMETER;
import static brs.http.common.Parameters.RECIPIENT_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Asset;
import brs.Attachment;
import brs.Blockchain;
import brs.BurstException;
import brs.TransactionProcessor;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.services.AccountService;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;

public class TransferAssetTest extends AbstractTransactionTest {

  private TransferAsset t;

  private ParameterService parameterServiceMock;
  private Blockchain blockchainMock;
  private TransactionProcessor transactionProcessorMock;
  private APITransactionManager apiTransactionManagerMock;
  private AccountService accountServiceMock;

  @Before
  public void setUp() {
    parameterServiceMock = mock(ParameterService.class);
    blockchainMock = mock(Blockchain.class);
    apiTransactionManagerMock = mock(APITransactionManager.class);
    transactionProcessorMock = mock(TransactionProcessor.class);
    accountServiceMock = mock(AccountService.class);

    t = new TransferAsset(parameterServiceMock, blockchainMock, apiTransactionManagerMock, accountServiceMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final long recipientParameter = 34L;
    final long assetIdParameter = 456L;
    final long quantityQNTParameter = 56L;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(RECIPIENT_PARAMETER, recipientParameter),
        new MockParam(ASSET_PARAMETER, assetIdParameter),
        new MockParam(QUANTITY_QNT_PARAMETER, quantityQNTParameter)
    );

    Asset mockAsset = mock(Asset.class);

    when(parameterServiceMock.getAsset(eq(req))).thenReturn(mockAsset);
    when(mockAsset.getId()).thenReturn(assetIdParameter);

    final Account mockSenderAccount = mock(Account.class);
    when(accountServiceMock.getUnconfirmedAssetBalanceQNT(eq(mockSenderAccount), eq(assetIdParameter))).thenReturn(500L);

    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(mockSenderAccount);

    final Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer) attachmentCreatedTransaction(() -> t.processRequest(req), apiTransactionManagerMock);
    assertNotNull(attachment);

    assertEquals(ASSET_TRANSFER, attachment.getTransactionType());
    assertEquals(assetIdParameter, attachment.getAssetId());
    assertEquals(quantityQNTParameter, attachment.getQuantityQNT());
  }

  @Test
  public void processRequest_assetBalanceLowerThanQuantityNQTParameter() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(RECIPIENT_PARAMETER, "123"),
        new MockParam(ASSET_PARAMETER, "456"),
        new MockParam(QUANTITY_QNT_PARAMETER, "5")
    );

    Asset mockAsset = mock(Asset.class);

    when(parameterServiceMock.getAsset(eq(req))).thenReturn(mockAsset);
    when(mockAsset.getId()).thenReturn(456l);

    final Account mockSenderAccount = mock(Account.class);
    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(mockSenderAccount);

    when(accountServiceMock.getUnconfirmedAssetBalanceQNT(eq(mockSenderAccount), anyLong())).thenReturn(2L);

    assertEquals(NOT_ENOUGH_ASSETS, t.processRequest(req));
  }
}
