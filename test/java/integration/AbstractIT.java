package integration;

import static org.powermock.api.mockito.PowerMockito.mockStatic;

import brs.Burst;
import brs.common.Props;
import brs.peer.Peers;
import java.util.Properties;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Peers.class)
public abstract class AbstractIT {

  @Before
  public void setUp() {
    mockStatic(Peers.class);
    Burst.init(testProperties());
  }

  private Properties testProperties() {
    final Properties props = new Properties();

    props.setProperty(Props.DEV_OFFLINE, "true");
    props.setProperty(Props.DB_URL, "jdbc:h2:mem:test;");
    props.setProperty(Props.BRS_MAX_ROLLBACK, "1440");
    props.setProperty(Props.DB_CONNECTIONS, "1");

    return props;
  }
}
