package brs.fluxcapacitor;

import brs.fluxcapacitor.FluxHistory.Element;

public abstract class TypeCapacitor<T> {

  private final HistorianImpl historian;

  public TypeCapacitor(HistorianImpl historian) {
    this.historian = historian;
  }

  public T getValueAtHeight(FluxHistory<T> flux, int height) {
    for (int i = flux.getHistory().size() - 1; i >= 0; i--) {
      final Element<T> historicalElement = flux.getHistory().get(i);

      if (historian.hasHappened(historicalElement.getMoment(), height)) {
        return historicalElement.getValue();
      }
    }
    return flux.getDefaultValue();
  }

}
