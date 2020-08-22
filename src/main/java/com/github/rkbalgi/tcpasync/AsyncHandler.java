package com.github.rkbalgi.tcpasync;

import io.netty.buffer.ByteBuf;
import io.vavr.Function2;
import java.util.function.BiConsumer;

public interface AsyncHandler extends BiConsumer<TcpMessage, ByteBuf> {

  public void accept(TcpMessage tcpReqMessage, ByteBuf responseBuf);

}
