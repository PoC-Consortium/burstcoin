package integration.common;

import static brs.peer.ProcessBlock.ACCEPTED;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import brs.Burst;
import brs.common.Props;
import brs.common.TestInfrastructure;
import brs.peer.Peers;
import brs.peer.ProcessBlock;
import java.util.Properties;
import org.h2.jdbc.JdbcSQLException;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Peers.class)
@PowerMockIgnore("javax.net.ssl.*")
public abstract class AbstractIT {

  private ProcessBlock processBlock;

  protected APISender apiSender = new APISender();

  @Before
  public void setUp() {
    mockStatic(Peers.class);
    Burst.init(testProperties());

    processBlock = new ProcessBlock(Burst.getBlockchain(), Burst.getBlockchainProcessor());
  }

  @After
  public void shutdown() {
    Burst.shutdown(true);
  }

  private Properties testProperties() {
    final Properties props = new Properties();

    props.setProperty(Props.DEV_OFFLINE, "true");
    props.setProperty(Props.DB_URL, TestInfrastructure.IN_MEMORY_DB_URL);
    props.setProperty(Props.DB_MAX_ROLLBACK, "1440");
    props.setProperty(Props.DB_CONNECTIONS, "1");

    props.setProperty(Props.API_SERVER, "on");
    props.setProperty(Props.API_LISTEN, "127.0.0.1");
    props.setProperty(Props.API_PORT,   "" + TestInfrastructure.TEST_API_PORT);
    props.setProperty(Props.API_ALLOWED,   "*");
    props.setProperty(Props.API_UI_DIR, "html/ui");

    return props;
  }

  public void processBlock(JSONObject jsonFirstBlock) {
    assertEquals(ACCEPTED, processBlock.processRequest(jsonFirstBlock, null));
  }
}
