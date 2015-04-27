package com.github.rkbalgi.tcpasync;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TcpAsync {

    private static final Log log = LogFactory.getLog(TcpAsync.class);

    private static final ClientBootstrap clientBootstrap = new ClientBootstrap();
    private static ConcurrentHashMap<String, LengthPrefixedTcpMessage> flightMap = new ConcurrentHashMap<String,
            LengthPrefixedTcpMessage>();
    private static ScheduledExecutorService timeoutService = Executors
            .newScheduledThreadPool(2);

    private static volatile Channel channel = null;
    private static MLI_TYPE mliType;
    private static KeyExtractor keyExtractor;

    public static void initialize(String host, int port, MLI_TYPE _mliType, KeyExtractor keyExtractorImpl) {

        mliType = _mliType;
        keyExtractor = keyExtractorImpl;

        clientBootstrap.setFactory(new NioClientSocketChannelFactory(Executors
                .newFixedThreadPool(4), Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1)));
        clientBootstrap.getPipeline().addLast("handler",
                new LengthPrefixedTcpChannelHandler(mliType));
        ChannelFuture future = clientBootstrap.connect(new InetSocketAddress(
                host, port));
        future.awaitUninterruptibly(2, TimeUnit.SECONDS);
        channel = future.getChannel();
    }

    public static void shutdown() {
        try {
            timeoutService.shutdownNow();
        } catch (Exception e) {
            log.error("shutdown error", e);
        }
        try {
            channel.close();
            clientBootstrap.releaseExternalResources();
        } catch (Exception e) {
            log.error("shutdown error", e);
        }
    }


    public static void send(final LengthPrefixedTcpMessage tcpReq) {
        try {
            final String key = keyExtractor.getRequestKey(tcpReq);
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer(2 + tcpReq
                    .getRequestData().length);

            short prefix = (short) tcpReq.getRequestData().length;
            if (mliType == MLI_TYPE.MLI_2I) {
                prefix += 2;
            }

            buf.writeShort(prefix);
            buf.writeBytes(tcpReq.getRequestData());
            flightMap.put(key, tcpReq);
            timeoutService.schedule(new Runnable() {

                @Override
                public void run() {
                    LengthPrefixedTcpMessage tcpReq = flightMap.remove(key);
                    if (tcpReq != null) {
                        log.warn(String.format("Request = (%s) timed out.",
                                key));
                        tcpReq.timedOut();
                    }

                }
            }, 500, TimeUnit.MILLISECONDS);

            channel.write(buf);
            tcpReq.waitForResponse();


        } catch (Exception e) {
            log.error("unexpected exception", e);
            e.printStackTrace();
        }

    }

    public static void receivedMsg(ChannelBuffer outBuf) {

        LengthPrefixedTcpMessage responseMsg=new LengthPrefixedTcpMessage(outBuf.array(),false);

        String key = keyExtractor.getResponseKey(responseMsg);
        LengthPrefixedTcpMessage tcpReq = flightMap.remove(key);
        if (tcpReq != null) {
            tcpReq.receivedResponse(outBuf.array());
        }

    }

}
