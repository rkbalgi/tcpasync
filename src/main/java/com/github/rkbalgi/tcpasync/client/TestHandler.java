package com.github.rkbalgi.tcpasync.client;

import com.github.rkbalgi.tcpasync.server.TcpServerChannelHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.apache.log4j.Logger;

public class TestHandler extends ChannelDuplexHandler {


  private static final Logger LOG = Logger.getLogger(TestHandler.class);


/*  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    System.out.println("writing ... ");
    super.write(ctx, msg, promise);
  }*/

/*  @Override
  public void flush(ChannelHandlerContext ctx) throws Exception {
    System.out.println("flushing ... ");
    ctx.flush();
  }*/
}
