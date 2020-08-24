package com.github.rkbalgi.tcptester.client;

import io.netty.channel.ChannelDuplexHandler;
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
