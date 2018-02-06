package brs.services;

import brs.common.FeatureToggle;

public interface FeatureService {

  boolean isActive(FeatureToggle featureToggle);

}
