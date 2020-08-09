package com.github.rkbalgi.tcpasync.client;

import com.github.rkbalgi.tcpasync.KeyExtractor;
import com.github.rkbalgi.tcpasync.TcpMessage;
import com.github.rkbalgi.tcpasync.MLI_TYPE;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
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
      .newScheduledThreadPool(2);

  private static volatile Channel channel = null;
  private static MLI_TYPE mliType;
  private static KeyExtractor keyExtractor;

  private static final AtomicLong reqCount = new AtomicLong();
  private static final AtomicLong respCount = new AtomicLong();
  private static final AtomicLong timedOutCount = new AtomicLong();

  // TODO:: provide a constructor to create objects of type TcpClient rather than static
  //  initialization

  public static void initialize(String host, int port, MLI_TYPE _mliType,
      KeyExtractor keyExtractorImpl) {

    mliType = _mliType;
    keyExtractor = keyExtractorImpl;

    bootstrap.group(new NioEventLoopGroup());
    bootstrap.channelFactory(NioSocketChannel::new);
    bootstrap.handler(new ChannelInitializer<>() {
      @Override
      protected void initChannel(Channel ch) throws Exception {
        switch (mliType) {
          case MLI_2E: {
            ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(999, 0, 2, 0, 2));
            break;
          }
          case MLI_2I: {
            ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(999, 0, 2, -2, 2));
            break;
          }
          default:
            throw new IllegalArgumentException(mliType + " is not supported");
        }

        ch.pipeline().addLast(new TcpClientChannelInboundHandler());
      }
    });

    ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port));
    future.awaitUninterruptibly(2, TimeUnit.SECONDS);
    channel = future.channel();
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

  // sends the request to the server and waits for the response
  public static void sendAsync(final TcpMessage tcpReq) {
    send(tcpReq, false);
  }

  private static void send(final TcpMessage tcpReq, boolean sync) {
    try {
      final String key = keyExtractor.getRequestKey(tcpReq);
      ByteBuf buf = Unpooled.buffer(2 + tcpReq
          .getRequestData().length);

      short prefix = (short) tcpReq.getRequestData().length;
      if (mliType == MLI_TYPE.MLI_2I) {
        prefix += 2;
      }

      buf.writeShort(prefix);
      buf.writeBytes(tcpReq.getRequestData());

      flightMap.put(key, tcpReq);

      timeoutService.schedule(() -> {
        TcpMessage tcpReq1 = flightMap.remove(key);
        if (tcpReq1 != null) {
          LOG.warn(String.format("Request = (%s) timed out.", key));
          tcpReq1.timedOut();
        }

      }, 500, TimeUnit.MILLISECONDS);

      reqCount.incrementAndGet();

      channel.writeAndFlush(buf);
      if (sync) {
        tcpReq.waitForResponse();
      }

    } catch (Exception e) {
      LOG.error("unexpected exception", e);
      e.printStackTrace();
    }

  }

  public static String snapshot() {
    return String
        .format("%s::summary requests: %d, responses: %d, timedOut: %d", TcpClient.class.getName(),
            reqCount.get(), respCount.get(),
            timedOutCount.get());
  }

  public static void receivedMsg(ByteBuf outBuf) {

    TcpMessage responseMsg = new TcpMessage(outBuf.array(), false);

    String key = keyExtractor.getResponseKey(responseMsg);
    LOG.debug("Received a response with key = " + key);
    TcpMessage tcpReq = flightMap.remove(key);
    if (tcpReq != null) {
      respCount.incrementAndGet();
      tcpReq.receivedResponse(outBuf.array());
    } else {
      timedOutCount.incrementAndGet();
    }

  }

}
