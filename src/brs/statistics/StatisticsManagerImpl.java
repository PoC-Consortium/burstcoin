package brs.statistics;

import brs.services.TimeService;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatisticsManagerImpl {

  private final Logger logger = LoggerFactory.getLogger(StatisticsManagerImpl.class);

  private TimeService timeService;

  private int addedBlockCount;
  private int firstBlockAdded;

  private Map<String, CacheStatisticsOverview> cacheStatistics = new HashMap<>();

  public StatisticsManagerImpl(TimeService timeService) {
    this.timeService = timeService;
  }

  public void foundObjectInCache(String cacheName) {
    getCacheStatisticsOverview(cacheName).cacheHit();
  }

  public void didNotFindObjectInCache(String cacheName) {
    getCacheStatisticsOverview(cacheName).cacheMiss();
  }

  private CacheStatisticsOverview getCacheStatisticsOverview(String cacheName) {
    if(! this.cacheStatistics.containsKey(cacheName)) {
      this.cacheStatistics.put(cacheName, new CacheStatisticsOverview(cacheName));
    }

    return cacheStatistics.get(cacheName);
  }

  public void blockAdded() {
    if (addedBlockCount++ == 0 ) {
      firstBlockAdded = timeService.getEpochTime();
    } else if ( addedBlockCount % 500 == 0 ) {
      float blocksPerSecond = 500 / (float) (timeService.getEpochTime() - firstBlockAdded);

      final String handleText = "handling {} blocks/s"
          + cacheStatistics.values().stream().map(cacheInfo -> " " + cacheInfo.getCacheInfoAndReset()).collect(Collectors.joining()).toString();

      logger.info(handleText, String.format("%.2f", blocksPerSecond));

      addedBlockCount = 0;
    }
  }

  private class CacheStatisticsOverview {
    private String cacheName;

    private long cacheHits;
    private long cacheMisses;

    private long totalCacheHits;
    private long totalCacheMisses;

    public CacheStatisticsOverview(String cacheName) {
      this.cacheName = cacheName;
    }

    private String getCacheInfoAndReset() {
      final Float hitRatio = (cacheHits + cacheMisses) > 0 ? (float) cacheHits / (cacheHits + cacheMisses) : null;
      final Float totalHitRatio = (totalCacheHits + totalCacheMisses) > 0 ? (float) totalCacheHits / (totalCacheHits + totalCacheMisses) : null;

      cacheHits = 0;
      cacheMisses = 0;

      return String.format("%s cache hit ratio now/total:%.2f%%/%.2f%%", cacheName, hitRatio * 100, totalHitRatio * 100);
    }

    private void cacheHit() {
      cacheHits++;
      totalCacheHits++;
    }

    private void cacheMiss() {
      cacheMisses++;
      totalCacheMisses++;
    }
  }
}
