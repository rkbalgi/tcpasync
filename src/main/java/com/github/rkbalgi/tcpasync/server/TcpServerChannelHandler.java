package com.github.rkbalgi.tcpasync.server;

import com.github.rkbalgi.tcpasync.MLI_TYPE;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Logger;


/**
 * Created by Raghavendra Balgi on 08-05-2015.
 */
public class TcpServerChannelHandler extends ChannelInboundHandlerAdapter {

  private static final Logger log = Logger.getLogger(TcpServerChannelHandler.class);
  private final MLI_TYPE mliType;
  private final TcpServerMessageHandler handler;


  public TcpServerChannelHandler(MLI_TYPE mliType, TcpServerMessageHandler handler) {
    this.mliType = mliType;
    this.handler = handler;

  }


  public void channelActive(ChannelHandlerContext ctx) {

    if (log.isDebugEnabled()) {
      log.debug(String.format("channel connected from [%s] ", ctx.channel().remoteAddress()));
    }

  }

  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    log.error("error on channel", cause);
  }

  public void channelRead(ChannelHandlerContext ctx, Object obj) {
    ByteBuf buf = (ByteBuf) obj;

    log.debug("Message Received - \n" + ByteBufUtil.hexDump(buf));

    try {
      if (log.isDebugEnabled()) {
        log.debug(String
            .format("message received from [%s] = %s", ctx.channel().remoteAddress(),
                ByteBufUtil.hexDump(buf)));
      }
      handler.handleMessage(ctx.channel(), buf);
    } finally {
      buf.release();
    }



    /*try {

      buf.markReaderIndex();
      if (buf.readableBytes() > 2) {
        int n = buf.readShort();
        if (mliType == MLI_TYPE.MLI_2I) {
          n = n - 2;
        }
        if (buf.readableBytes() >= n) {
          byte[] data = new byte[n];
          buf.readBytes(data);
          if (log.isDebugEnabled()) {
            log.debug(String
                .format("message received from [%s] = %s", ctx.channel().remoteAddress(),
                    ByteBufUtil.hexDump(data)));
          }
          handler.handleMessage(ctx.channel(), data);
        } else {
          buf.resetReaderIndex();
        }
      } else {
        buf.resetReaderIndex();
      }
    } finally {
      buf.release();
    }*/

  }

}
