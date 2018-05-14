package brs.fluxcapacitor;

import brs.common.Props;

enum HistoricalMoments {

  DIGITAL_GOODS_STORE_BLOCK(11800, 1440, Props.DEV_DIGITAL_GOODS_STORE_BLOCK_HEIGHT),
  AUTOMATED_TRANSACTION_BLOCK(49200, 1440, Props.DEV_AUTOMATED_TRANSACTION_BLOCK_HEIGHT),
  AT_FIX_BLOCK_2(67000, 2880, Props.DEV_AT_FIX_BLOCK_2_BLOCK_HEIGHT),
  AT_FIX_BLOCK_3(92000, 4320, Props.DEV_AT_FIX_BLOCK_3_BLOCK_HEIGHT),
  AT_FIX_BLOCK_4(255000, 5760, Props.DEV_AT_FIX_BLOCK_4_BLOCK_HEIGHT),
  PRE_DYMAXION(500000, 88000, Props.DEV_PRE_DYMAXION_BLOCK_HEIGHT),
  POC2(500000, 88000, Props.DEV_POC2_BLOCK_HEIGHT),
  DYMAXION(Integer.MAX_VALUE, Integer.MAX_VALUE, Props.DEV_DYMAXION_BLOCK_HEIGHT); // TBD :-)

  final int momentProductionNet;
  final int momentTestNet;
  final String overridingProperty;

  HistoricalMoments(int momentProductionNet, int momentTestNet, String overridingProperty) {
    this.momentProductionNet = momentProductionNet;
    this.momentTestNet = momentTestNet;
    this.overridingProperty = overridingProperty;
  }
}
