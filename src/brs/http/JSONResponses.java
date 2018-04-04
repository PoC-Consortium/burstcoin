package brs.http;

import brs.Constants;
import brs.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.Arrays;

import static brs.http.common.Parameters.*;
import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;

public final class JSONResponses {

  public static final JSONStreamAware INCORRECT_ALIAS = incorrect(ALIAS_PARAMETER);
  public static final JSONStreamAware INCORRECT_ALIAS_OWNER = incorrect(ALIAS_PARAMETER,"(invalid alias owner)");
  public static final JSONStreamAware INCORRECT_ALIAS_LENGTH = incorrect(ALIAS_PARAMETER, "(length must be in [1.." + Constants.MAX_ALIAS_LENGTH + "] range)");
  public static final JSONStreamAware INCORRECT_ALIAS_NAME = incorrect(ALIAS_PARAMETER, "(must contain only digits and latin letters)");
  public static final JSONStreamAware INCORRECT_ALIAS_NOTFORSALE = incorrect(ALIAS_PARAMETER, "(alias is not for sale at the moment)");
  public static final JSONStreamAware INCORRECT_URI_LENGTH = incorrect(URI_PARAMETER, "(length must be not longer than " + Constants.MAX_ALIAS_URI_LENGTH + " characters)");
  public static final JSONStreamAware MISSING_SECRET_PHRASE = missing(SECRET_PHRASE_PARAMETER);
  public static final JSONStreamAware INCORRECT_PUBLIC_KEY = incorrect(PUBLIC_KEY_PARAMETER);
  public static final JSONStreamAware MISSING_ALIAS_NAME = missing(ALIAS_NAME_PARAMETER);
  public static final JSONStreamAware MISSING_ALIAS_OR_ALIAS_NAME = missing(ALIAS_PARAMETER, "aliasName");
  public static final JSONStreamAware MISSING_FEE = missing(FEE_QT_PARAMETER);
  public static final JSONStreamAware MISSING_DEADLINE = missing(DEADLINE_PARAMETER);
  public static final JSONStreamAware INCORRECT_DEADLINE = incorrect(DEADLINE_PARAMETER);
  public static final JSONStreamAware INCORRECT_FEE = incorrect(FEE_PARAMETER);
  public static final JSONStreamAware MISSING_TRANSACTION_BYTES_OR_JSON = missing(TRANSACTION_BYTES_PARAMETER, TRANSACTION_JSON_PARAMETER);
  public static final JSONStreamAware MISSING_ORDER = missing(ORDER_PARAMETER);
  public static final JSONStreamAware INCORRECT_ORDER = incorrect(ORDER_PARAMETER);
  public static final JSONStreamAware UNKNOWN_ORDER = unknown(ORDER_PARAMETER);
  public static final JSONStreamAware MISSING_HALLMARK = missing(HALLMARK_PARAMETER);
  public static final JSONStreamAware INCORRECT_HALLMARK = incorrect(HALLMARK_PARAMETER);
  public static final JSONStreamAware MISSING_WEBSITE = missing(WEBSITE_PARAMETER);
  public static final JSONStreamAware INCORRECT_WEBSITE = incorrect(WEBSITE_PARAMETER);
  public static final JSONStreamAware MISSING_TOKEN = missing(TOKEN_PARAMETER);
  public static final JSONStreamAware MISSING_ACCOUNT = missing(ACCOUNT_PARAMETER);
  public static final JSONStreamAware INCORRECT_ACCOUNT = incorrect(ACCOUNT_PARAMETER);
  public static final JSONStreamAware INCORRECT_TIMESTAMP = incorrect(TIMESTAMP_PARAMETER);
  public static final JSONStreamAware UNKNOWN_ACCOUNT = unknown(ACCOUNT_PARAMETER);
  public static final JSONStreamAware UNKNOWN_ALIAS = unknown(ALIAS_PARAMETER);
  public static final JSONStreamAware MISSING_ASSET = missing(ASSET_PARAMETER);
  public static final JSONStreamAware UNKNOWN_ASSET = unknown(ASSET_PARAMETER);
  public static final JSONStreamAware INCORRECT_ASSET = incorrect(ASSET_PARAMETER);
  public static final JSONStreamAware UNKNOWN_BLOCK = unknown(BLOCK_PARAMETER);
  public static final JSONStreamAware INCORRECT_BLOCK = incorrect(BLOCK_PARAMETER);
  public static final JSONStreamAware INCORRECT_NUMBER_OF_CONFIRMATIONS = incorrect(NUMBER_OF_CONFIRMATIONS_PARAMETER);
  public static final JSONStreamAware MISSING_PEER = missing(PEER_PARAMETER);
  public static final JSONStreamAware UNKNOWN_PEER = unknown(PEER_PARAMETER);
  public static final JSONStreamAware MISSING_TRANSACTION = missing(TRANSACTION_PARAMETER);
  public static final JSONStreamAware UNKNOWN_TRANSACTION = unknown(TRANSACTION_PARAMETER);
  public static final JSONStreamAware INCORRECT_TRANSACTION = incorrect(TRANSACTION_PARAMETER);
  public static final JSONStreamAware INCORRECT_ASSET_DESCRIPTION = incorrect(DESCRIPTION_PARAMETER, "(length must not exceed " + Constants.MAX_ASSET_DESCRIPTION_LENGTH + " characters)");
  public static final JSONStreamAware INCORRECT_ASSET_NAME = incorrect(NAME_PARAMETER, "(must contain only digits and latin letters)");
  public static final JSONStreamAware INCORRECT_ASSET_NAME_LENGTH = incorrect(NAME_PARAMETER, "(length must be in [" + Constants.MIN_ASSET_NAME_LENGTH + ".." + Constants.MAX_ASSET_NAME_LENGTH + "] range)");
  public static final JSONStreamAware MISSING_NAME = missing(NAME_PARAMETER);
  public static final JSONStreamAware MISSING_QUANTITY = missing(QUANTITY_QNT_PARAMETER);
  public static final JSONStreamAware INCORRECT_QUANTITY = incorrect(QUANTITY_PARAMETER);
  public static final JSONStreamAware INCORRECT_ASSET_QUANTITY = incorrect(QUANTITY_PARAMETER, "(must be in [1.." + Constants.MAX_ASSET_QUANTITY_QNT + "] range)");
  public static final JSONStreamAware INCORRECT_DECIMALS = incorrect(DECIMALS_PARAMETER);
  public static final JSONStreamAware MISSING_HOST = missing(HOST_PARAMETER);
  public static final JSONStreamAware MISSING_DATE = missing(DATE_PARAMETER);
  public static final JSONStreamAware MISSING_WEIGHT = missing(WEIGHT_PARAMETER);
  public static final JSONStreamAware INCORRECT_HOST = incorrect(HOST_PARAMETER, "(the length exceeds 100 chars limit)");
  public static final JSONStreamAware INCORRECT_WEIGHT = incorrect(WEIGHT_PARAMETER);
  public static final JSONStreamAware INCORRECT_DATE = incorrect(DATE_PARAMETER);
  public static final JSONStreamAware MISSING_PRICE = missing(PRICE_NQT_PARAMETER);
  public static final JSONStreamAware INCORRECT_PRICE = incorrect(PRICE_PARAMETER);
  public static final JSONStreamAware INCORRECT_REFERENCED_TRANSACTION = incorrect(REFERENCED_TRANSACTION_FULL_HASH_PARAMETER);
  public static final JSONStreamAware MISSING_RECIPIENT = missing(RECIPIENT_PARAMETER);
  public static final JSONStreamAware INCORRECT_RECIPIENT = incorrect(RECIPIENT_PARAMETER);
  public static final JSONStreamAware INCORRECT_ARBITRARY_MESSAGE = incorrect(MESSAGE_PARAMETER);
  public static final JSONStreamAware MISSING_AMOUNT = missing(AMOUNT_NQT_PARAMETER);
  public static final JSONStreamAware INCORRECT_AMOUNT = incorrect(AMOUNT_PARAMETER);
  public static final JSONStreamAware INCORRECT_ACCOUNT_NAME_LENGTH = incorrect(NAME_PARAMETER, "(length must be less than " + Constants.MAX_ACCOUNT_NAME_LENGTH + " characters)");
  public static final JSONStreamAware INCORRECT_ACCOUNT_DESCRIPTION_LENGTH = incorrect(DESCRIPTION_PARAMETER, "(length must be less than " + Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH + " characters)");
  public static final JSONStreamAware MISSING_PERIOD = missing(PERIOD_PARAMETER);
  public static final JSONStreamAware INCORRECT_PERIOD = incorrect(PERIOD_PARAMETER, "(period must be at least 1440 blocks)");
  public static final JSONStreamAware MISSING_UNSIGNED_BYTES = missing(UNSIGNED_TRANSACTION_BYTES_PARAMETER);
  public static final JSONStreamAware MISSING_SIGNATURE_HASH = missing(SIGNATURE_HASH_PARAMETER);
  public static final JSONStreamAware INCORRECT_DGS_LISTING_NAME = incorrect(NAME_PARAMETER, "(length must be not longer than " + Constants.MAX_DGS_LISTING_NAME_LENGTH + " characters)");
  public static final JSONStreamAware INCORRECT_DGS_LISTING_DESCRIPTION = incorrect(DESCRIPTION_PARAMETER, "(length must be not longer than " + Constants.MAX_DGS_LISTING_DESCRIPTION_LENGTH + " characters)");
  public static final JSONStreamAware INCORRECT_DGS_LISTING_TAGS = incorrect(TAGS_PARAMETER, "(length must be not longer than " + Constants.MAX_DGS_LISTING_TAGS_LENGTH + " characters)");
  public static final JSONStreamAware MISSING_GOODS = missing(GOODS_PARAMETER);
  public static final JSONStreamAware INCORRECT_GOODS = incorrect(GOODS_PARAMETER);
  public static final JSONStreamAware UNKNOWN_GOODS = unknown(GOODS_PARAMETER);
  public static final JSONStreamAware INCORRECT_DELTA_QUANTITY = incorrect(DELTA_QUANTITY_PARAMETER);
  public static final JSONStreamAware MISSING_DELTA_QUANTITY = missing(DELTA_QUANTITY_PARAMETER);
  public static final JSONStreamAware MISSING_DELIVERY_DEADLINE_TIMESTAMP = missing(DELIVERY_DEADLINE_TIMESTAMP_PARAMETER);
  public static final JSONStreamAware INCORRECT_DELIVERY_DEADLINE_TIMESTAMP = incorrect(DELIVERY_DEADLINE_TIMESTAMP_PARAMETER);
  public static final JSONStreamAware INCORRECT_PURCHASE_QUANTITY = incorrect(QUANTITY_PARAMETER, "(quantity exceeds available goods quantity)");
  public static final JSONStreamAware INCORRECT_PURCHASE_PRICE = incorrect(PRICE_NQT_PARAMETER, "(purchase price doesn't match goods price)");
  public static final JSONStreamAware INCORRECT_PURCHASE = incorrect(PURCHASE_PARAMETER);
  public static final JSONStreamAware MISSING_PURCHASE = missing(PURCHASE_PARAMETER);
  public static final JSONStreamAware INCORRECT_DGS_GOODS = incorrect(GOODS_TO_ENCRYPT_PARAMETER);
  public static final JSONStreamAware INCORRECT_DGS_DISCOUNT = incorrect(DISCOUNT_NQT_PARAMETER);
  public static final JSONStreamAware INCORRECT_DGS_REFUND = incorrect(REFUND_NQT_PARAMETER);
  public static final JSONStreamAware MISSING_SELLER = missing(SELLER_PARAMETER);
  public static final JSONStreamAware INCORRECT_ENCRYPTED_MESSAGE = incorrect(ENCRYPTED_MESSAGE_DATA_PARAMETER);
  public static final JSONStreamAware INCORRECT_DGS_ENCRYPTED_GOODS = incorrect(GOODS_DATA_PARAMETER);
  public static final JSONStreamAware MISSING_SECRET_PHRASE_OR_PUBLIC_KEY = missing(SECRET_PHRASE_PARAMETER, PUBLIC_KEY_PARAMETER);
  public static final JSONStreamAware INCORRECT_HEIGHT = incorrect(HEIGHT_PARAMETER);
  public static final JSONStreamAware MISSING_HEIGHT = missing(HEIGHT_PARAMETER);
  public static final JSONStreamAware INCORRECT_PLAIN_MESSAGE = incorrect(MESSAGE_TO_ENCRYPT_PARAMETER);

