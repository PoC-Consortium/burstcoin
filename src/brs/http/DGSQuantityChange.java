package brs.http;

import static brs.http.JSONResponses.INCORRECT_DELTA_QUANTITY;
import static brs.http.JSONResponses.MISSING_DELTA_QUANTITY;
import static brs.http.JSONResponses.UNKNOWN_GOODS;
import static brs.http.common.Parameters.DELTA_QUANTITY_PARAMETER;
import static brs.http.common.Parameters.GOODS_PARAMETER;

import brs.Account;
import brs.Attachment;
import brs.Blockchain;
import brs.BurstException;
import brs.Constants;
import brs.DigitalGoodsStore;
import brs.services.ParameterService;
import brs.util.Convert;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;

public final class DGSQuantityChange extends CreateTransaction {

  private final ParameterService parameterService;
  private final Blockchain blockchain;

  DGSQuantityChange(ParameterService parameterService, Blockchain blockchain, APITransactionManager apiTransactionManager) {
    super(new APITag[]{APITag.DGS, APITag.CREATE_TRANSACTION}, apiTransactionManager, GOODS_PARAMETER, DELTA_QUANTITY_PARAMETER);

    this.parameterService = parameterService;
    this.blockchain = blockchain;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    Account account = parameterService.getSenderAccount(req);
    DigitalGoodsStore.Goods goods = parameterService.getGoods(req);
    if (goods.isDelisted() || goods.getSellerId() != account.getId()) {
      return UNKNOWN_GOODS;
    }

    int deltaQuantity;
    try {
      String deltaQuantityString = Convert.emptyToNull(req.getParameter(DELTA_QUANTITY_PARAMETER));
      if (deltaQuantityString == null) {
        return MISSING_DELTA_QUANTITY;
      }
      deltaQuantity = Integer.parseInt(deltaQuantityString);
      if (deltaQuantity > Constants.MAX_DGS_LISTING_QUANTITY || deltaQuantity < -Constants.MAX_DGS_LISTING_QUANTITY) {
        return INCORRECT_DELTA_QUANTITY;
      }
    } catch (NumberFormatException e) {
      return INCORRECT_DELTA_QUANTITY;
    }

    Attachment attachment = new Attachment.DigitalGoodsQuantityChange(goods.getId(), deltaQuantity, blockchain.getHeight());
    return createTransaction(req, account, attachment);

  }

}
