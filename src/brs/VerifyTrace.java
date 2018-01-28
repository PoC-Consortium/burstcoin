package brs;

import brs.util.Convert;
import brs.util.LoggerConfigurator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public final class VerifyTrace {

  private static final List<String> balanceHeaders = Arrays.asList("balance", "unconfirmed balance");
  private static final List<String> deltaHeaders = Arrays.asList("transaction amount", "transaction fee",
                                                                 "generation fee", "trade cost", "purchase cost", "discount", "refund");
  private static final List<String> assetQuantityHeaders = Arrays.asList("asset balance", "unconfirmed asset balance");
  private static final List<String> deltaAssetQuantityHeaders = Arrays.asList("asset quantity", "trade quantity");

  private static boolean isBalance(String header) {
    return balanceHeaders.contains(header);
  }

  private static boolean isDelta(String header) {
    return deltaHeaders.contains(header);
  }

  private static boolean isAssetQuantity(String header) {
    return assetQuantityHeaders.contains(header);
  }

  private static boolean isDeltaAssetQuantity(String header) {
    return deltaAssetQuantityHeaders.contains(header);
  }

  public static void main(String[] args) {
    String fileName = args.length == 1 ? args[0] : "burst-trace.csv";
    try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
      String line = reader.readLine();
      String[] headers = unquote(line.split(DebugTrace.SEPARATOR));

      Map<String,Map<String,Long>> totals = new HashMap<>();
      Map<String,Map<String,Map<String,Long>>> accountAssetTotals = new HashMap<>();
      Map<String,Long> issuedAssetQuantities = new HashMap<>();
      Map<String,Long> accountAssetQuantities = new HashMap<>();

      while ((line = reader.readLine()) != null) {
        String[] values = unquote(line.split(DebugTrace.SEPARATOR));
        Map<String,String> valueMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
          valueMap.put(headers[i], values[i]);
        }
        String accountId = valueMap.get("account");
        Map<String, Long> accountTotals = totals.computeIfAbsent(accountId, k -> new HashMap<>());
        Map<String, Map<String, Long>> accountAssetMap = accountAssetTotals.computeIfAbsent(accountId, k -> new HashMap<>());
        if ("asset issuance".equals(valueMap.get("event"))) {
          String assetId = valueMap.get("asset");
          issuedAssetQuantities.put(assetId, Long.parseLong(valueMap.get("asset quantity")));
        }
        for (Map.Entry<String,String> mapEntry : valueMap.entrySet()) {
          String header = mapEntry.getKey();
          String value = mapEntry.getValue();
          if (value == null || "".equals(value.trim())) {
            continue;
          }
          if (isBalance(header)) {
            accountTotals.put(header, Long.parseLong(value));
          } else if (isDelta(header)) {
            long previousValue = nullToZero(accountTotals.get(header));
            accountTotals.put(header, Convert.safeAdd(previousValue, Long.parseLong(value)));
          } else if (isAssetQuantity(header)) {
            String assetId = valueMap.get("asset");
            Map<String, Long> assetTotals = accountAssetMap.computeIfAbsent(assetId, k -> new HashMap<>());
            assetTotals.put(header, Long.parseLong(value));
          } else if (isDeltaAssetQuantity(header)) {
            String assetId = valueMap.get("asset");
            Map<String, Long> assetTotals = accountAssetMap.computeIfAbsent(assetId, k -> new HashMap<>());
            long previousValue = nullToZero(assetTotals.get(header));
            assetTotals.put(header, Convert.safeAdd(previousValue, Long.parseLong(value)));
          }
        }
      }

      Set<String> failed = new HashSet<>();
      for (Map.Entry<String,Map<String,Long>> mapEntry : totals.entrySet()) {
        String accountId = mapEntry.getKey();
        Map<String,Long> accountValues = mapEntry.getValue();
        System.out.println("account: " + accountId);
        balanceHeaders.forEach(balanceHeader -> System.out.println(balanceHeader + ": " + nullToZero(accountValues.get(balanceHeader))));
        System.out.println("totals:");
        long totalDelta = 0;
        for (String header : deltaHeaders) {
          long delta = nullToZero(accountValues.get(header));
          totalDelta = Convert.safeAdd(totalDelta, delta);
          System.out.println(header + ": " + delta);
        }
        System.out.println("total confirmed balance change: " + totalDelta);
        long balance = nullToZero(accountValues.get("balance"));
        if (balance != totalDelta) {
          System.out.println("ERROR: balance doesn't match total change!!!");
          failed.add(accountId);
        }
        Map<String,Map<String,Long>> accountAssetMap = accountAssetTotals.get(accountId);
        accountAssetMap.forEach((assetId, assetValues) -> {
          System.out.println("asset: " + assetId);
          assetValues.forEach((key, value) -> System.out.println(key + ": " + value));
          long totalAssetDelta = 0;
          for (String header : deltaAssetQuantityHeaders) {
            long delta = nullToZero(assetValues.get(header));
            totalAssetDelta = Convert.safeAdd(totalAssetDelta, delta);
          }
          System.out.println("total confirmed asset quantity change: " + totalAssetDelta);
          long assetBalance = assetValues.get("asset balance");
          if (assetBalance != totalAssetDelta) {
            System.out.println("ERROR: asset balance doesn't match total asset quantity change!!!");
            failed.add(accountId);
          }
          long previousAssetQuantity = nullToZero(accountAssetQuantities.get(assetId));
          accountAssetQuantities.put(assetId, Convert.safeAdd(previousAssetQuantity, assetBalance));
        });
        System.out.println();
      }
      Set<String> failedAssets = new HashSet<>();
      issuedAssetQuantities.forEach((assetId, value) -> {
        long issuedAssetQuantity = value;
        if (issuedAssetQuantity != nullToZero(accountAssetQuantities.get(assetId))) {
          System.out.println("ERROR: asset " + assetId + " balances don't match, issued: "
                  + issuedAssetQuantity
                  + ", total of account balances: " + accountAssetQuantities.get(assetId));
          failedAssets.add(assetId);
        }
      });
      if (failed.size() > 0) {
        System.out.println("ERROR: " + failed.size() + " accounts have incorrect balances");
        System.out.println(failed);
      } else {
        System.out.println("SUCCESS: all " + totals.size() + " account balances and asset balances match the transaction and trade totals!");
      }
      if (failedAssets.size() > 0) {
        System.out.println("ERROR: " + failedAssets.size() + " assets have incorrect balances");
        System.out.println(failedAssets);
      } else {
        System.out.println("SUCCESS: all " + issuedAssetQuantities.size() + " assets quantities are correct!");
      }

    } catch (IOException e) {
      System.out.println(e.toString());
      throw new RuntimeException(e);
    }
  }

  static {
    LoggerConfigurator.init();
  }

  private static final String beginQuote = "^" + DebugTrace.QUOTE;
  private static final String endQuote = DebugTrace.QUOTE + "$";

  private static String[] unquote(String[] values) {
    String[] result = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      result[i] = values[i].replaceFirst(beginQuote, "").replaceFirst(endQuote, "");
    }
    return result;
  }

  private static long nullToZero(Long l) {
    return l == null ? 0 : l;
  }

}
