package brs.blockchainlistener;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Block;
import brs.DigitalGoodsStore.Purchase;
import brs.common.AbstractUnitTest;
import brs.db.BurstIterator;
import brs.services.AccountService;
import brs.services.DGSGoodsStoreService;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;

public class DevNullListenerTest extends AbstractUnitTest {

  private AccountService accountServiceMock;
  private DGSGoodsStoreService dgsGoodsStoreServiceMock;

  private DevNullListener t;

  @Before
  public void setUp() {
    accountServiceMock = mock(AccountService.class);
    dgsGoodsStoreServiceMock = mock(DGSGoodsStoreService.class);

    t = new DevNullListener(accountServiceMock, dgsGoodsStoreServiceMock);
  }

  @Test
  public void notify_processesExpiredPurchases() {
    int blockTimestamp = 123;
    final Block block = mock(Block.class);
    when(block.getTimestamp()).thenReturn(blockTimestamp);

    long purchaseBuyerId = 34;
    final Account purchaseBuyer = mock(Account.class);
    when(purchaseBuyer.getId()).thenReturn(purchaseBuyerId);
    when(accountServiceMock.getAccount(eq(purchaseBuyer.getId()))).thenReturn(purchaseBuyer);

    final Purchase expiredPurchase = mock(Purchase.class);
    when(expiredPurchase.getQuantity()).thenReturn(5);
    when(expiredPurchase.getPriceNQT()).thenReturn(3000L);
    when(expiredPurchase.getBuyerId()).thenReturn(purchaseBuyerId);

    final BurstIterator<Purchase> mockIterator = mockBurstIterator(expiredPurchase);
    when(dgsGoodsStoreServiceMock.getExpiredPendingPurchases(eq(blockTimestamp))).thenReturn(mockIterator);

    t.notify(block);

    verify(purchaseBuyer).addToUnconfirmedBalanceNQT(eq(15000L));

    verify(dgsGoodsStoreServiceMock).setPending(eq(expiredPurchase), eq(false));
  }
}