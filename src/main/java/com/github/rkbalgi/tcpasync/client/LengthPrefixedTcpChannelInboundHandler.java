package com.github.rkbalgi.tcpasync.client;

import com.github.rkbalgi.tcpasync.MLI_TYPE;
import com.github.rkbalgi.tcpasync.client.TcpClient;
import com.github.rkbalgi.tcpasync.server.TcpServerChannelHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Logger;


public class LengthPrefixedTcpChannelInboundHandler extends ChannelInboundHandlerAdapter {

  private static final Logger LOG = Logger.getLogger(TcpServerChannelHandler.class);
  private final MLI_TYPE mliType;

  public LengthPrefixedTcpChannelInboundHandler(MLI_TYPE mliType) {
    this.mliType = mliType;

  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    ctx.fireChannelActive();
  }

  public void channelRead(ChannelHandlerContext ctx, Object obj) {

    ByteBuf buf = (ByteBuf) obj;
    try {
      LOG.debug(String.format("Received - %s, with %d bytes available", ByteBufUtil.hexDump(buf),
          buf.readableBytes()));
      buf.markReaderIndex();

      if (buf.readableBytes() > 2) {

        int n = buf.readShort();
        if (mliType == MLI_TYPE.MLI_2I) {
          n -= 2;
        }
        if (buf.readableBytes() >= n) {
          ByteBuf outBuf = Unpooled.buffer(n);
          buf.readBytes(outBuf);
          TcpClient.receivedMsg(outBuf);
          return;
        }
      }

      buf.resetReaderIndex();
    } finally {
      buf.release();
    }

  }

  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
  }
}
