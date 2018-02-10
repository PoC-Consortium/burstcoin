package brs.common;

import static brs.common.AliasNames.DYMAXION_END_BLOCK;
import static brs.common.AliasNames.DYMAXION_START_BLOCK;

public enum FeatureToggle {

  FEATURE_ONE(null, 420000), FEATURE_TWO(420000, 430000), FEATURE_THREE(DYMAXION_START_BLOCK, DYMAXION_END_BLOCK);

  private final Integer startHeight;
  private final Integer endHeight;
  private final String startHeightAlias;
  private final String endHeightAlias;

  FeatureToggle(Integer startHeight, Integer endHeight) {
    this.startHeight = startHeight;
    this.endHeight = endHeight;
    this.startHeightAlias = null;
    this.endHeightAlias = null;
  }

  FeatureToggle(String startHeightAlias, String endHeightAlias) {
    this.startHeightAlias = startHeightAlias;
    this.endHeightAlias = endHeightAlias;
    this.startHeight = null;
    this.endHeight = null;
  }

  public Integer getStartHeight() {
    return startHeight;
  }

  public Integer getEndHeight() {
    return endHeight;
  }

  public String getStartHeightAlias() {
    return startHeightAlias;
  }

  public String getEndHeightAlias() {
    return endHeightAlias;
  }
}
