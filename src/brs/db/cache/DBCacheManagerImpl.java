package brs.db.cache;

import brs.Account;
import brs.db.sql.DbKey;
import brs.statistics.StatisticsManagerImpl;
import java.util.HashMap;
import java.util.Map;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

public class DBCacheManagerImpl {

  private final CacheManager cacheManager;

  private final StatisticsManagerImpl statisticsManager;

  private final boolean statisticsEnabled;

  private final HashMap<String, CacheConfiguration> caches = new HashMap<String, CacheConfiguration>();

  public DBCacheManagerImpl(StatisticsManagerImpl statisticsManager) {
    this.statisticsManager = statisticsManager;
    statisticsEnabled = true;

    caches.put("account", CacheConfigurationBuilder.newCacheConfigurationBuilder(DbKey.class, Account.class, ResourcePoolsBuilder.heap(100)).build());

    CacheManagerBuilder cacheBuilder = CacheManagerBuilder.newCacheManagerBuilder();
    for (Map.Entry<String, CacheConfiguration> cache : caches.entrySet()) {
      cacheBuilder = cacheBuilder.withCache(cache.getKey(), cache.getValue());
    }
    cacheManager = cacheBuilder.build(true);
  }

  public void close() {
    cacheManager.close();
  }

  private Cache getEHCache(String name) {
    CacheConfiguration cacheConfiguration = caches.get(name);
    return cacheManager.getCache(name, cacheConfiguration.getKeyType(), cacheConfiguration.getValueType());
  }

  public Cache getCache(String name) {
    Cache cache = getEHCache(name);
    return statisticsEnabled ? new StatisticsCache(cache, name, statisticsManager) : cache;
  }

  public void flushCache() {
    for (String cacheName : caches.keySet()) {
      Cache cache = getEHCache(cacheName);
      if ( cache != null )
        cache.clear();
    }
  }

}
