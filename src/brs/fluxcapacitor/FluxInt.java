package brs.fluxcapacitor;

import brs.fluxcapacitor.FluxHistory.Element;

public enum FluxInt {

  MAX_NUMBER_TRANSACTIONS(new FluxHistory<>(255, new Element<>(HistoricalMoments.PRE_DYMAXION, 1020))),
  MAX_PAYLOAD_LENGTH(new FluxHistory<>(255 * 176, new Element<>(HistoricalMoments.PRE_DYMAXION, 1020 * 176)));

  private FluxHistory<Integer> flux;

  FluxInt(FluxHistory<Integer> flux) {
    this.flux = flux;
  }

  public FluxHistory<Integer> getFlux() {
    return flux;
  }

}
