package com.github.rkbalgi.tcpasync.client;

import com.github.rkbalgi.tcpasync.server.TcpServerChannelHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Logger;


public class TcpClientChannelInboundHandler extends ChannelInboundHandlerAdapter {

  private static final Logger LOG = Logger.getLogger(TcpServerChannelHandler.class);

  public TcpClientChannelInboundHandler() {
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    ctx.fireChannelActive();
  }

  public void channelRead(ChannelHandlerContext ctx, Object obj) {

    ByteBuf buf = (ByteBuf) obj;

    try {
      ByteBuf outBuf = Unpooled.buffer(buf.readableBytes());
      buf.readBytes(outBuf);
      TcpClient.receivedMsg(outBuf);
    } finally {
      buf.release();
    }


  }

  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOG.error("Exception while handling incoming response: ", cause);
  }
}
