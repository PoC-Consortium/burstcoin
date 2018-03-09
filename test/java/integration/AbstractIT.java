package integration;

import static org.powermock.api.mockito.PowerMockito.mockStatic;

import brs.Burst;
import brs.common.Props;
import brs.common.TestInfrastructure;
import brs.peer.Peers;
import java.util.Properties;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Peers.class)
@PowerMockIgnore("javax.net.ssl.*")
public abstract class AbstractIT {

  @Before
  public void setUp() {
    mockStatic(Peers.class);
    Burst.init(testProperties());
  }

  private Properties testProperties() {
    final Properties props = new Properties();

    props.setProperty(Props.DEV_OFFLINE, "true");
    props.setProperty(Props.DB_URL, TestInfrastructure.IN_MEMORY_DB_URL);
    props.setProperty(Props.DB_MAX_ROLLBACK, "1440");
    props.setProperty(Props.DB_CONNECTIONS, "1");

    props.setProperty(Props.API_SERVER, "on");
    props.setProperty(Props.API_LISTEN, "127.0.0.1");
    props.setProperty(Props.API_PORT,   "8125");

    return props;
  }
}