  public static final JSONStreamAware INCORRECT_AUTOMATED_TRANSACTION_NAME_LENGTH = incorrect(DESCRIPTION_PARAMETER, "(length must not exceed " + Constants.MAX_AUTOMATED_TRANSACTION_NAME_LENGTH+ " characters)");
  public static final JSONStreamAware INCORRECT_AUTOMATED_TRANSACTION_NAME = incorrect(NAME_PARAMETER, "(must contain only digits and latin letters)");
  public static final JSONStreamAware INCORRECT_AUTOMATED_TRANSACTION_DESCRIPTION = incorrect(DESCRIPTION_PARAMETER, "(length must not exceed " + Constants.MAX_AUTOMATED_TRANSACTION_DESCRIPTION_LENGTH + " characters)");
  public static final JSONStreamAware MISSING_AT = missing(AT_PARAMETER);
  public static final JSONStreamAware UNKNOWN_AT = unknown(AT_PARAMETER);
  public static final JSONStreamAware INCORRECT_AT = incorrect(AT_PARAMETER);
  public static final JSONStreamAware INCORRECT_CREATION_BYTES = incorrect("incorrect creation bytes");
    
    
  public static final JSONStreamAware NOT_ENOUGH_FUNDS;
  static {
    JSONObject response = new JSONObject();
    response.put(ERROR_CODE_RESPONSE, 6);
    response.put(ERROR_DESCRIPTION_RESPONSE, "Not enough funds");
    NOT_ENOUGH_FUNDS = JSON.prepare(response);
  }

