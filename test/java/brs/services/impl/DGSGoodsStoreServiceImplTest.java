package brs.services.impl;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Blockchain;
import brs.DigitalGoodsStore;
import brs.DigitalGoodsStore.Goods;
import brs.DigitalGoodsStore.Purchase;
import brs.common.AbstractUnitTest;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.BurstKey.LongKeyFactory;
import brs.db.VersionedEntityTable;
import brs.db.store.DigitalGoodsStoreStore;
import brs.services.AccountService;
import org.junit.Before;
import org.junit.Test;

public class DGSGoodsStoreServiceImplTest extends AbstractUnitTest {

  private Blockchain blockchain;

  private AccountService mockAccountService;
  private DigitalGoodsStoreStore mockDigitalGoodsStoreStore;

  private VersionedEntityTable<DigitalGoodsStore.Goods> mockGoodsTable;
  private VersionedEntityTable<DigitalGoodsStore.Purchase> mockPurchaseTable;
  private LongKeyFactory<DigitalGoodsStore.Goods> mockGoodsDbKeyFactory;

  private DGSGoodsStoreServiceImpl t;

  @Before
  public void setUp() {
    blockchain = mock(Blockchain.class);
    mockGoodsTable = mock(VersionedEntityTable.class);
    mockPurchaseTable = mock(VersionedEntityTable.class);
    mockDigitalGoodsStoreStore = mock(DigitalGoodsStoreStore.class);
    mockGoodsDbKeyFactory = mock(LongKeyFactory.class);
    mockAccountService = mock(AccountService.class);

    when(mockDigitalGoodsStoreStore.getGoodsTable()).thenReturn(mockGoodsTable);
    when(mockDigitalGoodsStoreStore.getPurchaseTable()).thenReturn(mockPurchaseTable);
    when(mockDigitalGoodsStoreStore.getGoodsDbKeyFactory()).thenReturn(mockGoodsDbKeyFactory);

    t = new DGSGoodsStoreServiceImpl(blockchain, mockDigitalGoodsStoreStore, mockAccountService);
  }

  @Test
  public void getGoods() {
    final BurstKey mockKey = mock(BurstKey.class);
    final Goods mockGoods = mock(Goods.class);

    when(mockGoodsDbKeyFactory.newKey(eq(1l))).thenReturn(mockKey);
    when(mockGoodsTable.get(eq(mockKey))).thenReturn(mockGoods);

    assertEquals(mockGoods, t.getGoods(1L));
  }

  @Test
  public void getAllGoods() {
    final int from = 1;
    final int to = 2;

    final BurstIterator<DigitalGoodsStore.Goods> mockIterator = mockBurstIterator();
    when(mockGoodsTable.getAll(eq(from), eq(to))).thenReturn(mockIterator);

    assertEquals(mockIterator, t.getAllGoods(from, to));
  }

  @Test
  public void getGoodsInStock() {
    final int from = 1;
    final int to = 2;

    final BurstIterator<DigitalGoodsStore.Goods> mockIterator = mockBurstIterator();
    when(mockDigitalGoodsStoreStore.getGoodsInStock(eq(from), eq(to))).thenReturn(mockIterator);

    assertEquals(mockIterator, t.getGoodsInStock(from, to));
  }

  @Test
  public void getSellerGoods() {
    final long sellerId = 1L;
    final boolean inStockOnly = false;
    final int from = 1;
    final int to = 2;

    final BurstIterator<DigitalGoodsStore.Goods> mockIterator = mockBurstIterator();
    when(mockDigitalGoodsStoreStore.getSellerGoods(eq(sellerId), eq(inStockOnly), eq(from), eq(to))).thenReturn(mockIterator);

    assertEquals(mockIterator, t.getSellerGoods(sellerId, inStockOnly, from, to));
  }

  @Test
  public void getAllPurchases() {
    final int from = 1;
    final int to = 2;

    final BurstIterator<DigitalGoodsStore.Purchase> mockIterator = mockBurstIterator();
    when(mockPurchaseTable.getAll(eq(from), eq(to))).thenReturn(mockIterator);

    assertEquals(mockIterator, t.getAllPurchases(from, to));
  }

  @Test
  public void getSellerPurchases() {
    final long sellerId = 1L;
    final int from = 2;
    final int to = 3;

    final BurstIterator<DigitalGoodsStore.Purchase> mockIterator = mockBurstIterator();
    when(mockDigitalGoodsStoreStore.getSellerPurchases(eq(sellerId), eq(from), eq(to))).thenReturn(mockIterator);

    assertEquals(mockIterator, t.getSellerPurchases(sellerId, from, to));
  }

  @Test
  public void getBuyerPurchases() {
    final long buyerId = 1L;
    final int from = 2;
    final int to = 3;

    final BurstIterator<DigitalGoodsStore.Purchase> mockIterator = mockBurstIterator();
    when(mockDigitalGoodsStoreStore.getBuyerPurchases(eq(buyerId), eq(from), eq(to))).thenReturn(mockIterator);

    assertEquals(mockIterator, t.getBuyerPurchases(buyerId, from, to));
  }

  @Test
  public void getSellerBuyerPurchases() {
    final long sellerId = 1L;
    final long buyerId = 2L;
    final int from = 3;
    final int to = 4;

    final BurstIterator<DigitalGoodsStore.Purchase> mockIterator = mockBurstIterator();
    when(mockDigitalGoodsStoreStore.getSellerBuyerPurchases( eq(sellerId), eq(buyerId), eq(from), eq(to))).thenReturn(mockIterator);

    assertEquals(mockIterator, t.getSellerBuyerPurchases(sellerId, buyerId, from, to));
  }

  @Test
  public void getPendingSellerPurchases() {
    final long sellerId = 123L;
    final int from = 1;
    final int to = 2;

    BurstIterator<Purchase> mockPurchaseIterator = mockBurstIterator();
    when(mockDigitalGoodsStoreStore.getPendingSellerPurchases(eq(sellerId), eq(from), eq(to))).thenReturn(mockPurchaseIterator);

    assertEquals(mockPurchaseIterator, t.getPendingSellerPurchases(sellerId, from, to));
  }

}
