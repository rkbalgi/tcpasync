import com.github.rkbalgi.tcpasync.KeyExtractor;
import com.github.rkbalgi.tcpasync.TcpMessage;
import com.github.rkbalgi.tcpasync.MLI_TYPE;
import com.github.rkbalgi.tcpasync.client.TcpClient;
import com.github.rkbalgi.tcpasync.server.TcpEchoHandler;
import com.github.rkbalgi.tcpasync.server.TcpServer;
import io.netty.buffer.ByteBufUtil;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TcpClientTest {

  public static final int PORT = 7879;

  @BeforeAll
  public static void setupServer() {

    try {
      TcpServer tcpServer = new TcpServer(MLI_TYPE.MLI_2E, "localhost", PORT, new TcpEchoHandler());
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }

  @Test
  @DisplayName("Test for TCP Client with Echo Server")
  public void testClient() throws InterruptedException {

    //let the server initialize
    Thread.sleep(5 * 1000);

    TcpClient.initialize("localhost", PORT, MLI_TYPE.MLI_2E, new KeyExtractor() {
      @Override
      public String getRequestKey(TcpMessage requestMsg) {
        return ByteBufUtil.hexDump(Arrays.copyOfRange(requestMsg.getRequestData(), 0, 4));
      }

      @Override
      public String getResponseKey(TcpMessage responseMsg) {
        return ByteBufUtil.hexDump(Arrays.copyOfRange(responseMsg.getResponseData(), 0, 4));
      }
    });

    long t1 = System.nanoTime();

    TcpMessage reqMsg = null;

    for (int i = 0; i < 5; i++) {
      reqMsg = new TcpMessage(
          ByteBufUtil.decodeHexDump(String.format("%02d", i) + "020304000000007654323456789087"));
      TcpClient.sendSync(reqMsg);
    }
    System.out.printf("Duration: %d ms.\n", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-t1));

    LocalDateTime now = LocalDateTime.now();

    while (true) {
      if (LocalDateTime.now().isAfter(now.plusSeconds(5))) {
        Assert.fail();
      } else {
        if (reqMsg.getResponseCode() != TcpMessage.INVALID) {
          break;
        }
      }
    }

    Thread.sleep(TimeUnit.SECONDS.toMillis(5));

    System.out.println(TcpClient.snapshot());
    TcpClient.shutdown();
  }

}