  public static final JSONStreamAware NOT_ENOUGH_ASSETS;
  static {
    JSONObject response = new JSONObject();
    response.put(ERROR_CODE_RESPONSE, 6);
    response.put(ERROR_DESCRIPTION_RESPONSE, "Not enough assets");
    NOT_ENOUGH_ASSETS = JSON.prepare(response);
  }

  public static final JSONStreamAware ERROR_NOT_ALLOWED;
  static {
    JSONObject response = new JSONObject();
    response.put(ERROR_CODE_RESPONSE, 7);
    response.put(ERROR_DESCRIPTION_RESPONSE, "Not allowed");
    ERROR_NOT_ALLOWED = JSON.prepare(response);
  }

  public static final JSONStreamAware ERROR_INCORRECT_REQUEST;
  static {
    JSONObject response  = new JSONObject();
    response.put(ERROR_CODE_RESPONSE, 1);
    response.put(ERROR_DESCRIPTION_RESPONSE, "Incorrect request");
    ERROR_INCORRECT_REQUEST = JSON.prepare(response);
  }

  public static final JSONStreamAware NOT_FORGING;
  static {
    JSONObject response = new JSONObject();
    response.put(ERROR_CODE_RESPONSE, 5);
    response.put(ERROR_DESCRIPTION_RESPONSE, "Account is not forging");
    NOT_FORGING = JSON.prepare(response);
  }

