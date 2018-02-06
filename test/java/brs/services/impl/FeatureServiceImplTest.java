package brs.services.impl;

import static brs.common.AliasNames.DYMAXION_END_BLOCK;
import static brs.common.AliasNames.DYMAXION_START_BLOCK;
import static brs.common.FeatureToggle.FEATURE_THREE;
import static brs.common.FeatureToggle.FEATURE_TWO;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Alias;
import brs.Blockchain;
import brs.services.AliasService;
import org.junit.Before;
import org.junit.Test;

public class FeatureServiceImplTest {

  private Blockchain blockchainMock;

  private AliasService aliasServiceMock;

  private FeatureServiceImpl t;

  @Before
  public void setUp() {
    blockchainMock = mock(Blockchain.class);
    aliasServiceMock = mock(AliasService.class);

    t = new FeatureServiceImpl(blockchainMock, aliasServiceMock);
  }

  @Test
  public void isActive_hardcodedHeights_withinConstraints() {
    when(blockchainMock.getHeight()).thenReturn(425000);

    assertTrue(t.isActive(FEATURE_TWO));
  }

  @Test
  public void isActive_hardcodedHeights_beforeConstraints() {
    when(blockchainMock.getHeight()).thenReturn(419999);

    assertFalse(t.isActive(FEATURE_TWO));
  }

  @Test
  public void isActive_hardcodedHeights_afterConstraints() {
    when(blockchainMock.getHeight()).thenReturn(430001);

    assertFalse(t.isActive(FEATURE_TWO));
  }

  @Test
  public void isActive_aliasBoundHeights_withinConstraints() {
    when(blockchainMock.getHeight()).thenReturn(5000);

    final Alias startAlias = mock(Alias.class);
    when(startAlias.getAliasURI()).thenReturn("1000");

    final Alias endAlias = mock(Alias.class);
    when(endAlias.getAliasURI()).thenReturn("9999");

    when(aliasServiceMock.getAlias(eq(DYMAXION_START_BLOCK))).thenReturn(startAlias);
    when(aliasServiceMock.getAlias(eq(DYMAXION_END_BLOCK))).thenReturn(endAlias);

    assertTrue(t.isActive(FEATURE_THREE));
  }
}
