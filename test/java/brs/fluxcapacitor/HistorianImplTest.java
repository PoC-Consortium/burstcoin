package brs.fluxcapacitor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.common.Props;
import brs.services.PropertyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class HistorianImplTest {

  private PropertyService propertyServiceMock;

  @BeforeEach
  void setUp() {
    propertyServiceMock = mock(PropertyService.class);
  }

  @DisplayName("We're on prodnet and the block has happened")
  @Test
  void hasHappenedAtProdNet() {
    when(propertyServiceMock.getBoolean(eq(Props.DEV_TESTNET))).thenReturn(false);

    final HistorianImpl t = new HistorianImpl(propertyServiceMock);

    assertTrue(t.hasHappened(HistoricalMoments.PRE_DYMAXION, 500000));
  }

  @DisplayName("We're on prodnet and the block has not happened")
  @Test
  void hasNotHappenedAtProdNet() {
    when(propertyServiceMock.getBoolean(eq(Props.DEV_TESTNET))).thenReturn(false);

    final HistorianImpl t = new HistorianImpl(propertyServiceMock);

    assertFalse(t.hasHappened(HistoricalMoments.PRE_DYMAXION, 499999));
  }

  @DisplayName("We're on testnet and the block has happened")
  @Test
  void hasHappenedAtTestNet() {
    when(propertyServiceMock.getBoolean(eq(Props.DEV_TESTNET))).thenReturn(true);

    final HistorianImpl t = new HistorianImpl(propertyServiceMock);

    assertTrue(t.hasHappened(HistoricalMoments.PRE_DYMAXION, 88000));
  }

  @DisplayName("We're doing a property override and the block has happened")
  @Test
  void hasHappenedPropertyOverride() {
    when(propertyServiceMock.getBoolean(eq(Props.DEV_TESTNET))).thenReturn(true);
    when(propertyServiceMock.getInt(eq(Props.DEV_PRE_DYMAXION_BLOCK_HEIGHT))).thenReturn(8);

    final HistorianImpl t = new HistorianImpl(propertyServiceMock);

    assertTrue(t.hasHappened(HistoricalMoments.PRE_DYMAXION, 8));
  }
}