  public static final JSONStreamAware POST_REQUIRED;
  static {
    JSONObject response = new JSONObject();
    response.put(ERROR_CODE_RESPONSE, 1);
    response.put(ERROR_DESCRIPTION_RESPONSE, "This request is only accepted using POST!");
    POST_REQUIRED = JSON.prepare(response);
  }

  public static final JSONStreamAware FEATURE_NOT_AVAILABLE;
  static {
    JSONObject response = new JSONObject();
    response.put(ERROR_CODE_RESPONSE, 9);
    response.put(ERROR_DESCRIPTION_RESPONSE, "Feature not available");
    FEATURE_NOT_AVAILABLE = JSON.prepare(response);
  }

  public static final JSONStreamAware DECRYPTION_FAILED;
  static {
    JSONObject response = new JSONObject();
    response.put(ERROR_CODE_RESPONSE, 8);
    response.put(ERROR_DESCRIPTION_RESPONSE, "Decryption failed");
    DECRYPTION_FAILED = JSON.prepare(response);
  }

  public static final JSONStreamAware ALREADY_DELIVERED;
  static {
    JSONObject response = new JSONObject();
    response.put(ERROR_CODE_RESPONSE, 8);
    response.put(ERROR_DESCRIPTION_RESPONSE, "Purchase already delivered");
    ALREADY_DELIVERED = JSON.prepare(response);
  }

