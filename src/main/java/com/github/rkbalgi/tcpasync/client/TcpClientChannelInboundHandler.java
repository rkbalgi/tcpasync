package com.github.rkbalgi.tcpasync.client;

import com.github.rkbalgi.tcpasync.server.TcpServerChannelHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.log4j.Logger;


public class TcpClientChannelInboundHandler extends ChannelInboundHandlerAdapter {

  private static final Logger LOG = Logger.getLogger(TcpServerChannelHandler.class);
  //private static final Executor taskExec = Executors.newCachedThreadPool();

  public TcpClientChannelInboundHandler() {
  }


  public void channelRead(ChannelHandlerContext ctx, Object obj) {

    ByteBuf buf = (ByteBuf) obj;

    ByteBuf outBuf = Unpooled.buffer(buf.readableBytes());
    buf.readBytes(outBuf);
    buf.release();
    TcpClient.receivedMsg(outBuf);
  }


  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOG.error("Exception while handling incoming response: ", cause);
  }
}
