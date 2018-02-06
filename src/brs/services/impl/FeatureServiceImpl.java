package brs.services.impl;

import brs.Alias;
import brs.Blockchain;
import brs.common.FeatureToggle;
import brs.services.AliasService;
import brs.services.FeatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureServiceImpl implements FeatureService {

  private static final Logger logger = LoggerFactory.getLogger(FeatureServiceImpl.class);

  private final Blockchain blockchain;

  private final AliasService aliasService;

  public FeatureServiceImpl(Blockchain blockchain, AliasService aliasService) {
    this.blockchain = blockchain;
    this.aliasService = aliasService;
  }

  @Override
  public boolean isActive(FeatureToggle featureToggle) {
    final int currentBlockHeight = blockchain.getHeight();

    final Integer minimumFeatureHeight = minimumFeatureHeight(featureToggle);
    final Integer maximumFeatureHeight = maximumFeatureHeight(featureToggle);

    return (minimumFeatureHeight == null || minimumFeatureHeight < currentBlockHeight) &&
           (maximumFeatureHeight == null || maximumFeatureHeight > currentBlockHeight);
  }

  private Integer minimumFeatureHeight(FeatureToggle featureToggle) {
    if(featureToggle.getStartHeight() != null) {
      return featureToggle.getStartHeight();
    } else {
      return aliasHeight(featureToggle.getStartHeightAlias());
    }
  }

  private Integer maximumFeatureHeight(FeatureToggle featureToggle) {
    if(featureToggle.getEndHeight() != null) {
      return featureToggle.getEndHeight();
    } else {
      return aliasHeight(featureToggle.getEndHeightAlias());
    }
  }

  private Integer aliasHeight(String aliasHeightName) {
    if(aliasHeightName != null) {
      try {
        final Alias heightAlias = aliasService.getAlias(aliasHeightName);
        return (heightAlias == null || heightAlias.getAliasURI() == null) ? null : Integer.parseInt(heightAlias.getAliasURI());
      } catch (NumberFormatException ex) {
        logger.debug("Unexpected value for feature height of alias " + aliasHeightName, ex);
      }
    }

    return null;
  }
}