  public static final JSONStreamAware DUPLICATE_REFUND;
  static {
    JSONObject response = new JSONObject();
    response.put(ERROR_CODE_RESPONSE, 8);
    response.put(ERROR_DESCRIPTION_RESPONSE, "Refund already sent");
    DUPLICATE_REFUND = JSON.prepare(response);
  }

  public static final JSONStreamAware GOODS_NOT_DELIVERED;
  static {
    JSONObject response = new JSONObject();
    response.put(ERROR_CODE_RESPONSE, 8);
    response.put(ERROR_DESCRIPTION_RESPONSE, "Goods have not been delivered yet");
    GOODS_NOT_DELIVERED = JSON.prepare(response);
  }

  public static final JSONStreamAware NO_MESSAGE;
  static {
    JSONObject response = new JSONObject();
    response.put(ERROR_CODE_RESPONSE, 8);
    response.put(ERROR_DESCRIPTION_RESPONSE, "No attached message found");
    NO_MESSAGE = JSON.prepare(response);
  }
    
  public static final JSONStreamAware HEIGHT_NOT_AVAILABLE;
  static {
    JSONObject response = new JSONObject();
    response.put(ERROR_CODE_RESPONSE, 8);
    response.put(ERROR_DESCRIPTION_RESPONSE, "Requested height not available");
    HEIGHT_NOT_AVAILABLE = JSON.prepare(response);
  }

  private static JSONStreamAware missing(String... paramNames) {
    JSONObject response = new JSONObject();
    response.put(ERROR_CODE_RESPONSE, 3);
    if (paramNames.length == 1) {
      response.put(ERROR_DESCRIPTION_RESPONSE, "\"" + paramNames[0] + "\"" + " not specified");
    } else {
      response.put(ERROR_DESCRIPTION_RESPONSE, "At least one of " + Arrays.toString(paramNames) + " must be specified");
    }
    return JSON.prepare(response);
  }

  private static JSONStreamAware incorrect(String paramName) {
    return incorrect(paramName, null);
  }

  private static JSONStreamAware incorrect(String paramName, String details) {
    JSONObject response = new JSONObject();
    response.put(ERROR_CODE_RESPONSE, 4);
    response.put(ERROR_DESCRIPTION_RESPONSE, "Incorrect \"" + paramName + (details != null ? "\" " + details : "\""));
    return JSON.prepare(response);
  }

  private static JSONStreamAware unknown(String objectName) {
    JSONObject response = new JSONObject();
    response.put(ERROR_CODE_RESPONSE, 5);
    response.put(ERROR_DESCRIPTION_RESPONSE, "Unknown " + objectName);
    return JSON.prepare(response);
  }

  private JSONResponses() {} // never

}
