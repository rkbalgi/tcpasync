package com.github.rkbalgi.tcpasync.server;

import com.github.rkbalgi.tcpasync.MLI_TYPE;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by Raghavendra Balgi on 08-05-2015.
 */
public class TcpServer {

  private final ServerBootstrap bootstrap = new ServerBootstrap();

  public TcpServer(MLI_TYPE mliType, String host, int port, TcpServerMessageHandler handler)
      throws InterruptedException {

    EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
    bootstrap.group(eventLoopGroup, eventLoopGroup)

        .channel(NioServerSocketChannel.class)
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel ch) throws Exception {
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

            ch.pipeline().addLast(new TcpServerChannelHandler(mliType, handler));
          }
        })
        .option(ChannelOption.SO_BACKLOG, 128)
        .childOption(ChannelOption.SO_KEEPALIVE, true);

    // Bind and start to accept incoming connections.
    bootstrap.bind(port);


  }

}
