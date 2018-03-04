package brs.blockchainlistener;

import brs.Account;
import brs.Block;
import brs.DigitalGoodsStore.Purchase;
import brs.db.BurstIterator;
import brs.services.AccountService;
import brs.services.DGSGoodsStoreService;
import brs.util.Convert;
import brs.util.Listener;

public class DevNullListener implements Listener<Block> {

  private final AccountService accountService;
  private final DGSGoodsStoreService goodsService;

  public DevNullListener(AccountService accountService, DGSGoodsStoreService goodsService) {
    this.accountService = accountService;
    this.goodsService = goodsService;
  }

  @Override
  public void notify(Block block) {
    try (BurstIterator<Purchase> purchases = goodsService.getExpiredPendingPurchases(block.getTimestamp())) {
      while (purchases.hasNext()) {
        Purchase purchase = purchases.next();
        Account buyer = accountService.getAccount(purchase.getBuyerId());
        accountService.addToUnconfirmedBalanceNQT(buyer, Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT()));
        goodsService.changeQuantity(purchase.getGoodsId(), purchase.getQuantity(), true);
        goodsService.setPending(purchase, false);
      }
    }
  }
}
