package brs.fluxcapacitor;

import brs.common.Props;
import brs.services.PropertyService;
import java.util.HashMap;
import java.util.Map;

public class HistorianImpl {

  private final Map<HistoricalMoments, Integer> startingHeights;

  public HistorianImpl(PropertyService propertyService) {
    startingHeights = loadStartingHeights(propertyService);
  }

  private Map<HistoricalMoments, Integer> loadStartingHeights(PropertyService propertyService) {
    final Map<HistoricalMoments, Integer> startingHeightsOverview = new HashMap<>();

    final boolean isTestNet = propertyService.getBoolean(Props.DEV_TESTNET);

    for(HistoricalMoments hm:HistoricalMoments.values()) {
      if(! isTestNet) {
        startingHeightsOverview.put(hm, hm.momentProductionNet);
      } else {
        int overrideSettingValue = propertyService.getInt(hm.overridingProperty, -1);

        if(overrideSettingValue > -1) {
          startingHeightsOverview.put(hm, overrideSettingValue);
        } else {
          startingHeightsOverview.put(hm, hm.momentTestNet);
        }
      }
    }

    return startingHeightsOverview;
  }

  public boolean hasHappened(HistoricalMoments moment, int currentBlockHeight) {
    return currentBlockHeight >= startingHeights.get(moment);
  }

}
