package com.github.rkbalgi.tcpasync.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.apache.log4j.Logger;

/**
 * Created by Raghavendra Balgi on 08-05-2015.
 */
public class TcpEchoHandler implements TcpServerMessageHandler {

  private static final Logger log = Logger.getLogger(TcpEchoHandler.class);

  @Override
  public void handleMessage(Channel channel, ByteBuf data) {
    ByteBuf buf = Unpooled.buffer(2 + data.readableBytes());
    buf.writeShort(data.readableBytes());//2E
    buf.writeBytes(data);
    if (log.isDebugEnabled()) {
      log.debug("Echo Response = \n" + ByteBufUtil.prettyHexDump(buf));
    }
    channel.writeAndFlush(buf);
  }
}
