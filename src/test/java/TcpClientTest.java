import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.github.rkbalgi.tcpasync.KeyExtractor;
import com.github.rkbalgi.tcpasync.TcpMessage;
import com.github.rkbalgi.tcpasync.MLI_TYPE;
import com.github.rkbalgi.tcpasync.client.TcpClient;
import com.github.rkbalgi.tcpasync.server.TcpEchoHandler;
import com.github.rkbalgi.tcpasync.server.TcpServer;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.netty.buffer.ByteBufUtil;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
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
  public void testWithRustServer() throws InterruptedException {

    //
    TcpClient.initialize("localhost", 6666, MLI_TYPE.MLI_2E, new KeyExtractor() {
      @Override
      public String getRequestKey(TcpMessage requestMsg) {
        return ByteBufUtil.hexDump(Arrays.copyOfRange(requestMsg.getRequestData(), 0, 4));
      }

      @Override
      public String getResponseKey(TcpMessage responseMsg) {
        return ByteBufUtil.hexDump(Arrays.copyOfRange(responseMsg.getResponseData(), 0, 4));
      }
    });

    MetricRegistry metrics = new MetricRegistry();
    Histogram responseTimeMetric = metrics.histogram("responseTime");

    RateLimiterConfig rlConfig = RateLimiterConfig.custom()
        .limitRefreshPeriod(Duration.ofSeconds(1)).limitForPeriod(5).build();
    RateLimiter rl = RateLimiter.of("tps", rlConfig);

    ForkJoinPool.commonPool().execute(() -> {

      try {
        Thread.sleep(TimeUnit.SECONDS.toMillis(2));
        rl.changeLimitForPeriod(10);
        Thread.sleep(TimeUnit.SECONDS.toMillis(2));
        rl.changeLimitForPeriod(20);
        Thread.sleep(TimeUnit.SECONDS.toMillis(2));
        rl.changeLimitForPeriod(50);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

    });

    long t1 = System.nanoTime();

    TcpMessage reqMsg = null;

    while (true) {
      reqMsg = new TcpMessage(
          ByteBufUtil.decodeHexDump(
              "31343230f02420000000000080000001000000000000000100000000313231323334353637383931303130303430303030303030303030303031393937373935383132323034f8f4f03132333435363738"));
      if (rl.acquirePermission()) {
        TcpClient.sendAsync(reqMsg);
      }

      /*if (i < 2000) {
        Thread.sleep(10);
      } else {
        Thread.sleep(100);
      }*/
    }
    //System.out.printf("Duration: %d ms.\n", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t1));

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
    System.out.printf("Duration: %d ms.\n", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t1));

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