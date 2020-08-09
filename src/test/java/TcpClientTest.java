import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

import com.github.rkbalgi.tcpasync.KeyExtractor;
import com.github.rkbalgi.tcpasync.LengthPrefixedTcpMessage;
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

  @BeforeAll
  public static void setupServer() {

    try {
      TcpServer tcpServer = new TcpServer(MLI_TYPE.MLI_2E, "localhost", 7879, new TcpEchoHandler());
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }

  @Test
  @DisplayName("Test for TCP Client with Echo Server")
  public void testClient() throws InterruptedException {

    //let the server initialize
    Thread.sleep(5 * 1000);

    TcpClient.initialize("localhost", 7879, MLI_TYPE.MLI_2E, new KeyExtractor() {
      @Override
      public String getRequestKey(LengthPrefixedTcpMessage requestMsg) {
        return ByteBufUtil.hexDump(Arrays.copyOfRange(requestMsg.getRequestData(), 0, 4));
      }

      @Override
      public String getResponseKey(LengthPrefixedTcpMessage responseMsg) {
        return ByteBufUtil.hexDump(Arrays.copyOfRange(responseMsg.getResponseData(), 0, 4));
      }
    });

    LengthPrefixedTcpMessage reqMsg = null;
    for (int i = 0; i < 5; i++) {
      reqMsg = new LengthPrefixedTcpMessage(
          ByteBufUtil.decodeHexDump(String.format("%02d", i) + "020304000000007654323456789087"));
      TcpClient.sendAsync(reqMsg);
    }

    LocalDateTime now = LocalDateTime.now();

    while (true) {
      if (LocalDateTime.now().isAfter(now.plusSeconds(5))) {
        Assert.fail();
      } else {
        if (reqMsg.getResponseCode() != LengthPrefixedTcpMessage.INVALID) {
          break;
        }
      }
    }

    Thread.sleep(TimeUnit.SECONDS.toMillis(5));

    System.out.println(TcpClient.snapshot());
    TcpClient.shutdown();
  }

}