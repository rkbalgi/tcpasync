package com.github.rkbalgi.tcptester.client;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.github.rkbalgi.tcptester.AsyncHandler;
import com.github.rkbalgi.tcptester.KeyExtractor;
import com.github.rkbalgi.tcptester.MLI_TYPE;
import com.github.rkbalgi.tcptester.TcpMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Logger;


public class TcpClient {

  private static final Logger LOG = Logger.getLogger(TcpClient.class);

  private static final Bootstrap bootstrap = new Bootstrap();
  private static final ConcurrentHashMap<String, TcpMessage> flightMap = new ConcurrentHashMap<String,
      TcpMessage>();
  private static final ScheduledExecutorService timeoutService = Executors
      .newScheduledThreadPool(1);

  private static volatile Channel channel = null;
  private static MLI_TYPE mliType;
  private static KeyExtractor keyExtractor;

  private static final AtomicLong reqCount = new AtomicLong();
  private static final AtomicLong respCount = new AtomicLong();
  private static final AtomicLong timedOutCount = new AtomicLong();

  // TODO:: provide a constructor to create objects of type TcpClient rather than static
  //  initialization

  private static MetricRegistry metrics;
  private static Histogram responseTimeMetric;
  private static final ScheduledExecutorService scheduledExec = Executors.newScheduledThreadPool(2);

  static {
    metrics = new MetricRegistry();
    responseTimeMetric = metrics.histogram("responseTime");
  }


  public static void initialize(String host, int port, MLI_TYPE _mliType,
      KeyExtractor keyExtractorImpl) throws Exception {

    mliType = _mliType;
    keyExtractor = keyExtractorImpl;

    bootstrap.group(new NioEventLoopGroup());
    bootstrap.channelFactory(NioSocketChannel::new);

    bootstrap.option(ChannelOption.WRITE_BUFFER_WATER_MARK,
        new WriteBufferWaterMark(8 * 1024, 32 * 1024));

    bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
    bootstrap.option(ChannelOption.AUTO_READ, true);

    bootstrap.handler(new ChannelInitializer<Channel>() {
      @Override
      protected void initChannel(Channel ch) throws Exception {

        //ch.pipeline().addLast(new LoggingHandler());
        //ch.pipeline().addLast(new TestHandler());
        switch (mliType) {
          case MLI_2E: {
            ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024, 0, 2, 0, 2));
            break;
          }
          case MLI_2I: {
            ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024, 0, 2, -2, 2));
            break;
          }
          default:
            throw new IllegalArgumentException(mliType + " is not supported");
        }

        ch.pipeline().addLast(new TcpClientChannelInboundHandler());


      }
    });

    ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port));
    future.sync();

    channel = future.channel();
    LOG.info(
        String.format("TcpClient connected to server %s, localAddress: %s", channel.remoteAddress(),
            channel.localAddress()));

    scheduledExec.scheduleWithFixedDelay(() -> {

      Snapshot metricSnapshot = responseTimeMetric.getSnapshot();
      LOG.info(
          String.format(
              "Metrics: #requests %d, #responses %d, #timeouts %d -- Response Times: #min(ms) %d, #max(ms) %d, #mean(ms) %f, #75th %f, #95th %f",
              reqCount.get(), respCount.get(), timedOutCount.get(), metricSnapshot.getMin(),
              metricSnapshot.getMax(), metricSnapshot.getMean(), metricSnapshot.get75thPercentile(),
              metricSnapshot.get95thPercentile()));

    }, 30, 30, TimeUnit.SECONDS);


  }

  public static void shutdown() {
    try {
      LOG.info("TcpClient shutting down..");
      timeoutService.shutdown();
      LOG.info("TcpClient awaiting termination...");
      timeoutService.awaitTermination(5, TimeUnit.SECONDS);
      LOG.info("TcpClient terminated.");
    } catch (Exception e) {
      LOG.error("shutdown error", e);
    }
    try {
      channel.close();
    } catch (Exception e) {
      LOG.error("shutdown error", e);
    }
  }

  // sends the request to the server and waits for the response
  public static void sendSync(final TcpMessage tcpReq) {
    send(tcpReq, true);
  }

  private static final ConcurrentHashMap<TcpMessage, AsyncHandler> handlerMap = new ConcurrentHashMap<>();

  // sends the request to the server without waiting for a response, and then executes
  // the handler when a response arrives
  public static void sendAsync(final TcpMessage tcpReq, final AsyncHandler handler) {
    if (handler != null) {
      handlerMap.put(tcpReq, handler);
    }
    send(tcpReq, false);
  }

  private static void send(final TcpMessage tcpReq, boolean sync) {
    try {
      final String key = keyExtractor.getRequestKey(tcpReq);
      ByteBuf buf = Unpooled.buffer(2 + tcpReq.getRequestData().length);

      short prefix = (short) tcpReq.getRequestData().length;
      if (mliType == MLI_TYPE.MLI_2I) {
        prefix += 2;
      }

      buf.writeShort(prefix);
      buf.writeBytes(tcpReq.getRequestData());

      reqCount.incrementAndGet();
      tcpReq.setReqTime(System.nanoTime());

      if (sync) {
        channel.writeAndFlush(buf).sync();
      } else {
        channel.writeAndFlush(buf).addListener((res) -> {
          if (!res.isSuccess()) {
            LOG.error("writeAndFlush ERROR", res.cause());
          }
        });
      }

      flightMap.put(key, tcpReq);
      timeoutService.schedule(() -> {
        TcpMessage tcpReq1 = flightMap.remove(key);
        if (tcpReq1 != null) {
          LOG.warn(String.format("Request = (%s) timed out.", key));
          tcpReq1.timedOut();

          //if this was a async request, call the handler (if there was one)
          AsyncHandler handler = handlerMap.remove(tcpReq1);
          if (handler != null) {
            handler.accept(tcpReq1, null);
          }

        }

      }, 500, TimeUnit.MILLISECONDS);

      if (sync) {
        tcpReq.waitForResponse();
      }

    } catch (Exception e) {
      LOG.error("unexpected exception", e);
    }

  }

  public static String snapshot() {
    return String
        .format("%s::summary requests: %d, responses: %d, timedOut: %d", TcpClient.class.getName(),
            reqCount.get(), respCount.get(), timedOutCount.get());
  }

  public static void receivedMsg(ByteBuf outBuf) {

    TcpMessage responseMsg = new TcpMessage(outBuf.array(), false);
    String key = keyExtractor.getResponseKey(responseMsg);

    TcpMessage tcpReq = flightMap.remove(key);

    if (tcpReq != null) {
      tcpReq.setRespTime(System.nanoTime());
      long totalTime = TimeUnit.MILLISECONDS
          .convert(tcpReq.getRespTime() - tcpReq.getReqTime(), TimeUnit.NANOSECONDS);
      responseTimeMetric.update(totalTime);
      LOG.trace("Received a response with key = " + key + ", totalTime (ms) = " + totalTime);
      respCount.incrementAndGet();
      tcpReq.receivedResponse(outBuf.array());

      //if this was a async request, call the handler (if there is one)
      AsyncHandler handler = handlerMap.remove(tcpReq);
      if (handler != null) {
        handler.accept(tcpReq, outBuf);
      }
    } else {
      LOG.info("Late response: key = " + key);
      timedOutCount.incrementAndGet();
    }

  }

}
