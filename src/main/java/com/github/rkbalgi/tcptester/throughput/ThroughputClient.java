package com.github.rkbalgi.tcptester.throughput;

import com.github.rkbalgi.tcptester.KeyExtractor;
import com.github.rkbalgi.tcptester.MLI_TYPE;
import com.github.rkbalgi.tcptester.MessageBuilder;
import com.github.rkbalgi.tcptester.TcpMessage;
import com.github.rkbalgi.tcptester.client.TcpClient;
import com.google.common.base.Preconditions;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import java.time.Duration;
import java.util.Objects;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class ThroughputClient {

  private static final Logger LOG = LogManager.getLogger(ThroughputClient.class);

  // default rate = 5 requests/sec
  private static final int DEFAULT_INITIAL_THROUGHPUT = 5;

  //throughput expressed in requests/sec
  private volatile int throughput = DEFAULT_INITIAL_THROUGHPUT;
  private MessageBuilder messageBuilder;
  private KeyExtractor keyExtractor;
  private MLI_TYPE mliType;
  private String host;
  private int port;


  public static class Builder {

    private int initialThroughput = DEFAULT_INITIAL_THROUGHPUT;
    private MessageBuilder messageBuilder;
    private KeyExtractor keyExtractor;
    private MLI_TYPE mliType;
    private String host;
    private int port;

    public int getInitialThroughput() {
      return initialThroughput;
    }

    /**
     * Sets initial throughput expressed in requests/sec
     */
    public Builder setInitialThroughput(int initialThroughput) {
      this.initialThroughput = initialThroughput;
      return this;
    }

    public MessageBuilder getMessageBuilder() {
      return messageBuilder;
    }

    public Builder setMessageBuilder(MessageBuilder messageBuilder) {
      this.messageBuilder = messageBuilder;
      return this;
    }

    public KeyExtractor getKeyExtractor() {
      return keyExtractor;
    }

    public Builder setKeyExtractor(KeyExtractor keyExtractor) {
      this.keyExtractor = keyExtractor;
      return this;
    }

    public ThroughputClient build() {

      ThroughputClient throughputClient = new ThroughputClient();

      throughputClient.keyExtractor = Objects
          .requireNonNull(this.keyExtractor, "keyExtractor cannot be null");
      throughputClient.host = Objects
          .requireNonNull(this.host, "host cannot be null");
      throughputClient.mliType = Objects
          .requireNonNull(this.mliType, "mliType cannot be null");
      Preconditions.checkArgument(port > 0 && port <= 65536, "invalid port");
      throughputClient.port = port;

      throughputClient.messageBuilder = Objects
          .requireNonNull(this.messageBuilder, "messageBuilder cannot be null");
      throughputClient.throughput = this.initialThroughput;

      return throughputClient;
    }

    public MLI_TYPE getMliType() {
      return mliType;
    }

    public Builder setMliType(MLI_TYPE mliType) {
      this.mliType = mliType;
      return this;
    }

    public String getHost() {
      return host;
    }

    public Builder setHost(String host) {
      this.host = host;
      return this;
    }

    public int getPort() {
      return port;
    }

    public Builder setPort(int port) {
      this.port = port;
      return this;
    }
  }

  private ThroughputClient() {

  }

  public void setThroughput(int newThroughput) {
    this.throughput = newThroughput;
  }

  /**
   * Starts the throughput client, generating message using the configured message builder and the
   * other properties like throughput set via the builder
   */
  public void start() {
    RateLimiterConfig rlConfig = RateLimiterConfig.custom()
        .limitRefreshPeriod(Duration.ofSeconds(1)).limitForPeriod(throughput).build();
    RateLimiter rl = RateLimiter.of("tps", rlConfig);

    new Thread(() -> {
      while (true) {
        if (throughput != rl.getRateLimiterConfig().getLimitForPeriod()) {
          LOG.info("Changing request rate (tps) to - " + throughput);
          rl.changeLimitForPeriod(throughput);
        }
        try {
          Thread.sleep(Duration.ofSeconds(30).toMillis());
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }).start();

    TcpClient.initialize(host, port, mliType, keyExtractor);

    try {
      while (true) {

        TcpMessage reqMsg = messageBuilder.newMessage();
        if (rl.acquirePermission()) {
          TcpClient.sendSync(reqMsg);
        }

      }//end while
    } catch (Exception e) {
      LOG.error("Exception while sending message", e);
    }

  }

}
