package brs.fluxcapacitor;

import static brs.fluxcapacitor.FeatureToggle.POC2;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Blockchain;
import brs.common.Props;
import brs.services.PropertyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class FluxCapacitorImplTest {

  private Blockchain blockchainMock;
  private PropertyService propertyServiceMock;

  private FluxCapacitorImpl t;

  @BeforeEach
  public void setUp() {
    blockchainMock = mock(Blockchain.class);
    propertyServiceMock = mock(PropertyService.class);
  }

  @DisplayName("Feature is active on ProdNet")
  @Test
  public void featureIsActiveOnProdNet() {
    when(propertyServiceMock.getBoolean(eq(Props.DEV_TESTNET))).thenReturn(false);
    when(blockchainMock.getHeight()).thenReturn(500000);

    t = new FluxCapacitorImpl(blockchainMock, propertyServiceMock);

    assertTrue(t.isActive(POC2));
  }

  @DisplayName("Feature is not active on ProdNet")
  @Test
  public void featureIsInactiveProdNet() {
    when(propertyServiceMock.getBoolean(eq(Props.DEV_TESTNET))).thenReturn(false);
    when(blockchainMock.getHeight()).thenReturn(499999);

    t = new FluxCapacitorImpl(blockchainMock, propertyServiceMock);

    assertFalse(t.isActive(POC2));
  }

  @DisplayName("Feature is active on TestNet")
  @Test
  public void featureIsActiveTestNet() {
    when(propertyServiceMock.getBoolean(eq(Props.DEV_TESTNET))).thenReturn(true);
    when(blockchainMock.getHeight()).thenReturn(88999);
    when(propertyServiceMock.getInt(anyString(), anyInt())).thenReturn(-1);

    t = new FluxCapacitorImpl(blockchainMock, propertyServiceMock);

    assertTrue(t.isActive(POC2));

    when(blockchainMock.getHeight()).thenReturn(30000);

    assertFalse(t.isActive(POC2));
  }

  @DisplayName("FluxInt gives its default value when no historical moments changed it yet")
  @Test
  public void fluxIntDefaultValue() {
    when(propertyServiceMock.getBoolean(eq(Props.DEV_TESTNET))).thenReturn(false);
    when(blockchainMock.getHeight()).thenReturn(88000);

    t = new FluxCapacitorImpl(blockchainMock, propertyServiceMock);

    assertEquals((Integer) 255, t.getInt(FluxInt.MAX_NUMBER_TRANSACTIONS));
  }

  @DisplayName("FluxInt gives a new value when a historical moment has passed")
  @Test
  public void fluxIntHistoricalValue() {
    when(propertyServiceMock.getBoolean(eq(Props.DEV_TESTNET))).thenReturn(false);
    when(blockchainMock.getHeight()).thenReturn(500000);

    t = new FluxCapacitorImpl(blockchainMock, propertyServiceMock);

    assertEquals((Integer) 1020, t.getInt(FluxInt.MAX_NUMBER_TRANSACTIONS));
  }

  @DisplayName("FluxInt on TestNet gives its default value when no historical moments changed it yet")
  @Test
  public void fluxIntTestNetDefaultValue() {
    when(propertyServiceMock.getBoolean(eq(Props.DEV_TESTNET))).thenReturn(true);
    when(propertyServiceMock.getInt(anyString(), anyInt())).thenReturn(-1);

    t = new FluxCapacitorImpl(blockchainMock, propertyServiceMock);

    when(blockchainMock.getHeight()).thenReturn(5);

    assertEquals((Integer) 255, t.getInt(FluxInt.MAX_NUMBER_TRANSACTIONS));
  }

  @DisplayName("FluxInt on TestNet gives a new value when a historical moment has passed")
  @Test
  public void fluxIntTestNetHistoricalValue() {
    when(propertyServiceMock.getBoolean(eq(Props.DEV_TESTNET))).thenReturn(true);

    t = new FluxCapacitorImpl(blockchainMock, propertyServiceMock);

    when(blockchainMock.getHeight()).thenReturn(88000);

    assertEquals((Integer) 1020, t.getInt(FluxInt.MAX_NUMBER_TRANSACTIONS));
  }

  @DisplayName("FluxInt on TestNet gives a different value because the historical moment configuration is different")
  @Test
  public void fluxIntTestNetHistoricalMomentChangedThroughProperty() {
    when(propertyServiceMock.getBoolean(eq(Props.DEV_TESTNET))).thenReturn(true);
    when(propertyServiceMock.getInt(eq(Props.DEV_PRE_DYMAXION_BLOCK_HEIGHT), anyInt())).thenReturn(12345);

    t = new FluxCapacitorImpl(blockchainMock, propertyServiceMock);

    assertEquals((Integer) 255, t.getInt(FluxInt.MAX_NUMBER_TRANSACTIONS, 12344));
    assertEquals((Integer) 1020, t.getInt(FluxInt.MAX_NUMBER_TRANSACTIONS, 12345));
  }

}
