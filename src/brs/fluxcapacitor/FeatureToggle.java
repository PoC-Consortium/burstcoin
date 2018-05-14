package brs.fluxcapacitor;

import brs.fluxcapacitor.FluxHistory.Element;

public enum FeatureToggle {

  DIGITAL_GOODS_STORE(new FluxHistory<>(false, new Element<>(HistoricalMoments.DIGITAL_GOODS_STORE_BLOCK, true))),
  AUTOMATED_TRANSACTION_BLOCK(new FluxHistory<>(false, new Element<>(HistoricalMoments.AUTOMATED_TRANSACTION_BLOCK, true))),
  AT_FIX_BLOCK_2(new FluxHistory<>(false, new Element<>(HistoricalMoments.AT_FIX_BLOCK_2, true))),
  AT_FIX_BLOCK_3(new FluxHistory<>(false, new Element<>(HistoricalMoments.AT_FIX_BLOCK_3, true))),
  AT_FIX_BLOCK_4(new FluxHistory<>(false, new Element<>(HistoricalMoments.AT_FIX_BLOCK_4, true))),
  POC2(new FluxHistory<>(false, new Element<>(HistoricalMoments.POC2, true))),
  PRE_DYMAXION(new FluxHistory<>(false, new Element<>(HistoricalMoments.PRE_DYMAXION, true))),
  DYMAXION(new FluxHistory<>(false, new Element<>(HistoricalMoments.DYMAXION, true)));

  private FluxHistory<Boolean> flux;

  FeatureToggle(FluxHistory<Boolean> flux) {
    this.flux = flux;
  }

  public FluxHistory<Boolean> getFlux() {
    return flux;
  }

}
